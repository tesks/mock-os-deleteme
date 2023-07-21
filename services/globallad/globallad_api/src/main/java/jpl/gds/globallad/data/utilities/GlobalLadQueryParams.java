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
package jpl.gds.globallad.data.utilities;

import java.util.Arrays;
import java.util.Collection;

import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.rest.resources.QueryOutputFormat;

/**
 * Query parameter class used by the global lad core functionality to manage query options.
 */
public class GlobalLadQueryParams {
	private final QueryType queryType;
	private final DataSource source;
	private final RecordedState recordedState;
	private final GlobalLadPrimaryTime timeType;
	private final QueryOutputFormat outputFormat;
	private final Collection<String> channelIdRegexes;
	private final Collection<String> evrLevelRegexes;
	private final Collection<String> evrNameRegexes;
	private final Collection<Long> evrIds;
	private final Collection<String> messageRegexes;
	private final Collection<Long> sessionIds;
	private final Collection<String> hostRegexes;
	private final Collection<String> venueRegexes;
	private final Collection<Integer> dssIds;
	private final Collection<Integer> vcids;
	private final Integer scid;
	private final Integer maxResults;
	private final String lowerBoundTimeString;
	private final String upperBoundTimeString;
	private final boolean verified;
	private final boolean showColumnHeaders;
	
	/**
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param timeType
	 * @param outputFormat
	 * @param channelIdRegexes
	 * @param evrIds
	 * @param evrLevelRegexes
	 * @param evrNameRegexes
	 * @param messageRegexes
	 * @param sessionIds
	 * @param hostRegexes
	 * @param venueRegexes
	 * @param dssIds
	 * @param vcids
	 * @param scid
	 * @param maxResults
	 * @param lowerBoundTimeString
	 * @param upperBoundTimeString
	 * @param verified - If true this will do a delta query and calculate the query status results.
	 * @param showColumnHeaders
	 */
	private GlobalLadQueryParams(final QueryType queryType, final DataSource source, final RecordedState recordedState,
			final GlobalLadPrimaryTime timeType, final QueryOutputFormat outputFormat, final Collection<String> channelIdRegexes,
			final Collection<Long> evrIds, final Collection<String> evrLevelRegexes, final Collection<String> evrNameRegexes,
			final Collection<String> messageRegexes, final Collection<Long> sessionIds, final Collection<String> hostRegexes,
			final Collection<String> venueRegexes, final Collection<Integer> dssIds, final Collection<Integer> vcids, final Integer scid,
			final Integer maxResults, final String lowerBoundTimeString, final String upperBoundTimeString, final boolean verified,
			final boolean showColumnHeaders) {
		super();
		this.queryType = queryType;
		this.source = source;
		this.recordedState = recordedState;
		this.timeType = timeType;
		this.outputFormat = outputFormat;
		this.channelIdRegexes = channelIdRegexes;
		this.evrIds = evrIds;
		this.evrLevelRegexes = evrLevelRegexes;
		this.evrNameRegexes = evrNameRegexes;
		this.messageRegexes = messageRegexes;
		this.sessionIds = sessionIds;
		this.hostRegexes = hostRegexes;
		this.venueRegexes = venueRegexes;
		this.dssIds = dssIds;
		this.vcids = vcids;
		this.scid = scid;
		this.maxResults = maxResults;
		this.lowerBoundTimeString = lowerBoundTimeString;
		this.upperBoundTimeString = upperBoundTimeString;
		this.verified = verified;
		this.showColumnHeaders = showColumnHeaders;
	}


	/**
	 * @return the queryType
	 */
	public QueryType getQueryType() {
		return queryType;
	}

	/**
	 * @return the source
	 */
	public DataSource getSource() {
		return source;
	}


	/**
	 * @return the recordedState
	 */
	public RecordedState getRecordedState() {
		return recordedState;
	}


	/**
	 * @return the timeType
	 */
	public GlobalLadPrimaryTime getTimeType() {
		return timeType;
	}

	/**
	 * @return the outputFormat
	 */
	public QueryOutputFormat getOutputFormat() {
		return outputFormat;
	}

	/**
	 * @return the showColumnHeaders
	 */
	public boolean isShowColumnHeaders() {
		return showColumnHeaders;
	}


