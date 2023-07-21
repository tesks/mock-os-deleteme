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
package jpl.gds.tc.impl.echo;

import java.io.File;

import org.springframework.context.ApplicationContext;

import jpl.gds.tc.api.echo.ICommandEchoInput;
import jpl.gds.tc.api.echo.ICommandEchoInputFactory;

/**
 * Factory for creating the command echo input classes. The input class returned
 * depends upon the number and type of arguments provided.
 * 
 *
 */
public class CommandEchoInputFactory implements ICommandEchoInputFactory {
    
    public ICommandEchoInput getClientSocketInput(ApplicationContext appContext, String host, int port){
        return new ClientSocketCommandEchoInput(appContext, host, port);
    }
    

    
    public ICommandEchoInput getServerSocketInput(ApplicationContext appContext, int port){
        return new ServerSocketCommandEchoInput(appContext, port);
    }
    
    public ICommandEchoInput getFileInput(ApplicationContext appContext, File inputFile){
        return new FileCommandEchoInput(appContext, inputFile);
    }

}
