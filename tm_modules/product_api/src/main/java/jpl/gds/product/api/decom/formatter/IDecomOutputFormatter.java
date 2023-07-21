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
/*
 * File: OutputFormatter.java
 * Created on Feb 27, 2006
 * 
 * Author: Marti DeMore
 *
 */
package jpl.gds.product.api.decom.formatter;

import java.io.PrintStream;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 *
 * IDecomOutputFormatter is the interface used to generate output from the generic
 * dictionary decom classes.
 */
public interface IDecomOutputFormatter {
    
    /**
     * Sets the output stream.
     * 
     * @param ps the output PrintStream
     */
    public void setPrintStream(PrintStream ps);

    /**
     * Gets the output stream.
     * 
     * @return the output PrintStream
     */
    public PrintStream getPrintStream();

    /**
     * Writes any overall output pre-amble.
     * 
     */
    public void startOutput();

    /**
     * Write a field in name-value format.
     * 
     * @param name the field name
     * @param value the field value
     */
    public void nameValue(String name, String value);

    /**
     * Writes a field in name-value-unit format.
     * 
     * @param name the field name
     * @param value the field value
     * @param units the field's units
     */
    public void nameValue(String name, String value, String units);

    /**
     * Writes a field in address-value-unit format.
     * 
     * @param address the field address
     * @param value the field value
     */
    public void addressValue(int address, String value);

    /**
     * Writes the header for an array field.
     * 
     * @param name the name of the array
     * @param length the length of the array
     */
    public void arrayStart(String name, int length);

    /**
     * Writes the trailer for an array field.
     */
    public void arrayEnd();

    /**
     * Writes an array index header.
     * 
     * @param name the array index name or number
     */
    public void arrayIndex(String name);

    /**
     * Writes an array index trailer.
     */
    public void arrayIndexEnd();

    /**
     * Writes a structure header.
     * 
     * @param name the name of the structure
     */
    public void structureStart(String name);

    /**
     * Writes a structure trailer.
     */
    public void structureEnd();

    /**
     * Writes the value of a structure or other free-form text value.
     * 
     * @param value value to write
     */
    public void structureValue(String value);

    /**
     * Writes any final overall output.
     */
    public void endOutput();
    
    
    /**
     * Gets the application context in use by this formatter.
     * 
     * @return application context
     */
    public ApplicationContext getApplicationContext();
    
    /**
     * Gets the context-aware print format object in use by this formatter.
     * 
     * @return print format object
     */
    public SprintfFormat getPrintFormatter();

    /**
     * Gets the message bus out of the context as returned by getApplicationContext.
     * 
     * @return message bus
     */
    public default IMessagePublicationBus getMessagePublicationBus() {
    	return getApplicationContext().getBean(IMessagePublicationBus.class);
    }
    
}
