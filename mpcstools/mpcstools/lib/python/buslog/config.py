#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import os.path
import sys
import importlib
import xml.parsers.expat

import buslog
from buslog import err
import mpcsutil

CONFIG_NAME = "1553_config.xml"

class LogConfig(object):
    """
    This class represents a 1553 configuration file, which can be considered to be a dictionary.

    There is no official schema at this time.  Roughly, the file contains a mapping of RTs (remote terminals)
    values to labels for these RTs, as well as a mapping of SAs (sub addresses) values to labels.

    A specific decoder can be specified for a sub address of a particular set of RTs.
    """

    def __init__(self):

        self.remote_terminals = []
        self.value_to_rt_dict = {}
        # Dict where the keys are 2-tuples constructed by (RT, SA), and the values are decoder objects to invoke
        self.rt_sa_to_decoder_dict = {}
        self.broadcast_rt = -1

    def _startElement(self,name,attrs):

        if name == 'RT':

            rt = buslog.RemoteTerminal()
            rt.value = int(attrs['value'])
            rt.name = attrs['name']

            if rt.name == 'BROADCAST':
                self.broadcast_rt = rt.value

            self.remote_terminals.append(rt)
            self.value_to_rt_dict[rt.value] = rt

        elif name == 'SA':

            sa = buslog.SubAddress()
            sa.value = int(attrs['value'])
            sa.transmit_name = attrs['transmit_name']
            sa.receive_name = attrs['receive_name']

            for rt in self.current_rts:

                rt.subaddresses.append(sa)
                rt.value_to_sa_map[sa.value] = sa

        elif name == 'Subaddress':

            self.current_rts = [self.value_to_rt_dict[int(rt)] for rt in attrs['RTs'].split(',')]

        elif name == 'Decoder':
            # Attempt to load the decoder class for a list of subaddresses of a list of RTs.

            # Parse the RT and SA lists
            rts = [int(rt) for rt in attrs['RTs'].split(',')]
            sas = [int(sa) for sa in attrs['SAs'].split(',')]
            decoder_name = attrs['name']
            decoder_class = attrs['decoder']

            decoder = None
            if decoder_class in self.decoder_cache:
                decoder = self.decoder_cache[decoder_class]
            else:
                # Import the module that contains the decoder
                # The full qualified class name is expected (e.g., includes package and module)
                module = decoder_class[:decoder_class.rfind('.')]
                try:
                    _module = importlib.import_module(module)
                    sys.modules[module] = _module
                except ImportError:
                    pass

                #Load the decoder via reflection
                decoder = eval('%s()' % decoder_class)
                decoder.name = decoder_name

                #cache this so we don't instantiate it again later needlessly
                self.decoder_cache[decoder_class] = decoder

            for rt in rts:
                for sa in sas:
                    self.rt_sa_to_decoder_dict[(rt,sa)] = decoder

    def _endElement(self,name):

        if name == 'Subaddress':

            del self.current_rts

    def parse_config(self):
        """ Parse the 1553 config file and load decoders. """
        gds_config = mpcsutil.config.GdsConfig()
        # MPCS-6683  - 10/15/15: Config file now not only in the mission config directory.
        # Use standard config hierarchy.
        log1553_config = mpcsutil.filesystem.get_file(gds_config.getConfigDirs(), CONFIG_NAME)
        outFile = None
        try:
            parser = xml.parsers.expat.ParserCreate()
            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            self.decoder_cache = {}

            with open(log1553_config,'rb') as outFile:
                parser.ParseFile(outFile)

            del self.decoder_cache

        except xml.parsers.expat.ExpatError as e:
            raise err.LogParseException('Error parsing 1553 configuration properties from XML (row {}, col {}):\n{}'.format(e.lineno,e.offset,traceback.format_exc()))


    def dump_config(self):
        """ Print the loaded 1553 config to stdout. """
        print(['{}\n\t{}\n'.format(rt, '\n\t'.join(['{}'.format(sa) for sa in rt.subaddresses])) for rt in self.remote_terminals])



def test(*args, **kwargs): pass
def main(*args, **kwargs): return test(*args, **kwargs)
if __name__ == "__main__": main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
