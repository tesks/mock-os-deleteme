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
package jpl.gds.eha.contrib.dsnmonitor;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.eha.channel.api.DerivationAlgorithmBase;
import jpl.gds.eha.channel.api.IChannelValue;


/**
 * AbstractAlgorithmMDateTime is an abstract M-channel algorithmic derivation
 * class that derives two ASCII channels: YYYY/DDD and HH:MM:SS.fff
 * from two parent channels. It mimics the algorithm in mon158.ccl.
 *
 */
abstract public class AbstractAlgorithmMDateTime extends DerivationAlgorithmBase
{
	
    /**
     * Perform derivation
     *
	 * @param inputSecs seconds
	 * @param nanosecs nanoseconds
	 * @param chan1Name name of first child channel
	 * @param chan2Name name of second child channel
	 *
	 * @return The output channels contained in a ACVMap
     */
    protected Map<String, IChannelValue> deriveMDateTimeChannels(long inputSecs, final long nanosecs, final String chan1Name, final String chan2Name) {
    	final Map<String, IChannelValue> result = new HashMap<String, IChannelValue>();

        long secs = inputSecs;
        
        /* calculate year */
        long oldsecs;
        long secsperyear;
        int year;
        
        for (year = 1970; true; ) {
        	oldsecs = secs;
        	
        	if (((year % 4) == 0 && (year % 100) != 0) || (year % 400) == 0) {
        		secsperyear = 31622400;
        	} else {
        		secsperyear = 31536000;
        	}

        	if (secs < secsperyear) {
        		secs = oldsecs;
        		break;
        	} else {
        		secs -= secsperyear;
        	}
        	
        	year++;
        }

        /* change year to ASCII */
        String Ayear = Integer.toString(year);
        StringBuilder sb = new StringBuilder(80);
        sb.append(Ayear); 	// sb = Ayear
        sb.append('/');		// sb = Ayear + "/"
        
        /* secs is now secs of this year */
        /* below gets secs into HH:MM:SS ASCII format */
        
        long doy = secs / 86400 + 1;
        secs = secs % 86400;	/* secs is now seconds of day */
        String Adoy = Long.toString(doy);
        
        int Alength = Adoy.length();
        
        if (Alength == 1) {	/* HH MM and SS all need leading zeroes if 1 digit */
        	sb.append("00");
        } else if (Alength == 2) {
        	sb.append('0');
        }
        
        sb.append(Adoy);	// sb = Ayear + "/" + DOY(3)
        sb.append('-');
        
        /* derive child channel 1 now */
        final IChannelValue child1 = createChannelValue(chan1Name, sb.toString());
        result.put(chan1Name, child1);

        sb.setLength(0);
        long hour = secs / 3600;
        secs = secs % 3600;	/* secs is now secs of hour */
        
        if (hour < 10) {
        	sb.append('0');
        }
        
        sb.append(hour);
        sb.append(':');
        long mm = secs / 60;
        secs = secs % 60; /* secs is now secs of minute */
        
        if (mm < 10) {
        	sb.append('0');
        }
        
        sb.append(mm);
        sb.append(':');
        
        if (secs < 10) {
        	sb.append('0');
        }
        
        sb.append(secs);
        sb.append('.');
        
        /* now do the nanoseconds */
        
        /* Code below from CCL is for EU, so omitted.
         * 
		 * eu = eu + (nano / 10.0**-9.0);
	     * PUT_EU(eu);
         */

        String Anano = Long.toString(nanosecs);
        int nanoZeros = 9 - Anano.length();	/* number of zeroes to add in front */
        
        if (nanoZeros != 0) {
        	for (; nanoZeros > 0; nanoZeros--) {
        		sb.append('0');
        	}
        }
        
        sb.append(nanosecs);
        sb.setLength(sb.length() - 6);	// seems only 3 significant decimals are desired/needed

        /* derive child channel 2 now */
        final IChannelValue child2 = createChannelValue(chan2Name, sb.toString());
        result.put(chan2Name, child2);

        return result;
    }

}
