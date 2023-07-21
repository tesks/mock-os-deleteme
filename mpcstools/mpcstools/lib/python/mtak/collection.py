#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines collection objects necessary to the operation of
MTAK, but that are generic enough to be used for anything.  All of these
objects are defined to be threadsafe and are mostly used by the downlink
proxy to store telemetry.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil
from six import StringIO
import threading
from contextlib import contextmanager

_log = lambda : logging.getLogger('mpcs.mtak')

class ChannelValueTable(dict):
    '''A lookup table of Channel values implemented as a thread-safe dictionary.  Channel values
    are indexed by channel ID.'''

    def __init__(self):
        '''Initialize this channel value

        Args
        -----
        None

        Returns
        --------
        None'''

        _log().debug('mtak.collection.ChannelValueTable()')

        dict.__init__(self)

        self.name_to_id_map = {}
        self.lock = threading.Lock()

    def getIds(self):
        '''Returns a list of Channel Id's (used as keys) from the Table

        Args
        -----
        None

        Returns
        --------
        A list of all the channel IDs for which at least one value has been received (list of strings)'''

        _log().debug('mtak.collection.ChannelValueTable.getIds()')

        return self.keys()

    getChannelIds = getIds

    def getById(self,id):
        '''Returns the ChanVal with the specified id

        Args
        -----
        id - The channel ID of the channel to be returned (case-sensitive) (string)

        Returns
        --------
        The channel value object corresponding to the latest value for the input ID
        or None if there isn't a value (mpcsutil.channel.ChannelValue)'''

        _log().debug('mtak.collection.ChannelValueTable.getById()')

        return self.get(id,[])

    getByChannelId = getById

    def getNames(self):
        '''Returns a list of unique channel names from the Table

        Args
        -----
        None

        Returns
        --------
        A list of all the channel names for which at least one value has been received (list of strings)'''

        _log().debug('mtak.collection.ChannelValueTable.getNames()')

        return self.name_to_id_map.keys()

    getChannelNames = getNames

    def getByName(self,name):
        '''Return a channel value for the channel with the given name.  Names should be unique.

        Args
        -----
        name - The name of the channel to be returned (case-sensitive) (string)

        Returns
        --------
        The channel value object corresponding to the latest value for the input name or None
        if there isn't a value (mpcsutil.channel.ChanVal)'''

        _log().debug('mtak.collection.ChannelValueTable.getByName()')

        strName = str(name)

        id = self.name_to_id_map.get(strName,None)

        result = self.get(id,[])

        return result

    getByChannelName = getByName

    def getByType(self,chanType):
        '''Returns a list of all channel values with the specified type.

        Args
        -----
        chanType - Return all channel values of a specified type (e.g. boolean)

        Returns
        --------
        A list of channel value objects that have the input type (list of mpcsutil.channel.ChannelValue)'''

        _log().debug('mtak.collection.ChannelValueTable.getByType()')

        strChanType = str(chanType)

        results = []
        for chan_list in self.values():
            if chan_list and chan_list[0].type == strChanType:
                results.extend(chan_list)

        #results = [chanval for chanval in self.values() if chanval.type == strChanType]

        return results

    getByChannelType = getByType

    def getByErtRange(self,start=mpcsutil.timeutil.minErtTime,end=mpcsutil.timeutil.maxErtTime):
        '''Returns a list of all Channel values in the specified ERT range.   Takes a ISO-formatted UTC time for start time and/or end time.

        Args
        -----
        start - The start time of the ERT time range (string)
        end - The end time of the ERT time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Channel value objects whose ERT falls within the input ERT range (list of mpcsutil.channel.ChannelValue)'''

        _log().debug('mtak.collection.ChannelValueTable.getByErtRange()')

        return compareByTimes(self.values(), 'ertExact', None , \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)))


    def getByScetRange(self,start=mpcsutil.timeutil.minScetTime,end=mpcsutil.timeutil.maxScetTime):
        '''Returns a list of all Channel values in the specified SCET range.   Takes a ISO-formatted UTC time for start time and/or end time.

        Args
        -----
        start - The start time of the SCET time range (string)
        end - The end time of the SCET time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Channel value objects whose SCET falls within the input SCET range (list of mpcsutil.channel.ChannelValue)'''

        _log().debug('mtak.collection.ChannelValueTable.getByScetRange()')

        return compareByTimes(self.values(), 'scetExact', 'scetNano', \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(start)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(end)))


    def getBySclkRange(self,start=mpcsutil.timeutil.minSclkTime,end=mpcsutil.timeutil.maxSclkTime):
        '''Returns a list of all Channel Values in the specified Sclk range.

        Args
        -----
        start - The start time of the SCLK time range (string)
        end - The end time of the SCLK time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Channel Value objects whose SCLK falls within the input SCLK range (list of mpcsutil.channel.ChannelValue)'''

        _log().debug('mtak.collection.ChannelValueTable.getBySclkRange()')

        return compareByTimes(self.values(), 'sclkExact', None, \
                              mpcsutil.timeutil.parseSclkString(str(start)), \
                              mpcsutil.timeutil.parseSclkString(str(end)))
        return results

    #########################################
    #The following functions are overridden to maintain
    #the contents of the name_to_id_map that maps channel names
    #to channel IDs
    #########################################
    def __setitem__(self, key, item):
        '''x.__setitem__(i, y) <==> x[i]=y'''

        dict.__setitem__(self,key,item)
        if item:
            chanval = item[0]
            if hasattr(chanval,'name') and not chanval.name in self.name_to_id_map:
                self.name_to_id_map[chanval.name] = key

    def __delitem__(self, key):
        '''x.__delitem__(y) <==> del x[y]'''

        item = self.get(key,None)
        if item:
            chanval = item[0]
            if hasattr(chanval,'name') and chanval.name in self.name_to_id_map:
                del self.name_to_id_map[chanval.name]

        dict.__delitem__(self,key)
