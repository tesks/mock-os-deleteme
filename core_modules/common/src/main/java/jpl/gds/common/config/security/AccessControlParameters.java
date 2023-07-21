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

import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;

/**
 * A container object for access control parameters, i.e., values needed
 * to authenticate a user or authorize an action.
 * 
 *
 */
public class AccessControlParameters {
    
    private String userId = null;
    private CommandUserRole userRole = null;
    private LoginEnum loginMethod = null;
    private String keytabFile = null;

   
    /**
     * Set the user ID of the user logged in for this session. This is used for
     * uplink in the ICMD configuration
     * 
     * @return User ID of the user logged in
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * Get the user ID of the user logged in for this session. This is used for
     * uplink in the ICMD configuration
     *
     * @param userId User ID of the user logged in
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * Get the role of the user logged in for this session
     * @return the user role of the user logged in for this session
     */
    public CommandUserRole getUserRole() {
        return userRole;
    }

    /**
     * Set the role of the user logged in for this session
     * @param userRole
     *            the user role of the user logged in for this session
     */
    public void setUserRole(CommandUserRole userRole) {
        this.userRole = userRole;
    }


    /**
     * Get login method.
     *
     * @return Login method
     */
    public LoginEnum getLoginMethod()
    {
        return loginMethod;
    }


    /**
     * Set login method.
     *
     * @param lm Login method
     */
    public void setLoginMethod(final LoginEnum lm)
    {
        loginMethod = lm;
    }


    /**
     * Get keytab file.
     *
     * @return Keytab file
     */
    public String getKeytabFile()
    {
        return keytabFile;
    }


    /**
     * Set keytab file.
     *
     * @param keytab Keytab file
     */
    public void setKeytabFile(final String keytab)
    {
        keytabFile = keytab;
    }


    /**
     * Copies members from the supplied AccessControlParameters object to this one.
     * 
     * @param toCopy object to copy data from
     */
    public void copyValuesFrom(AccessControlParameters toCopy) {
    	this.keytabFile = toCopy.getKeytabFile();
    	this.loginMethod = toCopy.getLoginMethod();
    	this.userId = toCopy.getUserId();
    	this.userRole = toCopy.getUserRole();
    }

}
