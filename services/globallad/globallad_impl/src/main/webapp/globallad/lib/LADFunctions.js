
/* cmp
* @a - first tree item
* @b - second tree item
*
* @return - 1 if a is greater than b, -1 if b is greater, 0 if they are equal
*
* Function for sorting children nodes. Especially important for identifier level values, like channel IDs
*/
var cmp = function(a, b) {
	a = a.title.toLowerCase();
	b = b.title.toLowerCase();
	return a > b ? 1 : a < b ? -1 : 0;
};

/*Jquery AJAX delete wrapper function.
* @url - URL to send the request to.
* @data - data to be sent to the server as part of the request, can (and often is) empty.
* @type - equivalent to header value of "Content-Type", which specifies the type of data in data. Can, and normally is, left empty. Will set to default.
*
* @return - returns the Jquery $.ajax structure formatted for a delete operation.
*/
function AJAXdelete(url, data, type){
 
	return $.ajax({
		url: url,
		type: 'DELETE',
		data: data,
		contentType: type
	});
}



/*Jquery AJAX options wrapper function.
* @url - URL to send the request to.
* @data - data to be sent to the server as part of the request, can (and often is) empty.
* @type - equivalent to header value of "Content-Type", which specifies the type of data in data. Can, and normally is, left empty. Will set to default.
*
* @return - returns the Jquery $.ajax structure formatted for an options operation.
*/
function AJAXoptions(url, data, type){
 
	return $.ajax({
		url: url,
		type: 'OPTIONS',
		data: data,
		contentType: type
	});
}

function AJAXget(url){

	return $.ajax({
		url: url,
		type: 'GET'
	});
}

/*Jquery AJAX put wrapper function.
* @url - URL to send the request to.
* @data - data to be sent to the server as part of the request.
* @type - equivalent to header value of "Content-Type", which specifies the type of data in data. By default, will be set to text/plain.
*
* @return - returns the Jquery $.ajax structure formatted for a put operation.
*/
function AJAXput(url, data, type){
	if(typeof type === 'undefined'){
		type = 'text/plain';
	}
 
	return $.ajax({
		type: 'PUT',
		data: data,
		url: url,
		contentType: type
	});
}


function logEvent(event, data, msg){
	//        var args = $.isArray(args) ? args.join(", ") :
	msg = msg ? ": " + msg : "";
	$.ui.fancytree.info("Event('" + event.type + "', node=" + data.node + ")" + msg);
}

/* createLADTree
* NO input
*
* NO return - a dynatree object that is placed into the id "tree" div of the page in which it is invoked, currently only data.html
*
* A dynatree object is created and attached to the id "tree" div of the page, currently only data.html. This tree is currently coded to ONLY utilize this div
* and ONLY the information returned that represents the LAD tree. The events/attributes specified below are overriden from their defaults. All others are still
* default, which is for no operation
*/
//use the dynatree javascript to create the base tree
function createLADTree(){
	// Attach the dynatree widget to an existing <div id="tree"> element
	// and pass the tree options as an argument to the dynatree() function:
	
	$("#tree").fancytree({
		
		//when the folder or name is clicked, just select the folder, don't expand it (default is 3 - activate and expand)
		clickFolderMode: 4,
		autoScroll: true,
		
		//start with just the root node of "LAD"
		source: [ // Pass an array of nodes.
			{title: "LAD", key:"", class: "", folder: true, lazy: true}
		],
		
		//function to do when a node is activated (selected/highlighted)
		activate: function(event, data){
			//request the info for the node and display it
			displayInfo(data.node);
		},
		
		//what to do when a node is collapsed
		collapse: function(event, data){
		
			// MPCS-7980 02/09/16 - make sure we only reset the lazy nodes now that there are non-lazy ones as well
			if(data.node.isLazy()){
				data.node.resetLazy();
			}
		},
		
		
		//when a lazy node (all but identifier nodes) is expanded, this method is called
		lazyLoad: function(event, data){
			//get the JSON of data, then process it before giving it to the data.result function
			data.result = $.getJSON(getUrl('params', data.node)).then(function(returnData){
				return growTree(data.node, returnData);
			});
		},
		loadError: function(event, data) {
			logEvent(event, data);
		},
		focus: function(event, data){
        	data.node.scrollIntoView(true);
      	},
	});
	
}



/* disconnectClients
* NO input parameters
*
* NO return data
*
* Called by the "Disconnect Clients" button on settings.html. Verifies with the user that they want to disconnect all clients.
* If confirmes, attempts to disconnect all clients. User is informed of the result
*/
function disconnectClients(){
	if(confirm("Are you sure you want to disconnect ALL clients attached to the Global LAD?")){

		var dcUrl = host() + 'server';
		AJAXdelete(dcUrl)
		.done(function(reutrnData, returnTextStatus, returnXhr){
			updateStatus("All clients disconnected!");
		})
		.fail(function(reutrnData, returnTextStatus, returnXhr){
			updateStatus("Unknown error disconnecting clients.<br>" + returnXhr.status + ' ' + returnTextStatus);
		});
	}
}



/* displayInfo
* @node - node from Dynatree
*
* NO return - Any results are posted to the info section of the page hosting this call, currently only the data.html
*
* The use of 'info' with getUrl informs it that the second parameter is data from Dynatree and a URL for getting info about this node is returned.
* $.getJSON is a Jquery function that uses the AJAX "get", but a JSON is known to be returned. If the request is successful, then a table containing the data is placed
* in the info div. If unsuccessful, the user is notified in the info div
*/
//fill the info div of data.html with a table with the info/stats of the currently selected item in the tree
function displayInfo(node){
	//generate the URL
	var infoUrl = getUrl('info', node);
	
	//get the data
	$.getJSON(infoUrl)
	.done(function(infoJSON){
		//create the table and insert it into the defined location
		createInfoTable(infoJSON,"info");
	})
	.fail(function(returnData){
		$("#info").html("<p>Could not retrieve selected stats!</p>");
		$("#info").append("<p>Address attempted: " + infoUrl + "</p>");
		$("#info").append("<p> Server response: " + returnData.toString() + "</p><p>Response data: " + JSON.stringify(returnData) + "</p>");
	});
}


/* downloadLADData
* NO input
*
* NO return
*
* An unanchored element with the 'a' tag is created with a reference URL of the dump address. It is then "clicked", which causes the browser to download the file
*/
//****In safari the file downloaded will always be titled 'dump'****//

