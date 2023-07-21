#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import mpcsplot
import mpcsutil
import copy
import matplotlib.colors
import six
if six.PY2:
    import Tkinter as tk
    import tkSimpleDialog
    import tkMessageBox
    import tkFont
else:
    import tkinter as tk
    from tkinter import simpledialog as tkSimpleDialog
    from tkinter import messagebox as tkMessageBox
    from tkinter import font as tkFont

import Pmw
import matplotlib.artist

sorted_colors = sorted([_color for _color in matplotlib.colors.cnames.keys()])

#Per-trace configuration
# matplotlib.lines.Line2D
class TraceConfig(object):

    def __init__(self):

        self.draw_style = None

        self.lines_on = None
        self.line_style = None
        self.line_width = None

        self.markers_on = None
        self.marker_style = None
        self.marker_size = None

        self.color = None
        self.label = None

        self.parent_config = None
        self.trace = None
        self.visible = True

    def copy_shared_props(self,trace_config):

        self.draw_style = mpcsplot.drawstyles.DrawStyles(numval=trace_config.draw_style.val_num) if trace_config.draw_style is not None else None

        self.lines_on = trace_config.lines_on if trace_config.lines_on is not None else None
        self.line_style = mpcsplot.linestyles.LineStyles(numval=trace_config.line_style.val_num) if trace_config.line_style is not None else None
        self.line_width = trace_config.line_width if trace_config.line_width is not None else None

        self.markers_on = trace_config.markers_on if trace_config.markers_on is not None else None
        self.marker_size = trace_config.marker_size if trace_config.marker_size is not None else None
        self.marker_style = mpcsplot.markerstyles.MarkerStyles(numval=trace_config.marker_style.val_num) if trace_config.marker_style is not None else None

    def get_property_dict(self):

        props = {}

        if self.color is not None:
            props['color'] = self.color

        if self.draw_style is not None:
            props['drawstyle'] = self.draw_style.get_prop_val()

        if self.line_style is not None and self.lines_on:
            props['linestyle'] = self.line_style.get_prop_val()

        if self.marker_style is not None and self.markers_on:
            props['marker'] = self.marker_style.get_prop_val()

        if self.label is not None:
            props['label'] = self.label

        return props

    def apply_to_trace(self):

        if self.draw_style is not None:
            self.trace.set_drawstyle(self.draw_style.get_prop_val())
        if self.color is not None:
            self.trace.set_color(self.color)
        if self.marker_size is not None and self.markers_on:
            self.trace.set_markersize(self.marker_size)
        if self.line_width is not None:
            self.trace.set_linewidth(self.line_width)
        if self.line_style is not None and self.lines_on:
            self.trace.set_linestyle(self.line_style.get_prop_val())
        else:
            self.trace.set_linestyle('')
        if self.marker_style is not None and self.markers_on:
            self.trace.set_marker(self.marker_style.get_prop_val())
        else:
            self.trace.set_marker('')
        if self.visible is not None:
            self.trace.set_visible(self.visible)

    def set_label_from_values(self):

        pass

    def get_xy_values(self,x_axis_type,y_axis_type):

        pass

    def get_id(self):

        return ''

    def get_data(self):

        return []

class EhaTraceConfig(TraceConfig):

    def __init__(self):

        TraceConfig.__init__(self)

        self.channel_id = None
        self.channel_name = None
        self.plotByEu = False
        self.chanvals = []

    def set_label_from_values(self):

        if self.channel_id is not None:
            self.label = self.channel_id + (' {EU}' if self.plotByEu else ' {DN}')
            if self.chanvals:
                self.channel_name = self.chanvals[0].name
                self.label += ' (%s)' % self.channel_name

    def get_xy_values(self,x_axis_type,y_axis_type):

        x_values = []
        y_values = []

        list = []
        if self.chanvals:
            list = self.chanvals

        for item in list:

            try:
                #Note that if we're plotting by time, these methods will just ignore the
                #"useEu" parameter
                xvalue = x_axis_type.format_item_for_axis(item,useEu=self.plotByEu)
                yvalue = y_axis_type.format_item_for_axis(item,useEu=self.plotByEu)
            except OverflowError:
                #Found a bad SCET/ERT...skip it
                continue

            x_values.append(xvalue)
            y_values.append(yvalue)

        return (x_values,y_values)

    def get_id(self):

        return self.channel_id

    def get_data(self):

        return self.chanvals

class EvrTraceConfig(TraceConfig):

    def __init__(self):

        TraceConfig.__init__(self)

        #EVR-related parameters
        self.evr_id = None
        self.evrs = []

    def set_label_from_values(self):

        if self.evr_id is not None:
            self.label = self.evr_id

    def get_xy_values(self,x_axis_type,y_axis_type):

        x_values = []
        y_values = []

        list = []
        if self.evrs:
            list = self.evrs

        for item in list:

            try:
                #Note that if we're plotting by time, these methods will just ignore the
                #"useEu" parameter
                #TODO: Need to get rid of the "useEu" flag
                xvalue = x_axis_type.format_item_for_axis(item,useEu=False)
                yvalue = y_axis_type.format_item_for_axis(item,useEu=False)
            except OverflowError:
                #Found a bad SCET/ERT...skip it
                continue

            x_values.append(xvalue)
            y_values.append(yvalue)

        return (x_values,y_values)

    def get_id(self):

        return self.evr_id

    def get_data(self):

        return self.evrs

