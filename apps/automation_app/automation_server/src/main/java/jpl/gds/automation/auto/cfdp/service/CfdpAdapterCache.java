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
package jpl.gds.automation.auto.cfdp.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.output.IRawOutputAdapter;

/**
 * Output adapter cache for the CFDP Automation proxy
 */
@Component
public class CfdpAdapterCache {
    Tracer                                         log;

    @Autowired
    private ApplicationContext                     appContext;

    private static Map<Integer, IRawOutputAdapter> store = new HashMap<>();

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);
    }

    /**
     * Puts in cache, the destination entity id's output adapter
     * 
     * @param output
     *            IRawOutputAdapter to use with the entity id
     * @param entityId
     *            destination entity id
     * @return IRawOutputAdapter output destination
     */
    @CachePut(value = "IRawOutputAdapter", key = "#entityId")
    public IRawOutputAdapter put(final IRawOutputAdapter output, final int entityId) {
        log.debug("Storing ", output.getClass().toString(), " in cache for id ", entityId);
        store.put(entityId, output);
        return output;
    }

    /**
     * Gets from cache, the IRawOutputAdapter for an entity id; may return null
     * 
     * @param entityId
     *            destination entity id
     * @return IRawOutputAdapter output destination
     */
    @Cacheable(value = "IRawOutputAdapter", key = "#entityId")
    public IRawOutputAdapter get(final int entityId) {
        log.debug("Getting adapter from cache ID=", entityId);
        return store.get(entityId);
    }

    /**
     * Removes from cache, the IRawOutputAdapter destination for a given entity id
     * 
     * @param entityId
     *            destination entity id
     */
    @CacheEvict(value = "IRawOutputAdapter", key = "#entityId")
    public void evict(final int entityId) {
        log.debug("Removing cached adapter for id ", entityId);
        store.remove(entityId);
    }

}
