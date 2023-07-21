/**
 * Message View module handles all functionality associated with the Message
 * accordion on the Monitor and Control interface.
 * @namespace MessageView
 */
var MessageView = (function(){
	
	var $messageTable;
	var $scrollCheckbox;
	var messageList;
	var dataSet = [];
	var autoScroll = true;
	
	/**
	 * Initialize component
	 * @public
	 * @memberof! MessageView
	 */
	function init() {
		
		// Init the auto-scroll toggle checkbox
		$scrollCheckbox = $("#message-table-scroll-checkbox").checkbox({
			onChecked : function(){
				autoScroll = true;
				console.log("Autoscroll is checked");
			},
			
			onUnchecked : function(){
				autoScroll = false;
				console.log("Autoscroll is unchecked");
			}
			
		}).checkbox('check');
		
		// Init the message Datatable
		$messageTable = $("#message-table").DataTable({
	        data: dataSet,
	        columns: [
	            { title: "Time" },
	            { title: "Type" },
	            { title: "Message" }
	        ],
			"scrollY":        "400px",
			"sScrollX": "100%",
	        "scrollCollapse": true,
	        "paging":         false
		}).draw();
		
		// Subscribe to Log Message events
		EventBus.subscribe(EventTopic.WEBSOCKET_EVENT_MESSAGE, eventMessageHandler);
	}
	
	/**
	 * Handle incoming messages from the websocket connection.
	 * @param {Object} eventMessage - message object which contains a batch of 
	 * log events.
	 * @private
	 * @memberof! MessageView
	 */
	function eventMessageHandler(eventMessage) {
		var messageList = eventMessage["message_list"];		
		loadMessagesFromList(messageList);
	}
	
	/**
	 * Load log messages into the Datatable. Messages come in through the websocket in batches
	 * and are loaded into the Datatable in batches to optimize element redraw events and 
	 * prevent the user interface from becoming unresponsive. This method converts the messages
	 * into a format compatible for loading into the Datatable.
	 * @param {Array} messageList - list of log messages
	 * @memberof! MessageView
	 * @private
	 */
	function loadMessagesFromList(messageList) {
		var newMessageList = [];
		
		var messageListSize = messageList.length;
		var numOfRecordsToRemove = 0;
		if (messageListSize > 1000) {
			numOfRecordsToRemove = messageListSize - 1000;
			messageList.splice(0, numOfRecordsToRemove);
		}
		
		messageList.forEach(function(message){
			var newMessageRow = [];
			newMessageRow.push(message.t);
			newMessageRow.push(message.l);
			newMessageRow.push(message.m);
			newMessageList.push(newMessageRow);
		});
		
		addNewDataList(newMessageList);
	}
	
	/**
	 * Add new row of data to the Datatable
	 * @private
	 * @memberof! MessageView
	 */
	function addNewDataRow(messageRow) {
		$messageTable.row.add(messageRow).draw( false );
		//$('.dataTables_scrollBody').scrollTop($('.dataTables_scrollBody')[0].scrollHeight);
	}
	
	/**
	 * Add a batch of messages to the Datatable. This method adds multiple rows to the 
	 * end of the Datatable and updates scroll position based on the auto-scroll toggle.
	 * @param {Array} messageList - list of messages formatted to be inserted into the Datatable
	 * @private
	 * @function
	 * @memberof! MessageView
	 */
	function addNewDataList(messageList) {
		var recordList = $messageTable.data();
		var numOfRecords = $messageTable.data().length;
		var numOfRecordsIncludingNewList = numOfRecords + messageList.length;
		var extraRecordCount;
		if (numOfRecordsIncludingNewList > 1000) {
			extraRecordCount = numOfRecordsIncludingNewList - 1000;
		}
		
		for (var i = 0; i < extraRecordCount; i++) {
			$messageTable.row(i).remove();
		}
		
		$messageTable.rows.add(messageList).draw( false );
		
		if (autoScroll) {
			$('.dataTables_scrollBody').scrollTop($('.dataTables_scrollBody')[0].scrollHeight);
		}
	}
	
	/**
	 * Load initial set of messages returned from the server into the Datatable.
	 * This method gets called at initialization time. 
	 * @public
	 * @function loadTable
	 * @memberof! MessageView
	 */
	function loadTable() {
		messageList = DataService.getMessages();
		loadMessagesFromList(messageList);
	}
	
	return {
		init: init,
		loadTable: loadTable,
	}
	
})();