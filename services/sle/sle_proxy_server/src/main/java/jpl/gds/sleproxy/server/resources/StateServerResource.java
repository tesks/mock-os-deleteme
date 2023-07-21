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
package jpl.gds.sleproxy.server.resources;

import java.util.HashMap;
import java.util.Map;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.IStateResource;
import jpl.gds.sleproxy.server.chillinterface.downlink.ChillInterfaceDownlinkManager;
import jpl.gds.sleproxy.server.chillinterface.uplink.ChillInterfaceUplinkManager;
import jpl.gds.sleproxy.server.sleinterface.fwd.SLEInterfaceForwardService;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.rtn.SLEInterfaceReturnService;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;

/**
 * Restlet resource for the "state" API.
 * 
 *
 */
public class StateServerResource extends ServerResource implements IStateResource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_common.resources.IStateResource#toJson()
	 */
	@Override
	public final Representation toJson() {
		Map<String, Object> stateMap = new HashMap<>();

		// Configured properties
		Map<String, Object> sleInterfaceForwardServiceConfiguration = new HashMap<>();
		stateMap.put("sle_interface_forward_service_configuration", sleInterfaceForwardServiceConfiguration);
		sleInterfaceForwardServiceConfiguration.put("allowable_modindex_min",
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableModindexRange().getMinimum());
		sleInterfaceForwardServiceConfiguration.put("allowable_modindex_max",
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableModindexRange().getMaximum());
		sleInterfaceForwardServiceConfiguration.put("allowable_bitrates",
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableBitrates());

		// SLE interface forward service items
		Map<String, Object> sleInterfaceForwardServiceState = new HashMap<>();
		stateMap.put("sle_interface_forward_service_state", sleInterfaceForwardServiceState);
		sleInterfaceForwardServiceState.put("last_used_profile",
				SLEInterfaceForwardService.INSTANCE.getLastSLEForwardServiceProfileName() != null
						? SLEInterfaceForwardService.INSTANCE.getLastSLEForwardServiceProfileName() : "");
		sleInterfaceForwardServiceState.put("bound_profile",
				SLEInterfaceForwardService.INSTANCE.getBoundProfileName() != null
						? SLEInterfaceForwardService.INSTANCE.getBoundProfileName() : "");
		sleInterfaceForwardServiceState.put("state", SLEInterfaceForwardService.INSTANCE.getServiceStateString());
		sleInterfaceForwardServiceState.put("state_change_time",
				SLEInterfaceForwardService.INSTANCE.getForwardServiceStateChangeTime() != null
						? SLEInterfaceForwardService.INSTANCE.getForwardServiceStateChangeTime()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		sleInterfaceForwardServiceState.put("last_transfer_data_time",
				SLEInterfaceForwardService.INSTANCE.getLastTransferDataTimeSinceLastBind() != null
						? SLEInterfaceForwardService.INSTANCE.getLastTransferDataTimeSinceLastBind()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		sleInterfaceForwardServiceState.put("transferred_data_count",
				Long.toString(SLEInterfaceForwardService.INSTANCE.getTransferredDataCountSinceLastBind()));
		sleInterfaceForwardServiceState.put("delivery_mode",
				SLEInterfaceForwardService.INSTANCE.getForwardServiceDeliveryMode() != null
						? SLEInterfaceForwardService.INSTANCE.getForwardServiceDeliveryMode() : "");
		sleInterfaceForwardServiceState.put("current_session_number",
				SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumber() >= 0
						? SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumber() : "");
		sleInterfaceForwardServiceState.put("last_accepted_bitrate",
				SLEInterfaceForwardService.INSTANCE.getLastAcceptedBitrateForReporting() != null
						? SLEInterfaceForwardService.INSTANCE.getLastAcceptedBitrateForReporting() : "");
		Integer modindex = SLEInterfaceForwardService.INSTANCE.getModindexFromProvider();
		sleInterfaceForwardServiceState.put("modindex", modindex != null ? modindex : "");
		sleInterfaceForwardServiceState.put("last_accepted_command_mod_state",
				SLEInterfaceForwardService.INSTANCE.getLastAcceptedCommandModStateForReporting() != null
						? SLEInterfaceForwardService.INSTANCE.getLastAcceptedCommandModStateForReporting() : "");
		sleInterfaceForwardServiceState.put("last_accepted_range_mod_state",
				SLEInterfaceForwardService.INSTANCE.getLastAcceptedRangeModStateForReporting() != null
						? SLEInterfaceForwardService.INSTANCE.getLastAcceptedRangeModStateForReporting() : "");

		// SLE interface return service items
		Map<String, Object> sleInterfaceReturnServiceState = new HashMap<>();
		stateMap.put("sle_interface_return_service_state", sleInterfaceReturnServiceState);
		sleInterfaceReturnServiceState.put("last_used_profile",
				SLEInterfaceReturnService.INSTANCE.getLastSLEReturnServiceProfileName() != null
						? SLEInterfaceReturnService.INSTANCE.getLastSLEReturnServiceProfileName() : "");
		sleInterfaceReturnServiceState.put("bound_profile",
				SLEInterfaceReturnService.INSTANCE.getBoundProfileName() != null
						? SLEInterfaceReturnService.INSTANCE.getBoundProfileName() : "");
		sleInterfaceReturnServiceState.put("state", SLEInterfaceReturnService.INSTANCE.getServiceState().name());
		sleInterfaceReturnServiceState.put("state_change_time",
				SLEInterfaceReturnService.INSTANCE.getReturnServiceStateChangeTime() != null
						? SLEInterfaceReturnService.INSTANCE.getReturnServiceStateChangeTime()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		sleInterfaceReturnServiceState.put("last_transfer_data_time",
				SLEInterfaceReturnService.INSTANCE.getLastTransferDataTimeSinceLastBind() != null
						? SLEInterfaceReturnService.INSTANCE.getLastTransferDataTimeSinceLastBind()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		sleInterfaceReturnServiceState.put("transferred_data_count",
				Long.toString(SLEInterfaceReturnService.INSTANCE.getTransferredDataCountSinceLastBind()));
		sleInterfaceReturnServiceState.put("delivery_mode",
				SLEInterfaceReturnService.INSTANCE.getReturnServiceDeliveryMode() != null
						? SLEInterfaceReturnService.INSTANCE.getReturnServiceDeliveryMode() : "");
		sleInterfaceReturnServiceState.put("current_session_number",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumber() >= 0
						? SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumber() : "");

		// chill interface uplink items
		Map<String, Object> chillInterfaceUplinkState = new HashMap<>();
		stateMap.put("chill_interface_uplink_state", chillInterfaceUplinkState);
		chillInterfaceUplinkState.put("state",
				ChillInterfaceUplinkManager.INSTANCE.isEnabled() ? "ENABLED" : "DISABLED");
		chillInterfaceUplinkState.put("state_change_time",
				ChillInterfaceUplinkManager.INSTANCE.getUplinkStateChangeTime() != null
						? ChillInterfaceUplinkManager.INSTANCE.getUplinkStateChangeTime()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		chillInterfaceUplinkState.put("connected_host", ChillInterfaceUplinkManager.INSTANCE.getConnectedHost() != null
				? ChillInterfaceUplinkManager.INSTANCE.getConnectedHost() : "");
		chillInterfaceUplinkState.put("last_data_received_time",
				ChillInterfaceUplinkManager.INSTANCE.getLastCLTUsReceivedTimeSinceLastEnable() != null
						? ChillInterfaceUplinkManager.INSTANCE.getLastCLTUsReceivedTimeSinceLastEnable()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		chillInterfaceUplinkState.put("received_data_count",
				ChillInterfaceUplinkManager.INSTANCE.getCLTUsReceivedCountSinceLastEnable());

		// chill interface downlink items
		Map<String, Object> chillInterfaceDownlinkState = new HashMap<>();
		stateMap.put("chill_interface_downlink_state", chillInterfaceDownlinkState);
		chillInterfaceDownlinkState.put("state",
				ChillInterfaceDownlinkManager.INSTANCE.isConnected() ? "CONNECTED" : "DISCONNECTED");
		chillInterfaceDownlinkState.put("state_change_time",
				ChillInterfaceDownlinkManager.INSTANCE.getDownlinkStateChangeTime() != null
						? ChillInterfaceDownlinkManager.INSTANCE.getDownlinkStateChangeTime()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		chillInterfaceDownlinkState.put("last_data_transferred_time",
				ChillInterfaceDownlinkManager.INSTANCE.getLastFramesTransferredTimeSinceLastConnection() != null
						? ChillInterfaceDownlinkManager.INSTANCE.getLastFramesTransferredTimeSinceLastConnection()
								.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
						: "");
		chillInterfaceDownlinkState.put("transferred_data_count",
				ChillInterfaceDownlinkManager.INSTANCE.getFramesTransferredCountSinceLastConnection());

		return new JacksonRepresentation<Map<String, Object>>(stateMap);
	}

}