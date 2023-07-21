/**
 * This module controls the Chill Down configuration modal
 */
var ChillDownConfigModal = (function() {
	
	var $downlinkHost;
	var $downlinkPort;
	
	var $cancelButton;
	var $submitButton;
	
	var $downlinkConfigModal;
	
	/**
	 * Initialize component
	 * @public
	 */
	function init() {
		
		// Modal selector and configuration
		$downlinkConfigModal = $("#downlink-edit-modal").modal({
			closable : false,
			allowMultiple : true
		});		
		
		$downlinkHost = $("#modal-downlink-host");
		$downlinkPort = $("#modal-downlink-port");
		
		$downlinkPort.keydown(allowNumericInputOnly);
		
		$cancelButton = $("#downlink-config-modal-cancel-button");
		$cancelButton.click(function() {
			hide();
		});
		
		$submitButton = $("#downlink-config-modal-submit-button");
		$submitButton.click(validateAndSubmitUpdates);
	}
	
	/**
	 * Set chill down config fields of the modal based on the 
	 * config object passed in and show the modal.
	 * @param {Object} chillConfig - config object which contains chill down properties
	 * @public
	 */
	function editDownlinkConfig(chillConfig) {
		setDownlinkHost(chillConfig[ChillConfigProperty.DOWNLINK_HOST]);
		setDownlinkPort(chillConfig[ChillConfigProperty.DOWNLINK_PORT]);
		show();
	}
	
	/**
	 * Validate user input and submit updates
	 * @private
	 */
	function validateAndSubmitUpdates() {
		var updateConfig = {};
		// TODO: Add validation logic for host and port
		updateConfig[ChillConfigProperty.DOWNLINK_HOST] = getDownlinkHost();
		updateConfig[ChillConfigProperty.DOWNLINK_PORT] = getDownlinkPort();
		DataService.updateDownlinkConfig(updateConfig, updateSuccessHandler);
	}
	
	/**
	 * Server update success callback
	 * @private
	 */
	function updateSuccessHandler() {
		hide();
	}
	
	/**
	 * Set the chill_down host field
	 * @param {String} downlinkHost - name of the host chill_down is running on
	 * @private
	 */
	function setDownlinkHost(downlinkHost) {
		$downlinkHost.val(downlinkHost);
	}
	
	/**
	 * Get chill_down host
	 * @returns {String} downlinkHost - name of the host chill_down is running on
	 * @private
	 */
	function getDownlinkHost() {
		return $downlinkHost.val();
	}
	
	/**
	 * Set the chill_down port - port number chill_down is listening on
	 * @param {Integer} downlinkPort - port number 
	 * @private
	 */
	function setDownlinkPort(downlinkPort) {
		$downlinkPort.val(downlinkPort);
	}
	
	/**
	 * Get chill_down port - port number chill_down is listening on
	 * @returns {Integer} downlinkPort
	 * @private
	 */
	function getDownlinkPort() {
		return $downlinkPort.val();
	}	
	
	/**
	 * Show the modal
	 * @private
	 */
	function show() {
		$downlinkConfigModal.modal('show');
	}
	
	/**
	 * Hide the modal
	 * @private
	 */
	function hide() {
		$downlinkConfigModal.modal('hide');
	}
	
	
	return {
		init: init,
		editDownlinkConfig: editDownlinkConfig
	}
	
})();