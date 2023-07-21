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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.types.IDbPacketUpdater;
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
import jpl.gds.station.api.InvalidFrameCode;


/**
 * This class provides a data object for the binary packet and various
 * attributes that the Packet Database knows explicitly.
 */
public class DatabasePacket extends AbstractDatabaseItem implements IDbPacketUpdater
{
	private static final double DEFAULT_BIT_RATE = 10000.0;
    private static final Calendar      calendar    =
                                           FastDateFormat.getStandardCalendar();
	private static final StringBuilder csv         = new StringBuilder(1024);
	private static final StringBuilder csv2        = new StringBuilder();

    /** MPCS-6808 String constants pushed up */
    private static final String CSV_COL_HDR = DQ + "Packet";

    /** MPCS-6808  Added */
    private static final List<String> csvSkip =
        new ArrayList<String>(0);



	/**
	 * The DB id of this packet.
	 */
	private PacketIdHolder id;

	/**
	 * The record creation time for this packet
	 */
	private IAccurateDateTime rct;

	/**
	 * The spacecraft event time for this packet
	 */
	private IAccurateDateTime scet;

	/**
	 * The local solar time for this packet.
	 */
	private ILocalSolarTime lst = null;
	
	/**
	 * The earth receive time for this packet
	 */
	private IAccurateDateTime ert;

	/**
	 * The spacecraft clock time for this packet
	 */
	private ISclk sclk;

	/**
	 * The APID value for this packet
	 */
	private Integer apid;

	/**
	 * The APID name for this packet
	 */
	private String apidName;

	/* 
	 * MPCS-6349 : DSS ID not set properly
	 * Removed dssId. Parent class has been updated with 
	 * protected fields sessionDssId and recordDssId with get/set 
	 * methods for both.
	 */

	/**
	 * The VCID value for this packet
	 */
	private Integer vcid;

	/**
	 * The source packet sequence counter value for this packet
	 */
	private Integer spsc;

	/**
	 * True if the packet came from the SSE, false otherwise
	 */
	private Boolean fromSse;

	/**
	 * The frame VCFCs in which this packet was found.
	 */
	private List<Long> vcfcs;


	/**
	 * The frame id in which this packet was found.
	 */
    private Long frameId = null;


	/**
	 * True if fill.
	 */
    private boolean fillFlag = false;

    private final SclkFormatter sclkFmt;

    /**
     * Constructor.
     * 
     * @param appContext
     *            Spring Application Context
     */
	public DatabasePacket(final ApplicationContext appContext)
	{
		super(appContext);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
	}