#    def __setitem__(self, key, item):
#        '''x.__setitem__(i, y) <==> x[i]=y'''
#
#        dict.__setitem__(self,key,item)
#        if item is not None and hasattr(item,'name') and not item.name in self.name_to_id_map:
#            self.name_to_id_map[item.name] = key
#
#    def __delitem__(self, key):
#        '''x.__delitem__(y) <==> del x[y]'''
#
#        item = self.get(key,None)
#        if item is not None and hasattr(item,'name') and item.name in self.name_to_id_map:
#            del self.name_to_id_map[item.name]
#        dict.__delitem__(self,key)

class EvrTable(dict):
    '''A lookup table of EVRs implemented as a thread-safe dictionary. The keys to the dictionary
    are the EVR ID and the values of the table are lists of EVR objects.'''

    def __init__(self):
        '''Initialize this EVR table.

        Args
        -----
        None

        Returns
        --------
        None'''

        _log().debug('mtak.collection.EvrTable()')

        dict.__init__(self)

        self.name_to_id_map = {}
        self.lock = threading.Lock()

    def getIds(self):
        '''Returns a list of unique EVR event IDs (used as keys) from the Table

        Args
        -----
        None

        Returns
        --------
        A list of all the EVR event IDs for which at least one value has been received (list of strings)'''

        _log().debug('mtak.collection.EvrTable.getEventIds()')

        return self.keys()

    getEventIds = getIds

    def getNames(self):
        '''Returns a list of unique EVR names (used as keys) from the Table

        Args
        -----
        None

        Returns
        --------
        A list of all the EVR names for which at least one value has been received (list of strings)'''

        _log().debug('mtak.collection.EvrTable.getNames()')

        return self.name_to_id_map.keys()

    getEvrNames = getNames

    def getLevelSummary(self):
        '''Get a summary of all the EVRs in the table summarized by level.

        Args
        -----
        None

        Returns
        --------
        A dictionary whose keys are EVR levels (string) and whose values are
        counts of the number of that type of EVR (int)'''

        _log().debug('mtak.collection.EvrTable.getLevelSummary()')

        summary = {}
        for evrList in self.values():
            for evr in evrList:
                level = evr.level
                if level in summary:
                    summary[level] += 1
                else:
                    summary[level] = 1

        return summary

    def getModuleSummary(self):
        '''Get a summary of all the EVRs in the table summarized by module.

        Args
        -----
        None

        Returns
        --------
        A dictionary whose keys are FSW modules (string) and whose values are
        counts of the number of that type of EVR (int)'''

        _log().debug('mtak.collection.EvrTable.getModuleSummary()')

        summary = {}
        for evrList in self.values():
            for evr in evrList:
                module = evr.module
                if module in summary:
                    summary[module] += 1
                else:
                    summary[module] = 1

        return summary

    def getByName(self,name):
        '''Returns a list of all evrs with the given name

        Args
        -----
        name - The name of the EVR(s) to retrieve (string)

        Returns
        --------
        A list of EVR objects, all of whom have a name equal to the input name (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getByName()')

        strName = str(name)

        if strName in self.name_to_id_map:
            id = self.name_to_id_map.get(strName)
            return self.getByEventId(id)

        return []

    getByEvrName = getByName

    def getByLevel(self,level):
        '''Returns a list of all evrs matching the specified level

        Args
        -----
        level - The level of all EVRs to retrieve (string)

        Returns
        --------
        A list of EVR objects, all of whom have a level equal to the input level (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getByLevel()')

        strLevel = str(level)

        results = []
        for evrList in self.values():
            if evrList and evrList[0].level == strLevel:
                results.extend(evrList)

        return results

    def getById(self,eventId):
        '''Returns a list of all evrs matching the specified event ID

        Args
        -----
        eventId - The event ID of all EVRs to retrieve (string or int)

        Returns
        --------
        A list of EVR objects, all of whom have an event ID equal to the input event ID (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getByEventId()')

        intId = int(eventId)
        return self.get(intId,[])

    getByEventId = getById

    def getByModule(self,module):
        '''Returns a list of all evrs matching the specified module

        Args
        -----
        module - The module of all EVRs to retrieve (string)

        Returns
        --------
        A list of EVR objects, all of whom have a module equal to the input module (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getByModule()')

        strModule = str(module)

        results = []
        for evrList in self.values():
            if evrList and evrList[0].module == strModule:
                results.extend(evrList)

        return results

    getByFswModule = getByModule

    def getFromSse(self):
        '''Returns a list of all evrs from the SSE

        Returns
        --------
        A list of EVR objects from the SSE (list of mpcsutil.evr.Evr) or an empty list if
        the mission does not have an SSE.'''

        _log().debug('mtak.collection.EvrTable.getFromSse()')

        results = []
        for evrList in self.values():
            if evrList and getattr(evrList[0],'fromSse',None):
                results.extend(evrList)

        return results

    def getFromFsw(self):
        '''Returns a list of all evrs from the FSW

        Returns
        --------
        A list of EVR objects from the FSW (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getFromFsw()')

        results = []
        for evrList in self.values():
            if evrList and not getattr(evrList[0],'fromSse',None):
                results.extend(evrList)

        return results

    def getRealtime(self):
        '''Returns a list of all Realtime EVRs

        Returns
        --------
        A list of EVR objects whose "realtime" attribute is True (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getRealtime()')

        results = []
        for evrList in self.values():
            if evrList and getattr(evrList[0],'realtime',None):
                results.extend(evrList)

        return results

    def getRecorded(self):
        '''Returns a list of all Recorded EVRs

        Returns
        --------
        A list of EVR objects whose "recorded" attribute is True (list of mpcsutil.evr.Evr)'''

        _log().debug('mtak.collection.EvrTable.getRecorded()')

        results = []
        for evrList in self.values():
            if evrList and not getattr(evrList[0],'realtime',True):
                results.extend(evrList)

        return results


    def getByErtRange(self,start=mpcsutil.timeutil.minErtTime,end=mpcsutil.timeutil.maxErtTime):
        '''Returns a list of all EVRs in the specified ERT range.

        Args
        -----
        start - The start time of the ERTtime range (string)
        end - The end time of the ERT time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of EVR objects whose ERT falls within the input ERT range'''

        _log().debug('mtak.collection.EvrTable.getByErtRange()')

        return compareByTimes(self.values(), 'ertExact', None, \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)))


    def getByScetRange(self,start=mpcsutil.timeutil.minScetTime,end=mpcsutil.timeutil.maxScetTime):
        '''Returns a list of all EVRs in the specified SCET range.

        Args
        -----
        start - The start time of the SCET time range (string)
        end - The end time of the SCET time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of EVR objects whose SCET falls within the input SCET range'''

        _log().debug('mtak.collection.EvrTable.getByScetRange()')

        return compareByTimes(self.values(), 'scetExact', 'scetNano', \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(start)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(end)))

    def getBySclkRange(self,start=mpcsutil.timeutil.minSclkTime,end=mpcsutil.timeutil.maxSclkTime):
        '''Returns a list of all EVRs in the specified Sclk range.

        Args
        -----
        start - The start time of the SCLK time range (string)
        end - The end time of the SCLK time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of EVR objects whose SCLK falls within the input SCLK range'''

        _log().debug('mtak.collection.EvrTable.getBySclkRange()')

        return compareByTimes(self.values(), 'sclkExact', None, \
                              mpcsutil.timeutil.parseSclkString(str(start)), \
                              mpcsutil.timeutil.parseSclkString(str(end)))

    def count(self):
        '''Return the number of EVRs in this table.

        Returns
        --------
        The number of EVRs in this table.'''

        count = 0
        for evrList in self.values():
            count += len(evrList)
        return count

    def to_list(self):

        evrs = []
        for evrList in self.values():
            evrs.extend(evrList)
        return evrs

    def getByAttributes(self,name=None,eventId=None,level=None,module=None,ertStart=None,ertEnd=None,
                        scetStart=None,scetEnd=None,sclkStart=None,sclkEnd=None,message=None,messageSubstr=None):

        if name:
            name = str(name)

        if eventId:
            eventId = int(eventId)

        if level:
            level = str(level)

        if module:
            module = str(module)

        ertExactStartVal = None
        if ertStart is not None:
            ertExactStartVal = mpcsutil.timeutil.parseTimeString(ertStart)

        ertExactEndVal = None
        if ertEnd is not None:
            ertExactEndVal = mpcsutil.timeutil.parseTimeString(ertEnd)

        scetExactStartVal = None
        if scetStart is not None:
            scetExactStartVal = mpcsutil.timeutil.parseTimeString(scetStart)

        scetExactEndVal = None
        if scetEnd is not None:
            scetExactEndVal = mpcsutil.timeutil.parseTimeString(scetEnd)

        sclkExactStartVal = None
        if sclkStart is not None:
            sclkExactStartVal = mpcsutil.timeutil.parseSclkString(sclkStart)

        sclkExactEndVal = None
        if sclkEnd is not None:
            sclkExactEndVal = mpcsutil.timeutil.parseSclkString(sclkEnd)

        if message:
            message = str(message)

        if messageSubstr:
            messageSubstr = str(messageSubstr)

        evrs = []

        ###################################
        #Try our best to look up by
        #name or event ID first to limit the amount of list comprehension we have
        #to do with the other parameters
        ###################################

        #If both name and event ID are given, make sure they match
        if name is not None and eventId is not None:
            if name in self.name_to_id_map and self.name_to_id_map[name] != eventId:
                return []
        #If only name is given, map it to event ID
        elif name is not None and eventId is None:
            if name in self.name_to_id_map:
                eventId = self.name_to_id_map[name]
            else:
                return []

        #Lookup by event ID
        if eventId is not None:
            evrs = self.getByEventId(eventId)
        #No name or event ID...have to go through the whole table
        else:
            evrs = self.to_list()

        #Filter list by level
        if level is not None and evrs:
            evrs = [evr for evr in evrs if evr.level.lower() == level.lower()]

        #Filter list by module
        if module is not None and evrs:
            evrs = [evr for evr in evrs if evr.module == module]

        #Filter list by ERT start
        if ertExactStartVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.ertExact >= ertExactStartVal]

        #Filter list by ERT end
        if ertExactEndVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.ertExact <= ertExactEndVal]

        #Filter list by SCET start
        if scetExactStartVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.scetExact >= scetExactStartVal]

        #Filter list by SCET end
        if scetExactEndVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.scetExact <= scetExactEndVal]

        #Filter list by SCLK start
        if sclkExactStartVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.sclkExact >= sclkExactStartVal]

        #Filter list by SCLK end
        if sclkExactEndVal is not None and evrs:
            evrs = [evr for evr in evrs if evr.sclkExact <= sclkExactEndVal]

        #Filter list by message
        if message is not None and evrs:
            evrs = [evr for evr in evrs if evr.message == message]

        #Filter list by message substring
        if messageSubstr is not None and evrs:
            evrs = [evr for evr in evrs if messageSubstr in evr.message]

        return evrs

    #########################################
    #The following functions are overridden to maintain
    #the contents of the name_to_id_map that maps EVR names
    #to EVR IDs
    #########################################
    def __setitem__(self, key, item):
        '''x.__setitem__(i, y) <==> x[i]=y'''

        dict.__setitem__(self,key,item)
        if item:
            evr = item[0]
            if hasattr(evr,'name') and not evr.name in self.name_to_id_map:
                self.name_to_id_map[evr.name] = key

    def __delitem__(self, key):
        '''x.__delitem__(y) <==> del x[y]'''

        item = self.get(key,None)
        if item:
            evr = item[0]
            if hasattr(evr,'name') and evr.name in self.name_to_id_map:
                del self.name_to_id_map[evr.name]

        dict.__delitem__(self,key)

