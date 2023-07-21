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
package jpl.gds.security.cam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.ws.rs.core.Cookie;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Project: AMMOS Mission Data Processing and Control System (AMPCS)
 * Package: jpl.gds.tc.impl.security
 * File:    AccessControl.java
 *
 */

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import gov.nasa.jpl.ammos.css.accesscontrol.AccessControlManager;
import gov.nasa.jpl.ammos.css.accesscontrol.AccessControlManager.LoginMethod;
import gov.nasa.jpl.ammos.css.accesscontrol.AccessControlManagerException;
import gov.nasa.jpl.ammos.css.accesscontrol.AuthorizationDecisionException;
import gov.nasa.jpl.ammos.css.accesscontrol.AuthorizationDecisionManager;
import gov.nasa.jpl.ammos.css.accesscontrol.AuthorizationTarget;
import gov.nasa.jpl.ammos.css.accesscontrol.SsoToken;
import gov.nasa.jpl.ammos.css.accesscontrol.SsoTokenManager;
import gov.nasa.jpl.ammos.css.accesscontrol.SsoTokenManagerException;
import gov.nasa.jpl.ammos.css.accesscontrol.SsoTokenManagerException.SsoTokenManagerErrorCode;
import gov.nasa.jpl.ammos.css.accesscontrol.UserProfile;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.shared.config.GdsSystemProperties;
//import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.sys.Shutdown.IShutdown;
import jpl.gds.shared.thread.SleepUtilities;


/**
 * Low-level interface to security classes. We get a token from security and
 * use it to create cookies for authentication. If the token becomes invalid
 * for any reason, we attempt to reauthenticate.
 *
 * NB: If the PerspectiveListener is non-null then he can provide a flag
 * that tells us that everything is shutting down, so we can skip any kind of
 * authentication activity. He gets that from the ExitPerspectiveMessage.
 *
 */
public class AccessControl extends Object
{
    private static final String NAME = "AccessControl";

    private static final long TENTH_SECOND   =  100L;
    private static final long ONE_SECOND     = 1000L;
    private static final long INVOKE_TIMEOUT = 10L * ONE_SECOND;
    private static final long LOGIN_TIMEOUT  = Long.MAX_VALUE;
    private static final long FILE_TIMEOUT   = 10L * ONE_SECOND;
    private static final long EXEC_TIMEOUT   = 10L * ONE_SECOND;
    private static final long SECURITY_DELAY = 0L;
    private static final long TOKEN_REQUEST_INTERVAL = 10L * ONE_SECOND;

    private static final boolean USE_SHUTDOWN_HOOK = true;
    private static final boolean USE_NEW_SHUTDOWN  = true;

    /** Number of login attempts allowed before giving up */
    private static final int RETRY_COUNT = 3;

    private static final String LOGIN_METHOD_PARM  =
        "-" + AccessControlManager.CFG_LOGIN_METHOD + "=";
    private static final String KEYTAB_FILE_PARM =
        "-" + AccessControlManager.CFG_KEYTAB_FILE + "=";
    private static final String USERNAME_PARM = "-username=";

    private static final String REALM_JAVA_PROPERTY = "java.security.krb5.realm";
    private static final String KDC_JAVA_PROPERTY   = "java.security.krb5.kdc";
    
    /** Access control manager */
    private final AccessControlManager _acm = new AccessControlManager();

    /** SSO token manager */
    private SsoTokenManager _stm = null;

    /** Authorization decision manager */
    private AuthorizationDecisionManager _adm = null;

    /** Configuration */
 //   private static final SecurityProperties secProps = SecurityProperties.getGlobalInstance();
    
    /** Synchronize on this to prevent multiple popups */
    private static final Object SINGLE_POPUP = new Object();

    /** User */
    private String _user = "";

    /** User's role */
    private CommandUserRole _role = null;

    /** Current token (may be null) */
    private SsoToken _token = null;

    /** Current cookie from token (may be null) */
    private Cookie _cookie = null;

    /** Roles supported in this configuration */
    private final Set<CommandUserRole> _supportedRoles;

    /** Selected login method */
    private LoginMethod _loginMethod = null;

    /** True if in GUI mode */
    private final boolean _gui;

    /** Logger */
    private final Tracer _log;

    /** Used to release token in shutdown hook */
    private final ReleaseToken _releaseToken;

    /** Main shell or null */
    private Shell _mainShell = null;

    /** If true, just run without authorizing */
    private boolean _skipAuthorization = false;

