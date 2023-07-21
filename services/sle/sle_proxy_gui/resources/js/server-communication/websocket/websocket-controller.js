/**
 * The Websocket connection is established using a Web Worker
 * which runs in the background without interfering with the 
 * main execution thread. This approach reduces the possibility
 * of the the websocket connection from taking over the main execution
 * thread and making the UI unresponsive.
 * 
 * Communication with Web Workers from the main thread is done through
 * message passing. Objects passed through messages are copied or transferred,
 * not shared.
 */

// Websocket connection path
var websocketPath;

/**
 * Function which receives messages from the main thread
 */
onmessage = function(e) {
	websocketPath = e.data.websocketPath;
	WebsocketController.init();
}

/**
 * Module which establishes the Websocket connection with the server
 */
var WebsocketController = (function() {
	
	var webSocketClient;
	var workMessage = {};
	
	/**
	 * Initialize
	 * @public
	 */
	function init() {
		
		webSocketClient = new WebSocket(websocketPath);
		
		/**
		 * Connection On Open
		 */
		webSocketClient.onopen = function(event) {
			workMessage = {
				"worker_message" : "websocket_connection_opened",
			}
			postMessage(workMessage);
		};
		
		/**
		 * Connection On Close
		 */
		webSocketClient.onclose = function(event) {
			workMessage = {
				"worker_message" : "websocket_connection_closed",
			}
			postMessage(workMessage);
		};
		
		/**
		 * On Message
		 */
		webSocketClient.onmessage = function(event) {
			workMessage = {
				"worker_message" : "received_websocket_message",
				"websocket_message" : JSON.parse(event.data)
			}
			postMessage(workMessage);
		}
		
		/**
		 * On Error
		 */
		webSocketClient.onerror = function(event) {
			// Do nothing for now
		}		
	}
	
	return {
		init: init
	}
	
})();