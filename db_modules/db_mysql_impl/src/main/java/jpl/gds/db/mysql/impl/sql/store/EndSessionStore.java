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
package jpl.gds.db.mysql.impl.sql.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.Pair;


/**
 * This is the database write/storage interface to the EndSession table in the
 * MPCS database. This class subscribes to the end test message and writes the
 * end-of-session information.
 *
 */
public class EndSessionStore extends AbstractMySqlStore implements IEndSessionStore
{
    private static final String ALT_DB_TABLE_NAME = "." + DB_END_SESSION_DATA_TABLE_NAME;

    /** The SQL template to use for inserting a new end session */
    private static final String INSERT_SQL_TEMPLATE;

    private static final String[] FIELDS =
        {
            SESSION_ID,

            "endTime",
            "endTimeCoarse",
            "endTimeFine",

            HOST_ID,
            FRAGMENT_ID
        };

    static
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO ").append(DB_END_SESSION_DATA_TABLE_NAME).append('(');

        boolean first = true;

        for (final String s : FIELDS)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append(s);
        }
        
        sb.append(") VALUES (");

        first = true;

        for (int i = 0; i < FIELDS.length; ++i)
        {
            if (first)
            {
                first = false;

                sb.append('?');
            }
            else
            {
                sb.append(",?");
            }
        }

        // Take latest times

        sb.append(") ON DUPLICATE KEY UPDATE ");

        sb.append("endTime=IF(VALUES(endTime) > endTime,");
        sb.append(            "VALUES(endTime),");
        sb.append(            "endTime)");
        sb.append(',');
        sb.append("endTimeCoarse=IF(VALUES(endTimeCoarse) > endTimeCoarse,");
        sb.append(                  "VALUES(endTimeCoarse),");
        sb.append(                  "endTimeCoarse)");
        sb.append(',');
        sb.append("endTimeFine=IF((VALUES(endTimeCoarse) > endTimeCoarse) OR ");
        sb.append(                    "((VALUES(endTimeCoarse) = endTimeCoarse) AND (VALUES(endTimeFine) > endTimeFine)),");
        sb.append(                "VALUES(endTimeFine),");
        sb.append(                "endTimeFine)");

        INSERT_SQL_TEMPLATE = sb.toString();
    }



    /**
     * Creates an instance of EndSessionStore.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public EndSessionStore(final ApplicationContext appContext)
    {
        super(appContext, IEndSessionStore.STORE_IDENTIFIER, true);

        /** MPCS-8128  Defeat shutdown hook close */
        getConnection().markControlled();

        /** MPCS-7733 Get rid of batchSize */
    }

	/**
	 * Stop this store from running. Just punt to super.
	 */
