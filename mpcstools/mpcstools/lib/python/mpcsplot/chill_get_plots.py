#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import sys
import logging
import optparse
import warnings

import mpcsutil
import mpcsplot

_log = lambda : logging.getLogger('mpcs.chill_get_plots')

def create_options():

    parser = mpcsutil.create_option_parser()

    #EHA-related parameters
    eha_group = optparse.OptionGroup(parser,'EHA-related Query Parameters',
                                     'These parameters are all used to restrict the EHA values returned by the database query.')
    eha_group.is_java_group = True
    eha_group.add_option("-b","--beginTime",action="store",type="string",dest="begin_time",metavar="ISO_OR_DOY_TIME",default=None,
                      help="Begin time of data time range (for SCLK, SCET, ERT, LST). This is a time range on telemetry, not sessions.")
    eha_group.add_option("-e","--endTime",action="store",type="string",dest="end_time",metavar="ISO_OR_DOY_TIME",default=None,
                      help="End time of data time range (for SCLK, SCET, ERT, LST). This is a time range on telemetry, not sessions.")
    eha_group.add_option("-p","--channelIdFile",action="store",type="string",dest="channel_id_file",metavar="FILENAME",default=None,
                      help="The name of a file containing a list of channel IDs to query for.  An alternative option for the -z option. Channel IDs " +
                           "use the same format as in the -z option.")

    # MPCS-8237 9/29/2016 - Added LST support

    eha_group.add_option("-t","--timeType",action="store",type="string",dest="time_type",metavar="TYPE",default=None,
                      help="The time type for the --beginTime and --endTime options.  Valid values are: [SCLK, SCET, ERT, LST]")
    eha_group.add_option("-z","--channelIds",action="store",type="string",dest="channel_ids",metavar="STRING",default=None,
                      help="A comma-separated list of channel IDs to query or channel ID ranges (e.g. DMX-0001 or PWR-1000..PWR-1100)")
    parser.add_option_group(eha_group)

    #Session-related parameters
    session_group = optparse.OptionGroup(parser,'Session-related Query Parameters',
                                     'These parameters are all used to restrict the Sessions examined by the database query.')
    session_group.is_java_group = True
    session_group.add_option("-w","--fromTestStart",action="store",type="string",dest="from_session_start",metavar="ISO_OR_DOY_TIME",default=None,
                      help="Start time of session time range (lower bound on the start time of sessions to query). This is a time range on sessions, not telemetry.")
    session_group.add_option("-x","--toTestStart",action="store",type="string",dest="to_session_start",metavar="ISO_OR_DOY_TIME",default=None,
                      help="End time of session time range (upper bound on the start time of sessions to query). This is a time range on sessions, not telemetry.")
    session_group.add_option("-D","--fswVersionPattern",action="store",type="string",dest="fsw_version_pattern",metavar="STRING",default=None,
                      help="A query on the 'FSW version' field of the session information. Individual entries may be specified using an SQL LIKE pattern with wildcards like %." +
                           " Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-E","--downlinkStreamId",action="store",type="string",dest="downlink_stream_id",metavar="STRING",default=None,
                      help='A query on the "Downlink Stream ID" field on the session information. Valid values are: ["Selected DL", LV, TZ, "Command Echo"].' +
                           'Multiples values may be supplied in a comma-separated value (CSV) format.')
    session_group.add_option("-K","--testKey",action="store",type="string",dest="session_key",metavar="NUMBER",default=None,
                      help="The unique numeric identifier for a session. Multiple values may be supplied in a comma-separated value (CSV) format." +
                           "Ranges of values may also be specified using '..' notation (e.g. -K 102..105 to query sessions 102,103,104 and 105).")
    session_group.add_option("-L","--testDescriptionPattern",action="store",type="string",dest="session_desc_pattern",metavar="STRING",default=None,
                      help="A query on the 'description' field of the session information. Individual entries may be specified using an SQL LIKE pattern with wildcards like %." +
                           " Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-M","--testNamePattern",action="store",type="string",dest="session_name_pattern",metavar="STRING",default=None,
                      help="A query on the 'name' field of the session information. Individual entries may be specified using an SQL LIKE pattern with wildcards like %." +
                           " Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-O","--testHostPattern",action="store",type="string",dest="session_host_pattern",metavar="STRING",default=None,
                      help="A query on the 'host' field of the session information. This is generally the name of the host that the session was run on." +
                           "Individual entries may be specified using an SQL LIKE pattern with wildcards like %. Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-P","--testUserPattern",action="store",type="string",dest="session_user_pattern",metavar="STRING",default=None,
                      help="A query on the 'user' field of the session information. This is generally the username of the user that ran the session." +
                           "Individual entries may be specified using an SQL LIKE pattern with wildcards like %. Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-Q","--testTypePattern",action="store",type="string",dest="session_type_pattern",metavar="STRING",default=None,
                      help="A query on the 'type' field of the session information. Individual entries may be specified using an SQL LIKE pattern with wildcards like %." +
                           " Multiple values may be specified in a comma-separated value (CSV) format.")
    session_group.add_option("-W","--sseVersionPattern",action="store",type="string",dest="sse_version_pattern",metavar="STRING",default=None,
                      help="A query on the 'SSE version' field of the session information. Individual entries may be specified using an SQL LIKE pattern with wildcards like %." +
                           " Multiple values may be specified in a comma-separated value (CSV) format.")

    session_group.add_option("--dssId",action="store",type="string",dest="cv_dssid",metavar="STRING",default=None,
                      help="A query on the 'DSS ID' field of the channel value information. Multiple values may be specified in a comma-separated value (CSV) format.")

    session_group.add_option("--vcid",action="store",type="string",dest="cv_vcid",metavar="STRING",default=None,
                      help="A query on the 'VCID' field of the channel value information. Multiple values may be specified in a comma-separated value (CSV) format.")

    session_group.add_option("--stringId",action="store",type="string",dest="cv_string",metavar="STRING",default=None,
                      help="String id turned into VCID of the channel value information. Multiple values may be specified in a comma-separated value (CSV) format.")

    session_group.add_option("--channelTypes",action="store",type="string",dest="cv_types",metavar="STRING",default=None,
                      help="Flags that specify the types of channel values desired: s=SSE, h=header, m=monitor, f=FSW realtime, r=FSW recorded.")

    parser.add_option_group(session_group)

    #Network parameters
    network_group = optparse.OptionGroup(parser,'Network-related Database Parameters',
                                     'These parameters are all used to control which database on the network is being queried.')
    network_group.is_java_group = True
    network_group.add_option("--dbPwd",action="store",type="string",dest="database_password",metavar="STRING",default=None,
                      help="The password required to connect to the database.")
    network_group.add_option("--dbUser",action="store",type="string",dest="database_user",metavar="STRING",default=None,
                      help="The username required to connect to the database.")
    network_group.add_option("-j","--databaseHost",action="store",type="string",dest="database_host",metavar="HOSTNAME",default=None,
                      help="The host that the database to query resides on. This value may also be the name of one of the testbeds (e.g. FSWTB) and the query " +
                           "application will automatically determine the proper hostname of the GDS machine in the specified testbed. Defaults to the localhost.")
    network_group.add_option("-n","--databasePort",action="store",type="int",dest="database_port",metavar="PORT_NUMBER",default=None,
                      help="The port number that the database to query is listening on.")
    parser.add_option_group(network_group)

    #Plotting output parameters
    plotting_group = optparse.OptionGroup(parser,'Plotting-related Parameters',
                                     'These parameters are all used to control the style and format of the output plots. Trace colors are assigned to particular traces on' +\
                                     ' the plot in a deterministic order. To see the list of colors assigned to traces (in the order of channel IDs on the command line), execute the' +\
                                     ' following command on the Unix command line: "python -c \'import matplotlib.colors; print matplotlib.colors.cnames.keys()\'"')
    plotting_group.is_java_group = False
    plotting_group.add_option("-a","--outputFile",action="store",type="string",dest="output_file",metavar="FILENAME",default=None,
                      help="Sets the destination file path if plots are being written to a file. By specifying this option, the tool will NOT bring up a GUI.")
    eha_group.add_option("-c","--chartConfigFile",action="store",type="string",dest="config_filename",metavar="FILENAME",default=None,
                      help="A configuration file describing all of the charts to be generated. NOT CURRENTLY IMPLEMENTED.")
    plotting_group.add_option("--legendLocation",action="store",type="string",dest="legend_location",metavar="STRING",default=mpcsplot.legendlocations.LegendLocations.get_default().val_str,
                      help="Sets the location of the legend on the chart. Valid options are: %s. Default is '%s'." % (mpcsplot.legendlocations.LegendLocations.VALUES,mpcsplot.legendlocations.LegendLocations.get_default()))
    plotting_group.add_option("--lineStyle",action="store",type="string",dest="line_style",metavar="STRING",default=mpcsplot.linestyles.LineStyles.get_default().val_str,
                      help="Sets the line style to use. Valid options are: %s. Default is '%s'." % (mpcsplot.linestyles.LineStyles.VALUES,mpcsplot.linestyles.LineStyles.get_default()))
    plotting_group.add_option("--markerStyle",action="store",type="string",dest="marker_style",metavar="STRING",default=mpcsplot.markerstyles.MarkerStyles.get_default().val_str,
                      help="Sets the style of points to use. This option will be ignored on any plots with lines if the --showShapes option is not present. Valid values are: %s. Default value is '%s'." %
                      (mpcsplot.markerstyles.MarkerStyles.VALUES,mpcsplot.markerstyles.MarkerStyles.get_default()))
    plotting_group.add_option("--markerSize",action="store",type="int",dest="marker_size",metavar="INT",default=mpcsplot.DEFAULT_MARKER_SIZE,
                      help="Sets the size (in pts.) for displayed data points. This option will be ignored on any plots with lines if the --showShapes option is not present. Default value is %d pts." %
                      (mpcsplot.DEFAULT_MARKER_SIZE))
    plotting_group.add_option("-o","--outputFormat",action="store",type="string",dest="output_format",metavar="FORMAT",default=mpcsplot.fileformats.FileFormats.get_default().val_str,
                      help="Sets the output format for files generated when tool is run in non-GUI mode. " +
                      "Valid options are: %s. Default is '%s'. NOTE: this extension will automatically  be added to the end of the file specified by --outputFile. This option is ignored if --outputFile is not specified." %
                      (mpcsplot.fileformats.FileFormats.VALUES,mpcsplot.fileformats.FileFormats.get_default()))
    plotting_group.add_option("--onlyShapes",action="store_true",dest="only_shapes",default=False,
                      help="Display only individual data point markers, not lines, on the plot.")
    plotting_group.add_option("--plotByEU",action="store_true",dest="plot_by_eu",default=False,
                      help="Use EU values on the Y-Axis instead of DN values.  DN values are plotted by default.")
    plotting_group.add_option("--plotStyle",action="store",type="string",dest="plot_style",metavar="STRING",default=mpcsplot.drawstyles.DrawStyles.get_default().val_str,
                      help="The type of plot to draw. Valid options are: %s. Default is '%s'." %
                      (mpcsplot.drawstyles.DrawStyles.VALUES,mpcsplot.drawstyles.DrawStyles.get_default()))
    plotting_group.add_option("--plotTitle",action="store",type="string",dest="plot_title",metavar="STRING",default=None,
                      help="The title to display above the plot. Will default to <Yaxis> vs. <Xaxis> if not specified.")
    plotting_group.add_option("--showGridlines",action="store_true",dest="show_gridlines",default=False,
                      help="Display gridlines on the plots.")
    plotting_group.add_option("--showShapes",action="store_true",dest="show_shapes",default=False,
                      help="Display markers for individual data points on the plot.")
    plotting_group.add_option("--x_axisTitle",action="store",type="string",dest="x_axis_title",metavar="STRING",default=None,
                      help="The title to display on the X Axis of the plot. Defaults to the name of the data on the axis (e.g. ERT).")
    plotting_group.add_option("--y_axisTitle",action="store",type="string",dest="y_axis_title",metavar="STRING",default=None,
                      help="The title to display on the Y Axis of the plot. Defaults to the name of the data on the axis (e.g. DN).")
    parser.add_option_group(plotting_group)

    return parser

def test():

    # Turn off this warning, which is harmless
    warnings.filterwarnings('ignore', 'No labeled objects found', UserWarning)

    parser = create_options()
    parser.parse_args()

    #headless mode
    if parser.values.output_file is not None:
        import mpcsplot.headless
        plotter = mpcsplot.headless.HeadlessPlotManager()
    #gui mode
    else:
        import mpcsplot.gui
        plotter = mpcsplot.gui.GuiPlotManager()

    try:
        plotter.parse_plot_options(parser)
    except mpcsplot.err.DataRetrievalError as dre:
        _log().fatal('Could not execute initial data query: {}'.format(dre))
        sys.exit(1)

    sys.exit(plotter.run())

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
