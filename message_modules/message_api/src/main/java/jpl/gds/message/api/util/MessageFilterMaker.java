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
package jpl.gds.message.api.util;

import java.util.Collection;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A utility class for building JMS-compatible message subscription filters.
 */
public class MessageFilterMaker {

    private static final char SQ = '\'';    
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    
    private MessageFilterMaker() {
        // do nothing
    }
    
    /**
     * Method to construct a JMS-compatible subscription filter for specific message types.
     * @param messageTypes array of internal message types as strings (subscription tags)
     * @return filter string
     * 
     * @deprecated use the methods that takes IMessageType array instead
     */
    @Deprecated
    public static String createFilterForMessageTypes(final String[] messageTypes) {
        if(messageTypes.length > 0)
        {
            final StringBuilder filterBuilder = new StringBuilder(1024);
            filterBuilder.append(MetadataKey.MESSAGE_TYPE.toString() + " in ('" + messageTypes[0] + "'");
            for(int i=1; i < messageTypes.length; i++)
            {
                filterBuilder.append(",");
                filterBuilder.append("'" + messageTypes[i] + "'");
            }
            filterBuilder.append(")");
            return filterBuilder.toString();
        } else {
            return "";
        }
    }
    
    
    /**
     * Method to construct a JMS-compatible subscription filter for specific message types.
     * @param messageTypes array of internal message types
     * @return filter string
     */
    public static String createFilterForMessageTypes(final Collection<IMessageType> messageTypes) {
        
        if(!messageTypes.isEmpty())
        {
            final StringBuilder filterBuilder = new StringBuilder(1024);
            filterBuilder.append(MetadataKey.MESSAGE_TYPE.toString() + " in (");
            boolean first = true;
            for(final IMessageType m: messageTypes)
            {
                if (!first) {
                    filterBuilder.append(",");
                }
                filterBuilder.append("'" + m.getSubscriptionTag() + "'");
                first = false;
            }
            filterBuilder.append(")");
            return filterBuilder.toString();
        } else {
            return "";
        }
    }
    
    /**
     * Method to construct a JMS-compatible subscription filter for a specific spacecraft
     * @param scid numeric spacecraft ID
     * @return filter string
     */
    public static String createFilterForSpacecraft(final int scid) {
        return MetadataKey.SPACECRAFT_ID.toString() + "='" + scid + "'";
    }
    

    /**
     * Method to construct a JMS-compatible subscription filter for any metadata key,
     * assuming equivalence to the supplied value is desired.
     *
     * @param tag    metadata key 
     * @param target metadata value 
     *
     * @return Filter string
     */
    public static String createFilter(final MetadataKey tag,
                                       final Object target)
    {
        final StringBuilder sb = new StringBuilder(tag.toString());

        sb.append('=').append(SQ).append(target).append(SQ);

        return sb.toString();
    }
    

    /**
     * Creates a JMS subscription filter for the given metadata key that will detect
     * if the associated header property is null (not there).
     * 
     * @param tag metadata key
     * @return filter string
     */
    public static String createNullFilter(final MetadataKey tag)
    {
        final StringBuilder sb = new StringBuilder(tag.toString());

        sb.append(" IS NULL");

        return sb.toString();
    } 
    
    /**
     * Method to construct a JMS-compatible subscription filter for any metadata
     * key, assuming a regular expression match to the supplied value is
     * desired.
     *
     * @param tag
     *            metadata key
     * @param target
     *            metadata value
     *
     * @return Filter string
     */
    public static String createLikeFilter(final MetadataKey tag,
                                       final Object target)
    {
        final StringBuilder sb = new StringBuilder(tag.toString());

        sb.append(" LIKE ").append(SQ).append(target).append(SQ);

        return sb.toString();
    }
    
    /**
     * Method to construct a JMS-compatible subscription filter for a specific virtual channel.
     * @param vcid virtual channel ID
     * @return filter string
     */
    public static String createFilterForVcid(final int vcid) {
        return createFilter(MetadataKey.CONFIGURED_VCID, vcid);
    }


    /**
     * Method to construct a JMS-compatible subscription filter for a specific
     * context number.
     *
     * @param number context number
     *
     * @return Filter string
     */
    public static String createFilterForContextNumber(final long number)
    {
        return createLikeFilter(MetadataKey.CONTEXT_ID, String.valueOf(number) + IContextKey.ID_SEPARATOR + "%" + 
           IContextKey.ID_SEPARATOR + "%" + IContextKey.ID_SEPARATOR + "%");
    }


    /**
     * Method to construct a JMS-compatible subscription filter for a specific
     * context host.
     *
     * @param host context host
     *
     * @return Filter string
     */
    public static String createFilterForContextHost(final String host)
    {
        return createLikeFilter(MetadataKey.CONTEXT_ID, "%" + IContextKey.ID_SEPARATOR + host + 
                IContextKey.ID_SEPARATOR + "%" + IContextKey.ID_SEPARATOR + "%");
    }


    /**
     * Method to construct a JMS-compatible subscription filter for a specific station ID.
     * @param dssId station ID
     * @return filter string
     */
    public static String createFilterForStation(final int dssId) {
        return createFilter(MetadataKey.CONFIGURED_DSSID, dssId);
    }
    
