#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import atexit
import errno
import os
import select
import signal
import subprocess
import sys
import traceback

import mpcsutil
import mpcsutil.templatemgr
import buslog as buslog
import buslog.err

import optparse

SCRIPT_SUCCESS = 0
SCRIPT_FAILURE = 1

DEFAULT_TEMPLATE = "onelinesummary"
TEMPLATE_SUBDIR = '1553'

running = True

def _atexitCleanup():

    global running
    running = False

#Try to register an exit handler that will call shutdown so we can
#be sure everything terminates properly on exit
try:
    atexit.register(_atexitCleanup)
except:
    pass

def get_available_templates(templateMgr):

    available = templateMgr.getAvailableTemplates()
    try:
        available.remove(buslog.ENCODED_TEMPLATE)
    except ValueError:
        # Okay because we just want to ensure the ENCODED_TEMPLATE is not in the list
        pass
    return available

def create_options(available_templates):

    global DEFAULT_TEMPLATE

    parser = mpcsutil.create_option_parser()
    parser.usage = '%s [options] <input_1553_log_file>' % (mpcsutil.get_script_name())

    # MPCS-6683  - 10/15/15: Removed a to-do comment calling for a config file override
    # for older data.  Since the normal Gds hierarchy lookup is now used, this is possible by providing
    # a config file in the user config directory.

    output_group = optparse.OptionGroup(parser,'Output Parameters',
                                        'These parameters are used to control the where and how the raw and decode 1553 information are output.')
    output_group.add_option('--rawFile',action='store',type='string',dest='raw_output_file',metavar='FILENAME',default=None,
                            help='Dump the resulting raw 1553 log to the given output file. No decoding will be done, but you can use this option' +\
                            ' to get a 1553 log filtered by the time ranges, RTs, SAs, etc. that you request. If not specified, raw files will be' +\
                            'written to the console. This option is ignored unless the ' +\
                            '"--doRaw" option is specified. If this filename is the same as the --decodeFile, output will be merged.')
    output_group.add_option('--decodeFile',action='store',type='string',dest='decode_output_file',metavar='FILENAME',default=None,
                            help='Dump the decode 1553 log to the given output file.  If not specified, decode files will be' +\
                            'written to the console. This option will be ignored' +\
                            'if the "--noDecode" option is specified.  If this filename is the same as the --rawFile, output will be merged.')
    output_group.add_option('--doRaw',action='store_true',dest='do_raw',default=False,
                            help='By default, only a decode 1533 output file will be generated.  If you use this option, a raw 1553 output file will ' +\
                            'also be generated. It will be the same format as the original 1553 log file, but it will be filtered by all the times, RTs, ' +\
                            'SAs, etc. that you specified on the command line.')
    output_group.add_option('--noDecode',action='store_false',dest='do_decode',default=True,
                            help='By default, a decode 1533 output file will be generated. If you specify this option, the decode file will not be ' +\
                            'generated, but you can still use the "--doRaw" option to only generate a raw output file.')
    output_group.add_option('--dumpNames',action='store_true',dest='dump_names',default=False,
                            help='Debug output that dumps all the remote terminal (RT) and subaddresses (SA) numeric values and their associated string names. If this option is used, any 1553 ' +\
                            'input file will be ignored. The debug data will be dumped to the console and the program will terminate.')

    output_group.add_option('-o','--outputFormat',action='store',type='string',dest='template',default=DEFAULT_TEMPLATE,
                            help='The templated format of the decoded output.  The default value is "%s". The available templates are: %s' % (DEFAULT_TEMPLATE,available_templates))
    output_group.add_option('--verbose',action='store_true',dest='verbose',default=False,
                            help='Turn on verbose output decoding.  This will cause extra fields to be displayed such as RTI number, control word, status word, word count, error word, dt,' +\
                            ' and other data specific fields.')
    output_group.add_option('--realtime',action='store_true',dest='realtime',default=False,
                            help='Turn on realtime decoding. If this option is enabled, the decoder will execute a "tail" command on the input file and decode it as it grows starting' +\
                            ' at the end of the file rather than starting at the beginning of the file.')

    parser.add_option_group(output_group)

    info_group = optparse.OptionGroup(parser,'Information Restriction Parameters',
                                      'These parameters are used to restrict which particular log entries are output (e.g. which Remote Terminals (RTs) or Subaddresses (SAs))')
    #valid values for both of these are between 0 and 31
    info_group.add_option('--RTs',action='store',type='string',dest='rts',metavar='RT_LIST',default=None,
                          help='The list of remote terminals (RTs) to include in the output. Can be a single value (e.g. "10"), a range of values (e.g. "10..14"), or a comma-separated' +\
                               ' list of ranges and individual values (e.g. "1..4,10..14,28").')
    info_group.add_option('--SAs',action='store',type='string',dest='sas',metavar='SA_LIST',default=None,
                          help='The list of SubAddresses (SAs) to include in the output. Can be a single value (e.g. "10"), a range of values (e.g. "10..14"), or a comma-separated' +\
                               ' list of ranges and individual values (e.g. "1..4,10..14,28").')
    parser.add_option_group(info_group)

    time_group = optparse.OptionGroup(parser,'Timing Parameters',
                                      'These parameters are used to restrict the time range processed by the decoder.')

    time_group.add_option("--sysTimeStart",action="store",type="string",dest="sys_time_start",metavar="ISO/DOY/1553_TIME",default=None,
                      help="Begin time of desired log entry time range using log file system time. The time may be specified in one of three " +\
                      "formats: ISO (YYYY-MM-DDThh:mm:ss), DOY (YYYY-DDDThh:mm:ss) or 1553 (YYYYMMDDTHHMMSS).")
    time_group.add_option("--sysTimeEnd",action="store",type="string",dest="sys_time_end",metavar="ISO/DOY/1553_TIME",default=None,
                      help="End time of desired log entry time range using log file system time. The time may be specified in one of three " +\
                      "formats: ISO (YYYY-MM-DDThh:mm:ss), DOY (YYYY-DDDThh:mm:ss) or 1553 (YYYYMMDDTHHMMSS).")
    time_group.add_option("--sclkStart",action="store",type="string",dest="sclk_start",metavar="SCLK",default=None,
                      help="Begin time of desired log entry time range using Spacecraft Clock (SCLK). The time may be specified in " +\
                      "SECONDS.SUBSECONDS format, COARSE-FINE format or as an integer containing SCLK_MICROSECONDS (like in the raw 1553 log).")
    time_group.add_option("--sclkEnd",action="store",type="string",dest="sclk_end",metavar="SCLK",default=None,
                      help="End time of desired log entry time range using Spacecraft Clock (SCLK). The time may be specified in " +\
                      "SECONDS.SUBSECONDS format, COARSE-FINE format or as an integer containing SCLK_MICROSECONDS (like in the raw 1553 log).")
    parser.add_option_group(time_group)

    return parser

