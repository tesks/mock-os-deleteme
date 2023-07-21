#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains all of the objects used to register wait conditions in MTAK.
"""

from __future__ import (absolute_import, division, print_function)


import logging
import mpcsutil
import mtak.err

_log = lambda : logging.getLogger('mpcs.mtak')

class AbstractWait(object):
    '''This is the superclass for all wait condition objects.

    If you're looking at the code, the thing to note is that we keep track of
    which member variables have been set on the wait condition.  By doing so, each
    time we check the attributes on a wait condition, we only have to check the ones
    that are set instead of having to check all of them.

    Attributes
    -----------
    conditionMet - Sticky flag that is set to true once this condition has been
                   registered and met. (boolean)
    result - Sticky object that contains the object that satisfies the wait condition
             once such an object has been found. (various types)
    conditionSyncVar - A synchronization variable used in checking wait conditions (mtak.wait.Condition)
    _setList - A list of all the attributes that have been set on this object (list of strings)
    __slots__ - Special variable that restricts the attributes allowed to be set on this object.'''

    __slots__ = ['_setList','_uniqueList','conditionMet','conditionSyncVar','result','sclkTimeoutExact']

    def __init__(self,**kwargs):
        '''Initialize the wait condition

        Args
        -----
        kwargs - A dictionary of key/value pairs that correspond to attributes
        that can be set on these objects (dictionary)

        Returns
        --------
        None'''

        _log().debug('mtak.wait.AbstractWait()')

        object.__init__(self)

        self._setList = []
        self._uniqueList = []
        self.result = None
        self.conditionMet = False
        self.conditionSyncVar = None
        self.sclkTimeoutExact = None

        for key in kwargs:
            setattr(self,key,kwargs[key])

    def __setattr__(self,name,value):
        '''setattr(obj,name,value) <==> obj.name = value'''

        _log().debug('mtak.wait.AbstractWait.__setattr__()')

        #Set the attribute like normal
        object.__setattr__(self,name,value)

        #if it's in AbstractWait's __slots__, it's a special
        #member variable not set by the user
        if name in AbstractWait.__slots__:
            return

        #If the value's been set to None, get rid of it
        if value is None:
            delattr(self,name)
            return

        #If the value hasn't been set yet, add it to the list
        #of things that have been set
        if not name in self._setList and not name in self._uniqueList:
            self._setList.append(name)

    def __delattr__(self,name):
        '''delattr(obj,name) <==> del obj.name'''

        _log().debug('mtak.wait.AbstractWait.__delattr__()')

        #Delete the attribute like normal
        object.__delattr__(self,name)

        #If the value has been set previously, remove it
        #from the list of set values
        if name in self._setList:
            self._setList.remove(name)

    def _checkUniqueConditions(self,obj):

        return True

    def checkRequiredSettings(self):

        pass

    def checkCondition(self,obj):
        '''Check if the input object solves the current wait condition.

        This method assumes that wait condition attributes follow a naming
        convention where values that correspond have names that end in
        "Start" or "End" or "Substr" or "List".  Any attribute that does not
        have those suffixes is assumed to be an equality comparison.

        If the input object solves the wait condition, it will set the sticky flag
        conditionMet to True and will store the object "obj" in the class
        attribute "result".

        Args
        -----
        obj - The object to test to see if it solves this wait condition (various types)

        Returns
        --------
        None'''

        _log().debug('mtak.wait.AbstractWait.checkCondition()')

        #If this condition has already been met, return True
        if self.conditionMet:
            return True

        #If the input object isn't the right type, return False
        if not self.checkType(obj):
            return False

        if not self._checkUniqueConditions(obj):
            return False

        #Loop through all the attributes that have been
        #set on this wait condition
        for name in self._setList:

            #Get the value set on the wait condition
            targetVal = getattr(self,name)
            testVal = None

            #Is this the bottom end of a range comparison?
            if name.endswith('Start'):

                #See if the input object even has the
                #attribute we're interested in (the attribute
                #on the input object won't have "Start on it")
                try:
                    testVal = getattr(obj,name[0:-5])
                except AttributeError:
                    return False

                #If the input value is smaller, it's not in our range
                if targetVal > testVal:
                    return False

            #Is this the upper end of a range comparison?
            elif name.endswith('End'):

                #See if the input object even has the attribute
                #we're interested in (the attribute on the input object
                #won't have "End" on it)
                try:
                    testVal = getattr(obj,name[0:-3])
                except AttributeError:
                    return False

                #If the input value is bigger, it's not in our range
                if targetVal < testVal:
                    return False

            #It's a substring
            elif name.endswith('Substr'):

                #See if the input object even has the
                #attribute we're interested in (the attribute
                #on the input object won't have "Substr" on it)
                try:
                    testVal = getattr(obj,name[0:-6])
                except AttributeError:
                    return False

                #See if substring is present:
                if not targetVal in testVal:
                    return False

            #It's a list
            elif name.endswith('List'):

                #See if the input object even has the
                #attribute we're interested in (the attribute
                #on the input object won't have "List" on it)
                try:
                    testVal = getattr(obj,name[0:-4])
                except AttributeError:
                    return False

                #See if obj item is in wait condition's list:
                if not testVal in targetVal:
                    return False

            #It's a direct equality comparison
            else:

                #See if the input object even has the attribute
                #w'ere interested in
                try:
                    testVal = getattr(obj,name)
                except AttributeError:
                    return False

                #See if the values are equal
                if targetVal != testVal:
                    return False

        #If we didn't return False anywhere else, this object
        #must've met the wait condition...store its details
        #and return True
        self.conditionMet = True
        self.result = obj

        return True

    def checkType(self,obj):
        '''Check the type of the input object and see if it is
        a type that is accepted by this type of wait condition.

        This superclass method does nothing and should be overridden
        by subclasses.

        Args
        -----
        obj - The input object to test the type of (various types)

        Returns
        --------
        True if the object is accepted by this type of wait condition,
        False otherwise (boolean)'''

        _log().debug('mtak.wait.AbstractWait.checkType()')

        return True

    def __str__(self):
        '''x.__str__() <==> str(x)'''

        result = '%s(' % (self.__class__)
        for name in self._setList:
            targetVal = getattr(self,name)
            result += '%s=%s' % (name,targetVal)
        result += ')'

        return(result)


class ChanValWait(AbstractWait):
    '''This object creates a wait condition that will wait until
    a particular channel value arrives in the downlink stream

    All of the attributes of this object default to 'None'.  If any of the attributes are
    set, they become part of the condition that is checked.  All attributes set on this object
    are ANDed together to form a single channel value wait condition (e.g. if the 'channelId' field is set
    to 'A-0001' and the 'dnLowerBound' field was set to 50, then the wait condition would not be
    satisfied until a channel value message is received where the channelId is equal to 'A-0001' AND
    the DN is greater than 50).  To create ORing conditions, a CompoundWait object must be used.

    Attributes
    ----------
    dnStart - Inclusive lower bound for the DN value (various types).
    dnEnd - Inclusive upper bound for the DN value (various types).
    euStart - Inclusive lower bound for the EU value (floating point)
    euEnd - Inclusive upper bound for the EU value (floating point)
    ertStart - Inclusive lower bound for the Earth Receive Time (string)
    ertEnd - Inclusive upper bound for the Earth Receive Time (string)
    scetStart - Inclusive lower bound for the Spacecraft Event Time (string)
    scetEnd - Inclusive upper bound for the Spacecraft Event Time (string)
    sclkStart - Inclusive lower bound for the SCLK (string)
    sclkEnd - Inclusive upper bound for the SCLK (string)
    channelId - The ID of the channel to wait for a value on (e.g. 'FSW1-0001') (string)
    name - The name of the channel to wait for a value on (string)
    dn - The exact DN value to look for (various types)
    eu - The exact EU value to look for (floating point)
    _formatted - A private value used to indicate whether or not input DN/EU values have
    been properly formatted into the correct Python type before comparisons are done. (boolean)
    __slots__ - Special Python variable to restrict which attributes can be set on this object.'''

    __slots__ = ['channelId','name','dnStart','dnEnd','euStart','euEnd','ertExactStart','ertExactEnd','scetExactStart','scetExactEnd',
                 'sclkExactStart','sclkExactEnd','dn','eu', 'realtime', '_formatted']

    def __init__(self,**kwargs):
        '''Initialize the wait condition

        Args
        -----
        kwargs - A dictionary of attributes to be set on the wait condition when it is created (dictionary)

        Returns
        --------
        None'''

        _log().debug('mtak.wait.ChanValWait')

        AbstractWait.__init__(self,**kwargs)

        self._uniqueList = ['channelId','name']
        self._formatted = False

    def checkType(self,obj):
        '''Check the type of the input object and see if it is
        a type that is accepted by this type of wait condition.

        Args
        -----
        obj - The input object to test the type of (various types)

        Returns
        --------
        True if the  input object is a channel value,
        False otherwise (boolean)'''

        _log().debug('mtak.wait.ChanValWait.checkType()')

        return isinstance(obj,mpcsutil.channel.ChanVal)

    def __setattr__(self,name,value):
        '''setattr(obj,name,value) <==> obj.name = value'''

        _log().debug('mtak.wait.AbstractWait.__setattr__()')

        #Call setattr like normal
        object.__setattr__(self,name,value)

        #Ignore attributes that weren't set by the user
        if name == '_formatted' or name in AbstractWait.__slots__:
            return

        #If the attribute is being set to None,
        #just get rid of it
        if value is None:
            delattr(self,name)
            return

        #If the attribute hasn't been set yet,
        #add it to the set list
        if not name in self._setList and not name in self._uniqueList:
            self._setList.append(name)

    def __delattr__(self,name):
        '''delattr(obj,name) <==> del obj.name'''

        _log().debug('mtak.wait.AbstractWait.__delattr__()')

        #Call delattr like normal
        object.__delattr__(self,name)

        #Ignore the formatted attribute
        if name == '_formatted':
            return

        #If the attribute was set previously,
        #get rid of it now
        if name in self._setList:
            self._setList.remove(name)

    def _checkUniqueConditions(self,obj):

        if hasattr(self,'channelId') and self.channelId != obj.channelId:
                return False
        elif hasattr(self,'name') and self.name != obj.name:
                return False
        return True

    def checkRequiredSettings(self):

        if not hasattr(self,'channelId') and not hasattr(self,'name'):
            raise mtak.err.WaitError('A channel value wait condition must either specify a channel ID or channel name.')

    def checkCondition(self,obj):
        '''Check if the input object solves the current wait condition.

        This method assumes that wait condition attributes follow a naming
        convention where values that correspond have names that end in
        "Start" or "End".  Any attribute that does not have those suffixes
        is assumed to be an equality comparison.

        If the input object solves the wait condition, it will set the sticky flag
        conditionMet to True and will store the object "obj" in the class
        attribute "result".

        This method overrides the superclass method because it has to ensure that
        DN/EU variables get formatted properly before any comparisons are done.

        Args
        -----
        obj - The object to test to see if it solves this wait condition (various types)

        Returns
        --------
        None'''

        _log().debug('mtak.wait.AbstractWait.checkCondition()')

        #If the condition has been met already, return True
        if self.conditionMet:
            return True

        #If this object isn't the correct type, return False
        if not self.checkType(obj):
            return False

        #If there's a name/ID mismatch, return False
        if not self._checkUniqueConditions(obj):
            return False

        #Now we know that the ID of the input test channel value object matches
        #the value we're waiting for.  This means that we can take the type off of
        #input object and use it to format the DN values on this wait condition
        if not self._formatted:

            if 'dnStart' in self._setList:
                self.dnStart = mpcsutil.channel.formatDn(obj.type,self.dnStart)

            if 'dnEnd' in self._setList:
                self.dnEnd = mpcsutil.channel.formatDn(obj.type,self.dnEnd)

            if 'dn' in self._setList:
                self.dn = mpcsutil.channel.formatDn(obj.type,self.dn)

            self._formatted = True

        #Loop through all the attributes that have been
        #set on this wait condition
        for name in self._setList:

            #Get the value set on the wait condition
            targetVal = getattr(self,name)
            testVal = None

            #Is this the bottom end of a range comparison?
            if name.endswith('Start'):

                #See if the input object even has the
                #attribute we're interested in (the attribute
                #on the input object won't have "Start on it")
                try:
                    testVal = getattr(obj,name[0:-5])
                except AttributeError:
                    return False

                #If the input value is smaller, it's not in our range
                if targetVal > testVal:

                    return False

            #Is this the upper end of a range comparison?
            elif name.endswith('End'):

                #See if the input object even has the attribute
                #we're interested in (the attribute on the input object
                #won't have "End" on it)
                try:
                    testVal = getattr(obj,name[0:-3])
                except AttributeError:
                    return False

                #If the input value is bigger, it's not in our range
                if targetVal < testVal:

                    return False

            #It's a direct equality comparison
            else:

                #See if the input object even has the attribute
                #we're interested in
                try:
                    testVal = getattr(obj,name)
                except AttributeError:
                    return False

                #See if the values are equal
                if targetVal != testVal:

                    return False

        #If we didn't return False anywhere else, this object
        #must've met the wait condition...store its details
        #and return True
        self.conditionMet = True
        self.result = obj

        return True

class EvrWait(AbstractWait):
    '''This object creates a wait condition that will wait until
    a particular EVR arrives in the downlink stream

    All of the attributes of this object default to 'None'.  If any of the attributes are
    set, they become part of the condition that is checked.  All attributes set on this object
    are ANDed together to form a single EVR wait condition (e.g. if the 'level' field is set
    to 'Command' and the 'eventId' field was set to 50, then the wait condition would not be
    satisfied until an EVR message is received where the level is equal to 'Command' AND
    the 'eventId' is equal to 50).  To create ORing conditions, a CompoundWait object must be used.

    Attributes
    ----------
    name - The name of this EVR (string)
    eventId - The event ID of this EVR (int)
    level - The level of this EVR (string)
    module - The module for this EVR (string)
    ertStart - Inclusive lower bound for the Earth Receive Time (ISO format) (string)
    ertEnd - Inclusive upper bound for the Earth Receive Time (ISO format) (string)
    scetStart - Inclusive lower bound for the Spacecraft Event Time (ISO format) (string)
    scetEnd - Inclusive upper bound for the Spacecraft Event Time (ISO format) (string)
    sclkStart - Inclusive lower bound for the SCLK (format is CCCCCCCCCC-FFFFF) (string)
    sclkEnd - Inclusive upper bound for the SCLK (format is CCCCCCCCCC-FFFFF) (string)
    message - An exact message to match against (string)
    messageSubstr - A substring of the message to match against (string)
    __slots__ - Special Python variable to restrict which attributes can be set on this object.'''

    __slots__ = ['name','eventId','level','module','ertExactStart','ertExactEnd','scetExactStart','scetExactEnd',
                 'sclkExactStart','sclkExactEnd','message','realtime','messageSubstr']

    def __init__(self,**kwargs):
        ''' Initialize this wait condition.

        If no arguments are given and no attributes are set, then this wait condition will match any
        EVR message that is received.

        Args
        ----
        kwargs - Any keyword arguments that match the names of attributes in this object (allows multiple
        attributes to be set at once through the init method)'''

        _log().debug('mtak.wait.EvrWait()')

        AbstractWait.__init__(self,**kwargs)

        self._uniqueList = ['eventId','name']

    def checkType(self,obj):
        '''Check the type of the input object and see if it is
        a type that is accepted by this type of wait condition.

        Args
        -----
        obj - The input object to test the type of (various types)

        Returns
        --------
        True if the  input object is an EVR,
        False otherwise (boolean)'''

        _log().debug('mtak.wait.EvrWait.checkType()')

        return isinstance(obj,mpcsutil.evr.Evr)

    def checkRequiredSettings(self):

        if not hasattr(self,'eventId') and not hasattr(self,'name'):
            raise mtak.err.WaitError('An EVR wait condition must either specify an event ID or EVR name.')

    def _checkUniqueConditions(self,obj):

        if hasattr(self,'eventId') and self.eventId != obj.eventId:
                return False
        elif hasattr(self,'name') and self.name != obj.name:
                return False
        return True


class ProductWait(AbstractWait):
    '''This object creates a wait condition that will wait until information about
    a particular product arrives in the downlink stream

    All of the attributes of this object default to 'None'.  If any of the attributes are
    set, they become part of the condition that is checked.  All attributes set on this object
    are ANDed together to form a single product wait condition (e.g. if the 'status' field is set
    to 'InProgress' and the 'apid' field was set to 50, then the wait condition would not be
    satisfied until a Product message is received where the status is equal to 'InProgress' AND
    the 'apid' is equal to 50).  To create ORing conditions, a CompoundWait object must be used.

    Object Attributes
    ------------------
    transactionId - The transaction ID unique to this product (string)
    apid - The APID of this product (int)
    status - The current status of the product('NotStarted','InProgress','Complete','Partial') (string)
    dvtScetStart - Inclusive lower bound for the DVT Spacecraft Event Time (ISO format) (string)
    dvtScetEnd - Inclusive upper bound for the DVT Spacecraft Event Time (ISO format) (string)
    dvtSclkStart - Inclusive lower bound for the DVT SCLK (format is CCCCCCCCCC-FFFFF) (string)
    dvtSclkEnd - Inclusive upper bound for the DVT SCLK (format is CCCCCCCCCC-FFFFF) (string)
    __slots__ - Special Python variable to restrict which attributes can be set on this object.'''

    __slots__ = ['name','apid','transactionId','status','dvtScetExactStart','dvtScetExactEnd',
                 'dvtSclkExactStart','dvtSclkExactEnd','ertExactStart','ertExactEnd']

    def __init__(self,**kwargs):
        ''' Initialize this wait condition.

        If no arguments are given and no attributes are set, then this wait condition will match any
        product message that is received.

        Args
        ----
        kwargs - Any keyword arguments that match the names of attributes in this object (allows multiple
        attributes to be set at once through the init method)'''

        _log().debug('mtak.wait.ProductWait()')

        AbstractWait.__init__(self,**kwargs)

        self._uniqueList = ['apid','name']

    def checkType(self,obj):
        '''Check the type of the input object and see if it is
        a type that is accepted by this type of wait condition.

        Args
        -----
        obj - The input object to test the type of (various types)

        Returns
        --------
        True if the  input object is a product,
        False otherwise (boolean)'''

        _log().debug('mtak.wait.ProductWait.checkType()')

        return isinstance(obj,mpcsutil.product.Product)

    def checkRequiredSettings(self):

        if not hasattr(self,'apid') and not hasattr(self,'name'):
            raise mtak.err.WaitError('A product wait condition must either specify an APID or product name.')

    def _checkUniqueConditions(self,obj):

        if hasattr(self,'apid') and self.apid != obj.apid:
                return False
        elif hasattr(self,'name') and self.name != obj.name:
                return False

        return True

class CfdpIndicationWait(AbstractWait):
    '''This object creates a wait condition that will wait until information about
    a particular CFDP Indication arrives in the message bus

    All of the attributes of this object default to 'None'.  If any of the attributes are
    set, they become part of the condition that is checked.  All attributes set on this object
    are ANDed together to form a single CFDP Indication wait condition (e.g. if the 'indicationTypeList'
    field is set to ["tf"], 'sourceEntityId' is set to '1', and the 'transactionSequenceNumber' field
    is set to '50', then the wait condition would not be satisfied until a CFDP Indication message
    of Transaction Finished is received for the specific transaction with the ID '1:50'. To create
    ORing conditions, a CompoundWait object must be used.

    Object Attributes
    ------------------
    indicationTypeList - The CFDP Indication types: "tx" (Transaction), "ft" (Fault), "tf" (Transaction
                     Finished), "ab" (Abandoned), and "txd" (New Transaction Detected) (string)
    sourceEntityId - Source entity ID for the transaction (list of strings)
    transactionSequenceNumber - The transaction sequence number (string)
    __slots__ - Special Python variable to restrict which attributes can be set on this object.'''

    __slots__ = ['indicationTypeList','sourceEntityId','transactionSequenceNumber']

    def __init__(self,**kwargs):
        ''' Initialize this wait condition.

        If no arguments are given and no attributes are set, then this wait condition will match any
        CFDP Indication message that is received.

        Args
        ----
        kwargs - Any keyword arguments that match the names of attributes in this object (allows multiple
        attributes to be set at once through the init method)'''

        _log().debug('mtak.wait.CfdpIndicationWait()')

        AbstractWait.__init__(self,**kwargs)

        self._uniqueList = []

    def checkType(self,obj):
        '''Check the type of the input object and see if it is
        a type that is accepted by this type of wait condition.

        Args
        -----
        obj - The input object to test the type of (various types)

        Returns
        --------
        True if the input object is a CFDP Indication,
        False otherwise (boolean)'''

        _log().debug('mtak.wait.CfdpIndicationWait.checkType()')

        return isinstance(obj,mpcsutil.cfdp.CfdpIndication)

    def checkRequiredSettings(self):
        return True

    def _checkUniqueConditions(self,obj):
        return True

class CompoundWait(AbstractWait):
    '''
    The CompoundWait is a wait condition that can be used to model arbitrary levels of
    complexity in ANDing and ORing together multiple other wait conditions.

    A compound wait consists of a list of wait conditions and a boolean operation used
    to tie those conditions together.  Currently, compound wait conditions support ANDing of all
    internal conditions or ORing of all internal conditions.

    CompoundWait objects may contain other compound wait objects to create arbitrary levels of
    complexity.  A single CompoundWait object may contain multiple different types of wait conditions.

    Attributes
    -----------
    waitList - A list of wait condition objects
    __slots__ - Special Python variable to restrict which attributes can be set on this object.'''

    __slots__ = ['checkCondition','waitList']

    def __init__(self,operator='OR'):
        '''
        Initialize the compound wait condition

        Args
        -----
        operator - Should be the string 'AND' or the string 'OR'.  Specifies if all
        the internal conditions should be ANDed together or ORed together.

        args - Any number of wait conditions to be included in this compound wait condition
        result - A list of all the objects that met this wait condition
        checkCondition - A pointer to which method should be used to check conditions (AND vs. OR)'''

        _log().debug('mtak.wait.CompoundWait()')

        AbstractWait.__init__(self)

        self.result = []

        #Figure out if the internal conditions are ANDed or ORed together
        if operator.upper() == 'AND':
            self.checkCondition = self._checkConditionAnd
        else:
            self.checkCondition = self._checkConditionOr

        #Create the internal wait list as a new list object
        self.waitList = []

    def _checkConditionOr(self,obj):
        '''
        Check if this condition has been met by ORing together all the results
        of the internal wait conditions

        Args
        -----
        obj - The object to test to see if it solves this wait condition

        Returns
        --------
        True if the condition has already been met or if 'obj' caused the condition to be met.  False otherwise.'''

        _log().debug('mtak.wait.CompoundWait._checkConditionOr()')

        #If this condition has already been met, just return true
        if self.conditionMet:

            return True

        self.conditionMet = False

        #Loop through all the wait conditions contained in this
        #compound wait condition
        for condition in self.waitList:

            #We're ORing conditions, so if any of the internal conditions
            #evaluate to True, then this compound condition also evaluates
            #to True (but to make sure we allow all the internal conditions
            #to set themselves, we don't return until looping through all
            #of them)
            if condition.checkCondition(obj) == True:

                self.conditionMet = True

        #if none of the internal conditions were True, this condition
        #has not been met yet
        self._setupResults()

        return self.conditionMet

    def _checkConditionAnd(self,obj):
        '''
        Check if this condition has been met by ANDing together all the results
        of the internal wait conditions

        Args
        -----
        obj - The object to test to see if it solves this wait condition

        Returns
        --------
        True if the condition has already been met or if 'obj' caused the condition to be met.  False otherwise.'''

        _log().debug('mtak.wait.CompoundWait._checkConditionAnd()')

        #If this condition has already been met, just return true
        if self.conditionMet:
            return True

        self.conditionMet = True

        #Loop through all the wait conditions contained in this
        #compound wait condition
        for condition in self.waitList:

            #We're ANDing conditions, so if any of the internal conditions
            #evaluate to False, then this compound conditions also evaluates
            #to False (but to make sure we allow all the internal conditions
            #to set themselves, we don't return until looping through all
            #of them)
            if condition.checkCondition(obj) == False:

                self.conditionMet = False

        #If all of the internal conditions were True, this condition must
        #have been met
        self._setupResults()

        return self.conditionMet

    def _setupResults(self):
        '''Build up the final list of results of all the objects
        that caused this compound wait condition to be met.

        Args
        -----
        None

        Returns
        --------
        None'''

        _log().debug('mtak.wait.CompoundWait._setupResults()')

        self.result = []

        #If for some reason the condition isn't met, return
        if not self.conditionMet:

            return

        #Loop through each condition in the list of
        #wait conditions
        for condition in self.waitList:

            #Grab the results of the child wait condition
            obj = condition.result

            #If the child wait condition is a compound wait,
            #grab all of the objects that solved it
            if isinstance(condition,CompoundWait):

                for val in obj:

                    if not val in self.result:

                        self.result.append(obj)

            #The child was a normal wait condition,
            #grab its result
            else:

                if not obj in self.result and obj:

                    self.result.append(obj)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
