#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module is responsible for connecting to the MySQL database, using user input parameters to filter output, and
dump all the corresponding database contents to MySQL importable .SQL files.

MPCS-5244 06/06/14 Add --describe and --testKey throughout. Remove --fromKey and --toKey.
MPCS-8384 12/05/16 Add --extendedSCET
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import optparse
import os
import re
import subprocess
import socket
import sys
import tempfile
import encryption
from datetime import datetime

def createFiles(dbName,outputDir,sessionHost=None,startTimeLowerBound=None,startTimeUpperBound=None,dbUser=None,dbPassword=None,dbHost=None,
                dbPort=None,dumpOpts=None,dumpTables=None,describe=None,extendedSCET=None,testKey=None):
    '''Find all the sessions in the database corresponding to the input information and write their SQL
    dump files to the specified output directory 'outputDir'

    If no time values are specified, it will default to any session that was created in the last 24 hours

    Parameters
    -----------
    dbName - The actual name of the database to dump (e.g. '<mission>_gdsdb_v3_1') (string)
    outputDir - The directory where the dump files should be placed (e.g. '/tmp') (string)
    sessionHost - The value of the 'host' field in the session table...identify all the sessions to be dumped by host.
                  Sessions can only be dumped from one host at a time (string)
    startTimeLowerBound - An ISO-formatted time that is the lower bound on the start of session time for session to dump.
                          The expected format is 'YYYY-MM-DDTHH:MM:SS'. (string)
    startTimeUpperBound - An ISO-formatted time that is the upper bound on the start of session time for session to dump.
                          The expected format is 'YYYY-MM-DDTHH:MM:SS'. (string)
    dbUser - The username to use to login to the database (string)
    dbPassword - The password to use to login to the database (string)
    dbHost - The hostname of the machine of the database to connect to (string)
    dbPort - The port number of the database to connect to (int)
    dumpOpts - Additional operations to MySqlDump (string)
    dumpTables - Tables to dump (default all except Host, Session, and EndSession)
    describe - If true, just print out what will happen, don't do it
    extendedSCET - Use extended tables (with "2" suffix)
    testKey - Array of 4-tuples with sessions and fragments

    Returns
    --------
    0 if operation succeeded
    -1 if no matching sessions found
    other if an error occurred'''

    # MPCS-11855 - dump data without session IDs
    dumpContextTables = "ContextConfig ContextConfigKeyValue"
    dumpNoSessionTables = "CfdpFileGeneration CfdpFileUplinkFinished CfdpIndication CfdpPduReceived CfdpPduSent CfdpRequestReceived CfdpRequestResult"

    #Dump out the tables to SQL executable files (the session table has its own separate executable file)
    #(All the command line constructed below is documented in the manual for the 'mysqldump' script)
    if dbHost is None:
        dbHost = mpcsutil.database.getDatabaseHost()
    if dbPort is None:
        dbPort = mpcsutil.database.getDatabasePort()
    if dbUser is None:
        dbUser = mpcsutil.database.getDatabaseUserName()

    # MPCS-8384 12/05/16 Add protocol to make it use the port instead of the linux domain socket
    mysqlLoginString = '--user %s --host %s --port %s --protocol=TCP' % (dbUser,dbHost,dbPort)

    if dbPassword is not None:
        mysqlLoginString += (' -p\'%s\'' % (dbPassword))
    else:
        configPw = mpcsutil.database.getDatabasePassword()
        decryptedPw = encryption.triple_des_decrypt_from_base64_str(configPw)
        if decryptedPw is not None and decryptedPw != b'':
            mysqlLoginString += (' -p\'%s\'' % (decryptedPw.decode("utf-8")))

    whereClauseString = constructWhereClause(sessionHost,startTimeLowerBound, startTimeUpperBound, testKey)

    print('')
    print('Scanning for session data on database %s with conditions %s' % (dbName,whereClauseString))
    print('')

    # MPCS-5244 06/04/14 Add sessionFragment

    findSessionCmd = 'mysql %s -e "SELECT sessionId,hostId,host,sessionFragment from %s.Session WHERE %s\"' % (mysqlLoginString,dbName,whereClauseString)

    if describe:
        print("Find session command: " + findSessionCmd)

    sessionQueryFile = open(outputDir + '/' + dbName + '_session_query.out','w')
    sessionQuery = subprocess.Popen(findSessionCmd,shell=True,stdout=sessionQueryFile)
    sessionQuery.wait()
    sessionQueryFile.close()

    _session_query_out=os.path.join(outputDir, '{}_session_query.out'.format(dbName))
    results = readQueryResults(_session_query_out)

    sessionDumpCmd = 'mysqldump %s %s --extended-insert --hex-blob --no-create-db --no-create-info ' % (mysqlLoginString,dumpOpts)

    dataDumpCmd = str(sessionDumpCmd)
    dataContextDumpCmd = str(sessionDumpCmd)
    dataNoSessionDumpCmd = str(sessionDumpCmd)

    returnVal = 0

    if results:

        print('')
        print('Sessions with the following keys will be dumped (host/hostId key/fragment):')

        for i in enumerate(results):
            result = i[1]
            key = str(result[0])
            hostId = str(result[1])
            host = str(result[2])
            fragment = str(result[3])

            print ("%s/%s %s/%s" % (host, hostId, key, fragment))

        print('')

        sessionDumpCmd += '--where "'
        dataDumpCmd += '--where "'
        dataContextDumpCmd += '--where "(contextId IS NOT NULL)" '
        dataNoSessionDumpCmd += '--where "(sessionId IS NULL)" '

        hostDumpFile = open(outputDir + '/' + dbName + '_host_dump.sql','w')
        hostDumpFile.write('SET @hostOffset=(SELECT DISTINCT(hostOffset) FROM Host);\n')

        for i in enumerate(results):
            if i[0] != 0:
                sessionDumpCmd += " OR "
                dataDumpCmd += " OR "
            result = i[1]
            key = str(result[0])
            hostId = str(result[1])
            host = str(result[2])
            fragment = str(result[3])

            select = ("((sessionId=%s) AND (hostId=%s) AND (sessionFragment=%s))" % (key,hostId,fragment))

            sessionDumpCmd += select
            dataDumpCmd    += select

            hostDumpStr = "INSERT IGNORE INTO Host SET hostId=" + hostId + ",hostName='" + host +"',hostOffset=@hostOffset;"
            hostDumpFile.write(hostDumpStr + '\n')

        sessionDumpCmd += '" '
        dataDumpCmd += '" '

        sessionDumpCmd += (dbName + ' Session EndSession')
        dataDumpCmd += (dbName + ' ' + dumpTables)
        dataContextDumpCmd += (dbName + ' ' + dumpContextTables)
        dataNoSessionDumpCmd += (dbName + ' ' + dumpNoSessionTables)

        sessionDumpFile = open(outputDir + '/' + dbName + '_session_dump.sql','w')
        dataDumpFile = open(outputDir + '/' + dbName + '_tables_dump.sql','w')
        sessionErrFile = open(outputDir + '/' + dbName + '_session_dump.err','w')
        sessionErrFile.truncate(0);
        dataErrFile = open(outputDir + '/' + dbName + '_tables_dump.err','w')
        dataErrFile.truncate(0);

        print('Will be dumping tables: ' + dumpTables)

        if describe:
            print('')
            print('Sessions: ' + sessionDumpCmd)
            print('')
            print('Tables: ' + dataDumpCmd)
            print('')
            return -1

        sessionReturnCode = subprocess.Popen(sessionDumpCmd,shell=True,stdout=sessionDumpFile,stderr=sessionErrFile).wait()

        tablesReturnCode = subprocess.Popen(dataDumpCmd,shell=True,stdout=dataDumpFile,stderr=dataErrFile).wait()

        if sessionReturnCode != 0:
            print('Error dumping session data; mysqldump return code was ' + str(sessionReturnCode))
            print('Session dump error log written to ' + outputDir + '/' + dbName + '_session_dump.err')
            sessionDumpFile.truncate(0)
            dataDumpFile.truncate(0)
            hostDumpFile.truncate(0)
            returnVal = sessionReturnCode
        else:
            print('Session configuration output written to ' + outputDir + '/' + dbName + '_session_dump.sql')

        if tablesReturnCode != 0:
            print('Error dumping table data; mysqldump return code was ' + str(tablesReturnCode))
            print('Table dump error log written to ' + outputDir + '/' + dbName + '_tables_dump.err')
            dataDumpFile.truncate(0);
            sessionDumpFile.truncate(0);
            hostDumpFile.truncate(0)
            returnVal = tablesReturnCode
        else:
            print('Table table data written to ' + outputDir + '/' + dbName + '_tables_dump.sql')

        print('Host table data written to ' + outputDir + '/' + dbName + '_host_dump.sql')

        print('Will be dumping tables with no session data: ' + dumpContextTables + dumpNoSessionTables)

        # add context data to the same table file
        contextReturnCode = subprocess.Popen(dataContextDumpCmd,shell=True,stdout=dataDumpFile,stderr=dataErrFile).wait()
        if contextReturnCode != 0:
            print('Error dumping table data; mysqldump return code was ' + str(contextReturnCode))
            print('Table dump error log written to ' + outputDir + '/' + dbName + '_tables_dump.err')
            returnVal = contextReturnCode
        else:
            print('Table data (context) added to ' + outputDir + '/' + dbName + '_tables_dump.sql')

        # add data with no session ID to the same table file
        noSessionReturnCode = subprocess.Popen(dataNoSessionDumpCmd,shell=True,stdout=dataDumpFile,stderr=dataErrFile).wait()
        if noSessionReturnCode != 0:
            print('Error dumping table data; mysqldump return code was ' + str(noSessionReturnCode))
            print('Table dump error log written to ' + outputDir + '/' + dbName + '_tables_dump.err')
            returnVal = noSessionReturnCode
        else:
            print('Table data (no session) added to ' + outputDir + '/' + dbName + '_tables_dump.sql')

        sessionDumpFile.close()
        dataDumpFile.close()
        hostDumpFile.close()
        sessionErrFile.close()
        dataErrFile.close()

    else:
        returnVal = -1
        print('No matching sessions were found')

    return returnVal

