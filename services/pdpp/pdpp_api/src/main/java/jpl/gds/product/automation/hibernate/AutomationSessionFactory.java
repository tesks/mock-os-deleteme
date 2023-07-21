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
package jpl.gds.product.automation.hibernate;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringEncrypter;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * Configures and provides access to Hibernate sessions, tied to the
 * current thread of execution. Follows the Thread Local Session
 * pattern, see {@link "http://hibernate.org/42.html"}.
 *
 * Gets all hibernate properties from the ProductAutomationConfig and sets them in the hibernate configuration
 * programmatically.
 * This eliminates the need for a hibernate config file.
 *
 * @version 06/15/16  - MPCS-8179 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
public class AutomationSessionFactory {
    /*
     * Use the ProductAutomationTracer but not the AutomationLogger.
     * AutomationLogger includes formatting and info that's specific to the arbiter and process threads.
     */
    private static Tracer                trace;

	/*
	 * MPCS-8179 06/07/16 - removed property names
	 *               Moved to ProductAutomationConfig or deleted
	 */

	// Hibernate properties
	private static final String HIBERNATE_URL = "hibernate.connection.url";
	private static final String HIBERNATE_USER = "hibernate.connection.username";
	private static final String HIBERNATE_PASSWORD = "hibernate.connection.password";

	private static final String HIBERNATE_QUERY_CACHE = "hibernate.cache.use_query_cache";
	private static final String HIBERNATE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";

	private final ThreadLocal<Session> THREADLOCAL = new ThreadLocal<Session>();

    private Configuration CONFIGURATION;
    private org.hibernate.SessionFactory SESSION_FACTORY;

    /**
     * Set to true if database connection is OK, false if not.
	 * MPCS-11649  - Since we are no longer verifying the database connection in the constructor,
	 * and since PDPP applications need this condition to be true before beginning their own startup, initializing
	 * to true instead of false. When the connection is tested later, it can be set to false if there is a problem.
     */
    public boolean DATABASE_STATUS = true;

    // property values
    private String DATABASE;
    private String DATABASE_PORT;
    
    /*
	 * MPCS-8382  8/22/2016 - Adding PDPP db host, user and password.
	 */
    private String HOST_NAME;
    private String USER;
    private String PASSWORD;

    // MPCS-8179 06/15/16 - Added property. "jdbc:mysql://" was hard-coded, but is now configurable
    private String CONNECTION_PREFIX;
    private boolean USE_CACHE;

    
    
    /**
     *  As part of the R8 refactor, this init and setup needs to be called once the session factory is 
     * created.  I think this class should be deprecated and recreated later using non-home grown solutions but I am 
     * just getting it working for now.  
     * 
     */