function downloadLADData(){
	//generate the URL
	var LADUrl = host() + 'dump',
	
	//create the element
	a = document.createElement('a');
	//assign properties to it
	a.download = "dump.json";
	a.href = LADUrl;
	//"click" the item and download the database
	a.click();
}


/* getLADStats
* NO input
*
* NO return - all data is "output" through a table in the stats div
*
* The stats URL is constructed and a JSON is requested from the server. If successful, a table is created in the stats div (of the info.html page)
* If unsuccessful, the user is notified through the same stats div.
*/

function getLADStats(){
	//generate URL
    var statsJSONLocation = host() + 'server/';

	//get the JSON holding the stats, but alert the user if it couldn't load
    $.getJSON(statsJSONLocation)
    .done(function(statsJSON){
		createStatsTables(statsJSON, 'stats');
    })
    .fail(function(settingsJSON){
		("#stats").html("<br><br><b>Could not load server stats.</b>");
    });
}



/* getLADStatus
* NO input
*
* NO return - all data is "output" through a div utilized on multiple pages
*
* The base address is utilized as a "ping" address. If an OK response (2XX) is received and data was retuned, then the server is good. If failed (any code not 2XX),
* the value is further checked and the status is set accordingly.
*/
function getLADStatus(){
	//address in place that responds with a basic string response
        var location = host();
        var statCell = document.getElementById("statusCell");

	//check it. Depending on the response, or lack thereof, the element is set appropriately
        $.get(location)
        //server response is OK
        .done(function(returnData){
        		//don't care what the return data is, but it should return something
                if(JSON.stringify(returnData).length > 0){
                        statCell.style.backgroundColor = '#00CC00';
                        statCell.innerHTML = 'UP';
                }
                //something's wrong
                else{
                        statCell.style.backgroundColor = 'yellow';
                        statCell.innerHTML = 'UNEXPECTED REPONSE';
                }
        })
        //no or bad response
        .fail(function(reutrnData, returnTextStatus, returnXhr){
        		//no response means it's down
                if(returnXhr.status === 0 || returnXhr.status === undefined){
	                statCell.style.backgroundColor = 'red';
	                statCell.innerHTML = 'DOWN';
	            }
	            //bad response - notify!
	            else{
	            	statCell.style.backgroundColor = 'yellow';
                    statCell.innerHTML = 'UNEXPECTED ' + returnXhr.status;
	            }
        });
}


/* getUDTTitle
* @titleNumber - number received from the server that maps to a User Data Type
*
* @return - string that specifies the User Data Type passed in
*
* On the server, User Data Type (UDT) is stored by mapping the 8 current values to 0-7 byte values.
* Since this list is very short, a simple switch is used to pick the correct one and return it.
*/
function getUDTTitle(titleNumber){

	var titleString = '';

	//since it's a short list currently, just using a switch to get the proper value
	switch(titleNumber){
		case "0":
			titleString = 'FSW Channel Value Real Time';
			break;
		case "1":
			titleString = 'FSW Channel Value Recorded';
			break;
		case "2":
			titleString = 'Header Channel Value';
			break;
		case "3":
			titleString = 'Monitor Channel Value';
			break;
		case "4":
			titleString = 'SSE Channel Value';
			break;
		case "5":
			titleString = 'FSW EVR Real Time';
			break;
		case "6":
			titleString = 'FSW EVR Recorded';
			break;
		case "7":
			titleString = 'SSE EVR Real Time';
			break;
		default:
			alert("A data type could not be found!\nUDT: " + titleNumber + ' ' + typeof titleNumber);
			//still return something
			titleString = 'UNDEFINED - ' + titleNumber;
	}
	return titleString;
}

/* getUrl
* @type - tell the function which type of URL is necessary
* @data - arguments to be passed to the sub function
*
* @return - string composing the requested URL with parameters
*
* Calls the sub function that constructs the proper URL. these can probably be condensed into this single function.
*/
function getUrl(type, data){

	//placeholder, by default at least we'll return an empty string
	var returnString = '';

	//get the URL depending on the type
	switch(type){
		case 'info':
		case 'params':
			returnString = getUrlTree(type, data);
			break;
		case 'getData':
			returnString = getUrlGetData(data);
			break;
		case'insertData':
			returnString = getUrlInsertData(data);
			break;
		case 'deleteData':
			returnString = getUrlDeleteData(data);
			break;
		case 'LADDump':
			returnString = host() + 'binary/dump';
			break;
		default:
			//add anything for default?
	}

	return returnString;
}

/* getUrlDeleteData
* @optionsBoolean - true/false. wether or not the optional query parameters are to be included
*
* @return - the constructed URL as a string
*
* First, the base URL is constructed from the fixed values that are ALWAYS present and must be included.
* Depending on true/false state passed in the optional parameters are added.
*/
function getUrlDeleteData(optionsBoolean){
	//construct base URL for delete
	var returnString = host();
	returnString += document.getElementById("deleteDataQType").value + '/';
	returnString += document.getElementById("deleteDataDataSource").value + '/';
	returnString += document.getElementById("deleteDataRecordState").value;
	
	//add the parameters?
	if(optionsBoolean){
		//make sure the table exists before we continue, that way the function always completes & returns
		var optionsTable = document.getElementById("deleteDataQueryOptionsTable");
		
		if(optionsTable !== null){
			//get all of the input items
			var optionsArr = optionsTable.getElementsByTagName("input"),
			tempString = '';
			//the ? signifies to the server that query params are to follow
			returnString += '?';

			//for each input text string
			for(var i = 0 ; i < optionsArr.length ; i++){
				//strip spaces
				tempString = optionsArr[i].value.replace(/ /g,"");
				//trim leading and trailing commas
				tempString = tempString.replace(/(^,)|(,$)/g, "");

				//check to see if there's anything worthwhile left
				if(tempString !== ''){
					//insert first ID and replace all commas with &ID= and place the corrected parameters onto the URL
					returnString += "&" + optionsArr[i].id + "=" + tempString.replace(/,/g, "&" + optionsArr[i].id + "=");
				}
				//done with this item
			}
		}
		//done with options
	}
	return returnString;
}


