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
package jpl.gds.telem.input.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.impl.message.RawSfduPktMessage;
import jpl.gds.telem.input.impl.message.RawSfduTfMessage;

/**
 * PVLInfo holds the information contained within a PVL file. Most of the data
 * is held "as-is", or as it was read from the PVL file. Each line of the PVL
 * file ends with a semicolon, therefore not being quite even with .properties
 * standard. If the first time in the time_range is not "NOW" and the format is
 * ERT, then it will be kept up to date. This is accomplished by subscribing to
 * SFDU_PKT and SFDU_TF data messages. As SFDU messages are received, their time
 * is extracted and used to update the time. While other time formats are
 * supported for use in PVL files, ERT is the only time format that can be
 * quickly and easily read from the SFDU messages.
 * 
 * 
 *
 */
public class PVLInfo {

	/**
	 * Format for time used in PVL files. TDS supports up to seven subsecond digits
	 * of accuracy
	 */
	public static final String ERT_TIME_FORMAT = "YYYY/D-HH:mm:ss.SSSeeee";

	public static final String TIME_QUERY_PARAMETER_NAME = "TIME_RANGE";

	private List<String> PVLFile = new ArrayList<>();

	private int timeLineNumber = -1;
	private String timeRangeFrom;
	private String timeRangeUntil;
	private String timeRangeType;

	private MessageSubscriber subscriber;

	private final ApplicationContext appContext;

	/**
	 * Constructor. Takes the name of a file, with path, which is opened and used to
	 * populate the values
	 * 
	 * @param appContext    the current application context
	 * @param pvlFileString the filepath to a PVL file
	 * @throws IOException If there was an issue with the pvlFile
	 */
	public PVLInfo(final String pvlFileString, final ApplicationContext appContext) throws IOException {
		this.appContext = appContext;

		if (!(pvlFileString == null || pvlFileString.isEmpty())) {
			setFromFile(pvlFileString);
		}

	}

	/**
	 * Get the TIME_RANGE value of the PVL file
	 * 
	 * @return the TIME_RANGE value as a string
	 */
	public String getTimeRange() {
		return "{ " + this.timeRangeFrom + " .. " + this.timeRangeUntil + " } " + this.timeRangeType;
	}

	/**
	 * Set a new "from" time in the TIME_RANGE if the from time is not NOW. If the
	 * until time is not FOREVER the from time will only go up to the until time.
	 * While this can be used with any time format, it should only be done with ERT
	 * 
	 * @param newTimeRangeFrom the new from time to be saved
	 */
	private void updateTimeRangeFrom(final String newTimeRangeFrom) {
		final String updateTime = formatTimeRange(newTimeRangeFrom);
		if (!this.timeRangeFrom.equalsIgnoreCase("NOW") && updateTime.compareTo(this.timeRangeFrom) > 0
				&& updateTime.compareTo(this.timeRangeUntil) <= 0) {
			this.timeRangeFrom = updateTime;
		}
		if (!this.timeRangeUntil.equalsIgnoreCase("FOREVER") && updateTime.compareTo(this.timeRangeUntil) > 0) {
			this.timeRangeFrom = this.timeRangeUntil;
		}
		
		this.PVLFile.remove(timeLineNumber);
		this.PVLFile.add(timeLineNumber, this.buildLine(TIME_QUERY_PARAMETER_NAME, getTimeRange()));
	}

	/**
	 * Get the PVL file as an InputStream
	 * 
	 * @return the PVL file as a string in an InputStream
	 */
	public InputStream toDataStream() {
		return new ByteArrayInputStream(this.toString().getBytes());
	}

	/**
	 * Convert this PVLInfo object to the equivalent String value.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (String line : this.PVLFile) {
			sb.append(buildLine(line));
		}

		return sb.toString();
	}

	/**
	 * Get the String value of one line
	 * 
	 * @param entryLine the line to be read
	 * @return the value stored on the given line
	 */
	private String getValue(final String entryLine) {
		final String[] entryLineSplit = entryLine.split("=");

		// less than 2 means there was no =, or one side or the other did not have
		// value. there should not be more than one =. If there is, the there's a
		// problem with the line
		if (entryLineSplit.length != 2) {
			return "";
		}

		String retVal = entryLineSplit[1].trim();

		// ; should be the end of the line and TDS treats it as such. anyting after it
		// will be treated like a comment.
		final int endIndex = retVal.indexOf(';');

		if (endIndex >= 0) {
			retVal = retVal.substring(0, endIndex).trim();
		}

		return retVal;
	}

