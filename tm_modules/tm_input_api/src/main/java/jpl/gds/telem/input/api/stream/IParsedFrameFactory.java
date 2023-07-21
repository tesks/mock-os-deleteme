/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.input.api.stream;

import jpl.gds.station.api.IStationTelemHeader;

/**
 *  An interface to be implemented by the factory for parsed frames
 *
 */
public interface IParsedFrameFactory {
    /**
     * Creates an instance of IParsedFrame.
     *
     * @return new Parsed Frame
     */
    IParsedFrame createParsedFrame(IStationTelemHeader header, byte[] frame, byte[] trailer);
}