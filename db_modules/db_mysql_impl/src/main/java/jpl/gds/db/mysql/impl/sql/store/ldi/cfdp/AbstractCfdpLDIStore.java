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
package jpl.gds.db.mysql.impl.sql.store.ldi.cfdp;

import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.mysql.impl.sql.store.ldi.AbstractLDIStore;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.metadata.context.IContextKey;
import org.springframework.context.ApplicationContext;

/**
 * {@code AbstractCfdpLDIStore} is an abstract class that implements common logic to all CFDP LDI stores.
 *
 */
public abstract class AbstractCfdpLDIStore extends AbstractLDIStore {

    /**
     * Creates an instance of AbstractCfdpLDIStore.
     *
     * @param appContext    The information about the current test session
     * @param si            The store identifier
     * @param supportsAsync indicates whether this store supports asynchronous LDI serialization
     */
    public AbstractCfdpLDIStore(final ApplicationContext appContext, final StoreIdentifier si,
                                final boolean supportsAsync) {
        super(appContext, si, supportsAsync);
    }

    /**
     * Return the CFDP context ID.
     *
     * @param contextKey Either the CFDP context or session of a CFDP context.
     * @return CFDP context ID
     */
    protected long extractContextId(final IContextKey contextKey) {

        if (contextKey.getParentNumber() == null) {
            return contextKey.getNumber();
        } else {
            return contextKey.getParentNumber();
        }

    }

    /**
     * Return the CFDP context host ID.
     *
     * @param contextKey Either the CFDP context or session of a CFDP context.
     * @return CFDP context host ID
     */
    protected int extractContextHostId(final IContextKey contextKey) {

        if (contextKey.getParentHostId() == null) {
            return contextKey.getHostId();
        } else {
            return contextKey.getParentHostId();
        }

    }

    /**
     * Return the session ID if it exists.
     *
     * @param contextKey Either the CFDP context or session of a CFDP context.
     * @return Session ID
     */
    protected Long extractSessionIdIfAny(final IContextKey contextKey) {

        if (contextKey.getType() == ContextConfigurationType.SESSION) {
            return contextKey.getNumber();
        } else {
            return null;
        }

    }

    /**
     * Return the session host ID if it exists.
     *
     * @param contextKey Either the CFDP context or session of a CFDP context.
     * @return Session host ID
     */
    protected Integer extractSessionHostIdIfAny(final IContextKey contextKey) {

        if (contextKey.getType() == ContextConfigurationType.SESSION) {
            return contextKey.getHostId();
        } else {
            return null;
        }

    }

    /**
     * Return the session fragment if it exists.
     *
     * @param contextKey Either the CFDP context or session of a CFDP context.
     * @return Session fragment
     */
    protected Integer extractSessionFragmentIfAny(final IContextKey contextKey) {

        if (contextKey.getType() == ContextConfigurationType.SESSION) {
            return contextKey.getFragment();
        } else {
            return null;
        }

    }

}