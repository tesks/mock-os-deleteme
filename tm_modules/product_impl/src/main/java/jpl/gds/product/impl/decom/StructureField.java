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
/**
 * 
 */
package jpl.gds.product.impl.decom;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.ISimpleField;
import jpl.gds.product.api.decom.IStructureField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.ByteStream;

/**
 * The StructureField class is a decom definition that represents a field that
 * is a non-repeating random collection of other fields.
 * 
 *
 */
public class StructureField extends AbstractFieldContainer implements IStructureField {

    private static Tracer log = TraceManager.getDefaultTracer();
    private String typeName;
	
	/**
	 * Constructs a structure with the given name.
	 * 
	 * @param name of the structure
	 * 
	 * @deprecated use ProductDecomFieldFactory instead to create this object
	 */
    @Deprecated
	public StructureField(final String name) {
	    super(ProductDecomFieldType.STRUCTURE_FIELD);
	    setName(name);
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {
        printIndent(out, depth);
        out.println("Structure: " + this.getName() + " (type="
                + this.getTypeName() + ")");
        Indent.incr();
        if (this.getFswDescription() != null) {
            printIndent(out, depth + 1);
            out.println("Description: " + this.getFswDescription());
        }
        for (final IProductDecomField e : elements) {
            e.printType(out, depth + 1);
        }
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
                                                                            throws IOException {
        long bytesRead = 0;
        out.structureStart(this.getName());

        // if the output format is not null, use the specified format

        if (printFormat != null) {
            bytesRead += printFormattedStructure(stream, out, depth);

        } else {

            for (final IProductDecomField element : elements) {
                // has potential to go past end of field
                if (!stream.hasMore()) {
                    log.warn("StructureField ran out of data");
                    break;
                }
                bytesRead += element.printValue(stream, out, depth + 1);
            }
        }
        out.structureEnd();

        return (int) bytesRead;
    }

    /**
     * Print an structure of different elements according to the specified
     * output format.
     * 
     * @param data the data to display
     * @param the number of records to process
     * @param the output stream to which the formatted data is to be written
     * @param the indentation level of the formatted output
     * @return number of bytes processed
     * @throws IOException
     */
    private int printFormattedStructure(final ByteStream data,
            final IDecomOutputFormatter out, final int depth) throws IOException {

        final ArrayList<Object> objectsToPrint = new ArrayList<Object>();

        final SprintfFormat dataFormatter = out.getApplicationContext().getBean(SprintfFormat.class);
        // now get the values and convert them to Java objects

        int bytesUsed = 0;

        for (final IProductDecomField element : elements) {
            // has potential to go past end of field
            if (!data.hasMore()) {
                log.warn("StructureField ran out of data");
                break;
            }
            final ISimpleField df = (ISimpleField) element;
            final Object currentObject = df.getValue(df.getDataType(), data);
            bytesUsed += df.getDataType().getByteLength();
            objectsToPrint.add(currentObject);
        }
        printFormattedLine(objectsToPrint, out, depth + 1, dataFormatter);
        return bytesUsed;
    }

    /**
     * Utility for printing a formatted line.
     * 
     * @param objectsToPrint list of objects to print
     * @param out output formatter to write data to
     * @param dataFormatter data formatter in use
     */
    private void printFormattedLine(final ArrayList<Object> objectsToPrint,
            final IDecomOutputFormatter out, final int depth,
            final SprintfFormat dataFormatter) throws IOException {

        String formattedData = "";
        if (objectsToPrint.size() == 1) {
            formattedData = dataFormatter.anCsprintf(printFormat,
                    objectsToPrint.get(0));
        } else if (objectsToPrint.size() < getItemsToPrint()) {
            final String newFormat = adjustOutputFormatterForCount(objectsToPrint
                    .size());
            formattedData = dataFormatter.sprintf(newFormat, objectsToPrint
                    .toArray());
        } else {
            formattedData = dataFormatter.sprintf(printFormat,
                    objectsToPrint.toArray());
        }

        out.structureValue(formattedData);
    }

    private String adjustOutputFormatterForCount(final int count) {
        final int diff = getItemsToPrint() - count;
        int lastPercentIndex = printFormat.length() - 1;
        for (int i = 0; i < diff; i++) {
            lastPercentIndex = printFormat.lastIndexOf('%',
                    lastPercentIndex);
        }
        final String result = printFormat.substring(0, lastPercentIndex);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getTypeName() {
        return typeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }
}
