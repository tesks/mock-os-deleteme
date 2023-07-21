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
package jpl.gds.shared.message;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * BaseXmlMessageParseHandler is a base class for performing parsing of XML messages
 * using SAX. It provides common functionality and convenience methods to its
 * subclasses.
 */
public abstract class BaseXmlMessageParseHandler extends DefaultHandler implements IXmlMessageParseHandler {
    private static Tracer log = TraceManager.getDefaultTracer();


    private static DateFormat dateParser = TimeUtility.getFormatter();
    /**
     * All parsed messages.
     */
    private List<IMessage> messages;
    /**
     * Buffer used to build messages.
     */
    protected StringBuilder buffer;
    
    private ISerializableMetadata contextHeader;
    
    private boolean inMessage;
   
    /**
     * Latest message.
     */
    
    protected final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();
    private IMessage currentMessage = null;
    
    /**
     * Indicates if currently parsing XML context.
     */
    protected boolean inContext = false;
    
    @Override
    public IMessage[] parse(final String xml) throws XmlMessageParseException {
        final SAXParserPool pool = SAXParserPool.getInstance();
        SAXParser sp = null;
        StringReader sr = null;
        try {
            sp = pool.get();
            sr = new StringReader(xml);
            sp.parse(new InputSource(sr), this);
            sr.close();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new XmlMessageParseException("Error parsing XML message", e);
        } finally {
            if (sp != null) {
                pool.release(sp);
            }
            if (sr != null) {
                sr.close();
            }

        }
        return getMessages();
    }

    /**
     * Sets the flag indicating that parsing is currently within the XML block for
     * the message itself.
     * 
     * @param in true if in a message, false if not
     */
    protected void setInMessage(final boolean in) {
    	this.inMessage = in;
    }

    /**
     * Sets the message object that resulted from the parse. Also sets the
     * current message. Usually SessionBasedMessage, but Message is for
     * ExitPerspectiveMessage which is now a Message and not a
     * SessionBasedMessage.
     *
     * @param m the message to set
     */
    protected void addMessage(final IMessage m)
    {
        if (this.messages == null)
        {
            this.messages = new ArrayList<IMessage>();
        }

        this.messages.add(m);

        this.currentMessage = m;
        
        if (this.contextHeader != null) {
            if (!this.contextHeader.isIdOnly()) {
            	this.currentMessage.setContextHeader(this.contextHeader);
            }
            this.currentMessage.setContextKey(this.contextHeader.getContextKey());
        }
    }


    /**
     * Clears the current message and metadata context header.
     */
    protected void clearCurrentMessage() {
        this.currentMessage = null;
        this.contextHeader = null;
    }

