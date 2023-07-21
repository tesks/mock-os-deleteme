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


/**
 * Possible SCLK fine format types.
 *
 */
public enum SclkFineFormat
{
    /** Ticks format*/
	TICKS,

    /** Subseconds format*/
	SUBSECONDS;
	

    /**
     * Get CSV format list.
     *
     * @return List as string
     */	
	public static String getCsvFormatList()
	{
		final StringBuilder buffer = new StringBuilder(1024);
		
		for(final SclkFineFormat value : values())
		{
			buffer.append(value);
			buffer.append(",");
		}
		
		String outputValue = buffer.toString();
        outputValue = outputValue.substring(0,outputValue.length()-1);
		return(outputValue);
	}
}
