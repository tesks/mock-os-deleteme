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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.FrameQueryOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IFrameOrderByType;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.order.FrameOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;


/**
 * This class is an application that queries the database for frames and
 * displays information about the gaps in the frame sequence.
 *
 * Translated from Jesse Wright's chill_frame_gaps python script.
 */
public class FrameGapApp extends AbstractFetchApp
{
    private static final String APP_NAME =
        ApplicationConfiguration.getApplicationName("chill_frame_gaps");
    private static final int NUM_QUERY_PARAMS = 5;

    private static final boolean DEBUG = false;

    /** The frame type to query for */
    private String type = null;

    /** The frame relay ID to query for */
    private Long relayId = null;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;


    /**
     * Creates an instance of FrameGapApp.
     */
    public FrameGapApp()
    {
        super(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME,
              APP_NAME,
              null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {
        super.createRequiredOptions();

        requiredOptions.add(BEGIN_TIME_LONG);
        requiredOptions.add(END_TIME_LONG);
        requiredOptions.add(FRAME_TYPE_LONG);
        requiredOptions.add(RELAY_SCID_LONG);
        requiredOptions.add(DssVcidOptions.VCID_OPTION_LONG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline)
        throws MissingOptionException, ParseException
    {
        super.configureApp(cmdline);

        if (cmdline.hasOption(FRAME_TYPE_SHORT))
        {
            final String typeString = cmdline.getOptionValue(FRAME_TYPE_SHORT);

            if (typeString == null)
            {
                throw new MissingArgumentException(
                              "The argument -" +
                              FRAME_TYPE_SHORT +
                              " requires a value");
            }

            type = typeString.trim();
        }

        if (cmdline.hasOption(RELAY_SCID_SHORT))
        {
            final String relayScidString =
                cmdline.getOptionValue(RELAY_SCID_SHORT);

            if (relayScidString == null)
            {
                throw new MissingArgumentException("The argument -" +
                                                   RELAY_SCID_SHORT +
                                                   " requires a value");
            }

            try
            {
                relayId = Long.valueOf(relayScidString.trim());
            }
            catch (final NumberFormatException e1)
            {
                throw new MissingOptionException("Relay Spacecraft ID is not " +
                                                 "a valid integer");
            }

            if (relayId < 0)
            {
                throw new ParseException("Invalid Relay Spacecraft ID");
            }
        }

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline, (String) null);
        dssIds = DssVcidOptions.parseDssId(cmdline, (String) null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
        return appContext.getBean(IDbSqlFetchFactory.class).getFrameFetch(sqlStmtOnly);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
        final Object[] params = new Object[NUM_QUERY_PARAMS];

        IFrameOrderByType orderType = FrameOrderByType.DEFAULT;

        if (orderByString != null)
        {
            try
            {
                orderType = new FrameOrderByType(orderByString.trim());
            }
            catch(final IllegalArgumentException iae)
            {
                throw new IllegalArgumentException(
                              "The value '"                                   +
                                  orderByString                               +
                                  "' is not a legal ordering value for this " +
                                  "application",
                              iae);
            }
        }

        final FrameQueryOptions qo = new FrameQueryOptions();

        qo.setFrameType(type);
        qo.setVcid(vcids);
        qo.setDss(dssIds);
        qo.setRelayId(relayId);
        qo.setGood(null);
        qo.setVcfcs(null);

        params[0] = qo;
        params[1] = false;
        params[2] = orderType;
        params[3] = null;
        params[4] = false;

        return params;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range)
        throws ParseException
    {
        if (range.getTimeType().getValueAsInt() != DatabaseTimeType.ERT_TYPE)
        {
            throw new ParseException("TimeType is not ERT");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return DatabaseTimeType.ERT;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return FrameOrderByType.orderByTypes;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return(APP_NAME + " [" +
                "--" + BEGIN_TIME_LONG + " <time> "      +
                "--" + END_TIME_LONG   + " <time> "      +
                "--" + FRAME_TYPE_LONG + " <frameType> " +
                "--" + RELAY_SCID_LONG + " <scid>\n"     +
                "                 "                      +
                "[Session search options - Not required]\n");
    }


    /**
     * Add options for this appkication.
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(FRAME_TYPE_SHORT, FRAME_TYPE_LONG, "frameType",
                  "Frame Type (string)");

        addOption(RELAY_SCID_SHORT, RELAY_SCID_LONG, "scid",
                  "Relay spacecraft ID");

        addOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "Time",
                  "Begin time of frame range");

        addOption(END_TIME_SHORT, END_TIME_LONG, "Time",
                  "End time of frame range");

        addOrderOption();

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);
    }


    /**
     * Print help text.
     */
    @Override
    public void showHelp()
    {
        super.showHelp();

        System.out.println("Consecutive frame ranges (by sequence ID) found "  +
                           "in the selected frames will be printed to the\n"   +
                           "console. Gaps in the sequence are indicated "      +
                           "whenever there is a new consecutive range\n"       +
                           "displayed. Gaps are assessed based upon the sort " +
                           "order of the query. The default ordering is by "   +
                           "ERT.");
    }


    /**
     * Master run method.
     */
    @Override
    public void run()
    {
        setExitCode(SUCCESS);
        addGlobalContext();

		// Make sure we connected to the database
        final IDbSqlFetch fetch = getFetch(sqlStmtOnly);
		if (!fetch.isConnected()) {
			setExitCode(OTHER_ERROR);
			return;
		}

        int           recordCount = 0;

		final PrintWriter pw = (reportRows ? new PrintWriter(System.out, false) : null);
		final Map<Integer, FrameGapDumper> dumpers = new TreeMap<Integer, FrameGapDumper>();

        try
        {
            List<? extends IDbRecord> out =
                fetch.get(dbSessionInfo,times,defaultBatchSize,getFetchParameters());

            final SprintfFormat sprintf = new SprintfFormat();

            if (out.size() != 0 && ! shutdown)
            {
                System.out.println(
                    sprintf.sprintf("%26s %6s",
                                new Object[] { "ERT", "VCFC" }));
            }
            else
            {
                trace.info(
                    "There were no frames found that matched your query.");
            }

            while (out.size() != 0 && ! shutdown)
            {
                final ListIterator<? extends IDbRecord> iter =
                    out.listIterator();

                while (iter.hasNext())
                {
                    final IDbFrameProvider dq = (IDbFrameProvider) iter.next();

                    /*
                     * Bad frames were being thrown out, but they should
                     * be handled.
                     */

                    final int vcid = dq.getVcid();

                    FrameGapDumper dumper = dumpers.get(vcid);

                    if (dumper == null)
                    {
                        dumper = new FrameGapDumper(vcid);

                        dumpers.put(vcid, dumper);
                    }

                    dumper.incrementLines();

                    if (! dumper.process(dq))
                    {
                        dumper.printFrameRange();
                        dumper.copyNewToStart();
                        dumper.copyNewToLast();
                        dumper.setCount(1);

                        dumper.clearLines();
                    }

                    recordCount++;
                }

                if (! shutdown)
                {
                    out = fetch.getNextResultBatch();
                }
            }

            if (! shutdown)
            {
                for (final FrameGapDumper dumper : dumpers.values())
                {
                    dumper.printFrameRange();
                }
            }

            if (pw != null)
            {
                if (template != null)
                {
                    writeTrailerMetaData(pw);
                }

                pw.flush();
            }

            trace.debug("Retrieved " + recordCount + " records.");
        }
        catch (final DatabaseException e)
        {
            if (shutdown)
            {
                setExitCode(OTHER_ERROR);
            }
            else
            {
                e.printStackTrace(System.out);
                 trace.error("Problem encountered while retrieving records: " +
                             e.getMessage());

                 setExitCode(OTHER_ERROR);
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace(System.out);
            trace.error("Problem encountered while retrieving records: " +
                        e.getMessage());

            setExitCode(OTHER_ERROR);
        }
        finally
        {
            fetch.close();

            if (pw != null)
            {
                pw.flush();
                pw.close();
            }
        }
    }


    /**
     * This class performs the work of keeping track of gaps in the frame
     * sequence. Basically, it tracks starting timestamps and sequence number
     * and ending timestamps and sequence number for a consecutive series of
     * frames. When a new gap is found, the consecutive sequence ends, and
     * this object re-initializes to start tracking a new consecutive sequence.
     */
    private static class FrameGapDumper extends Object
    {
        private static final int MAX_VCFC = 0xFFFFFF;

        private IAccurateDateTime start_ert  = null;
        private IAccurateDateTime last_ert   = null;
        private IAccurateDateTime ert        = null;
        private int              start_vcfc = 0;
        private int              last_vcfc  = 0;
        private int              vcfc       = 0;
        private final int        vcid;
        private int              count      = 0;
        private int              lines      = 0;
        private boolean          first      = true;


        /**
         * Constructer.
         * 
         * @param theVcid
         *            the VCID of the frames whose gaps should be dumped
         */
        public FrameGapDumper(final int theVcid)
        {
            super();

            vcid = theVcid;
        }


        /**
         * Sets the count of consecutive frames seen.
         *
         * @param val count of consecutive frames
         */
        public void setCount(final int val)
        {
            count = val;
        }


        /**
         * Increments the count of frames encountered.
         */
        public void incrementLines()
        {
            ++lines;
        }


        /**
         * Clears the count of frames encountered.
         */
        public void clearLines()
        {
            lines = 0;
        }


        /**
         * Copies the tracked fields from the latest frame to the fields
         * indicating where the latest consecutive sequence starts.
         */
        public void copyNewToStart()
        {
            start_ert  = ert;
            start_vcfc = vcfc;
        }


        /**
         * Copies the tracked fields from the latest frame to the fields
         * indicating where the latest consecutive sequence ends.
         */
        public void copyNewToLast()
        {
            last_ert  = ert;
            last_vcfc = vcfc;
        }


        /**
         * Processes a new frame record from the database.  Updates the latest
         * timestamp and sequence number from the incoming record. Determines
         * if this frame is consecutive with the previous one. If so the latest
         * values become the new end sequence values and false is returned.
         * If not, we have a gap and return false.
         *
         * @param frame the DatabaseFrame record to process
         *
         * @return true if frame is consecutive (no gap); false if not.
         */
        public boolean process(final IDbFrameProvider frame)
        {
            ert  = frame.getErt();
            vcfc = frame.getVcfc();

            if (DEBUG)
            {
                System.out.println("DEBUG " + ert.getFormattedErt(false) +
                                   " "      + vcfc);
            }

            if (first)
            {
                copyNewToStart();
                copyNewToLast();

                count = 1;
                first = false;

                return true;
            }

            // We expect the next one to advance, but watch for overflow
            final int expected_vcfc = (last_vcfc + 1) % (MAX_VCFC + 1);

            // A duplicate of the last one is OK
            if ((vcfc == expected_vcfc) || (vcfc == last_vcfc))
            {
                ++count;

                copyNewToLast();

                return true;
            }

            return false;
        }


        /**
         * Prints the information for the latest consecutive frame range.
         */
        public void printFrameRange()
        {
            if (lines == 0)
            {
                return;
            }

            final String startErtStr = start_ert.getFormattedErt(false);
            final String lastErtStr  = last_ert.getFormattedErt(false);

            final SprintfFormat sprintf = new SprintfFormat();

            System.out.println(
                sprintf.sprintf("%26s %6d VCID=%d",
                            new Object[] { startErtStr, start_vcfc, vcid }));

            System.out.println(
                sprintf.sprintf("%26s %6d count=%d",
                            new Object[] { lastErtStr, last_vcfc, count }));

            System.out.println("--------------------------------------------");
        }

    }


    /**
     * Main execution method.
     *
     * @param args list of command line arguments supplied by the user
     */
    public static void main(final String[] args)
    {
        final FrameGapApp app = new FrameGapApp();

        app.runMain(args);
    }
}
