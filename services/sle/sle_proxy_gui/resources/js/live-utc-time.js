/**
 * Live UTC time component
 */
var LiveUTC = (function() {
	
	var $liveUtcTime;
	
	// Update interval in milliseconds
	var UPDATE_INTERVAL = 1000;
	
	/**
	 * Initialize
	 */
	function init() {
		$liveUtcTime = $("#live-utc-time");
		
		Date.prototype.isLeapYear = function() {
			var year = this.getFullYear();
			if ((year & 3) != 0) return false;
			return ((year % 100) != 0 || (year % 400) == 0);
		};

		// Get Day of Year
		Date.prototype.getDOY = function() {
			var dayCount = [ 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 ];
			var mn = this.getUTCMonth();
			var dn = this.getUTCDate();
			var dayOfYear = dayCount[mn] + dn;
			if (mn > 1 && this.isLeapYear()) dayOfYear++;
			// 0 pad 3 digit DOY
			dayOfYear = ("000" + dayOfYear).substr(-3,3);
			return dayOfYear;
		};
	}
	
	/**
	 * Show UTC in application footer.
	 * @private
	 */
	function showTime() {
		var dateNow = new Date();

		var year = dateNow.getFullYear();
		var doy = dateNow.getDOY();
		var datetext = dateNow.toUTCString();
		datetext = datetext.split(' ')[4];

		var utcNow = "Current UTC : " + year + "-" + doy + "T" + datetext;

		$liveUtcTime.text(utcNow);
	}
	
	/**
	 * Start the timer interval.
	 * @public
	 */
	function startTimer() {
		showTime();
		setInterval(showTime, UPDATE_INTERVAL);
	}
	
	// public methods
	return {
		init: init,
		startTimer: startTimer
	}
	
})();