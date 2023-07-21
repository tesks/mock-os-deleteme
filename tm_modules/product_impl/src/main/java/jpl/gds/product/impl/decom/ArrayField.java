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
package jpl.gds.product.impl.decom;

import jpl.gds.product.api.decom.*;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.types.ByteStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * StructuredArrayField represents a decom definition that describes a field
 * whose value is an array of various field types. Array elements may be defined
 * by DefinitionFields, StreamFields, BitFields, or other array fields.
 * 
 */
public class ArrayField extends AbstractFieldContainer implements IArrayField {

    private static final String NO_MORE_DATA_MSG = "StructuredArrayField ran out of data";
    private List<String> indexLabels;
    private final int maxlength;
    private int valueSize;
    private ArrayType arrayType = ArrayType.FIXED_LENGTH;
    private ISimpleField lengthVariable;
    private boolean lengthIsInBytes = false;
    private String indexEnum = null;
    
    
    /**
     * {@inheritDoc}
     */
    @Override
	public boolean isLengthIsInBytes() {
        return lengthIsInBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setLengthIsInBytes(final boolean lengthIsInBytes) {
        this.lengthIsInBytes = lengthIsInBytes;
    }

    /**
     * Creates an instance of StructuredArrayField. This constructor is used for
     * fixed arrays whose length is known, or for variable length arrays with
     * (maxLength = -1) that occur at the very end of a data stream.
     * 
     * @param name the name of the field
     * @param maxlength the maximum length of the array, or -1 for unknown
     *            lengths
     */
    @Deprecated
    public ArrayField(final String name, final int maxlength) {
        super(ProductDecomFieldType.ARRAY_FIELD);
        this.name = name;
        this.maxlength = maxlength;
         if (this.maxlength == -1) {
            arrayType = ArrayType.VARIABLE_LENGTH_UNKNOWN;
        }
    }

    /**
     * Creates an instance of StructuredArrayField. This constructor is used for
     * variable length arrays in which the length is in the data stream
     * immediately before the array data.
     * 
     * @param name the name of the field
     * @param maxlength the maximum length of the array, or -1 if not known
     * @param lengthLength the size in bytes of the array length field in the
     *            data stream
     * @param fieldFactory the decom field factory to use when creating sub-fields
     */
    @Deprecated
    public ArrayField(final String name, final int maxlength,
            final int lengthLength, final IProductDecomFieldFactory fieldFactory) {
        super(ProductDecomFieldType.ARRAY_FIELD);
        this.name = name;
        this.maxlength = maxlength;
        arrayType = ArrayType.VARIABLE_LENGTH_IN_DATA;

        lengthVariable = fieldFactory.createSimpleField("lengthPrefix", new DecomDataType(BaseDecomDataType.UNSIGNED_INT,lengthLength * 8), null, false);
    }

    /**
     * Creates an instance of StructuredArrayField. This constructor is used for
     * dynamic arrays whose length stored in another field.
     * 
     * @param name the name of the field
     * @param lengthField the decom field in which the array length will be
     *            found
     */
    @Deprecated
    public ArrayField(final String name, final ISimpleField lengthField) {
        super(ProductDecomFieldType.ARRAY_FIELD);
        this.name = name;
        maxlength = -1;
        lengthVariable = lengthField;
        arrayType = ArrayType.VARIABLE_LENGTH_IN_FIELD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public ISimpleField getLengthVariable() {
        return lengthVariable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setLengthVariable(final ISimpleField lengthVariable) {
        this.lengthVariable = lengthVariable;
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIndexEnum() {
		return indexEnum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIndexEnum(final String indexEnum) {
		this.indexEnum = indexEnum;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public ArrayType getArrayType() {
        return arrayType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setArrayType(final ArrayType type) {
        arrayType = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void addIndexLabel(final String title) {
        if (indexLabels == null) {
            indexLabels = new ArrayList<>();
        }
        indexLabels.add(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out,
            final int depth) throws IOException {
        if (name.equalsIgnoreCase("fill")) {
            if (maxlength < 0) {
                final long skippedBytes = stream.remainingBytes();
                stream.skip(skippedBytes);
                return (int) skippedBytes;
            } else {
                stream.skip(getValueSize());
                return getValueSize();
            }
        }

        // calculate the number of records in the array

        long bytesRead = 0;
        int numRecords = 0;

        switch (arrayType) {

            case FIXED_LENGTH:
                numRecords = maxlength;
                break;

            case VARIABLE_LENGTH_IN_FIELD:
                numRecords = lengthVariable.getLengthValue();
                break;

            case VARIABLE_LENGTH_UNKNOWN:
            	// Do not try to calculate # of records prior to going through them,

                // numRecords = ((int) stream.remainingBytes()) / getValueSize();
            	numRecords = -1; // to indicate variable length
                break;

            case VARIABLE_LENGTH_IN_DATA:
                final Number length = (Number) this.getValue(lengthVariable
                        .getDataType(), stream);
                numRecords = length.intValue();
                if (lengthIsInBytes) {
                    numRecords = numRecords / this.getValueSize();
                }
                bytesRead += lengthVariable.getDataType().getByteLength();
                break;
            default:
            	throw new IllegalStateException("Unknown array type in ArrayField: " + arrayType);
        }

        // if the output format is not null, use the specified format

        if (getPrintFormat() != null) {

			if (elements.size() == 1) {
				bytesRead += printFormattedUniformArray(stream, numRecords, out, depth);
			} else {
				bytesRead += printFormattedStructuredArray(stream, numRecords, out, depth);
			}
            return (int)bytesRead;
        }

        // the default output
        if (elements.size() == 1) {
            IProductDecomField element = elements.get(0);
            element.setSuppressName(true);

            out.arrayStart(getName(), numRecords);

            int i = 0;

            while (numRecords != -1 ? (i < numRecords) : stream.hasMore()) {
            	
            	// has potential to go past end of field if relying on numRecords
                if (!stream.hasMore()) {
                    tracer.log(numRecords != -1 ? TraceSeverity.WARN : TraceSeverity.DEBUG, NO_MORE_DATA_MSG);
                    break;
                }
                out.arrayIndex(getIndexLabel(i));
                bytesRead += element.printValue(stream, out, depth + 1);
                out.arrayIndexEnd();
                
                ++i;
            }
            
        } else { // (elements.size() != 1)

            out.arrayStart(name, numRecords);

            int i = 0;

            while (numRecords != -1 ? (i < numRecords) : stream.hasMore()) {
                out.arrayIndex(getIndexLabel(i));
                for (final IProductDecomField element : elements) {
                    // has potential to go past end of field if relying on numRecords

                    // 7/22/20: Update conditional to also check
                    // the bytes read vs getValueSize()
                    // index (i) starts at 0 but length is i + 1
                    if (!stream.hasMore() && bytesRead < getValueSize()) {
                        tracer.log(numRecords != -1 ? TraceSeverity.WARN : TraceSeverity.DEBUG, NO_MORE_DATA_MSG);
                        break;
                    }

                    bytesRead += element.printValue(stream, out, depth + 1);
                }
                out.arrayIndexEnd();

                ++i;
            }
            
        }

        if (numRecords > 0 && bytesRead < getValueSize()) {
            tracer.warn("more data available: "
                    + (getValueSize() - bytesRead)
                    + " remaining in field, read " + bytesRead);
        }
        out.arrayEnd();

        return (int) bytesRead;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth)
                                                                 throws IOException {
        printIndent(out, depth);
        if (arrayType.equals(ArrayType.FIXED_LENGTH)) {
            out.println("(Array of " + maxlength + "): " + name);
        } else if (arrayType
                .equals(ArrayType.VARIABLE_LENGTH_IN_FIELD)) {
            out.println("(Array of [" + lengthVariable.getName() + "]): "
                    + name);
        } else if (arrayType
                .equals(ArrayType.VARIABLE_LENGTH_IN_DATA)) {
            out.println("(Array of [" + (lengthIsInBytes ? "byte length" : "element count") + " found in data prefix" + 
                    "]): " + name);
        } else {
            out.println("(Array of variable length): " + name);
        }
        for (final IProductDecomField element : elements) {
            element.printType(out, depth + 1);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractFieldContainer#getValueSize()
     */
    @Override
    public int getValueSize() {
        if (valueSize == 0) {
            if (!arrayType.equals(ArrayType.FIXED_LENGTH)) {
                for (final IProductDecomField element : elements) {
                    valueSize += element.getValueSize();
                }
            } else {
                for (int i = 0; i < maxlength; ++i) {
                    for (final IProductDecomField element : elements) {
                        valueSize += element.getValueSize();
                    }
                }
            }
        }
        return valueSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getIndexLabel(final int index) {
        String label = null;
        if ((indexLabels != null) && (indexLabels.size() > index)) {
            label = indexLabels.get(index);
        }
        if (label == null) {
            label = Integer.toString(index);
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int getMaxLength() {
        return maxlength;
    }


    /**
     * Print an array of different elements according to the specified output
     * format.
     * 
     * @param data the data stream being read
     * @param recordCount the number of records to process
     * @param out the output formatter to which the formatted data is to be
     *            written
     * @param depth the indentation level of the formatted output
     * @return number of bytes processed
     * @throws IOException when an IO error occurs
     */
	private int printFormattedStructuredArray(final ByteStream data,
			final int recordCount, final IDecomOutputFormatter out,
			final int depth) throws IOException {

        final ArrayList<Object> objectsToPrint = new ArrayList<>();

        out.arrayStart(name, maxlength);

        final SprintfFormat dataFormatter = out.getApplicationContext().getBean(SprintfFormat.class);
        // now get the values and convert them to Java objects

        int bytesUsed = 0;
        int elementsPrinted = 0;
        int previousBytesUsed = bytesUsed;

		int icount = 0;

		while (recordCount != -1 ? (icount < recordCount) : data.hasMore()) {

            for (final IProductDecomField element : elements) {
                // has potential to go past end of field if relying on recordCount
                if (!data.hasMore()) {
                    tracer.log(recordCount != -1 ? TraceSeverity.WARN : TraceSeverity.DEBUG, NO_MORE_DATA_MSG);

                    break;
                }
                final ISimpleField df = (ISimpleField) element;
                final Object currentObject = df.getValue(df.getDataType(), data);
                bytesUsed += df.getDataType().getByteLength();
                objectsToPrint.add(currentObject);
                elementsPrinted++;
                if ((elementsPrinted % getItemsToPrint()) == 0) {
                    printFormattedLine(objectsToPrint, out, depth + 1, previousBytesUsed, dataFormatter);
                    objectsToPrint.clear();
                    previousBytesUsed = bytesUsed;
                }
            }

            ++icount;
		}

        // print any leftover objects

        if (!objectsToPrint.isEmpty()) {
            printFormattedLine(objectsToPrint, out, depth + 1,
                    previousBytesUsed, dataFormatter);
        }

        out.arrayEnd();
        return bytesUsed;
    }

    /**
     * Print an array of identical elements according to the specified output
     * format
     * 
     * @param data the data stream being processed
     * @param recordCount the number of records to process
     * @param out the output formatter to which the formatted data is to be
     *            written
     * @param depth the current indentation level
     * @return number of bytes processed
     * @throws IOException when an IO error occurs
     */
	private int printFormattedUniformArray(final ByteStream data,
			final int recordCount, final IDecomOutputFormatter out,
			final int depth) throws IOException {

        final ArrayList<Object> objectsToPrint = new ArrayList<>();

        out.arrayStart(name, maxlength);

        // now get the values and convert them to Java objects

        int bytesUsed = 0;
        int previousBytesUsed = bytesUsed;

        final ISimpleField df = (ISimpleField) (elements.get(0));
        final SprintfFormat dataFormatter = out.getApplicationContext().getBean(SprintfFormat.class);
        objectsToPrint.clear();

		int icount = 0;

		while (recordCount != -1 ? (icount < recordCount) : data.hasMore()) {

			// has potential to go past end of field if relying on recordCount
			if (!data.hasMore()) {
                tracer.log(recordCount != -1 ? TraceSeverity.WARN : TraceSeverity.DEBUG, NO_MORE_DATA_MSG);

				break;
			}

			Object currentObject = df.getValue(df.getDataType(), data);
			final Object tempObject = df.getResolvedValue(currentObject, out);
			if (tempObject != null) {
				currentObject = tempObject;
			}
			bytesUsed += df.getDataType().getByteLength();
			objectsToPrint.add(currentObject);

			if ((objectsToPrint.size() % getItemsToPrint()) == 0) {
				printFormattedLine(objectsToPrint, out, depth + 1,
						previousBytesUsed, dataFormatter);
				objectsToPrint.clear();
				previousBytesUsed = bytesUsed;
			}

			++icount;
		}

        // print any leftover objects

        if (!objectsToPrint.isEmpty()) {
            printFormattedLine(objectsToPrint, out, depth + 1,
                    previousBytesUsed, dataFormatter);
        }

        out.arrayEnd();

        return bytesUsed;
    }

    /**
     * Utility for printing a formatted line
     * 
     * @param objectsToPrint list of objects to print
     * @param out output formatter to write data to
     * @param depth the current indentation level
     * @param firstAddress address of the beginning of the first element to be
     *            printed
     * @param dataFormatter data formatter in use
     */
    private void printFormattedLine(final List<Object> objectsToPrint,
            final IDecomOutputFormatter out, final int depth, final int firstAddress,
            final SprintfFormat dataFormatter) throws IOException {

        String formattedData = "";
        if (objectsToPrint.size() == 1) {
            formattedData = dataFormatter.anCsprintf(getPrintFormat(),
                    objectsToPrint.get(0));
        } else if (objectsToPrint.size() < getItemsToPrint()) {
            final String newFormat = adjustOutputFormatterForCount(objectsToPrint
                    .size());
            formattedData = dataFormatter.sprintf(newFormat, objectsToPrint
                    .toArray());
        } else {
            formattedData = dataFormatter.sprintf(getPrintFormat(),
                    objectsToPrint.toArray());
        }

        out.addressValue(firstAddress, formattedData);
    }

    private String adjustOutputFormatterForCount(final int count) {
        final int diff = getItemsToPrint() - count;
        final String outputFormat = getPrintFormat();
        int lastPercentIndex = outputFormat.length() - 1;
        for (int i = 0; i < diff && lastPercentIndex > 0; i++) {
            lastPercentIndex = outputFormat.lastIndexOf('%',
                    lastPercentIndex - 1);
        }
        return outputFormat.substring(0, lastPercentIndex);
    }
}
