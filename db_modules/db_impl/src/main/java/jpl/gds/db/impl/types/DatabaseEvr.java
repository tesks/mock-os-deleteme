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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.db.api.types.IDbEvrUpdater;
//MPCS-7587 Added to support VCID name column
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.Pair;


/**
 * A representation of an EVR as it's stored in the database
 */
public class DatabaseEvr extends AbstractDatabaseItem implements IDbEvrUpdater
{
    private static final Calendar      calendar    =
                                           FastDateFormat.getStandardCalendar();
	private static final StringBuilder csv         = new StringBuilder(1024);
	private static final StringBuilder csv2        = new StringBuilder();

    /** MPCS-6808 Most of these are inherited */
    private static final String CSV_COL_HDR = DQ + "Evr";

	private Long id;
	private String name;
	private Long eventId;
	private IAccurateDateTime ert;
	private IAccurateDateTime scet;
	private IAccurateDateTime rct;
	private ISclk sclk;
	private ILocalSolarTime lst = null;
	private String level;
	private String module;
	private String message;
	private Boolean fromSse;
	private Boolean isRealtime;
    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    private Integer vcid;

    /** MPCS-5935 Use holder */
    private PacketIdHolder packetId = PacketIdHolder.UNSUPPORTED;

    private final EvrMetadata metadata = new EvrMetadata();

    private static final List<String> csvSkip =
        new ArrayList<String>(0);
    
