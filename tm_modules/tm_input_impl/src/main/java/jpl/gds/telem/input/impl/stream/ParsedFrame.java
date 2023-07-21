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

/**
 * Multi-mission implementation for IParsedFrame
 *
 */
public class ParsedFrame implements IParsedFrame{
    private IStationTelemHeader header;
    private byte[] frame;
    private byte[] trailer;

    /**
     * Constructor.
     *
     * @param headerObj header object
     * @param frame  Frame bytes
     * @param trailer  Trailer bytes
     */
    public ParsedFrame(final IStationTelemHeader headerObj, final byte[] frame, final byte[] trailer) {
        this.header = headerObj;
        this.frame = frame;
        this.trailer = trailer;
    }

    @Override
    public IStationTelemHeader getHeader() {
        return header;
    }

    @Override
    public byte[] getFrame() {
        return frame;
    }

    @Override
    public byte[] getTrailer() {
        return trailer;
    }
}
