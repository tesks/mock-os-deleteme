#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains data structures representing responses from the
Command Preparation and Delivery (CPD) system

THIS MODULE IS CONTROLLED BY THE AUTO SIS (D-001008_MM_AUTO_SIS)
Any modifications to this module must be reflected in the SIS
"""

from __future__ import (absolute_import, division, print_function)
import collections, json, re
import six


class CpdResponse(object):
    '''
    This class encapsulates a response message from the CPD server
    '''
    message = property(
        lambda x:x.__dict__.setdefault('_message',None),
        lambda x,y: x.__dict__.update({'_message': y}),
        doc='CPD Response Message')
    def __getattr__(self, name): return self.__dict__.get(name, None)
    def __init__(self, message=None, **kwargs):
        '''
        Initialize the object

        Args
        -----
        message - the CPD response message
        '''
        self.message = message
    def _asdict(self):
        return collections.OrderedDict([ (_prop, getattr(self, _prop)) for _prop in filter(lambda x: isinstance(getattr(self.__class__,x),property), sorted(dir(self.__class__)))])
    def __format__(self, spec):
        '''
        Get the formatted string representation of this object
        '''
        _dict=self._asdict()
        _dict = collections.OrderedDict([(kk,_dict[kk]) for kk in sorted(_dict.keys()) if re.match(r'^([^_]+)$', kk)])
        if re.match(r'^.*json.*$', spec, flags=re.I): return json.dumps(_dict, indent=4)
        return ', '.join("{}: {}".format( getattr(self.__class__, _prop).__doc__, _prop_val) for _prop, _prop_val in _dict.items())
    def __repr__(self): return '{}'.format(self)
    def __str__(self): return self.__repr__()

class CpdConnectionStatus(CpdResponse):
    '''
    This class encpasulates the Connection Status of the CPD Server
    '''
    connection_status = property(
        lambda x:x.__dict__.setdefault('_connection_status',None),
        lambda x,y: x.__dict__.update({'_connection_status': y}),
        doc='CPD Connection Status')
    connected_station = property(
        lambda x:x.__dict__.setdefault('_connected_station',None),
        lambda x,y: x.__dict__.update({'_connected_station': y}),
        doc='CPD Connected Station')
    def __init__(self, message=None, connection_status=None, connected_station=None, **kwargs):
        '''
        Initialize the object

        Args
        -----
        message - the CPD response message
        connectionStatus - the connection status of the CPD server
        connectedStation - the station that is connected to the CPD server, if applicable
        '''
        super(CpdConnectionStatus, self).__init__(message)
        [self.__setattr__(_name, _value) for _name, _value in zip(['connection_status', 'connected_station'], [connection_status, connected_station])]

CpdConnectionStatus.connectionStatus=CpdConnectionStatus.connection_status
CpdConnectionStatus.connectedStation=CpdConnectionStatus.connected_station

class CpdConfiguration(CpdResponse):
    '''
    This class encapsulates the Configuration of the CPD server
    '''

    preparation_state = property(
        lambda x:x.__dict__.setdefault('_preparation_state',None),
        lambda x,y: x.__dict__.update({'_preparation_state': y}),
        doc='CPD Preparation State')
    execution_mode = property(
        lambda x:x.__dict__.setdefault('_execution_mode',None),
        lambda x,y: x.__dict__.update({'_execution_mode': y}),
        doc='CPD Execution Mode')
    execution_method = property(
        lambda x:x.__dict__.setdefault('_execution_method',None),
        lambda x,y: x.__dict__.update({'_execution_method': y}),
        doc='CPD Execution Method')
    execution_state = property(
        lambda x:x.__dict__.setdefault('_execution_state',None),
        lambda x,y: x.__dict__.update({'_execution_state': y}),
        doc='CPD Execution State')
    aggregation_method = property(
        lambda x:x.__dict__.setdefault('_aggregation_method',None),
        lambda x,y: x.__dict__.update({'_aggregation_method': y}),
        doc='CPD Aggregation Method')

    def __init__(self, message=None, preparation_state=None, execution_mode=None, execution_method=None, execution_state=None, aggregation_method=None, **kwargs):
        '''
        Initialize the object
        Args
        -----
        message - the CPD response message
        preparationState - the CPD server's current preparation state
        executionMode - the CPD server's current execution mode
        executionMethod - the CPD server's current execution method
        executionState - the CPD server's current execution state
        aggregationMethod - the CPD server's current aggregation method
        '''
        super(CpdConfiguration, self).__init__(message)
        [self.__setattr__(_name, _value) for _name, _value in zip(['preparation_state', 'execution_mode', 'execution_method', 'execution_state', 'aggregation_method'], [preparation_state, execution_mode, execution_method, execution_state, aggregation_method])]

CpdConfiguration.preparationState=CpdConfiguration.preparation_state
CpdConfiguration.executionMode=CpdConfiguration.execution_mode
CpdConfiguration.executionMethod=CpdConfiguration.execution_method
CpdConfiguration.executionState=CpdConfiguration.execution_state
CpdConfiguration.aggregationMethod=CpdConfiguration.aggregation_method

def test(*args, **kwargs):
    cr=CpdResponse('test message')
    print('Test CpdResponse:\n\t{uut}\n{uut:json}\n'.format(uut=cr))

    ccs=CpdConnectionStatus('test message', 'ACTIVE', 'Test Station')
    print('Test CpdResponse:\n\t{uut}\n{uut:json}\n'.format(uut=ccs))

    cc = CpdConfiguration('test message','test preparation state', 'test execution mode', 'test execution method', 'test execution state', 'test aggregation method')
    print('Test CpdConfiguration:\n\t{uut}\n{uut:json}\n'.format(uut=cc))

    print(CpdResponse('OK'))
    print(CpdConnectionStatus('test message', 'ACTIVE', 'Test Station'))
    print(CpdConfiguration('test message','test preparation state', 'test execution mode', 'test execution method', 'test execution state', 'test aggregation method'))


def main(*args, **kwargs):
    return test(*args, **kwargs)

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
