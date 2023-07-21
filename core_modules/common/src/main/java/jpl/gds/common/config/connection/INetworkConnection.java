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
package jpl.gds.common.config.connection;

/**
 * An interface to be implemented by connection configurations that use
 * a network connection.
 * 
 * @since R8
 */
public interface INetworkConnection extends IConnection {

	/**
	 * Sets the host name for the connection.
	 * 
	 * @param host host name to set
	 */
	public void setHost(String host);

	/**
	 * Gets the host name for the connection.
	 * 
	 * @return host name
	 */
	public String getHost();

	/**
	 * Sets the port number for the connection.
	 * 
	 * @param port port number to set
	 */
	public void setPort(int port);

	/**
	 * Gets the port number for the connection.
	 * 
	 * @return port number
	 */
	public int getPort();
	
}
