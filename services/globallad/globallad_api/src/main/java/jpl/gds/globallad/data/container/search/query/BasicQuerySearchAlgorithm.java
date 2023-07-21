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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import jpl.gds.globallad.data.GlobalLadSearchAlgorithmException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.GlobalLadContainerFactory;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * 
 * Search algorithm implementation that matches both child containers and data. 
 * 
 * Due to the configurable nature of the global lad, a methodology had to be devised that would allow search algorithms to dynamically
 * query the search criteria for a given container at a given level using the same search algorithm implementation.
 * 
 * <p>
 * As an example, the data levels of the global lad can be configured as follows:
 * <p>
 * master -> host -> sessionNumber -> userDataType -> identifier
 * <p>
 * In this case, the master container hold host containers, host containers hold sessionNumber containers, and so on.  However, a different configuration 
 * may have the structure as follows:
 * <p> 
 * master -> host -> userDataType -> identifier
 * <p>
 * 
 * In order to support all possible configurations of the data levels in the global lad, a static map of all methods is constructed
 * using reflection.  This cached map of methods is then used at the time of the query to lookup the getter method to get the search criteria
 * for the container being searched.
 * <p>
 * Using the first example above, when checking to find child matches of a host container the getSearchData method will see that the container being
 * queried is a host container and it will look up in the map and run the getSessionNumbers method.  In the second example, the same lookup will return 
 * the results of running the getUserDataType method instead.
 * <p>
 * The same is true for the getLocalSearchData method, however this is used when attempting to see if the container being queried matches
 * some criteria instead of checking the children of the given container.  
 *
 */
public class BasicQuerySearchAlgorithm extends GlobalLadDataSearchAlgorithm implements IGlobalLadSearchAlgorithm {
	private static final String VALID_TIME_TYPES = StringUtils.join(GlobalLadPrimaryTime.values(), "|");
	
	private enum MethodInvokeType {
		/**
		 * An exact query will match the child containers identifier directly and must be equal.
		 */
		exact, 
		
		/**
		 * Uses a regex match to match any child container identifiers.
		 */
		regex, 
		
		/**
		 * Local search is an exact search on the current containers identifier.
		 */
		local, 
		
		/**
		 * Local regex match on the current containers identifier.
		 */
		localRegex
	}
	
	/**
	 * Build a map of the getter methods based on type of query and the configured data structure.
	 */
	private static final Map<String, Method> exactMethods;
	private static final Map<String, Method> wildCardMethods;
	private static final Map<String, Method> localCheckMethods;
	private static final Map<String, Method> localCheckWildCardMethods;
	private static final ArrayList<String> levels;
	
