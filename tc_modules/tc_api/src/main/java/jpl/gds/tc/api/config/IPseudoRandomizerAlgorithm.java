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
package jpl.gds.tc.api.config;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * This is the interface that must be implemented by a class that can be configured
 * to be used as a telecommand pseudo randomization class.
 * 
 * It should be noted that this class only operates on byte arrays, it does not operate
 * at the bit level.  This is because Telecommand frames always contain an integral number
 * of bytes and telecommand pseudo randomization is always applied at the telecommand frame
 * level (technically the CLTU codeblock data level...which is telecommand frame and fill data)
 * 
 */
@CustomerAccessible(immutable = true)
public interface IPseudoRandomizerAlgorithm
{
	/**
	 * Randomize the input bytes
	 * 
	 * @param inputBytes The bytes to randomize
	 * 
	 * @return The randomized version of the input bytes
	 */
	public abstract byte[] randomize(final byte[] inputBytes);
	
	/**
	 * Randomize the input bytes from the offset to the end of the array
	 * 
	 * @param inputBytes The input byte array containing the bytes to randomize
	 * @param offset The offset into the array where randomization should begin
	 * 
	 * @return The randomized version of the input bytes starting from the offset and going to
	 * the end of the input array
	 */
	public abstract byte[] randomize(final byte[] inputBytes, final int offset);
	
	/**
	 * Randomize "length" input bytes starting at the offset 
	 * 
	 * @param inputBytes The input byte array containing the bytes to randomize
	 * @param offset The offset into the array where randomization should begin
	 * @param length The number of bytes to randomize
	 * 
	 * @return The randomized version of the input bytes starting from the offset and going 
	 * for "length" bytes
	 */
	public abstract byte[] randomize(final byte[] inputBytes, final int offset, final int length);
	
	/**
	 * Derandomize the input bytes
	 * 
	 * @param inputBytes The bytes to derandomize
	 * 
	 * @return The derandomized version of the input bytes
	 */
	public abstract byte[] derandomize(final byte[] inputBytes);
	
	/**
	 * Derandomize the input bytes from the offset to the end of the array
	 * 
	 * @param inputBytes The input byte array containing the bytes to derandomize
	 * @param offset The offset into the array where randomization should begin
	 * 
	 * @return The derandomized version of the input bytes starting from the offset and going to
	 * the end of the input array
	 */
	public abstract byte[] derandomize(final byte[] inputBytes, final int offset);
	
	/**
	 * Derandomize "length" input bytes starting at the offset 
	 * 
	 * @param inputBytes The input byte array containing the bytes to derandomize
	 * @param offset The offset into the array where randomization should begin
	 * @param length The number of bytes to derandomize
	 * 
	 * @return The derandomized version of the input bytes starting from the offset and going 
	 * for "length" bytes
	 */
	public abstract byte[] derandomize(final byte[] inputBytes, final int offset, final int length);
}