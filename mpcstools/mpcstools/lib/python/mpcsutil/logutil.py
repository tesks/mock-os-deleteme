#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import logging

class NoOpHandler(logging.StreamHandler):
    """
    A handler class which writes does nothing but prevent a logger from complaining
    that it has no handlers."""

    def __init__(self):
        """Initialize the handler."""

        logging.Handler.__init__(self)

    def flush(self):
        """Does nothing."""
        pass

    def emit(self, record):
        """Does nothing."""
        pass

class DenyAllFilter(logging.Filter):
    """Just reject everything."""

    def __init__(self, name=''):

        logging.Filter.__init__(self,'DenyAll')

    def filter(self, record):
        """Deny everything by always returning false."""

        return 0

def test():
    pass
    
def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
