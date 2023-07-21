#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
from mpcsutil.gui import progressbar

__all__ = ['progressbar']

import mpcsutil
import mpcsplot
import matplotlib
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
import mpcsutil.gui.colorutil

sorted_colors = sorted([_color for _color in matplotlib.colors.cnames.keys()])

class SimpleTextEntryDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,window_title,label_title,default_label_text=''):

        self.label_title = label_title
        self.default_label_text = default_label_text

        tkSimpleDialog.Dialog.__init__(self,parent=parent,title=window_title)

        self.label = None
        self.entry = None

    def body(self, master):

        self.label = tk.Label(master,text=self.label_title)
        self.label.grid(row=0)

        self.entry = tk.Entry(master)
        self.entry.grid(row=0,column=1)
        self.entry.insert(0,self.default_label_text)

        return self.entry # initial focus

    def apply(self):

        self.result = self.entry.get()

class ChannelChooserDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,window_title):

        tkSimpleDialog.Dialog.__init__(self,parent=parent,title=window_title)

        self.combo = None
        self.entry = None
        self.dn_button = None
        self.eu_button = None

    def body(self, master):

        self.label = tk.Label(master,text='Channel ID')
        self.label.grid(row=0,column=0)
        #self.combo = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=["Channel ID","Channel Name"])
        #self.combo.grid(row=0,column=0)

        self.entry = tk.Entry(master)
        self.entry.grid(row=0,column=1)
        self.entry.insert(0,'')

        self.isDn = tk.IntVar()

        frame1 = tk.Frame(master)
        frame1.grid(row=1,column=0)
        frame2 = tk.Frame(master)
        frame2.grid(row=1,column=1)

        self.dn_button = tk.Radiobutton(frame2, text="DN", variable=self.isDn, value=0)
        self.dn_button.grid(row=0,column=0)
        self.eu_button = tk.Radiobutton(frame2, text="EU", variable=self.isDn, value=1)
        self.eu_button.grid(row=0,column=1)
        self.dn_button.select()

        return self.entry # initial focus

    def apply(self):

        #type = self.combo.get()

        value = self.entry.get()
        isDn = int(self.isDn.get()) == 0

        self.result = (value,isDn)

class EhaConfigDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,tc):

        self.trace_config = tc

        self.top_label = None

        self.color_label = None
        self.color_combobox = None
        self.draw_style_label = None
        self.draw_style_combobox = None
        self.lines_on_label = None
        self.lines_on_combobox = None
        self.line_style_label = None
        self.line_style_combobox = None
        self.line_width_label = None
        self.line_width_entry = None
        self.markers_on_label = None
        self.markers_on_combobox = None
        self.marker_size_label = None
        self.marker_size_entry = None
        self.marker_style_label = None
        self.marker_style_combobox = None
        self.plot_by_label = None
        self.plot_by_combobox = None

        tkSimpleDialog.Dialog.__init__(self,parent=parent,title='Configure Trace')

    def body(self, master):

        global sorted_colors

        grid_row = 0
        self.top_label = tk.Label(master, text="%s" % (self.trace_config.label))
        f = tkFont.Font(font=self.top_label['font'])
        f['weight'] = 'bold'
        f['size'] = 16
        self.top_label['font'] = f.name
        self.top_label.grid(row=grid_row, columnspan=2, sticky=tk.W)
        grid_row += 1

        title_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        title_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.color_label = tk.Label(master,text='Color: ')
        self.color_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.color_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=sorted_colors)
        if self.trace_config.color is not None:
            self.color_combobox.selectitem(self.trace_config.color)
        else:
            self.color_combobox.selectitem(matplotlib.colors.cnames.keys()[0])
        self.color_combobox.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        lines_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        lines_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.lines_on_label = tk.Label(master,text='Show Line: ')
        self.lines_on_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.lines_on_combobox = Pmw.ComboBox(master,dropdown=1,selectioncommand=self._lines_on_callback,
                                              scrolledlist_items=[str(True).lower(),str(False).lower()])
        if self.trace_config.lines_on is not None:
            self.lines_on_combobox.selectitem(str(self.trace_config.lines_on).lower())
        else:
            self.lines_on_combobox.selectitem(str(True).lower())
        self.lines_on_combobox.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        self.draw_style_label = tk.Label(master,text='Draw Style: ')
        self.draw_style_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.draw_style_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=mpcsplot.drawstyles.DrawStyles.VALUES)
        if self.trace_config.draw_style is not None:
            self.draw_style_combobox.selectitem(self.trace_config.draw_style.val_str)
        else:
            self.draw_style_combobox.selectitem(mpcsplot.drawstyles.DrawStyles.get_default().val_str)
        self.draw_style_combobox.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        self.line_style_label = tk.Label(master,text='Line Style: ')
        self.line_style_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.line_style_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=mpcsplot.linestyles.LineStyles.VALUES)
        if self.trace_config.line_style is not None:
            self.line_style_combobox.selectitem(self.trace_config.line_style.val_str)
        else:
            self.line_style_combobox.selectitem(mpcsplot.linestyles.LineStyles.get_default().val_str)
        self.line_style_combobox.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        self.line_width_label = tk.Label(master,text='Line Width (pts): ')
        self.line_width_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.line_width_entry = tk.Entry(master)
        if self.trace_config.line_width is not None:
            self.line_width_entry.insert(0,str(float(self.trace_config.line_width)))
        else:
            self.line_width_entry.insert(0,str(float(mpcsplot.DEFAULT_LINE_WIDTH)))
        self.line_width_entry.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        marker_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        marker_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.markers_on_label = tk.Label(master,text='Show Markers: ')
        self.markers_on_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.markers_on_combobox = Pmw.ComboBox(master,dropdown=1,selectioncommand=self._markers_on_callback,
                                                scrolledlist_items=[str(True).lower(),str(False).lower()])
        if self.trace_config.markers_on is not None:
            self.markers_on_combobox.selectitem(str(self.trace_config.markers_on).lower())
        else:
            self.markers_on_combobox.selectitem(str(True).lower())
        self.markers_on_combobox.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        self.marker_style_label = tk.Label(master,text='Marker Style: ')
        self.marker_style_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.marker_style_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=mpcsplot.markerstyles.MarkerStyles.VALUES)
        if self.trace_config.marker_style is not None:
            self.marker_style_combobox.selectitem(self.trace_config.marker_style.val_str)
        else:
            self.marker_style_combobox.selectitem(mpcsplot.markerstyles.MarkerStyles.get_default().val_str)
        self.marker_style_combobox.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        self.marker_size_label = tk.Label(master,text='Marker Size (pts): ')
        self.marker_size_label.grid(row=grid_row,column=0, sticky=tk.W)
        self.marker_size_entry = tk.Entry(master)
        if self.trace_config.marker_size is not None:
            self.marker_size_entry.insert(0,str(float(self.trace_config.marker_size)))
        else:
            self.marker_size_entry.insert(0,str(float(mpcsplot.DEFAULT_MARKER_SIZE)))
        self.marker_size_entry.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        plot_by_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        plot_by_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        #TODO: Don't let user choose EU if channel has no defined EU
        self.plot_by_label = tk.Label(master,text='Plot Value: ')
        self.plot_by_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.plot_by_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=['DN','EU'])
        if self.trace_config.plotByEu is not None:
            self.plot_by_combobox.selectitem('EU' if self.trace_config.plotByEu else 'DN')
        else:
            self.plot_by_combobox.selectitem('DN')
        self.plot_by_combobox.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        #Enable/disable fields based on beginning state
        self._lines_on_callback(self.lines_on_combobox.get())
        self._markers_on_callback(self.markers_on_combobox.get())

        return self.color_combobox

    def validate(self):

        test_color = self.color_combobox.get().lower()
        if test_color not in matplotlib.colors.cnames:
            tkMessageBox.showerror(title='Bad Color Value',
                                   message='Choose a valid color entry from the dropdown list. The entry "%s" is not valid.' % (test_color))
            return 0

        test_drawstyle = self.draw_style_combobox.get().lower()
        if not test_drawstyle in mpcsplot.drawstyles.DrawStyles.VALUES:
            tkMessageBox.showerror(title='Bad Draw Style Value',
                                   message='Choose a valid draw style entry from the dropdown list. The entry "%s" is not valid.' % (test_drawstyle))
            return 0

        test_lines_on = self.lines_on_combobox.get().lower()
        if test_lines_on not in [str(True).lower(),str(False).lower()]:
            tkMessageBox.showerror(title='Bad Lines On Value',
                                   message='"Lines On" must be set to %s or %s' % (str(True).lower(),str(False).lower()))
            return 0

        test_linestyle = self.line_style_combobox.get().lower()
        if not test_linestyle in mpcsplot.linestyles.LineStyles.VALUES:
            tkMessageBox.showerror(title='Bad Line Style Value',
                                   message='Choose a valid line style entry from the dropdown list. The entry "%s" is not valid.' % (test_linestyle))
            return 0

        test_linewidth = self.line_width_entry.get()
        try:
            test_linewidth=float(test_linewidth)
        except:
            tkMessageBox.showerror(title='Bad Line Width Value',
                                   message='The line width must be a floating point number greater than zero.')
            return 0
        if test_linewidth <=0:
            tkMessageBox.showerror(title='Bad Line Width Value',
                                   message='The line width must be a floating point number greater than zero.')
            return 0

        test_markers_on = self.markers_on_combobox.get().lower()
        if test_markers_on not in [str(True).lower(),str(False).lower()]:
            tkMessageBox.showerror(title='Bad Markers On Value',
                                   message='"Markers On" must be set to %s or %s' % (str(True).lower(),str(False).lower()))
            return 0

        test_markerstyle = self.marker_style_combobox.get().lower()
        if not test_markerstyle in mpcsplot.markerstyles.MarkerStyles.VALUES:
            tkMessageBox.showerror(title='Bad Marker Style Value',
                                   message='Choose a valid marker style entry from the dropdown list. The entry "%s" is not valid.' % (test_markerstyle))
            return 0

        test_markersize = self.marker_size_entry.get()
        try:
            test_markersize=float(test_markersize)
        except:
            tkMessageBox.showerror(title='Bad Marker Size Value',
                                   message='The marker size must be a floating point number greater than zero.')
            return 0
        if test_markersize <=0:
            tkMessageBox.showerror(title='Bad Marker Size Value',
                                   message='The marker size must be a floating point number greater than zero.')
            return 0

        #if there aren't any lines or markers, no data is going to show up
        if not mpcsutil.getBooleanFromString(test_markers_on) and not mpcsutil.getBooleanFromString(test_lines_on):
            tkMessageBox.showerror(title='No data display options selected',
                                   message='Both Lines and Markers are currently turned off.  No data will be displayed. Please turn on lines and/or markers.')
            return 0

        test_plot_value = self.plot_by_combobox.get().upper()
        if test_plot_value not in ['DN','EU']:
            tkMessageBox.showerror(title='Bad "Plot Value" Entry',
                                   message='"Plot Value" must be set to %s or %s' % ('DN','EU'))
            return 0

        return 1

    def _lines_on_callback(self,item):

        lines_on = mpcsutil.getBooleanFromString(item)

        if lines_on:
            self.line_style_combobox.configure(entry_state=tk.NORMAL)
            self.line_width_entry.configure(state=tk.NORMAL)
        else:
            self.line_style_combobox.configure(entry_state=tk.DISABLED)
            self.line_width_entry.configure(state=tk.DISABLED)

    def _markers_on_callback(self,item):

        markers_on = mpcsutil.getBooleanFromString(item)

        if markers_on:
            self.marker_style_combobox.configure(entry_state=tk.NORMAL)
            self.marker_size_entry.configure(state=tk.NORMAL)
        else:
            self.marker_style_combobox.configure(entry_state=tk.DISABLED)
            self.marker_size_entry.configure(state=tk.DISABLED)

    def apply(self):

        self.trace_config.color = self.color_combobox.get()
        self.trace_config.draw_style = mpcsplot.drawstyles.DrawStyles(strval=self.draw_style_combobox.get())
        self.trace_config.lines_on = mpcsutil.getBooleanFromString(self.lines_on_combobox.get())
        self.trace_config.line_style = mpcsplot.linestyles.LineStyles(strval=self.line_style_combobox.get())
        self.trace_config.line_width = float(self.line_width_entry.get())
        self.trace_config.markers_on = mpcsutil.getBooleanFromString(self.markers_on_combobox.get())
        self.trace_config.marker_style = mpcsplot.markerstyles.MarkerStyles(strval=self.marker_style_combobox.get())
        self.trace_config.marker_size = float(self.marker_size_entry.get())
        self.trace_config.plotByEu = self.plot_by_combobox.get().upper() == 'EU'

        self.result = True

class EvrChooserDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,window_title):

        self.evr_levels = mpcsutil.config.GdsConfig().getEvrLevelList()

        self.level_button_vars = []
        for _level in self.evr_levels:
            var = tk.IntVar()
            var.set(1)
            self.level_button_vars.append(var)

        self.level_labels = []
        self.level_buttons = []
        self.level_comboboxes = []

        #This function calls "body", so we have to make sure we do all of our initialization
        #for stuff we need in "body" before we call this
        tkSimpleDialog.Dialog.__init__(self,parent=parent,title=window_title)

    def body(self, master):

        grid_row = 0
        self.label = tk.Label(master,text='     EVR Levels to Plot     ')
        self.label.grid(row=grid_row,column=0,columnspan=2)
        grid_row += 1

        level_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        level_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        for i in range(0,len(self.evr_levels)):
            level_button = tk.Checkbutton(master=master,text=self.evr_levels[i],variable=self.level_button_vars[i])
            level_button.grid(row=grid_row,column=0,columnspan=1,sticky=tk.W)
            self.level_buttons.append(level_button)
            grid_row += 1

        return self.label # initial focus

    def apply(self):

        self.result = {}
        for i in range(0,len(self.evr_levels)):
            self.result[self.evr_levels[i]] = self.level_button_vars[i].get()

class EvrConfigDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,cc):

        self.gds_config = mpcsutil.config.GdsConfig()
        self.top_label = None

        self.marker_size_label = None
        self.marker_size_entry = None
        self.marker_style_label = None
        self.marker_style_combobox = None

        self.evr_levels = mpcsutil.config.GdsConfig().getEvrLevelList()

        self.level_button_vars = []
        for _level in self.evr_levels:
            var = tk.IntVar()
            var.set(1)
            self.level_button_vars.append(var)

        self.level_labels = []
        self.level_buttons = []
        self.level_comboboxes = []

        self.chart_config = cc

        tkSimpleDialog.Dialog.__init__(self,parent=parent,title='Configure EVRs')

    def _get_trace_config_for_level(self,level):

        config = None
        for trace_config in self.chart_config.trace_configs:
            data = trace_config.get_data()
            if data:
                if level == data[0].level:
                    config = trace_config

        return config

    def body(self, master):

        global sorted_colors

        grid_row = 0
        self.top_label = tk.Label(master, text="EVRs")
        f = tkFont.Font(font=self.top_label['font'])
        f['weight'] = 'bold'
        f['size'] = 16
        self.top_label['font'] = f.name
        self.top_label.grid(row=grid_row, columnspan=5, sticky=tk.W+tk.E+tk.N)
        grid_row += 1

        title_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        title_separator.grid(row=grid_row,column=0,columnspan=5,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        for i in range(0,len(self.evr_levels)):

            level = self.evr_levels[i]

            config = self._get_trace_config_for_level(level)

            color = None
            if config is None:
                # MPCS-9665 - 5/3/18 - R8 allows a default for background color.
                rgb_csv = self.gds_config.getProperty('evr.backgroundColor.%s' % (level),'Undefined')
                if rgb_csv == 'Undefined':
                    rgb_csv = self.gds_config.getProperty('evr.backgroundColor.default','0,0,255')
                (red,green,blue) = map(int,rgb_csv.split(","))
                color = '#%02x%02x%02x' % (red,green,blue)
            else:
                color = config.color

            grid_col = 0

            #Label for the level name
            level_label = tk.Label(master=master,text=level)
            f2 = tkFont.Font(font=level_label['font'])
            f2['weight'] = 'bold'
            f2['size'] = 12
            level_label['font'] = f.name
            level_label.grid(row=grid_row,column=grid_col,sticky=tk.W)
            self.level_labels.append(level_label)
            grid_col += 1

            #Label and checkbox for showing the level
            show_frame = tk.Frame(master=master)
            show_label = tk.Label(master=show_frame,text="Show:")
            show_label.pack(side=tk.LEFT,fill=tk.Y)
            self.level_button_vars[i].set(1 if config and config.visible else 0)
            level_button = tk.Checkbutton(master=show_frame,variable=self.level_button_vars[i])
            level_button.pack(side=tk.RIGHT,fill=tk.BOTH)
            self.level_buttons.append(level_button)
            show_frame.grid(row=grid_row,column=grid_col,padx=10)
            grid_col += 1

            color_frame = tk.Frame(master=master)
            color_label = tk.Label(master=color_frame,text='Color:')
            color_label.pack(side=tk.LEFT,fill=tk.Y)
            color_combobox = Pmw.ComboBox(parent=color_frame,dropdown=1,scrolledlist_items=sorted_colors)
            color_combobox.selectitem(colorutil.rgbHexToName(color))
            color_combobox.pack(side=tk.RIGHT,fill=tk.BOTH)
            self.level_comboboxes.append(color_combobox)
            color_frame.grid(row=grid_row,column=grid_col)
            grid_col += 1

            grid_row += 1

        marker_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        marker_separator.grid(row=grid_row,column=0,columnspan=5,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        self.marker_style_label = tk.Label(master,text='Marker Style: ')
        self.marker_style_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.marker_style_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=mpcsplot.markerstyles.MarkerStyles.VALUES)
        self.marker_style_combobox.selectitem(self.chart_config.trace_configs[0].marker_style.val_str)
        self.marker_style_combobox.grid(row=grid_row,column=1, sticky=tk.W)
        grid_row += 1

        self.marker_size_label = tk.Label(master,text='Marker Size (pts): ')
        self.marker_size_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.marker_size_entry = tk.Entry(master)
        self.marker_size_entry.insert(0,str(float(self.chart_config.trace_configs[0].marker_size)))
        self.marker_size_entry.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        return self.level_labels[0]

    def validate(self):

        #Test combo box colors
        for combobox in self.level_comboboxes:
            test_color = combobox.get().lower()
            if test_color not in matplotlib.colors.cnames:
                tkMessageBox.showerror(title='Bad Color Value',
                                       message='Choose a valid color entry from the dropdown list. The entry "%s" is not valid.' % (test_color))
                return 0

        test_markerstyle = self.marker_style_combobox.get().lower()
        if not test_markerstyle in mpcsplot.markerstyles.MarkerStyles.VALUES:
            tkMessageBox.showerror(title='Bad Marker Style Value',
                                   message='Choose a valid marker style entry from the dropdown list. The entry "%s" is not valid.' % (test_markerstyle))
            return 0

        test_markersize = self.marker_size_entry.get()
        try:
            test_markersize=float(test_markersize)
        except:
            tkMessageBox.showerror(title='Bad Marker Size Value',
                                   message='The marker size must be a floating point number greater than zero.')
            return 0
        if test_markersize <=0:
            tkMessageBox.showerror(title='Bad Marker Size Value',
                                   message='The marker size must be a floating point number greater than zero.')
            return 0

        return 1

    def apply(self):

        #Must apply colors to individual traces, but apply marker settings to all traces
        self.level_button_vars
        self.level_comboboxes
        self.marker_size_entry
        self.marker_style_combobox

        for i in range(0,len(self.evr_levels)):

            level = self.evr_levels[i]
            config = self._get_trace_config_for_level(level)
            if config is None:
                continue

            config.color = self.level_comboboxes[i].get()
            config.visible = bool(self.level_button_vars[i].get())

            config.marker_style = mpcsplot.markerstyles.MarkerStyles(strval=self.marker_style_combobox.get())
            config.marker_size = float(self.marker_size_entry.get())

            config.apply_to_trace()

        self.result = True

class AxisConfigDialog(tkSimpleDialog.Dialog):

    def __init__(self,parent,chart_config,axis):

        self.gds_config = mpcsutil.config.GdsConfig()
        self.top_label = None

        self.title_label = None
        self.title_entry = None
        self.show_minorticks_label = None
        self.show_minorticks_button = None
        self.axis_type_label = None
        self.axis_type_combobox = None

        self.chart_config = chart_config
        self.axes = chart_config.chart
        self.axis = axis
        self.is_x_axis = isinstance(self.axis,matplotlib.axis.XAxis)
        self.axis_type_changed = False

        self.axis_type = self.chart_config.y_axis_type
        self.axis_formatter_func = self.axes.format_ydata
        self.axis_limit_func = self.axes.set_ylim
        if self.is_x_axis:
            self.axis_type = self.chart_config.x_axis_type
            self.axis_formatter_func = self.axes.format_xdata
            self.axis_limit_func = self.axes.set_xlim

        tkSimpleDialog.Dialog.__init__(self,parent=parent,title='Configure Trace')

    def body(self, master):

        global sorted_colors

        grid_row = 0
        self.top_label = tk.Label(master, text="%s-Axis Settings" % ('X' if self.is_x_axis else 'Y'))
        f = tkFont.Font(font=self.top_label['font'])
        f['weight'] = 'bold'
        f['size'] = 16
        self.top_label['font'] = f.name
        self.top_label.grid(row=grid_row, columnspan=2, sticky=tk.W+tk.E)
        grid_row += 1

        title_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        title_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        #####################################

        self.title_label = tk.Label(master,text="Title: ")
        self.title_label.grid(row=grid_row,column=0,sticky=tk.W)
        self.title_entry = tk.Entry(master)
        self.title_entry.insert(0,self.chart_config.x_axis_title if self.is_x_axis else self.chart_config.y_axis_title)
        self.title_entry.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        self.axis_type_label = tk.Label(master,text='Axis Type: ')
        self.axis_type_label.grid(row=grid_row,column=0,sticky=tk.W)
        axis_types = [] #TODO: Make only one type available for Y-axis and grey it out
        if self.is_x_axis:
            axis_types = [type for type in mpcsplot.axistypes.AxisTypes.VALUES if mpcsplot.axistypes.AxisTypes.is_x_axis_type(type)]
        else:
            axis_types = [self.chart_config.y_axis_type.val_str]
        self.axis_type_combobox = Pmw.ComboBox(master,dropdown=1,scrolledlist_items=axis_types)
        #Disable?  self.axis_type_combobox.component('entryfield_entry').configure( state=DISABLED)
        #Populate field using self.axis_type_combobox.selectitem('')
        if self.is_x_axis:
            self.axis_type_combobox.selectitem(str(self.axis_type.val_str))
        else:
            self.axis_type_combobox.selectitem(str(self.axis_type.val_str))
        self.axis_type_combobox.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        self.show_minorticks_label = tk.Label(master,text='Show Minor Ticks: ')
        self.show_minorticks_label.grid(row=grid_row,column=0,sticky=tk.W)
        minorticks_on = tk.IntVar()
        minorticks_on.set(int(self.chart_config.enable_x_minor_ticks) if self.is_x_axis else int(self.chart_config.enable_y_minor_ticks))
        self.show_minorticks_button = tk.Checkbutton(master=master,variable=minorticks_on)
        self.show_minorticks_button.val = minorticks_on
        self.show_minorticks_button.grid(row=grid_row,column=1,sticky=tk.W)
        grid_row += 1

        range_separator = tk.Frame(master=master,height=2, bd=1, relief=tk.SUNKEN, background='black')
        range_separator.grid(row=grid_row,column=0,columnspan=2,sticky=tk.W+tk.E,padx=5,pady=5)
        grid_row += 1

        if self.axis_type.is_redefinable_range():

            self.axis_range_label = tk.Label(master, text="Axis Range")
            self.axis_range_label.grid(row=grid_row, columnspan=2, sticky=tk.W+tk.E)
            grid_row += 1

            #Format from axis to user format
            current_lower,current_upper = self.axis.get_view_interval()
            current_lower = self.axis_formatter_func(current_lower).replace('\n','T').strip()
            current_upper = self.axis_formatter_func(current_upper).replace('\n','T').strip()

            self.lower_bound_label = tk.Label(master,text="Lower Bound: ")
            self.lower_bound_label.grid(row=grid_row,column=0,sticky=tk.W)
            self.lower_bound_entry = tk.Entry(master)
            self.lower_bound_entry.insert(0,current_lower)
            self.lower_bound_entry.grid(row=grid_row,column=1,sticky=tk.W)
            grid_row += 1

            self.upper_bound_label = tk.Label(master,text="Upper Bound: ")
            self.upper_bound_label.grid(row=grid_row,column=0,sticky=tk.W)
            self.upper_bound_entry = tk.Entry(master)
            self.upper_bound_entry.insert(0,current_upper)
            self.upper_bound_entry.grid(row=grid_row,column=1,sticky=tk.W)
            grid_row += 1

        return self.title_entry

    def validate(self):

        test_title = self.title_entry.get()
        if not test_title.strip():
            tkMessageBox.showerror(title='Bad Title Value',
                                   message='The axis title cannot be empty. The entry "%s" is not valid.' % (test_title))
            return 0

        test_axis_type = self.axis_type_combobox.get()
        if (self.is_x_axis and not mpcsplot.axistypes.AxisTypes.is_x_axis_type(test_axis_type)) or \
           (not self.is_x_axis and not mpcsplot.axistypes.AxisTypes.is_y_axis_type(test_axis_type)):
                tkMessageBox.showerror(title='Bad Axis Type',
                                   message='The entry "%s" is not a valid axis type.  Choose a proper value from the dropdown list.' % (test_title))
                return 0

        if self.axis_type.is_redefinable_range():
            lower_bound = self.lower_bound_entry.get()
            try:
                self.axis_type.user_to_axis_format(lower_bound)
            except:
                tkMessageBox.showerror(title='Bad Axis Lower Bound',
                                       message='The axis lower bound value of %s is not a proper format.' % (lower_bound))
                return 0

            upper_bound = self.upper_bound_entry.get()
            try:
                self.axis_type.user_to_axis_format(upper_bound)
            except:
                tkMessageBox.showerror(title='Bad Axis Upper Bound',
                                       message='The axis upper bound value of %s is not a proper format.' % (lower_bound))
                return 0

        return 1

    def apply(self):

        title = self.title_entry.get()

        show = bool(self.show_minorticks_button.val.get())

        #How do we apply the new axis type to everything? (e.g. change from ERT to SCET)
        if self.is_x_axis:
            new_axis_type_str = self.axis_type_combobox.get()
            new_axis_type = mpcsplot.axistypes.AxisTypes(strval=new_axis_type_str)
            if new_axis_type != self.chart_config.x_axis_type:

                self.axis_type_changed = True

                #If the current title is the same default title for the axis, we should
                #change it when we change the axis type
                if title == self.chart_config.x_axis_type.get_default_title():
                    title = new_axis_type.get_default_title()

                self.chart_config.x_axis_type = new_axis_type

        if self.is_x_axis:
            self.chart_config.x_axis_title = title
        else:
            self.chart_config.y_axis_title = title
        self.axis.set_label_text(title)

        mpcsplot.set_axis_minor_ticks(self.axis,show)
        if self.is_x_axis:
            self.chart_config.enable_x_minor_ticks = show
        else:
            self.chart_config.enable_y_minor_ticks = show

        if self.axis_type.is_redefinable_range():
            lower_bound = self.lower_bound_entry.get()
            # Error occurs if we try to plot with extended precision
            # Cut any subseconds past the 3rd spot
            # If configured, extended time is displayed on mouseover
            index = lower_bound.find(".")
            if index > 0 and len(lower_bound.split(".")[1]) > 3:
                lower_bound=lower_bound[0:index+4]
            new_lower_bound = int(self.axis_type.user_to_axis_format(lower_bound))
            upper_bound = self.upper_bound_entry.get()
            new_upper_bound = self.axis_type.user_to_axis_format(upper_bound)

            self.axis_limit_func(new_lower_bound,new_upper_bound)

        self.result = True

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
