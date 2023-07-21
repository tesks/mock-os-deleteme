#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import matplotlib.widgets
import mpcsutil
import mpcsplot
import math

class SnapToCursor(matplotlib.widgets.Cursor):

    def __init__(self,ax, useblit=True, do_snap=True, **lineprops):

        matplotlib.widgets.Cursor.__init__(self, ax, useblit=True, **lineprops)

        #The base axes instance for the cursor
        self.ax = ax

        #Should the guide snap to the nearest point?
        self.do_snap = do_snap

        #Always show both the horizontal and vertical parts of the guide
        self.vertOn = True
        self.horizOn = True

    def onmove(self, event):
        '''Callback that's called anytime the cursor moves across the axes.'''

        #If you look at matplotlib.backend_bases.LocationEvent, you can see that matplotlib recognizes that a
        #MouseEvent can occur in multiple axes at the same time, but then it cuts down the list of axes and
        #just passes back the Axes instance with the highest Z-order as event.inaxes. Bullshit. What this
        #means is that we have to manually go back and find all the possible axes and then figure out
        #mouse (x,y) coordinates in each of their coordinate systems. (bnash)

        if self.do_snap and self.visible and event.xdata is not None and event.ydata is not None:

            best_xdata,best_ydata,best_axes,_,_ = mpcsplot.axes.find_closest_point_for_mouse_event(event)

            #One final complication.  Matplotlib is trying to draw the cursor on top of the axes that
            #the event originally occured in (event.inaxes), so if the point we found closest to the cursor
            #is not on the axes for the event (event.inaxes), then we have to linearly interpolate the current
            #(x,y) point back onto the event.inaxes axes instance.
            if best_axes != event.inaxes:
                best_ydata = mpcsplot.axes.translate_point_to_axis(best_axes.yaxis,event.inaxes.yaxis,best_ydata)

            #Set the point where the cursor should be when this method returns
            event.xdata = best_xdata
            event.ydata = best_ydata

        #Call the superclass "onmove" event to do the actual redrawing of the cursor
        matplotlib.widgets.Cursor.onmove(self, event)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
