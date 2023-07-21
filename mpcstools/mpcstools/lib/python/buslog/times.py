#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import re
import time

import buslog.err

log1553Fmt = '%Y%m%dT%H%M%S' #e.g. 20090224T165930

log1553Regexp = '^[0-9]{8}[T ]{1}[0-9]{6}$'
log1553RegexpObj = re.compile(log1553Regexp)

def get1553Time(seconds):
    '''Take the input value (in seconds) and convert it to a time string
    formatted in the manner of the 1553 bug log.'''

    global log1553Fmt

    if seconds is None:
        seconds = time.time()

    time_obj = time.gmtime(int(seconds))
    result = time.strftime(log1553Fmt,time_obj)

    return result

def parse1553String(timeString):
    '''Take a string representing a 1553 log time format (e.g. 20090224T165930) and return the number of
    seconds since the epoch that it represents.'''

    timeString = str(timeString).strip()

    timeVal = time.strptime(timeString,log1553Fmt)

    secs = time.mktime(timeVal)

    return secs

def parseTimeString(timeString):
    '''Take a string representing an ISO or a DOY formatted time and return the number of milliseconds since the epoch
    that it represents.'''

    global log1553RegexpObj

    seconds = 0
    try:
        seconds = mpcsutil.timeutil.parseTimeString(timeString)/1000.0
        return seconds
    except mpcsutil.err.MpcsUtilError:
        pass

    if log1553RegexpObj.match(timeString):

        seconds = parse1553String(timeString)
        return seconds

    raise mpcsutil.err.MpcsUtilError('Could not interpret the input time string "{}" as a valid input time'.format(timeString))

def parseSclkString(sclk):
    '''Take a SCLK string representation in either COARSE-FINE format, SECONDS.SUBSECONDS format
    or an integer number of SCLK microseconds and convert it to a
     simple floating point number that we can use for comparisons.'''

    if not mpcsutil.timeutil.sclkRegexpObj.match(sclk):
        raise mpcsutil.err.MpcsUtilError('Could not interpret the input time string "%s" as a valid SCLK time' % (sclk))

    #It's already a floating point format
    index = sclk.find(mpcsutil.timeutil.sclkFractionalSep)
    if index > 0:
        return float(mpcsutil.timeutil.removeLeadingZeros(sclk))

    coarseTicks = 0
    fineTicks = 0

    index = sclk.find(mpcsutil.timeutil.sclkTicksSep)
    if index > 0:
        coarseTicks = mpcsutil.timeutil.removeLeadingZeros(sclk[0:index])
        coarseTicks = float(coarseTicks) if coarseTicks else float(0)
        if coarseTicks >= 2**mpcsutil.timeutil.sclkCoarseSize:
            raise buslog.err.TimeConversionException('The input SCLK time "%s" has a coarse ticks value greater than the max value of "%s"' %
                                          (sclk,2**mpcsutil.timeutil.sclkCoarseSize))
        fineTicks = mpcsutil.timeutil.removeLeadingZeros(sclk[index+1:])
        fineTicks = float(fineTicks) if fineTicks else float(0)
        if fineTicks >= 2**mpcsutil.timeutil.sclkFineSize:
            raise buslog.err.TimeConversionException('The input SCLK time "%s" has a fine ticks value greater than the max value of "%s"' %
                                          (sclk,2**mpcsutil.timeutil.sclkFineSize))
        sclk = coarseTicks + float(fineTicks)/(2**mpcsutil.timeutil.sclkFineSize)
        return sclk

    return float(sclk)/float(10**6)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
