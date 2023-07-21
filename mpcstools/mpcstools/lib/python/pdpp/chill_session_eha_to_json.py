#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import json
import sys
import subprocess as SP
import os
import argparse
import contextlib
import tarfile
import tempfile
import shutil

''' A test script that will create a JSON file of channel values that is used with the PDPPExtractTester test tool. '''

COLS = ['SCLK', 'ChannelId', 'Status', 'SCET', 'DataNumber', 'DNAlarmState', 'EUAlarmState', 'SessionVcId']

COLS = {3  : "ChannelId",
        8  : "SCET",
        10 : "SCLK",
        11 : "DataNumber",
        12 : "EU",
        13 : "Status",
        14 : "DNAlarmState",
        15 : "EUAlarmState",
        17 : "ChannelType",}

EU_COLS = [12,15]

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


def convert_line_to_dict(line):
    d = {}

    values = line.strip().replace('"', '').split(",")

    has_eu = True if values[12] else False

    for index, name in COLS.items():
        if index not in EU_COLS or has_eu:
            d[name] = values[index]

    return d

def convert_line_to_dict_one_line_summary(line, keep_vals=None):
    '''Not used'''
    d = {}

    for l in line.strip().split(" "):
        kv = l.split("=")
        if len(kv) != 2:
            continue

        if keep_vals is not None and kv[0] not in keep_vals:
            continue
        else:
            d[kv[0]] = kv[1]

    return d

def get_session_products(sid):
    products = []
    proc = SP.Popen(os.path.expandvars("$CHILL_GDS/bin/chill_get_products -o filenameonly -K {}".format(sid)), shell=True, stdout=SP.PIPE)
    stdout, stderr = proc.communicate()
    stdout=stdout.decode('utf-8') if isinstace(stdout, bytes) else stdout
    for line in stdout.split('\n'):
        if not line:
            continue

        products.append(line.strip())

    return products

def get_session_chanvals(sid):
    chanvals = []

    proc = SP.Popen(os.path.expandvars("$CHILL_GDS/bin/chill_get_chanvals --channelTypes r -K {}".format(sid)), shell=True, stdout=SP.PIPE)
    stdout, stderr = proc.communicate()
    stdout=stdout.decode('utf-8') if isinstace(stdout, bytes) else stdout
    for line in stdout.split('\n'):
        if not line:
            continue

        d = convert_line_to_dict(line)
        chanvals.append(d)

    return chanvals

@contextlib.contextmanager
def cd(newdir, cleanup=lambda: True):
    prevdir = os.getcwd()
    os.chdir(os.path.expanduser(newdir))
    try:
        yield
    finally:
        os.chdir(prevdir)
        cleanup()

@contextlib.contextmanager
def tempdir():
    dirpath = tempfile.mkdtemp()
    def cleanup():
        shutil.rmtree(dirpath)
    with cd(dirpath, cleanup):
        yield dirpath

def build_data_and_emd_files(product_path):
    '''Creates a pair of full paths for product_path.
    Returns (product_path, product_path pair)
    '''
    base, ext = os.path.splitext(product_path)

    pair_ext = get_ext(ext)

    if pair_ext is None:
        print("Bogus file with extension {}".format(product_path))
        return {}
    else:
        emd = "{}{}".format(base, ext if ext in emds else pair_ext)
        dat = "{}{}".format(base, ext if ext in dats else pair_ext)

        return dict(dat=dat, emd=emd)




def parse_args():
      parser = argparse.ArgumentParser("Creates a JSON file to be used with the PDPPExtractTester.")
      parser.add_argument("-K", "--sessionId", action="store", dest="sid", type=int, required=True, help="Session ID to be queried to create channel values.")
      parser.add_argument("-f", "--outputDirectory", action="store", dest="opdir", required=True, help="The output directory to write the final TAR archive file to.")

      return parser.parse_args()


def fake_join(*args):
    '''Join has some weird behavior (in my book) so this will join absolute paths.'''
    replace_args = []

    for a in args:
        replace_args.append(a.replace("/", "%"))

    joined = os.path.join(*replace_args)

    return joined.replace("%", "/").replace("//", "/")


def test():
    args = parse_args()

    if 'CHILL_GDS' not in os.environ.keys("CHILL_GDS"):
        exit("CHILL_GDS must be set")

    sid = args.sid
    opdir = args.opdir

    chanvals = get_session_chanvals(sid)
    products = get_session_products(sid)

    with tempdir() as dirpath:
        chanfile = os.path.join(dirpath, "expected_chanvals.json")

        products_dir = os.path.join(dirpath, "PRODUCTS")
        os.makedirs(products_dir)

        with open(chanfile, "w") as cfile:
            json.dump(chanvals, cfile, indent=True)

            for product in products:
                prod_files = build_data_and_emd_files(product)

                for pf in prod_files.values():
                    new_path = fake_join(products_dir, pf)

                    d = os.path.dirname(new_path)

                    if not os.path.isdir(d):
                        os.makedirs(d)

                    shutil.copy(pf, new_path)

        tf = tarfile.open(os.path.join(opdir, "extract-test-archive.tar.gz"), "w:gz")
        tf.add(dirpath, arcname="extract")
        tf.close()

        print("Created extract archive file {}".format(tf.name))

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
