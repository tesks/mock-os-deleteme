#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import functools
from lad.client import LadClient, ChanValQuery, EvrQuery
import mpcsutil.channel
import mpcsutil.evr
from mpcsutil.channel import ChanVal
from mpcsutil.evr import Evr
from mpcsutil.timeutil import doyToIso


def dictToEvr(evrDict):
    """
    Convert a dict representation of an EVR into an EVR object

    :param evrDict: a dict with fields as the global LAD would name them

    :return: resulting EVR
    :rtype: `mpcsutil.evr.Evr`
    """

    # Strategy is to make the mapping verbose for the sake of explicitness. This
    # code needs to be kept in sync with the global LAD's JSON output format
    prepared = {}
    # Begin corresponding attributes
    prepared['dssId'] = evrDict['dssId']
    prepared['sclk'] = evrDict['sclk']
    prepared['scet'] = doyToIso(evrDict['scet'])
    prepared['ert'] = doyToIso(evrDict['ert'])
    prepared['lst'] = evrDict['lst']
    prepared['message'] = evrDict['message']
    prepared['eventTime'] = doyToIso(evrDict['eventTime'])
    # Rename and convert boolean attributes
    prepared['realtime'] = (evrDict['isRealTime'])
    prepared['fromSse'] = (evrDict['isFsw'] == 'false')
    # Rename attributes
    prepared['vcidMapping'] = evrDict['vcid']
    prepared['eventId'] = evrDict['evrId']
    prepared['level'] = evrDict['evrLevel']
    prepared['module'] = ''
    prepared['vcidMapping'] = evrDict['vcid']
    prepared['name'] = evrDict['evrName']
    # metadata is handled specially, see mpcsutil.evr.Evr
    prepared['metadata'] = [
        ('TaskName', evrDict['TaskName']),
        ('SequenceId', evrDict['SequenceId']),
        ('CategorySequenceId', evrDict['CategorySequenceId']),
        ('AddressStack', evrDict['AddressStack']),
        ('TaskId', evrDict['TaskId']),
        ('Source', evrDict['Source']),
        ('errno', evrDict['errno'])
    ]

    return mpcsutil.evr.Evr(**prepared)


def dictToChanVal(chanDict):
    """
    Convert a dict representation of an channel value into a ChanVal object

    :param chanDict: a dict with fields as the global LAD would name them

    :return: resulting channel value
    :rtype: `mpcsutil.channel.ChanVal`
    """

    # Strategy is to make the mapping verbose for the sake of explicitness. This
    # code needs to be kept in sync with the global LAD's JSON output format
    prepared = {} # This dict will stage reformatted attributes

    prepared['dssId'] = chanDict['dssId']
    prepared['sclk'] = chanDict['sclk']
    prepared['scet'] = doyToIso(chanDict['scet'])
    prepared['ert'] = doyToIso(chanDict['ert'])
    prepared['lst'] = chanDict['lst']
    prepared['dn'] = chanDict['dn']
    prepared['eu'] = chanDict['eu']
    prepared['channelId'] = chanDict['channelId']
    prepared['status'] = chanDict['status']
    prepared['eventTime'] = doyToIso(chanDict['eventTime'])
    # Rename and convert boolean attributes
    prepared['realtime'] = (chanDict['isRealTime'])
    prepared['vcidMapping'] = chanDict['vcid']

    prepared['alarms'] = [
        (chanDict['dnAlarmState'], chanDict['dnAlarmLevel']),
        (chanDict['euAlarmState'], chanDict['euAlarmLevel'])
    ]

    prepared['type'] = chanDict['channelType'].lower()
    return mpcsutil.channel.ChanVal(**prepared)


def flattenDict(dictToFlatten):
    """
    Flattens a dictionary of lists to a flat list

    :param dictToFlatten: dict with lists as values

    :return: flattened list
    :rtype: list
    """
    return functools.reduce(lambda a,b: a+b, dictToFlatten.values(), [])

def evrsFromDict(evrsByLevel):
    """
    Converts LAD output into a flattened list of Evrs.

    :param evrsByLevel: a dictionary of evrLevel : list pairs
    :return: dict with Evr level streams as keys, lists of mpcsutil.evr.Evr objects as values
    """
    return { level : list(map(dictToEvr, evrs)) for level, evrs in evrsByLevel.items() }

def chanValsFromDict(chanValsById):
    """
    Converts LAD output into a flattened list of ChanVals.

    :param chanValsById: a dictionary of channelId : list pairs
    :return: a dict with channelId strings as keys, lists of mpcsutil.channel.ChanVal objects as values
    """
    return { channelId : list(map(dictToChanVal, chanVals)) for channelId, chanVals in chanValsById.items() }

class GdsLadClient(LadClient):
    """
    Subclass of lad.client.LadClient that returns mpcsutil ChanVal and Evr objects from queries.
    Appropriate for use when uninterpreted results are not desired.
    """
    def __init__(self, host=None, port=None, https=None):
        """
        Create a LAD client that will query the global LAD at the address host:port

        :param host: global LAD server host name
        :param port: global LAD server REST port
        :param https: whether or not ot use https for queries
        """
        super(GdsLadClient, self).__init__(host, port, https)

    def fetchChannels(self, chanValQuery=ChanValQuery(), timeout=None):
        """
        Queries the global LAD and converts its output into individual channel value objects.

        :param chanValQuery: a ChanValQuery object configured with desired parameters.
            See `ChanValQuery` class for the default configuration.

        :param timeout: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: dict of where channel IDs are keys and lists of `ChanVal` objects are values
        """
        return chanValsFromDict(super(GdsLadClient, self).fetchChannels(chanValQuery, timeout=timeout))

    def fetchEvrLevels(self, evrQuery=EvrQuery(), timeout=None):
        """
        Queries the global LAD for EVRs and converts individual results as EVR objects.

        :param evrQuery: an EvrQuery object configured with desired parameters.
            See EvrQuery class for the default configuration.

        :param timeout:  Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: dict where EVR levels are keys, and lists of `Evr` objects are the values
        """
        return evrsFromDict(super(GdsLadClient, self).fetchEvrs(evrQuery, timeout=timeout))

    def fetchChanVals(self, chanValQuery=ChanValQuery(), timeout=None):
        """
        Queries the global LAD and converts its output into individual channel value objects.

        :param chanValQuery:  a `ChanValQuery` object configured with desired parameters.
            See ChanValQuery class for the default configuration.
        :param timeout: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: list of `mpcsutil.channel.ChanVal` objects
        """
        return flattenDict(self.fetchChannels(chanValQuery, timeout=timeout))

    def fetchEvrs(self, evrQuery=EvrQuery(), timeout=None):
        """
        Queries the global LAD for Evrs and converts the results to EVR objects.

        :param evrQuery: an EvrQuery object configured with desired parameters.
            See EvrQuery class for the default configuration.

        :param timeout: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: - list of `mpcsutil.evr.Evr` objects
        """
        return flattenDict(self.fetchEvrLevels(evrQuery, timeout=timeout))

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
