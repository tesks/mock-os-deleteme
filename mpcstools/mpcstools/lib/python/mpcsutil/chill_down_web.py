#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import sys
import time
import mpcsutil
import logging
from mpcsutil import chill_web

logger = logging.getLogger('mpcs.chill_web')

class ChillDownWebApp(mpcsutil.chill_web.ChillWebApp):
    def __init__(self):
        mpcsutil.chill_web.ChillWebApp.__init__(self)


def test():
    app = ChillDownWebApp()
    try:
        app.parse_cli()
    except Exception as e:
        logger.error("Unable to parsing command-line arguments %s", e)
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

    logger.info("Exiting chill_down_web python script")

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
