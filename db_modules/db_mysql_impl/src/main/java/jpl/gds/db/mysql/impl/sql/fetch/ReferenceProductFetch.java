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
package jpl.gds.db.mysql.impl.sql.fetch;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.types.IDbProductMetadataFactory;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IReferenceProductMetadataUpdater;

/**
 * The REFERENCE mission specific adaptation of the general product fetch application. See the
 * parent classes for usage details.
 *
 */
public class ReferenceProductFetch extends AbstractMySqlProductFetch implements IDbProductFetch
{
//	/** The SELECT fields that will be used to retrieve product information
//	 *  from the database.
//	 */ 
//	private final static String selectFields = 
//			tableAbbrev + ".checksum, " +
//			tableAbbrev + ".cfdpTransactionId, " +
//			tableAbbrev + ".fileSize ";

    private final IDbProductMetadataFactory dbProductMetadataFactory;

	/**
     * Creates an instance of ReferenceProductFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     */
	public ReferenceProductFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        dbProductMetadataFactory = appContext.getBean(IDbProductMetadataFactory.class);
	}

	/**
     * Creates an instance of ReferenceProductFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public ReferenceProductFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IProductMetadataUpdater> T  createEmptyMetadata() {
        return (T) dbProductMetadataFactory.createQueryableUpdater();
    }

    /**
     * Create the mission specific product metadata object and populate it with
     * any mission specific values
     * 
     * @return A mission-specific product metadata object with mission-specific
     *         values already set
     * 
     * @throws DatabaseException
     *             If the creation and population of the metadata fails
     */
    @Override
    protected <T extends IProductMetadataUpdater> T createAndPopulateMetadata() throws DatabaseException {
        return populateMetadata(null);
    }

    /**
     * Populate specified metadata object with any mission specific values
     * 
     * @param pmd
     *            the specified metadata object to populate
     * @return A mission-specific product metadata object with mission-specific
     *         values already set
     * 
     * @throws DatabaseException
     *             If the creation and population of the metadata fails
     */
    @Override
	protected <T extends IProductMetadataUpdater> T populateMetadata(T pmd) throws DatabaseException {
        if (null == pmd) {
            pmd = createEmptyMetadata();
        }
        
        super.populateMetadata(pmd);

        /** MPCS-6809 As unsigned */
        /** MPCS-7639  Use new method */

        pmd.setChecksum(getUnsignedLong(results, DB_PRODUCT_TABLE_ABBREV + ".checksum").longValue());

        ((IReferenceProductMetadataUpdater)pmd).setCfdpTransactionId(getUnsignedLong(results, DB_PRODUCT_TABLE_ABBREV + ".cfdpTransactionId").longValue());

        pmd.setFileSize(getUnsignedLong(results, DB_PRODUCT_TABLE_ABBREV + ".fileSize").longValue());

        return pmd;
    }
}
