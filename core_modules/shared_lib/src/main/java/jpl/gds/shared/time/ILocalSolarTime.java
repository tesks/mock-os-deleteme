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

import java.text.ParseException;

import jpl.gds.serialization.primitives.time.Proto3Lst;

public interface ILocalSolarTime extends IAccurateDateTime, IImmutableLocalSolarTime {                                              // milliseconds

    /**
     * Parse SOL string.
     *
     * is it safe to assume the leap seconds (if any) were already factored into
     * the sol string? I think so.
     * im just going to set the member variable leapseconds to the correct value
     *
     * @param timeStr String to parse
     *
     * @throws ParseException If unable to parse
     */
    void parseSolString(String timeStr) throws ParseException;

    /**
     * Sets the SCLK/SCET converter used by this class. For test purposes only.
     * 
     * @param converter
     *            the SclkScetConverter to set
     */
    void setConverter(SclkScetConverter converter);

    /**
     * Gets the SCLK/SCET converter used by this class. For test purposes only.
     * 
     * @return SclkScetConverter
     */
    SclkScetConverter getConverter();
    
    /**
     * Sets the SCET 0 (landing SCET) value used to compute sol time.
     * 
     * @param scet0
     *            the landing SCET
     */
    void setScet0(IAccurateDateTime scet0);

    /**
     * Calculates Local Solar Time based on given SCLK
     * 
     * @param iSclk
     *            is ISclk object that will be converted to Local Solar Time
     *
     * @return Local solar time
     */
    @Override
    ILocalSolarTime sclkToSol(ISclk iSclk);

    /**
     * Calculates Local Solar Time based on given SCET
     * 
     * @param scet
     *            is the Date object that will be converted to Local Solar Time
     *
     * @return Local solar time
     */
    @Override
    ILocalSolarTime scetToSol(IAccurateDateTime scet);

    /**
     * SOL to SCET conversion
     * 
     * @return scet is the value of this sol
     */
    @Override
    IAccurateDateTime toScet();

    /**
     * Populate the local solar time from a protobuf message.
     * 
     * @param msg
     *            the ILocalSolarTime in a protobuf message
     */
    public void loadLocalSolarTime(Proto3Lst msg);

}
