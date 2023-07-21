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
package jpl.gds.evr.impl.service.notify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Parser for EVR notification configuration XML files.
 *
 */
public final class EvrNotificationFileParser extends DefaultHandler {

    private static final Tracer logger = TraceManager.getDefaultTracer();

    
	/**
	 * Name of the template file to be used when formatting e-mail messages.
	 * This style will be used if none specified in the destination definition.
	 */
	public static final String DEFAULT_EMAIL_TEMPLATE = "paragraph"
			+ TemplateManager.EXTENSION;
	
    /**
     * Default EVR notify file.
     */
    public static final String DEFAULT_EVR_NOTIFY_FILE = "EvrNotification.xml";

	private EvrNotificationDefinition currentNotification;
	private EvrIdTrigger currentEvrIdTrigger;
	private EvrNameTrigger currentEvrNameTrigger;
	private EvrCategorialTrigger currentEvrCategorialTrigger;
	private final List<EvrNotificationDefinition> notificationList;
	private String host;

	/**
	 * The current value of the character data between tags. This value is first
	 * populated when an opening tag (<tag>) is encountered and then appending
	 * stops when a closing tag is encountered (</tag>).
	 */
	private final StringBuilder currentString;

	private final NotificationProperties notifyProps;

	/**
	 * Default constructor.
	 * @param notifyProps the current NotificationProperties object
	 */
	public EvrNotificationFileParser(final NotificationProperties notifyProps) {
		currentString = new StringBuilder(1024);

		currentNotification = null;
		currentEvrIdTrigger = null;
		currentEvrNameTrigger = null;
		currentEvrCategorialTrigger = null;

		notificationList = new ArrayList<EvrNotificationDefinition>(64);
		this.notifyProps = notifyProps;
	}

	/**
     * This method will find the default notification file (in dictionary
     * directories) and will start the SAX parsing of it.
     * 
     * @param config
     *            the dictionary configuration object
     * @param host
     *            the host name to sue when constructing the notification file name
     * @param sseFlag
     *            The SSE context flag
     * @throws IOException
     *             thrown if file I/O error is encountered
     * @throws ParserConfigurationException
     *             thrown if problem occurs during configuration of the SAX
     *             parser
     * @throws SAXException
     *             thrown if a problem is encountered during parsing
     */
    public void parseConfiguration(final DictionaryProperties config, final String host, final SseContextFlag sseFlag)
            throws IOException,
			ParserConfigurationException, SAXException {
	    this.host = host;

		/*
		 * We look for the EVR notification file in the dictionary directories.
		 */
		String configFileName = HostPortUtility.getLocalHostName() + "_"
				+ EvrNotificationFileParser.DEFAULT_EVR_NOTIFY_FILE;

		try {
			configFileName = config.getDictionaryFile(configFileName);
		} catch (final DictionaryException ex) {
			final String dictDir = sseFlag.isApplicationSse() ? config.getSseDictionaryDir() : config.getFswDictionaryDir();
			final String version = sseFlag.isApplicationSse() ? config.getSseVersion() : config.getFswVersion();

            final String mission = GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse());
			throw new FileNotFoundException(
					"Could not find the EVR notification file in the locations "
							+ (dictDir + File.separator + mission
									+ File.separator + version + File.separator + configFileName)
							+ " or "
							+ (dictDir + File.separator + mission
									+ File.separator + configFileName)

							+ ".");
		}

