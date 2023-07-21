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

import java.util.Hashtable;

import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * This is a utility class for using SCLK/SCET correlation files to
 * convert between times in a SCLK format and times in a SCET format.
 * 
 *
 */
public class SclkScetUtility
{
	/** A mapping from spacecraft ID to SCLK/SCET conversion object
	 * 
	 * (key,value) pairs are in the form of (Integer, SclkScetConverter)
	 */
    private static Hashtable<Integer, SclkScetConverter> converters = new Hashtable<>();
	
    private static final Tracer                          tracer     = TraceManager.getDefaultTracer();

	/**
	 * Get a SCLK time from the given SCET time for the given spacecraft.
	 * 
	 * @param scet The SCET time to convert
	 * 
	 * @param ert The associated ERT...unless there is a concern over SCLK resets
	 * occurring, this parameter can be null
	 * 
	 * @param scid The ID of the spacecraft whose correlation table should be used
	 *
	 * @return The SCLK time corresponding to the input SCET and spacecraft ID or null if
	 * the correlation file could not be found
	 */
    public static ISclk getSclk(final IAccurateDateTime scet, final IAccurateDateTime ert, final int scid) {
        return getSclk(scet, ert, scid, tracer);
	}
	
    /**
     * Get a SCLK time from the given SCET time for the given spacecraft.
     * 
     * @param scet
     *            The SCET time to convert
     * 
     * @param ert
     *            The associated ERT...unless there is a concern over SCLK resets
     *            occurring, this parameter can be null
     * 
     * @param scid
     *            The ID of the spacecraft whose correlation table should be used
     * @param log
     *            the Tracer to log with
     *
     * @return The SCLK time corresponding to the input SCET and spacecraft ID or null if
     *         the correlation file could not be found
     */
    public static ISclk getSclk(final IAccurateDateTime scet, final IAccurateDateTime ert, final int scid,
                                final Tracer log) {
        if (scet == null) {
            throw new IllegalArgumentException("Null input SCET value");
        }

        // get the proper converter
        final SclkScetConverter converter = getConverterFromSpacecraftId(scid, log);
        if (converter == null) {
            return (null);
        }

        // do the conversion
        return (converter.to_sclk(scet, ert));

    }

    /**
	 * Get a SCET time from the given SCLK time for the given spacecraft.
	 * 
	 * @param sclk The SCLK time to convert
	 * 
	 * @param ert The associated ERT...unless there is a concern over SCLK resets
	 * occurring, this parameter can be null
	 * 
	 * @param scid The ID of the spacecraft whose correlation table should be used
	 *
	 * @return The SCET time corresponding to the input SCLK and spacecraft ID or null
	 * if the correlation file could not be found
	 */
    public static IAccurateDateTime getScet(final ICoarseFineTime sclk,
                                           final IAccurateDateTime ert,
                                           final int              scid)
	{
        return getScet(sclk, ert, scid, tracer);
	}

    /**
     * Get a SCET time from the given SCLK time for the given spacecraft.
     * 
     * @param sclk
     *            The SCLK time to convert
     * 
     * @param ert
     *            The associated ERT...unless there is a concern over SCLK resets
     *            occurring, this parameter can be null
     * 
     * @param scid
     *            The ID of the spacecraft whose correlation table should be used
     * @param log
     *            The Tracer to log with
     *
     * @return The SCET time corresponding to the input SCLK and spacecraft ID or null
     *         if the correlation file could not be found
     */
    public static IAccurateDateTime getScet(final ICoarseFineTime sclk, final IAccurateDateTime ert, final int scid,
                                            final Tracer log) {
        if (sclk == null) {
            throw new IllegalArgumentException("Null input SCLK value");
        }

        // get the proper converter
        final SclkScetConverter converter = getConverterFromSpacecraftId(scid, log);
        if (converter == null) {
            return null;
        }

        // do the conversion
        return (converter.to_scet(sclk, ert));
    }


	/**
	 * Given a spacecraft ID, get the associated SclkScetConverter object.  If the object has already
	 * been created (the correlation file sclkscet.scid has been parsed), then that object will be returned
	 * without re-parsing.  Otherwise, the associated sclkscet file will be found and parsed and then a converter
	 * will be created.
	 * 
	 * @param scid The ID of the spacecraft for which a sclk/scet converter is needed
	 * 
	 * @return The sclk/scet converter for the given spacecraft or null if the correlation file cannot be found
	 */
	public static SclkScetConverter getConverterFromSpacecraftId(final int scid)
	{
        return getConverterFromSpacecraftId(scid, tracer);
	}

    /**
     * Given a spacecraft ID, get the associated SclkScetConverter object. If the object has already
     * been created (the correlation file sclkscet.scid has been parsed), then that object will be returned
     * without re-parsing. Otherwise, the associated sclkscet file will be found and parsed and then a converter
     * will be created.
     * 
     * @param scid
     *            The ID of the spacecraft for which a sclk/scet converter is needed
     * @param trace
     *            Tracer to log with
     * 
     * @return The sclk/scet converter for the given spacecraft or null if the correlation file cannot be found
     */
    public static SclkScetConverter getConverterFromSpacecraftId(final int scid, final Tracer trace) {
        // look in the hashtable to see if the converter already exists (if so, just return it)
        SclkScetConverter converter = converters.get(scid);
        if (converter == null) {
            // try to create the converter if it doesn't exist
            converter = SclkScetConverter.createConverter(scid, trace);
            if (converter != null) {
                // if we successfully created the converter, store it locally so we
                // don't have to create it again
                converters.put(Integer.valueOf(scid), converter);
            }
        }

        return (converter);
    }


    /**
     * Get a SCLK/SCET converter. If one has already been loaded for the given spacecraft ID it will be returned.
     * Otherwise, the specified file will be loaded and returned as a converter.
     *
     * @param scid     S/C id
     * @param filename File name
     *
     * @return Converter
     */	
	public static SclkScetConverter getConverterFromFile(final int scid,final String filename)
	{
		//look in the hashtable to see if the converter already exists (if so, just return it)
		SclkScetConverter converter = converters.get(scid);
		if(converter == null)
		{
			//try to create the converter if it doesn't exist
            converter = SclkScetConverter.createConverter(filename);
			if(converter != null)
			{
				
				/* sclk/scet utility is the only non-test to use the string createConverter
				                           check to see if the file matches what the user is looking for.
				*/
				try{
					if(scid != Integer.parseInt(converter.getMetaValue("SPACECRAFT_ID"))){
                        tracer.warn(Markers.TIME_CORR,
                                "Given Spacecraft ID and value in SCLK/SCET file do not match: Expected: " + scid
							+ " in file: " + converter.getMetaValue("SPACECRAFT_ID"));
					}
				}
				catch(final NumberFormatException e){
                    tracer.warn(Markers.TIME_CORR,
                            "Spacecraft ID in loaded SCLK/SCET file is not a valid integer: Expected: " + scid
							+ " in file: "+ converter.getMetaValue("SPACECRAFT_ID"));
				}
				
				//if we successfully created the converter, store it locally so we
				//don't have to create it again
				converters.put(Integer.valueOf(scid),converter);
			}
		}
		
		return(converter);
	}
}
