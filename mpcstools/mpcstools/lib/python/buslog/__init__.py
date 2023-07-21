#! /usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import (absolute_import, division, print_function)

import os.path
import traceback
import mpcsutil
import json
import six
long=long if six.PY2 else int
try:
    import err, times, decoders
except (ImportError, ModuleNotFoundError):
    from buslog import (err, times, decoders)

try:
    from Cheetah.Template import Template
except (ImportError, ModuleNotFoundError):
    class Template(object):
        def __init__(self, *args, **kwargs): [self.__setattr__(kk,vv) for kk,vv in kwargs.items()]
        def __getattr__(self, name): return self.__dict__.get(name, None)
        def __str__(self):
            _self=self.searchList[0] if self.searchList else self
            _dd=_self.__dict__.copy()
            _dd.update({
                'data': '"{}"'.format(' '.join(_self.decode_data)) if _self.decode_data else '{}'.format(_self.decode_data),
                'sub_address': _self.sub_address.transmit_name if _self.is_transmit else _self.sub_address.receive_name,
                'sclk': '{:.5f}'.format(_self.sclk) if _self.sclk else '{}'.format(_self.sclk)
            })
            if _self.verbose:
                _dd.update({
                    'status_word': _self.status_word if (_self.is_broadcast or _self.is_no_response) else ('0x{:04x}'.format(_self.status_word) if _self.status_word else '{}'.format(_self.status_word)),
                    'word_count': '{:02d}'.format(_self.mode_code if (_self.is_broadcast and _self.mode_code) else '{}'.format(_self.word_count)),
                    'Err': '0x{:04x}'.format(_self.error_status) if _self.error_status else "''",
                    'dt': '{:05d}'.format(_self.message_gap) if _self.message_gap else "''",
                })
                return '{sys_time} {rti_number} {sclk} {bus} C=0x{command_word:04x} S={status_word} RT="{remote_terminal.name}" SA="{sub_address}" TR={transmit_receive_status} WC={word_count} Err={Err} dt={dt} Data={data}'.format(**_dd)
            else:
                return '{sys_time} {sclk} {bus} RT="{remote_terminal.name}" SA="{sub_address}" TR={transmit_receive_status} Data={data}'.format(**_dd)

ENCODED_TEMPLATE = 'default'

class LogEntryFilter(object):

    def __init__(self,props):

        try:
            self.rts = self._parse_numeric_list(props.rts)
        except Exception:
            raise err.LogParseException('Could not parse RT list from the command line:\n{}'.format(traceback.format_exc()) )

        try:
            self.sas = self._parse_numeric_list(props.sas)
        except Exception:
            raise err.LogParseException('Could not parse SA list from the command line:\n{}'.format(traceback.format_exc()) )

        try:
            self._set_sys_time_range(props.sys_time_start,props.sys_time_end)
        except Exception:
            raise err.LogParseException('Could not parse the input system time from the command line:\n{}'.fomrat(traceback.format_exc()) )

        try:
            self._set_sclk_range(props.sclk_start,props.sclk_end)
        except Exception:
            raise err.LogParseException('Could not parse the input SCLK from the command line:\n{}'.format(traceback.format_exc()) )

    def _parse_numeric_list(self,input):

        list = []
        if input is None:
            return list

        for item in input.split(','):

            #we got a list
            if '..' in item:

                minmax = item.split('..')
                min = int(minmax[0])
                max = int(minmax[1])

                for i in range(min,max+1):
                    list.append(i)

            #it's just a number
            else:

                list.append(int(item))

        #Make sure that everything in the list is a valid RT or SA
        for i in list:
            if i < 0 or i > 31:
                raise ValueError('Input value "%s" is outside the valid range 0-31.' % (i))

        return list

    def _set_sys_time_range(self,sys_time_start,sys_time_end):

        #Times are in SECONDS!
        self.sys_time_start = None
        self.sys_time_end = None

        if sys_time_start is not None:
            self.sys_time_start = times.parseTimeString(sys_time_start)

        if sys_time_end is not None:
            self.sys_time_end = times.parseTimeString(sys_time_end)

    def _set_sclk_range(self,sclk_start,sclk_end):

        #Times are SCLK as a float!
        self.sclk_start = None
        self.sclk_end = None

        if sclk_start is not None:
            self.sclk_start = times.parseSclkString(sclk_start)

        if sclk_end is not None:
            self.sclk_end = times.parseSclkString(sclk_end)

    def accept(self,entry):

        if entry.data_entry.filtered_out:
            return False

        if self.rts and not entry.rt_value in self.rts:
            return False

        if self.sas and not entry.sa_value in self.sas:
            return False

        if self.sys_time_start and entry.sys_time_seconds < self.sys_time_start:
            return False

        if self.sclk_start and entry.sclk < self.sclk_start:
            return False

        if self.sys_time_end and entry.sys_time_seconds > self.sys_time_end:
            raise err.EndOfTimeRange('Reached the end of the specified system time range')

        if self.sclk_end and entry.sclk > self.sclk_end:
            raise err.EndOfTimeRange('Reached the end of the specified SCLK range')

        return True

