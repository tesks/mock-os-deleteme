/**
 * Websocket Message Handler functions like a router which 
 * transfers messages received from the websocket connection 
 * to the applicable topics on the internal message/event bus
 */
var WebsocketMessageHandler = (function() {
	
	/**
	 * Message handler function
	 * @param {Object} message - message object received through the 
	 * websocket connection
	 */
	function handleMessage(message) {
		
		switch (message.message_type) {
		case WebsocketMessagaType.CHILL_DOWN_STATE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_CHILL_DOWN_STATE_CHANGE, message);
			break;
		case WebsocketMessagaType.CHILL_UP_STATE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_CHILL_UP_STATE_CHANGE, message);
			break;
		case WebsocketMessagaType.SLE_RETURN_PROVIDER_STATE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_RETURN_PROVIDER_BIND, message);
			break;
		case WebsocketMessagaType.SLE_FORWARD_PROVIDER_STATE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_FORWARD_PROVIDER_BIND, message);
			break;
		case WebsocketMessagaType.SLE_FORWARD_PROVIDER_THROW_EVENT_SUCCESS:
			EventBus.publish(EventTopic.WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_SUCCESS, message);
			break;
		case WebsocketMessagaType.SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE:
			EventBus.publish(EventTopic.WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_FAILURE, message);
			break;
		case WebsocketMessagaType.SLE_PROFILE_CREATE:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_PROFILE_CREATE, message);
			break;
		case WebsocketMessagaType.SLE_PROFILE_UPDATE:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_PROFILE_UPDATE, message);
			break;
		case WebsocketMessagaType.SLE_PROFILE_DELETE:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_PROFILE_DELETE, message);
			break;
		case WebsocketMessagaType.CHILL_CONFIG_UPDATE:
			EventBus.publish(EventTopic.WEBSOCKET_CHILL_CONFIG_UPDATE, message);
			break;
		case WebsocketMessagaType.CHILL_DOWN_DATA_FLOW:
			EventBus.publish(EventTopic.WEBSOCKET_CHILL_DOWN_DATA_FLOW, message);
			break;
		case WebsocketMessagaType.CHILL_UP_DATA_FLOW:
			EventBus.publish(EventTopic.WEBSOCKET_CHILL_UP_DATA_FLOW, message);
			break;
		case WebsocketMessagaType.SLE_RETURN_DATA_FLOW:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_RETURN_DATA_FLOW, message);
			break;
		case WebsocketMessagaType.SLE_FORWARD_DATA_FLOW:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_FORWARD_DATA_FLOW, message);
			break;
		case WebsocketMessagaType.EVENT_MESSAGE:
			EventBus.publish(EventTopic.WEBSOCKET_EVENT_MESSAGE, message);
			break;
		case WebsocketMessagaType.SLE_FORWARD_DELIVERY_MODE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_FORWARD_DELIVERY_MODE_CHANGE, message);
			break;
		case WebsocketMessagaType.SLE_RETURN_DELIVERY_MODE_CHANGE:
			EventBus.publish(EventTopic.WEBSOCKET_SLE_RETURN_DELIVERY_MODE_CHANGE, message);
			break;
		default:
			break;
		}
	}
	
	return {
		handleMessage: handleMessage
	}
	
})();