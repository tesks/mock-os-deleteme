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
package jpl.gds.tc.api.echo;

/**
 * ICommandEchoInput is the interface class for command echo input sources. All
 * configuration is done at instantiation with the assistance of the factory.
 * These functions are used to orchestrate having the class connect to its input
 * source, take data from the source and publish messages, and shutdown.
 * 
 *
 */
public interface ICommandEchoInput {
    
    /**
     * Connect to the data source
     * 
     * @return TRUE if the connection was successful, FALSE otherwise
     */
    public boolean connect();
    
    /**
     * Report if ICommandEchoInput is connected to its data source
     * 
     * @return TRUE if connected, FALSE otherwise
     */
    public boolean isConnected();
    
    /**
     * Report if the ICommandEchoInput is going to be stopping/shutting down
     * 
     * @return TRUE if it is stopping, FALSE if not
     */
    public boolean isStopping();
    
    /**
     * Stop processing data and disconnect from the source
     */
    public void stopSource();
    
    /**
     * Start taking data from the source, package it into messages, and publish
     */
    public void ingestData();

}