		/*
		 * Parse the notification file once we know it exists.
		 */
		parseConfiguration(configFileName);
	}

	private void parseConfiguration(final String configFilePath)
			throws IOException, ParserConfigurationException, SAXException {

		if (configFilePath == null) {
			logger.error("EVR notification file path is undefined. Cannot read "
							+ "EVR notifications.");
			throw new FileNotFoundException("EVR notification file path is undefined. Cannot read "
                    + "EVR notifications.");
		}

		final File path = new File(configFilePath);

		if (path.exists() == false) {
			logger.error("EVR notification file " + configFilePath
							+ " does not exist. Cannot read EVR notifications.");
			throw new FileNotFoundException("EVR notification file " + configFilePath
                    + " does not exist. Cannot read EVR notifications.");
		}

		final String message = "Parsing EVR notifications from "
				+  FileUtility.createFilePathLogMessage(path);
		logger.info(message);
	

		SAXParser sp = null;
		try {
			sp = SAXParserPool.getInstance().getNonPooledParser();
			sp.parse(path, this);
		} catch (final IOException e) {
			logger.error(
                    "I/O error encountered while reading EVR notification file "
							+ path.getAbsolutePath());
			throw new SAXException("I/O error encountered while reading EVR notification file "
                    + path.getAbsolutePath(), e);
		} catch (final ParserConfigurationException e) {
			logger.error(
                    "Error configuring SAX parser for EVR notification file "
							+ path.getAbsolutePath());
			throw new SAXException("Error configuring SAX parser for EVR notification file "
                    + path.getAbsolutePath(), e);
		} catch (final Exception e) {
			logger.error(
                    "Error parsing EVR notification file "
							+ path.getAbsolutePath() + ": " + e.getMessage());
			throw new SAXException("Error parsing EVR notification file "
                    + path.getAbsolutePath() + ": " + e.getMessage(), e);
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(final char[] ch, final int start, final int length)
			throws SAXException {
		this.currentString.append(ch, start, length);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {
		currentString.setLength(0);

		if ("Notification".equals(localName))
        {
            final String mode     = StringUtil.emptyAsNull(
                                        attr.getValue("mode"));
            boolean      realtime = false;
            boolean      recorded = false;

            if ((mode == null) || mode.equalsIgnoreCase("all"))
            {
                realtime = true;
                recorded = true;
            }
            else if (mode.equalsIgnoreCase("realtime"))
            {
                realtime = true;
            }
            else if (mode.equalsIgnoreCase("recorded"))
            {
                recorded = true;
            }
            else if (! mode.equalsIgnoreCase("none"))
            {
                throw new SAXException("Attribute Notification.mode is set " +
                                       "to illegal value: '"                 +
                                       mode                                  +
                                       "'");
            }

            this.currentNotification =
                new EvrNotificationDefinition(attr.getValue("name"),
                                              realtime,
                                              recorded);
		}
        else if ("Id".equals(localName)) {
			final int id = XmlUtility.getIntFromAttr(attr, "id");
			addEvrIdAsTrigger(id);
		} else if ("IdRange".equals(localName)) {
			final int begin = XmlUtility.getIntFromAttr(attr, "begin");
			final int end = XmlUtility.getIntFromAttr(attr, "end");
			addEvrIdRangeAsTrigger(begin, end);
		} else if ("Level".equals(localName)) {
			final String level = attr.getValue("level");
			addLevelAsTrigger(level);
		} else if ("Module".equals(localName)) {
			final String module = attr.getValue("module");
			addModuleAsTrigger(module);
		} else if ("OperationalCategory".equals(localName)) {
			final String opscat = attr.getValue("category");
			addOpsCatAsTrigger(opscat);
		} else if ("PhoneNumber".equals(localName)) {
			// TODO: We should only send one SMS message per trigger
			final String number = attr.getValue("number");
			final String provider = attr.getValue("provider");
			currentNotification.addNotificationDestination(new EvrSmsNotifier(notifyProps,
					number, provider, host));
		} else if ("Email".equals(localName)) {
			// TODO: We should only send one email per style
			final String address = attr.getValue("address");
			String style = attr.getValue("style");

			if (style == null) {
				style = DEFAULT_EMAIL_TEMPLATE;
			} else if (style.endsWith(TemplateManager.EXTENSION) == false) {
				style += TemplateManager.EXTENSION;
			}

			currentNotification.addNotificationDestination(new EvrEmailNotifier(
					notifyProps, address, style, host));
		}

	}

	private void addEvrIdAsTrigger(final int id) {

		if (currentEvrIdTrigger == null) {
			currentEvrIdTrigger = new EvrIdTrigger();
		}

		currentEvrIdTrigger.addId(id);
	}

	private void addEvrIdRangeAsTrigger(final int begin, final int end) {

		if (currentEvrIdTrigger == null) {
			currentEvrIdTrigger = new EvrIdTrigger();
		}

		currentEvrIdTrigger.addIdRange(begin, end);
	}

	private void addEvrNamePatternAsTrigger(final String pattern) {

		if (currentEvrNameTrigger == null) {
			currentEvrNameTrigger = new EvrNameTrigger();
		}

		currentEvrNameTrigger.addName(pattern);
	}

	private void addLevelAsTrigger(final String level) {

		if (currentEvrCategorialTrigger == null) {
			currentEvrCategorialTrigger = new EvrCategorialTrigger();
		}

		currentEvrCategorialTrigger.addLevel(level);
	}

	private void addModuleAsTrigger(final String module) {

		if (currentEvrCategorialTrigger == null) {
			currentEvrCategorialTrigger = new EvrCategorialTrigger();
		}

		currentEvrCategorialTrigger.addModule(module);
	}

	private void addOpsCatAsTrigger(final String opscat) {

		if (currentEvrCategorialTrigger == null) {
			currentEvrCategorialTrigger = new EvrCategorialTrigger();
		}

		currentEvrCategorialTrigger.addOperationalCategory(opscat);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {

		if ("NameRegex".equals(localName)) {
			addEvrNamePatternAsTrigger(currentString.toString());
		} else if ("Notification".equals(localName)) {

			if (currentEvrIdTrigger != null) {
				currentNotification.addAlarmTrigger(currentEvrIdTrigger);
			}

			if (currentEvrNameTrigger != null) {
				currentNotification.addAlarmTrigger(currentEvrNameTrigger);
			}

			if (currentEvrCategorialTrigger != null) {
				currentNotification
						.addAlarmTrigger(currentEvrCategorialTrigger);
			}

			notificationList.add(currentNotification);

			currentEvrIdTrigger = null;
			currentEvrNameTrigger = null;
			currentEvrCategorialTrigger = null;
			currentNotification = null;
		}

		currentString.setLength(0);
	}

	/**
	 * Get method to retrieve the list of notifications parsed by the parser.
	 * 
	 * @return returns the notifications list
	 */
	public List<EvrNotificationDefinition> getNotificationList() {
		return notificationList;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	@Override
	public void error(final SAXParseException e) throws SAXException {
		throw new SAXException(
				"Error while parsing EVR notification file, line: "
						+ e.getLineNumber() + " col: " + e.getColumnNumber()
						+ " msg: " + e.getMessage());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	@Override
	public void fatalError(final SAXParseException e) throws SAXException {
		throw new SAXException(
				"Fatal error while parsing EVR notification file, line: "
						+ e.getLineNumber() + " col: " + e.getColumnNumber()
						+ " msg: " + e.getMessage());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	@Override
	public void warning(final SAXParseException e) {
		logger.warn(
                "Warning while parsing EVR notification file, line: "
						+ e.getLineNumber() + " col: " + e.getColumnNumber()
						+ " msg: " + e.getMessage());
	}

}