def constructWhereClause(sessionHost=None,startTimeLowerBound=None,startTimeUpperBound=None,testKey=None):
    '''Build the SQL 'WHERE' clause to use for querying sessions out of the database

    Parameters
    -----------
    sessionHost - The value of the 'host' field in the session table...identify all the sessions to be dumped by host.
                  Sessions can only be dumped from one host at a time (string)
    startTimeLowerBound - An ISO-formatted time that is the lower bound on the start of session time for session to dump.
                          The expected format is 'YYYY-MM-DDTHH:MM:SS'.  If this is not specified and no upper bound is,
                          specified, then this parameter will be defaulted to 24 hours prior to the time this script is run. (string)
    startTimeUpperBound - An ISO-formatted time that is the upper bound on the start of session time for session to dump.
                          The expected format is 'YYYY-MM-DDTHH:MM:SS'. If this is not specified, it will default
                          to the time at which this script is run. (string)
    testKey - 4-tuples of sessions and fragments

    Returns
    --------
    None

    Raises
    -------
    ValueError - If the start time lower bound given is greater than the start time upper bound given'''

    whereClauseString = ''

    yesterday = '(now() - interval 1 day)'
    today = 'now()'

    #If no start time and no session key range is specified, default to the last 24 hours
    if testKey is None and startTimeLowerBound is None and startTimeUpperBound is None:
        startTimeLowerBound = yesterday
        startTimeUpperBound = today

    #Remove the 'T' from the ISO time format if there is one
    if startTimeLowerBound is not None:
        if startTimeLowerBound != yesterday:
            startTimeLowerBound = '\'' + startTimeLowerBound.replace('T',' ',1) + '\''
        whereClauseString = (' startTime >= ' + startTimeLowerBound + ' ')

    #If an end-time is specified, then remove the 'T' as above and check to see the end-time is > start-time.
    if startTimeUpperBound is not None:
        if startTimeUpperBound != today:
            startTimeUpperBound = '\'' + startTimeUpperBound.replace('T',' ',1) + '\''
        if whereClauseString:
            whereClauseString += 'AND'
        whereClauseString += (' startTime <= ' + startTimeUpperBound + ' ')

    if sessionHost is not None:
        if whereClauseString:
            whereClauseString += 'AND'
        whereClauseString += (' host LIKE \'' + sessionHost + '\' ')

    if testKey is not None:
        if whereClauseString:
            whereClauseString += ' AND '
        else:
            whereClauseString += ' '

        firstTime = True

        if len(testKey) > 1:
            whereClauseString += '('

        for key in testKey:
            sessionFirst = key[0]
            sessionLast = key[1]
            first = key[2]
            last = key[3]

            if firstTime:
                firstTime = False
            else:
                whereClauseString += ' OR '

            if last != None:

                if first != last:
                    whereClauseString += ('((sessionId=' + str(sessionFirst) + ') AND (sessionFragment BETWEEN ' +
                                          str(first) + ' AND ' + str(last) + '))')
                else:
                    whereClauseString += ('((sessionId=' + str(sessionFirst) + ') AND (sessionFragment=' + str(last) + '))')

            elif first != None:
                whereClauseString += ('((sessionId=' + str(sessionFirst) + ') AND (sessionFragment>=' + str(first) + '))')

            elif sessionLast == None:
                whereClauseString += ('(sessionId>=' + str(sessionFirst) + ')')

            elif sessionFirst != sessionLast:
                whereClauseString += ('(sessionId BETWEEN ' + str(sessionFirst) + ' AND ' + str(sessionLast) + ')')

            else:
                whereClauseString += ('(sessionId=' + str(sessionFirst) + ')')

        if len(testKey) > 1:
            whereClauseString += ')'

    if startTimeLowerBound is not None and startTimeUpperBound is not None:
        if startTimeUpperBound < startTimeLowerBound and startTimeLowerBound != yesterday and startTimeUpperBound != today:
            raise ValueError('The start time lower bound %s is not allowed to be greater than the specified start time upper bound %s' % (startTimeLowerBound,startTimeUpperBound))

    # MPCS-5103 08/22/13
    # MPCS-5243 09/05/13 Redo
    # MPCS-5244 06/04/14 Do all fragments
    #
    # We look at all sessionFragments when looking for sessions, and all fragments will be dumped.
    #
    # Note that whereClauseString cannot be empty at this point, but don't trust that just in case
    # the logic above is altered later.

    whereClauseString += ' ORDER BY hostId,sessionId,sessionFragment'

    return whereClauseString

