/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.globallad.data.container.search.query;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.search.IGlobalLadDataSearchAlgorithm;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * Data search algorithm basic used to match global lad data objects.
 */
public abstract class GlobalLadDataSearchAlgorithm implements IGlobalLadDataSearchAlgorithm {
	/**
	 * The data types enum is used to check if a given data type has been set as a 
	 * tree node type or if it is a data filter since we need to honor all query types. 
	 */
	protected enum DataTypes {
		host, venue, sessionNumber, scid, dssId, vcid, userDataType
	}
	
	/**
	 * Data search objects.  These are not final and have setters.  
	 */
	private final long upperBoundMilliseconds;
	private final long lowerBoundMilliseconds;
	private final long upperBoundNanoseconds;
	private final long lowerBoundNanoseconds;
	
	// All the data level queryable stuff.  Holds all types.
	private final Collection<String> hosts;
	private final Collection<String> venues;
	private final Collection<Long> sessionNumbers;
	private final Collection<Byte> vcids;
	private final Collection<Byte> dssIds;
	private final Collection<Byte> userDataTypes;
	private final Collection<Integer> scids;

	
	private final IGlobalLADData.GlobalLadPrimaryTime timeType;
	
	/**
	 * Full protected constructor.
	 * 
	 * @param upperBoundMilliseconds
	 * @param lowerBoundMilliseconds
	 * @param upperBoundNanoseconds
	 * @param lowerBoundNanoseconds
	 * @param vcids
	 * @param dssIds
	 * @param timeType
	 */
	protected GlobalLadDataSearchAlgorithm(final long upperBoundMilliseconds, final long lowerBoundMilliseconds,
			final long upperBoundNanoseconds, final long lowerBoundNanoseconds, 
			final Collection<String> hosts, 
			final Collection<String> venues,
			final Collection<Long> sessionNumbers,
			final Collection<Integer> scids,
			final Collection<Byte> dataTypes,
			final Collection<Byte> vcids,
			final Collection<Byte> dssIds,
			final IGlobalLADData.GlobalLadPrimaryTime timeType) {
		this.upperBoundMilliseconds = upperBoundMilliseconds;
		this.lowerBoundMilliseconds = lowerBoundMilliseconds;
		this.upperBoundNanoseconds = upperBoundNanoseconds;
		this.lowerBoundNanoseconds = lowerBoundNanoseconds;
		this.hosts = hosts;
		this.venues = venues;
		this.vcids = vcids;
		this.userDataTypes = dataTypes;
		this.dssIds = dssIds;
		this.sessionNumbers = sessionNumbers;
		this.scids = scids;
				
		this.timeType = timeType == null ? IGlobalLADData.GlobalLadPrimaryTime.SCET : timeType;
	}
	
	/**
	 * Checks the time values to figure out if this is an exact time query.  
	 * 
	 * @return true if both the upper and lower bound times are equal.
	 */
	public boolean isExactTimeSearch() {
		return  upperBoundMilliseconds > 0 && 
				lowerBoundMilliseconds > 0 && 
				upperBoundNanoseconds >= 0 && 
				lowerBoundNanoseconds >= 0 &&
				upperBoundMilliseconds == lowerBoundMilliseconds &&
				upperBoundNanoseconds == lowerBoundNanoseconds;
	}
	
	/**
	 * Constructs an accurate datetime with the upper values. 
	 * 
	 * @return accurate datetime with the upper values.
	 */
	public IAccurateDateTime getUpperBoundTime() {
		return new AccurateDateTime(upperBoundMilliseconds, upperBoundNanoseconds);
	}
	
	/**
	 * Constructs an accurate datetime with the lower values.
	 * 
	 * @return accurate datetime with the lower values
	 */
	public IAccurateDateTime getLowerBoundTime() {
		return new AccurateDateTime(lowerBoundMilliseconds, lowerBoundNanoseconds);
	}
	
	/**
	 * @return the upperBoundMilliseconds
	 */
	public long getUpperBoundMilliseconds() {
		return upperBoundMilliseconds;
	}

	/**
	 * @return the lowerBoundMilliseconds
	 */
	@Override
    public long getLowerBoundMilliseconds() {
		return lowerBoundMilliseconds;
	}

