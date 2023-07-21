#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines a common object to represent an
EVR that can be used across MPCS missions.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil
import random
import six

long = int if six.PY3 else long
# _log = logging.getLogger('mpcs.util')
gds_config = mpcsutil.config.GdsConfig()

# levels = gds_config.getEvrLevelList()

# level_to_value_map = {}
level_to_value_map = {_level:int(mpcsutil.config.GdsConfig().getProperty('evr.plot.ordinal.{}'.format(_level), -1)) for _level in gds_config.getEvrLevelList()}
value_to_level_map = {vv:kk for kk,vv in level_to_value_map.items()}
# value_to_level_map = {}
# for level in levels:
#     property = 'evr.plot.ordinal.%s' % (level)
#     value = int(mpcsutil.config.GdsConfig().getProperty(property, -1))
#     level_to_value_map[level] = value
#     value_to_level_map[value] = level

class Evr(mpcsutil.TelemetryItem):
    '''An Evr represents a single event report.

        Object Attributes
        ------------------
        name - The name of the EVR (string)
        eventId - The EVR event ID (int)
        level - The level of the EVR (string)
        fromSse - True if the EVR is from the SSE, false otherwise (Boolean)
        eventTime - The ISO-formatted time at which MPCS generated the EVR message for this EVR (string)
        ert - The ISO-formatted earth receive time for this EVR (string)
        sclk - The Spacecraft Clock Time for this EVR in the form CCCCCCCCCC-FFFFF(string)
        scet - The ISO-formatted spacecraft event time for this EVR (string)
        scetExact - Milliseconds since epoch for the SCET timestring (int)
        scetNano - Nanoseconds for the SCET timestring (int)
        lst - The Local Solar Time value in SOL-XXXXMHH:MM:SS.sss format
        realtime - True if this is a realtime EVR, false otherwise (Boolean)
        message - The message associated with this EVR (string)
        metadata - A list of tuples containing associated pairs of metadata keywords and values ( type is [(string,string)] )
        module - The FSW module that generated this EVR (string)
        dssId - The receiving station Id for this EVR (int)
        vcid - The VCID number on which the EVR was received (int)
        vcidMapping - The VCID on which the EVR was received, or the project specific string mapped to the VCID (string)

        Dynamic Attributes
        -------------------
        All of an EVR's available metadata can be found through the "metadata" attribute on the object, but in addition, all
        metadata values are actually appended to the object itself.  Example:

        >>> e = get_evr(name='FBM_EVR_MONITOR_ANNOUNCE')[0]
        >>> print e.metadata
        [('TaskName', 'fbm???'), ('SequenceId', 'RT:1286'), ('CategorySequenceId', 3']
        >>> print e.TaskName
        fbm???
        >>> print e.SequenceId
        RT:1286
        >>> print e.CategorySequenceId
        3'''

    '''
    THIS CLASS IS CONTROLLED BY THE AUTO SIS (D-001008_MM_AUTO_SIS)
    Any modifications to this class must be reflected in the SIS
    '''
    #Item on the left is an alias for item on the right
    #(This works for setting and getting of attributes)
    _ALIASES = { }

    def __init__(self, csvString=None, **kwargs):
        ''' Initializer can take a csv string and construct an EVR object,
        or it can take an arbitrary set of key value arguments which will be set on
        the object (assuming the entries are already attributes of this object)

        Args
        -----
        csvString - A comma separated value string representing an EVR.  The order of this string is:

                    evr\,$name\,$level\,$event\,$message\,$fromSse\,$eventTime\,$realtime\,$sclk\,$scet\,$lst\,$ert\,$dssId\,$vcid\,$vcidMapping\,$module\,$metadataKey\,metadataValue (metadata keys values can repeat)
                    (Defined by templates/common/message/EVR/Mtak.vm)

        kwargs - Any keyword arguments whose names match the names of attributes on this object'''

        mpcsutil.TelemetryItem.__init__(self)

        self.clear()

        if csvString is not None:
            self.parseFromCsv(csvString)

        for key in kwargs.keys():
            if hasattr(self, key):
                if key == 'eventId' or key == 'scetNano' or key == 'scetExact':
                    setattr(self, key, int(kwargs[key]))
                elif key == 'fromSse' or key == 'realtime':
                    setattr(self, key, bool(kwargs[key]))
                elif key == 'eventTimeExact' or key == 'sclkCoarse' or key == 'sclkFine' or \
                     key == 'lstExact' or key == 'ertExact' or key == 'ertExactFine' or key == 'sclkExact':
                    setattr(self, key, long(kwargs[key]))
                elif key == 'metadata':
                    setattr(self, key, kwargs[key])
                    self._parse_metadata(kwargs[key])
                else:
                    setattr(self, key, kwargs[key])
        idx = getattr(self, 'scet', -1).find(".")
        if getattr(self, 'scetExact') is None or getattr(self, 'scetExact') == 0 and idx > 0:
            setattr(self, 'scetExact', int(mpcsutil.timeutil.parseTimeString(getattr(self, 'scet'))))
        if getattr(self, 'scetNano') is None or getattr(self, 'scetNano') == 0 and idx > 0 and \
                        mpcsutil.timeutil.getScetPrecision() > 3 and len(getattr(self, 'scet').split(".")[1]) > 3:
            setattr(self, 'scetNano', int(getattr(self, 'scet')[idx+3:]))

    def _parse_metadata(self, metaDataList):
        ''' Parses the metadata list of tuples, and forms object attributes for each type of metadata

        Args
        -----
        metaDataList - A list of tuples containing associated pairs of metadata keywords and values ( type is [(string,string)] )
        '''

        new_metadata_list = []
        for metaPair in metaDataList:

            name = ''
            temp_name = metaPair[0]
            #if temp_name[0].isupper():
            #    name = temp_name[0].lower()
            #    if len(temp_name) > 1:
            #        name += temp_name[1:]
            #else:
            #    name = temp_name
            name = temp_name

            value = str(metaPair[1])

            if value.isdigit(): #if value is a number, change it to an int (doesn't work for floats)
                value = int(value)

            setattr(self, name, value)
            new_metadata_list.append((name, value))

        self.metadata = new_metadata_list

    def parseFromCsv(self, csvString):
        ''' Parses the Csv string and construct the Evr.   The CSV string
        must be well formed or an exception may be generated.

        Args
        -----
        csvString - A comma separated value string representing an EVR.  The order of this string is:

                    evr\,$name\,$level\,$event\,$message\,$fromSse\,$eventTime\,$realtime\,$sclk\,$scet\,$scetExact\,$scetNano\,$lst\,$ert\,$dssId\,$vcid\,$module\,$metadataKey\,metadataValue (metadata keys values can repeat)
                    (Defined by templates/common/message/EVR/Mtak.vm)

        Returns
        --------
        None'''


        splitString = csvString.split('\,')
        self.name = splitString[1]
        self.level = splitString[2]
        if splitString[3]:
            self.eventId = int(splitString[3])
        self.message = splitString[4]
        self.fromSse = mpcsutil.getBooleanFromString(splitString[5])
        self.eventTime = splitString[6]
        if splitString[7]:
            self.eventTimeExact = long(splitString[7])
        self.realtime = mpcsutil.getBooleanFromString(splitString[8])
        self.sclk = splitString[9]
        if splitString[10]:
            self.sclkCoarse = long(splitString[10])
        if splitString[11]:
            self.sclkFine = long(splitString[11])
        if splitString[12]:
            self.sclkExact = long(splitString[12])
        self.scet = splitString[13]
        if splitString[14]:
            self.scetExact = int(splitString[14])
            if self.scetExact == 0:
                self.scetExact = int(mpcsutil.timeutil.parseTimeString(self.scet))
        if (splitString[15]):
            self.scetNano = int(splitString[15])
        self.lst = splitString[16]
        if splitString[17]:
            self.lstExact = long(splitString[17])
        self.ert = splitString[18]
        if splitString[19]:
            self.ertExact = long(splitString[19])
        if splitString[20]:
            self.ertExactFine = long(splitString[20])
        if splitString[21].strip() != '':
            self.dssId = int(splitString[21].strip())
        else:
            self.dssId = None
        self.vcid = ''
        if splitString[22]:
            self.vcid = splitString[22]

        self.vcidMapping = gds_config.getVcidMapping(self.vcid)

        self.module = splitString[23]
        self.metadata = []
        i = 24
        while i + 1 < splitString.__len__():
            self.metadata.append((splitString[i], splitString[i + 1]))
            i += 2

        if self.metadata:
            self._parse_metadata(self.metadata)

    @staticmethod
    def get_value_from_level(level):

        property = 'evr.plot.ordinal.%s' % (level)
        value = int(gds_config.getProperty(property, 0))
        return value


    @staticmethod
    def get_from_database_csv(csv):

        global gds_config

        add_noise = False
        evr = Evr()

        pieces = csv[1:-2].split('","')

        #recordType,sessionId,sessionHost,name,module,level,eventId,vcid,dssId,fromSse,realtime,sclk,scet,lst,ert,rct,message,metadataKeywordList,metadataValuesList

        evr.name = pieces[3]
        evr.module = pieces[4]
        evr.level = pieces[5]

        if evr.level not in level_to_value_map:
            evr.level = 'EVR_UNKNOWN'

        #TODO: Add a little offset noise to the Y value?  As much as .25 above or below?
        evr.level_value = Evr.get_value_from_level(evr.level)
        if add_noise:
            evr.level_value += ((random.random() - 0.5) * 10)
        evr.dn = evr.level_value #TODO: Complete hack to try to get this working

        evr.eventId = pieces[6]
        if evr.eventId:
            evr.eventId = int(evr.eventId)
        evr.fromSse = mpcsutil.getBooleanFromString(pieces[9])
        evr.realtime = mpcsutil.getBooleanFromString(pieces[10])
        evr.sclk = pieces[11]
        evr.scet = pieces[12]
        evr.scetExact = int(mpcsutil.timeutil.parseTimeString(evr.scet))
        evr.scetNano = mpcsutil.timeutil.getTimeStringNanos(evr.scet)
        evr.lst = pieces[13]
        evr.ert = pieces[14]
        # pieces[15] is rct
        evr.message = pieces[16]

        #ignore metadata for now

        #TODO: Ignore EVRs that start with "EVR processing error" in the message?

        #Keywords and values look like this: [(TaskName),(SequenceId),(CategorySequenceId)]
        #
        #So [2:-2] strips the leading '[(' and the trailing ')]'
        #and then split('),(') will end up giving us the list ['TaskName','SequenceId','CategorySequenceId']
        #keywords = pieces[17][2:-2].split('),(')
        #values = pieces[18][2:-2].split('),(')

        evr.dssId = pieces[8]
        evr.vcidMapping = pieces[7]

        return evr

    def toCsv(self):
        ''' Returns a CSV representation of the Evr.   This Csv representation
        can be used to construct a new EVR

        Args
        -----
        None

        Returns
        --------
        A CSV-formatted string representing this object (string)'''


        returnStr = "evr\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s" % \
                  (str(self.name), str(self.level), str(self.eventId),
                   str(self.message), str(self.fromSse), str(self.eventTime),
                   str(self.eventTimeExact), str(self.realtime), str(self.sclk),
                   str(self.sclkCoarse), str(self.sclkFine), str(self.sclkExact),
                   str(self.scet), str(self.scetExact), str(self.scetNano), str(self.lst),
                   str(self.lstExact), str(self.ert), str(self.ertExact),
                   str(self.ertExactFine), str(self.dssId), str(self.vcid), str(self.module))

        for data in self.metadata:
            returnStr += "\,%s\,%s" % (str(data[0]), str(data[1]))
        return returnStr

    def __getitem__(self, item):
        return self.__getattribute__(item)

    def __setitem__(self, key, value):
        self.__setattr__(key, value)

    def clear(self):
        ''' Internal method that clears the values of all the attributes of this Evr.

        Args
        -----
        None

        Returns
        --------
        None'''


        self.name = ''
        self.receiveTime = 0
        self.eventId = 0
        self.module = ''
        self.level = ''
        self.fromSse = False
        self.eventTime = ''
        self.eventTimeExact = 0
        self.ert = ''
        self.ertExact = 0
        self.ertExactFine = 0
        self.sclk = ''
        self.sclkCoarse = 0
        self.sclkFine = 0
        self.sclkExact = 0
        self.scet = ''
        self.scetExact = 0
        self.scetNano = 0
        self.lst = ''
        self.lstExact = 0
        self.realtime = False
        self.message = ''
        self.metadata = []
        self.dssId = 0
        self.vcid = ''
        self.vcidMapping = ''
        self.injected = False

    def get_plot_label(self):

        return self.name

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        return  ('mpcsutil.evr.Evr(name="%s",level="%s",eventId=%s,'\
                'message="%s",fromSse=%s,eventTime="%s",eventTimeExact=%s,'\
                'realtime=%s,sclk="%s",sclkCoarse=%s,sclkFine=%s,sclkExact=%s,'\
                'scet="%s",scetExact=%s,scetNano=%s,lst="%s",lstExact=%s,ert="%s",'\
                'ertExact=%s,ertExactFine=%s,module="%s",metadata=%s,'\
                'injected=%s,dssId=%s,vcid="%s",vcidMapping="%s")') % \
                 (self.name, self.level, self.eventId, self.message, self.fromSse,
                  self.eventTime, self.eventTimeExact, self.realtime, self.sclk,
                  self.sclkCoarse, self.sclkFine, self.sclkExact, self.scet,
                  self.scetExact, self.scetNano, self.lst, self.lstExact, self.ert, self.ertExact,
                  self.ertExactFine, self.module, repr(self.metadata),
                  self.injected, self.dssId, self.vcid, self.vcidMapping)

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        return self.__repr__()

    def __eq__(self, other):
        '''x.__eq__(y) <==> x == y'''

        if other == None:
            return False

        try:
            return    self.name == other.name and\
                      self.level == other.level and \
                      self.eventId == other.eventId and \
                      self.message == other.message and \
                      self.fromSse == other.fromSse and \
                      self.realtime == other.realtime and \
                      self.sclkExact == other.sclkExact and\
                      self.scetExact == other.scetExact and \
                      self.scetNano == other.scetNano and \
                      self.ertExact == other.ertExact and \
                      self.ertExactFine == other.ertExactFine and \
                      self.module == other.module and \
                      self.metadata == other.metadata and \
                      self.dssId == other.dssId and \
                      self.vcid == other.vcid and \
                      self.vcidMapping == other.vcidMapping
        except AttributeError:
            return False

    def __ne__(self, other):
        '''x.__eq__(y) <==> x != y'''

        return self.__eq__(other) == False

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
