#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines a common object to represent an
EHA channel value that can be used across MPCS missions
"""

from __future__ import (absolute_import, division, print_function)


import collections
import logging
import mpcsutil
import time
import six
import csv
import re
import json
long=int if six.PY3 else long

_log =lambda : logging.getLogger('mpcs.util')

# MPCS-8022  - 2/29/2016: Added channel type for time, which behaves like a unsigned_int would
valid_channel_types = ["ascii", "signed_int", "status", "float", "double", "boolean", "bool", "unsigned_int", "digital", "time"]
config = mpcsutil.config.GdsConfig()
_str = lambda x : x if isinstance(x, six.string_types) else ( x.decode('utf-8') if isinstance(x, bytes) else '{}'.format(x) )
def formatDn(chanType, dn):
    '''Format the DN portion of a channel value based on the channel type. Input is generally
    a string and the return value is of the proper Python type based on the channel type.

    Args
    -----
    chanType - A string value representing the type of the EHA channel (string)
    (known values are 'ascii','signed_int','float','double','boolean','bool','unsigned_int','status','digital', 'time')

        'ascii' type will return a string object
        'signed_int' or 'status' will return an int object
        'boolean' or 'bool' will return a boolean object
        'unsigned_int', 'digital', or 'time' will return a long object (exception will be raised if the value is negative)
        'float' or 'double' will return a float object

    dn - The actual channel value to convert to the specified format (string,int,long, or float).  If the input dn value
         is None, then None will be returned. Generally this value is input as a string.

    Raises
    -------
    ValueError - If an invalid conversion between dn and channel type occurs (e.g. input 'abc' as a 'signed_int') or
    if an unrecognized channel type is passed in'''


    if dn is None:
        _log().warning('Input DN value of "None" given to formatDn for chanType {}'.format(chanType))
        return None

    def _long(dn):
        value = long(dn)
        if value < 0:
            errString = 'The value dn={} could not be converted to a {} value because it is negative.'.format(dn, chanType)
            _log().error(errString)
            raise ValueError(errString)
        return value
    def _default(dn):
        errString = 'The Channel Value type "{}" is unrecognized by MTAK. Valid types are: {}'.format(chanType, valid_channel_types)
        _log().error(errString)
        raise ValueError(errString)
    #Convert the chantype to lower case to make comparisons easier
    type = str(chanType).lower()
    _dd = {
        'ascii':str,
        "signed_int":int,
        "status":int,
        'bool':int,
        'boolean':int,
        'float':float,
        'double':float,
        'unsigned_int':_long,
        'digital':_long,
        'time':_long
        }

    return _dd.get(type, _default)(dn)

_channel_fields = collections.OrderedDict([
    ('receiveTime',0),
    ('eventTime',''),
    ('eventTimeExact',0),
    ('sclk',''),
    ('sclkCoarse',0),
    ('sclkFine',0),
    ('sclkExact',0),
    ('ert',''),
    ('ertExact',0),
    ('ertExactFine',0),
    ('scet',''),
    ('scetExact',0),
    ('scetNano',0),
    ('lst',''),
    ('lstExact',0),
    ('realtime',''),
    ('channelId',''),
    ('type',''),
    ('name',''),
    ('module',''),
    ('dnUnits',''),
    ('euUnits',''),
    ('status',''),
    ('dn',''),
    ('eu',''),
    ('alarms',''),
    ('dssId',''),
    ('vcid',''),
    ('vcidMapping',''),
    ('injected','')])
ChanVal_NT = mpcsutil.NamedTuple('ChanVal_NT', _channel_fields.keys(), _channel_fields)
_str_to_bool = lambda x: bool(mpcsutil.getBooleanFromString(x))
_list_or_str = lambda x: x if isinstance(x, (list, six.string_types)) else str(x)
_fmt_dd = {
        'receiveTime': int,
        'eu': float,
        'eventTimeExact': long,
        'sclkCoarse': long,
        'sclkFine': long,
        'ertExact': long,
        'ertExactFine': long,
        'lstExact': long,
        'sclkExact': long,
        'scetNano': int,
        'scetExact': int,
        'dssId': int,
        'realtime': _str_to_bool,
        'injected': _str_to_bool,
        'alarms': _list_or_str
    }
class ChanVal(mpcsutil.TelemetryItem):
    '''A ChanVal object represents a single EHA channel value from a single
    instance in time.

    Object Attributes
    ------------------
    receiveTime - The time, in milliseconds, that MTAK received this telemetry value (long)
    eventTime - The time that MPCS sent out the JMS message for this value (string)
    sclk - The SCLK value for when this value was read (format CCCCCCCCCC[.-]FFF[FF] (string)
    ert - The ERT for when this value was received in ISO format (string)
    scet - The correlated SCLK time in ISO format (string)
    scetExact - The scet milliseconds since epoch for the SCET time string (int)
    scetNano - The nanoseconds for the SCET time string (int)
    lst - The Local Solar Time value in SOL-XXXXMHH:MM:SS.sss format
    channelId - The ID of the channel that this value is part of (string)
    type - The type of this channel (string)
    dnUnits - The units for the DN (string)
    euUnits - The units for the EU (string)
    dn - The DN value for the channel...type varies based on type (string, int, long, or float)
    eu - The EU value for the channel (float)
    status - The string value of the channel for table-driven channels (e.g. status, boolean, etc.)
    alarms - A list of any alarms set on this channel ( list of tuples [(alarmDef,alarmLevel)] )
    dssId - The receiving station ID
    vcid - The VC number on which the channel was received (int)
    vcidMapping - The VC on which the channel was received, or the project-specific string it maps to'''

    '''
    THIS CLASS IS CONTROLLED BY THE AUTO SIS (D-001008_MM_AUTO_SIS)
    Any modifications to this class must be reflected in the SIS
    '''

    # Item on the left is an alias for item on the right
    # (This works for setting and getting of attributes)
    _ALIASES = {}
    log = property(
        lambda x: x.__dict__.setdefault('_log', logging.getLogger('mpcs.util')),
        lambda x,y: x.__dict__.update({'_log': y}))
    data = property(
        lambda x:x.__dict__.setdefault('_data', x.data_container()),
        lambda x,y: x.__dict__.update({'_data':y}))
    data_container = property(
        lambda x: x.__dict__.setdefault('_data_container', ChanVal_NT),
        lambda x,y: x.__dict__.update({'_data_container': y}))
    rct = property(
        lambda x:x.__dict__.setdefault('_rct', time.time()),
        lambda x,y: x.__dict__.update({'_rct': y}))

    def __init__(self, csvString=None, **kwargs):
        """ Initialize this channel value object

        Args
        -----
        csvString - A comma separated value string representing a channel value.  The order of this string is:

                    chan\,$eventTime\,$eventTimeExact\,$sclk\,$sclkCoarse\,$sclkFine\,$sclkExact\,$ert\,$ertExact\,$ertExactFine\,$scet\,$scetExact\,$scetNano\,$lst\,$lstExact\,$realtime\,$channelId\,$channelType\,$channelName\,$channelModule\,$dnUnits\,$euUnits\,$status\,$dataNumber\,$eu\,$dssId\,$vcid\,$vcidMapping\,$alarmDef\,$alarmLevel (alarm def & level can repeat)

        kwargs - Any keyword arguments whose names match the names of attributes on this object

        Returns
        --------
        None"""
        super(ChanVal, self).__init__()

        if csvString:
            self.csvString = csvString
            kwargs = dict(self.data._asdict(), **kwargs)

        _data_dd = {
            kk: _fmt_dd.get(kk, str)(vv) for kk, vv in kwargs.items() if (kk in self.data_container._fields) and (vv is not None) and (not(re.match(r'^\s*$', '{}'.format(vv))))
        }
        if bool(re.match(r'.*(\.).*', str(_data_dd.get('scet')))):
            _data_dd.update({'scetExact': int(mpcsutil.timeutil.parseTimeString(_data_dd.get('scet')))})
            _data_dd.update({'scetNano': int(mpcsutil.timeutil.getTimeStringNanos(_data_dd.get('scet'))) })
            _data_dd.update({'scet': mpcsutil.timeutil.getTimeStringExt(ms=_data_dd.get('scetExact'), nanos=_data_dd.get('scetNano'), precision=mpcsutil.timeutil.getScetPrecision())})

        _status = _data_dd.get('status')
        _type = _data_dd.get('type')
        _eu = str(_data_dd.get('eu')).strip()
        _data_dd.update(
            dict(
                realtime=_str_to_bool(_data_dd.get('realtime')),
                injected=_str_to_bool(_data_dd.get('injected')),
                dn=formatDn(_data_dd.get('type'), _data_dd.get('dn')),
                status=self._formatBooleanStatus(None if re.match(r'^\s*$', '{}'.format(_status)) else _status, _type),
                eu=float(_eu) if (_eu and re.match(r'^[-+]?[\d]*?\.?[\d]*[-+eE]*[\d]*$', str(_eu))) else None
            )
        )

        _dc = self.data_container(**_data_dd)
        self.data = _dc

    def __deepcopy__(self, memo):
        result = super(ChanVal, self).__deepcopy__(memo)
        result.rct = self.rct
        return result

    def __getattr__(self, name):
        return getattr(self.data, name, self.__dict__.get(name))

    def __setattr__(self, name, value):
        if name in self.data_container._fields:
            self.data = self.data_container(**dict(self.data._asdict(), **{name:value}))
        else:
            super(ChanVal, self).__setattr__(name, value)

    def _formatBooleanStatus(self, status=None, type=None):
        """ If self.type is 'boolean', casts certain status strings to a boolean and set self.status """
        # MPCS-8016  - 2/29/2016: boolean channels are allowed to have custom statuses for
        # true / false values.  In order to support this but not erroneously set a "true" value to false,
        # check the true/false aliases and only cast status field to boolean if it is one or the other.
        status = status if status else self.status
        type = type if type else self.type
        if type.lower() == 'boolean' and not(status is(None)):
            if mpcsutil._isFalseValue(status): status = False
            elif mpcsutil._isTrueValue(status): status = True
        return status

    @property
    def csvString(self): return self._csvString
    @csvString.setter
    def csvString(self, value): return self.parseFromCsv(value) if value and isinstance(value, six.string_types) else None

    def parseFromCsv(self, csvString):
        '''Parse the input CSV string and populate all the attributes of this object.  If the
        CSV string is not properly formed, exceptions may be raised.

        Args
        -----
        csvString - A comma separated value string representing a channel value.  The order of this string is:

                    chan\,$eventTime\,$eventTimeExact\,$sclk\,$sclkCoarse\,$sclkFine\,$sclkExact\,$scetNano\,$ert\,$ertExact\,$ertExactFine\,$scet\,$scetExact\,$lst\,$lstExact\,$realtime\,$channelId\,$channelType\,$channelName\,$channelModule\,$dnUnits\,$euUnits\,$status\,$dataNumber\,$eu\,$dssId\,$vcid\,$alarmDef\,$alarmLevel (alarm def & level can repeat)

        Returns
        --------
        None'''


        def divide_chunks(_iter, _num):
            for ii in range(0, len(_iter), _num):
                _chunk=_iter[ii:ii + _num]
                if all(_chunk): yield _chunk

        #Split up the CSV string into pieces
        splitString = csvString.split('\,')

        _fields=['eventTime', 'eventTimeExact', 'sclk', 'sclkCoarse', 'sclkFine', 'sclkExact', 'ert', 'ertExact', 'ertExactFine', 'scet', 'scetExact', 'scetNano', 'lst', 'lstExact', 'realtime', 'channelId', 'type', 'name', 'module', 'dnUnits', 'euUnits', 'status', 'dn', 'eu', 'dssId', 'vcid']
        _data=splitString[1:27]
        _dd = dict(zip(_fields, _data))

        _ints=['receiveTime','eventTimeExact', 'sclkCoarse', 'sclkFine', 'sclkExact', 'ertExactFine', 'scetExact', 'scetNano', 'lstExact']
        _toint=lambda x: long(x) if (x and re.match(r'[\d]+', x)) else x
        _dd.update({kk: _toint(_dd.get(kk)) for kk in _ints})

        _scet=str(_dd.get('scet'))
        if bool(re.match(r'.*(\.).*', _scet)):
            _dd.update({'scetExact': int(mpcsutil.timeutil.parseTimeString(_dd.get('scet')))})
            _dd.update({'scetNano': int(mpcsutil.timeutil.getTimeStringNanos(_dd.get('scet'))) })
            _dd.update({'scet':mpcsutil.timeutil.getTimeStringExt(ms=_dd.get('scetExact'), nanos=_dd.get('scetNano'), precision=mpcsutil.timeutil.getScetPrecision())})

        _status = _dd.get('status').strip()
        _type = _dd.get('type').strip()
        _eu = _dd.get('eu').strip()
        _dssId = _dd.get('dssId').strip()
        _vcid = _dd.get('vcid').strip()

        _dd.update({
            'realtime': _str_to_bool(_dd.get('realtime')),
            'injected': _str_to_bool(_dd.get('injected')),
            'status': self._formatBooleanStatus(None if re.match(r'^\s*$', _status) else _status, _type),
            'dn': formatDn(_type, _dd.get('dn')),
            'eu': float(_eu) if (_eu and re.match(r'^[-+]?[\d]*?\.?[\d]*[-+eE]*[\d]*$', _eu)) else None,
            'dssId': int(_dssId) if _dssId else None,
            'vcid': _vcid,
            'vcidMapping': config.getVcidMapping(_vcid),
            'alarms': [tuple(_chunk) for _chunk in divide_chunks(splitString[27:], 2) ]
            })

        self.data = self.data_container(**_dd)

    @staticmethod
    def get_from_database_csv(csv):
        # MPCS-8421 - 09/07/16: Remove conditional LST parsing. MPCS-8210
        # made it so that an empty column appears if LST is disabled.

        # chanval = ChanVal()

        #slice off leading/trailing quote mark & then use "split"
        #to separate out the CSV into pieces
        pieces = _str(csv[1:-2]).split('","')
        # Example of an expected channel value record. Also see the "plot_csv.vm" Velocity template.
        # "Eha","22","localhost","WAKE-4056","0","0","FSM_WAKE_SHUTDWN_IN_PROGRESS","FSM",
        # "2016-244T16:27:34.692","2015-176T19:05:00.925","","0488530115.50000","0","","NONE","true","STATUS"
        _fields = ['channelId', 'dssId', 'vcid', 'name', 'module', 'ert', 'scet', 'lst', 'sclk', 'dn', 'eu', 'status', 'realtime', 'type']
        _dd = dict(zip(_fields, pieces[3:17]))
        _eu = _dd.get('eu').strip()

        _scet=str(_dd.get('scet'))
        if bool(re.match(r'.*(\.).*', _scet)):
            _dd.update({'scetExact': int(mpcsutil.timeutil.parseTimeString(_dd.get('scet')))})
            _dd.update({'scetNano': int(mpcsutil.timeutil.getTimeStringNanos(_dd.get('scet'))) })
            _dd.update({'scet':mpcsutil.timeutil.getTimeStringExt(ms=_dd.get('scetExact'), nanos=_dd.get('scetNano'), precision=mpcsutil.timeutil.getScetPrecision())})

        _dd.update({
            'vcidMapping': config.getVcidMapping(_dd.get('vcid')),
            'scetExact': int(mpcsutil.timeutil.parseTimeString(_dd.get('scet'))),
            'scetNano': mpcsutil.timeutil.getTimeStringNanos(_dd.get('scet')),
            'eu': float(_eu) if _eu and re.match(r'^[-+]?[\d]*?\.?[\d]*[-+eE]*[\d]*$', _eu) else None,
            'realtime': _str_to_bool(_dd.get('realtime')),
            'injected': _str_to_bool(_dd.get('injected'))
        })
        _dd.update({'scet':mpcsutil.timeutil.getTimeStringExt(ms=_dd.get('scetExact'), nanos=_dd.get('scetNano'), precision=mpcsutil.timeutil.getScetPrecision())})
        return ChanVal(**_dd)

    def toCsv(self):
        '''Generate a CSV Representation of this channel value.  This value is in the format expected
        by the parseFromCsv method.

        Args
        -----
        None

        Returns
        --------
        A CSV-formmatted string representing this object (string)'''


        _ss = self.data._asdict()
        _alarms = _ss.get('alarms',[])
        _alarm_str = "".join("\,{}\,{}".format(data[0], data[1]) for data in _alarms) if _alarms else ""
        _ss.update({'alarms':_alarm_str})
        _ss={kk: '' if vv is None else vv for kk,vv in _ss.items()}
        return "chan\,{eventTime}\,{eventTimeExact}\,{sclk}\,{sclkCoarse}\,{sclkFine}\,{sclkExact}\,{ert}\,{ertExact}\,{ertExactFine}\,{scet}\,{scetExact}\,{scetNano}\,{lst}\,{lstExact}\,{realtime}\,{channelId}\,{type}\,{name}\,{module}\,{dnUnits}\,{euUnits}\,{status}\,{dn}\,{eu}\,{dssId}\,{vcid}{alarms}".format(**_ss)

    def clear(self):
        ''' Internal method that clears all the values on this channel value object. This method is also used by the __init__
        method to attach all the normal attributes to this object.

        Args
        -----
        None

        Returns
        --------
        None'''
        self.data=self.data_container()

    def get_plot_label(self): return '{} ({})'.format(self.name, self.channelId)
    def _asdict(self):
        return self.data._asdict()
    def _asjson(self, indent=4):
        return json.dumps(self._asdict(), indent=indent)
    def __getitem__(self, item): return self.__getattr__(item)
    def __setitem__(self, key, value): self.__setattr__(key, value)
    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''
        return '{}.{}({})'.format(self.__class__.__module__, self.__class__.__name__, ','.join('{}={}'.format(kk, repr(vv)) for kk,vv in self._asdict().items()))

    def __str__(self):
        '''x.__str__() <==> str(x)'''
        return self.__repr__()

    def __format__(self, spec):
        return self._asjson() if re.match(r'^.*(json).*$', spec, flags=re.I) else super(ChanVal, self).__format__(spec)

    def __eq__(self, other):
        '''x.__eq__(y) <==> x == y'''

        if other is(None):
            return False

        #Ignore eventTime and receiveTime and status
        try:
            return   self.channelId == other.channelId and \
                     self.name == other.name and \
                     self.module == other.module and \
                     self.type == other.type and \
                     self.dn == other.dn and \
                     self.dnUnits == other.dnUnits and \
                     self.eu == other.eu and \
                     self.euUnits == other.euUnits and \
                     self.sclkExact == other.sclkExact and \
                     self.scetExact == other.scetExact and \
                     self.scetNano == other.scetNano and \
                     self.lstExact == other.lstExact and \
                     self.ertExact == other.ertExact and \
                     self.ertExactFine == other.ertExactFine and \
                     self.realtime == other.realtime and \
                     self.alarms == other.alarms and \
                     self.dssId == other.dssId and \
                     self.vcid == other.vcid and \
                     self.vcidMapping == other.vcidMapping
        except AttributeError:
            return False

    def __ne__(self, other):
        '''x.__ne__(y) <==> x != y'''

        return self.__eq__(other) == False

    def isInAlarm(self):

        return bool(self.alarms)

def test():
    import mpcsutil.channel
    def _equality():
        chanval1 = mpcsutil.channel.ChanVal(
            eventTime='1982-01-02T12:43:21',
            sclk='0000054321-01234',
            scet='1999-10-21T01:02:32',
            lst='SOL-9745M22:16:54.217',
            ert='2000-11-11T10:10:10',
            channelId='A-0001',
            type='unsigned_int',
            name="chanval1",
            module="acs",
            dnUnits='feet',
            euUnits='meters per second',
            dn='124',
            eu='99.22')

        chanval2 = mpcsutil.channel.ChanVal(
            eventTime='1982-01-02T12:43:21',
            sclk='0000054321-01234',
            scet='1999-10-21T01:02:32',
            lst='SOL-9745M22:16:54.217',
            ert='2000-11-11T10:10:10',
            channelId='A-0001',
            type='unsigned_int',
            name="chanval1",
            module="acs",
            dnUnits='feet',
            euUnits='meters per second',
            dn='124',
            eu='99.22')

        chanval3 = mpcsutil.channel.ChanVal(
            eventTime='1982-01-02T12:43:21',
            sclk='0000054321-01234',
            scet='1999-10-21T01:02:32',
            lst='SOL-9745M22:16:54.217',
            ert='2000-11-11T10:10:10',
            channelId='A-0201',
            type='unsigned_int',
            name="chanval2",
            module="acs",
            dnUnits='feet',
            euUnits='meters per second',
            dn='1223',
            eu='991.23')
        print(chanval1)
        print(chanval2)
        print(chanval3)

    def _bool_status():
        csvStringTemplate = '''chan\,2007-07-13T23:24:40.143\,0\,0188878425-079\,0\,0\,123\,2005-12-26T14:20:26.127\,0\,0\,2005-12-26T14:10:12.063\,0\,0\,SOL-9745M22:16:54.217\,818720214217\,True\,S-4001\,BOOLEAN\,name1\,module1\,\,\,{status}\,11188\,\,0\,\,high\,RED\,low\,RED'''
        [print('Status is False: {}'.format(mpcsutil.channel.ChanVal(csvString=csvStringTemplate.format(status=statusString)).status)) for statusString in ['n', 'no', 'f', 'false']]
        [print('Status is True: {}'.format(mpcsutil.channel.ChanVal(csvString=csvStringTemplate.format(status=statusString)).status)) for statusString in ['y', 'yes', 't', 'true']]
        [print('Status is {}: {}'.format(statusString, mpcsutil.channel.ChanVal(csvString=csvStringTemplate.format(status=statusString)).status)) for statusString in ['ON', 'OFF', 'blah', 'something']]

    def _csv():
        csvString = '''chan\,2007-07-13T23:24:40.143\,0\,0188878425-079\,188878425\,79\,811226658294988879\,2005-12-26T14:20:26.127\,0\,0\,2005-12-26T14:10:12.063\,0\,0\,SOL-9745M22:16:54.217\,818720214217\,True\,S-4001\,UNSIGNED_INT\,name1\,module1\,\,\,\,11188\,   -0.999032\,0\,0\,high\,RED\,low\,RED'''
        _chan = mpcsutil.channel.ChanVal(csvString=csvString)
        print('eventTime is "2007-07-13T23:24:40.143" :: {}'.format(_chan.eventTime))
        print('alarms is [(\'high\',\'RED\'),(\'low\',\'RED\')] :: {} ({}) :: equal? {}'.format(_chan.alarms, type(_chan.alarms), ([('high','RED'),('low','RED')] == _chan.alarms)))
        print("vcidMapping is 'A' :: {} :: equal? {}".format(_chan.vcidMapping, ('A' == _chan.vcidMapping)))
        _ascsv=_chan.toCsv()
        print('CSV:\n{}\n'.format(_ascsv))
        _chan2 = mpcsutil.channel.ChanVal(csvString=_ascsv)
        print('Round trip ::\n\t{}\n\t\t==\n\t{}\n\t??\n\t{}\n\n'.format(_chan, _chan2, (_chan==_chan2)))


    def _repr():
        chanval1 = mpcsutil.channel.ChanVal(
            eventTime='1982-01-02T12:43:21',
            eventTimeExact='123',
            sclk='0000054321-01234',
            sclkCoarse='234',
            sclkFine='324',
            sclkExact='2342342',
            scetNano='241421',
            scet='1999-10-21T01:02:32',
            scetExact='234378',
            lst='SOL-9745M22:16:54.217',
            lstExact='818720214217',
            ert='2000-11-11T10:10:10',
            ertExact='32748',
            ertExactFine='324',
            channelId='A-0001',
            type='unsigned_int',
            name="chanval1",
            module="acs",
            dnUnits='feet',
            euUnits='meters per second',
            dn='124',
            eu='99.22',
            realtime='True')

        chanval2 = eval(repr(chanval1))
        print('Round trip ::\n\t{}\n\t\t==\n\t{}\n\t??\n\t{}\n\n'.format(chanval1, chanval2, (chanval1==chanval2)))

    def _scet_ext():
        chan=mpcsutil.channel.ChanVal(
            receiveTime=1555365530.37,
            eventTime="2019-105T21:58:34.001",
            eventTimeExact=1555365514001,
            sclk="0457509443.12500",
            sclkCoarse=457509443,
            sclkFine=8192,
            sclkExact=29983338864640,
            ert="2019-105T21:58:34.000",
            ertExact=1555365514000,
            ertExactFine=0,
            scet="2014-182T18:08:55.778",
            scetExact=1404238135778,
            scetNano=90683,
            lst="",
            lstExact=0,
            channelId="SAPP-0102",
            type="FLOAT",
            name="SAPP_POSITION_Y",
            module="sapp",
            dn="0.0",
            dnUnits="m",
            eu=0.0,
            euUnits="m",
            status="None",
            realtime=True,
            alarms=[],
            injected=False,
            dssId=0,
            vcidMapping="")
        print('{}'.format(chan))
        chan=mpcsutil.channel.ChanVal(
            receiveTime=1555365530.37,
            eventTime="2019-105T21:58:34.001",
            eventTimeExact=1555365514001,
            sclk="0457509443.12500",
            sclkCoarse=457509443,
            sclkFine=8192,
            sclkExact=29983338864640,
            ert="2019-105T21:58:34.000",
            ertExact=1555365514000,
            ertExactFine=0,
            scet="2014-182T18:08:55.778",
            scetExact=1404238135778,
            scetNano=0,
            lst="",
            lstExact=0,
            channelId="SAPP-0102",
            type="FLOAT",
            name="SAPP_POSITION_Y",
            module="sapp",
            dn="0.0",
            dnUnits="m",
            eu=0.0,
            euUnits="m",
            status="None",
            realtime=True,
            alarms=[],
            injected=False,
            dssId=0,
            vcidMapping="")
        print('{}'.format(chan))

    _equality()
    _bool_status()
    _csv()
    _repr()
    _scet_ext()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
