#! /usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import (absolute_import, division, print_function)

import mpcsutil.err


class EnumError(mpcsutil.err.MpcsUtilError):

    pass

class EnumType(object):

    def __init__(self,numval=None,strval=None):

        if numval is not None:
            self.set_from_numval(numval)
        elif strval is not None:
            self.set_from_strval(strval)

    def get_strval_from_numval(self,numval):

        if numval < 0 or numval > self.get_max_numval():
            return None

        return self.get_values()[numval]

    def get_numval_from_strval(self,strval):

        strval = strval.lower()
        values = self.get_values()
        for i in range(0,self.__len__()):
            if strval == values[i].lower():
                return i

        return None

    def get_max_numval(self):
        return self.__len__()-1

    def set_from_strval(self,strval):

        test_val = str(strval).lower()

        for i in range(0,self.__len__()):

            val = self.get_strval_from_numval(i).lower()
            if val == test_val:

                self.val_num = i
                self.val_str = val
                return

        raise EnumError('Invalid enumeration string value "%s"' % (strval))

    def set_from_numval(self,numval):

        numval = int(numval)

        if numval < 0 or numval > self.get_max_numval():
            raise EnumError('Invalid enumeration numval value "%s"' % (numval))

        self.val_num = numval
        self.val_str = self.get_strval_from_numval(numval)

    def get_prop_val(self):
        return ''

    def get_values(self):
        return []

    def __hash__(self):
        return hash(self.val_num)

    def __str__(self):
        return self.val_str

    def __repr__(self):
        return 'mpcsutil.enum.EnumType(numval=%d)' % self.val_num

    def __eq__(self,other):
        return isinstance(other,type(self)) and self.val_num == other.val_num

    def __ne__(self,other):
        return not isinstance(other,type(self)) or self.val_num != other.val_num

    def __lt__(self, other):
        return self.val_num < other.val_num

    def __le__(self, other):
        return self.val_num <= other.val_num

    def __gt__(self, other):
        return self.val_num > other.val_num

    def __ge__(self, other):
        return self.val_num >= other.val_num

    def __len__(self):
        return len(self.get_values())

    def __setattr__(self, name, value):
        if not name == 'val_num' and not name == 'val_str':
            raise EnumError('Enums are immutable.  Cannot modify attribute "%s".' % (name))
        object.__setattr__(self,name,value)

    def __delattr__(self, name):
        raise EnumError('Enums are immutable.  Cannot modify attribute "%s".' % (name))

    def __setitem__(self, numval, value):
        raise EnumError('Enums are immutable.  Cannot modify attribute "%s".' % (numval))

    def __delitem__(self, numval):
        raise EnumError('Enums are immutable.  Cannot modify attribute "%s".' % (numval))

    def __getitem__(self, numval):
        return self.get_strval_from_numval(numval)

    def __iter__(self):
        return iter(self.get_values())

    def __contains__(self, value):

        try:
            if self.get_numval_from_strval(value) is not None:
                return True
        except ValueError:
            pass

        try:
            if self.get_strval_from_numval(value) is not None:
                return True
        except ValueError:
            pass

        return False

def test(*args, **kwargs): pass
def main(*args, **kwargs): return test(*args, **kwargs)
if __name__ == "__main__": main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
