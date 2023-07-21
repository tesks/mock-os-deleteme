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

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.shared.formatting.SprintfFormat;

/**
 * A product output formatter that generates no output.
 * 
 *
 */
public class NullProductOutputFormatter extends NullOutputFormatter implements IProductDecomOutputFormatter {

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 * @param format context-aware print format object to use
	 */
	public NullProductOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super(appContext, format);
	}

	@Override
	public void headerStart(final IProductMetadataProvider metadata) {
        // Deliberately empty
	}

	@Override
	public void headerEnd() {
        // Deliberately empty
	}

	@Override
	public void dpoStart(final String name, final int dpoId) {
        // Deliberately empty
	}

	@Override
	public void dpoEnd() {
        // Deliberately empty
	}

	@Override
	public void println() {
        // Deliberately empty
	}

	@Override
	public void setApidDictionary(final IApidDefinitionProvider dict) {
		// Deliberately empty
	}
}