    private final ExitableSecureApplication _application;

    /** Singleton */
    private static AccessControl _instance = null;

    /** For testing only */
    private static final boolean FORCE_SERIOUS = false;

    /** Continue to request token indefinitely */
    private boolean runContinuously = false;


    /**
     * Get instance.
     *
     * @return Instance or null if not yet created
     */
    public synchronized static AccessControl getInstance()
    {
        return _instance;
    }


    /**
     * Initialize singleton.
     *
     * @param rawUser    User
     * @param role       User role
     * @param loginEnum  Login method
     * @param keytabFile Keytab file
     * @param gui        True if a GUI login
     * @param mainShell  Main shell or null
     * @param log        Tracer
     *
     * @return Instance
     *
     * @throws AccessControlException If errors in creating
     */
    public synchronized static AccessControl
        createAccessControl(final SecurityProperties secProps,
        		            final String      rawUser,
                            final CommandUserRole role,
                            final LoginEnum   loginEnum,
                            final String      keytabFile,
                            final boolean     gui,
                            final Shell       mainShell,
                            final Tracer      log)
        throws AccessControlException
    {
        return createAccessControl(secProps,
        		                   rawUser,
                                   role,
                                   loginEnum,
                                   keytabFile,
                                   gui,
                                   mainShell,
                                   null,
                                   log);
    }


    /**
     * Initialize singleton.
     *
     * @param rawUser     User
     * @param role        User role
     * @param loginEnum   Login method
     * @param keytabFile  Keytab file
     * @param gui         True if a GUI login
     * @param mainShell   Main shell or null
     * @param perspective Perspective object or null
     * @param log         Tracer
     *
     * @return Instance
     *
     * @throws AccessControlException If errors in creating
     */
    public synchronized static AccessControl
        createAccessControl(final SecurityProperties secProps,
        		            final String              rawUser,
                            final CommandUserRole         role,
                            final LoginEnum           loginEnum,
                            final String              keytabFile,
                            final boolean             gui,
                            final Shell               mainShell,
                            final ExitableSecureApplication app,
                            final Tracer              log)
        throws AccessControlException
    {

        if (_instance != null)
        {
            throw new AccessControlException("Attempt to create " +
                                                 NAME             +
                                                 " more than once",
                                             true);
        }

        _instance = new AccessControl(secProps,
        		                      rawUser,
                                      role,
                                      loginEnum,
                                      keytabFile,
                                      gui,
                                      mainShell,
                                      app,
                                      log);
        return _instance;
    }


