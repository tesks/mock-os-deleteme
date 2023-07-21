#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import matplotlib
#Use the TkInter backend for matplot lib
#NOTE: This MUST be done BEFORE you import
#and use any matplotlib backend stuff
matplotlib.use('TkAgg')
import matplotlib.widgets
import matplotlib.transforms

import mpcsplot
import mpcsplot.config
import mpcsplot.canvas
import mpcsplot.toolbar
import mpcsutil.gui

import copy
import six
if six.PY2:
    import Tkinter as tk
    import tkMessageBox
else:
    import tkinter as tk
    from tkinter import messagebox as tkMessageBox

import logging
import threading

_log = lambda : logging.getLogger('mpcs.chill_get_plots')

class GuiPlotManager(mpcsplot.AbstractPlotManager):

    def __init__(self):

        mpcsplot.AbstractPlotManager.__init__(self)

    def run(self):

        result = mpcsplot.AbstractPlotManager.run(self)
        if result != 0:
            return result

        self.show()

        return 0

    def _create_menu(self):

        self._menu_bar = tk.Menu(master=self.root)

        self._file_menu = tk.Menu(self._menu_bar,tearoff=0)
        self._file_menu.add_command(label='Export Image...',command=self.figure.canvas.toolbar.save_figure)
        self._file_menu.add_separator()
        self._file_menu.add_command(label='Exit',command=self._exit)
        self._menu_bar.add_cascade(label='File',menu=self._file_menu)

        self._view_menu = tk.Menu(self._menu_bar,tearoff=0)
        self._view_menu.add_command(label='Set Title...',command=self._set_figure_title)
        self._view_menu.add_separator()

        self.show_gridlines = tk.BooleanVar(master=self._view_menu,value=False)
        self._view_menu.add_checkbutton(label='Show Gridlines',command=self._toggle_gridlines,variable=self.show_gridlines)

        self._view_menu.add_separator()

        self.show_cursor = tk.BooleanVar(master=self._view_menu,value=False)
        self._view_menu.add_checkbutton(label='Show Cursor Guides',command=self._toggle_cursor,variable=self.show_cursor)

        self.do_cursor_snap = tk.BooleanVar(master=self._view_menu,value=False)
        self._view_menu.add_checkbutton(label='Snap Cursor to Points',command=self._toggle_cursor_snap,variable=self.do_cursor_snap)

        self._view_menu.add_separator()

        self.show_legend = tk.BooleanVar(master=self._view_menu,value=True)
        self._view_menu.add_checkbutton(label='Show Legend',command=self._toggle_legend,variable=self.show_legend)
        self._menu_bar.add_cascade(label='Plot',menu=self._view_menu)

        #The configure subplots movements don't work properly with multiple y-axes
        #(at least not the way we have them working right now)
        #self._view_menu.add_separator()
        #self._view_menu.add_command(label='Configure Subplots...',command=self.figure.canvas.toolbar.configure_subplots)

        self._axes_menu = tk.Menu(self._menu_bar,tearoff=0)
        self._configure_axis_menu = tk.Menu(self._axes_menu,tearoff=0)
        self._axes_menu.add_cascade(label='Configure Axis',menu=self._configure_axis_menu)
        #X-axis menu
#        self._xaxis_menu = tk.Menu(self._axes_menu,tearoff=0)
#        self._axes_menu.add_cascade(label='X Axis',menu=self._xaxis_menu)
#        self._xaxis_menu.add_command(label='Set Title...',command=lambda: self._set_axis_title('X'))
#        self.show_x_minorticks = tk.BooleanVar(master=self._xaxis_menu,value=True)
#        self._xaxis_menu.add_checkbutton(label='Show Minor Ticks',command=lambda: self._toggle_minorticks('X'),variable=self.show_x_minorticks)
#        #Y-axis menu
#        self._yaxis_menu = tk.Menu(self._axes_menu,tearoff=0)
#        self._axes_menu.add_cascade(label='Y Axis',menu=self._yaxis_menu)
#        self._yaxis_menu.add_command(label='Set Title...',command=lambda: self._set_axis_title('Y'))
#        self.show_y_minorticks = tk.BooleanVar(master=self._yaxis_menu,value=True)
#        self._yaxis_menu.add_checkbutton(label='Show Minor Ticks',command=lambda: self._toggle_minorticks('Y'),variable=self.show_y_minorticks)
#        ############
        self._menu_bar.add_cascade(label='Axes',menu=self._axes_menu)

        self._eha_menu = tk.Menu(self._menu_bar,tearoff=0)
        self._eha_menu.add_command(label='Add EHA Trace...',command=self._add_eha_trace)
        self._configure_eha_menu = tk.Menu(self._eha_menu,tearoff=0)
        self._eha_menu.add_cascade(label='Configure Trace',menu=self._configure_eha_menu)
        self._remove_eha_list_menu = tk.Menu(self._eha_menu,tearoff=0)
        self._eha_menu.add_cascade(label='Remove Trace',menu=self._remove_eha_list_menu)
        self._menu_bar.add_cascade(label='EHA',menu=self._eha_menu)

        self._evr_menu = tk.Menu(self._menu_bar,tearoff=0)
        self.show_evrs = tk.BooleanVar(master=self._evr_menu,value=False)
        #self._evr_menu.add_checkbutton(label='Show EVRs',command=lambda: self._toggle_evrs(),variable=self.show_evrs)
        #self._evr_menu.add_checkbutton(label='Some other thing')
        index = 0
        self._evr_menu.insert_checkbutton(index,label='Show FSW EVRs',command=lambda: self._toggle_evrs(),variable=self.show_evrs)
        self.show_evrs_button_index = index
        index += 1
        self._evr_menu.insert_command(index,label='Configure EVRs...',command=self._configure_evr_trace)
        self.configure_evrs_button_index = index
        self._evr_menu.entryconfigure(self.configure_evrs_button_index,state=tk.DISABLED)
