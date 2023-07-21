#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Created on Apr 6, 2010

"""

from __future__ import (absolute_import, division, print_function)

import matplotlib.axes
import matplotlib.projections
import matplotlib.transforms
import math
import numpy
import warnings

class Axes(matplotlib.axes.Axes):

    name = 'mpcsaxes'

    def __init__(self, fig, rect,
                 axisbg = None, # defaults to rc axes.facecolor
                 frameon = True,
                 sharex=None, # use Axes instance's xaxis info
                 sharey=None, # use Axes instance's yaxis info
                 label='',
                 xscale=None,
                 yscale=None,
                 **kwargs):

        matplotlib.axes.Axes.__init__(self,fig,rect,
                             axisbg,
                             frameon,
                             sharex,
                             sharey,
                             label,
                             xscale,
                             yscale,
                             **kwargs)

    def drag_pan(self, button, key, x, y):
        """
        Called when the mouse moves during a pan operation.

        *button* is the mouse button number:

        * 1: LEFT
        * 2: MIDDLE
        * 3: RIGHT

        *key* is a "shift" key

        *x*, *y* are the mouse coordinates in display coords.

        .. note::
            Intended to be overridden by new projection types.
        """
        def format_deltas(key, dx, dy):
            if key=='control':
                if(abs(dx)>abs(dy)):
                    dy = dx
                else:
                    dx = dy
            elif key=='x':
                dy = 0
            elif key=='y':
                dx = 0
            elif key=='shift':
                if 2*abs(dx) < abs(dy):
                    dx=0
                elif 2*abs(dy) < abs(dx):
                    dy=0
                elif(abs(dx)>abs(dy)):
                    dy=dy/abs(dy)*abs(dx)
                else:
                    dx=dx/abs(dx)*abs(dy)
            return (dx,dy)

        p = self._pan_start
        dx = x - p.x
        dy = y - p.y
        if dx == 0 and dy == 0:
            return

        if button == 1:

            dx, dy = format_deltas(key, dx, dy)

            #Ignore Y-axis movements on parasite axes
            if self.chart_config.is_parasite:
                dy = 0

            result = p.bbox.translated(-dx, -dy).transformed(p.trans_inverse)

        elif button == 3:

            try:
                dx = -dx / float(self.bbox.width)
                dy = -dy / float(self.bbox.height)

                #Ignore Y-axis movements on parasite axes
                if self.chart_config.is_parasite:
                    dy = 0

                dx, dy = format_deltas(key, dx, dy)
                if self.get_aspect() != 'auto':
                    dx = 0.5 * (dx + dy)
                    dy = dx

                alpha = numpy.power(10.0, (dx, dy))
                start = numpy.array([p.x, p.y])
                oldpoints = p.lim.transformed(p.trans)
                newpoints = start + alpha * (oldpoints - start)
                result = matplotlib.transforms.Bbox(newpoints).transformed(p.trans_inverse)

            except OverflowError:
                warnings.warn('Overflow while panning')
                return

        self.set_xlim(*result.intervalx)
        self.set_ylim(*result.intervaly)

class EhaAxes(Axes):

    name = 'mpcs_eha_axes'

    def __init__(self, fig, rect,
                 axisbg = None, # defaults to rc axes.facecolor
                 frameon = True,
                 sharex=None, # use Axes instance's xaxis info
                 sharey=None, # use Axes instance's yaxis info
                 label='',
                 xscale=None,
                 yscale=None,
                 **kwargs):

        Axes.__init__(self,fig,rect,
                             axisbg,
                             frameon,
                             sharex,
                             sharey,
                             label,
                             xscale,
                             yscale,
                             **kwargs)

class EvrAxes(Axes):

    name = 'mpcs_evr_axes'

    def __init__(self, fig, rect,
                 axisbg = None, # defaults to rc axes.facecolor
                 frameon = True,
                 sharex=None, # use Axes instance's xaxis info
                 sharey=None, # use Axes instance's yaxis info
                 label='',
                 xscale=None,
                 yscale=None,
                 **kwargs):

        Axes.__init__(self,fig,rect,
                             axisbg,
                             frameon,
                             sharex,
                             sharey,
                             label,
                             xscale,
                             yscale,
                             **kwargs)

def find_closest_point_for_mouse_event(event):

    #If you look at matplotlib.backend_bases.LocationEvent, you can see that matplotlib recognizes that a
    #MouseEvent can occur in multiple axes at the same time, but then it cuts down the list of axes and
    #just passes back the Axes instance with the highest Z-order as event.inaxes. Bullshit. What this
    #means is that we have to manually go back and find all the possible axes and then figure out
    #mouse (x,y) coordinates in each of their coordinate systems. (bnash)

    figure = event.inaxes.chart_config.parent_config.figure
    #Get a list of all the Traces (a.k.a. lines) on the chart and put them in the list "lines"
    all_axes = figure.get_axes() #Grab all the known Axes
    lines = figure.get_all_traces()

    #Figure out all the possible mouse locations.  The event.xdata and event.ydata passed in
    #by the event are in the coordinates of one of the axes (specifically in the coordinates of
    #event.inaxes), but since we have multiple axes potentially, we need to get a mouse position coordinate
    #for each one of the axes.  This section creates a list "events" that contains (x,y,axes) tuples representing
    #the mouse coordinates in each axes instance.
    events = []
    for ax in all_axes:
        new_x = event.xdata
        new_y = event.ydata
        to_axis = event.inaxes
        if not ax == event.inaxes:
            from_axis = event.inaxes
            to_axis = ax
            new_x = translate_point_to_axis(from_axis.xaxis,to_axis.xaxis,event.xdata)
            new_y = translate_point_to_axis(from_axis.yaxis,to_axis.yaxis,event.ydata)
        events.append((new_x,new_y,to_axis))

    #These variables will store all the details about the closest point to the mouse
    best_point_index = None
    best_trace = None
    best_axes = event.inaxes
    best_distance = float('inf')

    #Loop through all the lines in the entire plot
    for i in range(0,len(lines)):

        line = lines[i]
        line_x_data = line.get_xdata()
        line_y_data = line.get_ydata()

        #Each "axis_event" is the (x,y,axes) tuple that represent the MouseEvent
        #coordinates in each of the axes
        for axis_event in events:

            event_x,event_y,event_axis = axis_event

            #If this line is not in the axes instance for this
            #current event, don't bother checking it
            #(this greatly speeds up performance)
            if line.axes is not event_axis:
                continue

            #Here comes some brute force.  For every single (x,y) point on the
            #current trace we use the distance formula to figure out how far that
            #point is from the MouseEvent (x,y) point.  I realize there are probably
            #more efficient ways to do this, but this is the cleanest solution and until
            #it compromises the user experience, I'm just leaving it in.
            for j in range(len(line_x_data)):

                x = float(line_x_data[j])

                if line_y_data[j] != None:
                    y = float(line_y_data[j])

                    #Find the distance from this point to the MouseEvent point
                    distance = math.sqrt((x-event_x)**2 + (y-event_y)**2)
                else:
                    # Have no value, so make sure it's no better
                    distance = float('inf')

                #If the current distance is closer to the MouseEvent point than any
                #previous distance, store all the relevant details
                if distance < best_distance:
                    best_trace = line
                    best_point_index = j
                    best_distance = distance
                    best_axes = line.axes

    if best_trace == None:
        raise ValueError('No data points')

    #These are the best (x,y) coordinates we found
    best_xdata = best_trace.get_xdata()[best_point_index]
    best_ydata = best_trace.get_ydata()[best_point_index]

    #One final complication.  Matplotlib is trying to draw the cursor on top of the axes that
    #the event originally occured in (event.inaxes), so if the point we found closest to the cursor
    #is not on the axes for the event (event.inaxes), then we have to linearly interpolate the current
    #(x,y) point back onto the event.inaxes axes instance.
#        if best_axes != event.inaxes:
#            best_ydata = translate_point_to_axis(best_axes.yaxis,event.inaxes.yaxis,best_ydata)

    return (best_xdata,best_ydata,best_axes,best_trace,best_point_index)


def translate_point_to_axis(from_axis,to_axis,from_point):
    '''Use linear interpolation to translate a point from the coordinates of
    one axis to the coordinates of another axis.

    Args
    -----
    from_axis - The axis the current point rests on
    to_axis - The axis to translate the point to
    from_point - The point to be translated from the from_axis to the to_axis'''

    from_point = float(from_point)

    to_low,to_high = [float(i) for i in to_axis.get_view_interval()]
    from_low,from_high = [float(i) for i in from_axis.get_view_interval()]

    #linearly interpolate Y value for other axis
    to_point = (to_high-to_low) * ((from_point - from_low)/(from_high-from_low)) + to_low

    return to_point

#Add a new projection to the registry.  Now whenever we call
#add_subplot or add_axes on a figure, we provide it with the
#kwarg 'projection="mpcsaxes"' and that will cause the internals
#of matplotlib to make an instance of this class instead of an
#instance of the normal matplotlib.axes.Axes class.  This is how
#we can now override pieces of the matplotlib.axes.Axes class in
#this class and have it all actually work.  Scary. (brn)
matplotlib.projections.projection_registry.register(Axes)
matplotlib.projections.projection_registry.register(EhaAxes)
matplotlib.projections.projection_registry.register(EvrAxes)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
