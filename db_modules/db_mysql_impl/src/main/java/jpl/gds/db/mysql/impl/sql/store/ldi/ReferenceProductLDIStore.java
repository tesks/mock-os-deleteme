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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IReferenceProductMetadataProvider;
import jpl.gds.shared.database.BytesBuilder;


/**
 * This is the REFERENCE-specific database write/storage interface to the Product
 * table in the REFERENCE MPCS database. This class will receive an input product
 * and write it to the Product table in the database. Done via LDI.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 */
public class ReferenceProductLDIStore extends AbstractProductLDIStore implements IProductLDIStore
{
    /**
     * Creates an instance of ReferenceProductLDIStore.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public ReferenceProductLDIStore(final ApplicationContext appContext)
    {
        super(appContext, IProductLDIStore.STORE_IDENTIFIER);
    }


    /**
     * Get the fields.
     *
     * @return The comma-separated field list
     */
    @Override
	public String getFields()
    {
        return DB_REFERENCE_PRODUCT_DATA_FIELDS;
    }


    /**
     * Insert Product.
     *
     * @param tempPmd
     *            The product to be inserted
     * @throws DatabaseException
     *             if an error occurs inserting product into database
     */
    @Override
    public void insertProduct(final IProductMetadataProvider tempPmd) throws DatabaseException
    {
        final IReferenceProductMetadataProvider pmd = (IReferenceProductMetadataProvider)tempPmd;
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        if (pmd == null)
        {
            throw new IllegalArgumentException("Null input product metadata");
        }

        try
        {
            synchronized(this)
            {
                if (!archiveController.isUp())
                {
                    throw new IllegalStateException(
                                  "This connection has already been closed");
                }

                final IReferenceProductMetadataProvider mpmd = pmd;

                // Set all the data in the LDI line

                setCommonInsertFields(mpmd);

                final BytesBuilder bb = getBB();

                /** MPCS-6809 As unsigned */
                bb.insertLongAsUnsigned(mpmd.getChecksum());
                bb.insertSeparator();
                bb.insertLongAsUnsigned(mpmd.getCfdpTransactionId());
                bb.insertSeparator();
                bb.insertLongAsUnsigned(mpmd.getFileSize());
                bb.insertTerminator();

                // Add the line to the LDI batch

                writeToStream(bb);
            }
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new DatabaseException("Error inserting REFERENCE Product record into " +
                                   "database: "                                +
                                   e);
        }
    }
}
