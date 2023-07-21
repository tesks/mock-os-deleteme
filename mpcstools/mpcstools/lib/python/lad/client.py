#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Module that provides key classes for building and executing global LAD queries.

The `ChanValQuery` and EvrQuery classes abstract away the structuring of the
server API. They can be used in both a builder mode, by calling their methods
to add and modify parameters, or by providing key word arguments.

They offer two main patterns of use.

Builder Pattern
===============
    `TelemQuery` and its subclass are meant to ease the use of the Global LAD (GLAD) Rest API by encoding its
    various parameters in methods. The classes provide a builder design pattern, so users may find it convenient to
    chain methods or set parameters imperatively.

    Any method that begins with "add" indicates that multiple values can be specified for a query parameter, e.g.,
    "addVcid" indicates that calling addVcid(0) and then addVcid(1) will result in telemetry from both vcid 0 and 1
    being returned.

    Any method that begins with "set" indicates that only one value can be specified for that query parameter per
    query, e.g, "setScid" indicates that calling setScid(44), and then scetScid(99), will overwrite the first call
    and only telemetry for scid 99 will be returned.

    The \*Only functions work the same way.  The wording is different for clarity to the user.

    Optional parameters for GLAD queries are omitted unless specified.  Required parameters default to:

    Data source - defaults to 'all'
    Recorded state - defaults to 'both'
    time type - defaults to 'ert'

.. _kwargs-label:

Keyword arguments
-----------------
    The classes also provide keyword arguments for setting each parameter
    as well - see below.
    The \*\*kwargs accepted by the constructor and the `update()` function are as follows:

    - source: (str) conforming to the pattern: fsw|sse|header|monitor|all
    - recordedState: (str) conforming to the pattern: realtime|recorded|both
    - timeType: (str) conforming to the pattern ert|scet|event|sclk|lst|all (lst not applicable to all misisons)
    - sessionNumber: list of strings of session IDs or session ID ranges
    - host: list of strings of session hosts
    - venue: list of strings of venues
    - dssId: list of session DSS identifiers
    - vcid: list of virtual channel identifiers
    - scid: int containing spacecraft ID
    - maxResults: int specifying the Maximum query results for each matching identifier
    - lowerBoundTime: str specifying the lower bound time for a time box query
    - upperBoundTime: str specifying the upper bound time for a time box query

    For the `ChanValQuery` only:
    - channelId:  list of stings of channel Ids or channel wildcard patterns, e.g. "A-1000" or "A-\*"

    For the `EvrQuery` only:
    - evrLevel: list of strings of EVR level regular expressions, e.g. "DIAGNOSTIC" or "WARNING\*"
    - evrName:  list of strings of EVR name regular expressions
    - evrMessage: list of EVR message regular expressions
    - evrId: list of strings of EVR event Ids

