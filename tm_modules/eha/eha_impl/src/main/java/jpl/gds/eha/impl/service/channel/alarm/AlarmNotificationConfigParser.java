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
package jpl.gds.eha.impl.service.channel.alarm;

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
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * The parser for an alarm notification XML configuration file.
 *
 */
public final class AlarmNotificationConfigParser extends DefaultHandler
{
	protected static final Tracer trace = TraceManager.getDefaultTracer();

	
    /**
     * name of the default e-mail template for notifications
     */
	public static final String DEFAULT_EMAIL_TEMPLATE = "paragraph" + TemplateManager.EXTENSION;
	
	/**
     * Default alarm notify file.
     */
    public static final String DEFAULT_ALARM_NOTIFY_FILE = "AlarmNotification.xml";
	
	private AlarmNotification currentNotification;

	private AbstractAlarmTrigger currentTrigger;

	private final List<AlarmNotification> notificationList;
	
	private final NotificationProperties notifyProps;

	/**
     * The current value of the character data between tags. This value is first
     * populated when an opening tag (<tag>) is encountered and then appending
     * stops when a closing tag is encountered (</tag>).
     */
    private final StringBuilder currentString;
    
    private String host;

    /**
     * Constructor.
     * 
     * @param notifyProps
     *            notfication properties
     */
    public AlarmNotificationConfigParser(final NotificationProperties notifyProps)
    {
    	this.currentString = new StringBuilder(1024);

    	this.currentNotification = null;
    	this.currentTrigger = null;

    	this.notificationList = new ArrayList<AlarmNotification>(64);
    	
    	this.notifyProps = notifyProps;
    }

	/* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
	@Override
    public void error(final SAXParseException e) throws SAXException {
        throw new SAXException("Parse error in alarm notification config file line " + e.getLineNumber() +
                " col " + e.getColumnNumber() + ": " + e.getMessage());
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
	@Override
    public void fatalError(final SAXParseException e) throws SAXException {
        throw new SAXException("Fatal parse error in alarm notification config file line " + e.getLineNumber() +
                " col " + e.getColumnNumber() + ": " + e.getMessage());
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
	@Override
    public void warning(final SAXParseException e) {
        trace.warn("Parse warning in alarm notification config file line " + e.getLineNumber() +
                " col " + e.getColumnNumber() + ": " + e.getMessage());
    }

    /**
     * Parses the configuration file.
     * 
     * @param config
     *            the current dictionary configuration
     * @param host
     *            the current host name
     * @param sseFlag
     *            the SSE context flag
     * @throws IOException
     *             if there is a problem reading the file
     * @throws ParserConfigurationException
     *             if the parser cannot be created
     * @throws SAXException
     *             if there is an error during parsing
     */
    public void parseConfiguration(final DictionaryProperties config, final String host, final SseContextFlag sseFlag)
            throws IOException, ParserConfigurationException, SAXException
	{
	    this.host = host;

		//this is for if the alarm config file is in the System config dir
		//String defaultConfigFile = GdsSystemProperties.getSystemConfigDir() + File.separator + CONFIG_FILE_NAME;
		
		//this is for if the alarm config file is in the root of the dictionary directory
		String configFileName = HostPortUtility.getLocalHostName() + "_" + DEFAULT_ALARM_NOTIFY_FILE;

		try {
			configFileName = config.getDictionaryFile(configFileName);

		} catch (final DictionaryException ex) {
			final String dictDir = sseFlag.isApplicationSse() ? config.getSseDictionaryDir() : config.getFswDictionaryDir();
			final String version = sseFlag.isApplicationSse() ? config.getSseVersion() : config.getFswVersion();
            final String mission = GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse());
			throw new FileNotFoundException("Could not find the Alarm Notification configuration file in the locations "
					+ (dictDir + File.separator + mission + File.separator + configFileName) + " or " 
					+ (dictDir + File.separator + mission + File.separator + version + File.separator + configFileName) + ".");
		}
		