	/**
	 * @return the upperBoundNanoseconds
	 */
	public long getUpperBoundNanoseconds() {
		return upperBoundNanoseconds;
	}

	/**
	 * @return the lowerBoundNanoseconds
	 */
	public long getLowerBoundNanoseconds() {
		return lowerBoundNanoseconds;
	}

	/**
	 * @return the hosts
	 */
	public Collection<String> getHosts() {
		return hosts == null ? Collections.<String>emptyList() : hosts;
	}

	/**
	 * @return the venues
	 */
	public Collection<String> getVenues() {
		return venues == null ? Collections.<String>emptyList() : venues;
	}

	/**
	 * @return the sessions
	 */
	public Collection<Long> getSessionNumbers() {
		return sessionNumbers == null ? Collections.<Long>emptyList() : sessionNumbers;
	}
	
	/**
	 * @return the scids
	 */
	public Collection<Integer> getScids() {
		return scids == null ? Collections.<Integer>emptyList() : scids;
	}
	
	/**
	 * @return the userDataTypes
	 */
	public Collection<Byte> getUserDataTypes() {
		return userDataTypes == null ? Collections.<Byte>emptyList() : userDataTypes;
	}

	
	/**
	 * @return the vcids
	 */
	public Collection<Byte> getVcids() {
		return vcids == null ? Collections.<Byte>emptyList() : vcids;
	}

	/**
	 * @return the dssIds
	 */
	public Collection<Byte> getDssIds() {
		return dssIds == null ? Collections.<Byte>emptyList() :  dssIds;
	}
	
	/**
	 * @return the timeType
	 */
	@Override
    public GlobalLadPrimaryTime getTimeType() {
		return timeType;
	}

	/**
	 * Below are the data matching methods.
	 */
	
	/**
	 * Checks if the check time are greater than zero and then does a box search for the check* time values to see if 
	 * they are boxed in to the lower and upper times (on or after lower and before upper).
	 * 
	 * If both the upper and the lower times are not null, greater than zero AND equal this then assumes the 
	 * box is actually finding a data object with that exact time.  It will use the isExactTime method instead of using 
	 * the box query by calling the checkOnOrAfter and the checkBefore method.
	 * 
	 * @param checkCoarse - coarse time to test
	 * @param checkFine - fine time to test
	 * @param coarseTimeLower - lower bound coarse time for the box.
	 * @param fineTimeLower - lower bound fine time for the box.
	 * @param coarseTimeUpper - upper bound coarse time for the box.
	 * @param fineTimeUpper - upper bound fine time for the box.
	 * 
	 * @return true if either of the lower times are null or 
	 *   both of the lower times are less than zero or the check times are on or after the lower times and 
	 *   true if the upper times are null or are less than zero or the check times are before the upper times..
	 */
	protected boolean checkTimeBox(final long checkCoarse, final long checkFine, final long coarseTimeLower, final long fineTimeLower, final long coarseTimeUpper, final long fineTimeUpper) {
		boolean result;
		if (isExactTimeSearch()) {
			/**
			 * Using the wrong time value.  Since both upper and lower are the same, just
			 * pick one and use both the coarse and fine for that time.
			 */
			result = isExactTime(checkCoarse, checkFine, coarseTimeLower, fineTimeLower);
		} else {
			final boolean onOrAfter = 	checkOnOrAfter(checkCoarse, checkFine, coarseTimeLower, fineTimeLower);
			final boolean before = checkBefore(checkCoarse, checkFine, coarseTimeUpper, fineTimeUpper);
			
			result = onOrAfter && before;
		}
		
		return result;
	}
	
	/**
	 * Checks if the coarse times are equal and the fine times are equal.  If any value is null is false.
	 * @param checkCoarse
	 * @param checkFine
	 * @param coarseTime
	 * @param fineTime
	 * @return true if the coarse and fine times are equal to each other.
	 */
	protected boolean isExactTime(final long checkCoarse, final long checkFine, final long coarseTime, final long fineTime) {
		return checkCoarse > 0 && checkFine >= 0 && coarseTime > 0 && fineTime >= 0 &&
				checkCoarse == coarseTime && checkFine == fineTime;
	}
	
