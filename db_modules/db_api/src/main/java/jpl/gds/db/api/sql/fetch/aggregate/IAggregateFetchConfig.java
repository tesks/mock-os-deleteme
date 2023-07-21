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
package jpl.gds.db.api.sql.fetch.aggregate;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.db.api.sql.fetch.AlarmControl;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.DatabaseTimeRange;

/**
 * An interface implemented by the Aggregate Fetch Configuration
 *
 */
public interface IAggregateFetchConfig {
	
	public void setChannelLookupTable(Set<String> channelLookup);
	
	public Set<String> getChannelLookupTable();
	
	public void setOrderings(List<IChannelAggregateOrderByType> orderings);
	
	public List<IChannelAggregateOrderByType> getOrderings();
	
	public void setDatabaseTimeRanges(DatabaseTimeRange dbTimeRanges);
	
	public DatabaseTimeRange getDatabaseTimeRanges();

	public void setOutputFile(String outFile);

	public void setChunkSize(int chunkSize);

	public void setChunkDir(String chunkDir);

	public void setParallelThreads(int pthreads);

	public void setKeepTempFiles(boolean b);

	public void setAlarms(AlarmControl alarms);

	public void setPrintWriter(PrintWriter pw);

	public void setTableName(String tableName);

	public void setTemplateName(String optionValue);

	public void setTemplateGlobalContext(Map<String, Object> globalContext);

	public void setCsvColumns(List<String> csvColumns);

	public int getParallelThreads();

	public Writer getPrintWriter();

	public List<String> getCsvColumns();

	public String getOutputFile();

	public boolean isTemplateSpecified();

	public boolean isUsingAlarmFilter();

	public AlarmControl getAlarms();

	public TemplateManager getNewTemplateManager();

	public String getFullTemplateName();

	public Map<String, Object> getTemplateGlobalContext();

	public String getChunkDir();

	public int getChunkSize();
	
	public int getProcessorQueueSize();
	
	public int getOutputControllerQueueSize();

	public boolean isKeepTempFiles();

	public void setIncludePacketInfo(boolean includePacketInfo);

	public boolean isIncludePacketInfo();

	public boolean isUsingChannelFilter();

	public boolean isUsingTimeRange();
		
	public void setBatchTempFileThreshold(int maxBatchFileCount);
	
	public int getBatchTempFileThreshold();

	public void setSortEnabled();
	
	public boolean isSortEnabled();

	public void setModulePattern(String module);
	
	public String getModulePattern();
	
	public boolean isUsingModuleFilter();
	
	public String getModulePatternRegex();

	public void setChangesOnly();
	
	public boolean isUsingChangesOnlyFilter();

	public void setShowColumnHeaders(boolean showColHeaders);
	
	public boolean isShowColumnHeaders();

	public void setSessionPreFetch(IDbSessionPreFetch sessionPreFetch);
	
	public IDbSessionPreFetch getSessionPreFetch();

	public void setCsvHeaders(List<String> csvHeaders);
	
	public List<String> getCsvHeaders();
	
	public String getOrderByString();

    public void setOrderByType(int type);
    
    public int getOrderByType();

	/**
	 * Set the time based query begin-time pad in seconds. This config setting
	 * is only used when retrieving Channel Aggregate records from the database
	 *
	 * @param padInSeconds time pad in seconds
	 */
    public void setBeginTimePad(int padInSeconds);

	/**
	 * Get the time based query begin-time pad. This config setting
	 * is only used when retrieving Channel Aggregate records from the database
	 *
	 * @return the time pad
	 */
    public int getBeginTimePad();
}

