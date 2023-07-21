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
package jpl.gds.globallad.service;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;

/**
 * Basic implementation of the converter.  This recognizes only Evr and Eha messages.  
 * 
 * Couldn't think of a better way to do this. Needed to remove the user data type mappings from the data objects themselves.
 * So had to have somewhere to include these mappings, but keep in mind the mappings are meant to be updated as needed,
 * so they are included in the IGlobalLadDataFactory interface and this factory class is set in the properties file and is configurable.  
 *
 * Ideally we would want to set the user data type in the GLAD object when they are created. We can do this when
 * we are creating the objects from the byte array because the UDT has been set.  However, at the data producer side the UDT is not known at 
 * the constructor. In order to get the mappings we would have to create a data factory and I did not want to tie that to the data classes themselves.
 *
 * This is the first time the data objects are being created and the UDT is unknown at the constructor.  The data converter
 * will create a factory, and when you pass this in it will look up the user data type and call the setUserData of the object.  This was just
 * a conv method. The user data type can be gotten and set it here as well.
 */
public class BasicMessageToGlobalLadEventTranslator extends AbstractMessageToGlobalLadEventTranslator {
	private static final Tracer log = GlobalLadProperties.getTracer();
	private final ApplicationContext appContext;
	private final IAlarmValueSetFactory alarmSetFactory;
	
	public BasicMessageToGlobalLadEventTranslator(final ApplicationContext appContext) {
		this.appContext = appContext;
		this.alarmSetFactory = appContext.getBean(IAlarmValueSetFactory.class);
	}

	/**
	 * @param message
	 * @return
	 * @throws GlobalLadConversionException 
	 */
	private IGlobalLADData createEvr(final IMessage message) throws GlobalLadConversionException {
		final IEvrMessage m = (IEvrMessage) message;
		final IEvr evr = m.getEvr();
        final ISclk sclk = evr.getSclk();
		final IAccurateDateTime ert = evr.getErt();
		final IAccurateDateTime scet = evr.getScet();

		/**
		 * Added conditional to stop processing of bad EVRs
		 */
		if (evr.isBadEvr()) {
			log.debug(String.format("Skipping bad EVR ID:%s", evr.getEventId()));
			return null;
		}
		
		/**
		 * Including evr metadata.
		 */
		final EvrMetadata md = evr.getMetadata();

		/**
		 * Checking the definition to make sure it is valid.
		 */
		if (evr.getName() == null) {
			/**
			 * This means the def is bad and this will cause a NPE in the create.  Throw here with 
			 * a valid error message.
			 */
			throw new GlobalLadConversionException("EVR definition is not properly set: " + m.getOneLineSummary());
		}
		/**
		 * In the case that we are running in no database mode the session number will be null.  It needs to be
		 * checked so it can be added properly.
		 */
		final IContextKey idObj = appContext.getBean(IContextKey.class);		
		final long sid = idObj.getNumber() == null ? -1L : idObj.getNumber();
		
		final IGlobalLADData data = new EvrGlobalLadData(
				evr.getEventId(), // evr id
				evr.getLevel(), // evrLevel, 
				evr.getName(), // evrName, 
				evr.isRealtime(), // isRealTime, 
				!evr.isFromSse(), // isFsw
				evr.getMessage(), // evr message 
				md.getMetadataValue(EvrMetadataKeywordEnum.TASKNAME),
				md.getMetadataValue(EvrMetadataKeywordEnum.SEQUENCEID),
				md.getMetadataValue(EvrMetadataKeywordEnum.CATEGORYSEQUENCEID),
				md.getMetadataValue(EvrMetadataKeywordEnum.ADDRESSSTACK),
				md.getMetadataValue(EvrMetadataKeywordEnum.SOURCE),
				md.getMetadataValue(EvrMetadataKeywordEnum.TASKID),
				md.getMetadataValue(EvrMetadataKeywordEnum.ERRNO),
				sclk.getCoarse(), // sclkCoarse, 
				sclk.getFine(), // sclkFine, 
				ert.getTime(), // ertMilliseconds, 
				ert.getNanoseconds(), 
				scet.getTime(), 
				scet.getNanoseconds(), 
				sid, 
			    appContext.getBean(IContextIdentification.class).getSpacecraftId(),
				appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
				(byte) evr.getDssId(), 
				evr.getVcid().byteValue(), //vcid, 
				idObj.getHost()
				);
		
		GlobalLadUserDatatypeConverter.setUserDataTypeFromData(data);
		
		return data;
	}
	
