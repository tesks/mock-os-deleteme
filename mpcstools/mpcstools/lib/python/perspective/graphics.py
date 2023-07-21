#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains constants and data types that represent GUI settings
and GUI objects in the MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)


import mpcsutil
import re

class Color(object):
    '''This class represents a color, consisting of red, green, and blue constituents,
    each of which has a value between 0 and 255.

    Object Attributes
    -----------------
    red - the red constituent of the color (integer, 0-255)
    green - the green constituent of the color (integer, 0-255)
    blue - the red constituent of the color (integer, 0-255)'''

    def __init__(self,
                 red,
                 green,
                 blue):
        self.setRed(red)
        self.setGreen(green)
        self.setBlue(blue)

    def setRed(self,red):
        '''Sets the red constituent of the Color.

        Args
        ----
        red - red value, from 0 to 155 (integer)'''

        if red is None or int(red) < 0 or int(red) > 255:
            raise mpcsutil.err.InvalidInitError('The red argument is None or out of the range 0-255.')

        self.red = int(red)

    def setGreen(self,green):
        '''Sets the green constituent of the Color.

        Args
        ----
        green - green value, from 0 to 155 (integer)'''

        if green is None or int(green) < 0 or int(green) > 255:
            raise mpcsutil.err.InvalidInitError('The green argument is None or out of the range 0-255.')

        self.green = int(green)

    def setBlue(self,blue):
        '''Sets the blue constituent of the Color.

        Args
        ----
        blue - blue value, from 0 to 155 (integer)'''

        if blue is None or int(blue) < 0 or int(blue) > 255:
            raise mpcsutil.err.InvalidInitError('The blue argument is None or out of the range 0-255.')

        self.blue = int(blue)

    def getRed(self):
        '''Gets the red constituent of the Color.

        Returns
        -------
        red - the red value (integer)'''

        return self.red

    def getGreen(self):
        '''Gets the green constituent of the Color.

        Returns
        -------
        green - the green value (integer)'''

        return self.green

    def getBlue(self):
        '''Gets the blue constituent of the Color.

        Returns
        -------
        red - the blue value (integer)'''

        return self.blue

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        return "%03d,%03d,%03d" % (self.red,self.green,self.blue)

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        return str(self)

def getColorFromString(rgbString):
    '''Creates a Color object from a "red,green,blue" comma-separated string.

    Args
    ----
    rgbString - a string of the form "red,green,blue" where each color value is
                between 0 and 255

    Returns
    -------
    color - the Color object created from the input string'''

    pattern = '\d{1,3}[,]\d{1,3}[,]\d{1,3}'

    if rgbString is None or (re.match(pattern, rgbString) is None):
        raise mpcsutil.err.InvalidInitError("The RGB string argument is None or does not have the proper format.")

    rgbList = re.split('[,]', rgbString);

    color = Color(rgbList[0],rgbList[1],rgbList[2])

    return color

NORMAL = 0
BOLD = 1
ITALIC = 2

class Font(object):
    '''This class represents a font, consisting of face, point size, and style constituents.

    Object Attributes
    -----------------
    face - the name of the font face, e.g., "Courier" (string)
    pointSize - the point size of the font, e.g. 12 (integer)
    style - the style of the font: NORMAL, BOLD, or ITALIC (integer)
    reverse - a flag indicating if the font should be displayed in reverse video (boolean)'''

    def __init__(self,
              face,
              size,
              style=NORMAL,
              reverse=False):
        self.setFace(face)
        self.setPointSize(size)
        self.setStyle(style)
        self.setReverse(reverse)

    def setFace(self,face):
        '''Sets the face constituent of the Font.

        Args
        ----
        face - face name '''

        if face is None:
            raise mpcsutil.err.InvalidInitError('The face argument cannot be None.')

        self.face = str(face)

    def setPointSize(self,size):
        '''Sets the point size constituent of the Font.

        Args
        ----
        size - size value, from 0 to 100 (integer)'''

        if size is None or int(size) < 0 or int(size) > 100:
            raise mpcsutil.err.InvalidInitError('The size argument is None or out of the range 0-100.')

        self.pointSize = int(size)

    def setStyle(self,style):
        '''Sets the style constituent of the Font.

        Args
        ----
        style - style value, NORMAL, BOLD, or ITALIC (integer)'''

        if style is None or int(style) not in [NORMAL, BOLD, ITALIC]:
            raise mpcsutil.err.InvalidInitError('The style argument is None or is not one of [NORMAL, BOLD, or ITALIC].')

        self.style = int(style)

    def setReverse(self, reverse):

        '''Sets the reverse flag for the Font, indicating whether it should be displayed in
        reverse video.

        Args
        ----
        reverse - True to indicate reverse video, false for normal video (boolean)'''

        if reverse is None or reverse not in [True, False]:
            raise mpcsutil.err.InvalidInitError('The reverse argument is none or is not boolean.')

        self.reverse = bool(reverse)

    def getFace(self):
        '''Gets the face constituent of the Font.

        Returns
        -------
        face - face name '''

        return self.face

    def getPointSize(self):
        '''Gets the point size constituent of the Font.

        Returns
        -------
        size - size value, from 0 to 100 (integer)'''

        return self.pointSize

    def getStyle(self):
        '''Gets the style constituent of the Font.

        Returns
        -------
        style - style value, NORMAL, BOLD, or ITALIC (integer)'''

        return self.style

    def getReverse(self):

        '''Gets the reverse flag for the Font, indicating whether it should be displayed in
        reverse video.

        Returns
        -------
        reverse - True to indicate reverse video, false for normal video (boolean)'''

        return self.reverse

    def __str__(self):

        '''x.__str__() <==> str(x)'''

        returnStr = "%s,%d," % (self.face, self.pointSize)

        if (self.style == NORMAL):
            returnStr += 'NORMAL'
        elif (self.style == BOLD):
            returnStr += 'BOLD'
        else:
            returnStr += 'ITALIC'

        if self.reverse == True:
            returnStr += ',REVERSE'

        return returnStr

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        return str(self)

def getFontFromString(fontString):
    '''Creates a Font object from a "face,point-size,style,[REVERSE]" comma-separated string.

    Args
    ----
    fonttring - a string of the form "face,point-size,style,[REVERSE]"

    Returns
    -------
    font - the Font object created from the input string'''

    pattern = '[\w\-\#]+[,]\d{1,3}[,](NORMAL|ITALIC|BOLD)(,REVERSE)?'

    if fontString is None or (re.match(pattern, fontString) is None):
        raise mpcsutil.err.InvalidInitError("The font string argument is None or does not have the proper format.")

    fontList = re.split('[,]', fontString);

    if fontList[2] == 'NORMAL':
        style = Font.NORMAL
    elif fontList[2] == 'BOLD':
        style = Font.BOLD
    else:
        style = Font.ITALIC

    if len(fontList) == 3:
        font = Font(fontList[0],fontList[1],style)
    else:
        font = Font(fontList[0],fontList[1],style,True)

    return font

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