/*
    @Override
	public void stop()
	{
        super.stop();
    }
*/

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IEndSessionStore#insertEndSession(jpl.gds.session.config.SessionIdentification, jpl.gds.session.util.SessionSummary)
     */
    @Override
    public void insertEndSession(final IContextIdentification tc)
        throws DatabaseException
    {
        if (tc == null)
        {
            throw new DatabaseException("Null input test configuration");
        }
        
        if (dbProperties.getExportLDIAny())
        {
			// Create LDI file and write it directly to the export
			// directory. It is needed when exporting.

			writeLDI(tc);
		}

        // Make sure we're supposed to be using the database
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        synchronized (this)
        {
            if (! isConnected())
            {
                throw new IllegalStateException(
                              "The database connection in " +
                              getClass().getName()          +
                              " has already been closed");
            }

            /*
             * The majority of the code in the try block below is responsible
             * for reading the information out of the input and inserting it
             * into the SQL insert template. A few fields will
             * throw exceptions if they are null, but otherwise all null values
             * are simply replaced with reasonable defaults.
             */

            int i = 1;

            preparedStatement = null;

            try
            {
                preparedStatement = getPreparedStatement(INSERT_SQL_TEMPLATE);

                // sessionId
                preparedStatement.setLong(i++, tc.getNumber().longValue());

                // endTime and endTimeCoarse and Fine

                i = insertTimes(preparedStatement, i, tc.getEndTime(),
                                "EndSession.endTime");

                // hostId
                
                preparedStatement.setInt(i++, tc.getHostId());

                // sessionFragment
                preparedStatement.setInt(i++, tc.getFragment());

                // Insert into the database

                if (preparedStatement.executeUpdate() == 0)
                {
                    throw new DatabaseException("EndSession was not inserted " +
                                           "into database");
                }
            }
            catch (final SQLException sqle)
            {
                sqle.printStackTrace();

                final String sql = (preparedStatement != null)
                                       ? preparedStatement.toString()
                                       : "";

                final String msg = "Exception encountered while inserting " +
                                   "EndSession into database: "             +
                                   sqle.getMessage()                        +
                                   ". ("                                    +
                                   sql                                      +
                                   ")";

                System.err.println(msg);

                throw new DatabaseException(msg, sqle);
            }
            finally
            {
                if (preparedStatement != null)
                {
                    try
                    {
                        preparedStatement.close();
                    }
                    catch (final SQLException sqle)
                    {
                        // Do nothing
                    }
                    finally
                    {
                        preparedStatement = null;
                    }
                }

                close();
            }
        }
    }


    /**
     * Insert a Date safely, as both timestamp and exact.
     *
     * @param ps      Prepared statement to insert into
     * @param index   Index at which to insert
     * @param theDate The date to insert
     * @param what    Name of column for logging
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    private int insertTimes(final PreparedStatement ps,
                            final int               index,
                            final IAccurateDateTime theDate,
                            final String            what)
        throws DatabaseException
    {
        final IAccurateDateTime date = ((theDate != null) ? theDate : new AccurateDateTime());
        int        insert  = index;

        final long millis = date.getTime();

        try {
            // Insert as timestamp
            ps.setTimestamp(insert++, new Timestamp(millis));

            // Insert as exact

            long coarse = 0L;
            int fine = 0;

            try {
                coarse = DbTimeUtility.coarseFromExact(millis);
                fine = DbTimeUtility.fineFromExact(millis);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(dateExceedsWarning(what, null, date), ttle);

                coarse = DbTimeUtility.MAX_COARSE;
                fine = DbTimeUtility.MAX_FINE;
            }

            ps.setLong(insert++, coarse);
            ps.setInt(insert++, fine);

            return insert;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * Write LDI file to export directory. It will not be used for LDI here,
     * but will be picked up by the remote. We will force the id, which is
     * otherwise autoincrement.
     *
     * In order to synchronize with the import process, we actually write to
     * the export directory with a prepended "." in the file name, hard link to
     * the export directory without the ".", and then delete the original file.
     * That makes sure that the importing process sees only completed files in
     * the export directory.
     *
     * I'm suppressing the warning because ss may be needed soon.
     *
     * @param tc Test configuration WITH unique id field.
     * @param ss Session summary
     *
     * @throws DatabaseException SQL exception
     */
    private void writeLDI(final IContextIdentification tc) throws DatabaseException
    {
        final BytesBuilder bb = new BytesBuilder();

        // Format as a line for LDI

        // sessionId
        bb.insert(tc.getNumber().longValue());
        bb.insertSeparator();

        // endTime and endTimeCoarse and Fine
        insertTimes(bb, tc.getEndTime(), "EndSession.endTime");

        // hostId
        bb.insert(tc.getHostId());
        bb.insertSeparator();

        // sessionFragment
        bb.insert(tc.getFragment());
        bb.insertTerminator();

        final String     edir = dbProperties.getExportLDIDir() + File.separator;
        File             file = null;
        FileOutputStream fos  = null;

        try
        {
            // Get file; it will be created

            final Pair<File, FileOutputStream> pfos =
                archiveController.openStream(ALT_DB_TABLE_NAME, edir, false);

            if (pfos == null)
            {
                // Errors have already been logged

                return;
            }

            file = pfos.getOne();
            fos  = pfos.getTwo();

            try
            {
                bb.write(fos);

                trace.debug("Wrote " + file);
            }
            catch (final IOException ioe)
            {
                trace.error("Unable to write " + ALT_DB_TABLE_NAME + ": ",
                                        ioe);
            }
        }
        finally
        {
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (final IOException ioe)
                {
                    trace.error(Markers.DB, "Unable to close: " + file + ": ",
                                            ioe);
                }
            }
        }

        // Hard link it to the export directory under the regular name,
        // and then delete it

        final String   linked  = edir + file.getName().substring(1);
        final String   name    = file.getAbsolutePath();
        final String[] command = new String[] {"/bin/ln", name, linked};

        try
        {
            final int status = ProcessLauncher.launchSimple(command);

            if (status == 0)
            {
                trace.debug("Exported '" + name + "' as '" + linked + "'");
            }
            else
            {
                trace.error(Markers.DB, "Unable to export '" + name + "' as '" + linked + "': " + status);
            }
        }
        catch (final IOException ioe)
        {
            trace.error(Markers.DB, "Unable to export '" + name + "' as '" + linked + "': ",
                                    ioe);
        }

        if (! file.delete())
        {
            trace.error(Markers.DB, "Unable to delete: " + name);
        }
    }


    /**
     * Insert a Date safely, as both timestamp and exact.
     *
     * @param bb      Where to store it
     * @param theDate Date to store
     * @param time    Kind of time
     */
    private void insertTimes(final BytesBuilder bb, final IAccurateDateTime theDate, final String time) {
        final IAccurateDateTime date = ((theDate != null) ? theDate : new AccurateDateTime());

        // Insert as timestamp

        bb.insert(date);
        bb.insertSeparator();

        // Insert as exact

        try
        {
            bb.insertDateAsCoarseFineSeparate(date);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(dateExceedsWarning(time, null, date));
        }
    }
}
