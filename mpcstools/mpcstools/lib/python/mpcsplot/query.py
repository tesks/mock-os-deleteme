#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import mpcsplot.err

import copy
import logging
import os.path
import random
import subprocess
import tempfile
import sys

_log = logging.getLogger('mpcs.chill_get_plots')
CHANNEL_ID_SEPARATOR = '-'

class QueryHelper(object):

    def __init__(self,parser=None):

        gds_config = mpcsutil.config.GdsConfig()
        self._maxTraces = int(gds_config.getProperty('automationApp.mtak.plots.maxTraces','20'))

        self.begin_time = None
        self.end_time = None
        self.channel_id_file = None
        self.time_type = None
        self.channel_ids = None

        self.from_session_start = None
        self.to_session_start = None
        self.fsw_version_pattern = None
        self.downlink_stream_id = None
        self.session_key = None
        self.session_desc_pattern = None
        self.session_name_pattern = None
        self.session_host_pattern = None
        self.session_user_pattern = None
        self.session_type_pattern = None
        self.sse_version_pattern = None
        self.cv_dssid = None
        self.cv_string = None
        self.cv_vcid = None
        self.cv_types = None

        self.database_password = None
        self.database_user = None
        self.database_host = None
        self.database_port = None

        self.fetched_evrs = False

        if parser is not None:
            self.set_values_from_parser(parser)

    def set_values_from_parser(self,parser):

        if parser.values.config_filename is not None:
            raise NotImplementedError('The command line option -c/--chartConfigFile is not yet supported.')

        #Loop through each of our groups of options, but ignore the options that aren't
        #meant for chill_get_chanvals
        for group in parser.option_groups:

            #if the group doesn't have args meant for Java, skip it...we only care
            #about parsing the stuff destined for chill_get_chanvals
            if not group.is_java_group:
                continue

            for option in group.option_list:

                opt_val = getattr(parser.values,option.dest)

                if opt_val is not None:
                    setattr(self,option.dest,opt_val)

        self._check_query_limits()

    def _check_query_limits(self):

        full_list = []
        full_list.extend(self._parse_id_csv_list(self.channel_ids))

        #parse the channel ID file
        if self.channel_id_file:
            with open(self.channel_id_file) as infile:
                for line in infile:
                    line = line.strip()
                    if len(line) == 0:
                        continue
                    if line.startswith("#"):
                        continue
                    full_list.extend(self._parse_id_csv_list(line))

        if not full_list:
            raise mpcsplot.err.DataRetrievalError('You did not specify any channels to plot. Please specify between 1 and %d channels and try again.' % (self._maxTraces))
        elif len(full_list) > self._maxTraces:
            raise mpcsplot.err.DataRetrievalError('Your query returned too many results.  The maximum amount of traces allowed per plot is %d, but you are querying for %d.' %
                                     (self._maxTraces,len(full_list)))

        # MPCS-8237 9/29/2016 - Added LST support
        # Since all channel query results have channel ids capitalized even if they were input with lower case,
        # just make sure they are all caps here.  If the cases were different, the plots did not find the values
        # because the cases were not the same.
        return list(map(lambda chan_id: chan_id.upper(), full_list))

    def _parse_id_csv_list(self,input):

        global CHANNEL_ID_SEPARATOR

        list = []
        if input is None:
            return list

        input = input.strip()
        for item in input.split(','):

            #strip off comment at end of line
            if item.find("#") != -1:
                item = item[0:item.find("#")].strip()

            #we got a list of channel IDs
            if '..' in item:

                minmax = item.split('..')
                min_id = minmax[0]
                max_id = minmax[1]

                #Assuming that the channel ID has a - in it
                (min_prefix,_min_suffix,min) = self._parse_channel_id(min_id)
                (max_prefix,_max_suffix,max) = self._parse_channel_id(max_id)

                if min_prefix != max_prefix:
                    raise mpcsplot.err.DataRetrievalError('Invalid EHA range specified "%s".  Channel prefixes "%s" and "%s" must match.' % (item,min_prefix,max_prefix))
                elif min > max:
                    raise mpcsplot.err.DataRetrievalError('Invalid EHA range specified "%s".  Start of range value "%s" is bigger than end of range value "%s".' % (item,min,max))

                for num in range(min,max+1):
                    id = '%s%s%04d' % (min_prefix,CHANNEL_ID_SEPARATOR,num)
                    list.append(id)

            #it's just a channel ID
            else:

                #This parsing is actually just for validation...we don't need the return values
                (_,_,_) = self._parse_channel_id(item)
                list.append(item)

        return list

    def _parse_channel_id(self,channel_id):

        global CHANNEL_ID_SEPARATOR

        for wildcard in mpcsutil.database.db_wildcard_chars:
            if wildcard in channel_id:
                raise mpcsplot.err.DataRetrievalError('Found invalid wildcard character "%s" in channel ID "%s". Wildcard queries are not allowed by this application.'
                                                      % (wildcard,channel_id))

        dash_index = channel_id.find(CHANNEL_ID_SEPARATOR)

        prefix = channel_id[:dash_index]
        suffix = channel_id[dash_index+1:]

        value = 0
        try:
            value = int(suffix)
        except ValueError as ve:
            raise mpcsplot.err.DataRetrievalError('Invalid channel ID "%s" found: %s' % (channel_id,ve))

        return (prefix,suffix,value)

    def _construct_query(self,order_by_str):

        #query = '%s/chill_get_chanvals ' % (mpcsutil.chillBinDirectory)

        query = '--fromTestStart "%s" ' % (self.from_session_start) if self.from_session_start is not None else ''
        query += '--toTestStart "%s" ' % (self.to_session_start) if self.to_session_start is not None else ''
        query += '--fswVersionPattern "%s" ' % (self.fsw_version_pattern) if self.fsw_version_pattern is not None else ''
        query += '--downlinkStreamId "%s" ' % (self.downlink_stream_id) if self.downlink_stream_id is not None else ''
        query += '--testKey "%s" ' % (self.session_key) if self.session_key is not None else ''
        query += '--testDescriptionPattern "%s" ' % (self.session_desc_pattern) if self.session_desc_pattern is not None else ''
        query += '--testNamePattern "%s" ' % (self.session_name_pattern) if self.session_name_pattern is not None else ''
        query += '--testHostPattern "%s" ' % (self.session_host_pattern) if self.session_host_pattern is not None else ''
        query += '--testUserPattern "%s" ' % (self.session_user_pattern) if self.session_user_pattern is not None else ''
        query += '--testTypePattern "%s" ' % (self.session_type_pattern) if self.session_type_pattern is not None else ''
        query += '--sseVersionPattern "%s" ' % (self.sse_version_pattern) if self.sse_version_pattern is not None else ''
        query += '--dssId "%s" ' % (self.cv_dssid) if self.cv_dssid is not None else ''
        query += '--vcid "%s" ' % (self.cv_vcid) if self.cv_vcid is not None else ''
        query += '--stringId "%s" ' % (self.cv_string) if self.cv_string is not None else ''
        query += '--channelTypes "%s" ' % (self.cv_types) if self.cv_types is not None else ''

        query += '--dbPwd "%s" ' % (self.database_password) if self.database_password is not None else ''
        query += '--dbUser "%s" ' % (self.database_user) if self.database_user is not None else ''
        query += '--databaseHost "%s" ' % (self.database_host) if self.database_host is not None else ''
        query += '--databasePort "%s" ' % (self.database_port) if self.database_port is not None else ''

        query += '--beginTime "%s" ' % (self.begin_time) if self.begin_time is not None else ''
        query += '--endTime "%s" ' % (self.end_time) if self.end_time is not None else ''
        query += '--timeType "%s" ' % (self.time_type) if self.time_type is not None else ''

        query += '--orderBy "%s" ' % (order_by_str)

        return query

    def _construct_evr_query(self,order_by_str):

        query = '--fromTestStart "%s" ' % (self.from_session_start) if self.from_session_start is not None else ''
        query += '--toTestStart "%s" ' % (self.to_session_start) if self.to_session_start is not None else ''
        query += '--fswVersionPattern "%s" ' % (self.fsw_version_pattern) if self.fsw_version_pattern is not None else ''
        query += '--downlinkStreamId "%s" ' % (self.downlink_stream_id) if self.downlink_stream_id is not None else ''
        query += '--testKey "%s" ' % (self.session_key) if self.session_key is not None else ''
        query += '--testDescriptionPattern "%s" ' % (self.session_desc_pattern) if self.session_desc_pattern is not None else ''
        query += '--testNamePattern "%s" ' % (self.session_name_pattern) if self.session_name_pattern is not None else ''
        query += '--testHostPattern "%s" ' % (self.session_host_pattern) if self.session_host_pattern is not None else ''
        query += '--testUserPattern "%s" ' % (self.session_user_pattern) if self.session_user_pattern is not None else ''
        query += '--testTypePattern "%s" ' % (self.session_type_pattern) if self.session_type_pattern is not None else ''
        query += '--sseVersionPattern "%s" ' % (self.sse_version_pattern) if self.sse_version_pattern is not None else ''
        query += '--dssId "%s" ' % (self.cv_dssid) if self.cv_dssid is not None else ''
        query += '--vcid "%s" ' % (self.cv_vcid) if self.cv_vcid is not None else ''
        query += '--stringId "%s" ' % (self.cv_string) if self.cv_string is not None else ''

        query += '--dbPwd "%s" ' % (self.database_password) if self.database_password is not None else ''
        query += '--dbUser "%s" ' % (self.database_user) if self.database_user is not None else ''
        query += '--databaseHost "%s" ' % (self.database_host) if self.database_host is not None else ''
        query += '--databasePort "%s" ' % (self.database_port) if self.database_port is not None else ''

        query += '--beginTime "%s" ' % (self.begin_time) if self.begin_time is not None else ''
        query += '--endTime "%s" ' % (self.end_time) if self.end_time is not None else ''
        query += '--timeType "%s" ' % (self.time_type) if self.time_type is not None else ''

        query += '--orderBy "%s" ' % (order_by_str)

        return query

    def execute_eha_query(self,order_by_str):

        global _log

        result_table = EhaResultTable()
        result_table.ordered_channel_ids = self._check_query_limits()

        # MPCS-8210 05/18/16 use plot_csv template. Fixes problems with plotting in m20 and prevents problems in other adaptations
        query = '%s %s -o plot_csv --channelIds %s' % (os.path.join(mpcsutil.chillBinDirectory,'chill_get_chanvals'),
                                           self._construct_query(order_by_str),
                                           ','.join(result_table.ordered_channel_ids))

        try:
            query_result_file = tempfile.NamedTemporaryFile(prefix='MPCS',suffix='.txt',delete=False)

            process = None
            try:
                process = subprocess.Popen(query,shell=True,stderr=subprocess.PIPE,stdout=query_result_file)
                # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
                errors = process.communicate()[1]
                errors = errors.decode('utf-8') if isinstance(errors, bytes) else errors
                status = process.returncode

            except OSError:
                errLines = ''.join(process.stderr)
                dummy_exc_class, dummy_exc, traceback = sys.exc_info()
                my_exc = mpcsplot.err.DataRetrievalError('Error retrieving data: %s' % (errLines))
                raise my_exc.__class__( my_exc, traceback)

            # chill_get tools can return errors, but terminate with a 0 status code, if the command line is erroneous
            if status != 0 or errors:

                message = 'Could not execute query: ' + errors
                raise mpcsplot.err.DataRetrievalError(message)

            #Go back to the beginning of the file
            query_result_file.file.flush()
            query_result_file.file.seek(0)
            for csv in query_result_file:

                chanval = mpcsutil.channel.ChanVal.get_from_database_csv(csv)
                if not chanval.channelId in result_table:
                    result_table[chanval.channelId] = []
                result_table[chanval.channelId].append(chanval)
        finally:
            query_result_file.close()

        #Need to make a temporary copy of the list because we don't want
        #to be mutating it while we loop through it
        channel_ids_copy = copy.copy(result_table.ordered_channel_ids)
        for channel_id in channel_ids_copy:
            if not channel_id in result_table:
                _log.warning('Channel ID "%s" did not appear in the query results.  It will be omitted from the plot.' % (channel_id))
                result_table.ordered_channel_ids.remove(channel_id)

        return result_table

    def execute_evr_query(self,order_by_str):

        #TODO: By default just query all the EVRs...it's faster to fetch them all at once because
        #we might need them later
        #
        #TODO: Intentionally updated this to make it only query FSW EVRs for now

        query = '%s %s -o plot_csv --evrTypes fr' % (os.path.join(mpcsutil.chillBinDirectory,'chill_get_evrs'),self._construct_evr_query(order_by_str))

        values = {}

        try:
            query_result_file = tempfile.NamedTemporaryFile(prefix='MPCS',suffix='.txt',delete=False)

            process = None
            try:
                process = subprocess.Popen(query,shell=True,stderr=subprocess.PIPE,stdout=query_result_file)
                # MPCS-6999 09/28/16 - get the Java return status value. If nonzero, throw an error
                errors = process.communicate()[1]
                status = process.returncode
            except OSError:
                errLines = ''.join(process.stderr)
                dummy_exc_class, dummy_exc, traceback = sys.exc_info()
                my_exc = mpcsplot.err.DataRetrievalError('Error retrieving data: %s' % (errLines))
                raise my_exc.__class__( my_exc, traceback)

            # chill_get tools can return errors, but terminate with a 0 status code, if the command line is erroneous
            if status != 0 or errors:

                message = 'Could not execute query: ' + errors
                raise mpcsplot.err.DataRetrievalError(message)

            #Go back to the beginning of the file
            query_result_file.file.flush()
            query_result_file.file.seek(0)
            for csv in query_result_file:

                evr = mpcsutil.evr.Evr.get_from_database_csv(csv)
                if not evr.level in values:
                    values[evr.level] = []
                values[evr.level].append(evr)
        finally:
            query_result_file.close()

        self.fetched_evrs = True

        return values

class EhaResultTable(dict):

    def __init__(self):

        dict.__init__(self)

        self.ordered_channel_ids = []

    def get_index_for_id(self,id):

        try:
            return self.ordered_channel_ids.index(id)
        except ValueError:
            return -1

    #################################
    #Not sure if we'll need these...
    #################################

    def __setitem__(self, key, item):
        '''x.__setitem__(i, y) <==> x[i]=y'''

        dict.__setitem__(self,key,item)

    def __delitem__(self, key):
        '''x.__delitem__(y) <==> del x[y]'''

        dict.__delitem__(self,key)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
