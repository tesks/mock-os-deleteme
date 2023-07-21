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
package jpl.gds.telem.ingest;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.config.AbstractDownlinkConfiguration;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;


/**
 * IngestConfiguration contains configuration values used by the downlink
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
public class IngestConfiguration extends AbstractDownlinkConfiguration {
    private       long                meterInterval;
    private final TelemetryInputProperties    inputConfig;
  
    /**
     * Creates an instance of IngestConfiguration. This triggers
     * loading of the configuration properties.
     * 
     * @param sseFlag
     *            the sse context flag
     * @param inputProps
     *            telemetry input properties
     * @param msgServiceProps
     *            message service configuration
     * @param dbProps
     *            database properties
     */
    public IngestConfiguration(final SseContextFlag sseFlag,
            final TelemetryInputProperties inputProps, final MessageServiceConfiguration msgServiceProps,
            final IDatabaseProperties dbProps) {
        super(dbProps, msgServiceProps);
        this.featureSet = new IngestAppProperties(sseFlag);

        this.inputConfig = inputProps;
        this.meterInterval = inputConfig.getMeterInterval();
    }
    
    @Override
	public IIngestProperties getFeatureSet() {
		return (IIngestProperties) featureSet;
	}

	/**
     * Gets the wait interval (in milliseconds) between reads from the data telemetry 
     * source. This is a crude throttling mechanism.
     * 
     * @return the meter interval in milliseconds
     */
    public long getMeterInterval() {
        return meterInterval;
    }

    /**
     * Sets the meter interval. This is the wait interval (in milliseconds)
     * between reads from the data telemetry source. Any value set by this call 
     * temporarily overrides the value in the configuration. 
     *
     * @param meterInterval The meterInterval to set.
     */
    public void setMeterInterval(final long meterInterval) {
        this.meterInterval = meterInterval;
    }

}
