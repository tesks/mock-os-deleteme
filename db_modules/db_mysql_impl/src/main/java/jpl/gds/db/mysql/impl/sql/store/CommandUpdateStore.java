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

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.shared.string.StringUtil;


/**
 * Class which updates (finalizes) CommandMessage rows.
 *
 */
public class CommandUpdateStore extends AbstractMySqlStore implements ICommandUpdateStore
{
    private static final String HEADER            = "Update_";
    private static final String UNDER_HEADER      = "_" + HEADER;
    private static final String PATH_UNDER_HEADER = File.separator +
                                                        UNDER_HEADER;

    /** Basic SQL statement */
    private static final String UPDATE_SQL_TEMPLATE =
        "UPDATE "                            +
        ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME +
        " SET finalized=1 WHERE ";

    /** Last query issued */
    private String _lastQuery = null;


    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public CommandUpdateStore(final ApplicationContext appContext)
    {
        super(appContext, ICommandUpdateStore.STORE_IDENTIFIER, true); // Connection wanted
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ICommandUpdateStore#getLastQuery()
     */
    @Override
    public String getLastQuery()
    {
        return _lastQuery;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ICommandUpdateStore#stopUpdateStore()
     */
    @Override
    public void stopUpdateStore()
    {
        stopResource();
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ICommandUpdateStore#finalize(int, long, int, java.lang.String)
     */
    @Override
    @SuppressWarnings({"FI_FINALIZER_NULLS_FIELDS", "PMD.FinalizeOverloaded"})
    public void finalize(final int                   hostId,
                         final long                  sessionId,
                         final int                   sessionFragment,
                         final String                requestId)
        throws DatabaseException
    {
        final String        rid = StringUtil.safeTrim(requestId);
        final StringBuilder sb  = new StringBuilder(UPDATE_SQL_TEMPLATE);

        sb.append('(');

        sb.append('(');
        sb.append(ICommandMessageLDIStore.HOST_ID);
        sb.append('=').append(hostId);
        sb.append(')');

        sb.append(" AND ");

        sb.append('(');
        sb.append(ICommandMessageLDIStore.SESSION_ID);
        sb.append('=').append(sessionId);
        sb.append(')');

        sb.append(" AND ");

        sb.append('(');
        sb.append(ICommandMessageLDIStore.FRAGMENT_ID);
        sb.append('=').append(sessionFragment);
        sb.append(')');

        sb.append(" AND ");

        sb.append('(');
        sb.append(ICommandMessageLDIStore.REQUEST_ID);
        sb.append("='").append(rid);
        sb.append("')");

        sb.append(')');

        final String sql = sb.toString();

        // Write to remote if necessary
        sendToRemote(sql);

        synchronized (this)
        {
            if (! isConnected())
            {
                throw new IllegalStateException("The database connection in " +
                                                getStoreIdentifier()          +
                                                " has already been closed");
            }

            preparedStatement = null;

            try
            {
                preparedStatement = getPreparedStatement(sql);
                _lastQuery        = sql;

                final int rows = preparedStatement.executeUpdate();

                if (rows > 1)
                {
                    trace.error(getStoreIdentifier()     +
                                " update was performed " +
                                rows                     +
                                " times: "               +
                                sql);
                }
                else if (rows == 0)
                {
                    trace.warn(getStoreIdentifier()              +
                               " update was performed 0 times: " +
                               sql);
                }
                else
                {
                    trace.debug(getStoreIdentifier()     +
                                " update was performed " +
                                rows                     +
                                " times: "               +
                                sql);
                }
            }
            catch (final SQLException sqle)
            {
                final String msg = getStoreIdentifier()                 +
                                   " encountered error while updating " +
                                   "record in database: "               +
                                   sqle.getMessage()                    +
                                   ". ("                                +
                                   sql                                  +
                                   ")";
                trace.error(msg);

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
            }
        }
    }


    /**
     * Write SQL to a file in the export directory. We must finish writing
     * the file before it is picked up by the remote script, so we start with a
     * prepended dot in the file name. (The file name is created as unique.)
     *
     * Ater writing, we rename it without the dot.
     *
     * @param sql SQL to perform update
     */
    private void sendToRemote(final String sql)
    {
        final IMySqlAdaptationProperties dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);

        if (! dbProperties.getExportLDI() &&
            ! dbProperties.getExportLDICommands())
        {
            return;
        }

        // Create a unique file in the export directory with an underscore so
        // it will be ignored by the remote script.

        File       temp = null;
        FileWriter pw   = null;

        try
        {
            temp = File.createTempFile(UNDER_HEADER,
                                       "",
                                       new File(dbProperties.getExportLDIDir()));

            // Write the SQL to the file and flush and close.
            // Terminate with semicolon in case file is concatenated with
            // another.

            pw = new FileWriter(temp);

            pw.write(sql);
            pw.write(";\n");
        }
        catch (final IOException ioe)
        {
            trace.error(getStoreIdentifier() +
                        " Unable to write "  +
                        HEADER               +
                        " file: "            +
                        rollUpMessages(ioe));

            temp = null;
        }
        finally
        {
            if (pw != null)
            {
                try
                {
                    pw.close();
                }
                catch (final IOException ioe)
                {
                    pw = null;
                }
            }
        }

        if (temp == null)
        {
            // We had some kind of an error

            return;
        }

        // Now rename without the dot. This is done in two stages. First we
        // hard link to the desired name, then we delete the old name.

        final String dotName = temp.getAbsolutePath();
        final int    index   = dotName.lastIndexOf(PATH_UNDER_HEADER);
        final Path name    = Paths.get(dotName.substring(0, index) +
                                   File.separator          +
                                   HEADER                  +
                                   dotName.substring(
                                       index + PATH_UNDER_HEADER.length()));
        final Path link = Paths.get(dotName);

        boolean        linked  = false;
        try { 
        	Files.createLink(link, name.toAbsolutePath());
        	
        	linked = true;
        	trace.debug(getStoreIdentifier() + " Exported " + HEADER + " file as " + name);
        } 
        catch(final IOException e) { 
        	trace.error(getStoreIdentifier() + " Unable to export " + HEADER + " file as " + name +
                    ": "	+
                    rollUpMessages(e));
        }

        if (linked && ! temp.delete())
        {
            trace.error(getStoreIdentifier() +
                        " Unable to delete " +
                        HEADER               +
                        " file "             +
                        dotName);
        }
    }
}