class ProductTable(dict):
    '''A lookup table of Products implemented as a thread-safe dictionary.  Products
    are indexed into the product table by their APID.'''

    def __init__(self):
        '''Initialize this product table'''

        _log().debug('mtak.collection.ProbTable()')

        dict.__init__(self)

        self.name_to_id_map = {}
        self.lock = threading.Lock()

    def getIds(self):
        '''Returns a list of unique product APIDs (used as keys) from the table

        Args
        -----
        None

        Returns
        --------
        A list of all the product APIDs for which at least one product (partial or complete) has been received (list of strings)'''

        _log().debug('mtak.collection.ProductTable.getIds()')

        return self.keys()

    getApids = getIds

    def getNames(self):
        '''Returns a list of unique product names from the table

        Args
        -----
        None

        Returns
        --------
        A list of all the product names for which at least one product (partial or complete) has been received (list of strings)'''

        _log().debug('mtak.collection.ProductTable.getNames()')

        return self.name_to_id_map.keys()

    getProductNames = getNames

    def getByStatus(self,status):
        '''Returns a list of all products with the specified status

        Args
        -----
        status - The status of the product(s) to retrieve (string)

        Returns
        --------
        A list of Product objects whose status matches the input status (list of mpcsutil.product.Product)'''

        _log().debug('mtak.collection.ProductTable.getByStatus()')

        status = str(status)

        results = []
        for productList in self.values():
            results.extend([product for product in productList if product.status == status])

        return results

    def getByName(self,name):

        _log().debug('mtak.collection.ProductTable.getByName()')

        name = str(name)

        if name in self.name_to_id_map:
            id = self.name_to_id_map.get(name)
            return self.getByApid(id)

        return []

    getByProductName = getByName

    def getById(self,apid):
        '''Returns a list of all products with the specified apid

        Args
        -----
        apid - The APID of the product(s) to retrieve (string or int)

        Returns
        --------
        A list of Product objects whose APID matches the input APID (list of mpcsutil.product.Product)'''

        _log().debug('mtak.collection.ProductTable.getById()')

        apid = int(apid)
        return self.get(apid,[])

    getByApid = getById

    def getByErtRange(self,start=mpcsutil.timeutil.minErtTime,end=mpcsutil.timeutil.maxErtTime):
        '''Returns a list of all products in the specified ERT range.   Takes a ISO or DOY formatted UTC time for start time and/or end time.

        Args
        -----
        start - The start time of the ERT time range (string)
        end - The end time of the ERT time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Product objects whose ERT falls within the input ERT range (list of mpcsutil.product.Product)'''

        _log().debug('mtak.collection.ProductTable.getByErtRange()')

        return compareByTimes(self.values(), 'ertExact', None, \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)))


    def getByDvtScetRange(self,start=mpcsutil.timeutil.minScetTime,end=mpcsutil.timeutil.maxScetTime):
        '''returns a list of all products in the specified Scet range.   Takes a ISO-formatted UTC time for start time and/or end time.

        Args
        -----
        start - The start time of the DVT SCET time range (string)
        end - The end time of the DVT SCET time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Product objects whose DVT SCET falls within the input DVT SCET range (list of mpcsutil.product.Product)'''

        _log().debug('mtak.collection.ProductTable.getByDvtScetRange()')

        return compareByTimes(self.values(), 'dvtScetExact', 'dvtScetNano', \
                              mpcsutil.timeutil.parseTimeString(str(start)), \
                              mpcsutil.timeutil.parseTimeString(str(end)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(start)), \
                              mpcsutil.timeutil.getTimeStringNanos(str(end)))

    def getByDvtSclkRange(self,start=mpcsutil.timeutil.minSclkTime,end=mpcsutil.timeutil.maxSclkTime):
        '''returns a list of all products in the specified Sclk range.

        Args
        -----
        start - The start time of the DVT SCLK time range (string)
        end - The end time of the DVT SCLK time range (string)

        If no start time is specified, it is assumed to be the earliest possible.
        If no end time is specified, it is assumed to be the latest possible.
        If no start or end time is specified, all values will be retrieved.

        Returns
        --------
        A list of Product objects whose DVT SCLK falls within the input DVT SCLK range (list of mpcsutil.product.Product)'''

        _log().debug('mtak.collection.ProductTable.getByDvtSclkRange()')

        return compareByTimes(self.values(), 'dvtSclkExact', None, \
                              mpcsutil.timeutil.parseDvtString(str(start)),
                              mpcsutil.timeutil.parseDvtString(str(end)), )

    def getApidSummary(self):
        '''Get a summary of all the products in the table summarized by APID.

        Args
        -----
        None

        Returns
        --------
        A dictionary whose keys are product APIDs (string) and whose values are
        counts of the number of that type of product (int)'''

        _log().debug('mtak.collection.ProbTable.getApidSummary()')

        summary = {}

        for apid in self.keys():
            summary[apid] = len(self[apid])

        return summary

    def count(self):
        '''Return the number of products in this table.

        Returns
        --------
        The number of products in this table.'''

        count = 0
        for productList in self.values():
            count += len(productList)
        return count

    def to_list(self):

        products = []
        for productList in self.values():
            products.extend(productList)
        return products

    def getByAttributes(self,name=None,transactionId=None,status=None,apid=None,
                        ertStart=None, ertEnd=None, dvtScetStart=None, dvtScetEnd=None,
                        dvtSclkStart=None,dvtSclkEnd=None):

        if name:
            name = str(name)

        if transactionId:
            transactionId = str(transactionId)

        if status:
            status = str(status)

        if apid:
            apid = int(apid)

        ertExactStartVal = None
        if ertStart is not None:
            ertExactStartVal = mpcsutil.timeutil.parseTimeString(ertStart)

        ertExactEndVal = None
        if ertEnd is not None:
            ertExactEndVal = mpcsutil.timeutil.parseTimeString(ertEnd)

        scetExactStartVal = None
        if dvtScetStart is not None:
            scetExactStartVal = mpcsutil.timeutil.parseTimeString(dvtScetStart)

        scetExactEndVal = None
        if dvtScetEnd is not None:
            scetExactEndVal = mpcsutil.timeutil.parseTimeString(dvtScetEnd)

        sclkExactStartVal = None
        if dvtSclkStart is not None:
            sclkExactStartVal = mpcsutil.timeutil.parseDvtString(dvtSclkStart)

        sclkExactEndVal = None
        if dvtSclkEnd is not None:
            sclkExactEndVal = mpcsutil.timeutil.parseDvtString(dvtSclkEnd)

        products = []

        ###################################
        #Try our best to look up by
        #name or apid first to limit the amount of list comprehension we have
        #to do with the other parameters
        ###################################

        #If both name and apid are given, make sure they match
        if name is not None and apid is not None:
            if name in self.name_to_id_map and self.name_to_id_map[name] != apid:
                return []
        #If only name is given, map it to apid
        elif name is not None and apid is None:
            if name in self.name_to_id_map:
                apid = self.name_to_id_map[name]
            else:
                return []

        #Lookup by apid
        if apid is not None:
            products = self.getById(apid)
        #No name or apid...have to go through the whole table
        else:
            products = self.to_list()

        if transactionId is not None and products:

            products = [product for product in products if product.transactionId == transactionId]

        if status is not None and products:

            products = [product for product in products if product.status == status]

        if ertExactStartVal is not None and products:

            products = [product for product in products if product.ertExact >= ertExactStartVal]

        if ertExactEndVal is not None and products:

            products = [product for product in products if product.ertExact <= ertExactEndVal]

        if scetExactStartVal is not None and products:

            products = [product for product in products if product.dvtScetExact >= scetExactStartVal]

        if scetExactEndVal is not None and products:

            products = [product for product in products if product.dvtScetExact <= scetExactEndVal]

        if sclkExactStartVal is not None and products:

            products = [product for product in products if product.dvtSclkExact >= sclkExactStartVal]

        if sclkExactEndVal is not None and products:

            products = [product for product in products if product.dvtSclkExact <= sclkExactEndVal]

        return products

    #########################################
    #The following functions are overridden to maintain
    #the contents of the name_to_id_map that maps product names
    #to product APIDs
    #########################################
    def __setitem__(self, key, item):
        '''x.__setitem__(i, y) <==> x[i]=y'''

        dict.__setitem__(self,key,item)
        if item:
            product = item[0]
            if hasattr(product,'name') and not product.name in self.name_to_id_map:
                self.name_to_id_map[product.name] = key

    def __delitem__(self, key):
        '''x.__delitem__(y) <==> del x[y]'''

        item = self.get(key,None)
        if item:
            prod = item[0]
            if hasattr(prod,'name') and prod.name in self.name_to_id_map:
                del self.name_to_id_map[prod.name]

        dict.__delitem__(self,key)

