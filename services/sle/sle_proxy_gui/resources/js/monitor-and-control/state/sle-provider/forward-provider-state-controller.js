/**
 * This module encapsulates all functionality associated with the
 * SLE Forward Provider state component on the Monitor and Control interface.
 */
var ForwardProviderStateController = (function() {

	var $forwardProviderStateLabel;
	var $forwardProviderStateLabelText;
	var $forwardProviderStateTime;
	var $forwardProviderLastMessageTime;
	var $forwardProviderDataMessageCnt;
	var $connectionNumber;
	var $deliveryMode;
	
	var $bindButton;
	var $unbindButton;
	
	var $cmdModOnButton;
	var $cmdModOffButton;
	var $rangeModOnButton;
	var $rangeModOffButton;
	
	var $startButton;
	var $stopButton;
	var $abortButton;
	
	var currentForwardProfile;
	var $modIndexBitrateConfigDiv;
	var commandMap = {};
	
	var CommandModState = {
		ON : "on",
		OFF : "off"
	}
	
	var RangeModState = {
		ON : "on",
		OFF : "off",
		DISABLED : "disabled"
	}
	
	var currentState;
	
	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		$forwardProviderStateLabel = $("#forward-provider-state-label");
		$forwardProviderStateLabelText = $("#forward-provider-state-label-text");
		$forwardProviderStateTime = $("#forward-provider-state-time");
		$forwardProviderLastMessageTime = $("#forward-provider-last-message-time");
		$forwardProviderDataMessageCnt = $("#forward-provider-data-message-count");
		$connectionNumber = $("#forward-provider-connection-number");
		$deliveryMode = $("#forward-provider-delivery-mode");
		
		$modIndexBitrateConfigDiv = $("#mod-index-bitrate-config");
		
		
		$bindButton = $("#forward-provider-bind-button");
		$unbindButton = $("#forward-provider-unbind-button");
		
		$cmdModOnButton = $("#forward-command-mod-on-button");
		$cmdModOffButton = $("#forward-command-mod-off-button");
		
		$rangeModOnButton = $("#forward-range-mod-on-button");
		$rangeModOffButton = $("#forward-range-mod-off-button");
		
		$startButton = $("#forward-provider-start-button");
		$stopButton = $("#forward-provider-stop-button");
		$abortButton = $("#forward-provider-abort-button");
		
		$bindButton.click(function (){
			currentForwardProfile = ProviderConfig.getSelectedForwardProvider();
			DataService.providerStateChange("forward", ProviderStateAction.BIND, currentForwardProfile, failureHandler);			
		});
		
		$unbindButton.click(function (){
			DataService.providerStateChange("forward", ProviderStateAction.UNBIND, null, failureHandler);
		});
		
		$cmdModOnButton.click(function (){
			commandMap = {};
			commandMap["set-command-mod"] = "on";
			DataService.forwardProviderThrow(commandMap, failureHandler);
		});
		
		$cmdModOffButton.click(function (){
			commandMap = {};
			commandMap["set-command-mod"] = "off";
			DataService.forwardProviderThrow(commandMap, failureHandler);
		});
		
		$rangeModOnButton.click(function (){
			commandMap = {};
			commandMap["set-range-mod"] = "on";
			DataService.forwardProviderThrow(commandMap, failureHandler);
		});
		
		$rangeModOffButton.click(function (){
			commandMap = {};
			commandMap["set-range-mod"] = "off";
			DataService.forwardProviderThrow(commandMap, failureHandler);
		});
		
		$startButton.click(function (){
			DataService.providerStateChange("forward", ProviderStateAction.START, null, failureHandler);
		});
		
		$stopButton.click(function (){
			DataService.providerStateChange("forward", ProviderStateAction.STOP, null, failureHandler);
		});
		
		$abortButton.click(function (){
			DataService.providerStateChange("forward", ProviderStateAction.ABORT, null, failureHandler);
		});
		
		// Subscribe to Forward Provider profile selection event
		EventBus.subscribe(EventTopic.FORWARD_PROVIDER_SELECTION, handleProviderSelectionEvent);
		
		// Subscribe to Forward Provider state change events
		EventBus.subscribe(EventTopic.WEBSOCKET_FORWARD_PROVIDER_BIND, handleWebsocketEvent);
		
		// Subscribe to Forward Provider throw events
		EventBus.subscribe(EventTopic.WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_SUCCESS, handleWebsocketThrowEventSuccess);

		// Subscribe to Forward Provider throw events
		EventBus.subscribe(EventTopic.WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT_FAILURE, handleWebsocketThrowEventFailure);		
		
		// Subscribe to Forward Provider data flow events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_FORWARD_DATA_FLOW, handleDataFlowMessage);
		
		// Subscribe to Forward Provider delivery mode change events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_FORWARD_DELIVERY_MODE_CHANGE, handleDeliveryModeChangeMessage);
		
		currentState = ProviderState.UNBOUND;
		
		if (serverState != null) {
			setState(serverState[StateObjectProperty.SLE_INTERFACE_FORWARD_SERVICE_STATE]);
		}
	}
	
	/**
	 * Handle data flow messages coming through the websocket connection.
	 * @param {Object} message - message object with time/count properties
	 * @private
	 */
	function handleDataFlowMessage(message) {
		setLastMessageTime(message[SleStateProperty.LAST_TRANSFER_DATE_TIME]);
		setLastMessageCount(message[SleStateProperty.TRANSFER_DATA_COUNT]);		
	}
	
	/**
	 * Handle delivery mode change message coming through the websocket connection.
	 * @param {Object} message - message object with delivery mode information
	 * @private
	 */	
	function handleDeliveryModeChangeMessage(message) {
		var delMode = message[SleStateProperty.DELIVERY_MODE];
		if (delMode !== "") {
			setDeliveryMode(message[SleStateProperty.DELIVERY_MODE]);
		} else {
			setDeliveryMode("...");
		}
	}
	
	/**
	 * Handle successful throw event messages coming through the websocket connection.
	 * @param {Object} message - message object with throw event properties
	 * @private
	 */
	function handleWebsocketThrowEventSuccess(message) {
		if (message.hasOwnProperty("command_modulation")) {
			if (message["command_modulation"] === "on") {
				setCmdModOn();
			} else if (message["command_modulation"] === "off") {
				setCmdModOff();
			}
		}
		
		if (message.hasOwnProperty("range_modulation")) {
			if (message["range_modulation"] === "on") {
				setRangeModOn()
			} else if (message["range_modulation"] === "off") {
				setRangeModOff();
			}
		}
		
	}

	/**
	 * Handle failure throw event messages coming through the websocket connection.
	 * @param {Object} message - message object with throw event properties
	 * @private
	 */
	function handleWebsocketThrowEventFailure(message) {
		var errorMessage =  message["error_message"] + "<br/>";
		errorMessage += "See log messages for more detail.<br/><br/>";
		errorMessage += "Please note that the UI will re-synchronize with the server after this modal<br/>";
		errorMessage += "closes to prevent UI-Server out of sync conditions";
		ErrorModal.setHeader("Encountered Throw Event Failure");
		ErrorModal.setMessage(errorMessage);
		
		// We should re-sync the GUI with the server
		ErrorModal.setCallbackFunction(function() {
			location.reload();
		});	
		
		ErrorModal.show();
	}
	
	/**
	 * Handle state change messages coming through the websocket connection.
	 * @param {Object} message - message object with connection state properties
	 * @private
	 */
	function handleWebsocketEvent(message) {
		switch (message[WebsocketMesssageProperty.ACTION]) {
		case ProviderStateAction.BIND:
			/**
			 * MPCS-8701 - 06/15/17
			 * Reset data count to 0 when BIND event occurs
			 */
			setLastMessageCount(0);
		case ProviderStateAction.STOP:
			setConnectionState(ProviderState.READY);
			break;
		case ProviderStateAction.UNBIND:
		case ProviderStateAction.ABORT:
			setConnectionState(ProviderState.UNBOUND);
			break;
		case ProviderStateAction.START:
			setConnectionState(ProviderState.ACTIVE);
			break;
		default:
			break;
		}
		
		setStateTime(message[WebsocketMesssageProperty.STATE_CHANGE_TIME]);
		setConnectionNumber(message[SleStateProperty.CURRENT_CONNECTION_NUMBER]);
	}

	
		
	/**
	 * Set the init state of the component during application load
	 * @param {Object} sleForwardState - object which contains state properties
	 * @private
	 */
	function setState(sleForwardState) {
		
		if (sleForwardState == null) {
			return;
		}
		
		setConnectionState(sleForwardState[SleStateProperty.STATE]);

		if (sleForwardState[SleStateProperty.BOUND_PROFILE] === "") {
			$bindButton.addClass("disabled");
		}	
		
		setCommandModState(sleForwardState[SleStateProperty.COMMAND_MOD_STATE]);
		setRangeModState(sleForwardState[SleStateProperty.RANGE_MOD_STATE]);
		
		setStateTime(sleForwardState[SleStateProperty.STATE_CHANGE_TIME] || "...");
		setLastMessageTime(sleForwardState[SleStateProperty.LAST_TRANSFER_DATE_TIME] || "...");
		setLastMessageCount(sleForwardState[SleStateProperty.TRANSFER_DATA_COUNT] || "...");
		setConnectionNumber(sleForwardState[SleStateProperty.CURRENT_CONNECTION_NUMBER] || "...");
		setDeliveryMode(sleForwardState[SleStateProperty.DELIVERY_MODE] || "...");
	}
	
	/**
	 * Set connection state based on the passed in state object
	 * @param {Object} state - connection state object
	 * @private
	 */
	function setConnectionState(state, providerStateAction) {
		switch(state) {
		case ProviderState.UNBOUND:
			unbindStateChange();
			break;
		case ProviderState.READY:
			if (currentState === ProviderState.UNBOUND) {
				bindStateChange();
			}
			if (currentState === ProviderState.ACTIVE) {
				$forwardProviderStateLabelText.text("Ready");
				$forwardProviderStateLabel.removeClass("green");
				$forwardProviderStateLabel.addClass("blue");
				$startButton.removeClass("disabled");
				$stopButton.addClass("disabled");
				$unbindButton.removeClass("disabled");
			}	
			break;	
		case ProviderState.ACTIVE:
			startStateChange();
			break;
		}
		
		currentState = state;
	}
	
	/**
	 * Set Command Modulation state
	 * @param {CommandModState} state - state of command modulation
	 * which can be on or off
	 */
	function setCommandModState(state) {
		var commandModState = state.toLowerCase();
		
		switch (commandModState) {
		case CommandModState.ON:
			setCmdModOn();
			break;
		case CommandModState.OFF:
			setCmdModOff();
			break;
		default:
			break;
		}
	}
	
	/**
	 * Set Range Modulation state
	 * @param {RangeModState} state - state of range modulation
	 * which can be on, off or disabled
	 */
	function setRangeModState(state) {
		var rangeModState = state.toLowerCase();
		
		switch (rangeModState) {
		case RangeModState.ON:
			setRangeModOn();
			break;
		case RangeModState.OFF:
			setRangeModOff();
			break;
		case RangeModState.DISABLED:
			setRangeModDisabled();
			break;
		default:
			break;
		}
	}
		
	/**
	 * Handle server failure events
	 * @param {Object} response - object containing error information
	 * @private
	 */
	function failureHandler(response) {
		var setReloadCallback = true;
		var message = "<b>Response Code:</b> " + response.responseJSON.code + "<br/>";
		message += "<b>Response Phrase:</b> " + response.responseJSON.reasonPhrase + "<br/>";
		message += "<b>Description:</b> " + response.responseJSON.description + "<br/><br/>";

		// We should re-sync the GUI with the server for all error conditions except 
		// for those errors we know for sure won't cause the out of sync condition.
		if (setReloadCallback) {
			message += "Please note that the UI will re-synchronize with the server after this modal<br/>";
			message += "closes to prevent UI-Server out of sync conditions";

			ErrorModal.setCallbackFunction(function() {
				location.reload();
			});	
		}
		
		ErrorModal.setMessage(message);
		ErrorModal.show();
	}
	
	/**
	 * Enable the Mod Index and Bitrate component. This component is enabled
	 * when the connection is in bound state.
	 * @private
	 */
	function enableModIndexBitrateDiv() {
		$modIndexBitrateConfigDiv.removeClass("disableddiv");
	}
	
	/**
	 * Disable the Mod Index and Bitrate component. This component is diabled
	 * when the connection is in unbound state.
	 * @private
	 */
	function disableModIndexBitrateDiv() {
		$modIndexBitrateConfigDiv.addClass("disableddiv");
	}	
	
	/**
	 * Update UI elements to reflect bound/ready state
	 * @private
	 */
	function bindStateChange() {

		EventBus.publish(EventTopic.FORWARD_PROVIDER_BIND, {});
		
		$forwardProviderStateLabelText.text("Ready");
		
		enableModIndexBitrateDiv();
		$forwardProviderStateLabel.addClass("blue");
		$bindButton.addClass("disabled");
		$cmdModOnButton.removeClass("disabled");
		$rangeModOnButton.removeClass("disabled");
		$startButton.removeClass("disabled");
		$stopButton.addClass("disabled");
		$unbindButton.removeClass("disabled");
		$abortButton.removeClass("disabled");
	}

	/**
	 * Update UI elements to reflect unbound state
	 * @private
	 */
	function unbindStateChange() {
		
		EventBus.publish(EventTopic.FORWARD_PROVIDER_UNBIND, {});
		$forwardProviderStateLabelText.text("Unbound");
		
		disableModIndexBitrateDiv();
		$forwardProviderStateLabel.removeClass("blue");
		$forwardProviderStateLabel.removeClass("green");
		$forwardProviderStateLabel.removeClass("red");

		$bindButton.removeClass("disabled");
		$cmdModOnButton.addClass("disabled");
		$rangeModOnButton.addClass("disabled");
		$cmdModOffButton.addClass("disabled");
		$rangeModOffButton.addClass("disabled");
		$startButton.addClass("disabled");
		$stopButton.addClass("disabled");
		$unbindButton.addClass("disabled");
		$abortButton.addClass("disabled");
	}
	
	/**
	 * Get the state change time.
	 * @returns {String} time - state change time
	 * @private
	 */
	function getStateTime() {
		return $forwardProviderStateTime.text();
	}
	
	/**
	 * Set the state change time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setStateTime(time) {
		$forwardProviderStateTime.text(time);
	}
	
	/**
	 * Get the last message time.
	 * @returns {String} time - last message time
	 * @private
	 */
	function getLastMessageTime() {
		return $forwardProviderLastMessageTime.text();
	}

	/**
	 * Set the last message time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setLastMessageTime(time) {
		$forwardProviderLastMessageTime.text(time);
	}
	
	/**
	 * Get the last message count.
	 * @returns {String} count - last message count
	 * @private
	 */
	function getLastMessageCount() {
		return $forwardProviderDataMessageCnt.text();
	}
	
	/**
	 * Set the last message count.
	 * @param {String} time - the count to set
	 * @private
	 */
	function setLastMessageCount(count) {
		$forwardProviderDataMessageCnt.text(count);
	}
	
	/**
	 * Get the connection number.
	 * @returns {String} number - connection number
	 * @private
	 */
	function getConnectionNumber() {
		return $connectionNumber.text();
	}
	
	/**
	 * Set the connection number.
	 * @param {String} number - connection number to set
	 * @private
	 */
	function setConnectionNumber(connNumber) {
		$connectionNumber.text(connNumber);
	}	
	
	/**
	 * Get the delivery mode.
	 * @returns {String} text - delivery mode
	 * @private
	 */
	function getDeliveryMode() {
		return $deliveryMode.text();
	}
	
	/**
	 * Set the delivery mode.
	 * @param {String} text - delivery mode to set
	 * @private
	 */
	function setDeliveryMode(deliveryMode) {
		$deliveryMode.text(deliveryMode);
	}
	
	/**
	 * Update UI elements to reflect Command Modulation 
	 * is in On state
	 * @private
	 */
	function setCmdModOn() {
		$cmdModOnButton.addClass("disabled");
		$cmdModOffButton.removeClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect Command Modulation 
	 * is in Off state
	 * @private
	 */
	function setCmdModOff() {
		$cmdModOnButton.removeClass("disabled");
		$cmdModOffButton.addClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect Range Modulation
	 * is in On state
	 * @private
	 */
	function setRangeModOn() {
		$rangeModOnButton.addClass("disabled");
		$rangeModOffButton.removeClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect Range Modulation
	 * is in Off state
	 * @private
	 */
	function setRangeModOff() {
		$rangeModOnButton.removeClass("disabled");
		$rangeModOffButton.addClass("disabled");		
	}

	/**
	 * Update UI elements to reflect Range Modulation
	 * is in Disabled state
	 * @private
	 */
	function setRangeModDisabled() {
		$rangeModOnButton.addClass("disabled");
		$rangeModOffButton.addClass("disabled");		
	}	
	
	/**
	 * Update UI elements to reflect active state when start is initiated
	 * on the connection.
	 * @private
	 */
	function startStateChange() {
		$forwardProviderStateLabelText.text("Active");

		$forwardProviderStateLabel.removeClass("blue");
		$forwardProviderStateLabel.addClass("green");

		$bindButton.addClass("disabled");
		$startButton.addClass("disabled");
		$stopButton.removeClass("disabled");
		$unbindButton.addClass("disabled");
		$abortButton.removeClass("disabled");
		
		enableModIndexBitrateDiv();
		//$cmdModOnButton.removeClass("disabled");
		//$rangeModOnButton.removeClass("disabled");

	}

	/**
	 * Update UI elements to reflect ready state when stop is initiated
	 * on the connection.
	 * @private
	 */
	function stopStateChange() {
		$forwardProviderStateLabelText.text("Ready");

		$forwardProviderStateLabel.removeClass("green");
		$forwardProviderStateLabel.addClass("blue");

		$bindButton.addClass("disabled");
		$startButton.removeClass("disabled");
		$stopButton.addClass("disabled");
		$unbindButton.removeClass("disabled");
		$abortButton.removeClass("disabled");
	}
	
	/**
	 * Update UI elements to reflect unbound state when abort is initiated
	 * on the connection.
	 * @private
	 */
	function abortStateChange() {
		unbindStateChange();
	}
	
	/**
	 * Enable the Bind button
	 * @private
	 */
	function enableBind() {
		$bindButton.removeClass("disabled");
	}
	
	/**
	 * Handle provider profile selection event
	 * @private
	 */
	function handleProviderSelectionEvent() {
		enableBind();
	}
	
	// public methods
	return {
		init: init,
	}

})();