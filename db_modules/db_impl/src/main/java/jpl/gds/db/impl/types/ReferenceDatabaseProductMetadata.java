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
package jpl.gds.db.impl.types;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IReferenceProductMetadataUpdater;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.shared.holders.SessionFragmentHolder;


public class ReferenceDatabaseProductMetadata extends DatabaseProductMetadata implements
        IReferenceProductMetadataUpdater {
    
    /**
     * @param appContext
     *            the Spring Application Context
     */
    public ReferenceDatabaseProductMetadata(final ApplicationContext appContext) {
        super(appContext, appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater());
    }

    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the Product Metadata to be wrapped by this ReferenceDatabaseProductMetadata object
     */
    public ReferenceDatabaseProductMetadata(final ApplicationContext appContext, final IReferenceProductMetadataUpdater md) {
        super(appContext, md);
    }

    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the Product Metadata to be wrapped by this ReferenceDatabaseProductMetadata object
     * @param sessionId
     *            the session ID
     * @param fragment
     *            the session fragment
     * @param sessionHost
     *            the session host
     */
    public ReferenceDatabaseProductMetadata(final ApplicationContext appContext, final IReferenceProductMetadataUpdater md, final Long sessionId,
            final SessionFragmentHolder fragment, final String sessionHost) {
        super(appContext, md, sessionId, fragment, sessionHost);
    }
    
    /**
     * @param appContext
     *            the Spring Application Context
     * @param md
     *            the Product Metadata to be wrapped by this ReferenceDatabaseProductMetadata object
     * @param sessionId
     *            the session ID
     * @param sessionHost
     *            the session host
     */
    public ReferenceDatabaseProductMetadata(final ApplicationContext appContext, final IReferenceProductMetadataUpdater md, final Long sessionId,
            final String sessionHost) {
        super(appContext, md, sessionId, sessionHost);
    }
    
    @Override // IReferenceProductMetadataUpdater
    public void setSourceEntityId(final int get_u8) {
        ((IReferenceProductMetadataUpdater)md).setSourceEntityId(get_u8);
    }

    @Override // IReferenceProductMetadataUpdater
    public void setCfdpTransactionId(final long transId) {       
        ((IReferenceProductMetadataUpdater)md).setCfdpTransactionId(transId);
    }


    @Override // IReferenceProductMetadataUpdater
    public int getSourceEntityId() {
        return ((IReferenceProductMetadataUpdater)md).getSourceEntityId();
    }
    

    @Override // IReferenceProductMetadataProvider
    public long getCfdpTransactionId() {
        return ((IReferenceProductMetadataUpdater)md).getCfdpTransactionId();
    }
}
