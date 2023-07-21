#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import os
import mtak.wrapper


def _get_vxworks_ip_addr(self):
        '''Gets the IP address of either the VxSim or VxWorks instance
        for establishing a telnet connection. If running on WSTS, the
        VxSim address is read from the PTF VxSim configuration file and
        the latest VxSim instance number.

        Return
        ------
        String containing the VxSim/VxWorks IP address, or None if an
        error occurred.'''

        if self.vx_addr:
            return self.vx_addr

        if mtak.wrapper.get_session_config().venueType == "TESTSET":

            # Get the VxSim subnet and session id from WSTS for telnet
            # The VxSim subnet is contained in the PTF configuration files.

            config_file = os.environ["MSL_ROOT"] + \
                        "/ptf/scripts/configurations/target/vxsim-6.4.env"

            if not os.path.isfile(config_file):
                self._fail("Cannot find file %s" % (config_file))
                return None

            scripts.helpers.parsers.env_file_parser.EnvFileReader().\
                                                      read_file(config_file)

            subnet = os.environ["TARGET_ip_subnet"]

            #subnet = '192.168.90'

            session_file = "/dev/shm/vxsim_instances"

            if not os.path.isfile(session_file):
                self._fail("Cannot find file %s" % (session_file))
                return None

            instances = open(session_file,'r').readlines()
            instance_list = []
            for instance in instances:
                instance_list.append(instance.rstrip())

            session = instance_list.pop()

            self.vx_addr = subnet + "." + session

        else:
            # Get the VxWorks IP address for the testbed. The address is
            # fixed for all testbeds. Normal telnet port is used.
            self.vx_addr = "192.9.200.99"

        return self.vx_addr

def _get_sse_host(self):
        '''Obtains the SSE workstation host name by one of two methods:
        1) If the environment variable FIT_SSE_HOST is set it is used
        2) Assumes you are on a testbed GDS workstation whose name is
           of the form 'msl<testbed>gds1', and from there assembles a
           name 'msl<testbed>sse'.'''

        try:
            return os.environ["FIT_SSE_HOST"]
        except KeyError:
            pass

        my_host = os.environ["HOST"].split('.')[0]

        if my_host.startswith("msl") and my_host.endswith("gds1"):
            my_testbed = my_host[:len(my_host)-4]
            return my_testbed + "sse"
        else:
            return "BAD_HOST: %s" % (my_host)


def _ssh_to_vxworks(self,cmd,addr,delay=10):
        '''Establishes a password-less SSH session from the GDS machine
        to the SSE machine connected to the VxWorks RCE for the testbed.
        Issues the cmd via telnet, capture all output for 'delay' seconds
        and return output.'''

        func = "_ssh_to_vxworks"

        try:
            # This script assumes that your private key is named
            # 'fit_ssh' and is located in your ./ssh directory.
            private_key = "~/.ssh/fit_ssh"

            # Assuming we're running on the testbed's GDS workstation,
            # derive the SSE workstation name.
            my_sse_ws = self._get_sse_host()

            ssh_cmd = "ssh -tt -i " + private_key + " " + \
                      my_sse_ws + " telnet -r " + addr

            self.func_log(func,"SSH cmd   : %s" % (ssh_cmd))
            self.func_log(func,"Telnet cmd: %s" % (cmd))

            p = subprocess.Popen(ssh_cmd,
                                 shell=True,
                                 stdin=subprocess.PIPE,
                                 stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE)

            # Let the SSH/Telnet connection get established.
            time.sleep(3)

            # Issue the caller's command, and delay for a bit.
            p.stdin.write(cmd + "\n")

            time.sleep(delay)

            # Terminate the SSH/telnet session and return all the output.
            out, err = p.communicate("exit\n")

            return out

        except OSError as e:
            self._fail("Error in ssh/telnet to VxWorks: {}".format(str(e)))
        except Exception as err:
            self._fail("Unk error in ssh/telnet to VxWorks: {}".format(str(err)))

        return None


def vxworks_cmd(self,cmd,delay=2):
        '''Issue a VxWorks command-line command on the flight CPU. This is
        different from the shell_cmd() function. The returned string contains
        the output as a result of the command. This function will pause
        for 'delay' seconds after issuing the function to collect any
        additional output. This is useful if the command spawns a background
        task.

        Beware the following. They are out to get you:

        1) This assumes only a single user (i.e. you) on the workstation
           when running WSTS. It selects just the last VxSim instance as
           the target of the telnet connection. If it is not you, then
           someone else might get pretty annoyed when you start issuing
           VxWorks commands to their sim.

        2) Use of a command that requires interaction with the telnet
           session is problematic. An example is 'i', which requires
           the user to enter <cr> to continue.

        3) Some commands may initiate background processing but return
           immediately. In this case, set a non-zero delay in order for
           all the desired output to be captured.

        4) When running on a testbed, this assumes you've gone through
           the steps to establish password-less ssh for the GDS machine
           on which MTAK/MPCS is running. That is, you need a public/
           private key pair in your .ssh on the SSE workstation allowing
           your secure login from the GDS workstation.

        5) This function will not declare a 'PASS' for the command. That
           is for the caller to interpret. It will, however, declare a
           'FAIL' if there is a problem with the connection.

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        cmd - The full VxWorks command (with arguments)
        delay - Seconds to pause for additional output to be received

        Return
        ------
        String containing the results of the command. The caller is
        responsible for interpreting the results and assessing pass/fail.'''

        func = "vxworks_cmd"

        addr = self._get_vxworks_ip_addr()

        if not addr:
            self._fail("Did not get IP address")
            return None

        if mtak.wrapper.get_session_config().venueType == "TESTSET":

            # We're on WSTS. Just use normal telnet.
            self.func_log(func,"Establishing telnet to WSTS %s" % (self.vx_addr))

            tn = telnetlib.Telnet(self.vx_addr)
            tn.read_until('->')
            tn.write(cmd + '\n')

            val = tn.read_until("Hopefully this string won't match",delay)

            tn.close()
            return val

        else:

            # We're on a testbed. Need to do SSH to the SSE, then telnet.
            return self._ssh_to_vxworks(cmd,addr,delay)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
