#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains all the objects related to command.  There are objects for
hardware commands, sequence directives, fsw commands, sse commands, file loads, raw uplink data files, and scmfs.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil.config
import mpcsutil.err
import sys
import xml.parsers.expat
import re

_log = lambda : logging.getLogger('mpcs.util')

class HardwareCommand(object):
    '''This object represents a hardware command that can be sent to the FSW.

    Object Attributes
    ------------------
    stem - The command stem (a.k.a. mnemonic) (string)
    eventTime - The time that the JMS message with this command was sent out by MPCS. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML Format is:

        <HardwareCommandMessage eventTime="$eventTime">
            <HardwareCommand>
                <CommandString>$commandString</CommandString>
            </HardwareCommand>
        </HardwareCommandMessage>
        (Defined by the templates/common/message/HardwareCommand/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of a hardware command (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.HardwareCommand.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse hardware command XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^HardwareCommandMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if re.match(r'^CommandString$', name):
            self.stem = self._xmlData.strip()

    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __init__(self,stem=None):
        '''Initialize this hardware command

        Args
        -----
        stem - The stem/mnemonic for this HW command as specified in the command dictionary (string)

        Returns
        --------
        None'''



        _log().debug('mpcsutil.command.HardwareCommand()')

        self.receiveTime = 0
        self.stem = stem
        self.eventTime = ''

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.HardwareCommand.__str__()')

        return self.stem

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.HardwareCommand.__repr__()')

        return 'mpcsutil.command.HardwareCommand(stem=\'%s\')' % self.stem

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.HardwareCommand.__eq__()')

        if other == None:
            return False

        #Ignore Event Time

        return   self.stem == other.stem

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.HardwareCommand.__ne__()')

        return self.__eq__(other) == False


class FlightSoftwareCommand(object):
    '''This object represents an FSW command that can be sent to the FSW.

    Object Attributes
    ------------------
    stem - The command stem (a.k.a. mnemonic) (string)
    argVals - A list of the argument values (in order) for this command (list of various types)
    eventTime - The time that the JMS message with this command was sent out by MPCS. ISO-formatted. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <FlightSoftwareCommandMessage eventTime="$eventTime">
            <FlightSoftwareCommand>
                <CommandString>$commandString</CommandString>
            </FlightSoftwareCommand>
        </FlightSoftwareCommandMessage>
        (Defined by templates/common/message/FlightSoftwareCommand/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of an FSW command (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse flight software command XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^FlightSoftwareCommandMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if re.match(r'^CommandString$', name):
            self.setFromString(self._xmlData.strip())


    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __init__(self,stem=None,argValues=[],cmdString=None):
        '''Initialize this FSW command

        Args
        -----
        stem - The command stem (a.k.a. mnemonic) (string)
        argVals - A list of the argument values (in order) for this command (list of various types)
        cmdString - A comma-separated string like the one that would be passed to the chill_send_cmd command line
                    (for more information, see the documentation for setFromString(...) in this object)

        Returns
        --------
        None

        Raises
        ------------------
        mtak.err.InvalidInitError - You may specify a combination of stem/argValues OR a cmdString, but not both or
                                    this exception will be raised.'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand()')

        if cmdString is not None and (stem is not None or argValues):
            raise mpcsutil.err.InvalidInitError('A FlightSoftwareCommand can be created from a stem with argument values OR a command string.  Both may not be specified.')
        elif cmdString is not None:
            self.setFromString(cmdString)
        else:
            self.stem = stem
            self.argVals = argValues

        self.receiveTime = 0
        self.eventTime = ''

    def setFromString(self,cmdString):
        '''Set the attributes of this object from a CSV-formatted string like the one that would comprise the
        input to chill_send_cmd

        Args
        -----
        cmdString - A CSV-formatted string in the format described below:

        Individual Command Format:
        'stem,arg1_value,...,argN_value'
                OR
        '0xopcode,arg1_value,...,argN_value'
        (NOTE: The entire command is enclosed in single quotes)

        Argument Value Format (within the command format):
                Fill Arg Value: Fill arguments are not input (skip them)!
                Numeric Arg Value: value
                Look Arg Value: value
                String Arg Value: "value"
                (NOTE: String values should be enclosed in double quotes)
                Repeat Arg Value: #_repeats,arg1_value,...,argN_value

        Any argument value may be specified in ASCII format by simply typing its value.
        Any argument value may be specified  in hexadecimal or binary instead of ASCII by
        preceding it with a 0x or 0b.  Similarly, an opcode may be specified in place of the
        stem  value if it is preceded with a 0x or a 0b.  If an argument has a default value,
        the ASCII value "default" can be specified on the
        command line in place of an argument value.

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.setFromString()')

        cmdPieces = cmdString.split(",")
        self.setFromList(cmdPieces)

    def setFromList(self,cmdPieces):
        '''Set the values of this object's attributes from a list

        Args
        -----
        cmdPieces - A list of the pieces of this FSW command.  The first item in the list (cmdPieces[0])
        should be the command stem/mnemonic.  The remaining items in the list (cmdPieces[1:]),if any, should
        be all the argument values for this command in order.

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.setFromList()')

        if cmdPieces:
            self.stem = cmdPieces[0]
            self.argVals = cmdPieces[1:]
        else:
            raise mpcsutil.err.InvalidInitError('A flight software command list may not be empty')

    def __str__(self):
        ''' x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.__str__()')

        outStr = self.stem

        if self.argVals:
            outStr = outStr + ',' + ','.join([str(argval) for argval in self.argVals])

        return outStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.__repr__()')

        return 'mpcsutil.command.FlightSoftwareCommand(cmdString=\'%s\')' % self.__str__()

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.__eq__()')

        if other == None:
            return False

        #Ignore Event Time
        return  (self.stem == other.stem and
                self.argVals == other.argVals)

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.FlightSoftwareCommand.__ne__()')

        return self.__eq__(other) == False

class SequenceDirective(FlightSoftwareCommand):
    '''This object represents a sequence directive that can be sent to the FSW.

    The only difference is that it is sent as a VC-2 and its XML format is different.'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <SequenceDirectiveMessage eventTime="$eventTime">
            <SequenceDirective>
                <CommandString>$commandString</CommandString>
            </SequenceDirective>
        </SequenceDirectiveMessage>
        (Defined by templates/common/message/SequenceDirective/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of a sequence directive (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.SequenceDirective.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse sequence directive XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def __init__(self,stem=None,argValues=[],cmdString=None):
        '''Initialize this Sequence Directive command

        Args
        -----
        stem - The command stem (a.k.a. mnemonic) (string)
        argVals - A list of the argument values (in order) for this command (list of various types)
        cmdString - A comma-separated string like the one that would be passed to the chill_send_cmd command line
                    (for more information, see the documentation for setFromString(...) in this object)

        Raised Exceptions
        ------------------
        mtak.err.InvalidInitError - You may specify a combination of stem/argValues OR a cmdString, but not both or
                                    this exception will be raised.'''

        _log().debug('mpcsutil.command.SequenceDirective()')

        FlightSoftwareCommand.__init__(self,stem,argValues,cmdString)

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^SequenceDirectiveMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def __str__(self):
        ''' x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.SequenceDirective.__str__()')

        outStr = self.stem

        if self.argVals:
            outStr = outStr + ',' + ','.join([str(argval) for argval in self.argVals])

        return outStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.SequenceDirective.__repr__()')

        return 'mpcsutil.command.SequenceDirective(cmdString=\'%s\')' % self.__str__()

class SseCommand(object):
    '''This object represents an SSE command that can be sent to the SSE.

    Object Attributes
    ------------------
    cmdString - The complete SSE command directive (beginning with the SSE prefix in the GDS configuration) (string)
    eventTime - The time that the JMS message with this command was sent out by MPCS. ISO-formatted. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <SseCommandMessage eventTime="$eventTime">
            <SseCommand>
                <CommandString>$commandString</CommandString>
            </SseCommand>
        </SseCommandMessage>
        (Defined by templates/common/message/SseCommand/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of an SSE command (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.SseCommand.parseFromXmlString()')

        self._xmlData = ''

        try:

            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse SSE command XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^SseCommandMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if re.match(r'^CommandString$', name):
            self.cmdString = self._xmlData.strip()

    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __new__(cls, *a, **k):
        ''' T.__new__(S, ...) -> a new object with type S, a subtype of T

        This method is overriden to attach the SSE prefix from the configuration
        to each of the SSE command objects that is created.'''

        if not hasattr(cls,'ssePrefix'):
            gdsConfig = mpcsutil.config.GdsConfig()
            cls.ssePrefix = gdsConfig.getProperty('Uplink.SSE.commandPrefix','sse:')

        #newInstance = super(SseCommand,cls).__new__(cls, *a, **k)
        newInstance = super(SseCommand,cls).__new__(cls)
        newInstance.__ssePrefix = cls.ssePrefix

        return newInstance

    def __init__(self,cmdString=None):
        '''Initialize this SSE command

        Args
        -----
        cmdString - The command string for this SSE command.  If it does not begin with the
                    configured SSE prefix, the prefix will be added. (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.SseCommand()')

        self.__cmdString = ''
        self.receiveTime = 0
        self.eventTime = ''

        if cmdString is not None:
            self.setCmdString(cmdString)

    def getCmdString(self):
        '''Get the stored SSE command string.  This method is used as the getter
        for the 'cmdString' property

        Args
        -----
        None

        Returns
        --------
        The value of 'cmdString' '''

        _log().debug('mpcsutil.command.SseCommand.getCmdString()')

        return self.__cmdString

    def setCmdString(self,cmdString):
        '''Set the stored SSE command string...this method will add the SSE prefix if necessary.
        This method is used as the setter for the 'cmdString' property

        Args
        -----
        cmdString - The command string for this SSE command.  If it does not begin with the
                    configured SSE prefix, the prefix will be added. (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.SseCommand.setCmdString()')

        self.__cmdString = cmdString
        if not self.__cmdString.startswith(self.__ssePrefix):
            self.__cmdString = self.__ssePrefix + self.__cmdString

    cmdString = property(getCmdString,setCmdString)

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.SseCommand.__str__()')

        return self.cmdString

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.SseCommand.__repr__()')

        return 'mpcsutil.command.SseCommand(\'%s\')' % self.__str__()

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.SseCommand.__eq__()')

        if other == None:
            return False

        #Ignore Event Time

        return   self.cmdString == other.cmdString

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.SseCommand.__ne__()')

        return self.__eq__(other) == False

class FileLoad(object):
    '''This object represents a file upload that can be sent to the FSW.

    Object Attributes
    ------------------
    fileType = The type of file as defined in the FGICD.  Generally 0 for a normal file
               and 1 for a sequence file (int)
    sourceFile = The path to the file on local disk to be uploaded (string)
    targetFile = The target file path where the file will be placed on flight software (string)
    eventTime - The time that the JMS message with this file load was sent out by MPCS. ISO-formatted. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <FileLoadMessage eventTime="$eventTime">
            <FileLoad>
                <Source>$sourceFile</Source>
                <Destination>$destinationFile</Destination>
                <FileType>$fileType</FileType>
            </FileLoad>
        </FileLoadMessage>
        (Defined by templates/common/message/FileLoad/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of a file load

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FileLoad.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse file load XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^FileLoadMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if name == 'Source':
            self.sourceFile = self._xmlData.strip()
            self._xmlData = ''
        elif name == 'Destination':
            self.targetFile = self._xmlData.strip()
            self._xmlData = ''
        elif name == 'FileType':
            self.fileType = self._xmlData.strip()
            self._xmlData = ''

    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __init__(self,fileType=None,sourceFile=None,targetFile=None,loadString=None):
        '''Initialize this file load

        Args
        -----
        fileType - The type of file being uplinked (usually 0 for normal file, 1 for sequence file)
        sourceFile - The path to the file on local disk to be uploaded (string)
        targetFile - The target file path where the file will be placed on flight software (string)
        loadString - A CSV representation of a file load (string)
                     (See the 'setFromString' method on this object for more details)

        Raises
        ------------------
        mpcsutil.err.InvalidInitError - The fileType/sourceFile/targetFile can be specified OR the loadString can be specified,
                                    but this exception will raise if both are specified.'''

        _log().debug('mpcsutil.command.FileLoad()')

        if loadString is not None and (fileType is not None or sourceFile is not None or targetFile is not None):
            raise mpcsutil.err.InvalidInitError('A FileLoad can be created from a fileType, local file, and target file OR a load string.  Both sets of information may not be specified.')
        elif loadString is not None:
            self.setFromString(loadString)
        else:
            if fileType:
                self.fileType = fileType
            else:
                self.fileType = 0
            self.sourceFile = sourceFile
            self.targetFile = targetFile

        self.receiveTime = 0
        self.eventTime = ''

    def setFromString(self,loadString):
        '''Set the attributes of this object from a CSV string

        Args
        -----
        loadString - A CSV string representing a file load.  The string should be formatted as described below:

        {file_type1,}input_filename1,targetfile_name1 ... {file_typeN,}input_filenameN,target_filenameN

        file_type - Should be "1" if the file is a sequence file or a "0" otherwise.  This value is
                    optional and will default to "0" if omitted.  Any value that can be represented
                    by 7 bits will be accepted as input.
        input_file_name - The name (including path) of the file to send.
        target_file_name - The target file name on the spacecraft file system where this
                           file will be placed.

        A file to send should be specified in a comma-separated triple of the form file_type,input_file_name,target_file_name or
        a comma-separated double of the form input_file_name,target_file_name. If a double is specified, the file type will
        default to "0" (see below). Multiple doubles and/or triples can be specified on the same command line and should be separated by whitespace.

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FileLoad.setFromString()')

        loadPieces = loadString.split(',')
        self.setFromList(loadPieces)

    def setFromList(self,loadPieces):
        '''Set the attributes of this object from a list

        Args
        -----
        loadPieces - The list of file load data.  If the list has a length of 2, then
                     fileType is assumed to be zero and list[0] is the source file and
                     list[1] is the target file.  Otherwise, if the length is 3, then
                     fileType is list[0], source file is list[1], and target file is list[2].

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.FileLoad.setFromList()')

        length = len(loadPieces)

        if length == 2:
            self.fileType = 0
            self.sourceFile = loadPieces[0]
            self.targetFile = loadPieces[1]
        elif length == 3:
            self.fileType = loadPieces[0]
            self.sourceFile = loadPieces[1]
            self.targetFile = loadPieces[2]
        else:
            raise mpcsutil.err.InvalidInitError('A file load list must be 2 or 3 elements long')

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.FileLoad.__str__()')

        return '%s,%s,%s' % (self.fileType,self.sourceFile,self.targetFile)

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.FileLoad.__repr__()')

        return 'mpcsutil.command.FileLoad(loadString=\'%s\')' % self.__str__()

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.FileLoad.__eq__()')

        if other == None:
            return False

        #Ignore Event Time

        return  (str(self.fileType) == str(other.fileType) and
                 self.sourceFile == other.sourceFile and
                 self.targetFile == other.targetFile)

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.FileLoad.__ne__()')

        return self.__eq__(other) == False


class Scmf(object):
    '''This object represents a file upload that can be sent to the FSW.

    Object Attributes
    ------------------
    filename = The filename of the SCMF file on disk (string)
    eventTime - The time that the JMS message with this SCMF was sent out by MPCS. ISO-formatted. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <ScmfMessage eventTime="$eventTime">
            <ScmfFilename>$filename</ScmfFilename>
        </ScmfMessage>
        (Defined by templates/common/message/Scmf/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of an scmf (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.Scmf.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse SCMF XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^ScmfMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if re.match(r'^ScmfFilename$', name):
            self.filename = self._xmlData.strip()

    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __init__(self,filename=None):
        '''Initialize this SCMF object

        Args
        -----
        filename - The name of the SCMF file on disk (string)'''

        _log().debug('mpcsutil.command.Scmf()')

        self.receiveTime = 0
        self.filename = filename
        self.eventTime = ''

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.Scmf.__str__()')

        return self.filename

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.Scmf.__repr__()')

        return 'mpcsutil.command.Scmf(filename=\'%s\')' % self.filename

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.Scmf.__eq__()')

        if other == None:
            return False

        #Ignore Event Time

        return   self.filename == other.filename

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.Scmf.__ne__()')

        return self.__eq__(other) == False

class RawUplinkDataFile(object):
    '''This object represents a file upload that can be sent to the FSW.

    Object Attributes
    ------------------
    filename = The filename of the raw data file on disk (string)
    eventTime - The time that the JMS message with this data file was sent out by MPCS. ISO-formatted. (string)'''

    def parseFromXmlString(self,xmlString):
        '''Parse the input XML string and use it to set the attributes of this object

        XML format is:

        <RawUplinkDataFileMessage eventTime="$eventTime">
            <DataFilename>$filename</DataFilename>
        </RawUplinkDataFileMessage>
        (Defined by templates/common/message/RawUplinkData/Mtak.vm)

        Args
        -----
        xmlString - An XML-formatted string representation of a raw uplink data file (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.RawUplinkDataFile.parseFromXmlString()')

        self._xmlData = ''

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement
            parser.CharacterDataHandler = self._characters

            parser.Parse(xmlString,True)
        except xml.parsers.expat.ExpatError:
            _log().error('Failed to parse SCMF XML %s: %s' % (xmlString,sys.exc_info()))
            raise

        del self._xmlData

    def _startElement(self,name,attrs):
        '''A SAX parsing callback for the start element event

        Args
        -----
        name - The name of the start element that was encountered (string)
        attrs - The XML attributes of the element (dictionary)

        Returns
        --------
        None'''

        if re.match(r'^RawUplinkDataMessage$', name) and ('eventTime' in attrs.keys()):
            self.eventTime = attrs['eventTime']

    def _endElement(self,name):
        '''A SAX parsing callback for the end element event

        Args
        -----
        name - The name of the end element that was encountered (string)

        Returns
        --------
        None'''

        if re.match(r'^DataFilename$', name):
            self.filename = self._xmlData.strip()

    def _characters(self,data):
        '''A SAX parsing callback for the character data event

        Args
        -----
        data - The character data that the SAX parser encountered (string)

        Returns
        --------
        None'''

        self._xmlData += data

    def __init__(self,filename=None):
        '''Initialize this SCMF object

        Args
        -----
        filename - The name of the data file on disk (string)

        Returns
        --------
        None'''

        _log().debug('mpcsutil.command.RawUplinkDataFile()')

        self.receiveTime = 0
        self.filename = filename
        self.eventTime = ''

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        _log().debug('mpcsutil.command.RawUplinkDataFile.__str__()')

        return self.filename

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        _log().debug('mpcsutil.command.RawUplinkDataFile.__repr__()')

        return 'mpcsutil.command.RawUplinkDataFile(filename=\'%s\')' % self.filename

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        _log().debug('mpcsutil.command.RawUplinkDataFile.__eq__()')

        if other == None:
            return False

        #Ignore Event Time

        return   self.filename == other.filename

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        _log().debug('mpcsutil.command.RawUplinkDataFile.__ne__()')

        return self.__eq__(other) == False

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