QueryResult=mpcsutil.NamedTuple('QueryResult', ['sessionId','hostId','host','sessionFragment'])
def readQueryResults(outStream):
    '''Read the results of the database session query into parallel lists of session keys and hosts.

    Parameters
    -----------
    outStream - The output from the mysql application

    Returns
    --------
    A list of tuples of the form (key,host) where "key" is a session key and "host" is the corresponding session host for that key'''

    #Get a list of all the session keys/hosts that are new by digging through the output
    _re=re.compile(r'^\s*(?P<sessionId>[\d]+)\s*(?P<hostId>[^\s]+)\s*(?P<host>[^\s]+)\s*(?P<sessionFragment>[^\s]+)\s*$')
    _get_result = lambda _line: (lambda x: QueryResult(**x.groupdict()) if x else None)(_re.match(_line))
    with open(outStream, 'r') as ff:
        return list(filter(lambda x: bool(x), map(_get_result, ff.readlines())))

def createCommandLineOptions():
    '''Create all the command line options for this application

    Returns
    --------
    An optparse.OptionParser object for parsing the command line arguments fed to this application'''

    parser = mpcsutil.create_option_parser()

    #optparse already automatically has its own --help and --version options
    parser.add_option("-O","--sessionHost",action="callback",type="string",metavar="HOSTNAME",callback=parseHost,
                      help="The session host for all the session to retrieve from the database [All sessions from the localhost are retrieved by default]")
    parser.add_option("-w","--startTimeLowerBound",action="callback",type="string",metavar='ISO_OR_DOY_TIME',callback=parseStartTimeLowerBound,
                      help="The lower bound on the start time for sessions to retrieve from the database [By default the start time will be 24 hours prior to the current time]" +
                      ". This time is expected to be either in the ISO format YYYY-MM-DDTHH:mm:ss or DOY format YYYY-DDDTHH:mm:ss. Subseconds will be ignored.")
    parser.add_option("-x","--startTimeUpperBound",action="callback",type="string",metavar='ISO_OR_DOY_TIME',callback=parseStartTimeUpperBound,
                      help="The upper bound on the start time for sessions to retrieve from the database [By default the start time will be after the last available session]" +
                      ". This time is expected to be either in the ISO format YYYY-MM-DDTHH:mm:ss or DOY format YYYY-DDDTHH:mm:ss. Subseconds will be ignored.")
    parser.add_option("-o","--outputDirectory",action="callback",type="string",metavar="DIRECTORY",callback=parseOutputDir,
                      help="The directory where the output dump files should be placed.  Defaults to '" + tempfile.gettempdir() + "'")
    parser.add_option("-y","--dumpOptions",action="callback",type="string",metavar="OPTIONS",callback=parseDumpOpts,
                      help="Additional options to mysqldump.  Defaults to '--disable-keys=FALSE'")
    parser.add_option("-d","--dumpTables",action="callback",type="string",metavar="STRING",callback=parseDumpTables,
                      help="Comma-separated tables to dump. Defaults to all tables except Host, Session, and EndSession")
    parser.add_option("-j","--databaseHost",action="callback",type="string",metavar="HOSTNAME",callback=parseDatabaseHost,
                      help="Host on which the database server is running")
    parser.add_option("-n","--databasePort",action="callback",type="int",metavar="INT",callback=parseDatabasePort,
                      help="Port on which the database server is running")
    parser.add_option("-u","--databaseUser",action="callback",type="string",metavar="USERNAME",callback=parseDatabaseUser,
                      help="Database user name")
    parser.add_option("-p","--databasePassword",action="callback",type="string",metavar="PASSWORD",callback=parseDatabasePwd,
                      help="Database password")

    parser.add_option("--describe",action="store_true",dest="describe",help="Describe actions only")

    parser.add_option("--extendedSCET",action="store_true",dest="extendedSCET",help="Dump extended tables by default")

    parser.add_option("-K", "--testKey", action="callback", type="string", metavar="STRING", callback=parseTestKey,
                      help="Sessions and/or fragments")
    return parser

