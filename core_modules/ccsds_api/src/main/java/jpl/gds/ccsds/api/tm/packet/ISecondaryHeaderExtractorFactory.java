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
package jpl.gds.ccsds.api.tm.packet;

import jpl.gds.shared.time.ISclkExtractor;

/**
 * An interface to be implemented by secondary header extractor factories.
 *
 * 
 * @since R8
 *
 */
public interface ISecondaryHeaderExtractorFactory {
    
    /**
     * Gets a GPS time secondary header extractor.
     * 
     * @return header extractor instance
     */
    public ISecondaryPacketHeaderExtractor getGpsTimeSecondaryHeaderExtractor();
    
    
    /**
     * Gets a SCLK time secondary header extractor.
     * 
     * @param extractor ISclkExtractor for the SCLK in the header
     * 
     * @return header extractor instance
     */
    public ISecondaryPacketHeaderExtractor getSclkSecondaryHeaderExtractor(ISclkExtractor extractor);
    
    /**
     * Gets a null secondary header extractor, meaning there is no timecode field
     * 
     * @return header extractor instance
     */
    public ISecondaryPacketHeaderExtractor getNullSecondaryHeaderExtractor();

}
