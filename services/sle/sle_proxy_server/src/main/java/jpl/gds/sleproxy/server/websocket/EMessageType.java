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
 * Enumerates all of the message types that can be sent to the Web Client GUI
 * through the websocket connection
 * 
 */
public enum EMessageType {
	
	/**
	 * Creation of SLE Provider profile
	 */
	SLE_PROFILE_CREATE("sle_profile_create"),

	/**
	 * Update of SLE Provider profile
	 */
	SLE_PROFILE_UPDATE("sle_profile_update"),

	/**
	 * Deletion of SLE Provider profile
	 */
	SLE_PROFILE_DELETE("sle_profile_delete"),
	
	/**
	 * AMPCS Downlink state change
	 */
	CHILL_DOWN_STATE_CHANGE("chill_down_state_change"),
	
	/**
	 * AMPCS Downlink data flow event
	 */
	CHILL_DOWN_DATA_FLOW("chill_down_data_flow"),
	
	/**
	 * AMPCS Uplink data flow event
	 */
	CHILL_UP_DATA_FLOW("chill_up_data_flow"),
	
	/**
	 * AMPCS Uplink state change
	 */
	CHILL_UP_STATE_CHANGE("chill_up_state_change"),
	
	/**
	 * SLE Return Provider state change
	 */
	SLE_RETURN_PROVIDER_STATE_CHANGE("sle_return_provider_state_change"),
	
	/**
	 * SLE Forward Provider state change
	 */
	SLE_FORWARD_PROVIDER_STATE_CHANGE("sle_forward_provider_state_change"),
	
	/**
	 * AMPCS Downlink config update
	 */
	CHILL_CONFIG_UPDATE("chill_config_update"),
	
	/**
	 * SLE Forward Provider data flow event
	 */
	SLE_FORWARD_DATA_FLOW("sle_forward_data_flow"),
	
	/**
	 * SLE Return Provider data flow event
	 */
	SLE_RETURN_DATA_FLOW("sle_return_data_flow"),
	
	/**
	 * Event log message
	 */
	EVENT_MESSAGE("event_message"),
	
	/**
	 * SLE Forward Provider Successful Throw event
	 */
	SLE_FORWARD_PROVIDER_THROW_EVENT_SUCCESS("sle_forward_provider_throw_event_success"),
	
	/**
	 * SLE Forward Provider Failed Throw event
	 */
	SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE("sle_forward_provider_throw_event_failure"),
	
	/**
	 * SLE Return Provider delivery mode change
	 */
	SLE_RETURN_DELIVERY_MODE_CHANGE("sle_return_delivery_mode_change"),
	
	/**
	 * SLE Forward Provider delivery mode change
	 */
	SLE_FORWARD_DELIVERY_MODE_CHANGE("sle_forward_delivery_mode_change");
	
    private final String val;

    private EMessageType(final String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
