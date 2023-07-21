function host(){
	/* MPCS-8056 03/22/16 - changed again. Due to tomcat deployment global lad is not
	   necessarily located just off the host address. However, both the globallad and webapp
	   will be deployed under the same path. Because of this the href for the current page
	   being served is retrieved, the page name is trimmed off, and then the globallad can
	   be found.
	*/

	var locationHref = location.href,
	path;
	//var pathEnd = location.href.lastIndexOf('/');
	var whereGui = locationHref.indexOf("dashboard");

	if (whereGui){
		path = locationHref.slice(0, whereGui);
	} else {
		path = location.host + "/globallad/";
	}

/*	if(pathEnd >= 0){
		path = location.href.substring(0,pathEnd);
	}
	else{
		path = location.href;
	}*/
	
//	return path + '/globallad/';
	return path;
}

/* MPCS-8157 05/06/16 - created variables. Now can update/change "fixed" drop-down options from one location
 *           SEE loadDropDownOptions function in LADFunctions.js
 */ 
var timeOptionsText = ["ERT", "SCET", "Event", "SCLK", "LST"];