def get_file_object(output_file_name):

    output_file = None
    if output_file_name is not None:
        output_file = open(output_file_name,'w')
    else:
        output_file = sys.stdout

    return output_file

def decode_line(raw_output_file,raw_template,decode_output_file,decode_template,log_config,verbose,line,filter):

    entry = None
    try:
        entry = buslog.LogEntry()
        entry.verbose = verbose
        entry.parse_entry(line,log_config)
    except (buslog.err.LogParseException,ValueError) as e:
        print(sys.stderr,'ERROR parsing log file entry \'\'\'%s\'\'\': %s' % (line,e), file=sys.stderr, flush=True)
        return

    if filter.accept(entry):

        if raw_output_file is not None:
            raw_output_file.write(entry.encode_str(template=raw_template))
            raw_output_file.write('\n')

        if decode_output_file is not None:
            decode_output_file.write(entry.decode_str(template=decode_template))
            decode_output_file.write('\n')

def run_postprocess_mode(input_filename,raw_output_file,raw_template,decode_output_file,decode_template,log_config,verbose,filter):

    with open(input_filename) as infile:
        for line in infile:
            if line:
                decode_line(raw_output_file,raw_template,decode_output_file,decode_template,log_config,verbose,line,filter)

def run_realtime_mode(input_filename,raw_output_file,raw_template,decode_output_file,decode_template,log_config,verbose,filter):

    global running

    process = subprocess.Popen('tail -f %s' % (input_filename),
                               shell=True,
                               stdout=subprocess.PIPE,
                               stdin=None,
                               stderr=subprocess.PIPE,
                               env=os.environ)

    process_stderr = process.stderr
    process_stdout = process.stdout
    stderr_fileno = process_stderr.fileno()
    stdout_fileno = process_stdout.fileno()

    #Set up some pollers so we're not doing busy waiting looking for lines in the log
    poller = select.poll()
    poller.register(stdout_fileno,select.POLLIN|select.POLLPRI|select.POLLERR|select.POLLHUP)
    poller.register(stderr_fileno,select.POLLIN|select.POLLPRI|select.POLLERR|select.POLLHUP)

    while running:

        try:
            #Poll for new messages
            events = poller.poll(100)
            for event in events:

                if event[0] == stdout_fileno:
                    line = process_stdout.readline()[:-1]
                    if line:
                        decode_line(raw_output_file,raw_template,decode_output_file,decode_template,log_config,verbose,line,filter)

                elif event[0] == stderr_fileno:
                    errline = process_stderr.readline().strip()
                    print('Error from background 1553 file reader process: %s' % (errline), file=sys.stderr, flush=True)

                elif (event[1] & select.POLLHUP):
                    print('Background 1553 file reader process died or was killed.', file=sys.stderr, flush=True)
                    running = False
                    break

        except Exception as exc:

            if hasattr(exc,'errno'):
                if exc.errno == errno.EINTR:
                    continue

            raise

    try:
        os.kill(process.pid,signal.SIGTERM)
    except OSError:
        pass


