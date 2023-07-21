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
/**
 * File: MessageCaptureHandler.java
 */
package jpl.gds.message.api.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.MessageUtility;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageHeaderMode;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;

/**
 * MessageCaptureHandler manages capture of messages to a file or directory by the monitor.
 * Messages are routed here by the MonitorMessageController class. 
 * This class uses a singleton design pattern.
 */
public final class MessageCaptureHandler implements IMessageServiceListener {
	
    /**
     * CaptureType is an enumeration of the various monitor message capture
     * states.
     */
	public enum CaptureType {
		/**
		 * Write mode: Do not write messages to files
		 */
		WRITE_NONE,
		/**
		 * Write to multiple message files in the same directory.
		 */
		WRITE_DIR,
		/**
		 * Write all messages to a single file.
		 */
		WRITE_FILE;
	}
	
    private final Tracer                  log;


    private Map<IMessageType,String> actualStyles;
    private String lastStyle = "xml";
    private BufferedWriter outputWriter;
    private boolean enableConsole = true;
    private long receiveCount = 0;
    private boolean shutdown = false;
	private CaptureType writeMode = CaptureType.WRITE_NONE;
	private List<IMessageType> messageFilters;
	private String defStyle;
	private File messageOutput;
	private MessageHeaderMode headerMode = MessageHeaderMode.HEADERS_OFF;

	private final ApplicationContext appContext;
    private final IExternalMessageUtility externalMessageUtil;
    private final Object processingLock = new Object();


    /**
     * Creates an instance of MessageCaptureHandler. 
     * @param appContext the current application context
     */
    public MessageCaptureHandler(final ApplicationContext appContext) {
    	this.appContext = appContext;
        log = TraceManager.getDefaultTracer(appContext);
        externalMessageUtil = this.appContext.getBean(IExternalMessageUtility.class);
    }

    /**
     * Enables or disables console output. 
     * 
     * @param enable true to enable, false to not
     */
    public void setEnableConsole(final boolean enable) {
        this.enableConsole = enable;
    }
    
	/**
	 * Retrieves the write mode.
	 * 
	 * @return the writeMode as one of the CaptureType constants.
	 */
	public CaptureType getWriteMode() {
		return writeMode;
	}

	/**
	 * Sets the value of the write mode member.
	 * 
	 * @param writeMode the writeMode to set, as one of the CaptureType
	 *            constants.
	 */
	public void setWriteMode(final CaptureType writeMode) {
		this.writeMode = writeMode;
		if (writeMode != CaptureType.WRITE_NONE) {
		    enableConsole = false;
		}
	}
    
	/**
	 * Sets the output directory for writing messages and the write mode to
	 * WRITE_DIR.
	 * 
	 * @param dirName the name of the directory
	 * @return true if directory is valid; false otherwise
	 */
	public boolean setOutputDir(final String dirName) {
		final File output = new File(dirName);
		if (!output.exists() || !output.canWrite()) {
			return false;
		}
		setWriteMode(CaptureType.WRITE_DIR);
		setMessageOutput(output);
		return true;
	}

	/**
	 * Sets the output filename for writing messages and the write mode to
	 * WRITE_FILE.
	 * 
	 * @param fileName the name of the file
	 * @return true if fileName is valid; false otherwise
	 */
	public boolean setOutputFile(final String fileName) {
		final File output = new File(fileName);
	
		if (output.exists() && !output.isFile()) {
			return false;
		}
		setWriteMode(CaptureType.WRITE_FILE);
		setMessageOutput(output);
		return true;
	}
	
	/**
	 * Sets desired message types (message filter) from a comma separated string
	 * of types. This list will be used to filter messages for the given
	 * subscriber.
	 * 
	 * @param types the list of message types to capture; may be null to capture everything
	 */
	public synchronized void setCaptureMessageFilter(final String types) {
		if (types != null) {
			final StringTokenizer tokens = new StringTokenizer(types, ",");
			messageFilters = new ArrayList<>();
			while (tokens.hasMoreTokens()) {
			    final String temp = tokens.nextToken().trim();
			    final IMessageConfiguration config = MessageRegistry.getMessageConfig(temp);
			    if (config != null) {
			        messageFilters.add(config.getMessageType());
			    } else {
			        log.warn("Unrecognized message type requested for capture: " + temp + ". Ignored.");
			    }
			}
		} else {
			messageFilters = null;
		}
	}
	
	/**
	 * Returns the current list of message types used for filtering captured
	 * messages.
	 * 
	 * @return Vector of message types
	 */
	public List<IMessageType> getCaptureMessageFilters() {
		return messageFilters;
	}

	/**
	 * Returns the current list of message types used for filtering captured
	 * messages as an array of strings.
	 * 
	 * @return array of message types, or null if all messages should be
	 *         captured (no filter)
	 */
	public String[] getCaptureMessageFiltersAsStrings() {
		if (messageFilters == null) {
			return new String[] {};
		}
		final String[] result = new String[messageFilters.size()];
		int i = 0;
		for (final IMessageType mt: messageFilters) {
		    result[i++] = mt.getSubscriptionTag();
		}
		return result;
	}
	

	/**
	 * Sets the header mode, indicating whether message header properties should be
	 * written along with message text.
	 * 
	 * @param mode HEADERS_ON or HEADERS_OFF
	 */
	public void setHeaderMode(final MessageHeaderMode mode) {
		headerMode = mode;
	}

	/**
	 * Returns the current header mode.
	 * 
	 * @return the current header mode: HEADERS_ON or HEADERS_OFF
	 */
	public MessageHeaderMode getHeaderMode() {
		return headerMode;
	}

