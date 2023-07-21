#!/usr/bin/env python

import argparse
import re
import os
import time
import subprocess as SP
import tempfile
import struct

file_name_fmt = "connections_%d_inserters_%d_depth_%d_client-buffer_%d_inserter-buffer_%d_bitrate_%d_tree-depth_%d"
file_name_rx = "connections_(\d+)_inserters_(\d+)_depth_(\d+)_client-buffer_(\d+)_inserter-buffer_(\d+)_bitrate_(\d+)_tree-depth_(\d+)"

GLAD_CMD = "$CHILL_GDS/bin/test/chill_test_lad_server"
METER_CMD = "$CHILL_GDS/bin/tools/chill_meter_to_socket -c -p 8900 -r %d -s %d -f %s -verbose"
METER_BUFFER_SECONDS = 5

properties_file = "~/CHILL/globallad.properties"

tree_structs = ["master-->userDataType-->identifier", 
                "master-->dssId-->userDataType-->identifier",
                "master-->vcid-->dssId-->userDataType-->identifier",
                "master-->sessionNumber-->vcid-->dssId-->userDataType-->identifier",
                "master-->venue-->sessionNumber-->vcid-->dssId-->userDataType-->identifier",
                "master-->scid-->venue-->sessionNumber-->vcid-->dssId-->userDataType-->identifier",
                "master-->host-->scid-->venue-->sessionNumber-->vcid-->dssId-->userDataType-->identifier"]

class GladTester:
    
    def __init__(self, parser):
        self.client_buffer_sizes = set(map(lambda v: int(v), parser.cbuffer.split(",")))
        self.inserter_buffer_sizes = set(map(lambda v: int(v), parser.ibuffer.split(",")))
        self.bitrates = set(map(lambda v: int(v), parser.brate.split(",")))
        self.connections = set(map(lambda v: int(v), parser.connections.split(",")))
        self.inserters = set(map(lambda v: int(v), parser.inserters.split(",")))
        self.depths = set(map(lambda v: int(v), parser.depth.split(",")))
        self.output_dir = parser.output_dir
        self.data_file = list(set(parser.data.split(",")))
        self.tree_depths = set(map(lambda v: int(v), parser.tdepth.split(",")))
        self.sample_size = parser.scount
        self.threshold = parser.threshold

        
    def run(self):
        '''Runs the set of tests.'''
        count = 0
        for tree_depth in self.tree_depths:
            index = tree_depth - 3
            
            if index < 0:
                print "Bogus depth %d, must an integer between 3 and 7." % tree_depth
                continue
            
            tree_struct = tree_structs[index]
            for num_connections in self.connections:
                for num_inserters in self.inserters:
                    for client_buffer_size in self.client_buffer_sizes:
                        for inserter_buffer_size in self.inserter_buffer_sizes:
                            for bitrate in self.bitrates:
                                for depth in self.depths:
                                    fname = self.update_properties(client_buffer_size, inserter_buffer_size, bitrate, num_connections, num_inserters, depth, tree_struct)
                                    count += 1
                                    
                                    # Start up the lad server. 
                                    proc = SP.Popen(GLAD_CMD, shell=True)
                                    self.start_clients(num_connections, bitrate)

                                    # Finished with the run, kill and wait.
                                    proc.terminate()
                                    proc.wait()
                                    print "%6d: Finished process: %s" % (count, fname)
                                    
                                    
        
    def start_clients(self, num_connections, bitrate):
        '''Starts up num_connection meter to socket commands and waits for them to finish before returning.  This will use the 
        global metered backup seconds to compute the buffer size allowed in the metered call by checking the bitrate and allowing for 
        a certain number of seconds of backlog.'''
        procs = []
        bufferSize = bitrate * METER_BUFFER_SECONDS / 8
        
        while len(procs) < num_connections:
            cmd = METER_CMD % (bitrate, bufferSize, self.data_file[len(procs) % len(self.data_file)])
            proc = SP.Popen(cmd, shell=True)
            procs.append(proc)
            
        # Now wait on the procs to finish.  Have to assume they will exit when done.
        for proc in procs:
            proc.wait()
            
    def add_structure(self, props, tree_struct):
        ts = re.split("-->", tree_struct)
        
        if ts[0] != "master" or ts[-1] != "identifier":
            raise Exception("what the hell dude?  Structure is bogus: " + tree_struct)
        
        base_ = "globallad.containers.childContainers"
        
        for index, deal in enumerate(ts):        
            if index == len(ts) - 1:
                break
            
            props.append("%s.%s=%s" % (base_, deal, ts[index+1]))
    
    def create_file_name(self, client_buffer_size, inserter_buffer_size, bitrate, num_connections, num_inserters, depth, tree_struct):
        return os.path.join(self.output_dir, file_name_fmt % (num_connections, num_inserters, depth, client_buffer_size, inserter_buffer_size, bitrate, len(tree_struct.split("-->"))))
        
    def update_properties(self, client_buffer_size, inserter_buffer_size, bitrate, num_connections, num_inserters, depth, tree_struct):
        '''Reads in the properties file and will update it with the values given and will save it to the CHILL directory.  Also 
        builds the name of the file.  Returns the created directory file name.'''
        fname = self.create_file_name(client_buffer_size, inserter_buffer_size, bitrate, num_connections, num_inserters, depth, tree_struct)
        props = ["globallad.enabled=true",
                 "globallad.test.plot.pdf.file=plots.pdf",
                 "globallad.test.plot.samplesize=%d" % self.sample_size,
                 "globallad.test.plot.threshold=%d" % self.threshold,
                 "globallad.disruptor.client.ringBufferSize=%d" % client_buffer_size, 
                 "globallad.disruptor.globallad.ringBufferSize=%d" % inserter_buffer_size,
                 "globallad.disruptor.globallad.inserters=%d" % num_inserters,
                 "globallad.containers.depth=%d" % depth,
                 "globallad.test.disruptor.insert.sample.dump.dir=%s" % fname, 
                 ""]
        
        self.add_structure(props, tree_struct)
        
        open(os.path.expanduser(properties_file), "w").write("\n".join(props))
        return fname
        