    /**
     * Private constructor.
     *
     * @param rawUser     User
     * @param role        User role
     * @param loginEnum   Login method
     * @param keytabFile  Keytab file if necessary
     * @param gui         True if a GUI login
     * @param mainShell   Main shell or null
     * @param perspective Perspective object or null
     * @param log         Tracer
     *
     * @throws AccessControlException If errors in creating managers
     */
    private AccessControl(final SecurityProperties secProps,
    		              final String              rawUser,
                          final CommandUserRole         role,
                          final LoginEnum           loginEnum,
                          final String              keytabFile,
                          final boolean             gui,
                          final Shell               mainShell,
                          final ExitableSecureApplication app,
                          final Tracer              log)
        throws AccessControlException
    {

        super();

        // Do first
        _log = log;

        _releaseToken = (USE_SHUTDOWN_HOOK ? new ReleaseToken(_log) : null);

        if (! secProps.getEnabled())
        {
            throw new AccessControlException(NAME + " not enabled", true);
        }

       _application = app;

        if (secProps.getKerberosRealm() != null && secProps.getKerberosKdc() != null) {
            _log.debug("Setting system properties realm=" + secProps.getKerberosRealm() + " and KDC="
                    + secProps.getKerberosKdc());
        	GdsSystemProperties.setSystemProperty(REALM_JAVA_PROPERTY, secProps.getKerberosRealm());
            GdsSystemProperties.setSystemProperty(KDC_JAVA_PROPERTY, secProps.getKerberosKdc());
        }

        _mainShell = mainShell;
        _user      = StringUtil.safeTrim(rawUser);
        _gui       = gui;

        if (_user.isEmpty())
        {
            throw new AccessControlException("User cannot be empty", true);
        }

        // Get supported roles, which may be a subset of those in enum
        _supportedRoles = secProps.getRoles();

        setUserRole(secProps, role);

        final LoginEnum temp =
            (loginEnum != null)
                ? loginEnum
                : (gui ? secProps.getDefaultGuiAuthorizationMode()
                       : secProps.getDefaultCliAuthorizationMode());

        _loginMethod = convert(temp);

        String _keytabFile = StringUtil.emptyAsNull(keytabFile);

        if (_keytabFile == null)
        {
            _keytabFile = secProps.getDefaultKeytabFile();
        }

        final List<String> args = new ArrayList<String>(2);

        if (_loginMethod == LoginMethod.KEYTAB_FILE)
        {
            args.add(LOGIN_METHOD_PARM  + _loginMethod);
            args.add(KEYTAB_FILE_PARM + _keytabFile);
            args.add(USERNAME_PARM + _user);
        }
        else
        {
            args.add(LOGIN_METHOD_PARM + _loginMethod);
        }

        try
        {
        	_acm.initialize(args.toArray(new String[args.size()]));
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to initialize access control manager: ", acme);

            throw new AccessControlException(
                          "Unable to initialize access control manager",
                          acme,
                          true);
        }

        try
        {
            _stm = _acm.getSsoTokenManager();
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to create SSO token manager: ", acme);

            throw new AccessControlException(
                          "Unable to create SSO token manager",
                          acme,
                          true);
        }

        try
        {
            _adm = _acm.getAuthorizationDecisionManager();
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to create authorization decision manager: ", acme);

            throw new AccessControlException(
                          "Unable to create authorization decision manager",
                          acme,
                          true);
        }

        try
        {
            _adm.initialize(_acm);
        }
        catch (final AuthorizationDecisionException ade)
        {
            _log.error("Unable to initialize authorization decision " + "manager: ", ade);

            throw new AccessControlException(
                          "Unable to initialize authorization decision " +
                              "manager",
                          ade,
                          true);
        }

        if (_loginMethod == LoginMethod.KEYTAB_FILE)
        {
            _log.debug("Setting up for SSO tokens with " + _user + "/" + _role + " and " + _loginMethod + " at '"
                    + _keytabFile + "'");
        }
        else
        {
            _log.debug("Setting up for SSO tokens with " + _user + "/" + _role + " and " + _loginMethod);
        }

        if (USE_SHUTDOWN_HOOK)
        {

            if (! USE_NEW_SHUTDOWN)
            {
                final Thread thread = new Thread(_releaseToken, "ReleaseToken");

                thread.setDaemon(true);

                Runtime.getRuntime().addShutdownHook(thread);
            }

            _releaseToken.setTokenManager(_stm);
        }
    }


