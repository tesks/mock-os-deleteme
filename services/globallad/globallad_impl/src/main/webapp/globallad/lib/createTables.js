//main function for creating the depth table
/* createSettingsDepthTable
* NO input
*
* NO return
*
* The depth table consists of data from three different pieces - the default depth, eha entry depths, and evr entry depths.
* The table is created and then it is populated with either the received depths for each segment or "unavailable".
*/
function createSettingsDepthTable(){

	//create the three URLs
	var defaltDepthJSONLocation = host() + 'depth',
	ehaDepthJSONLocation = host() + 'eha/depth',
	evrDepthJSONLocation = host() + 'evr/depth';
	
	
	//create the base table
	var baseTable = '<p><h2>Current depth settings</h2></p><table align="center" id="settingsDepthTable" class="basicTable"></table>';
	document.getElementById("settingsView").innerHTML = baseTable;
	
	//create all of the rows
	for(var i = 0; i<9; i++){
		document.getElementById("settingsDepthTable").insertRow(i);
	}

	//get the JSON holding the depth info
	$.getJSON(defaltDepthJSONLocation)
	.done(function(settingsJSON){
		populateSettingsDepthTable(settingsJSON);
	})
	.fail(function(settingsJSON){
		settingsJSON = JSON.parse('{"defaultDepth" : "Unavailable"}');
		populateSettingsDepthTable(settingsJSON);
	});
	
	//get the JSON with the eha depths
	$.getJSON(ehaDepthJSONLocation)
	.done(function(settingsJSON){
		populateSettingsDepthTable(settingsJSON);
	})
	.fail(function(settingsJSON){
		settingsJSON = JSON.parse('{' + 
									'"0" : "Unavailable",' +
									'"1" : "Unavailable",' +
									'"2" : "Unavailable",' +
									'"3" : "Unavailable",' +
									'"4" : "Unavailable"'  +
								   '}');
		populateSettingsDepthTable(settingsJSON);
	});
	
	//get the JSON with the evr depths
	$.getJSON(evrDepthJSONLocation)
	.done(function(settingsJSON){
		populateSettingsDepthTable(settingsJSON);
	})
	.fail(function(settingsJSON){
		settingsJSON = JSON.parse('{' + 
									'"5" : "Unavailable",' +
									'"6" : "Unavailable",' +
									'"7" : "Unavailable"'  +
								   '}');
		populateSettingsDepthTable(settingsJSON);
	});
}

/* populateSettingsDepthTable
* settingsJSON - JSON with the depth values. It must contain keys equivalent to "defaultDepth" or a numeric value.
*
* NO return - data is pushed out to the table
*
* The JSON needs to be a simple list of key/value pairs. Since all keys are numbers except default depth this is used to
* update the appropriate row of the table. To be safe in case additional user types are added and the table itself has not been updated,
* if the user data type number currently would not update a specific row it is appended to the end of the table.
*/
//fill the settings selector with the names that correspond to the numerical values
function populateSettingsDepthTable(settingsJSON){

	//shortcut so we're not looking up the name all the time
	var table = document.getElementById("settingsDepthTable"),
	tableKey = '',
	insertLoc, row, header, val;

	//set up all of the other values if they exist
	for(key in settingsJSON){

		//if it's not default depth, then it's a number. default is in row 0 and UDT's start at 0
		if(key !== 'defaultDepth'){
			//the row the UDT is located on
			insertLoc = parseInt(key) + 1;
			//get the correct title
			tableKey = getUDTTitle(key);
		}
		//default depth
		else{
			insertLoc = 0;
			tableKey = 'Default Depth';
		}

		//if the UDT is 8+, just append it (shouldn't be necessary since the functions here should be updated
		if(insertLoc > table.rows.length){
			insertLoc = table.row.length;
		}
		
		//remove stale data
		table.deleteRow(insertLoc);
		//create the fresh row with empty values
		row = table.insertRow(insertLoc);
		
		if(insertLoc % 2){
			row.className = 'alt';
		}
		
		header = row.insertCell(0);
		val = row.insertCell(1);

		//insert the new data
		header.innerHTML= tableKey;
		val.setAttribute('align', 'right');
		val.innerHTML = settingsJSON[key];

	}	
}


