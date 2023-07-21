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
package jpl.gds.watcher.responder.app;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * An extension of ProductHandlerApp for the recorded engineering watcher.
 */
public class RecordedProductHandlerApp extends ProductHandlerApp {

    /**
     * Constructor
     * 
     * @param appContext the current application context
     */
    public RecordedProductHandlerApp(final ApplicationContext appContext) {
        super(appContext);        
    }
    
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        
        // Override the general watcher shutdown time with that in the recorded product
        // properties
        this.drainTime = appContext.getBean(RecordedProductProperties.class).
                getRecordedProcessShutdownTimeout() * 1000L;
    }

}