	/**
	 * Checks if the lower times or on or after the check times.
	 * 
	 * @param checkCoarse - coarse time to test
	 * @param checkFine - fine time to test
	 * @param coarseTimeLower - lower bound coarse time for on or after check.
	 * @param fineTimeLower - lower bound fine time for on or after check.
	 * 
	 * @return true if either of the lower times are null or 
	 *   both of the lower times are less than zero or the check times are on or after the lower times.
	 */
	protected boolean checkOnOrAfter(final long checkCoarse, final long checkFine, final long coarseTimeLower, final long fineTimeLower) {
		return coarseTimeLower < 0 ||
			   // Coarse time was not set.  In this case we don't care about the fine time.
			   coarseTimeLower < checkCoarse ||
			   coarseTimeLower == checkCoarse && (fineTimeLower < 0 || fineTimeLower <= checkFine);
			   // Only time checking fine time when coarse times are equal and fine time has been set.  Kind of redundant since the case
	}
	
	/**
	 * Checks if the upper times are after the check times.  
	 * 
	 * @param checkCoarse - coarse time to test.  
	 * @param checkFine - fine time to test
	 * @param coarseTimeLower - upper bound coarse time for the before check.
	 * @param fineTimeLower - upper bound fine time for the before check.

	 * @return true if the upper times are null or are less than zero or the check times are before the upper times.
	 */
	protected boolean checkBefore(final long checkCoarse, final long checkFine, final long coarseTimeUpper, final long fineTimeUpper) {
		return coarseTimeUpper < 0 || 
			   // Coarse time was not set.  In this case we don't care about the fine time.
			   coarseTimeUpper > checkCoarse || // If coarse times are good we are good. 
			   // Only look at the fine time in the case where the coarse times are equal.
			   coarseTimeUpper == checkCoarse && (fineTimeUpper > 0 && fineTimeUpper > checkFine);
	}

	/** 
	 * Time box check on data using the global primary time type.
	 * 
	 * @param data match target
	 * @return true if data is within the time box defined by this search algorithm.
	 */
	protected boolean inTimeBoxPrimary(final IGlobalLADData data) {
		return checkTimeBox(data.getPrimaryMilliseconds(), data.getPrimaryTimeNanoseconds(), 
				lowerBoundMilliseconds, lowerBoundNanoseconds, upperBoundMilliseconds, upperBoundNanoseconds);
	}
	
	/**
	 * On or after check on data using the global primary time type.
	 * 
	 * @param data match target
	 * @return true if data is on or after the lower bound time defined in this search algorithm.
	 */
	protected boolean onOrAfterPrimary(final IGlobalLADData data) {
		return checkOnOrAfter(data.getPrimaryMilliseconds(), data.getPrimaryTimeNanoseconds(), lowerBoundMilliseconds, lowerBoundNanoseconds);
	}
	
	/**
	 * Before check search on data using the global primary time type.
	 * @param data
	 * @return true if data is before the upper bound 
	 */
	protected boolean beforePrimary(final IGlobalLADData data) {
		return checkBefore(data.getPrimaryMilliseconds(), data.getPrimaryTimeNanoseconds(), upperBoundMilliseconds, upperBoundNanoseconds);
	}
	
	/**
	 * @param data
	 * @return milliseconds of data corresponding to the primary time set in this search algorithm.
	 */
	private long retrieveMilliseconds(final IGlobalLADData data) {
		switch(timeType) {
		case ERT:
			return data.getErtMilliseconds();
		case EVENT:
			return data.getEventTime();
		case SCET:
		case SCLK:
		case LST:
		default:
			return data.getScetMilliseconds();
		}
	}
	
	/**
	 * @param data
	 * @return nanoseconds of data corresponding to the primary time set in this search algorithm.
	 */
	private long retrieveNanoseconds(final IGlobalLADData data) {
		switch(timeType) {
		case ERT:
			return data.getErtNanoseconds();
		case EVENT:
			return 0;
		case SCET:
		case SCLK:
		case LST:
		default:
			return data.getScetNanoseconds();
		}
	}
	
	/**
	 * Checks if the data is in the time box for the time type defined is timeType.
	 * 
	 * @param data check target
	 * @return true if data is within the time box set with the time bounds set in this search algorithm.
	 */
	protected boolean inTimeBox(final IGlobalLADData data) {
		final long checkCoarse = retrieveMilliseconds(data);
		final long checkFine = retrieveNanoseconds(data);
		return checkTimeBox(checkCoarse, checkFine,
				lowerBoundMilliseconds, lowerBoundNanoseconds, upperBoundMilliseconds, upperBoundNanoseconds);
	}

