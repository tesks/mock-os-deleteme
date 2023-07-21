#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines configuration-related objects that make all of
the MPCS-related configuration information available to MPCS Python
processes.

MPCS-8652  2/6/2017 Adding magic methods to support dot references of objects.  Required for backward
compatibility.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil
import os.path
import socket
from six import (StringIO, BytesIO)
import subprocess
import sys
import tempfile
import xml.parsers.expat
import re
import traceback

def get_logger():
    return logging.getLogger('mpcs.util')

class ReleaseProperties(object):
    '''The Python representation of the MPCS release.properties file.  This makes the
    version and product line information available via Python.

    This object is implemented as a monostate (borg) object that will only parse the
    configuration once, but can be instantiated many times.'''

    __coreVersion = None
    __version = None
    __productLine = None

    def __init__(self, propertyFile=None):
        '''Initialize this Release properties object

        Args
        -----
        propertyFile - The release properties file to parse (string)

        Returns
        --------
        None'''

        get_logger().debug('mpcsutil.config.ReleaseProperties()')

        self.version = self.__version
        self.productLine = self.__productLine

        if not self.version or not self.productLine:
            self._readPropertiesFile(propertyFile)

    def _readPropertiesFile(self, propertyFile=None):
        '''Read in the given release properties file.

        Args
        -----
        propertyFile - The release properties file to parse. If not specified,
        will default to the normal MPCS release properties file. (string)

        Returns
        --------
        None'''

        get_logger().debug('mpcsutil.config.ReleaseProperties()._readPropertiesFile()')

        #Get the name of the file to parse
        releaseFilename = mpcsutil.releaseFile
        if propertyFile:
            releaseFilename = str(propertyFile)

        get_logger().info('Reading property file %s' % (releaseFilename))

        #Parse the release properties file
        _comment=re.compile(r'^\s*#.*$')
        _version=re.compile(r'^\s*(release\.internal\.coreVersion)\s*(=)\s*(?P<version>[^\s]+)\s*$')
        _product=re.compile(r'^\s*(release\.internal\.productLine)\s*(=)\s*(?P<product>[^\s]+)\s*$')
        try:
            with open(releaseFilename, 'r') as ff:
                for line in ff.readlines():
                    if _comment.match(line): continue
                    # if line.startswith('#'): continue
                    _vmatch=_version.match(line)
                    _pmatch=_product.match(line)

                    if _vmatch:
                        self.version = _vmatch.groupdict().get('version')
                        continue

                    if _pmatch:
                        self.productLine = _pmatch.groupdict().get('product')
                        continue

                    # elif line.startswith('release.internal.coreVersion='): self.version = line.split('=')[1].strip()
                    # elif line.startswith('release.internal.productLine='): self.productLine = line.split('=')[1].strip()
        except IOError:
            get_logger().error('There was a problem reading the Release properties file %s: %s' % (releaseFilename, sys.exc_info()))
            raise

        get_logger().info(self.__str__())

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        return 'MPCS Release Version = %s, Product Line = %s' % (self.version, self.productLine)

