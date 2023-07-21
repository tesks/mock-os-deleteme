#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import mpcsutil
import logging
import matplotlib.artist

_log = lambda : logging.getLogger('mpcs.chill_get_plots')

import matplotlib
from mpcsplot import (axistypes, config, cursor, drawstyles, err, fileformats, formatter, legendlocations, linestyles, markerstyles, query, figure, axes)
__all__ = ['axistypes', 'config', 'cursor', 'drawstyles', 'err', 'fileformats', 'formatter', 'legendlocations', 'linestyles', 'markerstyles', 'query', 'figure', 'axes']

import copy
import traceback

DEFAULT_FIGURE_WIDTH = 16
DEFAULT_FIGURE_HEIGHT = 12
DEFAULT_MARKER_SIZE = 5
DEFAULT_LINE_WIDTH = 2
DEFAULT_X_TICK_ROTATION = 15
DEFAULT_Y_TICK_ROTATION = 0

class AbstractPlotManager(object):

    def __init__(self):

        self.gds_config = mpcsutil.config.GdsConfig()
        self._maxTraces = int(self.gds_config.getProperty('automationApp.mtak.plots.maxTraces','20'))

        self.default_chart_config = None
        self.default_trace_config = None

        self.fig_config = None
        self.figure = None

        self.query_values = None
        self.query_ids = [] #the list of channel IDs in the order the user entered them
        self.query_helper = None

        #The command line parser object (and all the command line option values)
        self.parser = None

    def _do_initial_query(self):

        print('Executing initial database query...')
        try:
            #TODO: passing in x_axis_type.val_str will be a problem once we potentially have EHA values on the X-axis
            self.query_values = self.query_helper.execute_eha_query(self.default_chart_config.x_axis_type.val_str)
        except err.DataRetrievalError:
            _log().fatal('Could not execute initial data query:\n{}'.format(traceback.format_exc()))
            return 1

        if not self.query_values:
            _log().error('Your query returned no values. Please double-check your parameters and try again.')
            return 1
        elif len(self.query_values) > self._maxTraces:
            _log().error('Your query returned too many results.  The maximum amount of traces allowed per plot is {}, but your query returned {}.'.format(self._maxTraces,len(self.query_values)))
            return 1

        return 0

    def run(self):

        result = self._do_initial_query()
        if result != 0:
            return result

        self.setup_figure()
        self.do_plot()

        return 0

    def parse_plot_options(self,parser):

        global DEFAULT_FIGURE_WIDTH, DEFAULT_FIGURE_HEIGHT, DEFAULT_MARKER_SIZE, DEFAULT_LINE_WIDTH

        self.parser = parser
        self.query_helper = query.QueryHelper(parser=parser)

        if self.parser.values.output_file is not None:
            self.dpi = None
            self.result_file = self.parser.values.output_file.strip()
            self.output_format = fileformats.FileFormats(strval=str(self.parser.values.output_format).strip())
            if not self.result_file.endswith(self.output_format.val_str):
                self.result_file += '.%s' % (self.output_format.val_str)

        self.fig_config = config.FigureConfig()
        self.fig_config.fig_width = DEFAULT_FIGURE_WIDTH
        self.fig_config.fig_height = DEFAULT_FIGURE_HEIGHT
        self.fig_config.alpha = None
        self.fig_config.edge_color = None
        self.fig_config.face_color = None
        self.fig_config.label = None

        self.default_chart_config = config.EhaChartConfig()
        self.default_chart_config.autoscale_x = True
        self.default_chart_config.autoscale_y = True
        self.default_chart_config.enable_x_minor_ticks = True
        self.default_chart_config.enable_y_minor_ticks = True
        self.default_chart_config.enable_grid_lines = True if self.parser.values.show_gridlines else False
        self.default_chart_config.x_axis_type = axistypes.ERT if self.parser.values.time_type is None else axistypes.AxisTypes(strval=self.parser.values.time_type.strip())
        self.default_chart_config.x_axis_title = self.default_chart_config.x_axis_type.get_default_title() if self.parser.values.x_axis_title is None else self.parser.values.x_axis_title
        self.default_chart_config.x_axis_tick_rotation = DEFAULT_X_TICK_ROTATION
        self.default_chart_config.x_axis_tick_align = 'center'
        (self.default_chart_config.x_axis_major_formatter,self.default_chart_config.x_axis_minor_formatter) = self.default_chart_config.x_axis_type.get_tick_formatters()
        self.default_chart_config.x_axis_major_tick_locator = None
        self.default_chart_config.x_axis_minor_tick_locator = None
        self.default_chart_config.y_axis_type = axistypes.EHA
        self.default_chart_config.y_axis_title = self.default_chart_config.y_axis_type.get_default_title() if self.parser.values.y_axis_title is None else self.parser.values.y_axis_title
        self.default_chart_config.y_axis_tick_rotation = DEFAULT_Y_TICK_ROTATION
        self.default_chart_config.y_axis_tick_align = 'center'
        self.default_chart_config.y_axis_major_formatter = None
        self.default_chart_config.y_axis_minor_formatter = None
        self.default_chart_config.y_axis_major_tick_locator = None
        self.default_chart_config.y_axis_minor_tick_locator = None
        self.default_chart_config.plot_title = '%s vs. %s' % (self.default_chart_config.y_axis_title,self.default_chart_config.x_axis_title) if self.parser.values.plot_title is None else self.parser.values.plot_title
        #self.default_chart_config.legend_location = legendlocations.BEST
        self.default_chart_config.legend_location = legendlocations.LegendLocations(strval=str(self.parser.values.legend_location).strip())

        self.default_trace_config = config.TraceConfig()
        try:
            self.default_trace_config.marker_size = int(parser.values.marker_size)
            if self.default_trace_config.marker_size <= 0:
                raise ValueError
        except ValueError:
            raise err.DataRetrievalError('Marker size must be a positive numeric value.  The entered value "%s" is invalid.' % (parser.values.marker_size))
        self.default_trace_config.line_width = DEFAULT_LINE_WIDTH
        self.default_trace_config.draw_style = drawstyles.DrawStyles(strval=str(self.parser.values.plot_style).strip())
        self.default_trace_config.line_style = linestyles.LineStyles(strval=str(self.parser.values.line_style).strip())
        self.default_trace_config.marker_style = markerstyles.MarkerStyles(strval=str(self.parser.values.marker_style).strip())
        self.default_trace_config.lines_on = not self.parser.values.only_shapes
        self.default_trace_config.markers_on = self.parser.values.show_shapes or not self.default_trace_config.lines_on
        self.default_trace_config.color = None
        self.default_trace_config.label = None

        #TODO: Not sure what to do with this guy yet
        self.default_trace_config.plotByEu = self.parser.values.plot_by_eu

    def setup_figure(self):

        import matplotlib.pyplot

        if self.figure is None:
            #Generate the figure as our own subclass of Figure instead of the normal matplotlib Figure
            self.figure = matplotlib.pyplot.figure(figsize=(self.fig_config.fig_width,self.fig_config.fig_height),
                                                   FigureClass=figure.Figure)

        self.fig_config.chart_configs.append(copy.deepcopy(self.default_chart_config,{}))
        self.fig_config.figure = self.figure
        self.figure.fig_config = self.fig_config

        #TODO: Here's where we'll loop through and create multiple charts within a figure once we're ready to
        for chart_config in self.fig_config.chart_configs:

            #projection='mpcs_eha_axes' is what makes matplotlib load our implementation
            #of an Axes class instead of its own (see mpcsplot.axes module)
            chart = self.figure.add_subplot(111,projection='mpcs_eha_axes')
            chart_config.is_host = True
            chart_config.label = 'EHA'
            #chart.get_yaxis().set_ticks_position('left') #TODO: test code
            chart.get_yaxis().tick_left()
            chart.get_xaxis().tick_bottom()
            chart.chart_config = chart_config
            chart_config.chart = chart
            chart_config.apply_to_chart()

            self.fig_config.charts.append(chart)
            chart_config.parent_config = self.fig_config

            #channel_ids = self.query_values.keys()
            channel_ids = self.query_values.ordered_channel_ids

            for channel_count in range(0,len(channel_ids)):

                trace_config = config.EhaTraceConfig()

                trace_config.plotByEu = self.parser.values.plot_by_eu
                trace_config.copy_shared_props(self.default_trace_config)
                trace_config.channel_id = channel_ids[channel_count]
                trace_config.channel_name = None
                trace_config.chanvals = self.query_values[trace_config.channel_id]
                trace_config.set_label_from_values()
                trace_config.color = list(matplotlib.colors.cnames.keys())[self.query_values.get_index_for_id(trace_config.channel_id)]
                #trace_config.color=matplotlib.colors.cnames.keys()[channel_count]

                chart_config.trace_configs.append(trace_config)
                trace_config.parent_config = chart_config

    def do_plot(self):

        legend_handles = []
        legend_labels = []

        #Loop through all the charts in a figure
        for chart_config in self.fig_config.chart_configs:

            #Loop through all the traces in a chart
            for k in range(0,len(chart_config.trace_configs)):
                self._plot_trace(chart_config.chart,chart_config.trace_configs[k])

            chart_config.apply_to_chart()
            if chart_config.legend_location is not None:
                handles, labels = chart_config.chart.get_legend_handles_labels()
                legend_handles.extend(handles)
                legend_labels.extend(labels)

        #TODO: This won't work once we have multiple host charts
        for chart_config in self.fig_config.chart_configs:
            if chart_config.is_host:
                chart_config.chart.legend(legend_handles,legend_labels,
                                          loc=chart_config.legend_location.get_prop_val())

    def _plot_trace(self,chart,trace_config):

        chart_config = trace_config.parent_config
        (x_values,y_values) = trace_config.get_xy_values(chart_config.x_axis_type,chart_config.y_axis_type)
        trace_config.set_label_from_values()

        #TODO: Temporary experiment...or is it?
        traces = chart.plot(x_values,y_values,label=trace_config.label,antialiased=True)
        #traces = chart.plot(x_values,y_values,label=trace_config.label,antialiased=True,scalex=False,scaley=False)

        trace = traces[-1]
        trace.set_antialiased(True)
        trace.set_visible(trace_config.visible)
        trace.trace_config = trace_config
        trace_config.trace = trace
        trace_config.apply_to_trace()
        chart_config.traces.append(trace)

def set_axis_minor_ticks(axis,enable):

    #this code came straight from the matplotlib.axes.Axes.minorticks_on method...but there's currently
    #no method in the matplotlib APIs to only do this for one axis
    if enable:
        if axis.get_scale() == 'log':
            s = axis._scale
            axis.set_minor_locator(matplotlib.ticker.LogLocator(s.base,s.subs))
        else:
            axis.set_minor_locator(matplotlib.ticker.AutoMinorLocator())
    else:
        axis.set_minor_locator(matplotlib.ticker.NullLocator())

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