    /**
     * Constructor.
     * 
     * @param appContext
     *            Spring Application Context
     * @param id
     *            Id
     * @param sessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param rct
     *            RCT
     * @param scet
     *            SCET
     * @param ert
     *            ERT
     * @param sclk
     *            SCLK
     * @param sol
     *            LST
     * @param apid
     *            APID
     * @param apidName
     *            APID name
     * @param dssId
     *            DSS id
     * @param vcid
     *            VCID
     * @param spsc
     *            SPSC
     * @param fromSse
     *            From-SSE state
     * @param body
     *            Body bytes
     * @param vcfcs
     *            VCFCS
     * @param fileByteOffset
     *            File byte offset
     * @param frameId
     *            Frame id
     * @param fillFlag
     *            Fill flag
     */
    @SuppressWarnings("EI_EXPOSE_REP2")
	public DatabasePacket(final ApplicationContext appContext, 
						  final PacketIdHolder id,
						  final Long sessionId,
						  final String sessionHost,
						  final IAccurateDateTime rct,
			              final IAccurateDateTime scet,
			              final IAccurateDateTime ert,
			              final ISclk sclk, 
			              final ILocalSolarTime sol,
			              final Integer apid,
			              final String apidName,
			              final int dssId,
			              final Integer vcid,
			              final Integer spsc,
			              final Boolean fromSse,
			              final byte[] body,
			              final List<Long> vcfcs,
			              final Long fileByteOffset,
                          final Long frameId,
                          final boolean fillFlag)
	{
		super(appContext, sessionId,sessionHost);
		
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
		
		this.id = id;
		this.rct = rct;
		this.scet = scet;
		this.ert = ert;
		this.sclk = sclk;
		this.lst = (useSolTime ? sol : null);
		this.apid = apid;
		this.apidName = apidName;
		/* 
		 * MPCS-6349 : DSS ID not set properly
		 * Removed dssId. Parent class has been updated with 
		 * protected fields sessionDssId and recordDssId with get/set 
		 * methods for both.
		 */
		this.recordDssId = dssId;
		this.vcid = vcid;
		this.spsc = spsc;
		this.fromSse = fromSse;

        setRecordBytes(body);

		this.vcfcs = vcfcs;
		this.recordOffset = fileByteOffset;
        this.frameId      = frameId;
        this.fillFlag     = fillFlag;
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public String toString()
	{
        final StringBuilder sb = new StringBuilder("  ");

		sb.append(FastDateFormat.format(this.rct, null, null)).append(' ');
		sb.append(this.ert.getFormattedErt(true)).append(' ');
		sb.append(this.scet.getFormattedScet(true)).append(' ');

		if (useSolTime && (this.lst != null))
        {
			sb.append(this.lst.getFormattedSol(true)).append(' ');
		}

		sb.append(this.sclk).append("   ");
		sb.append(this.apid).append("   ");
		sb.append(this.spsc).append("   ");

		sb.append(getRecordLength());
		
		return sb.toString();
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("DatabasePacket objects are not hashable.");
    }


    /**
     * {@inheritDoc}
     */
	@Override
	public boolean equals(final Object obj) {
		if ((obj instanceof DatabasePacket) == false) {
			return (false);
		}

		final DatabasePacket packet = (DatabasePacket) obj;

		if (this.getApid().intValue() != packet.getApid().intValue()
				|| this.getSpsc().intValue() != packet.getSpsc().intValue()
				|| this.getRecordLength() != packet.getRecordLength())
        {
			return (false);
		}

        final Integer tvcid = this.getVcid();
        final Integer pvcid = packet.getVcid();

        if ((tvcid == null) != (pvcid == null))
        {
            return false;
        }

        // So either both are null or neither is null

        if ((tvcid != null) && (tvcid.intValue() != pvcid.intValue()))
        {
            return false;
		}
        
    	/* 
    	 * MPCS-6349 : DSS ID not set properly
    	 * Removed dssId. Parent class has been updated with 
    	 * protected fields sessionDssId and recordDssId with get/set 
    	 * methods for both.
    	 */
        if((this.recordDssId < 0) != (packet.recordDssId < 0)) {
        	return false;
        }
        
        if(this.recordDssId >= 0 && (this.recordDssId != packet.recordDssId)) {
        	return false;
        }

		if (this.getScet() == null) {
			if (packet.getScet() != null) {
				return (false);
			}
		} else {
			if (packet.getScet() == null) {
				return (false);
			} else if (!this.getScet().equals(packet.getScet())) {
				return (false);
			}
		}

        if (useSolTime)
        {
            if (this.getLst() == null)
            {
                if (packet.getLst() != null)
                {
                    return (false);
                }
            }
            else
            {
                if (packet.getLst() == null)
                {
                    return (false);
                }
                else if (!this.getLst().equals(packet.getLst()))
                {
                    return (false);
                }
            }
        }

		if (this.getErt() == null) {
			if (packet.getErt() != null) {
				return (false);
			}
		} else {
			if (packet.getErt() == null) {
				return (false);
			} else if (!this.getErt().equals(packet.getErt())) {
				return (false);
			}
		}

		if (this.getSclk() == null) {
			if (packet.getSclk() != null) {
				return (false);
			}
		} else {
			if (packet.getSclk() == null) {
				return (false);
			} else if ((this.getSclk().getCoarse() != packet.getSclk()
					.getCoarse())
					|| this.getSclk().getFine() != packet.getSclk().getFine()) {
				return (false);
			}
		}


        // MPCS-5189
        // If both the bodies and the lengths are equal, we are equal.
        // We have to check both because if there are no bodies, the bodies
        // will be zero-length and the lengths may not be.

        if (! Arrays.equals(getRecordBytes(), packet.getRecordBytes()))
        {
            return false;
        }

        if (getRecordLength() != packet.getRecordLength())
        {
            return false;
        }

		if (this.fromSse.booleanValue() != packet.fromSse.booleanValue()) {
			return (false);
		}

		if (this.vcfcs == null && packet.getVcfcs() != null
				|| this.vcfcs != null && packet.getVcfcs() == null) {
			return false;
		}

		if (this.vcfcs == null) {
			return true;
		}

		if (this.vcfcs.size() != packet.getVcfcs().size()) {
			return false;
		}

		
		for (int i = 0; i < this.vcfcs.size(); i++) {
			if (!this.vcfcs.get(i).equals(packet.getVcfcs().get(i))) {
				return false;
			}
		}
		return (true);
	}


	/**
     * {@inheritDoc}
	 * 
	 * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
	 * static variables to do its work for performance reasons.
     *
     * @version MPCS-6808 Massive rewrite
     * @version MPCS-7587 Add named VCID column.
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

                case "RCT":
                   if (rct != null)
                   {
                       csv.append(FastDateFormat.format(rct, calendar, csv2));
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

                case "SCLK":
                    if (sclk != null)
                    {
                        csv.append(sclk);
                    }
                    break;

                case "VCID":
                    if (vcid != null)
                    {
                        csv.append(vcid);
                    }
                    break;

                case "DSSID":
                    csv.append(recordDssId); // int
                    break;

                case "APID":
                    if (apid != null)
                    {
                        csv.append(apid);
                    }
                    break;

                case "APIDNAME":
                    if (apidName != null)
                    {
                        csv.append(apidName);
                    }
                    break;

                case "FROMSSE":
                    if (fromSse != null)
                    {
                        csv.append(fromSse);
                    }
                    break;

                case "SPSC":
                    if (spsc != null)
                    {
                        csv.append(spsc);
                    }
                    break;

                case "LENGTH":
                    csv.append(getRecordLength()); // int
                    break;

                case "SOURCEVCFCS":
                    if (vcfcs != null)
                    {
                        boolean first = true;

                        for (final long vcfc : vcfcs)
                        {
                            if (first)
                            {
                                first = false;
                            }
                            else
                            {
                                csv.append(':');
                            }

                            csv.append(vcfc);
                        }
                    }
                    break;

                case "FILEBYTEOFFSET":
                    if (recordOffset != null)
                    {
                        csv.append(recordOffset);
                    }
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

                	// MPCS-7587  Add named VCID column to csv.
                	// MPCS-8021  - updated for better parsing
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
        rct          = null;
        scet         = null;
        lst          = null;
        ert          = null;
        sclk         = null;
        vcid         = null;
        recordDssId  = StationIdHolder.UNSPECIFIED_VALUE;
        apid         = null;
        apidName     = null;
        fromSse      = null;
        spsc         = null;
        vcfcs        = null;
        recordOffset = null;

		//LST may not be present:
        //
        //recordType,sessionId,sessionHost,rct,scet,lst,ert,sclk,
        //vcid,dssId,apid,apidName,fromSse,spsc,length,sourceVcfcs,
        //fileByteOffset

        int    next  = 1; // Skip recordType
        String token = null;

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

                    case "RCT":
                        rct = new AccurateDateTime(token);
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

                    case "ERT":
                        ert = new AccurateDateTime(token);
                        break; 

                    case "SCLK":
                        sclk = sclkFmt.valueOf(token);
                        break; 

                    case "VCID":
                        try
                        {
                            vcid = Integer.valueOf(token);
                        }
                        catch (final NumberFormatException e)
                        {
                            vcid = getTransformedStringId(token);
                        }
                        break;

                    case "DSSID":
                        recordDssId = Integer.valueOf(token);
                        break;

                    case "APID":
                        apid = Integer.valueOf(token);
                        break; 

                    case "APIDNAME":
                        apidName = token;
                        break;

                    case "FROMSSE":
                        fromSse = Boolean.valueOf(token);
                        break;

                    case "SPSC":
                        spsc = Integer.valueOf(token);
                        break;

                    case "LENGTH":
                        // Nothing to do
                        break;

                    case "SOURCEVCFCS":
                        final String[] tokens = token.split(":");

                        vcfcs = new ArrayList<Long>(tokens.length);

                        for (final String vcfc : tokens)
                        {
                            vcfcs.add(Long.valueOf(vcfc));
                        }
                        break;

                    case "FILEBYTEOFFSET":
                        recordOffset = Long.valueOf(token);
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
	}


    /**
     * {@inheritDoc}
     * 
     * MPCS-7587 Add named VCID column
     */
    @Override
	public void setTemplateContext(final Map<String,Object> map)
	{
		super.setTemplateContext(map);

        if (this.rct != null)
        {
            map.put("rct", FastDateFormat.format(this.rct, null, null));
        }

        if (this.scet != null)
        {
            map.put("scet", this.scet.getFormattedScetFast(true));
        }
        
        if (useSolTime && (this.lst != null))
        {
            map.put("lst", this.lst.getFormattedSolFast(true));
        }

        if (this.ert != null)
        {
            map.put("ert", this.ert.getFormattedErtFast(true));
        }

        if (this.sclk != null)
        {
            map.put("sclk", this.sclk);
        }

        final Integer zero = Integer.valueOf(0);

        map.put("vcid", this.vcid != null ? this.vcid : zero);

        //MPCS-7587 - add mapping of VCID name
        // MPCS-8021 - updated for efficiency
        if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null)
        {
        	map.put(missionProperties.getVcidColumnName(),
        			missionProperties.mapDownlinkVcidToName(this.vcid));
        }

		/* 
		 * MPCS-6349: DSS ID not set properly
		 * Removed dssId. Parent class has been updated with 
		 * protected fields sessionDssId and recordDssId with get/set 
		 * methods for both.
		 */
		map.put("dssId", this.recordDssId >= 0 ? this.recordDssId : "");
		map.put("apid", this.apid != null ? this.apid : zero);
		map.put("apidName", this.apidName != null ? this.apidName : "");
		
		map.put("spsc", (this.spsc != null) ? this.spsc : zero);
		map.put("fromSse",
                (this.fromSse != null) ? this.fromSse : Boolean.FALSE);

		if (this.vcfcs != null)
		{
            final StringBuilder sources = new StringBuilder(128);
            final int           size    = this.vcfcs.size();

	       	for (int i = 0; i < size; ++i)
	       	{
	       		if (i != 0)
	       		{
	       			sources.append(';');
	       		}

	       		sources.append(this.vcfcs.get(i));
	       	}

			map.put("sourceVcfcs", sources.toString());
		}
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public PacketIdHolder getPacketId()
    {
		return id;
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
    public IAccurateDateTime getErt() {
    	return ert;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ISclk getSclk() {
    	return sclk;
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
    public Long getFrameId()
    {
    	return frameId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getVcfcs() {
    	return vcfcs;
    }


    /**
     * Returns a map of data to be displayed to various output files.
     *
     * @param NO_DATA is the string to be used to represent no data
     *
     * @return Populated map or null
     */
    @Override
    public Map<String,String> getFileData(final String NO_DATA) {
    	return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public InvalidFrameCode getBadReason()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Double getBitRate() {
        return DEFAULT_BIT_RATE;
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
    public Integer getRecordDssIdAsInt() {
        return recordDssId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSpsc() {
    	return spsc;
    }


    /**
     * {@inheritDoc}
     */
	@Override
    public void setPacketId(final PacketIdHolder id)
    {
		this.id = id;
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
    @SuppressWarnings("EI_EXPOSE_REP2")
	public void setScet(final IAccurateDateTime scet)
    {
		this.scet = scet;
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ILocalSolarTime getLst() {
    	return this.lst;
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
    public void setErt(final IAccurateDateTime ert) {
		this.ert = ert;
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
    public Integer getApid() {
		return apid;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setApid(final Integer apid) {
		this.apid = apid;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String getApidName()
    {
		return apidName;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setApidName(final String apidName)
    {
		this.apidName = apidName;
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
    public void setVcid(final Integer vcid) {
		this.vcid = vcid;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setSpsc(final Integer spsc) {
		this.spsc = spsc;
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
    public boolean getFillFlag()
    {
		return fillFlag;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public void setFillFlag(final boolean fill)
    {
		this.fillFlag = fill;
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public void setFrameId(final Long frameId)
    {
		this.frameId = frameId;
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public void setSourceVcfcs(final List<Long> sourceVcfcs) {
		this.vcfcs = sourceVcfcs;
	}
}