"""

from __future__ import (absolute_import, division, print_function)
import os, re, requests
from requests.adapters import HTTPAdapter
from abc import ABCMeta, abstractproperty
import mpcsutil.config

class TelemQuery(object):
    """
    Abstract base class for LAD telemetry query classes.  Cannot be instantiated directly.
    """

    __metaclass__ = ABCMeta

    """ The following constants codify the parameter names and the URI for the global LAD. """
    _SESSION_PARAM = 'sessionNumber'
    _HOST_PARAM = 'host'
    _VENUE_PARAM = 'venue'
    _DSSID_PARAM = 'dssId'
    _VCID_PARAM = 'vcid'
    _SCID_PARAM = 'scid'
    _MAX_RESULTS_PARAM = 'maxResults'
    _LOWER_BOUND_TIME_PARAM = 'lowerBoundTime'
    _UPPER_BOUND_TIME_PARAM = 'upperBoundTime'
    _URI_TEMPLATE = '{telemType}/{dataSource}/{recordedState}/{timeType}'
    _RECORDED_STATE_PARAM = 'recordedState'
    _DATA_SOURCE_PARAM = 'source'
    _TIME_TYPE_PARAM = 'timeType'
    _VERIFIED_PARAM = 'verified'
    _BINARY_RESPONSE = 'binaryResponse'
    _OUTPUT_FORMAT = 'outputFormat'

    _LIST_PARAMS= [_SESSION_PARAM, _HOST_PARAM, _VENUE_PARAM, _DSSID_PARAM, _VCID_PARAM]
    _SINGLE_PARAMS= [_MAX_RESULTS_PARAM, _LOWER_BOUND_TIME_PARAM, _UPPER_BOUND_TIME_PARAM, _SCID_PARAM]
    _UNSUPPORTED_PARAMS = [_VERIFIED_PARAM]

    def __init__(self, **kwargs):
        self._dataSource = 'all'
        self._recordedState = 'both'
        self._timeType = 'ert'
        self._params = {}
        self.update(**kwargs)

    def update(self, **kwargs):
        """
        Update the internal parameters from a dict of values
        List parameters will overwrite pre-existing parameter values as opposed to merging
        e.g., a call to updateDict({'sessionNumber' = [13,14]}) followed by a call  to
        update({'sessionNumber': [15,16]) will result in an effective value of [15,16] for
        sessionNumber in the resulting query

        :param kwargs: see :ref:`kwargs-label`
        """
        for key, value in kwargs.items():
            if key == self._RECORDED_STATE_PARAM: self._recordedState = value
            elif key == self._DATA_SOURCE_PARAM:  self._dataSource = value
            elif key == self._TIME_TYPE_PARAM: self._timeType = value
            elif key in self._listTypeParams:
                if type(value) is list:
                    self._params[key] = value
                else: self._addToListParam(key,str(value),resetExisting=True)
            elif key in self._SINGLE_PARAMS: self._params[key] = value
            else: pass # Ignores unsupported kwargs

    @abstractproperty
    def _listTypeParams(self):
        pass

    @abstractproperty
    def _telemType(self):
        pass

    def _addToListParam(self, paramName, paramValue, resetExisting=False):
        if paramName not in self._params or resetExisting:
            self._params[paramName] = list()
        self._params[paramName].append(paramValue)
        return self

    def addSessionNumber(self, sessionNumber):
        """
        Add a session number to filter on.  Before this is called the first time,
        telemetry from all sessions is returned.

        :param sessionNumber: a str representing the session number to include.
            In other AMPCS utilities, this may be referred to as the database key.
            Consider using this in conjunction with addHost,
            since sessionNumbers are not unique across different database instances.

        """
        self._addToListParam(self._SESSION_PARAM, str(sessionNumber))
        return self

    def addHost(self, host):
        """
        Add a host to filter results on.  Before this is called the first time,
        telemetry from all hosts is returned.

        :param host: str session host name to include
        """
        self._addToListParam(self._HOST_PARAM, host)
        return self

    def addVenue(self, venue):
        """
        Add a venue to filter results on.  Before this is called the first time,
        telemetry from all venues is returned.

        :param venue: a string containing the venue name to include

        """
        self._addToListParam(self._VENUE_PARAM, venue)
        return self

    def addDssId(self, dssId):
        """
        Add a Deep Space Station (DSS) identifier to filter results on.  Before this is called,
        all DssIds are included by default.

        :param dssId: an int identifying the deep space station identifier to include
        """

        self._addToListParam(self._DSSID_PARAM, dssId)
        return self

    def addVcid(self, vcid):
        """
        Add a virtual channel ID (VCID) to filter results on. Before this is called, all
        VCIDs are included by default.

        :param vcid: an int identifying a virtual channel ID to include.
        """
        self._addToListParam(self._VCID_PARAM, vcid)
        return self

    def setScid(self, scid):
        """
        Limit query results to a single scid.

        :param scid: an int representing the desired Spacecraft Identifier (SCID)

        """
        self._params[self._SCID_PARAM] = scid
        return self

    def setMaxResults(self, numResults):
        """
        Set the maximum number of results per telemetry identifier to return.
        Be careful - this means the more values returned by a query will affect performance.
        This number is per-channel and per-EVR level, so 10 results for 1000 channels will
        return up to 10000 results!

        :param numResults: int specifying the number of results per telemetry identifier,
            e.g. per channel and per EVR level
        """
        self._params[self._MAX_RESULTS_PARAM] = numResults
        return self

    def useErt(self):
        """
        Interpret 'before' and 'after' times as Earth Receive Times (ERT).
        These times should be provided in a ISO YYY-MM-DDYHH:mm:ss.ttt[mmm[n]] format
        or DOY YYYY-DOYTHH:mm:ss.ttt[mmm[n]] format, using the GMT timezone.
        """
        self._timeType = 'ert'
        return self

    def useEventTime(self):
        """
        Interpret 'before' and 'after' times as event times, the time at which the global LAD
        received telemetry.
        These times should be provided in a ISO YYY-MM-DDYHH:mm:ss.ttt[mmm[n]] format
        or DOY YYYY-DOYTHH:mm:ss.ttt[mmm[n]] format, using the GMT timezone.
        """
        self._timeType = 'event'
        return self

    def useScet(self):
        """
        Interpret 'before' and 'after' times as Spacecraft Event Times (SCET).
        These times should be provided in a ISO YYY-MM-DDYHH:mm:ss.ttt[mmm[n]] format
        or DOY YYYY-DOYTHH:mm:ss.ttt[mmm[n]] format, using the GMT timezone.

        """
        self._timeType = 'scet'
        return self

    def useSclk(self, scid):
        """
        Interpret 'before' and 'after' times as a Spacecraft clock (SCLK) associated with a numeric scid.
        This makes a call to `setScid()` on this object.

        The LAD server cannot be queried using Sclk times unless a SCID is provided to
        interpret the Sclk.

        :param scid: an int associated with a spacecraft. Query results will be telemetry for this scid only.

        """

        self.setScid(scid)
        self._timeType = 'sclk'
        return self

    def after(self, time):
        """
        Query for telemetry values occurring after some time.

        :param time: str representing a time according to some time format.
            ERT is used by default, but can be changed with `useScet()` or `useSclk()`.
            See pydoc for those methods for example formats.
        """

        self._params[self._LOWER_BOUND_TIME_PARAM] = time
        return self

    def before(self, time):
        """
        Query for telemetry values occurring before some time.

        :param time: str representing a time according to some time format.
            ERT is used by default, but can be changed with `useScet()` or `useSclk()`.
            See pydoc for those methods for example formats.
        """
        self._params[self._UPPER_BOUND_TIME_PARAM] = time
        return self

    def realtimeOnly(self):
        """ Limit the query to only telemetry marked as realtime. """
        self._recordedState = 'realtime'
        return self

    def recordedOnly(self):
        """ Limit the query to only telemetry marked as recorded. """
        self._recordedState = 'recorded'
        return self

    def fswOnly(self):
        """ Limit the query to only telmetry produced by flight software. """
        self._dataSource = 'fsw'
        return self

    def sseOnly(self):
        """ Limit the query to only telemetry produced by simulation support equipment (SSE). """
        self._dataSource = 'sse'
        return self

    """
    Unimplemented.
    def verifyTimeRange(self):
        self._params[self.VERIFIED_PARAM] = 'true'
        return self
    """

    def getUri(self):
        """
        :return: the string URI to be used for the currently configured query.
        """
        return self._URI_TEMPLATE.format(telemType=self._telemType,
                                         dataSource=self._dataSource,
                                         recordedState=self._recordedState,
                                         timeType=self._timeType
                                         )

    def getParams(self):
        """
        :return: a dict of (URL parameter name, configured value) pairs
        """
        return self._params

class ChanValQuery(TelemQuery):
    """
    Helper class for constructing a channel global LAD query.
    For constructor argument see :ref:`kwargs-label`
    """
    _CHANNEL_ID_PARAM = 'channelId'
    _CHANNEL_LIST_TYPE_PARAMS = [
        _CHANNEL_ID_PARAM,
    ] + TelemQuery._LIST_PARAMS

    def __init__(self, **kwargs):
        """
        :param kwargs: see :ref:`kwargs-label`
        """
        super(ChanValQuery, self).__init__()
        self._params[self._MAX_RESULTS_PARAM] = 10
        self.update(**kwargs)

    @property
    def _telemType(self):
        return 'eha'

    @property
    def _listTypeParams(self):
        return self._CHANNEL_LIST_TYPE_PARAMS

    def addChannelId(self, channelIdString):
        """
        Add channel ID string to query.  Supports the asterisk (*) wildcard.

        :param channelIdString: the channel Id string or expression to add

        """
        self._addToListParam(self._CHANNEL_ID_PARAM, channelIdString)
        return self

    def monitorOnly(self):
        """ Limit query results to monitor channels only. """
        self._dataSource = 'monitor'
        return self

    def headerOnly(self):
        """ Limit query results to header channels only.  """
        self._dataSource = 'header'
        return self

class EvrQuery(TelemQuery):
    """
    Class for specifying LAD EVR query parameters.

    maxResults for EVR queries is per-EVR level.  The default for this class is 100 EVRs per-level.

    """

    _EVR_LEVEL_PARAM = 'evrLevel'
    _EVR_NAME_PARAM = 'evrName'
    _EVR_MESSAGE_PARAM = 'evrMessage'
    _EVR_EVENT_ID_PARAM = 'evrId'
    _EVR_LIST_TYPE_PARAMS = [
            _EVR_NAME_PARAM,
            _EVR_LEVEL_PARAM,
            _EVR_MESSAGE_PARAM,
            _EVR_EVENT_ID_PARAM,
        ] \
                            + TelemQuery._LIST_PARAMS

    def __init__(self, **kwargs):
        """
        :param kwargs: see :ref:`kwargs-label`
        """
        super(EvrQuery, self).__init__()
        self._evrLevels = list()
        self._evrNames = list()
        self._evrMessages = list()
        self._params[self._MAX_RESULTS_PARAM] = 100
        self.update(**kwargs)

    @property
    def _telemType(self):
        return 'evr'

    @property
    def _listTypeParams(self):
        return self._EVR_LIST_TYPE_PARAMS

    def addEvrLevel(self, evrLevelString):
        """
        Specify an EVR level string to filter for.  Accepts the '*' wildcard.

        :param evrLevelString: the evr level or level expression to add

        """
        self._addToListParam(self._EVR_LEVEL_PARAM, evrLevelString)
        return self

    def addEvrName(self, evrNameString):
        """
        Specify an EVR name string to filter for.  Accepts the '*' wildcard.

        :param evrNameString: the evr name or expression to add

        """
        self._addToListParam(self._EVR_NAME_PARAM, evrNameString)
        return self

    def addMessagePattern(self, evrMessageString):
        """
        Specify an EVR message string to filter for.  Accepts the '*' wildcard.

        :param evrMessageString: the evr message string or expression to add

        """
        self._addToListParam(self._EVR_MESSAGE_PARAM, evrMessageString)
        return self

    def addEventId(self, eventId):
        """
        Specify a numeric event ID to filter for.

        :param eventId: the event ID to add a filter for

        """
        self._addToListParam(self._EVR_EVENT_ID_PARAM, eventId)
        return self

class LadClient(HTTPAdapter):
    """ LadClient can be used to fetch telemetry from a global LAD instance. """

    # This template is used to construct the URI for queries.
    _URI_TEMPLATE = '{prefix}://{host}:{port}/globallad/{queryUri}'

    @property
    def gds_config(self):
        if self.__dict__.get('_gds_config') is None:
            self.gds_config=mpcsutil.config.GdsConfig()
        return self.__dict__.get('_gds_config')
    @gds_config.setter
    def gds_config(self, value):
        self.__dict__.update({'_gds_config': value})

    @property
    def host(self):
        if self.__dict__.get('_host') is None:
            self.host = self.gds_config.getLadHost()
        return self.__dict__.get('_host')
    @host.setter
    def host(self, value):
        self.__dict__.update({'_host':value})

    @property
    def port(self):
        if self.__dict__.get('_port') is None:
            self.port = self.gds_config.getLadPort()
        return self.__dict__.get('_port')
    @port.setter
    def port(self, value):
        self.__dict__.update({'_port':value})

    @property
    def https(self):
        if self.__dict__.get('_https') is None:
            self.https = bool(re.match(r'^https.*$', self.gds_config.getLadUri()))
        return self.__dict__.get('_https')
    @https.setter
    def https(self, value):
        self.__dict__.update({'_https':value})

    @property
    def prefix(self):
        if self.__dict__.get('_prefix') is None:
            self.prefix = 'http{}'.format('s' if self.https else '')
        return self.__dict__.get('_prefix')
    @prefix.setter
    def prefix(self, value):
        self.__dict__.update({'_prefix':value})

    @property
    def ca_bundle(self):
        if self.__dict__.get('_ca_bundle') is None:
            self.ca_bundle = os.path.join(os.path.dirname(self.gds_config.getProperty('server.ssl.trust-store', '/etc/pki/tls/certs/ca-bundle.crt')), 'ca-bundle.crt')
        return self.__dict__.get('_ca_bundle')
    @ca_bundle.setter
    def ca_bundle(self, value):
        self.__dict__.update({'_ca_bundle':value})

    def __init__(self, host=None, port=None, https=False):
        """
        Create a LAD client that will query the global LAD at the address host:port

        :param host: global LAD server host name
        :param port: global LAD server REST port
        :param https: whether or not ot use https for queries

        """
        super(LadClient, self).__init__()
        [self.__setattr__(key, value) for key, value in dict(zip(['host', 'port', 'https'], [host, port, https])).items() if value is not(None)]

        if https is True:
            self.init_poolmanager(connections=5, maxsize=5)
            with requests.Session() as _sess:
                _sess.mount(
                    '{prefix}://{host}:{port}'.format(prefix=self.prefix, host=self.host, port=self.port), self
                    )

    def fetchEvrs(self, evrQuery=EvrQuery(),timeout=None):
        """
        Queries the global LAD for Evrs and returns the result directly.

        :param evrQuery: an `EvrQuery` object configured with desired parameters.
            See `EvrQuery` class for the default configuration.

        :param timeOut: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: a dict where each key is an EVR level, and each value is list of EVR dictionaries
        """

        if not type(evrQuery) is EvrQuery:
            raise TypeError('evrQuery is not of type EvrQuery')
        return self._queryLad(evrQuery,timeout=timeout)


    def fetchChannels(self, chanValQuery=ChanValQuery(), timeout=None):
        """
        Queries the global LAD for channel values and returns the results directly.

        :param chanValQuery: a ChanValQuery object configured with default parameters.
            See `ChanValQuery` class for the default configuration.
        :param timeout: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: dictionary where each key is a channel ID, and each value is a list
            of channel value dictionaries
        """
        if not type(chanValQuery) is ChanValQuery:
            raise TypeError('chanValQuery is not of type ChanValQuery')
        return self._queryLad(chanValQuery, timeout)


    def _queryLad(self, telemQuery, timeout=None):
        """
        Executes a global LAD query.
        :param  telemQuery: TelemQuery object with configured parameters for the query.
        :param timeout: Either a number to use as a connection and read timeout on an underlying
            socket, or a tuple with each value separately.
            See http://docs.python-requests.org/en/master/user/advanced/#timeouts

        :return: the global LAD's json response to the query
        """
        full_uri = self._URI_TEMPLATE.format(prefix=self.prefix, host=self.host, port=self.port, queryUri=telemQuery.getUri())

        # MPCS-11737: requests fails SSL handshake without explicitly supplying CA Bundle path
        _dd=dict(params=telemQuery.getParams(), timeout=timeout)

        if self.https:
            _dd.update(dict(verify=self.ca_bundle))

        with requests.Session() as _sess:
            r = _sess.get(full_uri, **_dd)
            r.raise_for_status()
            return r.json()

    def getFormattedQuery(self, telemQuery):
        """
        Gets a formatted global LAD query

        :param telemQuery: `TelemQuery` object with configured parameters for the query

        :return: a formatted global LAD query
        """
        return self._URI_TEMPLATE.format(prefix=self._prefix, host=self._host, port=self._port, queryUri=telemQuery.getUri())

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
