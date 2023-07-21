#! /usr/bin/env python
# -*- coding: utf-8 -*-

class Log1553Exception(Exception):

    def __init__(self, args=None):
        '''constructor

            args = the string description of the exception (string)'''
        super(Log1553Exception, self).__init__(args)

class LogParseException(Log1553Exception): pass
class EndOfTimeRange(Exception): pass
class TimeConversionException(Log1553Exception): pass

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
