#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains common exceptions that will be used by the
AMPCS Utility Toolkit for Operations (AUTO).

THIS MODULE IS CONTROLLED BY THE AUTO SIS (D-001008_MM_AUTO_SIS)
Any modifications to this module must be reflected in the SIS
"""

from __future__ import (absolute_import, division, print_function)
import six
import re


def get_error(statusCode, message=None):
    """
    Get the error object given a status code.

    Args
    -----
    statusCode - the AMPCS error code

    message - the AMPCS response message to include in the error object

    Returns
    --------
    The error object associated with the specified error code.
    """



    _ctx = dict()

    if message:
        _ctx.update(dict(message=message))

    if isinstance(statusCode, six.string_types):
        statusCode=statusCode.strip()
        statusCode = int(statusCode) if re.match(r'^[\d]+$', statusCode) else None

    def _get_error():
        if isinstance(statusCode, int):
            _exc, _message = _status_errors.get(statusCode, (AmpcsError,"AMPCS encountered an error"))
            if _exc:
                return _exc(_ctx.setdefault('message', _message if _message else "AMPCS encountered an error"))

    return None if statusCode in [None, 0] else _get_error()


def get_error_from_http_status(statusCode, message=None):
    '''
    Get the error object given a status code.

    Args
    -----
    statusCode - the HTTP status code

    message - the message to include in the error object

    Returns
    --------
    The error object associated with the specified error code.
    '''

    _ctx = dict()

    if message:
        _ctx.update(dict(message=message))

    if isinstance(statusCode, six.string_types):
        statusCode=statusCode.strip()
        statusCode = int(statusCode) if re.match(r'^[\d]+$', statusCode) else None

    def _get_error():
        if isinstance(statusCode, int):
            _exc, _message = _http_errors.get(statusCode, (None,None))
            if _exc:
                return _exc(_ctx.setdefault('message', _message))
            if statusCode >= 400:
                return AUTOError(_ctx.setdefault('message', "The AUTO API encountered an error"))
            return UnexpectedStatus()

    return None if statusCode in [200] else _get_error()

class AmpcsError(Exception):
    '''AmpcsError is the base exception for all of the exceptions that
    are raised by Python code in AUTO resulting from an exception raised
    by AMPCS'''

    def __init__(self, message=None):
        '''constructor

            message = the string description of the exception (string)'''
        super(AmpcsError, self).__init__(message)

class AuthenticationError(AmpcsError):
    '''AuthenticationError is the exception that is generated when an action is
    taken without proper authentication (unknown user)'''

    pass

class AuthorizationError(AmpcsError):
    '''AuthorizationError is the exception that is generated when an action is
    taken without the proper permissions (known user, but insufficient privileges)'''

    pass

class CommandServiceError(AmpcsError):
    '''CommandServiceError is the exception that is generated when the command
    service accepted a request, but could not complete it due to an error
    state within the command service'''

    pass

class ConnectionError(Exception):
    '''ConnectionError is the exception that is generated when AMPCS or AUTO fails to connect
    to the specified uplink host or to the Global LAD service'''

    pass

class AUTOError(Exception):
    '''AUTOError is the base exception for all of the exceptions that
    are raised by Python code in AUTO'''

    def __init__(self, message=None):
        '''constructor

            message = the string description of the exception (string)'''
        super(AUTOError, self).__init__(message)

class UnexpectedStatus(Exception):
    '''Raised when an unexpected status from the uplink proxy is encountered'''

    def __init__(self, message=None):
        '''constructor

            message = the string description of the exception (string)'''
        super(UnexpectedStatus, self).__init__(message)

_status_errors={
    40: (AmpcsError, "AMPCS encountered an error parsing proxy command-line options"),
    41: (AuthenticationError, "Unable to authenticate user"),
    43: (AuthorizationError, "The user/role is not authorized to perform the action"),
    50: (AmpcsError, "AMPCS encountered an error"),
    52: (CommandServiceError, "The command service encountered an error"),
    54: (ConnectionError, "Unable to contact the command service")}

_http_errors={
    400: (AUTOError, "The AUTO API encountered an error"),
    401: (AuthenticationError, "Unable to authenticate user"),
    403: (AuthorizationError, "The user/role is not authorized to perform the action"),
    404: (AUTOError, "No mapping found for HTTP request"),
    500: (AmpcsError, "AMPCS encountered an error"),
    502: (CommandServiceError, "The command service encountered an error"),
    504: (ConnectionError, "Unable to contact the command service")}

# def test_section(fnc):
#     _pedigree=[__package__, __module__, 'test', fnc.__name__]
#     _log=logging.getLogger('.'.join(_pedigree[:-1]))
#     def wrap(*args, **kwarg):
#         _buff='-'*80
#         _header=[_buff, '# {}{}'.format('.'.join(_pedigree), ' :: {}'.format(fnc.__doc__) if hasattr(fnc, '__doc__') else ''), _buff]
#         _log.info('\n'.join(_header))



def test():

    def _status_error():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Status Errors'))
        def _get(key, msg=None):
            _exc = get_error(key, msg)
            return '{}({})'.format(_exc.__class__.__name__, _exc) if _exc else _exc

        print('\n\t## From Integer Key')
        [print('\t\tError for Return Code "{}": {}'.format(kk, _get(kk))) for kk in _status_errors.keys()]
        print('\n\t## From String Key')
        [print('\t\tError for Return Code "{}": {}'.format(' {} '.format(kk), _get( ' {} '.format(kk)))) for kk in _status_errors.keys()]
        print('\n\t## From Integer Key with Custom Message')
        [print('\t\tError for Return Code "{}": {}'.format(kk, _get(kk, 'Custom Message'))) for kk in _status_errors.keys()]
        print('\n\t## From String Key with Custom Message')
        [print('\t\tError for Return Code "{}": {}'.format(str(kk), _get(str(kk), 'Custom Message'))) for kk in _status_errors.keys()]
        print('\n\t## From None')
        print('\t\tError for Return Code "{}": {}'.format(None, _get(None)))
        print('\n\t## From Unknown Integer')
        print('\t\tError for Return Code "{}": {}'.format(9999, _get(9999)))
        print('\n\t## From String None')
        print('\t\tError for Return Code "{}": {}'.format(str(None), _get(str(None))))
        print('\n\t## From String Unknown Integer')
        print('\t\tError for Return Code "{}": {}'.format(str(9999), _get(str(9999))))

    def _http_error():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='HTTP Errors'))
        def _get(key, msg=None):
            _exc = _get_from_http_status(key, msg)
            return '{}({})'.format(_exc.__class__.__name__, _exc) if _exc else _exc
        print('\n\t## From Integer Key')
        [print('\t\tError for HTTP Code "{}": {}'.format(kk, _get(kk))) for kk in _http_errors.keys()]
        print('\n\t## From String Key')
        [print('\t\tError for HTTP Code "{}": {}'.format(' {} '.format(kk), _get( ' {} '.format(kk)))) for kk in _http_errors.keys()]
        print('\n\t## From Integer Key with Custom Message')
        [print('\t\tError for HTTP Code "{}": {}'.format(kk, _get(kk, 'Custom Message'))) for kk in _http_errors.keys()]
        print('\n\t## From String Key with Custom Message')
        [print('\t\tError for HTTP Code "{}": {}'.format(str(kk), _get(str(kk), 'Custom Message'))) for kk in _http_errors.keys()]
        print('\n\t## From None')
        print('\t\tError for HTTP Code "{}": {}'.format(None, _get(None)))
        print('\n\t## From Good Return')
        print('\t\tError for HTTP Code "{}": {}'.format(200, _get(200)))
        print('\n\t## From Unknown')
        print('\t\tError for HTTP Code "{}": {}'.format(202, _get(202)))
        print('\n\t## From Known Range')
        print('\t\tError for HTTP Code "{}": {}'.format(450, _get(450)))
        print('\n\t## From High Unknown')
        print('\t\tError for HTTP Code "{}": {}'.format(9999, _get(9999)))
        print('\n\t## From String None')
        print('\t\tError for HTTP Code "{}": {}'.format(str(None), _get(str(None))))
        print('\n\t## From String Good Return')
        print('\t\tError for HTTP Code "{}": {}'.format(str(200), _get(str(200))))
        print('\n\t## From String Unknown')
        print('\t\tError for HTTP Code "{}": {}'.format(str(202), _get(str(202))))
        print('\n\t## From String Known Range')
        print('\t\tError for HTTP Code "{}": {}'.format(str(450), _get(str(450))))
        print('\n\t## From String High Unknown')
        print('\t\tError for HTTP Code "{}": {}'.format(str(9999), _get(str(9999))))

    _status_error()
    _http_error()



def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
