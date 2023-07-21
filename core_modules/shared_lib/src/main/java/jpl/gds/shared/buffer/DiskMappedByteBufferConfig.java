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
package jpl.gds.shared.buffer;


/**
 * This object is a container to hold all of the variables necessary for starting up a <code>DiskMappedByteBuffer</code>.
 * It contains the following elements:
 *		bufferItemSize		- number of bytes each DiskMappedByteBufferItem will utilize for its byte buffer
 *		bufferItemLimit		- size of the DiskMappedByteBuffer's array, in number of elements
 *		consumerWindowSize  - maximum number of items to be kept in memory
 *		fileSize			- number of bytes each backing file will back 
 *		fileLimit			- number of files allowed to be used for backing data
 *		useFiles			- boolean indicating if files are being used to back byte buffers
 *		deleteFiles			- boolean indicating if backing files are being deleted when no longer in use
 * 		backupAllData		- boolean indicating if all data that passes through the buffer is put in a backing file
 *		bufferDir			- String representing the directory, where backing files will be placed
 *
 *
 * Created DiskMappedByteBufferConfig. This object has been created in order to allow the
 * 			DiskMappedByteBuffer to be created and initialized with a set of values regardless of the location in AMPCS it is being used
 * 			while keeping the constructor's function signature short and legible.
 * 			It contains one constructor that requires a RawInputConfig object due to its initial usage as a buffer for the Raw Input system;
 * 			this system's configuration information contains a set of user configurable variables for its use in this location.
 * 			Additionally a default constructor and set functions are available in order to allow the buffer to be immediately configured if used
 * 			in any other location.
 * 			Alteration of the values in an instance of this class after it has been used to initialize a DiskMappedByteBuffer will NOT alter the values
 * 			of the DiskMappedByteBuffer
 *
 */
public class DiskMappedByteBufferConfig{ 
	//contained values
	private int bufferItemSize;
	private int bufferItemLimit;
	private int consumerWindowSize;
	private int maintenanceDelay;
	private long fileSize;
	private int fileLimit;
	private boolean useFiles;
	private boolean deleteFiles;
	private boolean backupAllData;
	private String bufferDir;
	
	/**
	 * Create a DiskMappedByteBufferConfig object with very basic information. 
	 * These default values can be used, but are meant to be overwritten before use.
	 */
	public DiskMappedByteBufferConfig(){
		bufferItemSize = 1000000;
		bufferItemLimit = 100;
		consumerWindowSize = 10;
		maintenanceDelay = 200;
		fileSize = 10000000;
		fileLimit = 65536;
		useFiles = true;
		deleteFiles = true;
		backupAllData = false;
		bufferDir = ".";
	}
	

	/**
	 * Set the value of the bufferItemSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the item size specifies the number of bytes each DiskMappedByteBuffer can contain
	 * 
	 * @param newVal sets new value of the bufferItemSize in this object
	 */
	public void setBufferItemSize(int newVal){
		this.bufferItemSize = newVal;
	}
	
	/**
	 * Get the value of the bufferItemSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the item size specifies the number of bytes each DiskMappedByteBuffer can contain
	 * 
	 * @return current value of bufferItemSize in this object 
	 */
	public int getBufferItemSize(){
		return this.bufferItemSize;
	}
	
	/**
	 * Set the value of the bufferItemLimit variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the item limit specifies the length (number of elements) of the array holding DiskMappedByteBufferItems
	 * when it is first created
	 * 
	 * @param newVal sets new value of bufferItemLimit in this object
	 */
	public void setBufferItemLimit(int newVal){
		this.bufferItemLimit = newVal;
	}
	
	/**
	 * Get the value of the bufferItemLimit variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the item limit specifies the length (number of elements) of the array holding DiskMappedByteBufferItems
	 * when it is first created
	 * 
	 * @return current value of bufferItemLimit in this object
	 */
	public int getBufferItemLimit(){
		return this.bufferItemLimit;
	}
	
	/**
	 * Set the value of the consumerWindowSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedButeBuffer the consumer window size specifies the number of DiskMappedByteBufferItems in the array are to be pulled back into memory,
	 * or kept in memory, for the read function. This value includes the element currently being read from. the consumer window size within the DiskMappedByteBuffer
	 * may enlarge in order to increase performance
	 * 
	 * @param newVal sets new value of consumerWindowSize in this object
	 */
	public void setConsumerWindowSize(int newVal){
		this.consumerWindowSize = newVal;
	}
	
	/**
	 * Get the value of the consumerWindowSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedButeBuffer the consumer window size specifies the number of DiskMappedByteBufferItems in the array are to be pulled back into memory,
	 * or kept in memory, for the read function. This value includes the element currently being read from. the consumer window size within the DiskMappedByteBuffer
	 * may enlarge in order to increase performance
	 * 
	 * @return value of consumerWindowSize in this object
	 */
	public int getConsumerWindowSize(){
		return this.consumerWindowSize;
	}
	
	/**
	 * Set the value of the maintenanceDelay variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the maintenance delay specifies how much time, in milliseconds, must pass between executions of the maintenance function
	 * by the maintenance executor. While a value of 0 ms would require the maintenance to occur immediately after the previous and directly cause problems,
	 * lowering this value can decrease performance. Conversely, a high maintenance interval can decrease performance, primarily in data ingestion.
	 * 
	 * @param newVal sets new value of maintenanceDelay in this object
	 */
	public void setMaintenanceDelay(int newVal){
		this.maintenanceDelay = newVal;
	}
	