    /**
     * Overrides the characters() method in DefaultHandler. Appends parsed text
     * to the text buffer.
     * @param chars the characters from the XML document
     * @param start the start position in the array
     * @param length the number of characters to read from the array
     * @throws SAXException if encountered
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start,
            final int length) throws SAXException {

        if (this.buffer == null) {
            newBuffer();
        }
        final String newText = new String(chars, start, length);

        /*
         * Code here was throwing out
         * the new text if it was just a line feed. The problem with that
         * is that it can throw out a line feed anywhere in the content, 
         * not just a completely blank line, because this method is not called
         * for a whole line at a time by the SAX parser, but rather for chunks
         * of text.  AMPCS messages should not be generated with blank lines 
         * in XML elements, so the code to remove them should not be necessary.
         */
         this.buffer.append(newText);
         
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attr) throws SAXException {
        
    	newBuffer();
    	
    	if (localName.equals(ISerializableMetadata.CONTEXT_ROOT_XML_TAG) && inMessage) {
    	    this.inContext = true;
    	    try {
    	        this.contextHeader =  new MetadataMap(attr.getValue("id"));
    	      
    	    } catch (final IllegalArgumentException e) {
    	        throw new SAXException("unrecognized header context type " + attr.getValue("type"));
    	    }
    	    if (this.contextHeader != null) {
    	        this.contextHeader.parseFromXmlElement(localName, attr);
    	    }
    	} else if (this.inContext) {
    	    if (this.contextHeader != null) {
    	        this.contextHeader.parseFromXmlElement(localName, attr);
    	    }
    	}

    }
    
    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
    	
        if (localName.equals(ISerializableMetadata.CONTEXT_ROOT_XML_TAG) && inMessage) {
        	if (this.contextHeader != null && this.currentMessage != null) {
        		this.currentMessage.setContextKey(this.contextHeader.getContextKey());
        		if (!this.contextHeader.isIdOnly()) {
      	            this.currentMessage.setContextHeader(this.contextHeader);
      	        }
        	}
            this.inContext = false;
            
        } else if (qName.equalsIgnoreCase("fromSSE")) {
            if (this.currentMessage != null)
            {
                this.currentMessage.setFromSse(Boolean.valueOf(getBufferText()));
            }
        } 
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#error(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * Converts the current contents of the text buffer into an IAccurateDateTime
     * object.
     * @return the IAccurateDateTime, or null if the text buffer is empty
     */
    protected IAccurateDateTime getAccurateDateFromBuffer() {
        final String timeStr = getBufferText();
        IAccurateDateTime d = null;

        if (timeStr != null) {
            try {
                d = new AccurateDateTime(timeStr);
            } catch (final ParseException e) {
                log.error("Failed to format date string to date object"
                        + " using ISO format", e);
                d = null;
            }
        }

        return (d);
    }

    /**
     * Converts the current contents of an XML attribute into a boolean.
     * @param attr the SAX Attributes object as passed to the startElement()
     *        method by the parser
     * @param attrName the name of the attribute to be converted
     * @return the boolean value, or false if the attribute does not exist
     */
    protected boolean getBooleanFromAttr(final Attributes attr,
            final String attrName) {
        final String boolStr = attr.getValue(attrName);
        boolean b = false;
        if (boolStr != null) {
            b = Boolean.valueOf(boolStr);
        }
        return b;
    }

    /**
     * Converts the current contents of the text buffer into an integer.
     * @return the boolean value, or false if the text buffer is empty
     */
    protected boolean getBooleanFromBuffer() {
        final String boolStr = getBufferText();
        boolean b = false;
        if (boolStr != null) {
            b = Boolean.valueOf(boolStr);
        }
        return b;
    }

    /**
     * Returns the current content on the text buffer as a String.
     * @return the text String
     */
    protected String getBufferText() {
        if (this.buffer == null) {
            return null;
        }
        return this.buffer.toString().trim();
    }

    /**
     * Gets the message current being constructed.
     * @return the current message
     */
    protected IMessage getCurrentMessage() {
        return this.currentMessage;
    }

    /**
     * Converts the current contents of an XML attribute into a Date object.
     * @param attr the SAX Attributes object as passed to the startElement()
     *        method by the parser
     * @param attrName the name of the attribute to be converted
     * @return the Date object, or null if the attribute does not exist or
     *         cannot be parsed
     */
    protected IAccurateDateTime getDateFromAttr(final Attributes attr,
            final String attrName) {
        final String dateStr = attr.getValue(attrName);
        IAccurateDateTime d = null;

        if (dateStr != null) {
            try {
                synchronized (dateParser) {
                    d = new AccurateDateTime(dateParser.parse(dateStr));
                }
            } catch (final ParseException e) {
                log.error("Failed to format date string to date object"
                        + " using ISO format", e);
                d = null;
            }
        }

        return (d);
    }

    /**
     * Converts the current contents of the text buffer into a Date object.
     * @return the Date, or null if the text buffer is empty
     */
    protected IAccurateDateTime getDateFromBuffer() {
        final String timeStr = getBufferText();
        IAccurateDateTime d = null;
        final DateFormat df = TimeUtility.getFormatterFromPool();
        if (timeStr != null) {
            try {
                d = new AccurateDateTime(df.parse(timeStr));
            } catch (final ParseException e) {
                log.error("Failed to format date string to date object"
                        + " using ISO format", e);
                d = null;
            }
        }

        TimeUtility.releaseFormatterToPool(df);
        return (d);
    }

    /**
     * Converts the current contents of an XML attribute into an integer.
     * @param attr the SAX Attributes object as passed to the startElement()
     *        method by the parser
     * @param attrName the name of the attribute to be converted
     * @return the integer value, or 0 if the attribute does not exist
     */
    protected int getIntFromAttr(final Attributes attr, final String attrName) {
        final String intStr = attr.getValue(attrName);
        int i = 0;
        if (intStr != null) {
            i = Integer.parseInt(intStr);
        }
        return i;
    }

    /**
     * Converts the current contents of the text buffer into an integer.
     * @return the int value, or 0 if the text buffer is empty
     */
    protected int getIntFromBuffer() {
        final String intStr = getBufferText();
        int i = 0;
        if (intStr != null) {
            i = Integer.parseInt(intStr);
        }
        return i;
    }

    /**
     * Converts the current contents of the text buffer into an ILocalSolarTime
     * object.
     * @return the ILocalSolarTime, or null if the text buffer is empty
     */
    protected ILocalSolarTime getLocalSolarTimeFromBuffer() {
        final String timeStr = getBufferText();
        ILocalSolarTime d = null;

        if (timeStr != null) {
            try {
                d = new LocalSolarTime(timeStr);
            } catch (final ParseException e) {
                log.error("Failed to format date string to local solar"
                        + " time object using ISO format", e);
                d = null;
            }
        }

        return (d);
    }

    /**
     * Converts the current contents of an XML attribute into a long.
     * @param attr the SAX Attributes object as passed to the startElement()
     *        method by the parser
     * @param attrName the name of the attribute to be converted
     * @return the integer value, or 0 if the attribute does not exist
     */
    protected long getLongFromAttr(final Attributes attr,
            final String attrName) {
        final String intStr = attr.getValue(attrName);
        long i = 0;
        if (intStr != null) {
            i = Long.parseLong(intStr);
        }
        return i;
    }

    /**
     * Converts the current contents of the text buffer into a long.
     * @return the long value, or 0 if the text buffer is empty
     */
    protected long getLongFromBuffer() {
        final String longStr = getBufferText();
        long i = 0;
        if (longStr != null) {
            i = Long.parseLong(longStr);
        }
        return i;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.IXmlMessageParseHandler#getMessages()
     */
    @Override
    public IMessage[] getMessages() {
        if (this.messages == null) {
            return null;
        }
        final IMessage[] result = new IMessage[this.messages.size()];
        return this.messages.toArray(result);
    }

    /**
     * Converts the current contents of the text buffer into a ISclk object.
     * @return the Sclk, or null if the text buffer is empty
     */
    protected ISclk getSclkFromBuffer() {
        final String sclkStr = this.buffer.toString().trim();
        ISclk s = null;
        if (sclkStr != null) {
        	s = sclkFmt.valueOf(sclkStr);
        }
        return s;
    }

    /**
     * Starts a new test buffer, which captures text parsed from XML element
     * values.
     */
    protected void newBuffer() {
        this.buffer = new StringBuilder();
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#warning(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
        log.warn("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
    }
    
}
