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
package jpl.gds.dictionary.api.eu;

/**
 * EUGenerationException is thrown by DN to EU conversions.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * This exception is the only exception that should be thrown by DN to EU conversions
 * when an error occurs during the conversion or creation of the objects that
 * performs conversion.
 * <p>
 */
@SuppressWarnings("serial")
public class EUGenerationException extends Exception {
	
    /**
     * ID of the channel associated with this conversion error.
     */
	private String channelId;
	
	/**
	 * Creates an EUGenerationException with the given message.
	 * @param message the detail message
	 */
	public EUGenerationException(final String message) {
		super(message);
	}
	
	/**
	 * Creates an EUGenerationException with the given message and root throwable.
	 * @param message the detail message
	 * @param t the throwable that triggered this exception
	 */
	public EUGenerationException(final String message, final Throwable t) {
		super(message, t);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Throwable#toString()
	 */
	@Override
    public String toString() {
		if (channelId == null) {
			return getMessage();
		} else {
			return getMessage() + " (for channel ID " + channelId + ")";
		}
	}

	/**
	 * Gets the channel ID, if any, associated with this exception
	 * @return channel ID 
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Sets the channel ID, if any, associated with this exception
	 * @param channelId the channel ID to set 
	 */
	public void setChannelId(final String channelId) {
		this.channelId = channelId;
	}
}
