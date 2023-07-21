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
package jpl.gds.db.app;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.FrameQueryOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.SpscRanges;
import jpl.gds.db.api.sql.fetch.VcfcRanges;
import jpl.gds.db.api.sql.order.IPacketOrderByType;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbPacketProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.order.FrameOrderByType;
import jpl.gds.db.mysql.impl.sql.order.PacketOrderByType;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.time.api.config.TimeCorrelationProperties;
import jpl.gds.time.api.service.ITimeCorrelationParser;

/**
 * This class is an application that queries the database for packets with a specific APID and
 * displays information about the gaps in the packet sequence.
 * 
 * Translated from Jesse Wright's chill_packet_gaps python script.
 *
 */
public class TimeCorrelationApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_time_corr");
    private static final int NUM_QUERY_PARAMS = 8;
    private static final long MILLISECONDS_IN_HOUR = 3600000;
    
    private static final String TC_FORMAT_LONG = "tcFileFormat";
    private static final String TC_FORMAT_SHORT = "o";
	private ITimeCorrelationParser pktParser;
	
    /** 
     * The APIDs to query by 
     */
    private final ApidRanges apid;
    /**
     * The packet sequence numbers to query by.
     */
    private final SpscRanges spsc;
    
    /**
     * Fetch object for frames.
     */
    private IFrameFetch frameFetch;
    
    /**
     * The virtual channel to query by
     */
