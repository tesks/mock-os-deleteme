#!/usr/bin/python
#
# This script converts a monitor channel dictionary from the old-style AMPCS
# monitor schema to multimission channel dictionary format.  It then adds
# a custom EU definition (not supported by the old schema) to the resulting
# files for channels that require a specific EU conversion that computes 
# nanoseconds.
# 
# The same EU conversion is required for many MON channels. In addition
# to the nanoseconds channel the EU conversion is attached to, these EU 
# conversions also require a "seconds" channel, which in the MON standard
# is always the channel with channel number one less than the channel to
# to which the EU is attached. (Thank goodness those DSN folks are 
# consistent!)
#
# The list of channels that require the custom EU conversion must be in
# support file "MonitorEUChannelList.txt", one per line.
#
# The text to be inserted in order to define the EU conversion must be in
# support file "MonitorEUDefinition.txt", exactly as it is desired to appear 
# in the output file, and with a {channel} token in the position in which
# the parent "seconds" channel ID is to be inserted.
#
#
# This script was added in support of Jira MPCS-6588 and was last updated
# on 9/19/14 for MON-0158 Revision G.

import subprocess
import os
import argparse
import sys
import datetime

ADG_INSTALLATION = os.environ.get('ADG_HOME')
if ADG_INSTALLATION is None:
    print 'The environment variable "ADG_HOME" is not set. It must be set to the installation \
directory of your AMPCS data generator package.'
    exit(-1)

#                                                                                                                                                
# Main Entry Point                                                                                                                               
#                                                                                                                                                
if __name__ == "__main__":
    argParser = argparse.ArgumentParser(description=
        'Convert old style AMPCS MON-0158 dictionary to new format and add EU algorithm definitions to it.')

    argParser.add_argument('-i', '--inputDictionary', help='Path to input dictionary (required)', 
                           default=None, required=True)
    argParser.add_argument('-o', '--outputDictionary', 
                           help='Path to output dictionary (defaults to ./monitor_channel.xml)', 
                           default='./monitor_channel.xml')
    argParser.add_argument('-s', '--supportPath', 
                           help='Path to conversion support files (defaults to .)', 
                           default='.')

    args = argParser.parse_args()

    if len(sys.argv) < 2:
        argParser.print_help()
        exit(-1)
    
    # Run the MM converter on the input file and write the output to a temp file
    temp_file = args.outputDictionary + "_temp"
    converter = ADG_INSTALLATION + "/bin/mm_convert_channel_xml"
    converter_args = [converter] + ["--dictionary", args.inputDictionary, 
                                    "--sourceSchema", "monitor", 
                                    "--outputFile", temp_file]
    
    ret_code = subprocess.call(converter_args)
    
    if ret_code != 0:
       print "Execution of converter failed"
       exit(-1)

    # Read contents of support files
    channels = open(args.supportPath + "/MonitorEUchannelList.txt", "r")
    eu_text = open(args.supportPath + "/MonitorEUDefinition.txt", "r")
    
    # This is the list of channels needing EUs
    channel_list = channels.read().split('\n')
    # This is the EU text to insert
    eu_text_str = eu_text.read()
    
    found = False
    
    input = open(temp_file, "r")
    output = open(args.outputDictionary, "w")
    
    output.write("<!--\n")
    output.write("# AMPCS MON-0158 multimission monitor channel dictionary\n")
    output.write("#\n")
    output.write("# Generated: " + str(datetime.datetime.now()) + "\n")
    output.write("# ARE YOU UPDATING THIS FILE BY HAND?\n")
    output.write("# STOP!!\n")
    output.write("#\n")
    output.write("# This file is generated from file " + args.inputDictionary + "\n")
    output.write("# See instructions in that file.\n")
    output.write("#\n") 
    output.write("-->\n")
    
    # Read the temp file line by line
    for line in input:
        
        # Every existing line gets written to the output file
        output.write(line)
        
        # Now see if we have found a channel that is in the
        # list of those needing EU. All channels start with
        # the <telemetry> element
        if "<telemetry" in line:
            
           # Need to extract the channel ID from the "abbreviation"
           # attribute on this element 
           channel_index = line.find("abbreviation=")
           channel = line[channel_index + 13:]
           channel_index = channel.find('"')
           channel = channel[channel_index + 1:]
           channel_index = channel.find('"')
           channel = channel[0:channel_index]
           
           # Got channel ID. Is it in the EU list?
           if channel in channel_list:
               found = True
    
        # If processing a channel that needs EU, then the
        # EU definition goes after the <raw_units> element.
        if found == True and "<raw_units" in line:
    
           # Compute the "seconds" channel ID: one less than the current 
           channel_num = channel.split('-')[1]
           secs_channel= "M-%04d" % (int(channel_num) - 1);
    
           # Write the EU definition and reset for the next channel
           output.write(eu_text_str.format(channel=secs_channel))
           channel = None
           found = False  
    
    # Clean up
    input.close()
    output.close()
    channels.close()
    eu_text.close()
    os.remove(temp_file)
        
    print "Final converted output written to " + args.outputDictionary
