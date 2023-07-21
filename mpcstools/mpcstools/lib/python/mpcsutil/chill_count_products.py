#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
    A little script that will take in a directory name as an argument
    (or start with your current directory if run with no arguments) and
    recursively traverse the directory structure underneath and count the number
    of products (.dat files only) that it finds.
"""

from __future__ import (absolute_import, division, print_function)

import os
import os.path
import sys

def count_products(dir):

    print('Current dir = %s' % (dir))
    os.chdir(dir)

    total = 0
    files = []
    dirs = []

    #Split up all the files in the current directory into
    #subdirectories or .dat files
    for entry in os.listdir(dir):
        if os.path.isdir(entry):
            dirs.append(entry)
        elif os.path.isfile(entry) and entry.endswith('.dat'):
            files.append(entry)

    print('\tdirs = %d' % (len(dirs)))
    print('\tfiles = %d' % (len(files)))

    #Recursively count each of the subdirectories
    for subdir in dirs:
        print('')
        total += count_products(os.path.abspath(subdir))
        os.chdir('..')

    #Return the total count of .dat files from all the subdirectories
    #plus the count of .dat files from this directory
    return total + len(files)

def test():

    starting_dir = os.curdir
    if len(sys.argv) == 2:
        starting_dir = sys.argv[1]
    starting_dir = os.path.abspath(starting_dir)

    total_products = count_products(dir=starting_dir)
    print('\nTotal Products = %d\n' % (total_products))

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