	/**
	 * Retrieves the default message formatting style for captured messages.
	 * 
	 * @return the style as a String
	 */
	public String getCaptureMessageStyle() {
		return defStyle;
	}

	/**
	 * Sets the value of the default message formatting style for captured
	 * messages.
	 * 
	 * @param defStyle the style to set, as a String.
	 */
	public void setCaptureMessageStyle(final String defStyle) {
		this.defStyle = defStyle.toLowerCase();
	}

	/**
	 * Retrieves the message output File member. The File object may represent
	 * either a directory or a file. If the writeMode member is WRITE_NONE, this
	 * method may return null.
	 * 
	 * @return the messageOutput as a File
	 */
	public File getMessageOutput() {
		return messageOutput;
	}

	/**
	 * Sets the value of the message output file member. The File object may
	 * represent either a directory or a file.
	 * 
	 * @param messageOutput the messageOutput to set, as a File.
	 */
	public void setMessageOutput(final File messageOutput) {
		/*
		 * Whenever the messageOutput is updated, check to see if there is currently
		 * an open outputWriter. If so, check to see if the messageOutput has actually changed.
		 * If it has changed, then close and null out the outputWriter so that it will
		 * be recreated when next activated.
		 */
		if (null != this.outputWriter) {
			if ((null != messageOutput) && !messageOutput.equals(this.messageOutput)) {
				this.messageOutput = messageOutput;
				this.outputWriter = null;
			}
		}
		else {
			this.messageOutput = messageOutput;
		}
	}
	
    /**
     * 
     * Gets the total count of messages received by this object.
     * 
     * @return the receive count
     */
    public long getReceiveCount()
    {
    	return receiveCount;
    }

    /**
     * Gets the message style to be used for the given message type when formatting 
     * message text.
     * 
     * @param type the message type
     * @return the message formatting style
     */
    private String getStyle(final IMessageType type) {
        // Create the style table to first time through, or if the
        // user has changed the default style
        final String curStyle = getCaptureMessageStyle();
        if (curStyle == null) {
            return curStyle;
        }
        if (!curStyle.equals(lastStyle) || actualStyles == null) {
            actualStyles = new HashMap<>();
            lastStyle = curStyle;
        }
        String style = actualStyles.get(type);
        if (style == null) {
            final List<String> v = MessageUtility.getMessageStyles(type);
            style = curStyle;
            if (v.contains(style)) {
                actualStyles.put(type, style);
            } else {
                style = "xml";
                actualStyles.put(type, style);
            }
        }
        return style;
    }

    /**
     * Indicates if the given message type is one that should be displayed.
     * 
     * @param type the internal message type
     * @return true if the message type is selected for monitoring
     */
    public boolean isSelectedType(final IMessageType type) {
        final List<IMessageType> list = getCaptureMessageFilters();
        if (list == null || list.isEmpty()) {
            return true;
        }
        return list.contains(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void onMessage(final IExternalMessage m) {
        try {
            
        	// Do nothing is capture handler has been shut down
        	if (shutdown) {
        		return;
        	}
        	
        	// Increment the total message count
        	receiveCount++;
        	
        	// If console not enabled and no file capture enabled, do nothing.
        	if (!enableConsole && writeMode.equals(CaptureType.WRITE_NONE)) {
        		return;
        	}
           
        	// Get the message type and process the message if it is a type we are interested in
            final IMessageType type = externalMessageUtil.getInternalType(m);
            if (!isSelectedType(type)) {
                return;
            }

            /** 
             * Synchronize here rather than on the whole method for performance reasons. The vast majority
             * of the time we do not even make it to this point.
             */
            synchronized (processingLock) {
            
                final String style = getStyle(type);

                final IMessage[] internalMessages = externalMessageUtil.instantiateMessages(m);

                // Write the message to the console if enabled
                if (enableConsole) {
                    System.out.print(externalMessageUtil.getHeaderText(m, getHeaderMode()));
                    System.out.print(MessageUtility.getMessageText(style, internalMessages));
                }
                // Write the data to the capture file if so configured
                if (!writeMode.equals(CaptureType.WRITE_NONE)) {

                    // Writing all messages to a single file
                    if (writeMode.equals(CaptureType.WRITE_FILE) &&
                            outputWriter == null) {
                        outputWriter = new BufferedWriter(new FileWriter(getMessageOutput()));
                        if (defStyle != null && defStyle.equalsIgnoreCase("xml")) {
                            outputWriter.write("<?xml version=\"1.0\"?>\n");
                        }
                    }

                    // Writing each message to a separate file in a directory
                    if (writeMode.equals(CaptureType.WRITE_DIR)) {
                        final File f = new File(getMessageOutput(), externalMessageUtil.getMessageId(m));
                        final BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                        if (getCaptureMessageStyle().equalsIgnoreCase("xml")) {
                            writer.write("<?xml version=\"1.0\"?>\n");
                        }
                        writer.write(externalMessageUtil.getHeaderText(m, getHeaderMode()));
                        writer.write(MessageUtility.getMessageText(style, internalMessages));
                        writer.close();

                    } else {
                        outputWriter.write(externalMessageUtil.getHeaderText(m, getHeaderMode()));
                        outputWriter.write(MessageUtility.getMessageText(style, internalMessages));
                        outputWriter.flush();
                    }
                }
                System.out.flush();
            }
            
        } catch (final MessageServiceException e) {
            log.error("Error receiving message: " + e.getMessage());
        } catch (final IOException e) {
            log.error("Error writing message to file: " + e.getMessage());
        }
    }

    /**
     * Stops the message capture.
     */
    public void shutdown() {
    	shutdown = true;
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
