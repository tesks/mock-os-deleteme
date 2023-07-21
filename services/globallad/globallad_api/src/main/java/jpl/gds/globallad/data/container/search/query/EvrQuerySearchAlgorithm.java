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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;

/**
 * Search algorithm for EVRs.  Note the EVR level is not stored independently because that is used as the 
 * identifier.  
 */
public class EvrQuerySearchAlgorithm extends BasicQuerySearchAlgorithm {
	private Collection<String> names;
	private Collection<String> nameRegex;
	private Collection<String> messageRegex;
	private Collection<Long> evrIds;

	/**
	 * Full constructor.
	 * 
	 * @param identifiers
	 * @param hosts
	 * @param venues
	 * @param sessions
	 * @param dataTypes
	 * @param hostWildCards
	 * @param venueWildCards
	 * @param sessionWildCards
	 * @param dataTypeWildCards
	 * @param identifierWildCards
	 * @param upperBoundCoarseTime
	 * @param lowerBoundCoarseTime
	 * @param upperBoundFineTime
	 * @param lowerBoundFineTime
	 * @param vcids
	 * @param dssIds
	 * @param timeType
	 * @param names
	 * @param evrIds
	 */
	public EvrQuerySearchAlgorithm(Collection<Object> identifiers, 
			Collection<String> hosts,
			Collection<String> venues, 
			Collection<Long> sessions, 
			Collection<Integer> scids, 
			Collection<Byte> dataTypes,
			Collection<String> hostWildCards, 
			Collection<String> venueWildCards,
			Collection<String> sessionWildCards, 
			Collection<String> dataTypeWildCards,
			Collection<String> identifierWildCards, 
			long upperBoundCoarseTime,
			long lowerBoundCoarseTime, 
			long upperBoundFineTime,
			long lowerBoundFineTime, 
			Collection<Byte> vcids, 
			Collection<Byte> dssIds,
			GlobalLadPrimaryTime timeType, 
			boolean matchOnNullOrEmpty,
			Collection<String> names, 
			Collection<String> nameRegex,
			Collection<Long> evrIds, 
			Collection<String> messageRegex) {
		super(identifiers, hosts, venues, sessions, scids, dataTypes, hostWildCards,
				venueWildCards, sessionWildCards, dataTypeWildCards,
				identifierWildCards, upperBoundCoarseTime,
				lowerBoundCoarseTime, upperBoundFineTime, lowerBoundFineTime,
				vcids, dssIds, timeType, matchOnNullOrEmpty);
		this.names = names;
		this.evrIds = evrIds;
		this.nameRegex = nameRegex;
		this.messageRegex = messageRegex;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.search.query.GlobalLadDataSearchAlgorithm#isSpecialMatched(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public boolean isSpecialMatched(IGlobalLADData data) {
		if (data instanceof EvrGlobalLadData) {
			EvrGlobalLadData evr = (EvrGlobalLadData) data;
			
			return isNameMatched(evr) && 
				   isNameRegexMatch(evr) &&
				   isEvrIdMatched(evr) &&
				   isMessageRegex(evr);
		} else {
			return false;
		}
	}
	
	/**
	 * @param evr
	 * @return true if the evr id of evr matches the evrIds specified in this search algorithm or if no evrIds were defined.
	 */
	private boolean isEvrIdMatched(EvrGlobalLadData evr) {
		if (evrIds == null || evrIds.isEmpty()) {
			return true;
		} else {
			for (long id : evrIds) {
				if (id == evr.getEvrId()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param evr
	 * @return true if the evr name of evr matches the name specified in this search algorithm or if no names were defined.
	 */
	private boolean isNameMatched(EvrGlobalLadData evr) {
		if (names == null || names.isEmpty()) {
			return true;
		} else {
			for (String name: names) {
				if (name.equals(evr.getEvrName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @param evr
	 * @return true if the evr name match the regexes specified in this search algorithm or if no regexes were defined.
	 */
	private boolean isNameRegexMatch(EvrGlobalLadData evr) {
		if (nameRegex == null || nameRegex.isEmpty()) {
			return true;
		} else {
			for (String rx : nameRegex) {
				if (evr.getEvrName().matches(rx)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	/**
	 * @param evr
	 * @return true if the message matches the message regex specified in this search algorithm or if no regex was defined.
	 */
	private boolean isMessageRegex(EvrGlobalLadData evr) {
		if (messageRegex == null || messageRegex.isEmpty()) {
			return true;
		} else {
			for (String rx : messageRegex) {
				if (evr.getMessage().matches(rx)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Creates an empty builder class.
	 * 
	 * @return builder class.
	 */
	public static EvrSearchAlgorithmBuilder createBuilder() {
		return new EvrSearchAlgorithmBuilder();
	}

	/**
	 * Builder class to create a query search algorithm.  
	 * 
	 * Note:  the default value for matchOnNullOrEmpty is true which means if nothing is set for that level it will 
	 * match.  To change this a call to set this value must be made.
	 * 
	 * This builder is the same as the basic query builder, only proxies the name for the identifiers to channel ids.
	 */
	public static class EvrSearchAlgorithmBuilder extends BasicQuerySearchAlgorithmBuilder {
		private Collection<String> names;
		private Collection<String> nameRegex;
		private Collection<String> messageRegex;
		private Collection<Long> evrIds;
	
		/* (non-Javadoc)
		 * @see jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm.BasicQuerySearchAlgorithmBuilder#build()
		 */
		@Override
		public IGlobalLadSearchAlgorithm build() {
			return new EvrQuerySearchAlgorithm(identifiers == null ? null : new ArrayList<Object>(identifiers), 
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
					matchOnNullOrEmpty,			
					names, 
					nameRegex,
					evrIds, 
					messageRegex);
		}
		
		
		/**
		 * @param evrLevels
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setEvrLevels(Collection<String> evrLevels) {
			setIdentifiers(evrLevels);
			
			return this;
		}
		
		/**
		 * @param evrLevel
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setEvrLevel(String evrLevel) {
			if (evrLevel != null) {
				setEvrLevels(Arrays.asList(evrLevel));
			}
			
			return this;
		}

		/**
		 * @param evrLevelWildCards
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setEvrLevelWildCards(Collection<String> evrLevelWildCards) {
			setIdentifierWildCards(evrLevelWildCards);
			
			return this;
		}
		
		/**
		 * @param evrLevelWildCards
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setEvrLevelWildCard(String evrLevelWildCards) {
			if (evrLevelWildCards != null) {
				setEvrLevelWildCards(Arrays.asList(evrLevelWildCards));
			}
			
			return this;
		}

		/**
		 * @param names the names to set
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setNames(Collection<String> names) {
			this.names = names;
			return this;
		}
		
		/**
		 * @param name
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setName(String name) {
			if (name != null) {
				this.setNames(Arrays.asList(name));
			}
			
			return this;
		}
	
		/**
		 * @param evrIds the evrIds to set
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setEvrIds(Collection<Long> evrIds) {
			this.evrIds = evrIds;
			return this;
		}
	
		/**
		 * @param nameRegex the nameRegex to set
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setNameWildCards(Collection<String> nameRegex) {
			this.nameRegex = nameRegex;
			return this;
		}
	
	
		/**
		 * @param messageRegex the messageRegex to set
		 * @return this
		 */
		public EvrSearchAlgorithmBuilder setMessageWildCards(Collection<String> messageRegex) {
			this.messageRegex = messageRegex;
			return this;
		}
	}
}
