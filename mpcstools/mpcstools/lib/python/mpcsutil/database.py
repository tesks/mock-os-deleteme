#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
# import commands
from mpcsutil import (config, err)

import logging
import os.path
import socket
import subprocess
import sys

import six
if six.PY2:
    import MySQLdb as dbmodule
else:
    import pymysql as dbmodule

_log = lambda : logging.getLogger('mpcs.util')

#The characters that can be used as wildcards for SQL LIKE queries
#(MySQL uses % like the Unix * and _ like the Unix ?)
db_wildcard_chars = ['%','_']

def getMissionDatabaseName(mission=None):
    '''Get the name of the database for the given mission.

    Args
    -----
    mission - The name of the mission whose database is needed (string)

    Returns
    --------
    The full name of the current database for this mission (string)'''

    _log().debug('mpcsutil.config.getMissionDatabaseName()')

    baseDbName = config.GdsConfig().getProperty('database.internal.baseDatabaseName',default='ampcs_v5_0_3')
    mission = mission if mission else config.GdsConfig().getMission()

    return '{}_{}'.format(mission,baseDbName)

def getDatabaseUserName():
    '''Get the username for the database.

    Args
    -----
    None

    Returns
    --------
    The username for the current database (string)'''

    _log().debug('mpcsutil.config.getDatabaseUserName()')

    return config.GdsConfig().getProperty('database.username','mpcs')

def setDatabaseUserName(username):
    '''Set the username for the database.

    Args
    -----
    username - The username to use for connecting to the database. (string)

    Returns
    --------
    None'''

    _log().debug('mpcsutil.config.setDatabaseUserName()')

    config.GdsConfig().setProperty('database.username',username)

def getDatabasePassword():
    '''Get the password for the database.

    Args
    -----
    None

    Returns
    --------
    The password for the current database (string)'''

    _log().debug('mpcsutil.config.getDatabasePassword()')

    return config.GdsConfig().getProperty('database.password',None)

def setDatabasePassword(password):
    '''Set the password for the database.

    Args
    -----
    password - The password to use for connecting to the database. (string)

    Returns
    --------
    None'''

    _log().debug('mpcsutil.config.setDatabasePassword()')

    config.GdsConfig().setProperty('database.password',password)

def getDatabaseHost():
    '''Get the hostname for the database.

    Args
    -----
    None

    Returns
    --------
    The hostname for the current database (string)'''

    _log().debug('mpcsutil.config.getDatabaseHost()')

    return config.GdsConfig().getProperty('database.host',socket.gethostname())

def setDatabaseHost(host):
    '''Set the host for the database.

    Args
    -----
    host - The host to use for connecting to the database. (string)

    Returns
    --------
    None'''

    _log().debug('mpcsutil.config.setDatabaseHost()')

    config.GdsConfig().setProperty('database.host',host)

def getDatabasePort():
    '''Get the port number for the database.

    Args
    -----
    None

    Returns
    --------
    The port number for the current database (string)'''

    _log().debug('mpcsutil.config.getDatabasePort()')

    return int(config.GdsConfig().getProperty('database.port','3306'))

def setDatabasePort(port):
    '''Set the port for the database.

    Args
    -----
    port - The port to use for connecting to the database. (string)

    Returns
    --------
    None'''

    _log().debug('mpcsutil.config.setDatabasePort()')

    config.GdsConfig().setProperty('database.port',port)

def getDatabaseConnection():
    # args = {}
    # host = getDatabaseHost()
    # if host is not None:
    #     args['host'] = host
    #
    # port = getDatabasePort()
    # if port is not None:
    #     args['port'] = port
    #
    # user = getDatabaseUserName()
    # if user is not None:
    #     args['user'] = user
    #
    # passwd = getDatabasePassword()
    # if passwd is not None:
    #     args['passwd'] = passwd
    #
    # socket = getDatabaseSocket()
    # if socket is not None:
    #     args['unix_socket'] = socket

    return dbmodule.connect(**{kk:vv for kk,vv in dict(
        host = getDatabaseHost(),
        port = getDatabasePort(),
        user = getDatabaseUserName(),
        passwd = getDatabasePassword(),
        unix_socket = getDatabaseSocket()).items() if vv is not(None)})

    # return conn

