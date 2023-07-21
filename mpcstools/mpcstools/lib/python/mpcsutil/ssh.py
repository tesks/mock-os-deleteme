#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Created on Oct 6, 2010

"""

from __future__ import (absolute_import, division, print_function)

import paramiko
import socket
import sys

def connect_via_ssh(host,user):
    '''Perform a password-less SSH to the hostname "host"
    as the username "user".  Requires the legwork of setting up
    passworld-less SSH to have been done already.

    Return an instance of paramiko.SSHClient that is connected
    as the given user to the given host.'''

    try:
        client = paramiko.SSHClient()
        client.load_system_host_keys()
        client.connect(hostname=host,username=user,timeout=3600)
        return client
    except socket.error as err:
        print>>sys.stderr,'Error attempting to make SSH connection to "%s@%s": %s' % (user,host,err)
        return None

def run_remote_cmd(ssh_client,cmd):
    '''Given a "ssh_client" that is an instance of paramiko.SSHClient,
    run a command-line command on the remote host via the SSH connection
    and return the exit status of the command.'''

    chan = ssh_client.get_transport().open_session()
    chan.exec_command(cmd)
    return chan.recv_exit_status()

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
