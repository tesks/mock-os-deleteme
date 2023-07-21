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

/**
 * Enumeration of all defined JSON message properties used to notify Web GUI 
 * clients through WebSocket connection of state and event changes.
 * 
 */
public enum EMessageProperty {

	/**
	 * All messages must specify a message_type for proper routing
	 * and handling on the GUI side
	 */
	MESSAGE_TYPE("message_type"),
	
	/**
	 * Profile Name
	 */
	PROFILE_NAME("profile_name"),
	
	/**
	 * This property is used to identify the profile object
	 */
	PROFILE("profile"),
	
	/**
	 * State change time
	 */
	STATE_CHANGE_TIME("state_change_time"),
	
	/**
	 * Used to identify the action which triggered the event/change
	 */
	ACTION("action"),
	
	/**
	 * Last data transfer time which maps to the same property
	 * defined in the state resource for AMPCS downlink
	 */
	DATA_TRANSFER_TIME("last_data_transferred_time"),
	
	/**
	 * Last transfer date/time which maps to the same property
	 * defined in the state resource for SLE Providers
	 */
	SLE_DATA_TRANSFER_TIME("last_transfer_data_time"),
	
	/**
	 * Transferred data count
	 */
	DATA_COUNT("transferred_data_count"),
	
	/**
	 * Connection number which is applicable for SLE Provider connection only
	 */
	CURRENT_CONNECTION_NUMBER("current_session_number"),
	
	/**
	 * Last data received time which maps to the same property
	 * defined in the state resource for AMPCS Uplink
	 */
	LAST_DATA_RECEIVED_TIME("last_data_received_time"),
	
	/**
	 * Received data count which maps to the same property
	 * defined in the state resource for AMPCS Uplink
	 */
	RECEIVED_DATA_COUNT("received_data_count"), 
	
	/**
	 * Throw event applicable to SLE Forward connections only
	 */
	THROW_EVENT("throw_event"),
		
	/**
	 * Delivery mode applicable to SLE Providers only
	 */
	DELIVERY_MODE("delivery_mode");
	
    private final String val;

    private EMessageProperty(final String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