class GdsConfig(object):
    '''This is the Python equivalent of the GDS configuration class.  It relies on an MPCS script called chill_property_dump
    to generate an XML list of all the configuration properties and values from the System, Project, and User configuration
    files.  All of the -D Java System properties are also read in via the XML file.  This class will be a monostate (borg) that the
    rest of the MPCS Python can use to retrieve and manipulate configuration information.

    NOTE:  This class is a monostate and should NOT be subclassed or otherwise circumvented.

    Object Attributes
    ------------------
    dict - A dictionary of all the stored configuration properties (all keys and values are stored as strings)'''

    __sharedDict = {}

    def __init__(self, **kwargs):
        ''' Initialize the GDS configuration.

        Args
        -----
        kwargs - Keyword/Value arguments that will be set as properties in this GDS configuration (dictionary)'''

        get_logger().debug('mpcsutil.config.GdsConfig()')

        self.dict = GdsConfig.__sharedDict

        if not self.dict:
            self._readProperties()

        self.dict.update(kwargs)

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        return str(GdsConfig.__sharedDict)

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        return repr(GdsConfig.__sharedDict)


    def _startElement(self, name, attrs):
        '''Start element callback for SAX parsing.

        Args
        -----
        name - The name of the current XML element (string)
        attrs - The attributes of the current XML element (string)

        Returns
        --------
        None'''

        self._currentElement = name
        self._currentValue = ''

    def _endElement(self, name):
        '''End element callback for SAX parsing.

        Args
        -----
        name - The name of the current XML element (string)

        Returns
        --------
        None'''

        if name == 'Value':
            self.dict[self._currentName] = self._currentValue
            self._currentName = ''
            self._currentValue = ''

        self._currentElement = ''

    def _characters(self, data):
        '''Character data callback for SAX parsing

        Args
        -----
        data - The current character data from the XML

        Returns
        --------
        None'''

        if self._currentElement == 'Name':
            self._currentName += data
        elif self._currentElement == 'Value':
            self._currentValue += data

    def _readProperties(self):
        '''This is the function that reads in all the GDS (System, Project, User) configuration files via the
        MPCS Java installation and makes them all available through this class object.  It launches a Java
        subprocess that generates an XML output which is then parsed into this object using the Python Expat
        XML SAX parser.

        Args
        -----
        None

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        ConfigError - If there is a problem running the chill property dump script
        EnvironmentError - If the chill property dump script cannot be found
        XMLParsingError - If the XML dump of GDS properties from MPCS cannot be parsed'''

        get_logger().debug('mpcsutil.config.GdsConfig._readProperties()')

        outFile = tempfile.NamedTemporaryFile(prefix='MPCS')
        script = mpcsutil.propertyDumpScript
        process = None
        try:

            process = subprocess.Popen(script, shell=True, stderr=subprocess.PIPE, stdout=outFile)
            err = process.communicate()[1]
            err=err.decode('utf-8') if isinstance(err, bytes) else err
            status = process.returncode

            # MPCS-6999 09/27/16 - If the process ran and returned a nonzero return code, throw an error

            if status != 0:
                outFile.seek(0)
                out = outFile.read()
                errString = 'The Java process %s terminated with the return code %s.\n\nProcess stdout: %s\nProcess stderr: %s' % (script, status, out, err)
                raise mpcsutil.err.ConfigError(errString)

        except OSError:

            errLines = ''.join(process.stderr)
            get_logger().critical('Exception encountered while attempting to run %s to retrieve GDS configuration properties: %s\n%s' % (script, str(sys.exc_info()), errLines))
            raise

        get_logger().info('Parsing GDS configuration XML')

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            self._currentElement = ''
            self._currentName = ''
            self._currentValue = ''

            outFile.seek(0)
            parser.ParseFile(outFile)

            del self._currentElement
            del self._currentName
            del self._currentValue


        except xml.parsers.expat.ExpatError as e:
            get_logger().critical('Error parsing GDS configuration properties from XML {} (row {}, col {}):\n{}'.format(script, e.lineno, e.offset, traceback.format_exc()))
            raise
        finally:
            outFile.close()

        get_logger().debug(self)

    def getProperty(self, propertyName, default=None):
        '''Get the value of a GDS config property or return default if the property doesn't exist

        Args
        -----
        propertyName - The name of the property whose value is to be retrieved (string)
        default - The default value to return if the property does not exist (defaults to None)

        Returns
        --------
        Returns the value of 'propertyName' if the property can be found or returns the value of
        'default' otherwise (if no default is specified, None will be returned)'''

        get_logger().debug('mpcsutil.config.GdsConfig.getProperty()')

        if propertyName in self.dict:
            return self.dict[propertyName]

        return default

    def setProperty(self, propertyName, propertyValue):
        '''Set the value of a GDS config property. Will overwrite the value of an existing property or
        create a new value for a non-existent property.

        Args
        -----
        propertyName - The name of the property whose value is to be set (string)
        propertyValue - The actual value of the property that will be set (string)

        Returns
        --------
        None'''

        get_logger().debug('mpcsutil.config.GdsConfig.setProperty()')

        propertyName = str(propertyName)
        propertyValue = str(propertyValue)

        self.dict[propertyName] = propertyValue


    def getMission(self):
        '''Get the current mission name

        Args
        -----
        None

        Returns
        --------
        The name of the current mission (string)'''

        return self.getProperty('GdsMission')

    def isSse(self):
        '''Returns whether or not the current mission is an SSE.

        Args
        -----
        None

        Returns
        --------
        True if the mission is SSE, False otherwise (boolean)'''

        mission = self.getMission()
        return mission.lower().endswith('sse')

    def hasSse(self):
        '''Returns whether or not the current mission has an SSE.

        Args
        -----
        None

        Returns
        --------
        True if the mission has an SSE, False otherwise (boolean)'''

        return mpcsutil.getBooleanFromString(self.getProperty('mission.needsSse'))

    def hasCfdp(self):
        '''Returns whether or not the current mission uses CFDP.

        Args
        -----
        None

        Returns
        --------
        True if the mission uses CFDP, False otherwise (boolean)'''

        # MPCS-10788  - 4/17/19 - Added  automationApp.mtak.cfdp.enable property
        return mpcsutil.getBooleanFromString(self.getProperty('automationApp.mtak.cfdp.enable'))

    def getConfigDirs(self):
        return self.getProperty("GdsConfigFullPath").split(os.pathsep)

    def getTemplateDirs(self):
        return self.getProperty('GdsTemplateFullPath').split(os.pathsep)


    def getSystemConfigDir(self):
        '''Returns the system configuration directory path.'''

        fullPath = str(self.getProperty('GdsDirectory'));
        fullPath = os.path.join(fullPath, 'config')
        return fullPath + "/";

    def getProjectConfigDir(self):
        '''Returns the project configuration directory path'''
        fullPath = self.getSystemConfigDir()
        fullPath = os.path.join(fullPath, self.getMission());
        return fullPath + "/"

    def getDefaultFswDictDir(self):
        ''' Returns a string containing the default flight dictionary directory for the current mission. '''
        return self.getProperty('dictionary.flight.defaultDirectory')

    def getDefaultFswVersion(self):
        ''' Returns a string containing the default flight dictionary version for the current mission. '''
        return self.getProperty('dictionary.flight.defaultVersion')

    def setDefaultFswDictDir(self, directory):
        '''Sets the default FSW directory'''
        if directory is not None:
            self.setProperty('dictionary.flight.defaultDirectory', directory)

    def setDefaultFswVersion(self, version):
        ''' Sets the default FSW version '''
        if version is not None:
            self.setProperty('dictionary.flight.defaultVersion', version)

    def getDefaultSseDictDir(self):
        ''' Returns a string containing the default SSE dictionary directory for the current mission. '''
        return self.getProperty('dictionary.sse.defaultDirectory')

    def getDefaultSseVersion(self):
        ''' Returns a string containing the default SSE dictionary version for the current mission. '''
        return self.getProperty('dictionary.sse.defaultVersion')

    def setDefaultSseDictDir(self, directory):
        ''' Sets the default SSE directory '''
        if directory is not None:
            self.setProperty('dictionary.sse.defaultDirectory', directory)

    def setDefaultSseVersion(self, version):
        ''' Sets the default SSE version '''
        if version is not None:
            self.setProperty('dictionary.sse.defaultVersion', version)

    def isUplinkEnabled(self):
        ''' Returns a boolean indicating whether the current mission is configured for uplink. '''
        return self.getProperty('mission.uplink.enable') == 'true'

    def getAllowedVenueTypes(self):
        return self.getProperty('mission.venueType.allowed')

    def getEvrLevelList(self):
        '''Returns the list of unique EVR levels.

        Args
        -----
        None

        Returns
        --------
        List, may be empty by not None'''

        evrLevelList = []
        fswListStr = mpcsutil.config.GdsConfig().getProperty('evr.flight.levels.all')
        if fswListStr is not None:
            fswList = fswListStr.split(',')
            evrLevelList = evrLevelList + fswList

        if self.hasSse():
            sseListStr = mpcsutil.config.GdsConfig().getProperty('evr.sse.levels.all')
            if sseListStr is not None:
                sseList = sseListStr.split(',')
                evrLevelList = evrLevelList + sseList

        return list(set(evrLevelList))

    def getConversionFactor(self):
        '''Gets the earth seconds conversion factor as a float.'''
        return self.getProperty("time.date.localSolarTime.earthSecondConversionFactor")

    def getLstPrefix(self):
        '''Gets LST prefix'''
        return self.getProperty("time.date.localSolarTime.lstPrefix")

    def getLstPrecision(self):
        '''Gets LST precision'''
        return self.getProperty("time.date.localSolarTime.lstPrecision")

    def getScetPrecision(self):
        '''Gets SCET precision'''
        return self.getProperty("time.date.scetPrecision")

    def getErtPrecision(self):
        '''Gets ERT precision'''
        return self.getProperty("time.date.ertPrecision")

    def getEpochScet(self):
        '''Gets epoch SCET'''
        return self.getProperty("time.date.localSolarTime.epochScet")

    def getSpacecraftIds(self):
        '''Gets the current mission's allowed Spacecraft IDs (SCID) as a CSV list of Strings '''
        return self.getProperty("mission.spacecraft.ids")

    def getLadHost(self):
        ''' Gets the currently configured globallad server host'''
        return self.getProperty('globallad.server.host', 'localhost')

    def getLadPort(self):
        ''' Gets the currently configured globallad rest port'''
        return self.getProperty('globallad.rest.port', '8887')

    def getLadUri(self):
        '''Get the uri for the globallad server.
        returns a string template that contains a %s formatter for host and %d formatter for port.
        Callers of this function should use string formatting to plug-in these values prior to usage. '''
        return self.getProperty('globallad.rest.uri', 'http://%s:%d/globallad/')

    def isVcidMappingEnabled(self):
        '''Gets the configuration for VCID query mapping 'mission.downlink.virtualChannels.enableQueryMapping' '''
        return self.getProperty('mission.downlink.virtualChannels.enableQueryMapping', False)


    def getDownlinkVcids(self):
        vcids = []
        vcidList = self.getProperty('mission.downlink.virtualChannels.ids', 0)
        if vcidList is not None:
            if "," in vcidList:
                vc = vcidList.split(',')
                vcids = vcids + vc
            else:
                vcids = vcidList

        return list(set(vcids))

    def getDownlinkVcidNames(self):
        names = []
        namesList = self.getProperty('mission.downlink.virtualChannels.names', 'UNKNOWN')
        if namesList is not None:
            if "," in namesList:
                n = namesList.split(',')
                names = names + n
            else:
                names = namesList

        return list(names)

    def getVcidMapping(self, vcid):
        '''
        Maps the supplied downlink VCID to its configured name

        :param vcid: the virtual channel id

        :return: configured name of the vcid if vcid mapping is enabled, otherwise returns the vcid
        '''
        if vcid is not None and vcid != "" and self.isVcidMappingEnabled():
            allDownlinkVcids = self.getDownlinkVcids()
            vcidIndex = allDownlinkVcids.index(vcid)
            if vcidIndex == -1:
                return "UNKNOWN"

            names = self.getDownlinkVcidNames()
            if names is None or names.__sizeof__() == 0:
                return "UNKNOWN"
            return names[vcidIndex]

        return vcid

