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
import java.sql.SQLException;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Pair;

/**
 * Class which creates unique hostId numbers for hostNames.
 *
 */
public class HostStore extends AbstractMySqlStore implements IHostStore
{
    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public HostStore(final ApplicationContext appContext)
    {
        super(appContext, IHostStore.STORE_IDENTIFIER, true); // Connection wanted
    }


    private static final String INSERT_SQL_TEMPLATE =
        "INSERT INTO " +
        DB_HOST_STORE_TABLE_NAME  +
        "(hostId, hostName, hostOffset) VALUES";

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IHostStore#insertHostName(java.lang.String, int, int)
     */
    @Override
    public void insertHostName(final String hostName,
                               final int    hostId,
                               final int    hostOffset)
        throws DatabaseException
    {
        final StringBuilder sb = new StringBuilder(INSERT_SQL_TEMPLATE);

        sb.append("(");
        sb.append(hostId).append(",");

        /** MPCS-5153  */
        sb.append("'").append(StringUtil.checkSqlText(hostName)).append("',");

        sb.append(hostOffset);
        sb.append(")");

        final String sql = sb.toString();

        synchronized (this)
        {
            if (! isConnected())
            {
                throw new IllegalStateException("The database connection in " +
                                                getClass().getName()          +
                                                " has already been closed");
            }

            preparedStatement = null;

            try
            {
                preparedStatement = getPreparedStatement(sql);

                final int rows = preparedStatement.executeUpdate();

                if (rows == 0)
                {
                    throw new DatabaseException(
                                  "Host information was not inserted " +
                                  "into database.");
                }
            }
            catch (final SQLException sqle)
            {
                final String msg = "Exception encountered while inserting Host " +
                             "record into database: "                      +
                             sqle.getMessage()                             +
                             ". ("                                         +
                             sql                                           +
                             ")";

                System.err.println(msg);

                throw new DatabaseException(msg, sqle);
            }
            finally
            {
                if (preparedStatement != null)
                {
                    try {
                        preparedStatement.close();
                    }
                    catch (final SQLException e) {
                        // ignore
                    }
                    preparedStatement = null;
                }
                close();
            }
        }
    }


    /**
     * Protect from negative numbers (or null).
     *
     * @param n Number
     * @param message Message
     *
     * @return T
     *
     * @param <T> Number type
     */
    private static <T extends Number> T throwNegative(final T      n,
                                                      final String message)
    {
        if ((n == null) || (n.doubleValue() < 0.0D))
        {
            throw new IllegalArgumentException(message);
        }

        return n;
    }
    

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IHostStore#writeLDI(jpl.gds.session.config.SessionConfiguration)
     */
    @Override
    public void writeLDI(final IContextConfiguration contextConfig) throws DatabaseException
    {
        try {
            final BytesBuilder bb  = new BytesBuilder();
    
            // Format as a line for LDI
    
            bb.insert(throwNull(contextConfig.getContextId().getHostId(), "HostId cannot be null"));
            bb.insertSeparator();
    
            /** MPCS-5153 */
            bb.insertTextComplainReplace(
                throwNull(contextConfig.getContextId().getHost(), "HostName cannot be null"));
    
            bb.insertSeparator();
    
            // hostOffset is not ours but the destination's
    
            bb.insert(throwNegative(dbProperties.getExportLDIHostOffset(),
                                    "Host offset cannot be negative"));
            bb.insertTerminator();
    
            final String     edir = dbProperties.getExportLDIDir() + File.separator;
            File             file = null;
            FileOutputStream fos  = null;
    
            try
            {
                // Get file; it will be created
    
                final Pair<File, FileOutputStream> pfos =
                    archiveController.openStream(ALT_HOST_STORE_DB_TABLE_NAME, edir, false);
    
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
                    trace.error("Unable to write " + ALT_HOST_STORE_DB_TABLE_NAME +
                                            ": ",
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
                    trace.debug("Exported '" + name + "' as '" + linked +
                                            "'");
                }
                else
                {
                    trace.error(Markers.DB, "Unable to export '" + name + "' as '" + linked + "': " +
                                            status);
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
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }
}
