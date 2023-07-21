#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains common exceptions that will be used by the
AMPCS Utility Toolkit for Operations (AUTO).
"""

from __future__ import (absolute_import, division, print_function)


#import xml.sax
import mpcsutil.evr
import mpcsutil.channel
import xml.parsers.expat

class GlobalLadEvrParser(xml.sax.handler.ContentHandler):
#MPCS-5411  10/02/13: Removed concept of SCN from Global LAD
    def __init__(self, eventId=None):
        self._eventId = eventId
        self._evrList = []
        self._inEvrList = False
        self._inEvr = False
        self._currContent = ''
#        self._scn = 0
        self._primaryTimestamp = 0
        self._secondaryTimestamp = 0

    def startElement(self, name, attrs):
        if name == 'EvrList':
            self._inEvrList = True
#            self._scn = attrs['scn'] if 'scn' in attrs else 0
            self._primaryTimestamp = attrs['primaryTimestamp'] if 'primaryTimestamp' in attrs else 0
            self._secondaryTimestamp = attrs['secondaryTimestamp'] if 'secondaryTimestamp' in attrs else 0
        elif name == 'Evr':
            self._inEvr = True

    def characters(self, content):
        if self._inEvr:
            self._currContent += content

    def endElement(self, name):
        if name == 'Evr':
            self._inEvr = False

            newEvr = mpcsutil.evr.Evr(csvString=self._currContent)

            #add to the list only if the EVR matches our event ID
            if self._eventId is not None and self._eventId == newEvr.eventId:
                self._evrList.append(newEvr)

            self._currContent = ''

class GlobalLadEhaParser(xml.sax.handler.ContentHandler):
#MPCS-5411  10/02/13: Removed concept of SCN from Global LAD, so no more queryToken
    def __init__(self):
        self._ehaList = []
        self._inEhaList = False
        self._inEha = False
        self._currContent = ''
#        self._scn = 0
        self._primaryTimestamp = 0
        self._secondaryTimestamp = 0

    def startElement(self, name, attrs):
        if name == 'EhaList':
            self._inEhaList = True
#            self._scn = attrs['scn'] if 'scn' in attrs else 0
            self._primaryTimestamp = attrs['primaryTimestamp'] if 'primaryTimestamp' in attrs else 0
            self._secondaryTimestamp = attrs['secondaryTimestamp'] if 'secondaryTimestamp' in attrs else 0
        elif name == 'Eha':
            self._inEha = True

    def characters(self, content):
        if self._inEha:
            self._currContent += content

    def endElement(self, name):
        if name == 'Eha':
            self._inEha = False
            self._ehaList.append(mpcsutil.channel.ChanVal(csvString=self._currContent))
            self._currContent = ''

class GlobalLadMetadataParser(xml.sax.handler.ContentHandler):

    def __init__(self):
        self._name = ''
        self._id = ''
        self._type = ''
        self._primaryTimeSystem = ''
        self._secondaryTimeSystem = ''
        self._dataFormat = ''

        self._inName = False
        self._inId = False
        self._inType = False
        self._inPrimary = False
        self._inSecondary = False
        self._inDataFormat = False

    def startElement(self, name, attrs):
        if name == 'Name':
            self._inName = True
        elif name == 'ID':
            self._inId = True
        elif name == 'Type':
            self._inType = True
        elif name == 'Primary':
            self._inPrimary = True
        elif name == 'Secondary':
            self._inSecondary = True
        elif name == 'DataFormat':
            self._inDataFormat = True

    def characters(self, content):
        if self._inName:
            self._name += content
        elif self._inId:
            self._id += content
        elif self._inType:
            self._type += content
        elif self._inPrimary:
            self._primaryTimeSystem += content
        elif self._inSecondary:
            self._secondaryTimeSystem += content
        elif self._inDataFormat:
            self._dataFormat += content

    def endElement(self, name):
        if name == 'Name':
            self._inName = False
        elif name == 'ID':
            self._inId = False
        elif name == 'Type':
            self._inType = False
        elif name == 'Primary':
            self._inPrimary = False
        elif name == 'Secondary':
            self._inSecondary = False
        elif name == 'DataFormat':
            self._inDataFormat = False

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