#Per-chart configuration
# - contains 1 to N traces
# matplotlib.axes.AxesSubplot
class ChartConfig(object):

    def __init__(self):

        self.plot_title = None
        self.label = None
        self.enable_grid_lines = None
        self.legend_location = None

        self.autoscale_x = None
        self.enable_x_minor_ticks = None
        self.x_axis_type = None
        self.x_axis_title = None
        self.x_axis_tick_rotation = None
        self.x_axis_tick_align = None
        self.x_axis_major_formatter = None
        self.x_axis_minor_formatter = None
        self.x_axis_major_tick_locator = None
        self.x_axis_minor_tick_locator = None

        self.autoscale_y = None
        self.enable_y_minor_ticks = None
        self.y_axis_type = None
        self.y_axis_title = None
        self.y_axis_tick_rotation = None
        self.y_axis_tick_align = None
        self.y_axis_major_formatter = None
        self.y_axis_minor_formatter = None
        self.y_axis_major_tick_locator = None
        self.y_axis_minor_tick_locator = None

        self.host = None
        self.parasites = []
        self.is_parasite = False
        self.is_host = False

        self.trace_configs = []
        self.traces = []

        self.parent_config = None
        self.chart = None

    def copy_shared_props(self,chart_config):

        #if self.is_host and chart_config.is_host:
        self.plot_title = chart_config.plot_title
        self.enable_grid_lines = chart_config.enable_grid_lines
        self.autoscale_x = chart_config.autoscale_x
        self.enable_x_minor_ticks = chart_config.enable_x_minor_ticks
        self.x_axis_title = chart_config.x_axis_title
        self.x_axis_tick_rotation = chart_config.x_axis_tick_rotation
        self.x_axis_tick_align = chart_config.x_axis_tick_align
        self.x_axis_type = chart_config.x_axis_type
        #TODO: Fix tick formatter settings?
        self.x_axis_major_formatter, self.x_axis_minor_formatter = self.x_axis_type.get_tick_formatters()
        self.legend_location = chart_config.legend_location
        #TODO: Do the tick locators ever get set?  I guess not...

        self.x_axis_type = mpcsplot.axistypes.AxisTypes(numval=chart_config.x_axis_type.val_num) if chart_config.x_axis_type is not None else None

        self.autoscale_x = chart_config.autoscale_x
        self.enable_y_minor_ticks = chart_config.enable_y_minor_ticks
        self.y_axis_type = mpcsplot.axistypes.AxisTypes(numval=chart_config.y_axis_type.val_num) if chart_config.y_axis_type is not None else None
        self.y_axis_title = chart_config.y_axis_title
        self.y_axis_tick_rotation = chart_config.y_axis_tick_rotation
        self.y_axis_tick_align = chart_config.y_axis_tick_align
        #TODO: Fix tick formatter settings?
        self.y_axis_major_formatter, self.y_axis_minor_formatter = self.y_axis_type.get_tick_formatters()

    def apply_to_chart(self):

        if self.label is not None:
            self.chart.set_label(self.label)

        #Only set X-axis details if the chart is a host axis
        #(parasite axes just vulch off of a host's X-axis)
        #if self.is_host:
        self.chart.grid(True if self.enable_grid_lines else False)
        self.chart.set_autoscalex_on(True if self.autoscale_x else False)
        self.chart.set_title(self.plot_title)
        self.chart.xaxis.set_label_text(self.x_axis_title)
        if self.x_axis_major_formatter is not None:
            self.chart.xaxis.set_major_formatter(self.x_axis_major_formatter)
        #Turn this on to have minor ticks display values as well
        #chart.xaxis.set_minor_formatter(chart_config.x_axis_minor_formatter)
        mpcsplot.set_axis_minor_ticks(self.chart.xaxis,self.enable_x_minor_ticks)

        labels = self.chart.get_xticklabels()
        for label in labels:
            #print 'Tick Rotation = %s' % (self.x_axis_tick_rotation)
            #print 'Tick align = %s' % (self.x_axis_tick_align)
            label.set_rotation(self.x_axis_tick_rotation)
            label.set_horizontalalignment(self.x_axis_tick_align)
        #matplotlib.artist.setp(self.chart.get_xmajorticklabels(),rotation=self.x_axis_tick_rotation)
        #matplotlib.artist.setp(self.chart.get_xmajorticklabels(),horizontalalignment=self.x_axis_tick_align)
        #matplotlib.artist.setp(self.chart.get_xminorticklabels(),rotation=self.x_axis_tick_rotation)
        #matplotlib.artist.setp(self.chart.get_xminorticklabels(),horizontalalignment=self.x_axis_tick_align)

        self.chart.set_autoscaley_on(True if self.autoscale_y else False)
        self.chart.yaxis.set_label_text(self.y_axis_title)
        if self.y_axis_major_formatter is not None:
            self.chart.yaxis.set_major_formatter(self.y_axis_major_formatter)
        mpcsplot.set_axis_minor_ticks(self.chart.yaxis,self.enable_y_minor_ticks)

        #if chart_config.x_axis_type == axistypes.ERT or chart_config.x_axis_type == axistypes.SCET:
        #    self.chart.figure.autofmt_xdate(rotation=chart_config.x_axis_tick_rotation,ha=chart_config.x_axis_tick_align)

class EhaChartConfig(ChartConfig): pass
class EvrChartConfig(ChartConfig): pass

#Configuration for an entire figure
# - contains 1 to N charts
# matplotlib.figure.Figure
class FigureConfig(object):

    def __init__(self):

        self.alpha = None
        self.edge_color = None
        self.face_color = None
        self.fig_height = None
        self.fig_width = None
        self.label = None

        self.chart_configs = []
        self.charts = []

        self.figure = None

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