def parseDumpOpts(option, opt, value, parser):
    '''A callback function for parsing the mysqldump options option

    Params
    -------
    option - The dumpOptions Option instance
    opt - The long or short option value of the dumpOptions option (that was used)
    value - The value of the dumpOptions to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()

    parser.dumpOpts = value

def parseDumpTables(option, opt, value, parser):
    '''A callback function for parsing the dumpTables option

    Params
    -------
    option - The dumpTables Option instance
    opt - The long or short option value of the dumpTables option (that was used)
    value - The value of the dumpTables to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.replace(',', ' ').strip()

    if value == '':
        value = None

    parser.dumpTables = value

def parseDatabaseUser(option, opt, value, parser):
    '''A callback function for parsing the database username option

    Params
    -------
    option - The databaseUser Option instance
    opt - The long or short option value of the databaseUser option (that was used)
    value - The value of the databaseUser to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.lower().strip()

    parser.databaseUser = value

def parseDatabasePwd(option, opt, value, parser):
    '''A callback function for parsing the database password option

    Params
    -------
    option - The databasePassword Option instance
    opt - The long or short option value of the databasePassword option (that was used)
    value - The value of the databasePassword to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()

    parser.databasePassword = value

def parseDatabaseHost(option, opt, value, parser):
    '''A callback function for parsing the database host option

    Params
    -------
    option - The databaseHost Option instance
    opt - The long or short option value of the databaseHost option (that was used)
    value - The value of the databaseHost to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.lower().strip()

    parser.databaseHost = value

def parseDatabasePort(option, opt, value, parser):
    '''A callback function for parsing the database port option

    Params
    -------
    option - The databasePort Option instance
    opt - The long or short option value of the databasePort option (that was used)
    value - The value of the databaseport to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        parser.databasePort = int(value)