    private void setupConstants(final ApplicationContext appContext) {
    	final ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);
        final IDatabaseProperties dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);

    	DATABASE = config.getDatabaseName();
    	DATABASE_PORT = config.getDatabasePort();;
    	HOST_NAME = config.getDatabaseHost();
    	USER = config.getUser();
    	PASSWORD = config.getEncryptedPassword();
    	USE_CACHE = config.getUseCacheForGui();

    	CONNECTION_PREFIX = dbProperties.getConnectionStringPrefix();
        trace = TraceManager.getTracer(appContext, Loggers.PDPP);
    }

    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public AutomationSessionFactory(final ApplicationContext appContext) {
		setupConstants(appContext);
		
		//  - 2/26/2013 - Need the config dir in the classpath.
		addConfigDirToClassPath();

		// Decrypt the password if it is set.
		if (!PASSWORD.isEmpty()) {
			try {
				final StringEncrypter dc = new StringEncrypter();
				PASSWORD = dc.decrypt(PASSWORD);
			} catch (final GeneralSecurityException e) {
				trace.error("AutomationSessionFactory - Could not decrypt the database password: " + e.getMessage());
			}
		}
		resetConfiguration();
    }

	/**
	 * For ehcache, the config directory needs to be on the classpath.  So this adds it, pretty simple.
	 */
	private void addConfigDirToClassPath() {
		try {
			final File file = new File(GdsSystemProperties.getGdsDirectory());

			final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		    method.setAccessible(true);
		    method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{file.toURI().toURL()});
		} catch (final Exception e) {
			trace.error("AutomationSessionFactory - Failed to add config dir to classpath: " + e.getMessage());
		}
	}

	private void resetConfiguration() {
		CONFIGURATION = new Configuration();
	}

	/**
     * Builds the connection URL.
     * 
     * @param connectionPrefix
     *            the prefix for the connection
     * @param hostName
     *            the name of the host to which the connection is targeted
     * @param databasePort
     *            the port number to use for the connection
     * @param database
     *            the name of the database to which the connection will be made
     * @return the URL to use for the connection
     */
	public String buildConnectionUrl(final String connectionPrefix, final String hostName, final String databasePort, final String database) {
		return new StringBuilder(connectionPrefix)
				.append(hostName)
				.append(':')
				.append(databasePort)
				.append('/')
				.append(database)
				.toString();
	}

	/**
	 * Test the connection to the database. This is used to make sure that all of the configured host, port, dbname and
	 * password work before we configure the session factory.  If these do not work, the session factory will
	 * not be created.
	 * @return true if the configured values allow us to connect to the database.
	 */
	private boolean testConnection(final String url) throws DatabaseException {

		boolean couldConnect = false;
    	
		try {
	    	final Connection dbConn = DriverManager.getConnection(url, USER, PASSWORD);
	    	dbConn.close();
	    	couldConnect = true;
		} catch (final Exception e) {
			throw new DatabaseException("AutomationSessionFactory - Could not connect to database \n" +
					"Addr: " + url + ", user: " + USER + ", and using a password: " + PASSWORD.isEmpty() + 
                    "\n" + e.getCause() + ": ", e);
		}
		
		return couldConnect;
	}

	private synchronized void configure(final ApplicationContext appContext, final String hostName, final String databasePort, final String database) throws DatabaseException {
		/**
		 * MPCS-8382  8/23/2016 - Test the connection because building the session factory does not see if everything works.  It could
		 * create it properly if all of the configuration was correct, but the URL and password were not correct.  This will make sure all
		 * of the configured values are correct and actually make a DB connection before we bother setting up the session factory.
		 * 
		 * MPCS-8380 11/17/2016 - Added DB HOST/PORT override and minor updates to checking connection logic
		 * If the connection is unsuccessful the application will now shut down
		 */
		if (hostName != null) { 
			HOST_NAME = hostName;
		} 
		
		if (databasePort != null) { 
			DATABASE_PORT = databasePort;
		}

		final String url = buildConnectionUrl(CONNECTION_PREFIX,
				HOST_NAME,
				DATABASE_PORT,
				database == null ? DATABASE : database);

        DATABASE_STATUS = testConnection(url);
		if (DATABASE_STATUS) {
			try {
				// Check to see if we can connect first.
				CONFIGURATION.setProperty(HIBERNATE_URL, url)
				.setProperty(HIBERNATE_USER, USER)
				.setProperty(HIBERNATE_PASSWORD, PASSWORD);

				//  - 2/15/2013 - Set the cache enabled values.
				CONFIGURATION.setProperty(HIBERNATE_QUERY_CACHE, String.valueOf(USE_CACHE));
				CONFIGURATION.setProperty(HIBERNATE_SECOND_LEVEL_CACHE, String.valueOf(USE_CACHE));

				final ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);

				// MPCS-9529 03/20/18 - Updated to use new hibernate properties function in ProductAutomationProperties
                /*
                 * Set all the hibernateConfig properties
                 * DO NOT USE hibernate configurationg.setProperties - only sets NEW properties, won't override any
                 * exisiting properties...
                 */
                for (final Map.Entry<String, String> set : config.getHibernateProperties().entrySet()) {
                    CONFIGURATION.setProperty(set.getKey(), set.getValue());
                }

				// Get all the classes from the config.
				for (final String mappedClass : config.getClassesList()) {
					CONFIGURATION.addAnnotatedClass(Class.forName(mappedClass));
				}

				SESSION_FACTORY = CONFIGURATION.buildSessionFactory();
			} catch (final Exception e) {
				trace.error("AutomationSessionFactory - Error Creating SessionFactory", e);
			}
		}
	}

	/**
     * Returns the ThreadLocal Session instance. Lazy initialize
     * the <code>SessionFactory</code> if needed.
     * 
     * @param appContext
     *            the Spring Application Context *
     * @return the ThreadLocal Session instance
     * @throws HibernateException
     *             if a Hibernate database error occurs
     */
    public Session getSession(final ApplicationContext appContext) throws HibernateException {

		Session session = THREADLOCAL.get();

		if (session == null || !session.isOpen()) {
			if (SESSION_FACTORY == null) {
				// If we get here, no overrides will be used.
				rebuildSessionFactory(appContext);
			}

			session = (SESSION_FACTORY != null) ? SESSION_FACTORY.openSession()
					: null;

			//  - 2/15/2013 - Set the cache mode if caching enabled.
			if (USE_CACHE) {
				session.setCacheMode(CacheMode.NORMAL);
			}

			THREADLOCAL.set(session);
		}

        return session;
    }

	/**
     * Rebuilds the session factory using the configured default host, database port and database
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public void rebuildSessionFactory(final ApplicationContext appContext) {
		rebuildSessionFactory(appContext, HOST_NAME, DATABASE_PORT, DATABASE);
	}

	/**
     * Rebuild hibernate session factory
     * 
     * @param appContext
     *            the Spring Application Context
     * @param hostName
     *            hostname on which the session resides
     * @param databasePort
     *            the port number to use when rebuilding the connection
     * @param database
     *            the database to which the connection will be made
     */
    public void rebuildSessionFactory(final ApplicationContext appContext, final String hostName,
                                      final String databasePort, final String database) {
        try {
            resetConfiguration();
            configure(appContext, hostName, databasePort, database);
        }
        catch (final Exception e) {
            TraceManager.getDefaultTracer().error("AutomationSessionFactory - Error Creating SessionFactory", e);
        }
    }

	/**
     *  Close the single hibernate session instance.
     *
     *  @throws HibernateException indicates problems cleaning up
     */
    public void closeSession() throws HibernateException {
        final Session session = THREADLOCAL.get();
        THREADLOCAL.set(null);

        if (session != null) {
            session.close();
        }
    }

	/**
	 * Return session factory
	 *
	 * @return stored instance of the hibernate SessionFactory
	 *
	 */
	public org.hibernate.SessionFactory getSessionFactory() {
		return SESSION_FACTORY;
	}
	
	public synchronized void closeSessionFactory() {
		if (SESSION_FACTORY != null) {
			SESSION_FACTORY.close();
		}
	}

	/**
     *  Return hibernate configuration
     *
     * @return stored instance of the hibernate configuration
     */
	public Configuration getConfiguration() {
		return CONFIGURATION;
	}
}