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
package jpl.gds.session.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;

/**
 * A utility class for miscellaneous session related tasks.
 * 
 */
public class SessionConfigurationUtilities {
    private final ApplicationContext appContext;
    private final MissionProperties  missionProps;

    /**
     * @param appContext
     *            the Spring Application Context
     */
    public SessionConfigurationUtilities(final ApplicationContext appContext) {
        super();
        this.appContext = appContext;
        this.missionProps = appContext.getBean(MissionProperties.class);
    }

    /**
     * Creates a map of current Context Configuration information
     * 
     * @param contextConfig
     *            the current Context Configuration
     * 
     * @return a map of current Context Configuration information
     */
    public Map<String, Object> assembleSessionConfigData(final ISimpleContextConfiguration contextConfig) {
        // Dictionary fields will be marked as overridden in chill_monitor
        // when the user starts that application with overridden dictionary
        // as opposed to letting it automatically load the dictionary for
        // the
        // session
        final boolean dictOverride = GdsSystemProperties.isDictionaryOverridden();
        String overrideText = "(Auto-Loaded)";
        if (dictOverride) {
            overrideText = "(Overridden)";
        }

        final Map<String, Object> map = new HashMap<>();
        appContext.getBean(MessageServiceConfiguration.class).setTemplateContext(map);
        appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).setTemplateContext(map);
        contextConfig.setTemplateContext(map);
        map.put("dictOverrideText", overrideText);
        map.put("body", true);
        map.put("hasSse", missionProps.missionHasSse()
                && !appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue());
        return map;
    }
}
