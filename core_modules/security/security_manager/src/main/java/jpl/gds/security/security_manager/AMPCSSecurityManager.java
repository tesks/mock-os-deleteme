/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.security.security_manager;

import java.security.Permission;
import java.security.Policy;

/**
 * This security manager instantiates and sets its own security policy. The
 * security manager is set on the command line, but our security policy is
 * implemented in code, which (as of this writing) is unable to be referenced on
 * the command line.
 * 
 *
 * @see SandboxSecurityPolicy
 */
public class AMPCSSecurityManager extends SecurityManager {
    static {
        Policy.setPolicy(new SandboxSecurityPolicy());
    }

    @Override
    public void checkPermission(final Permission perm) {
        // we need to call super version, otherwise SandboxSecurityPolicy.getPermissions() will never be called
        super.checkPermission(perm);
    }

}