def getJmsHost():
    '''Get the hostname for the jms.

    Args
    -----
    None

    Returns
    --------
    The hostname for the current jms (string)'''

    get_logger().debug('mpcsutil.config.getJmsHost()')

    return GdsConfig().getProperty('jms.host', None)

def setJmsHost(host):
    '''Set the host for the jms.

    Args
    -----
    host - The host to use for connecting to the jms. (string)

    Returns
    --------
    None'''

    get_logger().debug('mpcsutil.config.setJmsHost()')

    GdsConfig().setProperty('jms.host', host)

def getJmsPort():
    '''Get the port number for the jms.

    Args
    -----
    None

    Returns
    --------
    The port number for the current jms. (int)'''

    get_logger().debug('mpcsutil.config.getJmsPort()')

    return int(GdsConfig().getProperty('jms.port', '61614'))

def setJmsPort(port):
    '''Set the port for the jms.

    Args
    -----
    port - The port to use for connecting to the jms. (int)

    Returns
    --------
    None'''

    get_logger().debug('mpcsutil.config.setJmsPort()')

    GdsConfig().setProperty('jms.port', port)

def getDefaultSessionConfigFilename():
    '''Get the name of the default session configuration file.

    Args
    -----
    None

    Returns
    --------
    The name of the default session configuration file. (string)'''

    get_logger().debug('mpcsutil.config.getDefaultConfigFilename()')

    suffix = GdsConfig().getProperty('general.context.defaultConfigFile', 'TempTestConfig.xml')
    host = getCurrentHostShortName()

    return '%s/%s_%s' % (mpcsutil.userConfigDir, host, suffix)

def getCurrentHostShortName():
    '''Get the short name of the current host.  For example, if the current host is
    mymachine.jpl.nasa.gov, this method will just return the string "mymachine".

    Args
    -----
    None

    Returns
    --------
    The shortest possible name of the current host machine. (string)'''

    host = socket.gethostname()
    if host.find('.') != -1:
        host = host.split('.')[0]

    return host




