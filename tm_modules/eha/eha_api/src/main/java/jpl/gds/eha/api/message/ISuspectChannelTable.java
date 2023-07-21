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
package jpl.gds.eha.api.message;

import java.io.IOException;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * An interface to be implemented by suspect channel tables, which track
 * channels that have been identified as unreliable.
 * 
 * @since R8
 */
public interface ISuspectChannelTable extends Templatable, StaxSerializable {

    /**
     * Initializes the table.
     * 
     * @param appContext
     *            the current application context
     * 
     * @return true if the operation was successful, false if not
     */
    public boolean init(ApplicationContext appContext);

    /**
     * Add a channel to the list of channels with suspicious DN values.
     * @param channelId the channel identifier
     */
    public void addSuspectDN(String channelId);

    /**
     * Removes a channel from the list of channels with suspicious DN values.
     * @param channelId the channel identifier
     */
    public void removeSuspectDN(String channelId);

    /**
     * Add a channel to the list of channels with suspicious EU values.
     * @param channelId the channel identifier
     */
    public void addSuspectEU(String channelId);

    /**
     * Remove a channel from the list of channels with suspicious EU values.
     * @param channelId the channel identifier
     */
    public void removeSuspectEU(String channelId);

    /**
     * Add a channel to the list of channels with suspicious alarms.
     * @param channelId the channel identifier
     */
    public void addSuspectAlarm(String channelId);

    /**
     * Remove a channel from the list of channels with suspicious alarms.
     * @param channelId the channel identifier
     */
    public void removeSuspectAlarm(String channelId);

    /**
     * Gets the list of channels that have suspect DN values.
     * @return the list of channel identifiers
     */
    public List<String> getChannelsWithSuspectDN();

    /**
     * Gets the list of channels that have suspect EU values.
     * @return the list of channel identifiers
     */
    public List<String> getChannelsWithSuspectEU();

    /**
     * Gets the list of channels that have suspect alarms.
     * @return the list of channel identifiers
     */
    public List<String> getChannelsWithSuspectAlarms();

    /**
     * Indicates whether the given channel has a suspicious DN
     * @param channelId the channel identifier
     * @return true if channel has suspect DN, false if not
     */
    public boolean hasSuspectDn(String channelId);

    /**
     * Indicates whether the given channel has a suspicious EU
     * @param channelId the channel identifier
     * @return true if channel has suspect EU, false if not
     */
    public boolean hasSuspectEu(String channelId);

    /**
     * Indicates whether the given channel has suspicious alarms.
     * @param channelId the channel identifier
     * @return true if channel has suspect alarms, false if not
     */
    public boolean hasSuspectAlarms(String channelId);

    /**
     * Gets the total count of suspect alarms for all channels.
     * @return the total count of suspect alarms
     */
    public int getSuspectAlarmCount();

    /**
     * Gets a list of all unique channel IDs in the suspect lists.
     * @return List of channel IDs as strings
     */
    public List<String> getAllSuspectChannelIds();

    /**
     * Clears all entries from the suspect channel and alarm lists.
     */
    public void clear();

    /**
     * Find the configured suspect channel file and return its path.
     * @return the file path to the suspect channel file, or null if none found
     */
    public String locateSuspectChannelFile();

    /**
     * Sets the current suspect channel file location, for use by the next save 
     * or parse call.
     * @param path the path to the suspect channel file
     */
    public void setFileLocation(String path);

    /**
     * Saves the current contents of the table to the currently established location for the
     * suspect channel file.
     * 
     * @throws IOException if there is an I/O problem writing the file
     */
    public void save() throws IOException;

    /**
     * Parses a suspect channel file into the table.
     * 
     * @param filePath
     *            path to the suspect channel file
     * @throws DictionaryException
     *             if there is an error parsing the file
     */
    public void parse(String filePath) throws DictionaryException;

    /**
     * Gets tables current SseContextFlag
     * 
     * @return true if application is SSE
     */
    public SseContextFlag getTableSseContextFlag();

    /**
     * Sets the default location of the suspect channel file.
     * @param path the path to the default suspect channel file
     */
    public void setDefaultFileLocation(final String path);

}