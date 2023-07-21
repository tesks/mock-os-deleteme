/* generateQueryOptions
* @type - specifies the specific query options table in which the data will reside
* @paramJSON - JSON data received from the swagger doc containing the available optional query parameters
*
* NO return - indirectly returned to the user via the display
*
* All of the "param" tagged items within the received XML are utilized to create a form. This form can be filled in by the user to allow them to enter the optional
* query parameters.
*/
function generateQueryOptions(type, paramJSON){
	var tableHTML = '<table id="' + type + 'QueryOptionsTable" class="queryOptionsTable">',
	displayName = '',
	inType = '';
	
	//make sure we have data before continuing
	if(typeof paramJSON !== 'undefined'){
		var paramArr = paramJSON.parameters;
		if(paramArr.length > 0){
			tableHTML += '<tr><th colspan="2">Optional Parameters</th></tr>';
			tableHTML += '<tr><td colspan="2">Wildcards (*) may be used. Separate each parameter with a comma (,)</td></tr>';
			//add each param to the parameter table
			for (var i = 0 ; i < paramArr.length ; i++) {

				// MPCS-11704 - Shakeh Brys - 04/20/20 - Update to use new swagger response data
				if (paramArr[i].name !== 'queryType' && paramArr[i].name !== 'source' && paramArr[i].name !== 'recordedState' && paramArr[i].name !== 'timeType') {

					//hide away (don't include) the EVR options when the user will be doing an EHA request and hide EHA on EVR
					if ((document.getElementById(type + "QType").value === "eha" && (paramArr[i].name.search(/evr/i) !== 0)) ||
							(document.getElementById(type + "QType").value === "evr" && (paramArr[i].name.search(/channel/i) === -1))) {

						//format the displayed name so it looks nicer for the user (capitalize the first letter of the first word and add spaces between known words
						displayName = paramArr[i].name.charAt(0).toUpperCase() +
								paramArr[i].name.slice(1);
						displayName = displayName.replace(/([a-z])([A-Z])/g, '$1 $2');
						tableHTML += '<tr><td>' + displayName + '</td>';

						//the input is where things really matter, this name/id isn't changed
						if (paramArr[i].type === 'string') {
							inType = 'string';
						} else if (paramArr[i].type === 'integer') {
							inType = 'number';
						} else if (paramArr[i].type === 'boolean') {
							inType = 'checkbox';
						} else {
							inType = 'string';
						}
						tableHTML += '<td><input type="' + inType + '" id="' +
								paramArr[i].name + '"/></td></tr>';
					}
				}
			}
		}
	}

	//close the table
	tableHTML += '</table>';

	//create a table and move it over to the param div
	document.getElementById(type + "QueryOptions").innerHTML = tableHTML;
}