def getDatabaseSocket():
    '''Gets the database socket (unix socket).

    Args
    -----
    None

    Returns
    --------
    The database socket (string)'''

    _command_string="mysql -u mpcs -e \"show variables like 'socket'\""
    proc = subprocess.Popen(_command_string,shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    output, errors = proc.communicate()
    status = proc.returncode
    output = output.decode('utf-8') if isinstance(output, bytes) else output
    errors = errors.decode('utf-8') if isinstance(errors, bytes) else errors

    # If there was a error w/ the mysql command:
    if status != 0:
        error_msg = 'Error in getting database socket: ' + output
        _log().error(error_msg)
        # raise err.MpcsUtilError(error_msg)
        return

    output_list = output.split() # Split on whitespace

    # If there was not enough elements returned by the mysql command:
    if len(output_list) < 4:
        error_msg = 'Error in getting database socket: Not enough elements returned by mysql command'
        _log().error(error_msg)
        # raise err.MpcsUtilError(error_msg)
        return

    socket = output_list[3]

    # Check if socket is a valid file system path:
    if not os.path.exists(socket):
        error_msg = 'Error in getting database socket: %s is not a valid system path' % socket
        _log().error(error_msg)
        # raise err.MpcsUtilError(error_msg)
        return

    return socket

def getConnectionString(show_password=False):
    '''Just an accessor to generate a displayable connection string (excluding password by default)'''

    host = getDatabaseHost()
    port = getDatabasePort()
    user = getDatabaseUserName()
    pw = getDatabasePassword

    result = '%s@%s:%s' % (user,host,port)
    if show_password and pw is not None:
        result = result + (' (password=%s)' % pw)

    return result

def get_session_from_database(session_key,session_host):

    gdsConfig = mpcsutil.config.GdsConfig()

    _sessionQueryApp = gdsConfig.getProperty('automationApp.internal.mtak.app.sessionQuery','chill_get_sessions')
    _sessionQueryScript = '%s/%s' % (mpcsutil.chillBinDirectory,_sessionQueryApp)
    if not os.path.isfile(_sessionQueryScript):
        errString = 'Cannot find the required script ' + _sessionQueryScript
        print>>sys.stderr,errString
        raise mpcsutil.err.EnvironmentError(errString)

    _sessionKeyArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionKey','-K')
    _sessionHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionHost','-O')
    _databaseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseHost','--databaseHost')
    _databasePortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePort','--databasePort')
    _databaseUserArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseUser','--dbUser')
    _databasePasswordArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePassword','--dbPwd')
    # MPCS-6999 09/28/16 - found that this function failed in M20 due to default csv changes. Added template to match expected format
    _outputFormatArg = gdsConfig.getProperty('automationApp.internal.mtak.args.outputFormat', '-o')
    _outputFormatValue = 'csv_mpcsutil'
    _args = [_sessionQueryScript, _sessionKeyArg, session_key, _sessionHostArg, session_host, _databaseHostArg, mpcsutil.database.getDatabaseHost(), _databasePortArg, mpcsutil.database.getDatabasePort(), _outputFormatArg, _outputFormatValue]

    dbUser = getDatabaseUserName()
    if dbUser: _args.extend([_databaseUserArg, dbUser])

    dbPassword = getDatabasePassword()
    if dbPassword: _args.extend([_databasePasswordArg,dbPassword])

    _args.extend(['|','tail', '-n', '1'])

    processString = ' '.join('{}'.format(aa) for aa in _args)

    # processString = '%s %s %s %s %s %s %s %s %d %s %s' % (
    #                                           _sessionQueryScript,
    #                                           _sessionKeyArg,
    #                                           session_key,
    #                                           _sessionHostArg,
    #                                           session_host,
    #                                           _databaseHostArg,
    #                                           mpcsutil.database.getDatabaseHost(),
    #                                           _databasePortArg,
    #                                           int(mpcsutil.database.getDatabasePort()),
    #                                           _outputFormatArg,
    #                                           _outputFormatValue)





    #Use tail to only grab the most recent session
    # processString += ' | tail -n 1'

    proc = subprocess.Popen(processString,shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
    results, errors = proc.communicate()
    results=results.decode('utf-8') if isinstance(results, bytes) else results
    errors=errors.decode('utf-8') if isinstance(errors, bytes) else errors
    status = proc.returncode

    # chill_get tools can return errors, but terminate with a 0 status code, if the command line is erronenous
    if (status != 0) or errors or not results:
        raise mpcsutil.err.EnvironmentError('Could not retrieve session with (key,host) = ({},{}) from the database at {}: {}'.format(session_key,session_host,mpcsutil.database.getConnectionString(),''.join(errors)))

    return mpcsutil.config.SessionConfig.get_from_database_csv(results.split('\n')[0])

def print_available_sessions(n=3):
    '''Display the last N available sessions from the database to the console.'''

    gdsConfig = mpcsutil.config.GdsConfig()

    _sessionQueryApp = gdsConfig.getProperty('automationApp.internal.mtak.app.sessionQuery','chill_get_sessions')
    _sessionQueryScript = '%s/%s' % (mpcsutil.chillBinDirectory,_sessionQueryApp)
    if not os.path.isfile(_sessionQueryScript):
        errString = 'Cannot find the required script {}\n'.format(_sessionQueryScript)
        sys.stderr.write(errString)
        sysstderr.flush()
        raise mpcsutil.err.EnvironmentError(errString)

    _databaseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseHost','--databaseHost')
    _databasePortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePort','--databasePort')
    _databaseUserArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseUser','--dbUser')
    _databasePasswordArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePassword','--dbPwd')
    # MPCS-6999 09/28/16 - found that this function failed in M20 due to default csv changes. Added template to match expected format
    _outputFormatArg = gdsConfig.getProperty('automationApp.internal.mtak.args.outputFormat', '-o')
    _outputFormatValue = 'csv_mpcsutil'

    # processString = '%s %s %s %s %d %s %s' % (_sessionQueryScript,_databaseHostArg,mpcsutil.database.getDatabaseHost(),_databasePortArg,int(mpcsutil.database.getDatabasePort()),_outputFormatArg,_outputFormatValue)

    _args = [_sessionQueryScript,_databaseHostArg,mpcsutil.database.getDatabaseHost(),_databasePortArg,mpcsutil.database.getDatabasePort(),_outputFormatArg,_outputFormatValue]

    dbUser = getDatabaseUserName()
    if dbUser: _args.extend([_databaseUserArg,dbUser])

    dbPassword = getDatabasePassword()
    if dbPassword: _args.extend([_databasePasswordArg,dbPassword])

    #Use tail to only grab the N most recent sessions
    _args.extend(['|','tail','-n',n])

    proc = subprocess.Popen(' '.join('{}'.format(aa) for aa in _args), shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
    out, errors = proc.communicate()
    out = out.decode('utf-8') if isinstance(out, bytes) else out
    errors=errors.decode('utf-8') if isinstance(errors, bytes) else errors
    status = proc.returncode

    # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
    if (status != 0) or errors:
        raise mpcsutil.err.EnvironmentError('Could not retrieve existing sessions from the database at {}: {}'.format(mpcsutil.database.getConnectionString(),''.join(errors)))

    #Little tricks like this make me love Python sometimes
    db_string = 'These are the last {} sessions from database {}'.format(n,mpcsutil.database.getConnectionString())
    print('\n'.join(['{sep}','{content}','{sep}']).format(sep='='*len(db_string), content=db_string))

    results = out[:-1].split('\n')
    for line in results:
        sc = mpcsutil.config.SessionConfig.get_from_database_csv(line)
        print('Session ({},{}) named "{}}" was started at {}'.format(sc.key,sc.host,sc.name,sc.startTime))

def test(*args, **kwargs):
    pass

def main(*args, **kwargs):
    return test(*args, **kwargs)

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
