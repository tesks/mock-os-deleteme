#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import json
import os
import shutil
import tempfile
import contextlib
import tarfile
import argparse

import six
if six.PY2:
    import MySQLdb as mysql
else:
    import pymysql as mysql


extensions = {
    ".dat" : ".emd",
    ".daz" : ".emz",
    ".pdat" : ".pemd",
    ".pdaz" : ".pemz",
    ".emd" : ".dat",
    ".emz" : ".daz",
    ".pemd" : ".pdat",
    ".pemz" : ".pdaz"
    }

emds = [".emd", ".emz", ".pemd", ".pemz"]
dats = [".dat", ".daz", ".pdat", ".pdaz"]

get_ext = lambda ext: extensions.get(ext)


class PDPPSetup:

    def __init__(self, sid, pdpp_database, opdir, name, password="", user=None, build_id=None):
        self.sid = sid
        self.pdpp_database = pdpp_database
        self.password = password
        self.user = user
        self.opdir = opdir
        self.name = name
        self.match_build_id = build_id

        self.tf = None
        self.build_ids = []

    def run(self):
        '''Runs the whole deal'''
        products = self.build_products()

        with self.tempdir() as dirpath:
            self.build_products_directory(products, dirpath)

            with open(os.path.join(dirpath, "product_map.json"), "w") as jfile:
                json.dump(self.flatten(products), jfile, indent=True)

            # write the dictionary build id to a file as well.
            if len(self.build_ids) > 1:
                print("WARNING: Multiple build ids found, some of the files will fail being processed by PDPP: ids =>" + str(self.build_ids))

            with open(os.path.join(dirpath, "dictionary.txt"), "w") as dfile:
                dfile.write(str(self.build_ids[0]))

            self.tf = tarfile.open(os.path.join(opdir, "%s.tar.gz" % self.name), "w:gz")
            self.tf.add(dirpath, arcname="PDPP")
            self.tf.close()



        print("Completed creating product test directory archive for session %s" % self.sid)
        print("Archive file: " + self.tf.name)

    def flatten_product(self, product):
        '''Flattens a product by merging the keys.'''
        fproduct = {}

        for type_key, paths in product.items():
            for file_key, ppath in paths.items():
                fproduct["-".join((type_key, file_key))] = ppath

        return fproduct

    def flatten(self, products):
        '''Flattens each product entry.  This makes it easier for the test suite to import the json
        file and create objects using jackson.'''
        # Each product is a map of maps.  Flatten it out by merging keys.
        flat_products = []

        for product in products:
            flat_products.append(self.flatten_product(product))

        return flat_products

    def build_data_and_emd_files(self, product_path):
        '''Creates a pair of full paths for product_path.
        Returns (product_path, product_path pair)
        '''
        base, ext = os.path.splitext(product_path)

        pair_ext = get_ext(ext)

        if pair_ext is None:
            print("Bogus file with extension " + product_path)
            return {}
        else:
            emd = "%s%s" % (base, ext if ext in emds else pair_ext)
            dat = "%s%s" % (base, ext if ext in dats else pair_ext)

            return dict(dat=dat, emd=emd)


    def build_products(self):
        '''Fetches the products from the database for sid and creates a map for parent, child_master and child.
        Parent will be the product that is going to be processed for the test to create child.  child_master will be
        used to compare the product.
        '''
        db = mysql.connect(db=self.pdpp_database, user=self.user, passwd=self.password)

        db.query("select productPath, parent, fswBuildId from products where sessionId=%s" % self.sid)
        results = db.store_result()

        rows = results.fetch_row()

        products = []
        while rows:
            for product_path, parent_id, build_id in rows:
                if self.match_build_id is not None and build_id != self.match_build_id:
                    continue

                if build_id not in self.build_ids:
                    self.build_ids.append(build_id)

                db.query("select productPath from products where productId=%s" % parent_id)
                pr = db.store_result()
                parent = pr.fetch_row()[0][0]

                product = dict(parent=self.build_data_and_emd_files(parent), expected=self.build_data_and_emd_files(product_path))
                products.append(product)

            rows = results.fetch_row()

        return products

    @contextlib.contextmanager
    def cd(self, newdir, cleanup=lambda: True):
        prevdir = os.getcwd()
        os.chdir(os.path.expanduser(newdir))
        try:
            yield
        finally:
            os.chdir(prevdir)
            cleanup()

    @contextlib.contextmanager
    def tempdir(self):
        dirpath = tempfile.mkdtemp()
        def cleanup():
            shutil.rmtree(dirpath)
        with self.cd(dirpath, cleanup):
            yield dirpath

