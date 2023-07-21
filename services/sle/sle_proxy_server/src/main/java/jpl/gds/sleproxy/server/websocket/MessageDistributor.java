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
package jpl.gds.sleproxy.server.websocket;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jpl.gds.sleproxy.server.chillinterface.downlink.action.EDownlinkActionType;
import jpl.gds.sleproxy.server.chillinterface.uplink.action.EUplinkActionType;
import jpl.gds.sleproxy.server.sleinterface.fwd.action.EForwardActionType;
import jpl.gds.sleproxy.server.sleinterface.rtn.action.EReturnActionType;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;

/**
 * This class handles all of the websocket based message distribution 
 * to the GUI clients
 * 
 */
public enum MessageDistributor {
	
	INSTANCE;

	/**
	 * User and Provider password field names
	 */
	private static final String USER_PASSWORD = "user_password";
	private static final String PROVIDER_PASSWORD = "provider_password";	
	
	/**
	 * Jackson library ObjectMapper used to create ObjectNode instances
	 */
	private final ObjectMapper mapper;
	
	/**
	 * Jackson library ObjectNode used to define JSON objects
	 */
	private ObjectNode node;
	
	/**
	 * Map used to buffer log event messages which get flushed
	 * at set intervals
	 */
	private ConcurrentHashMap<String, ObjectNode> messageMap;
	
	/**
	 * Client Connection Manager used to obtain a list of all
	 * currently connected clients
	 */
	private ClientConnectionManager clientConnectionManager;
	
	/**
	 * Constructor.
	 */
	private MessageDistributor() {
		messageMap = new ConcurrentHashMap<>();
		mapper = new ObjectMapper();
		clientConnectionManager = ClientConnectionManager.INSTANCE;
	}
	
