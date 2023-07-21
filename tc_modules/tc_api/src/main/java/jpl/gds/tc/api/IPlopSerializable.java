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
package jpl.gds.tc.api;

/**
 * An interface implemented by all objects that can be serialized into binary
 * and transferred across any type of interface to a spacecraft.
 * 
 *
 */
public interface IPlopSerializable
{
	/**
	 * Get a binary representation of this object in a form that can be
	 * interpreted by a receiving spacecraft.
	 * 
	 * @return A byte array representation of the current object.
	 */
	public abstract byte[] getPlopBytes();
}
