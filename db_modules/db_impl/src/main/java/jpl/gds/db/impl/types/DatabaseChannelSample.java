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
package jpl.gds.db.impl.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.ApidNameHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.SpscHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.VcfcHolder;
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;


/**
 * A representation of a channel value as it's stored in the database.
 *
 * Category is not displayed in CSV or templates at this time.
 */
public class DatabaseChannelSample extends AbstractDatabaseItem implements IDbChannelSampleUpdater, FullyTemplatable//, IDbQueryableAggregate
{
    /** MPCS-6808 Remove field counts */

    protected static final Calendar calendar =
                                      FastDateFormat.getStandardCalendar();

    private static final StringBuilder csv  = new StringBuilder(1024);
    private static final StringBuilder csv2 = new StringBuilder(1024);

    /** MPCS-6808 String constants pushed up */
    protected static final String CSV_COL_HDR = DQ + "Eha";

    protected static final List<String> csvSkip =
        new ArrayList<String>(0);

    /**
     * True if this value is from the SSE, false otherwise
     */
    protected Boolean fromSse;

    /**
     * True if this value is realtime, false otherwise
     */
    protected Boolean isRealtime;

    /**
     * The record creation time.
     */
    protected IAccurateDateTime rct = null;

    /**
     * The packet id
     */
    private PacketIdHolder packetId = PacketIdHolder.UNSUPPORTED;

    /**
     * The frame id
     */
    private Long frameId = null;

    /**
     * The spacecraft clock value for this channel value
     */
    protected ISclk sclk;

    /**
     * The spacecraft event time for this channel value
     */
    protected IAccurateDateTime scet;

    /**
     * The earth receive time for this channel value
     */
    protected IAccurateDateTime ert;

    /**
     * The local solar time for this channel value.
     */
    protected ILocalSolarTime lst = null;

    /**
     * The actual value for this channel value
     */
    protected Object value;

    /**
     * The type of channel this value represents
     */
    protected ChannelType channelType;

    /**
     * The ID of the channel this value is for
     */
    protected String channelId;

    /**
     * The index of the channel this value is for
     */
    protected Long channelIndex;

    /**
     * The FSW/SSE module this channel is for.
     */
    protected String module;

    /**
     * The EU value for the channel, if any.
     */
    protected Double eu;

    /**
     * The status (symbolic) value for the channel, if any.
     */
    protected String status;

    /**
     * DN Alarm state of this channel value
     */
    protected String dnAlarmState;

    /**
     * EU Alarm state of this channel value
     */
    protected String euAlarmState;

    /**
     * DN delta value for this channel value. Used only in on-change queries.
     */
    protected Object deltaValue;

    /**
     * Spacecraft ID for this channel value.
     */
    protected Integer spacecraftId;

    /**
     * Name of the channel.
     */
    protected String name = null;

    /**
     * EU formatter for the channel value.
     */
    protected String euFormat;

    /**
     * DN formatter for the channel value.
     */
    protected String dnFormat;

    /**
     * Previous DN value for this channel value.
     */
    protected Object previousValue;

    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    
    /**
     * Virtual channel ID for this channel value.
     */
    protected Integer vcid;


    /** Channel category */
    private ChannelCategoryEnum category = null;

    /** Optional columns from Packet or SsePacket.
     *  These will be null ONLY if Packet was not included.
     */
    protected ApidHolder                 apid        = null;
    protected ApidNameHolder             apidName    = null;
    protected SpscHolder                 spsc        = null;
    protected IAccurateDateTime          packetRct   = null;
    protected VcfcHolder                 vcfc        = null;

    /** True if Packet data included */
    protected boolean hasPacket = false;
    
    private final SclkFormatter sclkFmt;

    protected final boolean useFormatters;
    protected final SprintfFormat formatUtil;
    
    /**
     * Constructor
     * 
     * @param appContext
     *            Spring Application Context
     */
    public DatabaseChannelSample(final ApplicationContext appContext)
    {
        super(appContext);
        sclkFmt = TimeProperties.getInstance().getSclkFormatter();
        useFormatters = appContext.getBean(DictionaryProperties.class).useChannelFormatters();
        formatUtil = appContext.getBean(SprintfFormat.class);
    }


