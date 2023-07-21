/**
 * Profile Manager module which encapsulates all functionality 
 * associated with Profile Editor Modal opened from within the 
 * Manage Service Providers interface.
 */
var ProfileManager = (function() {

	var $addEditModal;

	var $profileName;
	var $providerName;
	var $profileHosts;
	var $instanceID;

	var $returnTypeDropdown;
	var $serviceModeDropdown;
	var $userAuthModeDropdown;
	var $userName;
	var $userPassword;
	var $userPasswordConfirmation;
	var $providerAuthMode;
	var $providerAuthModeDropdown;
	var $providerPassword;
	var $providerPasswordConfirmation;

	var $cancelButton;
	var $submitButton;

	var $modalDimmer;

	var $cancelConfirmModal;
	var $cancelConfirmModalNoButton;
	var $cancelConfirmModalYesButton;

	var userAction;
	var currentProviderType;
	var profileBeforeEdit;

	var $returnChannelFields;
	var $returnAllFields;

	var $startTime;
	var $stopTime;
	var $scId;
	var $frameVersion;
	var $vcId;
	var $frameQualityDropdown;

	var newProfile = false;
	var validationPassed = true;
	
	/**
	 * Enum for User Actions
	 * @enum
	 */
	var UserAction = {
		ADD : "ADD",
		EDIT : "EDIT"
	}
	
	/**
	 * Enum for SLE Provider Type
	 * @enum
	 */
	var ProviderType = {
		RETURN : "RETURN",
		FORWARD : "FORWARD"
	}

	/**
	 * Enum for Frame Quality
	 * @enum
	 */
	var FrameQuality = {
		GOOD : "GOOD",
		ERRED : "ERRED",
		ALL : "ALL"
	}
	
	/**
	 * Enum for Authentication Mode
	 * @enum
	 */ 
	var AuthMode = {
		NONE : "NONE",
		BIND : "BIND",
		ALL : "ALL"
	}
	
	/**
	 * Initialize the module
	 * @public
	 */
	function init() {
		$profileName = $("#profile-name");
		$providerName = $("#provider-name");
		$profileHosts = $("#hosts");

		$instanceID = $("#instance-id");

		$frameQualityDropdown = $("#frame-quality-dropdown").dropdown();
		$serviceModeDropdown = $("#service-mode-dropdown").dropdown();

		$returnChannelFields = $("#return-channel-fields");
		$returnAllFields = $("#return-all-fields");

		$returnChannelFields.hide();
		$returnAllFields.hide();

		$returnTypeDropdown = $("#return-type-dropdown").dropdown({
			action : 'activate',
			onChange : function(value, text, $selectedItem) {
				
				// Show the appropriate fields based on the Return Type
				if (value === ReturnType.RETURN_CHANNEL) {
					$returnAllFields.hide();
					$returnChannelFields.show();
				} else if (value === ReturnType.RETURN_ALL) {
					$returnChannelFields.hide();
					$returnAllFields.show();
				}
			}
		});

		$userAuthModeDropdown = $("#user-auth-mode-dropdown").dropdown();

		$userName = $("#user-name");
		$userPassword = $("#user-password");
		$userPasswordConfirmation = $("#user-password-confirmation");

		$providerAuthMode = $("#provider-auth-mode");
		$providerAuthModeDropdown = $("#provider-auth-mode-dropdown").dropdown();
		$providerPassword = $("#provider-password");
		$providerPasswordConfirmation = $("#provider-password-confirmation");

		$startTime = $("#start-time");
		$stopTime = $("#stop-time");
		$scId = $("#sc-id");
		
		// Restrict the Spacecraft Id field input to only 
		// numeric characters
		$scId.keydown(allowNumericInputOnly);

		$frameVersion = $("#frame-version");
		
		// Restrict the Frame Version field input to only 
		// numeric characters
		$frameVersion.keydown(allowNumericInputOnly);

		$vcId = $("#vc-id");
		
		// Restrict the VC Id field input to only 
		// numeric characters
		$vcId.keydown(allowNumericInputOnly);

		$frameQualityDropdown = $("#frame-quality-dropdown").dropdown();

		$cancelButton = $("#provider-modal-cancel-button");
		$submitButton = $("#provider-modal-submit-button");

		$submitButton.click(submitButtonHandler);
		$cancelButton.click(cancelButtonHandler);

		// Add/Edit Modal
		$addEditModal = $("#provider-add-edit-modal").modal({
			closable : false,
			allowMultiple : true
		});
		
		// Cancel Confirmation Modal
		$cancelConfirmModal = $("#cancel-confirm-modal").modal({
			closable : false,
			allowMultiple : true
		});
		
		// Dimmer which overlays the Add/Edit Modal
		$modalDimmer = $("#provider-add-edit-modal").dimmer({
			closable : false,
			debug : false
		});

		$cancelConfirmModalNoButton = $("#cancel-confirm-no-button");
		$cancelConfirmModalNoButton.click(cancelConfirmModalNoButtonHandler);
		$cancelConfirmModalYesButton = $("#cancel-confirm-yes-button");
		$cancelConfirmModalYesButton.click(cancelConfirmModalYesButtonHandler);

		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_CREATE, function(message){
			console.log(message);
		});	
	}
	
	/**
	 * Modal submit button handler
	 * @private
	 */
	function submitButtonHandler() {
		switch (userAction) {
		case UserAction.ADD:
			createNewProfile();
			break;
		case UserAction.EDIT:
			handleProfileEdit();
			break;
		default:
			break;
		}

	}
	
	/**
	 * Configure the Modal based on the profile type. 
	 * @param {ProviderType} providerType - profile type
	 * @private
	 */
	function configureModalForType(providerType) {
		switch (providerType) {
		case ProviderType.RETURN:
			configureModalForDownlink();
			break;
		case ProviderType.FORWARD:
			configureModalForForward();
			break;
		default:

		}

		setModalHeader(providerType);
	}	
	
	/**
	 * Handle profile edit
	 * @private
	 */
	function handleProfileEdit() {
		
		// Get the properties of the edited profile
		var profileAfterEdit = getProfileFromForm();
		
		var updateProfile = {};
		
		newProfile = false;
		
		// Construct an update profile object which contains only
		// the fields that have been modified. 
		for (var prop in profileBeforeEdit) {
			if (profileBeforeEdit.hasOwnProperty(prop)
				&& profileBeforeEdit[prop] != null && profileAfterEdit[prop] != null) {
				if (profileBeforeEdit[prop] !== profileAfterEdit[prop]) {
					updateProfile[prop] = profileAfterEdit[prop];
				}
			}
		}
		
		// If the update profile object is empty then just close
		// the modal. This happens when the use edits a profile but 
		// does not make any changes to the properties.
		// If there are updated properties, validate the updates and 
		// only submit the updates to the server when validation checks
		// have passed.
		if (!isEmpty(updateProfile)) {
			validateProfile(updateProfile);
			if (validationPassed) {
				DataService.updateProfile(profileBeforeEdit[SharedProfileProperty.PROFILE_NAME], updateProfile, addEditSuccessResponseHandler);				
			}
		} else {
			hide();
		}
	}
	
	/**
	 * Add new profile. Method configures the modal according to 
	 * the provider type, sets the default values and shows the modal. 
	 * @public
	 * @param {ProviderType} providerType - type of provider
	 * profile being added (Forward/Return)
	 */
	function addProfile(providerType) {
		userAction = UserAction.ADD;
		currentProviderType = providerType;
		makeProfileNameEditable();
		configureModalForType(providerType);
		setDefaultValues();
		show();
	}
	
	/**
	 * Make the Profile Name field read only. Profile names
	 * cannot be updated after creation. Profile Name field
	 * is set to read only when an existing profile is being
	 * edited 
	 * @private
	 */
	function makeProfileNameReadOnly() {
		$profileName.prop('readonly', true);
		$profileName.css("pointer-events", "none");
	}
	
	/**
	 * Make the Profile Name editable. Profile names
	 * cannot be updated after creation. When a new profile
	 * is being created the Profile Name must be editable.
	 * @private
	 */
	function makeProfileNameEditable() {
		$profileName.prop('readonly', false);
		$profileName.css("pointer-events", "");
	}
	
	/**
	 * Set default values
	 * @private
	 */
	function setDefaultValues() {
		setProfileName("");
		setHosts("");
		setInstanceId("");
		setUserName("");
		setUserPassword("");
		setUserPasswordConfirmation("");
		setProviderName("");
		setProviderPassword("");
		setProviderPasswordConfirmation("");
		setStartTime("");
		setStopTime("");
		setScId("");
		setFrameVersion("");
		setVcId("");

		setUserAuthMode(AuthMode.NONE);
		setProviderAuthMode(AuthMode.NONE);
		setReturnType(ReturnType.RETURN_ALL);
		setFrameQuality(FrameQuality.ALL);
	}
	
	
	/**
	 * Datatable requires all fields to be present for each
	 * record based on the defined columns. This utility method 
	 * filters out non applicable fields from the input profile 
	 * object based on the Provider Type
	 * 
	 * @private
	 * @param {Object} profile - profile object
	 * @return {Object} filteredProfile - profile object
	 * containing only applicable fields
	 */
	function filterOutNonApplicableFields(profile) {
		var providerType = profile[SharedProfileProperty.PROVIDER_TYPE];
		var filteredProfile = {};

		if (providerType === ProviderType.FORWARD) {

			for (var prop in SharedProfileProperty) {
				if (SharedProfileProperty.hasOwnProperty(prop)) {
					var propVal = SharedProfileProperty[prop]
					if (profile.hasOwnProperty(propVal)) {
						filteredProfile[propVal] = profile[propVal];
					}
				}
			}
		} else {
			filteredProfile = profile;
		}

		return filteredProfile;
	}
	
	
	/**
	 * Delete a list of profiles.
	 * @public
	 * @param {Array} profileList - list of profile objects selected
	 * for deletion.
	 */
	function deleteProfiles(profileList) {
		profileList.forEach(function(profile, i, array) {
			DataService.deleteProfile(profile[SharedProfileProperty.PROFILE_NAME]);
		});
	}
	
	
	function addEditSuccessResponseHandler() {
		hide();
	}
	
	
	/**
	 * Validate profile. Checks all profile properties
	 * and displays the validation error modal if any of the
	 * fields do not meet the validation criteria
	 * @private
	 * @param {Object} profile - profile being validated
	 */
	function validateProfile(profile) {

		var validTimeFmt = "Must be specified using the following DOY " +
			"format with the milliseconds being optional: yyyy-dddThh:mm:ss.sss"

		var errorMessage = "Following Field(s) Are Not Valid:<br><br>";
		var msg = "";
		validationPassed = true;

		var profileName;

		var startTime = profile[ReturnProfileProperty.START_TIME];
		var stopTime = profile[ReturnProfileProperty.STOP_TIME];

		var scId = profile[ReturnProfileProperty.SPACECRAFT_ID];
		var vcId = profile[ReturnProfileProperty.VIRTUAL_CHANNEL];
		var frameVersion = profile[ReturnProfileProperty.FRAME_VERSION];

		var returnType = profile[ReturnProfileProperty.RETURN_TYPE];

		var userPass = profile[SharedProfileProperty.USER_PASSWORD];
		var userPassConf = profile[SharedProfileProperty.USER_PASSWORD_CONFIRMATION];
		var provPass = profile[SharedProfileProperty.PROVIDER_PASSWORD];
		var provPassConf = profile[SharedProfileProperty.PROVIDER_PASSWORD_CONFIRMATION];
		var hosts = profile[SharedProfileProperty.PROVIDER_HOSTS];

		if (newProfile) {
			profileName = profile[SharedProfileProperty.PROFILE_NAME];
			if (profileName === "" || /^\s*$/.test(profileName)) {
				msg = " - <b>Profile Name</b>: is required.<br><br>";
				errorMessage += msg;
				validationPassed = false;
			}

			if (profileName.includes(".")) {
				msg = " - <b>Profile Name</b>: cannot contain the '.' character<br><br>";
				errorMessage += msg;
				validationPassed = false;
			}
			
			if (profileName.includes(" ")) {
				msg = " - <b>Profile Name</b>: cannot contain spaces<br><br>";
				errorMessage += msg;
				validationPassed = false;
			}
		}

		if (hosts != null && hosts !== "") {
			if (!/^[a-zA-Z0-9.\-:|]*$/.test(hosts)) {
				msg = " - <b>Profile Hosts</b>: contains invalid characters.<br><br>";
				errorMessage += msg;
				validationPassed = false;
			} else {
				let hostsArray = hosts.split('|');
				if (hostsArray.length < 1) {
					msg = " - <b>Profile Hosts</b>: must include hosts in the form 'host:port [|host:port]' where host:port pairs are separated by the pipe character<br><br>";
					errorMessage += msg;
					validationPassed = false;
				}
				for (let i = 0; i < hostsArray.length; i++) {
					let pair = hostsArray[i].trim();
					let components = pair.split(':');
					if (components.length !== 2) {
						msg = " - <b>Profile Hosts</b>: '" + pair + "' is not a valid host:port pair<br><br>";
						errorMessage += msg;
						validationPassed = false;
					}

					let port = components[1];
					if (!/^\d+$/.test(port)) {
						msg = " - <b>Profile Hosts</b>: '" + pair + "' port number is not valid.<br><br>";
						errorMessage += msg;
						validationPassed = false;
					}

					let portInt = parseInt(port);
					if (isNaN(portInt) || portInt < 1 || portInt > 65535) {
						msg = " - <b>Profile Hosts</b>: '" + pair + "' port number is not valid.<br><br>";
						errorMessage += msg;
						validationPassed = false;
					}
				}
			}
		} else if (newProfile) {
			msg = " - <b>Profile Hosts</b>: must not be empty.<br><br>";
			errorMessage += msg;
			validationPassed = false;
		}

		if (startTime != null && startTime !== "") {
			if (!isValidDoyTime(startTime)) {
				validationPassed = false;
				msg = " - <b>Start Time</b>: '" + startTime + "' not valid.<br>" + validTimeFmt + "<br><br>";
				errorMessage = errorMessage + msg;
			}
		}

		if (stopTime != null && stopTime !== "") {
			if (!isValidDoyTime(stopTime)) {
				validationPassed = false;
				msg = " - <b>Stop Time</b>: '" + stopTime + "' not valid.<br>" + validTimeFmt + "<br><br>";
				errorMessage = errorMessage + msg;
			}
		}

		if (returnType === ReturnType.RETURN_CHANNEL) {
			if (scId != null && scId !== "") {
				if (!/^\d+$/.test(scId)) {
					msg = " - <b>Spacecraft ID</b>: '" + scId + "' not valid.<br>Must be a positive integer<br><br>";
					errorMessage += msg;
					validationPassed = false;
				}
			}

			if (frameVersion != null && frameVersion !== "") {
				if (!/^\d+$/.test(frameVersion)) {
					msg = " - <b>Frame Version</b>: '" + frameVersion + "' not valid.<br>Must be a positive integer<br><br>";
					errorMessage += msg;
					validationPassed = false;
				}
			}

			if (vcId != null && vcId !== "") {
				if (!/^\d+$/.test(vcId)) {
					msg = " - <b>Virtual Channel ID</b>: '" + vcId + "' not valid.<br>Must be a positive integer<br><br>";
					errorMessage += msg;
					validationPassed = false;
				}
			}
		}
		
		if (!(userPass == null && userPassConf == null)) {
			if (userPass !== userPassConf) {
				msg = " - <b>User Password Confirmation</b>: Does not match<br><br>";
				errorMessage += msg;
				validationPassed = false;

			}			
		}
		
		if (!(provPass == null && provPassConf == null)) {
			if (provPass !== provPassConf) {
				msg = " - <b>Provider Password Confirmation</b>: Does not match<br><br>";
				errorMessage += msg;
				validationPassed = false;

			}			
		}
		
		// Remove client side fields before sending the profile to the server
		delete profile[SharedProfileProperty.PROFILE_NAME];
		delete profile[SharedProfileProperty.PROVIDER_TYPE];
		delete profile[SharedProfileProperty.USER_PASSWORD_CONFIRMATION];
		delete profile[SharedProfileProperty.PROVIDER_PASSWORD_CONFIRMATION];
		delete profile[ReturnProfileProperty.RETURN_TYPE];
		
		if (!validationPassed) {
			showValidationErrorModal(errorMessage);
		}
	}
	
	/**
	 * Show Error Modal with validation error information.
	 * @private
	 * @param {String} message - string containing the validation errors
	 */
	function showValidationErrorModal(message) {
		$modalDimmer.dimmer('toggle');

		ErrorModal.setHeader("Validation Errors");
		ErrorModal.setMessage(message);
		ErrorModal.setCallbackFunction(validationCallback);

		ErrorModal.show();
	}

	function validationCallback() {
		$modalDimmer.dimmer('toggle');
	}
	
	
	/**
	 * Construct a profile object from the modal form fields.
	 * @private
	 * @returns {Object} profile - profile object 
	 */
	function getProfileFromForm() {
		var profileObj = {};

		if (currentProviderType === ProviderType.RETURN) {
			profileObj[SharedProfileProperty.INTERFACE_TYPE] = getReturnType();
			profileObj[ReturnProfileProperty.RETURN_TYPE] = getReturnType();
			profileObj[ReturnProfileProperty.START_TIME] = getStartTime();
			profileObj[ReturnProfileProperty.STOP_TIME] = getStopTime();
		} else {
			profileObj[SharedProfileProperty.INTERFACE_TYPE] = ProviderType.FORWARD;
		}

		profileObj[SharedProfileProperty.PROVIDER_TYPE] = currentProviderType;
		profileObj[SharedProfileProperty.PROFILE_NAME] = getProfileName();
		profileObj[SharedProfileProperty.PROVIDER_NAME] = getProviderName();
		profileObj[SharedProfileProperty.PROVIDER_HOSTS] = getHosts();
		profileObj[SharedProfileProperty.SERVICE_INSTANCE_ID] = getInstanceId();
		profileObj[SharedProfileProperty.PROVIDER_AUTHENTICATION_MODE] = getProviderAuthMode();
		profileObj[SharedProfileProperty.USER_AUTHENTICATION_MODE] = getUserAuthMode();
		profileObj[SharedProfileProperty.USER_NAME] = getUserName();
		profileObj[SharedProfileProperty.USER_PASSWORD] = getUserPassword();
		profileObj[SharedProfileProperty.USER_PASSWORD_CONFIRMATION] = getUserPasswordConfirmation();
		profileObj[SharedProfileProperty.PROVIDER_PASSWORD] = getProviderPassword();
		profileObj[SharedProfileProperty.PROVIDER_PASSWORD_CONFIRMATION] = getProviderPasswordConfirmation();

		if (getReturnType() === ReturnType.RETURN_CHANNEL) {
			profileObj[ReturnProfileProperty.SPACECRAFT_ID] = getScId();
			profileObj[ReturnProfileProperty.FRAME_VERSION] = getFrameVersion();
			profileObj[ReturnProfileProperty.VIRTUAL_CHANNEL] = getVcId();
		}

		if (getReturnType() === ReturnType.RETURN_ALL) {
			profileObj[ReturnProfileProperty.FRAME_QUALITY] = getFrameQuality();
		}

		return profileObj;
	}
	
	/**
	 * Create new profile. Retrieves the profile object from the modal form,
	 * validates the profile and submits the new profile request to the server.
	 * @private
	 */
	function createNewProfile() {
		var profileData = {};
		var profileName = getProfileName();
		newProfile = true;
		profileData = getProfileFromForm();
		validateProfile(profileData);
		if (validationPassed) {
			DataService.createNewProfile(profileName, profileData, addEditSuccessResponseHandler);
		}
	}
	
	
	/**
	 * Edit existing profile. Configures and populates the modal properties
	 * according to the profile being edited and shows the modal.
	 * @param {Object} profile - profile object being edited
	 * @public
	 */
	function editProfile(profile) {
		setDefaultValues();
		userAction = UserAction.EDIT;
		currentProviderType = profile[SharedProfileProperty.PROVIDER_TYPE];
		profileBeforeEdit = filterOutNonApplicableFields(profile);
		configureModalForType(currentProviderType);

		setProfileName(profile[SharedProfileProperty.PROFILE_NAME]);
		makeProfileNameReadOnly();

		setProviderName(profile[SharedProfileProperty.PROVIDER_NAME]);
		setHosts(profile[SharedProfileProperty.PROVIDER_HOSTS]);
		setInstanceId(profile[SharedProfileProperty.SERVICE_INSTANCE_ID]);

		if (profile[SharedProfileProperty.PROVIDER_TYPE] === ProviderType.RETURN) {
			setReturnType(profile[ReturnProfileProperty.RETURN_TYPE]);
		}

		setUserAuthMode(profile[SharedProfileProperty.USER_AUTHENTICATION_MODE]);
		setUserName(profile[SharedProfileProperty.USER_NAME]);
		setProviderAuthMode(profile[SharedProfileProperty.PROVIDER_AUTHENTICATION_MODE])
		setStartTime(profile[ReturnProfileProperty.START_TIME]);
		setStopTime(profile[ReturnProfileProperty.STOP_TIME]);

		if (profile[SharedProfileProperty.RETURN_TYPE] === ReturnType.RETURN_ALL) {
			setFrameQuality(profile[ReturnProfileProperty.FRAME_QUALITY]);
		}

		if (profile[ReturnProfileProperty.RETURN_TYPE] === ReturnType.RETURN_CHANNEL) {
			setScId(profile[ReturnProfileProperty.SPACECRAFT_ID]);
			setVcId(profile[ReturnProfileProperty.VIRTUAL_CHANNEL]);
			setFrameVersion(profile[ReturnProfileProperty.FRAME_VERSION]);
		}

		show();
	}
	
	/**
	 * Cancel button handler. Opens the Cancel Confirmation modal.
	 * @private
	 */
	function cancelButtonHandler() {
		$modalDimmer.dimmer('toggle');
		$cancelConfirmModal.modal('show');
	}
	
	/**
	 * Cancel Confirmation Modal 'No' button handler. If the user presses
	 * the No button, the Cancel Confirmation modal is hidden but the Profile
	 * modal remains show.
	 * @private
	 */
	function cancelConfirmModalNoButtonHandler() {
		$cancelConfirmModal.modal('hide');
		$modalDimmer.dimmer('toggle');
	}

	/**
	 * Cancel Confirmation Modal 'Yes' button handler. If the user presses
	 * the Yes button, both the Cancel Confirmation modal the Profile modal
	 * are hidden.
	 * @private
	 */
	function cancelConfirmModalYesButtonHandler() {
		$cancelConfirmModal.modal('hide');
		$addEditModal.form('clear');
		$modalDimmer.dimmer('toggle');
		$addEditModal.modal('hide');
	}
	
	/**
	 * Get profile name
	 * @returns {String} profile name
	 */
	function getProfileName() {
		return $profileName.val();
	}
	
	/**
	 * Set profile name
	 * @param {String} name - profile name to set
	 */
	function setProfileName(name) {
		$profileName.val(name);
	}

	/**
	 * Get provider name
	 * @returns {String} provider name
	 */
	function getProviderName() {
		return $providerName.val();
	}

	/**
	 * Set provider name
	 * @param {String} name - provider name to set
	 */
	function setProviderName(name) {
		$providerName.val(name);
	}
	
	/**
	 * Get hosts
	 * @returns {String} hosts
	 */
	function getHosts() {
		return $profileHosts.val();
	}

	/**
	 * Set hosts
	 * @param {String} hosts - host names and ports to set
	 */
	function setHosts(hosts) {
		$profileHosts.val(hosts);
	}

	/**
	 * Get instance id
	 * @returns {String} instance id
	 */
	function getInstanceId() {
		return $instanceID.val();
	}

	/**
	 * Set instance id
	 * @param {String} instanceId - instance id to set
	 */
	function setInstanceId(instanceId) {
		$instanceID.val(instanceId);
	}
	
	/**
	 * Set return type
	 * @param {String} returnType - return type to set
	 */
	function setReturnType(returnType) {
		$returnTypeDropdown.dropdown('set selected', returnType);
	}

	/**
	 * Get return type
	 * @returns {String} return type
	 */
	function getReturnType() {
		return $returnTypeDropdown.dropdown('get value');
	}

	/**
	 * Get user name
	 * @returns {String} user name
	 */
	function getUserName() {
		return $userName.val();
	}

	/**
	 * Set user name
	 * @param {String} userName - user name to set
	 */
	function setUserName(userName) {
		$userName.val(userName);
	}
	
	/**
	 * Get user authentication mode
	 * @returns {AuthMode} user authentication mode
	 */
	function getUserAuthMode() {
		return $userAuthModeDropdown.dropdown('get value');
	}
	
	/**
	 * Set user authentication mode
	 * @param {AuthMode} user authentication mode
	 */
	function setUserAuthMode(userAuthMode) {
		$userAuthModeDropdown.dropdown('set selected', userAuthMode);
	}
	
	/**
	 * Get provider authentication mode
	 * @returns {AuthMode} provider authentication mode
	 */
	function getProviderAuthMode() {
		return $providerAuthModeDropdown.dropdown('get value');
	}
	
	/**
	 * Set provider authentication mode
	 * @param {AuthMode} provider authentication mode
	 */
	function setProviderAuthMode(providerAuthMode) {
		$providerAuthModeDropdown.dropdown('set selected', providerAuthMode);
	}
	
	/**
	 * Get user password
	 * @returns {String} user password
	 */
	function getUserPassword() {
		return $userPassword.val();
	}
	
	/**
	 * Set user password
	 * @param {String} user password
	 */
	function setUserPassword(userPwd) {
		$userPassword.val(userPwd);
	}
	
	/**
	 * Get user password confirmation
	 * @returns {String} user password confirmation
	 */
	function getUserPasswordConfirmation() {
		return $userPasswordConfirmation.val();
	}

	/**
	 * Set user password confirmation
	 * @param {String} user password confirmation
	 */
	function setUserPasswordConfirmation(userPwdConfirmation) {
		$userPasswordConfirmation.val(userPwdConfirmation);
	}
	
	/**
	 * Get provider password
	 * @returns {String} provider password
	 */
	function getProviderPassword() {
		return $providerPassword.val();
	}
	
	/**
	 * Set provider password
	 * @param {String} provider password
	 */
	function setProviderPassword(providerPwd) {
		$providerPassword.val(providerPwd);
	}

	/**
	 * Get provider password confirmation
	 * @returns {String} user password confirmation
	 */
	function getProviderPasswordConfirmation() {
		return $providerPasswordConfirmation.val();
	}
	
	/**
	 * Set provider password confirmation
	 * @param {String} user password confirmation
	 */
	function setProviderPasswordConfirmation(providerPwdConfirmation) {
		$providerPasswordConfirmation.val(providerPwdConfirmation);
	}
	
	/**
	 * Get start time
	 * @returns {String} startTime
	 */
	function getStartTime() {
		return $startTime.val();
	}
	
	/**
	 * Set start time
	 * @param {String} startTime
	 */
	function setStartTime(startTime) {
		$startTime.val(startTime);
	}

	/**
	 * Get stop time
	 * @returns {String} stopTime
	 */
	function getStopTime() {
		return $stopTime.val();
	}
	
	/**
	 * Set stop time
	 * @param {String} stopTime
	 */
	function setStopTime(stopTime) {
		$stopTime.val(stopTime);
	}
	
	/**
	 * Get spacecraft id
	 * @returns {Integer} scId
	 */
	function getScId() {
		return $scId.val();
	}

	/**
	 * Set spacecraft id
	 * @param {Integer} scId
	 */
	function setScId(scId) {
		$scId.val(scId);
	}

	/**
	 * Get virtual channel id
	 * @returns {Integer} vcId
	 */
	function getVcId() {
		return $vcId.val();
	}

	/**
	 * Set virtual channel id
	 * @param {Integer} vcId
	 */
	function setVcId(vcId) {
		$vcId.val(vcId);
	}
	
	/**
	 * Get frame version
	 * @returns {Integer} frameVersion
	 */
	function getFrameVersion() {
		return $frameVersion.val();
	}

	/**
	 * Set frame version
	 * @param {Integer} frameVersion
	 */
	function setFrameVersion(vcId) {
		$frameVersion.val(vcId);
	}
	
	/**
	 * Get frame quality
	 * @returns {FrameQuality} frameQuality
	 */
	function getFrameQuality() {
		return $frameQualityDropdown.dropdown('get value');
	}
	
	/**
	 * Set frame quality
	 * @param {FrameQuality} frameQuality
	 */
	function setFrameQuality(frameQuality) {
		$frameQualityDropdown.dropdown('set selected', frameQuality);
	}
	
	/**
	 * Configure modal for working with Forward Provider profiles
	 */
	function configureModalForForward() {
		$("#return-type-mode-content").hide();
	}
	
	/**
	 * Configure modal for working with Return Provider profiles
	 */
	function configureModalForDownlink() {
		$("#return-type-mode-content").show();
	}
	
	/**
	 * Set modal header
	 * @param {String} headerString
	 */
	function setModalHeader(headerString) {
		$("#provider-add-edit-modal-header").text("Provider Type - " + headerString);
	}
	
	/**
	 * Show the modal
	 */
	function show() {
		$addEditModal.modal('show');
	}
	
	/**
	 * Hide the modal
	 */
	function hide() {
		$addEditModal.modal('hide');
	}
	
	// Exposes public methods. Methods not mapped here
	// are private.
	return {
		init : init,
		addProfile : addProfile,
		editProfile : editProfile,
		deleteProfiles : deleteProfiles,
		validationCallback : validationCallback
	}

})();