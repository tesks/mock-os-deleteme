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
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.tc.api.echo.ICommandEchoInput;
import jpl.gds.tc.api.message.ICommandMessageFactory;

/**
 * The CommandEchoInput classes all share a number of the same components. This
 * class houses these shared elements and the repeated functions within each
 * 
 *
 */
public abstract class AbstractCommandEchoInput implements ICommandEchoInput {
    
    /** The application context where this CommandEchoInput object is being used */
    protected ApplicationContext appContext;
    
    /** The tracer to be used by this CommandEchoInput */
    protected Tracer trace;
    
    /** Keeps track of this CommandEchoInput object is shutting down */
    protected AtomicBoolean stopping = new AtomicBoolean(false);
    
    /** The message factory for creating CommandEchoMessages */
    protected ICommandMessageFactory msgFactory;
    
    /** where CommandEchoMessages are published */
    protected IMessagePublicationBus msgBus;
    
    /**
     * Constructor. Primary function is just to set the AbstractCommandEchoInput
     * variables.
     * 
     * @param appContext
     *            the current ApplicaitonContext
     */
    public AbstractCommandEchoInput(final ApplicationContext appContext){
        this.appContext = appContext;
        this.trace = TraceManager.getTracer(appContext, Loggers.CMD_ECHO);
        this.msgFactory = appContext.getBean(ICommandMessageFactory.class);
        this.msgBus = appContext.getBean(IMessagePublicationBus.class);
    }
    
    /**
     * Report if this class is in the process of shutting down
     * 
     * @return TRUE if shutting down, FALSE otherwise
     */
    @Override
    public boolean isStopping(){
        return this.stopping.get();
    }
    
    /**
     * External function to be called for an orderly shutdown
     */
    @Override
    public void stopSource(){
        stopping.set(true);
        this.disconnect();
    }
    
    /**
     * Disconnect from the input, but allow the CommandEchoInput to continue
     */
    protected void disconnect(){
        trace.info("Disconnecting from input source.");
        //do nothing
    }

}
