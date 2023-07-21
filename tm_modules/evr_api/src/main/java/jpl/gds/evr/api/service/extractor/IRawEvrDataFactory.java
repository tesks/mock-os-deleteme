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
package jpl.gds.evr.api.service.extractor;

/**
 *
 * An interface to be implemented by factories that create raw EVR data field
 * objects.
 * 
 * @since R8
 */
public interface IRawEvrDataFactory {

    /**
     * Creates a raw EVR data object.
     * 
     * @return new raw EVR data instance
     * @throws EvrExtractorException
     *             if there is a problem creating the object
     */
    public IRawEvrData create() throws EvrExtractorException;

    /**
     * Constructor for raw EVR data fields with two arguments--the byte array
     * containing the EVR data and the expected type of the data. It is assumed
     * that the EVR data occupies all of the inputData array.
     * 
     * @param inputData
     *            the byte array containing the EVR data field
     * @param inputClass
     *            the expected type of the data
     * @return new raw EVR data instance
     * @throws EvrExtractorException
     *             thrown if encountered.
     */
    public IRawEvrData create(byte[] inputData, Class<?> inputClass)
            throws EvrExtractorException;

    /**
     * This constructor initializes the internal fields of raw EVR data field
     * from a byte array which may contain more than the data item to be saved.
     * The data is set to the input data; the length is set to the data element
     * size; the data is extracted from the input array beginning at the index
     * specified by offset; and the type is set to the input type. Finally, the
     * validity flag is set to true.
     * 
     * @param inputData
     *            the byte array containing the EVR data
     * @param dataElementSize
     *            the size in bytes of the data element to be saved
     * @param offset
     *            the index of the inputData array at which to begin extracting
     *            the data
     * @param inputClass
     *            the expected type of the data
     * @return new raw EVR data instance
     * @throws EvrExtractorException
     *             thrown if encountered.
     */
    public IRawEvrData create(byte[] inputData, int dataElementSize,
            int offset, Class<?> inputClass) throws EvrExtractorException;

}