/* getUrlGetData
* @optionsBoolean - true/false. whether or not the optional query parameters are to be included
*
* @return - the constructed URL as a string
*
* First, the base URL is constructed from the fixed values that are ALWAYS present and must be included.
* Depending on true/false state passed in the optional parameters are added.
*/
function getUrlGetData(optionsBoolean){
	//make base URL
	var returnString = host();
	returnString += document.getElementById("getDataQType").value + '/';
	returnString += document.getElementById("getDataDataSource").value + '/';
	returnString += document.getElementById("getDataRecordState").value + '/';
	returnString += document.getElementById("getDataTimeType").value + '/';

	//add optional params?
	if(optionsBoolean){
		//get the table		
		var optionsTable = document.getElementById("getDataQueryOptionsTable");
		
		//verify table exists before inserting so this function always returns
		if(optionsTable !== null){
			//get all input fields
			var optionsArr = optionsTable.getElementsByTagName("input"),
			tempString = '';

			returnString += '?';
			//format and add each item
			for(var i = 0 ; i < optionsArr.length ; i++){
				if(optionsArr[i].type === 'text'){
					//strip spaces
					tempString = optionsArr[i].value.replace(/ /g,"");
					//trim leading and trailing commas
					tempString = tempString.replace(/(^,)|(,$)/g, "");
					//replace all commas with &ID=
					tempString = tempString.replace(/,/g, "&" + optionsArr[i].id + "=");
				}
				else if(optionsArr[i].type === 'checkbox'){
					tempString = optionsArr[i].checked;
				}
				else if(optionsArr[i].type === 'number'){
					tempString = optionsArr[i].value;
				}

				//check to see if there's anything worthwhile left
				if(tempString !== ''){
					//insert first ID and place the corrected parameters onto the URL
					returnString += "&" + optionsArr[i].id + "=" + tempString;
					tempString = '';
				}
				//done with this item
			}
		}
		//done with options
	}
	
	return returnString;
}

/* getUrlInsertData
* @optionsBoolean - true/false. wether or not the optional query parameters are to be included
*
* @return - the constructed URL as a string
*
* First, the base URL is constructed from the fixed values that are ALWAYS present and must be included.
* Depending on true/false state passed in the optional parameters are added.
*/
function getUrlInsertData(optionsBoolean){
	//base URL
	var returnString = host();
	returnString += document.getElementById("insertDataQType").value + '/';
	returnString += document.getElementById("insertDataDataSource").value + '/';
	returnString += document.getElementById("insertDataRecordState").value + '/';
	// MPCS-8017  03/09/16 - no longer adding string 'timeType/' + timeType + '/' to url
	returnString += 'insert';
	
	//add optional params?
	if(optionsBoolean){
		var optionsTable = document.getElementById("insertDataQueryOptionsTable");

		//verify table exists before pulling items so it always returns
		if(optionsTable !== null){
			var optionsArr = optionsTable.getElementsByTagName("input"),
			tempString = '';

			returnString += '?';

			for(var i = 0 ; i < optionsArr.length ; i++){
				//strip spaces
				tempString = optionsArr[i].value.replace(/ /g,"");
				//trim leading and trailing commas
				tempString = tempString.replace(/(^,)|(,$)/g, "");

				//check to see if there's anything worthwhile left
				if(tempString !== ''){
					//insert first ID and replace all commas with &ID= and place the corrected parameters onto the URL
					returnString += "&" + optionsArr[i].id + "=" + tempString.replace(/,/g, "&" + optionsArr[i].id + "=");
				}
				//done with this item
			}
		}
		//done with options
	}
	
	return returnString;
}

/* getUrlTree
* @type - params or info - a tree URL is almost the same except params requires a query parameter of isSummary=true
* @ node - node for which data is being requested 
*
* @return - the constructed URL as a string
*
* The node's class is used to add to the URL string. All classes except for 'userDataType' are utilized by
* constructing a query parameter consisting of the class and title of the node. If the node's class is 'userDataType',
* the UDT value, which is only present in this class node, is used to determine part of the URL string.
* The base address and address string from the user data type are combined.
* If this is a parameter URL and not an info, then the query parameter of op=true is added.
* All query parameters that came from the tree are added.
* Finally, the address string is returned.
*/
//It is used to either get the next level info ('params') or statistical info ('info')
function getUrlTree(type, node){
	//the two parts that need to be calculated are the extension (addrString) and the query parameters (paramString)
	var paramString = '',
	addrString = '',
	returnString = '',
	//starting at the current node
	parNode = node;
	
	
	//go until we bubble up to the root
	while(parNode.getLevel() > 1){
		//the user data type is currently the only portion that translates to the addrString
		if(parNode.data.class === 'userDataType'){
			switch(parNode.data.UDT){
				case "0":
					addrString = 'eha/fsw/realtime/';
					break;
				case "1":
					addrString = 'eha/fsw/recorded/';
					break;
				case "2":
					addrString = 'eha/header/realtime/';
					break;
				case "3":
					addrString = 'eha/monitor/realtime/';
					break;
				case "4":
					addrString = 'eha/sse/realtime/';
					break;
				case "5":
					addrString = 'evr/fsw/realtime/';
					break;
				case "6":
					addrString = 'evr/fsw/recorded/';
					break;
				case "7":
					addrString = 'evr/sse/realtime/';
					break;
				default:
					alert("we didn't match a user data type!\nUDT: " + parNode.data.UDT);
			}
		}
		//all other types of nodes are just query parameters
		else{
			paramString = '&' + parNode.data.class + '=' + parNode.data.value + paramString;
		}
		
		//bubble up to next level
		parNode = parNode.getParent();
	}
	
	//combine the pieces. base string plus ? for query parameters
	returnString = host() + addrString + '?';

	//MPCS-8051 03/22/16 - query parameter op was renamed to isSummary. Changed it here as well.
	//isSummary=true must be included first
	if(type === 'params'){
		returnString += '&isSummary=true';
	}

	//then the rest of the query parameters
	returnString += paramString;
	
	return returnString;
}

