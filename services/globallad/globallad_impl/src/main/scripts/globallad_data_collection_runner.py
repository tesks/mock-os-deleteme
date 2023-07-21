#!/usr/bin/env python

import sys
import os
import re
import subprocess as SP

line_rx = re.compile("^(.*?)#", re.M)
cmd_fmt = " ".join(["./globallad_data_collection.py",
                    "--numConnections %s",
                    "--numInserters %s",
                    "--depth %s",
                    "--clientRingBufferSize %s",
                    "--inserterRingBufferSize %s",
                    "--bitRate %s",
                    "--outputDir %s",
                    "--dataFile %s",
                    "--sampleCount %s",
                    "--plotThreshold %s",
                    "--treeDepth %s"
                    ])

def do_runs(input_files):
    '''Parses the input files and do runs.'''
    for input_file in input_files:
        vals = [f for f in re.findall(line_rx, open(input_file).read()) if f]
        
        try:
            cmd = cmd_fmt % tuple(vals)
            proc = SP.Popen(cmd, shell=True)
            proc.wait()
        except Exception, e:
            print "Failed to create command for file " + input_file
            

if __name__ == "__main__":
    do_runs(sys.argv[1:])