	/**
	 * @param col
	 * @return true if col is null or empty.
	 */
	private boolean isNotNullOrEmpty(final Collection<?> col) {
		return col != null && !col.isEmpty();
	}

	/**
	 * @return true if there are channelId regexes.
	 */
	public boolean hasChannelIdRegexes() {
		return isNotNullOrEmpty(channelIdRegexes);
	}

	/**
	 * @return the channelIdRegexes
	 */
	public Collection<String> getChannelIdRegexes() {
		return channelIdRegexes;
	}


	/**
	 * @return true if there are evr IDs
	 */
	public boolean hasEvrIds() {
		return isNotNullOrEmpty(evrIds);
	}
	
	/**
	 * @return the evrIds
	 */
	public Collection<Long> getEvrIds() {
		return evrIds;
	}
	
	/**
	 * @return true if there are evr level regexes.
	 */
	public boolean hasEvrLevels() {
		return isNotNullOrEmpty(evrLevelRegexes);
	}
	
	/**
	 * @return the evrLevelRegexes
	 */
	public Collection<String> getEvrLevelRegexes() {
		return evrLevelRegexes;
	}

	/**
	 * @return true if there are evr name regexes.
	 */
	public boolean hasEvrNameRegexes() {
		return isNotNullOrEmpty(evrNameRegexes);
	}
	
	/**
	 * @return the evrNameRegexes
	 */
	public Collection<String> getEvrNameRegexes() {
		return evrNameRegexes;
	}


	/**
	 * @return true if there are evr message regexes.
	 */
	public boolean hasMessagesRegexes() {
		return isNotNullOrEmpty(this.messageRegexes);
	}

	
	/**
	 * @return the messageRegexes
	 */
	public Collection<String> getMessageRegexes() {
		return messageRegexes;
	}


	/**
	 * @return true if there are session ids.
	 */
	public boolean hasSessionIds() {
		return isNotNullOrEmpty(sessionIds);
	}

	/**
	 * @return the sessionIds
	 */
	public Collection<Long> getSessionIds() {
		return sessionIds;
	}

	/**
	 * @return true if there are host regexes.
	 */
	public boolean hasHostRegexes() {
		return isNotNullOrEmpty(hostRegexes);
	}

	/**
	 * @return the hostRegexes
	 */
	public Collection<String> getHostRegexes() {
		return hostRegexes;
	}

	/**
	 * @return true if there are venue regexes.
	 */
	public boolean hasVenueRegexes() {
		return isNotNullOrEmpty(venueRegexes);
	}

	/**
	 * @return the venueRegexes
	 */
	public Collection<String> getVenueRegexes() {
		return venueRegexes;
	}

	/**
	 * @return true if there are dssIds.
	 */
	public boolean hasDssIds() {
		return isNotNullOrEmpty(dssIds);
	}

	/**
	 * @return the dssIds
	 */
	public Collection<Integer> getDssIds() {
		return dssIds;
	}

	/**
	 * @return true if there are vcids.
	 */
	public boolean hasVcids() {
		return isNotNullOrEmpty(vcids);
	}

	/**
	 * @return the vcids
	 */
	public Collection<Integer> getVcids() {
		return vcids;
	}

	/**
	 * @return true if the scid was defined
	 */
	public Integer getScid() {
		return scid;
	}


	/**
	 * @return the maxResults
	 */
	public Integer getMaxResults() {
		return maxResults;
	}


	/**
	 * @return the lowerBoundTimeString
	 */
	public String getLowerBoundTimeString() {
		return lowerBoundTimeString;
	}


