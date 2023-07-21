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
package jpl.gds.product.impl.decom.formatter;

import java.io.PrintStream;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkScetUtility;

/**
 * OutputFormatter is the abstract parent subclass for project and format
 * specific output classes for writing product content according to the product
 * definition during the decom process.
 * 
 */
public abstract class AbstractProductOutputFormatter implements IProductDecomOutputFormatter
{
	/**
	 * APID Dictionary reference.
	 */
	protected IApidDefinitionProvider apids;
	/**
	 * Current application context 
	 */
	protected final ApplicationContext appContext;
	/**
	 * Context-aware print format object to use
	 */
	protected final SprintfFormat format;
	 
	/**
	 * Output stream to be used by this formatter, shared with subclasses.
	 */
    protected PrintStream out = System.out;

    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param format the print formatter object to use
     * 
     */
    public AbstractProductOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super();
		this.appContext = appContext;
		this.format = format;
	}
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.formatter.IDecomOutputFormatter#getApplicationContext()
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		return appContext;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.formatter.IDecomOutputFormatter#getPrintFormatter()
	 */
	@Override
	public SprintfFormat getPrintFormatter() {
		return format;
	}



	/**
     * {@inheritDoc}
     * @see jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter#setApidDictionary(jpl.gds.dictionary.impl.impl.api.apid.IApidDictionary)
     */
    @Override
    public void setApidDictionary(final IApidDefinitionProvider apids) {
    	this.apids = apids;
    }

    /**
     * Sets the output stream.
     * 
     * @param ps the output PrintStream
     */
    @Override
    public void setPrintStream(final PrintStream ps) {
        out = ps;
    }

    /**
     * Gets the output stream.
     * 
     * @return the output PrintStream
     */
    @Override
    public PrintStream getPrintStream() {
        return out;
    }
    
    /**
     * Prints a line feed to the output stream.
     */
    @Override
    public void println() {
        out.println();
    }

    /**
     * Maps a DVT time (SCLK) to a SCET
     * 
     * @param dvt the SCLK to map
     * @param scid the spacecraft ID
     * @return the SCET as a string
     */
    public String getDvtScet(final ISclk dvt, final int scid) {
        final IAccurateDateTime scet = SclkScetUtility.getScet(dvt, null, scid);
        if (scet == null) {
            return "[BAD_TIME_CORRELATION]";
        } else {
            final String result = scet.getFormattedScet(true);
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    abstract public void headerStart(IProductMetadataProvider metadata);

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter#headerEnd()
     */
    @Override
    abstract public void headerEnd();

    /**
     * Gets the product type name for the given APID
     * 
     * @param apid the apid number
     * @return the product type name, or NO_TYPE if apid is not found
     */
    protected String getProductType(final int apid) {
        String type = null;
    
        if (apids != null) {
            /* Go through APID definition to get name. */
            type = apids.getApidDefinition(apid).getName();
        }
        if (type == null) {
            return "NO_TYPE";
        }
        return type;
    }
}
