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
package jpl.gds.time.impl.message;

import java.util.List;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;
import jpl.gds.time.api.message.IFswTimeCorrelationMessage;
import jpl.gds.time.api.message.ISseTimeCorrelationMessage;
import jpl.gds.time.api.message.ITimeCorrelationMessageFactory;

/**
 * A factory that creates time correlation messages.
 * 
 *
 * @since R8
 */
public class TimeCorrelationMessageFactory implements ITimeCorrelationMessageFactory {

    /**
     * Costructor.
     */
    public TimeCorrelationMessageFactory() {
        // do nothing
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IFswTimeCorrelationMessage createFswTimeCorrelationMessage(
            final ISclk desiredSclk, final ISclk pSclk, final IAccurateDateTime fErt,
            final IAccurateDateTime pErt, final long vcfc, final int vcid, final int fLength,
            final boolean refFrameFound, final EncodingType encoding, final double rate,
            final long rateIndex) {
        return new FswTimeCorrelationMessage(desiredSclk, pSclk, fErt,
                pErt, vcfc, vcid, fLength,
                refFrameFound, encoding, rate, rateIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISseTimeCorrelationMessage createSseTimeCorrelationMessage(
            final List<Pair<ISclk, IAccurateDateTime>> correlations,
            final IAccurateDateTime packetErt, final ISclk packetSclk) {
        return new SseTimeCorrelationMessage(correlations, packetErt, packetSclk);
    }
}
