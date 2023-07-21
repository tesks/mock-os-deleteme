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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;

/**
 * An enumeration indicating what version of the 
 * Physical Layer Operations Procedure (PLOP) should be used for uplink.
 * 
 * There is always tons of confusion about what PLOP actually means, but it's actually
 * pretty simple.  One you have a mass of CLTUs to transmit, the PLOP value is simply
 * a way of describing where acquisition sequence(s) should be located in relation to
 * the CLTUs.
 * 
 * In PLOP-1, EVERY CLTU is preceded by an acquisition sequence.
 * 
 * In PLOP-2, only THE FIRST CLTU is preceded by an acquisition sequence.
 * 
 * Idle sequences are always optional, so regardless of the PLOP type, they can be
 * inserted before and after CLTUs.
 * 
 */
public class PlopType extends EnumeratedType
{
	//integer enum values
	public static final int NONE_TYPE = 0;
	public static final int PLOP_1_TYPE = 1;
	public static final int PLOP_2_TYPE = 2;

	//string enum values
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String[] types = new String[]
	{
		"NONE",
		"PLOP-1",
		"PLOP-2"
	};
	
	//static instances
	public static final PlopType NONE = new PlopType(NONE_TYPE);
	public static final PlopType PLOP_1 = new PlopType(PLOP_1_TYPE);
	public static final PlopType PLOP_2 = new PlopType(PLOP_2_TYPE);
	                  
	/**
	 * Create a new PlopType enum
	 * 
	 * @param intVal The integer value of the enum
	 */
	public PlopType(int intVal)
	{
		super(intVal);
	}
	
	/**
	 * Create a new PlopType enum
	 * 
	 * @param strVal The string value of the enum
	 */
	public PlopType(String strVal)
	{
		super(strVal);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex()
	{
		return(types.length-1);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(int index)
	{
		if(index < 0 || index > getMaxIndex())
		{
			throw new IllegalArgumentException("The index value " + index + " is not a valid index for this enumeration.");
		}
		
		return(types[index]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#setValueFromString(java.lang.String)
	 */
	@Override
	protected synchronized void setValueFromString(String strVal)
    {
    	if(this.maxIndex == -1)
    	{
            this.maxIndex = getMaxIndex();
        }
    	
        for (int i = 0; i <= this.maxIndex; i++)
        {
            if (getStringValue(i).equalsIgnoreCase(strVal))
            {
                this.valIndex = i;
                this.valString = getStringValue(i);
                return;
            }
        }
        
        try
        {
        	int intVal = Integer.parseInt(strVal);
        	switch(intVal)
        	{
        		case PLOP_1_TYPE:
        		case PLOP_2_TYPE:
        			
        			setValueFromIndex(intVal);
        			break;
        			
        		default:
        			
        			break;
        	}
        }
        catch(NumberFormatException nfe)
        {
        	//ignore this and let the later exception throw
        }
        
        throw new IllegalArgumentException("Invalid enumeration value \"" + strVal + "\"");
    }
}