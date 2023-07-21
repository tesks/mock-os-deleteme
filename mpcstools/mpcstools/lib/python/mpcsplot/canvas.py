#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import matplotlib
import matplotlib.backends
import matplotlib.backends.backend_tkagg

_ignored_filetypes = ['emf','rgba','raw','svgz']

class PlotCanvas(matplotlib.backends.backend_tkagg.FigureCanvasTkAgg):

    def __init__(self,figure,master=None,resize_callback=None):

        global _ignored_filetypes

        matplotlib.backends.backend_tkagg.FigureCanvasTkAgg.__init__(self,figure,master=master,resize_callback=self._resize())

        for filetype in _ignored_filetypes:
            if filetype in self.filetypes:
                del self.filetypes[filetype]

    def _resize(self,*args):

        pass

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
