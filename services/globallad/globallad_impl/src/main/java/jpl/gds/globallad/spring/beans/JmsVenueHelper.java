/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.globallad.spring.beans;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.globallad.GlobalLadProperties;
import org.springframework.context.ApplicationContext;

/**
 * JMS Venue initialization helper. Venue is not configured for GLAD as it is in downlink sessions, and topic strings
 * are built from mission properties, venue configuration, and context identification. This helper  sets these values
 * for the default JMS topics to be configured correctly.
 */
public class JmsVenueHelper {

    /**
     * Initialize the downlink venue for JMS topic creation
     *
     * @param applicationContext
     * @param gladConfig
     */
    static void initJmsVenue(final ApplicationContext applicationContext, final GlobalLadProperties gladConfig) {
        final IVenueConfiguration vc = applicationContext.getBean(IVenueConfiguration.class);
        // Set defaults
        final MissionProperties missionProperties = applicationContext.getBean(MissionProperties.class);
        final VenueType         vt                = missionProperties.getDefaultVenueType();
        vc.setVenueType(missionProperties.getDefaultVenueType());
        if (vt != null && vt.hasTestbedName()) {
            vc.setTestbedName(missionProperties.getDefaultTestbedName(vt, gladConfig.getJmsHostName()));
        }
        // Override from CLI
        final VenueType vtFromConf = gladConfig.getVenueType();
        if (gladConfig.getVenueType() != null) {
            vc.setVenueType(gladConfig.getVenueType());
            if (vtFromConf.hasTestbedName()) {
                if (gladConfig.getTestbedName() != null) {
                    vc.setTestbedName(gladConfig.getTestbedName());
                } else {
                    vc.setTestbedName(missionProperties.getDefaultTestbedName(vtFromConf, gladConfig.getJmsHostName()));
                }
            }
        }

        if (gladConfig.getDownlinkStreamType() != null) {
            vc.setDownlinkStreamId(gladConfig.getDownlinkStreamType());
        }

        if (gladConfig.getJmsHostName() != null) {
            // topic host name comes out of context identification, override there
            final IContextIdentification contextIdentification = applicationContext.getBean(IContextIdentification.class);
            contextIdentification.setHost(gladConfig.getJmsHostName());
        }
    }
}
