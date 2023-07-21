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
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.message.IEvrMessageFactory;
import jpl.gds.globallad.GlobalLadClientConversionException;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.Sclk;

/**
 * Simple class to encapsulate the conversion of a global LAD EVR representation
 * back into an IEvr object with the EvrDefinition restored.  In order
 * to accomplish that, the global EvrDefinitionTable will be used. Consequently, the most globally loaded Evr dictionary
 * will be used to look up Evr definitions.
 * 
 * Most logic here moved from the defunct GladEvr class.
 */
public class LadEvrConverter {
    
    private final DictionaryProperties dictConfig;
    private final IEvrUtilityDictionaryManager evrDict;
    private final IEvrFactory evrFactory;
    private final IEvrMessageFactory evrMsgFactory;
    private final IEvrDictionaryFactory evrDictFactory;

    public LadEvrConverter(final ApplicationContext appContext) {
        dictConfig = appContext.getBean(DictionaryProperties.class);
        evrDict = appContext.getBean(IEvrUtilityDictionaryManager.class);
        evrFactory = appContext.getBean(IEvrFactory.class);
        evrDictFactory = appContext.getBean(IEvrDictionaryFactory.class);
        evrMsgFactory = appContext.getBean(IEvrMessageFactory.class);
    }

	/**
	 * Convert a global LAD EVR into an IEvr with definition restored, if possible.
	 * @param ladEvr to convert to an IEvr
	 * @return IEvr with as much information as possible restored.
	 * @throws DictionaryException if no EVR definition is found, and a dummy one cannot be created
	 * @throws GlobalLadClientConversionException 
	 */
	public IEvr convert(final EvrGlobalLadData ladEvr) throws GlobalLadClientConversionException  {
		IEvrDefinition evrDef = ladEvr.isFsw() ? evrDict.getFswDefinition(ladEvr.getEvrName()) : evrDict.getSseDefinition(ladEvr.getEvrName());

		if (evrDef == null) {
			evrDef = evrDictFactory.getMultimissionEvrDefinition();
			evrDef.setId(ladEvr.getEvrId());
			evrDef.setName(ladEvr.getEvrName());
			evrDef.setLevel(ladEvr.getEvrLevel());
		}
		
		final IEvr evr = evrFactory.createEvr();
		
		/**
		 * Set the time values from the glad object.
		 */
		final ISclk sclk = new Sclk(ladEvr.getSclkCoarse(), ladEvr.getSclkFine());
		evr.setSclk(sclk);
		evr.setScet(new AccurateDateTime(ladEvr.getScetMilliseconds(), ladEvr.getScetNanoseconds()));
		evr.setErt(new AccurateDateTime(ladEvr.getErtMilliseconds(), ladEvr.getErtNanoseconds()));
		
		// Set LST directly from object.
		final ILocalSolarTime lst = new LocalSolarTime(false);
		try {
			lst.parseSolString(ladEvr.getLst());
		} catch (final ParseException e) {
			throw new GlobalLadClientConversionException("Error parsing LST", e);
		}
		evr.setSol(lst);
		evr.setRct(new AccurateDateTime(ladEvr.getEventTime()));
		

		evr.setRealtime(ladEvr.isRealTime());

		evr.setVcid((int)ladEvr.getVcid());
		evr.setDssId(ladEvr.getDssId());
		
		evr.setMessage(ladEvr.getMessage());
		evr.setFromSse(!ladEvr.isFsw());

		evr.setEvrDefinition(evrDef);
		
		/**
		 * Create an evr metadata object.
		 */
		final List<EvrMetadataKeywordEnum> keys = new ArrayList<EvrMetadataKeywordEnum>();
		final List<String> vals = new ArrayList<String>();
		
		for (final EvrMetadataKeywordEnum md : EvrMetadataKeywordEnum.values()) {
			keys.add(md);
			vals.add(lookupMetadataValue(md, ladEvr));
		}
		
		evr.setMetadataKeyValuesFromStrings(keys, vals);
		return evr;
	}
	
	/**
	 * Convert EVR object returned from a global LAD query to an EvrMessage representation.
	 * @param ladEvr - the LAD Evr object that should be converted.
	 * @return an EvrMessage containing an IEvr converted from the LAD object and timestamped with the LAD event time.
	 * @throws GlobalLadClientConversionException if something went wrong with parsing a field or using dictionaries.
	 */
	public IEvrMessage convertToMessage(final EvrGlobalLadData ladEvr) throws GlobalLadClientConversionException {
		final IEvr evr = convert(ladEvr);
		final IEvrMessage msg = evrMsgFactory.createEvrMessage(evr);
        msg.setEventTime(new AccurateDateTime(ladEvr.getEventTime()));
		return msg;
	}
	
	/**
	 * @param md
	 * @param evr
	 * @return
	 */
	private String lookupMetadataValue(final EvrMetadataKeywordEnum md, final EvrGlobalLadData evr) {
		String value;
		switch(md) {
		case ADDRESSSTACK:
			value = evr.getAddressStack();
			break;
		case CATEGORYSEQUENCEID:
			value = evr.getCategorySequenceId();
			break;
		case ERRNO:
			value = evr.getErrno();
			break;
		case SEQUENCEID:
			value = evr.getSequenceId();
			break;
		case SOURCE:
			value = evr.getSource();
			break;
		case TASKID:
			value = evr.getTaskId();
			break;
		case TASKNAME:
			value = evr.getTaskName();
			break;
		case UNKNOWN:
		default:
			value = "";
			break;
		
		}
		
		return value;
	}
} 
