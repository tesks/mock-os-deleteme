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
package jpl.gds.tc.impl.output.adapter;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;

/**
 * A factory for IRawOutputAdapter objects.
 * 
 *
 * MPCS-7677 - 9/15/15. Added class, content taken from
 *          UplinkConnectionType.
 */
public final class RawOutputAdapterFactory implements IRawOutputAdapterFactory {

    private final Tracer             trace;
    private final ApplicationContext appContext; 


    /**
     * Enforce static nature.
     */
    public RawOutputAdapterFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        trace = TraceManager.getDefaultTracer(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRawOutputAdapter getUplinkOutput(final UplinkConnectionType connType) throws RawOutputException {
        final CommandProperties cc = appContext.getBean(CommandProperties.class);

        final String bof = cc.getBinaryOutputFile();

        // MPCS-8656 02/09/17 - Get the file adapter property and attempt to instantiate it
        if (bof != null) {
        	IRawOutputAdapter fileOutput = null;
        	
        	final String clzz = appContext.getBean(CommandProperties.class).getOutputAdapterClass("FILE");
        	if (clzz == null) {
                throw new RawOutputException("No adapter class defined for file output ");
            }

            final Class<?>[] argsClass = {appContext.getClass(), bof.getClass()};
			final Object[]   args      = {appContext, bof};
            
            try {
                fileOutput = (IRawOutputAdapter) ReflectionToolkit.createObject(clzz, argsClass, args);
            } catch (final Exception e) {
                trace.error("Could not create instance of " + clzz);

                throw new RawOutputException("Could not create instance of "
                        + clzz, e);
            }
            return (fileOutput);
        }

        final String clazz = appContext.getBean(CommandProperties.class).getOutputAdapterClass(connType.name());

        if (clazz == null) {
            throw new RawOutputException("No adapter class defined for raw "
                    + "output type " + connType.name());
        }
        IRawOutputAdapter outputAdapter = null;

        try {
            final Class<?> c = Class.forName(clazz);

            outputAdapter = (IRawOutputAdapter) ReflectionToolkit.createObject(c,
            		new Class[] { ApplicationContext.class}, new Object[] {appContext});
        } catch (final Exception e) {
            trace.error("Could not create instance of " + clazz);

            throw new RawOutputException("Could not create instance of "
                    + clazz, e);
        }

        outputAdapter.init();
        return outputAdapter;
    }
}
