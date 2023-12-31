/**
 * Event Topic Enum which defines all possible Event Types.
 * @enum
 */
var EventTopic = {
	FORWARD_PROVIDER_SELECTION : "FORWARD_PROVIDER_SELECTION",
	RETURN_PROVIDER_SELECTION : "RETURN_PROVIDER_SELECTION",
	FORWARD_PROVIDER_BIND : "FORWARD_PROVIDER_BIND",
	RETURN_PROVIDER_BIND : "RETURN_PROVIDER_BIND",
	FORWARD_PROVIDER_UNBIND : "FORWARD_PROVIDER_UNBIND",
	RETURN_PROVIDER_UNBIND : "RETURN_PROVIDER_UNBIND",
	WEBSOCKET_CHILL_DOWN_STATE_CHANGE : "WEBSOCKET_CHILL_DOWN_STATE_CHANGE",
	WEBSOCKET_CHILL_UP_STATE_CHANGE : "WEBSOCKET_CHILL_UP_STATE_CHANGE",
	WEBSOCKET_RETURN_PROVIDER_BIND : "WEBSOCKET_RETURN_PROVIDER_BIND",
	WEBSOCKET_FORWARD_PROVIDER_BIND : "WEBSOCKET_FORWARD_PROVIDER_BIND",
	WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_SUCCESS : "WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_SUCCESS",
	WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_FAILURE : "WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_FAILURE",
	WEBSOCKET_SLE_PROFILE_CREATE : "WEBSOCKET_SLE_PROFILE_CREATE",
	WEBSOCKET_SLE_PROFILE_UPDATE : "WEBSOCKET_SLE_PROFILE_UPDATE",
	WEBSOCKET_SLE_PROFILE_DELETE : "WEBSOCKET_SLE_PROFILE_DELETE",
	WEBSOCKET_CHILL_CONFIG_UPDATE : "WEBSOCKET_CHILL_CONFIG_UPDATE",
	WEBSOCKET_CHILL_DOWN_DATA_FLOW : "WEBSOCKET_CHILL_DOWN_DATA_FLOW",
	WEBSOCKET_CHILL_UP_DATA_FLOW : "WEBSOCKET_CHILL_UP_DATA_FLOW",
	WEBSOCKET_SLE_RETURN_DATA_FLOW : "WEBSOCKET_SLE_RETURN_DATA_FLOW",
	WEBSOCKET_SLE_FORWARD_DATA_FLOW : "WEBSOCKET_SLE_FORWARD_DATA_FLOW",
	WEBSOCKET_EVENT_MESSAGE : "WEBSOCKET_EVENT_MESSAGE",
	WEBSOCKET_SLE_FORWARD_DELIVERY_MODE_CHANGE : "WEBSOCKET_SLE_FORWARD_DELIVERY_MODE_CHANGE",
	WEBSOCKET_SLE_RETURN_DELIVERY_MODE_CHANGE : "WEBSOCKET_SLE_RETURN_DELIVERY_MODE_CHANGE"
}