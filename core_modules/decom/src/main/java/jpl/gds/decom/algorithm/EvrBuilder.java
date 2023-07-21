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
package jpl.gds.decom.algorithm;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * Builder object used to hold data needed to recreate an EVR
 * in a generic fashion.
 *
 */
public class EvrBuilder {

	private final List<Object> argumentList = new ArrayList<>();
	private ISclk sclk;
	private long eventId;

	/**
	 * Create a new EVR builder instance.
	 */
	public EvrBuilder() {
		sclk = new Sclk(0, 0);
	}
	/**
	 * Set the SCLK on this builder.
	 * @param sclk spacecraft time of the resulting EVR
	 * @return this builder
	 */
    public EvrBuilder setSclk(final ISclk sclk) {
		this.sclk = sclk;
		return this;
	}
	
	/**
	 * @return the SCLK of this EVR
	 */
    public ISclk getSclk() {
		return sclk;
	}

	/**
	 * Add an EVR argument to the builder.  This method is
	 * sensitive to ordering - arguments need to be added
	 * in the order they appear in the EVR message string in the
	 * dictionary.
	 * @param value the argument value
	 * @return this builder
	 */
	public EvrBuilder addArgument(final Object value) {
		argumentList.add(value);
		return this;
	}
	
	/**
	 * @return the ordered list of EVR arguments 
	 */
	public List<Object> getArguments() {
		return argumentList;
	}
	
	/**
	 * Set the event ID for this object. This should match
	 * an entry in the EVR dictionary.
	 * @param eventId numeric event identifier
	 * @return this builder
	 */
	public EvrBuilder setEventId(final long eventId) {
		this.eventId = eventId;
		return this;
	}
	
	/**
	 * @return the event ID
	 */
	public long getEventId() {
		return eventId;
	}
	
}