def parseStartTimeLowerBound(option, opt, value, parser):
    '''A callback function for parsing the start time lower bound command line option

    Params
    -------
    option - The start time lower bound Option instance
    opt - The long or short option value of the start time lower bound option (that was used)
    value - The value of the start time lower bound to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()
        if mpcsutil.timeutil.doyRegexpObj.match(value):
            value = mpcsutil.timeutil.removeTimeStringSubseconds(value)
            value = datetime.strptime(value, mpcsutil.timeutil.doyFmt).strftime(mpcsutil.timeutil.isoFmt)
        elif not mpcsutil.timeutil.isoRegexpObj.match(value):
            raise optparse.OptionValueError('Input time ' + value + ' does not match neither ISO format YYYY-MM-DDTHH:mm:ss nor DOY format YYYY-DDDTHH:mm:ss')

    parser.startTimeLowerBound = value

def parseStartTimeUpperBound(option, opt, value, parser):
    '''A callback function for parsing the start time upper bound command line option

    Params
    -------
    option - The start time upper bound Option instance
    opt - The long or short option value of the start time upper bound option (that was used)
    value - The value of the start time upper bound to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()
        if mpcsutil.timeutil.doyRegexpObj.match(value):
            value = mpcsutil.timeutil.removeTimeStringSubseconds(value)
            value = datetime.strptime(value, mpcsutil.timeutil.doyFmt).strftime(mpcsutil.timeutil.isoFmt)
        elif not mpcsutil.timeutil.isoRegexpObj.match(value):
            raise optparse.OptionValueError('Input time ' + value + ' does not match neither ISO format YYYY-MM-DDTHH:mm:ss nor DOY format YYYY-DDDTHH:mm:ss')

    parser.startTimeUpperBound = value

