#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines classes used for defining the individual fields in Fixed Layout
Views in the MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import perspective.linestyletypes
import perspective.fixedfieldtypes
from perspective.graphics import (Color, Font)


class AbstractFixedField(object):
    '''An AbstractFixedField object represents the basic attributes of all fixed view
    fields. This class is extended to create specific field classes. The most
    important attribute of a a field is its type, which indicates the type of
    drawing object it maps to in the realtime view.

    Object Attributes
    ------------------
    fieldType - The type of the field (FixedFieldTypes)
    xStart - The starting x coordinate of the field (integer)
    yStart - the starting y coordinate of the field (integer)
    condition - the condition string to be evaluated before displaying this field (string)'''

    def __init__(self,
                 fieldType,
                 xStart=0,
                 yStart=0,
                 condition=None):

        '''Initializes the AbstractField object.

        Args
        -----
        fieldType - The type of the Fixed Field. One of the perspective.FIELD_TYPE constants. Required (no default) (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        condition - the condition string to be evaluated before displaying the field (string)'''

        self.setFieldType(fieldType)
        self.setXStart(xStart)
        self.setYStart(yStart)
        self.setCondition(condition)

    def getFieldType(self):
        '''Gets the actual field type of this AbstractField.

        Returns
        -------
        type - the type of this fixed field (FixedFieldTypes)'''

        return self.fieldType

    def getXStart(self):
        '''Gets the starting X coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Returns
        -------
        xStart - starting X coordinate of this field (integer)'''

        return self.xStart

    def getYStart(self):
        '''Gets the starting Y coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Returns
        -------
        yStart - starting Y coordinate of this field (integer)'''

        return self.yStart

    def setFieldType(self, type):
        '''Sets the field type of this AbstractField.

        Args
        ----
        type - the fixed field type (FixedFieldTypes); may not be None'''

        if type is None or not str(type) in perspective.fixedfieldtypes.FixedFieldTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The type argument cannot be None and must be one of the valid FixedFieldTypes.')

        self.fieldType = str(type)

    def setXStart(self, pos):
        '''Sets the starting X coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Args
        -------
        pos - starting X coordinate of this field (integer)'''

        if pos is None or int(pos) < 0:
            raise mpcsutil.err.InvalidInitError('The xStart argument cannot be None or less than 0.')

        self.xStart = pos

    def setYStart(self, pos):
        '''Sets the starting Y coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Args
        -------
        pos - starting Y coordinate of this field (integer)'''

        if pos is None or int(pos) < 0:
            raise mpcsutil.err.InvalidInitError('The yStart argument cannot be None or less than 0.')

        self.yStart = pos

    def setCondition(self, condition):
        '''Sets the condition string to be evaluated for this field.

        Args
        ----
        condition - the condition string for this field (string)'''

        if condition is None:
           self.condition = None
        else:
           self.condition = str(condition)

    def getCondition(self):
        '''Gets the condition string to be evaluated for this field.

        Returns
        -------
        condition - the condition string; may be None (string)'''

        return self.condition

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = "Fixed Field (Type=%s)\n\txStart=%d\n\tyStart=%d\n\tconditions=%s\n" % \
        (self.fieldType, self.xStart, self.yStart, self.condition)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        if self.condition is None:
            returnStr = 'xStart="%d" yStart="%d" ' % (self.xStart, self.yStart)
        else:
            returnStr = 'xStart="%d" yStart="%d" conditions="%s" ' % (self.xStart, self.yStart, self.condition)

        return returnStr