class LogEntry(object):

    def __init__(self):

        self.verbose = False

        self.sys_time_str = None
        self.rti_number = None
        self.sclk_microseconds = None
        self.bus = None
        self.command_word = None
        self.status_word = None
        self.rt_value = None
        self.sa_value = None
        self.transmit_receive_status = None
        self.mode_code = None
        self.word_count = None
        self.error_status = None
        self.message_gap = None
        self.data = []
        self.decode_data = []

        self.is_broadcast = False
        self.is_no_response = False

        self.remote_terminal = None
        self.sub_address = None
        self.sys_time_seconds = None
        self.sclk = None
        self.sys_time = None
        self.is_transmit = None

    def parse_entry(self,line,config):

        line_pieces = line.split()
        if len(line_pieces) < 13:
            raise err.LogParseException("Input line missing elements!")

        self.sys_time_str = line_pieces[0]
        self.rti_number = int(line_pieces[1])
        self.sclk_microseconds = long(line_pieces[2])
        self.bus = line_pieces[3][4:] #strip off the "Bus="
        self.command_word = int(line_pieces[4][4:],16) #strip off the "C=0x"
        self.rt_value = int(line_pieces[6][3:]) #strip off the "RT="

        self.status_word = line_pieces[5][2:] #strip off the "S="
        if self.rt_value == config.broadcast_rt or self.status_word == 'BRDCST':
            self.is_broadcast = True
        elif self.status_word == 'NORESP':
            self.is_no_response = True
        else:
            self.status_word = int(self.status_word,16)

        self.sa_value = int(line_pieces[7][3:]) #strip off the "SA="
        self.transmit_receive_status = line_pieces[8][3:] #strip off the "TR="
        self.is_transmit = self.transmit_receive_status == 'T'
        self.mode_code = None
        self.word_count = None
        if self.is_broadcast:
            self.mode_code = int(line_pieces[9][3:]) #strip off the "WC="
        else:
            self.word_count = int(line_pieces[9][3:]) #strip off the "WC="

        #self.error_status = int(line_pieces[10][6:],16) #strip off the "Err=0x"
        err = line_pieces[10][4:] #strip off the Err=
        self.error_status = int(err[2:],16) if err else '' #strip off the 0x if it's there

        #self.message_gap = int(line_pieces[11][3:]) #strip off the "dt="
        gap = line_pieces[11][3:] #strip off the dt=
        self.message_gap = int(gap) if gap else ''

        #Make sure to turn all the data into integers also
        if len(line_pieces) > 13:
            self.data = [int(item,16) for item in line_pieces[13:]] #item 12 is the string "Data="
        else:
            self.data = []

        #convert rt into an RT object
        self.remote_terminal = config.value_to_rt_dict[self.rt_value]
        #convert sub_address into an SA object
        self.sub_address = self.remote_terminal.value_to_sa_map[self.sa_value]
        #convert sys_time into seconds since epoch
        self.sys_time_seconds = times.parse1553String(self.sys_time_str)
        #convert sclk_microseconds into normal decimal SCLK
        self.sclk = float(self.sclk_microseconds)/float(10**6)

        self.sys_time = mpcsutil.timeutil.getIsoTime(self.sys_time_seconds, include_subseconds=False)

        data_decoder = decoders.GenericDecoder()
        if (self.rt_value,self.sa_value) in config.rt_sa_to_decoder_dict:
            data_decoder = config.rt_sa_to_decoder_dict[(self.rt_value,self.sa_value)]

        self.data_entry = data_decoder.decode(self.remote_terminal,self.sub_address,self.data,verbose=self.verbose)
        if not self.is_no_response:
            self.decode_data = self.data_entry.translated_values if not self.data_entry.filtered_out else []
        else:
            self.decode_data = []
            for item in self.data:
                self.decode_data.append('%04x' % (item))

    def decode_str(self,template,verbose=False):

        return str(Template(file=template,searchList=[self]))

    def encode_str(self,template,verbose=False):

        return str(Template(file=template,searchList=[self]))

    def __str__(self):

        result = '%s %s %s Bus=%s C=0x%04x ' % (self.sys_time_str,self.rti_number,self.sclk_microseconds,self.bus,self.command_word)

        result += '%s ' % (self.status_word) if self.is_broadcast else ('0x%04x ' % (self.status_word) if self.status_word else '{}'.format(self.status_word))

        result += 'RT=%02d SA=%02d TR=%s WC=%02d Err=0x%04x dt=%05d Data=' % (self.rt_value,self.sa_value,self.transmit_receive_status,
                                              self.mode_code if self.is_broadcast else self.word_count,
                                              self.error_status,self.message_gap)

        for item in self.decode_data:
            result += ' %s' % (item)

        return result

    def __repr__(self):

        return self.__str__()

