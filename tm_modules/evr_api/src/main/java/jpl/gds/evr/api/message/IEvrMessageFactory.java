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
package jpl.gds.evr.api.message;

import jpl.gds.evr.api.IEvr;

/**
 * An interface to be implemented by EVR message factories.
 * 
 * @since R8
 */
public interface IEvrMessageFactory {

    /**
     * Creates an EVR message.
     * 
     * @param evr the IEvr object to put into the message
     * @return new message instance
     */
    public IEvrMessage createEvrMessage(IEvr evr);

}