def parseHost(option, opt, value, parser):
    '''A callback function for parsing the session host command line option

    Params
    -------
    option - The session host Option instance
    opt - The long or short option value of the session host option (that was used)
    value - The value of the session host to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()

    parser.sessionHost = value

def parseOutputDir(option, opt, value, parser):
    '''A callback function for parsing the output directory command line option

    Params
    -------
    option - The output directory Option instance
    opt - The long or short option value of the output directory option (that was used)
    value - The value of the output directory to use
    parser - is the OptionParser instance driving the whole thing'''

    if value is not None:
        value = value.strip()

    if not os.path.isdir(value):
        raise optparse.OptionValueError('The output directory %s does not exist' % (value))

    parser.outputDir = value


def parseTestKey(option, opt, value, parser):
    '''A callback function for parsing the testKey option.
       This parameter is a comma-separated list of session references.
       These are the kinds of session references we support:
           3       A single session, all fragments
           5..10   A range of sessions, all fragments
           5..     A range of sessions, open-ended, all fragments
           6:9     A single fragment of a single session
           7:1..3  A range of fragments of a single session
           8:7..   An open-ended range of fragments of a single session

       The result is an array of 4-tuples.
       The first element is the starting session,
       The second element is the ending session or None
       The third element is the starting fragment or None
       The fourth element is the ending fragment or None

       MPCS-5244 06/06/14 New method

    Params
    -------
    option - The dumpTables Option instance
    opt - The long or short option value of the dumpTables option (that was used)
    value - The value of the dumpTables to use
    parser - is the OptionParser instance driving the whole thing'''

    value = value.strip()

    if value is None or value == '':
        raise optparse.OptionValueError('No test keys supplied')

    results = []

    for key in value.split(','):

        key = key.strip()

        if ':' not in key:
            # No fragments

            part = key.split('..')

            if len(part) > 2:
                raise optparse.OptionValueError(
                          "Malformed --testKey option: Bad session range (%s)" % (key))

            first = parseInteger(part[0], 'session', False)

            if len(part) == 2:
                last = parseInteger(part[1], 'session', True)

            else:
                last = first

            if last != None and first > last:
                raise optparse.OptionValueError(
                          "Malformed --testKey option: Session ranges must be non-empty (%d..%d)" % (first, last))

            # We have a range of sessions, range may be open-ended, and all fragments

            results.append((first, last, None, None))
        else:
            # Fragments

            part = key.split(':')

            if len(part) != 2:
                raise optparse.OptionValueError(
                          "Malformed --testKey option: Bad fragment syntax (%s)" % (key))

            session = parseInteger(part[0], 'session', False)

            part[1] = part[1].strip()

            part = part[1].split('..')

            if len(part) > 2:
                raise optparse.OptionValueError(
                          "Malformed --testKey option: Bad fragment range (%s)" % (part[1]))

            first = parseInteger(part[0], 'fragment', False)

            if len(part) == 2:
                last = parseInteger(part[1], 'fragment', True)

            else:
                last = first

            if (last != None) and (first > last):
                raise optparse.OptionValueError(
                          "Malformed --testKey option: Fragment ranges must be non-empty (%d..%d)" % (first, last))

            # We have a range of fragments, one session, range may be open-ended

            results.append((session, session, first, last))

    parser.testKey = results


def parseInteger(s, what, empty):
    '''Parse an integer from a string and validate as a session/fragment
       MPCS-5244 06/06/14 New method

    Params
    -------
    s     - The string to parse
    what  - The type of data for error messages
    empty - If true, allow empty to mean None

    Returns
    --------
    Integer parsed from the string or None'''

    s = s.strip()

    if empty and s == '':
        return None

    try:
        value = int(s)

        if value < 1:
            raise optparse.OptionValueError(
                      "Malformed --testKey option: %s must be > 0 (%d)" % (what, value))
    except ValueError:
        raise optparse.OptionValueError(
                  "Malformed --testKey option: Bad %s (%s)" % (what, s))

    return value


def parseCommandLine():
    '''Parse the command line options given to this application

    Returns
    --------
    A tuple of the form (mission,outputDirectory,sessionHost,startTimeLowerBound,startTimeUpperBound).  All values
    contained in the tuple are strings except the last two.'''

    parser = createCommandLineOptions()

    parser.startTimeLowerBound = None
    parser.startTimeUpperBound = None
    parser.sessionHost = None
    parser.outputDir = None
    parser.dumpOpts = None
    parser.dumpTables = None
    parser.databaseHost = None
    parser.databasePort = None
    parser.databaseUser = None
    parser.databasePassword = None
    parser.testKey = None

    (_options, _args) = parser.parse_args()

    mission = mpcsutil.config.GdsConfig().getMission()

    #create the output directory if it doesn't exist
    if parser.outputDir is None:
        parser.outputDir = tempfile.gettempdir()
        print('Defaulting dump output directory to \'%s\'' % (parser.outputDir))
    try:
        os.makedirs(parser.outputDir)
    except OSError:
        pass

    describe = _options.describe
    extendedSCET = _options.extendedSCET

    if parser.sessionHost is None:
        parser.sessionHost = socket.gethostname()
        if parser.sessionHost.find('.') != -1:
            parser.sessionHost = parser.sessionHost.split('.')[0]
        print('Defaulting session host query parameter to \'%s\'' % (parser.sessionHost))

    if parser.dumpOpts is None:
        parser.dumpOpts = "--disable-keys=FALSE"
        print('Defaulting mysqldump options to \'%s\'' % (parser.dumpOpts))

    if parser.dumpTables is None:
        if extendedSCET:
            parser.dumpTables = "ChannelAggregate ChannelData HeaderChannelAggregate MonitorChannelAggregate SseChannelAggregate CommandMessage CommandStatus Evr2 EvrMetadata SseEvr2 SseEvrMetadata Frame FrameBody LogMessage Packet2 PacketBody SsePacket2 SsePacketBody Product2 CfdpFileGeneration CfdpFileUplinkFinished CfdpIndication CfdpPduReceived CfdpPduSent CfdpRequestReceived CfdpRequestResult"
        else:
            parser.dumpTables = "ChannelAggregate ChannelData HeaderChannelAggregate MonitorChannelAggregate SseChannelAggregate CommandMessage CommandStatus Evr EvrMetadata SseEvr SseEvrMetadata Frame FrameBody LogMessage Packet PacketBody SsePacket SsePacketBody Product CfdpFileGeneration CfdpFileUplinkFinished CfdpIndication CfdpPduReceived CfdpPduSent CfdpRequestReceived CfdpRequestResult"

        print('Defaulting dumped tables to \'%s\'' % (parser.dumpTables))

    return (mission,parser.outputDir,parser.sessionHost,parser.startTimeLowerBound,parser.startTimeUpperBound,\
            parser.dumpOpts,parser.dumpTables,parser.databaseHost,parser.databasePort,\
            parser.databaseUser,parser.databasePassword,describe,extendedSCET,parser.testKey)



def test():

    (mission,outputDir,sessionHost,startTimeLowerBound,startTimeUpperBound,sqlOpts,dumpTables,\
     databaseHost,databasePort,databaseUser,databasePassword,describe,extendedSCET,testKey) = parseCommandLine()

    dbName = mpcsutil.database.getMissionDatabaseName(mission)

    #NOTE: There are placeholder parameters in this method call for the db username, db password, db host and db port which all work properly,
    # so if we need to add those to the command line for this utility, then the logic only needs to be implemented to parse them from the
    # command line and pass them into this method...beyond this point, they have already been implemented
    returnVal = createFiles(dbName,outputDir,sessionHost=sessionHost,startTimeLowerBound=startTimeLowerBound,\
                            startTimeUpperBound=startTimeUpperBound,dumpOpts=sqlOpts,dumpTables=dumpTables,\
                            dbHost=databaseHost,dbPort=databasePort,dbUser=databaseUser,\
                            dbPassword=databasePassword,describe=describe,extendedSCET=extendedSCET,testKey=testKey)
    sys.exit(returnVal)

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
