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
package jpl.gds.telem.process;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.config.AbstractDownlinkConfiguration;

/**
 * DownlinkConfiguration contains configuration values used by the downlink
 * application. These values actually reside in the property files or in
 * the user perspective. This is just a convenience class for
 * accessing configuration parameters in the downlink processor. 
 * <p>
 * Changing configuration values in this object will not affect the 
 * configuration or perspective files; those cannot be updated from here.
 *
 * TODO: Put this class behind an interface
 *
 */
public class ProcessConfiguration extends AbstractDownlinkConfiguration {

    /**
     * Creates an instance of ProcessConfiguration. This triggers
     * loading of the configuration properties.
     * 
     * @param missionProps
     *            mission properties
     * @param sseFlag
     *            the sse context flag
     * @param msgServiceProps
     *            message service configuration
     * @param dbProps
     *            database properties
     */
    public ProcessConfiguration(final MissionProperties missionProps, final SseContextFlag sseFlag,
            final MessageServiceConfiguration msgServiceProps,
            final IDatabaseProperties dbProps) {
        super(dbProps, msgServiceProps);
        this.featureSet = new ProcessAppProperties(missionProps, sseFlag);

    }

    @Override
	public IProcessProperties getFeatureSet() {
		return (IProcessProperties) featureSet;
	}

}
