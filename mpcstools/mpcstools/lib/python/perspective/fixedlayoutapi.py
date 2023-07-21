#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the wrapper API for generating Fixed Layout views for an MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)


import mpcsutil
import types
import six
import perspective.conditiontypes
import perspective.conditionfieldtypes
import perspective.channeltypes
from perspective.graphics import Color
from perspective.graphics import Font

# Line style constants
SOLID = perspective.linestyletypes.SOLID
DASHED = perspective.linestyletypes.DASHED

# Font style constants
NORMAL = perspective.graphics.NORMAL
BOLD = perspective.graphics.BOLD
ITALIC = perspective.graphics.ITALIC

# Font constants
FONT_COURIER_8 = Font('Courier',8,NORMAL)
FONT_COURIER_8_REVERSE = Font('Courier',8,NORMAL,True)
FONT_COURIER_8_BOLD = Font('Courier',8,BOLD)
FONT_COURIER_8_ITALIC = Font('Courier',8,ITALIC)
FONT_COURIER_10 = Font('Courier',10,NORMAL)
FONT_COURIER_10_REVERSE = Font('Courier',10,NORMAL,True)
FONT_COURIER_10_BOLD = Font('Courier',10,BOLD)
FONT_COURIER_10_ITALIC = Font('Courier',10,ITALIC)
FONT_COURIER_12_FONT = Font('Courier',12,NORMAL)
FONT_COURIER_12_REVERSE = Font('Courier',12,NORMAL,True)
FONT_COURIER_12_BOLD = Font('Courier',12,BOLD)
FONT_COURIER_12_ITALIC = Font('Courier',12,ITALIC)

# Color constants
COLOR_WHITE = Color(255,255,255)
COLOR_BLACK = Color(0,0,0)
COLOR_YELLOW = Color(255, 255, 0)
COLOR_GREEN = Color( 0, 255, 0)
COLOR_DARK_GREEN = Color( 0, 150, 0)
COLOR_RED = Color(255, 0, 0)
COLOR_GREY = Color(200, 200, 200)
COLOR_DARK_GREY = Color(109, 109, 109)
COLOR_BLUE = Color(0, 0, 255)
COLOR_DARK_RED = Color(220,0,0)
COLOR_LIGHT_GREY = Color(220,220,220)
COLOR_LIGHT_AQUA_BLUE = Color(175,238,238)
COLOR_ORANGE = Color(255,127,36)
COLOR_PURPLE = Color(191,62,255)
COLOR_PINK = Color(255,181,197)

# Comparison conditions
COMPARE_LT = perspective.conditiontypes.LT
COMPARE_LE = perspective.conditiontypes.LE
COMPARE_GT = perspective.conditiontypes.GT
COMPARE_GE = perspective.conditiontypes.GE
COMPARE_EQ = perspective.conditiontypes.EQ
COMPARE_NE = perspective.conditiontypes.NE

# Channel data types
TYPE_UNSIGNED_INT = perspective.channeltypes.UNSIGNED_INT
TYPE_SIGNED_INT = perspective.channeltypes.SIGNED_INT
TYPE_BOOLEAN = perspective.channeltypes.BOOLEAN
TYPE_FLOAT = perspective.channeltypes.FLOAT
TYPE_DOUBLE = perspective.channeltypes.DOUBLE
TYPE_STATUS = perspective.channeltypes.STATUS
TYPE_ASCII = perspective.channeltypes.ASCII
TYPE_DIGITAL = perspective.channeltypes.DIGITAL

# Condition field types
FIELD_DN = perspective.conditionfieldtypes.DN
FIELD_RAW = perspective.conditionfieldtypes.RAW
FIELD_EU = perspective.conditionfieldtypes.EU
FIELD_VALUE = perspective.conditionfieldtypes.VALUE
FIELD_STATUS = perspective.conditionfieldtypes.STATUS
FIELD_SCET = perspective.conditionfieldtypes.SCET
FIELD_SCLK = perspective.conditionfieldtypes.SCLK
FIELD_ERT = perspective.conditionfieldtypes.ERT
FIELD_MST = perspective.conditionfieldtypes.MST
FIELD_LST = perspective.conditionfieldtypes.LST
FIELD_RCT = perspective.conditionfieldtypes.RCT

# The fixed layout object being built
_currentLayout = None