/* createStatsTables
* @tableData - JSON with the data to be displayed
* @ tableLoc - ID of the div in which the tables will be placed
*
* NO return - all data is appended to the page during functionality
*
* Creates HTML to display tables showing the stats and client info. All are appended to the specified location
*/
function createStatsTables(tableData, tableLoc){
	//start the basic info table
	var tableString = '<table border="0" class="statsTableBasic">',
	tempString,
	tempNum,
	dateVal = new Date();
	
	$('#' + tableLoc).html('<br>');
	//get all of the data from the top level
	for(key in tableData){
		//not including client info
		if(key !== 'GlobalLadSocketServer'){
			//convert milliseconds to seconds or minutes, depending on the value
			if(key.match(/MS$/)){
				if(key.match(/RunTimeMS$/)){ //adjust runtime to get start time and D:H:M:S uptime

					//show the uptime
					tempNum = tableData[key];
					
					tempString = '</td><td>';
					tempNum = Math.floor(tempNum/1000); //to seconds
					tempString = pad(tempNum % 60) + tempString; //0-59 seconds
					
					tempString = ':' + tempString;
					tempNum = Math.floor(tempNum/60); //to minutes
					tempString = pad(tempNum % 60) + tempString; //0-59 minutes
					
					tempString = ':' + tempString;
					tempNum = Math.floor(tempNum/60); //to hours
					tempString = pad(tempNum % 24) + tempString; //0-23 hours
					
					tempString = ':' + tempString;
					tempNum = Math.floor(tempNum/24); //to days
					tempString = pad(tempNum) + tempString; //0-... days


					//get start time
					tempNum = dateVal.getTime() - tableData[key];
					//set the date value to this time
					dateVal.setTime(tempNum);
					tempString += "(" + dateVal.toISOString().replace(/T/, " ") + ")";
					
					key = key.replace(/MS$/,"");
				}
				else{
					tempNum = (tableData[key]/1000);
					key = key.replace(/MS$/,"Seconds");
					if(tempNum >= 60){
						key = key.replace(/Seconds$/,"Minutes");
						tableData[key] = tempNum/60;
					}
					tempString = tableData[key].toFixed(3);
				}
			}
			else{ //no processing
				tempString = tableData[key];
			}
			//put it in the table
			tableString += '<tr><th>' + key.charAt(0).toUpperCase() + key.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1) + ': </th>';
			tableString += '<td>'+ tempString + '</td></tr>';
		}
		//for client info let's put any generic client info on this table
		else{
			for(subKey in tableData[key]){
				if(!(subKey.match(/^open/i) || subKey.match(/^closed/i))){
					tableString += '<tr><th>' + subKey.charAt(0).toUpperCase() + subKey.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1) + ': </th>';
					tableString += '<td>'+ tableData[key][subKey] + '</td></tr>';
				}
			}
		}
	}
	//close table and put it on the page
	tableString += '</table>';
	$('#' + tableLoc).append(tableString);
	$('#' + tableLoc).append('<br><br>');
	

	$('#' + tableLoc).append('<h2>Open Clients</h2>');
	//table of open clients & one for closed clients
	tableString = '<table border="1" class="statsTable">';

	//put in headers
	tableString += createStatsTablesClientHeaders(tableData.GlobalLadSocketServer);
	
	//client info
	tableString += createStatsTablesClientInfo(tableData.GlobalLadSocketServer.openClients);
	
	//close table and append it
	tableString += '</table>';
	$('#' + tableLoc).append(tableString);
	
	//put some space
	$('#' + tableLoc).append('<br><br>');
	
	
	$('#' + tableLoc).append('<h2>Closed Clients</h2>');
	//table of open clients & one for closed clients
	tableString = '<table border="1" class="statsTable">';

	//put in headers
	tableString += createStatsTablesClientHeaders(tableData.GlobalLadSocketServer);
	
	//client info
	tableString += createStatsTablesClientInfo(tableData.GlobalLadSocketServer.closedClients);
	
	//close table and append it
	tableString += '</table>';
	$('#' + tableLoc).append(tableString);
}

//helper function to set up each number of a date-time / uptime value as two digits
function pad(n) {
	//if less than ten prepend a zero
    return (n < 10) ? ("0" + n) : n;
}