	/**
	 * Checks if the data is on or after the time defined in timeType.
	 * 
	 * @param data check target
	 * @return true if data is on or after the lower bound times set in this search algorithm.
	 */
	protected boolean onOrAfter(final IGlobalLADData data) {
		final long checkCoarse = retrieveMilliseconds(data);
		final long checkFine = retrieveNanoseconds(data);
		return checkOnOrAfter(checkCoarse, checkFine, lowerBoundMilliseconds, lowerBoundNanoseconds);
	}
	
	/**
	 * Check if the data is before the set time for the time define in timeType.
	 * 
	 * @param data check target
	 * @return true if data is befor the upper bound times set in this search algorithm.
	 */
	protected boolean before(final IGlobalLADData data) {
		final long checkCoarse = retrieveMilliseconds(data);
		final long checkFine = retrieveNanoseconds(data);

		return checkBefore(checkCoarse, checkFine, upperBoundMilliseconds, upperBoundNanoseconds);
	}

	/**
	 * Checks with the configuration to see if dataType, which should be one of the parent mapping 
	 * values from the global lad properties, is set up to be a container level value.
	 * 
	 * If it is a container level value that means that it is an identifier for a node in the tree, and does
	 * not need to be checked by the data search algorithms.  If it is a data filter, this algorithm needs 
	 * to check if it is a match.
	 * 
	 * False - is a container level value
	 * True - is a data filter value.
	 * 
	 * @param dataType
	 * @return true if this data type is a data level filter, false if it is a container level value.
	 */
	protected boolean isDataFilter(final DataTypes dataType) {
		return !GlobalLadProperties.getGlobalInstance().isValueContainerType(dataType.toString());
	}
	
	/**
	 * Checks if any scid filters were defined. 
	 * 
	 * @return true if scid is a data filter and scids have been given to match.
	 */
	protected boolean isScidFilter() {
		return isDataFilter(DataTypes.scid) && !(scids == null || scids.isEmpty());
	}
	
	/**
	 * Checks if any dssId filters were defined.
	 * @return true if dssId is a data filter and dssIds have been given to match.
	 */
	protected boolean isDssIdFilter() {
		return isDataFilter(DataTypes.dssId) && !(dssIds == null || dssIds.isEmpty());
	}
	
	/**
	 * Checks if any vcid filters were defined.
	 * @return true if vcid is a data filter and vcids have been given to match.
	 */
	protected boolean isVcidFilter() {
		return isDataFilter(DataTypes.vcid) && ! (vcids == null || vcids.isEmpty());
	}
	
	/**
	 * @return true if host is a data filter and hosts have been given to match.
	 */
	protected boolean isHostFilter() {
		return isDataFilter(DataTypes.host) && ! (hosts == null || hosts.isEmpty());
	}
	
	/**
	 * @return true if venue is a data filter and venues have been given to match.
	 */
	protected boolean isVenueFilter() {
		return isDataFilter(DataTypes.venue) && ! (venues == null || venues.isEmpty());
	}
	
	/**
	 * @return true if sessionNumber is a data filter and sessionNumbers have been given to match.
	 */
	protected boolean isSessionNumberFilter() {
		return isDataFilter(DataTypes.sessionNumber) && ! (sessionNumbers == null || sessionNumbers.isEmpty());
	}
	
	/**
	 * @return true if userDataType is a data filter and userDataTypes have been given to match.
	 */
	protected boolean isUserDataTypeFilter() {
		return isDataFilter(DataTypes.userDataType);
	}
	
