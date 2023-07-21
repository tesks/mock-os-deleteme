/**
 * This module handles all functionality associated with the
 * SLE Providers configuration component on the Monitor and Control
 * interface.
 */
var ProviderConfig = (function() {

	var $returnProviderDropdown;
	var $forwardProviderDropdown;

	var $returnProviderName;
	var $returnProviderHosts;
	var $returnProviderReturnType;
	var $returnProviderServiceMode;
	var $returnProviderInstanceId;
	var $returnProviderAuthMode;
	var $returnProviderUserAuthMode;
	var $returnProviderUsername;
	var $returnProviderStartTime;
	var $returnProviderStopTime;
	var $returnProviderFrameVersion;
	var $returnProviderFrameQuality;
	var $returnProviderScId;
	var $returnProviderVcId;
	var $returnProviderDropdownInnerInput;
	
	var $forwardProviderName;
	var $forwardProviderHosts;
	var $forwardProviderInstanceId;
	var $forwardProviderAuthMode;
	var $forwardProviderUserAuthMode;
	var $forwardProviderUsername;
	var $forwardProviderDropdownInnerInput;
	
	var settingInitialState = true;
	
	var providerMap = {};
	
	/**
	 * Initialize the component
	 * @public
	 */
	function init() {
		
		// Don't show the Return All and Return Channel specific
		// fields during initial application load
		hideReturnAllFields();
		hideReturnChannelFields();
		
		// JQuery selectors for the Return provider fields.
		$returnProviderName = $("#return-provider-name");
		$returnProviderHosts = $("#return-provider-hosts");
		$returnProviderReturnType = $("#return-provider-return-type");
		$returnProviderServiceMode = $("#return-provider-service-mode");
		$returnProviderInstanceId = $("#return-provider-instance-id");
		$returnProviderAuthMode = $("#return-provider-auth-mode");
		$returnProviderUserAuthMode = $("#return-provider-user-auth-mode");
		$returnProviderUsername = $("#return-provider-username");
		$returnProviderStartTime = $("#return-provider-start-time");
		$returnProviderStopTime = $("#return-provider-stop-time");
		$returnProviderFrameVersion = $("#return-provider-frame-version");
		$returnProviderFrameQuality = $("#return-provider-frame-quality");
		$returnProviderScId = $("#return-provider-sc-id");
		$returnProviderVcId = $("#return-provider-vc-id");

		$returnProviderDropdown = $("#return-provider-dropdown").dropdown({
			fireOnInit : true,
			onChange : function(value, text, $selectedItem) {
				var selectedProvider = providerMap[value];
				if (selectedProvider == null) { 
					return;
				}
				
				// Populate the Return provider profile
				// properties based on the selected profile
				setReturnProviderFields(value);
				
				// Notify the other components that a Return provider
				// profile has been selected
				EventBus.publish(EventTopic.RETURN_PROVIDER_SELECTION, {});
			}
		});
		
		// Need to clear the input field values at load time to make sure 
		// browser caching does not cause undesirable behavior when the user
		// selects the same profile name that was previously used before 
		// a browser refresh event. Seems to happen in Firefox but not Chrome. 
		$returnProviderDropdownInnerInput = $("#return-provider-dropdown-inner-input");
		$returnProviderDropdownInnerInput.val("");
		$forwardProviderDropdownInnerInput = $("#forward-provider-dropdown-inner-input");
		$forwardProviderDropdownInnerInput.val("");
		
		// JQuery selectors for the Forward provider fields.
		$forwardProviderName = $("#forward-provider-name");
		$forwardProviderHosts = $("#forward-provider-hosts");
		$forwardProviderInstanceId = $("#forward-provider-instance-id");
		$forwardProviderAuthMode = $("#forward-provider-auth-mode");
		$forwardProviderUserAuthMode = $("#forward-provider-user-auth-mode");
		$forwardProviderUsername = $("#forward-provider-username");

		$forwardProviderDropdown = $("#forward-provider-dropdown").dropdown({
			fireOnInit : true,
			onChange : function(value, text, $selectedItem) {
				var selectedProvider = providerMap[value];
				if (selectedProvider == null) {
					return;
				}
				
				// Populate the Forward provider profile
				// properties based on the selected profile
				setForwardProviderFields(value);
				
				// Notify the other components that a Forward provider
				// profile has been selected
				EventBus.publish(EventTopic.FORWARD_PROVIDER_SELECTION, {});
			}
		});
		
		// Clear all fields during initialization
		clearReturnProviderFields();
		clearForwardProviderFields();
		
		setupProviderMap();
		
		// Populate the Return and Forward providers dropdowns
		// with the available profile information for each.
		setupReturnProviderDropdown();
		setupForwardProviderDropdown();

		// Subscribe to the Forward provider BIND event
		// Need to disable the dropdown when in BOUND state
		EventBus.subscribe(EventTopic.FORWARD_PROVIDER_BIND, function() {
			disableForwardProviderDropdown();
		});
		
		// Subscribe to Forward provider UNBIND event
		// Need to enable the dropdown when in UNBOUND state
		EventBus.subscribe(EventTopic.FORWARD_PROVIDER_UNBIND, function() {
			enableForwardProviderDropdown();
		});		
		
		// Subscribe to the Return provider BIND event
		// Need to disable the dropdown when in BOUND state		
		EventBus.subscribe(EventTopic.RETURN_PROVIDER_BIND, function() {
			disableReturnProviderDropdown();
		});

		// Subscribe to Return provider UNBIND event
		// Need to enable the dropdown when in UNBOUND state
		EventBus.subscribe(EventTopic.RETURN_PROVIDER_UNBIND, function() {
			enableReturnProviderDropdown();
		});
		
		// Subscribe to BIND events generated from Websocket messages
		// These events are used to keep all instances of the GUI
		// applications in sync.
		EventBus.subscribe(EventTopic.WEBSOCKET_RETURN_PROVIDER_BIND, function(message) {
			
			if (message[WebsocketMesssageProperty.ACTION] === ProviderStateAction.BIND) {
				setSelectedReturnProvider(message[WebsocketMesssageProperty.PROFILE_NAME]);
				disableReturnProviderDropdown();
			}
			
			if (message[WebsocketMesssageProperty.ACTION] === ProviderStateAction.UNBIND) {
				enableReturnProviderDropdown();
			}

		});
		
		// Subscribe to UNBIND events generated from Websocket messages
		// These events are used to keep all instances of the GUI
		// applications in sync.		
		EventBus.subscribe(EventTopic.WEBSOCKET_FORWARD_PROVIDER_BIND, function(message) {
			
			if (message[WebsocketMesssageProperty.ACTION] === ProviderStateAction.BIND) {
				setSelectedForwardProvider(message[WebsocketMesssageProperty.PROFILE_NAME]);
				disableForwardProviderDropdown();
			}
			
			if (message[WebsocketMesssageProperty.ACTION] === ProviderStateAction.UNBIND) {
				enableForwardProviderDropdown();
			}			
			
		});

		// Subscribe to SLE provider profile create events
		// Need to update the provider dropdowns with the newly created
		// profile information
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_CREATE, function(message) {
			setupProviderMap();
			setupReturnProviderDropdown();
			setupForwardProviderDropdown();
		});		
		
		// Subscribe to SLE provider profile update events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_UPDATE, function(message) {
			var updatedProfile = message[WebsocketMesssageProperty.PROFILE];
			
			setupProviderMap();
			setupReturnProviderDropdown();
			setupForwardProviderDropdown();
			
			if (!isReturnProviderDropdownDisabled()) {
				if (getSelectedReturnProvider() === updatedProfile[WebsocketMesssageProperty.PROFILE_NAME]) {
					$returnProviderDropdown.dropdown("clear");
					$returnProviderDropdown.dropdown("refresh");
					setSelectedReturnProvider(updatedProfile[WebsocketMesssageProperty.PROFILE_NAME]);
				}
			}
			
			if (!isForwardProviderDropdownDisabled()) {
				if (getSelectedForwardProvider() === updatedProfile[WebsocketMesssageProperty.PROFILE_NAME]) {
					$forwardProviderDropdown.dropdown("clear");
					$forwardProviderDropdown.dropdown("refresh");
					setSelectedForwardProvider(updatedProfile[WebsocketMesssageProperty.PROFILE_NAME]);
				}
			}
		});
		
		// Subscribe to SLE provider profile delete events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_DELETE, function(message) {
			// If the dropdown value matches the deleted profile
			// and the state is UNBOUND then update the dropdown and 
			// associated fields
			setupProviderMap();
			setupReturnProviderDropdown();
			setupForwardProviderDropdown();
			
			if (!isReturnProviderDropdownDisabled()) {
				if (getSelectedReturnProvider() === message[WebsocketMesssageProperty.PROFILE_NAME]) {
					$returnProviderDropdown.dropdown("clear");
					$returnProviderDropdown.dropdown("refresh");
					clearReturnProviderFields();
				}
			}
			
			if (!isForwardProviderDropdownDisabled()) {
				if (getSelectedForwardProvider() === message[WebsocketMesssageProperty.PROFILE_NAME]) {
					$forwardProviderDropdown.dropdown("clear");
					$forwardProviderDropdown.dropdown("refresh");
					clearForwardProviderFields();
				}
			}
		});		
		
		setState(serverState);
	}
	
	/**
	 * Set SLE providers configuration state.
	 * @param {Object} stateObject - object which contains provider configuration
	 * state properties.
	 * @private
	 */
	function setState(stateObject) {
		if (stateObject == null) {
			return;
		} 
		
		var forwardState = stateObject[StateObjectProperty.SLE_INTERFACE_FORWARD_SERVICE_STATE];
		var returnState = stateObject[StateObjectProperty.SLE_INTERFACE_RETURN_SERVICE_STATE];
		
		if (forwardState[SleStateProperty.BOUND_PROFILE] !== "") {
			setSelectedForwardProvider(forwardState[SleStateProperty.BOUND_PROFILE]);
			setForwardProviderFields(forwardState[SleStateProperty.BOUND_PROFILE]);
			disableForwardProviderDropdown();
		} else {
			settingInitialState = false;
		}
		
		if (returnState[SleStateProperty.BOUND_PROFILE] !== "") {
			setSelectedReturnProvider(returnState[SleStateProperty.BOUND_PROFILE]);
			setReturnProviderFields(returnState[SleStateProperty.BOUND_PROFILE]);
			disableReturnProviderDropdown();
		} else {
			settingInitialState = false;
		}
	}
	
	/**
	 * Set Return Provider fields. Configures and populates the profile fields that are 
	 * applicable to the selected profile.
	 * @param {String} profileName - name of the provider profile which is currently selected
	 * @private
	 */
	function setReturnProviderFields(profileName) {
		var selectedProvider = providerMap[profileName];
		
		$returnProviderName.val(selectedProvider[SharedProfileProperty.PROVIDER_NAME]);
		$returnProviderHosts.val(selectedProvider[SharedProfileProperty.PROVIDER_HOSTS]);
		$returnProviderReturnType.val(selectedProvider[ReturnProfileProperty.RETURN_TYPE]);
		$returnProviderServiceMode.val(selectedProvider[SharedProfileProperty.SERVICE_MODE]);
		$returnProviderInstanceId.val(selectedProvider[SharedProfileProperty.SERVICE_INSTANCE_ID]);
		$returnProviderAuthMode.val(selectedProvider[SharedProfileProperty.PROVIDER_AUTHENTICATION_MODE]);
		$returnProviderUserAuthMode.val(selectedProvider[SharedProfileProperty.USER_AUTHENTICATION_MODE]);
		$returnProviderUsername.val(selectedProvider[SharedProfileProperty.USER_NAME]);
		$returnProviderStartTime.val(selectedProvider[ReturnProfileProperty.START_TIME]);
		$returnProviderStopTime.val(selectedProvider[ReturnProfileProperty.STOP_TIME]);
		$returnProviderFrameVersion.val(selectedProvider[ReturnProfileProperty.FRAME_VERSION]);
		$returnProviderFrameQuality.val(selectedProvider[ReturnProfileProperty.FRAME_QUALITY]);
		$returnProviderScId.val(selectedProvider[ReturnProfileProperty.SPACECRAFT_ID]);
		$returnProviderVcId.val(selectedProvider[ReturnProfileProperty.VIRTUAL_CHANNEL]);
		
		if (selectedProvider[ReturnProfileProperty.RETURN_TYPE] === ReturnType.RETURN_ALL) {
			showReturnAllFields();
			hideReturnChannelFields();
		}
		
		if (selectedProvider[ReturnProfileProperty.RETURN_TYPE] === ReturnType.RETURN_CHANNEL) {
			showReturnChannelFields()
			hideReturnAllFields();
		}
		settingInitialState = false;
	}
	
	/**
	 * Set Forward Provider fields. Populates the profile fields that is currently selected.
	 * @param {String} profileName - name of the provider profile which is currently selected
	 * @private
	 */
	function setForwardProviderFields(profileName) {
		var selectedProvider = providerMap[profileName];
		
		$forwardProviderName.val(selectedProvider[SharedProfileProperty.PROVIDER_NAME]);
		$forwardProviderHosts.val(selectedProvider[SharedProfileProperty.PROVIDER_HOSTS]);
		$forwardProviderInstanceId.val(selectedProvider[SharedProfileProperty.SERVICE_INSTANCE_ID]);
		$forwardProviderAuthMode.val(selectedProvider[SharedProfileProperty.PROVIDER_AUTHENTICATION_MODE]);
		$forwardProviderUserAuthMode.val(selectedProvider[SharedProfileProperty.USER_AUTHENTICATION_MODE]);
		$forwardProviderUsername.val(selectedProvider[SharedProfileProperty.USER_NAME]);		
	}
	
	/**
	 * Show Return Channel fields.
	 * @private
	 */
	function showReturnChannelFields() {
		$("[name=return-channel-config-field]").show();
	}

	/**
	 * Hide Return Channel fields.
	 * @private
	 */
	function hideReturnChannelFields() {
		$("[name=return-channel-config-field]").hide();
	}	
	
	/**
	 * Show Return All fields.
	 * @private
	 */
	function showReturnAllFields() {
		$("[name=return-all-config-field]").show();
	}
	
	/**
	 * Hide Return All fields.
	 * @private
	 */
	function hideReturnAllFields() {
		$("[name=return-all-config-field]").hide();
	}
	
	/**
	 * Set up provider profile map
	 * @private
	 */
	function setupProviderMap() {
		for (var i = 0; i < profileList.length; i++) {
			var providerObj = profileList[i];
			providerMap[providerObj[SharedProfileProperty.PROFILE_NAME]] = providerObj;
		}
	}
	
	/**
	 * Set up the Return provider dropdown. Populates the dropdown with all of the 
	 * Return provider profile names using the global profileList.
	 * @private
	 */
	function setupReturnProviderDropdown() {
		$returnProviderDropdown.find('.menu').empty();
		for (var i = 0; i < profileList.length; i++) {
			var providerObj = profileList[i];
			if (providerObj[SharedProfileProperty.PROVIDER_TYPE] === "RETURN") {
				$returnProviderDropdown.find('.menu').append($("<div></div>")
					.attr("class", "item")
					.attr("data-value", providerObj[SharedProfileProperty.PROFILE_NAME])
					.text(providerObj[SharedProfileProperty.PROFILE_NAME]));
			}
		}
		
		// Need to refresh the dropdown element so that all of the event handlers
		// are registered in the DOM after dynamically populating the dropdown.
		$returnProviderDropdown.dropdown('refresh');
	}

	/**
	 * Set up the Forward provider dropdown. Populates the dropdown with all of the 
	 * Forward provider profile names using the global profileList.
	 * @private
	 */
	function setupForwardProviderDropdown() {
		$forwardProviderDropdown.find('.menu').empty();
		for (var i = 0; i < profileList.length; i++) {
			var providerObj = profileList[i];
			if (providerObj[SharedProfileProperty.PROVIDER_TYPE] === "FORWARD") {
				$forwardProviderDropdown.find('.menu').append($("<div></div>")
					.attr("class", "item")
					.attr("data-value", providerObj[SharedProfileProperty.PROFILE_NAME])
					.text(providerObj[SharedProfileProperty.PROFILE_NAME]));
			}
		}
		
		// Need to refresh the dropdown element so that all of the event handlers
		// are registered in the DOM after dynamically populating the dropdown.
		$forwardProviderDropdown.dropdown('refresh');
	}
	
	/**
	 * Get selected Return provider profile name from the dropdown
	 * @returns {String} profileName - name of the selected profile
	 * @public
	 */
	function getSelectedReturnProvider() {
		return $returnProviderDropdown.dropdown('get value');
	}
	
	/** 
	 * Set selected Return provider profile name.
	 * @private
	 */
	function setSelectedReturnProvider(profileName) {
		if (profileName === "") {
			return;
		}
		$returnProviderDropdown.dropdown('set selected', profileName);
	}
	
	/**
	 * Get selected Forward provider profile name from the dropdown
	 * @returns {String} profileName - name of the selected profile
	 * @public
	 */
	function getSelectedForwardProvider() {
		return $forwardProviderDropdown.dropdown('get value');
	}
	
	/** 
	 * Set selected Forward provider profile name.
	 * @private
	 */
	function setSelectedForwardProvider(profileName) {
		if (profileName === "") {
			return;
		}
		$forwardProviderDropdown.dropdown('set selected', profileName);
	}	
	
	/**
	 * Disable Forward provider dropdown. Dropdown is disabled when Forward
	 * connection is in BOUND state. 
	 * @private
	 */
	function disableForwardProviderDropdown() {
		$forwardProviderDropdown.addClass("disabled");
	}

	/**
	 * Enable Forward provider dropdown. Dropdown needs to be enabled when Forward
	 * connection is in UNBOUND state. 
	 * @private
	 */
	function enableForwardProviderDropdown() {
		$forwardProviderDropdown.removeClass("disabled");
	}	
	
	/**
	 * Disable Return provider dropdown. Dropdown is disabled when Return
	 * connection is in BOUND state. 
	 * @private
	 */
	function disableReturnProviderDropdown() {
		$returnProviderDropdown.addClass("disabled");
	}
	
	/**
	 * Enable Return provider dropdown. Dropdown needs to be enabled when Return
	 * connection is in UNBOUND state. 
	 * @private
	 */
	function enableReturnProviderDropdown() {
		$returnProviderDropdown.removeClass("disabled");
	}
	
	/**
	 * Check if Forward provider dropdown is disabled.
	 * @returns {Boolean}
	 * @private
	 */
	function isForwardProviderDropdownDisabled() {
		if ($forwardProviderDropdown.hasClass("disabled")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check if Return provider dropdown is disabled.
	 * @returns {Boolean}
	 * @private
	 */
	function isReturnProviderDropdownDisabled() {
		if ($returnProviderDropdown.hasClass("disabled")) {
			return true;
		} else {
			return false;
		}
	}	
	
	/**
	 * Clear Return provider fields.
	 * @private
	 */
	function clearReturnProviderFields() {
		$returnProviderName.val("");
		$returnProviderHosts.val("");
		$returnProviderReturnType.val("");
		$returnProviderServiceMode.val("");
		$returnProviderInstanceId.val("");
		$returnProviderAuthMode.val("");
		$returnProviderUserAuthMode.val("");
		$returnProviderUsername.val("");
		$returnProviderStartTime.val("");
		$returnProviderStopTime.val("");
		$returnProviderFrameVersion.val("");
		$returnProviderFrameQuality.val("");
		$returnProviderScId.val("");
		$returnProviderVcId.val("");
	}
	
	/**
	 * Clear Forward provider fields.
	 * @private
	 */
	function clearForwardProviderFields() {
		$forwardProviderName.val("");
		$forwardProviderHosts.val("");
		$forwardProviderInstanceId.val("");
		$forwardProviderAuthMode.val("");
		$forwardProviderUserAuthMode.val("");
		$forwardProviderUsername.val("");		
	}
	
	return {
		init : init,
		getSelectedReturnProvider: getSelectedReturnProvider,
		getSelectedForwardProvider: getSelectedForwardProvider
	}

})();