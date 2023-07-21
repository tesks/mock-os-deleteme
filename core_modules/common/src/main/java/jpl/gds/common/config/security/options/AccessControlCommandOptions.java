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
package jpl.gds.common.config.security.options;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.EnumOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.filesystem.FileOptionParser;

/**
 * This class creates command line option objects used for parsing access
 * control options and automatically setting the parsed values into a
 * AccessControlParameters object. It is supplied with a unique instance of
 * AccessControlParameters in the constructor. It also takes an
 * AccessControlConfiguration object, from which default values are obtained.
 * Either the global AccessControlConfiguration can be used, or a unique
 * instance. Once an instance of this class is constructed, it provides public
 * members for each defined option, which can be individually added to a class
 * that extends BaseCommandOptions and can be individually parsed by an
 * application. Alternatively, there are convenience methods to get or parse
 * collections of options.
 * <p>
 * CAVEAT: At this time, it is impossible to create a unique instance of
 * AccessControlConfiguration.
 * 
 * 
 */
public class AccessControlCommandOptions implements ICommandLineOptionsGroup {

    private final AccessControlParameters acParams;
    private final SecurityProperties acConfig;
    
    /** Long name for user role option */
    public static final String ROLE_LONG = "role";
    /** Long name for login method option */
    public static final String METHOD_LONG = "loginMethod";
    /** Long name for keytab file option */
    public static final String KEYTAB_LONG = "keytabFile";
    /** Long name for user ID option */
    public static final String USER_LONG = "username";

    /**
     * USER_ROLE command option for supplying the user role. Parsing this option
     * sets the "user role" property in the AccessControlParameters member
     * instance.
     */
    public final EnumOption<CommandUserRole> USER_ROLE;

    /**
     * LOGIN_METHOD_GUI command option for supplying the login method, for GUI
     * applications. Parsing this option sets the "login method" property in the
     * AccessControlParameters member instance.
     */
    public final LoginEnumOption LOGIN_METHOD_GUI;

    /**
     * LOGIN_METHOD_NON_GUI command option for supplying the login method, for
     * non-GUI applications. Parsing this option sets the "login method"
     * property in the AccessControlParameters member instance.
     */
    public final LoginEnumOption LOGIN_METHOD_NON_GUI; 

    /**
     * KEYTAB_FILE command option for supplying a keytab file. Will verify the
     * file exists. Parsing this option sets the "keytab file" property in the
     * AccessControlParameters member instance.
     */
    public final FileOption KEYTAB_FILE;

    /**
     * USER_ID command option for supplying the user name. Parsing this option
     * sets the "user id" property in the AccessControlParameters member
     * instance.
     */
    public final StringOption USER_ID;

    /**
     * Constructor that takes a unique instance of AccessControlConfiguration
     * for default configuration.
     * 
     * @param config
     *            the SecurityProperties containing default
     *            configuration.
     * @param params
     *            The AccessControlParameters object the command line options in
     *            this class will populate.
     */
    public AccessControlCommandOptions(final SecurityProperties config,
            final AccessControlParameters params) {

        if (config == null || params == null) {
            throw new IllegalArgumentException(
                    "Arguments to constructor may not be null");
        }

        acParams = params;
        acConfig = config;
        
        LOGIN_METHOD_GUI = new LoginEnumOption(true);
        LOGIN_METHOD_NON_GUI = new LoginEnumOption(
                false);
        USER_ID = new StringOption(null, USER_LONG,
                "username", "security username; needed with --"
                        + LOGIN_METHOD_NON_GUI.getLongOpt() + " of "
                        + LoginEnum.KEYTAB_FILE, false);
        USER_ID.setDefaultValue("");
        
        KEYTAB_FILE = new FileOption(null, KEYTAB_LONG,
                "keytabFile", "security keytab file; needed with --"
                        + LOGIN_METHOD_NON_GUI.getLongOpt() + " of "
                        + LoginEnum.KEYTAB_FILE, false, true);
        
        USER_ROLE = new EnumOption<>(
                CommandUserRole.class, null, ROLE_LONG, "userRole",
                "security user role", false);

        USER_ROLE.setParser(new UserRoleOptionParser());
        KEYTAB_FILE.setParser(new KeytabOptionParser());
        USER_ID.setParser(new UserNameOptionParser());

    }

