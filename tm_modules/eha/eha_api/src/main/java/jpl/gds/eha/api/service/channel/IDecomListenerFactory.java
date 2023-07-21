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
package jpl.gds.eha.api.service.channel;

import java.io.PrintWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.decom.IDecomListener;

/**
 * An interface to be implemented by factories that create channel decom
 * listeners.
 * 
 * @since R8
 */
public interface IDecomListenerFactory {

    /**
     * Creates a channelization listener.
     * 
     * @param appContext
     *            the current application context
     * @return new channelization listener instance
     */
    public IChannelizationListener createChannelizationListener(ApplicationContext appContext);

    /**
     * Creates a decom printer listener, which writes out received channel
     * values to the specified writer.
     * 
     * @param out
     *            the printer writer to output to
     * @param appContext 
     *            the current application context
     * @return new channelization listener instance
     */
    public IDecomListener createDecomPrinterListener(PrintWriter out, ApplicationContext appContext);

    /**
     * Creates a hybrid channelization listener, which listens for EVRs from
     * decom as well as channel samples.
     * 
     * @param appContext
     *            the current application context
     * @return new hybrid channelization listener instance
     */
    public IChannelizationListener createHybridEvrChannelizationListener(ApplicationContext appContext);

}