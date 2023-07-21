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
package jpl.gds.eha.impl.service.channel;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IHeaderChannelDefinition;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * A table used to store various mappings related to header channel processing,
 * such as packet header, frame header, or SFDU header field to channel
 * definition.
 * 
 * @since R8
 */
public class HeaderChannelTable {
    private static final Tracer                                  log = TraceManager.getTracer(Loggers.TLM_HEADER);
	/** Mapping from packet header field name to H-channel definition */
	private final Map<PacketHeaderFieldName, IChannelDefinition> pktHdrFieldMapping;

	/** Mapping of H-channel definition to packet header field name that overlap with SFDU's */
	private final Map<IChannelDefinition, PacketHeaderFieldName> pktSfduOverlapMapping;

	/** Mapping from frame header field name to H-channel definition */
	private final Map<FrameHeaderFieldName, IChannelDefinition> frmHdrFieldMapping;

	/** Mapping of H-channel definition to packet header field name that overlap with SFDU's */
	private final Map<IChannelDefinition, FrameHeaderFieldName> frmSfduOverlapMapping;

	/** Flag that indicates frame H-channels that overlap with SFDU's are disabled */
	private boolean sfduFrmOverlappedChannelsAreDisabled;

	/** Flag that indicates packet H-channels that overlap with SFDU's are disabled */
	private boolean sfduPktOverlappedChannelsAreDisabled;

	/** Mapping from SFDU header fieldId to H-channel definition */
	private final Map<String, IChannelDefinition> sfduHdrFieldMapping;


	/**
	 * Creates an instance of HeaderChannelTable. Initialize all the mappings to empty.
	 */
	public HeaderChannelTable()
	{
		pktHdrFieldMapping = new EnumMap<PacketHeaderFieldName, IChannelDefinition>(PacketHeaderFieldName.class);
		pktSfduOverlapMapping = new HashMap<IChannelDefinition, PacketHeaderFieldName>(3);
		frmHdrFieldMapping = new EnumMap<FrameHeaderFieldName, IChannelDefinition>(FrameHeaderFieldName.class);
		frmSfduOverlapMapping = new HashMap<IChannelDefinition, FrameHeaderFieldName>(5);
		sfduHdrFieldMapping = new HashMap<String, IChannelDefinition>(50);
		sfduFrmOverlappedChannelsAreDisabled = false;
		sfduPktOverlappedChannelsAreDisabled = false;

	}

    /**
     * Adds header channel mappings for a set of header channel definitions.
     * 
     * @param defs
     *            collection of header channel definitions
     */
	public synchronized void addDefinitions(final Collection<IChannelDefinition> defs) {
	    if (defs == null) {
	        return;
	    }
	    defs.forEach(def->addDefinition(def));
	}
	
	/**
	 * Add a new generic channel definition to the definition set.
	 *
	 * See validDefinition for error checking.
	 *
	 * Note that if a channel definition with the same id, definition type,
	 * and index exists, it will be overwritten.
	 *
	 * Note also that an index of zero is not stored in the index maps. It is
	 * not a real index.
	 *
	 * @param def  the definition of the new channel
	 */
	public synchronized void addDefinition(final IChannelDefinition def)
	{
		if (def == null)
		{
			throw new IllegalArgumentException("Null channel definition input!");
		}

	
		if (def.getDefinitionType() == ChannelDefinitionType.H) {
			    setupHeaderFieldMapping((IHeaderChannelDefinition)def);
		}
	}


	

	/**
	 * Clears all information
	 */
	public void clear() {
		pktHdrFieldMapping.clear();
		pktSfduOverlapMapping.clear();
		frmHdrFieldMapping.clear();
		frmSfduOverlapMapping.clear();
		sfduHdrFieldMapping.clear();
		sfduFrmOverlappedChannelsAreDisabled = false;
		sfduPktOverlappedChannelsAreDisabled = false; 
	}
	
