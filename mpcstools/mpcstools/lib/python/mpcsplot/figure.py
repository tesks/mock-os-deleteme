#! /usr/bin/env python
# -*- coding: utf-8 -*-


"""
Created on Apr 6, 2010

"""

from __future__ import (absolute_import, division, print_function)
import matplotlib.figure

class Figure(matplotlib.figure.Figure):

    def get_all_traces(self):

        lines = []
        for ax in self.get_axes():
            for line in ax.get_lines():
                #If the line doesn't have a trace config attached to it, it's not one of the traces
                #that we drew...it's something else (e.g. the cursor lines show up in this list of lines).
                #That was fun to figure out too.
                if not hasattr(line,'trace_config'):
                    continue
                lines.append(line)

        return lines

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
