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
package jpl.gds.shared.time;

import java.math.BigInteger;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import jpl.gds.serialization.primitives.time.Proto3SclkEncoding;

/**
 * This class holds the parameters for encoding a coarse fine time
 * as a series of bytes.
 * 
 * Must be immutable.
 */
public class CoarseFineEncoding {
	
	private final int coarseBits;
	private final int fineBits;
	private final long maxCoarse;
	private final long maxFine;
	private final int coarseBytes;
	private final int fineBytes;
	
	/**
	 * Construct a coarse-fine encoding using powers of 2
	 * for maximum segment values.
	 * @param coarseBits the number of bits allowed for the coarse field
	 * @param fineBits the number of bits allowed for the fine field
	 */
	public CoarseFineEncoding(int coarseBits, int fineBits) {
		this.coarseBits = coarseBits;
		this.fineBits = fineBits;
		coarseBytes = (int) Math.ceil(coarseBits / 8.0);
		fineBytes = (int) Math.ceil(fineBits / 8.0);
		this.maxCoarse = (long) (Math.pow(2, coarseBits) - 1);
		this.maxFine = (long) (Math.pow(2, fineBits) - 1);
	}

	/**
	 * Construct a coarse-fine encoding, specifying a custom
	 * maximum fine value.
	 * @param coarseBits the number of bits allowed for the coarse field
	 * @param fineBits the number of bits allowed for the fine field
	 * @param maxFine the maximum value of fine ticks allowed in the fine field
	 */
	public CoarseFineEncoding(int coarseBits, int fineBits, int maxFine) {
		this.coarseBits = coarseBits;
		this.fineBits = fineBits;
		coarseBytes = (int) Math.ceil(coarseBits / 8.0);
		fineBytes = (int) Math.ceil(fineBits / 8.0);
		this.maxFine = Integer.toUnsignedLong(maxFine);
		this.maxCoarse = (long) (Math.pow(2, coarseBits) - 1);
	}
	
	/**
	 * Construct a coarse-fine encoding, specifying a custom
	 * maximum fine value.
	 * @param coarseBits the number of bits allowed for the coarse field
	 * @param fineBits the number of bits allowed for the fine field
	 * @param fineLimit the maximum value of fine ticks allowed in the fine field
	 */
	public CoarseFineEncoding(int coarseBits, int fineBits, long fineLimit) {
		this.coarseBits = coarseBits;
		this.fineBits = fineBits;
		coarseBytes = (int) Math.ceil(coarseBits / 8.0);
		fineBytes = (int) Math.ceil(fineBits / 8.0);
		this.maxFine = fineLimit;
		this.maxCoarse = (long) (Math.pow(2, coarseBits) - 1);
	}

	/**
	 * Construct a coarse-fine encoding by copying from another one.
	 * 
	 * @param other the object to copy data from
	 * the number of bits allowed for the coarse field
	 * @param maxFine the number of bits allowed for the fine field
	 */
	public CoarseFineEncoding(CoarseFineEncoding other, int maxFine) {
		this.coarseBits = other.getCoarseBits();
		this.fineBits = other.getFineBits();
		coarseBytes = other.getCoarseByteLength();
		fineBytes = other.fineBytes;
		this.maxCoarse = other.maxCoarse;
		this.maxFine = maxFine;
	}
	
	/**
	 * Construct a coarse-fine encoding from a protobuf message
	 * @param msg a protobuf message detailing coarse-fine encoding values
	 */
	public CoarseFineEncoding(Proto3SclkEncoding msg){
	    this.coarseBits = msg.getCoarseBits();
	    this.fineBits = msg.getFineBits();
	    this.maxFine = msg.getMaxFine();
	    this.maxCoarse = (long) (Math.pow(2, coarseBits) - 1);
	    
	    coarseBytes = (int) Math.ceil(coarseBits / 8.0);
        fineBytes = (int) Math.ceil(fineBits / 8.0);
	}

	/**
	 * Gets the number of coarse bits.
	 * 
	 * @return bit count
	 */
	public int getCoarseBits() {
		return coarseBits;
	}
	
	/**
	 * Gets the number of fine bits.
	 * 
	 * @return bit count
	 */
	public int getFineBits() {
		return fineBits;
	}
	
	/**
	 * @return the maximum allowed coarse value
	 */
	public long getMaxCoarse() {
		return maxCoarse;
	}

	/**
	 * @return the maximum allowed fine value.
	 */
	public long getMaxFine() {
		return maxFine;
	}
	
	/**
	 * Converts a number of fine ticks from one modulus to another.  Operations
	 * do not round; when changing modulus, the resulting fine should not cause an event
	 * measure on one precision to move past in a timeline as compared to an event
	 * on another timeline.
	 * @param fineTicks the number of fine ticks to be converted
	 * @param sourceUpperFine the maximum fine value of the source time system
	 * @param targetUpperFine the maximum fine value of the destination time system
	 * @return the number of fine ticks converted to a new time system
	 */
	public static long normalizeFine (long fineTicks, long sourceUpperFine, long targetUpperFine) {
		BigInteger n = BigInteger.valueOf(fineTicks).multiply(BigInteger.valueOf(targetUpperFine + 1));
		return n.divide(BigInteger.valueOf(sourceUpperFine + 1)).longValueExact();
	}

	/**
	 * Get the total bit length used by this encoding.
	 * @return the number of bits
	 */
	public int getBitLength() {
		return coarseBits + fineBits;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(coarseBits)
			.append(fineBits)
			.append(maxFine)
			.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CoarseFineEncoding)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		CoarseFineEncoding other = (CoarseFineEncoding) obj;
		return new EqualsBuilder()
			.append(coarseBits, other.getCoarseBits())
			.append(fineBits, other.getFineBits())
			.append(maxFine, other.getMaxFine())
			.isEquals();
	}

	/** 
	 * Get the number of bytes
	 * needed to fully encode a coarse time segment
	 * with respect to this encoding.
	 * @return number of bytes
	 */
	public int getCoarseByteLength() {
		return coarseBytes;
	}

	/** 
	 * Get the number of bytes
	 * needed to fully encode a fine time segment
	 * with respect to this encoding.
	 * @return number of bytes
	 */
	public int getFineByteLength() {
		return fineBytes;
	}

	/** 
	 * Get the number of bytes
	 * needed to fully encode a coarse fine time
	 * with respect to this encoding.
	 * @return number of bytes
	 */
	public int getByteLength() {
		return coarseBytes + fineBytes;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Coarse bits: " + coarseBits + " ")
			.append("Fine Bits: " + fineBits)
			.append("Max fine: " + maxFine);
		return builder.toString();
	}
}
