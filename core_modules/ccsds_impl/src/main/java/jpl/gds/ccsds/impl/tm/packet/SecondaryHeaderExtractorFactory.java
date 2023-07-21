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
package jpl.gds.ccsds.impl.tm.packet;

import jpl.gds.ccsds.api.tm.packet.ISecondaryHeaderExtractorFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.shared.time.ISclkExtractor;

/**
 * A secondary header extractor factory.
 * 
 * @since R8
 * 
 */
public class SecondaryHeaderExtractorFactory implements ISecondaryHeaderExtractorFactory {

    @Override
    public ISecondaryPacketHeaderExtractor getGpsTimeSecondaryHeaderExtractor() {
        return new GpsTimeSecondaryHeaderExtractor();
    }

    @Override
    public ISecondaryPacketHeaderExtractor getSclkSecondaryHeaderExtractor(final ISclkExtractor extractor) {
        return new SclkSecondaryHeaderExtractor(extractor);
    }

    @Override
    public ISecondaryPacketHeaderExtractor getNullSecondaryHeaderExtractor() {
        return new NullSecondaryHeaderExtractor();
    }

}