    /**
     * Method to construct a JMS-compatible subscription filter for multiple stations 
     * using a DssIdFilter object.
     * @param filter the DSS ID filtering object
     * @return filter string
     */
    public static String createFilterForStation(final DssIdFilter filter) {
        
        final StringBuilder sb = new StringBuilder();
        
        final Collection<UnsignedInteger> stations = filter.getStations();
        for (final UnsignedInteger station: stations) {
            if (sb.length() != 0) {
                sb.append(OR);
            }
            sb.append(createFilter(MetadataKey.CONFIGURED_DSSID, station.intValue()));
        }
        if (filter.allowsNone()) {
            if (sb.length() != 0) {
                sb.append(OR);
            }
            sb.append(createFilter(MetadataKey.CONFIGURED_DSSID, StationIdHolder.UNSPECIFIED_VALUE));
            sb.append(OR);
            sb.append(createNullFilter(MetadataKey.CONFIGURED_DSSID));
        }
        
        if (sb.length() != 0) {
            return "(" + sb.toString() + ")";
        } else {
            return "";
        }
    }
    
    /**
     * Method to construct a JMS-compatible subscription filter for multiple VCIDs 
     * using a VcidFilter object.
     * @param filter the VCID filtering object
     * @return filter string
     */
    public static String createFilterForVcid(final VcidFilter filter) {
        
        final StringBuilder sb = new StringBuilder();
        
        final Collection<UnsignedInteger> vcids = filter.getVcids();
        for (final UnsignedInteger vc: vcids) {
            if (sb.length() != 0) {
                sb.append(OR);
            }
            sb.append(createFilter(MetadataKey.CONFIGURED_VCID, vc.intValue()));
        }
        if (filter.allowsNone()) {
            if (sb.length() != 0) {
                sb.append(OR);
            }
            sb.append(createNullFilter(MetadataKey.CONFIGURED_VCID));
        }
        
        if (sb.length() != 0) {
            return "(" + sb.toString() + ")";
        } else {
            return "";
        }
    }

     /**
     * Creates a JMS-compatible subscription filter for context SCID, VCID, and DSS ID.
     * @param appContext the current application context
     *
     * Use MINIMUM_DSSID
     *
     * @return filter string, or null if no filters for SCID, VCID, or DSS ID needed
     *    according to the given application context
     */
    public static String createFilterFromContext(final ApplicationContext appContext) {
        final StringBuilder filter = new StringBuilder();
        
        final IContextFilterInformation info = appContext.getBean(IContextFilterInformation.class);
        
        final IContextIdentification id = appContext.getBean(IContextIdentification.class);
        
        if (id.getSpacecraftId() != 0) {
            filter.append(createFilterForSpacecraft(id.getSpacecraftId()));
        }
        
        if (info.getDssId() != null && info.getDssId() != StationIdHolder.MIN_VALUE) {
            if (filter.length() != 0) {
                filter.append (AND);
            }
            filter.append(makeOrFilter(createFilterForStation(info.getDssId()),
            createFilterForStation(0),
            createNullFilter(MetadataKey.CONFIGURED_DSSID)));
        }
    
        if (info.getVcid() != null)
        {
            if (filter.length() != 0) {
                filter.append (AND);
            }
            filter.append(makeOrFilter(createFilterForVcid(info.getVcid()),
                    createNullFilter(MetadataKey.CONFIGURED_VCID)));
        }
    
        if (filter.length() == 0) {
            return null;
        } else {
            return filter.toString();
        }
    }
    
    /**
     * Combines the given JMS subscription filter strings into an "OR" filter
     * condition surrounded by parenthesis.
     * 
     * @param filterStrings list of filter strings
     * @return "OR" filter string combining the input filter strings
     */
    public static String makeOrFilter(final String...filterStrings) {
        final StringBuilder sb = new StringBuilder();
        for (final String s: filterStrings) {
            if (sb.length() != 0) {
                sb.append(OR);
            }
            sb.append(s);
        }
        
        return "(" + sb.toString() + ")";
    }
    
    /**
     * Creates a subscription filter for spacecraft, message types, DSS IDs, and VCIDs.
     * 
     * @param scid spacecraft ID to filter for, may be null
     * @param messageTypes message types to filter for; may be null or empty
     * @param dssFilter DSS ID filter object; may be null
     * @param vcFilter VC ID filter object, may be null
     * @return entire filter string
     */
    public static String createSubscriptionFilter(final UnsignedInteger scid, final Collection<IMessageType> messageTypes, 
            final DssIdFilter dssFilter, final VcidFilter vcFilter) {
        final StringBuilder filter = new StringBuilder(1024);
        if (scid != null) {
            filter.append(createFilterForSpacecraft(scid.intValue()));
        }
        if (messageTypes != null && !messageTypes.isEmpty()) {
            if (filter.length() != 0) {
                filter.append(AND);
            }
            filter.append(createFilterForMessageTypes(messageTypes));
        }
        if (dssFilter != null) {
            if (filter.length() != 0) {
                filter.append(AND);
            }
            filter.append(createFilterForStation(dssFilter));
        }
        
        if (vcFilter != null) {
            if (filter.length() != 0) {
                filter.append(AND);
            }
            filter.append(createFilterForVcid(vcFilter));
        }
        
        return filter.toString();
    }
}
