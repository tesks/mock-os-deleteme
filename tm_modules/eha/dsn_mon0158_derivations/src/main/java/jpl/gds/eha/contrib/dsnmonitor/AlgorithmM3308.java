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
import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.eha.channel.api.IChannelValue;


/**
 * AlgorithmM3308 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM3308 extends DerivationAlgorithmBase
{
    /**
     * {@inheritDoc}
     */
    @Override
	public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue> parentChannelValues)
        throws DerivationException
    {
    	  final IChannelValue parentM1065 = parentChannelValues.get("M-1065");
    	  final IChannelValue parentM1076 = parentChannelValues.get("M-1076");
  	      final IChannelValue parentM3308 = parentChannelValues.get("M-3308");
    	  final IChannelValue parentM3309 = parentChannelValues.get("M-3309");
          final IChannelValue parentM3310 = parentChannelValues.get("M-3310");
          final IChannelValue parentM3311 = parentChannelValues.get("M-3311");
          final IChannelValue parentM3312 = parentChannelValues.get("M-3312");
          final IChannelValue parentM3313 = parentChannelValues.get("M-3313");
          final IChannelValue parentM3314 = parentChannelValues.get("M-3314");
          final IChannelValue parentM3315 = parentChannelValues.get("M-3315");
          final IChannelValue parentM3316 = parentChannelValues.get("M-3316");
          final IChannelValue parentM3317 = parentChannelValues.get("M-3317");
          final IChannelValue parentM3318 = parentChannelValues.get("M-3318");
          final IChannelValue parentM3319 = parentChannelValues.get("M-3319");

          if (parentM3308 == null)
          {
              throw new DerivationException("Missing parent M-3308");
          }

          if (parentM3309 == null)
          {
              throw new DerivationException("Missing parent M-3309");
          }

          if (parentM3310 == null)
          {
              throw new DerivationException("Missing parent M-3310");
          }
          
          if (parentM3311 == null)
          {
              throw new DerivationException("Missing parent M-3311");
          }
          
          if (parentM3312 == null)
          {
              throw new DerivationException("Missing parent M-3312");
          }
          
          if (parentM3313 == null)
          {
              throw new DerivationException("Missing parent M-3313");
          }
          
          if (parentM3314 == null)
          {
              throw new DerivationException("Missing parent M-3314");
          }
          
          if (parentM3315 == null)
          {
              throw new DerivationException("Missing parent M-3315");
          }
          
          if (parentM3316 == null)
          {
              throw new DerivationException("Missing parent M-3316");
          }
          
          if (parentM3317 == null)
          {
              throw new DerivationException("Missing parent M-3317");
          }
          
          if (parentM3318 == null)
          {
              throw new DerivationException("Missing parent M-3318");
          }
          
          if (parentM3319 == null)
          {
              throw new DerivationException("Missing parent M-3319");
          }
          
          final Map<String,IChannelValue> children = new HashMap<String,IChannelValue>();
          
          /* Get DN values for the uplink channels */
          final String uplTxrName = parentM1065.stringValue();
          final int uplBand = parentM1076.intValue();
          
          /* Get the DN values for the USC transmitter name and polarization */
          final String[] uscTxrNames = new String[6];
          final String[] uscTxrPolar = new String[6];
          
          uscTxrNames[0] = parentM3308.stringValue();
          uscTxrPolar[0] = parentM3309.stringValue();
          uscTxrNames[1] = parentM3310.stringValue();
          uscTxrPolar[1] = parentM3311.stringValue();
          uscTxrNames[2] = parentM3312.stringValue();
          uscTxrPolar[2] = parentM3313.stringValue();
          uscTxrNames[3] = parentM3314.stringValue();
          uscTxrPolar[3] = parentM3315.stringValue();
          uscTxrNames[4] = parentM3316.stringValue();
          uscTxrPolar[4] = parentM3317.stringValue();
          uscTxrNames[5] = parentM3318.stringValue();
          uscTxrPolar[5] = parentM3319.stringValue();
       
          /* Match up the uplink and microwave transmitter information to 
           * get the polarization.
           */
          boolean stopLoop = false;
          for (int index = 0; index < 6 && !stopLoop; index++) {
        	  
        	  /* If the uplink and microwave transmitter names match,
               * then set the derived channels to the microwave polarization
               * and transmitter name.
              */
        	  if (uplTxrName.equalsIgnoreCase(uscTxrNames[index])) {
        		  final IChannelValue childM4002 = createChannelValue("M-4002", uscTxrPolar[index]);
        		  children.put("M-4002", childM4002);
        		  final IChannelValue childM4003 = createChannelValue("M-4003", uscTxrNames[index]);
        		  children.put("M-4003", childM4003);
        		  stopLoop = true;
        	  } else {
        	      /* The uplink and microwave transmitter names don't match.
                   * If the uplink transmitter name is TXR20K, check the
                   * microwave transmitter name and uplink frequency band.
                   * If the microwave transmitter name and the uplink band correlate,
                   * then derive the channels.
                  */
        		  if (uplTxrName.equalsIgnoreCase("TXR20K")) {
        			  if ((uscTxrNames[index].equalsIgnoreCase("S20K") && uplBand == 1) ||
        			      (uscTxrNames[index].equalsIgnoreCase("X20K") && uplBand == 2)) {
        				  final IChannelValue childM4002 = createChannelValue("M-4002", uscTxrPolar[index]);
                		  children.put("M-4002", childM4002);
                		  final IChannelValue childM4003 = createChannelValue("M-4003", uscTxrNames[index]);
                		  children.put("M-4003", childM4003);
                		  stopLoop = true;
        			  }
        		  }

        	  }
          }
          
          /* Note children may be empty at this point */
          return children;         
    }

}
