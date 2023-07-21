#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines a common object to represent a
CFDP Indication that can be used across MPCS missions.
"""

from __future__ import (absolute_import, division, print_function)


import mpcsutil

class CfdpIndication:
    '''A CfdpIndication object represents a key indication genereated by the CFDP Processor,
    which provides information about the state of a CFDP transaction. The key indication types
    provided to MTAK via this class are: (1) Transaction, (2) New Transaction Detected, (3)
    Fault, (4) Transaction Finished, and (5) Abandoned.'''

    def __init__(self, csvString=None, **kwargs):
        ''' Initializer can take a csv CFDP Indication message and construct a CfdpIndication
        object, or it can take an arbitrary set of key value arguments which will be set on
        the object (assuming the entries are already attributes of this object)

        Args
        -----
        csvString - A comma separated value string representing the CFDP Indication
        kwargs - Any keyword arguments whose names match the names of attributes on this object (dictionary {})

        Object Attributes
        ------------------
        indicationType - The CFDP Indication type: "tx" (Transaction), "ft" (Fault),
        "tf" (Transaction Finished), "ab" (Abandoned), and "txd" (New Transaction Detected) (string)
        sourceEntityId - Source entity ID for the transaction (string)
        transactionSequenceNumber - The transaction sequence number (string)
        eventTime - The time (ISO format) that MPCS published the CFDP Indication message (string)'''

        self.clear()

        if csvString is not None:
            self.parseFromCsv(csvString)

        for key in kwargs.keys():
            if hasattr(self,key):
                value = kwargs[key]
                setattr(self,key,value)

    def parseFromCsv(self, csvString):
        ''' Parses the CSV string and construct the CFDP Indication. The CSV string
        must be well formed or an exception may be generated.

        Args
        -----
        csvString - A comma separated value string representing the CFDP Indication

        Returns
        --------
        None'''

        splitString = csvString.split('\,')

        # indicationType must match jpl.gds.cfdp.data.api.ECfdpIndicationType's CSV keywords
        self.indicationType=splitString[1]
        self.eventTime=splitString[2]
        self.sourceEntityId=splitString[3]
        self.transactionSequenceNumber=splitString[4]

    def toCsv(self):
        '''Generate a CSV representation of this CFDP Indication. This value is in
        the format expected by the parseFromCsv method.

        Args
        -----
        None

        Returns
        --------
        A CSV-formmatted string representing this object (string)'''

        return "cfdp-ind\,%s\,%s\,%s\,%s" % \
               (self.indicationType, self.eventTime, self.sourceEntityId, self.transactionSequenceNumber)

    def clear(self):
        ''' Internal method that clears out the values of all local attributes from the Product.

        Args
        -----
        None

        Returns
        --------
        None'''

        self.indicationType = ''
        self.eventTime = ''
        self.sourceEntityId = ''
        self.transactionSequenceNumber = ''

    def __getitem__(self, item):
        return self.__getattribute__(item)

    def __setitem__(self, key, value):
        self.__setattr__(key, value)

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        val = '%s.%s(' % (self.__module__,self.__class__.__name__)
        for i in dir(self):
            attr = getattr(self,i)
            if not i.startswith('_') and not callable(attr):
                if isinstance(attr,str):
                    val += '%s=\"%s\",' % (i,attr)
                else:
                    val += '%s=%s,' % (i,attr)
        val = val[:-1] + ')'

        return val

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        return self.__repr__()

    def __eq__(self,other):
        '''x.__eq__(y) <==> x == y'''

        if other == None:
            return False

        try:
            return    self.indicationType == other.indicationType and\
                      self.eventTime == other.eventTime and\
                      self.sourceEntityId == other.sourceEntityId and\
                      self.transactionSequenceNumber == other.transactionSequenceNumber

        except AttributeError:
            return False

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        return self.__eq__(other) == False

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