def parse_args():                                                                                                                                            
      parser = argparse.ArgumentParser("Run test script to collect insert statistics for the global lad.")
      parser.add_argument("-c", "--numConnections", action="store", dest="connections", required=True)
      parser.add_argument("-i", "--numInserters", action="store", dest="inserters", required=True)
      parser.add_argument("-d", "--depth", action="store", dest="depth", required=True, help="CSV of the depth values to use.")
      parser.add_argument("-l", "--clientRingBufferSize", action="store", dest="cbuffer", required=True, help="CSV of sizes of the ring buffer used to pass raw data from the client to the data constructor.")
      parser.add_argument("-k", "--inserterRingBufferSize", action="store", dest="ibuffer", required=True, help="CSV of sizes of the ring buffer to pass messages to the inserters to be inserted into the global lad.")
      parser.add_argument("-b", "--bitRate", action="store", dest="brate", required=True, help="CSV of bitrates for chill_meter_to_socket to send to the global lad.")
      parser.add_argument("-o", "--outputDir", action="store", dest="output_dir", required=True, help="Directory to store all of the results.")
      parser.add_argument("-f", "--dataFiles", action="store", dest="data", required=True, help="CSV of data file to be used to send to the global lad.  This should be backup or dump file from the global lad.  Multiple values can be given when using multiple connections in order to simulate real input clients.  In this case the data files should not be the same session.")
      parser.add_argument("-s", "--sampleCount", action="store", dest="scount", required=True, type=int, help="Sample count to set for the plot PDF of the glad server.")
      parser.add_argument("-p", "--plotThreshold", action="store", dest="threshold", required=True, type=int, help="Threshold for the plots blah.")
      parser.add_argument("-t", "--treeDepth", action="store", dest="tdepth", required=True, help="CSV of tree depth from 3 to the max which is 7.  At 3 map would be master-->userDataType-->identifier and at 7 the tree map would be master-->host-->scid-->venue-->sessionNumber-->vcid-->dssId-->userDataType-->identifier.")

      
      return parser.parse_args()
    
      
if __name__ == "__main__":
    args = parse_args()
    tester = GladTester(args)
    tester.run()

    