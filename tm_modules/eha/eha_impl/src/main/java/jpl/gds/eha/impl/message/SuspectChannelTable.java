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
package jpl.gds.eha.impl.message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.xml.parse.SAXParserPool;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * SuspectChannelTable contains information about channels with suspicious DN/EU
 * values and alarm calculations. It is a DownlinkService implementation that
 * will periodically send out SuspectChannelMessages, so that clients can mark
 * the channels as suspect on displays. The publication interval is
 * configurable.
 * 
 * @see SuspectChannelMessage
 */
class SuspectChannelTable extends DefaultHandler implements StaxSerializable, ISuspectChannelTable {
    
    /**
     * SuspectChannelMode is an enumeration of the types of suspect entries that 
     * can be made on a channel. It may be the DN that is suspect, the EU, or the 
     * alarms.
     * 
     */
    private enum SuspectChannelMode {
        /**
         * Channel DN is suspect.
         */
        DN,
        /**
         * Channel EU is suspect.
         */
        EU,
        /**
         * Channel alarm is suspect.
         */
        ALARM
    }

    
    private Tracer log;  


	private final static String VERSION = "1.0";

	private final ArrayList<String> suspectDnList = new ArrayList<String>();
	private final ArrayList<String> suspectEuList = new ArrayList<String>();
	private final ArrayList<String> suspectAlarmList = new ArrayList<String>();

	private String location = null;

    private String                  environLoc;

