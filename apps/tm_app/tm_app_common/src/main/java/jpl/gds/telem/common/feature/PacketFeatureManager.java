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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.service.telem.*;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.tm.service.api.packet.IPacketExtractService;
import jpl.gds.tm.service.api.packet.IPacketTrackingService;
import org.springframework.context.ApplicationContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PacketFeatureManager handles everything required to initialize and shutdown the 
 * packet extraction capability in the downlink process.
 *
 *
 */
public class PacketFeatureManager extends AbstractTelemetryFeatureManager {
    
    private IPacketTrackingService packetTracker;
    private final Map<Integer, IService> packetExtractors = new TreeMap<Integer, IService>();
    private boolean isEnablePacketExtract;
    private boolean isEnablePacketTracking;
    
    /**
     * Indicates if packet extraction is enabled.
     * 
     * @return true if enabled, false if not
     */
    public boolean isEnablePacketExtract() {
        return isEnablePacketExtract;
    }

    /**
     * Sets the flag to enable or disable packet extraction.
     * 
     * @param isEnablePacketExtract true to enable, false to disable
     */
    public void setEnablePacketExtract(final boolean isEnablePacketExtract) {
        this.isEnablePacketExtract = isEnablePacketExtract;
    }

    /**
     * Indicates if packet tracking is enabled.
     * 
     * @return true if enabled, false if not
     */
    public boolean isEnablePacketTracking() {
        return isEnablePacketTracking;
    }

    /**
     * Sets the flag to enable or disable packet tracking.
     * 
     * @param isEnablePacketTracking true to enable, false to disable
     */
    public void setEnablePacketTracking(final boolean isEnablePacketTracking) {
        this.isEnablePacketTracking = isEnablePacketTracking;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) 
    {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        setValid(false);
		if (!this.isEnabled())
        {
			return true;
		}
		setValid(true);
		
		packetExtractors.clear();
		
		final Integer sessionVcid = springContext.getBean(IContextFilterInformation.class).getVcid();
		
	    try {
            springContext.getBean(IApidDefinitionProvider.class);
        } catch (final Exception e) {
            log.error("Unable to start packet services: " + e.toString());
            e.printStackTrace();
            setValid(false);
            return false;
        }
		
		if (isEnablePacketExtract()) {
        
		    List<Integer> vcidList = springContext.getBean(MissionProperties.class).getPacketExtractVcids();

		    // Look for valid vcid values in the XML Config files or session configuration
            if (!springContext.getBean(SseContextFlag.class).isApplicationSse() && sessionVcid != null) {
		        vcidList = new LinkedList<Integer>();
		        vcidList.add(sessionVcid);
		    }
		    if ( vcidList == null || vcidList.isEmpty())
		    { 
		        try {
		            final IService extractor = springContext.getBean(IPacketExtractService.class, 0);
		            this.packetExtractors.put(0, extractor);
		            addService(extractor);
		            log.debug ( "Packet extraction feature for vcid 0 successfully initialized" );
		        } catch (final Exception e) {
		            log.error("Unable to initialize packet extract feature for vcid 0");
		            e.printStackTrace();
		            setValid(false);
		            return false;
		        }

		    } 
		    else
		    { 
		        for ( final Integer vcid : vcidList)
		        {
		            try {
		                final IService extractor = springContext.getBean(IPacketExtractService.class, vcid);
		                this.packetExtractors.put(vcid, extractor);
		                addService(extractor);
                        log.debug("Packet extraction feature for vcid ", vcid, " successfully initialized");
		            } catch (final Exception e) {
		                log.error("Unable to initialize packet extract feature for vcid " + vcid);
		                e.printStackTrace();
		                setValid(false);
		                return false;
		            }
		        } 
		    } 
		}
		
		if (isEnablePacketTracking()) {
		    this.packetTracker = springContext.getBean(IPacketTrackingService.class);
            addService(packetTracker);
            log.debug ( "Packet tracking feature for successfully initialized" );
		}

        setValid(startAllServices());

        if (this.isValid()) {
            log.debug ( "Packet features successfully initialized" );
        }
		return isValid();
	} // end member function init
	
	/**
	 * Gets a packet extraction service object.
	 * 
	 * @param vcid the virtual channel ID to get the service object for
	 * @return Packet Extract service instance, or null if none defined
	 */
	public IService getPacketExtract( final int vcid)
	{
	    return this.packetExtractors.get(vcid);
	}

	
    @Override
    public void populateSummary(final ITelemetrySummary summary) {
        if (packetTracker != null && summary != null) {
            if (summary instanceof IDownlinkSummary) {
                final DownlinkSummary sum = (DownlinkSummary) summary;

                sum.setEvrPackets(packetTracker.getEvrPackets());
                sum.setEhaPackets(packetTracker.getEhaPackets());
                sum.setProductPackets(packetTracker.getProductPackets());
                sum.setBadPackets(packetTracker.getInvalidPackets());
                sum.setFrameGaps(packetTracker.getFrameGaps());
                sum.setFrameRegressions(packetTracker.getFrameRegressions());
                sum.setFrameRepeats(packetTracker.getFrameRepeats());
                sum.setPackets(packetTracker.getValidPackets());
                sum.setStationPackets(packetTracker.getStationPackets());
                sum.setIdlePackets(packetTracker.getFillPackets());
                sum.setCfdpPackets(packetTracker.getCfdpPackets());
				sum.setCfdpPackets(packetTracker.getCfdpPackets());

            }
            else if (summary instanceof ITelemetryIngestorSummary) {
                final TelemetryIngestorSummary sum = (TelemetryIngestorSummary) summary;

                sum.setProductPackets(packetTracker.getProductPackets());
                sum.setFrameGaps(packetTracker.getFrameGaps());
                sum.setFrameRegressions(packetTracker.getFrameRegressions());
                sum.setFrameRepeats(packetTracker.getFrameRepeats());
                sum.setStationPackets(packetTracker.getStationPackets());
                sum.setPackets(packetTracker.getValidPackets());
                sum.setBadPackets(packetTracker.getInvalidPackets());
                sum.setFillPackets(packetTracker.getFillPackets());
				sum.setCfdpPackets(packetTracker.getCfdpPackets());
            }
            else if (summary instanceof ITelemetryProcessorSummary) {
                final TelemetryProcessorSummary sum = (TelemetryProcessorSummary) summary;

                sum.setEvrPackets(packetTracker.getEvrPackets());
                sum.setEhaPackets(packetTracker.getEhaPackets());
            }


	    }
	}
}
