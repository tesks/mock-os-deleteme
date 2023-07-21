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
package jpl.gds.eha.impl.service.channel;

import java.io.PrintWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.decom.IDecomListener;
import jpl.gds.eha.api.service.channel.IChannelizationListener;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;

/**
 * A factory for creating generic decom listeners.
 * 
 * @since R8
 */
public class DecomListenerFactory implements IDecomListenerFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IChannelizationListener createChannelizationListener(final ApplicationContext appContext) {
        return new ChannelizationListener(appContext);
     }
     
    /**
     * {@inheritDoc}
     */
    @Override
    public IDecomListener createDecomPrinterListener(final PrintWriter out, final ApplicationContext appContext) {
         return new DecomPrinterListener(out, appContext);
     }
     
    /**
     * {@inheritDoc}
     */
    @Override
    public IChannelizationListener createHybridEvrChannelizationListener(final ApplicationContext appContext) {
        return new HybridChannelEvrListener(appContext);
     }

}
