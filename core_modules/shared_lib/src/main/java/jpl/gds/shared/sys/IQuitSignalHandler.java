/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.sys;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;

/**
 * Interface for SIGTERM and SIGINT shutdown hook handlers
 * 
 */
public interface IQuitSignalHandler {

    /**
     * Cleanup operations to execute when the application receives a SIGTERM
     * signal or shuts down
     */
    public default void exitCleanly() {
        TraceManager.getDefaultTracer().debug(this.getClass().getName(),
                                              " shutting down using default QuitSignalHandler");
    }

    /**
     * Sets the the sig term handler's ICommandLine member variable to provide a command line summary at the
     * application's termination
     * 
     * When implementing this interface directly instead of extending AbstractCommaneLine, explicitly setCommandine
     * after configuring it.
     * 
     * @param cmdline
     *            ICommandLine
     */
    public default void setCommandLine(final ICommandLine cmdline) {
        // Default no implementation for Tests that extend ICommandLine
    }
}
