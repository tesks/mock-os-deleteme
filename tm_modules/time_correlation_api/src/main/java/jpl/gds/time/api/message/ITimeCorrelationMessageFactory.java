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

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;

/**
 * An interface to be implemented by factories that create time correlation messages.
 * 
 *
 * @since R8
 */
public interface ITimeCorrelationMessageFactory {

    /**
     * Creates a flight time correlation message.
     * 
     * @param desiredSclk
     *            the "expected" SCLK from inside the TC packet
     * @param pSclk
     *            the SCLK from the TC packet header
     * @param fErt
     *            the ERT of the reference frame
     * @param pErt
     *            the ERT of the TC packet
     * @param vcfc
     *            the reference frame sequence counter
     * @param vcid
     *            the reference frame and TC packet virtual channel ID
     * @param fLength
     *            the length of the reference frame in bytes
     * @param refFrameFound true if the reference frame was located, false if not
     * @param encoding encoding type of the reference frame
     * @param rate  bitrate at which the reference frame was received
     * @param rateIndex flight rate index the rate corresponds to
     * @return new message instance           
     */
    public IFswTimeCorrelationMessage createFswTimeCorrelationMessage(ISclk desiredSclk, ISclk pSclk,
            IAccurateDateTime fErt, IAccurateDateTime pErt, long vcfc, int vcid, int fLength, boolean refFrameFound,
            EncodingType encoding, double rate, long rateIndex);

    /**
     * Creates an SSE time correlation message
     * 
     * @param sseTC2
     *            SCLK/ERT pairs from the SSE TC packet
     * @param packetErt
     *            ERT of the TC Packet
     * @param packetSclk
     *            SCLK of the TC packet
     * @return new message instance
     */
    public ISseTimeCorrelationMessage createSseTimeCorrelationMessage(List<Pair<ISclk, IAccurateDateTime>> sseTC2,
                                                                      IAccurateDateTime packetErt, ISclk packetSclk);

}