	static {
		exactMethods = new ConcurrentHashMap<String, Method>();
		wildCardMethods = new ConcurrentHashMap<String, Method>();
		localCheckMethods = new ConcurrentHashMap<String, Method>();
		localCheckWildCardMethods = new ConcurrentHashMap<String, Method>();
		
		final Map<String, String> cm = GlobalLadContainerFactory.getChildContainerMap();
		
		for (final Object parentObj : cm.keySet()) {
			final String parent = (String) parentObj;
			final String child = cm.get(parentObj);
			final String exactMethodName = String.format("get%ss", WordUtils.capitalize(child));
			final String regexMethodName = String.format("get%sRegex", WordUtils.capitalize(child));
			
			// Some of the container types do not support wild cards.  If a method does not exist we silently catch and move on.
			try {
				/**
				 * All should support exact methods so put those in a group at the top.
				 */
				final Method em = BasicQuerySearchAlgorithm.class.getMethod(exactMethodName, nullClassArray);
				exactMethods.put(parent, em);
				localCheckMethods.put(child, em);
				
				/**
				 * Some may not include wild cards.  Put those together at the bottom.
				 */
				final Method wm = BasicQuerySearchAlgorithm.class.getMethod(regexMethodName, nullClassArray);
				wildCardMethods.put(parent, wm);
				localCheckWildCardMethods.put(child, wm);

			} catch (final NoSuchMethodException e) {
				// In the case the get method exists it is not an error, just can't use that search.
			} catch (final SecurityException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Keeping track of the level types.  This will be used so we know
		 * if there are defined query parameters for the values after the container being checked.  
		 */
		levels = new ArrayList<String>();

		/**
		 * Build a list of the levels a single time.  Since the order is unknown we start 
		 * at master and keep getting the type that is mapped as the child until we reach the end.
		 */
		String parentType = "master";
		
		do {
			levels.add(parentType);
			parentType = GlobalLadContainerFactory.getChildContainerMap().get(parentType);
		} while (parentType != null); 
	}
	

	/**
	 * Container wild card objects.
	 */
	private final Collection<?> identifiers;
	private final Collection<String> hostWildCards;
	private final Collection<String> venueWildCards;
	private final Collection<String> sessionNumberWildCards;
	private final Collection<String> userDataTypeWildCards;
	private final Collection<String> identifierWildCards;

	
	/**
	 * If true will match when the objects to match are not set.  If false will not.
	 */
	private final boolean matchOnNullOrEmpty;
	
	/**
	 * Full protected constructor.
	 * 
	 * @param identifiers
	 * @param hosts
	 * @param venues
	 * @param sessions
	 * @param scids
	 * @param dataTypes
	 * @param hostWildCards
	 * @param venueWildCards
	 * @param sessionWildCards
	 * @param dataTypeWildCards
	 * @param identifierWildCards
	 * @param upperBoundMilliseconds
	 * @param lowerBoundMilliseconds
	 * @param upperBoundNanoseconds
	 * @param lowerBoundNanoseconds
	 * @param vcids
	 * @param dssIds
	 * @param timeType
	 * @param matchOnNullOrEmpty
	 */
	protected BasicQuerySearchAlgorithm(final Collection<Object> identifiers, 
			final Collection<String> hosts, 
			final Collection<String> venues,
			final Collection<Long> sessions, 
			final Collection<Integer> scids,
			final Collection<Byte> dataTypes, 
			final Collection<String> hostWildCards,
			final Collection<String> venueWildCards, 
			final Collection<String> sessionWildCards,
			final Collection<String> dataTypeWildCards, 
			final Collection<String> identifierWildCards, 
			final long upperBoundMilliseconds, final long lowerBoundMilliseconds,
			final long upperBoundNanoseconds, final long lowerBoundNanoseconds, 
			final Collection<Byte> vcids,
			final Collection<Byte> dssIds,
			final IGlobalLADData.GlobalLadPrimaryTime timeType,
			final boolean matchOnNullOrEmpty
			) {
		super(upperBoundMilliseconds, lowerBoundMilliseconds, upperBoundNanoseconds, lowerBoundNanoseconds, hosts, venues, sessions, scids, dataTypes, vcids, dssIds, timeType);
		
		this.identifiers = identifiers;
		
		this.hostWildCards = hostWildCards;
		this.venueWildCards = venueWildCards;
		this.sessionNumberWildCards = sessionWildCards;
		this.userDataTypeWildCards = dataTypeWildCards;
		this.identifierWildCards = identifierWildCards;
		
		this.matchOnNullOrEmpty = matchOnNullOrEmpty;
	}
	

	/**
	 * @return the matchOnNullOrEmpty
	 */
	public boolean isMatchOnNullOrEmpty() {
		return matchOnNullOrEmpty;
	}

	/**
	 * @return the identifiers
	 */
	public Collection<?> getIdentifiers() {
		return identifiers == null ? Collections.emptyList() : identifiers;
	}

	/**
	 * @return the hostWildCards
	 */
	public Collection<String> getHostWildCards() {
		return hostWildCards;
	}

	/**
	 * @return the venueWildCards
	 */
	public Collection<String> getVenueWildCards() {
		return venueWildCards;
	}

	/**
	 * @return the sessionNumberWildCards
	 */
	public Collection<String> getSessionNumberWildCards() {
		return sessionNumberWildCards;
	}

	/**
	 * @return the dataTypeWildCards
	 */
	public Collection<String> getUserDataTypeWildCards() {
		return userDataTypeWildCards;
	}

	/**
	 * @return the identifierWildCards
	 */
	public Collection<String> getIdentifierWildCards() {
		return identifierWildCards;
	}
	
	/**
	 * @param list
	 * @return true if list is null or zero length.
	 */
	private boolean checkEmptyOrNull(final Collection<?> list) {
		return list == null || list.isEmpty();
	}

	/**
	 * Joins the host wild cards with the regex '|' character.  Return will 
	 * be null or empty if the array is null or empty, respectively.
	 * 
	 * @return joined host wild card string or null if no wild cards were defined.
	 */
	public String getHostRegex() {
		return checkEmptyOrNull(hostWildCards) ? null : StringUtils.join(hostWildCards, "|");
	}
	
	/**
	 * Joins the venue wild cards with the regex '|' character.  Return will 
	 * be null or empty if the array is null or empty, respectively.
	 * 
	 * @return joined venue wild card string or null if no wild cards were defined.
	 */	
	public String getVenueRegex() {
		return checkEmptyOrNull(venueWildCards) ? null : StringUtils.join(venueWildCards, "|");
	}
	
	/**
	 * Joins the data type wild cards with the regex '|' character.  Return will 
	 * be null or empty if the array is null or empty, respectively.
	 * 
	 * NOTE:  This doesn't really make any sense to have a wild card for user data type.
	 * 
	 * @return joined user data type wild card string or null if no wild cards were defined.
	 */
	public String getUserDataTypeRegex() {
		return checkEmptyOrNull(userDataTypeWildCards) ? null : StringUtils.join(userDataTypeWildCards, "|");
	}
	
	/**
	 * Joins the sessions wild cards with the regex '|' character.  Return will 
	 * be null or empty if the array is null or empty, respectively.
	 * 
	 * Session wild cards can contain ranges, i.e 1..10, which will be all values from 1 to 10.
	 * 
	 * @return joined session wild card string or null if no wild cards were defined.
	 */	
	public String getSessionNumberRegex() {
		return checkEmptyOrNull(sessionNumberWildCards) ? null : StringUtils.join(sessionNumberWildCards, "|");
	}

	/**
	 * Joins the container identifiers wild cards with the regex '|' character.  Return will 
	 * be null or empty if the array is null or empty, respectively.
	 * 
	 * @return joined identifier wild card string or null if no wild cards were defined.
	 */	
	public String getIdentifierRegex() {
		return checkEmptyOrNull(identifierWildCards) ? null : StringUtils.join(identifierWildCards, "|");
	}
	
	/**
	 * Return the collection of objects to check if the containers identifier is equal.  Used in the isMatched
	 * method.  
	 * 
	 * @param container search target
	 * @return collection of data to be used to match children of container.
	 */
	@SuppressWarnings("unchecked")
	public Collection<Object> getSearchData(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		final Object result = invokeGetterMethod(MethodInvokeType.exact, container);
		return result == null ? Collections.emptyList() : (Collection<Object>) result;
	}
	
	/**
	 * Gets the local search data array to be used to match the container directly.
	 * 
	 * @param container search target
	 * @return collection of data to be used to match container.
	 * @throws GlobalLadSearchAlgorithmException
	 */
	@SuppressWarnings("unchecked")
	public Collection<Object> getLocalSearchData(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		final Object result = invokeGetterMethod(MethodInvokeType.local, container);
		return result == null ? Collections.emptyList() : (Collection<Object>) result;
	}

	/**
	 * Gets the regex string to match for local container matches.
	 * 
	 * @param container search target
	 * @return regex string to be used to match the identifier of container.
	 * @throws GlobalLadSearchAlgorithmException
	 */
	public String getLocalRegex(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		return (String) invokeGetterMethod(MethodInvokeType.localRegex, container);
	}
	/**
	 * Return the regex to be used when checking the container identifier.  
	 * 
	 * NOTE:  The object identifiers toString method will be used to convert to a string in 
	 * order to test the regex returned by this method so it is up to the developer to make sure
	 * that these two things will match up correctly.
	 * 
	 * @return regex string to be used to match container.
	 */
	public String getRegex(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		return (String) invokeGetterMethod(MethodInvokeType.regex, container);
	}

	/**
	 * Added a method so that more checking could be done.
	 */

	/**
	 * 
	 * @param invokeType
	 * @param container
	 * @return
	 * @throws GlobalLadSearchAlgorithmException
	 */
	private Object invokeGetterMethod(final MethodInvokeType invokeType, final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		return invokeGetterMethod(invokeType, container.getContainerType());
	}
	
	/**
	 * 
	 * @param invokeType
	 * @param containerType 
	 * @return
	 * @throws GlobalLadSearchAlgorithmException
	 */
	private Object invokeGetterMethod(final MethodInvokeType invokeType, final String containerType) throws GlobalLadSearchAlgorithmException {
		
		Method method;
		
		switch(invokeType) {
		case exact:
			method = exactMethods.get(containerType);
			break;
		case regex:
			method = wildCardMethods.get(containerType);
			break;
		case local:
			method = localCheckMethods.get(containerType);
			break;
		case localRegex:
		default:
			method = localCheckWildCardMethods.get(containerType);
			break;
		}
		
		/**
		 * If there is no specific mapping for a container type we don't want to bring the system down.  Just return 
		 * an empty collection.
		 */
		// In case there are some other things that don't map, just return null.  
		if (method == null) {
			return null;
		}
		
		try {
			return method.invoke(this, nullObjectArray);
		} catch (final Exception e) {
				throw new GlobalLadSearchAlgorithmException(String.format("Failed to invoke the %s getter method for container type %s: %s", 
				invokeType, containerType, e.getMessage()));		
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#getMatchedChildren(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public Collection<IGlobalLadContainer> getMatchedChildren(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		/**
		 * If the container has no children return quickly and quietly.
		 */
		final Collection<IGlobalLadContainer> matched = new ArrayList<IGlobalLadContainer>();
		
		if (!container.isEmpty()) {
			final String regex = getRegex(container);
			/**
			 * If the there is a regex don't bother with the exact search even if one is there. 
			 * Only do a regex search.
			 */
			if (regex != null && !regex.isEmpty()) {
				matched.addAll(container.getChildrenWithRegex(regex));
			} else {
				/**
				 * Exact search.  Get the search data array and find the children that match.
				 */
				final Collection<Object> searchArray = getSearchData(container);
				if (!checkEmptyOrNull(searchArray)) {
					for (final Object toMatch : searchArray) {
						final IGlobalLadContainer match = container.getChild(toMatch);
						
						if (match != null) {
							matched.add(match);
						}
					}
				} else if (matchOnNullOrEmpty) {
					// Matches all children since it is empty and the match null or empty flag is set.
					matched.addAll(container.getChildren());
				} else {
					// nothing matches is the match null or empty flag is not set.
				}
			}
		}
		
		return matched;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#isContainerMatch(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean isContainerMatch(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		/**
		 * Check to see if there is a local regex.
		 */
		final String localRegex = this.getLocalRegex(container);
		
		if (localRegex != null && !localRegex.isEmpty()) {
			return container.getContainerIdentifier().toString().matches(localRegex);
		} else {
			final Collection<Object> searchArray = getLocalSearchData(container);
	
			/**
			 * If this is empty it is a match.
			 */
			if (!searchArray.isEmpty()) {
				for (final Object searchObj : searchArray) {
					if (container.getContainerIdentifier().equals(searchObj)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.query.GlobalLadDataSearchAlgorithm#isSpecialMatched(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public boolean isSpecialMatched(final IGlobalLADData data) {
		return true;
	}


	/**
	 * Checks to see if there is search data defined for a descendant of container.
	 * 
	 * @param container
	 * @return true if search data is defined for a descendant of container.
	 * @throws GlobalLadSearchAlgorithmException
	 */
	@SuppressWarnings("unchecked")
	private boolean descendantHasMatchParameters(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		/**
		 * Start from the index of the container and check after.
		 */
		for (int index = levels.indexOf(container.getContainerType())+1; index < levels.size(); index++) {
			final String l = levels.get(index);

			/**
			 * Check if exact searches are defined.
			 */

			final Object exact = invokeGetterMethod(MethodInvokeType.local, l); 
			if (exact != null && !((Collection<Object>) exact).isEmpty()) {
				/**
				 * Exact stuff is defined.
				 */
				return true;
			}

			final Object regex = invokeGetterMethod(MethodInvokeType.localRegex, l); 
			if (regex != null) {
				/**
				 * There is a regex defined.
				 */
				return true;
			} 
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#isChildMatchNeeded(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean isChildMatchNeeded(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		return !getSearchData(container).isEmpty() || 
				getRegex(container) != null ||
				descendantHasMatchParameters(container);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(super.toString());
		builder.append("BasicQuerySearchAlgorithm [");
		if (identifiers != null) {
			builder.append("identifiers=");
			builder.append(identifiers);
			builder.append(", ");
		}
		if (hostWildCards != null) {
			builder.append("hostWildCards=");
			builder.append(hostWildCards);
			builder.append(", ");
		}
		if (venueWildCards != null) {
			builder.append("venueWildCards=");
			builder.append(venueWildCards);
			builder.append(", ");
		}
		if (sessionNumberWildCards != null) {
			builder.append("sessionNumberWildCards=");
			builder.append(sessionNumberWildCards);
			builder.append(", ");
		}
		if (userDataTypeWildCards != null) {
			builder.append("userDataTypeWildCards=");
			builder.append(userDataTypeWildCards);
			builder.append(", ");
		}
		if (identifierWildCards != null) {
			builder.append("identifierWildCards=");
			builder.append(identifierWildCards);
			builder.append(", ");
		}
		builder.append("matchOnNullOrEmpty=");
		builder.append(matchOnNullOrEmpty);
		builder.append("]");
		return builder.toString();
	}
	
	
	/**
	 * Created builder class and a create method.
	 */
	
	/**
	 * Creates an empty builder class.
	 * 
	 * @return builder class.
	 */
	public static BasicQuerySearchAlgorithmBuilder createBuilder() {
		return new BasicQuerySearchAlgorithmBuilder();
	}

	/**
	 * Builder class to create a query search algorithm.  
	 * 
	 * Note:  the default value for matchOnNullOrEmpty is true which means if nothing is set for that level it will 
	 * match.  To change this a call to set this value must be made.
	 */
	public static class BasicQuerySearchAlgorithmBuilder {
		/**
		 * Data search objects.  These are not final and have setters.  Since all primitives are being used
		 * initialize all times to -1.  This way values that are unset will cause the time boxes to pass their test.
		 */
		protected long upperBoundMilliseconds = -1;
		protected long lowerBoundMilliseconds = -1;
		protected long upperBoundNanoseconds = -1;
		protected long lowerBoundNanoseconds = -1;

		protected IGlobalLADData.GlobalLadPrimaryTime timeType;

		// All the data level queryable stuff.  Holds all types.
		protected Collection<?> identifiers;
		protected Collection<String> hosts;
		protected Collection<String> venues;
		protected Collection<Long> sessionNumbers;
		protected Collection<Byte> vcids;
		protected Collection<Byte> dssIds;
		protected Collection<Byte> userDataTypes;
		protected Collection<Integer> scids;
		protected Collection<String> hostWildCards;
		protected Collection<String> venueWildCards;
		protected Collection<String> sessionNumberWildCards;
		protected Collection<String> userDataTypeWildCards;
		protected Collection<String> identifierWildCards;

		
		/**
		 * If true will match when the objects to match are not set.  If false will not.
		 */
		protected boolean matchOnNullOrEmpty = true;
		
		/**
		 * Creates a search algorithm with the set values from this builder.
		 * 
		 * @return new search algorithm.
		 */
		public IGlobalLadSearchAlgorithm build() {
			return new BasicQuerySearchAlgorithm(identifiers == null ? null : new ArrayList<Object>(identifiers), 
					hosts, 
					venues, 
					sessionNumbers, 
					scids, 
					userDataTypes, 
					hostWildCards, 
					venueWildCards, 
					sessionNumberWildCards, 
					userDataTypeWildCards, 
					identifierWildCards, 
					upperBoundMilliseconds, 
					lowerBoundMilliseconds, 
					upperBoundNanoseconds, 
					lowerBoundNanoseconds, 
					vcids, 
					dssIds, 
					timeType, 
					matchOnNullOrEmpty);
		}
		
		/**
		 * @param identifiers
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setIdentifiers(final Collection<?> identifiers) {
			this.identifiers = identifiers;
			
			return this;
		}
		
		/**
		 * @param identifier
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setIdentifier(final Object identifier) {
			return this.setIdentifiers(Arrays.asList(identifier));
		}

		/**
		 * @param hostWildCards
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setHostWildCards(final Collection<String> hostWildCards) {
			this.hostWildCards = hostWildCards;
			
			return this;
		}

		/**
		 * @param venueWildCards
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVenueWildCards(final Collection<String> venueWildCards) {
			this.venueWildCards = venueWildCards;
			
			return this;
		}

		/**
		 * @param sessionNumberWildCards
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setSessionNumberWildCards(final Collection<String> sessionNumberWildCards) {
			this.sessionNumberWildCards = sessionNumberWildCards;
			
			return this;
		}

		/**
		 * @param userDataTypeWildCards
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUserDataTypeWildCards(final Collection<String> userDataTypeWildCards) {
			this.userDataTypeWildCards = userDataTypeWildCards;
			
			return this;
		}

		/**
		 * @param identifierWildCards
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setIdentifierWildCards(final Collection<String> identifierWildCards) {
			this.identifierWildCards = identifierWildCards;
			
			return this;
		}

		/**
		 * @param matchOnNullOrEmpty
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setMatchOnNullOrEmpty(final boolean matchOnNullOrEmpty) {
			this.matchOnNullOrEmpty = matchOnNullOrEmpty;
			
			return this;
		}

		/**
		 * @param timeType
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setTimeType(final GlobalLadPrimaryTime timeType) {
			if (timeType != null) {
				this.timeType = timeType;
			}
			
			return this;
		}
		
		/**
		 * @param timeType - scet|ert|event.  any case.
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setTimeType(final String timeType) {
			if (timeType != null && timeType.toUpperCase().matches(VALID_TIME_TYPES)) {
				setTimeType(GlobalLadPrimaryTime.valueOf(timeType.toUpperCase()));
			}
			
			return this;
		}
		
		/**
		 * Sets the upper bound times (micro / nano) from upper.  This is safe to call with 
		 * a null input value, this will just be a no-op.
		 * 
		 * @param upper
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUpperBound(final IAccurateDateTime upper) {
			if (upper != null) {
				/**
				 * This was calling getMicros instead of getTime, so it
				 * was always going to be way off.
				 */
				setUpperBoundMilliseconds(upper.getTime());
				setUpperBoundNanoseconds(upper.getNanoseconds());
			}
			
			return this;
		}

		/**
		 * Sets the lower bound times (micro / nano) from lower.  This is safe to call with 
		 * a null input value, this will just be a no-op.
		 * 
		 * @param lower
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setLowerBound(final IAccurateDateTime lower) {
			if (lower != null) {
				/**
				 * Was using the incorrect call to get milliseconds.
				 */

				setLowerBoundMilliseconds(lower.getTime());
				setLowerBoundNanoseconds(lower.getNanoseconds());
			}
			
			return this;
		}
		
		/**
		 * @param upperBoundMilliseconds the upperBoundMilliseconds to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUpperBoundMilliseconds(final long upperBoundMilliseconds) {
			this.upperBoundMilliseconds = upperBoundMilliseconds;
			return this;
		}


		/**
		 * @param lowerBoundMilliseconds the lowerBoundMilliseconds to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setLowerBoundMilliseconds(final long lowerBoundMilliseconds) {
			this.lowerBoundMilliseconds = lowerBoundMilliseconds;
			return this;
		}


		/**
		 * @param upperBoundNanoseconds the upperBoundNanoseconds to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUpperBoundNanoseconds(final long upperBoundNanoseconds) {
			this.upperBoundNanoseconds = upperBoundNanoseconds;
			return this;
		}


		/**
		 * @param lowerBoundNanoseconds the lowerBoundNanoseconds to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setLowerBoundNanoseconds(final long lowerBoundNanoseconds) {
			this.lowerBoundNanoseconds = lowerBoundNanoseconds;
			return this;
		}
		
		/**
		 * @param hosts the hosts to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setHosts(final Collection<String> hosts) {
			this.hosts = hosts;
			return this;
		}

		/**
		 * @param host
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setHost(final String host) {
			if (host != null) {
				setHosts(Arrays.asList(host));
			}
			
			return this;
		}
		
		/**
		 * @param venues the venues to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVenues(final Collection<String> venues) {
			this.venues = venues;
			return this;
		}
		
		/**
		 * @param venue
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVenue(final String venue) {
			if (venue != null) {
				setVenues(Arrays.asList(venue));
			}
			
			return this;
		}

		/**
		 * @param sessionNumbers the sessionNumbers to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setSessionNumbers(final Collection<Long> sessionNumbers) {
			this.sessionNumbers = sessionNumbers;
			return this;
		}

		/**
		 * @param sessionNumber
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setSessionNumber(final Long sessionNumber) {
			if (sessionNumber != null) {
				setSessionNumbers(Arrays.asList(sessionNumber));
			}
			
			return this;
		}
		
		/**
		 * @param scids
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setScids(final Collection<Integer> scids) {
			this.scids = scids;
			return this;
		}

		/**
		 * @param scid
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setScid(final Integer scid) {
			if (scid != null) {
				setScids(Arrays.asList(scid));
			}
			
			return this;
		}
		
		/**
		 * @param vcids the vcids to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVcids(final Collection<Byte> vcids) {
			this.vcids = vcids;
			
			return this;
		}

		/**
		 * @param vcid
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVcid(final Byte vcid) {
			if (vcid != null) {
				setVcids(Arrays.asList(vcid));
			}
			
			return this;
		}
		
		/**
		 * Converts the string to an integer and then a byte to a byte to make sure 
		 * there is no parse error.  If vcid is null does nothing.
		 * 
		 * @param vcid
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setVcid(final String vcid) {
			if (vcid != null && GDR.isIntString(vcid)) {
				setVcid(Integer.valueOf(vcid).byteValue());
			}
			
			return this;
		}
		
		/**
		 * @param dssIds the dssIds to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setDssIds(final Collection<Byte> dssIds) {
			this.dssIds = dssIds;
			
			return this;
		}

		/**
		 * @param dssId
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setDssId(final Byte dssId) {
			if (dssId != null) {
				setDssIds(Arrays.asList(dssId));
			}
			
			return this;
		}
		
		/**
		 * Converts the string to an integer and then a byte to a byte to make sure 
		 * there is no parse error.  If dssId is null does nothing.
		 * 
		 * @param dssId
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setDssId(final String dssId) {
			if (dssId != null && GDR.isIntString(dssId)) {
				setDssId(Integer.valueOf(dssId).byteValue());
			}
			
			return this;
		}
		
		/**
		 * @param DataTypes the dataTypes to set
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUserDataTypes(final Collection<Byte> userDataTypes) {
			this.userDataTypes = userDataTypes;
			
			return this;
		}

		/**
		 * @param userDataType
		 * @return this
		 */
		public BasicQuerySearchAlgorithmBuilder setUserDataType(final byte userDataType) {
			setUserDataTypes(Arrays.asList(userDataType));
			
			return this;
		}
	}
}
