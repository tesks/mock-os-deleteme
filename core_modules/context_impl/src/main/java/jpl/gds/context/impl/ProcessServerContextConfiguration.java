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
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.xml.XmlUtility;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * Telemetry Processor Server context configuration
 * See https://wiki.jpl.nasa.gov/display/AMPCS/Context+Concept#ContextConcept-PossibleTypesofContexts
 *
 */
public class ProcessServerContextConfiguration extends SimpleContextConfiguration {

    private TimeComparisonStrategyContextFlag tcFlag;

    /**
     * Constructor that initializes the object from an ApplicationContext.
     *
     * @param appContext the ApplicationContext to get configuration defaults and objects from
     * @param tcFlag Time Comparison Strategy
     */
    public ProcessServerContextConfiguration(final ApplicationContext appContext,
                                             final TimeComparisonStrategyContextFlag tcFlag) {
        super(appContext);
        this.tcFlag = tcFlag;
        this.contextId.setType(ContextConfigurationType.TELEM_PROCESS_SERVER.toString());
        this.contextId.getContextKey().setType(ContextConfigurationType.TELEM_PROCESS_SERVER);

    }

    /**
     * Constructor that initializes the object from a MissionProperties.
     *
     * @param props the MissionProperties to get configuration defaults and objects from
     * @param tcFlag Time Comparison Strategy
     */
    public ProcessServerContextConfiguration(final MissionProperties props, final TimeComparisonStrategyContextFlag tcFlag) {
        super(props);
        this.tcFlag = tcFlag;
        this.contextId.setType(ContextConfigurationType.TELEM_PROCESS_SERVER.toString());
        this.contextId.getContextKey().setType(ContextConfigurationType.TELEM_PROCESS_SERVER);
    }

    /**
     * Set the time comparison strategy context flag
     * @param tcFlag time comparison strategy context flag
     */
    public void setTimeComparisonStrategyContextFlag(TimeComparisonStrategyContextFlag tcFlag) {
        this.tcFlag = tcFlag;
    }

    /**
     * Get the time comparison strategy context flag
     * @return time comparison strategy context flag
     */
    public TimeComparisonStrategyContextFlag getTimeComparisonStrategyContextFlag() {
        return tcFlag;
    }

    @Override
    public MetadataMap getMetadata() {
        final MetadataMap metadata = super.getMetadata();

        //set SSE flag
        metadata.setValue(MetadataKey.SSE_ENABLED,  generalInfo.getSseContextFlag().isApplicationSse());

        //set time comparison strategy
        metadata.setValue(MetadataKey.TIME_COMPARISON_STRATEGY, tcFlag.getTimeComparisonStrategy());

        //TODO alarm override strategy

        return metadata;
    }

    @Override
    public void generateBodyStaxXml(final XMLStreamWriter writer) throws XMLStreamException {

        XmlUtility.writeSimpleElement(writer, "TimeComparisonStrategy", tcFlag.getTimeComparisonStrategy().toString());

        super.generateBodyStaxXml(writer);

    }
}
