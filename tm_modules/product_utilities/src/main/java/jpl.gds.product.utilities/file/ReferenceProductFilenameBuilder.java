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
package jpl.gds.product.utilities.file;

import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilder;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.file.ProductFilenameException;

/**
 * 9/26/2016 - Very simple builder implementation for the reference product builder.
 *
 */
public class ReferenceProductFilenameBuilder extends AbstractProductFilenameBuilder implements IProductFilenameBuilder {


	/**
	 * Constructor
	 * @param appContext the ApplicationContext to get configuration defaults from
	 */
	public ReferenceProductFilenameBuilder(final ApplicationContext appContext) {
		super(appContext);
	}

	@Override
	public IProductFilename build() throws ProductFilenameException {
		return new ReferenceProductFilename(isPartial, productPath, productName);
	}
}
