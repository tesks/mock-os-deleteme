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

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IPacketOrderByType;
import jpl.gds.db.api.types.IDbPacketProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.app.PacketFetchApp.PacketTypeSelect;
import jpl.gds.db.mysql.impl.sql.order.PacketOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeUtility;

/**
 * This class is an application that queries the database for packets with a specific APID and
 * displays information about the gaps in the packet sequence.
 * 
 * Translated from Jesse Wright's chill_packet_gaps python script.
 *
 */
public class PacketGapApp extends AbstractFetchApp
{	
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_packet_gaps");
    private static final int NUM_QUERY_PARAMS = 8;

    /** The APIDs to query by */
    private final ApidRanges apid = new ApidRanges();

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;

    /** Packet types desired */
    private final PacketTypeSelect packetTypeSelect =
        new PacketTypeSelect();


    /**
     * Creates an instance of PacketGapApp.
     */
    public PacketGapApp()
    {
        super(IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
              APP_NAME,
              null);
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
        this.requiredOptions.add(PACKET_APID_LONG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws MissingOptionException, ParseException
    {
        super.configureApp(cmdline);

        if(cmdline.hasOption(PACKET_APID_SHORT))
        {
            apid.processOptions(cmdline.getOptionValue(PACKET_APID_SHORT),PACKET_APID_SHORT);
            if (!apid.isSingleNumber()) {
            	throw new ParseException("For this application, the -" + PACKET_APID_SHORT 
            			+ " option must take a single, integer value.");
            }
        } else {
        	throw new MissingOptionException("You must specific a packet APID using -" + PACKET_APID_SHORT + 
        			" or --" + PACKET_APID_LONG);
        }

        
        // Process packet type selection arguments
        PacketFetchApp.getPacketTypes(dbProperties, cmdline, packetTypeSelect);

		if (packetTypeSelect.fsw && packetTypeSelect.sse)
		{
			throw new ParseException("Cannot set both F and S for --" +
					                 PacketFetchApp.PACKET_TYPES_LONG);
		}

        fromSse = packetTypeSelect.sse;

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline,
                                          packetTypeSelect.sse ? 'S' : null,
                                          null,
                                          PacketFetchApp.PACKET_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           packetTypeSelect.sse ? 'S' : null,
                                           PacketFetchApp.PACKET_TYPES_LONG);
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
    	params[2] = null;
    	params[3] = this.fromSse;
    	params[4] = null;
    	params[5] = orderType;
    	params[6] = false;
    	params[7] = dssIds;

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
                "--" + TIME_TYPE_LONG + " <" + TIME_TYPE_ARG + "> " +
                "--" + PACKET_APID_LONG + " <int> --fswOnly|--sseOnly]\n" + "                  " +
                "[Session search options - Not  required]\n");
    }

