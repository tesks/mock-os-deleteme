/**
 * Websocket Message Type Enum
 * @enum
 */
var WebsocketMessagaType = {
	SLE_RETURN_PROVIDER_STATE_CHANGE: "sle_return_provider_state_change",
	SLE_FORWARD_PROVIDER_STATE_CHANGE: "sle_forward_provider_state_change",
	SLE_FORWARD_PROVIDER_THROW_EVENT_SUCCESS: "sle_forward_provider_throw_event_success",
	SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE: "sle_forward_provider_throw_event_failure",
	CHILL_DOWN_STATE_CHANGE: "chill_down_state_change",
	CHILL_UP_STATE_CHANGE: "chill_up_state_change",
	SLE_PROFILE_CREATE : "sle_profile_create",
	SLE_PROFILE_UPDATE : "sle_profile_update",
	SLE_PROFILE_DELETE : "sle_profile_delete",
	CHILL_CONFIG_UPDATE : "chill_config_update",
	CHILL_DOWN_DATA_FLOW : "chill_down_data_flow",
	CHILL_UP_DATA_FLOW : "chill_up_data_flow",
	SLE_RETURN_DATA_FLOW : "sle_return_data_flow",
	SLE_FORWARD_DATA_FLOW : "sle_forward_data_flow",
	EVENT_MESSAGE : "event_message",
	SLE_FORWARD_DELIVERY_MODE_CHANGE : "sle_forward_delivery_mode_change",
	SLE_RETURN_DELIVERY_MODE_CHANGE : "sle_return_delivery_mode_change",
	
}

/**
 * Websocket Message Property Enum
 * @enum
 */
var WebsocketMesssageProperty = {
	MESSAGE_TYPE : "message_type",
	PROFILE_NAME : "profile_name",
	PROFILE : "profile",
	STATE_CHANGE_TIME : "state_change_time",
	ACTION : "action",
	DATA_TRANSFER_TIME : "last_data_transferred_time",
	DATA_COUNT : "transferred_data_count"		
}