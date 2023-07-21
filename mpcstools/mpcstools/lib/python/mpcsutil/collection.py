#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines collection objects necessary to the operation of
MTAK, but that are generic enough to be used for anything.  All of these
objects are defined to be threadsafe.
"""

from __future__ import (absolute_import, division, print_function)


import collections
import logging
import itertools
import threading
import six
# if six.PY2:
#     import UserDict.IterableUserDict as UserDict
# else:
#     from collections import UserDict

_log = lambda : logging.getLogger('mpcs.util')

try:
    base_str = basestring
    items = 'iteritems'
except NameError:
    base_str = str, bytes, bytearray
    items = 'items'
# class SyncDict(UserDict.IterableUserDict):
def cmp(a, b):
    if   a>b:
        return 1
    elif a<b:
        return -1
    return 0

class SyncDict(collections.OrderedDict):
    '''SyncDict is a synchronized, threadsafe dictionary object.  It is built by simple overriding all the
    normal dictionary methods with methods that acquire and release a class-owned threading.Lock object.

    For all intents and purposes, this object should be able to be used in exactly the same manner as a
    normal python dictionary object.

    NOTE: If the 'data' dictionary attribute is accessed directly, it will circumvent the synchronization that has
    been applied to this object.'''
    __slots__ = ('_classLock','data')

    # @staticmethod
    # def _process_args(map_or_it=(), **kwargs):
    #     if hasattr(map_or_it, items):
    #         map_or_it = getattr(map_or_it, items)()
    #     it_chain = itertools.chain
    #     return ((k, v) for k, v in it_chain(map_or_it, getattr(kwargs, items)()))

    def __init__(self, *args, **kwargs):
        '''Initialize the dictionary object.

        Args
        -----
        kwargs - keyword arguments may be specified as initial inputs to the dictionary (dictionary {})

        Returns
        --------
        None'''

        # _log().debug('mpcsutil.collection.SyncDict()')
        super(SyncDict, self).__init__(*args, **kwargs)
        self._classLock = threading.Lock()


    def __enter__(self):
        """Context manager enter the block, acquire the lock."""
        self._classLock.acquire()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit the block, release the lock."""
        self._classLock.release()

    def __getstate__(self):
        """Enable Pickling inside context blocks,
        through inclusion of the slot entries without the lock."""
        return collections.OrderedDict( (slot, getattr(self, slot)) for slot in self.__slots__ if hasattr(self, slot) and slot != '_classLock' )

    def __setstate__(self, state):
        """Restore the instance from pickle including the slot entries,
        without addition of a fresh lock.
        """
        [setattr(self, slot, value) for slot, value in getattr(state, items)()]
        self._classLock = threading.Lock()

    def __getitem__(self, k):
        """For now plain delegation of getitem method to base class."""
        return super(SyncDict, self).__getitem__(k)

    def __setitem__(self, k, v):
        """For now plain delegation of setitem method to base class."""
        return super(SyncDict, self).__setitem__(k, v)

    def __delitem__(self, k):
        """For now plain delegation of del method to base class."""
        return super(SyncDict, self).__delitem__(k)

    def get(self, k, failobj=None):
        """For now plain delegation of get method to base class."""
        return super(SyncDict, self).get(k, failobj)

    def setdefault(self, k, failobj=None):
        """For now plain delegation of setdefault method to base class."""
        return super(SyncDict, self).setdefault(k, failobj)

    def pop(self, k, d=None):
        """For now plain delegation of pop method to base class."""

        return super(SyncDict, self).pop(k) if d is(None) else super(SyncDict, self).pop(k, d)

    def __contains__(self, k):
        """For now plain delegation of contains method to base class."""
        return super(SyncDict, self).__contains__(k)

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''
        return '{}.{}({})'.format(self.__class__.__module__, self.__class__.__name__, ','.join('{}={}'.format(kk, repr(vv)) for kk,vv in self.items()))

    def iteritems(self):
        for kk,vv in self.items():
            yield kk,vv
    def iterkeys(self):
        for kk in self.keys():
            yield kk
    def itervalues(self):
        for vv in self.values():
            yield vv

    @classmethod
    def fromkeys(cls, seq, value=None):
        """For now plain delegation of fromkeys class method to base."""
        return super(SyncDict, cls).fromkeys(seq, value)

    def __cmp__(self, dict):
        '''x.__cmp__(y) <==> cmp(x,y)'''
        return cmp(sorted([(kk,vv) for kk,vv in self.items()], key=lambda x: x[0]), sorted([(kk,vv) for kk,vv in dict.items()], key=lambda x:x[0]))

    def __len__(self):
        '''x.__len__() <==> len(x)'''
        return super(SyncDict, self).__len__()

    def __iter__(self):
        return super(SyncDict, self).__iter__()
    def __gt__(self, other):
        return len(self) > len(other)
    def __lt__(self, other):
        return len(self) < len(other)
    def __ge__(self, other):
        return len(self) >= len(other)
    def __le__(self, other):
        return len(self) <= len(other)

    def update(self, dict=None, **kwargs):
        '''D.update(E, **F) -> None.  Update D from E and F: for k in E: D[k] = E[k]
        (if E has keys else: for (k, v) in E: D[k] = v) then: for k in F: D[k] = F[k]'''

        super(SyncDict, self).update(collections.OrderedDict(dict if dict else {}, **kwargs))

    def _getLock(self):

        return self._classLock

def test():
    import mpcsutil.collection
    def _manip():
        dict1 = mpcsutil.collection.SyncDict(**{'a':'b','c':'d','e':12345})
        dict1['f'] = 'g'
        del dict1['a']

        dict2 = {'w':'x','y':'z'}
        dict1.update(**dict2)

        dict3 = mpcsutil.collection.SyncDict(**{'m':'n'})
        dict1.update(dict=dict3)

        print('{}'.format(dict1))
        print('{}'.format(dict1.get('m')))
        print('{}'.format(dict1.get('non-existent-key',failobj='q')))
    _manip()

def main():
    return test()
if __name__ == "__main__":
    main()