	/**
	 * Opens and reads the file. If any individual line in the file corresponds to a
	 * property stored in PVLInfo, it is parsed and saved
	 * 
	 * @param pvlFileString the filepath to the initial PVL file
	 * @throws IOException if there was an error opening or reading the PVL file
	 */
	private void setFromFile(final String pvlFileString) throws IOException {
		
		String lineIn;
		try (BufferedReader pvlBuffer = new BufferedReader(new FileReader(new File(pvlFileString)))){
			while ((lineIn = pvlBuffer.readLine()) != null) {

				lineIn = lineIn.trim();

				if (lineIn.startsWith(TIME_QUERY_PARAMETER_NAME)) {

					parseAndSetTimeRange(getValue(lineIn));
					if (this.timeRangeFrom.equalsIgnoreCase("NOW")) {
						TraceManager.getDefaultTracer().info(
								"The supplied PVL has a starting time of NOW. The from time will not be updated as data is received.");

					} else if (this.timeRangeType.contains("ERT")) {
						subscriber = new PvlSfduSubscriber();
					} else {
						TraceManager.getDefaultTracer()
								.info("The supplied PVL uses the time range " + this.timeRangeType.replaceAll("\\;", "")
										+ ", which is not supported for range updates");

					}
				} else {
					this.PVLFile.add(lineIn);
				}
			}
		} catch (final FileNotFoundException e1) {
			TraceManager.getDefaultTracer().warn("Unable to open the PVL file " + pvlFileString
					+ ". Cannot populate PVL data to be sent to TDS server for data query.");

			throw e1;
		} catch (final IOException e) {
			TraceManager.getDefaultTracer().warn("There was an error reading from the PVL file " + pvlFileString
					+ " . Please verifty the file is valid and try again.");

			throw e;
		}
	}

	/**
	 * Parses out the from, until, and type values stored in the TIME_RANGE value
	 * and sets them in the appropriate PVLInfo values.
	 * 
	 * @param timeRangeValues the TIME_RANGE value from the PVL file
	 */
	private void parseAndSetTimeRange(final String timeRangeValues) {
		final int timeFromStart = timeRangeValues.indexOf("{") + 1;
		final int timeFromEnd = timeRangeValues.indexOf(".");
		final int timeUntilStart = timeRangeValues.lastIndexOf(".") + 1;
		final int timeUntilEnd = timeRangeValues.indexOf("}");
		this.timeLineNumber = this.PVLFile.size();

		if (timeFromStart > -1 && timeFromEnd > timeFromStart && timeUntilStart > timeFromEnd
				&& timeUntilEnd > timeUntilStart) {
			this.timeRangeFrom = timeRangeValues.substring(timeFromStart, timeFromEnd).trim();
			this.timeRangeUntil = timeRangeValues.substring(timeUntilStart, timeUntilEnd).trim();
			this.timeRangeType = timeRangeValues.substring(timeUntilEnd + 1).trim();
			if (this.timeRangeType.contains("ERT")) {
				this.timeRangeFrom = formatTimeRange(timeRangeFrom);
				this.timeRangeUntil = formatTimeRange(timeRangeUntil);
			}
		}

		this.PVLFile.add(this.buildLine(TIME_QUERY_PARAMETER_NAME, getTimeRange()));

	}

