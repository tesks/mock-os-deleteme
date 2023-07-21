#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import matplotlib
#Use the Agg backend for matplot lib (we have to use Agg for environments with no DISPLAY)
#NOTE: This MUST be done BEFORE you import
#and use any matplotlib backend stuff
matplotlib.use('Agg')

import mpcsplot

import logging

_log = logging.getLogger('mpcs.chill_get_plots')

class HeadlessPlotManager(mpcsplot.AbstractPlotManager):

    def __init__(self):

        mpcsplot.AbstractPlotManager.__init__(self)

        self.result_file = None
        self.output_format = None
        self.dpi = None

    def parse_plot_options(self,parser):

        mpcsplot.AbstractPlotManager.parse_plot_options(self, parser)

        if self.parser.values.output_file is not None:
            self.dpi = None
            self.result_file = self.parser.values.output_file.strip()
            self.output_format = mpcsplot.fileformats.FileFormats(strval=str(self.parser.values.output_format).strip())
            if not self.result_file.endswith(self.output_format.val_str):
                self.result_file = '{}.{}'.format(self.result_file, self.output_format.val_str)

    def do_plot(self):

        mpcsplot.AbstractPlotManager.do_plot(self)

        self.figure.savefig(self.result_file,format=self.output_format.val_str)
        print('Wrote results to {}'.format(self.result_file))

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
