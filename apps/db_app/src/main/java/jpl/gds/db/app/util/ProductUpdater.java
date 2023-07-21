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
package jpl.gds.db.app.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.mysql.impl.sql.AbstractMySqlInteractor;
import jpl.gds.shared.string.StringUtil;


/**
 * ProductUpdate is used to perform updates to records in the product database table.
 *
 */
public class ProductUpdater extends AbstractMySqlInteractor
{
    /** Database table name */
    public static final String DB_TABLE_NAME = IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME;

    /** Database table abbreviation */
    public static final String DB_TABLE_PREFIX = "p";
    
    /**
     * The SQL template to use for updating the output directory of a 
     * product that is already stored in the database.
     */
    private final String OUTPUT_DIR_UPDATE_SQL_TEMPLATE =
        "UPDATE "                                                 +
        getActualTableName(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME) +
        " "                                                       +
        DB_TABLE_PREFIX                                           +
        " SET "                                                   +
        DB_TABLE_PREFIX                                           +
        ".fullPath=replace("                                      +
        DB_TABLE_PREFIX                                           +
        ".fullPath, ?, ?) WHERE "                                 + 
        DB_TABLE_PREFIX                                           +
        ".fullPath LIKE ?";

    private final IDbSqlFetchFactory fetchFactory;

    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public ProductUpdater(final ApplicationContext appContext) {
        super(appContext, true, true);
        this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
    }
    
    /**
     * Update the product directories in the database for the given session
     * configurations
     * 
     * @param tc
     *            The session selection information
     * @param oldDir
     *            the directory prefix to replace
     * @param newDir
     *            the new directory to set in the database
     * @throws DatabaseException
     *             if a database error occurs
     */
    public void updateOutputDirectory(final IDbSessionInfoProvider tc, final String oldDir,
            final String newDir) throws DatabaseException {
        if (tc == null) {
            throw new NullPointerException("Null input session configuration");
        } 
        
        synchronized (this)
        {
            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false, PreFetchType.GET_OD);
            
            try {
                spf.get(tc);
            } finally {
                spf.close();
            }
            
            if (! isConnected())
            {
                throw new IllegalStateException("The database connection in "
                        + this.getClass().getName()
                        + " has already been closed");
            }

            String sql = null;
            
            sql = OUTPUT_DIR_UPDATE_SQL_TEMPLATE + " AND " + spf.getIdHostWhereClause(DB_TABLE_PREFIX);
            
            int i = 1;
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = getPreparedStatement(sql);

                preparedStatement.setString(i++,
                                            StringUtil.checkSqlText(oldDir));  
                preparedStatement.setString(i++,
                                            StringUtil.checkSqlText(newDir));
                preparedStatement.setString(
                    i++,
                    StringUtil.checkSqlText(oldDir + "%"));

                preparedStatement.executeUpdate();
               
            } catch (final SQLException e) {
                throw new DatabaseException(
                        "Error updating output directory for Products: "
                                + e.getMessage());
            } finally {
                try {
                    preparedStatement.cancel();
                    preparedStatement.close();
                }
                catch (final SQLException e) {
                    throw new DatabaseException(e.getMessage(), e);
                }
            }
        }
    }
}
