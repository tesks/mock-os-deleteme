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
package jpl.gds.tc.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.message.ICommandMessage;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.IScmfCommandMessage;

/**
 * A static utility for writing command data and messages to either the
 * console or the chill_up GUI.
 * 
 *
 * MPCS-11285 - 09/24/19 - added writeScmfToDisplay
 */
public class DisplayUtility
{
	private static Tracer trace; 

	private static final int BUFFER_SIZE = 800;
	
	private DisplayUtility() {
	    //do nothing
	}

	/**
	 * Displays the given message. If in GUI mode, it goes to the GUI. If not,
	 * it goes to the console
	 * @param appContext the current application context
	 * @param s the message to display
	 * @param debugLevel true if the console output should be at DEBUG level,
	 *        false for INFO level
	 */
	public static void printMessage(final ApplicationContext appContext, final String  s,
                                    final boolean debugLevel)
	{
        trace = TraceManager.getDefaultTracer(appContext);
		if(appContext.getBean(CommandProperties.class).getShowGui())
		{
			appContext.getBean(IMessagePublicationBus.class).publish(
			        appContext.getBean(ICommandMessageFactory.class).createUplinkGuiLogMessage(s));
		}
		else
		{
            if (debugLevel)
            {
                trace.debug(s);
            }
            else
            {
                trace.info(s);
            }
		}
	}
	
	/**
     * Displays the given message. If in GUI mode, it goes to the GUI. If not,
     * it goes to the console
     * @param appContext the current application context
     * @param s the message to display
     */
	public static void printMessage(final ApplicationContext appContext, final String s)
	{
        trace = TraceManager.getDefaultTracer(appContext);
        printMessage(appContext, s, false);
    }

	/**
	 * Displays the given SCMF command message data in hex format. If in GUI mode, it goes to the GUI. If not,
	 * it goes to the console
	 * @param appContext the current application context
	 * @param cmdMsgs the list of SCMF command messages to display
	 */
	public static void writeScmfToDisplay(final ApplicationContext appContext, final IScmf scmf) throws CltuEndecException {
		trace = TraceManager.getDefaultTracer(appContext);

		List<IScmfCommandMessage> cmdMsgs = scmf.getCommandMessages();
		List<ICltu> plopCltus = scmf.getCltusFromScmf();

		printMessage(appContext, "\n");
		for(int i = 0 ; i < plopCltus.size() ; i++) {
			if(plopCltus.get(i) != null) {
				printMessage(appContext, plopCltus.get(i).getHexDisplayString() + "\n\n");
			} else {
				printMessage(appContext, BinOctHexUtility.toHexFromBytes(cmdMsgs.get(i).getData()));
			}
		}
		printMessage(appContext, "\n");
	}

	/**
     * Displays the given CLTU data in hex format. If in GUI mode, it goes to the GUI. If not,
     * it goes to the console
     * @param appContext the current application context
     * @param plopCltus the list of CLTUs to display
     */
	public static void writeCltusToDisplay(final ApplicationContext appContext, final List<ICltu> plopCltus)
    {
        trace = TraceManager.getDefaultTracer(appContext);
        final StringBuilder sb = new StringBuilder("\n");
		for (final ICltu cltu : plopCltus) {
			sb.append(cltu.getHexDisplayString()).append("\n\n");
		}
		sb.append("\n");
		printMessage(appContext, sb.toString());
    }

	/**
     * Displays the given SCMF messages in hex format. If in GUI mode, it goes to the GUI. If not,
     * it goes to the console
     * @param appContext the current application context
     * @param messages the list of SCMF messages to display
     */
    public static void writeSpacecraftMessagesToDisplay(final ApplicationContext appContext, final List<IScmfCommandMessage> messages)
    {
    	trace = TraceManager.getDefaultTracer();
    	printMessage(appContext, "\n");
		for(int i=0; i < messages.size(); i++)
    	{
    		final byte[] bytes = messages.get(i).getData();
    		final String hex = BinOctHexUtility.toHexFromBytes(bytes);
            printMessage(appContext, BinOctHexUtility.formatHexString(hex, 40) + "\n\n");
    	}
		printMessage(appContext, "\n");
    }

    /**
     * Displays the given raw data file in hex format. If in GUI mode, it goes to the GUI. If not,
     * it goes to the console
     * @param appContext the current application context
     * @param rawDataFile the path to the data file with content to display
     * @param isHexFile true if the file is already an ASCII file of hex characters, false if it is binary
     */
    public static void writeRawUplinkDataToDisplay(final ApplicationContext appContext, final File rawDataFile, final boolean isHexFile)
    {
        trace = TraceManager.getDefaultTracer(appContext);
    	printMessage(appContext, "\n");
    	try
    	{
	    	if (isHexFile)
			{
				final BufferedReader reader = new BufferedReader(new FileReader(rawDataFile));
				String line = reader.readLine();
				while(line != null)
				{
					final String charString = GDR.removeWhitespaceFromString(line.trim());
					printMessage(appContext, BinOctHexUtility.formatHexString(charString,40) + "\n\n");
					line = reader.readLine();
				}
				reader.close();
			}
			else
			{
				byte[] inputBytes = new byte[BUFFER_SIZE];
				byte[] tempBytes = null;
				final FileInputStream fis = new FileInputStream(rawDataFile);
	
				int result = fis.read(inputBytes);
				while(result != -1)
				{
					if(result != inputBytes.length)
					{
						tempBytes = new byte[result];
						System.arraycopy(inputBytes,0,tempBytes,0,result);
						inputBytes = tempBytes;
					}
					final String hex = BinOctHexUtility.toHexFromBytes(inputBytes);
					printMessage(appContext, BinOctHexUtility.formatHexString(hex,40) + "\n\n");
					result = fis.read(inputBytes);
				}
				fis.close();
			}
    	}
    	catch(final Exception e)
    	{
    		printMessage(appContext, "Could not write hex output to display: " + e.getMessage());
    	}
		printMessage(appContext, "\n");
    }
}
