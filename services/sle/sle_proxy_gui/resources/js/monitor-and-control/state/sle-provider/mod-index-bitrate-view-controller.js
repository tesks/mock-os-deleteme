/**
 * This module encapsulates all functionality associated with the
 * SLE Forward Provider Mod Index/Bitrate component 
 * on the Monitor and Control interface.
 */
var ModIndexBitrateViewController = (function() {
	
	var $modIndexInput;
	var $modIndexLabel;
	var $bitrateDropdown;
	var $bitrateLabel;
	
	var $resetButton;
	var $applyButton;
	
	var allowedBitrateValues;
	var modIndexMin;
	var modIndexMax;
	
	/**
	 * SLE Forward Config enum
	 * @enum
	 */
	var SleForwardConfig = {
		ALLOWABLE_MODINDEX_MIN : "allowable_modindex_min",
		ALLOWABLE_MODINDEX_MAX : "allowable_modindex_max",
		ALLOWABLE_BITRATES : "allowable_bitrates",
	}
	
	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		$modIndexLabel = $("#mod-index-label");
		$modIndexInput = $("#mod-index-input");
		
		// Restrict the Port field input to only numeric characters
		$modIndexInput.keydown(allowNumericInputOnly);
		
		$bitrateDropdown = $("#bitrate-dropdown").dropdown();
		$bitrateLabel = $("#bitrate-label");
		
		$resetButton = $("#mod-index-bitrate-reset-button");
		$applyButton = $("#mod-index-bitrate-apply-button");
		
		$applyButton.click(applyButtonHandler);
		
		$resetButton.click(function () {
			clearNewValues();
		});
		
		// Subscribe to Forward Provider Throw events
		EventBus.subscribe(EventTopic.WEBSOCKET_FORWARD_PROVIDER_THROW_EVENT, handleWebsocketThrowEvent);
		
		// Clear the New Mod Index and Bitrate fields
		clearNewValues();
		
		if (serverState != null) {
			setState(serverState);
		}
	}
	
	/**
	 * Set the init state of the component during application load
	 * @param {Object} stateObject - object which contains state properties
	 * @private
	 */
	function setState(stateObject) {
		var forwardServiceConfig = stateObject[StateObjectProperty.SLE_INTERFACE_FORWARD_SERVICE_CONFIGURATION];
		allowedBitrateValues = forwardServiceConfig[SleForwardConfig.ALLOWABLE_BITRATES];
		modIndexMin = forwardServiceConfig[SleForwardConfig.ALLOWABLE_MODINDEX_MIN];
		modIndexMax = forwardServiceConfig[SleForwardConfig.ALLOWABLE_MODINDEX_MAX];
		setupBitrateDropdown();
	}
	
	/**
	 * Handle throw event messages coming through the websocket connection.
	 * @param {Object} message - message object with throw event properties
	 * @private
	 */
	function handleWebsocketThrowEvent(message) {
		if (message.hasOwnProperty("bitrate")) {
			setBitrateLabel(message["bitrate"]);
		}
		
		if (message.hasOwnProperty("mod_index")) {
			setModIndexLabel(message["mod_index"]);	
		}
		clearNewValues();
	}
	
	/**
	 * Clear/reset the New values
	 * @private 
	 */
	function clearNewValues() {
		setBitrateDropdown("");
		setModIndexInput("");
	}
	
	/**
	 * Handle Apply button click event
	 * @private
	 */
	function applyButtonHandler() {
		
		// User has clicked the Apply button without setting any new values.
		// There are a few ways of handling this but this approach seems simplest,
		// compared to enabling/disabling the Apply button
		if (getModIndexInput() === "" && getBitrateDropdown() === "") {
			ErrorModal.setMessage("Neither Mod Index nor Bitrate has a new value set!");
			ErrorModal.show();
			return;
		}
		
		var submissionModIndex = getSubmissionModIndex();
		var submissionBitrate = getSubmissionBitrate();
		
		if (submissionModIndex === "" || submissionBitrate === "") {
			var message;
			if (submissionModIndex === "") {
				message = " - Mod Index does not have any set value</br></br>";
			}
			
			if (submissionBitrate === "") {
				if (message != null) {
					message += " - Bitrate does not have any set value";
				} else {
					message = " - Bitrate does not have any set value";
				}
			}	
			
			ErrorModal.setMessage(message);
			ErrorModal.show();
			return;
		}
		
		// Mod Index is an input field, need to validate the user input 
		// to make sure the value is within the allowed range.
		if (isValidModIndex(parseInt(submissionModIndex))) {
			DataService.updateModIndexBitrate(submissionModIndex, submissionBitrate, failureHandler);		
		}
	}
	
	/**
	 * Get the Mod Index value that will be submitted to the server.
	 * If the New value is set, return the New value, otherwise return the 
	 * Current value. The server resource requires the Mod Index and Bitrate
	 * to be submitted together.
	 * @private
	 */
	function getSubmissionModIndex() {
		var currModIndex = getModIndexLabel();
		var newModIndex = getModIndexInput();
		
		if (newModIndex != null && newModIndex !== "") {
			return newModIndex;
		} else {
			return currModIndex;
		}
	}
	
	/**
	 * Get the Bitrate value that will be submitted to the server.
	 * If the New value is set, return the New value, otherwise return the 
	 * Current value. The server resource requires the Mod Index and Bitrate
	 * to be submitted together.
	 * @private
	 */
	function getSubmissionBitrate() {
		var currBitrate = getBitrateLabel();
		var newBitrate = getBitrateDropdown();
		
		if (newBitrate != null && newBitrate !== "") {
			return newBitrate;
		} else {
			return currBitrate;
		}
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
	 * Check if Mod Index is valid/falls within the valid range. If not valid,
	 * show the error modal with message.
	 * @param {Integer} modIndex - integer value of the mod index
	 * @private
	 */
	function isValidModIndex(modIndex) {
		if (modIndex < modIndexMin || modIndex > modIndexMax) {

			ErrorModal.setMessage("New Mod Index '"
				+ modIndex + "' not within valid range: "
				+ modIndexMin + "-" + modIndexMax);
			ErrorModal.show();
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Disable the Apply button.
	 * @private
	 */
	function disableApplyButton() {
		$applyButton.addClass("disabled");
	}
	
	/**
	 * Enable the Apply button.
	 * @private
	 */
	function enableApplyButton() {
		$applyButton.removeClass("disabled");
	}
	
	/**
	 * Disable the Reset button
	 * @private
	 */
	function disableResetButton() {
		$resetButton.addClass("disabled");
	}
	
	/**
	 * Enable the Reset button
	 * @private
	 */
	function enableResetButton() {
		$resetButton.removeClass("disabled");
	}
	
	/**
	 * Set up the Bitrate dropdown.
	 * @private
	 */
	function setupBitrateDropdown() {
		$bitrateDropdown.find('.menu').empty();
		$bitrateDropdown.find('.menu').append($("<div></div>")
				.attr("class", "item")
				.attr("data-value", "")
				.text(""));		
		for (var i = 0; i < allowedBitrateValues.length; i++) {
			var bitrate = allowedBitrateValues[i];

			$bitrateDropdown.find('.menu').append($("<div></div>")
				.attr("class", "item")
				.attr("data-value", bitrate)
				.text(bitrate));
		}
	}	
	
	/**
	 * Set the Mod Index label which show the user the Current value
	 * @param {String} modIndex - modIndex value to set
	 * @private
	 */
	function setModIndexLabel(modIndex) {
		$modIndexLabel.text(modIndex);
	}
	
	/**
	 * Get the Mod Index value
	 * @returns modIndex
	 * @private
	 */
	function getModIndexLabel() {
		return $modIndexLabel.text();
	}	
	
	/**
	 * Set the Mod Index input field value which is the New value
	 * @param {String} modIndex - modIndex value to set
	 * @private
	 */
	function setModIndexInput(modIndex) {
		$modIndexInput.val(modIndex);
	}
	
	/**
	 * Get the Mod Index value from the input field
	 * @returns modIndex
	 * @private
	 */
	function getModIndexInput() {
		return $modIndexInput.val();
	}
	
	/**
	 * Set the selected Bitrate dropdown value
	 * @param {String} bitrate - bitrate value to set
	 * @private
	 */
	function setBitrateDropdown(bitrate) {
		$bitrateDropdown.dropdown("set selected", bitrate);
	}
	
	/**
	 * Get the selected Bitrate value
	 * @returns {String} bitrate
	 */
	function getBitrateDropdown() {
		return $bitrateDropdown.dropdown("get value");
	}

	/**
	 * Set the Bitrate label, Current value
	 * @param {String} bitrate - bitrate to set
	 */
	function setBitrateLabel(bitrate) {
		$bitrateLabel.text(bitrate);
	}
	
	/**
	 * Get the Current Bitrate value
	 * @returns {String} bitrate 
	 */
	function getBitrateLabel() {
		return $bitrateLabel.text();
	}
	
	// public methods
	return {
		init: init,
	}
	
})();