/* growTree
* @growNode - node on the dynatree that has been expanded
*
* NO return
*
* This function is called when a dynatree folder node that is "lazy" (all non-identifier nodes) is expanded.
* The URL to get this node's children info is constructed and is then used to get info.
* The returned JSON is known to have a "children" key, which will have an array consisting of basic information of its children.
* If a child's containerType is identifier, then it is a leaf node and WILL NOT have children, but all others potentially have children.
* All non-leaf nodes are treated as potentially having children and are therefore constructed as lazy nodes.
* After the array of children nodes is populated, this list is sorted and pushed to the node.
*
* If a folder has no children, or is unable to be loaded, a single child with this information is displayed to the user.
*/
//function to handle growing the tree from a particular node
function growTree(growNode, growData){

	//calculate the URL necessary
	var growUrl = getUrl('params', growNode),
	//hold the list of children. this is much faster than adding them individually
	children = [],
	//shortcut to make the code slightly easier to read
	childArr = growData["children"],
	titleString = '';
	
	if(childArr === undefined || childArr.length === 0){
		children.push({title: "No Data - Reload again later", key:growNode.key + "+" + String(0)});
		return children;
	}
	// MPCS-7980  - 02/10/16 - added check to be able to put a long channel list into subfolders
	//only channel values should get this long, but let's validate anyway
	else if(childArr.length > 100 && childArr[0]["containerType"] === 'identifier' && /[A-Z]-\d/.test(childArr[0]["containerIdentifier"])){
	
		return createChannelFolders(childArr, growNode);
	}

	//each item in the result set needs to be made into a child
	//the type is stored as class for dynamic URL construction and future post processing
	for(var jKey = 0 ; jKey < childArr.length ; jKey ++){
		
		//currently assuming 'identifier' types are definitively leaves, but this can change
		if(childArr[jKey]["containerType"] === 'identifier'){
			//if it matches the pattern, then it's a channel ID/EHA (done to save time bubbling up to find user data type parent to tell if it's EHA or EVR 
			if(/[A-Z]-\d/.test(childArr[jKey]["containerIdentifier"])){
				children.push({title: childArr[jKey]["containerIdentifier"], key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: "channelId", value:childArr[jKey]["containerIdentifier"]});
			}
			//otherwise it's an EVR
			else{
				children.push({title: childArr[jKey]["containerIdentifier"], key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: "evrLevel", value:childArr[jKey]["containerIdentifier"]});
			}
		}
		
		//since user data types are mapped, the name shown needs to be changed from the number returned to the readable name
		//additionally, still store this number away for any later processing/evaluation since this is less likely to change than the name
		else if(childArr[jKey]["containerType"] === 'userDataType'){
			titleString = childArr[jKey]["containerType"].replace(/([a-z])([A-Z])/g, '$1 $2');
			titleString = titleString.charAt(0).toUpperCase() + titleString.slice(1) + ': ' + getUDTTitle(childArr[jKey]["containerIdentifier"])
			children.push({title: titleString, key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: childArr[jKey]["containerType"], value:childArr[jKey]["containerIdentifier"], UDT:childArr[jKey]["containerIdentifier"], folder: true, lazy: true});
		}
		//most returned children are going to have children of their own, make them lazy nodes so this process can be continued down the line
		else{
			titleString = childArr[jKey]["containerType"].replace(/([a-z])([A-Z])/g, '$1 $2');
			titleString = titleString.charAt(0).toUpperCase() + titleString.slice(1) + ': ' + childArr[jKey]["containerIdentifier"];
			children.push({title: titleString, key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: childArr[jKey]["containerType"], value:childArr[jKey]["containerIdentifier"], folder: true, lazy: true});
		}
	}
	
	//sort before insertion. A lot faster than sorting afterwards, especially for a large array
	//
	// Lavin Zhang May 17 2016 MPCS-8124
	// Perform special sorting for parent nodes
	// where the titleString "*: \d+"
	if (!isNaN(parseInt(childArr[0]["containerIdentifier"])) && isFinite(childArr[0]["containerIdentifier"])) {
		children.sort(function(a, b) {
			a = parseInt(a.title.split(": ")[1]);
			b = parseInt(b.title.split(": ")[1]);
			if (!isNaN(a) && isFinite(a) &&  !isNaN(b) && isFinite(b)) {
				return a > b ? 1 : a < b ? -1 : 0;
			} else {
				return -1;
			}
		});
	}
	else {
		children.sort(cmp);
	}
	
	//add the children array to the current node
	//growNode.addChild(children);
	return children;
}

/*createChannelFolders
* @childArr - set of items to be added as children to the growNode
*
* @growNode - node that all items in the childArr will be appended
*
* @return - array of folders containing all of the children in childArr, organized properly for insertion into a fancytree object
*
* Used within growTree if a set of channelId leaves is excessively large. Because the channels are
* in timestamp order, not alphanumeric, the values are first sorted into folders based upon their
* alphabetic prefix (eg: A, B, ACM, THRM, etc.). The values in these folders are then sorted and if this list
* is still excessively long, then they are further subdivided into folders of a fixed size.
* Folders created in this method are specifically created as NOT lazy because all of this data is returned
* all at once.
*/
function createChannelFolders(childArr, growNode){

	var children = [];
	//for each of the items either add it to a current folder OR create a new folder for it
	for(var jKey = 0 ; jKey < childArr.length ; jKey++){

		var added = false;
		var shortName = childArr[jKey]["containerIdentifier"].match(/[A-Z]+/);
		
		for(var kKey in children){

			var testVal = (shortName + "...");
			//if a folder exists for it already, then put it there and we're done
			if( children[kKey].title === testVal){
			
				children[kKey]["children"].push({title: childArr[jKey]["containerIdentifier"], key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: "channelId", value:childArr[jKey]["containerIdentifier"]});
				
				added = true;
				break;
			}
		
		}

		//if a folder for its type doesn't exist, then we need to create it and insert this item as its first element
		if(added === false){

				var tempItem = {title: shortName + "...", key: growNode.key + "+" + shortName + "-folder", class: "identFolder", value: shortName, folder: true, lazy: false, children: []};

				tempItem["children"].push({title: childArr[jKey]["containerIdentifier"], key: growNode.key + "+" + childArr[jKey]["containerIdentifier"], class: "channelId", value:childArr[jKey]["containerIdentifier"]});
			
				children.push(tempItem);
		}

	}
	
	//fancytree won't sort it, gotta do it ourselves
	
	//sort children first, since we probably need to further subdivide the data
	for(var kKey in children){
	
		children[kKey]["children"].sort(cmp);
		
		if(children[kKey]["children"].length > 100){
		
			//holder array for folders that will hold the children
			var newChildren = [];	
			var perFolder = 25;
			var subFolder, sliceMax, tempItems;
			
			//create a folder, add the specified number of items to it
			//set these as the folder's children
			//add this folder to the list
			for(var i = 0; i < children[kKey]["children"].length ; i += perFolder){
			
				sliceMax = Math.min(i + perFolder, children[kKey]["children"].length);
				
				tempItems = children[kKey]["children"].slice(i,sliceMax);
				
				subFolder = {title: tempItems[0].title + "..." + tempItems[tempItems.length - 1].title, key: children[kKey].title + tempItems[0].title + "-folder", class: "identFolder", value: tempItems[0]["containerIdentifier"], folder: true, lazy: false, children: []};
				
			
				subFolder["children"] = tempItems;
			
				newChildren.push(subFolder);
				
			}
			
			//put this list as the new children
			children[kKey]["children"] = newChildren;
		
		}
	
	}
	
	children.sort(cmp);
	
	return children;
}