def createPixelLayout(name, width, height):
    '''Creates an empty fixed layout in which the coordinate system is PIXEL. All
       coordinates specified in the layout will be assumes to be pixel offsets from 0,0,
       were 0,0 is at the upper left corner of the fixed layout canvas. The default
       foreground color will be black, and the default background will be white. The
       default font for the layout will be a fixed-width courier 10 point font. By default,
       the fixed layout will be configured to display only realtime data.

    Args
    ----
    The name of the fixed layout. Cannot be None. (string)
    width - width of the fixed layout in pixels (integer)
    height - height of the fixed layout in pixels (integer)'''

    global _currentLayout

    if _currentLayout is not None:
        raise mpcsutil.err.InvalidStateError("Current layout exists. Call discardLayout() to get rid of it")

    _currentLayout = perspective.fixedlayout.FixedLayout(name,
                                                         perspective.coordinatetypes.PIXEL,
                                                         width,
                                                         height)

def createCharacterLayout(name, width, height):
    '''Creates an empty fixed layout in which the coordinate system is CHARACTER. All
       coordinates specified in the layout will be assumed to be character offsets from 0,0,
       where 0,0 is at the upper left corner of the fixed layout canvas. The default
       foreground color will be black, and the default background will be white. The
       default font for the layout will be a fixed-width courier 10 point font. By default,
       the fixed layout will be configured to display only realtime data.

    Args
    ----
    The name of the fixed layout. Cannot be None. (string)
    width - width of the fixed layout in characters(integer)
    height - height of the fixed layout in characters (integer)'''

    global _currentLayout

    if _currentLayout is not None:
        raise mpcsutil.err.InvalidStateError("Current layout exists. Call discardLayout() to get rid of it")

    _currentLayout = perspective.fixedlayout.FixedLayout(name,
                                                         perspective.coordinatetypes.CHARACTER,
                                                         width, height)

def setDefaultFont(font):
    '''Sets the default font for the entire fixed layout view. A number of pre-fabricated
    Font objects exist in this API module. Otherwise, font can be supplied as a
    "face,point-size,style,[REVERSE]" comma-separated string.

    Args
    ----
    font - the default font to set (Font or string)'''

    global _currentLayout

    _checkCurrentLayout()

    if font is not None:
        if isinstance (font, Font):
            _currentLayout.setFont(font)
        elif isinstance(font, six.string_types):
            _currentLayout.setFont(perspective.graphics.getFontFromString(font))
    else:
        raise mpcsutil.err.InvalidInitError("font argument cannot be None")

