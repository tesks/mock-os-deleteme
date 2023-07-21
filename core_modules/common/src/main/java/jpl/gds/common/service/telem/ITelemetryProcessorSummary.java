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
package jpl.gds.common.service.telem;

/**
 * The Interface for a POJO to return Telemetry Processor Summary Status via RESTful interface
 * 
 * @since R8.1
 */
public interface ITelemetryProcessorSummary extends ITelemetrySummary {

    /**
     * Gets the channelized packet count.
     * 
     * @return the EHA packet count.
     */
    long getEhaPackets();

    /**
     * Gets the EVR packet count.
     * 
     * @return EVR packet count.
     */
    long getEvrPackets();

    /**
     * Gets the partial product count.
     * 
     * @return the partial product count.
     */
    long getPartialProducts();

    /**
     * Gets the total bytes of product data processed.
     * 
     * @return product data byte count
     */
    long getProductDataBytes();

    /**
     * Gets the complete product count.
     * 
     * @return the count of complete products
     */
    long getProducts();

}
