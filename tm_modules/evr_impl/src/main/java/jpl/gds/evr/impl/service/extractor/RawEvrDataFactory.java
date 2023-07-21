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
package jpl.gds.evr.impl.service.extractor;

import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IRawEvrData;
import jpl.gds.evr.api.service.extractor.IRawEvrDataFactory;

/**
 * A factory for creating raw EVR data field objects.
 * 
 * @since R8
 */
public class RawEvrDataFactory implements IRawEvrDataFactory {
    /**
     * Constructor
     */
    public RawEvrDataFactory() {
        // do nothing
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IRawEvrDataFactory#create()
     */
    @Override
    public IRawEvrData create() throws EvrExtractorException {
        return new RawEvrData();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IRawEvrDataFactory#create(byte[], java.lang.Class)
     */
    @Override
    public IRawEvrData create(final byte[] inputData, final Class<?> inputClass) throws EvrExtractorException {
        return new RawEvrData(inputData, inputClass);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IRawEvrDataFactory#create(byte[], int, int, java.lang.Class)
     */
    @Override
    public IRawEvrData create(final byte[] inputData, final int dataElementSize, final int offset, final Class<?> inputClass)
            throws EvrExtractorException {
        return new RawEvrData(inputData, dataElementSize, offset, inputClass);
    }

}
