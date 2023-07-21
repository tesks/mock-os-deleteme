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
package jpl.gds.watcher.app.handler;

import java.util.Collection;

import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.shared.message.IMessageType;

/**
 * The ICaptureHandler interface is used for interfacing the capture handler
 * with the capture app
 */
public interface ICaptureHandler extends IMessageServiceListener {
	
    /**
     * Sets the output filename for writing messages and the write mode to
     * WRITE_FILE.
     * 
     * @param filename
     *            the name of the file
     * @return true if fileName is valid; false otherwise
     */
	public boolean setDataOutputFile(final String filename);
	
	/**
     * Sets the output filename for writing messages and the write mode to
     * WRITE_FILE.
     * 
     * @param filename
     *            the name of the file
     * @return true if fileName is valid; false otherwise
     */
	public boolean setMetadataOutputFile(final String filename);
	
	/**
	 * Set the output stream for forwarding all captured messages
	 * @param host the hostname of the server where messages will be forwarded
	 * @param port the port of the server where messages will be forwarded
	 */
	public void setForwardingStream(final String host,final int port);
	
	/**
	 * Set the bytes to be sent on the forwarding stream between each message
	 * @param syncMarker the bytes to be sent between messages
	 */
	public void setSyncMarker(final byte[] syncMarker);
	
	/**
     * Set the bytes to be sent on the forwarding stream between each message
     * @param syncMarker the bytes to be sent between messages as a String
     */
	public void setSyncMarker(final String syncMarker);
	
	/**
    * Sets desired message types (message filter) from a comma separated string
    * of types. This list will be used to filter messages for the given
    * subscriber.
    * 
    * @param types the list of message types to capture; may be null to capture everything
    */
	public void setCaptureMessageFilter(final Collection<IMessageType> types);
	
    /**
     * Set the name of the style to be used for displaying the captured metadata
     * 
     * @param dbTableName
     *            The name of the database table where the query is being done
     * 
     * @param defStyle
     *            the name of the style to be used
     */
	public void setCaptureMessageStyle(final String dbTableName, final String defStyle);
	
	/**
	 * Set to store captured messages, in Protobuf format,to the specified data
	 * file instead of data only.
	 * 
	 * @param captureMessages
	 *            TRUE if messages are to be captured, FALSE otherwise.
	 */
	public void setCaptureMessages(final boolean captureMessages);
	
	/**
	 * Get if Protobuf formatted messages are being store in the specified data
	 * file instead of data only.
	 * 
	 * @return TRUE if messages are being stored, FALSE otherwise
	 */
	public boolean isCaptureMessages();
	
	/**
     * Stops the message capture.
     */
    public void shutdown();
	
}