    /**
     * Gets the AccessControlParameters member object.
     * 
     * @return AccessControlParameters; never null
     */
    public AccessControlParameters getAccessControlParameters() {
        return acParams;
    }

    /**
     * Gets the SecurityProperties member object.
     * 
     * @return SecurityProperties; never null
     */
    public SecurityProperties getSecurityProperties() {
        return acConfig;
    }

    /**
     * Gets the Collection of all command line options defined in this class
     * that apply to GUIs.
     * 
     * @return Collection of ICommandLineOption; never null
     */
    public Collection<ICommandLineOption<?>> getAllGuiOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(this.KEYTAB_FILE);
        result.add(this.LOGIN_METHOD_GUI);
        result.add(this.USER_ROLE);
        result.add(this.USER_ID);

        return result;
    }

    /**
     * Gets the Collection of all command line options defined in this class
     * that apply to non-GUIs.
     * 
     * @return Collection of ICommandLineOption; never null
     */
    public Collection<ICommandLineOption<?>> getAllNonGuiOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(this.KEYTAB_FILE);
        result.add(this.LOGIN_METHOD_NON_GUI);
        result.add(this.USER_ROLE);
        result.add(this.USER_ID);

        return result;
    }

    /**
     * Parses all the GUI options defined by this class from the supplied
     * command line object. Requires none of the options, but WILL result in
     * non-supplied access control properties being set to their defaults in the
     * AccessControlParameters instance if and only if no previous value has
     * been set. Result values are set into the AccessControlParameters member.
     * Any option not used by the supplied command line is effectively ignored.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllGuiOptionsAsOptional(final ICommandLine commandLine)
            throws ParseException {
        LOGIN_METHOD_GUI.parse(commandLine);
        KEYTAB_FILE.parse(commandLine);
        USER_ROLE.parse(commandLine);
        USER_ID.parse(commandLine);
    }

    /**
     * Parses all the non-GUI options defined by this class from the supplied
     * command line object. Requires none of the options, but WILL result in
     * non-supplied access control properties being set to their defaults in the
     * AccessControlParameters instance if and only if no previous value has
     * been set. Result values are set into the AccessControlParameters member.
     * Any option not used by the supplied command line is effectively ignored.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllNonGuiOptionsAsOptional(final ICommandLine commandLine)
            throws ParseException {
        LOGIN_METHOD_NON_GUI.parse(commandLine);
        KEYTAB_FILE.parse(commandLine);
        USER_ROLE.parse(commandLine);
        USER_ID.parse(commandLine);
    }

    /**
     * Option class for the LOGIN_METHOD options. May be configured for GUI or
     * non-GUI operation. Note this inner class is deliberately public. We may
     * encounter a situation in which we need to set the GUI/non-GUI flag after
     * construction.
     * 
     */
    public class LoginEnumOption extends EnumOption<LoginEnum> {

        private static final long serialVersionUID = 1L;
        private boolean isGui;
        

        /**
         * Constructor.
         * 
         * @param isGui
         *            true if this option is for a GUI application; false for
         *            non-GUI
         */
        public LoginEnumOption(final boolean isGui)
        {
            super(LoginEnum.class, null, METHOD_LONG, "loginMethod",
                    "security login method", false,
                           (isGui ? LoginEnum.guiChoicesList()
                                  : LoginEnum.nonGuiChoicesList()));
            setGui(isGui);
        }

        /**
         * Sets GUI or non-GUI configuration of this Option. Resets the parser
         * instance attached to this option.
         * 
         * @param gui
         *            true if GUI option, false if non-GUI option
         */
        public void setGui(final boolean gui) {
            isGui = gui;
            setParser(new LoginEnumOptionParser(gui));
        }
        
        /**
         * Indicates if this is a GUI or non-GUI configuration of this Option.
         * 
         * @return true if GUI option, false if non-GUI option
         */
        public boolean isGui() {
            return isGui;
        }

    }

    /**
     * Option parser for the LOGIN_METHOD options. Will set the parsed login
     * method into the AccessControlParameters. May be configured for GUI or
     * non-GUI operation.
     * 
     */
    protected class LoginEnumOptionParser extends EnumOptionParser<LoginEnum> {

        private LoginEnum defaultVal;


        /**
         * Constructor
         * 
         * @param isGui
         *            true if this parser should assume a GUI configuration
         *
         */
        public LoginEnumOptionParser(final boolean isGui) {
            super(LoginEnum.class,
                  isGui ? LoginEnum.guiChoicesList()
                        : LoginEnum.nonGuiChoicesList());

            setGui(isGui);
            // Convert options to uppercase
            setConvertToUpperCase(true);
        }


        /**
         * Sets GUI or non-GUI configuration of this OptionParser. Resets the
         * default value of the option.
         * 
         * @param gui
         *            true if GUI option, false if non-GUI option
         */
        private void setGui(final boolean gui) {
            defaultVal = gui ? acConfig.getDefaultGuiAuthorizationMode()
                    : acConfig.getDefaultCliAuthorizationMode();
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public LoginEnum parse(final ICommandLine commandLine,
                final ICommandLineOption<LoginEnum> opt) throws ParseException {
            LoginEnum loginMethod = super.parse(commandLine, opt);

            /*
             * The original code in ReservedOptions seemed to go to a lot of
             * trouble to make sure that any current setting in the AC params
             * was preserved if no value was supplied for the option. If there
             * was nothing already set in the params object, then the default
             * value is assigned.
             */
            if (loginMethod == null) {
                loginMethod = acParams.getLoginMethod() == null ? defaultVal
                        : acParams.getLoginMethod();
            }

            /*
             * Check for conflicting options. KEYTAB_FILE and USER_NAME should
             * only be supplied with login method of KEYTAB_FILE.
             */
            if (!loginMethod.equals(LoginEnum.KEYTAB_FILE)
                    && (isPresent(commandLine, KEYTAB_FILE) || commandLine
                            .hasOption(USER_ID.getLongOpt()))) {
                throw new ParseException("Login method: " + loginMethod
                        + " does " + "not require a keytab file and username");
            }

            acParams.setLoginMethod(loginMethod);

            return loginMethod;
        }

    }

    /**
     * Option parser class for the USER_ID option. Will set the parsed user
     * ID into the AccessControlParameters.
     * 
     */
    protected class UserNameOptionParser extends StringOptionParser {
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            String user = super.parse(commandLine, opt);

            /*
             * The original code in ReservedOptions seemed to go to a lot of
             * trouble to make sure that any current setting in the AC params
             * was preserved if no value was supplied for the option. If there
             * was nothing already set in the params object, then the default
             * value is assigned, which is the empty string.
             */
            if (user == null) {
                user = (acParams.getUserId() == null) ? "" : acParams
                        .getUserId();
            }

            acParams.setUserId(user);

            return user;
        }
    }

    /**
     * Option parser class for the USER_ROLE option. Will set the parsed user
     * role into the AccessControlParameters.
     * 
     */
    protected class UserRoleOptionParser extends
    EnumOptionParser<CommandUserRole> {

        /**
         * Constructor.
         */
        public UserRoleOptionParser() {
            super(CommandUserRole.class);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public CommandUserRole parse(final ICommandLine commandLine,
                final ICommandLineOption<CommandUserRole> opt) throws ParseException {
            CommandUserRole role = super.parse(commandLine, opt);

            /*
             * The original code in ReservedOptions seemed to go to a lot of
             * trouble to make sure that any current setting in the AC params
             * was preserved if no value was supplied for the option. If there
             * was nothing already set in the params object, then the default
             * value is assigned, which is the default role in the
             * AccessControlConfiguration.
             */
            if (role == null) {
                role = (acParams.getUserRole() == null) ? acConfig
                        .getDefaultRole() : acParams.getUserRole();
            }
            acParams.setUserRole(role);

            return role;
        }
    }

    /**
     * Option parser class for the KEYTAB option. Will set the keytab file name
     * into the AccessControlParameters.
     * 
     */
    protected class KeytabOptionParser extends FileOptionParser {

        /**
         * Constructor.
         */
        public KeytabOptionParser() {
            super(true);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.filesystem.FileOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            String file = super.parse(commandLine, opt);

            if (file == null && acParams.getLoginMethod() == LoginEnum.KEYTAB_FILE) {
                file = (acParams.getKeytabFile() == null) ? acConfig
                        .getDefaultKeytabFile() : acParams.getKeytabFile();

            }
            if (file != null) {
                acParams.setKeytabFile(file);
            }
            return file;
        }

    }

}
