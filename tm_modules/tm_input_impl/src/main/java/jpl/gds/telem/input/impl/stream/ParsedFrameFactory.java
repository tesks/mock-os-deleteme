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

package jpl.gds.telem.input.impl.stream;

import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.telem.input.api.stream.IParsedFrame;
import jpl.gds.telem.input.api.stream.IParsedFrameFactory;

/**
 * The multimission parsed frame factory class.
 *
 */
public class ParsedFrameFactory implements IParsedFrameFactory {
    @Override
    public IParsedFrame createParsedFrame(final IStationTelemHeader header, final byte[] frame, final byte[] trailer) {
        return new ParsedFrame(header, frame, trailer);
    }
}