#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Created on Oct 6, 2010

"""

from __future__ import (absolute_import, division, print_function)

import errno
import os
import shutil

def mkdir_p(path):
    '''Python equivalent of the Unix command "mkdir -p" that
    creates a directory and all of its subdirectories.'''

    try:
        os.makedirs(path)
    except OSError as exc: # Python >2.5
        if exc.errno == errno.EEXIST:
            return
        raise

def copyfile(src,dst,skip_duplicates=True):

    if skip_duplicates and src == dst:
        return

    shutil.copyfile(src, dst)

def files_from_dirs(dirs, filename):
    """ Given a list of directories and a file name, return a list of paths as if the file were in each directory """

    return [os.path.join(single_dir, filename) for single_dir in dirs]

def get_file(dirs, filename):
    """
    Given a list of directories and a filename, return a list of file paths resulting from
    concatenating the filename with each directory.  Only include such a path if the file exists.
    """

    for single_file in files_from_dirs(dirs, filename):
        if os.path.exists(single_file):
            return single_file
    return ''

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