class SessionConfig(object):
    '''SessionConfig is the python representation of the MPCS Session Configuration.  It relies on an MPCS script
       called chill_get_sessions to generate an XML list of all the configuration properties and values from an existing
       session.   It will accept a specified file with session configuration properties in order to generate a new session,
       and it provides a means of inputting the session configuration properties as a dictionary in order to override
       values specified in the file, or to generate a new session without a file.

       MPCS-8652 2/6/2017 - The following attributes had name changes since MPCS for MSL.  Adding in an aliasing capability
       to get / set works correctly.

       Aliasing will be enabled for the following:

       MPCS Attribute | Mapped AMPCS Attribute
       ---------------------------------------
       fswDictDir       fswDictionaryDirectory
       sseDictDir       sseDictionaryDirectory
       outputDir        outputDirectory
       connectionType   downlinkConnectionType
       fswHost          fswDownlinkHost
       mpcsVersion      ampcsVersion

       The following attributes have been deprecated since MPCS for MSL and will not be included.

       fswBuildId
       logDir
       downlinkSpacecraftSide
    '''
    log = property(
        lambda x:x.__dict__.setdefault('_log', get_logger()),
        lambda x,y: x.__dict__.update({'_log':y}))

    def __init__(self, key=None, host=None, filename=None, validate_session=False, **configDict):
        ''' Initializer that will set the dictionary with all config values that are defined.   If a key is specified
        then chill_get_sessions is invoked to gather the session configuration data.   If a filename is specified then
        the file is read in. If a configDict is specified, then the values of the configDict will be placed in the configuration
        dictionary and will override any values from the file.  In addition this constructor will invoke the readProperties()
        method to read in the Session Configuration information if needed.

        Args
        -----
        key - The session key of the session to associated this config object with (int)
        filename - The filename of a session configuration file (string)
        configDict - A dictionary of session configuration information (dictionary {})
        validate_session - True if this object should validate the session by fetching it from the database or parsing the
                           input file.  False if you just want to use this object as a container with getters/setters. Defaults
                           to False. (boolean)

        Only one piece of information relating to identifying a session should be supplied.  If a session key is supplied, no filename
        should be supplied and the configDict should not contain a session key.  If a filename is specified, no session key should be supplied and
        the configDict should not contain a session key.  If neither a session key or filename is specified, the configDict must contain a session key
        entry.

        Raised Exceptions
        ------------------
        InvalidInitError - If session information is specified in an illegal manner (e.g. referencing multiple sessions,
                           referencing a non-existing session, etc.)'''

        _log = self.log
        _log.debug('mpcsutil.config.SessionConfig()')

        self._readGdsConfig()
        self.clear()

        if any([ (key and filename), (key and configDict.get('key')), (filename and configDict.get('key')) ]):

            errString = 'When creating a session configuration, only one type of input may be used to specify a session.  You ' + \
                                       'must specify a session key OR a session config file OR all the session information in ' + \
                                       'the keyword dictionary arguments'
            self.log.error(errString)
            raise mpcsutil.err.InvalidInitError(errString)

        #If there's a session key in the configDict, pull it out
        if 'key' in configDict:
            key = configDict['key']
            self.number = key

        if 'host' in configDict:
            host = configDict['host']

        #If a session key was input, grab the session from the database
        if key and not host:
            host = getCurrentHostShortName()

        self.log.info('Session Key = %s' % (key))
        self.log.info('Session Host = %s' % (host))
        self.log.info('Session Config File = %s' % (filename))

        if validate_session:
            if key and host:

                self._readSessionFromDatabase(key, host)
                self.number = key

            #A config file was specified
            elif filename:

                #Couldn't find the config file
                if not os.path.exists(filename):

                    errString = 'Could not find the input session configuration file %s' % (filename)
                    self.log.error(errString)
                    raise mpcsutil.err.InvalidInitError(errString)

                #validate schema
                self._validateSchema(filename)

                #Open and read the file
                with open(filename, "rb") as fStream:
                    self._readProperties(fStream)

                #If the file did not contain a session key, create a new session
                if self.key is None:
                    self._createNewSession(**configDict)

                #Do we need to verify that the key exists in the database?

            #Create a new session
            else:

                self._createNewSession(**configDict)

        for val in configDict:
            self._setLocalAttribute(val, configDict[val])
        if validate_session:

            if not self.venueType or self.venueType.upper() == 'UNKNOWN':

                errString = 'The venue type of the input session is UNKNOWN.  MTAK must have a valid venue type to function properly.'
                self.log.critical(errString)
                raise mpcsutil.err.InvalidInitError(errString)

            self._setNetworkDefaults()

    #   MPCS-8652 2/6/2017 - Using property annotations for the aliases.
    @property
    def mpcsVersion(self):
        return self.ampcsVersion

    @mpcsVersion.setter
    def mpcsVersion(self, value):
        self.ampcsVersion = value

    @property
    def fswHost(self):
        return self.fswDownlinkHost

    @fswHost.setter
    def fswHost(self, value):
        self.fswDownlinkHost = value

    @property
    def connectionType(self):
        return self.downlinkConnectionType

    @connectionType.setter
    def connectionType(self, value):
        self.downlinkConnectionType = value

    @property
    def outputDir(self):
        return self.outputDirectory

    @outputDir.setter
    def outputDir(self, value):
        self.outputDirectory = value

    @property
    def sseDictDir(self):
        return self.sseDictionaryDirectory

    @sseDictDir.setter
    def sseDictDir(self, value):
        self.sseDictionaryDirectory = value

    @property
    def fswDictDir(self):
        return self.fswDictionaryDirectory

    @fswDictDir.setter
    def fswDictDir(self, value):
        self.fswDictionaryDirectory = value


    def _validateSchema(self, filename):
        ''' Validate config file against schema

        Args
        -----
        filename - The filename of a session configuration file (string)


        Raised Exceptions
        ------------------
        InvalidInitError - If session config file failed to validate against schema'''

        self.log.debug('mpcsutil.config.SessionConfig()')


        gdsConfig = GdsConfig()

        self._validateScript = '%s/%s' % (mpcsutil.chillToolsDirectory, 'chill_validate_xml')
        if not os.path.isfile(self._validateScript):
            errString = 'Cannot find the required script ' + self._validateScript
            self.log.critical(errString)
            raise mpcsutil.err.EnvironmentError(errString)


        self.log.debug('MPCS schema validate script is %s.' % (self._validateScript))

        self._schemaFile = '%s/%s' % (mpcsutil.chillSchemaDirectory, 'SessionConfigFile.rnc')
        if not os.path.isfile(self._schemaFile):
            errString = 'Cannot find the required schema file ' + self._schemaFile
            self.log.critical(errString)
            raise mpcsutil.err.EnvironmentError(errString)

        self.log.debug('MPCS config schema file is %s.' % (self._schemaFile))
        try:
            processString = '%s %s %s' % (self._validateScript,
                                          self._schemaFile,
                                          filename)
            self.log.debug('Executing: %s' % (processString))
            process = subprocess.Popen(processString, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            status = process.wait()

            if status != 0:
               errString = 'Session configuration file does not conform to schema definition'
               self.log.critical(errString)
               raise mpcsutil.err.EnvironmentError(errString)

        except TypeError:

            errString = 'Error occurred while validating session configuration file against schema definition'
            self.log.critical(errString)
            raise mpcsutil.err.EnvironmentError(errString)



    def _readGdsConfig(self):
        '''Read any required information from the GDS configuration file. This is mainly used to get the
        name of the session query script and any other needed command line arguments for that script.

        Args
        -----
        None

        Returns
        --------
        None'''

        self.log.debug('mpcsutil.config.SessionConfig._readGdsConfig()')

        gdsConfig = GdsConfig()

        _sessionQueryApp = gdsConfig.getProperty('automationApp.internal.mtak.app.sessionQuery', 'chill_get_sessions')
        self._sessionQueryScript = '%s/%s' % (mpcsutil.chillBinDirectory, _sessionQueryApp)
        if not os.path.isfile(self._sessionQueryScript):
            errString = 'Cannot find the required script ' + self._sessionQueryScript
            self.log.critical(errString)
            raise mpcsutil.err.EnvironmentError(errString)
        self.log.debug('MPCS session query script is %s.' % (self._sessionQueryScript))

        self._sessionKeyArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionKey', '-K')
        self._sessionHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionHost', '-O')
        self._outputFormatArg = gdsConfig.getProperty('automationApp.internal.mtak.args.outputFormat', '-o')
        self._xmlFormatValue = gdsConfig.getProperty('automationApp.mtak.args.values.xmlFormat', 'Xml')
        self._databaseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseHost', '--databaseHost')
        self._databasePortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePort', '--databasePort')
        self._databaseUserArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseUser', '--dbUser')
        self._databasePasswordArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePassword', '--dbPwd')

    def __str__(self):
        '''x.__str()__() <==> str(x)'''

        self.log.debug('mpcsutil.config.SessionConfig.__str__()')

        return self.__repr__()

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        self.log.debug('mpcsutil.config.SessionConfig.__repr__()')

        return ('SessionConfig(**{' +
                                 'number:' + str(self.number) + ',' +
                                 'spacecraftId:' + str(self.spacecraftId) + ',' +
                                 'name:' + str(self.name) + ',' +
                                 'type:' + str(self.type) + ',' +
                                 'description:' + str(self.description) + ',' +
                                 'user:' + str(self.user) + ',' +
                                 'host:' + str(self.host) + ',' +
                                 'hostId:' + str(self.hostId) + ',' +
                                 'ladPort:' + str(self.ladPort) + ',' +
                                 'startTime:' + str(self.startTime) + ',' +
                                 'endTime:' + str(self.endTime) + ',' +
                                 'ampcsVersion:' + str(self.ampcsVersion) + ',' +
                                 'fullName:' + str(self.fullName) + ',' +
                                 'fswVersion:' + str(self.fswVersion) + ',' +
                                 'sseVersion:' + str(self.sseVersion) + ',' +
                                 'runFswDownlink:' + str(self.runFswDownlink) + ',' +
                                 'runSseDownlink:' + str(self.runSseDownlink) + ',' +
                                 'runUplink:' + str(self.runUplink) + ',' +
                                 'fswDictionaryDirectory:' + str(self.fswDictionaryDirectory) + ',' +
                                 'sseDictionaryDirectory:' + str(self.sseDictionaryDirectory) + ',' +
                                 'outputDirectory:' + str(self.outputDirectory) + ',' +
                                 'outputDirOverridden:' + str(self.outputDirOverridden) + ',' +
                                 'venueType:' + str(self.venueType) + ',' +
                                 'inputFormat:' + str(self.inputFormat) + ',' +
                                 'downlinkConnectionType:' + str(self.downlinkConnectionType) + ',' +
                                 'uplinkConnectionType:' + str(self.uplinkConnectionType) + ',' +
                                 'testbedName:' + str(self.testbedName) + ',' +
                                 'downlinkStreamId:' + str(self.downlinkStreamId) + ',' +
                                 'sessionDssId:' + str(self.sessionDssId) + ',' +
                                 'sessionVcid:' + str(self.sessionVcid) + ',' +
                                 'inputFile:' + str(self.inputFile) + ',' +
                                 'databaseSessionKey:' + str(self.databaseSessionKey) + ',' +
                                 'databaseSessionHost:' + str(self.databaseSessionHost) + ',' +
                                 'jmsSubtopic:' + str(self.jmsSubtopic) + ',' +
                                 'topic:' + str(self.topic) + ',' +
                                 'fswDownlinkHost:' + str(self.fswDownlinkHost) + ',' +
                                 'fswUplinkHost:' + str(self.fswUplinkHost) + ',' +
                                 'sseHost:' + str(self.sseHost) + ',' +
                                 'fswUplinkPort:' + str(self.fswUplinkPort) + ',' +
                                 'sseUplinkPort:' + str(self.sseUplinkPort) + ',' +
                                 'fswDownlinkPort:' + str(self.fswDownlinkPort) + ',' +
                                 'sseDownlinkPort:' + str(self.sseDownlinkPort) + ',' +
                                 '})')

    def clear(self):
        '''Clear out all the values from this configuration object.  This is also used by the __init__
        method to bind all of the known attributes to this object.

        Args
        -----
        None

        Returns
        --------
        None'''

        self.log.debug('mpcsutil.config.SessionConfig.clear()')

        self.key = None
        self.number = None
        self.spacecraftId = None
        self.name = None
        self.type = None
        self.description = None
        self.user = None
        self.host = None
        self.hostId = None
        self.ladPort = None
        self.startTime = None
        self.endTime = None
        self.ampcsVersion = None
        self.fullName = None
        self.fswVersion = None
        self.sseVersion = None
        self.runFswDownlink = None
        self.runSseDownlink = None
        self.runUplink = None
        self.fswDictionaryDirectory = None
        self.sseDictionaryDirectory = None
        self.outputDirectory = None
        self.outputDirOverridden = None
        self.venueType = None
        self.inputFormat = None
        self.downlinkConnectionType = None
        self.uplinkConnectionType = None
        self.testbedName = None
        self.downlinkStreamId = 'Not applicable'
        self.sessionDssId = None
        self.sessionVcid = None
        self.inputFile = None
        self.databaseSessionKey = None
        self.databaseSessionHost = None
        self.jmsSubtopic = None
        self.topic = None
        self.fswDownlinkHost = None
        self.fswUplinkHost = None
        self.sseHost = None
        self.fswUplinkPort = None
        self.sseUplinkPort = None
        self.fswDownlinkPort = None
        self.sseDownlinkPort = None

    def _readSessionFromDatabase(self, sessionKey, sessionHost):
        '''Read the MPCS session from the database that has the associated session key

        Args
        -----
        sessionKey - The numeric session key of the test to retrieve from the database (int)

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        ConfigError - If the session query output cannot be interpreted properly
        EnvironmentError - If the session query script can't be found
        InvalidSessionKeyError - If an invalid/non-existent session key is provided'''

        self.log.debug('mpcsutil.config.SessionConfig._readSessionFromDatabase()')
        self.log.info('Reading session from database with key=%s and host=%s' % (sessionKey, sessionHost))

        try:
            processString = '%s %s %d %s %s %s %s %s %s %s %d %s %s' % (self._sessionQueryScript,
                                                                        self._sessionKeyArg,
                                                                        int(sessionKey),
                                                                        self._sessionHostArg,
                                                                        str(sessionHost),
                                                                        self._outputFormatArg,
                                                                        self._xmlFormatValue,
                                                                        self._databaseHostArg,
                                                                        mpcsutil.database.getDatabaseHost(),
                                                                        self._databasePortArg,
                                                                        int(mpcsutil.database.getDatabasePort()),
                                                                        self._databaseUserArg,
                                                                        mpcsutil.database.getDatabaseUserName())

            dbPassword = mpcsutil.database.getDatabasePassword()
            if dbPassword:
                processString += (' %s %s' % (self._databasePasswordArg, dbPassword))

            self.log.debug('Executing: %s' % (processString))
            process = subprocess.Popen(processString, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
            sessionData, errData = process.communicate()
            status = process.returncode
            # inStream = StringIO()
            inStream = BytesIO()


            # chill_get tools can return errors, but terminate with a 0 status code, if the command line is malformed
            if (errData) or (status != 0):
                errString = 'Exception encountered while attempting to run %s to retrieve valid sessions: %s' % (self._sessionQueryScript, errData)
                self.log.critical(errString)
                raise mpcsutil.err.ConfigError(errString)

            if not sessionData:
                errString = 'No session with the session key "%s" and session host "%s" exists.' % (sessionKey, sessionHost)
                self.log.critical(errString)
                raise mpcsutil.err.InvalidSessionKeyError(errString)

            # inStream.writelines(sessionData.split('\n'))
            inStream.write(sessionData)
            inStream.flush()
            inStream.seek(0)

            self._readProperties(inStream)

            inStream.close()

        except TypeError:

            errString = 'The session key "%s" is invalid.  Make sure its an existing numeric session key.  %s' % (sessionKey, str(sys.exc_info()))
            self.log.critical(errString)
            raise mpcsutil.err.InvalidSessionKeyError(errString)

        except xml.parsers.expat.ExpatError:

            errString = 'The session key "%s" cannot be parsed.  %s' % (sessionKey, str(sys.exc_info()))
            self.log.critical(errString)
            raise mpcsutil.err.InvalidSessionKeyError(errString)

        except Exception:

            _, _, traceback = sys.exc_info()
            errString = 'Could not retrieve the session with key "%s" and host "%s". Are you sure this session exists?' % (sessionKey, sessionHost)
            self.log.critical(errString)
            raise mpcsutil.err.InvalidInitError(errString)
            # raise my_exc.__class__(my_exc, traceback)

        self.log.info(self)

    def _createNewSession(self, **kwargs):
        '''
        Currently not implemented.  Will need to be implemented if MTAK has to ever launch its own sessions.

        Raises
        -------
        Always raises InvalidInitError
        '''

        self.log.debug('mpcsutil.config.SessionConfig._createNewSession()')

        errString = '''MTAK does not currently support creating new sessions.  Please pass in a session key or make sure
        that you're properly set up to connect to an existing MPCS session.'''
        self.log.critical(errString)
        raise mpcsutil.err.InvalidInitError(errString)

    def _setLocalAttribute(self, name, value):
        '''Set the value of an attribute on this object. This method ensures values are set as
        the appropriate Python type instead of just being set as whatever type they're passed in as.

        Args
        -----
        name - The name of the attribute to set (string)
        value - The value to set the attribute to (various types)

        Returns
        --------
        None'''

        self.log.debug('mpcsutil.config.SessionConfig._setLocalAttribute()')

        if(name == 'number' or name == 'spacecraftId'
          or name == 'fswUplinkPort' or name == 'sseUplinkPort'
          or name == 'fswDownlinkPort' or name == 'sseDownlinkPort'
          or name == 'sessionDssId' or name == 'sessionVcid'
          or name == 'databaseKey' or name == 'hostId'
          or name == 'ladPort'):

            setattr(self, name, int(value))

        else:

            value = str(value).strip()
            setattr(self, name, value)
            if name == 'outputDirectory':
                self.logDir = value

    def _readProperties(self, inputStream):
        '''This is the function that reads in an xml configuration file without attributes (elements only), and adds adds each
        hierarchical key / value pair to the dictionary. If there is no value present for a given element/key then it is not
        added to the dictionary.   Currently it strips all value strings before inserting them into the dictionary.
        It uses the Python Expat XML SAX parser.

        Args
        -----
        InputStream - the input stream from which to parse the properties

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        XMLParsingError - If the XML dump of GDS properties from MPCS cannot be parsed'''

        self.log.debug('mpcsutil.config.SessionConfig._readProperties()')
        self.log.info('Reading session configuration from an XML file.')

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            self._currentElement = ''
            self._currentValue = ''

            parser.ParseFile(inputStream)

            del self._currentElement
            del self._currentValue

        except xml.parsers.expat.ExpatError as e:
            self.log.critical('Error parsing session configuration properties from XML (row {}, col {}):\n{}'.format(e.lineno, e.offset, traceback.format_exc()))
            raise

        self.log.info(self)

    def _startElement(self, name, attrs):
        '''Start element callback for SAX parsing.

        Args
        -----
        name - The name of the current XML element (string)
        attrs - The attributes of the current XML element (string)

        Returns
        --------
        None'''

        self._currentValue = ''
        self._currentElement = name

        if name == 'Session':

            self._version = attrs['version']
            if self._version != '3':
                errString = 'Invalid Session schema version.   The current version is 3.'
                self.log.error(errString)
                raise mpcsutil.err.InvalidStateError(errString)



    def _endElement(self, name):
        '''End element callback for SAX parsing.

        Args
        -----
        name - The name of the current XML element (string)

        Returns
        --------
        None'''



        if name == 'Number':

            self.key = int(self._currentValue)
            self.number = int(self._currentValue)

        elif name == 'FswDictionaryDirectory':

            self.fswDictionaryDirectory = self._currentValue

        elif name == 'SseDictionaryDirectory':

            self.sseDictionaryDirectory = self._currentValue

        elif name == 'OutputDirectory':

            self.outputDirectory = self._currentValue
            self.logDir = self.outputDirectory

        else:

            elemName = self._currentElement
            firstLetter = elemName[0].swapcase()
            remainder = elemName[1:]
            elemName = firstLetter + remainder
            self._setLocalAttribute(elemName, self._currentValue)

    def _characters(self, data):
        '''Character data callback for SAX parsing

        Args
        -----
        data - The current character data from the XML

        Returns
        --------
        None'''

        self._currentValue += data

    def _setNetworkDefaults(self):
        ''' _setNetworkDefaults - is called to ensure that the required default values
        are set for the session object.   The host and port are required.   If they are
        not currently set, then set them to the gdsConfiguration defined default values.

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the venue information is not set properly'''

        self.log.debug('mpcsutil.config.SessionConfig._setNetworkDefaults()')

        gdsConfiguration = mpcsutil.config.GdsConfig()

        if not self.venueType:
            errString = 'Cannot set default network properties because the session config has no \"venueType\" value set.'
            self.log.error(errString)
            raise mpcsutil.err.InvalidStateError(errString)

        #
        # MPCS-9879  - 6/4/18. I believe the following code was not even correct for R7, as was
        # reflected by a previous comment.  I am updating it
        # to try to do the right thing for R8, but I am going on record that this code DOES NOT BELONG in
        # the Python code. The defaulting and overriding of hosts and ports in the session configuration is complex,
        # and should be only one place in the code, namely in the Java class that handles
        # connection properties.
        #
        # Having this code here is an error prone maintenance nightmare.  Why does this python code need
        # to update the supplied session configuration? If the session configuration was passed as is to
        # the proxy processes, they are smart enough to fill in defaults! If this really needs to be done here,
        # then perhaps a Java utility for getting these defaults is a better answer?  I have no idea how to
        # test enough to ensure all of this logic is working.
        #

        # Define the property names for FSW uplink/downlink host and port, SSE host, and SSE
        # uplink/downlink port, for both the overall default, and the venue-specific default.
        fswDownlinkPortDefaultProperty = "connection.flight.downlink.defaultPort"
        fswDownlinkPortVenueProperty = fswDownlinkPortDefaultProperty + "." + self.venueType.upper()
        fswDownlinkHostDefaultProperty = "connection.flight.downlink.defaultHost"
        fswDownlinkHostVenueProperty = fswDownlinkHostDefaultProperty + "." + self.venueType.upper()
        fswUplinkPortDefaultProperty = "connection.flight.uplink.defaultPort"
        fswUplinkPortVenueProperty =  fswUplinkPortDefaultProperty + "." + self.venueType.upper()
        fswUplinkHostDefaultProperty = "connection.flight.uplink.defaultHost"
        fswUplinkHostVenueProperty = fswUplinkHostDefaultProperty + "." + self.venueType.upper()
        sseHostDefaultProperty = "connection.sse.downlink.defaultHost"
        sseHostVenueProperty = sseHostDefaultProperty + "." + self.venueType.upper()
        sseDownlinkPortDefaultProperty = "connection.sse.downlink.defaultPort"
        sseDownlinkPortVenueProperty = sseDownlinkPortDefaultProperty + "." + self.venueType.upper()
        sseUplinkPortDefaultProperty = "connection.sse.uplink.defaultPort"
        sseUplinkPortVenueProperty = sseUplinkPortDefaultProperty + "." + self.venueType.upper()

        # These property names may or may not apply, depending on venue, testbed, and stream type
        # checks below. Setting this to a non-existent property name actually makes things easier
        # later, because then they do not all have to be checked to see if they are None.  Asking
        # for a property with bogus name should just return None.
        fswDownlinkPortTestbedProperty = "bogus"
        fswDownlinkPortStreamProperty = "bogus"
        fswDownlinkHostTestbedProperty = "bogus"
        fswDownlinkHostStreamProperty = "bogus"
        fswUplinkHostTestbedProperty = "bogus"
        fswUplinkPortTestbedProperty = "bogus"
        sseHostTestbedProperty = "bogus"
        sseDownlinkPortTestbedProperty = "bogus"
        sseUplinkPortTestbedProperty = "bogus"

        if self.venueType.upper() == "TESTBED" or self.venueType.upper() == "ATLO":
            if not self.testbedName:
                errString = 'Cannot set default network properties because the venue type is "%s", but the session config has no \"testbedName\" value set.' % (self.venueType)
                self.log.error(errString)
                raise mpcsutil.err.InvalidStateError(errString)
            # MPCS-5015 07/10/2013 - downlinkStreamId should not be checked for chill_up
            # MPCS-5069 9/16/13: downlinkStreamId should only be checked for NEN_SN_SERVER and NEN_SN_CLIENT in TESTBED or ATLO
            elif self.downlinkStreamId == "Not applicable" and  self.isUplinkOnly() == False and (self.downlinkConnectionType == "NEN_SN_SERVER" or self.downlinkConnectionType == "NEN_SN_CLIENT"):
                errString = 'Cannot set default network properties because the venue type is "%s", but the session config has no \"downlinkStreamId\" value set.' % (self.venueType)
                self.log.error(errString)
                raise mpcsutil.err.InvalidStateError(errString)

            # Create the testbed and stream type-specific property names
            fswDownlinkPortTestbedProperty = fswDownlinkPortVenueProperty + "." + self.testbedName.upper()
            fswDownlinkPortStreamProperty = fswDownlinkPortTestbedProperty + "." +  self.downlinkStreamId
            fswDownlinkHostTestbedProperty = fswDownlinkHostVenueProperty + "." + self.testbedName.upper()
            fswDownlinkHostStreamProperty = fswDownlinkHostTestbedProperty + "." +  self.downlinkStreamId
            fswUplinkHostTestbedProperty = fswUplinkHostVenueProperty + "." + self.testbedName.upper()
            fswUplinkPortTestbedProperty = fswUplinkPortVenueProperty + "." + self.testbedName.upper()
            sseHostTestbedProperty = sseHostVenueProperty + "." + self.testbedName.upper()
            sseDownlinkPortTestbedProperty = sseDownlinkPortVenueProperty + "." + self.testbedName.upper()
            sseUplinkPortTestbedProperty = sseUplinkPortVenueProperty + "." + self.testbedName.upper()

        # Downlink host can be defined for specific stream, on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property.  If none of these properties is defined, we go with localhost.
        if self.fswDownlinkHost is None:
            self.fswDownlinkHost = gdsConfiguration.getProperty(fswDownlinkHostStreamProperty,
                 gdsConfiguration.getProperty(fswDownlinkHostTestbedProperty,
                 gdsConfiguration.getProperty(fswDownlinkHostVenueProperty,
                 gdsConfiguration.getProperty(fswDownlinkHostDefaultProperty, 'localhost'))))

        # Uplink port can be defined on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property. If none of these properties is defined, we go with 12345.
        if self.fswUplinkPort is None:
            self.fswUplinkPort = int(gdsConfiguration.getProperty(fswUplinkPortTestbedProperty,
                 gdsConfiguration.getProperty(fswUplinkPortVenueProperty,
                 gdsConfiguration.getProperty(fswUplinkPortDefaultProperty, 12345))))


        # Downlink port can be defined for specific stream, on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property.   If none of these properties is defined, we go with 12346.
        if self.fswDownlinkPort is None:
            self.fswDownlinkPort = int(gdsConfiguration.getProperty(fswDownlinkPortStreamProperty,
                 gdsConfiguration.getProperty(fswDownlinkPortTestbedProperty,
                 gdsConfiguration.getProperty(fswDownlinkPortVenueProperty,
                 gdsConfiguration.getProperty(fswDownlinkPortDefaultProperty, 12346)))))

        # SSE host can be defined on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property.  Note that R8 defines SSE hosts for uplink and downlink
        # separately, but the session configuration only supports one SSE host. I have used
        # the configuration properties for the SSE downlink host here.
        # If none of these properties is defined, we go with localhost.
        if self.sseHost is None:
            self.sseHost = gdsConfiguration.getProperty(sseHostTestbedProperty,
                 gdsConfiguration.getProperty(sseHostVenueProperty,
                 gdsConfiguration.getProperty(sseHostDefaultProperty, 'localhost')))

        # SSE uplink port can be defined on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property.  If none of these properties is defined, we go with 12347.
        if self.sseUplinkPort is None:
            self.sseUplinkPort = int(gdsConfiguration.getProperty(sseUplinkPortTestbedProperty,
                 gdsConfiguration.getProperty(sseUplinkPortVenueProperty,
                 gdsConfiguration.getProperty(sseUplinkPortDefaultProperty, 12347))))

        # SSE downlink port can be defined on specific testbed, in specific venue, or
        # just have an overall default. If the more specific property is defined, it overrides the
        # less specific property.   If none of these properties is defined, we go with 12348.
        if self.sseDownlinkPort is None:
            self.sseDownlinkPort = int(gdsConfiguration.getProperty(sseDownlinkPortTestbedProperty,
                 gdsConfiguration.getProperty(sseDownlinkPortVenueProperty,
                 gdsConfiguration.getProperty(sseDownlinkPortDefaultProperty, 12348))))

        self.log.info('FSW uplink to %s:%s and downlink from %s:%s' % (self.fswUplinkHost, self.fswUplinkPort, self.fswDownlinkHost, self.fswDownlinkPort))
        self.log.info('SSE uplink to %s:%s and downlink from %s:%s' % (self.sseHost, self.sseUplinkPort, self.sseHost, self.sseDownlinkPort))

    def toXml(self):
        '''Create an XML representation of this session configuration object. This is based on the TempTestConfig.xml file.

        Args
        -----
        None

        Returns
        --------
        The XML string representing this configuration (string)'''

        self.log.debug('mpcsutil.config.SessionConfig.toXml()')

        xml = '<?xml version="1.0"?>\n<Sessions>\n\t<Session version="3">\n\t\t<SessionId>\n\t\t\t<BasicSessionInfo>\n'

        if self.key is not None:

            xml += '\t\t\t\t<Number>' + str(self.number) + '</Number>\n'

        if self.spacecraftId is not None:

            xml += '\t\t\t\t<SpacecraftId>' + str(self.spacecraftId) + '</SpacecraftId>\n'

        if self.name is not None:

            xml += '\t\t\t\t<Name>' + str(self.name) + '</Name>\n'

        if self.type is not None and self.type != "":

            xml += '\t\t\t\t<Type>' + '<![CDATA[' + str(self.type) + ']]>' + '</Type>\n'

        if self.description is not None and self.description != "":

            xml += '\t\t\t\t<Description>' + '<![CDATA[' + str(self.description) + ']]>' + '</Description>\n'

        xml += '\t\t\t</BasicSessionInfo>\n\t\t\t<Venue>\n'

        if self.user is not None:

            xml += '\t\t\t\t<User>' + str(self.user) + '</User>\n'

        if self.host is not None:

            xml += '\t\t\t\t<Host>' + str(self.host) + '</Host>\n'

        if self.hostId is not None:

            xml += '\t\t\t\t<HostId>' + str(self.hostId) + '</HostId>\n'

        if self.ladPort is not None:

            xml += '\t\t\t\t<LadPort>' + str(self.ladPort) + '</LadPort>\n'
        xml += '\t\t\t</Venue>\n'

        if self.startTime is not None:

            xml += '\t\t\t<StartTime>' + str(self.startTime) + '</StartTime>\n'

        if self.endTime is not None:

            xml += '\t\t\t<EndTime>' + str(self.endTime) + '</EndTime>\n'

        xml += '\t\t</SessionId>\n'

        if self.ampcsVersion is not None:

            xml += '\t\t<AmpcsVersion>' + str(self.ampcsVersion) + '</AmpcsVersion>\n'

        if self.fullName is not None:

            xml += '\t\t<FullName>' + str(self.fullName) + '</FullName>\n'

        if self.fswVersion is not None:

            xml += '\t\t<FswVersion>' + str(self.fswVersion) + '</FswVersion>\n'

        if self.sseVersion is not None:

            xml += '\t\t<SseVersion>' + str(self.sseVersion) + '</SseVersion>\n'

        if self.runFswDownlink is not None:

            xml += '\t\t<RunFswDownlink>' + str(self.runFswDownlink) + '</RunFswDownlink>\n'

        if self.runSseDownlink is not None:

            xml += '\t\t<RunSseDownlink>' + str(self.runSseDownlink) + '</RunSseDownlink>\n'

        if self.runUplink is not None:

            xml += '\t\t<RunUplink>' + str(self.runUplink) + '</RunUplink>\n'


        if self.fswDictionaryDirectory is not None:

            xml += '\t\t<FswDictionaryDirectory>' + '<![CDATA[' + str(self.fswDictionaryDirectory) + ']]>' + '</FswDictionaryDirectory>\n'

        if self.sseDictionaryDirectory is not None:

            xml += '\t\t<SseDictionaryDirectory>' + '<![CDATA[' + str(self.sseDictionaryDirectory) + ']]>' + '</SseDictionaryDirectory>\n'

        if self.outputDirectory is not None:

            xml += '\t\t<OutputDirectory>' + '<![CDATA[' +  str(self.outputDirectory) + ']]>' + '</OutputDirectory>\n'

        if self.outputDirOverridden is not None:

            xml += '\t\t<OutputDirOverridden>' + str(self.outputDirOverridden) + '</OutputDirOverridden>\n'


        xml += '\t\t<VenueInformation>\n'

        if self.venueType is not None:

            xml += '\t\t\t<VenueType>' + str(self.venueType) + '</VenueType>\n'

        if self.inputFormat is not None:

            xml += '\t\t\t<InputFormat>' + str(self.inputFormat) + '</InputFormat>\n'

        if self.downlinkConnectionType is not None:

            xml += '\t\t\t<DownlinkConnectionType>' + str(self.downlinkConnectionType) + '</DownlinkConnectionType>\n'

        if self.uplinkConnectionType is not None:

            xml += '\t\t\t<UplinkConnectionType>' + str(self.uplinkConnectionType) + '</UplinkConnectionType>\n'

        if self.testbedName is not None and self.testbedName != "" :

            xml += '\t\t\t<TestbedName>' + str(self.testbedName) + '</TestbedName>\n'

        temStr = self.venueType.upper()

        if temStr == "TESTBED" or temStr == "ATLO" and self.downlinkConnectionType != "UNKNOWN":

            xml += '\t\t\t<DownlinkStreamId>' + str(self.downlinkStreamId) + '</DownlinkStreamId>\n'

        if self.sessionDssId is not None and self.sessionDssId != 0 :

            xml += '\t\t\t<SessionDssId>' + str(self.sessionDssId) + '</SessionDssId>\n'

        if self.sessionVcid is not None and self.sessionVcid != "" :

            xml += '\t\t\t<SessioinVcid>' + str(self.sessionVcid) + '</SessionVcid>\n'

        if self.inputFile is not None and self.inputFile != "" :

            xml += '\t\t\t<InputFile>' + str(self.inputFile) + '</InputFile>\n'


        if self.downlinkConnectionType == "DATABASE" and self.databaseSessionKey is not None:

            xml += '\t\t\t<DatabaseSessionKey>' + str(self.databaseSessionKey) + '</DatabaseSessionKey>\n'

        if self.downlinkConnectionType == "DATABASE" and self.databaseSessionHost is not None:

            xml += '\t\t\t<DatabaseSessionHost>' + str(self.databaseSessionHost) + '</DatabaseSessionHost>\n'

        if self.jmsSubtopic is not None and self.jmsSubtopic != "":

            xml += '\t\t\t<JmsSubtopic>' + str(self.jmsSubtopic) + '</JmsSubtopic>\n'

        if self.topic is not None and self.topic != "":

            xml += '\t\t\t<Topic>' + str(self.topic) + '</Topic>\n'

        xml += '\t\t</VenueInformation>\n\t\t<HostInformation>\n'

        if self.fswDownlinkHost is not None and self.fswDownlinkHost != "":

            xml += '\t\t\t<FswDownlinkHost>' + str(self.fswDownlinkHost) + '</FswDownlinkHost>\n'

        if self.fswUplinkHost is not None and self.fswUplinkHost != "":

            xml += '\t\t\t<FswUplinkHost>' + str(self.fswUplinkHost) + '</FswUplinkHost>\n'

        if self.sseHost is not None and self.sseHost != "":

            xml += '\t\t\t<SseHost>' + str(self.sseHost) + '</SseHost>\n'

        if self.fswUplinkPort is not None and self.fswUplinkPort != "":

            xml += '\t\t\t<FswUplinkPort>' + str(self.fswUplinkPort) + '</FswUplinkPort>\n'

        if self.sseUplinkPort is not None and self.sseUplinkPort != "":

            xml += '\t\t\t<SseUplinkPort>' + str(self.sseUplinkPort) + '</SseUplinkPort>\n'

        if self.fswDownlinkPort is not None and self.fswDownlinkPort != "":

            xml += '\t\t\t<FswDownlinkPort>' + str(self.fswDownlinkPort) + '</FswDownlinkPort>\n'

        if self.sseDownlinkPort is not None and self.sseDownlinkPort != "":

            xml += '\t\t\t<SseDownlinkPort>' + str(self.sseDownlinkPort) + '</SseDownlinkPort>\n'

        xml += '\t\t</HostInformation>\n\t</Session>\n</Sessions>\n'

        return xml

    @staticmethod
    def get_from_database_csv(csv):

        #recordType,number,name,type,description,user,host,downlinkConnectionType,uplinkConnectionType,startTime,endTime,outputDirectory,outputDirOverridden,fswVersion,sseVersion,fswDictionaryDirectory,sseDictionaryDirectory,venueType,inputFormat,testbedName,downlinkStreamId,spacecraftId,ampcsVersion,fullName,fswDownlinkHost,fswUplinkHost,fswDownlinkPort,fswUplinkPort,sseHost,sseDownlinkPort,sseUplinkPort,inputFile,topic,jmsSubtopic,sessionDssId,sessionVcid,runFswDownlink,runSseDownlink,runUplink,databaseSessionKey,databaseSessionHost

        #"Session","1","pearl_session","","","pearl","pearl-3311523","CLIENT_SOCKET","UNKNOWN","2013-136T17:48:02.596","2013-136T17:49:03.993","/Users/pearl/Documents/MPCS_4576/msl_R6.0_Dev/dist/msl/test/2013/136/mpcs/pearl-3311523/pearl_pearl_session_2013_136T17_48_02_596","false","current","current","/Users/pearl/dict","/Users/pearl/dict","TESTSET","RAW_TF","","Normal","76","6.0.0B9/5.6.0","pearl_session/pearl-3311523.pearl/2013-136T17:48:02.596","pearl-3311523","","12346","-1","","-1","-1","/Users/pearl/Documents/MPCS_4576/msl_R6.0_Dev/dist/msl/bin","mpcs.msl.pearl-3311523.pearl","","0","","true","false","false","",""

        session = SessionConfig()

        #slice off leading/trailing quote mark & then use "split"
        #to separate out the CSV into pieces
        pieces = csv[1:-1].split('","')


        session.number = int(pieces[1])
        session.key = session.number
        session.name = pieces[2]
        session.type = pieces[3]
        session.description = pieces[4]
        session.user = pieces[5]
        session.host = pieces[6]
        session.downlinkConnectionType = pieces[7]
        session.uplinkConnectionType = pieces[8]
        session.startTime = pieces[9]
        session.endTime = pieces[10]
        session.outputDirectory = pieces[11]
        session.outputDirOverridden = pieces[12]
        session.fswVersion = pieces[13]
        session.sseVersion = pieces[14]
        session.fswDictionaryDirectory = pieces[15]
        session.sseDictionaryDirectory = pieces[16]
        session.venueType = pieces[17]
        session.inputFormat = pieces[18]
        session.testbedName = pieces[19]
        session.downlinkStreamId = pieces[20]
        session.spacecraftId = pieces[21]
        session.ampcsVersion = pieces[22]
        session.fullName = pieces[23]
        session.fswDownlinkHost = pieces[24]
        session.fswUplinkHost = pieces[25]
        session.fswDownlinkPort = pieces[26]
        session.fswUplinkPort = pieces[27]
        session.sseHost = pieces[28]
        session.sseDownlinkPort = pieces[29]
        session.sseUplinkPort = pieces[30]
        session.inputFile = pieces[31]
        session.topic = pieces[32]
        session.jmsSubtopic = pieces[33]
        session.sessionDssId = int(pieces[34])
        session.sessionVcid = pieces[35]
        session.runFswDownlink = pieces[36]
        session.runSseDownlink = pieces[37]
        session.runUplink = pieces[38]
        session.databaseSessionKey = pieces[39]
        session.databaseSessionHost = pieces[40]

        return session

    def get_fsw_dict_path(self):

        gds_config = GdsConfig()
        os.path.join(self.fswDictDir, gds_config.getMission(), self.fswVersion)

    def get_sse_dict_path(self):

        gds_config = GdsConfig()

        if gds_config.isSse():
            os.path.join(self.sseDictDir, gds_config.getMission(), self.sseVersion)
        else:
            os.path.join(self.sseDictDir, '%ssse' % (gds_config.getMission()), self.sseVersion)

    def get_product_dir(self):

        gds_config = GdsConfig()
        dir_property = 'product.storageSubdir'

        return os.path.join(self.outputDir, gds_config.getProperty(dir_property, 'products'))

    def isUplinkOnly(self):
        '''Returns whether or not the current mission is uplinkOnly.

        Args
        -----
        None

        Returns
        --------
        True if the mission is uplinkOnly, False otherwise (boolean)'''

        return  bool(self.downlinkConnectionType in [None, 'UNKNOWN'])


    def isDownlinkOnly(self):
        '''Returns whether or not the current mission is downlinkOnly.

        Args
        -----
        None

        Returns
        --------
        True if the mission is downlinkOnly, False otherwise (boolean)'''

        return  bool(self.uplinkConnectionType in [None, 'UNKNOWN'])

def test():
    import mpcsutil
    _config=mpcsutil.config.GdsConfig()
    print('GdsConfig.')

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
