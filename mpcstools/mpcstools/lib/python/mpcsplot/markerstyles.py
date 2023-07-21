#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import mpcsutil

class MarkerStyles(mpcsutil.enum.EnumType):

    POINT_TYPE = 0
    PIXEL_TYPE = 1
    CIRCLE_TYPE = 2
    TRIANGLE_DOWN_TYPE = 3
    TRIANGLE_UP_TYPE = 4
    TRIANGLE_LEFT_TYPE = 5
    TRIANGLE_RIGHT_TYPE = 6
    SQUARE_TYPE = 7
    PENTAGON_TYPE = 8
    STAR_TYPE = 9
    THIN_HEXAGON_TYPE = 10
    HEXAGON_TYPE = 11
    PLUS_TYPE = 12
    X_TYPE = 13
    THIN_DIAMOND_TYPE = 14
    DIAMOND_TYPE = 15
    VERTICAL_LINE_TYPE = 16
    HORIZONTAL_LINE_TYPE = 17

    VALUES = ['point','pixel','circle','triangle_down','triangle_up','triangle_left','triangle_right',
              'square','pentagon','star','thin_hexagon','hexagon','plus','x','thin_diamond','diamond',
              'vertical_line','horizontal_line']

    def __init__(self,numval=None,strval=None):
        super(MarkerStyles,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.markerstyles.MarkerStyles(numval=%d)' % self.val_num

    def get_prop_val(self):

        if self.val_num == MarkerStyles.POINT_TYPE:
            return '.'
        elif self.val_num == MarkerStyles.PIXEL_TYPE:
            return ','
        elif self.val_num == MarkerStyles.CIRCLE_TYPE:
            return 'o'
        elif self.val_num == MarkerStyles.TRIANGLE_DOWN_TYPE:
            return 'v'
        elif self.val_num == MarkerStyles.TRIANGLE_UP_TYPE:
            return '^'
        elif self.val_num == MarkerStyles.TRIANGLE_LEFT_TYPE:
            return '<'
        elif self.val_num == MarkerStyles.TRIANGLE_RIGHT_TYPE:
            return '>'
        elif self.val_num == MarkerStyles.SQUARE_TYPE:
            return 's'
        elif self.val_num == MarkerStyles.PENTAGON_TYPE:
            return 'p'
        elif self.val_num == MarkerStyles.STAR_TYPE:
            return '*'
        elif self.val_num == MarkerStyles.THIN_HEXAGON_TYPE:
            return 'h'
        elif self.val_num == MarkerStyles.HEXAGON_TYPE:
            return 'H'
        elif self.val_num == MarkerStyles.PLUS_TYPE:
            return '+'
        elif self.val_num == MarkerStyles.X_TYPE:
            return 'x'
        elif self.val_num == MarkerStyles.THIN_DIAMOND_TYPE:
            return 'd'
        elif self.val_num == MarkerStyles.DIAMOND_TYPE:
            return 'D'
        elif self.val_num == MarkerStyles.VERTICAL_LINE_TYPE:
            return '|'
        elif self.val_num == MarkerStyles.HORIZONTAL_LINE_TYPE:
            return '_'

    def get_values(self):
        return MarkerStyles.VALUES

    @staticmethod
    def get_default():
        return MarkerStyles(numval=MarkerStyles.CIRCLE_TYPE)

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return MarkerStyles(numval=self.val_num)

POINT = MarkerStyles(numval=MarkerStyles.POINT_TYPE)
PIXEL = MarkerStyles(numval=MarkerStyles.PIXEL_TYPE)
CIRCLE = MarkerStyles(numval=MarkerStyles.CIRCLE_TYPE)
TRIANGLE_DOWN = MarkerStyles(numval=MarkerStyles.TRIANGLE_DOWN_TYPE)
TRIANGLE_UP = MarkerStyles(numval=MarkerStyles.TRIANGLE_UP_TYPE)
TRIANGLE_LEFT = MarkerStyles(numval=MarkerStyles.TRIANGLE_LEFT_TYPE)
TRIANGLE_RIGHT = MarkerStyles(numval=MarkerStyles.TRIANGLE_RIGHT_TYPE)
SQUARE = MarkerStyles(numval=MarkerStyles.SQUARE_TYPE)
PENTAGON = MarkerStyles(numval=MarkerStyles.PENTAGON_TYPE)
STAR = MarkerStyles(numval=MarkerStyles.STAR_TYPE)
THIN_HEXAGON = MarkerStyles(numval=MarkerStyles.THIN_HEXAGON_TYPE)
HEXAGON = MarkerStyles(numval=MarkerStyles.HEXAGON_TYPE)
PLUS = MarkerStyles(numval=MarkerStyles.PLUS_TYPE)
X = MarkerStyles(numval=MarkerStyles.X_TYPE)
THIN_DIAMOND = MarkerStyles(numval=MarkerStyles.THIN_DIAMOND_TYPE)
DIAMOND = MarkerStyles(numval=MarkerStyles.DIAMOND_TYPE)
VERTICAL_LINE = MarkerStyles(numval=MarkerStyles.VERTICAL_LINE_TYPE)
HORIZONTAL_LINE = MarkerStyles(numval=MarkerStyles.HORIZONTAL_LINE_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
