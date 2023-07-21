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
package jpl.gds.shared.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

import jpl.gds.shared.holders.PortHolder;

/**
 * A utility class for use with host names and network port numbers.
 * 
 *
 * @since R8
 */
public class HostPortUtility {

    /**
     * Undefined port number.
     */
    public static final int UNDEFINED_PORT = -1;
    /**
     * The maximum allowable value for a port number... there is no Java
     * constant for this.
     */
    public static final int MAX_PORT_NUMBER = PortHolder.MAX_VALUE;
    /**
     * Constant variable for local host name.
     */
    public static final String LOCALHOST = "localhost";
    
    /**
     *  Cached copy of localHostname to reduce expensive lookups
     */
    public static String localHostName;

    private static final int MIN_REST_PORT = 8000;
    private static final int MAX_REST_PORT = 9000;

 
    /**
     * Indicates whether the given port number is a valid port number.
     * 
     * @param port a port number
     * @return true if the port number is valid (in range) and false if not
     */
    public static boolean isPortValid(final int port) {
    	return !(port < 0 || port == UNDEFINED_PORT || port > MAX_PORT_NUMBER);
    }

    /**
     * Gets the local IP host name, before the first dot only. Used when no
     * other host name is supplied.
     * @return the local host name
     */
    public static String getLocalHostName() {
        /**
         *  Cached copy of localHostname to reduce expensive lookups
         */
        if (localHostName == null) {
            String hostName = null;
            try {
                final InetAddress hostInet = InetAddress.getLocalHost();
                hostName = hostInet.getHostName();
                final int index = hostName.indexOf('.');
                if (index != -1) {
                    hostName = hostName.substring(0, index);
                }
            } catch (final UnknownHostException e) {
                hostName = LOCALHOST;
            }
            localHostName = hostName;
        }

        return (localHostName);
    }

    /**
     * Clean host name and remove localhost if present. Note that if there
     * is a problem getting the real host name, localhost will be used as a
     * last resort.
     *
     * @param hostName Original host name
     *
     * @return Cleansed host name
     */
    public static String cleanHostName(final String hostName)
    {

        String useName = hostName == null || hostName.isEmpty() ? null : hostName;
    
        if ((useName == null) || LOCALHOST.equalsIgnoreCase(useName))
        {
            useName = getLocalHostName();
        }
    
        return useName;
    }

    /**
     * Get random port for REST server; check if not used
     * @return Random port
     *
     */
    public static int getRandomRestPort(){
        int port = ThreadLocalRandom.current().nextInt(MIN_REST_PORT, MAX_REST_PORT + 1);
        if (portIsAvailable(port)){
            return port;
        }
        return getRandomRestPort();
    }

    /**
     * Check to see if a port is available.
     *
     * @param port the port to check for availability.
     *
     */
    public static boolean portIsAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port); DatagramSocket ds = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
