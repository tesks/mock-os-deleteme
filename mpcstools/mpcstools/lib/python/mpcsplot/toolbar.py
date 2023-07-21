#! /usr/bin/env python
# -*- coding: utf-8 -*-
# MPCS-11922 - Fixed chill_get_plots

from __future__ import (absolute_import, division, print_function)

import matplotlib
import matplotlib.backends
import matplotlib.backends.backend_tkagg
import matplotlib.backend_bases
import mpcsutil
import mpcsplot.axes
import numpy as np
import os.path
import six
if six.PY2:
    import Tkinter as tk
    import tkFont
else:
    import tkinter as tk
    from tkinter import font as tkFont

ZOOM_FACTOR = 0.1
LEFT_CLICK = 1
RIGHT_CLICK = 3
BACKGROUND = 'lightgray'

#Change the mouse icon used for the zoom operator from the default "tcross" to "plus" instead
defaultcursor = "plus"

ZOOM_MODE_HELP = "Left click: Zoom in (drag for zoom box)\nRight click: Zoom out (drag for zoom box)"
PAN_MODE_HELP = "Left click & drag: Pan in all directions\nRight click & drag: Stretch zoom in all directions"
EMPTY_HELP = " \n \n \n \n "

class PlotToolbar(matplotlib.backends.backend_tkagg.NavigationToolbar2Tk):

    def __init__(self,canvas,window,help_text,location_text):

        matplotlib.backends.backend_tkagg.NavigationToolbar2Tk.__init__(self,canvas,window)

        self.help_text = help_text
        self.help_text.set(EMPTY_HELP)
        self.location_text = location_text
        self.location_text.set(EMPTY_HELP)
        self._active = 'PAN'
        self._idDrag = ''

    #Vertical toolbar
    def _init_toolbar(self):

        self.bg = BACKGROUND

        ymin,ymax = self.canvas.figure.bbox.intervaly
        height, width = ymax-ymin, 50

        tk.Frame.__init__(self, master=self.window,width=width,height=height,borderwidth=2,bg=self.bg,relief=tk.FLAT)

        self.update()  # Make axes menu

        self.container = tk.Frame(master=self,bg=self.bg,bd=0)
        self.container.pack(side=tk.TOP)
        grid_row = 0

        history_separator = tk.Frame(master=self.container,height=5,bd=1,relief=tk.SUNKEN,background='black')
        history_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E+tk.N,padx=5,pady=5)
        grid_row += 1

        self.history_label = tk.Label(master=self.container,text='History',bg=self.bg)
        header_font = tkFont.Font(font=self.history_label['font'])
        header_font['weight'] = 'bold'
        header_font['size'] = 16
        self.history_label['font']=header_font.name
        self.history_label.grid(row=grid_row,column=0,columnspan=2,pady=5)
        grid_row += 1

        self.bHome = self._make_button(master=self.container, text="Home", file="home.ppm", command=self.home)
        self.bHome.grid(row=grid_row,column=0,columnspan=2,pady=5)
        grid_row += 1

        self.bBack = self._make_button(master=self.container, text="Back", file="back.ppm", command=self.back)
        self.bBack.grid(row=grid_row,column=0,pady=5)
        self.bForward = self._make_button(master=self.container, text="Forward", file="forward.ppm", command=self.forward)
        self.bForward.grid(row=grid_row,column=1,pady=5)
        grid_row += 1

        pan_zoom_separator = tk.Frame(master=self.container,height=5, bd=1, relief=tk.SUNKEN, background='black')
        pan_zoom_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.pan_zoom_label = tk.Label(master=self.container,text='Pan/Zoom',bg=self.bg)
        header_font = tkFont.Font(font=self.pan_zoom_label['font'])
        header_font['weight'] = 'bold'
        header_font['size'] = 16
        self.pan_zoom_label['font']=header_font.name
        self.pan_zoom_label.grid(row=grid_row,column=0,columnspan=2,pady=5)
        grid_row += 1

        self.arrow_frame = tk.Frame(master=self.container,bg=self.bg)
        self.bLeft = self._make_button(master=self.arrow_frame,text="Pan Left", file="stock_left.ppm",command=lambda x=-1: self.panx(x))
        self.bLeft.pack(side=tk.LEFT)
        self.bRight = self._make_button(master=self.arrow_frame,text="Pan Right", file="stock_right.ppm",command=lambda x=1: self.panx(x))
        self.bRight.pack(side=tk.RIGHT)
        self.bUp = self._make_button(master=self.arrow_frame,text="Pan Up", file="stock_up.ppm",command=lambda y=1: self.pany(y))
        self.bUp.pack(side=tk.TOP)
        self.bDown = self._make_button(master=self.arrow_frame,text="Pan Down", file="stock_down.ppm",command=lambda y=-1: self.pany(y))
        self.bDown.pack(side=tk.BOTTOM)
        self.arrow_frame.grid(row=grid_row,column=0,columnspan=2,pady=5)
        grid_row += 1

        self.bPan = self._make_button(master=self.container, text="Pan", file="move.ppm", command=self.pan)
        self.bPan.grid(row=grid_row,column=0,pady=5)
        self.bZoom = self._make_button(master=self.container, text="Zoom", file="zoom_to_rect.ppm", command=self.zoom)
        self.bZoom.grid(row=grid_row,column=1,pady=5)
        grid_row += 1

        next_separator = tk.Frame(master=self.container,height=5, bd=1, relief=tk.SUNKEN, background='black')
        next_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.message = tk.StringVar(master=self.container)
        #self._message_label = tk.Label(master=self, textvariable=self.message)
        #self._message_label.pack(side=tk.RIGHT)

    def pan(self,*args):

        matplotlib.backend_bases.NavigationToolbar2.pan(self,*args)

        if self._active == 'PAN':
            self.bZoom.deselect()
            self.help_text.set(PAN_MODE_HELP)
        elif self._active is None:
            self.bPan.deselect()
            self.help_text.set(EMPTY_HELP)

    def zoom(self,*args):

        matplotlib.backend_bases.NavigationToolbar2.zoom(self,*args)

        if self._active == 'ZOOM':
            self.bPan.deselect()
            self.help_text.set(ZOOM_MODE_HELP)
        elif self._active is None:
            self.bZoom.deselect()
            self.help_text.set(EMPTY_HELP)

    def home(self,*args):

        self.bHome.deselect()
        matplotlib.backend_bases.NavigationToolbar2.home(self,*args)

    def back(self,*args):

        self.bBack.deselect()
        matplotlib.backend_bases.NavigationToolbar2.back(self,*args)

    def forward(self,*args):

        self.bForward.deselect()
        matplotlib.backend_bases.NavigationToolbar2.forward(self,*args)

    def _make_button(self, master, text, file, command):
        # MPCS-11922 - Use local images
        file = os.path.join(mpcsutil.config.GdsConfig().getProperty('GdsDirectory') , 'config/images/', file)
        im = tk.PhotoImage(master=master, file=file)

        #It turns out I have to draw all these things as CheckButtons because if I draw them as normal buttons the drawing has a white border
        #on it that I can't get rid of. Yes, this also means that now you must add code to immediately "uncheck" the button every time it's clicked.
        b = tk.Checkbutton(master=master,image=im,padx=5,pady=5,indicatoron=0,bg=self.bg,bd=5,command=command)
        b._ntimage = im

        return b

    def clear_history(self):
        self.set_history_buttons()

    def set_history_buttons(self):
        pass

    def panx(self, direction):

        if direction == -1:
            self.bLeft.deselect()
        elif direction == 1:
            self.bRight.deselect()

        axes = self.canvas.figure.get_axes()
        if axes:
            for a in axes:
                a.xaxis.pan(direction)
            self.canvas.draw_idle()
            self.push_current()

    def pany(self, direction):

        if direction == -1:
            self.bDown.deselect()
        elif direction == 1:
            self.bUp.deselect()

        axes = self.canvas.figure.get_axes()
        if axes:
            for a in axes:
                if a.chart_config.is_parasite:
                    continue
                a.yaxis.pan(direction)
            self.canvas.draw_idle()
            self.push_current()

    def mouse_move(self,event):

        if not event.inaxes or not self._active:
            if self._lastCursor != matplotlib.backend_bases.cursors.POINTER:
                self.set_cursor(matplotlib.backend_bases.cursors.POINTER)
                self._lastCursor = matplotlib.backend_bases.cursors.POINTER
        else:
            if self._active=='ZOOM':
                if self._lastCursor != matplotlib.backend_bases.cursors.SELECT_REGION:
                    self.set_cursor(matplotlib.backend_bases.cursors.SELECT_REGION)
                    self._lastCursor = matplotlib.backend_bases.cursors.SELECT_REGION
                if self._xypress:
                    x, y = event.x, event.y
                    lastx, lasty, a, ind, lim, trans = self._xypress[0]
                    self.draw_rubberband(event, x, y, lastx, lasty)
            elif (self._active=='PAN' and
                  self._lastCursor != matplotlib.backend_bases.cursors.MOVE):
                self.set_cursor(matplotlib.backend_bases.cursors.MOVE)

                self._lastCursor = matplotlib.backend_bases.cursors.MOVE

        if event.inaxes and event.inaxes.get_navigate():

            try: s = event.inaxes.format_coord(event.xdata, event.ydata)
            except ValueError: pass
            except OverflowError: pass
            else:
                if len(self.mode):
                    self.set_message('%s, %s' % (self.mode, s))
                else:
                    self.set_message(s)
        else: self.set_message(self.mode)

        #matplotlib.backend_bases.NavigationToolbar2.mouse_move(self, event)

        if event.inaxes and event.inaxes.get_navigate() and event.inaxes.chart_config.traces:

            #TODO: Just finished adding a method in mpcsplot.axes to find the closest point to the cursor,
            #so now we need to find a way to map that all the way back to the EHA or EVR object that it
            #corresponds to and use self.location_text to actually display the details of the particular object
            try:

                _,_,best_axes,best_trace,best_index = mpcsplot.axes.find_closest_point_for_mouse_event(event)
                x = float(best_trace.get_xdata()[best_index])
                y = float(best_trace.get_ydata()[best_index])

                #Calling "format_data is causing us to use the same Formatter attached to the axis ticks...not sure this is desirable
                x = best_axes.format_xdata(x)
                y = best_axes.format_ydata(y)
                chart = best_trace.axes

                data_item = best_trace.trace_config.get_data()[best_index]
                data_label = best_trace.trace_config.get_id()

                self.location_text.set('%s\n%s = %s\n%s = %s\n' % (data_item.get_plot_label(),chart.get_xlabel(),x,chart.get_ylabel(),y))

            except ValueError:
                pass
            except OverflowError:
                pass
        else:
            self.set_message(self.mode)

    def release_pan(self, event):
        'the release mouse button callback in pan/zoom mode'

        if self._button_pressed is None:
            return
        self.canvas.mpl_disconnect(self._idDrag)
        self._idDrag=self.canvas.mpl_connect('motion_notify_event', self.mouse_move)
        for a, dummy_index in self._xypress:
            a.end_pan()
        if not self._xypress: return
        self._xypress = []
        self._button_pressed=None
        self.push_current()
        self.release(event)
        self.draw()

    def release_zoom(self, event):
        'the release mouse button callback in zoom to rect mode'

        global ZOOM_FACTOR, LEFT_CLICK, RIGHT_CLICK

        if not self._xypress: return

        last_a = []

        for cur_xypress in self._xypress:
            x, y = event.x, event.y
            lastx, lasty, a, dummy_ind, lim, dummy_trans = cur_xypress

            x0, y0, x1, y1 = lim.extents

            # zoom to point (if rect is 5 pixels or less)
            if abs(x-lastx) < 5 or abs(y-lasty) < 5:

                inverse = a.transData.inverted()

                #The call to inverse takes coordinate pixels from the chart
                #(e.g. (667,522.0)) and transforms them into the actual (x,y) values
                #that are on the plot (e.g. (ert,dn) = (1253640619.07,1439.71839249))
                x, y = inverse.transform_point( (x, y) )

                #Get the X bounds and size of the X view on the plot
                Xmin, Xmax = a.xaxis.get_view_interval()
                Xmin, Xmax = matplotlib.transforms.nonsingular(Xmin,Xmax,expander=0.05)
                x_interval  = Xmax-Xmin
                #calculate where the current center is
                center_x = Xmin + (x_interval)/2
                #How far did the user click away from the current center?
                x_diff = x - center_x

                if not a.chart_config.is_parasite:
                    #Get the Y bounds and size of the Y view on the plot
                    Ymin, Ymax = a.yaxis.get_view_interval()
                    Ymin, Ymax = matplotlib.transforms.nonsingular(Ymin,Ymax,expander=0.05)
                    y_interval = Ymax-Ymin
                    #calculate where the current center is
                    center_y = Ymin + (y_interval)/2
                    #How far did the user click away from the current center?
                    y_diff = y - center_y

                #How much should we zoom in? (10% by default)
                zoom_direction = 0
                if self._button_pressed == LEFT_CLICK:
                    zoom_direction = 1
                elif self._button_pressed == RIGHT_CLICK:
                    zoom_direction = -1
                else:
                    continue

                #Center the plot onto where the user clicked and also zoom in
                x_zoom_step = ZOOM_FACTOR * abs(x_interval) * zoom_direction
                a.xaxis.set_view_interval(Xmin+x_diff+x_zoom_step,Xmax+x_diff-x_zoom_step,ignore=True)

                if not a.chart_config.is_parasite:
                    #Center the plot onto where the user clicked and also zoom in
                    y_zoom_step = ZOOM_FACTOR * abs(y_interval) * zoom_direction
                    a.yaxis.set_view_interval(Ymin+y_diff+y_zoom_step,Ymax+y_diff-y_zoom_step,ignore=True)

            # zoom to rect
            else:

                inverse = a.transData.inverted()
                lastx, lasty = inverse.transform_point( (lastx, lasty) )
                x, y = inverse.transform_point( (x, y) )
                Xmin,Xmax=a.get_xlim()
                Ymin,Ymax=a.get_ylim()

                # detect twinx,y axes and avoid double zooming
                twinx, twiny = False, False
                if last_a:
                    for la in last_a:
                        if a.get_shared_x_axes().joined(a,la): twinx=True
                        if a.get_shared_y_axes().joined(a,la): twiny=True
                last_a.append(a)

                if twinx:
                    x0, x1 = Xmin, Xmax
                else:
                    if Xmin < Xmax:
                        if x<lastx:  x0, x1 = x, lastx
                        else: x0, x1 = lastx, x
                        if x0 < Xmin: x0=Xmin
                        if x1 > Xmax: x1=Xmax
                    else:
                        if x>lastx:  x0, x1 = x, lastx
                        else: x0, x1 = lastx, x
                        if x0 > Xmin: x0=Xmin
                        if x1 < Xmax: x1=Xmax

                if not a.chart_config.is_parasite:
                    if twiny:
                        y0, y1 = Ymin, Ymax
                    else:
                        if Ymin < Ymax:
                            if y<lasty:  y0, y1 = y, lasty
                            else: y0, y1 = lasty, y
                            if y0 < Ymin: y0=Ymin
                            if y1 > Ymax: y1=Ymax
                        else:
                            if y>lasty:  y0, y1 = y, lasty
                            else: y0, y1 = lasty, y
                            if y0 > Ymin: y0=Ymin
                            if y1 < Ymax: y1=Ymax

                if self._button_pressed == 1:
                    a.set_xlim((x0, x1))
                    if not a.chart_config.is_parasite:
                        a.set_ylim((y0, y1))
                elif self._button_pressed == 3:
                    if a.get_xscale()=='log':
                        alpha=np.log(Xmax/Xmin)/np.log(x1/x0)
                        rx1=pow(Xmin/x0,alpha)*Xmin
                        rx2=pow(Xmax/x0,alpha)*Xmin
                    else:
                        alpha=(Xmax-Xmin)/(x1-x0)
                        rx1=alpha*(Xmin-x0)+Xmin
                        rx2=alpha*(Xmax-x0)+Xmin
                    a.set_xlim((rx1, rx2))

                    if not a.chart_config.is_parasite:
                        if a.get_yscale()=='log':
                            alpha=np.log(Ymax/Ymin)/np.log(y1/y0)
                            ry1=pow(Ymin/y0,alpha)*Ymin
                            ry2=pow(Ymax/y0,alpha)*Ymin
                        else:
                            alpha=(Ymax-Ymin)/(y1-y0)
                            ry1=alpha*(Ymin-y0)+Ymin
                            ry2=alpha*(Ymax-y0)+Ymin
                        a.set_ylim((ry1, ry2))

        self.draw()
        self._xypress = None
        self._button_pressed = None

        self.push_current()
        self.release(event)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