class RemoteTerminal(object):

    def __init__(self):

        self.value = 0
        self.name = "Spare"
        self.subaddresses = []
        self.value_to_sa_map = {}

    def __str__(self):

        return 'Remote Terminal %02d (%s)' % (self.value,self.name)

    def __repr__(self):

        return self.__str__()

class SubAddress(object):

    def __init__(self):

        self.value = 0
        self.transmit_name = "Spare"
        self.receive_name = "Spare"

    def __str__(self):

        return 'SubAddress %02d (Transmit=%s,Receive=%s)' % (self.value,self.transmit_name,self.receive_name)

    def __repr__(self):

        return self.__str__()

try:
    import config
except (ImportError, ModuleNotFoundError):
    from buslog import config
__all__ = ['err','config','times']


def test(*args, **kwargs): pass
def main(*args, **kwargs): return test(*args, **kwargs)
if __name__ == "__main__": main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'


'''
20090224T165930 58 288766778906262 Bus=edlrc1-A C=0xf811 S=BRDCST RT=31 SA=00 TR=R WC=17 Err=0x8001 dt=65535 Data= 25c3

===20090224T165930     is the system time and date - this one was created on 02/24/2009 at 16:59:30 UTC -
This time is the local time on the SSE machine which captured the bus log.

===58     is the RTI number. On the 64 Hz EDL bus this goes from 0 to 63 and on the 8 Hz Rover bus it goes from 0 to 7

===288766778906262    is the SCLK time in microseconds

===Bus=edlrc1-A     is which bus the message was on. This was on the EDL 64 Hz bus on the A side of the 1553 bus.
Bus=rover-B would be on the B side of the 1553 on the rover 8 Hz bus. Note: Normally everything should be on the A
side of the 1553 bus. If you see a message that is on the B side (edlrc1-B) then something has failed on the A side bus!

===C=0xf811    is the command word. It comes from the Bus Controller and tells the RT what it wants to happen.
If you're curious about the bit packing see Figure 4-8 of the Intercomm FDD. Fortunately for us, the bus monitor
breaks out all the information in the control word in a human readable way as we'll see below.

===S=BRDCST    is the status word. It is the response from the Remote Terminal to the Bus Controller with its status.
S=BRDCST is a special case since this is a broadcast command to all remote terminals (SA=31) so no status is expected.
The bus monitor puts BRDCST in the space instead. Once again, the contents of the word are shown in Figure 4-8 of the
Intercomm FDD. For the common user, the most important thing to look for is S=NORESP which means that no response was received from the receiving RT.
If this is seen then the RT might be off, inhibited, not present or broken.

===RT=31    is the address of the receiving Remote Terminal. In this case it is 31 which is a broadcast command.
See the table in below for all the addresses. This field tells us which assembly the command was addressed to.

===SA=00     is the sub-address that the command is targeting. Different sub addresses do different things for
different remote terminals. See the PAM Sub Addresses section below for details on the various sub addresses in the PAM.

===TR=R     is the Transmit/Receive status from the perspective of the Remote Terminal. If this is R then the bus
controller is sending data for the Remote Terminal to receive. If this is T then the bus controller is asking the
Remote Terminal to send information to the bus controller.

===WC=17     is the word count if the command is to a single RT (RT=0 through RT=30) or the Mode Code if it is a
broadcast command (RT=31). The transaction in line 1 is a broadcast command so this message is Mode Code 17. See table
4-69 of the Intercomm FDD for the list of Mode Codes and what they do. In the case of a request, like line 2 in the example
above (where RT=12) there WC=20 there are 20 words requested.

===Err=0x8001    is the error status. If you have questions about this one talk to Steve Schroeder. :)

===dt=65535    is the inter-message gap between this message and the previous message. It is given in 1x10-7 seconds
so dt=00073 is 7.3 us. The counter maxes out at 65535 so there was a large gap between this message and the previous one.

===Data=    is the actual data being exchanged. The number of 16 bit data words is the same as giving in the WC category for
all but broadcast commands. For broadcast commands it is one word whose meaning depends on the mode code. If the trasmit status
is TR=R then these data words are being sent from the bus controller to the Remote Terminal. If the transmit status is TR=T then
 these data words are being sent from the remote terminal to the bus controller.
'''