def setDefaultColors(foreColor=COLOR_BLACK, backColor=COLOR_WHITE):
    '''Sets the default colors for the entire fixed layout view. A number of pre-fabricated
    Color objects exist in this API module. Otherwise, color can be supplied as a
    "red,green,blue" comma-separated string. All objects in the layout which do not supply
    their own color overrides will inherit these colors.

    Args
    ----
    foreColor - the default foreground color to set (Color or string)
    backColor - the default background color to set (Color or string)'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None:
        if isinstance (foreColor, perspective.graphics.Color):
            _currentLayout.setForegroundColor(foreColor)
        elif isinstance(foreColor, six.string_types):
            _currentLayout.setForegroundColor(perspective.graphics.getColorFromString(foreColor))
    else:
        raise(mpcsutil.err.InvalidInitError("foreColor argument cannot be None"))

    if backColor is not None:
        if isinstance (backColor, perspective.graphics.Color):
            _currentLayout.setBackgroundColor(backColor)
        elif isinstance(backColor, six.string_types):
            _currentLayout.setBackgroundColor(perspective.graphics.getColorFromString(backColor))
    else:
        raise mpcsutil.err.InvalidInitError("backColor argument cannot be None")

def setDefaultFswDictionary(dictDir, dictVersion):
    '''Sets the FSW dictionary with which the current fixed layout is to be defined.

    Args
    ----
    dictDir - the root FSW dictionary directory path (specified according to MPCS conventions) (string)
    dictVersion - the FSW dictionary version (specified according to MPCS conventions) (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if dictDir is None:
        raise(mpcsutil.err.InvalidInitError("dictDir argument cannot be None"))

    if dictVersion is None:
        raise(mpcsutil.err.InvalidInitError("dictVersion argument cannot be None"))

    _currentLayout.setFswDictionaryDir(dictDir)
    _currentLayout.setFswVersion(dictVersion)

def setDefaultSseDictionary(dictDir, dictVersion):
    '''Sets the SSE dictionary with which the current fixed layout is to be defined.

    Args
    ----
    dictDir - the root SSE dictionary directory path (specified according to MPCS conventions) (string)
    dictVersion - the SSE dictionary version (specified according to MPCS conventions) (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if dictDir is None:
        raise mpcsutil.err.InvalidInitError("dictDir argument cannot be None")

    if dictVersion is None:
        raise mpcsutil.err.InvalidInitError("dictVersion argument cannot be None")

    _currentLayout.setSseDictionaryDir(dictDir)
    _currentLayout.setSseVersion(dictVersion)


def setShowRecordedData(recorded):
    '''Sets the current fixed layout to display recorded or realtime data only. The
    default is realtime.

    Args
    ----
    recorded - True to enable display of recorded data, False to enable display of realtime data.'''

    global _currentLayout

    _checkCurrentLayout()

    _currentLayout.setShowRecorded(recorded)

def setStalenessInterval(interval):
    '''Sets the interval, in seconds, before dynamic values displayed in the current fixed layout
    will be considered stale. The default value is 600 seconds, or 10 minutes.

    Args
    ----
    interval - staleness interval in seconds (integer)'''

    global _currentLayout

    _checkCurrentLayout()

    _currentLayout.setStalenessInterval(interval)

def addMission(mission):
    '''Adds a mission to the list if supported missions for the current fixed layout.
    The default mission (indicated by the current setting of the CHILL_GDS environment
    variable) is included by default, so it is only necessary to call this method if
    the layout will support additional missions.

    Args
    ----
    mission - mission to add (e.g."msl") (string)'''

    global _currentLayout

    _checkCurrentLayout()

    _currentLayout.addMission(mission)

def addText(text, xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format="%s", condition=None):
    '''Adds a plain text field to the current fixed layout.

    Args
    ----
    text - the text to display in the field (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - C printf style formatter for the field
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    if backColor is not None and isinstance(backColor, six.string_types):
        backColor = perspective.graphics.getColorFromString(backColor)

    if font is not None and isinstance(font, six.string_types):
        font = perspective.graphics.getFontFromString(font)

    textField = perspective.fixedfields.TextField(text,
                                                  format,
                                                  xLoc,
                                                  yLoc,
                                                  font,
                                                  foreColor,
                                                  backColor,
                                                  transparent,
                                                  condition)
    _currentLayout.addField(textField)

def addSclkTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='%s', condition=None):
    '''Adds a current SCLK time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.SCLK, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addErtTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='HH:mm:ss.SSSSSS', condition=None):
    '''Adds a current ERT time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.ERT, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addUtcTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format="DDD'/'HH:mm:ss", condition=None):
    '''Adds a current UTC time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.UTC, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addScetTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a current SCET time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.SCET, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addMstTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a current MST time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.MST, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addLstTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a current LST time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.LST, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addRctTime(xLoc, yLoc, font=None, foreColor=None, backColor=None, transparent=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a current RCT time field to the current fixed layout.

    Args
    ----
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addTimeField(perspective.timetypes.RCT, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition)

def addChannelId(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                 transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel ID field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.ID,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelTitle(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                    transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel title field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.TITLE,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelFswName(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                      transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel FSW name field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.FSW_NAME,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelModule(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                     transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel module field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.MODULE,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelOpsCat(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                     transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel operational category field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.OPS_CAT,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelSubsystem(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                        transparent=False, showAlarms=False, format='%s',condition=None):
    '''Adds a channel subsystem field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.SUBSYSTEM,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelSclk(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                        transparent=False, showAlarms=False, format='%s', condition=None):
    '''Adds a channel SCLK field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.SCLK,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelErt(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                        transparent=False, showAlarms=False, format='HH:mm:ss.SSSSSS', condition=None):
    '''Adds a channel ERT field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the C-printf formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.ERT,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelScet(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                   transparent=False, showAlarms=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a channel SCET field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.SCET,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelMst(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                  transparent=False, showAlarms=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a channel MST field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.MST,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelLst(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                   transparent=False, showAlarms=False,format='HH:mm:ss.SSS', condition=None):
    '''Adds a channel LST field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.LST,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelRct(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                  transparent=False, showAlarms=False, format='HH:mm:ss.SSS', condition=None):
    '''Adds a channel RCT field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.RCT,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelDn(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                 transparent=False, showAlarms=True, format=None, condition=None):
    '''Adds a channel DN field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.DN,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelDnUnit(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                     transparent=False, showAlarms= False, format='%s', condition=None):
    '''Adds a channel DN Unit field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.DN_UNIT,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelEu(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                 transparent=False, showAlarms=True, format=None, condition=None):
    '''Adds a channel EU field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.EU,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelEuUnit(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                     transparent=False, showAlarms=False, format='%s',condition=None):
    '''Adds a channel EU Unit field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.EU_UNIT,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelRaw(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                  transparent=False, showAlarms=True, format=None,condition=None):
    '''Adds a channel raw field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.RAW,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelValue(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                    transparent=False, showAlarms=True, format=None,condition=None):
    '''Adds a channel value field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.VALUE,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelStatus(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                     transparent=False, showAlarms=True, format='%s', condition=None):
    '''Adds a channel status field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
   condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.STATUS,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelAlarmState(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                         transparent=False, showAlarms=True, format='%s', condition=None):
    '''Adds a channel alarm state field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.ALARM_STATE,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

# 11/4/13 - MPCS-5501: Add DSS and Recorded fields
def addChannelStation(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                 transparent=False, showAlarms=False, format=None, condition=None):
    '''Adds a channel station ID field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.DSS_ID,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addChannelRecorded(channelId, xLoc, yLoc, font=None, foreColor=None, backColor=None,
                 transparent=False, showAlarms=False, format=None, condition=None):
    '''Adds a channel recorded field to the current fixed layout.

    Args
    ----
    channelId - the dictionary ID of the source channel (string)
    xLoc - the starting X coordinate of the field within the fixed layout (integer)
    yLoc - the starting Y coordinate of the field within the fixed layout (integer)
    font - an optional font override for this field (Font or font string)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    transparent - an optional flag to make the background of the text transparent (boolean)
    showAlarms - an optional flag to enable highlighting of this field for alarm status (boolean)
    format - the Java date/time formatter for the field (string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _addChannelField(channelId,
                     perspective.channelfieldtypes.RECORDED,
                     format,
                     xLoc,
                     yLoc,
                     font,
                     foreColor,
                     backColor,
                     transparent,
                     showAlarms,
                     condition)

def addBox(xStart, yStart, xEnd, yEnd, foreColor=None, backColor=None, thickness=1, style=SOLID, transparent=False, condition=None):
    '''Adds a non-titled box to the current fixed layout.

    Args
    ----
    xStart - the starting X coordinate of the box within the fixed layout (integer)
    yStart - the starting Y coordinate of the box within the fixed layout (integer)
    xEnd - the ending X coordinate of the box within the fixed layout (integer)
    yEnd - the ending Y coordinate of the box within the fixed layout (integer)
    foreColor - an optional foreground color override for this box (Color or RGB string)
    backColor - an optional background/fill color for this box (Color or RGB string)
    thickness - optional width of box border line, in pixels (integer)
    style - an optional style specifier for the box border (DASHED or SOLID)
    transparent - an optional flag to draw the box with transparent background
    condition - an optional condition string to evaluate before displaying this field (string)'''

    addTitledBox(xStart, yStart, xEnd, yEnd, None, None, foreColor, backColor, thickness, style, transparent, condition)

def addTitledBox(xStart, yStart, xEnd, yEnd, title, font=None, foreColor=None, backColor=None,
                 thickness=1, style=SOLID, transparent=False, condition=None):
    '''Adds a titled box to the current fixed layout.

    Args
    ----
    xStart - the starting X coordinate of the box within the fixed layout (integer)
    yStart - the starting Y coordinate of the box within the fixed layout (integer)
    xEnd - the ending X coordinate of the box within the fixed layout (integer)
    yEnd - the ending Y coordinate of the box within the fixed layout (integer)
    title - the title text for the box (string)
    font - an optional font override for the box title text (Font or string)
    foreColor - an optional foreground color override for this box (Color or RGB string)
    backColor - an optional background/fill color for this box (Color or RGB string)
    thickness - optional width of box border line, in pixels (integer)
    style - an optional style specifier for the box border (SOLID or DASHED)
    transparent - an optional flag to draw the box title text with transparent background
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    if backColor is not None and isinstance(backColor, six.string_types):
        backColor = perspective.graphics.getColorFromString(backColor)

    if font is not None and isinstance(font, six.string_types):
        font = perspective.graphics.getFontFromString(font)

    boxField = perspective.fixedfields.BoxField(title,
                                                xStart,
                                                yStart,
                                                xEnd,
                                                yEnd,
                                                font,
                                                foreColor,
                                                backColor,
                                                thickness,
                                                style,
                                                transparent,
                                                condition)
    _currentLayout.addField(boxField)

def addLine (xStart, yStart, xEnd, yEnd, foreColor=None, thickness=1, style=SOLID, condition=None):
    '''Adds a straight line to the current fixed layout.

    Args
    ----
    xStart - the starting X coordinate of the line within the fixed layout (integer)
    yStart - the starting Y coordinate of the line within the fixed layout (integer)
    xEnd - the ending X coordinate of the line within the fixed layout (integer)
    yEnd - the ending Y coordinate of the line within the fixed layout (integer)
    foreColor - an optional foreground color override for this line (Color or RGB string)
    thickness - optional width of line, in pixels (integer)
    style - an optional style specifier for the line (SOLID or DASHED)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    lineField = perspective.fixedfields.LineField(xStart,
                                                  yStart,
                                                  xEnd,
                                                  yEnd,
                                                  foreColor,
                                                  thickness,
                                                  style,
                                                  condition)

    _currentLayout.addField(lineField)

def addPageButton(title, pageName, xStart, yStart, foreColor=None, backColor=None, condition=None):
    '''Adds a button to launch another fixed layout view. The button will be auto-sized
    to accommodate its title.

    Args
    ----
    title - title text for the button (string)
    pageName - name or path to fixed layout view to launch (string)
    xStart - the starting X coordinate of the button within the fixed layout (integer)
    yStart - the starting Y coordinate of the button within the fixed layout (integer)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _checkCurrentLayout()

    _addButtonField(title,
                    perspective.actiontypes.LAUNCH_PAGE,
                    pageName,
                    xStart,
                    yStart,
                    None,
                    None,
                    foreColor,
                    backColor,
                    condition)

def addScaledPageButton(title, pageName, xStart, yStart, xEnd, yEnd, foreColor=None, backColor=None, condition=None):
    '''Adds a button to launch another fixed layout view. The button size will be scaled
    to match the start and end points.

    Args
    ----
    title - title text for the button (string)
    pageName - name or path to fixed layout view to launch (string)
    xStart - the starting X coordinate of the button within the fixed layout (integer)
    yStart - the starting Y coordinate of the button within the fixed layout (integer)
    xEnd - the ending X coordinate of the button within the fixed layout (integer)
    yEnd - the ending Y coordinate of the button within the fixed layout (integer)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _checkCurrentLayout()

    _addButtonField(title,
                    perspective.actiontypes.LAUNCH_PAGE,
                    pageName,
                    xStart,
                    yStart,
                    xEnd,
                    yEnd,
                    foreColor,
                    backColor,
                    condition)

def addScriptButton(title, command, xStart, yStart, foreColor=None, backColor=None, condition=None):
    '''Adds a button to run a script. The button will be auto-sized
    to accommodate its title.

    Args
    ----
    title - title text for the button (string)
    command - command string to execute when the button is pressed (string)
    xStart - the starting X coordinate of the button within the fixed layout (integer)
    yStart - the starting Y coordinate of the button within the fixed layout (integer)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _checkCurrentLayout()

    _addButtonField(title,
                    perspective.actiontypes.LAUNCH_SCRIPT,
                    command,
                    xStart,
                    yStart,
                    None,
                    None,
                    foreColor,
                    backColor,
                    condition)

def addScaledScriptButton(title, command, xStart, yStart, xEnd, yEnd, foreColor=None, backColor=None, condition=None):
    '''Adds a button to run a script. The button size will be scaled to match the start
    and end points.

    Args
    ----
    title - title text for the button (string)
    command - command string to execute when the button is pressed (string)
    xStart - the starting X coordinate of the button within the fixed layout (integer)
    yStart - the starting Y coordinate of the button within the fixed layout (integer)
    xEnd - the ending X coordinate of the button within the fixed layout (integer)
    yEnd - the ending Y coordinate of the button within the fixed layout (integer)
    foreColor - an optional foreground color override for this field (Color or RGB string)
    backColor - an optional background color override for this field (Color or RGB string)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    _checkCurrentLayout()

    _addButtonField(title,
                    perspective.actiontypes.LAUNCH_SCRIPT,
                    command,
                    xStart,
                    yStart,
                    xEnd,
                    yEnd,
                    foreColor,
                    backColor,
                    condition)

def addImage(path, xStart, yStart, condition=None):
    '''Adds an image (JPG, GIF, PNG) to the current fixed layout, using the image's natural size.

    Args
    ----
    path - the path to the image file on the file system (string)
    xStart - the starting X coordinate of the image within the fixed layout (integer)
    yStart - the starting Y coordinate of the image within the fixed layout (integer)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    imageField = perspective.fixedfields.ImageField(path, xStart, yStart, None, None, condition)

    _currentLayout.addField(imageField)

def addScaledImage(path, xStart, yStart, xEnd, yEnd, condition=None):
    '''Adds an image (JPG, GIF, PNG) to the current fixed layout and specify how to scale it.

    Args
    ----
    path - the path to the image file on the file system (string)
    xStart - the starting X coordinate of the image within the fixed layout (integer)
    yStart - the starting Y coordinate of the image within the fixed layout (integer)
    xEnd - the ending X coordinate of the image within the fixed layout (integer)
    yEnd - the ending Y coordinate of the image within the fixed layout (integer)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    imageField = perspective.fixedfields.ImageField(path, xStart, yStart, xEnd, yEnd, condition)

    _currentLayout.addField(imageField)

def addHeader(headerType, xStart, yStart, foreColor=None, backColor=None, transparent=False, condition=None):
    '''Adds a header of the given type to the layout.

    Args
    ----
    headerType - Name of the header definition file, without the ".xml" (e.g., "MediumTimesOnlyHeader") (string)
    xStart - the starting X coordinate of the header within the fixed layout (integer)
    yStart - the starting Y coordinate of the header within the fixed layout (integer)
    foreColor - an optional foreground color override for this header (Color or RGB string)
    backColor - an optional background color override for this header (Color or RGB string)
    transparent - an optional flag to make the background of the header transparent (boolean)
    condition - an optional condition string to evaluate before displaying this field (string)'''

    global _currentLayout

    _checkCurrentLayout()

    headerField = perspective.fixedfields.HeaderField(headerType, xStart, yStart, foreColor, backColor, transparent, condition)

    _currentLayout.addField(headerField)

def writeLayout(filePath):
    '''Write the current layout to the given file location.

    Args
    ----
    filePath - the location at which to write the output file (string)'''

    global _currentLayout

    _checkCurrentLayout()

    if filePath is None:
        raise mpcsutil.err.InvalidInitError("The filePath argument cannot be None.")

    file = open(filePath, 'w')
    str = repr(_currentLayout)
    file.write(str)
    file.close()

def discardLayout():
    '''Discards the current layout so another can be created.'''

    global _currentLayout

    _currentLayout = None

def getXmlForLayout():
    ''' Gets the XML representation of the current layout.

    Returns
    -------
    xmlText - the XML representation of the current layout'''


    global _currentLayout

    _checkCurrentLayout()

    return repr(_currentLayout)

def addComparisonCondition(channelId, conditionId, comparisonType, sourceFieldType, comparisonValue):
    ''' Adds a channel comparison condition to the fixed view.

    Args
    ----
    channelId - channel ID of channel with the value we will compare (string)
    conditionId - unique ID string for this condition (string)
    comparisonType - the type of comparison to perform (ComparisonTypes)
    sourceFieldType - the channel source field to compare (ComparisonFieldTypes)
    comparsionValue - the value to compare the channel value with (string)'''

    global _currentLayout

    _checkCurrentLayout()

    _addCondition(channelId, conditionId, comparisonType, sourceFieldType, comparisonValue)

def addAlarmCondition(channelId, conditionId, setFlag=True, color='RED'):
    ''' Adds an alarm check condition to the fixed view.

    Args
    ----
    channelId - Channel to check for this condition; may not be None (string)
    conditionId - Unique identifier for this condition; may not be None (string)
    setFlag - True if condition is true when alarm is SET;
              false if condition is True when alarm is NOT set; defaults to True (string)
    color - alarm condition to check: 'RED' or 'YELLOW'; may not be None; defaults to 'RED' (string)'''

    global _currentLayout

    _checkCurrentLayout()

    comparisonType = perspective.conditiontypes.SET

    if setFlag is False:
        comparisonType = perspective.conditiontypes.NOT_SET

    _addCondition(channelId, conditionId, comparisonType, None, color)

def addNullCondition(channelId, conditionId):
    ''' Adds a null check condition to the fixed view.

    Args
    ----
    channelId - Channel to check for this condition; may not be None (string)
    conditionId - Unique identifier for this condition; may not be None (string)'''

    global _currentLayout

    _checkCurrentLayout()

    _addCondition(channelId, conditionId, perspective.conditiontypes.IS_NULL, None, None)

def addStaleCondition(channelId, conditionId):
    ''' Adds a stale check condition to the fixed view.

    Args
    ----
    channelId - Channel to check for this condition; may not be None (string)
    conditionId - Unique identifier for this condition; may not be None (string)'''

    global _currentLayout

    _checkCurrentLayout()

    _addCondition(channelId, conditionId, perspective.conditiontypes.STALE, None, None)

def addTypeCondition(channelId, conditionId, channelType):
    ''' Adds a data type check condition to the fixed view.

    Args
    ----
    channelId - Channel to check for this condition; may not be None (string)
    conditionId - Unique identifier for this condition; may not be None (string)
    channelType - The data type to check for; may not be None (ChannelTypes)'''

    global _currentLayout

    _checkCurrentLayout()

    _addCondition(channelId, conditionId, perspective.conditiontypes.TYPE, None, channelType)

##############################################
#
# Internal Functions
#
##############################################
def _checkCurrentLayout():
    '''Private method to check if layout exists.'''

    global _currentLayout

    if _currentLayout is None:
        raise mpcsutil.err.InvalidStateError("No current layout exists. Create a new layout first.")

def _addTimeField(timeType, format, xLoc, yLoc, font, foreColor, backColor, transparent, condition):
    '''Private generic method to add a time field.'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    if backColor is not None and isinstance(backColor, six.string_types):
        backColor = perspective.graphics.getColorFromString(backColor)

    if font is not None and isinstance(font, six.string_types):
        font = perspective.graphics.getFontFromString(font)

    timeField = perspective.fixedfields.TimeField(timeType,
                                                  format,
                                                  xLoc,
                                                  yLoc,
                                                  font,
                                                  foreColor,
                                                  backColor,
                                                  transparent,
                                                  condition)
    _currentLayout.addField(timeField)

def _addChannelField(channelId, sourceType, format, xLoc, yLoc, font, foreColor, backColor, transparent, showAlarms, condition):
    '''Private generic method to add a channel field.'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    if backColor is not None and isinstance(backColor, six.string_types):
        backColor = perspective.graphics.getColorFromString(backColor)

    if font is not None and isinstance(font, six.string_types):
        font = perspective.graphics.getFontFromString(font)

    channelField = perspective.fixedfields.ChannelField(channelId,
                                                        sourceType,
                                                        format,
                                                        showAlarms,
                                                        xLoc,
                                                        yLoc,
                                                        font,
                                                        foreColor,
                                                        backColor,
                                                        transparent,
                                                        condition)
    _currentLayout.addField(channelField)

def _addButtonField(title, actionType, actionString, xStart, yStart, xEnd, yEnd, foreColor, backColor, condition):
    '''Private generic method to add a button field.'''

    global _currentLayout

    _checkCurrentLayout()

    if foreColor is not None and isinstance(foreColor, six.string_types):
        foreColor = perspective.graphics.getColorFromString(foreColor)

    if backColor is not None and isinstance(backColor, six.string_types):
        backColor = perspective.graphics.getColorFromString(backColor)

    buttonField = perspective.fixedfields.ButtonField(title,
                                                      actionType,
                                                      actionString,
                                                      xStart,
                                                      yStart,
                                                      xEnd,
                                                      yEnd,
                                                      foreColor,
                                                      backColor,
                                                      condition)

    _currentLayout.addField(buttonField)

def _addCondition(channelId, conditionId, conditionType, sourceFieldType, value):
    '''Private generic method to add a condition.'''

    global _currentLayout

    _checkCurrentLayout()

    condition = perspective.fixedfields.Condition(conditionId,
                                                  channelId,
                                                  sourceFieldType,
                                                  conditionType,
                                                  value);
    _currentLayout.addCondition(condition)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
