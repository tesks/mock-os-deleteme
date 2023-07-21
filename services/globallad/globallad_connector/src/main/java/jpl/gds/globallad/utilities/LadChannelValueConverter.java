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
package jpl.gds.globallad.utilities;

import java.text.ParseException;
import java.util.Optional;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelDefinitionFactory;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.globallad.GlobalLadClientConversionException;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;

/**
 * Simple class to encapsulate the conversion of a global LAD object back to an IInternalChannelValue. 
 * Importantly, this will attempt to add back the original channel definition, using the static
 * ChannelValueFactory.
 * 
 * Most logic here moved from the defunct GladChannelValue class.
 */
public class LadChannelValueConverter {

	private final IEhaMessageFactory ehaMessageFactory;
    private final IChannelValueFactory chanFactory;
    private final IChannelDefinitionProvider chanProvider;
    private final int scid;

	public LadChannelValueConverter(final ApplicationContext appContext) {
		this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
		this.chanFactory = appContext.getBean(IChannelValueFactory.class);
		this.chanProvider = appContext.getBean(IChannelDefinitionProvider.class);
		this.scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
	}
	
	/**
	 * Convert a global LAD EHA into an IInternalChannelValue with definition restored, if possible.
	 * @param ladEha to convert to an IInternalChannelValue.
	 * @return IInternalChannelValue with as much information as possible restored
	 * @throws GlobalLadClientConversionException if a ladEha String field cannot be converted to the appropriate object
	 */
	public IServiceChannelValue convert(final EhaGlobalLadData ladEha) throws GlobalLadClientConversionException {  
        IChannelDefinition chanDef = chanProvider.getDefinitionFromChannelId(ladEha.getChannelId());
        if (chanDef == null) {
            ChannelDefinitionType cdt = ChannelDefinitionType.FSW;
            if (ladEha.isHeader()) {
                cdt = ChannelDefinitionType.H;
            } else if (ladEha.isMonitor()) {
                cdt = ChannelDefinitionType.M;
            } else if (ladEha.isSse()) {
                cdt = ChannelDefinitionType.SSE;
            }
            chanDef = ChannelDefinitionFactory.createChannel(ladEha.getChannelId(), ladEha.getChannelType(), cdt);
        }
		final IServiceChannelValue channelValue = chanFactory.createServiceChannelValue(chanDef);
		final ISclk sclk = new Sclk(ladEha.getSclkCoarse(), ladEha.getSclkFine());
		channelValue.setSclk(sclk);
		channelValue.setScet(new AccurateDateTime(ladEha.getScetMilliseconds(), ladEha.getScetNanoseconds()));
		channelValue.setErt(new AccurateDateTime(ladEha.getErtMilliseconds(), ladEha.getErtNanoseconds()));
		
		
		// Set LST directly from object and use proper scid to get LST object
		final ILocalSolarTime lst = LocalSolarTimeFactory.getNewLst(scid);
		try {
			lst.parseSolString(ladEha.getLst());
		} catch (final ParseException e) {
			throw new GlobalLadClientConversionException("Error parsing LST", e);
		}
		channelValue.setLst(lst);
		
		channelValue.setRct(new AccurateDateTime(ladEha.getEventTime()));
		channelValue.setRealtime(ladEha.isRealTime());
		
		channelValue.setDn(ladEha.getDn());
		
		final Optional<Double> eu = Optional.ofNullable(ladEha.getEu());
		if (eu.isPresent()) {
			channelValue.setEu(eu.get());
		}
		
		channelValue.setVcid((int) ladEha.getVcid());
		channelValue.setDssId(ladEha.getDssId());
		
		/**
		 * Adding the alarm information
		 */
		
		// Add the alarm set.
		channelValue.setAlarms(ladEha.getAlarmValueSet());
		
		return channelValue;
	}
	
	/**
	 * Convert an EHA returned from a global LAD query into an AlarmedChannelValueMessage.
	 * @param ladEha the global LAD EHA data to convert to an AlarmedChannelValueMessage
	 * @return AlarmedChannelValueMessage with as much information as possible restored
	 * @throws GlobalLadClientConversionException if a field of data cannot be parsed
	 */
	public IAlarmedChannelValueMessage convertToMessage(final EhaGlobalLadData ladEha) throws GlobalLadClientConversionException {
	    final IServiceChannelValue value = convert(ladEha);
		final IAlarmedChannelValueMessage ehaMsg = ehaMessageFactory.createAlarmedChannelMessage(value);
        ehaMsg.setEventTime(new AccurateDateTime(ladEha.getEventTime()));
		return ehaMsg;
	}
}
