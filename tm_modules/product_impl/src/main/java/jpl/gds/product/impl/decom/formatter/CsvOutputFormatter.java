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
package jpl.gds.product.impl.decom.formatter;

import java.util.StringTokenizer;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;

/**
 * 
 * TextOutputFormatter is the base class from which product decom formatters
 * that output CSV text can be extended. It provides basic text formatting
 * methods for data products.
 * 
 *
 */
public class CsvOutputFormatter extends AbstractProductOutputFormatter {

    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param format print formatter object to use
     */
    public CsvOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super(appContext, format);
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.formatter.AbstractProductOutputFormatter#headerEnd()
     */
    @Override
    public void headerEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void startOutput() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value) {
        out.print("," + value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value, final String units) {
       nameValue(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayStart(final String name, final int length) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayEnd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndex(final String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndexEnd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void endOutput() {
    	out.println();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureStart(final String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureEnd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void addressValue(final int address, final String value) {
        out.print("," + value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureValue(final String value) {
        final StringTokenizer tokens = new StringTokenizer(value, "\n");
        while (tokens.hasMoreTokens()) {
            out.print("," + tokens.nextToken());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void headerStart(final IProductMetadataProvider metadata) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoEnd() {
        out.print("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoStart(final String name, final int dpoId) {
        out.print(name + "," + dpoId);
    }
}
