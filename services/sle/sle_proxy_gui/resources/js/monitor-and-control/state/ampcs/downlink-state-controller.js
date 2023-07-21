/**
 * This module encapsulates all functionality associated with the
 * AMPCS Downlink state component on the Monitor and Control interface.
 */
var DownlinkStateController = (function() {
	
	var $stateLabel;
	var $stateLabelText;
	var $stateTime;
	var $lastMessageTime;
	var $lastMessageCount;
	
	var $connectButton;
	var $disconnectButton;
	
	/**
	 * Chill Down state enum
	 * @enum
	 */
	var ChillDownState = {
		CONNECTED : "CONNECTED",
		DISCONNECTED : "DISCONNECTED",
	}
	
	/**
	 * Chill Down state action enum
	 * @enum
	 */
	var ChillDownStateAction = {
		CONNECT : "connect",
		DISCONNECT : "disconnect",
	}
	
	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		
		$stateLabel = $("#chill-down-state-label");
		$stateLabelText = $("#chill-down-state-label-text");
		$stateTime = $("#chill-down-state-time");
		$lastMessageTime = $("#chill-down-last-message-time");
		$lastMessageCount = $("#chill-down-data-message-count");
		
		$connectButton = $("#downlink-connect-button");
		$disconnectButton = $("#downlink-disconnect-button");
		
		$connectButton.click(function (){
			DataService.chillStateChange("downlink", ChillDownStateAction.CONNECT, failureHandler);			
		});
		
		$disconnectButton.click(function (){
			DataService.chillStateChange("downlink", ChillDownStateAction.DISCONNECT, failureHandler);			
		});
		
		// Subscribe to STATE change events that come through the websocket connection
		// These events have to do with downlink connection state changes (Connected/Disconnected)
		EventBus.subscribe(EventTopic.WEBSOCKET_CHILL_DOWN_STATE_CHANGE, handleConnectionStateChangeMessage);
		
		// Subscribe to Data Flow events that come through the websocket connection
		// These events have to do with message counts and times
		EventBus.subscribe(EventTopic.WEBSOCKET_CHILL_DOWN_DATA_FLOW, handleDataFlowMessage);
		
		if (serverState != null) {
			setState(serverState[StateObjectProperty.CHILL_INTERFACE_DOWNLINK_STATE]);
		}
	}
	
	/**
	 * Handle data flow messages coming through the websocket connection.
	 * @param {Object} message - message object with time/count properties
	 * @private
	 */
	function handleDataFlowMessage(message) {
		setLastMessageTime(message[ChillStateProperty.LAST_DATA_TRANSFERRED_TIME]);
		setLastMessageCount(message[ChillStateProperty.TRANSFERRED_DATA_COUNT]);		
	}
	
	/**
	 * Set the init state of the component during application load
	 * @param {Object} downlinkState - object which contains state properties
	 * @private
	 */
	function setState(downlinkState) {
		setConnectionState(downlinkState[ChillStateProperty.STATE]);
		setStateTime(downlinkState[ChillStateProperty.STATE_CHANGE_TIME] || "...");
		setLastMessageTime(downlinkState[ChillStateProperty.LAST_DATA_TRANSFERRED_TIME] || "...");
		setLastMessageCount(downlinkState[ChillStateProperty.TRANSFERRED_DATA_COUNT] || "...");		
	}
	
	/**
	 * Handle connection state change messages coming through the websocket connection.
	 * @param {Object} message - message object with connection state properties
	 * @private
	 */
	function handleConnectionStateChangeMessage(message) {
		if (message["action"] === "connect") {
			setConnectionState(ChillDownState.CONNECTED);
		}
		
		if (message["action"] === "disconnect") {
			setConnectionState(ChillDownState.DISCONNECTED);
		}
		
		setStateTime(message[ChillStateProperty.STATE_CHANGE_TIME]);
		setLastMessageCount(message[ChillStateProperty.TRANSFERRED_DATA_COUNT]);		
	}	
	
	/**
	 * Set connection state based on the passed in state object
	 * @param {Object} state - connection state object
	 * @private
	 */
	function setConnectionState(state) {
		switch(state) {
		case ChillDownState.CONNECTED:
			connectStateChange();
			break;
		case ChillDownState.DISCONNECTED:
			disconnectStateChange();
			break;	
		default:
			break;
		}
	}	
	
	/**
	 * Update UI elements to reflect connected state
	 * @private
	 */
	function connectStateChange() {
		
		$stateLabelText.text("Connected");
		$stateLabel.addClass("green");
		$connectButton.addClass("disabled");
		$disconnectButton.removeClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect disconnected state
	 * @private
	 */
	function disconnectStateChange() {
		$stateLabelText.text("Disconnected");
		$stateLabel.removeClass("green");
		$disconnectButton.addClass("disabled");
		$connectButton.removeClass("disabled");
		
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
	 * Get the connection state change time.
	 * @returns {String} time - state change time
	 * @private
	 */
	function getStateTime() {
		return $stateTime.text();
	}

	/**
	 * Set the connection state change time.
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
