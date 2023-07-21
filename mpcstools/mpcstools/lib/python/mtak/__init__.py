#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This is the base module for the MTAK package that sets up
some common objects/methods/configuration need by MTAK as a whole.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil
import sys
import tempfile
import time
import traceback
import types
import inspect
import re
import types
import importlib
import six
from six import StringIO


get_logger = lambda : logging.getLogger('mpcs.mtak')

def get_wrapper_method_names():
    """
    Get a list of strings that are the names of all the "public" methods
    defined in the MTAK wrapper module.
    """

    from mtak import wrapper
    return sorted([_member[0] for _member in filter(lambda x: x[0] if re.match(r'^[^_].*$', x[0]) and isinstance(x[1], types.FunctionType) else None, inspect.getmembers(importlib.import_module('mtak.wrapper') ) )])

def log_stack_trace():
    get_logger().error('{}'.format(traceback.format_exc()))

class DatabaseHandler(logging.Handler):
    """
    A handler class which writes logging records, appropriately formatted,
    to the MPCS database.  Modeled after the Python code for the built-in
    logging.StreamHandler class (by "modeled after" I mean copy/paste and
    change a few lines).
    """
    def __init__(self,proxy):
        """
        Initialize the handler.
        """
        logging.Handler.__init__(self)
        self.proxy = proxy
        self.formatter = None

    def emit(self, record):
        """
        Emit a record.

        If a formatter is specified, it is used to format the record.
        The record is then written to the database without a trailing newline
        If exception information is present, it is formatted using
        traceback.print_exception and appended to the stream.
        """

        try:
            msg = self.format(record).encode('utf-8')
            level = record.levelname
            self.proxy.sendLog(msg,level)
            # if not hasattr(types, "UnicodeType"): #if no unicode support...
            #     self.proxy.sendLog(msg,level)
            # else:
            #     try:
            #         self.proxy.sendLog(msg,level)
            #     except UnicodeError:
            #         self.proxy.sendLog(msg.encode("UTF-8"),level)
        except (KeyboardInterrupt, SystemExit):
            raise
        except:
            self.handleError(record)

    def format(self, record):
        """
        Format the specified record as text.  Call the base formatting method,
        but then replace all the newline characters so that the string will
        only be one line when it's queried out of the database.
        """

        s = logging.Handler.format(self,record)
        s = s.strip().replace('\n',self.proxy._newline)

        return s

