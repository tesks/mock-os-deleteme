/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.context.cli.app.mc;

import jpl.gds.context.api.options.RestCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;

/**
 * Interface IRestFulServerCommandLineApp
 */
public interface IRestFulServerCommandLineApp extends ICommandLineApp {
    /**
     * The largest legal value for a port. For AMPCS RESTful servers, it has the special
     * meaning in that it indicates to the servlet initializatio code that the container should be closed
     * and therefore, disable the RESTful interface.
     */
    public int DUMMY_DISABLED_REST_PORT    = 0;

    /** The value of restPort that will cause the RESTful interface on the server to be disabled */
    public int SPECIFIED_DISABLE_REST_PORT = -1;

    /**
     * The port through which a RESTful M&C Service may be accessed. Will be 0 if not configured.
     * 
     * @return the port through which a RESTful M&C Service may be accessed
     */
    default public int getRestPort() {
        return RestCommandOptions.DEFAULT_REST_PORT;
    }

    /**
     * @return true if RESTful service is secured with SSL/TLS
     */
    default public boolean isRestSecure() {
        return false;
    }

    /**
     * Sets the application rest port
     * 
     * @param restPort
     *            Port to set
     */
    void setRestPort(Integer restPort);
}
