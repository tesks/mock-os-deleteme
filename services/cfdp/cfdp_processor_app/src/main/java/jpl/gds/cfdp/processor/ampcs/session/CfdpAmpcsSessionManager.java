/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.processor.ampcs.session;

import cfdp.engine.TransID;
import jpl.gds.context.api.IContextConfiguration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code CfdpAmpcsSessionManager} is a singleton bean to keep track of AMPCS sessions mapped to CFDP transactions. To
 * not introduce additional interfaces and their dependencies in JavaCFDP, this manager will be married to the
 * StatManager, with the StatManager the only writer of the mappings held herein.
 *
 * @since 8.1
 */
@Service
public class CfdpAmpcsSessionManager {

    private final Map<TransID, IContextConfiguration> sessionMap;

    /**
     * Constructor
     */
    public CfdpAmpcsSessionManager() {
        sessionMap = new HashMap<>();
    }

    /**
     * Add transaction
     * @param transId Transaction ID
     */
    public void addTransaction(final TransID transId) {
        sessionMap.put(transId, null);
    }

    /**
     * Remove transaction
     * @param transId Transaction ID
     */
    public void removeTransaction(final TransID transId) {
        sessionMap.remove(transId);
    }

    /**
     * Update transaction
     * @param transId Transaction ID
     * @param sessionConfig IContextConfiguration
     * @return True if the trans ID was in the map
     */
    public boolean updateTransaction(final TransID transId, IContextConfiguration sessionConfig) {

        if (!sessionMap.containsKey(transId)) {
            return false;
        }

        sessionMap.put(transId, sessionConfig);
        return true;
    }

    /**
     * Get session by transaction ID
     * @param transId Transaction ID
     * @return IContextConfiguration
     */
    public IContextConfiguration getSession(final TransID transId) {
        return sessionMap.get(transId);
    }

    /**
     * Get session by ID
     * @param sessionId Session ID
     * @return IContextConfiguration
     */
    public IContextConfiguration getSessionById(final long sessionId ) {
        for (Map.Entry<TransID,IContextConfiguration> entry : sessionMap.entrySet()) {
            final IContextConfiguration crt = entry.getValue();
            if(crt != null && crt.getContextId().getContextKey().getNumber() == sessionId) {
                    return crt;
                }
            }

        return null;
    }

}