	/**
	 * Notify clients that AMPCS Downlink connection state has changed
	 * 
	 * @param action 
	 * 			Action that caused the state change
	 * @param downlinkStateChangeTime
	 * 			Date/Time at which the state changed
	 */			
	public synchronized void chillDownStateChangeAction(final EDownlinkActionType action, final ZonedDateTime downlinkStateChangeTime) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.CHILL_DOWN_STATE_CHANGE.toString());
		node.put(EMessageProperty.ACTION.toString(), action.toString().toLowerCase());
		node.put(EMessageProperty.STATE_CHANGE_TIME.toString(), downlinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());

		if (action.equals(EDownlinkActionType.CONNECT)) {
			node.put(EMessageProperty.DATA_COUNT.toString(), 0);
		}
		notifyClients(node.toString());
	}
	
	/**
	 * Notify clients that AMPCS Uplink connection state has changed
	 * 
	 * @param action
	 * 			Action that caused the state change
	 * @param uplinkStateChangeTime
	 * 			Date/Time at which the state changed
	 */
	public synchronized void chillUpStateChangeAction(final EUplinkActionType action, final ZonedDateTime uplinkStateChangeTime) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.CHILL_UP_STATE_CHANGE.toString());
		node.put(EMessageProperty.ACTION.toString(), action.toString().toLowerCase());
		node.put(EMessageProperty.STATE_CHANGE_TIME.toString(), uplinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		notifyClients(node.toString());
	}	
	
	/**
	 * Notify clients that SLE Return Provider connection state has changed
	 * 
	 * @param action
	 * 			Action that caused the state change
	 * @param profileName
	 * 			Profile Name used to trigger a BIND event. Required only during BIND.
	 * @param returnServiceStateChangeTime
	 * 			Date/Time at which the state changed
	 * @param currentConnectionNumber
	 * 			Connection number used only during BIND event. 
	 */
	public synchronized void sleReturnProviderStateChange(final EReturnActionType action, final String profileName, final ZonedDateTime returnServiceStateChangeTime, final Long currentConnectionNumber) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_RETURN_PROVIDER_STATE_CHANGE.toString());
		node.put(EMessageProperty.ACTION.toString(), action.toString().toLowerCase());
		node.put(EMessageProperty.STATE_CHANGE_TIME.toString(), returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
				
		switch (action) {
		case BIND:
			node.put(EMessageProperty.PROFILE_NAME.toString(), profileName);			
			node.put(EMessageProperty.CURRENT_CONNECTION_NUMBER.toString(), currentConnectionNumber);
			break;
		default:
			break;
		}
		
		notifyClients(node.toString());		
	}
	
	/**
	 * Notify clients that SLE Forward Provider connection state has changed
	 * 
	 * @param action
	 * 			Action that caused the state change
	 * @param profileName
	 * 			Profile Name used to trigger a BIND event. Required only during BIND.
	 * @param returnServiceStateChangeTime
	 * 			Date/Time at which the state changed
	 * @param currentConnectionNumber
	 * 			Connection number used only during BIND event. 
	 */
	public synchronized void sleForwardProviderStateChange(final EForwardActionType action, final String profileName, final ZonedDateTime stateChangeTime, final Long currentConnectionNumber) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_FORWARD_PROVIDER_STATE_CHANGE.toString());
		node.put(EMessageProperty.ACTION.toString(), action.toString().toLowerCase());
		node.put(EMessageProperty.STATE_CHANGE_TIME.toString(), stateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		
		switch (action) {
		case BIND:
			node.put(EMessageProperty.PROFILE_NAME.toString(), profileName);			
			node.put(EMessageProperty.CURRENT_CONNECTION_NUMBER.toString(), currentConnectionNumber);
			break;
		default:
			break;
		}
		
		notifyClients(node.toString());		
	}

	/**
	 * Notify clients when the SLE Delivery/Service Mode changes 
	 * 
	 * @param providerType
	 * 				SLE provider type
	 * @param deliveryMode
	 * 				String value of the delivery mode
	 */
	public synchronized void sleDeliveryModeChange(final SLEProviderType providerType, final String deliveryMode) {
		node = mapper.createObjectNode();
		
		switch (providerType) {
		case FORWARD:
			node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_FORWARD_DELIVERY_MODE_CHANGE.toString());
			break;
		case RETURN:
			node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_RETURN_DELIVERY_MODE_CHANGE.toString());
			break;
		default:
			break;
		}
		
		node.put(EMessageProperty.DELIVERY_MODE.toString(), deliveryMode);
		notifyClients(node.toString());
	}
	
	
	/**
	 * Notify clients that a new SLE provider profile has been created
	 * 
	 * @param profileName
	 * 				Name of the new profile
	 * @param propertiesMap
	 * 				Map of all of the profile properties
	 */
	public synchronized void sleProfileCreate(final String profileName, final Map<String, String> propertiesMap) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_PROFILE_CREATE.toString());
		
		final ObjectNode profNode = mapper.createObjectNode();
		profNode.put(EMessageProperty.PROFILE_NAME.toString(), profileName);
		propertiesMap.forEach((k,v)->{
			// Skip password fields
			if (!(k.equalsIgnoreCase(USER_PASSWORD) || k.equalsIgnoreCase(PROVIDER_PASSWORD))) {
				profNode.put(k, v);
			}
		});
		node.set(EMessageProperty.PROFILE.toString(), profNode);
				
		notifyClients(node.toString());
	}
	
	/**
	 * Notify clients that an existing profile has been updated
	 * 
	 * @param profileName
	 * 				Name of the updated profile
	 * @param propertiesMap
	 * 				Map of all of the profile properties
	 */
	public synchronized void sleProfileUpdate(final String profileName, final Map<String, String> propertiesMap) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_PROFILE_UPDATE.toString());
		
		final ObjectNode profNode = mapper.createObjectNode();
		propertiesMap.forEach((k,v)->{
			// Skip password fields
			if (!(k.equalsIgnoreCase(USER_PASSWORD) || k.equalsIgnoreCase(PROVIDER_PASSWORD))) {
				profNode.put(k, v);
			}
		});
		
		// If the only updates were password fields and those have been filtered out
		// there is no need to send any notifications
		if (profNode.size() > 0) {
			profNode.put(EMessageProperty.PROFILE_NAME.toString(), profileName);
			node.set(EMessageProperty.PROFILE.toString(), profNode);
			notifyClients(node.toString());
		}
	}

	/**
	 * Notify clients that a profile has been deleted
	 * 
	 * @param profileName
	 * 				Name of the deleted profile	
	 */
	public synchronized void sleProfileDelete(final String profileName) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_PROFILE_DELETE.toString());
		node.put(EMessageProperty.PROFILE_NAME.toString(), profileName);
		
		notifyClients(node.toString());
	}
	
	
	/**
	 * Notify clients that AMPCS Downlink config has changed
	 * 
	 * @param configProperties
	 * 				Properties object containing Downlink config properties
	 */
	public synchronized void chillConfigUpdate(final Properties configProperties) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.CHILL_CONFIG_UPDATE.toString());
		configProperties.forEach((k,v)->{
			node.put(k.toString(), v.toString());
		});		
		notifyClients(node.toString());
	}

	/**
	 * Notify clients with AMPCS Downlink data flow updates which include the total number of frames
	 * transferred since last connection and the last frame time. 
	 * 
	 * @param lastFramesTransferredTimeSinceLastConnection
	 * 				Date/Time of last frame
	 * @param framesTransferredCountSinceLastConnection
	 * 				Total number of frames transferred since last connection
	 */
	public synchronized void chillDownDataFlow(final ZonedDateTime lastFramesTransferredTimeSinceLastConnection,
			final long framesTransferredCountSinceLastConnection) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.CHILL_DOWN_DATA_FLOW.toString());
		node.put(EMessageProperty.DATA_TRANSFER_TIME.toString(), 
				lastFramesTransferredTimeSinceLastConnection.format(
						DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		node.put(EMessageProperty.DATA_COUNT.toString(), framesTransferredCountSinceLastConnection);
		
		messageMap.put("chill_down", node);
	}

	/**
	 * Notify clients with SLE Forward data flow updates which include the total number of frames
	 * transferred since last BIND and the last frame data/time.
	 * 
	 * @param lastTransferDataTimeSinceLastBind
	 * 				Date/Time of last frame
	 * @param transferredDataCountSinceLastBind
	 * 				Total number of frames transferred since last BIND
	 */
	public synchronized void sleForwardProviderDataFlow(final ZonedDateTime lastTransferDataTimeSinceLastBind,
			final long transferredDataCountSinceLastBind) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_FORWARD_DATA_FLOW.toString());
		node.put(EMessageProperty.SLE_DATA_TRANSFER_TIME.toString(), 
				lastTransferDataTimeSinceLastBind.format(
						DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		node.put(EMessageProperty.DATA_COUNT.toString(), transferredDataCountSinceLastBind);
		messageMap.put("sle_forward", node);
	}

	/**
	 * Notify clients with SLE Return data flow updates which include the total number of frames
	 * transferred since last BIND and the last frame data/time.
	 * 
	 * @param lastTransferDataTimeSinceLastBind
	 * 				Date/Time of last frame
	 * @param transferredDataBytesSinceLastBind
	 * 				Total number of frames transferred since last BIND
	 */
	public synchronized void sleReturnProviderDataFlow(final ZonedDateTime lastTransferDataTimeSinceLastBind,
			final long transferredDataBytesSinceLastBind) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.SLE_RETURN_DATA_FLOW.toString());
		node.put(EMessageProperty.SLE_DATA_TRANSFER_TIME.toString(), 
				lastTransferDataTimeSinceLastBind.format(
						DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		node.put(EMessageProperty.DATA_COUNT.toString(), transferredDataBytesSinceLastBind);
		messageMap.put("sle_return", node);
		
	}

	/**
	 * Notify clients with AMPCS Uplink data flow updates which include the total number of CLTUs
	 * transferred since last Enable and the last CLTU time. 
	 * 
	 * @param lastCLTUsReceivedTimeSinceLastEnable
	 * 				Date/Time of last CLTU
	 * @param cltUsReceivedCountSinceLastEnable
	 * 				Total number of CLTUs transferred since last Enable
	 */
	public synchronized void chillUpDataFlow(final ZonedDateTime lastCLTUsReceivedTimeSinceLastEnable,
			final long cltUsReceivedCountSinceLastEnable) {
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.CHILL_UP_DATA_FLOW.toString());
		node.put(EMessageProperty.LAST_DATA_RECEIVED_TIME.toString(), 
				lastCLTUsReceivedTimeSinceLastEnable.format(
						DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()).toString());
		node.put(EMessageProperty.RECEIVED_DATA_COUNT.toString(), cltUsReceivedCountSinceLastEnable);
		messageMap.put("chill_up", node);
	}

	/**
	 * Add application log messages to an internal buffer which get sent
	 * to clients in batches based on a timer interval
	 * 
	 * @param messageItemTuple
	 * 				Map of log events
	 */
	public synchronized void eventMessage(final Map<String, String> messageItemTuple) {
		
		final ObjectNode messageNode = mapper.createObjectNode();
		ObjectNode containerNode = mapper.createObjectNode();
		ArrayNode listNode = mapper.createArrayNode();
		
		messageItemTuple.forEach((k,v)->{
			messageNode.put(k, v);
		});
		
		if (messageMap.containsKey("log_event_message")) {
			containerNode = messageMap.get("log_event_message");
			listNode = (ArrayNode) containerNode.get("message_list");
			listNode.add(messageNode);
		} else {
			listNode.add(messageNode);
			containerNode.put(EMessageProperty.MESSAGE_TYPE.toString(), EMessageType.EVENT_MESSAGE.toString());
			containerNode.set("message_list", listNode);
			messageMap.put("log_event_message", containerNode);
		}
		
	}

	/**
	 * Notify clients of SLE Forward Throw Events (bitrate, mod_index, command_modulation and range_modulation)
	 * 
	 * @param eventId
	 * 				Event ID which has the following mapping:
	 * 				<li>	4 - combined form of bitrate and mod_index
	 * 				<li>	5 - command_modulation
	 * 				<li>	6 - range_modulation
	 * @param qualifier
	 * 				Qualifier specified using a byte[]
	 */
	public synchronized void sleForwardProviderThrowEvent(final int eventId, final byte[] qualifier, final EMessageType throwEvent, final String errorMessage) {
		final String qualifierString = new String(qualifier);
		
		node = mapper.createObjectNode();
		node.put(EMessageProperty.MESSAGE_TYPE.toString(), throwEvent.toString());
		
		switch (throwEvent) {
		case SLE_FORWARD_PROVIDER_THROW_EVENT_SUCCESS:
			if (eventId == 4) {
				final String [] parts = qualifierString.split(" ");
				node.put("bitrate", parts[1]);
				node.put("mod_index", parts[3]);
			} else if (eventId == 5) {
				node.put("command_modulation", qualifierString);
			} else if (eventId == 6) {
				node.put("range_modulation", qualifierString);
			}
			break;
		case SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE:
			node.put("error_message", errorMessage);
			break;
		default:
			break;
		}
		notifyClients(node.toString());			
	}

	/**
	 * Send message to all connected clients without any delay
	 * 
	 * @param message 
	 * 			JSON formatted String message
	 */
	public synchronized void notifyClients(final String message) {
		for (final IWebsocketClient client : clientConnectionManager.getClientList()) {
			client.sendMessage(message);
		}
	}
	
	/**
	 * Flush log event message buffer and send messages in a batch to clients
	 */
	public synchronized void notifyClients() {
		
		if (messageMap != null && !messageMap.isEmpty()) {
			messageMap.forEach((k,v)->{
				notifyClients(v.toString());
				messageMap.remove(k);
			});
		}
	}	
}