#     def copy_product(self, product, base_dir):
#         for ppath in product.values():
#             dest = os.path.join(base_dir, ppath)
#
#             if not os.path.isdir(os.path.dirname(dest)):
#                 os.makedirs(os.path.dirname(dest))
#
#             base, ext = os.path.splitext(ppath)
#
#             if ext.ends_with("pdat"):
#                 print "dskfjsd"
#
#             #

    def fake_join(self, *args):
        '''Join has some weird behavior (in my book) so this will join absolute paths.'''
        replace_args = []

        for a in args:
            replace_args.append(a.replace("/", "%"))

        joined = os.path.join(*replace_args)

        return joined.replace("%", "/").replace("//", "/")

    def build_products_directory(self, products, dirpath):
        '''Copies files into a temp directory.  parents will be copied into a PRODUCTS directory and a directory
        called EXPECTED will be created as well.  An entry in each product will be added for the expected
        file

        Returns the temp directory path.
        '''
        pdir = os.path.join(dirpath, "PRODUCTS")
        # Already processed so
        edir = os.path.join(dirpath, "EXPECTED")

        os.makedirs(pdir)
        os.makedirs(edir)

        ## Copy the parents to the PRODUCTS directory.
        for product in products:
            for ptype, files in product.items():
                dest_dir = pdir if ptype == "parent" else edir

                for ppath in files.values():
                    dest = self.fake_join(dest_dir, ppath)
                    p = os.path.dirname(dest)

                    # Build the path.
                    if not os.path.isdir(p):
                        os.makedirs(p)

                    shutil.copy(ppath, dest)

            # After moving everything update the product paths.
            product["expected"] = self.build_data_and_emd_files(self.fake_join("EXPECTED", product.get("expected", {}).get("dat", "UNKNOWN")))
            product["parent"] = self.build_data_and_emd_files(self.fake_join("PRODUCTS", product.get("parent", {}).get("dat", "UNKNOWN")))


def parse_args():
    parser = argparse.ArgumentParser("Creates an archive TAR file of products for the input session to be used with one of the PDPP Product test suites.")
    parser.add_argument("-K", "--sessionId", action="store", dest="sid", type=int, required=True, help="Session ID to be queried to create channel values.")
    parser.add_argument("-f", "--outputDirectory", action="store", dest="opdir", required=True, help="The output directory to write the final TAR archive file to.")
    parser.add_argument("-n", "--archiveName", action="store", dest="name", required=True, help="The name of the output file.")
    parser.add_argument("-u", "--user", action="store", dest="user", default="mpcs", help="The mysql user to use when connecting to the PDPP database.  Defaults to 'mpcs'")
    parser.add_argument("-p", "--pwd", action="store", dest="pwd", default="", help="The mysql password to use when connecting to the PDPP database.  Defaults to empty string.")
    parser.add_argument("-D", "--database", action="store", dest="database", default="pdpp_automation_v1_1_0", help="The mysql user to use when connecting to the PDPP database.  Defaults to 'pdpp_automation_v2_0_0'")
    parser.add_argument("-b", "--buildId", action="store", type=int, dest="build_id", help="If there are multiple build ids found only products matching this will be used.")

    return parser.parse_args()


def test():
    args = parse_args()

    sid = args.sid
    pdpp_database = args.database
    opdir = args.opdir
    user = args.user
    pwd = args.pwd
    name = args.name
    build_id = args.build_id

    pdpp = PDPPSetup(sid, pdpp_database, opdir, name, pwd, user, build_id)
    pdpp.run()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
