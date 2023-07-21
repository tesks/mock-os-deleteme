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
package jpl.gds.time.api.message;

import java.util.List;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;

/**
 * An interface to be implemented by SSE time correlation messages.
 * 
 *
 * @since R8
 */
public interface ISseTimeCorrelationMessage extends IMessage {

    /**
     * Gets the list of the SCLK/ERT correlation pairs.
     * 
     * @return List of Pairs
     */
    public List<Pair<ISclk, IAccurateDateTime>> getTimeEntries();

    /**
     * Gets the TC packet ERT.
     * 
     * @return ERT object
     */
    public IAccurateDateTime getPacketErt();

    /**
     * Gets the TC packet SCLK.
     * 
     * @return SCLK object
     */
    public ISclk getPacketSclk();
}