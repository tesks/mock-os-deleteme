#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines classes used for defining Fixed Layout Views in the MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import perspective
from perspective.graphics import (Font, Color)
# import perspective.graphics
# import perspective.graphics.Font


_FONT_COURIER_10 = Font('Courier',10,perspective.graphics.NORMAL)
# _COLOR_WHITE = Color(255,255,255)
_COLOR_GRAY =  Color(200,200,200)
_COLOR_BLACK = Color(0,0,0)

class FixedLayout(object):
    ''' A FixedLayout object represents the definition of a single Fixed Layout View for
    use in the MPCS GUI perspective

    Object Attributes
    ------------------
    name - The name of the Fixed Layout. (string)
    coordinateType - the coordinate system used to define positions and sizes in the fixed view (CoordinateTypes)
    width - The width of the Fixed Layout, in the selected coordinate system (integer)
    height - The height of the Fixed Layout, in the selected coordinate system (integer)
    font - The default font to be used for all text in the Fixed Layout (Font)
    foreColor - The default foreground color to be used for all objects in the Fixed Layout (Color)
    backColor - The default background color to be used for all objects in the Fixed Layout (Color)
    fswDictDir - The FSW dictionary directory used to define the Fixed Layout (string)
    fswVersion - The FSW dictionary version used to define the Fixed Layout (string)
    sseDictDir - The SSE dictionary directory used to define the Fixed Layout (string)
    sseVersion - The SSE dictionary version used to define the Fixed Layout (string)
    showRecorded - Flag indicating whether this view shows realtime or recorded data (boolean)
    stalenessInterval - Interval in seconds before data is considered stale in this view (integer)
    missions - List of missions supported by this fixed layout (List)
    fields - The list of fixed field objects attached to this Fixed Layout (list)
    conditions - The list of channel confitions attached to this Fixed Layout (list)'''

    DEFAULT_STALENESS_INTERVAL = 600

    def __init__(self,
                 name,
                 coordinateType=perspective.coordinatetypes.PIXEL,
                 width=perspective.DEFAULT_FIXED_WIDTH,
                 height=perspective.DEFAULT_FIXED_HEIGHT,
                 defaultFont=_FONT_COURIER_10,
                 defaultForeColor=_COLOR_BLACK,
                 defaultBackColor=_COLOR_GRAY,
                 fswDictDir=perspective.fswDefaultDictDir,
                 fswVersion=perspective.fswDefaultVersion,
                 sseDictDir=perspective.sseDefaultDictDir,
                 sseVersion=perspective.sseDefaultVersion):

        '''Initializes the FixedLayout object.

        Args
        -----
        name - The name of the Fixed Layout. Required (no default) (string)
        coordinateType - the coordinate system used to define positions and sizes in the fixed view (PIXEL (default) or CHARACTER) (CoordinateTypes)
        width - The width of the Fixed Layout, in the selected coordinate system (integer)
        height - The height of the Fixed Layout, in the selected coordinate system (integer)
        defaultFont - The default font to be used for all text in the Fixed Layout (Font)
        defaultForeColor - The default foreground color to be used for all objects in the Fixed Layout (Color)
        defaultBackColor - The default background color to be used for all objects in the Fixed Layout (Color)
        fswDictDir - The FSW dictionary directory used to define the Fixed Layout (string)
        fswVersion - The FSW dictionary version used to define the Fixed Layout (string)
        sseDictDir - The SSE dictionary directory used to define the Fixed Layout. May be None. (string)
        sseVersion - The SSE dictionary version used to define the Fixed Layout. May be None. (string)'''

        _gdsConfig = mpcsutil.config.GdsConfig()

        self.setName(name)
        self.setWidth(width)
        self.setHeight(height)
        self.setFont(defaultFont)
        self.setForegroundColor(defaultForeColor)
        self.setBackgroundColor(defaultBackColor)
        self.setCoordinateType(coordinateType)
        self.version = perspective.FIXED_LAYOUT_VERSION
        self.setFswDictionaryDir(fswDictDir)
        self.setFswVersion(fswVersion)
        self.sseDictDir = None
        self.sseVersion = None
        if sseDictDir is not None:
            self.setSseDictionaryDir(sseDictDir)
        if sseVersion is not None:
            self.setSseVersion(sseVersion)
        self.setShowRecorded(False)
        self.setStalenessInterval(FixedLayout.DEFAULT_STALENESS_INTERVAL)
        self.fields = []
        self.missions = []
        self.conditions = []
        self.addMission(_gdsConfig.getMission());

    def getName(self):
        ''' Gets the name of the FixedLayout

        Returns
        -------
        name - the Fixed Layout name (string)'''

        return self.name

    def getWidth(self):
        ''' Gets the width of the FixedLayout. If the coordinate type is PIXEL, the
        return value should be interpreted as pixel count; if CHARACTER, the
        return value should be interpreted as character count.

        Returns
        --------
        width - X dimension of the layout (integer)'''

        return self.width

    def getHeight(self):
        ''' Gets the height of the FixedLayout. If the coordinate type is PIXEL, the
        return value should be interpreted as pixel count; if CHARACTER, the
        return value should be interpreted as character count.

        Returns
        --------
        height - Y dimension of the layout (integer)'''

        return self.height

    def getFont(self):
        ''' Gets the default font of the FixedLayout. The default font is used for
        display of all text fields that do not supply their own fonts.

        Returns
        --------
        font - the default font for the layout (Font)'''

        return self.font

    def getForegroundColor(self):
        ''' Gets the default foreground color of the FixedLayout. The default foreground
        color is used for drawing of all fields that do not supply their own foreground
        color.

        Returns
        --------
        color - the default foreground color for the layout (Color)'''

        return self.foreColor

    def getBackgroundColor(self):
        ''' Gets the default background color of the FixedLayout. The default background
        color is used for drawing of all fields that do not supply their own background
        color.

        Returns
        --------
        color - the default background color for the layout (Color)'''

        return self.backColor

    def getFswDictionaryDir(self):
        ''' Gets the FSW dictionary directory used when defining the FixedLayout.

        Returns
        -------
        directory - FSW dictionary directory (string)'''

        return self.fswDictDir

    def getFswVersion(self):
        ''' Gets the FSW version directory used when defining the FixedLayout.

        Returns
        -------
        version - FSW dictionary version (string)'''

        return self.fswVersion

    def getSseDictionaryDir(self):
        ''' Gets the SSE dictionary directory used when defining the FixedLayout.

        Returns
        -------
        directory - SSE dictionary directory (string)'''

        return self.sseDictDir

    def getSseVersion(self):
        ''' Gets the SSE dictionary version used when defining the FixedLayout.

        Returns
        -------
        version - SSE dictionary version (string)'''

        return self.sseVersion

    def getCoordinateType(self):
        ''' Gets the coordinate type used by the FixedLayout.

        Returns
        -------
        type - coordinate type (PIXEL or CHARACTER) (CoordinateType)'''

        return self.coordinateType

    def getShowRecorded(self):
        ''' Gets the flag indicating whether this fixed layout view displays recorded or
        realtime data.

        Returns
        -------
        showRecorded - True if this view should display recorded data, False if realtime (boolean)'''

        return self.showRecorded

    def getStalenessInterval(self):
        '''Gets the interval, in seconds, after which data in this fixed layout should be considered stale.

        Returns
        -------
        interval - the staleness interval in seconds (integer)'''

        return self.stalenessInterval

    def setName(self, name):
        '''Sets the name of the FixedLayout.

        Args
        ----
        name - the name of the FixedLayout'''

        if name is None:
            raise mpcsutil.err.InvalidInitError('The name argument cannot be None.')
        self.name = str(name)

    def setWidth(self, width):
        ''' Sets the width of the FixedLayout. If the coordinate type is PIXEL, the
        input value will be interpreted as pixel count; if CHARACTER, the
        input value will be interpreted as character count.

        Args
        ----
        width - X dimension of the layout (integer)'''

        if width is None or int(width) <= 0:
            raise mpcsutil.err.InvalidInitError('The width argument cannot be None and must be greater than 0.')

        self.width = int(width)

    def setHeight(self, height):
        ''' Sets the height of the FixedLayout. If the coordinate type is PIXEL, the
        input value will be interpreted as pixel count; if CHARACTER, the
        input value will be interpreted as character count.

        Args
        ----
        height - Y dimension of the layout (integer)'''

        if height is None or int(height) <= 0:
            raise mpcsutil.err.InvalidInitError('The height argument cannot be None and must be greater than 0.')

        self.height = int(height)

    def setFont(self, font):
        ''' Sets the default font of the FixedLayout. The default font is used for
        display of all text fields that do not supply their own fonts.

        Returns
        --------
        font - the default font for the layout (Font)'''
        if font is None:
            raise mpcsutil.err.InvalidInitError('The font argument cannot be None.')
        if not isinstance(font, (Font, type(_FONT_COURIER_10))):
            raise mpcsutil.err.InvalidInitError('The font argument must be of type Font.')
        if font.getReverse() is True:
            raise mpcsutil.err.InvalidInitError('The default font for a fixed layout cannot be reverse video.')

        self.font = font

    def setForegroundColor(self, color):
        ''' Sets the default foreground color of the FixedLayout. The default foreground
        color is used for drawing of all fields that do not supply their own foreground
        color.

        Args
        ----
        color - the default foreground color for the layout (Color)'''

        if color is None:
            raise mpcsutil.err.InvalidInitError('The foreground argument cannot be None.')
        if not isinstance(color, (Color, type(_COLOR_GRAY))):
            raise mpcsutil.err.InvalidInitError('The foreground argument must be of type Color.')

        self.foreColor = color

    def setBackgroundColor(self, color):
        ''' Sets the default background color of the FixedLayout. The default background
        color is used for drawing of all fields that do not supply their own background
        color.

        Args
        ----
        color - the default background color for the layout (Color)'''

        if color is None:
            raise mpcsutil.err.InvalidInitError('The background argument cannot be None.')
        if not isinstance(color, (Color, type(_COLOR_GRAY))):
            raise mpcsutil.err.InvalidInitError('The background argument must be of type Color.')

        self.backColor = color

    def setFswDictionaryDir(self, directory):
        ''' Sets the FSW dictionary directory used when defining the FixedLayout.

        Args
        ----
        directory - FSW dictionary directory (string)'''

        if directory is None:
            raise mpcsutil.err.InvalidInitError('The FSW directory argument cannot be None.')

        self.fswDictDir = str(directory)

    def setFswVersion(self, version):
        ''' Sets the FSW version directory used when defining the FixedLayout.

        Args
        ----
        version - FSW dictionary version (string)'''

        if version is None:
            raise mpcsutil.err.InvalidInitError('The FSW version argument cannot be None.')

        self.fswVersion = str(version)

    def setSseDictionaryDir(self, directory):
        ''' Sets the SSE dictionary directory used when defining the FixedLayout.

        Args
        ----
        directory - SSE dictionary directory (string)'''

        if directory is None:
            raise mpcsutil.err.InvalidInitError('The SSE directory argument cannot be None.')

        self.sseDictDir = str(directory)

    def setSseVersion(self, version):
        ''' Sets the SSE dictionary version used when defining the FixedLayout.

        Args
        ----
        version - SSE dictionary version (string)'''

        if version is None:
            raise mpcsutil.err.InvalidInitError('The SSE version argument cannot be None.')

        self.sseVersion = str(version)

    def setCoordinateType(self, type):
        ''' Sets the coordinate type used by the FixedLayout.

        Args
        ----
        type - coordinate type (PIXEL or CHARACTER) (CoordinateTypes)'''

        if type is None or not str(type) in perspective.coordinatetypes.CoordinateTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The coordinate type cannot be None and must be a valid CoordinateTypes value.')

        self.coordinateType = str(type)

    def setShowRecorded(self, recorded):
        ''' Sets the flag indicating whether this fixed layout view displays recorded or
        realtime data.

        Args
        ----
        recorded - True if this view should display recorded data, False if realtime (boolean)'''

        if recorded is None or not recorded in [True, False]:
            raise mpcsutil.err.InvalidInitError("The recorded argument cannot be None and must be a boolean value");

        self.showRecorded = bool(recorded)

    def setStalenessInterval(self, interval):
        '''Gets the interval, in seconds, after which data in this fixed layout should be considered stale.

        Args
        ----
        interval - the staleness interval in seconds (integer)'''

        if interval is None or int(interval) <= 0:
            raise mpcsutil.err.InvalidInitError("The interval argument cannot be None and must be greater than 0")

        self.stalenessInterval = int(interval)

    def addField(self,field):
        '''Adds a fixed field to the end of the field list for this Fixed Layout.

        Args
        ----
        field - the fixed field to add (AbstractFixedField)'''

        if field is None or not isinstance(field, perspective.fixedfields.AbstractFixedField):
            raise mpcsutil.err.InvalidInitError('The field cannot be None and must extend type AbstractFixedField.')

        self.fields.append(field)

    def addCondition(self,condition):
        '''Adds a channel condition to the condition list for this Fixed Layout.

        Args
        ----
        condition - the condition to add (Condition)'''

        if condition is None or not isinstance(condition, perspective.fixedfields.Condition):
            raise mpcsutil.err.InvalidInitError('The condition cannot be None and must be of type Condition.')

        self.conditions.append(condition)

    def addMission(self,mission):
        '''Adds a mission to the end of the mission list for this Fixed Layout.

        Args
        ----
        mission - the mission to add (string)'''

        if mission is None:
            raise mpcsutil.err.InvalidInitError('The mission cannot be None.')

        self.missions.append(mission)

    def getMissions(self):
        ''' Returns the list of missions supported by this fixed view.

        Returns
        -------
        missions - the supported missions (list of string)'''

        return self.missions

    def getFields(self):
        ''' Returns the list of fixed fields attached to this fixed view.

        Returns
        -------
        fields - the attached fixed fields (list of AbstractFixedFields)'''

        return self.fields

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = "Fixed Layout '%s' (Type=%s)\n\tHeight=%d\n\tWidth=%d\n\tFont=%s\n\tBackground Color=%s\n\tForegroundColor=%s\n" % \
        (self.name, self.coordinateType, self.height, self.width, str(self.font), str(self.foreColor), str(self.backColor))
        for f in self.fields:
            returnStr += str(f)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '<View name="%s" type="Fixed Layout" version="%s">\n' % (self.name, self.version)
        returnStr += '\t<viewClass>%s</viewClass>\n' % (perspective.FIXED_VIEW_CLASS)
        returnStr += '\t<coordinateType>%s</coordinateType>\n' % (self.coordinateType)
        returnStr += '\t<fswDictionaryDir>%s</fswDictionaryDir>\n' % (self.fswDictDir)
        returnStr += '\t<fswVersion>%s</fswVersion>\n' % (self.fswVersion)
        if self.sseDictDir is not None:
            returnStr += '\t<sseDictionaryDir>%s</sseDictionaryDir>\n' % (self.sseDictDir)
        if self.sseVersion is not None:
            returnStr += '\t<sseVersion>%s</sseVersion>\n' % (self.sseVersion)
        returnStr += '\t<defaultFont>%s</defaultFont>\n' % (repr(self.font))
        returnStr += '\t<defaultBackground>%s</defaultBackground>\n' % (repr(self.backColor))
        returnStr += '\t<defaultForeground>%s</defaultForeground>\n' % (repr(self.foreColor))
        returnStr += '\t<viewTitleEnabled>false</viewTitleEnabled>\n'
        returnStr += '\t<preferredWidth>%d</preferredWidth>\n' % (self.width)
        returnStr += '\t<preferredHeight>%d</preferredHeight>\n' % (self.height)

        for m in self.missions:
            returnStr += '\t<mission>%s</mission>\n' % m

        returnStr += '\t<stalenessInterval>%d</stalenessInterval>\n' % (self.stalenessInterval)
        returnStr += '\t<showRecorded>%s</showRecorded>\n' % (str(self.showRecorded).lower())

        for c in self.conditions:
            returnStr += repr(c)

        for f in self.fields:
            returnStr += repr(f)

        returnStr += '</View>\n'

        return returnStr

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
