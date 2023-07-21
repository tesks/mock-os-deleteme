/**
 * Config module.
 */
var Config = (function() {
	
	var config;
	
	var ConfigProperty = {
		REST_API_PATH : "restApiPath",
		WEBSOCKET_PATH : "websocketPath"
	}
	
	/**
	 * Initialize config.
	 * @public
	 */
	function init() {
		config = appConfig;
	}
	
	/**
	 * Get the REST API Path
	 * @public
	 * @returns {String} path - the base path of the REST API
	 */
	function getRestApiPath() {
		return config[ConfigProperty.REST_API_PATH];
	}
	
	/**
	 * Get the Websocket Path
	 * @public
	 * @returns {String} path - the Websocket connection path
	 */
	function getWebsocketPath() {
		return config[ConfigProperty.WEBSOCKET_PATH];
	}
	
	return {
		init: init,
		getRestApiPath: getRestApiPath,
		getWebsocketPath: getWebsocketPath
	}
	
})();