	/**
	 * @return the upperBoundTimeString
	 */
	public String getUpperBoundTimeString() {
		return upperBoundTimeString;
	}


	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return verified;
	}


	/**
	 * Creates a new instance of the param builder.
	 * 
	 * @return new builder instance
	 */
	public static GlobalLadQueryParamsBuilder createBuilder() {
		return new GlobalLadQueryParamsBuilder();
	}
	
    @Override
    public String toString() {
        return "type=" + queryType + ",source=" + source + ",recorded=" + recordedState
                + ",timeType=" + timeType + ",outputFormat=" + outputFormat + ",chanIdRegex=" + channelIdRegexes
                + ",evrIds=" + evrIds + ",evrLevels=" + evrLevelRegexes + ",evrNames=" + evrNameRegexes
                + ",messageRegex=" + messageRegexes + ",sessionIds=" + sessionIds + ",hosts=" + hostRegexes + ",venues="
                + venueRegexes + ",dssIds=" + dssIds + ",vcids=" + vcids + ",scid=" + scid + ",maxResults=" + maxResults
                + ",lowerBound=" + lowerBoundTimeString + ",upperBound=" + upperBoundTimeString + ",verified="
                + verified + ",headers=" + showColumnHeaders;

    }
	
	/**
	 * Query parameter builder class.
	 */
	public static class GlobalLadQueryParamsBuilder {
		public GlobalLadQueryParamsBuilder() {}
		
		private QueryType queryType;
		private DataSource source;
		private RecordedState recordedState;
		private GlobalLadPrimaryTime timeType;
		private Collection<String> channelIdRegexes;
		private Collection<Long> evrIds;
		private Collection<String> evrLevelRegexes;
		private Collection<String> evrNameRegexes;
		private Collection<String> messageRegexes;
		private Collection<Long> sessionIds;
		private Collection<String> hostRegexes;
		private Collection<String> venueRegexes;
		private Collection<Integer> dssIds;
		private Collection<Integer> vcids;
		private Integer scid;
		private Integer maxResults;
		private String lowerBoundTimeString;
		private String upperBoundTimeString;
		private boolean verified;
		private boolean showColumnHeaders;

		private QueryOutputFormat outputFormat = QueryOutputFormat.json;

		/**
		 * Creates a new, immutable param object based on this builder.
		 * 
		 * @return new query params class built from the values set in this builder.
		 */
		public GlobalLadQueryParams build() {
			return new GlobalLadQueryParams(queryType, source, recordedState, timeType, outputFormat, channelIdRegexes,
					evrIds, evrLevelRegexes, evrNameRegexes, messageRegexes, sessionIds, hostRegexes, venueRegexes,
					dssIds, vcids, scid, maxResults, lowerBoundTimeString, upperBoundTimeString, verified,
					showColumnHeaders);
		}

        @Override
        public String toString() {
            return "GlobalLad Query Parameters: type=" + queryType + ",source=" + source + ",recorded=" + recordedState
                    + ",timeType=" + timeType + ",outputFormat=" + outputFormat + ",chanIdRegex=" + channelIdRegexes
                    + ",evrIds=" + evrIds + ",evrLevels=" + evrLevelRegexes + ",evrNames=" + evrNameRegexes
                    + ",messageRegex=" + messageRegexes + ",sessionIds=" + sessionIds + ",hosts=" + hostRegexes
                    + ",venues=" + venueRegexes + ",dssIds=" + dssIds + ",vcids=" + vcids + ",scid=" + scid
                    + ",maxResults=" + maxResults + ",lowerBound=" + lowerBoundTimeString + ",upperBound="
                    + upperBoundTimeString + ",verified=" + verified + ",headers=" + showColumnHeaders;

        }

		/**
		 * @param queryType
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setVerified(final boolean verified) {
			this.verified = verified;
			return this;
		}

		/**
		 * @param queryType
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setShowColumnHeaders(final boolean showColumnHeaders) {
			this.showColumnHeaders = showColumnHeaders;
			return this;
		}


		
		/**
		 * @param queryType
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setQueryType(final QueryType queryType) {
			this.queryType = queryType;
			return this;
		}

		/**
		 * @param source
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setSource(final DataSource source) {
			this.source = source;
			return this;
		}

		/**
		 * @param recordedState
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setRecordedState(final RecordedState recordedState) {
			this.recordedState = recordedState;
			return this;
		}

		/**
		 * @param timeType
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setTimeType(final GlobalLadPrimaryTime timeType) {
			this.timeType = timeType;
			return this;
		}

		/**
		 * @param outputFormat
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setOutputFormat(final QueryOutputFormat outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}
		
		/**
		 * @param evrIds
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setEvrIds(final Collection<Long> evrIds) {
			this.evrIds = evrIds;
			return this;
		}

		/**
		 * @param evrLevelRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setEvrLevelRegexes(final Collection<String> evrLevelRegexes) {
			this.evrLevelRegexes = evrLevelRegexes;
			return this;
		}

		
		/**
		 * @param evrNameRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setEvrNameRegexes(final Collection<String> evrNameRegexes) {
			this.evrNameRegexes = evrNameRegexes;
			return this;
		}

		/**
		 * @param messageRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setMessageRegexes(final Collection<String> messageRegexes) {
			this.messageRegexes = messageRegexes;
			return this;
		}

		/**
		 * @param sessionIds
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setSessionIds(final Collection<Long> sessionIds) {
			this.sessionIds = sessionIds;
			return this;
		}

		/**
		 * @param hostRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setHostRegexes(final Collection<String> hostRegexes) {
			this.hostRegexes = hostRegexes;
			return this;
		}

		/**
		 * @param venueRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setVenueRegexes(final Collection<String> venueRegexes) {
			this.venueRegexes = venueRegexes;
			return this;
		}

		/**
		 * @param dssIds
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setDssIds(final Collection<Integer> dssIds) {
			this.dssIds = dssIds;
			return this;
		}

		/**
		 * @param vcids
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setVcids(final Collection<Integer> vcids) {
			this.vcids = vcids;
			return this;
		}

		/**
		 * @param channelIds - Collection of channel name regexes and / or channel range strings.
		 * 
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setChannelIds(final Collection<String> channelIds) {
			this.channelIdRegexes = channelIds;
			return this;
		}
		
		/**
		 * @param channelId - Can be a regex or channel range.
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setChannelId(final String channelId) {
			if (channelId != null) {
				this.setChannelIds(Arrays.<String>asList(channelId));
			}
			return this;
		}


		/**
		 * @param evrLevelRegex
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setEvrLevelRegexes(final String evrLevelRegex) {
			if (evrLevelRegex != null) {
				this.setEvrLevelRegexes(Arrays.<String>asList(evrLevelRegex));
			}
			return this;
		}
		
		/**
		 * @param evrNameRegex
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setEvrNameRegex(final String evrNameRegex) {
			if (evrNameRegex != null) {
				this.setEvrNameRegexes(Arrays.<String>asList(evrNameRegex));
			}
			return this;
		}

		/**
		 * @param messageRegex
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setMessageRegex(final String messageRegex) {
			if (messageRegex != null) {
				this.setMessageRegexes(Arrays.<String>asList(messageRegex));
			}
			
			return this;
		}

		/**
		 * @param sessionId
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setSessionId(final Long sessionId) {
			if (sessionId != null) {
				this.setSessionIds(Arrays.<Long>asList(sessionId));
			}
			
			return this;
		}

		/**
		 * @param hostRegexes
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setHostRegex(final String hostRegexes) {
			if (hostRegexes != null) {
				this.setHostRegexes(Arrays.<String>asList(hostRegexes));
			}
			
			return this;
		}

		/**
		 * @param venueRegex
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setVenueRegex(final String venueRegex) {
			if (venueRegex != null) {
				this.setVenueRegexes(Arrays.<String>asList(venueRegex));
			}
			
			return this;
		}

		/**
		 * @param dssId
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setDssId(final Integer dssId) {
			if (dssId != null) {
				this.setDssIds(Arrays.<Integer>asList(dssId));
			}
			
			return this;
		}

		/**
		 * @param vcid
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setVcids(final Integer vcid) {
			if (vcid != null) {
				this.setVcids(Arrays.<Integer>asList(vcid));
			}
			
			return this;
		}
		
		/**
		 * @param scid
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setScid(final Integer scid) {
			this.scid = scid;
			return this;
		}

		/**
		 * @param maxResults
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setMaxResults(final Integer maxResults) {
			this.maxResults = maxResults;
			return this;
		}
		
		/**
		 * The global lad server will convert this string based on the time type.  It is up to the 
		 * user to make sure the two match. 
		 * 
		 * @param lowerBoundTimeString
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setLowerBoundTimeString(final String lowerBoundTimeString) {
			this.lowerBoundTimeString = lowerBoundTimeString;
			return this;
		}

		/**
		 * The global lad server will convert this string based on the time type.  It is up to the 
		 * user to make sure the two match. 
		 * 
		 * @param upperBoundTimeString
		 * @return this
		 */
		public GlobalLadQueryParamsBuilder setUpperBoundTimeString(final String upperBoundTimeString) {
			this.upperBoundTimeString = upperBoundTimeString;
			return this;
		}
	}
}