    /**
     * Get token, prompting as required. We run the actual request through
     * Swing as a thread in order to make it work with SWT.
     *
     * @throws AccessControlException If errors in creating token
     */
    public synchronized void requestSsoToken()
        throws AccessControlException
    {
        _token  = null;
        _cookie = null;

        final AtomicReference<SsoToken>                 aToken   =
            new AtomicReference<SsoToken>(null);
        final AtomicReference<SsoTokenManagerException> aError   =
            new AtomicReference<SsoTokenManagerException>(null);
        final AtomicReference<Exception>                aUnknown =
            new AtomicReference<Exception>(null);
        
        final SsoTokenRunnable str = new SsoTokenRunnable(_user,
                                                          _loginMethod,
                                                          _stm,
                                                          aToken,
                                                          aError,
                                                          aUnknown);
        /*
         * Only post the
         * runnable to the Swing event loop if authenticating 
         * using the GUI window. We do not want a display to be required
         * otherwise.
         */
        if (_loginMethod == LoginMethod.GUI_WINDOW) {
        	SwingUtilities.invokeLater(str);
        } else {
        	str.run();
        }
        long start   = System.currentTimeMillis();
        long delta   = INVOKE_TIMEOUT;
        long timeout = start + delta;

        while (! str.getInvoked() && (delta > 0L))
        {
            SleepUtilities.checkedSleep(Math.min(delta, TENTH_SECOND));

            delta = timeout - System.currentTimeMillis();
        }
        
        if (! str.getInvoked())
        {
            throw new AccessControlException("Unable to get SSO token: " +
                                                 "invoke timeout",
                                             true);
        }

        _log.debug("Invoked after " + (System.currentTimeMillis() - start) + " MS");

        start   = System.currentTimeMillis();
        delta   = (_loginMethod == LoginMethod.KEYTAB_FILE)
                      ? FILE_TIMEOUT
                      : LOGIN_TIMEOUT;
        timeout = start + delta;

        while (! str.getFinished() && (delta > 0L))
        {
            SleepUtilities.checkedSleep(Math.min(delta, ONE_SECOND));

            delta = timeout - System.currentTimeMillis();
        }
        
        if (! str.getFinished())
        {
            throw new AccessControlException("Unable to get SSO token: " +
                                                 "login timeout",
                                             true);
        }

        /**
         * If we are shutting down just bail.
         */
        if (getPerspectiveExit())
        {
            return;
        }

        _log.log(TraceSeverity.DEBUG, "Login finished after " +
                      (System.currentTimeMillis() - start )+
                      " MS" +
                      true);

        final SsoToken                 token   = aToken.get();
        final SsoTokenManagerException error   = aError.get();
        final Exception                unknown = aUnknown.get();

        if (token == null)
        {
        	if (unknown != null)
            {
                throw new AccessControlException("Unable to get SSO token",
                                                 unknown,
                                                 true);
            }

            if (error != null)
            {
                final SsoTokenManagerErrorCode ec = error.getErrorCode();

                boolean serious =
                    ((ec != SsoTokenManagerErrorCode.INVALID_CREDENTIAL) &&
                     (ec != SsoTokenManagerErrorCode.NULL_VALUE));

                if (FORCE_SERIOUS)
                {
                    final File temp = new File("/tmp/force.serious");

                    if (temp.exists())
                    {
                        serious = true;

                        if (! temp.delete())
                        {
                            _log.error("Unable to delete " + temp.getAbsolutePath());
                        }
                    }
                }

                throw new AccessControlException("Unable to get SSO token",
                                                 error,
                                                 serious);
            }

            throw new AccessControlException("Unable to get SSO token",
                                             true);
        }

        // Add extra delay for security service
        SleepUtilities.checkedSleep(SECURITY_DELAY);

        _log.debug("Requested SSO token");

        _token  = token;
        _cookie = _stm.getSsoCookie(_token);

        if (USE_SHUTDOWN_HOOK)
        {
            _releaseToken.setToken(_token);
        }

        // Get actual user from token

        final UserProfile up = getUserProfile();

        _user = StringUtil.safeTrim(up.getUserId());

        _log.debug("User is now " + _user + " as a " + _role);

        for (final String group : up.getGroupList())
        {
            _log.debug("User " + _user + " is in group " + StringUtil.safeTrim(group));
        }
    }


    /**
     * Get cookie for SSO token.
     *
     * @return Cookie or null
     */
    public synchronized Cookie getSsoCookie()
    {
        return _cookie;
    }


    /**
     * Validate SSO token. We take an exception as meaning "false" unless it
     * is more than an invalid credential.
     *
     * @return True if token validates
     *
     * @throws AccessControlException If unexpected error
     */
    public synchronized boolean validateSsoToken()
        throws AccessControlException
    {
        if (_token == null)
        {
            return false;
        }

        boolean status = false;

        try
        {
            status = _stm.validateSsoToken(_token);
        }
        catch (final SsoTokenManagerException stme)
        {
            // Presume invalid. Throw if the reason is anything other than
            // username/password failure.

            final SsoTokenManagerErrorCode ec = stme.getErrorCode();

            if ((ec == SsoTokenManagerErrorCode.INVALID_CREDENTIAL) ||
                (ec == SsoTokenManagerErrorCode.NULL_VALUE))
            {
                _log.debug("Unable to validate SSO token: " + ec);
            }
            else
            {
                throw new AccessControlException(
                              "Unable to validate SSO token: " + ec,
                              stme,
                              true);
            }
        }

        return status;
    }


    /**
     * Invalidate SSO token.
     *
     * @return True if invalidation succeeded
     *
     * @throws AccessControlException If errors in invalidating token
     */
    public synchronized boolean invalidateSsoToken()
        throws AccessControlException
    {
        if (_token == null)
        {
            return true;
        }

        try
        {
            final boolean status = _stm.invalidateSsoToken(_token);

            if (status)
            {
                _token  = null;
                _cookie = null;
            }

            return status;
        }
        catch (final SsoTokenManagerException stme)
        {
            throw new AccessControlException("Unable to invalidate SSO token",
                                             stme);
        }
    }


    /**
     * Get user profile.
     *
     * @return User profile corresponding to token
     *
     * @throws AccessControlException If errors in getting profile
     */
    public synchronized UserProfile getUserProfile()
        throws AccessControlException
    {
        if (_token == null)
        {
            throw new AccessControlException(
                          "Unable to get profile for null SSO token");
        }

        try
        {
            return _stm.getUserProfile(_token);
        }
        catch (final SsoTokenManagerException stme)
        {
            throw new AccessControlException("Unable to get user profile",
                                             stme);
        }
    }


