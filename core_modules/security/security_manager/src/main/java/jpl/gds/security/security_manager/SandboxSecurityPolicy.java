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

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

import jpl.gds.security.loader.AmpcsUriPluginClassLoader;

/**
 * Security policy to return no permissions for sandboxed code (loaded with the
 * CustomURLClassLoader), and all permissions for all other code.
 * 
 * Adapted from
 * https://blog.jayway.com/2014/06/13/sandboxing-plugins-in-java/ 
 * 
 *
 */
public class SandboxSecurityPolicy extends Policy {
    
    /**
     * ALL_PERMS has all permissions assigned.
     */
    private static final Permissions ALL_PERMS = new Permissions();
    /**
     * NO_PERMS has no permissions assigned.
     */
    private static final Permissions NO_PERMS = new Permissions();
    
    static {
        // statically add the AllPermission permission to ALL_PERMS
        ALL_PERMS.add(new AllPermission());
    }
    
    @Override
    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        // check for restrictions and return the correct permissions
        if (isRestricted(domain)) {
            return pluginPermissions();
        } else {
            return applicationPermissions();
        }
    }

    private boolean isRestricted(final ProtectionDomain domain) {
        // check to see if the domain's classloader is restricted (i.e., an
        // instance of AmpcsUriPluginClassLoader)
        return domain.getClassLoader() instanceof AmpcsUriPluginClassLoader;
    }

    private PermissionCollection pluginPermissions() {
        return NO_PERMS;
    }

    private PermissionCollection applicationPermissions() {
        return ALL_PERMS;
    }
}
