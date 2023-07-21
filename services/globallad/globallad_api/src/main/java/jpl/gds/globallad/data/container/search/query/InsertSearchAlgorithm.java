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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.WordUtils;

import jpl.gds.globallad.data.GlobalLadSearchAlgorithmException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadContainerFactory;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;

/**
 * Search algorithm optimized for inserting into the global lad.  
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
 *
 * Making the fields final.  Requiring the use of a builder.
 */
public class InsertSearchAlgorithm implements IGlobalLadContainerSearchAlgorithm {

	/**
	 * Builds up the 
	 */
	private static final Map<String, Method> exactMethods;
	
	static {
		exactMethods = new ConcurrentHashMap<String, Method>();
		
		Map<String, String> cm = GlobalLadContainerFactory.getChildContainerMap();
		
		for (Object parentObj : cm.keySet()) {
			String parent = (String) parentObj;
			String child = (String) cm.get(parentObj);
			
			String exactMethodName = String.format("get%s", WordUtils.capitalize(child));
			
			try {
				Method em = InsertSearchAlgorithm.class.getMethod(exactMethodName, nullClassArray);
				exactMethods.put(parent, em);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Container search objects.  
	 */
	private final Object identifier;
	private final String host;
	private final String venue;
	private final Long sessionNumber;
	private final Byte vcid;
	private final Byte dssId;
	private final Integer scid;
	private final Byte userDataType;

	
	/**
	 * Private full constructor that is only called by the builder.
	 * 
	 * @param identifier
	 * @param host
	 * @param venue
	 * @param sessionNumber
	 * @param scid
	 * @param vcid
	 * @param dssId
	 * @param userDataType
	 */
	private InsertSearchAlgorithm(Object identifier, String host,
			String venue, Long sessionNumber, Integer scid, Byte vcid, Byte dssId, Byte userDataType) {
		super();
		this.identifier = identifier;
		this.host = host;
		this.venue = venue;
		this.sessionNumber = sessionNumber;
		this.vcid = vcid;
		this.dssId = dssId;
		this.userDataType = userDataType;
		this.scid = scid;
	}
	
	/**
	 * @return the scid
	 */
	public Integer getScid() {
		return scid;
	}

	/**
	 * @return the identifier
	 */
	public Object getIdentifier() {
		return identifier;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the venue
	 */
	public String getVenue() {
		return venue;
	}

	/**
	 * @return the sessionNumber
	 */
	public Long getSessionNumber() {
		return sessionNumber;
	}

	/**
	 * @return the userDataType
	 */
	public Byte getUserDataType() {
		return userDataType;
	}

	/**
	 * @return the vcid
	 */
	public Byte getVcid() {
		return vcid;
	}

	/**
	 * @return the dssId
	 */
	public Byte getDssId() {
		return dssId;
	}

	
	/**
	 * @return the exact methods
	 */
	public static Map<String, Method> getExactmethods() {
		return exactMethods;
	}

	/**
	 * Uses the mappings from the GDS configuration to look up the algorithm exact getter method tied to the 
	 * containers container type.   
	 * 
	 * @param container
	 * @return The search object to use to check a match against container.
	 * @throws GlobalLadSearchAlgorithmException
	 */
	public Object getSearchObject(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		return (Object) invokeGetterMethod(container);
	}
	
	private Object invokeGetterMethod(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		
		Method method = exactMethods.get(container.getContainerType());
		
		if (method == null) {
			throw new GlobalLadSearchAlgorithmException(String.format("The container type %s was not mapped to an exact getter method", 
					container.getContainerType()));
		}
		
		try {
			return method.invoke(this, nullObjectArray);
		} catch (Exception e) {
				throw new GlobalLadSearchAlgorithmException(String.format("Failed to invoke the exact getter method for container type %s: %s", 
				container.getContainerType(), e.getMessage()));		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#getMatchedChildren(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public Collection<IGlobalLadContainer> getMatchedChildren(IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		/**
		 * If the container has no children return quickly and quietly.
		 */
		Collection<IGlobalLadContainer> matched = new ArrayList<IGlobalLadContainer>();
		
		if (!container.isEmpty()) {
			/**
			 * Exact search.  Get the search field to find a child match.
			 */
			IGlobalLadContainer match = container.getChild(this.getSearchObject(container));
			
			if (match != null) {
				matched.add(match);
			}
		}
		
		return matched;
	}

	/**
	 * Always false for this search algorithm since this is optimized for inserts. 
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#isContainerMatch(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean isContainerMatch(IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException {
		// This is not used by the insert search algorithm so it will always be false.
		return false;
	}

	/**
	 * Always false for this search algorithm since this is optimized for inserts. 
	 * @see jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm#isChildMatchNeeded(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean isChildMatchNeeded(IGlobalLadContainer container) {
		// This is not used by the insert search algorithm so it will always be false.
		return false;
	}
	
	/**
	 * Created builder class and a create method.
	 */
	
	/**
	 * Creates an empty builder class.
	 * 
	 * @return builder class.
	 */
	public static InsertSearchAlgorithmBuilder createBuilder() {
		return new InsertSearchAlgorithmBuilder();
	}
	
	/**
	 * Builder class to build an insert search algorithm.
	 */
	public static class InsertSearchAlgorithmBuilder {
		private Object identifier;
		private String host;
		private String venue;
		private Long sessionNumber;
		private Byte vcid;
		private Byte dssId;
		private Integer scid;
		private Byte userDataType;
		
		/**
		 * 
		 * @param data
		 * 		The global lad data object to be inserted into the global lad.
		 */
		private InsertSearchAlgorithmBuilder() {}
		
		/**
		 * Sets all values from from data.
		 * 
		 * @param data
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder fromData(final IGlobalLADData data) {
			setIdentifier(data.getIdentifier())
			.setHost(data.getHost())
			.setVenue(data.getVenue())
			.setSessionNumber(data.getSessionNumber())
			.setScid(data.getScid())
			.setVcid(data.getVcid())
			.setDssId(data.getDssId())
			.setUserDataType(data.getUserDataType());
			
			return this;
		}

		/**
		 * Creates a new search algorithm based on the values set in this builder.
		 * 
		 * @return
		 */
		public InsertSearchAlgorithm build() {
			return new InsertSearchAlgorithm(identifier, host, venue, sessionNumber, scid, vcid, dssId, userDataType);
		}
		
		/**
		 * @param identifier
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setIdentifier(Object identifier) {
			this.identifier = identifier;
			return this;
		}
		
		/**
		 * @param host
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setHost(String host) {
			this.host = host;
			return this;
		}
		
		/**
		 * @param venue
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setVenue(String venue) {
			this.venue = venue;
			return this;
		}
		
		/**
		 * @param sessionNumber
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setSessionNumber(Long sessionNumber) {
			this.sessionNumber = sessionNumber;
			return this;
		}
		
		/**
		 * @param vcid
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setVcid(Byte vcid) {
			this.vcid = vcid;
			return this;
		}
		
		/**
		 * @param dssId
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setDssId(Byte dssId) {
			this.dssId = dssId;
			return this;
		}
		
		/**
		 * @param scid
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setScid(Integer scid) {
			this.scid = scid;
			return this;
		}
		
		/**
		 * @param userDataType
		 * @return this
		 */
		public InsertSearchAlgorithmBuilder setUserDataType(Byte userDataType) {
			this.userDataType = userDataType;
			return this;
		}
	}
}

