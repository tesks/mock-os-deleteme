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
package jpl.gds.telem.input.api.config;

import java.util.LinkedList;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * Option parser class for buffered input type mode.
 * 
 *
 * @since R8
 */
public class BufferedInputModeTypeOptionParser extends
EnumOptionParser<BufferedInputModeType> {
    
    private final TelemetryInputProperties ric;
    private final boolean forSse;
    private final boolean forFsw;

    
	/**
	 * Constructor. Note that both of the isFsw and isSse flags may be set if
	 * this command line option is used for an integrated (flight and SSE)
	 * application.
	 * 
	 * @param config
	 *            the current TelemetryInputConfig object
	 * @param isFsw
	 *            true if the argument is for a flight application
	 * @param isSse
	 *            true if the argument is for an SSE application
	 */
	public BufferedInputModeTypeOptionParser(final TelemetryInputProperties config,
			final boolean isFsw, final boolean isSse) {
		super(BufferedInputModeType.class,
				new LinkedList<BufferedInputModeType>(
						config.getAllowedBufferedInputModes()));
		ric = config;
		this.setDefaultValue(ric.getDefaultBufferedInputMode());
		forSse = isSse;
		forFsw = isFsw;
	}


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
     *      jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public BufferedInputModeType parse(final ICommandLine commandLine,
            final ICommandLineOption<BufferedInputModeType> opt)
                    throws ParseException {

        BufferedInputModeType mode = super.parse(commandLine, opt);

        if (mode != null && mode.equals(BufferedInputModeType.BOTH)) {
            if (forSse) {
                if (ric.getAllowedBufferedInputModes().contains(BufferedInputModeType.SSE)) {
                    mode = BufferedInputModeType.SSE;
                } else {
                    mode = BufferedInputModeType.NONE;
                } 
            } else if (forFsw) {
            
                if (ric.getAllowedBufferedInputModes().contains(BufferedInputModeType.FSW)){
                    mode = BufferedInputModeType.FSW;
                } else {
                    mode = BufferedInputModeType.NONE;
                }
            }
           
        }
               
        return mode;

    }


}
