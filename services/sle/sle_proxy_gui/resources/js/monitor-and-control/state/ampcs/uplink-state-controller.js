/**
 * This module encapsulates all functionality associated with the
 * AMPCS Uplink state component on the Monitor and Control interface.
 */
var UplinkStateController = (function() {
	
	var $stateLabel;
	var $stateLabelText;
	var $stateTime;
	var $lastMessageTime;
	var $lastMessageCount;
	
	var $enableButton;
	var $disableButton;
	
	/**
	 * Chill Up state enum
	 * @enum
	 */
	var ChillUpState = {
		ENABLED : "ENABLED",
		DISABLED : "DISABLED",
	}
	
	/**
	 * Chill Up state action enum
	 * @enum
	 */
	var ChillUpStateAction = {
		ENABLE : "enable",
		DISABLE : "disable",
	}

	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		
		$stateLabel = $("#chill-up-state-label");
		$stateLabelText = $("#chill-up-state-label-text");
		$stateTime = $("#chill-up-state-time");
		$lastMessageTime = $("#chill-up-last-message-time");
		$lastMessageCount = $("#chill-up-data-message-count");
		
		$enableButton = $("#uplink-enable-button");
		$disableButton = $("#uplink-disable-button");
		
		$enableButton.click(function (){
			DataService.chillStateChange("uplink", ChillUpStateAction.ENABLE, failureHandler);			
		});
		
		$disableButton.click(function (){
			DataService.chillStateChange("uplink", ChillUpStateAction.DISABLE, failureHandler);			
		});
		
		// Subscribe to STATE change events that come through the websocket connection
		// These events have to do with uplink state changes (Enabled/Disabled)
		EventBus.subscribe(EventTopic.WEBSOCKET_CHILL_UP_STATE_CHANGE, handleWebsocketEvent);
		
		// Subscribe to Data Flow events that come through the websocket connection
		// These events have to do with message counts and times
		EventBus.subscribe(EventTopic.WEBSOCKET_CHILL_UP_DATA_FLOW, handleDataFlowMessage);
		
		if (serverState != null) {
			setState(serverState[StateObjectProperty.CHILL_INTERFACE_UPLINK_STATE]);
		}
	}
	
	/**
	 * Handle data flow messages coming through the websocket connection.
	 * @param {Object} message - message object with time/count properties
	 * @private
	 */
	function handleDataFlowMessage(message) {
		setLastMessageTime(message[ChillStateProperty.LAST_DATA_RECEIVED_TIME]);
		setLastMessageCount(message[ChillStateProperty.RECEIVED_DATA_COUNT]);		
	}	
	
	/**
	 * Set the init state of the component during application load
	 * @param {Object} uplinkState - object which contains state properties
	 * @private
	 */
	function setState(uplinkState) {
		setConnectionState(uplinkState[ChillStateProperty.STATE]);
		setStateTime(uplinkState[ChillStateProperty.STATE_CHANGE_TIME] || "...");
		setLastMessageTime(uplinkState[ChillStateProperty.LAST_DATA_RECEIVED_TIME] || "...");
		setLastMessageCount(uplinkState[ChillStateProperty.RECEIVED_DATA_COUNT] || "...");			
	}
	
	/**
	 * Set connection state based on the passed in state object
	 * @param {Object} state - connection state object
	 * @private
	 */
	function setConnectionState(state) {
		switch(state) {
		case ChillUpState.ENABLED:
			enableStateChange();
			break;
		case ChillUpState.DISABLED:
			disableStateChange();
			break;	
		default:
			break;
		}
	}
	
	/**
	 * Handle uplink state change messages coming through the websocket connection.
	 * @param {Object} message - message object with connection state properties
	 * @private
	 */
	function handleWebsocketEvent(message) {
		if (message["action"] === "enable") {
			setConnectionState(ChillUpState.ENABLED);
		}
		
		if (message["action"] === "disable") {
			setConnectionState(ChillUpState.DISABLED);
		}
		setStateTime(message[ChillStateProperty.STATE_CHANGE_TIME]);
		setLastMessageCount(message[ChillStateProperty.RECEIVED_DATA_COUNT]);
	}
	
	/**
	 * Update UI elements to reflect enabled state
	 * @private
	 */
	function enableStateChange() {
		
		$stateLabelText.text("Enabled");
		$stateLabel.addClass("green");
		$enableButton.addClass("disabled");
		$disableButton.removeClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect disabled state
	 * @private
	 */
	function disableStateChange() {
		$stateLabelText.text("Disabled");
		$stateLabel.removeClass("green");
		$disableButton.addClass("disabled");
		$enableButton.removeClass("disabled");
		
	}
	
	/**
	 * Handle server failure events
	 * @param {Object} response - object containing error information
	 * @private
	 */
	function failureHandler(response) {
		var message = "<b>Response Code:</b> " + response.responseJSON.code + "<br/>";
		message = message + "<b>Response Phrase:</b> " + response.responseJSON.reasonPhrase + "<br/>";
		message = message + "<b>Description:</b> " + response.responseJSON.description + "<br/>";
		ErrorModal.setMessage(message);
		ErrorModal.show();		
	}	
	
	/**
	 * Get the state change time.
	 * @returns {String} time - state change time
	 * @private
	 */
	function getStateTime() {
		return $stateTime.text();
	}
	
	/**
	 * Set the state change time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setStateTime(time) {
		$stateTime.text(time);
	}
	
	/**
	 * Get the last message time.
	 * @returns {String} time - last message time
	 * @private
	 */
	function getLastMessageTime() {
		return $lastMessageTime.text();
	}
	
	/**
	 * Set the last message time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setLastMessageTime(time) {
		$lastMessageTime.text(time);
	}
	
	/**
	 * Get the last message count.
	 * @returns {String} count - last message count
	 * @private
	 */
	function getLastMessageCount() {
		return $lastMessageCount.text();
	}
	
	/**
	 * Set the last message count.
	 * @param {String} time - the count to set
	 * @private
	 */
	function setLastMessageCount(count) {
		$lastMessageCount.text(count);
	}	
	
	// public methods
	return {
		init: init
	}
	
})();
