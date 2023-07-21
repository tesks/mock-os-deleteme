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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IRawEvrData;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;

/**
 * RawEvrData is the abstract base class for storing the byte representation of
 * an EVR argument.
 * 
 *
 */
public class RawEvrData implements IRawEvrData {

    /**
     * Original raw evr data.
     */
    protected byte[] originalData;

    /**
     * Size of the original data.
     */
    protected int size;

    /**
     * Expected type of evr.
     */
    protected Class<?> expectedType;

    /**
     * If data is valid.
     */
    protected Boolean dataIsValid;

    /*
     * These don't need "Comparable" and can be assigned in line right here.
     */
    private static final Set<String> integerFormats = new TreeSet<String>(Arrays.asList("d","i","o","u","x","X","c"));
    private static final Set<String> floatingPointFormats = new TreeSet<String>(Arrays.asList("f","e","E","g","G"));
    private static final Set<String> stringFormats = new TreeSet<String>(Arrays.asList("c","s"));

    /**
     * The default constructor.
     * 
     */
    public RawEvrData() {

        this.dataIsValid = false;
    }
    
    /**
     * Constructor with two arguments--the byte array containing the evr data
     * and the expected type of the data. It is assumed that the evr data
     * occupies all of the inputData array.
     * 
     * @param inputData
     *            - the byte array containing the evr data
     * @param inputClass
     *            - the expected type of the data
     * @throws EvrExtractorException thrown if encountered.
     */
    public RawEvrData(final byte[] inputData, final Class<?> inputClass)
            throws EvrExtractorException {
 
        this();
    	   
        init(inputData, inputClass);
    }
    
    /**
     * this constructor initializes the internal fields of RawEvrData from a
     * byte array which may contain more than the data item to be saved. The
     * data is set to the input data; the length is set to the data element
     * size; the data is extracted from the input array beginning at the index
     * specified by offset; and the type is set to the input type. Finally, the
     * validity flag is set to true.
     * 
     * @param inputData
     *            - the byte array containing the evr data
     * @param dataElementSize
     *            - the size in bytes of the data element to be saved
     * @param offset
     *            -- the index of the inputData array at which to begin
     *            extracting the data
     * @param inputClass
     *            - the expected type of the data
     * @throws EvrExtractorException thrown if encountered.
     */
    public RawEvrData(final byte[] inputData, final int dataElementSize,
            final int offset, final Class<?> inputClass) throws EvrExtractorException {

    	this();
    	   
        init(inputData, dataElementSize, offset, inputClass);
    }

    /**
     * Initializes the internal fields of RawEvrData The data is set to the
     * input data; the length is set to the length of the input data; and the
     * type is set to the input type. Finally, the validity flag is set to true.
     * 
     * @param inputData
     *            - the byte array containing the evr data
     * @param inputClass
     *            - the expected type of the data
     * @throws EvrExtractorException
     *             thrown if encountered
     */
    private void init(final byte[] inputData, final Class<?> inputClass)
            throws EvrExtractorException {

        if (inputData.length == 0) {

            throw new EvrExtractorException(
                    "Cannot construct an EVR data item of length 0");
        }

        this.originalData = new byte[inputData.length];

        System.arraycopy(inputData, 0, this.originalData, 0, inputData.length);

        this.dataIsValid = true;

        this.size = this.originalData.length;

        this.expectedType = inputClass;
    }

    /**
     * this version of init initializes the internal fields of RawEvrData from a
     * byte array which may contain more than the data item to be saved. The
     * data is set to the input data; the length is set to the data element
     * size; the data is extracted from the input array beginning at the index
     * specified by offset; and the type is set to the input type. Finally, the
     * validity flag is set to true.
     * 
     * @param inputData
     *            - the byte array containing the evr data
     * @param dataElementSize
     *            - the size in bytes of the data element to be saved
     * @param offset
     *            -- the index of the inputData array at which to begin
     *            extracting the data
     * @param inputClass
     *            - the expected type of the data
     * @throws EvrExtractorException
     *             thrown if encountered
     */
    private void init(final byte[] inputData, final int dataElementSize, final int offset,
            final Class<?> inputClass) throws EvrExtractorException {

        if (inputData.length == 0) {

            throw new EvrExtractorException(
                    "Cannot construct an EVR data item of length 0");
        }

        if (dataElementSize <= 0) {

            throw new EvrExtractorException(
                    "The size of an EVR data element must be positive");
        }

        if (offset < 0) {

            throw new EvrExtractorException(
                    "The offset into the EVR data array must be "
                            + "non-negative");
        }

        this.originalData = new byte[dataElementSize];

        System.arraycopy(inputData, offset, this.originalData, 0,
                dataElementSize);

        this.dataIsValid = true;

        this.size = dataElementSize;

        this.expectedType = inputClass;
    }

