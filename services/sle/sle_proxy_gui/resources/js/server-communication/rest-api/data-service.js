/**
 * Server REST API wrapper module. All REST resources are accessed through 
 * this module.
 */
var DataService = (function() {

	var basePath;
	var chillInterface = "/chill-interface"
	var sleInterface = "/sle-interface";
	var chillInterface = "/chill-interface";
	var profilesResourcePath = "/sle-interface/profiles";
	
	/**
	 * Initialize the module with config data
	 * @public
	 */
	function init() {
		basePath = Config.getRestApiPath();
	}
	
	
	/**
	 * Get data from the 'state' resource
	 * Submit GET request to resource /sle-proxy/state 
	 * @returns {Object} stateObj - state object as returned by the server
	 * @public
	 */
	function getServerState() {
		var fullPath = basePath + "/state";
		var stateObj;

		submitRequest({
			successCallback : function(data) {
				stateObj = data;
			},
			errorCallback : function(data, status) {},
			async : false,
			apiURL : fullPath
		});

		return stateObj;
	}
	
	/**
	 * Get data from the 'messages' resource
	 * Submit GET request to resource /sle-proxy/messages
	 * @returns {Array} messageList - list of log messages
	 * @public
	 */
	function getMessages() {
		var fullPath = basePath + "/messages";
		var messageList = [];

		submitRequest({
			successCallback : function(data) {
				messageList = data;
			},
			errorCallback : function(data, status) {},
			async : false,
			apiURL : fullPath
		});

		return messageList;
	}
	
	/**
	 * Get chill configuration
	 * Submit GET request to resource /sle-proxy/chill-interface/config
	 * @returns {Object} config - chill config object
	 * @public
	 */
	function getChillConfig() {
		var fullPath = basePath + chillInterface + "/config";
		var config;

		submitRequest({
			successCallback : function(data) {
				config = data;
			},
			errorCallback : function(data, status) {},
			async : false,
			apiURL : fullPath
		});

		return config;
	}
	
	/**
	 * Get SLE Provider profiles
	 * Submit GET request to resource /sle-proxy/sle-interface/profiles
	 * @returns {Array} profileList - list of provider profile objects
	 * @public
	 */
	function getSleProviders() {
		var fullPath = basePath + profilesResourcePath;
		var profileList = [];
		
		submitRequest({
			successCallback : function(data) {
				
				data.forEach(function(profile, i, array) {

					for (var prop in SharedProfileProperty) {
						if (SharedProfileProperty.hasOwnProperty(prop)) {
							var propName = SharedProfileProperty[prop];
							if (!(propName in profile)) {
								profile[propName] = "";
							}
						}
					}

					for (var prop in ReturnProfileProperty) {
						if (ReturnProfileProperty.hasOwnProperty(prop)) {
							var propName = ReturnProfileProperty[prop];
							if (!(propName in profile)) {
								profile[propName] = "";
							}
						}
					}

					if (profile[SharedProfileProperty.INTERFACE_TYPE].includes("RETURN")) {
						profile[SharedProfileProperty.PROVIDER_TYPE] = "RETURN";
						profile[ReturnProfileProperty.RETURN_TYPE] = profile[SharedProfileProperty.INTERFACE_TYPE];

					}

					if (profile[SharedProfileProperty.INTERFACE_TYPE].includes("FORWARD")) {
						profile[SharedProfileProperty.PROVIDER_TYPE] = "FORWARD";
						profile[ReturnProfileProperty.RETURN_TYPE] = "";

					}

					profileList.push(profile);
				});
				
			},
			errorCallback : function(data, status) {
				var profiles = JSON.parse(data);
			},
			async : false,
			apiURL : fullPath
		});
		
		return profileList;
	}
	
	/**
	 * Update SLE Provider profile
	 * Submit POST request to resource /sle-proxy/sle-interface/profile/{profile-name}
	 * which JSON body.
	 * @param {String} profileName - name of profile to update
	 * @param {Object} profileData - object containing properties being updated
	 * @param {Callback} function - callback function when request succeeds
	 */
	function updateProfile(profileName, profileData, successResponseHandler) {
		var fullPath = basePath + profilesResourcePath + "/" + profileName;

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			contentType : "application/json",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			format : 'json',
			data : JSON.stringify(profileData),
			successCallback : function(data, status) {
				successResponseHandler();
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * Create New Profile
	 * Submit PUT request to resource /sle-proxy/sle-interface/profile/{profile-name}
	 * which JSON body.
	 * @param {String} profileName - name of profile to update
	 * @param {Object} profileData - object containing profile properties
	 * @param {Callback} function - callback function when request succeeds 
	 * @public
	 */
	function createNewProfile(profileName, profileData, successResponseHandler) {
		var fullPath = basePath + profilesResourcePath + "/" + profileName;

		submitRequest({
			apiURL : fullPath,
			method : "PUT",
			contentType : "application/json",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			format : 'json',
			data : JSON.stringify(profileData),
			successCallback : function(data, status) {
				successResponseHandler();
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * Update Mod Index and Bitrate
	 * @param {String} modIndex - mod index value
	 * @param {String} bitrate - bitrate value
	 * @param {Callback} function - callback function for failed response
	 * @public
	 */
	function updateModIndexBitrate(modIndex, bitrate, errorHandler) {
		// /sle-proxy/sle-interface/forward/action/throw?change-rate={value}&change-index={value}
		var fullPath = basePath + sleInterface
			+ "/forward/action/throw?change-rate=" + bitrate
			+ "&change-index=" + modIndex;

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			successCallback : function(data, status) {
				console.log(status);
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * Delete profile using the profile name.
	 * @param {String} profileName - name of the profile being deleted
	 * @public
	 */
	function deleteProfile(profileName) {
		var fullPath = basePath + profilesResourcePath + "/" + profileName;
		submitRequest({
			apiURL : fullPath,
			method : "DELETE",
			async : false,
			successCallback : function(data, status) {
				console.log(status);
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				}
			}
		});
	}
	
	/**
	 * SLE Provider State Change
	 * @param {String} providerType - forward or return
	 * @param {ProviderStateAction} action - state change action
	 * @param {String} profileName - name of the profile used during BIND
	 * @param {Callback} successHandler - callback when request succeeds
	 * @param {Callback} errorHandler - callback when request fails
	 * @public
	 */
	function providerStateChange(providerType, action, profileName, errorHandler) {
		var fullPath;

		if (action === ProviderStateAction.BIND) {
			fullPath = basePath + sleInterface + "/" + providerType + "/action/" + action + "?profile=" + profileName;
		} else {
			fullPath = basePath + sleInterface + "/" + providerType + "/action/" + action;
		}

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			successCallback : function(data, status) {
				// do nothing
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}

	/**
	 * Chill State Change
	 * Submit POST requests:
	 *   Uplink -   /sle-proxy/chill-interface/uplink/action/{enable|disable}
	 *   Downlink - /sle-proxy/chill-interface/downlink/action/{connect|disconnect}
	 * @param {String} uplinkDownlink - uplink or downlink
	 * @param {String} action - state change action
	 * @param {Callback} errorHandler - callback when request fails
	 * @public
	 */
	function chillStateChange(uplinkDownlink, action, errorHandler) {
		var fullPath;

		fullPath = basePath + chillInterface + "/" + uplinkDownlink + "/action/" + action;

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			successCallback : function(data, status) {
				// do nothing
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * SLE Forward Provider Throw command
	 * Submit a POST request to /sle-proxy/sle-interface/forward/action/throw?{set-command-mod|set-range-mod}={on|off}
	 * @param {Map} commandMap
	 * @param {Callback} errorHandler - callback when request fails
	 * @public
	 */
	function forwardProviderThrow(commandMap, errorHandler) {
		var fullPath;
		var firstCommand = true;
		fullPath = basePath + sleInterface + "/forward/action/throw?";

		for (var commandName in commandMap) {
			if (commandMap.hasOwnProperty(commandName)) {
				if (firstCommand) {
					fullPath = fullPath + commandName + "=" + commandMap[commandName];
					firstCommand = false;
				} else {
					fullPath = fullPath + "&" + commandName + "=" + commandMap[commandName];
				}
			}
		}

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			successCallback : function(data, status) {
				// do nothing
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * Update Downlink configuration
	 * Submit a POST request to /sle-proxy/chill-interface/config
	 * @param {Object} downlinkConfigData - object containing downlink properties
	 * @public
	 */
	function updateDownlinkConfig(downlinkConfigData, successResponseHandler) {
		
		var fullPath = basePath + chillInterface + "/config";

		submitRequest({
			apiURL : fullPath,
			method : "POST",
			contentType : "application/json",
			headers : {
				'Content-Type' : 'application/json',
			},
			async : false,
			format : 'json',
			data : JSON.stringify(downlinkConfigData),
			successCallback : function(data, status) {
				successResponseHandler();
			},
			errorCallback : function(data, status) {
				if (data != null && data.status === 403) {
					showNotAuthorizedMessage();
				} else {
					errorHandler(data);
				}
			}
		});
	}
	
	/**
	 * Show Not Authorized Error message.
	 * Users that are part of the READ only LDAP group are not allowed 
	 * to make any changes to the system and the server responds with a 403 error
	 * @private
	 */
	function showNotAuthorizedMessage() {
		ErrorModal.setHeader("Not Authorized");
		ErrorModal.setMessage("User has READ only privileges and cannot make any changes to the system.");
		ErrorModal.show();
	}
	
	/**
	 * Submit AJAX request with specified options
	 * @param {Object} options - object containing AJAX request options
	 * @private
	 */
	function submitRequest(options) {
		$.ajax({
			url : options.apiURL,
			headers : options.headers,
			contentType : options.contentType,
			dataType : options.format,
			async : options.async,
			data : options.data,
			method : options.method,
			jsonpCallback : options.jsonpCallback,
			success : options.successCallback,
			error : options.errorCallback,
			timeout : options.timeout
		});
	}
	
	// Expose public methods
	return {
		init: init,
		getChillConfig : getChillConfig,
		getSleProviders : getSleProviders,
		createNewProfile : createNewProfile,
		updateProfile : updateProfile,
		deleteProfile : deleteProfile,
		providerStateChange : providerStateChange,
		forwardProviderThrow : forwardProviderThrow,
		updateDownlinkConfig : updateDownlinkConfig,
		chillStateChange : chillStateChange,
		getServerState : getServerState,
		getMessages : getMessages,
		updateModIndexBitrate : updateModIndexBitrate
	}

})();