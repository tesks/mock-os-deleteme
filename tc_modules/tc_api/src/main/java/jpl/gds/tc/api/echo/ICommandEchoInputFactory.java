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

import java.io.File;

import org.springframework.context.ApplicationContext;

/**
 * The interface for the CommandEchoInputFactory.
 *
 */
public interface ICommandEchoInputFactory {
    
    /**
     * Creates an ICommandEchoInput object that uses the client socket design to receive data.
     * The data sender must be set up as a server socket
     * 
     * @param appContext
     *            the current application context
     * @param host
     *            the name of the machine that will send command echo data
     * @param port
     *            the port number on the host machine that will send command
     *            echo data
     * @return an ICommandEcho Input interfaced source
     */
    public ICommandEchoInput getClientSocketInput(ApplicationContext appContext, String host, int port);

    
    /**
     * Creates an ICommandEchoInput object that uses the server socket design to receive data.
     * Clients connect to this input through the specified port and give it data.
     * 
     * @param appContext
     *            the current application context
     * @param port
     *            the port number on the host machine that will send command
     *            echo data
     * @return an ICommandEcho Input interfaced source
     */
    public ICommandEchoInput getServerSocketInput(ApplicationContext appContext, int port);
    
    /**
     * Creates an object that uses the ICommandEchoInput interface that requires an file with
     * the data to be read.
     * @param appContext the current application context
     * @param inputFile the file to be read
     * @return an ICommandEchoInput interfaced source
     */
    public ICommandEchoInput getFileInput(ApplicationContext appContext, File inputFile);

}