/* showDataOptions
* @selected - id of the div to be toggled (string)
*
* NO return
*
* Hides non-selected divs and toggles the display of the selected div. Expands/shrinks the response area div
* depending on the status of the selected div visibility.
*/
function showDataOptions(selected){
	//setup a string containing the specified option's div ID in jquery compatible form
	var selectedId = '#' + selected + 'Options';

	//hide the non-selected options
	if(selected !== 'getData'){
		$("#getDataOptions").hide();
		if($("#getDataButton").hasClass('dataButtonSelected')){
			$("#getDataButton").attr('class', 'dataButton');
			$("#getDataButton").attr('value', 'Get Data');
		}
	}
	if(selected !== 'insertData'){
		$("#insertDataOptions").hide();
		if($("#insertDataButton").hasClass('dataButtonSelected')){
			$("#insertDataButton").attr('class', 'dataButton');
			$("#insertDataButton").attr('value', 'Insert Data');
		}
	}
	if(selected !== 'deleteData'){
		$("#deleteDataOptions").hide();
		if($("#deleteDataButton").hasClass('dataButtonSelected')){
			$("#deleteDataButton").attr('class', 'dataButton');
			$("#deleteDataButton").attr('value', 'Delete Data');
		}
	}
	//toggle display of the selected div
	$(selectedId).toggle();
	
	//if an option is visible, make bigger and change button 
	if($(selectedId).is(':visible')){
		$('#options').height( '45%');
		$('#' + selected + 'Button').attr('class', 'dataButtonSelected');
		$('#' + selected + 'Button').attr('value', 'Hide Options');
	}
	//otherwise make small
	else{
		$('#options').height( '27px');
		$("#" + selected + "Button").attr('class', 'dataButton');
		$('#' + selected + 'Button').attr('value', selected.charAt(0).toUpperCase() + selected.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1));
	}
	//remaining area to response area
	$('#responseArea').height( $('#responseArea').parent().height() - $('#options').height()-1);
	//fix the view
	$('#data').width(($("#body").width()-$("#leftPane").width())-6);
	$('#' + selected + 'FixedOptions').height($('#options').height()-65);
}

/* showDataOptions
* @selected - id of the div to be toggled (string)
*
* NO return
*
* Hides non-selected divs and toggles the display of the selected div. Expands/shrinks the response area div
* depending on the status of the selected div visibility.
*/
function showHelpInfo(selected){
	//setup a string containing the specified option's div ID in jquery compatible form
	var selectedId = '#' + selected + 'Help';

	//hide the non-selected options
	if(selected !== 'data'){
		$("#dataHelp").hide();
		if($("#dataHelpButton").hasClass('dataButtonSelected')){
			$("#dataHelpButton").attr('class', 'dataButton');
		}
	}
	if(selected !== 'settings'){
		$("#settingsHelp").hide();
		if($("#settingsHelpButton").hasClass('dataButtonSelected')){
			$("#settingsHelpButton").attr('class', 'dataButton');
		}
	}
	if(selected !== 'info'){
		$("#infoHelp").hide();
		if($("#infoHelpButton").hasClass('dataButtonSelected')){
			$("#infoHelpButton").attr('class', 'dataButton');
		}
	}
	//toggle display of the selected div
	$(selectedId).show();
	
	//if an option is visible, make bigger and change button 
	if($(selectedId).is(':visible')){
		$('#' + selected + 'HelpButton').attr('class', 'dataButtonSelected');
	}
	//otherwise make small
	else{
		$("#" + selected + "HelpButton").attr('class', 'dataButton');
	}
}

function getQueryOptions(type) {
	var queryUrl = host() + "v2/api-docs";
	AJAXget(queryUrl)
	.done(function(apiOptions){
		//generate the query parameters table

		// MPCS-11704 - Shakeh Brys - 04/20/20
		// get correct path from swagger api doc to sort through available parameters
		var paths = apiOptions.paths;
		if (type == "getData") {
			generateQueryOptions('getData', paths["/{queryType}/{source}/{recordedState}/{timeType}"].get);
		} else if (type == "deleteData") {
			generateQueryOptions('deleteData', paths["/{queryType}/{source}/{recordedState}"].delete);
		}

	})
	.fail(function(returnedData){
		console.log("unable to obtain query options");
	});
}

/* loadQueryOptions
* @type - specifies the type of URL to be constructed and where the form/table will be placed after the parameters are received
*
* NO return
*
* When an AJAX call of OPTIONS is performed with a given URL, the data returned is the XML that represents the function that
* the URL services. Within it are the optional query parameters that can be added to any call made to that address.
*/
function loadQueryOptions(type){
	if (type != 'insertData'){
		getQueryOptions(type);
	}

	//special handling below
	if(type === 'insertData'){
		//since the JSON that will be constructed and sent has certain values that are dependent upon if it's for eha or evr data,
		//show the ones necessary and hide the others.
		if(document.getElementById("insertDataQType").value === "eha"){
			$("#insertDataJSONFieldsEha").show();
			$("#insertDataJSONFieldsEvr").hide();
		}
		else{
			$("#insertDataJSONFieldsEha").hide();
			$("#insertDataJSONFieldsEvr").show();
		}
	}
}