    /**
     * getExpectedType returns the type of the data element as derived from the
     * evr definition
     * 
     * @return the Java class which is the expected type
     */
    @Override
    public Class<?> getExpectedType() {

        return this.expectedType;
    }

    /**
     * dumpDataInformation writes information about the data to the given Trace
     * logger.
     * 
     * @return dump data information
     */
    @Override
    public String getDumpDataInformation() {
        final StringBuilder message = new StringBuilder(64);
        /*  Fix NPE when expected type null */
        message.append("Expected parameter type "
                + (this.expectedType == null ? "UNKNOWN" : this.expectedType.getName()));
        message.append(", Size in bytes: " + this.size);
        message.append(", Hex Dump: ");

        final StringWriter strOut = new StringWriter();
        final PrintWriter out = new PrintWriter(strOut);
        for (int icount = 0; icount < this.size; ++icount) {
            out.printf("%02X ", Byte.valueOf(this.originalData[icount]));
        }

        message.append(strOut.toString());
        message.append('\n');
        return message.toString();
    }

    /**
     * buildIntegerObject constructs an integer object from the original byte
     * data in the RawEvrData object. buildIntegerObject uses the GDR class to
     * perform the construction.
     * 
     * @return the formatted integer number as a Java Number
     */
    private Number buildIntegerObject() {

        Number outputNumber;

        switch (this.size) {

        case 0: {

            outputNumber = null;
            break;
        }

        case 1: {

            outputNumber = Byte.valueOf(this.originalData[0]);
            break;
        }

        case 2: {

            short rawShort1 = this.originalData[0];
            rawShort1 <<= 8;
            short rawShort2 = this.originalData[1];
            rawShort2 &= 0x00ff;
            short finalRawShort = rawShort1;
            finalRawShort |= rawShort2;
            outputNumber = Short.valueOf(finalRawShort);
            break;
        }

        case 4: {

            int rawInt0 = this.originalData[0];
            rawInt0 <<= 24;
            rawInt0 &= 0xff000000;
            int rawInt1 = this.originalData[1];
            rawInt1 <<= 16;
            rawInt1 &= 0x00ff0000;
            int rawInt2 = this.originalData[2];
            rawInt2 <<= 8;
            rawInt2 &= 0x0000ff00;
            int rawInt3 = this.originalData[3];
            rawInt3 &= 0x000000ff;

            int finalRawInt = 0;
            finalRawInt |= rawInt0;
            finalRawInt |= rawInt1;
            finalRawInt |= rawInt2;
            finalRawInt |= rawInt3;
            outputNumber = Integer.valueOf(finalRawInt);

            break;

        }

        case 8: {

            long rawLong0 = this.originalData[0];
            rawLong0 <<= 56;
            rawLong0 &= 0xff00000000000000L;
            long rawLong1 = this.originalData[1];
            rawLong1 <<= 48;
            rawLong1 &= 0x00ff000000000000L;
            long rawLong2 = this.originalData[2];
            rawLong2 <<= 40;
            rawLong2 &= 0x0000ff0000000000L;
            long rawLong3 = this.originalData[3];
            rawLong3 <<= 32;
            rawLong3 &= 0x000000ff00000000L;

            long rawLong4 = this.originalData[4];
            rawLong4 <<= 24;
            rawLong4 &= 0x00000000ff000000L;
            long rawLong5 = this.originalData[5];
            rawLong5 <<= 16;
            rawLong5 &= 0x0000000000ff0000L;
            long rawLong6 = this.originalData[6];
            rawLong6 <<= 8;
            rawLong6 &= 0x000000000000ff00L;
            long rawLong7 = this.originalData[7];
            rawLong7 &= 0x00000000000000ffL;
            long finalRawLong = 0;
            finalRawLong |= rawLong0;
            finalRawLong |= rawLong1;
            finalRawLong |= rawLong2;
            finalRawLong |= rawLong3;
            finalRawLong |= rawLong4;
            finalRawLong |= rawLong5;
            finalRawLong |= rawLong6;
            finalRawLong |= rawLong7;
            outputNumber = Long.valueOf(finalRawLong);

            break;
        }

        default: {

            outputNumber = null;
            break;

        }

        } // end of switch statement

        return outputNumber;
    }

