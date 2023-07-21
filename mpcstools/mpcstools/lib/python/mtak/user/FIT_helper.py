#! /usr/bin/env python
# -*- coding: utf-8 -*-

#
#   Copyright 2008, by the California Institute of Technology.
#   ALL RIGHTS RESERVED. United States Government Sponsorship
#   acknowledged. Any commercial use must be negotiated with the Office
#   of Technology Transfer at the California Institute of Technology.
#
#   Information included herein is controlled under the International
#   Traffic in Arms Regulations ("ITAR") by the U.S. Department of State.
#   Export or transfer of this information to a Foreign Person or foreign
#   entity requires an export license issued by the U.S. State Department
#   or an ITAR exemption prior to the export or transfer.
#
# Author: Joe Hutcherson
# Date:   04/29/2008

from __future__ import (absolute_import, division, print_function)

import mtak
import mtak.wrapper

import scripts.helpers.parsers.env_file_parser

import subprocess
import os
import re
import subprocess
import sys
import telnetlib
import time
import traceback
import datetime
import platform

class FITHelper(object):
    '''A utility object to support FIT testing. It provides the necessary
    functions for FIT tests to send ground commands to the flight system,
    manipulate items in VxWorks directly, monitor EVRs and EHA.

    If the environment variable 'FIT_LOGS' is set then the FITHelper objects
    will creat a log file of the form
    <FIT_LOGS>/<testName>.<timestamp>.log

    Log (and console) entries are of the form
    <timestamp> <testName> <testStep>.<subStep> <specific_message>
    The log file will contain ONLY those statements produced by this
    object! It will not contain other stdout, such as MTAK messages.

    If the environment variable 'FIT_INTERACTIVE' is set to TRUE then the
    FITHelper objects will allow the user to interact with the wait and
    prompt commands.
    '''

    def __init__(self,name="UnNamedTest",interactive=False):
        '''Initializes this test object. Note that the log file and
        interactive behavior are controlled by the FIT_LOGS and
        FIT_INTERACTIVE environment variables.

        Args
        ----
        name - Name of the test procedure being run. This will appear
               in log messages generated when the script is run. It will
               also serve as the name of the log file generated.'''

        self.name = name
        self.cmdCounter = 0
        self.subStep = 0
        self.numPassed = 0
        self.numFailed = 0
        self.numIgnored = 0
        self.defaultTimeout = 10
        self.defaultLookback = 30
        self.logFile = None
        self.logFileName = None
        self.vx_addr = None
        self.ignore = False
        self.defSleepTime = 5

        _log_base=os.environ.get("FIT_LOGS")
        if _log_base:
            ts = "{:%Y%m%d.%H%M%S}".format(datetime.datetime.utcnow())
            self.logFileName = os.path.join(_log_base, '{}.{}.log'.format(name, ts))
            try:
                self.logFile = open(self.logFileName,"w")
            except IOError:
                self._print("Error opening log file: {}".format(traceback.format_exc()))
            else:
                self._print("Logging output to {}".format(self.logFileName))
        else:
            self._print("FIT_LOGS is not set. Logging to console only.")


        self.interactive = False
        if os.environ.get("FIT_INTERACTIVE", '') == "TRUE":
            self.interactive =  True
            self._print("Running in interactive mode")


    #=========================================================================
    # Internal support functions
    #=========================================================================


    def _print(self,message=None):
        '''Prints the message to the console and an output file (if any).
        Prepends a timestamp and test name to the message.

        Args
        ----
        message - Message to record.'''

        message = message if message else ''

        entry = "{:%Y%m%d-%H:%M:%S} {} {}".format(datetime.datetime.utcnow(), self.name, message)

        print(entry)

        try:
            with open(self.logFileName, 'a') as ff:
                print(entry, file=ff, flush=True)
        except:
            print('Exception caught while trying to write to logfile `{}`:\n{}'.format(self.logFileName, traceback.format_exc()))


    def _command(self,func,cmd):
        '''Use when issuing a command (e.g. radiate_fsw_cmd(),
        send_and_verify_fsw_cmd()). Increments and prints step count.

        Args
        ----
        func - Calling function name
        cmd - The command being issued via func'''

        self.cmdCounter += 1
        self.subStep = 0

        self._print("{}.{}: {}: {}".format(self.cmdCounter, self.subStep, func, cmd))


    def _command_shell(self,func,val):
        '''Use to record the results of issuing a shell or VxWorks cmd.
        Increments and prints step count.

        Args
        ----
        func - Calling function name
        val - The output of the shell command'''

        self.cmdCounter += 1
        self.subStep = 0

        self._print("{}.{}: {}: Returned:\n{}".format(self.cmdCounter, self.subStep, func, val))


    def _pwr_command(self,func,cmd,pwr,device,switch):
        '''Use when issuing a command (e.g. radiate_fsw_cmd(),
        send_and_verify_fsw_cmd()). Increments and prints step count.

        Args
        ----
        func - Calling function name
        cmd - The command being issued via func
        pwr - Indicates power on/off
        device - Number of device being powered
        switch - Switch being thrown'''

        self.cmdCounter += 1
        self.subStep = 0

        self._print("{}.{}: {}: {} Pwr: {} Dev: {} Switch: {}".format(self.cmdCounter, self.subStep, func, cmd, pwr, device, switch))


    def _evr(self,func,evr):
        '''Use when verifying an EVR. Increments and prints sub-step count.

        Args
        ----
        func - Calling function name
        evr - EVR being verified in func'''

        self.subStep += 1

        self._print("{}.{}: {}: Waiting for EVR: {}".format(self.cmdCounter, self.subStep, func, evr))


    def _eha(self,func,channelName,expect):
        '''Use when verifying an EHA channel. Increments and prints
        sub-step count.

        Args
        ----
        func - Calling function name
        channelName - Name of the EHA channel
        expect - Expected value'''

        self.subStep += 1

        self._print("{}.{}: {}: Verifying EHA: {} Expect: {}".format(self.cmdCounter, self.subStep, func, channelName, expect))


    def _eha_dn(self,func,channelName,dn,mask=None):
        '''Use when accessing an EHA dn value. Increments and prints
        sub-step count as well as obtained value.

        Args
        ----
        func - Calling function name
        channelName - Name of the EHA channel
        dn - DN value'''

        self.subStep += 1

        self._print("{}.{}: {}: Reading EHA: {} Value: {}{}".format(self.cmdCounter, self.subStep, func, channelName, dn, '' if mask is(None) else ' Mask: {}'.format(mask)))


    def _pass(self):
        '''Use when a FIT helper command succeeds the expected operation.
        This function prints a message and increments the success counter.'''

        _var = self.numIgnored if self.ignore else self.numPassed
        _var += 1

        self._print("{}.{}: PASS{}".format(self.cmdCounter,self.subStep, ' (Ignored)' if self.ignore else ''))

        return True


    def _fail(self,reason=None):
        '''Use when a FIT helper command fails the expected operation.
        This function prints a message and increments the fail counter.

        Args
        ----
        reason - Explanation for failure (optional)'''

        _var = self.numIgnored if self.ignore else self.numFailed
        _var += 1

        self._print("{}.{}: FAIL{}{}".format(self.cmdCounter, self.subStep, ' (Ignored)' if self.ignore else '', ' {}'.format(reason) if reason else ''))

        return False



    def _send_cmd(self,cmd,evrName,timeout):
        '''Performs MTAK send_fsw_cmd and checks for basic completion EVRS.

        Args
        ----
        cmd - Command, with args, to send
        evrName - Optional EVR to check for

        Return
        ------
        True if the command was sent and expected EVRs received.'''

        if mtak.wrapper.send_fsw_cmd(cmd,True):

            cmdName = cmd.split(',')[0]

            worked = True

            # Verify the successful command dispatch and completion EVRs
            # are received for the command we just issued. Verify the
            # optional EVR was also received (if provided).

            dispatch = "CMD_EVR_CMD_DISPATCH"
            cmdComp = "CMD_EVR_CMD_COMPLETED_SUCCESS"

            dispatch_patt = re.compile("Dispatch immediate command {} ".format(cmdName))

            cmdComp_patt = re.compile("Successfully completed command {} ".format(cmdName))

            dispatch_evr = self.wait_evr(evrName=dispatch,
                                         lookback=self.defaultLookback,
                                         timeout=timeout)

            if dispatch_evr:
                if not re.search(dispatch_patt,dispatch_evr.message):
                    self._fail("Did not find {} in message: {}".format(dispatch,dispatch_evr.message))
                    worked = False
            else:
                worked = False

            cmdComp_evr = self.wait_evr(evrName=cmdComp,
                                        lookback=self.defaultLookback,
                                        timeout=timeout)

            if cmdComp_evr:
                if not re.search(cmdComp_patt,cmdComp_evr.message):
                    self._fail("Did not find {} in message: {}".format(cmdComp,cmdComp_evr.message))
                    worked = False
            else:
                worked = False

            if evrName:
                if not self.wait_evr(evrName=evrName,
                                     timeout=timeout,
                                     lookback=self.defaultLookback):
                    worked = False

            if worked:
                return True

        return False


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

            config_file = os.path.join(os.environ["MSL_ROOT"], "ptf/scripts/configurations/target/vxsim-6.4.env")

            if not os.path.isfile(config_file):
                self._fail("Cannot find file {}".format(config_file))
                return None

            scripts.helpers.parsers.env_file_parser.EnvFileReader().read_file(config_file)

            subnet = os.environ["TARGET_ip_subnet"]

            session_file = "/dev/shm/vxsim_instances"

            if not os.path.isfile(session_file):
                self._fail("Cannot find file {}".format(session_file))
                return None

            with open(session_file,'r') as ff:
                instance_list = list(map(lambda x: x.strip(), ff.readlines()))

            session = instance_list.pop()

            self.vx_addr = subnet + "." + session

        else:
            # Get the VxWorks IP address for the testbed. The address is
            # fixed for all testbeds. Normal telnet port is used.
            self.vx_addr = "192.9.200.99"

        return self.vx_addr


    #=========================================================================
    # Startup/shutdown functions
    #=========================================================================


    def startup(self):
        '''Starts up the MTAK wrapper. This will automatically connect to
        the current (or last) MPCS session.'''

        self._print("Start of Test")

        mtak.wrapper.startup()


    def shutdown(self):
        '''Shuts down the MTAK wrapper, disconnects from the MPCS session
        and closes the log file and terminates the process. Status code is
        0 if no failures encountered.

        Args
        ----
        (None)

        Return
        ------
        Terminates with 0 exit status if no failures were encountered.'''

        self._print("End of Test")

        self._print("\tNumber of steps   : {}".format(self.cmdCounter))
        self._print("\tNumber of failures: {}".format(self.numFailed))
        self._print("\tNumber ignored    : {}".format(self.numIgnored))

        if self.numFailed > 0:
            self._print("Overall status: FAIL")
        else:
            self._print("Overall status: PASS")

        mtak.wrapper.shutdown()

        self._print("Log file: {}".format(self.logFileName))

        if self.status():
            sys.exit(0)
        else:
            sys.exit(-1)


    def status(self):
        '''Indicates if this test is successful or not.

        Return
        ------
        True if there have been no failures for this test'''

        return True if (self.numFailed == 0) else False


    #=========================================================================
    # Flight system command functions
    #=========================================================================


    def radiate_hw_cmd(self,command):
        '''Sends a hardware command to the flight system. Does not verify
        that the command was received and acted on correctly.

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        command - The hardware command stem to send

        Return
        ------
        True if the command radiated successfully.'''

        func = "radiate_hw_cmd"

        self._command(func,command)

        if mtak.wrappper.send_hw_cmd(command):
            return self._pass()
        else:
            return self._fail()


    def radiate_fsw_cmd(self,command,validate=True):
        '''Sends a command to the flight system. Does not verify that the
        command was received and acted on correctly.

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        command - The command, with comma-separated arguments, to send.
        validate - Flag indicating if command arguments are to be validated
                   against the command dictionary. Default is True.

        Return
        ------
        True if the command radiated successfully.'''

        func = "radiate_fsw_cmd"

        self._command(func,command)

        if mtak.wrapper.send_fsw_cmd(command,validate):
            return self._pass()
        else:
            return self._fail()


    def send_and_verify_fsw_cmd(self,cmd,evrName=None,timeout=mtak.defaultWaitTimeout):
        '''Sends a command to the flight system. Verifies that the expected
        command dispatch and completion EVRs are received. Verifies that the
        specified EVR (if any) is also received.

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        command - The command, with comma-separated arguments, to send.
        evrName - An additional EVR to check for completion.
        timeout - Length of time, in seconds, to wait for the optional EVR

        Return
        ------
        True if the command was sent successfully and all expected EVRs
        received.'''

        func = "send_and_verify_fsw_cmd"

        self._command(func,cmd)

        if evrName:
            self._print("\tAlso using {} for verification".format(evrName))

        return self._send_cmd(cmd,evrName,timeout)


    def send_and_verify_pwr_cmd(self,cmd,power,device,switch,evrName=None,timeout=mtak.defaultWaitTimeout):
        '''Sends a power command to the flight system using the function
        send_and_verify_fsw_cmd(), but also checks for additional PWR EVRs
        for the specific power switch in question.

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        command - The command, with comma-separated arguments, to send.
        power   - If True, turn power on. Otherwise turn power off. Note
                  that the caller must ensure this parameter is consistent
                  with the comma-separated command arguments!
        device  - The number of the device to monitor
        switch  - The power switch to monitor in the PWR EVRs
        evrName - An additional EVR to check for completion.
        timeout - Length of time, in seconds, to wait for the optional EVR

        Return
        ------
        True if the command was sent successfully and all expected EVRs
        received.'''

        func = "send_and_verify_pwr_cmd"

        self._pwr_command(func,cmd,power,device,switch)

        if self._send_cmd(cmd,evrName,timeout):

            worked = True

            # Verify the power switch EVRs.

            if power:
                state = 1
            else:
                state = 0

            sw_upd = "PWR_EVR_SWITCH_UPDATE"

            sw_upd_patt = re.compile("switch {} set to state {}".format(switch,state))

            sw_upd_evr = self.wait_evr(evrName=sw_upd,
                                       lookback=self.defaultLookback,
                                       timeout=timeout)

            if sw_upd_evr:
                if not re.search(sw_upd_patt,sw_upd_evr.message):
                    self._fail("Did not receive {}".format(sw_upd))
                    worked = False
            else:
                worked = False

            prime_off = "PWR_EVR_PRIME_SWITCH_OFF"

            prime_off_patt = re.compile("confirmed off for device {}".format(device))

            if not power:

                prime_off_evr = self.wait_evr(evrName=prime_off,
                                              lookback=self.defaultLookback,
                                              timeout=timeout)

                if prime_off_evr:
                    if not re.search(prime_off_patt,prime_off_evr.message):
                        self._fail("Did not receive {}".format(prime_off))
                        worked = False
                else:
                    worked = False

            if worked:
                return True

        return False


    #=========================================================================
    # Telemetry (EHA and EVR) functions
    #=========================================================================


    def wait_evr(self,evrName=None,eventId=None,level=None,module=None,timeout=mtak.defaultWaitTimeout,lookback=mtak.defaultWaitLookback):
        '''FIT_helper wrapper for MTAK function of same name.
        Pause the script to wait for a particular EVR to arrive.

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        Args
        ----
        evrName - The name of the EVR to wait for. (string)
        eventId - The event ID of the EVR to wait for. (int)
        level - The level of the EVR to wait for. (optional) (string)
        module - The module of the EVR to wait for. (optional) (string)
        timeout - The length of time, in seconds, to wait for the value
            to arrive before timing out. Defaults to 1 min. (optional) (float)
        lookback - The length of time, in seconds, to look back in time for
            telemetry to make sure it didn't already
        arrive before this call was made. Defaults to 0. (optional) (int)

        Return
        ------
        True if the wait condition succeeded, false otherwise (boolean)'''

        func = "wait_evr"

        self._evr(func,evrName)

        evr = mtak.wrapper.wait_evr(name=evrName,
                                    eventId=eventId,
                                    level=level,
                                    module=module,
                                    timeout=timeout,
                                    lookback=lookback)

        if evr:
            self._pass()
        else:
            self._fail()

        return evr


    def wait_eha(self,channelId,dn=None,timeout=mtak.defaultWaitTimeout,lookback=mtak.defaultWaitLookback):
        '''FIT_helper wrapper for MTAK function of same name.
        Pause the script to wait for a particular channel value to arrive.

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        *NOTE* EU is not supported.

        Args
        ----
        channelId - The ID of the channel whose EU value should be
            retrieved. (string)
        dn - The desired DN value of the channel. (optional) (various types)
        timeout - The length of time, in seconds, to wait for
            the value to arrive before timing out. Defaults to
            1 minute. (optional) (float)
        lookback - The length of time, in seconds, to look back in time
            for telemetry to make sure it didn't already
            arrive before this call was made. Defaults to 0. (optional) (int)

        Return
        ------
        True if the wait condition succeeded, false otherwise (boolean)'''

        func = "wait_eha"

        self._eha(func,channelId,dn)

        eha = mtak.wrapper.wait_eha(channelId=channelId,
                                    dn=dn,
                                    timeout=timeout,
                                    lookback=lookback)

        if eha:
            self._pass()
        else:
            # May want to reconsider this logic: If the wait_eha above
            # fails, it could be because the timeout and/or lookback
            # aren't really what we expected. So check the current
            # value, just in case it is correct.
            # Regardless of the above, we do want to print out the
            # current value (if known) to compare with our expected.
            val = mtak.wrapper.get_eha(channelId)
            if val:
                if val.dn == dn:
                    self._pass()
                    return val
                message = "{} is {}".format(channelId,val.dn)
            else:
                message = "{} is {}".format(channelId,None)
            self._fail(message)

        return eha


    def get_eha(self,channelId,timeout=0):
        '''Get the current DN value of a channel. If timeout is non-zero, will
        wait that many seconds for a non-None DN value.

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        Args
        ----
        channelId - The ID of the channel whose DN value should be
            retrieved. (string)
        timeout - Time to try for a non-None value, in seconds

        Return
        ------
        The channel value object of the requested channel or None
        if a value could not be found. (varying types)'''

        func = "get_eha"

        _timeout = timeout

        while _timeout >= 0:
            eha = mtak.wrapper.get_eha(channelId)

            if eha:
                self._eha_dn(func,channelId,eha.dn)
                self._pass()
                return eha

            if self.defSleepTime > 0:
                time.sleep(self.defSleepTime)
            _timeout -= self.defSleepTime

        self._eha_dn(func,channelId,None)
        self._fail("{} is None".format(channelId))
        return None


    def get_eha_dn_from_db(self,channelId):
        '''Queries MPCS for the requested EHA channel and returns the
        DN value if found. This function should be used in those cases
        where it is unclear if the channel has already been pushed in
        the current FIT/MTAK session.

        Callers will need to cast the returned value (a string) to
        whatever type they expect from the channel.

        *NOTE* this function does not record pass/fail. Use wait_eha
        if comparing current channel value to known value. This function
        also does not support a timeout. If you are expecting this channel
        to change, then use get_eha().

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        Args
        ----
        channelId - The ID of the channel whose DN value should be
                    retrieved. (string)

        Return
        ------
        DN value (as type string) of the requested channel if it exists
        in the MPCS database. Otherwise None is returned.'''

        func = "get_eha_dn_from_db"
        db_cmd = [os.path.join(os.environ["CHILL_GDS"], 'bin/chill_get_chanvals'), '-K',str(mtak.wrapper.get_session_config().key), '-z', str(channelId)]
        _proc = subprocess.Popen(' '.join(db_cmd), stdout=subproccess.PIPE, stderr=subprocess.PIPE, shell=True)
        output, error = _proc.communicate()
        status = _proc.returncode
        output = output.decode('utf-8') if isinstance(output, bytes) else output
        error = error.decode('utf-8') if isinstance(error, bytes) else error

        if output.count(channelId) != 0:
            try:
                all = output.split()
                latest = all[-1]
                vals = latest.split('=')
                self._eha_dn(func,channelId,vals[1])
                self._pass()
                return vals[1]
            except:
                pass

        self._eha_dn(func,channelId,str("N/A"))
        self._fail("No value found for {}".format(channelId))

        return None


    def get_eha_dn_mask(self,channelId,mask):
        '''Queries MPCS for the requested EHA channel and returns the
        DN value bit-wise and-ed with a specified mask.

        Return value is the integer DN value or None if not found.

        NOTE: This routine does not record pass/fail and does not increment
        the step counter.

        Args
        ----
        channelId - The ID of the channel whose DN value should be
                    retrieved. (string)
        mask - A hexadecimal value (of the form 0xYYYY), where a 1 specifies
               the bit to be checked.

        Return
        ------
        integer: value of the channel DN bit-wise and-ed with the mask,
                 or None if the channel did not have a value '''

        func = "get_eha_dn_mask"

        db_cmd = [os.path.join(os.environ["CHILL_GDS"], 'bin/chill_get_chanvals'), '-K',str(mtak.wrapper.get_session_config().key), '-z', str(channelId)]
        _proc = subprocess.Popen(' '.join(db_cmd), stdout=subproccess.PIPE, stderr=subprocess.PIPE, shell=True)
        output, error = _proc.communicate()
        status = _proc.returncode
        output = output.decode('utf-8') if isinstance(output, bytes) else output
        error = error.decode('utf-8') if isinstance(error, bytes) else error

        if output.count(channelId) != 0:
            try:
                all = output.split()
                latest = all[-1]
                vals = latest.split('=')
                self._eha_dn(func,channelId,vals[1])

                return int(vals[1]) & mask

            except Exception:
                print('{}'.format(traceback.format_exc()))

        self._eha_dn(func,channelId,str("N/A"))
        self._fail("No value found for {}".format(channelId))

        return None


    def wait_eha_dn_mask(self,channelId,mask,dn,timeout=mtak.defaultWaitTimeout):
        '''This function monitors the specified EHA channel within the
        timeout period. For every bit set in the mask it checks to see if
        the expected dn bit value matches the actual channel DN bit value.

        Args
        ----
        channelId - The ID of the channel to monitor
        mask - A bit mask identifying the bits of interest
        dn - The expected values of the DN value. Only
             those bits listed in the mask will be compared.
        timeout - The length of time, in seconds, to wait for
            the value to arrive before timing out. Defaults to
            1 minute. (optional) (float)

        Returns
        -------
        DN if the channel's DN value matches the masked bit pattern
        within the timeout period, otherwise None'''

        # Basic algorithm:
        # Use get_eha to obtain the current DN value. Do a bitwise
        # comparison with the mask and expected value. If they match,
        # return the DN. If they don't, sleep, then try again. If
        # we sleep as much as the timeout and never match, return None.

        func = "wait_eha_dn_mask"

        self._eha_dn(func,channelId,dn,mask)

        succeed = False
        eha = None

        dn_int = int(dn)
        mask_int = int(mask)

        _timeout = timeout

        while _timeout >= 0:

            eha = mtak.wrapper.get_eha(channelId)
            if eha:
                eha_int = int(eha.dn)

                if (mask_int & eha_int) == dn_int:
                    succeed = True
                    break

            if self.defSleepTime > 0:
                time.sleep(self.defSleepTime)
            _timeout -= self.defSleepTime

        if succeed:
            if eha:
                self._pass()
                return eha
            else:
                self._fail()
                return None
        else:
            if eha:
                self._fail("Value {} did not match expected".format(eha))
            else:
                self._fail()
            return None


    #=========================================================================
    # Shell and VxWorks functions
    #=========================================================================


    def shell_cmd(self,command):
        '''FIT_helper wrapper for MTAK function of same name.
        Execute a UNIX shell command, wait for it to finish and return the
        result in a string. This function executes the command on the host
        workstation, not the flight CPU VxWorks environment.

        *NOTE* Callers must independently invoke the pass/fail method after
        using the function!

        Increments the step counter for logging: <testname> <step>.<sub>

        Args
        ----
        command - The full UNIX command (with arguments) (string)

        Return
        ------
        String containing the output from the executed command (string)'''

        func = "shell_cmd"

        self._command(func,command)

        stat = mtak.wrapper.shell_cmd(command)

        self._command_shell(func,stat)

        if stat:
            self._pass()
        else:
            self._fail()

        return stat


    def _get_sse_host(self):
        """
        Obtains the SSE workstation host name by one of two methods:
        1) If the environment variable FIT_SSE_HOST is set it is used
        2) Assumes you are on a testbed GDS workstation whose name is
           of the form 'msl<testbed>gds1', and from there assembles a
           name 'msl<testbed>sse'.
        """

        _sse_host = os.environ.get("FIT_SSE_HOST")
        if _sse_host:
            return _sse_host

        my_host = platform.node().split('.')[0]

        if my_host.startswith("msl") and my_host.endswith("gds1"):
            my_testbed = my_host[:len(my_host)-4]
            return '{}sse'.format(my_testbed)
        else:
            return "BAD_HOST: {}".format(my_host)


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

            _cmd = ["ssh","-tt","-i",private_key, my_sse_ws, "telnet","-r", addr]
            ssh_cmd=' '.join(_cmd)

            self.func_log(func,"SSH cmd   : {}".format(ssh_cmd))
            self.func_log(func,"Telnet cmd: {}".format(cmd))

            p = subprocess.Popen(ssh_cmd,
                shell=True,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE)

            # Let the SSH/Telnet connection get established.
            time.sleep(3)

            # Issue the caller's command, and delay for a bit.
            p.stdin.write('{}\n'.format(cmd))

            time.sleep(delay)

            # Terminate the SSH/telnet session and return all the output.
            out, err = p.communicate("exit\n")

            self._command_shell(func,out)

            return out

        except OSError as e:
            self._fail("Error in ssh/telnet to VxWorks: {}".format(e))
        except Exception as err:
            self._fail("Unk error in ssh/telnet to VxWorks: {}".format(err))

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

        self._command(func,cmd)

        addr = self._get_vxworks_ip_addr()

        if not addr:
            self._fail("Did not get IP address")
            return None

        if mtak.wrapper.get_session_config().venueType == "TESTSET":

            # We're on WSTS. Just use normal telnet.
            self.func_log(func, "Establishing telnet to WSTS {}".format(self.vx_addr))

            tn = telnetlib.Telnet(self.vx_addr)
            tn.read_until('->')
            tn.write('{}\n'.format(cmd))

            val = tn.read_until("Hopefully this string won't match",delay)

            self._command_shell(func,val)

            tn.close()
            return val

        else:

            # We're on a testbed. Need to do SSH to the SSE, then telnet.
            return self._ssh_to_vxworks(cmd,addr,delay)


    #=========================================================================
    # Manual pass/fail functions
    #=========================================================================


    def passStep(self):
        '''Manually mark the current command step as PASS. Use
        after issuing shell commands.

        Args
        ----
        None

        Return
        ------
        None'''

        self._pass()


    def failStep(self):
        '''Manually mark the current command step as FAIL. Use
        after issuing shell commands.

        Args
        ----
        None

        Return
        ------
        None'''

        self._fail()


    def incrementStep(self):
        '''Manually increment the current command step.

        Args
        ----
        None

        Return
        ------
        New command step count'''

        self.cmdCounter += 1

        return self.cmdCounter


    def incrementSubStep(self):
        '''Manually increment the current sub step.

        Args
        ----
        None

        Return
        ------
        New sub step count'''

        self.subStep += 1

        return self.subStep


    def start_ignore(self,reason,venue="All"):
        '''Instructs the FIT_Helper functions to NOT evaluate PASS/FAIL status
        on any command/step while the ignore is in effect. The venue parameter
        identifies a specific platform, or all platforms, for which the ignore
        should be enforced.

        Args
        ----
        reason - String explaining the reason for ignoring the result of steps
        venue - String containing MTAK venue type or "All"

        Return
        ------
        None'''

        func = "start_ignore"

        self.func_log(func,"Start ignoring: {}".format(reason))

        if venue in [mtak.wrapper.get_session_config().venueType, "All"]:
            self.ignore = True


    def end_ignore(self):
        '''Instructs the FIT_Helper functions to stop ignoring the PASS/FAIL
        criteria. Terminates the 'ignore' status for all venues, even if start_ignore()
        was called multiple times.

        Args
        ----
        None

        Return
        ------
        None'''

        func = "end_ignore"

        self.func_log(func,"End ignore")

        self.ignore = False


    #=========================================================================
    # Convenience functions for I/O and logging
    #=========================================================================


    def wait_for_key_press(self,message='Press any key to continue...'):
        '''Convenience function that will pause script execution until
        the user presses a key. If non-interactive, will just wait
        self.defaultTimeout seconds.

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        *NOTE* Use manual pass/fail functions to indicate status if
        necessary.

        Args
        ----
        message - The message to display to the user (string)

        Return
        ------
        None'''

        func = "wait_for_key_press"

        self.subStep += 1

        self._print("{}.{}: {}: {}".format(self.cmdCounter, self.subStep, func, message))

        if self.interactive:
            mtak.wrapper.wait_for_key_press("")
        else:
            mtak.wrapper.wait(self.defaultTimeout)


    def prompt(self,message='Please enter a value: '):
        '''Convenience function to pause the script for the user to
        enter the requested value. If non-interactive, will return
        a 'y' for affirmative.

        Increments the sub-step counter for logging: <testname> <step>.<sub>

        *NOTE* Use manual pass/fail functions to indicate status if
        necessary.

        Args
        ----
        message - The message to display to the user (string)

        Return
        ------
        Value entered by the user or 'y' if non-interactive (string)'''

        func = "prompt"

        self.subStep += 1

        self._print("{}.{}: {}: {}".format(self.cmdCounter, self.subStep, func, message))

        if self.interactive:
            val = sys.stdin.readline()[:-1]
            self._print("{}.{}: {}: user entered: {}".format(self.cmdCounter,self.subStep,val))
            return val
        else:
            self._print("{}.{}: {}: auto-reply: {}".format(self.cmdCounter,self.subStep,func,"y"))
            return "y"


    def log(self,message,level=None):
        '''FIT_helper wrapper for MTAK function of same name.
        Write a message to the MTAK log file.  NOTE: The message in
        the log file will be prepended with the text "User Log Message: "
        to distinguish it from normal log messages.

        Args
        ----
        message - The message to write to the log. (string)
        level - The logging level of the message (e.g. DEBUG,INFO,
            WARNING,ERROR,CRITICAL). Defaults to INFO in MTAK log and nothing
            in FIT log. (optional) (string)'''

        return self.func_log("log", message, level)

    def func_log(self,func,message,level=None):
        """
        Write a log message to the FIT and MTAK logs, similar to log(). Allows
        caller to specify the calling function name. This is intended to allow higher
        level utility layers to be identified in the log entries directly.

        Args
        ----
        func - Calling function name
        message - The string to write to the log file
        level - The logging level of the message (e.g. DEBUG,INFO,
            WARNING,ERROR,CRITICAL). Defaults to INFO in MTAK log and nothing
            in FIT log. (optional) (string)
        """

        _level=level if level else "INFO"
        self._print("{}.{}: {}: {}{}".format(self.cmdCounter,self.subStep,func,message,' ({})'.format(level) if level else ''))
        mtak.wrapper.log(message, _level)


    def hasString(self,str,substr):
        '''Utility function that will return True if the substring argument
        is contained within the string. Increments the sub-step counter
        and registers PASS/FAIL.

        Args
        ----
        str - String to check
        substr - Substring to check for in the string

        Return
        ------
        True if substr is in str'''

        func = "hasString"

        self.subStep += 1

        self._print("{}.{}: {}: Checking if string \"{}\" contains \"{}\"".format(self.cmdCounter,self.subStep,func,str,substr))

        if str.rfind(substr):
            self._pass()
            return True
        else:
            self._fail()
            return False

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
