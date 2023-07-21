/**
 * This module controls the generic Error Modal
 */
var ErrorModal = (function() {
	
	var $modal;
	var $header;
	var $message;
	var callbackFunction;
	
	/**
	 * Initialize component.
	 * @public
	 */
	function init() {
		$modal = $("#error-modal").modal({
		      closable : false,
		      allowMultiple : true,
		      
		      onApprove : function() {
		    	  $modal.modal('hide');
		    	  
		    	  if (callbackFunction != null) {
		    		  callbackFunction();
		    	  }
		      }
		});
		$header = $("#error-modal-header");
		$message = $("#error-modal-message");
	}
	
	/**
	 * Set Callback function. This callback function
	 * gets executed when the user pressed the 'Ok' button.
	 * @param {Function} 
	 */
	function setCallbackFunction(functionName) {
		callbackFunction = functionName;
	}
	
	/**
	 * Set modal header.
	 * @param {String} header - string to show on the dimmer header.
	 * @public
	 */
	function setHeader(header) {
		$header.html(header)
	}
	
	/**
	 * Set dimmer message.
	 * @param {String} message - message to show on the dimmer.
	 * @public
	 */
	function setMessage(message) {
		$message.html(message);
	}
	
	/**
	 * Show the dimmer.
	 * @public
	 */
	function show() {
		$modal.modal('show');
	}
	
	// public methods
	return {
		init: init,
		setHeader: setHeader,
		setMessage: setMessage,
		setCallbackFunction: setCallbackFunction,
		show: show,
	}
	
})();