/* displayDataCurl
* @type - used to select the data specific to the functionality of the curl requested
* @location - spot on the page in which to place the data
*
* NO return - data is already inserted into the user's view
*
* The same URL that is utilized to get data to be displayed is obtained and prefaced with the correct information to allow the 
* URL to be used as a terminal curl command.
* All ampersand (&) characters are prefaced with a backslash (\) so they will be interpreted by the terminal properly when the line is entered.
* Currently, since this has been built and tested with local files both the plain text and button are placed on the screen since the button will not function properly until the files are served on the host.
* A button is also added with the same text and zerocliboard is utilized to copy that data to the clipboard.
*/
function displayCurl(type,location){
	//curl prefix, signify type of command is next
	var curlUrl = 'curl -X ',
	otherData; //currently only used by insert data's JSON information

	//switch based on the type. handle anything specific to the type
	switch(type){
		case 'getData':
			curlUrl += 'GET';
			break;
		case 'insertData':
			curlUrl += 'PUT';
			
			//get and stringify the data to be inserted
			otherData = JSON.stringify(createInsertDataJSON());

			//if the JSON is empty, then the curl won't be accepted anyway, only other early abort case
			if(otherData === '{}'){
				document.getElementById(location).innerHTML = '<p>Please fill in all required fields and try again.</p>';
				return;
			}
			
			//the JSON must be encapsulated with single quote marks. the -d flag signifies data
			//The data type needs to be overriten as application/json, otherwise it will return a 415 error. Add this flag info as well
			curlUrl += " -d '" + otherData + "'" + ' -H "Content-Type:application/json"';
			
			break;
		case 'deleteData':
			curlUrl += 'DELETE';
			break;
		default:
		console.log("unknown type in displayCurl, aborting function");
		return;
	}
	
	//MPCS-8122 - removed escaping of ampersands, now enclose url in quotation marks
	//add the URL with 
	curlUrl += ' -i "' + getUrl(type, true);
	
	curlUrl += '"';
	
	//display the command for the user so they can utilize it.
	document.getElementById(location).innerHTML = 'Copy the following line into a terminal window to get the requested data.<br><br>';
	document.getElementById(location).innerHTML += '<div id="urlBox">' + curlUrl + '</div><input type="button" value="Select curl text" onclick="selectTextById(' + "'urlBox'" + ')"/>';
	selectTextById('urlBox');
	
}



/* createInsertDataJSON
* NO input
*
* @return - Javascript object in JSON format that contains the properly formatted data to be inserted into the database,
*           or an empty object if the data was not complete.
*
* To insert data on the server, it must be contained within a JSON that has all of the necessary fields completed.
* The data for the JSON is contained in three fields - shared, eha-only, and evr-only. The JSON is constructed utilizing these
* fields with a set number of fixed keys and the user input values. either the eha or evr fields need to be filled, but not both.
* The eha field of "status" is only required if the dn type is "status".
* If all required fields have been filled, a constructed JSON is returned. Otherwise an empty JSON is returned. This way even if
* a function that called this one attempts to utilize the object there will not be unexpected results.
*/
function createInsertDataJSON(){

	var insertJSON = {}, //JSON object
	valid = true, //for validating all user fields were input
	JSONSet; //hold the sets of user input

	//get the shared fields
	JSONSet = document.getElementById("insertDataJSONFieldsShared").getElementsByTagName("input");
	
	//until all fields are exhausted, or an empty field is reached, move all of them to the JSON
	for(var i = 1 /*0 when eventTime is usable*/ ; (i < JSONSet.length) && valid ; i++){
		//if one is empty we're done
		if(JSONSet[i].value === ''){
			valid = false;
		}
		insertJSON[JSONSet[i].name] = JSONSet[i].value;
	}

	//if the input will be an EHA record use the eha set
	if(document.getElementById("insertDataQType").value === 'eha'){
		//get the set
		JSONSet = document.getElementById("insertDataJSONFieldsEha").getElementsByTagName("input");
	
		//the dnType has a tag type of "select", and therefore won't be caught by the above statement
		//insert it manually
		insertJSON["dnType"] = document.getElementById("insertDataDnType").value;
		
		//add the eha fields
		for(var i = 0 ; (i < JSONSet.length) && valid ; i++){
			//status is only required if dnType is status
			if(JSONSet[i].name === 'status'){
				if((JSONSet[i].value === '') && (document.getElementById("insertDataDnType").value === 'status')){
					valid = false;
				}
			}
			//all others need to have a value
			else if(JSONSet[i].value === ''){
				valid = false;
			}
			//a status key/value can be sent, even if the dn type isn't status. the server will just ignore it in parsing.
			insertJSON[JSONSet[i].name] = JSONSet[i].value;
		}
	}
	else{//all EVR inputs are required
		//get the set
		JSONSet = document.getElementById("insertDataJSONFieldsEvr").getElementsByTagName("input");
	
		for(var i = 0 ; (i < JSONSet.length) && valid ; i++){
			//all EVR inputs are required
			if(JSONSet[i].value === ''){
				valid = false;
			}
			insertJSON[JSONSet[i].name] = JSONSet[i].value;
		}
	}
	//if the input wasn't valid, no point in sending the partial JSON
	if(!valid){
		insertJSON = {};
	}
	
	return insertJSON;
}

/* Download
* @type - selects the name and address from which to download the file
*
* NO return
*
* When the user clicks the button to download in the get data view on data.html, an untethered a href item is created and "clicked".
* NOTE!!!!! The download attribute will work on most browsers and will name the file what is given, but Safari will ignore this attribute and will utilize the last segment of the URL
*           (not including query parameters) to name the file.
*/
function download(type){
	//create an a tag element
	var a = document.createElement('a');
	//label the download
	if(type === 'getData'){
		a.download = "data.json";
	}
	else if(type === 'LADDump'){
		a.download = "LAD_dump.bin";
	}

	a.href = getUrl(type, true);
	
	// MPCS-8052 03/22/16 - Append and remove the download item.
	//                          Firefox won't download if the a element isn't part of the page.
	//click this item and cause the download to start.
	document.body.appendChild(a);
	a.click();
	document.body.removeChild(a);
}

