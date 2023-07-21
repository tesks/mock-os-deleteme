#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import subprocess
import os
import socket
import sys
import re

class ProcessType(object):

    UNKNOWN=0
    JAVA=1
    ACTIVEMQ=2
    MYSQL=3
    TEE=4
    SHELL=5
    PYTHON=6
    MTAK=7

    map = {UNKNOWN : 'Unknown',
           JAVA : 'Java',
           ACTIVEMQ : 'ActiveMQ',
           MYSQL : 'MySQL',
           TEE : 'Logging',
           SHELL : 'Script',
           PYTHON : 'Python',
           MTAK : 'MTAK'}


class MpcsProcess(object):

    def __init__(self,ps_line):
        fields = ps_line.split()

        self.pid = int(fields[0])
        self.ppid = int(fields[1])
        self.pgid = int(fields[2])
        self.user = fields[3].strip()
        self.start_time = fields[4].strip()
        self.short_cmd = fields[5].strip()

        cmd = ''
        for piece in fields[6:]:
            cmd = cmd + ' ' + piece
        self.long_cmd = cmd.strip()

        self.type = None
        self.version = None
        self.mission = None
        self.name = None
        self.parent = None
        self.children = []

    def __str__(self):

        if self.type == ProcessType.ACTIVEMQ:

            return '%s (user=%s pid=%s ppid=%s start=%s)' % (self.name,self.user,self.pid,self.ppid,self.start_time)

        elif self.type == ProcessType.MYSQL:

            return '%s (user=%s pid=%s ppid=%s start=%s)' % (self.name,self.user,self.pid,self.ppid,self.start_time)

        else:

            return '%s (%s) (%s user=%s pid=%s ppid=%s start=%s)' % (self.name,self.mission,ProcessType.map[self.type] if self.type in ProcessType.map else 'Unknown',
                                                                     self.user,self.pid,self.ppid,self.start_time)

    def display_extended(self):

        self._display_extended_helper(0)

    def _display_extended_helper(self,indent):

        print(self.__str__())

        indent = indent + 1
        for child in self.children:
            for _ in range(0,indent):
                print('====', end='')
            child._display_extended_helper(indent)

def doMac():

    return _run_ps(['ps', '-Aww', '-o', 'pid= ppid= pgid= user= start= ucomm= args='])

def doLinux():

    return _run_ps(['ps', '-eww', '-o', 'pid ppid pgid euser start_time ucmd cmd', '--no-headers'])

