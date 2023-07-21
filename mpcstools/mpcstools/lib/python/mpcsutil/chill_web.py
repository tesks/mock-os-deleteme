#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mpcsutil import err
import logging
import sys
import requests
import signal
import time
import json
import webbrowser
import distutils.util
import mpcsutil
import os
from requests.adapters import HTTPAdapter

logger = logging.getLogger('mpcs.chill_web')

GET = "GET"
POST = "POST"

GUI = "gui"
INGEST = "ingest"
PROCESS = "process"
supportedServices = [GUI, INGEST, PROCESS]

CREATE = "create"
ATTACH = "attach"
START = "start"
STOP = "stop"
ABORT = "abort"
RELEASE = "release"
SHUTDOWN = "shutdown"

QUERY = "query"
STATUS = "status"
STATE = "state"
TELEM = "telem"

supportedEndpoints = [
    CREATE, ATTACH, START, STOP,
    STATE, QUERY, STATUS, TELEM,
    ABORT, RELEASE,
    SHUTDOWN
]

endpointMap = {
    CREATE: "/control/session/create",
    ATTACH: "/control/attach",
    START: "/control/start",
    STOP: "/control/stop",
    ABORT: "/control/abort",
    QUERY: "/control/query",
    STATUS: "/status",
    TELEM: "/status/telem",
    RELEASE: "/control/release",
    SHUTDOWN: "/shutdown",
    STATE: "/status/state"
}

WORKER_INIT = "INITIALIZING"
WORKER_READY = "READY"
WORKER_STARTING = "STARTING"
WORKER_ACTIVE = "ACTIVE"
WORKER_STOPPING = "STOPPING"
WORKER_STOPPED = "STOPPED"
WORKER_ABORTING = "ABORTING"
workerStates = [
    WORKER_INIT,
    WORKER_READY,
    WORKER_ACTIVE,
    WORKER_STOPPED,
    WORKER_STOPPING,
    WORKER_ABORTING
]


def build_session_request_params(key, host, fragment=None, args=None):
    """
    Helper method for building session request parameters to provide with a request

    :param key: The key identifier
    :param host: The host identifier
    :param fragment: The fragment identifier; defaults to None
    :param args: Additional arguments to add to the return param dict; defaults to None

    :return: A param dict of request parameters
    """
    params = {}
    if host:
        params['sessionKey'] = key
    if host:
        params['sessionHost'] = host
    if fragment:
        params['sessionFragment'] = fragment

    if args is not None:
        extended = params.copy()
        extended.update(args)
        return extended

    return params


def get_worker_directive_from_response(response):
    """
    Gets the next worker directive to execute based on the current state response
    :param response: Worker state response
    :return: next worker directive to execute
    """
    directive = None
    if response is None:
        logger.debug("Returning directive None, response is None")
        return None
    elif response.status_code != 200:
        # MCSECLIV-974: Make sure we didn't get invalid response
        # 404 Client Error is returned when worker no longer exists (already released)
        logger.debug("Returning directive %s, response is %s\n%s" % (directive, response.status_code, response.text))
        return directive

    current_state = response.json()['value']
    if current_state == WORKER_ACTIVE:
        directive = STOP
    elif current_state == WORKER_INIT or current_state == WORKER_STARTING or current_state == WORKER_READY:
        directive = ABORT
    elif current_state == WORKER_STOPPED:
        directive = RELEASE
    elif current_state == WORKER_STOPPING or current_state == WORKER_ABORTING:
        time.sleep(3)
        directive = RELEASE

    logger.debug("Returning directive %s because current state is %s " % (directive, current_state))
    return directive


def _create_cli_options():
    """
    Creates command-line options for this application to use
    :return: parser
    """
    parser = mpcsutil.create_option_parser()

    # optparse already automatically has its own --help and --version options
    parser.add_option("", "--autoStart", action="store_true", dest="autoStart")
    parser.add_option("-N", "--sessionConfig", action="store", dest="sessionConfig")
    parser.add_option("-H", "--noGUI", action="store_true", dest="noGUI")

    return parser


