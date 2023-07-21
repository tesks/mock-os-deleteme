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
package jpl.gds.db.impl.types.cfdp;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.cfdp.IDbCfdpFileGenerationFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileGenerationProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileGenerationUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * {@code DbCfdpFileGenerationFactory} implements IDbCfdpFileGenerationFactory and is used to create CFDP File
 * Generation database objects.
 */
public class DbCfdpFileGenerationFactory
		extends AbstractDbQueryableFactory<IDbCfdpFileGenerationProvider, IDbCfdpFileGenerationUpdater>
		implements IDbCfdpFileGenerationFactory {
	/**
	 * Private Constructor
	 * 
	 * @param appContext
	 *            the Spring Application Context
	 */
	public DbCfdpFileGenerationFactory(final ApplicationContext appContext) {
		super(appContext, IDbCfdpFileGenerationUpdater.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDbCfdpFileGenerationProvider createQueryableProvider() {
		return new DatabaseCfdpFileGeneration(appContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDbCfdpFileGenerationUpdater createQueryableUpdater() {
		return new DatabaseCfdpFileGeneration(appContext);
	}

}