class CfdpIndicationTable(dict):
    '''A lookup table of CFDP Indications implemented as a thread-safe dictionary.
    CFDP Indications are indexed into the product table by their sourceEntityId:
    transactionSequenceNumber string.'''

    def __init__(self):
        '''Initialize this CFDP Products table'''

        _log().debug('mtak.collection.CfdpIndicationTable()')

        dict.__init__(self)

        self.lock = threading.Lock()

    def getTransactionIds(self):
        '''Returns a list of unique sourceEntityId:transactionSequenceNumber strings
        (used as keys) from the table

        Args
        -----
        None

        Returns
        --------
        A list of all the sourceEntityId:transactionSequenceNumber strings for which
        at least one CFDP Indication has been received (list of strings)'''

        _log().debug('mtak.collection.CfdpIndicationTable.getIds()')

        return self.keys()

    def getByTransactionId(self, sourceEntityId, transactionSequenceNumber):
        '''Returns a list of all CFDP Indications that match the type provided sourceEntityId:transactionSequenceNumber key

        Args
        -----
        sourceEntityId - Source entity ID (string)
        transactionSequenceNumber - Transaction sequence number (string)

        Returns
        --------
        A list of CFDP Indications that match the specified transaction ID (list of mpcsutil.cfdp.CfdpIndication)'''

        _log().debug('mtak.collection.CfdpIndicationTable.getByTransactionId()')

        transId = sourceEntityId + ':' + transactionSequenceNumber
        return self.get(transId,[])

    def count(self):
        '''Return the number of CFDP Indications in this table.

        Returns
        --------
        The number of CFDP Indications in this table.'''

        count = 0
        for cfdpIndicationList in self.values():
            count += len(cfdpIndicationList)
        return count

    def to_list(self):

        cfdpIndications = []
        for cfdpIndicationList in self.values():
            cfdpIndications.extend(cfdpIndicationList)
        return cfdpIndications

