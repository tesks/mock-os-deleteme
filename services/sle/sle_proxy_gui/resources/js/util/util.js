/**
 * Check to see if string containing time in DOY format is valid with
 * 3 digit milliseconds being optional.
 * @param {String} timeString
 * @returns {Boolean} 
 */
function isValidDoyTime(timeString) {
    if (/^\d{4}-\d{3}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?$/.test(timeString)) {
        if (moment(timeString, "YYYY-DDDDTHH:mm:ss.SSS", true).isValid() ||
            moment(timeString, "YYYY-DDDDTHH:mm:ss", true).isValid()) {
            return true;
        } else {
            return false;
        }
    } else {
        return false;
    }   
}

/**
 * Check is Object is empty, has no properties.
 * @param {Object} obj
 * @returns {Boolean}
 */
function isEmpty(obj) {
	return Object.keys(obj).length === 0;
}

/**
 * Allow numeric input only
 */
function allowNumericInputOnly(e) {
    // Allow: backspace, delete, tab, escape, enter and .
    if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 110]) !== -1 ||
         // Allow: Ctrl+A
        (e.keyCode == 65 && e.ctrlKey === true) ||
         // Allow: Ctrl+C
        (e.keyCode == 67 && e.ctrlKey === true) ||
         // Allow: Ctrl+X
        (e.keyCode == 88 && e.ctrlKey === true) ||
         // Allow: home, end, left, right
        (e.keyCode >= 35 && e.keyCode <= 39)) {
             // let it happen, don't do anything
             return;
    }
    // Ensure that it is a number and stop the keypress
    if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
        e.preventDefault();
    }	
}

/**
 * Make profile object compatible with the profile summary Datatable.
 * Depending on how the Datatable is configured, it required the input 
 * data to contain a defined field for the configured columns. Some of 
 * the columns are only applicable to Return profiles but Forward profiles
 * need these properties defined to be properly loaded into the Datatable
 * 
 * @param {Object} profile - profile object that needs to be converted
 * @returns {Object} profile - converted profile object 
 */
function makeProfileCompatibleWithDatatable(profile) {
	for (var prop in SharedProfileProperty) {
		if (SharedProfileProperty.hasOwnProperty(prop)) {
			var propName = SharedProfileProperty[prop];
			if (!(propName in profile)) {
				profile[propName] = "";
			}
		}
	}
	
	for (var prop in ReturnProfileProperty) {
		if (ReturnProfileProperty.hasOwnProperty(prop)) {
			var propName = ReturnProfileProperty[prop];
			if (!(propName in profile)) {
				profile[propName] = "";
			}
		}
	}					
	
	if (profile[SharedProfileProperty.INTERFACE_TYPE].includes("RETURN")) {       				
		profile[SharedProfileProperty.PROVIDER_TYPE] = "RETURN";
		profile[ReturnProfileProperty.RETURN_TYPE] = profile[SharedProfileProperty.INTERFACE_TYPE];        				

	}
	
	if (profile[SharedProfileProperty.INTERFACE_TYPE].includes("FORWARD")) {
		profile[SharedProfileProperty.PROVIDER_TYPE] = "FORWARD";
		profile[ReturnProfileProperty.RETURN_TYPE] = "";
	}
	
	return profile;
}