    /**
     * Set user role.
     *
     * @param rawRole CPD user role or null
     *
     * @throws AccessControlException If errors in setting role
     */
    public synchronized void setUserRole(final SecurityProperties secProps, final CommandUserRole rawRole)
        throws AccessControlException
    {
        final CommandUserRole role = (rawRole != null)
                                     ? rawRole
                                     : secProps.getDefaultRole();

        if (! _supportedRoles.contains(role))
        {
            final String message = "Role "                      +
                                   role                         +
                                   " is not supported in this " +
                                   "configuration";

            _log.error(message);

            throw new AccessControlException(message, true);
        }

        _role = role;

        _log.debug("User " + _user + " is a " + _role);
    }


    /**
     * Get user role.
     *
     * @return CPD user role
     */
    public synchronized CommandUserRole getUserRole()
    {
        return _role;
    }


    /**
     * Get user.
     *
     * @return User
     */
    public synchronized String getUser()
    {
        return _user;
    }


    /**
     * Return true if we are skipping authorization.
     *
     * @return Skip authorization state
     */
    public synchronized boolean skipAuthorization()
    {
        return _skipAuthorization;
    }

    
    /**
     * Set boolean for requesting token indefinitely
     *
     * @param run true if access control should check for token indefinitely
     */
    public synchronized void runContinuously(final boolean run)
    {
        this.runContinuously = run;
    }


    /**
     * Set shell.
     *
     * @param shell Shell to use for confirm dialog
     */
    public synchronized void setShell(final Shell shell)
    {
        _mainShell = shell;
    }


    /**
     * Get authorization decision for an action on a resource, given a token.
     *
     * @param rawActionId   Action id
     * @param rawResourceId Resource id
     *
     * @return True if authorized
     *
     * @throws AccessControlException If errors in getting authorization
     */
    public synchronized boolean requestAuthorizationDecision(
                                    final String rawActionId,
                                    final String rawResourceId)
        throws AccessControlException
    {
        if (_token == null)
        {
            throw new AccessControlException(
                          "Unable to get decision for null SSO token");
        }

        final String actionId = StringUtil.safeTrim(rawActionId);

        if (actionId.isEmpty())
        {
            throw new AccessControlException(
                          "Unable to get decision for empty action id");
        }

        final String resourceId = StringUtil.safeTrim(rawResourceId);

        if (resourceId.isEmpty())
        {
            throw new AccessControlException(
                          "Unable to get decision for empty resource id");
        }

        try
        {
            return _adm.requestAuthorizationDecision(_token,
                                                     actionId,
                                                     resourceId);
        }
        catch (final AuthorizationDecisionException ade)
        {
            throw new AccessControlException("Unable to get decision on '" +
                                                 actionId                  +
                                                 "/"                       +
                                                 resourceId                +
                                                 "'",
                                             ade);
        }
    }


    /**
     * Get authorization decision for an action on a resource, given a token.
     *
     * @param target Action and resource id
     *
     * @return True if authorized
     *
     * @throws AccessControlException If errors in getting authorization
     */
    public boolean requestAuthorizationDecision(
                       final AuthorizationTarget target)
        throws AccessControlException
    {
        if (target == null)
        {
            throw new AccessControlException(
                          "Unable to get decision for null target");
        }

        return requestAuthorizationDecision(target.getActionId(),
                                            target.getResourceId());
    }


    /**
     * Get authorization decisions for a list of actions on resources, given a
     * token.
     *
     * @param targets Action ids and resource ids
     *
     * @return True if all authorized
     *
     * @throws AccessControlException If errors in getting authorization
     */
    public synchronized boolean requestAuthorizationDecisions(
                                    final List<AuthorizationTarget> targets)
        throws AccessControlException
    {
        if (_token == null)
        {
            throw new AccessControlException(
                          "Unable to get decisions for null SSO token");
        }

        if (targets == null)
        {
            throw new AccessControlException(
                          "Unable to get decisions for null targets");
        }

        if (targets.isEmpty())
        {
            throw new AccessControlException(
                          "Unable to get decisions for empty targets");
        }

        try
        {
            return _adm.requestAuthorizationDecisions(_token, targets);
        }
        catch (final AuthorizationDecisionException ade)
        {
            throw new AccessControlException("Unable to get decisions on " +
                                                 targets,
                                             ade);
        }
    }


