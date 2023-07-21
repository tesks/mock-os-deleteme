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
package jpl.gds.product.api.decom;

import java.io.IOException;
import java.io.PrintStream;

import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;
/**
 * The IProductDecomField interface is to be implemented by all classes that
 * implement data fields for decommutation.
 * 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IProductDecomField defines the methods that must be implemented by all data field
 * decommutation classes. All other decom field interfaces extend this one.
 * IProductDecomField defines several descriptions that can be attached to any decom field,
 * methods for printing the field definition, methods for extracting the field
 * value from a data stream, and methods that affect general formatting of
 * decom fields as they are extracted from the data stream. 
 * 
 */
public interface IProductDecomField {

    /**
     * Gets the decommutation type of this field, which indicates whether it
     * is a primitive field, an array, a structure, etc. Note that there is no
     * method to set this value. It must be established when the implementing
     * IProductDecomField object is instantiated.
     * 
     * @return deocm field type enumeration value
     */
    public abstract ProductDecomFieldType getDecomFieldType();
    
    /**
     * Gets the print format string. This is a C-style printf formatter
     * for the field.  Note that simple fields will have formatters that
     * are for one replacement value (e.g., %s) but formatters for structures and
     * arrays may contain multiple replacement values (e.g., %s = %d (%d)).
     * 
     * @return the print formatter string, or null if none set, in which case
     * Java default formatting will apply to the output data
     */
    public abstract String getPrintFormat();

    /**
     * Sets the formatter for the value of the field, to be used in generating
     * output. This is a C-style printf formatter
     * for the field.  Note that simple fields will have formatters that
     * are for one replacement value (e.g., %s) but formatters for structures and
     * arrays may contain multiple replacement values (e.g., %s = %d (%d)").
     * 
     * @param printFormat the format string (e.g., %s, %d, etc), or null
     * to use Java default formatting in the output
     */
    public void setPrintFormat(final String printFormat);

    /**
     * Gets the flight software description of this field..
     * 
     * @return the description text; may be null if none set
     */
    public abstract String getFswDescription();

    /**
     * Sets the flight software description of this field.
     * 
     * @param fswDescription the description text to set
     */
    public abstract void setFswDescription(String fswDescription);

    /**
     * Gets the system description of this field.
     * 
     * @return the description text; may be null if none set
     */
    public abstract String getSysDescription();

    /**
     * Sets the system description of this field.
     * 
     * @param sysDescription the description text
     */
    public abstract void setSysDescription(String sysDescription);

    /**
     * Prints a formatted description of the field definition to the given stream.
     * 
     * @param out the output stream to write to
     * @param depth the indentation level for formatted output
     * @throws IOException if there is a problem with the writing
     */
    public abstract void printType(PrintStream out, int depth)
            throws IOException;

    /**
     * Gets the size of the data value represented by this field definition, in
     * bytes.  For primitive fields, this will be the size of the primitive. For
     * pure container fields, this will be the sum of the sizes of the fields in
     * the container. For fixed array fields, it will be the size of the entire 
     * array. for variable length arrays, it will be the size of one element in the
     * array. (Note that each array element may contain multiple decom fields.)
     * 
     * @return the value size, in bytes
     */
    public abstract int getValueSize();

    /**
     * Prints the formatted field value to output stream and returns number of
     * bytes read. This method effectively consumes the field bytes from the
     * given input data. The field data is assumed to start at the beginning of
     * the ByteArraySlice.
     * 
     * @param data the ByteArraySlice containing the field value
     * @param out the OutputFormatter to send the formatted field value to
     * @return the number of bytes used from the ByteArraySlice
     * @throws IOException if there is a problem with the printing
     */
    public abstract int printValue(ByteArraySlice data, IDecomOutputFormatter out)
            throws IOException;

    /**
     * Prints the formatted field value to an output stream and returns the
     * number of bytes read/used. This method effectively consumes the field
     * bytes from the given input stream. The field data is assumed to start at
     * the beginning of the ByteStream.
     * 
     * @param stream the ByteStream containing the field value
     * @param out the OutputFormatter to send the formatted field value to
     * @param depth depth of indentation for formatted output
     * @return the number of bytes used from the ByteStream
     * @throws IOException if there is a problem with the printing
     */
    public abstract int printValue(ByteStream stream, IDecomOutputFormatter out, int depth)
            throws IOException;
    
    /**
     * Prints the formatted field value to an output stream and returns the
     * number of bytes read/used. This method effectively consumes the field
     * bytes from the given input stream. The field data is assumed to start at
     * the beginning of the ByteStream.
     * 
     * @param stream the ByteStream containing the field value
     * @param out the OutputFormatter to send the formatted field value to
     * @return the number of bytes used from the ByteStream
     * @throws IOException if there is a problem with the printing
     */
    public abstract int printValue(ByteStream stream, IDecomOutputFormatter out)
            throws IOException;

    /**
     * Prints a formatted description of the field to the given stream.
     * 
     * @param out the output stream to write to
     * @throws IOException if there is a problem with the printing
     */
    public abstract void printType(PrintStream out) throws IOException;

    /**
     * Sets the field name.
     * 
     * @param name the name
     */
    public abstract void setName(String name);

    /**
     * Gets the field name.
     * 
     * @return the name String
     */
    public abstract String getName();

    /**
     * @return the title
     */
    public abstract String getTitle();

    /**
     * @param title the title to set
     */
    public abstract void setTitle(String title);

    /**
     * Suppresses the printing of the field name to an output stream.
     * 
     * @param suppressName true to suppress; false to enable
     */
    public abstract void setSuppressName(boolean suppressName);

    /**
     * Gets the value of the suppressName flag.
     * 
     * @return true if generation of name is suppressed; false otherwise
     */
    public abstract boolean getSuppressName();

    /**
     * Gets the raw value of a numeric field in the given data stream.
     * 
     * @param dataType the dictionary definition type of the data
     * @param stream the ByteStream to read the value from
     * @return the field value as a Number object; the type will vary based upon
     *         the field type
     */
    public abstract Object getValue(DecomDataType dataType, ByteStream stream);

    /**
     * Retrieves the fswName (flight software name) member
     * 
     * @return the name String
     */
    public abstract String getFswName();

    /**
     * Sets the fswName (flight software name) member.
     * 
     * @param fswName the new name
     */
    public abstract void setFswName(String fswName);

}