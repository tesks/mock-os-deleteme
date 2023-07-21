/**
 * This module encapsulates all functionality associated with the
 * SLE Return Provider state component on the Monitor and Control interface.
 */
var ReturnProviderStateController = (function() {

	var $returnProviderStateLabel;
	var $returnProviderStateLabelText;
	var $returnProviderStateTime;
	var $returnProviderLastMessageTime;
	var $returnProviderDataMessageCnt;
	var $connectionNumber;
	var $deliveryMode;

	var $bindButton;
	var $unbindButton;
	var $startButton;
	var $stopButton;
	var $abortButton;
	
	var currentReturnProfile;
	
	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		$returnProviderStateLabel = $("#return-provider-state-label");
		$returnProviderStateLabelText = $("#return-provider-state-label-text");
		$returnProviderStateTime = $("#return-provider-state-time");
		$returnProviderLastMessageTime = $("#return-provider-last-message-time");
		$returnProviderDataMessageCnt = $("#return-provider-data-message-count");
		$connectionNumber = $("#return-provider-connection-number");
		$deliveryMode = $("#return-provider-delivery-mode");
		
		$bindButton = $("#return-provider-bind-button");
		$unbindButton = $("#return-provider-unbind-button");
		$startButton = $("#return-provider-start-button");
		$stopButton = $("#return-provider-stop-button");
		$abortButton = $("#return-provider-abort-button");
		
		// Subscribe to Return Provider profile selection event
		EventBus.subscribe(EventTopic.RETURN_PROVIDER_SELECTION, handleProviderSelectionEvent);
		
		// Subscribe to Return Provider state change events
		EventBus.subscribe(EventTopic.WEBSOCKET_RETURN_PROVIDER_BIND, handleWebsocketStateChangeEvent);
		
		// Subscribe to Return Provider data flow events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_RETURN_DATA_FLOW, handleDataFlowMessage);
		
		// Subscribe to Return Provider delivery mode change events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_RETURN_DELIVERY_MODE_CHANGE, handleDeliveryModeChangeMessage);
		
		$bindButton.click(function (){
			currentReturnProfile = ProviderConfig.getSelectedReturnProvider();
			DataService.providerStateChange("return", ProviderStateAction.BIND, currentReturnProfile, failureHandler);			
		});

		$startButton.click(function (){
			DataService.providerStateChange("return", ProviderStateAction.START, null, failureHandler);
		});
		
		$stopButton.click(function (){
			DataService.providerStateChange("return", ProviderStateAction.STOP, null, failureHandler);
		});
		
		$abortButton.click(function (){
			DataService.providerStateChange("return", ProviderStateAction.ABORT, null, failureHandler);
		});
		
		$unbindButton.click(function (){
			DataService.providerStateChange("return", ProviderStateAction.UNBIND, null, failureHandler);
		});
		
		if (serverState != null) {
			setState(serverState[StateObjectProperty.SLE_INTERFACE_RETURN_SERVICE_STATE]);
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
	 * Handle state change messages coming through the websocket connection.
	 * @param {Object} message - message object with connection state properties
	 * @private
	 */
	function handleWebsocketStateChangeEvent(message) {
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
	 * @param {Object} sleReturnState - object which contains state properties
	 * @private
	 */
	function setState(sleReturnState) {
		
		if (sleReturnState == null) {
			return;
		}
		
		setConnectionState(sleReturnState[SleStateProperty.STATE]);

		if (sleReturnState[SleStateProperty.BOUND_PROFILE] === "") {
			$bindButton.addClass("disabled");
		}		
		
		setStateTime(sleReturnState[SleStateProperty.STATE_CHANGE_TIME] || "...");
		setLastMessageTime(sleReturnState[SleStateProperty.LAST_TRANSFER_DATE_TIME] || "...");
		setLastMessageCount(sleReturnState[SleStateProperty.TRANSFER_DATA_COUNT] || "...");
		setConnectionNumber(sleReturnState[SleStateProperty.CURRENT_CONNECTION_NUMBER] || "...");
		setDeliveryMode(sleReturnState[SleStateProperty.DELIVERY_MODE] || "...");
	}
	
	/**
	 * Set connection state based on the passed in state object
	 * @param {Object} state - connection state object
	 * @private
	 */
	function setConnectionState(state) {
		switch(state) {
		case ProviderState.UNBOUND:
			unbindStateChange();
			break;
		case ProviderState.READY:
			bindStateChange();
			break;	
		case ProviderState.ACTIVE:
			startStateChange();
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
		
		// If the server error is a controlled condition and cannot cause the GUI to get
		// out of sync from the server then skip the reload callback. This specific error  
		// is caused by the user initiating START on the Return provider connection before
		// connecting AMPCS Downlink, therefore its a controlled error and does not cause 
		// an out of sync condition.
		if (response.responseJSON.description != undefined 
			&& response.responseJSON.description.includes("START is not allowed because chill interface downlink seems to be disconnected")) {
			setReloadCallback = false;
		}
		
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
	 * Get the state change time.
	 * @returns {String} time - state change time
	 * @private
	 */
	function getStateTime() {
		return $returnProviderStateTime.text();
	}

	/**
	 * Set the state change time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setStateTime(time) {
		$returnProviderStateTime.text(time);
	}

	/**
	 * Get the last message time.
	 * @returns {String} time - last message time
	 * @private
	 */
	function getLastMessageTime() {
		return $returnProviderLastMessageTime.text();
	}
	
	/**
	 * Set the last message time.
	 * @param {String} time - the time to set
	 * @private
	 */
	function setLastMessageTime(time) {
		$returnProviderLastMessageTime.text(time);
	}
	
	/**
	 * Get the last message count.
	 * @returns {String} count - last message count
	 * @private
	 */	
	function getLastMessageCount() {
		return $returnProviderDataMessageCnt.text();
	}

	/**
	 * Set the last message count.
	 * @param {String} time - the count to set
	 * @private
	 */
	function setLastMessageCount(count) {
		$returnProviderDataMessageCnt.text(count);
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
	 * Update UI elements to reflect bound/ready state
	 * @private
	 */
	function bindStateChange() {
		
		EventBus.publish(EventTopic.RETURN_PROVIDER_BIND, {});
		
		$returnProviderStateLabelText.text("Ready");

		$returnProviderStateLabel.addClass("blue");
		$bindButton.addClass("disabled");
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
		
		EventBus.publish(EventTopic.RETURN_PROVIDER_UNBIND, {});
		$returnProviderStateLabelText.text("Unbound");

		$returnProviderStateLabel.removeClass("blue");
		$returnProviderStateLabel.removeClass("green");
		$returnProviderStateLabel.removeClass("red");

		$bindButton.removeClass("disabled");
		$startButton.addClass("disabled");
		$stopButton.addClass("disabled");
		$unbindButton.addClass("disabled");
		$abortButton.addClass("disabled");
	}

	/**
	 * Update UI elements to reflect active state when start is initiated
	 * on the connection.
	 * @private
	 */
	function startStateChange() {
		$returnProviderStateLabelText.text("Active");

		$returnProviderStateLabel.removeClass("blue");
		$returnProviderStateLabel.addClass("green");

		$bindButton.addClass("disabled");
		$startButton.addClass("disabled");
		$stopButton.removeClass("disabled");
		$unbindButton.addClass("disabled");
		$abortButton.removeClass("disabled");
	}

	/**
	 * Update UI elements to reflect ready state when stop is initiated
	 * on the connection.
	 * @private
	 */
	function stopStateChange() {
		$returnProviderStateLabelText.text("Ready");

		$returnProviderStateLabel.removeClass("green");
		$returnProviderStateLabel.addClass("blue");

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