    /**
     * Revalidate token. Done as a unit to eliminate multiple prompts
     * if there are multiple clients. Synchronized so as to eliminate
     * multiple pop-ups.
     *
     * @return User
     *
     * @throws AccessControlException If errors in authorizing
     */
    public synchronized String revalidate()
        throws AccessControlException
    {

        synchronized (SINGLE_POPUP)
        {
            return innerRevalidate();
        }
    }


    /**
     * Validate token. Done as a unit to eliminate multiple prompts
     * if there are multiple clients.
     *
     * @return User
     *
     * @throws AccessControlException If errors in authorizing
     */
    @SuppressWarnings({"DM_EXIT"})
    private String innerRevalidate()
        throws AccessControlException
    {

        if (_skipAuthorization)
        {
            return _user;
        }

        while (true)
        {
            try
            {
                requestSsoToken();

                break;
            }
            catch (final AccessControlException ace)
            {
                final boolean serious = ace.getSerious();

                if (runContinuously) {
                    _log.error("Failure to revalidate but will " + "continue to request token indefinitely");

					SleepUtilities.checkedSleep(TOKEN_REQUEST_INTERVAL);
					
                	continue;
                }
                else if (serious)
                {
                	
                	if (! _gui)
                    {
                        _log.error("Failure to revalidate, serious " + "error, exiting");

                        System.exit(1);
                    }
                }
                else if (_loginMethod == LoginMethod.KEYTAB_FILE)
                {
                    if (! _gui)
                    {
                        _log.error("Failure to revalidate,keytab file is no longer valid, exiting");

                        System.exit(1);
                    }

                    // Failure to reauthenticate in GUI mode with a
                    // keytab file. Revert to prompting.

                    try
                    {
                        recreateAccessControl();

                        continue;
                    }
                    catch (final AccessControlException ace2)
                    {
                        _log.error("Failure to revalidate, keytab file is no longer valid, exiting");

                        System.exit(1);
                    }
                }

                if (_mainShell == null)
                {
                    _log.error("Failure to revalidate, exiting");

                    System.exit(1);
                }

                final ConfirmAbort popup = new ConfirmAbort(_mainShell,
                                                            serious);

                Display.getDefault().asyncExec(popup);

                long       delta   = EXEC_TIMEOUT;
                final long timeout = System.currentTimeMillis() + delta;

                while (! popup.getFinished() && (delta > 0L))
                {
                    SleepUtilities.checkedSleep(Math.min(delta, TENTH_SECOND));

                    delta = timeout - System.currentTimeMillis();
                }

                if (! popup.getFinished())
                {
                    throw new AccessControlException(
                                  "Unable to get user abort decision: " +
                                      "timeout",
                                  true);
                }

                final Exception error = popup.getError();

                if (error != null)
                {
                    throw new AccessControlException(
                                  "Unable to get user abort decision: " +
                                      error.getMessage(),
                                  error,
                                  true);
                }

                if (popup.getResult())
                {
                    _skipAuthorization = true;
                    break;
                }
            }
        }

        // User may have changed

        return _user;
    }


    /**
     * Recreate with GUI prompt.
     *
     * @throws AccessControlManagerException On failure to recreate
     */
    private void recreateAccessControl() throws AccessControlException
    {
        _loginMethod = LoginMethod.GUI_WINDOW;

        try
        {
            _acm.initialize(new String[] {LOGIN_METHOD_PARM + _loginMethod});
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to reinitialize access control manager: ", acme);

            throw new AccessControlException(
                          "Unable to reinitialize access control manager",
                          acme,
                          true);
        }

        try
        {
            _stm = _acm.getSsoTokenManager();
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to recreate SSO token manager: ", acme);

            throw new AccessControlException(
                          "Unable to recreate SSO token manager",
                          acme,
                          true);
        }

        if (USE_SHUTDOWN_HOOK)
        {
            _releaseToken.setTokenManager(_stm);
        }

        try
        {
            _adm = _acm.getAuthorizationDecisionManager();
        }
        catch (final AccessControlManagerException acme)
        {
            _log.error("Unable to recreate authorization decision manager: ", acme);

            throw new AccessControlException(
                          "Unable to recreate authorization decision manager",
                          acme,
                          true);
        }

        try
        {
            _adm.initialize(_acm);
        }
        catch (final AuthorizationDecisionException ade)
        {
            _log.error("Unable to reinitialize authorization decision manager: ", ade);

            throw new AccessControlException(
                          "Unable to reinitialize authorization decision " +
                              "manager",
                          ade,
                          true);
        }
    }