/* createStatsTablesClientInfo
* @tableData - JSON that contains the client info
*
* @RETURN - string that comprises the header portion of a client info table
*
* Because both open and closed clients tables will have the same header values, use the same function to create both.
* Because clients in both lists will have the same headers, but one of the lists may be empty, use the first one that does
*  have headers. Part of the headers are contained within a sub-JSON
*/
function createStatsTablesClientHeaders(tableData){

	var key_0, clientSet, tempString,
	tableString = '<tr class="alt"><th>Client</th>';
	if(Object.keys(tableData.openClients).length > 0){
		clientSet = tableData.openClients;
		key_0 = Object.keys(tableData.openClients)[0];
	}
	else{
		clientSet = tableData.closedClients;
		key_0 = Object.keys(tableData.closedClients)[0];
	}
	for(header in clientSet[key_0]){
			if(header !== 'dataConstructor'){
				tempString = header.charAt(0).toUpperCase() + header.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1);
				tempString = tempString.replace(/MS$/,"(S)");
				tempString = tempString.replace(/NS$/,"(S)");

				tableString += '<th>' + tempString + '</th>';
			}
			else{
				for(header2 in clientSet[key_0][header]){
					tempString = header2.charAt(0).toUpperCase() + header2.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1);
					tempString = tempString.replace(/MS$/,"(S)");
					tempString = tempString.replace(/NS$/,"(S)");
					tableString += '<th>' + tempString + '</th>';
				}
			}
	}
	tableString += '</tr>';

	return tableString;
}



/* createStatsTablesClientInfo
* @tableData - JSON that contains either the open or closed client info
*
* @RETURN - string that comprises the data portion of a client table
*
*
*/
function createStatsTablesClientInfo(tableData){
	var tableString = '',
	tempNum,
	altRow = false;
	
	//add all clients
	for(client in tableData){
		tableString += '<tr';
		
		//alternate to add a slight tinge of color to each line, better for readability
		if(altRow){
			tableString += ' class="alt"';
		}
		altRow = !altRow;
		
		tableString += '><td>' + client + '</td>';
		
		//client info
		for(key in tableData[client]){
			if(key !== 'dataConstructor'){
				//prep value
				tempNum = tableData[client][key];
				if(key.match(/NS$/)){
					tempNum /= 1000000000;
					tempNum = tempNum.toFixed(3);
				}
				else if(key.match(/MS$/)){
					tempNum /= 1000;
					tempNum = tempNum.toFixed(3);
				}
				tableString += '<td>' + tempNum + '</td>';
			}
			
			else{ //key === 'dataConstructor'
				for(key2 in tableData[client][key]){
					//prep value
					tempNum = tableData[client][key][key2];
					if(key2.match(/NS$/)){
						tempNum /= 1000000000;
						tempNum = tempNum.toFixed(3);
					}
					else if(key2.match(/MS$/)){
						tempNum /= 1000;
						tempNum = tempNum.toFixed(3);
					}
					tableString += '<td>' + tempNum + '</td>';
				}
			}
		}
		tableString += '</tr>';
	}
	
	return tableString;
}


/* createDataTable
* @inputJSON - JSON containing data from a Global LAD "get data" request to be displayed
* @location - div in which this table is to be located
*
* NO return - everything is already output into the given location
*
* This table format is specific to the data returned by the LAD in a way to better view it. The format of the first value is checked to see if the JSON to be displayed is in the
* "verified" format or not. Verified JSONs are comprised of a set of key/value pairs where the value is a non-verified data formatted JSON. If the JSON is in the verfied format, then
* the keys are utilized as titles for the corresponding tables. If it is not, then the data is just put into one table.
*
* Updated to work with both verified and non-verified data sets. Verified data has an additional level to the JSON where each set of data is grouped under headers.
*/
function createDataTable(inputJSON, location){
	var tableString = "";
	var key_0 = Object.keys(inputJSON)[0];

    if(key_0 === undefined){
        $(location).html('<h2>No Data Returned</h2>');
        return;
    }

    //MPCS-8014 03/16/16 - Added - If data is from a verified query, need to divide data up into tables based upon the larger categories.
    if( typeof inputJSON[key_0] === 'object' && !Array.isArray(inputJSON[key_0])){
        //get top level keys, which are the categories
        var keys = Object.keys(inputJSON);

        for(var i = 0; i < keys.length ; i++){
            //set the category as a header and then put the corresponding data as a table underneath, repeat for all data.
            tableString += "<h2>" + keys[i].charAt(0).toUpperCase() + keys[i].replace(/([a-z])([A-Z])/g, '$1 $2').slice(1) + "</h2>";
            tableString += createDataSubTable(inputJSON[keys[i]]);
        }
    }
    //otherwise it's an unverified set of data, only need the one table
    else{
        tableString += createDataSubTable(inputJSON);
    }

	$(location).html(tableString);
}