// MPCS-7808 01/20/16 - In getDataView function fixed references to channelIds - variable is channelId
/* getDataView
* NO input
*
* NO return - all data is added to the responseArea of the data.html
*
* Get the data requested by the user and display it in a table. Slightly more restrictive than downloading the data directly OR
* data that would be returned by the same curl command so the user does not have to wait a long time for a huge amount of data to
* be rendered in the browser or updated depending on what happens after displaying it.
*/
function getDataView(){
	//construct the URL
	var dataUrl = getUrl('getData', true),
	//check if the input is "ok" for display purposes
	validInput = true;
	
	//check to make sure the user isn't asking for EVERYTHING to be displayed. If so, it's no good.
	//there are ways around this, of course, but there are too many ways to check all of them.
	//check for wildcard only as last param
	// MPCS-7995 02/24/16 - fix references to evrLevel
	if(dataUrl.indexOf("channelId=*") === (dataUrl.length-11)){
		validInput = false;
	}
	//check for wildcard only as last param
	else if(dataUrl.indexOf("evrLevel=*") === (dataUrl.length-11)){
		validInput = false;
	}
	//check for wildcard only in the middle of the string
	else if ((dataUrl.indexOf("channelId=*&") !== -1) || (dataUrl.indexOf("evrLevel=*&") !== -1)){
		validInput = false;
	}
	//make sure some channel ID or EVR level has been specified
	else{
		validInput = false;
		if((dataUrl.indexOf("channelId") !== -1) || (dataUrl.indexOf("evrLevel") !== -1)){
			validInput = true;
		}
	}
	
	
	
	if(validInput){
		$.getJSON(dataUrl)
		.done(function(dataJSON){
			//flush and add the table.
			document.getElementById("responseArea").innerHTML = '';
			createDataTable(dataJSON,'#responseArea');
		})
		.fail(function(returnData){
			document.getElementById("responseArea").innerHTML = '<h2>Unable to process selected data</h2>';
			document.getElementById("responseArea").innerHTML += '<p><b>Status: </b>' + returnData.status + '\t-\t' + returnData.statusText + '</p>';
			document.getElementById("responseArea").innerHTML += '<p><b>Type: </b>' + typeof returnData + '</p>';
			document.getElementById("responseArea").innerHTML += '<p><b>Plain data</b></p>' + returnData + '<br>';
			document.getElementById("responseArea").innerHTML += '<p><b>JSON data</b></p>' + JSON.stringify(returnData) + '<br>';
		});
	}
	else{
		document.getElementById("responseArea").innerHTML = '<p>Invalid input for viewing.</p>';
		document.getElementById("responseArea").innerHTML += '<p>Please specify Channel Ids or EVR levels and try again.</p>';
		document.getElementById("responseArea").innerHTML += '<p>Examples of allowed input: A-0002, *-0002, A-000*, A-*2</p>';
	}
}


/* deleteData
* NO input
*
* NO return - data is displayed to the user through the responseArea div of the data.html page
*
* The URL with parameters to delete data is constructed and used to send a delete request to the server.
* The result of the response is given to the user
*/
function deleteData(){	
	var deleteUrl = getUrl('deleteData', true);
	
	//delete the specified data
	AJAXdelete(deleteUrl)
	//inform the user what the server replied
	.done(function(returnData){
		document.getElementById("responseArea").innerHTML = '<p><b>Data Deleted successfully!</b></p>';
	})
	//currently ALL delete requests will return "OK" (200), even if nothing was deleted
	.fail(function(returnData){
		document.getElementById("responseArea").innerHTML = '<p>Unable to retrieve selected data</p>';
		document.getElementById("responseArea").innerHTML += '<p><b>Type of data</b></p>' + typeof returnData + '<br>';
		document.getElementById("responseArea").innerHTML += '<p><b>Plain data</b></p>' + returnData + '<br>';
		document.getElementById("responseArea").innerHTML += '<p><b>JSON data</b></p>' + JSON.stringify(returnData) + '<br>';
		document.getElementById("responseArea").innerHTML += '<p><b>XML data</b></p>' + XMLSerializer().serializeToString(returnData); + '<br>';
		document.getElementById("responseArea").innerHTML += '<p><b>string data</b></p>' + String(returnData) + '<br>';
	});
}



/* insertData
* NO input
*
* NO return - data is displayed to the user through the responseArea div of the data.html page
*
* The JSON that potentially contains the data to be inserted is obtained. It is converted to a string because the AJAX request required the data
* to be in string format, however, the content type will tell the server what the data actually is.
* If the JSON is an empty object, then there was no data, or bad data, to be inserted and the insert is not attempted.
* Otherwise it's sent to the server as a PUT.
*/
function insertData(){
	var insertUrl = getUrl('insertData', true),
	//get the JSON and make it a string
	insertJSON = JSON.stringify(createInsertDataJSON());
	
	//if empty, then don't even send it
	if(insertJSON === '{}'){
		document.getElementById("responseArea").innerHTML = '<p>Please fill in all required fields and try again.</p>';
	}
	else{
		//PUT the JSON to the server with the proper insert data request parameters
		AJAXput(insertUrl,insertJSON, 'application/json')
		//notify the user depending on the result
		.done(function(){
			document.getElementById("responseArea").innerHTML = '<p>Data inserted successfully!</p>';
		})
		.fail(function(returnData){
			document.getElementById("responseArea").innerHTML = '<p>Unable to insert data</p>';
			document.getElementById("responseArea").innerHTML += '<p><b>Type of data</b></p>' + typeof returnData + '<br>';
			document.getElementById("responseArea").innerHTML += '<p><b>Plain data</b></p>' + returnData + '<br>';
			document.getElementById("responseArea").innerHTML += '<p><b>JSON data</b></p>' + JSON.stringify(returnData) + '<br>';
			document.getElementById("responseArea").innerHTML += '<p><b>XML data</b></p>' + XMLSerializer().serializeToString(returnData); + '<br>';
			document.getElementById("responseArea").innerHTML += '<p><b>string data</b></p>' + String(returnData) + '<br>';
		});
	}
	
	
}

