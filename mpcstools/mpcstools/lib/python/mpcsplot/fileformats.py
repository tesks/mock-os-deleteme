#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import copy

import mpcsutil

class FileFormats(mpcsutil.enum.EnumType):

    PNG_TYPE = 0
    PDF_TYPE = 1
    PS_TYPE = 2
    EPS_TYPE = 3
    SVG_TYPE = 4

    VALUES = ['png','pdf','ps','eps','svg']

    def __init__(self,numval=None,strval=None):
        super(FileFormats,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.fileformats.FileFormats(numval=%d)' % self.val_num

    def get_prop_val(self):
        return self.val_str

    def get_values(self):
        return FileFormats.VALUES

    @staticmethod
    def get_default():
        return FileFormats(numval=FileFormats.PNG_TYPE)

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return FileFormats(numval=self.val_num)

PNG = FileFormats(numval=FileFormats.PNG_TYPE)
PDF = FileFormats(numval=FileFormats.PDF_TYPE)
PS = FileFormats(numval=FileFormats.PS_TYPE)
EPS = FileFormats(numval=FileFormats.EPS_TYPE)
SVG = FileFormats(numval=FileFormats.SVG_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
