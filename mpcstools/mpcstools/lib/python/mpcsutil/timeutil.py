#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines common utility functions related to the
manipulation of time values.
"""

from __future__ import (absolute_import, division, print_function)

from datetime import datetime
import logging
import mpcsutil
import re
import time
import os
import math
import six
long = int if six.PY3 else long


_log = lambda : logging.getLogger('mpcs.util')

def getSleepTime(timeString):
    '''Returns the number of seconds to sleep, so that sleeping occurs up to the supplied timeString.'''

    currentTimeSeconds = time.time()
    futureTimeSeconds = parseTimeString(timeString) / 1000

    sleepTime = futureTimeSeconds - currentTimeSeconds

    return sleepTime

def waitUntilTime(timeString):

    sleepTime = getSleepTime(timeString)

    if sleepTime > 0:
        time.sleep(sleepTime)

def removeTimeStringSubseconds(time_string):
    '''Takes a time string and returns the sub seconds after the decimal place (if any)'''
    index = time_string.find('.')
    if index != -1:
        time_string = time_string[:index]
    return time_string

def getTimeStringNanos(timeString):
    '''Takes a time string and returns the nano seconds as an int (if any)'''
    index = timeString.find('.')
    if index != -1:
        if len(timeString[index + 1: len(timeString)]) > 3:
            return int(timeString[index + 4:]) # Get the nano seconds
    return 0

def parseTimeStringExt(timeString, timetype=None):
    ''' Take a string representing an ISO or DOY formatted time and return the number of nanoseconds since the epoch
    that it represents. '''
    ms = parseTimeString(timeString, timetype)
    nanos = getTimeStringNanos(timeString)
    nanos = int(nanos) / 10**len(str(nanos))
    return ms + nanos

def parseTimeString(timeString, timeType=None):
    '''Take a string representing an ISO or a DOY formatted time and return the number of milliseconds since the epoch
    that it represents.'''

    timeString = str(timeString).strip()
    isIso = False
    isDoy = False
    subseconds = "000"

    if timeString.find(".") > 0:
        subseconds = timeString.split(".")[1]
        if len(subseconds) > 3:
            subseconds = subseconds[:3] # cant calculate nano this method returns ms
        timeString = timeString.split(".")[0].strip() # remove subseconds from time string
    while len(subseconds) < 3:
        subseconds += "0"

    if isoRegexpObj.match(timeString):
        isIso = True
        timeVal = time.strptime(timeString, isoFmt)
    elif doyRegexpObj.match(timeString):
        isDoy = True
        timeVal = time.strptime(timeString, doyFmt)
    elif timeString.isdigit():
        return long(timeString)
    else:
        raise mpcsutil.err.MpcsUtilError('Could not interpret the input time string "%s" as a valid ISO or DOY time' % (timeString))

    return time.mktime(timeVal) * 1000 + int(subseconds)

#Number of milliseconds since the Unix epoch for J2000
j2000Seconds = 946727935.816

#The format of ISO & DOY date/time strings (without subseconds)
isoFmtTz = '%Y-%m-%dT%H:%M:%S %Z' #e.g. 2009-02-24T16:59:30
isoFmt = '%Y-%m-%dT%H:%M:%S' #e.g. 2009-02-24T16:59:30
isoFmtExt = '%Y-%m-%dT%H:%M:%S.%f' #e.g. 2009-02-24T16:59:30
doyFmt = '%Y-%jT%H:%M:%S' #e.g. 2009-055T16:59:30
doyFmtExt = '%Y-%jT%H:%M:%S.%f' #e.g. 2009-055T16:59:30

#The regular expression for an ISO formatted time YYYY-MM-DDTHH:mm:ss.SSSSSS (The 'T' is made optional, it can be replaced with ' ')
isoRegexp ='^[0-9]{4}-[0-9]{2}-[0-9]{2}[T ]{1}[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]{1,}){0,1}$'
isoRegexpObj = re.compile(isoRegexp)

#The regular expression for a DOY formatted time YYYY-DDDTHH:mm:ss.SSSSSS (The 'T' is made optional, it can be replaced with ' ')
doyRegexp = '^[0-9]{4}-[0-9]{3}[T ]{1}[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]{1,}){0,1}$'
doyRegexpObj = re.compile(doyRegexp)

#  5/16/2012 - MPCS-3673 - The regex to get the valid lines from the sclk scet file.
sclkScetRexp =  "(\d*\.\d*\s+\d{4}-\d{3}T\d{2}:\d{2}:\d{2}\.\d{3}\s+\d{2}\.\d{3}\s+\d*\.\d*)"

sclkScetRexpObj = re.compile(sclkScetRexp, re.M)
#Regular expression to detect leading zeros on a string.
leadingZerosRegexp = '^0+'
leadingZerosRegexpObj = re.compile(leadingZerosRegexp)

# Set up defaults for min/max time fields based upon GDS configuration file
gdsConfig = mpcsutil.config.GdsConfig()

useFractionalSclkFormat = mpcsutil.getBooleanFromString(gdsConfig.getProperty('time.timeTags.displayFormat.useFractionalFormat.canonical_sclk'))
useDoyFormat = mpcsutil.getBooleanFromString(gdsConfig.getProperty('time.date.useDoyOutputFormat'))

sclkFractionalSep = gdsConfig.getProperty('time.timeTags.displayFormat.fractionalSeparator.canonical_sclk')
sclkTicksSep = gdsConfig.getProperty('time.timeTags.displayFormat.ticksSeparator.canonical_sclk')
sclkCoarseSize = int(gdsConfig.getProperty('time.timeTags.coarseFineTime.coarseBits.canonical_sclk')) #Bytes
sclkFineSize = int(gdsConfig.getProperty('time.timeTags.coarseFineTime.fineBits.canonical_sclk')) #Bytes
sclkFineModulus = gdsConfig.getProperty('time.timeTags.coarseFineTime.fineModulus.canonical_sclk')
sclkFineModulus = 2**sclkFineSize if sclkFineModulus is None else long(sclkFineModulus)

sclkSize = sclkCoarseSize + sclkFineSize #Bytes
sclkRegexp = '^[0-9]{1,}([' + sclkFractionalSep + sclkTicksSep + '][0-9]{1,}){0,1}$'
sclkRegexpObj = re.compile(sclkRegexp)
dvtFractionalSep = gdsConfig.getProperty('time.timeTags.displayFormat.fractionalSeparator.dvt')
dvtTicksSep = gdsConfig.getProperty('time.timeTags.displayFormat.ticksSeparator.dvt')
dvtCoarseSize = int(gdsConfig.getProperty('time.timeTags.coarseFineTime.coarseBits.dvt')) #Bytes
dvtFineSize = int(gdsConfig.getProperty('time.timeTags.coarseFineTime.fineBits.dvt')) #Bytes
dvtFineModulus = gdsConfig.getProperty('time.timeTags.fineTime.fineModulus.dvt')
dvtFineModulus = 2**dvtFineSize if dvtFineModulus is None else long(dvtFineModulus)
dvtSize = dvtCoarseSize + dvtFineSize #Bytes
dvtRegexp = '^[0-9]{1,}([' + dvtFractionalSep + dvtTicksSep + '][0-9]{1,}){0,1}$'
dvtRegexpObj = re.compile(dvtRegexp)

minScetTime = None
maxScetTime = None
minEventTime = None
maxEventTime = None
minErtTime = None
maxErtTime = None
minSclkTime = None
maxSclkTime = None

#As of Python 2.5.1, these are the upper bound maximum times we
#can represent without overflow
#2038-018T19:14:07
#2038-01-18T19:14:07

if useDoyFormat:
    minScetTime = '1970-001T00:00:00.000'
    maxScetTime = '2038-018T19:14:07'
    minEventTime = '1970-001T00:00:00.000'
    maxEventTime = '2038-018T19:14:07'
    minErtTime = '1970-001T00:00:00.000'
    maxErtTime = '2038-018T19:14:07'
else:
    minScetTime = '1970-01-01T00:00:00.000'
    maxScetTime = '2038-01-18T19:14:07'
    minEventTime = '1970-01-01T00:00:00.000'
    maxEventTime = '2038-01-18T19:14:07'
    minErtTime = '1970-01-01T00:00:00.000'
    maxErtTime = '2038-01-18T19:14:07'

minSclkTime = '0' + sclkTicksSep + '0'
maxSclkTime = str(2 ** sclkCoarseSize) + sclkTicksSep + str(2 ** sclkFineSize)

def _getFormattedTime(ms, format, nanos=None, precision=3):
    '''
    Not for external use. Subject to change without notice.

    Take the input milliseconds and converts it to a formatted time string

    Args
    -----
    ms - milliseconds since epoch
    format - The desired format to convert the timestamp into
    nanos - nanoseconds time value if any
    precision - The subsecond precision to use. Defaults to 3

    Returns
    -----
    A converted timestamp string '''
    timeString = time.strftime(format, time.gmtime(ms/1000))

    try:
        precision=int(precision)
    except TypeError:
        precision = 3
    if precision < 0 or precision > 9:
        precision = 3

    subsecs = '{:03d}'.format(ms % 1000) # last 3 digits are ms
    nanoStr = None
    if nanos:
        try:
            nanoStr = '{:06d}'.format(nanos)
        except ValueError:
            nanoStr = None

    timeObj = time.gmtime(ms / 1000) # Takes seconds as argument, not MS
    if nanoStr and precision > 3:
        subsecs = subsecs + nanoStr # Append nanos
    subsecs = subsecs[ :precision]  # apply precision

    return timeString + ".%s" % subsecs


def getIsoTime(seconds=None, include_subseconds=False):
    '''
    DEPRECATED: Use getIsoTimeExt
    Take the input value (in seconds) and convert it to an
    ISO-formatted time string.  Subseconds are ignored.

    Args
    -----
    seconds - A numeric value of seconds since the 01/01/1970 epoch (long)'''

    global isoFmt

    if seconds is None:

        seconds = time.time()

    full_seconds = int(seconds)

    timeObj = time.gmtime(full_seconds)

    strResult = time.strftime(isoFmt, timeObj)

    if include_subseconds:
        milliseconds = str(int(round((seconds % 1) * 1000)))
        while len(milliseconds) < 3:
            milliseconds += '0'
        strResult = strResult + '.' + milliseconds

    return strResult

def getDoyTime(seconds=None, include_subseconds=False):
    '''
    DEPRECATED: Use getDoyTimeExt
    Take the input value (in seconds) and convert it to an
    DOY-formatted time string.  Subseconds are ignored.

    Args
    -----
    seconds - A numeric value of seconds since the 01/01/1970 epoch (long)'''

    global doyFmt

    if seconds is None:
        seconds = time.time()

    full_seconds = int(seconds)

    timeObj = time.gmtime(full_seconds)

    strResult = time.strftime(doyFmt, timeObj)

    if include_subseconds:
        milliseconds = str(int(round((seconds % 1) * 1000)))
        while len(milliseconds) < 3:
            milliseconds += '0'
        strResult = strResult + '.' + milliseconds

    return strResult

def getTimeString(seconds=None, format=None, include_subseconds=False):
    '''
    DEPRECATED: Use getTimeStringExt
    Take the input value (in seconds) and convert it to a time string.

    If the input format is specified, it will be used to do the conversion, otherwise
    a DOY-formatted or ISO-formatted time string will be returned
    based on the configuration settings.  Subseconds are ignored.

    Args
    -----
    seconds - A numeric value of seconds since the 01/01/1970 epoch (long)'''

    global useDoyFormat
    if seconds is None:

        seconds = time.time()

    if format is not None:

        timeObj = time.gmtime(seconds)
        return time.strftime(format, timeObj)

    else:

        if useDoyFormat:
            return getDoyTime(seconds=seconds, include_subseconds=include_subseconds)
        else:
            return getIsoTime(seconds=seconds, include_subseconds=include_subseconds)



def getIsoTimeExt(ms=None, nanos=None, precision=3):
    '''Take the input value (in milliseconds) and convert it to an
    ISO-formatted time string .

    Args
    -----
    ms - A numeric value of milliseconds since the 01/01/1970 epoch (long)
    nanos - nanoseconds time value if any
    precision - The subsecond precision to use. Defaults to 3
    '''

    global isoFmt

    if ms is None:
        ms = time.time()
    ms = int(ms)

    return _getFormattedTime(ms, isoFmt, nanos=nanos, precision=precision)


def getDoyTimeExt(ms=None, nanos=None, precision=3):
    '''Take the input value (in milliseconds) and convert it to an
    DOY-formatted time string.

    Args
    -----
    ms - A numeric value of milliseconds since the 01/01/1970 epoch (long)
    nanos - nanoseconds time value if any
    precision - The subsecond precision to use. Defaults to 3
    '''

    global doyFmt

    if ms is None:
        ms = time.time()
    ms = int(ms)

    return _getFormattedTime(ms, doyFmt, nanos=nanos, precision=precision)


def getTimeStringExt(format=None, ms=None, nanos=None, precision=3):
    '''Take the input value (in seconds) and convert it to a time string.

    If the input format is specified, it will be used to do the conversion, otherwise
    a DOY-formatted or ISO-formatted time string will be returned
    based on the configuration settings.

    Args
    -----
    ms - A numeric value of seconds since the 01/01/1970 epoch (long)
    format - The time format to use
    nanos - Nanoseconds time value as integer if available
    precision - The subsecond precision to use. Defaults to 3'''

    global useDoyFormat

    if ms is None:
        ms = time.time()
    subsecs = '{:03d}'.format(ms % 1000) # last 3 digits are ms

    nanoStr = None
    if nanos:
        try:
            nanoStr = '{:06d}'.format(nanos)
        except ValueError:
            nanoStr = None

    if format is not None:
        timeObj = time.gmtime(ms / 1000) # Takes seconds as argument, not MS
        if nanoStr and precision > 3:
            subsecs = subsecs + nanoStr # Append nanos
        subsecs = subsecs[ :precision]  # apply precision
        return time.strftime(format, timeObj) + ".%s" % subsecs
    else:
        if useDoyFormat:
            return getDoyTimeExt(ms=ms, nanos=nanos, precision=precision)
        else:
            return getIsoTimeExt(ms=ms, nanos=nanos, precision=precision)



def getSclkString(sclkExact):
    '''Take in a 64-bit SCLK exact representation and convert it to the
    mission-specified string format (generally ticks-subticks or
    seconds.subseconds).'''

    global sclkFineSize

    coarseTicks = long(sclkExact >> sclkFineSize)

    fineTicks = 0
    if sclkFineSize <= 8:
        fineTicks = long(sclkExact & 0x00000000000000ff)
    elif sclkFineSize <= 16:
        fineTicks = long(sclkExact & 0x000000000000ffff)
    elif sclkFineSize <= 24:
        fineTicks = long(sclkExact & 0x0000000000ffffff)
    elif sclkFineSize <= 32:
        fineTicks = long(sclkExact & 0x00000000ffffffff)

    maxDigits = len(str(2 ** sclkFineSize))

    #return the format that the project desires
    if useFractionalSclkFormat:
        sclkFloatingPoint = getSclkFloatingPoint(sclkExact)
        sclkCoarseFine = repr(round(sclkFloatingPoint, 5)).split('.')

        sclkFine = sclkCoarseFine[len(sclkCoarseFine) - 1]

        if len(sclkFine) < maxDigits:
            sclkFine = sclkFine.ljust(maxDigits, '0')

        return '%s%s%s' % (coarseTicks, sclkFractionalSep, sclkFine)
    else:
        #pad fine SCLK to the right number of digits, based on max SCLK fine size
        fineTicks = str(fineTicks).zfill(maxDigits)
        return '%s%s%s' % (coarseTicks, sclkTicksSep, fineTicks)

def getSclkSecondsOnly(sclkExact):
    '''Given a 64-bit SCLK exact representation, return only the
    ticks/seconds portion of the SCLK (ignore subticks/subseconds) as
    a long.'''

    global sclkFineSize

    coarseTicks = long(sclkExact >> sclkFineSize)

    return coarseTicks

def getSclkFloatingPoint(sclkExact):
    '''Given a 64-bit SCLK exact time, convert it to a floating
    point representation of seconds.subseconds and return it as
    as a float.'''

    global sclkFineSize

    coarseTicks = float(sclkExact >> sclkFineSize)

    fineTicks = 0
    if sclkFineSize <= 8:
        fineTicks = float(sclkExact & 0x00000000000000ff)
    elif sclkFineSize <= 16:
        fineTicks = float(sclkExact & 0x000000000000ffff)
    elif sclkFineSize <= 24:
        fineTicks = float(sclkExact & 0x0000000000ffffff)
    elif sclkFineSize <= 32:
        fineTicks = float(sclkExact & 0x00000000ffffffff)

    fineDecimal = fineTicks / (2 ** sclkFineSize)

    return coarseTicks + fineDecimal

def parseCoarseFineClockString(clock, regex, fractionalSep, ticksSep, fineModulus):
    '''Given a string representation of a time as ticks-subticks,
    seconds.subseconds or just ticks, convert it into a 64-bit exact
    representation.

    Args
    ----
    clock - the clock string to parse. Of form <coarse><separator><fine>
    regex - the regex to use to validate the clock string
    fractionalSep - the separator configured to indicate a ticks + fractional ticks format
    ticksSep - the separator configured to indicate a ticks + integer subticks format
    fineModulus - the number of fine ticks needed to make one coarse tick
    '''

    #Make sure it's conforms to the expected format
    if not regex.match(clock):
        raise mpcsutil.err.MpcsUtilError('Could not interpret the input time string %s as a valid coarse-fine time' % (clock))

    coarseTicks = 0
    fineTicks = 0

    #Find the fractional separator in the input string
    index = clock.find(fractionalSep)

    #We found a fractional separator
    if index > 0:
        coarseTicks = removeLeadingZeros(clock[0:index])
        coarseTicks = long(coarseTicks) if coarseTicks else 0
        fineFloat = float(clock[index:])
        fineFloat = fineFloat if fineFloat else 0
        fineTicks = long(round(fineFloat * fineModulus))
    #We didn't find a fractional separator
    else:

        #Find the ticks separator in the input string
        index = clock.find(ticksSep)

        #We found a ticks separator
        if index > 0:
            coarseTicks = removeLeadingZeros(clock[0:index])
            coarseTicks = long(coarseTicks) if coarseTicks else 0
            fineTicks = removeLeadingZeros(clock[index + 1:])
            fineTicks = long(fineTicks) if fineTicks else 0

        #There's no separator, the input string must be only ticks with no subticks
        else:
            coarseTicks = removeLeadingZeros(clock)
            coarseTicks = long(coarseTicks) if coarseTicks else 0
            fineTicks = 0

    exact = (coarseTicks * fineModulus) + fineTicks

    return exact

def parseSclkString(sclk):
    '''Given a string representation of a SCLK as ticks-subticks,
    seconds.subseconds or just ticks, convert it into a 64-bit SCLK exact
    representation.'''

    global sclkRegexpObj, sclkFractionalSep, sclkTicksSep, sclkFineModulus

    return parseCoarseFineClockString(sclk, sclkRegexpObj, sclkFractionalSep, sclkTicksSep, sclkFineModulus)

def parseDvtString(dvt):
    '''Given a string representation of a DVT as ticks-subticks,
    seconds.subseconds or just ticks, convert it into a 64-bit SCLK exact
    representation.'''

    global dvtRegexpObj, dvtFractionalSep, dvtTicksSep, dvtFineModulus

    return parseCoarseFineClockString(dvt, dvtRegexpObj, dvtFractionalSep, dvtTicksSep, dvtFineModulus)

def unixToJ2000(seconds):

    global j2000Seconds

    return seconds - j2000Seconds

def j2000ToUnix(seconds):

    global j2000Seconds

    return seconds + j2000Seconds

def removeLeadingZeros(value):

    global leadingZerosRegexpObj

    return leadingZerosRegexpObj.sub('', value)

def doyToIso(doy):
    # MPCS-10147 1/9/19
    # Python 2.7 does not support nano seconds. Need to strip off
    # nanoseconds before using date time formatting
    dateOnly, timeSuffix = doy.split('T')
    dt = datetime.strptime(dateOnly, "%Y-%j")
    return dt.strftime('%Y-%m-%d') + 'T' + timeSuffix

# 5/16/2012 - MPCS-3673 - Need to create a sclk to LST conversion option for the time util.
# In order to do that needed to create a full set of time conversion methods.

# This will hold the entries for the parsed sclk scet file.
sclkScetContainer = []
conversionFactor = gdsConfig.getConversionFactor()
conversionFactor = float(conversionFactor) if re.match("^\d+\.\d+$", conversionFactor)  else -1

# Use normal day since the times will be converted to mars seconds already.
minuteSeconds = 60
hourSeconds = minuteSeconds * 60
daySeconds = hourSeconds * 24
dayMilliSeconds = daySeconds * 1000

solPrefix = gdsConfig.getLstPrefix()
solPrecisionTag = gdsConfig.getLstPrecision()
scetPrecisionTag = gdsConfig.getScetPrecision()
solPrecision = int(solPrecisionTag) if str(solPrecisionTag).isdigit() else 3
scetPrecision = int(scetPrecisionTag) if str(scetPrecisionTag).isdigit() else 3

timeRegex = "^\d{2}:\d{2}:\d{2}(\.\d{1,6})?$"
timeRegexObj = re.compile(timeRegex)

lstRegex = "^(?i)%s[- ](?P<sol>\d{1,5})[ mM](?P<hours>\d{2}):(?P<minutes>\d{2}):(?P<seconds>\d{2}\.?\d{0,6})$" % solPrefix
lstRegexObj = re.compile(lstRegex)

timeZero = "00:00:00"


def sclkScetEntryWithScet(scet_ms):
    '''Pass in a scet value and get the sclk scet entry object for this time.  Will be the entry just before the scet value.
    Will return None if the scet value is before the sclk 0 scet time and when the parsed correlation file returns no
    entries.  If scet is before the first entry and after sclk 0 scet time, will return the first entry from the file.

    Args
    -----
     - scet_ms: milleseconds since Unix epoch as a numeric value (float, long, int).

     return: sclk-scet entry
    '''
    global sclkScetContainer

    if not isinstance(scet_ms, float) and not isinstance(scet_ms, int) and not isinstance(scet_ms, long):
        raise mpcsutil.err.MpcsUtilError("scet_ms value must be a float or int to find sclk scet entry." )

    if not sclkScetContainer:
        parseSclkScetFile()

    # If scet is before the sclk 0 time, return None.  Check against j2000Seconds.
    if scet_ms/1000 < j2000Seconds:
        return None

    # If the result is None, means it is before the last entry in the table.  Return the last entry.
    result = sclkScetContainer[0] if sclkScetContainer else None

    for container in reversed(sclkScetContainer):
        if scet_ms >= container.scet_msecs:
            result = container
            break

    return result

def sclkScetEntryWithSclk(sclk):
    '''Pass in a sclk value and get the sclk scet entry object for this time.  Will be the entry just before the sclk value.
    The sclks stored will be J2000 sclk times.

    Args
    -----
    sclk - sclk time of the time to be converted.  Should be a J2000 numeric value (float, long, int)
    '''
    global sclkScetContainer

    if not isinstance(sclk, float) and not isinstance(sclk, int) and not isinstance(sclk, long):
        raise mpcsutil.err.MpcsUtilError("sclk value must be a numeric value to find sclk scet entry." )

    if not sclkScetContainer:
        parseSclkScetFile()

    # Check if the sclk value is less than 0.  If so, just return None.
    if sclk < 0:
        return None

    # Check if sclk time is before the first entry, but after sclk 0, return the first entry.
    result = sclkScetContainer[0] if sclkScetContainer else None

    for container in reversed(sclkScetContainer):
        if sclk >= container.sclk:
            result = container
            break

    return result

class SclkScetValueContainer:
    '''
    A container class to hold the values parsed from the sclk scet file. Will convert the times to all be
    millisecond floats.  So all time values will be ms.
    '''
    def __init__(self, sclk, scet, dut, sclkrate, is_last=False, is_first=True):
        self.sclk = float(sclk)
        self.dut = float(dut)
        self.sclk_rate = float(sclkrate)
        self.scet = scet
        self.scet_msecs = parseTimeString(scet)
        self.is_last = is_last
        self.is_first = is_first

    def __str__(self):
        return " ".join(["SCLK: ", str(self.sclk),
                "SCET: ", str(self.scet),
                "DUT : ", str(self.dut),
                "RATE: ", str(self.sclk_rate),
                "FIRST: ", str(self.is_first),
                "LAST: ", str(self.is_last)])

def scetToSclk(scet):
    '''Converts scet to sclk.

    Args
    -----
    scet - Can be either of the following:
           1. A numeric value of seconds since the 01/01/1970 epoch (int or long)
           2. Floating point representing seconds and subseconds from epoch
           3. SCET DOY or ISO formatted time string

    returns: sclk
    '''
    global sclkScetContainer

    if isinstance(scet, str):
        scet_ms = parseTimeString(scet)
    elif isinstance(scet, int) or isinstance(scet, long):
        scet_ms = float(scet) * 1000
    elif not isinstance(scet, float):
        raise mpcsutil.err.MpcsUtilError("Invalid input to convert scet to sclk: (%s)" % str(scet))
    else:
        scet_ms = scet * 1000

    sclk_scet_entry =  sclkScetEntryWithScet(scet_ms)

    # Check conditions to see what actions to take for scet -> sclk conversion.
    if sclk_scet_entry is None:
        raise mpcsutil.err.MpcsUtilError("Scet value is before epoch: %s" % scet)

    # Break these out to make it easier to read.
    scet0_ms = sclk_scet_entry.scet_msecs
    sclk0_ms = sclk_scet_entry.sclk * 1000

    if sclk_scet_entry.is_first and scet0_ms > scet_ms:
        scet_diff = scet_ms - scet0_ms
        add_time = -1 * scet_diff / sclk_scet_entry.sclk_rate

    elif sclk_scet_entry.is_last:
        scet_diff = scet_ms - scet0_ms
        add_time = scet_diff / sclk_scet_entry.sclk_rate
    else:
        # Just in case
        try:
            next_entry = sclkScetContainer[sclkScetContainer.index(sclk_scet_entry) + 1]
        except ValueError:
            raise mpcsutil.err.MpcsUtilError("Could not find next entry in list")

        # Breaking it into pieces to that it can be easily debugged if need be.
        scet1_ms = next_entry.scet_msecs
        sclk1_ms = next_entry.sclk * 1000

        # SCET1 - SCET0
        scet_bin_width = scet1_ms - scet0_ms
        # (SCET - SCET0) / (SCET1 - SCET0)
        scet_diff = scet_ms - scet0_ms
        # SCLK1 - SCLK0
        sclk_bin_width = sclk1_ms - sclk0_ms
        # scet_diff / scet_bin_width
        rate = scet_diff / scet_bin_width

        # SCLK = ((SCET-SCET0)/(SCET1-SCET0)) * (SCLK1-SCLK0)
        add_time = sclk_bin_width * rate

    return (sclk0_ms + add_time) / 1000

def sclkToScet(sclk):
    '''Converts sclk to scet.

    Args
    -----
    sclk - numeric sclk value as a J2000 time.

    returns: scet as seconds from Unix epoch.
    '''
    global sclkScetContainer

    sclk_scet_entry =  sclkScetEntryWithSclk(sclk)

    if sclk_scet_entry is None:
        raise mpcsutil.err.MpcsUtilError("Could not find entry in sclk scet correlation file for sclk: %d" % sclk)

    # All times in the containers are in ms.  So, get everything to ms and unix time.
    sclk_ms = j2000ToUnix(sclk) * 1000
    # Convert sclk0 to unix time and ms..
    sclk0_ms = j2000ToUnix(sclk_scet_entry.sclk) * 1000
    # Just to be consistent, set all the times from the entry here.
    scet0_ms = sclk_scet_entry.scet_msecs
    # Check conditions to see what actions to take for scet -> sclk conversion.

    if sclk_scet_entry.is_first and sclk_ms < sclk0_ms:
        # This is the same as if it was after the entry, but you would subtract this instead.
        # SCLK - SCLK0
        sclk_diff = sclk_ms - sclk0_ms
        # (SCLK - SCLK0) * SCLK_RATE
        add_time = -1 * sclk_diff * sclk_scet_entry.sclk_rate
    elif sclk_scet_entry.is_last:
        # SCLK - SCLK0
        sclk_diff = sclk_ms - sclk0_ms
        # (SCLK - SCLK0) * SCLK_RATE
        add_time = sclk_diff * sclk_scet_entry.sclk_rate
    else:
        # This means it is between two entries.  Get the next entry for the calculation.
        try:
            next_entry = sclkScetContainer[sclkScetContainer.index(sclk_scet_entry) + 1]
        except ValueError:
            raise mpcsutil.err.MpcsUtilError("Could not find next entry in list")

        sclk1_ms = j2000ToUnix(next_entry.sclk) * 1000
        scet1_ms = next_entry.scet_msecs

        rate = (sclk_ms - sclk0_ms) / (sclk1_ms - sclk0_ms)
        diff = scet1_ms - scet0_ms
        add_time = diff * rate

    return (scet0_ms + add_time) / 1000

def sclkToScetStr(sclk, use_doy=True):
    '''Converts sclk to formated scet string.

    Args
    -----
    sclk - numeric sclk value as a J2000 time.
    use_doy - If true formats returned time as DOY otherwise uses ISO format.  Defaults to true.

    returns: formated scet time string.
    '''
    return getTimeString(sclkToScet(sclk), include_subseconds=True)

def scetToLst(scet):
    '''Converts scet to LST string.

    Args
    -----
    scet - Can be either of the following:
           1. A numeric value of seconds since the 01/01/1970 epoch (int or long)
           2. Floating point representing seconds and subseconds from epoch
           3. SCET DOY or ISO formatted time string

    returns:  LST string.

    '''
    global conversionFactor, dayMilliSeconds, solPrefix, scetZeroMS, solPrecision

    # Convert the time if needed.
    scet_str = str(scet)
    if re.match(doyRegexpObj, scet_str) or re.match(isoRegexpObj, scet_str):
        scet_ms = parseTimeString(scet)
    elif not isinstance(scet, float) and not isinstance(scet, int) and not isinstance(scet, long):
        raise mpcsutil.err.MpcsUtilError("Could not convert scet time to LST, format unknown: %s" % str(scet))
    else:
        scet_ms = float(scet) * 1000

    if scetZeroMS < 0:
        raise mpcsutil.err.MpcsUtilError("scet0 could not be retrieved from the Gds configuration")
    elif conversionFactor < 0:
        raise mpcsutil.err.MpcsUtilError("Could not get the earth seconds conversion factor from the Gds configuration")
    elif dayMilliSeconds < 0:
        raise mpcsutil.err.MpcsUtilError("Could not get the mean Solar day length from the Gds configuration")

    # SCET - SCET0
    earth_lst_ms = scet_ms - scetZeroMS
    lst_ms = earth_lst_ms * conversionFactor

    sol = math.floor(lst_ms/dayMilliSeconds)
    lst_no_sol = math.floor(lst_ms - sol*dayMilliSeconds)

    hms = getHMS(seconds=lst_no_sol/1000, precision=solPrecision, include_subseconds=True)

    # If the sol number is negative, means sclk is before sclk0 time.  return the dummy lst.
    if sol < 0:
        lst = "%s-0000M00:00:00.%s" % (solPrefix, "0" * solPrecision)
    else:
        lst = "%s-%04dM%s" % (solPrefix, sol, hms)

    return lst

def sclkToLst(sclk):
    '''Converts sclk to LST string.

    Args
    -----
    sclk - numeric sclk value as a J2000 time.

    returns:  LST string.

    '''
    return scetToLst(sclkToScet(sclk))

def lstToSclk(lst):
    '''Converts LST string to sclk.

    Args
    -----
    lst - Properly formated LST string.

    returns: numeric sclk value as a J2000 time.

    '''
    return scetToSclk(lstToScet(lst))

def lstToScet(lst):
    '''Converts LST string to scet.

    Args
    -----
    lst - Properly formated LST string.

    returns: scet as float number of seconds and subseconds since Unix epoch.
    '''
    global conversionFactor, dayMilliSeconds, scetZeroMS, minuteSeconds, hourSeconds, dayMilliSeconds

    lst_s = re.search(lstRegexObj, lst)
    if not lst_s:
        raise mpcsutil.err.MpcsUtilError("Invalid lst format: %s" % lst)

    # Convert to number of ms.  This will be in Mars seconds.  Do seperate to keep track and for readability.
    mars_seconds = float(lst_s.group("hours"))*hourSeconds + float(lst_s.group("minutes"))*minuteSeconds + float(lst_s.group("seconds"))
    mars_ms = dayMilliSeconds*float(lst_s.group("sol")) + (mars_seconds * 1000)
    earth_seconds = mars_ms / conversionFactor

    # Now, add the earch_seconds to the scet0 and boom goes the dynamite!
    scet = scetZeroMS + earth_seconds

    return scet / 1000

def lstToScetStr(lst, use_doy=True):
    '''Converts LST string to sclk.

    Args
    -----
    lst - Properly formated LST string.
    use_doy - If true formats returned time as DOY otherwise uses ISO format.  Defaults to true.

    returns: formated scet time string.
    '''
    target = getDoyTime if use_doy else getIsoTime
    return target(lstToScet(lst), True)

def getHMS(seconds, precision=3, include_subseconds=True):
    '''Give a number of seconds and returns H:M:S.%f.  This will not take into account
    any values over hours.

    -------------
    seconds -- Number of seconds to convert to H:M:S.f
    precision -- Number of decimal places subseconds
    '''
    # Have to keep track of subseconds
    ms, sec = math.modf(seconds)
    # Do the round here with precision.  If the value is > 1, add 1 to the deal and set to 0.
    ms = round(ms, precision)

    if ms >= 1:
        sec += 1
        ms_str = "0.000"
    else:
        ms_str = str(ms)

    t = time.gmtime(sec)

    subs = ""

    if include_subseconds:
        subs = ms_str[ms_str.index("."):]
        # Make sure it is 0 filled.
        subs += "0" * (precision+1 - len(subs))

    return "%02d:%02d:%02d%s" % (t.tm_hour, t.tm_min, t.tm_sec, subs)

def parseSclkScetFile(sclk_scet_file=None):
    '''Parse the sclkscet file and return an object container.'''
    global sclkScetContainer

    if sclk_scet_file is None:
        cgds = os.environ["CHILL_GDS"]
        mission = gdsConfig.getMission()
        scid = gdsConfig.getProperty("mission.spacecraft.ids.default")

        sclk_scet_file = "%s/config/%s/sclkscet.%s" % (cgds, mission, scid)


    if not os.path.isfile(sclk_scet_file):
        raise mpcsutil.err.MpcsUtilError("sclkscet file %s does not exist" % sclk_scet_file)

    sclkScetContainer = []

    # Also set the is_last and is_first entry.  This lets the entry instance know if it is is the first or last
    # entry from the file.  Helps with processing later down the line.
    sclk_scet_entries = re.findall(sclkScetRexpObj, open(sclk_scet_file).read())

    # Keep track of the indicies so doing this way.
    for index in range(0, len(sclk_scet_entries)):
        sclk, scet, dut, sclkrate = re.split("\s+", sclk_scet_entries[index])
        sclkScetContainer.append(SclkScetValueContainer(sclk, scet, dut, sclkrate, is_first=index == 0, is_last=index == len(sclk_scet_entries)-1))

def getLeapSeconds(sclk):
    '''Return the leap seconds to be added to a sclk time.  Sclk must should be a float, integer or string will all digits, including
    decimal point.
    '''
    try:
        sclkFloat = float(sclk)
    except Exception:
        raise ValueError("This is a place holder error message:\n{}".format(traceback.format_exc()))

    dutSclk0 = getDut(0.0)
    dutSclk = getDut(sclk)

    return dutSclk - dutSclk0

def getDut(sclk):
    '''Pass in a sclk value and get the dut value from the sclk scet file that corresponds to sclk.  Will parse the
    sclk scet file if it has not been parsed yet.

    Args
    -----
    sclk - sclk time of the time to be converted.  Should be float.
    '''
    result = sclkScetEntryWithSclk(sclk)

    if result is None:
        # Should I error?  Or warn?
        raise mpcsutil.err.MpcsUtilError("Could not find sclk scet entry for sclk:%d" % sclk)
    else:
        return result.dut

def getScetPrecision():
    return int(gdsConfig.getScetPrecision())

def getErtPrecision():
    return int(gdsConfig.getErtPrecision())

# Have this at the bottom so that everything would be loaded before this conversion.
scetZeroString = gdsConfig.getEpochScet()
scetZeroMS = parseTimeString(scetZeroString)

def test(*args, **kwargs):
    pass

def main(*args, **kwargs):
    return test(*args, **kwargs)

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
