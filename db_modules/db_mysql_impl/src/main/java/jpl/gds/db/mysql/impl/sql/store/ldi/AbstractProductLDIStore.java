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

import java.sql.SQLException;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.ProductStatusType;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;


/**
 * This is the mission-independent superclass for the database write/storage
 * interface to the Product table in the MPCS database. This class will receive
 * an input product and write it to the Product table in the database. This is
 * done via LDI.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 */
public abstract class AbstractProductLDIStore extends AbstractLDIStore implements IProductLDIStore
{
    private static final int APIDNAME_LENGTH =   64;
    private static final int FILE_LENGTH     = 1024;
    private static final int SC_LENGTH       =    8;

    private final BytesBuilder _bb = new BytesBuilder();
    private final Integer      _sessionVcid;  // NULL means not set


    /**
     * Creates an instance of AbstractProductLDIStore
     * 
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the Store Identifier associated with this LDI Store
     */
    protected AbstractProductLDIStore(final ApplicationContext appContext, final StoreIdentifier si)
    {
    	/* 
    	 * MPCS-7135 - Add second argument to indicate this
    	 * store does not operate asynchronously.
    	 */
        super(appContext, si, false);
        _sessionVcid = appContext.getBean(IContextFilterInformation.class).getVcid();
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.IProductLDIStore#getFields()
     */
    @Override
    public abstract String getFields();


    /**
     * Get BytesBuilder object.
     *
     * @return The BytesBuilder object
     */
    protected BytesBuilder getBB()
    {
        return _bb;
    }


    /**
     * Insert a product into the database
     *
     * @param pmd
     *            The metadata for the product to insert
     * @throws DatabaseException
     *             if an error occurs during the insertion of a product
     */
    @Override
    public abstract void insertProduct(final IProductMetadataProvider pmd) throws DatabaseException;


    /**
     * Set all the common field data on the templated SQL prepared statement.
     *
     * WARNING! Overridden by MSL and maybe others.
     *
     * @param pmd
     *            The product metadata to retrieve database information from
     *
     * @throws DatabaseException
     *             if a database error occurs
     */
    protected void setCommonInsertFields(final IProductMetadataProvider pmd)
        throws DatabaseException
    {
        if (pmd == null)
        {
            throw new IllegalArgumentException("Null input product metadata");
        }

        _bb.clear();

        // Format the common information as a partial line for LDI

        final IAccurateDateTime creationTime = pmd.getProductCreationTime();
        final IAccurateDateTime scet         = pmd.getScet();
        final long             dvtCoarse    = pmd.getDvtCoarse();
        final long             dvtFine      = pmd.getDvtFine();
        final IAccurateDateTime ert          = pmd.getErt();   
        ProductStatusType      groundStatus = pmd.getGroundStatus();

        if (groundStatus == null)
        {
            groundStatus = ProductStatusType.UNKNOWN;
        }

        // MPCS-10780 - 0 Insert session ID, host ID, and fragment
        _bb.insert(pmd.getSessionId());
        _bb.insertSeparator();
        _bb.insert(pmd.getSessionHostId() != null ? pmd.getSessionHostId() : 0);
        _bb.insertSeparator();

        _bb.insert(pmd.getSessionFragment().getValue());
        _bb.insertSeparator();

        final IAccurateDateTime rct = new AccurateDateTime();

        try
        {
            _bb.insertDateAsCoarseFineSeparate(rct);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(Markers.DB, dateExceedsWarning("Product.rct", null,
                                          rct));
        }

        try
        {
            _bb.insertDateAsCoarseFineSeparate(creationTime);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(Markers.DB, dateExceedsWarning("Product.creationTime", null,
                                          creationTime));
        }

        /** MPCS-8384 Modify for extended */
        try
        {
            if (_extendedDatabase)
            {
                _bb.insertScetAsCoarseFineSeparate(scet);
            }
            else
            {
                _bb.insertScetAsCoarseFineSeparateShort(scet);
            }
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(Markers.DB, scetExceedsWarning("Product.scet", null, scet, ert));
        }

        final int vcid = Math.max(pmd.getVcid(), 0);

        checkVcid(vcid);

        try {
            _bb.insert(vcid);
            _bb.insertSeparator();

            _bb.insert(GDR.getIntFromBoolean(pmd.isPartial()));
            _bb.insertSeparator();

            _bb.insert(pmd.getApid());
            _bb.insertSeparator();

            /** MPCS-5153  */
            _bb.insertTextOrNullComplainReplace(checkLength("Product.apidName", APIDNAME_LENGTH, pmd.getProductType()));
            _bb.insertSeparator();

            _bb.insert(pmd.getSequenceId());
            _bb.insertSeparator();
            _bb.insert(pmd.getSequenceVersion());
            _bb.insertSeparator();
            _bb.insert(pmd.getCommandNumber());
            _bb.insertSeparator();
            _bb.insert(pmd.getXmlVersion());
            _bb.insertSeparator();
            _bb.insert(pmd.getTotalParts());
            _bb.insertSeparator();

            _bb.insertSclkAsCoarseFineSeparate(dvtCoarse, dvtFine);

            /** MPCS-5153  */
            _bb.insertTextComplainReplace(checkLength("Product.fullPath", FILE_LENGTH, pmd.getFullPath()));
            _bb.insertSeparator();

            /** MPCS-5153  */
            _bb.insertTextComplainReplace(checkLength("Product.fileName", FILE_LENGTH, pmd.getFilename()));
            _bb.insertSeparator();

            try {
                _bb.insertErtAsCoarseFineSeparate(ert);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, ertExceedsWarning("Product.ert", null, ert));
            }

            /** MPCS-5153  */
            _bb.insertTextComplainReplace(groundStatus.toString());
            _bb.insertSeparator();

            /** MPCS-5153  */
            _bb.insertTextOrNullComplainReplace(checkLength("Product.sequenceCategory", SC_LENGTH,
                                                            pmd.getSequenceCategory()));

            _bb.insertSeparator();

            _bb.insertLongOrNull(pmd.getSequenceNumber());
            _bb.insertSeparator();

            final String version = pmd.getProductVersion();

            if (version == null) {
                _bb.insert(Double.valueOf(IProductLDIStore.VERSION_DEFAULT.toString()));
            }
            else {
                _bb.insert(Double.valueOf(version));
            }

            _bb.insertSeparator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    @Override
    protected void startResource() {
        super.startResource();

        handler = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m) {
                try {
                    handleProductAssembledMessage((IProductAssembledMessage) m);
                }
                catch (final DatabaseException e) {
                    e.printStackTrace();
                }
            }
        };

        handler2 = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m) {
                try {
                    handlePartialProductMessage((IPartialProductMessage) m);
                }
                catch (final DatabaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        if (messagePublicationBus != null) {
            // Subscribe to product messages on the internal bus
            messagePublicationBus.subscribe(ProductMessageType.ProductAssembled, handler);

            // Subscribe to partial product messages on the internal bus
            messagePublicationBus.subscribe(ProductMessageType.PartialProduct, handler2);
        }
    }

    @Override
    protected void stopResource() {
        super.stopResource();

        if (messagePublicationBus != null) {
            // Unsubscribe from product messages on the internal bus
            messagePublicationBus.unsubscribe(ProductMessageType.ProductAssembled, handler);

            // Unsubscribe from partial product messages on the internal bus
            messagePublicationBus.unsubscribe(ProductMessageType.PartialProduct, handler2);
        }

        if (archiveController.getProductStore() == null) {
            archiveController.setProductFields(this.getFields());
        }
    }

    /**
     * Handle a product message from the internal bus by extracting the
     * information and inserting it into the database.
     *
     * @param pam
     *            The product message to consume
     * @throws DatabaseException
     *             if an exception occurs updating the database
     */
    protected void handleProductAssembledMessage(
                                                 final IProductAssembledMessage pam)
            throws DatabaseException
    {
        if (pam == null)
        {
            throw new IllegalArgumentException("Null input message");
        }

        final IProductMetadataProvider bmd = pam.getMetadata();
        insertProduct(bmd);
        endSessionInfo.updateTimes(bmd.getErt(), bmd.getScet(), bmd.getSclk());
    }


    /**
     * Handle a partial product message from the internal bus by extracting the
     * information and inserting it into the database.
     *
     * @param ppm
     *            The partial product message to consume
     * @throws DatabaseException
     *             if an exception occurs updating the database
     */
    protected void handlePartialProductMessage(final IPartialProductMessage ppm) throws DatabaseException
    {
        if (ppm == null)
        {
            throw new IllegalArgumentException("Null input message");
        }

		final IProductMetadataProvider bmd = ppm.getMetadata();
        insertProduct(bmd);
        endSessionInfo.updateTimes(bmd.getErt(), bmd.getScet(), bmd.getSclk());
    }


    /**
     * Insert attitude protecting against infinity and NaN.
     *
     * @param attitude The attitude element
     * @param bb       The bytes builder in which to insert
     */
    protected void safeInsertAttitude(final double       attitude,
                                             final BytesBuilder bb)
    {
        if (Double.isNaN(attitude))
        {
            trace.error(Markers.DB, "NaN attitude set to zero");

            bb.insert(0.0f);
        }
        else if (Double.isInfinite(attitude))
        {
            trace.error(Markers.DB, "Infinite attitude set to zero");

            bb.insert(0.0f);
        }
        else
        {
            bb.insert(attitude);
        }
    }


    /**
     * Check VCID of Product with VCID from configuration.
     *
     * @param vcid VCID of Product
     */
    protected void checkVcid(final int vcid)
    {
        if ((_sessionVcid != null) && (_sessionVcid != vcid))
        {
            trace.warn(Markers.DB, "Product VCID " , vcid , " does not match Session VCID " ,
                       _sessionVcid);
        }
    }
}