class AbstractProxy(object):
    """
    The abstract superclass used by the MTAK uplink and downlink proxy objects. It takes
    care of common logging and configuration that is used by its subclasses.

    Object Attributes
    ------------------
    _sessionConfig - The session configuration being used by this run of MTAK. (mpcsutil.config.SessionConfig)
    """

    log = property(
        lambda x:x.__dict__.setdefault('_log', get_logger()),
        lambda x,y: x.__dict__.update({'_log':y}))

    def __init__(self,sessionConfig):
        """
        Initialize this proxy object

        Args
        -----
        sessionConfig - The session configuration used by this proxy (mpcsutil.config.SessionConfig)

        Returns
        --------
        None
        """

        #Make sure the input session configuration is actually a valid session
        if sessionConfig is None or not hasattr(sessionConfig,'key') or not sessionConfig.key:
            errString = 'Cannot create the proxy.  The input session config does not have a session key.'
            raise mpcsutil.err.InvalidInitError(errString)
        self._sessionConfig = sessionConfig
        self._readConfig()

        #Create the necessary loggers for this run of MTAK (this is the where the base logger gets initialized)
        AbstractProxy._createCoreLogHandlers(sessionConfig)

    def _readConfig(self):
        """
        Read all the GDS configuration values needed by this proxy object. This superclass
        method does nothing...it should be overridden by subclass implementations if they need configuration
        information.

        Args
        -----
        None

        Returns
        --------
        None
        """

        pass

    @classmethod
    def _createLogHeader(clazz,sessionConfig):
        """
        Create the header for the MTAK log file. This is a class method because it doesn't do
        anything specific to this object and also because it needs the "logFile" attribute that
        is actually stored on the class object.

        Args
        -----
        sessionConfig - The session configuration whose information will go into the log file header (mpcsutil.config.SessionConfig)

        Returns
        --------
        None
        """

        _buff='#'*28
        get_logger().info('\n'.join([_buff,
            'Script Startup Information',_buff,
            'MPCS Version = {}'.format( mpcsutil.config.ReleaseProperties().version ),
            'MPCSTools Version = {}'.format( mpcsutil.__version__ ),
            'CHILL_GDS = {}'.format( mpcsutil.chillGdsDirectory ),'',
            'Date/Time = {}'.format( mpcsutil.timeutil.getTimeString(seconds=time.time()) ),
            'Script name = {}'.format( sys.argv[0] if sys.argv[:1] else 'Python Interactive Interpreter' ),
            'Log name = {}'.format( clazz.logFile ),'',
            'Username = {}'.format( mpcsutil.user ),
            'Host = {}'.format( sessionConfig.host ),
            'Session ID = {}'.format( sessionConfig.key ),_buff]))

    def start(self):

        pass

    def stop(self):

        pass

    def isRunning(self):

        return False

    def getSummary(self):
        """
        Return a string giving the summary of activity done by this proxy. This superclass implementation
        does nothing.  Subclasses should override this method to provide their own summary output.

        Args
        -----
        None

        Returns
        --------
        The summary of activity done by this proxy (to be written to the log file) (string)
        """

        return ''

    @classmethod
    def _createCoreLogHandlers(clazz,sessionConfig):
        """
        Method to create the MTAK base file logger. This is a class method because it is not specific
        to the proxy that calls it and because the class object ("clazz") will be used to store the location
        of the log file so that other proxies may write to the same log file.

        Args
        -----
        sessionConfig - The sessionConfig whose information will be used to determine where the log file is written (mpcsutil.config.SessionConfig)

        Returns
        --------
        None
        """
        _log = get_logger()
        gdsConfig = mpcsutil.config.GdsConfig()

        logFilePrefix = gdsConfig.getProperty('automationApp.mtak.logging.file.base.logFilePrefix',default='mtak')
        logFileSuffix = gdsConfig.getProperty('automationApp.mtak.logging.file.base.logFileSuffix',default='.log')
        fileLevel = logging.getLevelName(gdsConfig.getProperty('automationApp.mtak.logging.file.base.level',default=logging.getLevelName(logging.INFO)))
        fileFormat = gdsConfig.getProperty('automationApp.mtak.logging.file.base.recordFormat',default='[%(levelname)s @ %(asctime)s]: %(message)s')
        datetimeFormat = gdsConfig.getProperty('automationApp.mtak.logging.file.base.fileTimeFormat','%Y_%m_%d_%H_%M_%S')
        consoleLevel = logging.getLevelName(gdsConfig.getProperty('automationApp.mtak.logging.console.base.level',default=logging.getLevelName(logging.INFO)))
        consoleFormat = gdsConfig.getProperty('automationApp.mtak.logging.console.base.recordFormat',default='[%(levelname)s @ %(asctime)s]: %(message)s')
        fileEnable = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.mtak.logging.file.base.enable', default='false'))
        consoleEnable = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.mtak.logging.console.base.enable', default='false'))

        # Remove any existing handlers or else multiple logs are generated
        if _log.handlers is not None:
            _log.handlers = []

        if not hasattr(clazz,'logConsole'):

            if not _log.disabled and consoleEnable:
                errMsg = None
                #Try to create logs in the following places (in order)
                handler = None
                try:
                    handler = logging.StreamHandler(sys.stderr)
                except IOError:
                    errMsg = ('Could not write the MTAK Core log to the console')

                if handler is not None:

                    #Once we get the log file handler created, add it to our logger
                    #(If the handler has a lower logging level than the logger...update the logger
                    #to have the same level...otherwise you won't see any of the lower level stuff
                    #out of the handler)
                    handler.setLevel(consoleLevel)
                    if _log.getEffectiveLevel() > handler.level:
                        _log.setLevel(handler.level)

                    handler.setFormatter(logging.Formatter(consoleFormat,mpcsutil.timeutil.isoFmt))
                    _log.addHandler(handler)
                    clazz.logConsole = True

                    if errMsg is not None:
                        _log.critical(errMsg)

        #If the log file has already been established for this MTAK run, just return so we
        #don't create it again
        if not hasattr(clazz,'logFile'):
            errMsg = None
            filename = None
            if not _log.disabled and fileEnable:

                #Get all the properties for the base logger from the config file
                #Setup the file handler for the MTAK wrapper logger
                datetimeValue = mpcsutil.timeutil.getTimeString(seconds=time.time(),format=datetimeFormat)
                filename = '%s_%s_%s%s' % (logFilePrefix,mpcsutil.user,datetimeValue,logFileSuffix)

                #Try to create logs in the following places (in order)
                logDirs = [sessionConfig.logDir,sessionConfig.outputDirectory,mpcsutil.homedir,tempfile.gettempdir()]
                handler = None

                for dir in logDirs:

                    if not dir:
                        continue

                    try:
                        handler = logging.FileHandler('%s/%s' % (dir,filename),'w')
                        sessionConfig.logDir = dir
                        break
                    except IOError:
                        errMsg = ('Could not write the MTAK Core log file to disk in the directory %s. ' % (dir) +
                                   'You may want to supply the "logDir" parameter to your "startup()" call. ' +
                                   'Attempting to write logs elsewhere...')

                if not handler:

                    errMsg += ('\nCould not write MTAK Core log file anywhere on disk. Logging will only be done to the console.')
                    handler = logging.StreamHandler(sys.stdout)
                    sessionConfig.logDir = None
                    filename = 'stdout'

                #Once we get the log file handler created, add it to our logger
                #(If the handler has a lower logging level than the logger...update the logger
                #to have the same level...otherwise you won't see any of the lower level stuff
                #out of the handler)
                handler.setLevel(fileLevel)
                if _log.getEffectiveLevel() > handler.level:
                    _log.setLevel(handler.level)

                handler.setFormatter(logging.Formatter(fileFormat,mpcsutil.timeutil.isoFmt))
                _log.addHandler(handler)

                #Tell the user where to find their log file (if there is one)
                if filename != 'stdout':
                    logFile = '%s/%s' % (sessionConfig.logDir,filename)