def main():

    # MPCS-6683  - 10/15/15: Use TemplateManager to find templates, rather than only looking
    # in the mission's template folder.
    templateMgr = mpcsutil.templatemgr.TemplateManager(TEMPLATE_SUBDIR)
    available_templates = get_available_templates(templateMgr)
    parser = create_options(available_templates)
    parser.parse_args()

    if not parser.values.do_raw and not parser.values.do_decode:
        print("You must generate raw and/or decode output. You have turned off both of these options. Please check your command line.", file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    if not parser.largs and not parser.values.dump_names:
        print("No input filename specified! (Use the -h option if you need help)", file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    log_config = buslog.config.LogConfig()
    try:
        log_config.parse_config()
    except Exception as e:
        print('Failed to parse the 1553 configuration file: {}'.format(e), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    if parser.values.dump_names:
        log_config.dump_config()
        sys.exit(SCRIPT_SUCCESS)

    decode_template = str(parser.values.template).lower()
    if not available_templates:
        print("Could not find any existing 1553 templates on disk!", file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)
    elif not decode_template in available_templates:
        print("The input template '%s' is not one of the available templates. Your choices are: %s." % (decode_template,available_templates), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    decode_template = templateMgr.getMostLocalTemplate(decode_template)
    raw_template = templateMgr.getMostLocalTemplate(buslog.ENCODED_TEMPLATE)
    """ MPCS-7159  - 08/15/2015: Cheetah does not support unicode file paths.  Attempt to cast to ascii """
    try:
        decode_template = str(decode_template)
        raw_template = str(raw_template)
    except UnicodeEncodeError:
        print('Unicode file paths are not supported; please ensure the paths to templates are ASCII', file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    if len(parser.largs) > 1:
        print('Found multiple filenames on the command line: "%s".' % (parser.largs), file=sys.stderr, flush=True)
        print('Only one 1553 log input file is allowed.', file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    input_filename = parser.largs[0]
    if not os.path.exists(input_filename):
        print('The input file "%s" does not exist.' % (input_filename), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)
    elif os.path.isdir(input_filename):
        print('The input file "%s" is a directory.' % (input_filename), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)
    input_filename = os.path.abspath(input_filename)

    try:
        filter = buslog.LogEntryFilter(parser.values)
    except Exception as e:
        print('Error parsing command line: %s' % (e), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    raw_output_file, raw_output_filename, decode_output_file, decode_output_filename = None,None,None,None

    if parser.values.raw_output_file is not None:
        raw_output_filename = os.path.abspath(parser.values.raw_output_file)

    if parser.values.decode_output_file is not None:
        decode_output_filename = os.path.abspath(parser.values.decode_output_file)

    try:
        if parser.values.do_raw or raw_output_filename is not None:
            raw_output_file = get_file_object(raw_output_filename)
    except IOError as ioe:
        print('Could not open raw output file!  %s' % (ioe), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)
    try:
        if parser.values.do_decode or decode_output_filename is not None:
            #If both output filenames are the same, don't open the file twice...just use the same handle
            if decode_output_filename == raw_output_filename and raw_output_file is not None:
                decode_output_file = raw_output_file
            else:
                decode_output_file = get_file_object(parser.values.decode_output_file)
    except IOError as ioe:
        print('Could not open decode output file!  %s' % (ioe), file=sys.stderr, flush=True)
        sys.exit(SCRIPT_FAILURE)

    try:
        if parser.values.realtime:
            run_realtime_mode(input_filename,raw_output_file,raw_template,decode_output_file,decode_template,log_config,parser.values.verbose,filter)
        else:
            run_postprocess_mode(input_filename,raw_output_file,raw_template,decode_output_file,decode_template,log_config,parser.values.verbose,filter)
    except (KeyboardInterrupt, buslog.err.EndOfTimeRange) as e:
        #If the user hits Ctrl-C or we've past the upper bound time range in the log file, these are both acceptable
        #ending conditions and are handled silently
        pass
    except:
        print('Encountered an unexpected exception.  Terminating the decoder...', file=sys.stderr, flush=True)
        print(sys.exc_info())
        traceback.print_tb(sys.exc_info()[2])
        sys.exit(SCRIPT_FAILURE)
    finally:
        if raw_output_file is not None:
            raw_output_file.close()
        if decode_output_file is not None:
            decode_output_file.close()

    sys.exit(SCRIPT_SUCCESS)

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
