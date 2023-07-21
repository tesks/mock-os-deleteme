#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module is the Command Preparation and Delivery (CPD) interface.
It is a wrapper for the chill_cmd_directive command line tool,
and it serves as an interface to issue directives to CPD via AMPCS.
"""

from __future__ import (absolute_import, division, print_function)
import collections, json, re, os
import six
import auto
import subprocess
import mpcsutil
import logging
import functools




#The GDS config property that can be used to retrieve the command to launch
#the command directive app
COMMAND_DIRECTIVE_APP_PROPERTY = 'automationApp.internal.auto.uplink.app.commandDirectiveApp'

#Connect to a station or accept a connection from a station
CONNECT_TO_STATION_DIRECTIVE = 'connect_to_station'

#Disconnect from the currently connected station
DISCONNECT_FROM_STATION_DIRECTIVE = 'disconnect_from_station'

#Query the CPD station connection status
QUERY_CONNECTION_STATUS_DIRECTIVE = 'query_connection_status'

#Set the CPD execution state
SET_EXECUTION_STATE_DIRECTIVE = 'set_execution_state'

#Query the CPD configuration parameters
QUERY_CONFIGURATION_DIRECTIVE = 'query_configuration'


class CpdClient(object):
    '''
    CpdClient is the main class in the cpdinterface module. Each instance of
    CpdClient is initialized with various user-specified or default options and
    then multiple consecutive CPD directives may be sent using those options.
    '''

    def __init__(self, cpdHost=None, cpdPort=None, logger=None, userRole=None,
                 keytabFile=None, username=None, logServicePort=None, logServiceHost="localhost", https=False, logFile=None):
        '''
        Initialize the CpdClient object

        Args
        -----
        cpdHost - the host name of the CPD server to communicate with. If not
                    specified, it will use the default uplink host configured
                    on AMPCS.

        cpdPort - the port of the CPD server to communicate with. If not
                    specified, it will use the default uplink port configured
                    on AMPCS.

        userRole - the command security role to issue the directive as

        keytabFile - the location of keytab file to use for authentication

        username - the username associated with the keytab file

        logServicePort - the port on localhost to send log messages to for integrated logging

        logServiceHost - the host name to send log messages to for integrated logging

        https - whether or not to use an HTTPS connection

		logFile - the file to log cmd directives to

        Returns
        --------
        None
        '''

        self.config = mpcsutil.config.GdsConfig()
        self.cpdHost = cpdHost
        self.cpdPort = cpdPort
        self.userRole = userRole
        self.keytabFile = keytabFile
        self.username = username
        self.logServicePort = logServicePort
        self.logServiceHost = logServiceHost
        self.protocol = "https" if https is True else "http"
        self.std_opt = None
        self.loggerAmpcs = logger
        self.logFile = logFile

    def _get_standard_options(self):
        '''
        Get a string that can be used for the AMPCS command directive tool's
        command line options. Options are based on the parameters provided to
        the constructor

        Args
        -----
        None

        Returns
        --------
        A string containing the command line options based on the parameters
        provided to the constructor
        '''
        if self.std_opt is None:
            std_opt = ' --fswUplinkHost %s' % (self.cpdHost) if self.cpdHost else ''
            std_opt += ' --fswUplinkPort %s' % (self.cpdPort) if self.cpdPort else ''
            std_opt += ' --role %s' % (self.userRole) if self.userRole else ''
            std_opt += ' --keytabFile %s' % (self.keytabFile) if self.keytabFile else ''
            std_opt += ' --username %s' % (self.username) if self.username else ''

            #login method is ALWAYS KEYTAB_FILE if a password file is provided.
            #otherwise, do not specify the --loginMethod option
            std_opt += ' --loginMethod KEYTAB_FILE' if self.keytabFile else ''
            std_opt += ' --logServiceUrl "%s://%s:%s/auto/log"' % (self.protocol , self.logServiceHost, (str(self.logServicePort)) if self.logServicePort else '')
            std_opt += ' --logLevelParam level' if self.logServicePort else ''
            std_opt += ' --logMessageParam message' if self.logServicePort else ''
            if self.logFile:
                std_opt += ' --logFile %s ' % str(self.logFile.name)

        return std_opt

    def _get_command_line(self):
        '''
        Get the command line to run the command directive app.  This contains the absolute path to the command line app in addition to standard options

        Returns
        --------
        A string containing the absolute path to the command line app in addition to standard options
        '''
        return '{}{}'.format(os.path.join(mpcsutil.chillBinDirectory, self.config.getProperty(COMMAND_DIRECTIVE_APP_PROPERTY, 'chill_cmd_directive')), self._get_standard_options())
        # chill_gds_bin = mpcsutil.chillBinDirectory
        # command_dir_app = self.config.getProperty(COMMAND_DIRECTIVE_APP_PROPERTY, 'chill_cmd_directive')
        # std_opt = self._get_standard_options()
        # return '%s/%s%s' % (chill_gds_bin, command_dir_app, std_opt)

    def ping(self):
        '''
        Ping the command service.

        Returns
        --------
        True if ping was successful

        Raises
        --------
        An error from auto.err, depending on exact error, if ping failed
        '''
        command_line = '%s --ping' % (self._get_command_line())
        # print('\nSending CPD Ping:\n\t{}\n'.format(command_line))
        command_line_arr = command_line.split(' ')
        proc = subprocess.Popen(command_line_arr, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        #stderr is where logs from the AMPCS command directive tool is coming in from
        (stdout, stderr) = proc.communicate()
        stdout=stdout.decode('utf-8') if isinstance(stdout, bytes) else stdout
        stderr=stderr.decode('utf-8') if isinstance(stderr, bytes) else stderr
        statusCode = proc.returncode
        # print('STDOUT:\n{}\n'.format(stdout))
        # print('STDERR:\n{}\n'.format(stderr))
        # print('STATUS:\n{}\n'.format(statusCode))

        # MPCS-9932 6/26/18: Also write stdout/stderr to log file
        # Note this implementation may cause logs to be out of order in the file
        if self.logFile: self.logFile.write('{}\n{}'.format(stdout, stderr))

        error = auto.err.get_error(statusCode, stdout)

        if error:
            raise error

        return True

    def send_directive(self, directive, **kwargs):
        '''
        Send a directive to CPD via AMPCS' command directive tool

        Args
        -----
        directive - the CPD directive to send
        **kwargs - a list of keyword arguments for the CPD directive.

        Returns
        --------
        A dictionary containing key/value pairs returned by AMPCS' command directive tool

        Raises
        --------
        An error from auto.err, depending on exact error, if ping failed
        '''

        command_line = '%s --directive %s ' % (self._get_command_line(), directive)

        if len(kwargs) > 0:
            args = ','.join(['%s=%s' % (key, value) for key, value in kwargs.items()])
            command_line += '--directiveArgs %s' % (args)

        command_line_arr = command_line.split(' ')

        # print('\nSending CPD Directive:\n\t{}\n'.format(command_line))
        proc = subprocess.Popen(command_line_arr, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        #stderr is where logs from the AMPCS command directive tool is coming in from
        (stdout, stderr) = proc.communicate()
        stdout=stdout.decode('utf-8') if isinstance(stdout, bytes) else stdout
        stderr=stderr.decode('utf-8') if isinstance(stderr, bytes) else stderr
        statusCode = proc.returncode

        # MPCS-9932 6/26/18: Also write stdout/stderr to log file
        # Note this implementation may cause logs to be out of order in the file
        if self.logFile:
            self.logFile.write('{}\n{}'.format(stdout, stderr))

        resp = None
        try:
            for line in stdout.split("\n"):
                if line.find("CPD Response") >= 0:
                    resp = line.split("CPD Response:", 1)[1].strip()
                    break
        except:
            self.loggerAmpcs.debug("Unable to find 'CPD Response' from stdout %s " % stdout)

        error = auto.err.get_error(statusCode, stdout if resp is None else resp)

        if error:
            raise error

        resp_dict = None # added to avoid "UnboundLocalError: local variable 'resp_dict' referenced before assignment"
        if resp is None:
            for line in stdout.split("\n"):
                if line.find("CPD Response") >= 0:
                    resp = line.split("CPD Response:", 1)[1].strip()
                    break

        try:
            resp_dict = dict([kv.split('=') for kv in resp.split(',')])
        except KeyError as e:
            raise auto.err.AmpcsError("AMPCS Command directive tool responded in an unexpected format: %s" % (resp))

        if resp_dict is None:
            raise auto.err.AmpcsError("Unable to parse CPD Response\n %s" % stdout)
        if 'message' in resp_dict:
                message = '%s, %s' % (resp_dict['status'], resp_dict['message'])
        else:
            resp_dict['message'] = resp_dict['status']
            
        return resp_dict

def test(*args, **kwargs):
    pass

def main(*args, **kwargs):
    return test(*args, **kwargs)

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
