#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():
    startup()

    newSize = 0
    oldSize = 0
    while True:

    	evrs = get_evr()
    	newSize = len(evrs)
        [print(_evr) for _evr in evrs[oldSize:newSize]]
    	oldSize = newSize
    	wait(5)

    shutdown()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