//    private final int vcid = -1;
  
    private boolean useTcFormat = false;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;
    
    /** DSS IDS to query for */
    private Set<Integer> dssids = null;
    
    /**
     * Creates an instance of TimeCorrelationApp.
     */
    public TimeCorrelationApp()
    {
        super(IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
              APP_NAME,
              null);

        this.apid = new ApidRanges();
        this.spsc = new SpscRanges();

    }

    /**
     * Gets the APID entered by the user, as an ApidRanges object.
     * 
     * @return ApidRanges, which in this case should define only one APID
     */
    public ApidRanges getApid()
	{
		return this.apid;
	}

		
    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {    	
    	super.createRequiredOptions();
        this.requiredOptions.add(BEGIN_TIME_LONG);
        this.requiredOptions.add(END_TIME_LONG);
        this.requiredOptions.add(DssVcidOptions.VCID_OPTION_LONG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws MissingOptionException, ParseException
    {
        super.configureApp(cmdline);
        fromSse = Boolean.FALSE;
        
        if(cmdline.hasOption(PACKET_SPSC_SHORT))
        {
            spsc.processOptions(cmdline.getOptionValue(PACKET_SPSC_SHORT),PACKET_SPSC_SHORT);
        }
        
        this.useTcFormat = cmdline.hasOption(TC_FORMAT_SHORT);
        
        final int timeApid = appContext.getBean(TimeCorrelationProperties.class).getTcPacketApid(false);
        apid.addLong(timeApid);
 
        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline, (String) null);
        if (vcids != null && vcids.size() > 1) {
        	throw new ParseException("Only one VCID may be entered for time correlation queries");
        }
        dssids = DssVcidOptions.parseDssId(cmdline, (String) null);
        if (dssids != null && dssids.size() > 1) {
        	throw new ParseException("Only one DSS ID may be entered for time correlation queries");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getPacketFetch(sqlStmtOnly);
    	return fetch;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];

    	IPacketOrderByType orderType = PacketOrderByType.DEFAULT;
    	if(this.orderByString != null)
    	{
    		try
    		{
    			orderType = new PacketOrderByType(this.orderByString.trim());
    		}
    		catch(final IllegalArgumentException iae)
    		{
    			throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
    		}
    	} else if (this.times != null) {
    		
    		switch(this.times.getTimeType().getValueAsInt()) {
    		case DatabaseTimeType.ERT_TYPE:
    			orderType = PacketOrderByType.ERT;
    			break;
    		case DatabaseTimeType.SCET_TYPE:
    			orderType = PacketOrderByType.SCET;
    			break;
    		case DatabaseTimeType.LST_TYPE:
    			orderType = PacketOrderByType.LST;
    			break;
    		case DatabaseTimeType.SCLK_TYPE:
    			orderType = PacketOrderByType.SCLK;
    			break;
    		case DatabaseTimeType.RCT_TYPE:
    			orderType = PacketOrderByType.RCT;
    			break;
    		}
    	}

    	params[0] = this.apid;
    	params[1] = vcids;
    	params[2] = this.spsc;
    	params[3] = this.fromSse;
    	params[4] = null;
    	params[5] = orderType;
    	params[6] = true;
        params[7] = dssids;

    	return(params);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {
    	switch(range.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.SCET_TYPE:
            case DatabaseTimeType.ERT_TYPE:
            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.RCT_TYPE:
            case DatabaseTimeType.LST_TYPE:

                break;

            default:

                throw new ParseException("TimeType is not one of: SCET, ERT, SCLK, RCT, LST");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return(DatabaseTimeType.ERT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return(PacketOrderByType.orderByTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return(APP_NAME + " " +
                "[--" + BEGIN_TIME_LONG + " <time> " +
                "--" + END_TIME_LONG + " <time>\n" + "                  " +
                "--" + TIME_TYPE_LONG + " <" + TIME_TYPE_ARG +"> " +
                "--" + DssVcidOptions.VCID_OPTION_LONG + " <int>\n" +
                "                  " +
                "--" + DssVcidOptions.DSS_ID_OPTION_LONG + "<int> " +
                "--" + PACKET_SPSC_LONG + " <int,...>\n                  " +
                "[Session search options - Not  required]\n");
    }

	/**
     * {@inheritDoc}
	 */
	@Override
    protected void addAppOptions()
    {
        super.addAppOptions();
        addOption(TC_FORMAT_SHORT,TC_FORMAT_LONG,null, "Output records in Time Correlation File Format");
        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG, TIME_TYPE_ARG, "Time Type should be one of (SCLK,ERT,SCET,RCT,LST).  Default is " + 
        		getDefaultTimeType().getValueAsString());
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "Time", "Begin time of packet range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "Time", "End time of packet range");
        addOption(PACKET_SPSC_SHORT,PACKET_SPSC_LONG,"int,...","Source Packet Seq Counter");

        addOrderOption();

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);
    }
	
	/**
     * {@inheritDoc}
	 */
	@Override
	public void showHelp()
	{
		super.showHelp();

		System.out.println("\nFlight software time correlation packets will be queried based upon command line options.");
		System.out.println("Reference frames for these packets will be queried. Time correlation");
		System.out.println("information for packet/frame pairs will then be displayed to the console.");
		System.out.println("Frames are located using the same search criteria as packets, except they must");
		System.out.println("have an ERT within one hour of the reference frame.");
		System.out.println("The default query ordering is by ERT.");
	}

	   
	/**
     * {@inheritDoc}
	 */
    @Override
	public void run()
	{
		setExitCode(SUCCESS);

		try {
			pktParser = appContext.getBean(ITimeCorrelationParser.class);
		} catch (final Exception e) {
			trace.error("Unable to instantiate time correlation packet parser: " + e.toString());
			setExitCode(OTHER_ERROR);
			return;
		}
		
		addGlobalContext();

		final IDbSqlFetch fetch = getFetch(sqlStmtOnly);

		// Make sure we connected to the database
		if (!fetch.isConnected())
		{
			setExitCode(OTHER_ERROR);
			return;
		}

		try
		{
			List<? extends IDbRecord> out = fetch.get(dbSessionInfo,times,defaultBatchSize,getFetchParameters());
		    
			if (out.size() == 0 && !shutdown) {
				TraceManager.getDefaultTracer().info("There were no packets found that matched your query.");

			}
			
			while(out.size() != 0 && !shutdown)
			{
                @SuppressWarnings("unchecked")
                final ListIterator<? extends IDbQueryable> iter = (ListIterator<? extends IDbQueryable>) out.listIterator();
				while(iter.hasNext() == true && !shutdown)
				{
					final IDbQueryable dq = iter.next();
                    final TimeCorrelationInfo timeCorr = new TimeCorrelationInfo((IDbPacketProvider) dq);
					timeCorr.getFrameInfo();
					timeCorr.print();
					System.out.println();
				}

				if (!shutdown) {
					out = fetch.getNextResultBatch();
				}
			}

		}
		catch(final DatabaseException e) {
        	if (shutdown) {
        		setExitCode(OTHER_ERROR);
        	} else {
                e.printStackTrace(System.out);
        		 trace.error("Problem encountered while retrieving records: " + e.getMessage());
                 setExitCode(OTHER_ERROR);
        	}
        }
		catch(final Exception e)
		{
			e.printStackTrace();
			trace.error("Problem encountered while retrieving records: " + e.getMessage());
			setExitCode(OTHER_ERROR);
		}
		finally
		{
			fetch.close();
		}
	}
	
	/**
	 * This class processes and prints time correlation packets.
	 *
	 */
	private class TimeCorrelationInfo {
		private final IAccurateDateTime packetErt;
		private final IAccurateDateTime packetScet;
		private final ISclk packetSclk;
		private final int spsc;
		private final int vcid;
		private final long vcfc;
		private final ISclk sclk;
		private final EncodingType encType;
        private List<IDbFrameProvider> referenceFrames;
		private final long rateIndex;
		
		/**
		 * Constructor.
		 * @param packet DatabasePacket record fetched from the database
		 */
        public TimeCorrelationInfo(final IDbPacketProvider packet) {
			packetErt = packet.getErt();
			packetSclk = packet.getSclk();
			packetScet = packet.getScet();
			spsc = packet.getSpsc();
			final byte[] packetData = packet.getRecordBytes();
            pktParser.parse(packetData);
            vcid = pktParser.getVcid();
            vcfc = pktParser.getVcfc();
            encType = pktParser.getEncType();
            rateIndex = pktParser.getRateIndex();
            sclk = pktParser.getSclk();
		}
		
		/**
		 * Attempts to fetch the reference frame that corresponds to the current time packet.
		 * @return true id reference frame was found
		 */
		public boolean getFrameInfo() {
			if (frameFetch == null) {
				frameFetch = appContext.getBean(IDbSqlFetchFactory.class).getFrameFetch();
			}
			final DatabaseTimeRange frameTimes = new DatabaseTimeRange(DatabaseTimeType.ERT);
			long time = packetErt.getTime();
			time = time - MILLISECONDS_IN_HOUR;
			final long subs = packetErt.getNanoseconds();
			final IAccurateDateTime startTime = new AccurateDateTime(time, subs);
			frameTimes.setStartTime(startTime);
			frameTimes.setStopTime(packetErt);
			final List<Integer> foundDssIds = new ArrayList<Integer>(1);
			
			try {
                @SuppressWarnings("unchecked")
                final List<IDbFrameProvider> frames = (List<IDbFrameProvider>) frameFetch.get(dbSessionInfo,
                                                                              frameTimes,
                                                                              50,
                                                                              getFrameFetchParameters());
				if (frames == null || frames.size() == 0) {
					return false;
				}
				
                referenceFrames = new ArrayList<IDbFrameProvider>(1);
				
				for (int i = frames.size() - 1; i >= 0; i--) {
                    final IDbFrameProvider frame = frames.get(i);
					if (foundDssIds.contains(frame.getRecordDssId())) {
						continue;
					} else {
						foundDssIds.add(frame.getRecordDssId());
						referenceFrames.add(frame);
					}
				}
				return true;
			} catch (final DatabaseException e) {
				e.printStackTrace();
				TraceManager.getDefaultTracer().warn("Error querying reference frames: " + e.toString());

				return false;
			} finally {
				frameFetch.abortQuery();
			}
		}

		private Object[] getFrameFetchParameters()
		{
			final Object[] params = new Object[5];

			final VcfcRanges vcfcs = new VcfcRanges();
			vcfcs.addLong(this.vcfc);

			final FrameQueryOptions qo = new FrameQueryOptions();

			qo.setFrameType(null);
			qo.setVcid(this.vcid);
			qo.setDss(null);
			qo.setRelayId(null);
			qo.setGood(null);
			qo.setVcfcs(vcfcs);

			params[0] = qo;
			params[1] = true;
			params[2] = FrameOrderByType.ERT;
			params[3] = null;
			params[4] = false;

			return(params);
		}
		
		/**
		 * Display information about the time correlation packet and reference frame to the console.
		 */
		public void print() {
            final SprintfFormat format = new SprintfFormat();
			if (useTcFormat) {
				if (referenceFrames == null) {
					System.out.print(format.sprintf("%s UNK UNK UNK",  
                            new Object[] { sclk.toString() }));
					} else {
                    for (final IDbFrameProvider frame : referenceFrames) {
							System.out.print(format.sprintf("%s %s %.0f %d",  
                                new Object[] { sclk.toString(), frame.getErt().getFormattedErt(true),
                                        frame.getBitRate(), frame.getRecordDssId() }));
						}
					}
			} else {
				System.out.println(format.sprintf("TC Packet: VCID=%d SPSC=%d ERT=%s SCLK=%s SCET=%s TC SCLK=%s Encoding=%s RateIndex=%d", 
						new Object[] {vcid, spsc, packetErt.getFormattedErt(true), packetSclk.toString(), packetScet.getFormattedScet(true), 
                                sclk.toString(), encType.toString(), rateIndex }));
				if (referenceFrames == null) {
					System.out.println("No reference frame found");
					return;
				}
                for (final IDbFrameProvider frame : referenceFrames) {
					System.out.println(format.sprintf("TC Frame:  VCID=%d DSS=%d BitRate=%5.0f VCFC=%d ERT=%s", 
                            new Object[] { frame.getVcid(), frame.getRecordDssId(), frame.getBitRate(), frame.getVcfc(),
                                    frame.getErt().getFormattedErt(true) }));
				}
			}
		}
	}

	@Override
	public void exitCleanly() {
		super.exitCleanly();
		if (this.frameFetch != null) {
			this.frameFetch.abortQuery();
			this.frameFetch.close();
		}
	}
	
    /**
     * Main execution method.
     * 
     * @param args list of command line arguments supplied by the user
     */
    public static void main(final String[] args)
    {
        final TimeCorrelationApp app = new TimeCorrelationApp();
        app.runMain(args);
   }
}
