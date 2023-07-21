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
package jpl.gds.product.api.builder;

import java.util.List;

import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.performance.IPerformanceData;

/**
 * An interface to be implemented by product builder services.
 * 
 *
 * @since R8
 */
public interface IProductBuilderService extends IService {

    /**
     * Gets the virtual channel ID for which this product builder is processing packets.
     * 
     * @return virtual channel number
     */
    public int getVcid();

    /**
     * Supplies performance data for the performance summary. This object just
     * asks its DiskProductStorage object for its performance data, and
     * currently supplies none of its own.
     * 
     * @return List of IPerformanceData objects
     * 
     */
    public List<IPerformanceData> getPerformanceData();

}