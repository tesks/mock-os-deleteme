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

import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.eha.channel.api.IChannelValue;


/**
 * AlgorithmM0300 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM0300 extends AbstractAlgorithmMHHMMSSFromPoly
{
    /**
     * Perform derivations.
     *
     * @param parentChannelValues The input channels
     *
     * @return The output channels
     *
     * @throws DerivationException Problem deriving
     */
    @Override
    public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue> parentChannelValues)
        throws DerivationException
    {
        final IChannelValue parentM0300 = parentChannelValues.get("M-0300");

        if (parentM0300 == null)
        {
            throw new DerivationException("Missing parent M-0300");
        }
        
        final Map<String, IChannelValue> result = new HashMap<String, IChannelValue>();
        
        /* creates 2 ASCII channels: YYYY/DDD and HH:MM:SS */
        long secs = parentM0300.longValue();
        
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
        sb.append("/");		// sb = Ayear + "/"
        
        /* secs is now secs of this year */
        /* below gets secs into HH:MM:SS ASCII format */
        
        long doy = secs / 86400 + 1;
        secs = secs % 86400;	/* secs is now seconds of day */
        String Adoy = Long.toString(doy);
        
        int Alength = Adoy.length();
        
        if (Alength == 1) {	/* HH MM and SS all need leading zeroes if 1 digit */
        	sb.append("00");
        } else if (Alength == 2) {
        	sb.append("0");
        }
        
        sb.append(Adoy);	// sb = Ayear + "/" + DOY(3)
        sb.append("-");
        
        /* derive child channel 1 now */
        final IChannelValue child1 = createChannelValue("M-3624", sb.toString());
        result.put("M-3624", child1);

        sb.setLength(0);
        long hour = secs / 3600;
        secs = secs % 3600;	/* secs is now secs of hour */
        
        if (hour < 10) {
        	sb.append("0");
        }
        
        sb.append(hour);
        sb.append(":");
        long mm = secs / 60;
        secs = secs % 60; /* secs is now secs of minute */
        
        if (mm < 10) {
        	sb.append("0");
        }
        
        sb.append(mm);
        sb.append(":");
        
        if (secs < 10) {
        	sb.append("0");
        }
        
        sb.append(secs);

        /* derive child channel 2 now */
        final IChannelValue child2 = createChannelValue("M-3625", sb.toString());
        result.put("M-3625", child2);

        return result;
    }

    @Override
    public void init()
    {
    	//do nothing
    }
    
    @Override
    public void cleanup()
    {
    	//do nothing
    }
    
}