#                    print ''
#                    print 'MTAK Core log messages being written to %s' % (logFile)
#                    print ''

                    clazz.logFile = logFile
                else:
                    clazz.logFile = filename

                if errMsg is not None:
                    _log.critical(errMsg)
                _log.debug("Writing to log file %s" % clazz.logFile)
                #Now that we've created the log file, go ahead and write the header to it
                AbstractProxy._createLogHeader(sessionConfig)

        #logging must be disabled if clazz.logFile is not set...so we can really just return whatever we want
        return clazz.logFile if hasattr(clazz,'logFile') else ''

    @classmethod
    def _destroyCoreLogHandlers(clazz):
        """
        Desotry the MTAK file logger that was created initially.  This method is
        not complete at the moment...it does not destroy the actual handler, it only removes the
        log file name from the class object. It is a class method because the name of the log file is actually
        stored on the class object.

        Args
        -----
        None

        Returns
        --------
        None
        """

        #More importantly how are we going to generate summaries at the end of the log
        #in the case of the base logger?
        if hasattr(clazz,'logFile'):
            del clazz.logFile

        if hasattr(clazz,'logConsole'):
            del clazz.logConsole

        for handler in get_logger().handlers:
            handler.flush()
            #If we remove all of these, we'll have some later log messages that will cause
            #printouts saying "No log handler can be found for mpcs.mtak" so let's not remove them,
            #it won't harm anything
            #_log.removeHandler(handler)

#####################################
#Set up the global properties that are used by MTAK wait conditions
#####################################
defaultWaitLookback = 0
defaultWaitTimeout = 60

def _setGlobalProperties():

    global defaultWaitLookback, defaultWaitTimeout

    gdsConfig = mpcsutil.config.GdsConfig()
    defaultWaitLookback = gdsConfig.getProperty('automationApp.mtak.wait.defaultLookback',default=0)
    defaultWaitTimeout = gdsConfig.getProperty('automationApp.mtak.wait.defaultTimeout',default=60)

_setGlobalProperties()

def getReceiveChannelIds():
    """
    Returns the channel ids that MTAK is interested in receiving

    Args
    -----
    None

    Returns
    --------
    A list of channel ids (comma separated or a range). (String)
    """

    get_logger().debug('mpcsutil.config.getReceiveChannelIds()')

    return mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveChannelIds')

def setReceiveChannelIds(receiveChannelIds):
    """
    Sets the channel ids that MTAK is interested in receiving

    Args
    -----
    receiveChannelIds - A list of channel ids (comma separated or a range). (String)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveChannelIds()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveChannelIds',receiveChannelIds)

def getReceiveModules():
    """
    Returns the modules that have data that MTAK is interested in receiving

    Args
    -----
    None

    Returns
    --------
    A list of comma separated modules. (String)
    """

    get_logger().debug('mpcsutil.config.getReceiveModules()')

    return mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveModules')

def setReceiveModules(receiveModules):
    """
    Sets the modules that have data that MTAK is interested in receiving

    Args
    -----
    receiveModules -  A list of comma separated modules. (String)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveModules()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveModules',receiveModules)

def getReceiveSubsystems():
    """
    Returns the subsystems that have data that MTAK is interested in receiving

    Args
    -----
    None

    Returns
    --------
    A list of comma separated subsystems. (String)
    """

    get_logger().debug('mpcsutil.config.getReceiveSubsystems()')

    return mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveSubsystems')

def setReceiveSubsystems(receiveSubsystems):
    """
    Sets the subsystems that have data that MTAK is interested in receiving

    Args
    -----
    receiveSubsystems -  A list of comma separated subsystems. (String)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveSubsystems()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveSubsystems',receiveSubsystems)

def getReceiveOpsCategories():
    """
    Returns the operational categories that have data that MTAK is interested in receiving

    Args
    -----
    None

    Returns
    --------
    A list of comma separated operational categories. (String)
    """

    get_logger().debug('mpcsutil.config.getReceiveOpsCategories()')

    return mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveOpsCategories')

def setReceiveOpsCategories(receiveOpsCategories):
    """
    Sets the operational categories that have data that MTAK is interested in receiving

    Args
    -----
    receiveModules -  A list of comma separated operational categories. (String)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveOpsCategories()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveOpsCategories',receiveOpsCategories)