	/**
	 * @param message
	 * @return
	 * @throws GlobalLadDataException
	 * @throws GlobalLadConversionException 
	 */
	private IGlobalLADData createEha(final IMessage message) throws GlobalLadDataException, GlobalLadConversionException {
	    final IAlarmedChannelValueMessage m = (IAlarmedChannelValueMessage) message;

		final IServiceChannelValue cv = (IServiceChannelValue) m.getChannelValue();

		/**
		 * Checking the definition to make sure it is valid.
		 */
		if (cv.getChanId() == null) {
			/**
			 * This means the def is bad and this will cause a NPE in the create.  Throw here with 
			 * a valid error message.
			 */
			throw new GlobalLadConversionException("Channel definition has not bee properly set: " + message);
		}

		
        final ISclk sclk = cv.getSclk();
		final IAccurateDateTime ert = cv.getErt();
		final IAccurateDateTime scet = cv.getScet();;
		
		/**
		 * In the case that we are running in no database mode the session number will be null.  It needs to be
		 * checked so it can be added properly.  
		 * 
		 * TODO:  If no database is set should it go to the lad?
		 */
		final IContextKey idObj = appContext.getBean(IContextKey.class);		
		final long sid = idObj.getNumber() == null ? -1L : idObj.getNumber();
		
		boolean isHeader = false;
		boolean isFsw = false;
		boolean isMonitor = false;
		boolean isSse = false;
		
		/**
		 * Only fsw can be recorded.
		 */
		boolean isRealtime = true;
		
		/**
		 * Must use the definition type and not the enum that is part of the channel.  That is for the 
		 * database and is not set properly at runtime.
		 */
		switch (cv.getDefinitionType()) {
		case FSW:
			isFsw = true;
			isRealtime = cv.isRealtime();
			break;
		case H:
			isHeader = true;
			break;
		case M:
			isMonitor = true;
			break;
		case SSE:
			isSse = true;
			break;
		default:
			break;
		}

		/**
		 * Add alarm information.
		 * VCID is null for header channels.
		 */
		final byte vcid = (byte) (cv.getVcid() == null ? -1 : cv.getVcid());
		
		// If there are no alarms, we want to create a new empty set.
		final IAlarmValueSet alarms = cv.getAlarms() == null ? alarmSetFactory.create() : cv.getAlarms();
		
		final IGlobalLADData data = new EhaGlobalLadData(
				cv.getChannelType(), // chanType, 
				cv.getChanId(), // channelId, 
				cv.getDn(), // dn, 
				cv.getEu(), // eu, 
				alarms, // alarm set
				isRealtime, // isRealTime, 
				isHeader, 
				isMonitor,
				isSse, 
				isFsw, 
				sclk.getCoarse(), 
				sclk.getFine(), 
				ert.getTime(), 
				ert.getNanoseconds(), 
				scet.getTime(), 
				scet.getNanoseconds(), 
				sid, 
			    appContext.getBean(IContextIdentification.class).getSpacecraftId(),
				appContext.getBean(IVenueConfiguration.class).getVenueType().toString(),
				(byte) cv.getDssId(), 
				vcid, 
				idObj.getHost(),
				cv.getStatus() // status
				);
		
		GlobalLadUserDatatypeConverter.setUserDataTypeFromData(data);

		return data;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.service.AbstractMessageToGlobalLadEventTranslator#convertTo(jpl.gds.shared.message.IMessage)
	 */
	@Override
	public IGlobalLADData convertTo(final IMessage message) throws GlobalLadConversionException {
		try {
			if (message.isType(EvrMessageType.Evr)) {
				return createEvr(message);
			} else if (message.isType(EhaMessageType.AlarmedEhaChannel)) {
				return createEha(message);
			}
		} catch (final GlobalLadConversionException e) {
			throw e;
		} catch (final Exception e) {
			throw new GlobalLadConversionException(e);
		}

		// If we get here the message type is not supported.
		throw new GlobalLadConversionException("Unsupported message type for global lad data conversion: " + message.getType());
	}
}