    /**
     * buildFloatingPointObject constructs a floating point object from the
     * original byte data in the RawEvrData object. buildFloatingPointObject
     * uses the GDR class to perform the construction.
     * 
     * @return formattedObject - the formatted number as obtained from a method
     *         in the GDR class.
     * 
     */
    private Number buildFloatingPointObject() {

        Number outputNumber;

        switch (this.size) {

        case 0: {

            outputNumber = null;
            break;
        }

        case 4: {

            outputNumber = Float.valueOf(GDR.get_float(this.originalData, 0));
            break;
        }

        case 8: {

            outputNumber = Double.valueOf(GDR.get_double(this.originalData, 0));
            break;
        }

        default: {

            outputNumber = null;
            break;
        }

        } // end of switch statement

        return outputNumber;

    }

    /**
     * buildStringObject constructs a String object from the original byte data
     * in the RawEvrData object.
     * 
     * @return formattedString - the formatted string as obtained from the GDR
     *         class.
     */
    private String buildStringObject() {

        String tempString = "";

        if (this.size > 0) {

            tempString = GDR.get_string(this.originalData, 0, this.size);
        }

        return tempString;

    }

    /**
     * integerFormatIsValid checks that the specified integer format is an
     * allowed integer format as stored in integerFormats
     * 
     * @param inputFormat
     *            input format to check
     * @return true if inputFormat is valid, false otherwise
     */
    private boolean integerFormatIsValid(final String inputFormat) {

        boolean formatIsValid = false;
        if (integerFormats.contains(inputFormat)) {
            formatIsValid = true;
        }
        return formatIsValid;
    }

    /**
     * floatingPtFormatIsValid checks that the specified floating poing format
     * is an allowed floating point format as stored in floatingPointFormats
     * 
     * @param inputFormat
     *            input format to check
     * @return true if inputFormat is valid, false otherwise
     */

    private boolean floatingPtFormatIsValid(final String inputFormat) {

        boolean formatIsValid = false;
        if (floatingPointFormats.contains(inputFormat)) {
            formatIsValid = true;
        }
        return formatIsValid;
    }

    /**
     * floatingPtFormatIsValid checks that the specified floating poing format
     * is an allowed floating point format as stored in floatingPointFormats
     * 
     * @param inputFormat
     *            input format to check
     * @return true if inputFormat is valid, false otherwise
     */
    private boolean stringFormatIsValid(final String inputFormat) {

        boolean formatIsValid = false;
        if (stringFormats.contains(inputFormat)) {
            formatIsValid = true;
        }
        return formatIsValid;
    }


    /**
     * replaceData replaces the original data with a string mnemonic this method
     * is used to replace opcodes with mnemonics for various reasons--ITAR, etc.
     * 
     * @param inputMnemonic
     *            mnemonic data to replace values with
     */
    @Override
    public void replaceData(final String inputMnemonic) {

        this.originalData = inputMnemonic.getBytes();
        this.expectedType = inputMnemonic.getClass();
        this.size = this.originalData.length;
        this.dataIsValid = true;
    }

    /**
     * formatData is an abstract method which is to be implemented in the
     * subclasses according to the needs of the project.
     * 
     * @param inputFormat
     *            -- the format which governs how the bits in the the returned
     *            object are to be filled. For example, if the input format is
     *            %d, a Long object is desired, and the original data is 4 bytes
     *            in length with the most significant bit( the sign bit ) set,
     *            the first four bytes of the Long object would be set to hex
     *            FFFF.
     * @return outputObject -- the converted object; it is null if the
     *         formatting failed.
     * 
     */
    @Override
    public Object formatData(final String inputFormat) {

        if (integerFormatIsValid(inputFormat)) {

            return buildIntegerObject();

        } else if (floatingPtFormatIsValid(inputFormat)) {

            return buildFloatingPointObject();

        } else if (stringFormatIsValid(inputFormat)) {

            return buildStringObject();

        }

        TraceManager

                .getDefaultTracer()
                .warn("Could not find format "
                        + inputFormat
                        + " in allowed integer, floating point, or string formats");

        return null;
    }

    /**
     * Returns the size of the original data.
     * 
     * @return data size
     */
    @Override
    public int getSize() {
        return this.size;
    }

    /**
     * Returns the original data.
     * 
     * @return original data
     */
    @Override
    public byte[] getByteArray() {
        return this.originalData;
    }

    /**
     * This method sets the length of the evr data element to 0 and validates
     * it. For MSL, this type of evr is only valid for string data, so the
     * expected data type is set to String.
     */

    @Override
    public void setDataToEmptyString() {

        dataIsValid = true;
        expectedType = String.class;
        size = 0;
    }
}