	/**
	 * Corrects the given time value to the time range values used in PVLInfo, if
	 * possible
	 * 
	 * @param startingValue the given time value
	 * @return the formatted time value
	 */
	protected String formatTimeRange(final String startingValue) {
		String retVal = startingValue;
		// MPCS-8723  03/16/17 - adjusted to handle any precision decimal place value
		if (retVal.matches("[0-9]{4}\\/[0-9]{1,3}\\-[0-9]{2}\\:[0-9]{2}\\:[0-9]{2}(\\.[0-9]{0,}){0,1}")) {
			// more than 7 or more decimal places, truncate to 7
			if (retVal.indexOf('.') >= retVal.length() - 8) {
				retVal = retVal.substring(0, retVal.indexOf('.') + 8);
			}
			// no decimal, add and bring up to 7
			else if (retVal.indexOf('.') == -1) {
				retVal += ".0000000";
			}
			// somewhere between 1 and 6, add necessary zeroes
			else {
				final int addPadding = 7 - (retVal.length() - (retVal.indexOf('.') + 1));
				final StringBuilder builder = new StringBuilder(retVal);
				for (int i = 0; i < addPadding && i < 7; i++) {
					builder.append("0");
				}

				retVal = builder.toString();
			}
		}

		else if (!("NOW".equalsIgnoreCase(retVal) || "FOREVER".equalsIgnoreCase(retVal))) {
			TraceManager.getDefaultTracer()
					.info("The PVL time range formatting is not as expected, there may be errors.");

		}

		return retVal;
	}

	/**
	 * Builds a line of the PVL as a String when the given line is not displayed
	 * with a key. (Header and footer)
	 * 
	 * @param value1 the value to be put on a line by itself
	 * @return the formatted line
	 */
	private String buildLine(final String value1) {
		final StringBuilder retval = new StringBuilder();

		retval.append(value1);
		retval.append(System.lineSeparator());

		return retval.toString();
	}

	/**
	 * Builds a line of the PVL as a String when the given line has a key and a
	 * single value
	 * 
	 * @param key   the String representation of the key for the line
	 * @param value the String representation of the value for the line
	 * @return the formatted line with both key and value added
	 */
	private String buildLine(final String key, final String value) {
		StringBuilder retval = new StringBuilder();
		retval.append(key);
		retval.append(" = ");
		retval.append(value);
		retval.append(";");

		return retval.toString();
	}

	/**
	 * Restarts PVLInfo's subscription service. If this object does not have any
	 * subscriptions, then they are started.
	 */
	public void restartSubscriber() {
		if (subscriber != null) {
			shutdownSubscriber();
		}
		if (!this.timeRangeFrom.equalsIgnoreCase("NOW") && this.timeRangeType.contains("ERT")) {
			subscriber = new PvlSfduSubscriber();
		}
	}

	/**
	 * Turns off the subscriptions service for the PVLInfo object.
	 */
	public void shutdownSubscriber() {
		if (subscriber != null) {
			appContext.getBean(IMessagePublicationBus.class).unsubscribeAll(subscriber);
			subscriber = null;
		}
	}

	/**
	 * Subscriber for this PVLInfo object. It subscribes to the RawSfduPktMessages
	 * and RawSfduTfMessages. When these are received, the TimeRangeFrom time is
	 * updated according to the ERT time stored in the metadata of the message
	 * 
	 *
	 */
	private class PvlSfduSubscriber extends BaseMessageHandler {
		public PvlSfduSubscriber() {
			final IMessagePublicationBus bus = appContext.getBean(IMessagePublicationBus.class);
			final TelemetryInputProperties telemConfig = appContext.getBean(TelemetryInputProperties.class);

			bus.subscribe(telemConfig.getRawDataMessageSubscriptionType(StreamType.SFDU_TF), this);
			bus.subscribe(telemConfig.getRawDataMessageSubscriptionType(StreamType.SFDU_PKT), this);
		}

		@Override
		public void handleMessage(final IMessage message) {
			if (message.isType(InternalTmInputMessageType.RawSfduPkt)) {
				updateTimeRangeFrom(
						((RawSfduPktMessage) message).getMetadata().getErt().formatCustom(PVLInfo.ERT_TIME_FORMAT));
			} else if (message.isType(InternalTmInputMessageType.RawSfduTf)) {
				updateTimeRangeFrom(
						((RawSfduTfMessage) message).getMetadata().getErt().formatCustom(PVLInfo.ERT_TIME_FORMAT));
			} else if (message.isType(CommonMessageType.EndOfData)) {
				appContext.getBean(IMessagePublicationBus.class).unsubscribeAll(this);
			}
		}
	}
}