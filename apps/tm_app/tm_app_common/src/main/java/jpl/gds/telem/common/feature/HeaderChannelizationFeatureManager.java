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
package jpl.gds.telem.common.feature;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.service.channel.IFrameHeaderChannelizerService;
import jpl.gds.eha.api.service.channel.IPacketHeaderChannelizerService;
import jpl.gds.eha.api.service.channel.ISfduHeaderChannelizerService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

/**
 * HeaderChannelizationFeatureManager handles everything required to initialize
 * and shutdown frame and header packet channelization in the downlink process.
 * 
 */
public class HeaderChannelizationFeatureManager extends AbstractTelemetryFeatureManager {
	private boolean packetHeadersEnabled = false;
	private boolean frameHeadersEnabled = false;
    private boolean sfduHeadersEnabled = false;

	/**
     * Enables or disables SFDU header channelization, which
     * for this feature manager, only indicates whether the header
     * dictioanry should be laoded.
     * 
     * @param sfduHeadersEnabled true to enable, false to disable
     * 
     */
    public void setSfduHeaderChannelsEnabled(
            final boolean sfduHeadersEnabled) {
        this.sfduHeadersEnabled  = sfduHeadersEnabled;
    }
	/**
	 * Enables or disables packet header channelization.
	 * 
	 * @param packetHeadersEnabled true to enable, false to disable
	 */
	public void setPacketHeaderChannelsEnabled(
	        final boolean packetHeadersEnabled) {
		this.packetHeadersEnabled = packetHeadersEnabled;
	}

	/**
	 * Enables or disables frame header channelization.
	 * 
	 * @param frameHeadersEnabled true to enable, false to disable
	 */
	public void setFrameHeaderChannelsEnabled(final boolean frameHeadersEnabled) {
		this.frameHeadersEnabled = frameHeadersEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.TLM_HEADER);
		setValid(false);
		if (!this.isEnabled()) {
			return true;
		}

		setValid(true);
		
		/* Load header channel dictionary if any header
		 * channelization is enabled. If it does not load, do not fail the session,
		 * but issue a warning.
		 */
		if (frameHeadersEnabled || packetHeadersEnabled || sfduHeadersEnabled) {
		    try {
		        final IChannelUtilityDictionaryManager chanTable = (IChannelUtilityDictionaryManager) springContext.getBean(IChannelDefinitionProvider.class);
	            chanTable.loadFswHeader(true);
		    } catch (final DictionaryException e) {
		        log.warn("Header channelization is enabled but the header channel dictionary could not be loaded");
		    } catch (final Exception e) {
		        e.printStackTrace();
                log.error(e.toString());
                log.error("Unable to start header channelization service");
                setValid(false);
                return false;
                
            }
		}

		if (frameHeadersEnabled) {
		    try {
                addService(springContext.getBean(IFrameHeaderChannelizerService.class));
            } catch (final Exception e) {
                log.error("Frame header channelization service could not be started: " + e.getMessage());
                e.printStackTrace();
                setValid(false);
            }
		}
		if (packetHeadersEnabled) {
		    try {
                addService(springContext.getBean(IPacketHeaderChannelizerService.class));
            } catch (final Exception e) {
                log.error("Packet header channelization service could not be started: " + e.getMessage());
                e.printStackTrace();
            }
		}		
		try {
		    if (sfduHeadersEnabled) {
                addService(springContext.getBean(ISfduHeaderChannelizerService.class));
		    }
		} catch (final Exception e) {
		    log.error("SFDU header channelization service could not be started: " + e.getMessage());
		    e.printStackTrace();
		    setValid(false);
		}
		
		if (!isValid()) {
		    return false;
		}
		
		setValid(startAllServices());

		if (this.isValid()) {
			log.debug("Header channelization feature successfully initialized");
		}

		return isValid();
	}
}