class ChillWebApp(HTTPAdapter):
    """
    The chill_web application
    """
    _shutdown = False

    def exit_handler(self, signum, frame):
        """
        Sets the shutdown flag to true so the application terminates
        and executes the shutdown sequence

        :param signum: signal int
        :param frame: signal term
        """
        if self.get_shutdown_status() is False:
            self.set_shutdown_status(True)
            logger.info("Integrated chill_web received a shutdown request.  Shutting down gracefully..")

            self.stop_downlink()
            logger.info("Integrated chill_web shutdown is finished")

    @property
    def ca_bundle(self):
        if self.__dict__.get('_ca_bundle') is None:
            self.ca_bundle = os.path.join(os.path.dirname(
                self.gdsConfig.getProperty('server.ssl.trust-store', '/etc/pki/tls/certs/ca-bundle.crt')),
                'ca-bundle.crt')
        return self.__dict__.get('_ca_bundle')

    @ca_bundle.setter
    def ca_bundle(self, value):
        self.__dict__.update({'_ca_bundle': value})

    def __init__(self):
        HTTPAdapter.__init__(self)

        # Register shutdown hooks for ctrl+c
        signal.signal(signal.SIGINT, self.exit_handler)
        signal.signal(signal.SIGTERM, self.exit_handler)

        self.gdsConfig = mpcsutil.config.GdsConfig()

        self.webUrl = self.gdsConfig.getProperty('webGui.url')
        self.webBrowser = self.gdsConfig.getProperty('webGui.browser')
        self.fswIngestUrl = self.gdsConfig.getProperty('webGui.fswIngest.url')
        self.sseIngestUrl = self.gdsConfig.getProperty('webGui.sseIngest.url')
        self.fswProcessUrl = self.gdsConfig.getProperty('webGui.fswProcess.url')
        self.sseProcessUrl = self.gdsConfig.getProperty('webGui.sseProcess.url')
        self._https = "https" in self.webUrl

        self._sessionConfig = None

        self.autoStart = False
        self.noGUI = False
        self.runSse = False
        self.runFsw = False

        self.sessionConfigFile = None
        self.sessionKey = None
        self.sessionHost = None

        self.fswIngestId = None
        self.sseIngestId = None
        self.fswProcessId = None
        self.sseProcessId = None

        self.fswIngestFragment = None
        self.sseIngestFragment = None
        self.fswProcessFragment = None
        self.sseProcessFragment = None

        self.workerMap = {}

        # HTTPS support
        if self._https is True:
            _session = requests.Session()
            _session.verify = self.ca_bundle
            _session.mount(self.webUrl, self)

    def _get_service_url(self, service, sse):
        """
        Gets the configured URL for a web service
        :param service: The web service to get a URL for
        :param sse: whether or not the service is sse
        :return: The configured service URL
        """
        if service == GUI:
            return self.webUrl
        elif service == INGEST:
            return self.sseIngestUrl if sse is True else self.fswIngestUrl
        elif service == PROCESS:
            return self.sseProcessUrl if sse is True else self.fswProcessUrl

    def _make_request(self, method, service, endpoint, sse=False, params=None, **kwargs):
        """
        Helper method to execute and log requests

        :param method: The REST method, e.g. GET
        :param service: The service to make a request to
        :param endpoint: The endpoint directive (operation)
        :param sse: Whether or not the service is sse, defaults to False if not provided
        :param params: Parameter arguments to provide with the request; defaults to None
        :param kwargs: Kwargs to provide with the request (optional)
        :return: The request response or None if it was not successful
        """
        if method is None:
            logger.error("Request method not specified, request aborted")
            return
        if service is None:
            logger.error('Destination service not specified, request aborted')
            return
        if service not in supportedServices:
            logger.error(
                service + " is not a supported service. Valid values are: " + supportedServices + ", request aborted")
            return
        if endpoint is None:
            logger.error('endpoint not specified, request aborted')
            return
        if endpoint not in supportedEndpoints:
            logger.error(
                endpoint + " is not a supported endpoint. Valid values are: " + supportedEndpoints + ", request aborted")
            return

        if params is None:
            logger.debug("Parameters not specified")

        full_url = self._get_service_url(service, sse) + endpointMap[endpoint]

        logger.debug("Requesting %s to URL %s with parameters %s"
                     % (method, full_url, "" if params is None else params))

        _dd = dict(kwargs, method=method, url=full_url, params=params)
        if self._https:
            _dd.update(dict(verify=self.ca_bundle))

        with requests.Session():
            response = requests.Session().request(**_dd)

        try:
            logger.debug(json.dumps(json.loads(response.text), indent=2))
        except Exception as e1:
            logger.debug(e1)  # Response might not return JSON

        return response

    def build_session_attach_params(self, sse=False):
        """
        Helper method for building session attach parameters
        :param sse: whether or not we are attaching to an SSE service
        :return: param dict with attach parameters
        """
        params = {}

        if self._sessionConfig.venueType:
            params['venueType'] = self._sessionConfig.venueType

        if self._sessionConfig.databaseSessionKey:
            params['dbSourceKey'] = self._sessionConfig.databaseSessionKey
        if self._sessionConfig.databaseSessionHost:
            params['dbSourceHost'] = self._sessionConfig.databaseSessionHost
        if self._sessionConfig.jmsSubtopic:
            params['subtopic'] = self._sessionConfig.jmsSubtopic
        if self._sessionConfig.outputDirectory:
            params['outputDir'] = self._sessionConfig.outputDirectory
        if self._sessionConfig.spacecraftId:
            params['spacecraftID'] = self._sessionConfig.spacecraftId

        if self._sessionConfig.name:
            params['sessionName'] = self._sessionConfig.name
        if self._sessionConfig.description:
            params['sessionDescription'] = self._sessionConfig.description
        if self._sessionConfig.user:
            params['sessionUser'] = self._sessionConfig.user
        if self._sessionConfig.type:
            params['sessionType'] = self._sessionConfig.type
        if self._sessionConfig.sessionVcid:
            params['sessionVcid'] = self._sessionConfig.sessionVcid
        if self._sessionConfig.sessionDssId:
            params['sessionDssId'] = self._sessionConfig.sessionDssId

        if sse:
            params['sseDownlinkHost'] = self._sessionConfig.sseHost
            params['sseDownlinkPort'] = self._sessionConfig.sseDownlinkPort
            params['sseDictionaryDir'] = self._sessionConfig.sseDictionaryDirectory
            params['sseVersion'] = self._sessionConfig.sseVersion
        else:
            params['fswDownlinkHost'] = self._sessionConfig.fswDownlinkHost
            params['fswDownlinkPort'] = self._sessionConfig.fswDownlinkPort
            params['fswDictionaryDir'] = self._sessionConfig.fswDictionaryDirectory
            params['fswVersion'] = self._sessionConfig.fswVersion

            if self._sessionConfig.downlinkConnectionType:
                params['downlinkConnectionType'] = self._sessionConfig.downlinkConnectionType
            if self._sessionConfig.inputFormat:
                params['inputFormat'] = self._sessionConfig.inputFormat
            if self._sessionConfig.inputFile is not None:
                params['inputFile'] = self._sessionConfig.inputFile

        if "applicable" not in self._sessionConfig.downlinkStreamId:
            params['downlinkStreamId'] = self._sessionConfig.downlinkStreamId

        if self._sessionConfig.topic:
            params['publishTopic'] = self._sessionConfig.topic

        return params

    def _ping_services(self):
        """
        Pings the services this application should talk with
        Failure to ping FSW/SSE service when we are running it will result in application shutdown
        """
        logger.info("Checking service(s) status... RunFsw is %s & RunSse is %s"
                    % ("True" if self.runFsw == 1 else "False", "True" if self.runSse == 1 else "False"))

        if self.runFsw:
            logger.info("Checking FSW services status")
            try:
                resp = self._make_request(GET, INGEST, STATUS).json()
                logger.info("PING FSW INGEST: %s", resp['value'])
            except Exception as e:
                logger.error("Unable to ping FSW Ingest: %s" % e)
                raise e

            try:
                resp = self._make_request(GET, PROCESS, STATUS).json()
                logger.info("PING FSW PROCESS: %s", resp['value'])
            except Exception as e:
                logger.error("Unable to ping FSW Process: %s" % e)
                raise e
        else:
            logger.info("Skipping FSW setup")

        if self.runSse:
            logger.info("Checking SSE services status")
            try:
                resp = self._make_request(GET, INGEST, STATUS, sse=True).json()
                logger.info("PING SSE INGEST: %s", resp['value'])
            except Exception as e:
                logger.error("Unable to ping SSE Ingest: %s" % e)
                raise e

            try:
                resp = self._make_request(GET, PROCESS, STATUS, sse=True).json()
                logger.info("PING SSE PROCESS: %s", resp['value'])
            except Exception as e:
                logger.error("Unable to ping SSE Ingest: %s" % e)
                raise e
        else:
            logger.info("Skipping SSE setup")


    def launch_mcgui(self):
        """
        Handles launching the web browser GUI
            ## MPCS-11643 - Make browser configurable
            ## MPCS-12303 - 10/2021: chill_down_web support for noGUI mode
            ## MPCS-12303 - 12/2021: chill_web also supports noGUI for WSTS
        """
        if self.noGUI is True:
            logger.info("Found --noGUI option, skipping MCGUI launch! "
                        "Navigate to %s in your browser for monitoring downlink status " % self.webUrl)
        else:
            webbrowser.get(self.webBrowser).open(self.webUrl)

    def setup_services(self):
        """
        Handles the pinging/setting up of all telemetry services on startup
        """
        self.launch_mcgui()

        self._ping_services()

        logger.info("Setting up telemetry services...")

        if self.runFsw:
            self._attach_worker(PROCESS)
            self._attach_worker(INGEST)

        if self.runSse:
            self._attach_worker(PROCESS, sse=True)
            self._attach_worker(INGEST, sse=True)

    def start_downlink(self):
        """
        Handles starting ingest/process services
        """
        logger.info("Starting downlink...")
        if self.runFsw:
            self.start_worker(PROCESS, self.fswProcessFragment)
        if self.runSse:
            self.start_worker(PROCESS, self.sseProcessFragment, sse=True)

        if self.runFsw:
            self.start_worker(INGEST, self.fswIngestFragment)
        if self.runSse:
            self.start_worker(INGEST, self.sseIngestFragment, sse=True)

    def start_worker(self, service, fragment, sse=False):
        """
        Method for sending START directive to a telemetry worker
        :param service: The service we are interacting with
        :param fragment: The worker fragment
        :param sse: Whether or not the service is sse
        """
        try:
            logger.info("STARTING %s %s with key %s on host %s and fragment %s"
                        % ("SSE" if sse else "FSW", service, self.sessionKey, self.sessionHost, fragment))
            resp = self._make_request(POST, service, START, sse, params=build_session_request_params(
                key=self.sessionKey,
                host=self.sessionHost,
                fragment=fragment))

            logger.debug("Response from STARTING %s %s is %s" % (service, "SSE" if sse else "FSW", resp))
        except err.InvalidStateError as e1:
            logger.error("Encountered an unexpected exception starting %s %s to  %s "
                         % ("SSE" if sse else "FSW", service, e1))
            raise e1

    def _attach_worker(self, service, sse=False):
        """
        Method for sending ATTACH directive to a telemetry service
        :param service: The service we are attaching to
        :param sse: Whether or not the service is sse
        """
        # POST /process/control/session/attach (key)
        logger.info("ATTACHING %s %s with key %s on host %s"
                    % ("SSE" if sse else "FSW", service, self.sessionKey, self.sessionHost))

        resp = None
        try:
            resp = self._make_request(POST, service, ATTACH, sse,
                                      params=build_session_request_params(
                                          self.sessionKey,
                                          self.sessionHost,
                                          args=self.build_session_attach_params(sse))
                                      ).json()

            worker_id = resp['workerId'].get('id')
            fragment = resp['workerId'].get('fragment')
            logger.debug("Response from ATTACHING %s %s is %s" % (service, "SSE" if sse else "FSW", worker_id))

            if service == INGEST:
                if sse:
                    self.sseIngestId = worker_id
                    self.sseIngestFragment = fragment
                    self.workerMap.update({"sseIngest": worker_id})
                else:
                    self.fswIngestId = worker_id
                    self.fswIngestFragment = fragment
                    self.workerMap.update({"fswIngest": worker_id})

            elif service == PROCESS:
                if sse:
                    self.sseProcessId = worker_id
                    self.sseProcessFragment = fragment
                    self.workerMap.update({"sseProcess": worker_id})
                else:
                    self.fswProcessId = worker_id
                    self.fswProcessFragment = fragment
                    self.workerMap.update({"fswProcess": worker_id})
        except err.InvalidStateError as e1:
            logger.error("Encountered an unexpected exception attaching %s %s %s "
                         % (service, "SSE" if sse else "FSW", e1))
            raise e1

    def _clean_worker(self, worker, directive):
        """
        Method to handle worker cleanup (Stopping or releasing)

        :param worker: The worker to clean up
        :param directive: The directive to execute (STOP, RELEASE, or ABORT)
        """
        if directive is None:
            return
        sse = True if "sse" in worker else False
        is_sse = "SSE" if sse else "FSW"
        service = INGEST if "Ingest" in worker else PROCESS

        if directive == RELEASE:
            logger.info("Gathering %s %s statistics" % (is_sse, service))
            self.get_worker_stats(worker)
        try:
            fragment = self.get_worker_fragment(worker)
            logger.info("%s %s %s with key %s on host %s and fragment %s"
                        % (directive, is_sse, service, self.sessionKey, self.sessionHost, fragment))
            resp = self._make_request(POST, service, directive, sse,
                                      params=build_session_request_params(
                                          key=self.sessionKey,
                                          host=self.sessionHost,
                                          fragment=fragment))

            logger.debug("Response from %s %s %s is %s" % (directive, is_sse, service, resp))

        except err.InvalidStateError as e:
            logger.debug("Encountered an unexpected exception %s %s %s" % (directive, is_sse, service))
            raise e

    def get_worker_fragment(self, worker):
        """
        Gets the fragment from a worker in the map
        :param worker: the telemetry worker
        :return: session fragment for the worker
        """
        return self.workerMap.get(worker).rsplit('/', 1)[-1]

    def get_telemetry_stats(self):
        """
        Method for gathering telemetry statistics for each telemetry worker
        """
        for worker in self.workerMap.copy():
            return self.get_worker_stats(self.workerMap.get(worker))

    def get_worker_stats(self, worker):
        if worker is None:
            return
        try:
            resp = self._make_request(GET,
                                      INGEST if "Ingest" in worker else PROCESS,
                                      TELEM,
                                      sse=True if "sse" in worker else False,
                                      params=build_session_request_params(
                                          key=self.sessionKey,
                                          host=self.sessionHost,
                                          fragment=self.get_worker_fragment(worker)))

            logger.info("%s %s Summary: %s ", worker, TELEM, json.dumps(json.loads(resp.text), indent=2))
        except Exception as e:
            logger.error("Encountered an unexpected exception getting %s summary: %s", worker, e)

    def get_worker_state(self, worker):
        sse = True if "sse" in worker else False
        is_sse = "SSE" if sse else "FSW"
        service = INGEST if "Ingest" in worker else PROCESS
        try:
            fragment = self.get_worker_fragment(worker)
            logger.debug("%s %s %s with key %s on host %s and fragment %s"
                         % (STATE, is_sse, service, self.sessionKey, self.sessionHost, fragment))
            resp = self._make_request(GET, service, STATE, sse,
                                      params=build_session_request_params(key=self.sessionKey,
                                                                          host=self.sessionHost,
                                                                          fragment=fragment))

            logger.debug("Response from %s %s %s is %s" % (STATE, is_sse, service, resp))
            return resp
        except Exception as e:
            logger.debug("Encountered an unexpected exception %s %s %s: %s" % (STATE, is_sse, service, e))
            return None

    def _shutdown_ingest_and_process(self, done_ingest, done_process):
        """
        Helper function to shut down workers based on their current state
        Used by the shutdown logic controller
        :param done_ingest: whether we've finished shutting down ingest workers
        :param done_process: whether we've finished shutting down process workers
        :return:
        """
        for worker in self.workerMap.copy():
            # First, clean up ingest
            if not done_ingest:
                if "Ingest" in worker:
                    directive = self.get_new_worker_directive(worker)
                    while directive is not None:
                        self._clean_worker(worker, directive)
                        directive = self.get_new_worker_directive(worker)
                    del self.workerMap[worker]
            else:
                # Now clean up processing
                if not done_process and "Process" in worker:
                    directive = self.get_new_worker_directive(worker)
                    while directive is not None:
                        self._clean_worker(worker, directive)
                        directive = self.get_new_worker_directive(worker)
                    del self.workerMap[worker]


    def get_new_worker_directive(self, worker):
        """
        Gets the new worker directive from its current status
        :param worker: worker to get directive for
        :return: new directive to exeute
        """
        return get_worker_directive_from_response(self.get_worker_state(worker))


    def stop_downlink(self):
        """
        Method for bringing down downlink workers and displaying telemetry statistics
        """
        logger.info("Stopping downlink...")
        logger.debug("CLEANING UP: WORKER MAP %s " % self.workerMap)

        # TODO: worker map can be empty if user ctrl+c before ATTACH is finished
        # how to resolve? Maybe query server for workers on the session ?
        while True:
            done_ingest = True
            done_process = True

            # redundant, certainly better way to do this
            # for now it works better than what existed
            if self.workerMap:
                for worker in self.workerMap.copy():
                    if "Ingest" in worker:
                        done_ingest = False
                        done_process = False
                        break
                    elif "Process" in worker:
                        done_process = False
                if done_ingest and done_process:
                    self.workerMap.clear()
                    break
            else:  # Only execute cleanup stuff if the worker map was populated
                self.workerMap.clear()
                break
            self._shutdown_ingest_and_process(done_ingest, done_process)

        logger.debug("FINISHED CLEANING UP: WORKER MAP %s " % self.workerMap)

    def get_shutdown_status(self):
        """
        Gets the shutdown flag
        :return: The shutdown flag status
        """
        return self._shutdown

    def set_shutdown_status(self, status):
        """
        Sets the shutdown flag
        :param status: True if we are shutting down; False otherwise
        """
        self._shutdown = status

    def parse_cli(self):
        """
        Parses command-line options and sets arguments into necessary variables
        """
        parser = _create_cli_options()

        (_options, _args) = parser.parse_args()

        if _options.autoStart:
            self.autoStart = True

        if _options.noGUI:
            self.noGUI = True

        self.sessionConfigFile = _options.sessionConfig

        if self.sessionConfigFile:
            self._sessionConfig = mpcsutil.config.SessionConfig(filename=self.sessionConfigFile, validate_session=True)
            self.sessionKey = self._sessionConfig.number
            self.sessionHost = self._sessionConfig.host
            self.runSse = distutils.util.strtobool(self._sessionConfig.runSseDownlink)
            self.runFsw = distutils.util.strtobool(self._sessionConfig.runFswDownlink)

        else:
            raise err.InvalidInitError("Failed initialization from missing session config file (--sessionConfig)")

        logger.debug("autoRun is %s " % self.autoStart)
        logger.debug("noGUI is %s " % self.noGUI)
        logger.debug("sessionConfigFile is %s " % self.sessionConfigFile)
        logger.debug("runSse is %s and fsw is %s" % (self.runSse, self.runFsw))
        logger.debug("session key is %s " % self.sessionKey)
        logger.debug("session host is %s \n" % self.sessionHost)


def test():
    app = ChillWebApp()

    try:
        app.parse_cli()
    except Exception as e:
        logger.error("Unable to parse command-line arguments %s", e)
        sys.exit(1)

    try:
        app.setup_services()
    except Exception as e:
        logger.error("Unexpected exception setting up services: %s " % e)
        app.set_shutdown_status(True)

    if app.autoStart and app.get_shutdown_status() is not True:
        logger.info("--autoStart was provided, attempting to start...")
        try:
            app.start_downlink()
        except Exception as e:
            logger.debug("Unexpected exception: %s " % e)
            app.set_shutdown_status(True)

    elif app.get_shutdown_status() is not True:
        logger.info("CLI option --autoStart not provided! Work can be started using the M&C GUI (%s) or CLI clients" %
                    app.webUrl)

    logger.debug("App shutdown status is %s " % app.get_shutdown_status())

    if app.get_shutdown_status() is not True:
        while True:
            time.sleep(1)
            if app.get_shutdown_status():
                break
    else:
        logger.info("Received shutdown request...")

    logger.info("Exiting chill_web python script")


def main():
    return test()


if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