    private final SclkFormatter sclkFmt;


    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public DatabaseEvr(final ApplicationContext appContext)
	{
		super(appContext);	
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public Long getId() {
		return id;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setId(final Long id) {
		this.id = id;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getName() {
		return name;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setName(final String name) {
		this.name = name;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public Long getEventId() {
		return eventId;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setEventId(final Long eventId) {
		this.eventId = eventId;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public IAccurateDateTime getErt() {
		return ert;
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
    @SuppressWarnings("EI_EXPOSE_REP")
	public IAccurateDateTime getScet()
    {
		return scet;
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
            this.lst = sol;
        }
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
    public ILocalSolarTime getLst()
	{
		return this.lst;
	}

    /**
     * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
	@Override
    public ISclk getSclk() {
		return sclk;
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
    public String getLevel() {
		return level;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setLevel(final String level) {
		this.level = level;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getModule() {
		return module;
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
    public String getMessage() {
		return message;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setMessage(final String message) {
		this.message = message;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public Boolean getFromSse() {
		return fromSse;
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
    public Boolean getIsRealtime() {
		return isRealtime;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setIsRealtime(final Boolean isRealtime) {
		this.isRealtime = isRealtime;
	}

    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
	
    /**
     * {@inheritDoc}
     */
	@Override
    public Integer getVcid() {
		return vcid;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setVcid(final Integer vcid) {
		this.vcid = vcid;
	}


    /**
     * {@inheritDoc}
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
    public void setPacketId(final PacketIdHolder packetId) {
		this.packetId = packetId;
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public EvrMetadata getMetadata()
    {
		return metadata;
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public void setMetadata(final EvrMetadata em)
    {
        metadata.set(em);
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public void addKeyValue(final EvrMetadataKeywordEnum key,
                            final String                 value)
    {
        metadata.addKeyValue(key, value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addKeyValue(final String key,
                            final String value)
    {
        metadata.addKeyValue(key, value);
    }


	/**
     * {@inheritDoc}
	 * 
	 * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
	 * static variables to do its work for performance reasons.
     *
     * MPCS-7587 Add named VCID column.
	 */
    @Override
	public String toCsv(final List<String> csvColumns)
	{
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */

        boolean first    = true;
        int     keywords = 0;
        int     values   = 0;

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

                case "LEVEL":
                    if (level != null)
                    {
                        csv.append(level);
                    }
                    break;

                case "EVENTID":
                    if (eventId != null)
                    {
                        csv.append(eventId);
                    }
                    break;

                case "VCID":
                    if (vcid != null)
                    {
                        csv.append(vcid);
                    }
                    break;

                case "DSSID":
                    csv.append(recordDssId); // NB: int
                    break;

                case "FROMSSE":
                    if (fromSse != null)
                    {
                        csv.append(fromSse);
                    }
                    break;

                case "REALTIME":
                    if (isRealtime != null)
                    {
                        csv.append(isRealtime);
                    }
                    break;

                case "SCLK":
                    if (sclk != null)
                    {
                        csv.append(sclk);
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

                case "ERT":
                    if (ert != null)
                    {
                        csv.append(ert.getFormattedErtFast(true));
                    }
                    break;

                case "RCT":
                    if (rct != null)
                    {
                        csv.append(FastDateFormat.format(rct, calendar, csv2));
                    }
                    break;

                case "MESSAGE":
                    if (message != null)
                    {
                        // Replacing embedded quotes with single quotes

                        csv.append(message.replaceAll(DQ, SQ));
                    }
                    break;

                case CsvQueryProperties.METADATAKEYWORDLIST:
                    csv.append('[');

                    if ((metadata != null) && ! metadata.isEmpty())
                    {
                        first    = true;
                        keywords = metadata.size();

                        for (final Pair<EvrMetadataKeywordEnum, String> p : metadata.asStrings())
                        {
                            if (first)
                            {
                                first = false;
                            }
                            else
                            {
                                csv.append(',');
                            }

                            csv.append('(').append(p.getOne().convert()).append(')');
                        }
                    }

                    csv.append(']');
                    break;

                case CsvQueryProperties.METADATAVALUESLIST:
                    csv.append('[');

                    if ((metadata != null) && ! metadata.isEmpty())
                    {
                        first  = true;
                        values = metadata.size();

                        for (final Pair<EvrMetadataKeywordEnum, String> p : metadata.asStrings())
                        {
                            if (first)
                            {
                                first = false;
                            }
                            else
                            {
                                csv.append(',');
                            }

                            csv.append('(').append(p.getTwo()).append(')');
                        }
                    }

                    csv.append(']');
                    break;

                //MPCS-7587 Add named VCID column to csv.
                case "VCIDNAME":
                	// MPCS-8021 - updated for better parsing
                	if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                		csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
                	} else {
                		csv.append("");
                	}

                	break;

                default:

                	// MPCS-7587 Add named VCID column to csv.
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

        if (keywords != values)
        {
            throw new IllegalArgumentException("CSV number of keys and values don't match! (" +
                                               keywords                                       +
                                               " keys and "                                   +
                                               values                                         +
                                               " values)");
        }

		return csv.toString();
	}

	
    /**
     * {@inheritDoc}
     *
     * @version MPCS-6808 Massive rewrite
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

        // LST may not be present:
		// recordType,sessionId,sessionHost,name,module,level,eventId,vcid,dssId,fromSse,
        // realtime,sclk,scet,lst,ert,rct,message,metadataKeywordList,metadataValuesList
		
		//dataArray[0] is recordType

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId   = null;
        sessionHost = null;
        name        = null;
        module      = null;
        level       = null;
        eventId     = null;
        recordDssId = StationIdHolder.UNSPECIFIED_VALUE;
        vcid        = null;
        fromSse     = null;
        isRealtime  = null;
        sclk        = null;
        scet        = null;
        lst         = null;
        ert         = null;
        rct         = null;
        message     = null;

        metadata.clear();

        int      next       = 1; // Skip recordType
        String   token      = null;
        String[] keyArray   = new String[0];
        String[] valueArray = new String[0];

        for (final String cce : csvColumns)
        {
            if (next >= dataArray.length)
            {
                // Skip extras
                break;
            }

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

                    case "NAME":
                        name = token;
                        break;

                    case "MODULE":
                        module = token;
                        break;

                    case "LEVEL":
                        level = token;
                        break;

                    case "EVENTID":
                        eventId = Long.valueOf(token);
                        break;

                    case "DSSID":
                        recordDssId = Integer.valueOf(token);
                        break;

                    case "VCID":
                        vcid = Integer.valueOf(token);
                        break;

                    case "FROMSSE":
                        fromSse = Boolean.valueOf(token);
                        break;

                    case "REALTIME":
                        isRealtime = Boolean.valueOf(token);
                        break;

                    case "SCLK":
                        sclk = sclkFmt.valueOf(token);
                        break;

                    case "SCET":
                        scet = new AccurateDateTime(token);
                        break;

                    case "LST":
                        lst = (useSolTime ? LocalSolarTimeFactory.getNewLst(token) : null);
                        break;

                    case "ERT":
                        ert = new AccurateDateTime(token);
                        break;

                    case "RCT":
                        rct = new AccurateDateTime(token);
                        break;

                    case "MESSAGE":
                        message = token;
                        break;

                    case CsvQueryProperties.METADATAKEYWORDLIST:
                        if (! token.equals("[]"))
                        {
                            // Chop off beginning [( and ending )]
                            // then split on ),( to get arrays of keys and values

                            keyArray = token.substring(2, token.length() - 2).split("\\),\\(", -1);
                        }
                        break;

                    case CsvQueryProperties.METADATAVALUESLIST:
                        if (! token.equals("[]"))
                        {
                            // Chop off beginning [( and ending )]
                            // then split on ),( to get arrays of keys and values

                            valueArray = token.substring(2, token.length() - 2).split("\\),\\(", -1);
                        }
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

        if (keyArray.length != valueArray.length)
        {
            throw new IllegalArgumentException("CSV number of keys and values don't match! (" +
                                               keyArray.length                                +
                                               " keys and "                                   +
                                               valueArray.length                                +
                                               " values)");
        }

        for (int k = 0; k < keyArray.length; ++k)
        {
            metadata.addKeyValue(keyArray[k], valueArray[k]);
        }
	}


    /**
     * {@inheritDoc}
     * 
     * MPCS-7587 Add named VCID column.
     */
	@Override
	public void setTemplateContext(final Map<String,Object> map)
	{
        final Long zero = Long.valueOf(0L);

		super.setTemplateContext(map);

        if ((this.name != null) && ! this.name.isEmpty())
        {
            map.put("name", this.name);
        }

		map.put("event", this.eventId); //don't care if it's null
		
        //null value is checked in velocity template
        map.put("vcid", vcid);
        
        //MPCS-7587 - add mapping of VCID name
        // MPCS-8021 - updated for efficiency
        if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null)
        {
        	map.put(missionProperties.getVcidColumnName(),
        			missionProperties.mapDownlinkVcidToName(this.vcid));
        }
      		
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        map.put("dssId", recordDssId);

        if (this.ert != null)
        {
            map.put("ert", this.ert.getFormattedErtFast(true));
            map.put("ertExact", this.ert.getTime());
            map.put("ertExactFine", this.ert.getNanoseconds());
        }
        else
        {
            map.put("ert", "");
            map.put("ertExact", zero);
            map.put("ertExactFine", zero);
        }

        if (this.scet != null)
        {
            map.put("scet", this.scet.getFormattedScetFast(true));
            map.put("scetExact", this.scet.getTime());
            map.put("scetExactFine", this.scet.getNanoseconds());
        }
        else
        {
            map.put("scet", "");
            map.put("scetExact", zero);
            map.put("scetExactFine", zero);
        }
        
        if (useSolTime && (this.lst != null))
        {
            map.put("lst", this.lst.getFormattedSolFast(true));
            map.put("lstExact", this.lst.getTime());
            map.put("lstExactFine", this.lst.getSolNumber());
        }

        if (this.rct != null)
        {
            map.put("rct", FastDateFormat.format(this.rct, null, null));
            map.put("rctExact", this.rct.getTime());
        }
        else
        {
            map.put("rct", "");

            map.put("rctExact", zero);
        }

        if (this.sclk != null)
        {
            map.put("sclk", this.sclk);
            map.put("sclkCoarse", this.sclk.getCoarse());
            map.put("sclkFine", this.sclk.getFine());
        }
        else
        {
            map.put("sclk", "");
            map.put("sclkCoarse", zero);
            map.put("sclkFine", zero);
        }

        if ((this.level != null) && ! this.level.isEmpty())
        {
            map.put("level", this.level);
        }

        if ((this.module != null) && ! this.module.isEmpty())
        {
            map.put("module", this.module);
        }

        map.put("message", this.message != null ? this.message : "");
        map.put("fromSse", this.fromSse); //don't care if it's null
        map.put("realTime",this.isRealtime); //don't care if it's null
        
        if (! metadata.isEmpty())
        {
            final StringBuilder mdKeysList = new StringBuilder();
            final StringBuilder mdValsList = new StringBuilder();

            final List<Pair<EvrMetadataKeywordEnum, String>> kv =
                 metadata.asStrings();
            final int                               size = kv.size();
            final List<EvrMetadataKeywordEnum>      keys =
                 new ArrayList<EvrMetadataKeywordEnum>(size);
            final List<String> vals = new ArrayList<String>(size);

            mdKeysList.append('[');
            mdValsList.append('[');

            boolean first = true;

            for (final Pair<EvrMetadataKeywordEnum, String> p : kv)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    mdKeysList.append(',');
                    mdValsList.append(',');
                }

                final EvrMetadataKeywordEnum one = p.getOne();
                final String                 two = p.getTwo();

                keys.add(one);
                vals.add(two);

                mdKeysList.append('(').append(one.convert()).append(')');
                mdValsList.append('(').append(two).append(')');
            }

            mdKeysList.append(']');
            mdValsList.append(']');

            map.put("mdKeysList", mdKeysList.toString());
            map.put("mdValsList", mdValsList.toString());
            map.put("metadataKeys", keys);
            map.put("metadataVals", vals);
        }
        else
        {
        	map.put("mdKeysList", "");
            map.put("mdValsList", "");
            map.put("metadataKeys", new ArrayList<String>(0));
            map.put("metadataVals", new ArrayList<String>(0));
        }
	}


    /**
     * Hash to go along with equals.
     *
     * @return Hash code
     *
     * @throws UnsupportedOperationException Because not hashable
     */
    @Override
	public int hashCode() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException(
                      "DatabaseEvr objects are not hashable");
    }


    /**
     * Equals for maps, etc.
     *
     * @param obj The object to compare against
     *
     * @return True if equal
     */
	@Override
	public boolean equals(final Object obj)
	{
		if (! (obj instanceof DatabaseEvr))
		{
			return(false);
		}

		final DatabaseEvr evr = (DatabaseEvr)obj;

		if(this.id == null)
		{
			if(evr.id != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.id == null)
			{
				return(false);
			}
			else if(this.id.equals(evr.id) == false)
			{
				return(false);
			}
		}
		
		if(this.name == null)
		{
			if(evr.name != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.name == null)
			{
				return(false);
			}
			else if(this.name.equals(evr.name) == false)
			{
				return(false);
			}
		}

		if(this.eventId == null)
		{
			if(evr.eventId != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.eventId == null)
			{
				return(false);
			}
			else if(this.eventId.equals(evr.eventId) == false)
			{
				return(false);
			}
		}
		
		if(this.vcid == null)
		{
			if(evr.vcid != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.vcid == null)
			{
				return(false);
			}
			else if(this.vcid.equals(evr.vcid) == false)
			{
				return(false);
			}
		}
		
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */		
		if(this.recordDssId < 0)
		{
			if(evr.recordDssId >= 0)
			{
				return(false);
			}
		}
		else
		{
			if(evr.recordDssId < 0 )
			{
				return(false);
			}
			else if((this.recordDssId == evr.recordDssId) == false)
			{
				return(false);
			}
		}
		
		if(this.ert == null)
		{
			if(evr.ert != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.ert == null)
			{
				return(false);
			}
			else if(this.ert.getTime() != evr.ert.getTime())
			{
				return(false);
			}
		}
		
		if(this.scet == null)
		{
			if(evr.scet != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.scet == null)
			{
				return(false);
			}
			else if(this.scet.getTime() != evr.scet.getTime())
			{
				return(false);
			}
		}

        if (useSolTime)
        {
            if(this.lst == null)
            {
                if(evr.lst != null)
                {
                    return(false);
                }
            }
            else
            {
                if(evr.lst == null)
                {
                    return(false);
                }
                else if(!this.lst.equals(evr.lst))
                {
                    return(false);
                }
            }
        }
		
		if(this.rct == null)
		{
			if(evr.rct != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.rct == null)
			{
				return(false);
			}
			else if(this.rct.getTime() != evr.rct.getTime())
			{
				return(false);
			}
		}
		
		if(this.sclk == null)
		{
			if(evr.sclk != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.sclk == null)
			{
				return(false);
			}
			else if(this.sclk.getBinaryGdrLong() != evr.sclk.getBinaryGdrLong())
			{
				return(false);
			}
		}
		
		if(this.level == null)
		{
			if(evr.level != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.level == null)
			{
				return(false);
			}
			else if(this.level.equals(evr.level) == false)
			{
				return(false);
			}
		}
		
		if(this.module == null)
		{
			if(evr.module != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.module == null)
			{
				return(false);
			}
			else if(this.module.equals(evr.module) == false)
			{
				return(false);
			}
		}
		
		if(this.message == null)
		{
			if(evr.message != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.message == null)
			{
				return(false);
			}
			else if(this.message.equals(evr.message) == false)
			{
				return(false);
			}
		}
		
		if(this.fromSse == null)
		{
			if(evr.fromSse != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.fromSse == null)
			{
				return(false);
			}
			else if(this.fromSse.equals(evr.fromSse) == false)
			{
				return(false);
			}
		}
		
		if(this.isRealtime == null)
		{
			if(evr.isRealtime != null)
			{
				return(false);
			}
		}
		else
		{
			if(evr.isRealtime == null)
			{
				return(false);
			}
			else if(this.isRealtime.equals(evr.isRealtime) == false)
			{
				return(false);
			}
		}

        return metadata.equals(evr.metadata);
	}
	

	/**
	 * Returns a map of data to be displayed to various output files.
     *
	 * @param NO_DATA is the string to be used to represent no data
     *
     * @return Populated map
	 */
    @Override
	public Map<String, String> getFileData(final String NO_DATA)
    {	
		final DateFormat df = TimeUtility.getFormatterFromPool();
		final Map<String,String> map = new HashMap<String,String>();
		
		map.put("evr_name",name != null ? name : NO_DATA);			    			
		map.put("ert",ert != null ? df.format(ert) : NO_DATA);
		map.put("scet",scet != null ? df.format(scet) : NO_DATA);

        if (useSolTime)
        {
            map.put("lst",lst != null ? lst.getFormattedSolFast(false) : NO_DATA);
        }

		map.put("sclk",sclk != null ? sclk.toString() : NO_DATA);
		
		if (fromSse == null || isRealtime == null) {
			map.put("source", NO_DATA);
		}
		else {
			if (fromSse) {
				map.put("source","SSE");
			}
			else {
				if (isRealtime) 
					map.put("source","FSW RT");
				else
					map.put("source","FSW REC");
			}
		}
		
		map.put("level",level != null ? level : NO_DATA);
		map.put("type", level == null ? "EVR" : "EVR " + level.toUpperCase());	 // For session report csv format
		map.put("module", module != null ? module : NO_DATA);
		
		String seq_id = null;
		if (metadata != null) {
			seq_id = metadata.getMetadataValue(EvrMetadataKeywordEnum.SEQUENCEID);
		}
		map.put("seq_id",seq_id != null ? seq_id : NO_DATA);	
		map.put("id",seq_id != null ? seq_id : "NOT_FOUND");
		// TODO Eventually we should not have to remove the newlines:
		String message_clean = message != null ? message.replaceAll("[\r\n]+", " ") : NO_DATA; // removes the newlines from message
		map.put("data", message_clean);

		message_clean = message_clean.replaceAll("\"", ""); // Removing embedded quotes
		message_clean = "\"" + message_clean + "\""; // Adding starting/ending quotes
		
		map.put("data_sr_csv",message_clean);	// for session report csv format
		
		TimeUtility.releaseFormatterToPool(df);
		
		return map;
	}
}