    /**
     * Creates an instance of DatabaseChannelSample.
     * 
     * @param appContext
     *            Spring Application Context
     * @param sessionId
     *            The initial test session ID
     * @param fromSse
     *            True if this value is from the SSE, false otherwise
     * @param isRealtime
     *            True if this value is real-time, false otherwise
     * @param sclk
     *            The initial SCLK
     * @param ert
     *            The initial ERT
     * @param scet
     *            The initial SCET
     * @param sol
     *            the initial LST
     * @param v
     *            The initial value
     * @param ct
     *            The initial channel type
     * @param cid
     *            The initial channel ID
     * @param cindex
     *            The initial channel index
     * @param module
     *            the initial FSW module for the channel
     * @param sessionHost
     *            the initial session host
     * @param eu
     *            the initial EU value
     * @param dnAlarm
     *            the initial DN alarm value
     * @param euAlarm
     *            the initial EU alarm value
     * @param status
     *            the initial status
     * @param scid
     *            the initial S/C id
     * @param name
     *            the initial name
     * @param dssId
     *            the initial DSS id
     * @param vcid
     *            the initial VCID
     * @param rct
     *            RCT
     * @param packetId
     *            The packet id
     * @param frameId
     *            The frame id
     */
    @SuppressWarnings("EI_EXPOSE_REP2")
    public DatabaseChannelSample(final ApplicationContext appContext, final Long sessionId,
            final Boolean fromSse,
            final Boolean isRealtime,
            final ISclk sclk,
            final IAccurateDateTime ert,
            final IAccurateDateTime scet,
            final ILocalSolarTime sol,
            final Object v,
            final ChannelType ct,
            final String cid,
            final Long cindex,
            final String module,
            final String sessionHost,
            final Double eu,
            final String dnAlarm,
            final String euAlarm,
            final String status,
            final Integer scid,
            final String name,
            final Integer dssId,
            final Integer vcid,
            final IAccurateDateTime rct,
            final PacketIdHolder packetId,
            final Long    frameId)
    {
        super(appContext, sessionId,sessionHost);
        
        sclkFmt = TimeProperties.getInstance().getSclkFormatter();
        useFormatters = appContext.getBean(DictionaryProperties.class).useChannelFormatters();
        formatUtil = appContext.getBean(SprintfFormat.class);

        this.sclk = sclk;

        if ((this.sclk != null) && this.sclk.isDummy())
        {
            this.sclk = null;
        }

        this.scet = scet;

        if ((this.scet != null) && this.scet.isDummy())
        {
            this.scet = null;
        }

        this.ert = ert;
        lst = (useSolTime ? sol : null);
        value = v;
        this.fromSse = fromSse;
        this.isRealtime = isRealtime;
        channelType = ct;
        channelId = cid;
        channelIndex = cindex;
        this.module = module;
        this.eu = eu;
        dnAlarmState = dnAlarm;
        euAlarmState = euAlarm;
        this.status = status;
        spacecraftId = scid;
        this.name = name;
        
        /*
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        setRecordDssId(dssId);
        setVcid(vcid);

        this.rct = rct;

        setPacketId(packetId);

        this.frameId = frameId;
    }


	/**
     * Set additional columns from Packet. Not NULL.
     *
     * @param pktApid     APID
     * @param pktApidName APID name
     * @param pktSpsc     SPSC
     * @param pktRct      Packet RCT
     * @param pktVcfc     Source VCFC
     */
    @Override
    public void setPacketInfo(final ApidHolder     pktApid,
            final ApidNameHolder              pktApidName,
            final SpscHolder                  pktSpsc,
            final IAccurateDateTime           pktRct,
            final VcfcHolder                  pktVcfc)
    {
        apid     =  pktApid;
        apidName  = pktApidName;
        spsc      = pktSpsc;
        packetRct = pktRct;
        vcfc      = pktVcfc;
        hasPacket = true;
    }


    /**
     * Hashcode of this object.
     *
     * @return int
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("DatabaseChannelSample objects cannot be hashed.");
    }


    /**
     * Equals against another object.
     *
     * @param obj The other object
     *
     * @return True if equal
     */
    @Override
    public boolean equals(final Object obj)
    {
        if((obj instanceof DatabaseChannelSample) == false)
        {
            return(false);
        }

        final DatabaseChannelSample value = (DatabaseChannelSample)obj;

        if(this.getChannelIndex().longValue() != value.getChannelIndex().longValue() ||
                this.getSpacecraftId().intValue() != value.getSpacecraftId().intValue())
        {
            return(false);
        }

        if(this.getSessionHost() == null)
        {
            if(value.getSessionHost() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getSessionHost() == null)
            {
                return(false);
            }
            else if(this.getSessionHost().equals(value.getSessionHost()) == false)
            {
                return(false);
            }
        }

        if(this.getChannelId() == null)
        {
            if(value.getChannelId() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getChannelId() == null)
            {
                return(false);
            }
            else if (! this.getChannelId().equals(value.getChannelId()))
            {
                return(false);
            }
        }

        if(this.getChannelType() == null)
        {
            if(value.getChannelType() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getChannelType() == null)
            {
                return(false);
            }
            else if(!this.getChannelType().equals(value.getChannelType()))
            {
                return(false);
            }
        }

        if(fromSse == null)
        {
            if(value.fromSse != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.fromSse == null)
            {
                return(false);
            }
            else if(fromSse.booleanValue() != value.fromSse.booleanValue())
            {
                return(false);
            }
        }

        if(isRealtime == null)
        {
            if(value.isRealtime != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.isRealtime == null)
            {
                return(false);
            }
            else if(isRealtime.booleanValue() != value.isRealtime.booleanValue())
            {
                return(false);
            }
        }

        if(this.getErt() == null)
        {
            if(value.getErt() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getErt() == null)
            {
                return(false);
            }
            else if(!this.getErt().equals(value.getErt()))
            {
                return(false);
            }
        }

        if(this.getScet() == null)
        {
            if(value.getScet() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getScet() == null)
            {
                return(false);
            }
            else if(!this.getScet().equals(value.getScet())) 
            {
                return(false);
            }
        }

        if (useSolTime)
        {
            if(this.getLst() == null)
            {
                if(value.getLst() != null)
                {
                    return(false);
                }
            }
            else
            {
                if(value.getLst() == null)
                {
                    return(false);
                }
                else if(!this.getLst().equals(value.getLst()))
                {
                    return(false);
                }
            }
        }

        if(this.getSclk() == null)
        {
            if(value.getSclk() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getSclk() == null)
            {
                return(false);
            }
            else if(this.getSclk().getBinaryGdrLong() != value.getSclk().getBinaryGdrLong())
            {
                return(false);
            }
        }
        if(this.getModule() == null)
        {
            if(value.getModule() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getModule() == null)
            {
                return(false);
            }
            else if(!this.getModule().equals(value.getModule()))
            {
                return(false);
            }
        }
        if(this.getDnAlarmState() == null)
        {
            if(value.getDnAlarmState() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getDnAlarmState() == null)
            {
                return(false);
            }
            else if(!this.getDnAlarmState().equals(value.getDnAlarmState()))
            {
                return(false);
            }
        }

        if(this.getEuAlarmState() == null)
        {
            if(value.getEuAlarmState() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getEuAlarmState() == null)
            {
                return(false);
            }
            else if(!this.getEuAlarmState().equals(value.getEuAlarmState()))
            {
                return(false);
            }
        }

        if(this.getValue() == null)
        {
            if(value.getValue() != null)
            {
                return(false);
            }
        }
        else
        {
            if(value.getValue() == null)
            {
                return(false);
            }
            else if(this.getValue().equals(value.getValue()))
            {
                int compareValue = 0;
                /* MPCS-6115 - Added TIME case below. */
                switch(channelType)
                {
                case ASCII:

                    compareValue = ((String)this.getValue()).compareTo(((String)value.getValue()));

                    break;

                case DIGITAL:
                case STATUS:
                case UNSIGNED_INT:
                case SIGNED_INT:
                case TIME:

                    compareValue = ((Long)this.getValue()).compareTo(((Long)value.getValue()));

                    break;

                case FLOAT:
                    /* MPCS-6115 - Removed check for DOUBLE type. */
                    compareValue = ((Double)this.getValue()).compareTo(((Double)value.getValue()));

                    break;

                case BOOLEAN:    

                    compareValue = ((Long)this.getValue()).compareTo(((Long)value.getValue()));

                    break;

                default:

                    throw new IllegalArgumentException("Illegal channel type of " + channelType);
                }


                if(compareValue != 0)
                {
                    return(false);
                }
            }
        }

        return(true);
    }

    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getVcid()
     */
    @Override
    public Integer getVcid() {
        return vcid;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setVcid(final Integer vcid)
    {
        if ((vcid != null) && (vcid < 0))
        {
            this.vcid = null;
        }
        else
        {
            this.vcid = vcid;
        }
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getSclk()
     */
    @Override
    public ISclk getSclk() {
        return sclk;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getErt()
     */
    @Override
    public IAccurateDateTime getErt() {
        return ert;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getScet()
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getScet()
    {
        return scet;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getLst()
     */
    @Override
    public ILocalSolarTime getLst()
    {
        return lst;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getValue()
     */
    @Override
    public Object getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getModule()
     */
    @Override
    public String getModule() {
        return module;
    }


    /**
     * {@inheritDoc}
     * 
     * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
     * static variables to do its work for performance reasons.
     *
     * MPCS-7587 Add named VCID column.
     * 
     */
    @Override
    public String toCsv(final List<String> csvColumns)
    {
        csv.setLength(0);

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    if (sessionId != null)
                    {
                        csv.append(sessionId);
                    }
                    break;

                case "SESSIONHOST":
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case "CHANNELID":
                   if (channelId != null)
                   {
                       csv.append(channelId);
                   }
                   break;

                case "DSSID":
                    /* 
                     * MPCS-6349 : DSS ID not set properly
                     * Removed dssId. Parent class has been updated with 
                     * protected fields sessionDssId and recordDssId with get/set 
                     * methods for both.
                     */
                    csv.append(recordDssId);
                    break;

                case "VCID":
                    if (vcid != null)
                    {
                        csv.append(vcid);
                    }
                    break;

                case "NAME":
                    if (name != null)
                    {
                        csv.append(name);
                    }
                    break;

                case "MODULE":
                    if (module != null)
                    {
                        csv.append(module);
                    }
                    break;

                case "ERT":
                    if (ert != null)
                    {
                        csv.append(ert.getFormattedErtFast(true));
                    }
                    break;

                case "SCET":
                    if (scet != null)
                    {
                        csv.append(scet.getFormattedScetFast(true));
                    }
                    break;

                case "LST":
                    if (useSolTime && (lst != null))
                    {
                        csv.append(lst.getFormattedSolFast(true));
                    }
                    break;

                case "SCLK":
                    if (sclk != null)
                    {
                        csv.append(sclk);
                    }
                    break;

                case CsvQueryProperties.DN:
                    if (value != null)
                    {
                        if (useFormatters && (dnFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(dnFormat, value).trim());
                        }
                        else
                        {
                            csv.append(value);
                        }
                    }
                    break;

                case "EU":
                    if (eu != null)
                    {
                        if (useFormatters && (euFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(euFormat, eu).trim());
                        }
                        else
                        {
                            csv.append(eu);
                        }
                    }
                    break;

                case "STATUS":
                    if ((channelType != null)        &&
                        channelType.hasEnumeration() &&
                        (status != null))
                    {
                        if (useFormatters && (euFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(euFormat, status).trim());
                        }
                        else
                        {
                            csv.append(status);
                        }
                    }
                    break;

                case "DNALARMSTATE":
                    if (dnAlarmState != null)
                    {
                        csv.append(dnAlarmState);
                    }
                    break;

                case "EUALARMSTATE":
                    if (euAlarmState != null)
                    {
                        csv.append(euAlarmState);
                    }
                    break;

                case "REALTIME":
                    if (isRealtime != null)
                    {
                        csv.append(isRealtime);
                    }
                    break;

                case CsvQueryProperties.TYPE:
                    if (channelType != null)
                    {
                        csv.append(channelType);
                    }
                    break;

                case "APID":
                    if (hasPacket && (apid != null))
                    {
                        csv.append(apid);
                    }
                    break;

                case "APIDNAME":
                    if (hasPacket          &&
                        (apidName != null) &&
                        ! apidName.isUnsupported())
                    {
                        csv.append(apidName);
                    }
                    break;

                case "SPSC":
                    if (hasPacket && (spsc != null))
                    {
                        csv.append(spsc);
                    }
                    break;

                case "PACKETRCT":
                    if (hasPacket && (packetRct != null))
                    {
                        csv.append(TimeUtility.format(packetRct));
                    }
                    break;

                case "SOURCEVCFC":
                    if (hasPacket      &&
                        (vcfc != null) &&
                        ! vcfc.isUnsupported())
                    {
                        csv.append(vcfc);
                    }
                    break;

                case "RCT":
                   if (rct != null)
                   {
                       csv.append(FastDateFormat.format(rct, calendar, csv2));
                   }
                   break;

                //MPCS-7587 - Add named VCID column to csv.
                case "VCIDNAME":
                	// MPCS-8021  - updated for better parsing
                	if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                		csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
                	} else {
                		csv.append("");
                	}

                	break;

                default:

                	// MPCS-7587 - Add named VCID column to csv.
                	// MPCS-8021 - updated for better parsing
                	// Put here due to the configurable nature of the column name
                	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                	{
                		if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                			csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
                		} else {
                			csv.append("");
                		}
                	}
                	else if (! csvSkip.contains(upcce))
                	{
                		log.warn("Column " + 
                				cce       +
                				" is not supported, skipped");

                		csvSkip.add(upcce);
                	}

                    break;
            }
        }

		csv.append(CSV_COL_TRL);

		return csv.toString();
    }


    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void parseCsv(final String              csvStr,
                         final List<String> csvColumns)
    {
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */

        // The following removes the start/end quotes w/ the substring
        // and splits based on ",".  It leaves the trailing empty string in the case that 
        // csvStr ends with "".  The empty strings server as place holders.
        final String[] dataArray = csvStr.substring(1, csvStr.length()-1).split("\",\"",-1);

        // dataArray[0] is record type

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId    = null;
        sessionHost  = null;
        channelId    = null;
        recordDssId  = StationIdHolder.UNSPECIFIED_VALUE;
        vcid         = null;
        name         = null;
        module       = null;
        ert          = null;
        scet         = null;
        lst          = null;
        sclk         = null;
        value        = null;
        eu           = null;
        status       = null;
        dnAlarmState = null;
        euAlarmState = null;
        isRealtime   = null;
        channelType  = null;
        apid         = null;
        apidName     = ApidNameHolder.UNSUPPORTED;
        spsc         = null;
        packetRct    = null;
        vcfc         = VcfcHolder.UNSUPPORTED;
        rct          = null;

        int    next    = 1; // Skip recordType
        String token   = null;
        String dnToken = null;

        for (final String cce : csvColumns)
        {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty())
            {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try
            {
                switch (upcce)
                {
                    case "SESSIONID":
                        sessionId = Long.valueOf(token);
                        break;

                    case "SESSIONHOST":
                        sessionHost = token;
                        break; 

                    case "CHANNELID":
                        channelId = token;
                        break;

                    case "DSSID":
                        recordDssId = Integer.valueOf(token);
                        break;

                    case "VCID":
                        vcid = Integer.valueOf(token);
                        break;

                    case "NAME":
                        name = token;
                        break;

                    case "MODULE":
                        module = token;
                        break;

                    case "ERT":
                        ert = new AccurateDateTime(token);
                        break;

                    case "SCET":
                        scet = new AccurateDateTime(token);
                        break;

                    case "LST":
                        if (useSolTime)
                        {
                            lst = LocalSolarTimeFactory.getNewLst(token);
                        }
                        break;

                    case "SCLK":
                        sclk = sclkFmt.valueOf(token);
                        break;

                    case CsvQueryProperties.DN:
                        // Temporary, see below. We need channel type to convert.
                        dnToken = token;
                        break;

                    case "EU":
                        eu = Double.valueOf(token);
                        break;

                    case "STATUS":
                        status = token;
                        break;

                    case "DNALARMSTATE":
                        dnAlarmState = token;
                        break;

                    case "EUALARMSTATE":
                        euAlarmState = token;
                        break;

                    case "REALTIME":
                        isRealtime = Boolean.valueOf(token);
                        break;

                    case CsvQueryProperties.TYPE:
                        channelType = ChannelType.valueOf(token);
                        break;

                    case "APID":
                        apid = ApidHolder.valueOfString(token);
                        break;

                    case "APIDNAME":
                        apidName = ApidNameHolder.valueOf(token);
                        break;

                    case "SPSC":
                        spsc = SpscHolder.valueOfString(token);
                        break;

                    case "PACKETRCT":
                        packetRct = new AccurateDateTime(token);
                        break;

                    case "SOURCEVCFC":
                        vcfc = VcfcHolder.valueOfString(token);
                        break;

                    case "RCT":
                        rct = new AccurateDateTime(token);
                        break;
                        
                    //MPCS-8021 added to handle named vcid column in parseCsv
                    case "VCIDNAME":
                    	//vcid name is mapped, not stored. do nothing
                    	break;

                    default:
                    	//MPCS-8021 added to handle named vcid column in parseCsv. Added here as well due to configurable nature of the column name
                    	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                    	{
                    		//vcid name is mapped, not stored. do nothing
                    	}
                    	else if (! csvSkip.contains(upcce))
                        {
                            log.warn("Column " + 
                                     cce       +
                                     " is not supported, skipped");

                            csvSkip.add(upcce);
                        }

                        break;
                }
             }
             catch (final RuntimeException re)
             {
                 re.printStackTrace();

                 throw re;
		     }
             catch (final Exception e)
             {
                 e.printStackTrace();
             }
        }

        // Format DN as proper type.
        // At end because we need channelType.
        // If DN was received, it will not be null or empty.
        // We must have a valid channelType.

        if (dnToken != null)
        {
            value = formatDnForType(dnToken);
        }
    }


    private Object formatDnForType(final String dn)
    {
        /* MPCS-6115 - Added TIME case below. */
        switch(channelType)
        {
        case ASCII:
        case STATUS:

            return dn;

        case BOOLEAN:

            return GDR.parse_boolean(dn);

        case DIGITAL:
        case UNSIGNED_INT:
        case SIGNED_INT:
        case TIME:

            return GDR.parse_long(dn);

        case FLOAT:	
            /* MPCS-6115 - Removed check for DOUBLE type. */
            return Double.valueOf(dn);

        /** MPCS-7917 Handle as string */
        case UNKNOWN:
            return dn;

        /** MPCS-7917 Handle as string */
        default:

            log.warn("Unsupported channel type " +
                     channelType                 +
                     ", assuming empty string");

            return dn;
        }
    }


    /**
     * {@inheritDoc}
     * 
     * @version MPCS-7587 Add named VCID column.
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map)
    {
        //super.setTemplateContext(map);
        this.setTemplateContextCommon(map);

        if(fromSse != null)
        {
            map.put("fromSse",fromSse);
        }

        if(isRealtime != null)
        {
            map.put("isRealTime",isRealtime);
        }


        if (rct != null)
        {
            // rct should never be null from the database

            map.put("rct", FastDateFormat.format(rct, null, null));
        }

        if (ert != null)
        {
            map.put("ert", ert.getFormattedErtFast(true));
            map.put("ertExact", ert.getTime());
            map.put("ertExactFine", ert.getNanoseconds());
        }

        if (scet != null)
        {
            map.put("scet", scet.getFormattedScetFast(true));
            map.put("scetExact", scet.getTime());
            map.put("scetExactFine", scet.getNanoseconds());
        }


        if (useSolTime && (lst != null))
        {
            map.put("lst", lst.getFormattedSolFast(true));
            map.put("lstExact", lst.getTime());
            map.put("lstExactFine", lst.getSolNumber());
        }

        if (sclk != null)
        {
            map.put("sclk", sclk);
            map.put("sclkCoarse",sclk.getCoarse());
            map.put("sclkFine",sclk.getFine());
        }
        if (module != null) {
            map.put("module", module);
        }

        if (value != null)
        {
            /*
             * MPCS-5526. Formatting data number in templates causes
             * template errors. Changed this to add two variables to the template for
             * data number: one formatted and the other unformatted, so templates can
             * choose which one to use. 
             */
            if (useFormatters && dnFormat != null) {
                map.put("formattedDataNumber", formatUtil.anCsprintf(dnFormat, value));
            } else {
                map.put("formattedDataNumber", value); 
            }
            map.put("dataNumber", value);
            map.put("channelId", (channelId != null) ? channelId : "");
            map.put("channelIndex", channelIndex);
            map.put("name", name);
            map.put("channelType",getChannelType().getBriefChannelType());
        }

        if (eu != null) {
            /*
             * MPCS-5526. Formatting EU in templates causes
             * template errors. Changed this to add two variables to the template for
             * EU: one formatted and the other unformatted, so templates can
             * choose which one to use. 
             */
            if (useFormatters && euFormat != null) {
                map.put("formattedEu", formatUtil.anCsprintf(euFormat, eu));
            } else {
                map.put("formattedEu", eu);
            }
            map.put("eu", eu);
        }

        if (dnAlarmState != null)
        {
            map.put("dnAlarmState", dnAlarmState);


            if (dnAlarmState.equalsIgnoreCase("RED"))
            {
                map.put("redDnType",  "EXCLUSIVE");
                map.put("redDnAlarm", "HIGH");
            }
            else if (dnAlarmState.equalsIgnoreCase("YELLOW"))
            {
                map.put("yellowDnType",  "EXCLUSIVE");
                map.put("yellowDnAlarm", "HIGH");
            }
        }

        if (euAlarmState != null) {
            map.put("euAlarmState", euAlarmState);
        }

        if (deltaValue != null) {
            map.put("delta", deltaValue);
        }
        if (previousValue != null) {
            map.put("previous", previousValue);
        }

        if (channelType != null)
        {
            map.put("channelType",      channelType);
            map.put("channelShortType", channelType.getBriefChannelType());
        }

        /*
         * MPCS-5526. Formatting status in templates causes
         * template errors. Changed this to add two variables to the template for
         * status: one formatted and the other unformatted, so templates can
         * choose which one to use. 
         */
        if (status != null) {
            if (useFormatters && euFormat != null) {
                map.put("formattedStatus", formatUtil.anCsprintf(euFormat, status));
            } else {
                map.put("formattedStatus", status);
            }
            map.put("status", status);
        }
        
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        map.put("dssId", recordDssId);
        
        if (vcid != null)
        {
            map.put("vcid", vcid);
        }
        
        // MPCS-7587 - add mapping of VCID name
        // MPCS-8021 - updated for efficiency
        if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null)
        {
        	map.put(missionProperties.getVcidColumnName(),
        			missionProperties.mapDownlinkVcidToName(this.vcid));
        }

        map.put("spacecraftID",spacecraftId);
        map.put("spacecraftName", missionProperties.mapScidToName(spacecraftId));

        if (hasPacket)
        {
            map.put("hasPacket", true);

            if (apid != null)
            {
                map.put("apid", apid);
            }

            if ((apidName != null) && ! apidName.isUnsupported())
            {
                map.put("apidName", apidName);
            }

            if (spsc != null)
            {
                map.put("spsc", spsc);
            }

            if (packetRct != null)
            {
                map.put("packetRct", TimeUtility.format(packetRct));
            }

            if ((vcfc != null) && ! vcfc.isUnsupported())
            {
                map.put("vcfc", vcfc);
            }
        }
    }


