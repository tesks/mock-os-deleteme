/**
 * This module controls the Server Communication Dimmer
 */
var ServerCommunicationDimmer = (function(){
	
	var $mainDimmer;
	var $reloadAppButton;
	var $message;
	var opacityValue = 0.6;
	
	/**
	 * Initialize component.
	 * @public
	 */
	function init() {
		$mainDimmer = $("#server-communication-dimmer").dimmer({
			opacity: opacityValue,
			closable: false
		});
		$message = $("#dimmer-message");
		$reloadAppButton = $("#reload-app-button");
		$reloadAppButton.click(function(){
			location.reload();
		});
	}
	
	/**
	 * Set dimmer message.
	 * @param {String} message - message to show on the dimmer.
	 * @public
	 */
	function setMessage(message) {
		$message.text(message);
	}
	
	/**
	 * Show the dimmer.
	 * @public
	 */
	function show() {
		$mainDimmer.dimmer({
			opacity: opacityValue,
			closable: false
		}).dimmer("show");
	}
	
	/**
	 * Hide the dimmer.
	 * @public
	 */
	function hide() {
		$mainDimmer.dimmer("hide");
	}
	
	// Exposes public methods. Methods not mapped here
	// are private.
	return {
		init: init,
		show: show,
		hide: hide,
		setMessage: setMessage
	}
	
})();