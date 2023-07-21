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

package jpl.gds.product.impl.builder;

import java.io.File;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductTransactionProvider;
import jpl.gds.product.api.builder.ITransactionLogStorage;
import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.shared.file.ISharedFileLock;

/**
 * Class AbstractBuilderProductObjectFactory
 *
 */
public abstract class AbstractBuilderProductObjectFactory implements IProductBuilderObjectFactory {

    /** The current application context */
	protected ApplicationContext appContext;
	/** Transaction log storage object */
	protected ITransactionLogStorage transactionLogStorage;

	/**
	 * Constructor.
	 * 
	 * @param appContext current application context
	 */
	public AbstractBuilderProductObjectFactory(final ApplicationContext appContext) {
		super();
		this.appContext = appContext;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductBuilderObjectFactory#setTransactionLogStorage(jpl.gds.product.api.builder.ITransactionLogStorage)
	 */
	@Override
	public void setTransactionLogStorage(final ITransactionLogStorage txLogStorage) {
		this.transactionLogStorage = txLogStorage;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductBuilderObjectFactory#getProductMetadata(jpl.gds.product.api.builder.IProductTransactionProvider)
	 */
	@Override
	public IProductMetadataProvider getProductMetadata(final IProductTransactionProvider tx) {
		if (this.transactionLogStorage == null) {
			throw new RuntimeException(new ProductException("The product instance factory requires a call to setTransactionLogStorage after it is created."));
		}

		return transactionLogStorage.getProductMetadata(tx);
	}

	@Override
	public ISharedFileLock createFileLock(final String fileName) {
	    return new ProductFileLock(fileName);
	}

	@Override
    public ISharedFileLock createFileLock(final int vcid, final String transactionId, final File file) {
	    return new ProductFileLock(vcid, transactionId, file);
	}
	
	@Override
    public IProductDataChecksum createProductChecksumCalculator() throws ProductDataChecksumException {
	    return appContext.getBean(IProductDataChecksum.class);
	}
}