	/**
	 * Adds header channel mappings to the appropriate maps.
	 * 
	 * @param hdrChanDef the IHeaderChannelDefinition for the header channel being added
	 * 
	 */
	private void setupHeaderFieldMapping(final IHeaderChannelDefinition hdrChanDef) {

	    /* Add packet header field mapping, if any. */
	    final PacketHeaderFieldName pktField = hdrChanDef.getPacketHeaderField();
	    if (pktField != null) {
	        pktHdrFieldMapping.put(pktField, hdrChanDef);
	    }
	    /* Add frame field mapping, if any. */
	    final FrameHeaderFieldName frmField = hdrChanDef.getFrameHeaderField();
	    if (frmField != null) {
	        frmHdrFieldMapping.put(frmField, hdrChanDef);
	    }
	    /* Add SFDU field mapping. If this SFDU field also corresponds
	     * to a packet or frame field, the SFDU field may override
	     * the packet or frame field based upon current configuration.
	     * So we add overlap mappings.
	     */
	    final String sfduField = hdrChanDef.getSfduHeaderField();
	    if (sfduField != null) {
	        sfduHdrFieldMapping.put(sfduField, hdrChanDef);
	        if (pktField != null) {
	            addPktSfduOverlapMapping(hdrChanDef, pktField);
	        }
	        if (frmField != null) {
	            addFrmSfduOverlapMapping(hdrChanDef, frmField);
	        }

	    }     
	}

	/**
	 * Gets the map of packet header fields to corresponding channel definitions.
	 * 
	 * @return a Map of PacketHeaderFieldName to ChannelDefinition
	 */
	public Map<PacketHeaderFieldName, IChannelDefinition> getPktHdrFieldMapping() {
		return pktHdrFieldMapping;
	}

	/**
	 * Gets the map of frame header fields to corresponding channel definitions.
	 * 
	 * @return a Map of FrameHeaderFieldName to ChannelDefinition
	 */
	public Map<FrameHeaderFieldName, IChannelDefinition> getFrmHdrFieldMapping() {
		return frmHdrFieldMapping;
	}

	/**
	 * Gets the map of SFDU/CHDO header fields to corresponding channel definitions.
	 * 
	 * @return a Map of CHDO field ID to ChannelDefinition
	 */
	public Map<String, IChannelDefinition> getSfduHdrFieldMapping() {
		return sfduHdrFieldMapping;
	}

	/**
	 * If SFDU/packet header field overlaps are disabled, removes the field from the
	 * packet header field mapping. Adds the field to the packet SFDU overlap map regardless.
	 * 
	 * @param currentChannel channel definition
	 * @param name packet header field name
	 * @return the header field name the channel definition was previously mapped to,
	 * or null if no previous mapping
	 * 
	 */
	private PacketHeaderFieldName addPktSfduOverlapMapping(final IChannelDefinition currentChannel, final PacketHeaderFieldName name) {

		if (sfduPktOverlappedChannelsAreDisabled) {
			// Remove from packet and frame mappings
			pktHdrFieldMapping.remove(name);
		}

		return pktSfduOverlapMapping.put(currentChannel, name);
	}

	/**
	 * If SFDU/frame header field overlaps are disabled, removes the field from the
	 * frame header field mapping. Adds the field to the frame SFDU overlap map regardless.
	 * 
	 * @param chanDef channel definition
	 * @param name frame header field name
	 * @return the frame field name the channel definition was previously mapped to,
	 * or null if no previous mapping
	 *
	 */
	private FrameHeaderFieldName addFrmSfduOverlapMapping(final IChannelDefinition chanDef, final FrameHeaderFieldName name) {

		if (sfduFrmOverlappedChannelsAreDisabled) {
			// Remove from packet and frame mappings
			frmHdrFieldMapping.remove(name);
		}

		return frmSfduOverlapMapping.put(chanDef, name);
	}

	/**
	 * Disables overlapping SFDU header channels for non-SFDU input types.
	 * 
	 * @param inputType the RawInputType of the telemetry currently being processed
	 */
	public void disableOtherChannelizersOfOverlaps(final TelemetryInputType inputType) {

		if (!inputType.equals(TelemetryInputType.SFDU_TF)) {
			sfduPktOverlappedChannelsAreDisabled = true;

			synchronized (pktHdrFieldMapping) {
				final Iterator<Entry<IChannelDefinition, PacketHeaderFieldName>> it = pktSfduOverlapMapping.entrySet().iterator();

				while (it.hasNext()) {
					pktHdrFieldMapping.remove(it.next().getValue());
				}

			}
		}

		if (!inputType.equals(TelemetryInputType.SFDU_PKT)) {
			sfduFrmOverlappedChannelsAreDisabled = true;

			synchronized (frmHdrFieldMapping) {
				final Iterator<Entry<IChannelDefinition, FrameHeaderFieldName>> it = frmSfduOverlapMapping.entrySet().iterator();

				while (it.hasNext()) {
					frmHdrFieldMapping.remove(it.next().getValue());
				}

			}
		}
	}
}
