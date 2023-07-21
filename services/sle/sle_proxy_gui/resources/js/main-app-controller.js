// Global variables shared by different components
var serverState;
var profileList = [];
var communicationErrorMessage = "Unable to communicate with server";

// Initialize configuration
Config.init();

$(function() {
	
	// Init communication dimmer
	ServerCommunicationDimmer.init();
	
	// Error handler at the window level
	window.onerror = function(message, url, lineNumber) {
		LoadingDimmer.hide();
		ServerCommunicationDimmer.setMessage(communicationErrorMessage);
		ServerCommunicationDimmer.show();
		return true;
	}; 
	
	MainAppController.init();
});

var MainAppController = (function() {

	var $configAccordion;
	var $stateAccordion;
	var $messgeAccordion;
	var $configAccordionTitle;

	function init() {
		
		LoadingDimmer.init();
		LoadingDimmer.show();
		
		LiveUTC.init();
		LiveUTC.startTimer();
		
		DataService.init();
		
		serverState = DataService.getServerState();
		
		if (serverState == null) {
			LoadingDimmer.hide();
			ServerCommunicationDimmer.setMessage(communicationErrorMessage);
			ServerCommunicationDimmer.show();
			return;
		}

		setupWebsocketConnection();
		
		profileList = DataService.getSleProviders();
		
		initAllComponents();
		
		setupMenu();
		
		setupAccordianViews();
		
		MessageView.loadTable();
		
		LoadingDimmer.hide();
	}
	
	function initAllComponents() {
		
		//display_ct();
		//display_c();
		MessageView.init();
		SleServiceProvider.init();
		ChillConfig.init();
		ChillDownConfigModal.init();
		ProviderConfig.init();
		ErrorModal.init();
		
		ReturnProviderStateController.init();
		ForwardProviderStateController.init();
		ModIndexBitrateViewController.init();
		DownlinkStateController.init();
		UplinkStateController.init();
	}
	
	function setupMenu() {
		$('.menu .item').tab({
			history : false
		});
	}
	
	function setupAccordianViews() {
		
		$configAccordionTitle = $("#config-accortion-title");
		$configAccordion = $("#config-accordion").accordion({
			onOpen : function() {
				$configAccordionTitle.empty();
				$configAccordionTitle.append("<i class=\"dropdown icon\"></i>");
				$configAccordionTitle.append("Configuration");
			},

			onClose : function() {
				var returnProfileName = ProviderConfig.getSelectedReturnProvider();
				var forwardProfileName = ProviderConfig.getSelectedForwardProvider();

				$configAccordionTitle.empty();
				$configAccordionTitle.append("<i class=\"dropdown icon\"></i>");
				$configAccordionTitle.append("Configuration");

				if (returnProfileName !== "" || forwardProfileName !== "") {
					$configAccordionTitle.append("&nbsp;&nbsp;--&nbsp;&nbsp;");
				}

				if (returnProfileName !== "") {

					profileList.forEach(function(profile) {
						if (profile[SharedProfileProperty.PROFILE_NAME] === returnProfileName) {
							var returnHosts = profile[SharedProfileProperty.PROVIDER_HOSTS];

							$configAccordionTitle.append("Return Provider: " + returnHosts);
							$configAccordionTitle.append("&nbsp;&nbsp;&nbsp;&nbsp;");
						}
					});

				}
				if (forwardProfileName !== "") {
					profileList.forEach(function(profile) {
						if (profile[SharedProfileProperty.PROFILE_NAME] === forwardProfileName) {
							var forwardHosts = profile[SharedProfileProperty.PROVIDER_HOSTS];
							$configAccordionTitle.append("Forward Provider: " + forwardHosts);
						}
					});
				}
			}
		});

		$configAccordion.accordion('open', 0);
		$stateAccordion = $("#state-accordion").accordion('open', 0);
		$messgeAccordion = $("#message-accordion").accordion('open', 0);		
	}
	
	function setupWebsocketConnection() {
		ServerConnectionIndicator.init();
		var worker = new Worker('resources/js/server-communication/websocket/websocket-controller.js');
		worker.onmessage = workerMessageHandler;
		worker.postMessage(appConfig);
	}

	function workerMessageHandler(workerMessage) {
		var messageData = workerMessage.data;
		switch (messageData["worker_message"]) {
		case "websocket_connection_opened":
			ServerConnectionIndicator.serverAvailable();
			break;
		case "websocket_connection_closed":
			ServerConnectionIndicator.serverNotAvailable();
			ServerCommunicationDimmer.setMessage(communicationErrorMessage);
			ServerCommunicationDimmer.show();
			break;
		case "received_websocket_message":
			var websocketMessage = messageData["websocket_message"];
			WebsocketMessageHandler.handleMessage(websocketMessage);
			break;
		default:
			break;
		}
		console.log(workerMessage);
	}

	return {
		init : init,
	}

})();