    /**
     * Convert to security's enum.
     *
     * @param le Our version
     *
     * @return Their version
     */
    private static LoginMethod convert(final LoginEnum le)
    {
        if (le == null)
        {
            return null;
        }

        switch (le)
        {
            case GUI_WINDOW:
                return LoginMethod.GUI_WINDOW;

            case KEYTAB_FILE:
                return LoginMethod.KEYTAB_FILE;

            case TEXT_PROMPT:
                return LoginMethod.TEXT_PROMPT;
            
            case KERBEROS:
            	return LoginMethod.KERBEROS;
            	
            case SECURID_GUI:
            	return LoginMethod.SECURID_GUI;
            case SECURID_CLI:
            	return LoginMethod.SECURID_CLI;

            default:
                break;
        }

        return null;
    }


    /**
     * Get perspective exit status, if any.
     *
     * @return True if perspective says to exit
     */
    private boolean getPerspectiveExit()
    {

        return ((_application != null) &&
                _application.getExit());
    }


    /**
     * Get release token functor.
     *
     * @return Functor
     */
    public IShutdown getReleaseTokenFunctor()
    {

        return (USE_NEW_SHUTDOWN ? _releaseToken : null);
    }


    /**
     * Isolate Swing calls that prompt in a separate thread.
     */
    private class SsoTokenRunnable extends Object implements Runnable
    {
        private final String                                    _user;
        private final LoginMethod                               _loginMethod;
        private final SsoTokenManager                           _stm;
        private final AtomicReference<SsoToken>                 _result;
        private final AtomicReference<SsoTokenManagerException> _error;
        private final AtomicReference<Exception>                _unknown;
        private final AtomicBoolean                             _invoked  =
            new AtomicBoolean(false);
        private final AtomicBoolean                             _finished =
            new AtomicBoolean(false);


        /**
         * Constructor.
         *
         * @param user    User
         * @param lm      Login method
         * @param stm     Token manager
         * @param result  Where to place token
         * @param error   Where to place SSO error
         * @param unknown Where to place other error
         */
        public SsoTokenRunnable(
                   final String                                    user,
                   final LoginMethod                               lm,
                   final SsoTokenManager                           stm,
                   final AtomicReference<SsoToken>                 result,
                   final AtomicReference<SsoTokenManagerException> error,
                   final AtomicReference<Exception>                unknown)
        {
            super();

            _user        = user;
            _loginMethod = lm;
            _stm         = stm;
            _result      = result;
            _error       = error;
            _unknown     = unknown;

            _result.set(null);
            _error.set(null);
            _unknown.set(null);
        }


        /**
         * Run method of thread. Creates token and stashes it in result.
         * Wraps call to actual run method.
         */
        @Override
        public void run()
        {
            _invoked.set(true);

            try
            {
                internalRun();
            }
            catch (final RuntimeException rte)
            {
                _unknown.set(rte);

                throw rte;
            }
            catch (final Exception e)
            {
                _unknown.set(e);
            }
            finally
            {
                _finished.set(true);
            }
        }


        /**
         * Actual run method of thread. Creates token and stashes it in result.
         */
        private void internalRun()
        {
            final String[]           args  = new String[0];
            SsoToken                 token = null;
            SsoTokenManagerException error = null;

            for (int i = 0; i < RETRY_COUNT; ++i)
            {
               /** Bail if we are exiting */
               if (getPerspectiveExit())
               {
                   return;
               }

               try
                {
                    token = _stm.requestSsoToken(args);
                    error = null;
                    break;
                }
                catch (final SsoTokenManagerException stme)
                {
                    /**
                     * Bail if we are exiting
                     * we don't care that authentication failed.
                     */
                    if (getPerspectiveExit())
                    {
                       return;
                    }

                    final SsoTokenManagerErrorCode ec =
                        stme.getErrorCode();

                    _log.warn("Unable to get SSO token: " + ec);

                    token = null;
                    error = stme;

                    if (_loginMethod == LoginMethod.KEYTAB_FILE)
                    {
                        // No point in retrying
                        break;
                    }

                    if ((ec != SsoTokenManagerErrorCode.INVALID_CREDENTIAL) &&
                        (ec != SsoTokenManagerErrorCode.NULL_VALUE))
                    {
                        // No point in retrying
                        break;
                    }
                }
            }

            _result.set(token);
            _error.set(error);
        }