	/**
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 */
	protected boolean checkHost(final IGlobalLADData data) {
		if (isHostFilter()) {
			for (final String host : hosts) {
				// If we match one we are done and passed.
				if (host.equals(data.getHost())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 */
	protected boolean checkVenue(final IGlobalLADData data) {
		if (isVenueFilter()) {
			for (final String venue : venues) {
				if (venue.equals(data.getVenue())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 */
	protected boolean checkSessionNumber(final IGlobalLADData data) {
		if (isSessionNumberFilter()) {
			for (final Long session : sessionNumbers) {
				if (session.equals(data.getSessionNumber())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 
	 * This one does not make any sense but is included for completeness.  We should never be filtering the 
	 * data based on this, it should be used to separate the data but whatever.
	 * 
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 */
	protected boolean checkUserDataType(final IGlobalLADData data) {
		if (isUserDataTypeFilter()) {
			for (final Byte udt : userDataTypes) {
				if (udt.equals(data.getUserDataType())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Dssid, scid and vcid had their logic backward in these check methods.  Instead of
	 * returning true as soon as one of the values matched they were returning false as soon as one did not match.  Clearly this 
	 * means that any range of values will always fail.  
	 */

	/**
	 * Checks to see if either the dssIds are null or empty, which means no dssId filter, or if 
	 * dssIds were defined, checks to see if data.dssId is in the list to be included.
	 * 
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 */
	protected boolean checkDssId(final IGlobalLADData data) {
		if (isDssIdFilter()) {
			for (final Byte dssId : dssIds) {
				if (dssId.equals(data.getDssId())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	  Checks to see if either the vcids are null or empty, which means no vcid filter, or if 
	 * vcids were defined, checks to see if data.vcid is in the list to be included.
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 * 
	 */
	protected boolean checkVcid(final IGlobalLADData data) {
		if (isVcidFilter()) {
			for (final Byte vcid : vcids) {
				if (vcid.equals(data.getVcid())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks to see if either the scids are null or empty, which means no scid filter, or if 
	 * scids were defined, checks to see if scid.vcid is in the list to be included.
	 * 
	 * @param data check target
	 * @return true if is a data filter and matches the values set in this algorithm or no filter has been set in this algorithm returns
	 * 
	 */
	protected boolean checkScid(final IGlobalLADData data) {
		if (isScidFilter()) {
			for (final Integer scid : scids) {
				if (scid.equals(data.getScid())) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadDataSearchAlgorithm#isMatched(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public boolean isMatched(final IGlobalLADData data) {
		return checkHost(data) &&
			   checkScid(data) &&
			   checkVenue(data) &&
			   checkSessionNumber(data) &&
			   checkUserDataType(data) &&
     		   checkDssId(data) &&
			   checkVcid(data) &&
			   inTimeBox(data) &&
			   isSpecialMatched(data);
	}

	/**
	 * Special matching options for any child matching beyond the basic set of matching parameters supplied
	 * in this abstract class.
	 * 
	 * @param data check target
	 * @return true if data passes the check.
	 */
	public abstract boolean isSpecialMatched(IGlobalLADData data);
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("GlobalLadDataSearchAlgorithm [upperBoundMilliseconds=");
		builder.append(upperBoundMilliseconds);
		builder.append(", lowerBoundMilliseconds=");
		builder.append(lowerBoundMilliseconds);
		builder.append(", upperBoundNanoseconds=");
		builder.append(upperBoundNanoseconds);
		builder.append(", lowerBoundNanoseconds=");
		builder.append(lowerBoundNanoseconds);
		builder.append(", ");
		if (hosts != null) {
			builder.append("hosts=");
			builder.append(toString(hosts, 10));
			builder.append(", ");
		}
		if (venues != null) {
			builder.append("venues=");
			builder.append(toString(venues, 10));
			builder.append(", ");
		}
		if (sessionNumbers != null) {
			builder.append("sessionNumbers=");
			builder.append(toString(sessionNumbers, 10));
			builder.append(", ");
		}
		if (vcids != null) {
			builder.append("vcids=");
			builder.append(toString(vcids, 10));
			builder.append(", ");
		}
		if (dssIds != null) {
			builder.append("dssIds=");
			builder.append(toString(dssIds, 10));
			builder.append(", ");
		}
		if (userDataTypes != null) {
			builder.append("userDataTypes=");
			builder.append(toString(userDataTypes, 10));
			builder.append(", ");
		}
		if (timeType != null) {
			builder.append("timeType=");
			builder.append(timeType);
		}
		builder.append("]");
		return builder.toString();
	}

	private String toString(final Collection<?> collection, final int maxLen) {
		final StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (final Iterator<?> iterator = collection.iterator(); iterator.hasNext()
				&& i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}
}
