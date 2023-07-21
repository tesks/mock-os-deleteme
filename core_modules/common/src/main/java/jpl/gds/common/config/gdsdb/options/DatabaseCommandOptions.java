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
package jpl.gds.common.config.gdsdb.options;

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.FlagOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * This class creates command line option objects used for parsing database
 * options and automatically setting the parsed values into a
 * DatabaseConfiguration object. It can utilize the global DatabaseConfiguration
 * object, or can be supplied with a unique instance in the constructor. Once an
 * instance of this class is constructed, it provides public members for each
 * defined option, which can be individually added to a class that extends
 * BaseCommandOptions and can be individually parsed by an application.
 * Alternatively, there are convenience methods to get or parse collections of
 * options.
 * <p>
 * CAVEAT: At this time, it is impossible to create a unique instance of
 * DatabaseConfiguration.
 * 
 */
public class DatabaseCommandOptions implements ICommandLineOptionsGroup {

    private static final UnsignedInteger MIN_PORT = UnsignedInteger.valueOf(
            PortHolder.MIN_VALUE);

    private static final UnsignedInteger MAX_PORT = UnsignedInteger.valueOf(
            PortHolder.MAX_VALUE);
    
    /** Long name for database host option */
    public static final String DB_HOST_LONG = "databaseHost";
    /** Long name for database port option */
    public static final String DB_PORT_LONG = "databasePort";
    /** Long name for no database option */
    public static final String NO_DB_LONG = "noDatabase";
    /** Long name for database password option */
    public static final String DB_PWD_LONG = "dbPwd";
    /** Long name for database user option */
    public static final String DB_USER_LONG = "dbUser";

    private final IDatabaseProperties    dbProperties;

    /**
     * The NO_DATABASE command option. Parsing this option sets the
     * "use database" flag in the DatabaseConfiguration member instance.
     */
    public final FlagOption NO_DATABASE = new FlagOption("I", NO_DB_LONG,
            "execute without connecting to a database", false);

    /**
     * The DATABASE_HOST command option. Parsing this option sets the "host"
     * property in the DatabaseConfiguration member instance.
     */
    public final StringOption DATABASE_HOST = new StringOption("j",
            DB_HOST_LONG, "host", "the host that the database resides on",
            false);

    /**
     * The DATABASE_PORT command option. Parsing this option sets the "port"
     * property in the DatabaseConfiguration member instance.
     */
    public final PortOption DATABASE_PORT = new PortOption("n", DB_PORT_LONG,
            "port", "the network port number the database is listening on", false);

    /**
     * The DATABASE_PASSWORD command option. Parsing this option sets the
     * "password" property in the DatabaseConfiguration member instance.
     */
    public final StringOption DATABASE_PASSWORD = new StringOption(null,
            DB_PWD_LONG, "password",
            "the password required to connect to the database", false);

    /**
     * The DATABASE_USERNAME command option. Parsing this option sets the
     * "username" property in the DatabaseConfiguration member instance.
     */
    public final StringOption DATABASE_USERNAME = new StringOption(null,
            DB_USER_LONG, "username",
            "the username required to connect to the database", false);

    /**
     * Constructor that takes a unique instance of DatabaseConfiguration. The
     * supplied DatabaseConfiguration will be used both to determine defaults,
     * and to set parsed values into.
     * 
     * @param localConfig
     *            the DatabaseConfiguration instance to use
     */
    public DatabaseCommandOptions(final IDatabaseProperties localConfig) {
        this.dbProperties = localConfig;

        NO_DATABASE.setParser(new NoDatabaseOptionParser());
        DATABASE_HOST.setParser(new DatabaseHostOptionParser());
        DATABASE_PORT.setParser(new DatabasePortOptionParser());
        DATABASE_USERNAME.setParser(new DatabaseUserOptionParser());
        DATABASE_PASSWORD.setParser(new DatabasePasswordOptionParser());
    }

    /**
     * Gets the DatabaseConfiguration member object.
     * 
     * @return DatabaseConfiguration; never null
     */
    public IDatabaseProperties getDatabaseConfiguration() {
        return dbProperties;
    }

    /**
     * Gets a Collection containing all command line options defined by this
     * class.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(DATABASE_HOST);
        result.add(DATABASE_PORT);
        result.add(DATABASE_USERNAME);
        result.add(DATABASE_PASSWORD);
        result.add(NO_DATABASE);

        return result;
    }

    /**
     * Gets a Collection containing the command line options used for locating
     * the database instance.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllLocationOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(DATABASE_HOST);
        result.add(DATABASE_PORT);

        return result;
    }

    /**
     * Gets a Collection containing all command line options defined by this
     * class, less the NO_DATABASE option (which would be meaningless for
     * queries, for instance).
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptionsWithoutNoDb() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(DATABASE_HOST);
        result.add(DATABASE_PORT);
        result.add(DATABASE_USERNAME);
        result.add(DATABASE_PASSWORD);

        return result;
    }

    /**
     * Parses all the options defined by this class from the supplied command
     * line object. Result values are set into the DatabaseConfiguration member.
     * Any option not used by the supplied command line is effectively ignored.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllOptionsAsOptional(final ICommandLine commandLine) throws ParseException {
        DATABASE_HOST.parse(commandLine);
        DATABASE_PORT.parse(commandLine);
        DATABASE_USERNAME.parse(commandLine);
        DATABASE_PASSWORD.parse(commandLine);
        NO_DATABASE.parse(commandLine);

    }

    /**
     * An option parser class for the NO_DATABASE command line option. Parsed
     * value will be set into the DatabaseConfiguration.
     * 
     */
    protected class NoDatabaseOptionParser extends FlagOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.FlagOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public Boolean parse(final ICommandLine commandLine,
                final ICommandLineOption<Boolean> opt) throws ParseException {

            final Boolean noDb = super.parse(commandLine, opt);

            if (noDb != null && noDb) {
                dbProperties.setUseDatabase(false);
            }

            return noDb;
        }
    }

    /**
     * An option parser class for the DATABASE_HOST command line option. Parsed
     * value will be set into the DatabaseConfiguration.
     * 
     */
    protected class DatabaseHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String name = super.parse(commandLine, opt);

            if (name != null) {
                dbProperties.setHost(name);
            }

            return name;
        }
    }

    /**
     * An option parser class for the DATABASE_USER command line option. Parsed
     * value will be set into the DatabaseConfiguration.
     * 
     */
    public class DatabaseUserOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String name = super.parse(commandLine, opt);

            if (name != null) {
                dbProperties.setUsername(name);
            }

            return name;
        }
    }

    /**
     * An option parser class for the DATABASE_PASSWORD command line option.
     * Parsed value will be set into the DatabaseConfiguration.
     * 
     */
    public class DatabasePasswordOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String pwd = super.parse(commandLine, opt);

            if (pwd != null) {
                dbProperties.setPassword(pwd);
            }

            return pwd;
        }
    }

    /**
     * An option parser class for the DATABASE_PORT command line option. Parsed
     * value will be set into the DatabaseConfiguration.
     * 
     */
    public class DatabasePortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public DatabasePortOptionParser() {
            super(MIN_PORT, MAX_PORT);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);
            if (port != null) {
                dbProperties.setPort(port.intValue());
            }
            return port;

        }

    }

}
