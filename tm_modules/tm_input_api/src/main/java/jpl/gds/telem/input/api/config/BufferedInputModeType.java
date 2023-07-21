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
package jpl.gds.telem.input.api.config;

/**
 * 
 * This class enumerates all the bufferedInputMode types that are allowed in session
 * configurations. The bufferedInputMode specifically defines which downlink modes can
 * use the BufferedRawInputStream when a supported input connection is utilized.
 * NONE, FSW, SSE, or BOTH are allowable modes.
 * Currently, only ClientSocketInputConnection, ServerSocketInputConnection, and TdsInputConnection
 * support BufferedRawInputStream
 * 
 */
public enum BufferedInputModeType {
	/**
	 * When neither downlink type can use BufferedRawInputStream
	 */
	NONE,
	
	/**
	 * FSW chill_down can only use BufferedRawInputStream 
	 */
	FSW,
	
	/**
	 * SSE chill_down can use BufferedRawInputStream
	 */
	SSE,
	
	/**
	 * Both FSW and SSE chill_down can use BufferedRawInputStream
	 */
	BOTH;
	
	/**
	 * Return true/false based upon whether or not the BufferedRawInputStream can be utilized or not
	 *  
	 * @param isSse - true if downlink is SSE (value from GdsSystemProperties.applicationIsSse())
	 * 
	 * @return - True if the buffer can be used, false if not.
	 */
	public boolean isAllowed(boolean isSse){
		boolean retVal = false;
		if(this.equals(BOTH)){
			retVal = true;
		}
		else if(isSse && this.equals(SSE)){
			retVal = true;
		}
		else if(!isSse && this.equals(FSW)){
			retVal = true;
		}
		return retVal;
	}
}