		//parse the config file once we're sure it exists
		parseConfiguration(configFileName);
	}

	private void parseConfiguration(final String configFilePath) throws IOException, ParserConfigurationException, SAXException
	{
		if(configFilePath == null)
		{
		    final String message = "Alarm notification configuration path is undefined. Not reading alarm notifications.";
        	trace.error(message);
            throw new FileNotFoundException(message);
        }

        final File path = new File(configFilePath);
        if(path.exists() == false)
        {
            final String message = "Alarm notification configuration file " + configFilePath +
                    " does not exist. Not reading alarm notifications.";
            trace.error(message);
            throw new FileNotFoundException(message);
        }

        trace.info("Parsing alarm notifications from " +  FileUtility.createFilePathLogMessage(path));

        SAXParser sp = null;
        try
        {
        	sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(path, this);
        }
        catch (final FileNotFoundException e)
        {
            final String message = "Alarm notification configuration file " + configFilePath +
                    " does not exist. Not reading alarm notifications.";
            trace.error(message);
            throw new SAXException(message,e);
        }
        catch (final IOException e)
        {
            final String message = "IO Error reading alarm notification file " + path.getAbsolutePath();
      	 	trace.error(message);
      	 	throw new SAXException(message,e);
        }
        catch (final ParserConfigurationException e)
        {
            final String message = "Error configuring SAX parser for alarm notification config file " + path.getAbsolutePath();
            trace.error(message);
        	throw new SAXException(message,e);
        }
        catch (final Exception e)
        {
            final String message = "Error parsing alarm notification config file " + path.getAbsolutePath() + ": " + e.getMessage();
            trace.error(message);
            throw new SAXException(message,e);
       }
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException
	{
		this.currentString.append(ch, start, length);
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName, final String qname, final Attributes attr) throws SAXException
	{
		this.currentString.setLength(0);

		if("Notification".equals(localName) == true)
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

			final String name = attr.getValue("name");

			this.currentNotification = new AlarmNotification(name, realtime, recorded);
		}
		else if("ChannelListAlarm".equals(localName) == true)
		{
			final String state = attr.getValue("state").toUpperCase();
			this.currentTrigger = new ChannelListTrigger();
			this.currentTrigger.setState(AlarmNotificationState.valueOf(state));
		}
		else if("ModuleAlarm".equals(localName) == true)
		{
			final String state = attr.getValue("state").toUpperCase();
			this.currentTrigger = new ModuleTrigger();
			this.currentTrigger.setState(AlarmNotificationState.valueOf(state));
		}
		else if("OperationalCategoryAlarm".equals(localName) == true)
		{
			final String state = attr.getValue("state").toUpperCase();
			this.currentTrigger = new OpsCategoryTrigger();
			this.currentTrigger.setState(AlarmNotificationState.valueOf(state));
		}
		else if("SubsystemAlarm".equals(localName) == true)
		{
			final String state = attr.getValue("state").toUpperCase();
			this.currentTrigger = new SubsystemTrigger();
			this.currentTrigger.setState(AlarmNotificationState.valueOf(state));	
		}
		else if("Channel".equals(localName) == true)
		{
			final String id = attr.getValue("id");
			((ChannelListTrigger)this.currentTrigger).addChannelId(id);
		}
		else if("ChannelRange".equals(localName) == true)
		{
			final String start = attr.getValue("start");
			final String end = attr.getValue("end");
			((ChannelListTrigger)this.currentTrigger).addChannelRange(start,end);
		}
		else if("Module".equals(localName) == true)
		{
			final String module = attr.getValue("module");
			((ModuleTrigger)this.currentTrigger).addModule(module);
		}
		else if("OperationalCategory".equals(localName) == true)
		{
			final String category = attr.getValue("category");
			((OpsCategoryTrigger)this.currentTrigger).addCategory(category);
		}
		else if("Subsystem".equals(localName) == true)
		{
			final String subsystem = attr.getValue("subsystem");
			((SubsystemTrigger)this.currentTrigger).addSubsystem(subsystem);
		}
		else if("PhoneNumber".equals(localName) == true)
		{
			//TODO: We should only send one SMS message per trigger
			final String number = attr.getValue("number");
			final String provider = attr.getValue("provider");
			
			this.currentTrigger.addNotifier(new AlarmSmsNotifier(notifyProps, number, provider,host));
		}
		else if("Email".equals(localName) == true)
		{
			//TODO: We should only send one email per style
			final String address = attr.getValue("address");
			String style = attr.getValue("style");
			if(style == null)
			{
				style = DEFAULT_EMAIL_TEMPLATE;
			}
			else if(style.endsWith(TemplateManager.EXTENSION) == false)
			{
				style += TemplateManager.EXTENSION;
			}
			
			this.currentTrigger.addNotifier(new AlarmEmailNotifier(notifyProps, address,style, host));
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qname) throws SAXException
	{
		if("ChannelListAlarm".equals(localName) == true ||
		   "ModuleAlarm".equals(localName) == true ||
		   "OperationalCategoryAlarm".equals(localName) == true ||
		   "SubsystemAlarm".equals(localName) == true)
		{
			this.currentNotification.addAlarmTrigger(this.currentTrigger);
		}
		else if("Notification".equals(localName) == true)
		{
			this.notificationList.add(this.currentNotification);
		}

		this.currentString.setLength(0);
	}

	/**
	 * @return Returns the notificationList.
	 */
	public List<AlarmNotification> getNotificationList()
	{
		return (this.notificationList);
	}
}
