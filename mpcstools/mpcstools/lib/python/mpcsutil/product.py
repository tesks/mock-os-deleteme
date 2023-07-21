#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines a common object to represent a
data product that can be used across MPCS missions.
"""

from __future__ import (absolute_import, division, print_function)


import mpcsutil
import six
long=int if six.PY3 else long

class Product(mpcsutil.TelemetryItem):
    '''A Product object represents a product built from one or more product messages. Unlike an EVR or an
    EHA channel value in MTAK, one data product can have multiple JMS messages that correspond to it. A data
    product is always a work in progress and this product object can be updated in place multiple times and
    can change its status multiple times (e.g. in progress to partial to in progress to complete)'''

    #Item on the left is an alias for item on the right, so now
    #if I do this: print dp.scet
    #
    #It's the same as doing: print dp.dvtScet
    #
    #This works for setting and getting of attributes
    _ALIASES = {
               'sclkFine'   : 'dvtFine',
               'sclkCoarse' : 'dvtCoarse',
               'sclk'       : 'dvtSclk',
               'sclkExact'  : 'dvtSclkExact',
               'scet'       : 'dvtScet',
               'scetExact'  : 'dvtScetExact',
               'scetNano'   : 'dvtScetNano',
               'lst'        : 'dvtLst',
               'lstExact'   : 'dvtLstExact'
               }

    def __init__(self, csvString=None, **kwargs):
        ''' Initializer can take a csv Product message and construct a Product object,
        or it can take an arbitrary set of key value arguments which will be set on
        the object (assuming the entries are already attributes of this object)

        Args
        -----
        csvString - A comma separated value string representing the product.  These strings are mission-dependent, so you
        should look at the following templates:

        templates/common/<mission>/PartialProduct/Mtak.vm
        templates/common/<mission>/ProductAssembled/Mtak.vm

        kwargs - Any keyword arguments whose names match the names of attributes on this object (dictionary {})

        Object Attributes
        ------------------
        name - The product name (string)
        transactionId - The transaction ID unique to this product (string)
        apid - The APID of this product (int)
        dvtCoarse - The coarse portion of the DVT SCLK (int)
        dvtFine - The fine portion of the DVT SCLK (int)
        eventTime - The time (ISO format) that MPCS published the product message (string)
        dvtScet - The DVT Spacecraft Event Time (ISO format) (string)
        dvtScetExact - milliseconds for the DVT Spacecraft Event Time (int)
        dvtScetNano - nanoseconds for the DVT Spacecraft Event Time (int)
        dvtLst - The Local Solar Time value in SOL-XXXXMHH:MM:SS.sss format
        dvtSclk - The DVT SCLK in the form CCCCCCCCCC-FFFFF (string)
        partialFiles - A list of partial product files associated with this product (tuples of the form ("filename","transaction log","creation time")
        completeFile - The complete product file path (string)
        totalParts - The total number of parts in this product (int)
        status - The current status of the product('Complete' or 'Partial') (string)
        reason - The reason a partial product was pushed out (string)
        creationTime - The time (ISO format) that this product was created (string)

        MSL-specific
        -------------
        requestId - The request ID of the product (int)
        priority - The priority of the product (int)'''

        mpcsutil.TelemetryItem.__init__(self)

        self.clear()

        if csvString is not None:
            self.parseFromCsv(csvString)

        for key in kwargs.keys():
            if hasattr(self,key):
                value = kwargs[key]
                if (key == "dvtCoarse" or key == "dvtFine" or key == "apid" or key == "totalParts"  or key == 'dvtSclkExact' or
                    key == "eventTimeExact" or key == "dvtScetExact" or key == "dvtLstExact" or key == "ertExact" or key == "ertExactFine" or key == 'receiveTime'):
                    if value:
                        setattr(self,key,long(value))
                elif key == "dvtScetNano":
                    if value:
                        setattr(self, key, int(value))
                else:
                    setattr(self,key,value)
        indx = getattr(self, 'dvtScet', -1).find(".")
        dvtScet =str(getattr(self, 'dvtScet'))
        if getattr(self, 'dvtScetExact') is None or getattr(self, 'dvtScetExact') == 0 and indx > 0:
            setattr(self, 'dvtScetExact', long(mpcsutil.timeutil.parseTimeString(getattr(self, 'dvtScet'))))
        if getattr(self, 'dvtScetNano') is None or getattr(self,'dvtScetNano') == 0 and indx > 0 and mpcsutil.timeutil.getScetPrecision() > 3:
            if len(dvtScet[indx + 1 : len(dvtScet)]) > 3:
                setattr(self, 'dvtScetNano', int(dvtScet[indx + 3:]))

    '''
    MPCS-7414  - 6/12/2015: moved aliasing functionality from TelemetryItem
    super class into Product itself.  Original implementation may have expected to
    support this feature for ChannelValue and EVR, but it does not seem to be needed
    and is very expensive to perform when MTAK is receiving telemetry off the JMS bus.
    In the future it will probably be better to not allow aliasing - time doesn't allow now.
    '''
    def __setattr__(self, name, value):

        if hasattr(self,'_ALIASES'):
            name = self._ALIASES.get(name, name)
        object.__setattr__(self, name, value)

    def __getattr__(self, name):

        if name == "_ALIASES":
            raise AttributeError  # http://nedbatchelder.com/blog/201010/surprising_getattr_recursion.html
        if hasattr(self,'_ALIASES'):
            name = self._ALIASES.get(name, name)
        return object.__getattribute__(self, name)
        #return getattr(self,name) this line causes an infinite recursion on non-existent attributes

    def __delattr__(self,name):
        if name == '_ALIASES':
            raise AttributeError('Cannot delete _ALIASES!')
        if hasattr(self,'_ALIASES'):
            name = self._ALIASES.get(name,name)
        return object.__delattr__(self,name)
        #return delattr(self,name) this line causes an infinite recursion on non-existent attributes

    def parseFromCsv(self, csvString):
        ''' Parses the Csv string and construct the Product.   The CSV string
        must be well formed or an exception may be generated.

        Args
        -----
        csvString - A comma separated value string representing the product.  These strings are mission-dependent, so you
        should look at the following templates:

        templates/common/PartialProduct/Mtak.vm
        templates/common/ProductAssembled/Mtak.vm

        Returns
        --------
        None'''

        splitString = csvString.split('\,')

        self.status=splitString[1]
        self.eventTime=splitString[2]
        if splitString[3]:
            self.eventTimeExact=long(splitString[3])
        self.transactionId = splitString[4]
        self.name = splitString[5]
        if splitString[6]:
            self.apid = int(splitString[6])
        if splitString[7]:
            self.totalParts=int(splitString[7])
        if splitString[8]:
            self.dvtSclk = splitString[8]
        if splitString[9]:
            self.dvtCoarse = long(splitString[9])
        if splitString[10]:
            self.dvtFine = long(splitString[10])
        if splitString[11]:
            self.dvtSclkExact = long(splitString[11])
        self.dvtScet = splitString[12]
        if splitString[13]:
            self.dvtScetExact = long(splitString[13])
            if self.dvtScetExact == 0:
                self.dvtScetExact = long(mpcsutil.timeutil.parseTimeString(self.dvtScet) / 1000)
        if splitString[14]:
            self.dvtScetNano = long(splitString[14])
        self.dvtLst = splitString[15]
        if splitString[16]:
            self.dvtLstExact = long(splitString[16])
        self.ert = splitString[17]
        if splitString[18]:
            self.ertExact = long(splitString[18])
        if splitString[19]:
            self.ertExactFine = long(splitString[19])

        filename = splitString[20]
        meta_filename = '%semd' % (filename[:-3])

        self.creationTime = splitString[21]
        self.reason = splitString[22]

        if self.status.startswith('P'): #Partial Product
            self.partialFiles.append([filename,meta_filename,self.creationTime]) #product file, null for transaction log, product creation time
        else: #Complete Product
            self.completeFile = filename
            self.metadataFile = meta_filename

        i = 23
        while i+1 < len(splitString):
            name = splitString[i]
            val = splitString[i+1]
            if val.isdigit(): #if property is a number, change it to an int (doesn't work for floats)
                val = int(val)
            setattr(self,name,val) #add mission properties onto this object dynamically
            i+=2


    def toCsv(self):
        '''Generate a CSV Representation of this data product.  This value is in the format expected
        by the parseFromCsv method.

        Args
        -----
        None

        Returns
        --------
        A CSV-formmatted string representing this object (string)'''

        #TODO: is there any way to make this method deal with the dynamically attached attributes?  Might have to start holding onto some
        #arrays with names/values
        return "prod\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,\%s,%s\,%s\,"\
                "%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s\,%s" % \
               (self.status,self.eventTime,self.eventTimeExact,self.name,
                self.transactionId,self.apid,self.totalParts,self.dvtSclk,
                self.dvtCoarse,self.dvtFine,self.dvtSclkExact,self.dvtScet,
                self.dvtScetExact, self.dvtScetNano, self.dvtLst,self.dvtLstExact,self.ert,
                self.ertExact,self.ertExactFine,
                self.completeFile if self.status.startswith('C') else self.partialFiles[-1][0],
                self.creationTime,self.reason)

    def clear(self):
        ''' Internal method that clears out the values of all local attributes from the Product.

        Args
        -----
        None

        Returns
        --------
        None'''

        self.receiveTime = 0
        self.name = ''
        self.transactionId = ''
        self.apid = 0
        self.dvtCoarse = 0
        self.dvtFine = 0
        self.dvtSclk = ''
        self.dvtSclkExact = 0
        self.dvtScet = ''
        self.dvtScetExact=0
        self.dvtScetNano=0
        self.dvtLst = ''
        self.dvtLstExact=0
        self.ert=''
        self.ertExact=0
        self.ertExactFine = 0
        self.eventTime = ''
        self.eventTimeExact=0
        self.totalParts=0
        self.completeFile=''
        self.metadataFile=''
        self.creationTime = ''
        self.partialFiles=[]
        self.status = ''
        self.reason = ''
        self.injected = False

    def __getitem__(self, item):
        return self.__getattribute__(item)

    def __setitem__(self, key, value):
        self.__setattr__(key, value)

    def update(self,productObj):
        '''Updates the current product with values of the specified product.
        The fields that are updated are: eventTime, partialFiles, completeFile, partNumbers, status, & reason.
        This should be used when multiple product messages construct a single product.

        Args
        -----
        productObj - A product object whose values will be used to update this product object's values (mpcsutil.product.Product)

        Returns
        --------
        None'''

        if self.eventTimeExact < productObj.eventTimeExact:
            self.eventTime = productObj.eventTime
            self.eventTimeExact = productObj.eventTimeExact

        if (self.ertExact < productObj.ertExact) or\
           (self.ertExact == productObj.ertExact and self.ertExactFine < productObj.ertExactFine):
            self.ert = productObj.ert
            self.ertExact = productObj.ertExact
            self.ertExactFine = productObj.ertExactFine

        if self.dvtScetExact < productObj.dvtScetExact:
            self.dvtScet = productObj.dvtScet
            self.dvtScetExact = productObj.dvtScetExact
            self.dvtScetNano = productObj.dvtScetNano

        if self.dvtLstExact < productObj.dvtLstExact:
            self.dvtLst = productObj.dvtLst
            self.dvtLstExact = productObj.dvtLstExact

        if self.dvtSclkExact < productObj.dvtSclkExact:
            self.dvtSclk = productObj.dvtSclk
            self.dvtSclkExact = productObj.dvtSclkExact

        self.reason = productObj.reason

        for partial in productObj.partialFiles:
            if not partial in self.partialFiles:
                self.partialFiles.append(partial)

        self.creationTime = productObj.creationTime
        self.receiveTime = productObj.receiveTime

        if not self.completeFile:
            self.completeFile = productObj.completeFile

        if not self.metadataFile:
            self.metadataFile = productObj.metadataFile

        if not self.status.startswith('C'):
            self.status = productObj.status

        #NOTE: currently does not pick up dynamically attached attributes like the requestId/priority for MSL

    def __repr__(self):
        '''x.__repr__() <==> repr(x)'''

        val = '%s.%s(' % (self.__module__,self.__class__.__name__)
        for i in dir(self):
            attr = getattr(self,i)
            if not i.startswith('_') and not callable(attr) and i not in self._ignore:
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
            return    self.name == other.name and\
                      self.transactionId == other.transactionId and\
                      self.apid == other.apid and\
                      self.dvtSclkExact == other.dvtSclkExact and\
                      self.dvtScetExact == other.dvtScetExact and\
                      self.dvtScetNano == other.dvtScetNano and\
                      self.dvtLstExact == other.dvtLstExact and\
                      self.ertExact == other.ertExact and\
                      self.ertExactFine == other.ertExactFine and\
                      self.totalParts == other.totalParts and\
                      self.completeFile == other.completeFile and\
                      self.metadataFile == other.metadataFile and\
                      self.partialFiles == other.partialFiles and\
                      self.status == other.status and\
                      self.reason == other.reason

        except AttributeError:
            return False

    def __ne__(self,other):
        '''x.__ne__(y) <==> x != y'''

        return self.__eq__(other) == False

    def is_partial(self):

        return self.status[0] == 'P'

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