        /**
         * Return true if thread has been started.
         *
         * @return Run status
         */
        public boolean getInvoked()
        {
            return _invoked.get();
        }


        /**
         * Return true if thread has been run and has finished.
         *
         * @return Run status
         */
        public boolean getFinished()
        {
            return _finished.get();
        }
    }


    /**
     * Call SWT in a separate thread.
     */
    private static class ConfirmAbort extends Object implements Runnable
    {
        private final Shell                      _shell;
        private final AtomicReference<Exception> _error    =
            new AtomicReference<Exception>(null);
        private final AtomicBoolean              _result   =
            new AtomicBoolean(false);
        private final AtomicBoolean              _finished =
            new AtomicBoolean(false);
        private final String                     _extra;


        /**
         * Constructor.
         *
         * @param shell   Main shell
         * @param serious True if a serious error
         */
        public ConfirmAbort(final Shell   shell,
                            final boolean serious)
        {
            super();

            _shell = shell;
            _extra = (serious ? "Serious error" : "Failure to login");
        }


        /**
         * Run method of thread. Calls pop-up and stashes result.
         * Wraps call to actual run method.
         */
        @Override
        public void run()
        {
            try
            {
                internalRun();
            }
            catch (final RuntimeException rte)
            {
                _error.set(rte);

                throw rte;
            }
            catch (final Exception e)
            {
                _error.set(e);
            }
            finally
            {
                _finished.set(true);
            }
        }


        /**
         * Actual run method of thread.
         */
        private void internalRun()
        {
            final boolean abort = SWTUtilities.showConfirmDialog(
                                      _shell,
                                      "Abort Login Confirmation",
                                      _extra                           +
                                          ". Do you want to continue " +
                                          "without authorization? "    +
                                          "No means keep trying.");
            _result.set(abort);
        }


        /**
         * Return result of pop-up.
         *
         * @return Result
         */
        public boolean getResult()
        {
            return _result.get();
        }


        /**
         * Return exception from pop-up.
         *
         * @return Exception
         */
        public Exception getError()
        {
            return _error.get();
        }


        /**
         * Return true if thread has been run and has finished.
         *
         * @return Run status
         */
        public boolean getFinished()
        {
            return _finished.get();
        }
    }


    /** Used for shutdown hook to release token */
    private static class ReleaseToken extends Object
        implements IShutdown, Runnable
    {

        private final Tracer _log;

        private SsoTokenManager  _stm   = null;
        private SsoToken         _token = null;


        /**
         * Constructor.
         *
         * @param log Tracer (may be null)
         */
        public ReleaseToken(final Tracer log)
        {
            super();

            _log = log;
        }


        /**
         * Reset token manager.
         *
         * @param stm Token manager
         */
        public synchronized void setTokenManager(final SsoTokenManager stm)
        {
            _stm = stm;
        }


        /**
         * Reset token.
         *
         * @param token Token
         */
        public synchronized void setToken(final SsoToken token)
        {
            _token = token;
        }


        /**
         * Method to perform release of token. Since this is used in a
         * shutdown hook, don't throw, no matter what. May log.
         * If we do not release the token, that is not a disaster.
         *
         * OK, we do throw for runtime exceptions.
         *
         * @param doLog If true, log result
         */
        @Override
        @SuppressWarnings("PMD.AvoidRethrowingException")
        public void runShutdown(final boolean doLog)
        {
            SsoTokenManager stm   = null;
            SsoToken        token = null;

            synchronized(this)
            {
                stm   = _stm;
                token = _token;
            }

            if ((stm == null) || (token == null))
            {
                return;
            }

            try
            {
                stm.invalidateSsoToken(token);
            }
            catch (final RuntimeException rte)
            {
                if (doLog && (_log != null))
                {
                    _log.debug("Could not invalidate SSO token: ", rte);
                }

                throw rte;
            }
            catch (final Exception e)
            {
                if (doLog && (_log != null))
                {
                    _log.debug("Could not invalidate SSO token: ", e);
                }

                return;
            }

            if (doLog && (_log != null))
            {
                _log.debug("Invalidated SSO token");
            }
        }


        /**
         * Method to perform release of token. Since this is used in a
         * shutdown hook, don't log or throw, no matter what. If we do not
         * release the token, that is not a disaster.
         */
        @Override
        public void run()
        {
            runShutdown(false);
        }
    }
}
