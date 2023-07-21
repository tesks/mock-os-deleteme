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

package jpl.gds.context.impl;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextConfigurationType;

import org.springframework.context.ApplicationContext;

/**
 *
 * Telemetry Ingestor Server context configuration
 * See https://wiki.jpl.nasa.gov/display/AMPCS/Context+Concept+-+Persistent+TI+Server#ContextConcept-PersistentTIServer-PossibleTypesofContexts
 *
 */
public class IngestServerContextConfiguration extends SimpleContextConfiguration {

    /**
     * Constructor that initializes the object from an ApplicationContext.
     *
     * @param appContext the ApplicationContext to get configuration defaults and objects from
     */
    public IngestServerContextConfiguration(final ApplicationContext appContext) {
        super(appContext);
        this.contextId.setType(ContextConfigurationType.TELEM_INGEST_SERVER.toString());
        this.contextId.getContextKey().setType(ContextConfigurationType.TELEM_INGEST_SERVER);
    }

    public IngestServerContextConfiguration(final MissionProperties props) {
        super(props);
        this.contextId.setType(ContextConfigurationType.TELEM_INGEST_SERVER.toString());
        this.contextId.getContextKey().setType(ContextConfigurationType.TELEM_INGEST_SERVER);
    }

    @Override
    public MetadataMap getMetadata() {
        final MetadataMap metadata = super.getMetadata();

        //set SSE flag
        metadata.setValue(MetadataKey.SSE_ENABLED,  generalInfo.getSseContextFlag().isApplicationSse());

        return metadata;
    }
}