    private SseContextFlag          sseFlag;
	

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean init(final ApplicationContext appContext) {
        log = TraceManager.getDefaultTracer(appContext);
        sseFlag = appContext.getBean(SseContextFlag.class);
        environLoc = appContext.getBean(EhaProperties.class).getSuspectChannelFilePath(sseFlag.isApplicationSse());
		final String suspectFile = locateSuspectChannelFile();
		if (suspectFile != null) {
			try {
				parse(suspectFile);
			} catch (final DictionaryException e) {
                TraceManager.getDefaultTracer()
                        .warn("There were problems reading the suspect channel file in " + suspectFile);

				return false;
			}
		}
		return true;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#addSuspectDN(java.lang.String)
     */
	@Override
    public synchronized void addSuspectDN(final String channelId) {
		if (!suspectDnList.contains(channelId)) {
			suspectDnList.add(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#removeSuspectDN(java.lang.String)
     */
	@Override
    public synchronized void removeSuspectDN(final String channelId) {
		if (suspectDnList.contains(channelId)) {
			suspectDnList.remove(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#addSuspectEU(java.lang.String)
     */
	@Override
    public synchronized void addSuspectEU(final String channelId) {
		if (!suspectEuList.contains(channelId)) {
			suspectEuList.add(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#removeSuspectEU(java.lang.String)
     */
	@Override
    public synchronized void removeSuspectEU(final String channelId) {
		if (suspectEuList.contains(channelId)) {
			suspectEuList.remove(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#addSuspectAlarm(java.lang.String)
     */
	@Override
    public synchronized void addSuspectAlarm(final String channelId) {
		if (!suspectAlarmList.contains(channelId)) {
			suspectAlarmList.add(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#removeSuspectAlarm(java.lang.String)
     */
	@Override
    public synchronized void removeSuspectAlarm(final String channelId) {
		if (suspectAlarmList.contains(channelId)) {
			suspectAlarmList.remove(channelId);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#getChannelsWithSuspectDN()
     */
	@Override
    @SuppressWarnings("unchecked")
    public synchronized List<String> getChannelsWithSuspectDN() {
		return (List<String>)suspectDnList.clone();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#getChannelsWithSuspectEU()
     */
	@Override
    @SuppressWarnings("unchecked")
    public synchronized List<String> getChannelsWithSuspectEU() {
		return (List<String>)suspectEuList.clone();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#getChannelsWithSuspectAlarms()
     */
	@Override
    @SuppressWarnings("unchecked")
    public synchronized List<String> getChannelsWithSuspectAlarms() {
		return (List<String>)suspectAlarmList.clone();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#hasSuspectDn(java.lang.String)
     */
	@Override
    public synchronized boolean hasSuspectDn(final String channelId) {
		return suspectDnList.contains(channelId);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#hasSuspectEu(java.lang.String)
     */
	@Override
    public synchronized boolean hasSuspectEu(final String channelId) {
		return suspectEuList.contains(channelId);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#hasSuspectAlarms(java.lang.String)
     */
	@Override
    public synchronized boolean hasSuspectAlarms(final String channelId) {
		return suspectAlarmList.contains(channelId);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#getSuspectAlarmCount()
     */
	@Override
    public synchronized int getSuspectAlarmCount() {
		return suspectAlarmList.size();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#getAllSuspectChannelIds()
     */
	@Override
    public synchronized List<String> getAllSuspectChannelIds() {
		final List<String> result = new ArrayList<String>();
		for (final String chan : suspectDnList) {
			if (!result.contains(chan)) {
				result.add(chan);
			}
		}

		for (final String chan : suspectEuList) {
			if (!result.contains(chan)) {
				result.add(chan);
			}
		}

		for (final String chan : suspectAlarmList) {
			if (!result.contains(chan)) {
				result.add(chan);
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri,
			final String localName,
			final String qname,
			final Attributes attr)
	throws SAXException {
		super.startElement(uri, localName, qname, attr);

        if (qname.equalsIgnoreCase("channel")) {
			final String chan = attr.getValue("channel_id");
			if (chan == null) {
				throw new SAXException("channel_id attribute on channel element in suspect channel table must be supplied");
			}
			final String type = attr.getValue("field");
			if (type == null) {
				throw new SAXException("field attribute on channel element in suspect channel table must be supplied");
			}
			SuspectChannelMode mode = null;
			try {
				mode = Enum.valueOf(SuspectChannelMode.class, type);
			} catch (final Exception e) {
				throw new SAXException("Illegal value for type attribute in channel element in suspect channel table: " + type);
			}
			switch (mode) {
			case DN: 
				this.addSuspectDN(chan);
				break;
			case EU:
				this.addSuspectEU(chan);
				break;
			case ALARM:
				this.addSuspectAlarm(chan);
				break;
			}
		} 
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri,
			final String localName,
			final String qname)
	throws SAXException {
		super.endElement(uri, localName, qname);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#clear()
     */
	@Override
    public synchronized void clear() {
		suspectAlarmList.clear();
		suspectDnList.clear();
		suspectEuList.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	throws XMLStreamException {
		writer.writeStartElement("SuspectChannels"); //<SuspectChannels>
		writer.writeAttribute("version", VERSION);

		for (final String chan : suspectDnList) {
			writer.writeStartElement("channel"); //<channel>
			writer.writeAttribute("channel_id", chan);
			writer.writeAttribute("field", SuspectChannelMode.DN.toString());
			writer.writeEndElement(); //</channel
		}

		for (final String chan : suspectEuList) {
			writer.writeStartElement("channel"); //<channel>
			writer.writeAttribute("channel_id", chan);
			writer.writeAttribute("field", SuspectChannelMode.EU.toString());
			writer.writeEndElement(); //</channel>
		}

		for (final String chan : suspectAlarmList) {
			writer.writeStartElement("channel"); //<channel>
			writer.writeAttribute("channel_id", chan);
			writer.writeAttribute("field", SuspectChannelMode.ALARM.toString());
			writer.writeEndElement(); //</channel>
		}

		writer.writeEndElement(); //</SuspectChannels>
	}

	/**
	 * Updates the given hash map with variable values for velocity output
	 * @param map the hash map to update
	 */
	@Override
    public void setTemplateContext(final Map<String, Object> map) {
	    map.put("version", VERSION);
		map.put("dnList", suspectDnList);
		map.put("euList", suspectEuList);
		map.put("alarmList", suspectAlarmList);
	}

	/**
	 * Parses the given XML File to populate the suspect channel lists.
	 * @param filename the path to the XML file to parse
	 * 
	 * @throws DictionaryException if there is an error parsing the file
	 */
	@Override
    public void parse(final String filename) throws DictionaryException {
		if (filename == null) {
			throw new DictionaryException("Suspect channel file path is undefined.");
		} 
		final File path = new File(filename);
		if (!path.exists()) {
			return;
		}
		log.info("Parsing suspect channel definitions from " +
		        FileUtility.createFilePathLogMessage(path));
		SAXParser sp = null;
		try {
			sp = SAXParserPool.getInstance().getNonPooledParser();
			sp.parse(path, this);
		} catch (final SAXException e) {
			log.error(e.getMessage());
			throw new DictionaryException(e.getMessage(), e);     
		} catch (final ParserConfigurationException e) {
		    final String message = "Unable to configure sax parser to read suspect channel file";
		    log.error(message);
			throw new DictionaryException(message, e);          
		} catch (final Exception e) {
		    final String message = "Unexpected error parsing or reading suspect channel file";
            log.error(message);
			throw new DictionaryException(message, e);
		}
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public String locateSuspectChannelFile() {

		if (location != null) {
			return location;
		}
		
		return environLoc;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#setFileLocation(java.lang.String)
     */
	@Override
    public void setFileLocation(final String path) {
		location = path;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelTable#save()
     */
	@Override
    public void save() throws IOException {
		final String loc = locateSuspectChannelFile();
		if (loc == null) {
			throw new IllegalStateException("Suspect channel file location is undefined");
		}
		final String xml = toXml();
		final FileWriter fw = new FileWriter(loc);
		fw.write("<?xml version=\"1.0\"?>\n");
		fw.write(xml);
		fw.close();
	}

	/**
	 * Retrieves an XML string representation of this object.
	 * @return XML text
	 */
	@Override
	public String toXml() {
		String output = "";
		try
		{
			output = StaxStreamWriterFactory.toPrettyXml(this);
		}
		catch(final XMLStreamException e)
		{
			e.printStackTrace();
			TraceManager.getDefaultTracer().error("Could not transform SuspectChannelTable object to XML: " + e.getMessage());

		}
		return output;
	}

    @Override
    public SseContextFlag getTableSseContextFlag() {
        return sseFlag != null ? sseFlag : new SseContextFlag();
    }

    @Override
    public void setDefaultFileLocation(final String path) {
        if (sseFlag != null ? sseFlag.isApplicationSse() : false) {
            GdsSystemProperties.setSystemProperty(EhaProperties.SSE_SUSPECT_FILE_LOCATION_PROPERTY, path);
        }
        else {
            GdsSystemProperties.setSystemProperty(EhaProperties.SUSPECT_FILE_LOCATION_PROPERTY, path);
        }
    }
}