def getReceiveEha():
    """
    Check whether or not MTAK is setup to receive EHA.

    Args
    -----
    None

    Returns
    --------
    True if EHA should be processed by MTAK, false otherwise. (Boolean)
    """

    get_logger().debug('mpcsutil.config.getReceiveEha()')

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveEha',True))

def setReceiveEha(receiveEha):
    """
    Set whether or not MTAK should receive EHA.

    Args
    -----
    receiveEha - Boolean indicating whether or not MTAK should receive EHA. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveEha()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveEha',mpcsutil.getBooleanFromString(receiveEha))

def getReceiveEvrs():
    """
    Check whether or not MTAK is setup to receive EVRs.

    Args
    -----
    None

    Returns
    --------
    True if EVRs should be processed by MTAK, false otherwise. (Boolean)
    """

    get_logger().debug('mpcsutil.config.getReceiveEvrs()')

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveEvrs',True))

def setReceiveEvrs(receiveEvrs):
    """
    Set whether or not MTAK should receive EVRs.

    Args
    -----
    receiveEvrs - Boolean indicating whether or not MTAK should receive EVRs. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveEvrs()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveEvrs',mpcsutil.getBooleanFromString(receiveEvrs))

def getReceiveProducts():
    """
    Check whether or not MTAK is setup to receive products.

    Args
    -----
    None

    Returns
    --------
    True if products should be processed by MTAK, false otherwise. (Boolean)
    """

    get_logger().debug('mpcsutil.config.getReceiveProducts()')

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveProducts',True))

def getReceiveCfdpIndications():
    """
    Check whether or not MTAK is setup to receive CFDP Indications.

    Args
    -----
    None

    Returns
    --------
    True if CFDP Indications should be processed by MTAK, false otherwise. (Boolean)
    """

    get_logger().debug('mpcsutil.config.getReceiveCfdpIndications()')

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveCfdpIndications',True))

def setReceiveProducts(receiveProducts):
    """
    Set whether or not MTAK should receive products.

    Args
    -----
    receiveProducts - Boolean indicating whether or not MTAK should receive products. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveProducts()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveProducts',mpcsutil.getBooleanFromString(receiveProducts))

def getFetchLad():
    '''Check whether or not MTAK is setup to fetch the LAD on startup.

    Args
    -----
    None

    Returns
    --------
    True if the LAD should be fetched by MTAK, false otherwise. (Boolean)'''

    get_logger().debug('automationApp.mtak.getFetchLad')

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.fetchLad',True))

def setFetchLad(fetchLad):
    """
    Set whether or not MTAK should fetch the LAD on startup.

    Args
    -----
    fetchLad - Boolean indicating whether or not MTAK should fetch the LAD on startup. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('automationApp.mtak.setFetchLad')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.fetchLad',mpcsutil.getBooleanFromString(fetchLad))

def getReceiveFsw():
    """
    Check whether or not MTAK is setup to receive FSW telemetry.

    Args
    -----
    None

    Returns
    --------
    True if products should be processed by MTAK, false otherwise. (Boolean)
    """

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveFsw',True))

def setReceiveFsw(receiveFsw):
    """
    Set whether or not MTAK should receive FSW telemetry.

    Args
    -----
    receiveFsw - Boolean indicating whether or not MTAK should receive FSW telemetry. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveFsw()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveFsw',mpcsutil.getBooleanFromString(receiveFsw))

def getReceiveSse():
    """
    Check whether or not MTAK is setup to receive SSE telemetry.

    Args
    -----
    None

    Returns
    --------
    True if products should be processed by MTAK, false otherwise. (Boolean)
    """

    return mpcsutil.getBooleanFromString(mpcsutil.config.GdsConfig().getProperty('automationApp.mtak.down.receiveSse',True))

def setReceiveSse(receiveSse):
    """
    Set whether or not MTAK should receive SSE telemetry.

    Args
    -----
    receiveSse - Boolean indicating whether or not MTAK should receive SSE telemetry. (Boolean)

    Returns
    --------
    None
    """

    get_logger().debug('mpcsutil.config.setReceiveSse()')

    mpcsutil.config.GdsConfig().setProperty('automationApp.mtak.down.receiveSse',mpcsutil.getBooleanFromString(receiveSse))

#Import the rest of the MTAK modules (this way if the user has "import mtak" at the
#top of their script, they'll get everything)
from mtak import collection, down, err, factory, up, wait
__all__ = ['collection','down','err','factory','up','wait']


################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
