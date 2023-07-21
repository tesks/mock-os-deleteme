/**
 * Server Connection Indicator controls the circular
 * label located within the footer of the application.
 * Color is Green when connection is established
 * or Grey when disconnected
 */
var ServerConnectionIndicator = (function(){
	
	var $serverConnectionIndicator;
	
	/**
	 * Initialize
	 */
	function init() {
		$serverConnectionIndicator = $("#server-connection-indicator");
	}
	
	/**
	 * Set the indicator color to green
	 */
	function serverAvailable() {
		$serverConnectionIndicator.addClass("green");
	}
	
	/**
	 * Set the indicator color to the default grey
	 */
	function serverNotAvailable() {
		$serverConnectionIndicator.removeClass("green");
	}
	
	return {
		init: init,
		serverAvailable: serverAvailable,
		serverNotAvailable: serverNotAvailable,
	}
	
})();