def _run_ps(pscmd):

    pid_to_proc_map = {}

    # MPCS-3918  - 10/31/2014: commands module deprecated in Python 2.7; use subprocess module
    result = ''
    try:
        result = subprocess.check_output(pscmd)

    except subprocess.CalledProcessError:
        print('Could not run "ps" command!', file=sys.stderr, flush=True)
        sys.exit(1)
    else:
        result = result.decode('utf-8') if isinstance(result, bytes) else result

    for line in result.strip().split('\n'):

        process = MpcsProcess(line)

        # MPCS-8876 07/17/17 Change string-matching patterns for latest ActiveMQ,
        # "find" and "match".

        if process.short_cmd.lower().startswith('java'):
            cmd_no_classpath = process.long_cmd[process.long_cmd.find('-D'):]
            if cmd_no_classpath.find('-Dactivemq.home') != -1:
                process.type = ProcessType.ACTIVEMQ
                process.name = 'ActiveMQ'
                process.version = None
            else:
                process.type = ProcessType.JAVA
                for arg in process.long_cmd.split(' '):
                    if arg.startswith('-DGdsMission=') and not process.mission:
                        process.mission = arg[13:]
                    elif arg.startswith('-DGdsAppName=') and not process.name:
                        process.name = arg[13:]

        elif process.short_cmd.lower() == 'tee':

            process.type = ProcessType.TEE

        elif process.short_cmd.lower().find('chill') != -1:

            process.type = ProcessType.SHELL
            process.name = process.short_cmd

        elif process.short_cmd.endswith('sh') and process.long_cmd.lower().find('chill') != -1:

            process.type = ProcessType.SHELL
            process.name = None

        elif process.short_cmd.lower().find('mysqld') != -1:

            process.type = ProcessType.MYSQL
            process.name = 'MySQL'

        elif process.short_cmd.lower() == 'python':

            process.type = ProcessType.PYTHON
            process.name = process.short_cmd

        else:

            process.type = ProcessType.UNKNOWN
            process.name = process.short_cmd

        pid_to_proc_map[process.pid] = process

    #Pair up parent & child processes
    for proc in pid_to_proc_map.values():

        if (proc.ppid in pid_to_proc_map.keys()) and proc.ppid > 1:

            parent = pid_to_proc_map[proc.ppid]
            proc.parent = parent
            parent.children.append(proc)

    _remove=[]
    for key, process in pid_to_proc_map.items():

        if process.type in [ProcessType.UNKNOWN, ProcessType.PYTHON]:
            for child in process.children:
                if child.name and (child.name in ['chill_mtak_uplink_server', 'chill_mtak_downlink_server']):

                    process.type = ProcessType.MTAK
                    if process.short_cmd.lower() == 'python':
                        pieces = process.long_cmd.split(' ')
                        if len(pieces) >= 2:
                            process.name = pieces[1]
                        else:
                            process.name = 'MTAK Script'
                    else:
                        process.name = process.short_cmd
                    break

            if process.type != ProcessType.MTAK:
                _remove.append(key)
                # del pid_to_proc_map[key]

        if process.type == ProcessType.JAVA and process.name is None:
            _remove.append(key)
            # del pid_to_proc_map[key]

    pid_to_proc_map={kk:vv for kk,vv in pid_to_proc_map.items() if kk not in _remove}

    pass_info_in_tree(pid_to_proc_map.values())

    return pid_to_proc_map

def pass_info_in_tree(processList):

    for process in processList:

        pass_info_to_siblings(process)
        pass_info_to_parent(process)
        pass_info_to_children(process)

def pass_info_to_parent(process):

    if not process or not process.parent:
        return

    if process.parent.type == ProcessType.UNKNOWN:
        process.parent = None
        return

    if process.mission and not process.parent.mission:
        process.parent.mission = process.mission

    if process.name and not process.parent.name:
        process.parent.name = process.name

    pass_info_to_parent(process.parent)

def pass_info_to_children(process):

    if not process or not process.children:
        return

    for child in process.children:

        if process.mission and not child.mission:
            child.mission = process.mission

        if process.name and not child.name:
            child.name = process.name

        pass_info_to_children(child)

def pass_info_to_siblings(process):

    if not process or not process.parent or not process.parent.children:
        return

    for sibling in process.parent.children:

        if sibling is not process:

                if process.mission and not sibling.mission:
                    sibling.mission = process.mission

                if process.name and not sibling.name:
                    sibling.name = process.name

def create_options():

    parser = mpcsutil.create_option_parser(usageText='''%s [options]

    This tool will list out all of the currently running MPCS-related processes.''' % mpcsutil.get_script_name())

    return parser

def test():

    parser = create_options()
    parser.parse_args()

    print('')
    print('Host Name    = %s' % (socket.gethostname()))
    print('OS           = %s' % (os.uname()[0]))
    print('OS Release   = %s' % (os.uname()[2]))
    print('OS Arch      = %s' % (os.uname()[4]))
    print('')
    print('Current Time = %s' % (mpcsutil.timeutil.getTimeString()))
    print('')

    map = {}
    os_name = os.uname()[0].lower()

    if os_name == 'linux':
        map = doLinux()
    elif os_name == 'darwin':
        map = doMac()

    print('-------------------------------------------')
    print('MPCS Processes')
    print('-------------------------------------------')

    for process in map.values():

        if not(process.ppid in map.keys()):

            #print '============================================================='
            #process.debug_display_one_line()
            print('')
            process.display_extended()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
