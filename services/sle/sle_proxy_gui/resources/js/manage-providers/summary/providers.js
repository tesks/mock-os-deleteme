/**
 * This module controls the Manage Service Providers summary view.
 * - lists the SLE provider profiles in a table(Datatable)
 * - allows the user to Add, Edit and Delete profiles
 */
var SleServiceProvider = (function() {

	var $paramAddButton;
	var $paramEditButton;
	var $paramDeleteButton;
	var $paramAddEditModal;
	var $confirmDeleteModal;

	var $profileSummaryTable;
	var summaryTableColumns = [];
	
	// Summary table columns
	summaryTableColumns = [
		{
			data : SharedProfileProperty.PROFILE_NAME
		}, {
			data : SharedProfileProperty.PROVIDER_TYPE
		}, {
			data : SharedProfileProperty.PROVIDER_HOSTS
		}, {
			data : SharedProfileProperty.SERVICE_INSTANCE_ID
		}, {
			data : SharedProfileProperty.PROVIDER_AUTHENTICATION_MODE
		}, {
			data : SharedProfileProperty.PROVIDER_NAME
		}, {
			data : SharedProfileProperty.USER_AUTHENTICATION_MODE
		}, {
			data : SharedProfileProperty.USER_NAME
		}, {
			data : ReturnProfileProperty.RETURN_TYPE
		}, {
			data : ReturnProfileProperty.START_TIME
		}, {
			data : ReturnProfileProperty.STOP_TIME
		}, {
			data : ReturnProfileProperty.FRAME_VERSION
		}, {
			data : ReturnProfileProperty.FRAME_QUALITY
		}, {
			data : ReturnProfileProperty.SPACECRAFT_ID
		}, {
			data : ReturnProfileProperty.VIRTUAL_CHANNEL
		}
	];
	
	/**
	 * Initialize view
	 * @public
	 */
	function init() {
		
		$confirmDeleteModal = $("#profile-delete-confirm-modal").modal({
			closable : false,
		    onApprove : function() {
		    	handleProfileDelete();
		    }
		});
		
		$paramAddButton = $("#param-add-button");
		$paramEditButton = $("#param-edit-button");
		$paramDeleteButton = $("#param-delete-button");
		$paramEditButton.click(handleProfileEdit);
		$paramDeleteButton.click(function() {
			$confirmDeleteModal.modal("show");
		});

		$paramAddButton.dropdown({
			action : providerAddHandler
		});

		// Show the profile table
		drawTable();
		
		// Initialize the Profile Manager
		ProfileManager.init();
		
		// Subscribe to the Profile Creation events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_CREATE, function(message) {
			var newProfile = message.profile;
			var datatableCompatibleProfile = makeProfileCompatibleWithDatatable(newProfile);
			
			profileList.push(datatableCompatibleProfile);
			redrawTable();
		});		
		
		// Subscribe to the Profile Update events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_UPDATE, function(message) {
			var updateProfile = message.profile;
			profileList.forEach(function(profile) {
				if (profile[SharedProfileProperty.PROFILE_NAME] === updateProfile[SharedProfileProperty.PROFILE_NAME]) {

					for (var prop in updateProfile) {
						if (updateProfile.hasOwnProperty(prop)) {
							if (prop !== SharedProfileProperty.PROFILE_NAME) {
								profile[prop] = updateProfile[prop];
							}
						}
					}
				}
			});

			redrawTable();
		});
		
		// Subscribe to the Profile Delete events
		EventBus.subscribe(EventTopic.WEBSOCKET_SLE_PROFILE_DELETE, function(message) {
			var deletedProfileName = message[WebsocketMesssageProperty.PROFILE_NAME];
			var newProfileList = [];
			
			profileList.forEach(function(profile) {
				if (profile[SharedProfileProperty.PROFILE_NAME] !== deletedProfileName) {
					newProfileList.push(profile);
				}
			});
			profileList = newProfileList;
			redrawTable();
		});		
		
	}
	
	/**
	 * Enable/Disable the Edit and Delete buttons based on the 
	 * profile selection state.
	 * - Enable both Edit and Delete buttons when a 
	 *   single profile is selected.
	 * - Enable the Delete button and disable the Edit button
	 *   when multiple profiles are selected. We allow the user
	 *   to delete multiple profiles but prevent the user from
	 *   editing multiple profiles at once.
	 *   
	 * @private
	 */
	function handleEditDeleteActivation() {
		var selectedRowsList = $profileSummaryTable.rows('.selected').data().toArray();

		if (selectedRowsList.length >= 1) {

			if ($paramDeleteButton.hasClass("disabled")) {
				$paramDeleteButton.removeClass('disabled');
			}

			if (selectedRowsList.length == 1) {
				if ($paramEditButton.hasClass("disabled")) {
					$paramEditButton.removeClass('disabled');
				}
			} else {
				if (!$paramEditButton.hasClass("disabled")) {
					$paramEditButton.addClass('disabled');
				}
			}
		} else {
			if (!$paramEditButton.hasClass("disabled")) {
				$paramEditButton.addClass('disabled');
			}

			if (!$paramDeleteButton.hasClass("disabled")) {
				$paramDeleteButton.addClass('disabled');
			}
		}
	}
	
	/**
	 * Add button handler. Show the profile modal using the 
	 * ProfileManager addProfile method, passing in the selected
	 * provider type.
	 * @private
	 */
	function providerAddHandler(text, value) {
		ProfileManager.addProfile(value);
	}
	
	/**
	 * Edit button handler. Show the profile modal using the 
	 * ProfileManager editProfile method, passing in the selected
	 * profile.
	 * @private
	 */
	function handleProfileEdit() {
		var selectedRowsList = $profileSummaryTable.rows('.selected').data().toArray();
		var selectedParam = selectedRowsList[0];

		ProfileManager.editProfile(selectedParam);
	}

	/**
	 * Delete button handler. Pass the list of selected profiles
	 * to the Profile Manager for deletion.
	 * @private
	 */
	function handleProfileDelete() {
		var selectedRowsList = $profileSummaryTable.rows('.selected').data().toArray();

		ProfileManager.deleteProfiles(selectedRowsList);
	}
	
	/**
	 * Render the Datatable.
	 * @private
	 */
	function drawTable() {
		$profileSummaryTable = $('#profile-summary-table').DataTable({
			data : profileList,
			columns : summaryTableColumns,
			"order" : [ [ 0, "asc" ] ],
			"bPaginate" : false,
			select : true,
			"destroy" : true,
		}).on('select', handleEditDeleteActivation)
		.on('deselect', handleEditDeleteActivation)
		.select.style('os');
	}
	
	/**
	 * Redraw the Datatable with the updated profile list. Also
	 * make sure the Edit and Delete buttons reflect the appropriate
	 * state.
	 * @private
	 */
	function redrawTable() {
		$profileSummaryTable.clear();
		$profileSummaryTable.rows.add(profileList);
		$profileSummaryTable.draw();
		handleEditDeleteActivation();
	}

	return {
		init : init
	}
})();