	/**
     * {@inheritDoc}
	 */
	@Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(PACKET_APID_SHORT,PACKET_APID_LONG, "int", "Packet Application ID");
        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG, TIME_TYPE_ARG, "Time Type should be one of (SCLK,ERT,SCET,RCT,LST).  Default is " + 
        		getDefaultTimeType().getValueAsString());
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "Time", "Begin time of packet range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "Time", "End time of packet range");
        addOrderOption();

		addOption(null,
                  PacketFetchApp.PACKET_TYPES_LONG,
                  "string",
                  "Retrieve selected types: " +
                      "s=SSE "                +
                      "f=FSW");

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

		System.out.println("\nOnly one packet APID may be supplied to this utility. ");
		System.out.println("Consecutive packet ranges (by sequence ID) found in the selected packets will");
		System.out.println("will be printed to the console. Gaps in the sequence are indicated whenever there");
		System.out.println("is a new consecutive range displayed. Gaps are assessed based upon the sort order");
		System.out.println("of the query. The default ordering is by ERT.");
	}
	   
	/**
     * {@inheritDoc}
	 */
	@Override
	public void run()
	{
		setExitCode(SUCCESS);

		addGlobalContext();

		int recordCount = 0;

		// Make sure we connected to the database
		final IDbSqlFetch fetch = getFetch(sqlStmtOnly);
		if (!fetch.isConnected())
		{
			setExitCode(OTHER_ERROR);
			return;
		}

		final PrintWriter pw = (reportRows ? new PrintWriter(System.out, false) : null);
        final Map<Integer, PacketGapDumper> dumpers =
            new TreeMap<Integer, PacketGapDumper>();

        // Map won't take a null key without a special comparator
        final PacketGapDumper dumperNull = new PacketGapDumper(null);

		try
		{
			List<? extends IDbRecord> out = fetch.get(dbSessionInfo,times,defaultBatchSize,getFetchParameters());
		    
            final SprintfFormat sprintf = new SprintfFormat();
			if (out.size() != 0 && !shutdown) {
                System.out.println(
                        sprintf.sprintf("%25s %23s %17s %7s", new Object[] { "ERT", "SCET", "SCLK", "PKTSEQ" }));
			} else {
				trace.info("There were no packets found that matched your query.");
			}
			
			while (out.size() != 0 && !shutdown)
			{
				final ListIterator<? extends IDbRecord> iter =
                    out.listIterator();

				while (iter.hasNext())
				{
                    final IDbPacketProvider dq = (IDbPacketProvider) iter.next();

					dq.setRecordOffset(0L);

                    final Integer   newVcid = dq.getVcid(); // May be NULL
                    PacketGapDumper dumper  = null;

                    if (newVcid != null)
                    {
                        dumper = dumpers.get(newVcid);

                        if (dumper == null)
                        {
                            dumper = new PacketGapDumper(newVcid);

                            dumpers.put(newVcid, dumper);
                        }
                    }
                    else
                    {
                        dumper = dumperNull;
                    }

                    dumper.incrementLines();

					if (! dumper.process(dq))
                    {
						dumper.printPacketRange();
						dumper.copyNewToStart();
						dumper.copyNewToLast();
						dumper.setCount(1);

                        dumper.clearLines();
					}

					recordCount++;
				}

				if (!shutdown) {
					out = fetch.getNextResultBatch();
				}
			}

			if (!shutdown)
            {
                for (final PacketGapDumper dumper : dumpers.values())
                {
                    dumper.printPacketRange();
                }

                dumperNull.printPacketRange();
			}
			
			if (pw != null)
			{
				if(template != null)
				{
					writeTrailerMetaData(pw);
				}
				pw.flush();
			}

			trace.debug("Retrieved " + recordCount + " records.");
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
            e.printStackTrace(System.out);
			trace.error("Problem encountered while retrieving records: " + e.getMessage());
			setExitCode(OTHER_ERROR);
		}
		finally
		{
			fetch.close();
			if(pw != null)
			{
				pw.flush();
				pw.close();
			}
		}
	}

	/**
	 * This class performs the work of keeping track of gaps in the packet sequence.
	 * Basically, it tracks starting timestamps and sequence number and ending timestamps
	 * and sequence number for a consecutive series of packets with the same APID. When
	 * a new gap is found, the consecutive sequence ends, and this objects re-initializes
	 * to start tracking a new consecutive sequence.
	 *
	 */
	private static class PacketGapDumper extends Object
    {
        private final String displayVcid;
        private IAccurateDateTime start_ert; 
        private IAccurateDateTime start_scet;
        private ISclk start_sclk;
        private int start_spsc = -1; 
        private IAccurateDateTime last_ert;
        private IAccurateDateTime last_scet;
        private ISclk last_sclk;
        private int last_spsc = 0;
        private IAccurateDateTime ert;
        private IAccurateDateTime scet;
        private ISclk sclk;
        private int spsc = 0;
        private int num = 0;
        private int lines = 0;
        private boolean first = true;
        private final DateFormat format = TimeUtility.getFormatter();


        /**
         * Constructer for PacketGapDumper.
         *
         * @param theVcid VCID of interest
         */
        public PacketGapDumper(final Integer theVcid)
        {
            super();

            displayVcid = (theVcid != null) ? theVcid.toString() : "NULL";
        }


        /**
         * Increments the count of packets encountered.
         */
        public void incrementLines()
        {
            ++lines;
        }


        /**
         * Clears the count of packets encountered.
         */
        public void clearLines()
        {
            lines = 0;
        }


        /**
         * Sets the count of consecutive packets seen within the current apid.
         * 
         * @param val count of consecutive packets
         */
        public void setCount(final int val) {
            this.num = val;
        }
        
        /**
         * Copies the tracked fields from the latest packet to the fields indicating where the
         * latest consecutive sequence starts.
         */
        public void copyNewToStart() {
            this.start_ert = this.ert;
            this.start_scet = this.scet;
            this.start_sclk = this.sclk;
            this.start_spsc = this.spsc;
        }
        
        
        /**
         * Copies the tracked fields from the latest packet to the fields indicating where the
         * latest consecutive sequence ends.
         */
        public void copyNewToLast() {
            this.last_ert = this.ert;
            this.last_scet = this.scet;
            this.last_sclk = this.sclk;
            this.last_spsc = this.spsc;
        }
        
        /**
         * Processes a new packet record from the database.  Updates the latest
         * timestamp and sequence number from the incoming record. Determines
         * if this packet is consecutive with the previous one. If so the latest values
         * become the new end sequence values and false is returned. If not,
         * we have a gap and return false.
         * 
         * @param packet the DatabasePacket record to process
         * @return true if packet is consecutive (no gap); false if not.
         */
        public boolean process(final IDbPacketProvider packet) {

            spsc = packet.getSpsc();
            ert = packet.getErt();
            scet = packet.getScet();
            sclk = packet.getSclk();
            if (this.first) {
                copyNewToStart();
                copyNewToLast();
                this.num = 1;
                this.first = false;
                return true;
            }

            // We expect the next one to advance
            final int expected_spsc = this.last_spsc + 1;

            // A duplicate of the last one is OK
            if ((this.spsc == expected_spsc) || (this.spsc == this.last_spsc))
            {
                this.num++;
                this.copyNewToLast();
                return true;
            }

            return false;
		}
        
        /**
         * Prints the information for the latest consecutive packet range.
         */
        public void printPacketRange()
        {
            if (lines == 0)
            {
                return;
            }

        	final String  startErtStr = this.start_ert.getFormattedErt(false);
        	final String  startScetStr = format.format(this.start_scet);
        	final String  startSclkStr = this.start_sclk.toString();
        	final String  lastErtStr = this.last_ert.getFormattedErt(false);
        	final String  lastScetStr = format.format(this.last_scet);
        	final String  lastSclkStr = this.last_sclk.toString();
        	
            final SprintfFormat sprintf = new SprintfFormat();
        	
        	System.out.println(sprintf.sprintf("%25s %23s %17s %7d VCID=%s",
                    new Object[] { startErtStr, startScetStr, startSclkStr, this.start_spsc, this.displayVcid }));

        	System.out.println(sprintf.sprintf("%25s %23s %17s %7d Total=%d",
                    new Object[] { lastErtStr, lastScetStr, lastSclkStr, this.last_spsc, this.num }));
            System.out.println("--------------------------------------------------------------------------------------");
        }
            
	}

    /**
     * Main execution method.
     * 
     * @param args list of command line arguments supplied by the user
     */
    public static void main(final String[] args)
    {
        final PacketGapApp app = new PacketGapApp();
        app.runMain(args);
   }
}
