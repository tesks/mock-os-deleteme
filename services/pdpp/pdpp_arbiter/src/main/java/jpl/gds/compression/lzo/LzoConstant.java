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
package jpl.gds.compression.lzo;

import java.util.HashMap;
import java.util.Map;

/**
 * LZO Constant
 */
public enum LzoConstant {
	LZO_E_OK(0),
	LZO_E_ERROR(-1),
	LZO_E_OUT_OF_MEMORY(-2),
	LZO_E_NOT_COMPRESSIBLE(-3),
	LZO_E_INPUT_OVERRUN(-4),
	LZO_E_OUTPUT_OVERRUN(-5),
	LZO_E_LOOKBEHIND_OVERRUN(-6),
	LZO_E_EOF_NOT_FOUND(-7),
	LZO_E_INPUT_NOT_CONSUMED(-8);

	/**
	 * 
	 */
	@SuppressWarnings("serial")
	private static final Map<Integer,LzoConstant> intToConstantMap = new HashMap<Integer, LzoConstant>(9) {{
		put(0, LZO_E_OK);
		put(-1, LZO_E_ERROR);
		put(-2, LZO_E_OUT_OF_MEMORY);
		put(-3, LZO_E_NOT_COMPRESSIBLE);
		put(-4, LZO_E_INPUT_OVERRUN);
		put(-5, LZO_E_OUTPUT_OVERRUN);
		put(-6, LZO_E_LOOKBEHIND_OVERRUN);
		put(-7, LZO_E_EOF_NOT_FOUND);
		put(-8, LZO_E_INPUT_NOT_CONSUMED);
	}};
	
	/**
	 * @param value
	 * @return
	 */
	public static final LzoConstant getValue(final int value) {
		return intToConstantMap.get(value);
	}
	
	/**
	 * 
	 */
	private int	value;

	/**
	 * @param value
	 */
	private LzoConstant(final int value) {
		this.value = value;
	}

	/**
	 * @return
	 */
	public int getValue() {
		return value;
	}
}