@contextmanager
def _buffer():
    buffer = StringIO()
    yield buffer
    buffer.close()

def getSummaryString(summary,linePrefix=''):
    '''Utility method to generate a summary string from a summary dictionary
    (see the getXXXSummary(...) methods on the objects in this module).

    Args
    -----
    summary - A dictionary whose keys & values will be displayed in a "key = value" format (dictionary {})
    linePrefix - Each key/value line will begin with this prefix (string)

    Returns
    --------
    A line by line summary string corresponding to the input dictionary (string)'''

    _log().debug('mtak.collection.getSummaryString()')

    buffer = StringIO()

    with _buffer() as buffer:
        [print('{}{} = {}'.format(linePrefix, key, value), file=buffer) for key, value in summary.items()]
        contents = buffer.getvalue()
        return contents if contents else 'Empty'

def compareByTimes(values, msKey, nsKey, startMs, endMs, startNs=0, endNs=0):
    ''' Compares telemetry objects between two times with nanosecond precision
        returning a list of telemetry items

     Args
    -----
    values - A list of tlm values (channels, evrs, or products)
    msKey - the tlm objects millisecond key (ie, 'scetExact')
    nsKey - the tlm object nanosecond key (ie, 'scetNano')
    startMs - start time ms since epoch
    endMs - end time ms since epoch
    startNs - start time ns precision. Default 0
    endNs - end time ns precision. Default 0

    Returns
    --------
    A list of telemetry items in the specified time range
     '''
    results = []
    for tlm_list in values:
        for tlm in tlm_list:
            if tlm[msKey] > startMs and tlm[msKey] < endMs:
                results.append(tlm)
            elif tlm[msKey] == startMs:
                if not nsKey: # nanos don't exist
                    results.append(tlm)
                elif tlm[nsKey] >= startNs:
                    results.append(tlm)
            elif tlm[msKey] == endMs:
                if not nsKey: # nanos don't exist
                    results.append(tlm)
                elif tlm[nsKey] <= endNs:
                    results.append(tlm)
    return results

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
