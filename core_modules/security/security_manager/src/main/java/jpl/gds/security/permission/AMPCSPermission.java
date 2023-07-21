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
package jpl.gds.security.permission;

import java.security.BasicPermission;

/**
 * Basic permission for AMPCS classes, intended for use with the @AMPCSLocked
 * annotations.
 * 
 *
 * @see jpl.gds.shared.annotation.AMPCSLocked
 *
 */
public class AMPCSPermission extends BasicPermission {

    /**
     * 
     */
    private static final long serialVersionUID = -6116123792484646543L;

    /**
     * @param name
     */
    public AMPCSPermission(String name) {
        super(name);
    }

    /**
     * @param name
     * @param actions
     */
    public AMPCSPermission(String name, String actions) {
        super(name, actions);
    }
}
