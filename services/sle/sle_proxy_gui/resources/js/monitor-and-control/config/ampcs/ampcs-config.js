/**
 * Chill Config module controls the AMPCS configuration component on the 
 * Monitor and Control interface.
 */
var ChillConfig = (function() {
	
	var $downlinkHost;
	var $downlinkPort;
	var $session;
	var $databaseHost;
	var $databasePort;
	var $uplinkPort;
	
	var $editDownlinkConfigButton;
	
	var chillConfig;
	
	/**
	 * Initialize the component
	 * @public
	 */
	function init() {
		$downlinkHost = $("#downlink-host");
		$downlinkPort = $("#downlink-port");
		$uplinkPort = $("#uplink-port");
		
		$editDownlinkConfigButton = $("#edit-downlink-config-button");
		$editDownlinkConfigButton.click(function (){
			ChillDownConfigModal.editDownlinkConfig(chillConfig);
		});
		
		// Get the configuration properties from the server
		loadConfigData();
		
		// Subscribe to Chill Config Update event
		EventBus.subscribe(EventTopic.WEBSOCKET_CHILL_CONFIG_UPDATE, function(message) {
			setDownlinkHost(message[ChillConfigProperty.DOWNLINK_HOST]);
			setDownlinkPort(message[ChillConfigProperty.DOWNLINK_PORT]);	
			chillConfig[ChillConfigProperty.DOWNLINK_HOST] = message[ChillConfigProperty.DOWNLINK_HOST];
			chillConfig[ChillConfigProperty.DOWNLINK_PORT] = message[ChillConfigProperty.DOWNLINK_PORT];
		});
	}
	
	/**
	 * Load config data from the server and populate the fields
	 * @private
	 */
	function loadConfigData() {
		chillConfig = DataService.getChillConfig();
		if (chillConfig != null) {
			setDownlinkHost(chillConfig[ChillConfigProperty.DOWNLINK_HOST]);
			setDownlinkPort(chillConfig[ChillConfigProperty.DOWNLINK_PORT]);
			setUplinkListeningPort(chillConfig[ChillConfigProperty.UPLINK_LISTENING_PORT]);
		}
	}
	
	/**
	 * Set downlink host
	 * @param {String} host - downlink host name
	 * @private
	 */
	function setDownlinkHost(host) {
		$downlinkHost.val(host);
	}
	
	/**
	 * Set downlink port 
	 * @param {Integer} port - downlink port number
	 * @private
	 */
	function setDownlinkPort(port) {
		$downlinkPort.val(port);
	}
		
	/**
	 * Set uplink listening port
	 * @param {Integer} port - uplink listening port
	 * @private
	 */
	function setUplinkListeningPort(port) {
		$uplinkPort.val(port);
	}
	
	return {
		init: init
	}
	
})();