/* updateDepth
* NO input - comes from the user select and input fields in settings.html
*
* NO return - user is notified of the result in the updateStatus field in settings.html
*
* The url for the specific depth to be updated is constructed. The values of the settingsDepthForm selector
*/
//function that is caled when the Update button is pressed.
//validates the input, prompts the user for verification, sends the change, reports if successful, and reloads the table
function updateDepth(){
	var updatePath = host() + document.getElementById('settingsDepthForm').value + 'depth';

	//the updateValue is the depth value to be changed
	var updateValue = document.getElementById('updateDepthValue').value;

	if(!(updateValue >= 1)){
		updateStatus('Invalid depth value selected');
		return false;
	}


	//validate before continuing
	if(confirm("Do you want to update the following information?\n" + document.getElementById('settingsDepthForm').options[document.getElementById('settingsDepthForm').selectedIndex].text + " = " + updateValue)){
		AJAXput(updatePath, updateValue)

		//if successful, let the user know and update the depth settings table
		.done(function(updateJSON){
			//populateSettingsTable(updateJSON);
			if(document.getElementById('settingsDepthForm').value === ''){
				createSettingsDepthTable();
			}
			else{
				populateSettingsDepthTable(updateJSON);
			}
			updateStatus('Depth Update successful');
			return false;
		})

		//if failed, let the user know and do not change anything
		.fail(function(){
			updateStatus('Depth update NOT successful!');
			return false;
		});
	}

	//if the user cancelled, just display a small note so they know it was not sent
	else{
		updateStatus('Depth update cancelled');
		return false;
	}	

	return false;
}



//wrapper function to set the status element's text
function updateStatus(updateStatusText){
	document.getElementById("updateStatus").innerHTML = updateStatusText;
}


/* uploadLADData
* NO input - comes from the user input file
*
* NO return - status of the upload is displayed to the user during the process
*
* The user specifies a file and then sends it up to the server. A synchronous send is required for the uploaded file.
* It is set up as a POST with the required parameters. The user is notified of the resulting status of the upload.
* The JSON containing the LAD data is NOT checked as this file could be VERY large and could take a long time to upload.
*/
//upload a JSON that contains LAD compatible data
function uploadLADData(){
	
	//prep info for the POST
	var fileData = $('#uploadFileSelect').get(0).files[0],
	uploadLoc = host() + 'binary/load',
	xhr;
	
	//since the file needs to be sent as application/octet-stream, we need to use an XMLHttpRequest to force the name
	try{
		xhr = new XMLHttpRequest(); //default method
	} catch (e){
		try{ //backup
			xhr = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e){ //uh oh!
			alert("Something went wrong with the upload!");
			console.log("unable to create xmlhttp request for full LAD upload");
			return false;
		}
	}
	
	updateStatus("Uploading data, please wait...");
	console.log("Uploading LAD...");

	//open in sync mode (false)
	xhr.open("POST",uploadLoc);

	//set the header modes necessary.
	xhr.setRequestHeader("Content-type", "application/octet-stream");

	xhr.upload.addEventListener("progress", progressHandler(), false);

	//when the state changes after the send, check and see what happened and update the user
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			// Handle response.
			updateStatus('File upload successful!'); // handle response.
		}
		else{
			updateStatus('Unknown error with upload\nState: ' + xhr.readyState + '\nStatus: ' + xhr.status);
		}
	};

	//send the file
	xhr.send(fileData);

	function progressHandler(){
		return function(event){
			var progressPercent = 0;
			if(event.lengthComputable){
				progressPercent= (event.loaded / event.total * 100);
			}
			console.log(progressPercent + "% uploaded");
		}
	}

}


/* wipeLADData
* NO input - user selected "wipe LAD" button and confirms in this function
*
* NO return - user is informed of the status of the wipe in the update status portion of the view
*
* After the user selects the "wipe LAD" button, a confirmation popup is shown. If the user OKs this
* an attempt to wipe the LAD is performed. If there are clients connected to the server, this will fail
* and a 500 HTTP status will be returned. A second popup confirmation is given to the user notifying
* them of clients being connected and if they would like to continue. If they do, then a second attempt
* is performed that will force disconnect all clients along with the wipe.
* No matter the end result the user is informed of the status of the wipe.
*/
//function called when the user selects the wipe button on the settings page
function wipeLADData(){
	if(confirm("Are you sure you want to wipe the Global LAD data?\nALL DATA WILL BE LOST.")){
		//a wipe is done with the base address
		var wipeLoc = host();

		//call the get address that wipes the database
		AJAXdelete(wipeLoc)
		//success! (no clients connected)
		.done(function(){
			updateStatus('Database wiped!');
		})
		.fail(function(reutrnData, returnTextStatus, returnXhr){
			//if failure was due to refusal to wipe, clients are connected. Prompt for retry
			if(returnXhr.status === 500){
				if(confirm("Clients are connected to the Global LAD.\nDo you want to disconnect ALL clients and wipe the Global LAD data?\nALL CLIENTS WILL BE DISCONNECTED AND DATA WILL BE LOST.")){
					wipeLoc += '?force=true';
					//retry with force
					AJAXdelete(wipeLoc)
					.done(function(){
						updateStatus('Database wiped!');
					})
					//unknown reason for failure
					.fail(function(return2Data, return2TextStatus, return2Xhr){
						updateStatus('Database NOT wiped!<br>' + return2Xhr.status + ' - ' + return2TextStatus);
					});
				}
				//user decided to not force the wipe
				else{
					updateStatus('Database wipe cancelled!');
				}
			}
			//unknown reason for basic wipe failure
			else{
				updateStatus('Database NOT wiped!<br>' +returnXhr.status);
			}
		});
	}
	//user decided to not attempt wipe
	else{
		updateStatus('Database wipe cancelled!');
	}
}


//function that highlights (selects) all text in an element so the user may easily copy the text manually
function selectTextById(element) {
    var doc = document
        , text = doc.getElementById(element)
        , range, selection
    ;    
    if (doc.body.createTextRange) {
        range = document.body.createTextRange();
        range.moveToElementText(text);
        range.select();
    } else if (window.getSelection) {
        selection = window.getSelection();        
        range = document.createRange();
        range.selectNodeContents(text);
        selection.removeAllRanges();
        selection.addRange(range);
    }
}

// MPS-8157 05/05/16 - added function
//function that appends a set list of options to the specified element
function loadDropDownOptions(optionType, locationName) {

	var loc = document.getElementById(locationName);

	switch (optionType){
	    case "TimeTypes":
	    
	    	for( var i = 0 ; i < timeOptionsText.length ; i++){
	    		var option = document.createElement("option");
	    		
	    		option.text = timeOptionsText[i];
	    		option.value = timeOptionsText[i].toLowerCase();
	    		
	    		loc.add(option);
	    	}
	}
}