	/**
	 * Get the value of the maintenanceDelay variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the maintenance delay specifies how much time, in milliseconds, must pass between executions of the maintenance function
	 * by the maintenance executor.
	 * 
	 * @return value of maintenanceDelay in this object
	 */
	public int getMaintenanceDelay(){
		return this.maintenanceDelay;
	}
	
	/**
	 * Set the value of the fileSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the fileSize is the minimum size where a file on disk backing one or more DiskMappedByteBufferItems is closed and a new file opened.
	 * 
	 * @param newVal sets new value of fileSize in this object
	 * 
	 */
	public void setFileSize(long newVal){
		this.fileSize = newVal;
	}
	
	/**
	 * Get the value of the fileSize variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the fileSize is the minimum size where a file on disk backing one or more DiskMappedByteBufferItems is closed and a new file opened.
	 * 
	 * @return value of fileSize in this object
	 */
	public long getFileSize(){
		return this.fileSize;
	}
	
	/**
	 * Set the value of the fileLimit variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the file limit specifies the maximum number of files that can be utilized to back the buffer at any point, excluding when
	 * "backup all data" mode is being utilized.
	 * 
	 * @param newVal set fileLimit in this object
	 */
	public void setFileLimit(int newVal){
		this.fileLimit = newVal;
	}
	
	/**
	 * Get the value of the fileLimit variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the file limit specifies the maximum number of files that can be utilized to back the buffer at any point, excluding when
	 * "backup all data" mode is being utilized.
	 * 
	 * @return current value of fileLimit in this object
	 */
	public int getFileLimit(){
		return this.fileLimit;
	}
	
	/**
	 * Set the value of the useFiles variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the setFile boolean is used to determine if files will be utilized to
	 * back all data between the write and read objects that are not in the consumer window.
	 * WILL BE IGNORED if backupAllData is true.
	 * 
	 * @param newVal set useFiles in this object
	 */
	public void setUseFiles(boolean newVal){
		this.useFiles = newVal;
	}
	
	/**
	 * Get the value of the useFiles variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the setFile boolean is used to determine if files will be utilized to
	 * back all data between the write and read objects that are not in the consumer window.
	 * WILL BE IGNORED if backupAllData is true.
	 * 
	 * @return current value of useFiles in this object
	 */
	public boolean getUseFiles(){
		return this.useFiles;
	}
	
	/**
	 * Set the value of the deleteFiles variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the deleteFiles boolean is used to determine if files will be deleted
	 * once the read/consumer has moved past all DiskMappedByteBufferItems that were backed by this file.
	 * WILL BE IGNORED if useFiles is false or backupAllData is true.
	 * 
	 * @param newVal set deleteFiles in this object
	 */
	public void setDeleteFiles(boolean newVal){
		this.deleteFiles = newVal;
	}
	
	/**
	 * Get the value of the deleteFiles variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the deleteFiles boolean is used to determine if files will be deleted
	 * once the read/consumer has moved past all DiskMappedByteBufferItems that were backed by this file.
	 * WILL BE IGNORED if useFiles is false or backupAllData is true.
	 * 
	 * @return current value of deleteFiles in this object
	 */
	public boolean getDeleteFiles(){
		return this.deleteFiles;
	}
	
	/**
	 * Set the value of the backupAllData variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the backupAllData boolean is used to determine if all data that passes through the buffer,
	 * regardless of how long it resides in the buffer or how far apart the producer and consumer indexes are, is placed in
	 * a backing file. If true, files WILL be used, WILL NOT be deleted, and file limit will be ignored.
	 * 
	 * @param newVal set backupAllData in this object
	 */
	public void setBackupAllData(boolean newVal){
		this.backupAllData = newVal;
	}
	
	/**
	 * Get the value of the backupAllData variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the backupAllData boolean is used to determine if all data that passes through the buffer,
	 * regardless of how long it resides in the buffer or how far apart the producer and consumer indexes are, is placed in
	 * a backing file. If true, files WILL be used, WILL NOT be deleted, and file limit will be ignored.
	 * 
	 * @return current value of backupAllData in this object
	 */
	public boolean getBackupAllData(){
		return this.backupAllData;
	}
	
	/**
	 * Set the value of the bufferDir variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the bufferDir string is used to determine the location where it will place its base directory.
	 * The backing files will not be created and deleted from here, but within a folder where one or more folders may reside. The
	 * number of folders within this folder is dependent upon if files are deleted and the number of concurrent DiskMappedByteBuffers.
	 * Each DiskMappedByteBuffer will receive a unique folder
	 * 
	 * @param newVal set the bufferDir string in this object
	 */
	public void setBufferDir(String newVal){
		this.bufferDir = newVal;
	}
	
	/**
	 * Get the value of the bufferDir variable in this DiskMappedByteBufferConfig object
	 * 
	 * In DiskMappedByteBuffer the bufferDir string is used to determine the location where it will place its base directory.
	 * The backing files will not be created and deleted from here, but within a folder where one or more folders may reside. The
	 * number of folders within this folder is dependent upon if files are deleted and the number of concurrent DiskMappedByteBuffers.
	 * Each DiskMappedByteBuffer will receive a unique folder
	 * 
	 * @return current value of the bufferDir string in this object
	 */
	public String getBufferDir(){
		return this.bufferDir;
	}
}