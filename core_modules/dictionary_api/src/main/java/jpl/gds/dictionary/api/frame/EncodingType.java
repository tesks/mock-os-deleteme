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
package jpl.gds.dictionary.api.frame;

/**
 * The EncodingType enumeration defines the possible encoding types attached to
 * an ITransferFrameDefinition, indicating whether the frame type is an
 * un-encoded, Reed-Solomon, or Turbo (etc) encoded frame. In a sense, this
 * actually combines encoding type and error-checking strategy for the frame,
 * since Reed-Solomon, for instance, so not actually describe an
 * encoding, but an error correction strategy.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 *
 * @see ITransferFrameDefinition
 */
public enum EncodingType {
	/**
	 * Frame Dictionary Encoding Type: TURBO_1_2 (2)
	 */
	TURBO_1_2 (2),
	/**
     * Frame Dictionary Encoding Type: TURBO_1_3 (3)
     */
	TURBO_1_3 (3),

	/**
     * Frame Dictionary Encoding Type: TURBO_1_4 (4)
     */
    TURBO_1_4 (4),
    /**
     * Frame Dictionary Encoding Type: TURBO_1_6 (6)
     */
	TURBO_1_6 (6),
	

    /**
     * Frame Dictionary Encoding Type: REED_SOLOMON
     */
	REED_SOLOMON,
	/**
	 * Frame Dictionary Encoding Type: UNENCODED
	 */
    UNENCODED,
    /**
     * Frame Dictionary Encoding Type: BYPASS
     */
    BYPASS,
    /**
     * Frame Dictionary Encoding Type: ANY_TURBO
     */
    ANY_TURBO;
	
	private int turboRate;
	
	/**
	 * Non-turbo or any-turbo constructor.
	 */
	private EncodingType() {
		this.turboRate = 0;
	}
	
	/**
	 * Turbo constructor.
	 */
	private EncodingType(int rate) {
		this.turboRate = rate;
	}
	
	/**
	 * Gets the turbo encoding rate denominator, or 0 if not applicable or unknown.
	 * 
	 * @return the encoding rate denominator only as an integer
	 */
	public int getEncodingRate() {
		return turboRate;
	}
	
	/**
	 * Indicates whether this encoding is one of the turbo encodings.
	 * 
	 * @return true if encoding is turbo, false if not 
	 */
	public boolean isTurbo() {
		return this == ANY_TURBO || 
		   this == TURBO_1_2 || this == TURBO_1_4 || this == TURBO_1_3 || this == TURBO_1_6;
	}
}    