    public void setTemplateContextCommon(final Map<String, Object> map) {
    	super.setTemplateContext(map);
	}


	/* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getChannelId()
     */
    @Override
    public String getChannelId()
    {
        return channelId;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getChannelIndex()
     */
    @Override
    public Long getChannelIndex()
    {
        return channelIndex;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getChannelType()
     */
    @Override
    public ChannelType getChannelType()
    {
        return channelType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getEu()
     */
    @Override
    public Double getEu() {
        return eu;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getFromSse()
     */
    @Override
    public Boolean getFromSse()
    {
        return fromSse;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getDeltaValue()
     */
    @Override
    public Object getDeltaValue() {
        return deltaValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setDeltaValue(final Object deltaValue) {
        this.deltaValue = deltaValue;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getDnAlarmState()
     */
    @Override
    public String getDnAlarmState() {
        return dnAlarmState;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getEuAlarmState()
     */
    @Override
    public String getEuAlarmState() {
        return euAlarmState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPreviousValue(final Object previousValue) {
        this.previousValue = previousValue;

    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getStatus()
     */
    @Override
    public String getStatus() {
        return status;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getSpacecraftId()
     */
    @Override
    public Integer getSpacecraftId()
    {
        return(spacecraftId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpacecraftId(final Integer id)
    {
        spacecraftId = id;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(final String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getIsRealtime()
     */
    @Override
    public Boolean getIsRealtime() {
        return isRealtime;
    }


    /** MPCS-6808 Remove getCsvFieldCount */


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getMtakFieldCount()
     */
    @Override
    public int getMtakFieldCount() {
        return 0;	// MTAK velocity template doesn't exist for this type
    }

    /**
     * Get XML root name
     *
     * @return String
     */
    @Override
    public String getXmlRootName() {
        return XML_ROOT_NAME;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getPreviousValue()
     */
    @Override
    public Object getPreviousValue() {
        return previousValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFromSse(final Boolean fromSse) {
        this.fromSse = fromSse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIsRealtime(final Boolean isRealtime) {
        this.isRealtime = isRealtime;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSclk(final ISclk sclk) {
        this.sclk = sclk;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setScet(final IAccurateDateTime scet)
    {
        this.scet = scet;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setLst(final ILocalSolarTime sol)
    {
        if (useSolTime)
        {
            lst = sol;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setErt(final IAccurateDateTime ert) {
        this.ert = ert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelType(final ChannelType channelType) {
        this.channelType = channelType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelIndex(final Long channelIndex) {
        this.channelIndex = channelIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModule(final String module) {
        this.module = module;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEu(final Double eu) {
        this.eu = eu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDnAlarmState(final String dnAlarmState) {
        this.dnAlarmState = dnAlarmState;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setEuAlarmState(final String euAlarmState) {
        this.euAlarmState = euAlarmState;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getRct()
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getRct()
    {
        return rct;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setRct(final IAccurateDateTime rct)
    {
        this.rct = rct;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getPacketId()
     */
    @Override
    public PacketIdHolder getPacketId()
    {
        return packetId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setPacketId(final PacketIdHolder packetId)
    {
        this.packetId = ((packetId != null)
                ? packetId
                        : PacketIdHolder.UNSUPPORTED);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getFrameId()
     */
    @Override
    public Long getFrameId()
    {
        return frameId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getFrameId(java.lang.Long)
     */
    @Override
    public void getFrameId(final Long frameId)
    {
        this.frameId = frameId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getFileData(java.lang.String)
     */
    @Override
    public Map<String,String> getFileData(final String NO_DATA)
    {	
        final Map<String,String> map = new HashMap<String,String>();

        map.put("ert",ert != null ? ert.getFormattedErtFast(true) : NO_DATA);
        map.put("scet",scet != null ? scet.getFormattedScetFast(true) : NO_DATA);
        map.put("sclk",sclk != null ? sclk.toString() : NO_DATA);

        if (useSolTime)
        {
            map.put("lst",
                    lst != null ? lst.getFormattedSolFast(true) : NO_DATA);
        }

        map.put("type", "EHA_UPDATE");	// for session report .csv file
        map.put("id", channelId != null ? channelId : NO_DATA);
        map.put("data",value != null ? value.toString() : NO_DATA);

        final String channel_name_t = name != null ? name : NO_DATA;
        final String chan_id = channelId != null ? " (" + channelId + ")" : NO_DATA;
        map.put("csv_id",channel_name_t + chan_id);	// for session report .cvs file

        String source = "FSW";
        String channel_name = channel_name_t;
        if (fromSse != null) {
            if (fromSse) {
                source = "SSE";		
                channel_name = "S:" + channel_name_t;
            }
        }
        map.put("source", source);	// for session report .cvs file
        map.put("channel_name", channel_name);

        map.put("eu",NO_DATA);	
        map.put("status", NO_DATA);
        if (channelType != null) {
            if (channelType.equals(ChannelType.STATUS) || channelType.equals(ChannelType.BOOLEAN)) {
                map.put("status",status != null ? status : NO_DATA);
            }
            else if (eu != null) {	
                map.put("eu",eu.toString());		
            }
        }

        if (dnAlarmState != null && !dnAlarmState.equals("NONE")) {
            map.put("dn_alarm_state",dnAlarmState);
        }
        else {
            map.put("dn_alarm_state",NO_DATA);
        }

        if (euAlarmState != null && !euAlarmState.equals("NONE")) {
            map.put("eu_alarm_state",euAlarmState);
        }
        else {
            map.put("eu_alarm_state",NO_DATA);
        }

        map.put("dn",value != null ? value.toString() : NO_DATA);
        
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        map.put("dssId", String.valueOf(recordDssId));

        if (vcid != null)
        {
            map.put("vcid", vcid.toString());
        }

        return map;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateDelta(Object prevVal,
            final Map<String,String> map,
            final String NO_DATA)
    {
        Object delta_val;
        map.put("delta", NO_DATA);
        if (prevVal != null) {
            if (channelType.equals(ChannelType.UNSIGNED_INT) || channelType.equals(ChannelType.SIGNED_INT) || channelType.equals(ChannelType.TIME)) {
                /* MPCS-6115 -  Added TIME case above. */
                // Need to check if it is a String in the case that value was read in by parseCsv():
                if (prevVal instanceof String) {
                    prevVal = Long.valueOf((String)prevVal);
                }
                if (value instanceof String) {
                    value = Long.valueOf((String)value);
                }
                delta_val = ((Long)value - (Long)prevVal);
                map.put("delta",delta_val.toString());
            }
            /* MPCS-6115 - Removed check for DOUBLE type. */
            else if (channelType.equals(ChannelType.FLOAT)) {
                if (prevVal instanceof String) {
                    prevVal = Double.valueOf((String)prevVal);
                }
                if (value instanceof String) {
                    value = Double.valueOf((String)value);
                }
                delta_val = ((Double)value - (Double)prevVal);
                map.put("delta",delta_val.toString());
            }
        }		
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setDnFormat(final String format) {
        this.dnFormat = format;		
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEuFormat(final String format) {
        this.euFormat = format;

    }
    
    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getDnFormat()
     */
    @Override
    public String getDnFormat() {
        return this.dnFormat;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getEuFormat()
     */
    @Override
    public String getEuFormat() {
        return this.euFormat;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getApid()
     */
    @Override
    public ApidHolder getApid()
    {
        return apid;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setApid(final ApidHolder ap)
    {
        apid = ap;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getApidName()
     */
    @Override
    public ApidNameHolder getApidName()
    {
        return apidName;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getSpsc()
     */
    @Override
    public SpscHolder getSpsc()
    {
        return spsc;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpsc(final SpscHolder s)
    {
        spsc = s;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getPacketRCT()
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getPacketRCT()
    {
        return packetRct;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setPacketRct(final IAccurateDateTime rct)
    {
        packetRct = rct;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getVcfc()
     */
    @Override
    public VcfcHolder getVcfc()
    {
        return vcfc;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setVcfc(final VcfcHolder v)
    {
        vcfc = v;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.impl.types.IDbChannelSampleProvider#getCategory()
     */
    @Override
    public ChannelCategoryEnum getCategory()
    {
        return category;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategory(final ChannelCategoryEnum cce)
    {
        category = cce;
    }
}