#        self._configure_evr_menu = tk.Menu(self._evr_menu,tearoff=0)
#        self._evr_menu.add_cascade(label='Configure EVRs',menu=self._configure_evr_menu)
#        self._remove_evr_list_menu = tk.Menu(self._evr_menu,tearoff=0)
#        self._evr_menu.add_cascade(label='Remove EVR Level...',menu=self._remove_evr_list_menu)
        self._menu_bar.add_cascade(label='EVR',menu=self._evr_menu)

        self.root.config(menu=self._menu_bar)

    def _exit(self):

        #TODO: Prompt for save here?

        self.root.quit()

    def _set_figure_title(self):

        title = None
        for chart in self.figure.get_axes():

            if title is None:
                tc = mpcsutil.gui.SimpleTextEntryDialog(self.root,'Set Plot Title','Plot Title: ',chart.get_title())
                if not tc.result:
                    return
                title = tc.result

            chart.set_title(title)
            chart.chart_config.plot_title = title

        self.update_plot()

    #TODO: Isn't there a better way to see if it's an X or Y axis than passing a string?
    def _set_axis_title(self,axis_name):
        '''axis_name - Should always be 'X' or 'Y'.'''

        #TODO: Only works for single charts right now
        #TODO: Need to fix this to allow you to choose which Y axis you want now

        title = None
        for chart in self.figure.get_axes():

            if title is None:
                tc = mpcsutil.gui.SimpleTextEntryDialog(self.root,'Set %s Axis Title' % (axis_name),'%s Axis Title: ' % (axis_name),
                                           chart.xaxis.get_label_text() if axis_name == 'X' else chart.yaxis.get_label_text())
                if not tc.result:
                    return
                title = tc.result

            if axis_name == 'X':
                chart.xaxis.set_label_text(title)
                chart.chart_config.x_axis_title = title
            else:
                chart.yaxis.set_label_text(title)
                chart.chart_config.y_axis_title = title

        self.update_plot()

    def _remove_trace(self,trace_config,remove_stored_values=True):

        chart_config = trace_config.parent_config
        chart = chart_config.chart

        #Remove the line from the plot
        chart_config.chart.lines.remove(trace_config.trace)

        #Update the legend (dereference the old one & create a new one)
        if chart_config.legend_location is not None and chart_config.is_host and chart.get_legend() is not None:
            chart.legend(loc=chart_config.legend_location.get_prop_val())

        #Remove from menu
        self._remove_eha_list_menu.delete(self._remove_eha_list_menu.index(trace_config.label))
        self._configure_eha_menu.delete(self._configure_eha_menu.index(trace_config.label))

        #Remove from queryValues (Have not decided if we should do this or not...what are the odds they re-add it later?)
        #
        #This could be useful for if we're doing things like changing ordering from ERT to SCLK and want to keep all the queried
        #data, but remove and re-add existing traces
        if remove_stored_values:
            del self.query_values[trace_config.get_id()]

        #Delete corresponding trace_config
        chart_config.trace_configs.remove(trace_config)
        chart_config.traces.remove(trace_config.trace)

        #Rescale the plot
        chart.relim() #tell matplotlib to recalc axis limits
        chart.autoscale_view(scalex=chart_config.autoscale_x,scaley=chart_config.autoscale_y) #Re-autoscale the new view

        self.update_plot()

    def _do_add_trace_query(self,*args,**kwargs):

        global _log

        #TODO: passing in x_axis_type.val_str will be a problem once we potentially have EHA values on the X-axis
        dialog = args[0]
        try:
            self.new_query_values = self.query_function(self.default_chart_config.x_axis_type.val_str)
            if not self.new_query_values:
                self.query_error = 'No values found in query for the given information.'
                _log.error(self.query_error)
        except mpcsplot.err.DataRetrievalError as dre:
            self.query_error = 'Could not execute data query: {}'.format(dre)
            _log.error(self.query_error)
            self.new_query_values = {}
        finally:
            # MPCS-7764  - 1/16/2016:
            # Avoid race condition where dialog.active flag has not yet been set to true by the dialog's
            # start function
            while not dialog.active:
                pass
            dialog.stop()

    def _add_eha_trace(self):

        if len(self.query_values) >= self._maxTraces:
            tkMessageBox.showerror(title='Trace Limit Reached',
                                   message='You have reached the maximum allowable amount of traces (%d) on a single plot. If you wish to add a new trace, please remove a trace first.' % (self._maxTraces))
            return

        tc = mpcsutil.gui.ChannelChooserDialog(self.root,'Trace Chooser')
        if not tc.result:
            return

        (channel_id,isDn) = tc.result
        if channel_id in self.query_values:
            tkMessageBox.showerror(title='Channel Already Exists',
                                   message='Channel ID "%s" is already on the plot.' % (channel_id))
            return

        self.query_helper.channel_ids = channel_id #channel_ids is a CSV field
        self.query_helper.channel_id_file = None

        #####################
        #(BRN): In order to make our "InfiniteBarDialog" animate properly, we need to hand it the event loop with the
        #call to self.root.wait_window(dialog), but "wait_window" also freezes the current thread until the dialog is
        #dismissed, thus we need a third thread that will actually go off and do the query while the dialog is animating. The
        #new thread has no way to directly pass data back, so it creates a temporary class variable called "self.new_query_values"
        #to hand back the results of the query and it then kills the dialog so when this main thread picks back up, it has
        #the query results and can proceed to put them on the plot.
        #####################
        # MPCS-7764  - 1/15/2015: InifiniteBarDialog blocks on start() now, so defer calling that method
        # until the _ioThread is started.
        # Tkinter calls need to be done from a single thread, or it and its underlying libraries need to be compiled / configured
        # with thread support.  For some reason, this is not working in our environment, InfiniteBarDialog now blocks, and only two threads
        # are present- the current (main tkinter thread), and the IO loop thread.
        dialog = mpcsutil.gui.progressbar.InfiniteBarDialog(parent=self.root,label='Querying values for channel "%s"...' % (channel_id))
        self.new_query_values = None
        self.query_error = None
        self.query_function = self.query_helper.execute_eha_query
        _ioThread = threading.Thread(target=self._do_add_trace_query,name='EHA Query Thread',args=(dialog,))
        _ioThread.start()
        dialog.start()
        #####################

        if self.query_error:
            tkMessageBox.showerror(title='Query failed for Channel ID %s' % (channel_id),
                                   message=self.query_error)
            del self.new_query_values
            del self.query_error
            del self.query_function
            return

        #TODO: How do we know which chart to add the trace to?  Assume there's only one for now...
        chart_config = self.fig_config.chart_configs[0]
        chart = self.fig_config.charts[0]

        #channel_count = len(self.query_values)
        for channel_id in self.new_query_values:

            if channel_id not in self.query_values.keys():

                self.query_values[channel_id] = self.new_query_values[channel_id]
                self.query_values.ordered_channel_ids.append(channel_id)

                trace_config = mpcsplot.config.EhaTraceConfig()
                if len(chart.chart_config.trace_configs) > 0:
                    trace_config.copy_shared_props(chart.chart_config.trace_configs[0])
                else:
                    #trace_config = copy.deepcopy(self.default_trace_config,{})
                    trace_config.copy_shared_props(self.default_trace_config)
                trace_config.parent_config = chart_config

                #TODO: Should maybe have a method on trace config to do this stuff?
                trace_config.channel_id = channel_id
                trace_config.plotByEu = not isDn
                trace_config.channel_name = None
                trace_config.chanvals = self.query_values[trace_config.channel_id]
                trace_config.set_label_from_values()
                trace_config.color = matplotlib.colors.cnames.keys()[self.query_values.get_index_for_id(trace_config.channel_id)]
                #trace_config.color=matplotlib.colors.cnames.keys()[channel_count]
                #channel_count += 1

                chart_config.trace_configs.append(trace_config)

                self._plot_trace(chart,trace_config)

        #Update the legend (dereference the old one & create a new one)
        #chart.legend_ = None
        #if  chart.get_legend() is not None:
        #        chart.get_legend().set_visible(self.show_legend.get())
        #chart.legend(loc=chart_config.legend_location.get_prop_val())
        #TODO: Currently this makes the parasites not appear on the set of labels...
        if chart_config.legend_location is not None and chart_config.is_host and chart.get_legend() is not None:
            chart.legend(loc=chart_config.legend_location.get_prop_val())

        #Rescale the plot
        #TODO:
        #TODO: Temporary commenting out to see what this does to changing X-axis type...
        #TODO:
        chart.autoscale_view(scalex=chart_config.autoscale_x,scaley=chart_config.autoscale_y) #Re-autoscale the new view
        chart.relim() #tell matplotlib to recalc axis limits

        self.update_plot()

        del self.new_query_values
        del self.query_error
        del self.query_function

    def _toggle_evrs(self):

        if self.show_evrs.get():

            self.new_query_values = {}
            self.query_error = None
            self.query_function = self.query_helper.execute_evr_query

            if self._add_evr_trace():
                #OK Tkinter, let me get this straight, you have a status called "tk.DISABLED" for disabling
                #things, but then to enable things, I have to use "tk.NORMAL"?  Wonderful.  (bnash)
                self._evr_menu.entryconfigure(self.configure_evrs_button_index,state=tk.NORMAL)
            else:
                self.show_evrs.set(False)

            del self.new_query_values
            del self.query_error
            del self.query_function

        else:

            if self._remove_evr_trace():
                self._evr_menu.entryconfigure(self.configure_evrs_button_index,state=tk.DISABLED)
            else:
                self.show_evrs.set(True)

    def _configure_evr_trace(self):

        evr_chart_config = None
        for chart_config in self.fig_config.chart_configs:
            if isinstance(chart_config,mpcsplot.config.EvrChartConfig):
                evr_chart_config = chart_config
                break

        ecd = mpcsutil.gui.EvrConfigDialog(self.root,evr_chart_config)
        if not ecd.result:
            return

        #Update the legend (dereference the old one & create a new one)
        if chart_config.legend_location is not None and chart_config.is_host and chart_config.chart.get_legend() is not None:
            chart_config.chart.legend(loc=chart_config.legend_location.get_prop_val())

        self.update_plot()

    def _remove_evr_trace(self):

        config_to_remove = None
        chart_to_remove = None
        index_to_remove = None

        for i in range(0,len(self.fig_config.charts)):
            chart = self.fig_config.charts[i]
            if isinstance(chart,mpcsplot.axes.EvrAxes):
                index_to_remove = i
                chart_to_remove = chart
                config_to_remove = chart.chart_config
                break

        chart_to_remove.cursor.visible = False
        self.figure.delaxes(chart_to_remove)
        del self.fig_config.chart_configs[index_to_remove]
        del self.fig_config.charts[index_to_remove]

        self._remove_axis(config_to_remove)

        self.update_plot()

        return True

    def _add_evr_trace(self):

        #TODO: Check if EVRs already show up on the plot? (We can allow this to happen if they need to re-add
        #levels that aren't already on the plot

        if len(self.query_values) >= self._maxTraces:
            tkMessageBox.showerror(title='Trace Limit Reached',
                                   message='You have reached the maximum allowable amount of traces (%d) on a single plot. If you wish to add a new trace, please remove a trace first.' % (self._maxTraces))
            return False

        #Fetch all levels by default
        level_map = {}
        for i in range(0,len(mpcsutil.evr.levels)):
            level_map[mpcsutil.evr.levels[i]] = True

        #Have not grabbed EVRs yet...need to fetch them from the database
        if not self.query_helper.fetched_evrs:
            #TODO: Level chooser needs to know what levels are already on the plot and default those to "checked"
            #TODO: Should there be a configuration setting for what levels are checked by default?
            #tc = mpcsutil.gui.EvrChooserDialog(self.root,'EVR Level Chooser')
            #if not tc.result:
            #    return

            #level_map = tc.result

            #####################
            #(BRN): In order to make our "InfiniteBarDialog" animate properly, we need to hand it the event loop with the
            #call to self.root.wait_window(dialog), but "wait_window" also freezes the current thread until the dialog is
            #dismissed, thus we need a third thread that will actually go off and do the query while the dialog is animating. The
            #new thread has no way to directly pass data back, so it creates a temporary class variable called "self.new_query_values"
            #to hand back the results of the query and it then kills the dialog so when this main thread picks back up, it has
            #the query results and can proceed to put them on the plot.
            #####################
            #TODO: This is still almost identical to the EHA code...we should be able to factor this out into a function
            # MPCS-7764  - 1/15/2015: InifiniteBarDialog blocks on start() now, so defer calling that method
            # until the _ioThread is started.
            # Tkinter calls need to be done from a single thread, or it and its underlying libraries need to be compiled / configured
            # with thread support.  For some reason, this is not working in our environment, InfiniteBarDialog now blocks, and only two threads
            # are present- the current (main tkinter thread), and the IO loop thread.
            dialog = mpcsutil.gui.progressbar.InfiniteBarDialog(parent=self.root,label='Querying for EVRs...')
            _ioThread = threading.Thread(target=self._do_add_trace_query,name='EVR Query Thread',args=(dialog,))
            _ioThread.start()
            dialog.start()
            #####################

            if self.query_error:
                tkMessageBox.showerror(title='Query failed for EVRs',
                                       message=self.query_error)
                return False

        #TODO:  tr = GuiPlotManager.TraceUpdater(pm=self,trace_config=trace_config)
        #self._remove_eha_list_menu.add_command(label=trace_config.label,command=tr.remove)

        #TODO: How do we know which chart to add the trace to?  Assume there's only one for now...
        chart_config = self.fig_config.chart_configs[0]
        chart = self.fig_config.charts[0]

        #Make the new chart config for the EVR
        evr_chart_config = mpcsplot.config.EvrChartConfig()
        evr_chart_config.parent_config = self.fig_config
        evr_chart_config.copy_shared_props(chart_config)
        evr_chart_config.y_axis_major_formatter = mpcsplot.formatter.EvrFormatter()
        evr_chart_config.y_axis_type = mpcsplot.axistypes.AxisTypes(numval=mpcsplot.axistypes.AxisTypes.EVR_TYPE)
        evr_chart_config.enable_y_minor_ticks = False
        evr_chart_config.y_axis_title = mpcsplot.axistypes.AxisTypes(numval=mpcsplot.axistypes.AxisTypes.EVR_TYPE).get_default_title()
        evr_chart_config.is_host = False
        evr_chart_config.is_parasite = True
        evr_chart_config.enable_y_minor_ticks = False

        #It would be great to use "twinx" if it actually worked, but it breaks all the panning/zooming functionality beyond repair
        #evr_chart = chart.twinx()

        evr_chart = self.figure.add_axes(chart.get_position(True),frameon=False,label='EVR',projection='mpcs_evr_axes')
        evr_chart.yaxis.tick_right()
        evr_chart.yaxis.set_label_position('right')
        evr_chart.yaxis.set_label_text(evr_chart_config.y_axis_title)

        #tick_locations = [mpcsutil.evr.Evr.get_value_from_level(level) for level in mpcsutil.evr.levels if level and level_map[level] != 0]
        #evr_chart.yaxis.set_major_locator(matplotlib.ticker.FixedLocator(locs=tick_locations))

        evr_chart.xaxis.set_visible(False)
        evr_chart_config.chart = evr_chart
        evr_chart.chart_config = evr_chart_config

        self.fig_config.charts.append(evr_chart)
        self.fig_config.chart_configs.append(evr_chart_config)

        #print 'Keys = %s' % (self.new_query_values.keys())
        for level in mpcsutil.evr.levels:
        #for level in self.new_query_values:

            #Ignore UNKNOWN or Error EVRs
            if not level or level_map[level] == 0:
                continue

            trace_config = mpcsplot.config.EvrTraceConfig()
            trace_config.evr_id = 'EVR_%s' % (level)

            if not trace_config.evr_id in self.query_values and level in self.new_query_values:
                self.query_values[trace_config.evr_id] = self.new_query_values[level]
            if trace_config.evr_id in self.query_values:
                trace_config.evrs = self.query_values[trace_config.evr_id]

            #TODO: Should maybe have a method on trace config to do this stuff?
            #TODO: Should probably make some of these values configurable (e.g. marker size)
            #trace_config = copy.deepcopy(self.default_trace_config,{})
            trace_config.copy_shared_props(self.default_trace_config)
            trace_config.parent_config = evr_chart_config
            trace_config.lines_on = False
            trace_config.markers_on = True
            trace_config.marker_size = 10
            trace_config.marker_style = mpcsplot.markerstyles.CIRCLE
            trace_config.set_label_from_values()

            #Colors are now stored in the GDS config as R,G,B instead of the name of a color
            # MPCS-9665  - 5/3/18 - R8 allows a default for background color. Also, the
            # default being supplied here "#000000" was not parseable as an RGB string
            rgb_csv = self.gds_config.getProperty('evr.backgroundColor.%s' % (level),'Undefined')
            if rgb_csv == 'Undefined':
                rgb_csv = self.gds_config.getProperty('evr.backgroundColor.default','0,0,255')
            (red,green,blue) = map(int,rgb_csv.split(","))
            color_hex = '#%02x%02x%02x' % (red,green,blue)
            trace_config.color = color_hex

            evr_chart_config.trace_configs.append(trace_config)

            #Tabbed this left one spot...
            self._plot_trace(evr_chart,trace_config)

        if chart_config.legend_location is not None and chart_config.is_host and chart.get_legend() is not None:
            chart.legend(loc=chart_config.legend_location.get_prop_val())

        #Make it so the highest and lowest EVR levels don't get drawn right on top of the axis
        evr_chart.relim() #tell matplotlib to recalc axis limits
        interval = evr_chart.dataLim.intervaly
        #Should dynamically calculate the value of "10" from the difference between two different EVR
        #levels maybe...otherwise what if we change the EVR values by a factor of 10?
        evr_chart.yaxis.set_view_interval(interval[0]-10,interval[1]+10)
        evr_chart.autoscale_view(scalex=chart_config.autoscale_x,scaley=False) #Re-autoscale the new view

        self._add_axis(evr_chart.chart_config)

        chart.relim()
        #Don't think we need to autoscale the other chart when we add an EVR overlay
        #chart.autoscale_view(scalex=chart_config.autoscale_x,scaley=chart_config.autoscale_y) #Re-autoscale the new view

        #Need to make a little extra room for the Y-axis values on the right side of the plot
        #Update: This messes up the positioning of the tick labels on the 2nd y-axis
        #self.figure.subplots_adjust(right=0.90)

        #evr_chart.annotate('SOME TEXT', xy=(0,0),  xycoords='axes fraction',
                #horizontalalignment='left', verticalalignment='bottom')

        #evr_chart.annotate('PIXELS', xy=(0,0),  xycoords='figure pixels')

        evr_chart.cursor = mpcsplot.cursor.SnapToCursor(evr_chart, useblit=True, color='black', linewidth=1, linestyle='--',do_snap=False)
        evr_chart.cursor.visible = self.show_cursor.get()

        #Since the EHA chart object and the EVR chart object are not actually sharing an XAxis object (each has its own,
        #the EVR chart's XAxis is just invisible), their view intervals may not quite be synchronized and this can cause
        #EVR markers to appear slightly out of position against the actual XAxis.  In order to rectify this, we generate a
        #view interval that we're sure contains all the data and then set it on both of the charts' X axes
        minview = min(chart.get_xaxis().get_view_interval()[0],evr_chart.get_xaxis().get_view_interval()[0])
        maxview = max(chart.get_xaxis().get_view_interval()[1],evr_chart.get_xaxis().get_view_interval()[1])
        chart.get_xaxis().set_view_interval(minview,maxview)
        evr_chart.get_xaxis().set_view_interval(minview,maxview)

        self.update_plot()

        return True

    def _configure_trace(self,trace_config):

        old_plotByEu = trace_config.plotByEu
        tcd = mpcsutil.gui.EhaConfigDialog(self.root,trace_config)
        if not tcd.result:
            return

        chart_config = trace_config.parent_config
        chart = chart_config.chart

        #The user is switching between plotting by DN/EU, so we have to redraw
        #the trace for this particular channel
        if old_plotByEu != trace_config.plotByEu:

            #Remove old trace
            self._remove_trace(trace_config,remove_stored_values=False)

            #Create new trace
            chart_config.trace_configs.append(trace_config)
            self._plot_trace(chart,trace_config)

            #Update the legend (dereference the old one & create a new one)
            if chart_config.legend_location is not None and chart_config.is_host and chart.get_legend() is not None:
                chart.legend(loc=chart_config.legend_location.get_prop_val())

            #Rescale the plot
            chart.relim() #tell matplotlib to recalc axis limits
            chart.autoscale_view(scalex=chart_config.autoscale_x,scaley=chart_config.autoscale_y) #Re-autoscale the new view

            self.update_plot()

        #Apply the new configuration to the trace
        trace_config.apply_to_trace()

        #Update the legend (dereference the old one & create a new one)
        if chart_config.legend_location is not None and chart_config.is_host and chart.get_legend() is not None:
            chart.legend(loc=chart_config.legend_location.get_prop_val())

        self.update_plot()

    def _toggle_minorticks(self,axis_name):

        for chart in self.figure.get_axes():

            if axis_name == 'X':
                mpcsplot.set_axis_minor_ticks(chart.xaxis,self.show_x_minorticks.get())
                chart.chart_config.enable_x_minor_ticks = self.show_x_minorticks.get()
            else:
                mpcsplot.set_axis_minor_ticks(chart.yaxis,self.show_y_minorticks.get())
                chart.chart_config.enable_y_minor_ticks = self.show_y_minorticks.get()

        self.update_plot()

    def _toggle_cursor(self):

        #NOTE: self.show_cursor has already been toggled before this method was called
        for chart in self.figure.get_axes():

            #if not chart.chart_config.is_host:
            #    continue

            #Simply toggle visible state of cursor to create/eliminate it for the user
            chart.cursor.visible = not chart.cursor.visible

            #Turn off the cursor-snap-to menu item too if cursor is getting turned off
            if not chart.cursor.visible:
                self.do_cursor_snap.set(False)

        self.update_plot()

    def _toggle_cursor_snap(self):

        #NOTE: self.do_cursor_snap has already been toggled before this method was called
        for chart in self.figure.get_axes():

            do_snap = self.do_cursor_snap.get()

            #First we need to turn the cursor on if it's not already
            chart.cursor.visible = do_snap
            self.show_cursor.set(do_snap)

            #Then, tell the cursor to start doing snap-to
            chart.cursor.do_snap = do_snap

        self.update_plot()

    def _toggle_gridlines(self):

        for chart in self.figure.get_axes():
            chart.grid(self.show_gridlines.get()) #No args toggles gridline state
            chart.chart_config.enable_grid_lines = self.show_gridlines.get()

        self.update_plot()

    def _toggle_legend(self):

        #NOTE: self.show_legend has already been toggled before this method was called
        for chart in self.figure.get_axes():
            if chart.get_legend() is not None:
                chart.get_legend().set_visible(self.show_legend.get())

        self.update_plot()

    def _create_canvas(self):

        #Be really, really careful about messing with the parameters to the "pack" calls below...
        #it took me a long time to make it so that the toolbar always stays as a fixed width on the
        #left side no matter how the window is resized and same with the text frame on the bottom (BRN)

        self.right_frame = tk.Frame(master=self.root)
        self.right_frame.pack(fill=tk.BOTH,expand=True,side=tk.RIGHT,anchor=tk.SE)

        my_canvas = mpcsplot.canvas.PlotCanvas(self.figure,master=self.right_frame)
        self.figure.canvas.draw()
        my_canvas.get_tk_widget().pack(side=tk.TOP,fill=tk.BOTH,expand=True,anchor=tk.NE)
        my_canvas.get_tk_widget().config(bd=0,relief=tk.SUNKEN,highlightbackground='black',highlightcolor='black')

        self.toolbar_help = tk.StringVar()
        self.toolbar_help.set(" ")
        self.location_text = tk.StringVar()
        self.location_text.set(" ")

        self.toolbar = mpcsplot.toolbar.PlotToolbar(canvas=my_canvas,window=self.root,
                                                  help_text=self.toolbar_help,location_text=self.location_text)
        self.toolbar.config(bd=2,relief=tk.GROOVE,highlightbackground='black',highlightcolor='black')
        self.toolbar.update()
        self.toolbar.pack(before=my_canvas.get_tk_widget(),expand=False,side=tk.LEFT,fill=tk.BOTH,anchor=tk.NW)

        self.text_frame = tk.Frame(master=self.right_frame,relief=tk.GROOVE,bd=2,bg='lightgray',highlightbackground='black',highlightcolor='black')
        self.text_frame.pack(before=my_canvas.get_tk_widget(),expand=False,side=tk.BOTTOM,fill=tk.BOTH,anchor=tk.SE)

        self.toolbar_help_label = tk.Label(self.text_frame,justify=tk.LEFT,textvariable=self.toolbar_help,text='',
                                           height=4,bg='lightgray',pady=5)
        self.toolbar_help_label.pack(side=tk.LEFT)

        self.location_text_label = tk.Label(self.text_frame,justify=tk.LEFT,textvariable=self.location_text,text='',
                                            height=4,bg='lightgray',pady=5)
        self.location_text_label.pack(side=tk.RIGHT)

        self.update_plot()

    def setup_figure(self):

        self.root = tk.Tk()
        self.root.config(bd=0,relief=tk.FLAT)
        self.root.wm_title(mpcsutil.get_script_name())

        self.figure = mpcsplot.figure.Figure(figsize=(16,12),
                                                dpi=65,
                                                facecolor='grey',
                                                edgecolor='grey',
                                                linewidth=1.0,
                                                frameon=True,
                                                subplotpars=matplotlib.figure.SubplotParams(left=0.10,right=0.90,top=.97,
                                                                                            bottom=.13,hspace=0.0,wspace=0.0))

        self.fig_config.figure = self.figure
        self.figure.fig_config = self.fig_config

        mpcsplot.AbstractPlotManager.setup_figure(self)

        self._create_canvas()
        self._create_menu()

    def _plot_trace(self,chart,trace_config):

        mpcsplot.AbstractPlotManager._plot_trace(self, chart, trace_config)

        if isinstance(trace_config,mpcsplot.config.EhaTraceConfig):
            tr = GuiPlotManager.TraceUpdater(pm=self,trace_config=trace_config)
            self._remove_eha_list_menu.add_command(label=trace_config.label,command=tr.remove)
            self._configure_eha_menu.add_command(label=trace_config.label,command=tr.configure)

    def do_plot(self):

        mpcsplot.AbstractPlotManager.do_plot(self)

        #Loop through all the charts in a figure
        for chart in self.fig_config.charts:

            #Make dummy cursors to start...otherwise we get weird drawing behavior if we attempt to make the cursor
            #itself in the _toggle_cursor callback. We immediately make a cursor and then make it invisible
            chart.cursor = mpcsplot.cursor.SnapToCursor(chart, useblit=True, color='black', linewidth=1, linestyle='--',do_snap=False)
            chart.cursor.visible = False

            self._add_axis(chart.chart_config)

    def show(self):

        self.update_plot()
        self.root.focus_set()
        self.root.mainloop()

    def update_plot(self):

        for config in self.fig_config.chart_configs:
            config.apply_to_chart()
        self.figure.canvas.draw()
        self.right_frame.update()

    class TraceUpdater(object):

        def __init__(self,pm,trace_config):

            self.pm = pm
            self.tc = trace_config

        def remove(self):
            self.pm._remove_trace(self.tc)

        def configure(self):

            self.pm._configure_trace(self.tc)

    class AxisUpdater(object):

        def __init__(self,pm,chart_config,axis):

            self.pm = pm
            self.cc = chart_config
            self.axis = axis

        def configure(self):

            self.pm._configure_axis(self.cc,self.axis)

    def _add_axis(self,chart_config):

        chart = chart_config.chart
        x_label = 'Configure X-Axis (%s)...' % (chart.get_xaxis().get_label_text())
        y_label = 'Configure Y-Axis (%s)...' % (chart.get_yaxis().get_label_text())

        if chart.get_xaxis().get_visible():
            au_x = GuiPlotManager.AxisUpdater(pm=self,chart_config=chart_config,axis=chart.get_xaxis())
            self._configure_axis_menu.add_command(label=x_label,command=au_x.configure)

        if chart.get_yaxis().get_visible():
            au_y = GuiPlotManager.AxisUpdater(pm=self,chart_config=chart_config,axis=chart.get_yaxis())
            self._configure_axis_menu.add_command(label=y_label,command=au_y.configure)

    def _remove_axis(self,chart_config):

        chart = chart_config.chart
        x_label = 'Configure X-Axis (%s)...' % (chart.get_xaxis().get_label_text())
        y_label = 'Configure Y-Axis (%s)...' % (chart.get_yaxis().get_label_text())

        if chart.get_xaxis().get_visible():
            self._configure_axis_menu.delete(self._configure_axis_menu.index(x_label))

        if chart.get_yaxis().get_visible():
            self._configure_axis_menu.delete(self._configure_axis_menu.index(y_label))

    def _configure_axis(self,chart_config,axis):

        self._remove_axis(chart_config)
        acd = mpcsutil.gui.AxisConfigDialog(self.root,chart_config,axis)
        self._add_axis(chart_config)

        if not acd.result:
            return

        #The X-Axis is currently shared by all the plots, so we need to change the
        #X-Axis across them all
        if acd.axis_type_changed and acd.is_x_axis:

            for cc in self.fig_config.chart_configs:
                cc.x_axis_type = chart_config.x_axis_type

            self._replot_all_traces()
            self.toolbar.clear_history()

        self.toolbar.push_current()
        self.update_plot()

    def _replot_all_traces(self):

        for chart in self.figure.get_axes():

            if isinstance(chart,mpcsplot.axes.EhaAxes):

                #
                # Remove all existing EHA traces
                #
                old_trace_configs = []
                for dummy in range(0,len(chart.chart_config.traces)):

                    trace = chart.chart_config.traces[0]
                    trace_config = trace.trace_config
                    id = trace_config.get_id()
                    trace_config.chanvals = None

                    #Need to remove and re-add query values from trace_config
                    old_trace_configs.append(trace_config)
                    self._remove_trace(trace_config,remove_stored_values=False)

                chart.clear()
                chart.legend_ = None
                chart.chart_config.copy_shared_props(chart.chart_config)
                chart.chart_config.apply_to_chart()

                #
                # Re-add all known EHA traces with new X-axis
                #
                for trace_config in old_trace_configs:

                    id = trace_config.get_id()

                    if not id in self.query_values:
                        continue

                    #Re-sort the already queried values by the new X-Axis ordering
                    axis_type = chart.chart_config.x_axis_type
                    sort_field = axis_type.get_sort_field()

                    def cmp_func(x,y):
                        x_val = getattr(x,sort_field)
                        y_val = getattr(y,sort_field)
                        return cmp(x_val,y_val)

                    self.query_values[id].sort(cmp=cmp_func)

                    #Re-plot the trace
                    trace_config.chanvals = self.query_values[id]
                    chart.chart_config.trace_configs.append(trace_config)
                    trace_config.set_label_from_values()
                    self._plot_trace(chart,trace_config)

                    if chart.chart_config.legend_location is not None and chart.chart_config.is_host:
                        chart.legend(loc=chart.chart_config.legend_location.get_prop_val())
                        if chart.get_legend() is not None:
                            chart.get_legend().set_visible(self.show_legend.get())

                chart.autoscale_view(scalex=chart.chart_config.autoscale_x,scaley=chart.chart_config.autoscale_y)
                chart.relim()

            elif isinstance(chart,mpcsplot.axes.EvrAxes):

                #Remove all the existing EVRs
                self.show_evrs.set(False)
                self._toggle_evrs()

                #resort all the EVR entries in self.query_values
                #by whatever the new X-axis type is
                for level in range(0,len(mpcsutil.evr.levels)):

                    if not level in self.query_values:
                        continue

                    axis_type = chart.chart_config.x_axis_type
                    sort_field = axis_type.get_sort_field()

                    def cmp_func(x,y):
                        x_val = getattr(x,sort_field)
                        y_val = getattr(y,sort_field)
                        return cmp(x_val,y_val)

                    self.query_values[level].sort(cmp=cmp_func)

                #self.show_evrs(change to true)
                #self._toggle_evrs()
                self.show_evrs.set(True)
                self._toggle_evrs()

            #TODO: This isn't working properly...our cursor is still broken
            chart.cursor = None
            chart.cursor = mpcsplot.cursor.SnapToCursor(chart, useblit=True, color='black', linewidth=1, linestyle='--',do_snap=False)
            chart.cursor.visible = self.show_cursor.get()
            chart.cursor.do_snap = self.do_cursor_snap.get()

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
