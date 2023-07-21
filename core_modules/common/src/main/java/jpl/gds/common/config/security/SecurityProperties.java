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
package jpl.gds.common.config.security;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A configuration properties class for security-related configuration defaults.
 * Not to be confused with @see AccessControlConfiguration, which contains the
 * mutable runtime security parameters.
 * 
 * @since R8
 */
public class SecurityProperties extends GdsHierarchicalProperties {
    
    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "security.properties";
    
    private static final String PROPERTY_PREFIX = "security.";
    
    private static final String ENABLED               = PROPERTY_PREFIX + "enabled";
    private static final String ROLES                 = PROPERTY_PREFIX + "roles";
    private static final String DEFAULT_GUI_AUTH_MODE =
                                    PROPERTY_PREFIX + "defaultGuiAuthMode";
    private static final String DEFAULT_CLI_AUTH_MODE =
                                    PROPERTY_PREFIX + "defaultCliAuthMode";
    private static final String DEFAULT_ROLE          = PROPERTY_PREFIX + "defaultRole";
    private static final String DEFAULT_KEYTAB_FILE =
                                    PROPERTY_PREFIX + "defaultKeytabFile";
    
    private static final String KERBEROS_REALM = PROPERTY_PREFIX + "kerberosJavaOptions.realm";
    private static final String KERBEROS_KDC = PROPERTY_PREFIX + "kerberosJavaOptions.kdc";
    
    private static final String DAEMON_KEYTAB_FILE = PROPERTY_PREFIX + "daemon.defaultKeytabFile";
    private static final String DAEMON_USERNAME = PROPERTY_PREFIX + "daemon.defaultUsername";

    private static final boolean     DEFAULT_ENABLED               = false;
    private static final LoginEnum   DEFAULT_DEFAULT_GUI_AUTH_MODE =
                                         LoginEnum.GUI_WINDOW;
    private static final LoginEnum   DEFAULT_DEFAULT_CLI_AUTH_MODE =
                                         LoginEnum.TEXT_PROMPT;
    private static final String      DEFAULT_DEFAULT_KEYTAB_FILE = "";
    private static final CommandUserRole DEFAULT_DEFAULT_ROLE          =
                                         CommandUserRole.VIEWER;
    private static final String      DEFAULT_DAEMON_KEYTAB_FILE = "";
    private static final String      DEFAULT_DAEMON_USERNAME = "";
    
    /**
     * Test constructor
     */
    public SecurityProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor. Reads the default property file.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public SecurityProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }
    
    /**
     * Get enabled state.
     *
     * @return True if enabled
     */
    public boolean getEnabled()
    {
        return getBooleanProperty(ENABLED, DEFAULT_ENABLED);
    }


    /**
     * Get roles.
     *
     * @return Set of roles
     */
    public Set<CommandUserRole> getRoles()
    {
        final List<String> roles = getListProperty(ROLES, null, ",");

        if ((roles == null) || roles.isEmpty())
        {
            return Collections.<CommandUserRole>emptySet();
        }

        final Set<CommandUserRole> set = new TreeSet<CommandUserRole>();

        for (final String s : roles)
        {
            CommandUserRole cur = null;

            try
            {
                cur = CommandUserRole.valueOf(s.trim());
            }
            catch (final IllegalArgumentException iae)
            {
                continue;
            }

            set.add(cur);
        }

        return Collections.unmodifiableSet(set);
    }


    /**
     * Get default authorization mode for GUIs.
     *
     * @return Default authorization mode
     */
    public LoginEnum getDefaultGuiAuthorizationMode()
    {
        final String found = getProperty(DEFAULT_GUI_AUTH_MODE, DEFAULT_DEFAULT_GUI_AUTH_MODE.name());

        try
        {
            return LoginEnum.valueOf(found.trim().toUpperCase());
        }
        catch (final IllegalArgumentException iae)
        {
            return DEFAULT_DEFAULT_GUI_AUTH_MODE;
        }
    }


    /**
     * Get default authorization mode for command-line mode.
     *
     * @return Default authorization mode
     */
    public LoginEnum getDefaultCliAuthorizationMode()
    {
        final String found = getProperty(DEFAULT_CLI_AUTH_MODE, DEFAULT_DEFAULT_CLI_AUTH_MODE.name());

        try
        {
            return LoginEnum.valueOf(found.trim().toUpperCase());
        }
        catch (final IllegalArgumentException iae)
        {
            return DEFAULT_DEFAULT_CLI_AUTH_MODE;
        }
    }


    /**
     * Get default keytab file (full path and name.)
     *
     * @return Default keytab file path and name
     */
    public String getDefaultKeytabFile()
    {
        return getProperty(DEFAULT_KEYTAB_FILE, DEFAULT_DEFAULT_KEYTAB_FILE);
    }


    /**
     * Get default role.
     *
     * @return CPD user role
     */
    public CommandUserRole getDefaultRole()
    {
        final String found = getProperty(DEFAULT_ROLE, DEFAULT_DEFAULT_ROLE.name());

        try
        {
            return CommandUserRole.valueOf(found.trim().toUpperCase());
        }
        catch (final IllegalArgumentException iae)
        {
            return DEFAULT_DEFAULT_ROLE;
        }
    }
    
    /**
     * Get the Kerberos realm
     * 
     * @return Kerberos realm
     */
    public String getKerberosRealm()
    {
        return getProperty(KERBEROS_REALM);
    }
    
    /**
     * Get the Kerberos KDC
     * 
     * @return Kerberos KDC
     */
    public String getKerberosKdc()
    {
        return getProperty(KERBEROS_KDC);
    }
    
    /**
     * Get default keytab file for daemon process (full path and name). If the configured
     * path is relative, then the $HOME directory is prepended.
     *
     * @return default keytab file path and name, or the empty string if none defined
     */
    public String getDefaultDaemonKeytabFile()
    {
        final String dir = getProperty(DAEMON_KEYTAB_FILE, DEFAULT_DAEMON_KEYTAB_FILE);
        if (dir.equals(DEFAULT_DAEMON_KEYTAB_FILE)) {
            return dir;
        }
        if (!dir.startsWith(File.separator)) {
            return GdsSystemProperties.getSystemProperty("user.home") + File.separator + dir;
        } else {
            return dir;
        }
    }

    /**
     * Get default authentication username for daemon process.
     *
     * @return Default username
     */
    public String getDefaultDaemonUsername()
    {
        return getProperty(DAEMON_USERNAME, DEFAULT_DAEMON_USERNAME);

    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
