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
/**
 *
 */
package jpl.gds.context.impl;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextConfigurationFactory;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import org.springframework.context.ApplicationContext;

/**
 * <code>ContextConfigurationFactory</code> is used to create
 * <code>ISimpleContextConfiguration</code> and
 * <code>IContextConfiguration</code> objects.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * An <code>ISimpleContextConfiguration</code> object is the multi-mission
 * representation of a basic context configuration, and
 * <code>IContextConfiguration</code> is the full representation of a context
 * configuration needed by a context-intensive AMPCS application.
 * <p>
 * This class contains only static methods. Once the
 * <code>ISimpleContextConfiguration</code> object is returned by this factory,
 * its additional members can be set through the methods in the
 * <code>ISimpleContextConfiguration</code> interface.
 *
 * @since R8
 *
 * @see ISimpleContextConfiguration
 * @see IContextConfiguration
 */
public class ContextConfigurationFactory implements IContextConfigurationFactory {

    private final ApplicationContext   appContext;
    private final MissionProperties    missionProperties;
    private final ConnectionProperties connectionProperties;

    /**
     * Constructor
     * @param appContext Spring Application Context
     */
    public ContextConfigurationFactory(ApplicationContext appContext) {
        super();
        this.appContext = appContext;
        this.missionProperties = appContext.getBean(MissionProperties.class);
        this.connectionProperties = appContext.getBean(ConnectionProperties.class);
    }

    @Override
    public ISimpleContextConfiguration createContextConfiguration(final ContextConfigurationType type,
                                                                  final boolean ephemeral) {

        switch (type) {
            case SIMPLE:
            case CFDP:
                return ephemeral ? new SimpleContextConfiguration(missionProperties) : new SimpleContextConfiguration(
                        appContext);
            case GENERIC_FULL:
                return ephemeral ? new ContextConfiguration(missionProperties, connectionProperties,
                        false) : new ContextConfiguration(appContext);
            case TELEM_INGEST_SERVER:
                return ephemeral ? new IngestServerContextConfiguration(
                        missionProperties) : new IngestServerContextConfiguration(appContext);
            case TELEM_PROCESS_SERVER:
                TimeComparisonStrategyContextFlag tcFlag = appContext.getBean(TimeComparisonStrategyContextFlag.class);
                return ephemeral ? new ProcessServerContextConfiguration(missionProperties, tcFlag) :
                        new ProcessServerContextConfiguration(appContext, tcFlag);
            case SESSION:
                throw new IllegalArgumentException("Session context configuration is not supported by this factory");

            default:
                throw new IllegalArgumentException("Unsupported context configuration type: " + type);
        }

    }

}