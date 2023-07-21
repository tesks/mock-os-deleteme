#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module does all the version checking and sets up all of the
necessary variables for all MPCS Python tools.

Added a check to see if environment variable CHILL_GDS_BUILD is set. If it is, DO NOT check mpcs version.
"""

from __future__ import (absolute_import, division, print_function)

import getpass
import logging
import logging.config
import optparse
import os
import os.path
import sys
import time
import traceback
import platform # MPCS-6965 - 02/02/2015
import re
import collections
# MPCS-12210 8/12/21 - backwards compatible collections import with python 2
try:
    from collections.abc import Mapping
except ImportError:
    from collections import Mapping

import simplejson as json
from mpcsutil import config

__tool__ = 'MPCSTools'
__version__ = "9.0.0.0"

#Set the required version of Python need by MPCS Python
# MPCS-6965 - 12/7/2014: mpcstools is now on Python 2.7, and
# it is known that OS X is incompatible with Python <2.7.6 due to the select.poll module.
# Require 2.7.6 or higher (Python 2 only)
# MPCS-6965 (rework) - 02/02/2015 Now checks for 2.7.1 for Linux, and 2.7.6 for Darwin
requiredPythonVersion = (2,7,6) if re.match('^.*(darwin).*$', platform.system(), flags=re.I) else (2,7,1)


_chillGdsEnvVar = 'CHILL_GDS'
chillGdsDirectory = ''
chillConfigDirectory = ''
chillMissionConfigDirectory = ''
_logConfigFileName = ''
logConfigFile = ''
_log = None
_homeDirEnvVar = 'HOME'
homedir = '/tmp'
#_userEnvVar = 'USER'
user = ''
userConfigDir = ''
# MPCS-10593 2/15/19: Made user dir case sensitive
_userConfigDirEnvVar = 'GdsUserConfigDir'
_releaseFileName = 'release.properties'
releaseFile = ''
chillBinDirectory = ''
chillAdminDirectory = ''
chillToolsDirectory = ''
chillInternalDirectory = ''
chillLibDirectory = ''
chillTemplateDirectory = ''
chillSchemaDirectory = ''
chillPythonLibDirectory = ''
chillImageDirectory = ''
_propertyDumpApp = 'chill_property_dump'
propertyDumpScript = ''
release = None
mpcsVersion = None
_logConfigFileName = 'python-logging.properties'

#The reason this is being done with a method instead of just in this module is so that
#we can unit test all the error cases within this method
def setupEnvironment():
    '''Set up all the global variables needed by MTAK & everything else'''

    global requiredPythonVersion, _chillGdsEnvVar, chillGdsDirectory, chillConfigDirectory, _logConfigFileName, logConfigFile, \
           _log, _homeDirEnvVar, homedir, _userEnvVar, user, userConfigDir, _userConfigDirEnvVar, _releaseFileName, \
           releaseFile, chillBinDirectory, chillAdminDirectory, chillToolsDirectory, chillTemplateDirectory, chillSchemaDirectory, chillInternalDirectory, chillLibDirectory, chillPythonLibDirectory, chillImageDirectory, \
           _propertyDumpApp, propertyDumpScript, release, mpcsVersion

    #########################################
    # Verify that we're using a compatible
    # Python version (greater than 2.7.1 for Linux, and 2.7.6 for Darwin) but matches the major version number)
    #########################################
    if tuple([ii for ii in sys.version_info if isinstance(ii, (int, float))]) < requiredPythonVersion:
        raise EnvironmentError('MTAK requires a Python version greater than or equal to {required}, but found the running Python version to be {current}'.format(required='.'.join('{}'.format(vv) for vv in requiredPythonVersion), current='.'.join('{}'.format(vv) for vv in  sys.version_info)))

    #MPCS-6965 - 02/03/2015: Also enforce 64-bit
    if not(sys.maxsize > 2**32):
        raise EnvironmentError('AMPCS requires 64-bit Python. Please ensure the correct Python is installed.')

    #Make sure that CHILL_GDS exists and set it

    try:
        chillGdsDirectory = os.environ.setdefault(_chillGdsEnvVar, os.path.abspath(os.path.join(os.path.dirname(__file__), '..','..'))).rstrip('/')
        # MPCS-6483 3/10/16: Removed trailing '/' from path
        # if chillGdsDirectory.endswith('/'):
        #     chillGdsDirectory = chillGdsDirectory[:-1]
    except KeyError as e:
        raise EnvironmentError('The required environment variable {} is undefined:\n{}'.format(_chillGdsEnvVar, traceback.format_exc()))

    # Set the CHILL bin directories
    chillBinDirectory = os.path.join(chillGdsDirectory, 'bin')
    chillConfigDirectory = os.path.join(chillGdsDirectory, 'config')
    chillImageDirectory = os.path.join(chillConfigDirectory, 'images')
    chillAdminDirectory = os.path.join(chillBinDirectory, 'admin')
    chillToolsDirectory = os.path.join(chillBinDirectory, 'tools')
    chillInternalDirectory = os.path.join(chillBinDirectory, 'internal')
    chillLibDirectory = os.path.join(chillGdsDirectory, 'lib')
    chillPythonLibDirectory = os.path.join(chillLibDirectory, 'python')
    chillTemplateDirectory = os.path.join(chillGdsDirectory, 'templates')
    chillSchemaDirectory = os.path.join(chillGdsDirectory, 'schema')

    #Get the value of the user's home directory
    homedir = os.path.expanduser('~')
    user = getpass.getuser()

    #########################################
    # Can't put these script names in the GDS
    # configuration file because we need them
    # in order to fetch the GDS configuration
    #########################################

    # The path to the default session configuration file
    userConfigDir = os.path.join(homedir, 'CHILL')
    try:
        userConfigDir = os.environ[_userConfigDirEnvVar]
        logOverrideMsg = 'The environment variable %s has been set! GDS User Config Directory set to %s' \
                      % (_userConfigDirEnvVar, userConfigDir)
    except KeyError:
        logOverrideMsg = 'The environment variable %s has not been set. GDS User Config Directory set to %s' \
                      % (_userConfigDirEnvVar, userConfigDir)

    # MPCS-8541 1/6/16: Check for logging override in user dir
    logConfigFile = os.path.join(userConfigDir, _logConfigFileName)

    if not os.path.isfile(logConfigFile):
        logOverrideMsg += "\nCannot find the log config '%s'. Searching system config directory %s" \
                          % (logConfigFile, chillConfigDirectory)

        logConfigFile = os.path.join(chillConfigDirectory, _logConfigFileName)

    if not os.path.isfile(logConfigFile):
        raise EnvironmentError('Cannot find the required file ' + logConfigFile)

    logging.addLevelName(100, 'MAXIMUM')
    logging.config.fileConfig(logConfigFile)
    _log = logging.getLogger('mpcs.util')

    logOverrideMsg += "\nUsing log config %s" % logConfigFile
    _log.debug(logOverrideMsg)

    #########################################
    # Verify that we're using a compatible
    # MPCS version (greater than the required one set at the top of this file)
    # IF WE ARE NOT DOING BUILD TESTS!!!!!!!!!
    #########################################

    # Find and set the chill_property_dump script
    propertyDumpScript = os.path.join(chillInternalDirectory, _propertyDumpApp)
    if not os.path.isfile(propertyDumpScript):
        errString = 'Cannot find the required script ' + propertyDumpScript
        _log.critical(errString)
        raise EnvironmentError(errString)

    # Set the release properties file
    releaseFile = os.path.join(chillConfigDirectory, _releaseFileName)
    # Grab the release properties file to get the version
    release = config.ReleaseProperties()
    #This will grab the version (e.g. 4.2.4) and split it into pieces as a list (e.g. ['4','2','4'])
    mpcsVersion = release.version.split('.')

    #Look through each digit in the version, strip out any non-numeric characters, and make the values
    #integers so we can do comparisons
    #(e.g. ['4','2','4a'] becomes [4,2,4])
    #
    #NOTE: Versions should not contain multiple numbers, so things like 4.2.11eg are ok, but
    #something like 4.2.1p2 is not

    for i in range(len(mpcsVersion)):
        strippedVersion = ''
        for c in mpcsVersion[i]:
            if c >= '0' and c <= '9':
                strippedVersion = strippedVersion + c
            else:
                break
        mpcsVersion[i] = int(strippedVersion)

    ## MPCS-7744 11/19/19: Remove mpcstools version check

    os.environ['TZ'] = 'UTC'
    time.tzset()

    _log.debug('User home directory is %s.' % (homedir))
    _log.debug('User logging file is %s.' % logConfigFile)
    _log.debug('Release properties file is %s.' % (releaseFile))
    _log.debug('MPCS configuration directory is %s.' % (chillConfigDirectory))
    _log.debug('MPCS bin directory is %s.' % (chillBinDirectory))
    _log.debug('MPCS admin directory is %s.' % (chillAdminDirectory))
    _log.debug('MPCS tools directory is %s.' % (chillToolsDirectory))
    _log.debug('MPCS internal directory is %s.' % (chillInternalDirectory))
    _log.debug('MPCS lib directory is %s.' % (chillLibDirectory))
    _log.debug('MPCS python lib directory is %s.' % (chillPythonLibDirectory))
    _log.debug('MPCS template directory is %s.' % (chillTemplateDirectory))
    _log.debug('MPCS schema directory is %s.' % (chillSchemaDirectory))
    _log.debug('MPCS property dump script is %s.' % (propertyDumpScript))


def NamedTuple(typename, field_names, default_values=(), rename=False, mro=None):
    """
        **Better Data Structure**
    """
    T = collections.namedtuple(typename, field_names, rename=rename)
    T.__new__.__defaults__ = ((None,) * len(T._fields))
    T.__new__.__defaults__ = tuple(T(**default_values) if isinstance(default_values, collections.Mapping) else T(*default_values))
    if mro:
        if isinstance(mro,type):
            return type(T.__name__, (mro, T), {})
            # return types.ClassType(T.__name__, (mro, T), {})
        else:
            return type(mro, (T,), {})
            # return types.ClassType(mro, (T,), {})
    return T


def get_logger(): return logging.getLogger('mpcs.util')


def _isTrueValue(inValue):
    return bool(re.match(r'^\s*(t[rue]*|y[es]*)\s*$', '{}'.format(inValue), flags=re.I))
def _isFalseValue(inValue):
    return bool(re.match(r'^\s*(f[alse]*|n[o]*)\s*$', '{}'.format(inValue), flags=re.I))

def getBooleanFromString(input):
        '''Convenience function to convert a string value such as
        "true" or "TRUE" to a Python boolean with the value True.

        Args
        -----
        input - A string boolean value.  The following values will evaluate
        to True (case insensitive): "true","t","yes","y", or a non-zero number (string)
        The following values will evaluate to false: 'false', 'f', 'no', 'n', or a zero number

        Returns
        --------
        Will return True if the input equals a true case (see above), False otherwise (boolean)'''

        # MPCS-8016 - 2/29/2016: Split out true / false string determination into separate functions

        inValue = str(input).lower().strip()

        def _as_bool():
            if re.match(r'^[-+]?[\d]*?\.?[\d]*[-+eE]*[\d]*$',inValue):
                return bool(float(inValue))
            elif re.match(r'^\d+$', inValue):
                return bool(int(inValue))
            elif re.match(r'^\s*(f[alse]*|n[o]*)\s*$', inValue, flags=re.I):
                return False
            else:
                return bool(re.match(r'^\s*(t[rue]*|y[es]*)\s*$', inValue, flags=re.I))

        result = _as_bool() if inValue else None

        get_logger().debug('getBooleanFromString({}) returned {}'.format(input,str(result)))

        return result

setupEnvironment()

MAX_PORT_NUMBER = 2**16


def get_script_name():
    '''Retrieve the name of the currently running script (ignore the path portion)'''
    return os.path.splitext(os.path.basename(sys.argv[0]))[0]

def get_version():
    '''Generate the command line version string for the current running application.'''

    try:
        rp = config.ReleaseProperties()
        return '{} {} Multi-Mission Core Version: {} ({} Version: {})'.format(rp.productLine,get_script_name(),rp.version,__tool__,__version__)
    except:
        return 'Error retrieving the version information for {}'.format(get_script_name())


def create_option_parser(usageText=None):
    '''Create an option parser for the currently running application.'''
    if usageText is None:
        usageText = '%s [options]' % (get_script_name())

    parser = optparse.OptionParser(usage=usageText)
    parser.version = get_version()

    #Version option (help option is auto-handled by optparse)
    #We don't let optparse auto-handle version because it only supports --version, not -v
    parser.add_option("-v","--version",action="version",help="Show program's version number and exit.")

    return parser


class ChillBase(Mapping):
    def __init__(self, *args, **kwargs):
        [self.__setattr__(kk,vv) for kk,vv in kwargs.items()]

    def __call__(self, *args, **kwargs):
        [self.__setattr__(kk,vv) for kk,vv in kwargs.items()]
        return self

    def __getattr__(self, name):
        return self.__dict__.get(name, None)

    def __getitem__(self, key):
        return self.__dict__.get(key, None)

    def __iter__(self):
        for _iter in self.dict:
            yield _iter

    def __len__(self):
        return len(self.dict)

    def __format__(self, spec):
        return '{}'.format(self._asjson())

    def __getstate__(self):
        return self._asdict()

    def __setstate__(self, state):
        self.__dict__ = state

    def __deepcopy__(self, memo):
        cls = self.__class__
        result = cls.__new__(cls)
        memo[id(self)] = result
        from copy import deepcopy
        for k, v in self.__dict__.items():
            setattr(result, k, deepcopy(v, memo))
        return result

    def _asdict(self):
        _props = list(filter(lambda x: isinstance(getattr(self.__class__,x),property), dir(self.__class__)))
        _keys = list(filter(lambda x: not(x.startswith('_')), self.__dict__.keys()))
        _dd=collections.OrderedDict([(kk,getattr(self, kk)) for kk in _keys + _props if kk not in self._ignore])
        __dd=collections.OrderedDict([(kk,(lambda x:x._asdict() if hasattr(x,'_asdict') else (x.name if hasattr(x, 'name') else x))(getattr(self, kk))) for kk in sorted(_dd.keys()) if (not(kk.startswith('_')) and (getattr(self, kk) is not(None)))])
        return __dd
    def _asjson(self):
        return json.dumps(self._asdict(), indent=4)
    def __format__(self, spec): return '{}'.format(self._asjson())
    def makedirs(self, *dirs):
        _umask = os.umask(0)
        try:
            [os.makedirs(_dir, 0o0775) for _dir in dirs if (_dir and not(os.path.exists(_dir)))]
        finally:
            os.umask(_umask)
            return dirs[0] if (len(dirs)==1) else dirs

    @property
    def dict(self):
        '''This instance as a `collections.OrderedDict` '''
        return self._asdict()
    @property
    def json(self):
        '''This instance as json'''
        return self._asjson()
    @property
    def _ignore(self):
        '''Instance attributes to ignore'''
        return ['dict', 'json']


class TelemetryItem(ChillBase):
    def get_plot_label(self): return ''

#Import the rest of the mpcsutil modules (this way if the user has "import mpcsutil" at the
#top of their script, they'll get everything)
# MPCS-6999 09/27/16 - import err first, allows the exceptions to be called from any other mpcsutil class
# MPCS-9813 08/29/18 Josh Choi - import cfdp
# try:
#     import err, channel, command, database, enum, evr, logutil, filesystem, product, cfdp, search, timeutil
# except (ImportError, ModuleNotFoundError):
    # from mpcsutil import err, channel, command, database, enum, evr, logutil, filesystem, product, cfdp, search, timeutil)

from mpcsutil import (err, channel, command, collection, database)
from mpcsutil import MpcsEnum as enum
from mpcsutil import (evr, logutil, filesystem, product, cfdp, search, timeutil, ColorString)


__all__ = ['err', 'channel','collection','command','config','database','enum','evr',
           'logutil','filesystem','product','cfdp','search','timeutil', 'ColorString']

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
