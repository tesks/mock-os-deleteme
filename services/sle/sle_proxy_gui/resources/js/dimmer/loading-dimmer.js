/**
 * This module controls the Application Loading Dimmer
 */
var LoadingDimmer = (function(){
	
	var $loadingDimmer;
	var $reloadAppButton;
	var opacityValue = 0.7;
	
	/**
	 * Initialize component.
	 * @public
	 */
	function init() {
		$loadingDimmer = $("#loading-dimmer").dimmer({
			opacity: opacityValue,
			closable: false
		});
		
	}
	
	/**
	 * Show the dimmer.
	 * @public
	 */
	function show() {
		$loadingDimmer.dimmer({
			opacity: opacityValue,
			closable: false
		}).dimmer("show");
	}
	
	/**
	 * Hide the dimmer.
	 * @public
	 */
	function hide() {
		$loadingDimmer.dimmer("hide");
	}
	
	// public methods
	return {
		init: init,
		show: show,
		hide: hide
	}
	
})();