/* createDataSubTable
* @subTableData - JSON containing data from a global LAD "get data" request to be displayed.
*
* @RETURN - string that comprises the data in subTableData in a table form.
*
* This table format is specific to the data returned by the LAD in a way to better view it. A header of ID is given to the first column.
* The first item of the first identifier is looked at to get the rest of the column keys being utilized. Because the value of each key/value
* pair of the encompassing JSON is an array of JSONs that are each record.
* The ID value is set to span down for all of the rows that its data will reside in, allowing the user to easily see each item if they need to scroll down to
* other items. Each record is then given a row. Because all data in a record is in the same order, there is no need to match them up with the headers, therefore a nested
* loop is utilized to handle this.
*
* All values are set to nowrap so the table will not attempt to condense itself to fit into the space of whatever div, making it easier for the user to compare values in the column.
*
* Created class, comprises code pulled from createDataTable in order to make it modular.
*/
function createDataSubTable(subTableData){
	//create the table and put the ID column header in it
	var tableString = '<table border="1" width="100%" class="dataTable"><tr>',
	altRow = false,
    key_0 = Object.keys(subTableData)[0];

	for(key in subTableData[key_0][0]){
		//get the keys from it. All entries will have the same keys
		tableString += '<th ';

		//messages are left justified, also do the same to the header
		if(key === 'message'){
			tableString += ' class="left" ';
		}

		tableString += '>' + key.charAt(0).toUpperCase() + key.replace(/([a-z])([A-Z])/g, '$1 $2').slice(1) + '</th>';
	}
	//close off the header row
	tableString += '</tr>';

	//get each channel/event level from the returned date
	for(ident in subTableData){
		//each record needs to be put in
		for(var i = 0 ; i < subTableData[ident].length ; i++){
			tableString += '<tr';

			//alternate to add a slight tinge of color to each line, better for readability
			if(altRow){
				tableString += ' class="alt"';
			}
			altRow = !altRow;
			tableString += '>';

			//put all of the values of this record into one row
			for(item in subTableData[ident][i]){
				tableString += '<td';

				//left justify messages because the difference in length that may be present in items
				if(item === 'message'){
					tableString += ' class="left" ';
				}
				tableString += '>' + subTableData[ident][i][item] + '</td>';
			}
			//close the row, done with this recod
			tableString += '</tr>';
		}
		//done with this identifier
	}

	//done with the returned data, close off the table and display it to the user
	tableString += '</table>';

	return tableString;
}


/* createInfoTable
* @infoJSON - JSON received from the server that contains info on a selected item in the tree
* @location - HTML ID where the table is to be placed
*
* NO return data - table is pushed back to the HTML element specified during the function
*
* The JSON received from the server for info is expected to be a "simple" JSON consisting of only key/value pairs
* where a value is an atomic value, NOT an array, JSON, or any other type of object. The data is placed into a basic
* two column table that is appended to the specified location when complete.
*/

//TODO: make this the new generic table creator where there are no nested JSONs or arrays
function createInfoTable(infoJSON,location){
	//start the table string
	var tableString = '<table class="infoTable">',
	altRow = false,
	tempName = '';
	
	//each item is a key/value pair that is unique, hence no header row
	for(key in infoJSON){
	
		//check to see if the value needs to be bumped up to MS
		if(key.match(/NS$/)){
			tempName = key.replace(/NS$/,"MS");
			infoJSON[key] /= 1000000
		}
		else{
			tempName = key;
		}
		
		//check to see if the value needs to be bumped up to seconds
		if((tempName.match(/MS$/)) && (infoJSON[key] >= 1000)){
			infoJSON[key]/1000;
			tempName = tempName.replace(/MS$/, "S");
		}
		
		//if it's a decimal value truncate it to 3 decimal points
		if(isFloat(infoJSON[key])){
			infoJSON[key] = infoJSON[key].toFixed(3);
		}
		
		tableString += '<tr ';
		
		if(altRow){
			tableString += 'class="alt"';
		}
		altRow = !altRow;
		
		tableString += '><th>' + tempName + '</th><td>' + infoJSON[key] + '</td></tr>';
	}
	
	//close the table and put it into the specified location
	tableString += '</table>';
	$('#'+location).html(tableString);
}

//assistant function which tells if a given value is a floating point decimal or an integer.
function isFloat(n){
	return ((typeof n === 'number') && (Math.abs(n % 1) > 0));
}