class AbstractTextField(AbstractFixedField):
    '''AbstractTextField is a fixed field subclass that contains attributes
    common to all types of text fields. This class is extended to create
    specific types of text fields.

    Object Attributes
    -----------------
    font - the font used to render the text field; if None, the font is inherited
           from the parent fixed layout (Font)
    foreColor - the foreground color used to render the text field; if None, the
                color is inherited from the parent fixed layout (Color)
    backColor - the background color used to render the text field; if None, the
                color is inherited from the parent fixed layout (Color)
    isTransparent - flag that indicates whether the field text should be drawn
                    with a transparent background (boolean)
    format - the C-printf or Java date/time formatter for the field (string)'''

    def __init__(self,
                 fieldType,
                 format=None,
                 xStart=0,
                 yStart=0,
                 font=None,
                 foreColor=None,
                 backColor=None,
                 transparent=False,
                 condition=None):

        '''Initializes an AbstractTextField object

        Args
        ----
        fieldType - The type of the field (FixedFieldTypes)
        format - the C-printf or Java date/time formatter for the field (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        font - the font used to render the text field; if None, the font is inherited
               from the parent fixed layout (string)
        foreColor - the foreground color used to render the text field; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the text field; if None, the
                    color is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractFixedField.__init__(self, fieldType, xStart, yStart, condition)

        self.setFont(font);
        self.setForegroundColor(foreColor)
        self.setBackgroundColor(backColor)
        self.setTransparent(transparent)
        self.setFormat(format)

    def setFormat(self, format):
        ''' Sets the format of the text field, as a C-printf or Java Date/Time format string.

        Args
        ----
        format - the C-printf or Java date/time formatter for the field (string)'''

        if format is None:
            self.format = None
        else:
            self.format = str(format)

    def setFont(self, font):
        ''' Sets the font of the text field. If None, the font defined
        in the parent fixed layout will used.

        Returns
        --------
        font - the font for the field (Font); may be None'''
        if not font is None and not isinstance(font, Font):
            raise mpcsutil.err.InvalidInitError('The font argument must be of type Font.')

        self.font = font

    def setForegroundColor(self, color):
        ''' Sets the foreground color of the text field. If None, the foreground
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the foreground color for the field (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The foreground argument must be of type Color.')
        self.foreColor = color

    def setBackgroundColor(self, color):
        ''' Sets the background color of the text field. If None, the color
        defined in the parent fixed layout will be used.
        Colors are represented as strings of the form "r,g,b".

        Args
        ----
        color - the background color for the field (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The background argument must be of type Color.')

        self.backColor = color

    def setTransparent(self, transparent=True):
        '''Sets the text background to be transparent, allowing fields beneath
        this text field to show through.

        Args
        ----
        transparent - True to set transparent background (default), False to disable (boolean)'''

        if transparent is None or transparent not in [True,False]:
            raise mpcsutil.err.InvalidInitError('The transparency argument cannot be None and must be boolean.')

        self.isTransparent = bool(transparent)

    def getFormat(self):
        ''' Gets the format of the text field.

        Returns
        -------
        format - the C-printf or Java date/time formatter for the field (string)'''

        return self.format

    def getFont(self):
        ''' Gets the font used to render this text field. If None, the font defined
        in the parent fixed layout will be used.

        Returns
        --------
        font - the font for the field (Font); may be None'''

        return self.font

    def getForegroundColor(self):
        '''Gets the foreground color to render this text field. If None,
        then the foreground color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the foreground color for the field (Color); may be None'''

        return self.foreColor

    def getBackgroundColor(self):
        '''Gets the background color of the FixedLayout. If None, then the
        background color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the background color for the field (Color); may be None'''

        return self.backColor

    def getTransparent(self):
        '''Gets the transparent flag for this field, indicating whether it will
        be drawn with a transparent background.

        Returns
        -------
        isTransparent - True if transparent flag is set; False if not (boolean)'''

        return self.isTransparent

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractFixedField.__str__(self)
        returnStr += "\tfont=%s\n\tforeColor=%s\n\tbackColor=%s\n\tisTransparent=%s" % \
             (self.font,str(self.foreColor),str(self.backColor),str(self.isTransparent))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = AbstractFixedField.__repr__(self)
        if self.font is not None:
            returnStr += 'font="%s" ' % (repr(self.font))

        if self.foreColor is not None:
            returnStr += 'foreground="%s" ' % repr(self.foreColor)

        if self.backColor is not None:
            returnStr += 'background="%s" ' % repr(self.backColor)

        if self.format is not None:
            returnStr += 'format="%s" ' % self.format

        returnStr += 'transparent="%s" ' %  str(self.isTransparent).lower()

        return returnStr

class TextField(AbstractTextField):
    '''TextField is a fixed field subclass that represents a simple text field.

    Object Attributes
    -----------------
    text - the text to display for this field (string)'''

    def __init__(self,
                 text='',
                 format='%s',
                 xStart=0,
                 yStart=0,
                 font=None,
                 foreColor=None,
                 backColor=None,
                 transparent=False,
                 condition=None):

        '''Initializes a TextField.

        Args
        ----
        text - the text to display for this field (string)
        format - the C-printf formatter for the field (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        font - the font used to render the text field; if None, the font is inherited
               from the parent fixed layout (Font)
        foreColor - the foreground color used to render the text field; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the text field; if None, the
                    color is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractTextField.__init__(self,
                                   perspective.fixedfieldtypes.TEXT,
                                   format,
                                   xStart,
                                   yStart,
                                   font,
                                   foreColor,
                                   backColor,
                                   transparent,
                                   condition)

        self.setText(text)

    def setText(self, text):
        '''Sets the string for the text field to display.

        Args
        ----
        text - the text for the field (string)'''

        if text is None:
            raise mpcsutil.err.InvalidInitError('The text argument cannot be None.')

        if not stringOkForXml(text):
            raise mpcsutil.err.InvalidInitError('The text argument cannot contain &, ", <, or >')

        self.text = str(text)

    def getText(self):
        '''Gets the text string displayed by the text field.

        Returns
        -------
        text - the text string for this field (string)'''

        return self.text

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTextField.__str__(self)
        returnStr += "\n\ttext=%s" % (self.text)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Text ';
        returnStr += AbstractTextField.__repr__(self);
        returnStr += 'text="%s" ' % (self.text)
        returnStr += '/>\n';
        return returnStr

class TimeField(AbstractTextField):
    '''TimeField is a fixed field subclass that represents any current time. The type
    of time (UTC, SCLK, ERT, etc) is customizable.

    Object Attributes
    -----------------
    timeType - the type of time to display for this field (TimeTypes)'''

    def __init__(self,
                 timeType,
                 format='HH:mm:ss.SSS',
                 xStart=0,
                 yStart=0,
                 font=None,
                 foreColor=None,
                 backColor=None,
                 transparent=False,
                 condition=None):

        '''Initializes a TimeField.

        Args
        ----
        timeType - the type of time to display for this field (TimeTypes)
        format - the C-printf or Java date/time formatter for the field (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        font - the font used to render the time field; if None, the font is inherited
              from the parent fixed layout (Font)
        foreColor - the foreground color used to render the time field; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the time field; if None, the
                    color is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractTextField.__init__(self,
                                   perspective.fixedfieldtypes.TIME,
                                   format,
                                   xStart,
                                   yStart,
                                   font,
                                   foreColor,
                                   backColor,
                                   transparent,
                                   condition)

        self.setTimeType(timeType)

    def setTimeType(self, timeType):
        '''Sets the time type for the time field to display.

        Args
        ----
        timeType - the time type for the field (TimeTypes)'''

        if timeType is None or str(timeType) not in perspective.timetypes.TimeTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The timeType argument cannot be None and must be one of the valid TimeTypes.')

        self.timeType = timeType

    def getTimeType(self):
        '''Gets the type of time displayed by the time field.

        Returns
        -------
        type - the time type for this field (TimeTypes)'''

        return self.timeType

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTextField.__str__(self)
        returnStr += "\n\tsourceTime=%s" % str(self.timeType)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<LatestTime ';
        returnStr += AbstractTextField.__repr__(self);
        returnStr += 'sourceTime="%s" ' % str(self.timeType)
        returnStr += '/>\n';
        return returnStr

class ChannelField(AbstractTextField):
    '''ChannelField is a fixed field subclass that represents any source field of a channelized
    telemetry value or its dictionary definition.

    Object Attributes
    -----------------
    channelId - the dictionary ID of the channel which is the source for this channel field (string)
    sourceType - the type of channel source field to display for this field (ChannelFieldTypes)
    isAlarmHighlight - indicates whether the field should be highlighted when the channel goes
                       into alarm (boolean)'''

    def __init__(self,
                 channelId,
                 sourceType,
                 format=None,
                 highlight=None,
                 xStart=0,
                 yStart=0,
                 font=None,
                 foreColor=None,
                 backColor=None,
                 transparent=False,
                 condition=None):

        '''Initializes a ChannelField.

        Args
        ----
        channelId - the dictionary ID of the channel which is the source for this channel field (string)
        sourceType - the type of channel source field to display for this field (ChannelFieldTypes)
        format - the C-printf or Java date/time formatter for the field (string)
        isAlarmHighlight - indicates whether the field should be highlighted when the channel goes
                           into alarm.  If None, the value will be defaulted based upon the
                           sourceType  (boolean)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        font - the font used to render the time field; if None, the font is inherited
               from the parent fixed layout (Font)
        foreColor - the foreground color used to render the time field; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the time field; if None, the
                    color is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractTextField.__init__(self,
                                   perspective.fixedfieldtypes.CHANNEL,
                                   format,
                                   xStart,
                                   yStart,
                                   font,
                                   foreColor,
                                   backColor,
                                   transparent,
                                   condition)

        self.setChannelId(channelId)
        self.setSourceType(sourceType)
        if highlight is None:
            self.isAlarmHighlight = sourceType.getDefaultHighlight()
        else:
            self.setAlarmHighlight(highlight)

    def setSourceType(self, sourceType):
        '''Sets the source-field type for the channel field to display.

        Args
        ----
        sourceType - the channel source field type for the channel field (ChannelFieldTypes)'''

        if sourceType is None or str(sourceType) not in perspective.channelfieldtypes.ChannelFieldTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The sourceType argument cannot be None and must be one of the valid ChannelFieldTypes.')

        self.sourceType = sourceType

    def setChannelId(self,channelId):
        '''Sets the dictionary ID of the channel used to populate this channel field.

        Args
        ----
        channelId - the dictionary channel ID (string)'''

        if channelId is None:
            raise mpcsutil.err.InvalidInitError('The channel ID argument cannot be None.')

        self.channelId = str(channelId)

    def setAlarmHighlight(self,highlight):
        '''Enables or disables alarm highlighting of this channel field.

        Args
        ----
        highlight - True to enable alarm highlighting, False to disable (boolean)'''

        if highlight is None or highlight not in [True, False]:
            raise mpcsutil.err.InvalidInitError('The highlight argument cannot be None and must be a boolean value.')

        self.isAlarmHighlight = bool(highlight)

    def getSourceType(self):
        '''Gets the source field type of the field displayed by the channel field.

        Returns
        -------
        type - the source type for this field (ChannelFieldTypes)'''

        return self.sourceType

    def getChannelId(self):
        '''Gets the dictionary ID of the channel used to populate this channel field.

        Returns
        -------
        channelId - the dictionary channel ID (string)'''

        return self.channelId

    def isAlarmHighlight(self):
        '''Gets the alarm highlighting flag for this channel field.

        Returns
        -------
        highlight - True if alarm highlighting is enabled, False if disabled (boolean)'''

        return self.isAlarmHighlight

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTextField.__str__(self)
        returnStr += "\n\tchannelId=%s\n\tsourceField=%s\n\talarmHighlight=%s" % \
            (self.channelId,str(self.sourceType),str(self.isAlarmHighlight))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Channel '
        returnStr += AbstractTextField.__repr__(self);
        returnStr += 'channelId="%s" sourceField="%s" alarmHighlight="%s"' % \
            (self.channelId, str(self.sourceType), str(self.isAlarmHighlight).lower())
        returnStr += '/>\n'

        return returnStr

class AbstractTwoPointFixedField(AbstractFixedField):
    '''AbstractTwoPointFixedField is a fixed field subclass that can be extended to
    represent fields that are defined by two points on the canvas.

    Object Attributes
    -----------------
    xEnd - The ending x coordinate of the field (integer)
    yEnd - the ending y coordinate of the field (integer)'''

    def __init__(self,
                 fieldType,
                 xStart=0,
                 yStart=0,
                 xEnd=None,
                 yEnd=None,
                 condition=None):

        '''Initializes an AbstractTwoPointFixedField.

        Args
        ----
        fieldType - The type of the Fixed Field. One of the perspective.FIELD_TYPE constants. Required (no default) (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        xEnd - The ending x coordinate of the field (integer)
        yEnd - the ending y coordinate of the field (integer)
        condition - the condition string for this field (string)'''

        AbstractFixedField.__init__(self,
                                   fieldType,
                                   xStart,
                                   yStart,
                                   condition)

        self.setXEnd(xEnd)
        self.setYEnd(yEnd)

    def setXEnd(self, pos):
        '''Sets the ending X coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Args
        -------
        pos - ending X coordinate of this field (integer)'''

        if pos is not None and int(pos) < 0:
            raise mpcsutil.err.InvalidInitError('The xEnd argument cannot be less than 0.')

        if pos is None:
            self.xEnd = None
        else:
            self.xEnd = int(pos)

    def setYEnd(self, pos):
        '''Sets the ending Y coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Args
        -------
        pos - ending Y coordinate of this field (integer)'''

        if pos is not None and int(pos) < 0:
            raise mpcsutil.err.InvalidInitError('The yEnd argument cannot be less than 0.')

        if pos is None:
            self.yEnd = None
        else:
            self.yEnd = int(pos)

    def getXEnd(self):
        '''Gets the ending X coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Returns
        -------
        xEnd - ending X coordinate of this field (integer)'''

        return self.xEnd

    def getYEnd(self):
        '''Gets the ending Y coordinate for this field. If the location type
        for the Fixed Layout this field is part of in PIXEL, this value will
        be interpreted as pixels. Otherwise, it will be interpreted as number
        of characters.

        Returns
        -------
        yEnd - ending Y coordinate of this field (integer)'''

        return self.yStart

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractFixedField.__str__(self)
        returnStr += '\n\txEnd=%d\n\tyEnd=%d' % (self.xEnd, self.yEnd)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = AbstractFixedField.__repr__(self);
        if self.xEnd is None:
            returnStr += 'xEnd="-1" '
        else:
            returnStr += 'xEnd="%d" ' % (self.xEnd)

        if self.yEnd is None:
            returnStr += 'yEnd="-1" '
        else:
            returnStr += 'yEnd="%d" ' % (self.yEnd)

        return returnStr

class LineField(AbstractTwoPointFixedField):
    '''LineField is a fixed field subclass that represents a simple straight line.

    Object Attributes
    -----------------
    foreColor - the foreground color used to render the line field; if None, the
                color is inherited from the parent fixed layout (Color)
    thickness - the thickness, in pixels, of the drawn line (integer)
    style - the line style (LineStyles)'''

    def __init__(self,
                 xStart=0,
                 yStart=0,
                 xEnd=None,
                 yEnd=None,
                 foreColor=None,
                 thickness=1,
                 style=perspective.linestyletypes.SOLID,
                 condition=None):

        '''Initializes a LineField object

        Args
        ----
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        xEnd - The ending x coordinate of the field (integer)
        yEnd - the ending y coordinate of the field (integer)
        foreColor - the foreground color used to render the line; if None, the
                    color is inherited from the parent fixed layout (Color)
        thickness - the thickness, in pixels, of the drawn line (integer)
        style - the line style (LineStyles)
        condition - the condition string for this field (string)'''

        AbstractTwoPointFixedField.__init__(self,
                                            perspective.fixedfieldtypes.LINE,
                                            xStart,
                                            yStart,
                                            xEnd,
                                            yEnd,
                                            condition)

        if xEnd is None or yEnd is None:
            raise mpcsutil.err.InvalidInitError("Neither xEnd nor yEnd can be None")

        self.setForegroundColor(foreColor)
        self.setThickness(thickness)
        self.setStyle(style)

    def setForegroundColor(self, color):
        ''' Sets the foreground color of the text field. If None, the foreground
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the foreground color for the field (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The foreground argument must be of type Color.')
        self.foreColor = color

    def setThickness(self,thickness):
        '''Sets the line thickness.

        Args
        ----
        thickness - the thickness of the line, in pixels (integer)'''

        if thickness is None or int(thickness) < 1 or int(thickness) > 20:
            raise mpcsutil.err.InvalidInitError('The thickness argument cannot be None and must in the range 1-20.')

        self.thickness = int(thickness)

    def setStyle(self,style):
        '''Sets the line style.

        Args
        ----
        style - the style of the line (LineStyleTypes)'''

        if style is None or not str(style) in perspective.linestyletypes.LineStyleTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The style argument cannot be None and must be a valid LineStyleTypes value')

        self.style = style

    def getForegroundColor(self):
        '''Gets the foreground color to render this text field. If None,
        then the foreground color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the foreground color for the field (Color); may be None'''

        return self.foreColor

    def getThickness(self):
        '''Gets the line thickness.

        Returns
        -------
        thickness - the thickness of the line, in pixels (integer)'''

        return self.thickness

    def getStyle(self):
        '''Gets the line style.

        Returns
        -------
        style - the style of the line (LineStyleTypes)'''

        return self.style

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTwoPointFixedField.__str__(self)
        returnStr += "\tforeColor=%s\n\tthickness=%d\n\tstyle=%s" % \
             (str(self.foreColor),self.thickness,str(self.style))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Line ';

        returnStr += AbstractTwoPointFixedField.__repr__(self)

        if self.foreColor is not None:
            returnStr += 'foreground="%s" ' % repr(self.foreColor)

        returnStr += 'lineThickness="%d" lineStyle="%s" ' %  (self.thickness, str(self.style))

        returnStr += '/>\n'

        return returnStr

class BoxField(AbstractTwoPointFixedField):
    '''BoxField is a fixed field subclass that represents a drawn, hollow or filled rectangle.
    If specified, a title will be drawn on the box border near the upper left corner of the box.

    Object Attributes
    -----------------
    title - the title text for the box, which may be None (string)
    foreColor - the foreground color used to render the box field border; if None, the
                color is inherited from the parent fixed layout (Color)
    backColor - the color used as background/fill for the box; if None, the box will not be filled (Color)
    font - the font used to render the box title, if any; if None, the font is inherited
           from the parent fixed layout (Font)
    thickness - the thickness, in pixels, of the drawn box border (integer)
    style - the line style for the box border (LineStyles)
    isTransparent - flag that indicates whether the box title should be drawn
                    with a transparent background (boolean)'''

    def __init__(self,
                 title=None,
                 xStart=0,
                 yStart=0,
                 xEnd=None,
                 yEnd=None,
                 font=None,
                 foreColor=None,
                 backColor=None,
                 thickness=1,
                 style=perspective.linestyletypes.SOLID,
                 transparent=False,
                 condition=None):

        '''Initializes a BoxField object

        Args
        ----
        title - the title text for the box, which may be None (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        xEnd - The ending x coordinate of the field (integer)
        yEnd - the ending y coordinate of the field (integer)
        font - the font used to render the box title, if any; if None, the font is inherited
               from the parent fixed layout (Font)
        foreColor - the foreground color used to render the box border; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the color used as background/fill for the box; if None, the box will not be filled (Color)
        thickness - the thickness, in pixels, of the drawn border (integer)
        style - the line style for the box border (LineStyles)
        transparent - flag that indicates whether the box title should be drawn
                      with a transparent background (boolean)
        condition - the condition string for this field (string)'''

        AbstractTwoPointFixedField.__init__(self,
                                            perspective.fixedfieldtypes.BOX,
                                            xStart,
                                            yStart,
                                            xEnd,
                                            yEnd,
                                            condition)

        if xEnd is None or yEnd is None:
            raise mpcsutil.err.InvalidInitError("Neither xEnd nor yEnd can be None")

        self.setTitle(title)
        self.setFont(font)
        self.setForegroundColor(foreColor)
        self.setBackgroundColor(backColor)
        self.setThickness(thickness)
        self.setStyle(style)
        self.setTransparent(transparent)

    def setTitle(self, title):
        '''Sets the optional title for the box.

        Args
        ----
        title - the title text; may be None (string)'''

        if title is None:
            self.title=None
        else:
            if not stringOkForXml(title):
                raise mpcsutil.err.InvalidInitError('The title argument cannot contain &, ", <, or >')

            self.title = str(title)

    def setForegroundColor(self, color):
        '''Sets the foreground color of the box border and title. If None, the foreground
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the foreground color for the box (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The color argument must be of type Color.')
        self.foreColor = color

    def setBackgroundColor(self, color):
        '''Sets the background/fill color of the box. If None, the box will not be filled.

        Args
        ----
        color - the background color for the box (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The color argument must be of type Color.')
        self.backColor = color

    def setFont(self, font):
        ''' Sets the font of the box title. If None, the font defined
        in the parent fixed layout will used.

        Returns
        --------
        font - the font for the box (Font); may be None'''
        if not font is None and not isinstance(font, Font):
            raise mpcsutil.err.InvalidInitError('The font argument must be of type Font.')

        self.font = font

    def setThickness(self,thickness):
        '''Sets the line thickness.

        Args
        ----
        thickness - the thickness of the line, in pixels (integer)'''

        if thickness is None or int(thickness) < 1 or int(thickness) > 20:
            raise mpcsutil.err.InvalidInitError('The thickness argument cannot be None and must in the range 1-20.')

        self.thickness = int(thickness)

    def setStyle(self,style):
        '''Sets the line style.

        Args
        ----
        style - the style of the line (LineStyleTypes)'''

        if style is None or not str(style) in perspective.linestyletypes.LineStyleTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The style argument cannot be None and must be a valid LineStyleTypes value')

        self.style = style

    def setTransparent(self, transparent=True):
        '''Sets the box title background to be transparent, allowing fields beneath
        this text field to show through.

        Args
        ----
        transparent - True to set transparent background (default), False to disable (boolean)'''

        if transparent is None or transparent not in [True,False]:
            raise mpcsutil.err.InvalidInitError('The transparency argument cannot be None and must be boolean.')

        self.isTransparent = bool(transparent)

    def getTitle(self):
        '''Gets the title for the box.

        Returns
        -------
        title - the box title text; may be None (string)'''

        return self.title

    def getForegroundColor(self):
        '''Gets the foreground color to render the box border and title. If None,
        then the foreground color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the foreground color for the field (Color); may be None'''

        return self.foreColor

    def getBackgroundColor(self):
        '''Gets the background/fill color for the box. If None, the box will not be filled.

        Returns
        --------
        color - the background color for the box (Color); may be None'''

        return self.backColor

    def getFont(self):
        ''' Gets the font used to render the box title. If None, the font defined
        in the parent fixed layout will be used.

        Returns
        --------
        font - the font for the box (Font); may be None'''

        return self.font

    def getThickness(self):
        '''Gets the line thickness.

        Returns
        -------
        thickness - the thickness of the line, in pixels (integer)'''

        return self.thickness

    def getStyle(self):
        '''Gets the line style.

        Returns
        -------
        style - the style of the line (LineStyleTypes)'''

        return self.style

    def getTransparent(self):
        '''Gets the transparent flag for this box, indicating whether the box title will
        be drawn with a transparent background.

        Returns
        -------
        isTransparent - True if transparent flag is set; False if not (boolean)'''

        return self.isTransparent

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTwoPointFixedField.__str__(self)
        returnStr += "\ttitle=%s\n\tfont=%s\n\tforeColor=%s\n\tfillColor=%s\n\tthickness=%d\n\tstyle=%s\n\ttransparent=%s" % \
             (self.title, str(self.font), str(self.foreColor), str(self.backColor), self.thickness, str(self.style), str(self.isTransparent))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Box ';

        returnStr += AbstractTwoPointFixedField.__repr__(self)

        if self.title is not None:
            returnStr += 'title="%s" ' % self.title

        if self.font is not None:
            returnStr += 'font="%s" ' % repr(self.font)

        if self.foreColor is not None:
            returnStr += 'foreground="%s" ' % repr(self.foreColor)

        if self.backColor is not None:
            returnStr += 'background="%s" ' % repr(self.backColor)

        returnStr += 'lineThickness="%d" lineStyle="%s" transparent="%s" ' %  \
            (self.thickness, str(self.style), str(self.isTransparent).lower())

        returnStr += '/>\n'

        return returnStr

class ButtonField(AbstractTwoPointFixedField):
    '''ButtonField is a fixed field subclass that represents a push button. An action to
    take when the button is pressed can be specified. If the second coordinate is omitted,
    the button will be automatically sized.

    Object Attributes
    -----------------
    title - the title text for the button (string)
    foreColor - the foreground color used to render the button title; if None, the
                color is inherited from the parent fixed layout (Color)
    backColor - the background color used to render the button; if None, the color
                is inherited from the parent fixed layout (Color)
    actionType - the type of action to take when the button is pressed (ActionTypes)
    actionString - the action string, which defines the exact action taken when the
                   button is pressed (string)'''

    def __init__(self,
                 title,
                 actionType,
                 actionString,
                 xStart=0,
                 yStart=0,
                 xEnd=None,
                 yEnd=None,
                 foreColor=None,
                 backColor=None,
                 condition=None):

        '''Initializes a ButtonField object

        Args
        ----
        title - the title text for the button (string)
        actionType - the type of action to take when the button is pressed (ActionTypes)
        actionString - the action string, which defines the exact action taken when the
                       button is pressed (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        xEnd - The ending x coordinate of the field; if omitted, the button will be
               automatically sized (integer)
        yEnd - the ending y coordinate of the field; if omitted, the button will be
               automatically sized (integer)
        foreColor - the foreground color used to render the button; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the button; if None, the color
                    is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractTwoPointFixedField.__init__(self,
                                            perspective.fixedfieldtypes.BUTTON,
                                            xStart,
                                            yStart,
                                            xEnd,
                                            yEnd,
                                            condition)

        self.setTitle(title)
        self.setForegroundColor(foreColor)
        self.setBackgroundColor(backColor)
        self.setActionType(actionType)
        self.setActionString(actionString)

    def setTitle(self, title):
        '''Sets the title for the button.

        Args
        ----
        title - the title text (string)'''

        if title is None:
            raise mpcsutil.err.InvalidInitError('The title argument cannot be None.')

        if not stringOkForXml(title):
            raise mpcsutil.err.InvalidInitError('The title argument cannot contain &, ", <, or >')

        self.title = str(title)

    def setActionType(self,actionType):
        '''Sets the type of action to be taken when this button is pressed.

        Args
        ----
        actionType - the type of action to take when the button is pressed (ActionTypes)'''

        if actionType is None:
            raise mpcsutil.err.InvalidInitError('The actionType argument cannot be None.')

        if not str(actionType) in perspective.actiontypes.ActionTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The actionType argument must be one of the valid ActionTypes.')

        self.actionType = actionType

    def setActionString(self,actionString):
        '''Sets the detailed action string, which defines exactly what happens when the button
        is pressed. If the action type is LAUNCH_PAGE, the action string should be the name or
        location of the fixed layout view to load. If the action type is LAUNCH_SCRIPT, the
        action string should be the complete command string to execute, etc.

        Args
        ----
        actionString - the action string, which defines the exact action taken when the
                       button is pressed (string)'''

        if actionString is None:
            raise mpcsutil.err.InvalidInitError('The actionString argument cannot be None.')

        self.actionString = actionString

    def setForegroundColor(self, color):
        '''Sets the foreground color of the button title. If None, the foreground
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the foreground color for the button (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The color argument must be of type Color.')

        self.foreColor = color

    def setBackgroundColor(self, color):
        '''Sets the background color of the button. If None, the background
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the background color for the button (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The color argument must be of type Color.')

        self.backColor = color

    def getTitle(self):
        '''Gets the title for the button.

        Returns
        -------
        title - the button title text (string)'''

        return self.title

    def getActionType(self):
        '''Gets the type of action to be taken when this button is pressed.

        Returns
        -------
        actionType - the type of action to take when the button is pressed (ActionTypes)'''

        return self.actionType

    def getActionString(self):
        '''Gets the detailed action string, which defines exactly what happens when the button
        is pressed.

        Returns
        -------
        actionString - the action string, which defines the exact action taken when the
                       button is pressed (string)'''

        return self.actionString

    def getForegroundColor(self):
        '''Gets the foreground color to render the button title. If None,
        then the foreground color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the foreground color for the button (Color); may be None'''

        return self.foreColor

    def getBackgroundColor(self):
        '''Gets the background color for the button. If None,
        then the background color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the background color for the button (Color); may be None'''

        return self.backColor

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTwoPointFixedField.__str__(self)
        returnStr += "\ttitle=%s\n\tactionType=%s\n\tactionString=%s\n\tforeColor=%s\n\tbackColor=%s\n\tthickness=%d\n\tstyle=%s\n\ttransparent=%s" % \
             (self.title, str(self.actionType), self.actionString, str(self.foreColor), str(self.backColor))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Button ';

        returnStr += AbstractTwoPointFixedField.__repr__(self)

        if self.title is not None:
            returnStr += 'title="%s" ' % self.title

        returnStr += 'actionType="%s" actionString="%s" ' %  (str(self.actionType), self.actionString)

        if self.foreColor is not None:
            returnStr += 'foreground="%s" ' % repr(self.foreColor)

        if self.backColor is not None:
            returnStr += 'background="%s" ' % repr(self.backColor)

        returnStr += '/>\n'

        return returnStr

class ImageField(AbstractTwoPointFixedField):
    '''ImageField is a fixed field subclass that represents a graphical image loaded from
    an image file. If the second coordinate is omitted, the image will be displayed at its
    default size. If the second coordinate is provided, the image will be scaled to match it.

    Object Attributes
    -----------------
    path - the path to the image file (JPG, GIF, PNG) on the filesystem (string)'''

    def __init__(self,
                 path,
                 xStart=0,
                 yStart=0,
                 xEnd=None,
                 yEnd=None,
                 condition=None):

        '''Initializes an ImageField object

        Args
        ----
        path - the path to the image file (JPG, GIF, PNG) on the filesystem (string)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        xEnd - The ending x coordinate of the field; if omitted, the image will be
               automatically sized (integer)
        yEnd - the ending y coordinate of the field; if omitted, the image will be
               automatically sized (integer)
        condition - the condition string for this field (string)'''

        AbstractTwoPointFixedField.__init__(self,
                                            perspective.fixedfieldtypes.IMAGE,
                                            xStart,
                                            yStart,
                                            xEnd,
                                            yEnd,
                                            condition)

        self.setPath(path)

    def setPath(self, path):
        '''Sets the path to the image file.

        Args
        ----
        path - the file path (string)'''

        if path is None:
            raise mpcsutil.err.InvalidInitError('The path argument cannot be None.')

        self.path = str(path)

    def getPath(self):
        '''Gets the path to the image file.

        Returns
        -------
        path - the file path (string)'''

        return self.path

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractTwoPointFixedField.__str__(self)
        returnStr += "\tpath=%s\n" % (self.path)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Image ';
        returnStr += AbstractTwoPointFixedField.__repr__(self)
        returnStr += 'path="%s" ' % self.path
        returnStr += '/>\n'

        return returnStr

class HeaderField(AbstractFixedField):
    '''HeaderField is a fixed field that represents a block of header information
    that is manipulate as one object.

    Object Attributes
    -----------------
    headerType - the type name for the header block (String)
    foreColor - the foreground color used to render the text field; if None, the
                color is inherited from the parent fixed layout (Color)
    backColor - the background color used to render the text field; if None, the
                color is inherited from the parent fixed layout (Color)
    isTransparent - flag that indicates whether the field text should be drawn
                    with a transparent background (boolean)'''
    def __init__(self,
                 headerType,
                 xStart=0,
                 yStart=0,
                 foreColor=None,
                 backColor=None,
                 transparent=False,
                 condition=None):

        '''Initializes a HeaderField object

        Args
        ----
        headerType - the type name for the header block (String)
        xStart - The starting x coordinate of the field (integer)
        yStart - the starting y coordinate of the field (integer)
        foreColor - the foreground color used to render the header field; if None, the
                    color is inherited from the parent fixed layout (Color)
        backColor - the background color used to render the header field; if None, the
                    color is inherited from the parent fixed layout (Color)
        condition - the condition string for this field (string)'''

        AbstractFixedField.__init__(self, perspective.fixedfieldtypes.HEADER, xStart, yStart, condition)

        self.setHeaderType(headerType);
        self.setForegroundColor(foreColor)
        self.setBackgroundColor(backColor)
        self.setTransparent(transparent)

    def setHeaderType(self, type):
        ''' Sets the header type name.

        Args
        ----
        type - the header type name (string)'''

        if type is None:
            raise mpcsutil.err.InvalidInitError('The type argument cannot be None.')

        self.headerType = str(type)

    def setForegroundColor(self, color):
        ''' Sets the foreground color of the text field. If None, the foreground
        color defined in the parent fixed layout will be used.

        Args
        ----
        color - the foreground color for the field (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The foreground argument must be of type Color.')
        self.foreColor = color

    def setBackgroundColor(self, color):
        ''' Sets the background color of the text field. If None, the color
        defined in the parent fixed layout will be used.
        Colors are represented as strings of the form "r,g,b".

        Args
        ----
        color - the background color for the field (Color); may be None'''

        if not color is None and not isinstance(color, Color):
            raise mpcsutil.err.InvalidInitError('The background argument must be of type Color.')

        self.backColor = color

    def setTransparent(self, transparent=True):
        '''Sets the text background to be transparent, allowing fields beneath
        this text field to show through.

        Args
        ----
        transparent - True to set transparent background (default), False to disable (boolean)'''

        if transparent is None or transparent not in [True,False]:
            raise mpcsutil.err.InvalidInitError('The transparency argument cannot be None and must be boolean.')

        self.isTransparent = bool(transparent)

    def getHeaderType(self):
        ''' Gets the header type name.

        Returns
        --------
        type - the header type name (string)'''

        return self.headerType

    def getForegroundColor(self):
        '''Gets the foreground color to render this text field. If None,
        then the foreground color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the foreground color for the field (Color); may be None'''

        return self.foreColor

    def getBackgroundColor(self):
        '''Gets the background color of the FixedLayout. If None, then the
        background color defined in the parent fixed layout will be used.

        Returns
        --------
        color - the background color for the field (Color); may be None'''

        return self.backColor

    def getTransparent(self):
        '''Gets the transparent flag for this field, indicating whether it will
        be drawn with a transparent background.

        Returns
        -------
        isTransparent - True if transparent flag is set; False if not (boolean)'''

        return self.isTransparent

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = AbstractFixedField.__str__(self)
        returnStr += "\theaderType=%s\n\tforeColor=%s\n\tbackColor=%s\n\tisTransparent=%s" % \
             (self.headerType,str(self.foreColor),str(self.backColor),str(self.isTransparent))
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<Header ';

        returnStr += AbstractFixedField.__repr__(self)

        returnStr += 'headerType="%s" ' % (self.headerType)

        if self.foreColor is not None:
            returnStr += 'foreground="%s" ' % repr(self.foreColor)

        if self.backColor is not None:
            returnStr += 'background="%s" ' % repr(self.backColor)

        returnStr += 'transparent="%s" ' %  str(self.isTransparent).lower()
        returnStr += '/>\n'

        return returnStr;

class Condition(object):
    ''' A condition object represents a channel condition, in which the value of a telemetry channel
    can be used to control the display of other fields in a fixed view.

    Object Attributes
    -----------------
    conditionId - the unique ID of the condition (string)
    channelId - the ID of the channel on which the condition is based (string)
    sourceType - the type of channel source field to examine to obtain the value for the condition (ConditionFieldTypes)
    conditionType - the type of condition this is (ConditionTypes)
    value - the value for comparison, for condition types that require one (string)'''

    def __init__(self,
             conditionId,
             channelId,
             sourceType,
             conditionType,
             value=None):

        '''Initializes a Condition object

        Args
        ----
        conditionId - the unique ID of the condition; may not be None (string)
        channelId - the ID of the channel on which the condition is based; may not be None (string)
        sourceType - the type of channel source field to examine to obtain the value for the condition;
                     will be None for alarm, stale, and null check conditions (ConditionFieldTypes)
        conditionType - the type of condition this is; may not be None (ConditionTypes)
        value - the value for comparison, for condition types that require one; may be None (string)'''

        self.setChannelId(channelId)
        self.setConditionId(conditionId)
        self.setSourceType(sourceType)
        self.setConditionType(conditionType)
        self.setValue(value)

    def setConditionType(self, conditionType):
        '''Sets the condition type for the condition.

        Args
        ----
        conditionType - the condition type of this condition (ConditionTypes)'''

        if conditionType is None or str(conditionType) not in perspective.conditiontypes.ConditionTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The conditionType argument cannot be None and must be one of the valid ConditionTypes.')

        self.conditionType = conditionType

    def setSourceType(self, sourceType):
        '''Sets the source-field type for the channel field to check for the condition.

        Args
        ----
        sourceType - the channel source field type for the channel field to check (ConditionFieldTypes)'''

        if sourceType is not None and str(sourceType) not in perspective.conditionfieldtypes.ConditionFieldTypes.VALUES:
            raise mpcsutil.err.InvalidInitError('The sourceType argument cannot be None and must be one of the valid ConditionFieldTypes.')

        self.sourceType = sourceType

    def setChannelId(self,channelId):
        '''Sets the dictionary ID of the channel used for the condition check.

        Args
        ----
        channelId - the dictionary channel ID (string)'''

        if channelId is None:
            raise mpcsutil.err.InvalidInitError('The channel ID argument cannot be None.')

        self.channelId = str(channelId)

    def setConditionId(self,conditionId):
        '''Sets the unique condition ID of this condition.

        Args
        ----
        conditionId - the condition ID; may not be None (string)'''

        if conditionId is None:
            raise mpcsutil.err.InvalidInitError('The condition ID argument cannot be None.')

        if not stringOkForXml(conditionId):
            raise mpcsutil.err.InvalidInitError('The condition ID argument cannot contain &, ", <, or >')

        self.conditionId = str(conditionId)

    def setValue(self,value):
        '''Sets the value to be used for comparison in this condition.

        Args
        ----
        value - the value to use for comparison (string)'''

        if value is None:
            self.value = None

        self.value = str(value)

    def getConditionType(self):
        '''Gets the condition type of this condition.

        Returns
        -------
        type - the condition type for this field (ConditionTypes)'''

        return self.conditionType

    def getSourceType(self):
        '''Gets the source field type of the channel field used for the condition check.

        Returns
        -------
        type - the source type for this field (ConditionFieldTypes)'''

        return self.sourceType

    def getChannelId(self):
        '''Gets the dictionary ID of the channel used for this condition check.

        Returns
        -------
        channelId - the dictionary channel ID (string)'''

        return self.channelId

    def getConditionId(self):
        '''Gets the condition ID of this condition.

        Returns
        -------
        id - the condition ID for this field (string)'''

        return self.conditionId

    def getValue(self):
        '''Gets the comparison value for this condition.

        Returns
        -------
        value - the value for this condition; may be None (string)'''

        return self.value

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        returnStr = "ChannelCondition "
        returnStr += "id=%s\n\tchannelId=%s\n\tsourceType=%s\n\tconditionType=%s\n\tvalue=s%s" % \
             (self.conditionId,self.channelId,str(self.sourceType),str(self.conditionType),self.value)
        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        returnStr = '\t<ChannelCondition ';

        returnStr += 'conditionID="%s" ' % (self.conditionId)
        returnStr += 'channelID="%s" ' % (self.channelId)
        returnStr += 'comparison="%s" ' % str(self.conditionType)

        if self.sourceType is not None:
            returnStr += 'sourceField="%s" ' % str(self.sourceType)

        if self.value is not None:
            returnStr += 'value="%s" ' % (self.value)

        returnStr += '/>\n'

        return returnStr

def stringOkForXml(text):
    '''Checks if the input text contains characters that are invalid in XML output.

    Returns
    -------
    ok - True if text is ok for XML, False if not'''

    if text is None:
        return True
    if text.find('&') != -1 or text.find('"') != -1 or text.find('<') != -1 or text.find('>') != -1:
        return False
    return True

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
