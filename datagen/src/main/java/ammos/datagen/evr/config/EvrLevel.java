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
package ammos.datagen.evr.config;

/**
 * This object tracks information loaded from the EVR mission
 * configuration regarding an individual EVR level. Typical
 * level names are "COMMAND," and "ACTIVITY".
 * 
 *
 */
public class EvrLevel {
	
	private final String levelName;
	private final int levelApid;
	private final boolean fatal;
	
	/**
	 * Constructor.
	 * 
	 * @param name the level name
	 * @param num the packet APID number for packets that contain EVRs of this level
	 * @param fatal true if this EVR level is considered a FATAL error, false if not
	 */
	public EvrLevel(final String name, final int num, final boolean fatal) {
		this.levelName = name;
		this.levelApid = num;
		this.fatal = fatal;
	}
	
	/**
	 * Gets the name of this EVR level.
	 * 
	 * @return the EVR level name
	 */
	public String getLevelName() {
		
		return this.levelName;
	}
	
	/**
	 * Gets the packet APID (application process identifier) associated with this 
	 * EVR level.
	 * 
	 * @return the EVR level APID
	 */
	public int getLevelApid() {
		
		return this.levelApid;
	}
	
	/**
	 * Indicates that this EVR level represents a FATAL event in the
	 * flight system.
	 * 
	 * @return true if fatal level, false if not
	 */
	public boolean isFatal() {
		
		return this.fatal;
	}
}