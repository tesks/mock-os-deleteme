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
 * 
 */
package jpl.gds.product.processors;

import jpl.gds.db.api.sql.store.StoreIdentifier;

/**
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 * MPCS-8568 - 12/12/2016 - Added fswDictionaryDirectory and get/set methods. Updated constructors.
 */
public class PostDownlinkProductProcessorOptions {
    private Boolean           displayToConsole;
    private Boolean           useDatabase;
    private Boolean           useJMS;
    private Boolean           isDoingMetadataCorrection;
    private Boolean           doOverrideDictionary;
    private String            overridingDictionary;
    private Long              fswBuildId;
	private String            fswDictionaryDirectory;
    private StoreIdentifier[] ldiStores;
    private Boolean           displayOptions;

	/**
	 * Null Constructor (sets default options).
	 */
	public PostDownlinkProductProcessorOptions() {
		super();
		this.displayToConsole = false;
		this.useDatabase = true;
		this.useJMS = true;
		this.isDoingMetadataCorrection = false;
		this.doOverrideDictionary = false;
		this.overridingDictionary = null;
		this.fswBuildId = null;
		this.fswDictionaryDirectory = null;
		this.ldiStores = null;
		this.displayOptions = null;
	}

	/**
	 * @param displayToConsole 
	 * @param useDatabase 
	 * @param useJMS 
	 * @param isDoingMetadataCorrection 
	 * @param doOverrideDictionary 
	 * @param overridingDictionary 
	 * @param fswBuildId 
	 * @param fswDictionaryDirectory 
	 * @param ldiStores 
	 * @param displayOptions 
	 */
	public PostDownlinkProductProcessorOptions(final Boolean displayToConsole, final Boolean useDatabase, final Boolean useJMS, final Boolean isDoingMetadataCorrection,
			final Boolean doOverrideDictionary, final String overridingDictionary, final Long fswBuildId, final String fswDictionaryDirectory, final StoreIdentifier[] ldiStores, final Boolean displayOptions) {
		super();
		this.displayToConsole = displayToConsole;
		this.useDatabase = useDatabase;
		this.useJMS = useJMS;
		this.isDoingMetadataCorrection = isDoingMetadataCorrection;
		this.doOverrideDictionary = doOverrideDictionary;
		this.overridingDictionary = overridingDictionary;
		this.fswBuildId = ((null == fswBuildId) || (fswBuildId <= 0)) ? null : fswBuildId;
		this.fswDictionaryDirectory = fswDictionaryDirectory;
		this.ldiStores = ldiStores;
		this.displayOptions = displayOptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder(PostDownlinkProductProcessorOptions.class.getSimpleName());

		final String format = "%1$s = %2$s;";
		final String format2 = "\n%1$s = %2$s;";

		s.append(String.format(format, "displayToConsole", displayToConsole));
		s.append(String.format(format2, "useDatabase", useDatabase));
		s.append(String.format(format2, "useJMS", useJMS));
		s.append(String.format(format2, "isDoingMetadataCorrection", isDoingMetadataCorrection));
		s.append(String.format(format2, "doOverrideDictionary", doOverrideDictionary));
		s.append(String.format(format2, "overridingDictionary", overridingDictionary));
		s.append(String.format(format2, "fswBuildId", fswBuildId));
		s.append(String.format(format2,  "fswDictionaryDirectory", fswDictionaryDirectory));
		for (int i = 0; i < ldiStores.length; i++) {
			s.append(String.format(format2,  "ldiStores[" + i + "]", ldiStores[i]));
		}
		s.append(String.format(format2, "displayOptions", displayOptions));
		s.append('\n');
		return s.toString();
	}

	/**
	 * @return the displayToConsole
	 */
	public Boolean getDisplayToConsole() {
		return displayToConsole;
	}

	/**
	 * @param displayToConsole
	 *            the displayToConsole to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setDisplayToConsole(final Boolean displayToConsole) {
		this.displayToConsole = displayToConsole;
		return this;
	}

	/**
	 * @return the useDatabase
	 */
	public Boolean getUseDatabase() {
		return useDatabase;
	}

	/**
	 * @param useDatabase
	 *            the useDatabase to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setUseDatabase(final Boolean useDatabase) {
		this.useDatabase = useDatabase;
		return this;
	}

	/**
	 * @return the useJMS
	 */
	public Boolean getUseJMS() {
		return useJMS;
	}

	/**
	 * @param useJMS
	 *            the useJMS to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setUseJMS(final Boolean useJMS) {
		this.useJMS = useJMS;
		return this;
	}

	/**
	 * @return the isDoingMetadataCorrection
	 */
	public Boolean getIsDoingMetadataCorrection() {
		return isDoingMetadataCorrection;
	}

	/**
	 * @param isDoingMetadataCorrection
	 *            the isDoingMetadataCorrection to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setIsDoingMetadataCorrection(final Boolean isDoingMetadataCorrection) {
		this.isDoingMetadataCorrection = isDoingMetadataCorrection;
		return this;
	}

	/**
	 * @return the doOverrideDictionary
	 */
	public Boolean getDoOverrideDictionary() {
		return doOverrideDictionary;
	}

	/**
	 * @param doOverrideDictionary
	 *            the doOverrideDictionary to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setDoOverrideDictionary(final Boolean doOverrideDictionary) {
		this.doOverrideDictionary = doOverrideDictionary;
		return this;
	}

	/**
	 * @return the overridingDictionary
	 */
	public String getOverridingDictionary() {
		return overridingDictionary;
	}

	/**
	 * @param overridingDictionary
	 *            the overridingDictionary to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setOverridingDictionary(final String overridingDictionary) {
		this.overridingDictionary = overridingDictionary;
		return this;
	}

	/**
	 * @return the fswBuildId
	 */
	public Long getFswBuildId() {
		return fswBuildId;
	}

	/**
	 * @param fswBuildId
	 *            the fswBuildId to set
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setFswBuildId(final Long fswBuildId) {
		this.fswBuildId = fswBuildId;
		return this;
	}
	
	/**
	 * @return the flight software dictionary directory
	 */
	public String getFswDictionaryDirectory(){
		return this.fswDictionaryDirectory;
	}
	
	/**
	 * @param fswDictionaryDirectory
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setFswDictionaryDirectory(final String fswDictionaryDirectory){
		this.fswDictionaryDirectory = fswDictionaryDirectory;
		return this;
	}
	
	/**
	 * @return the list of LDI stores to be used
	 */
	public StoreIdentifier[] getLdiStores() {
		return ldiStores == null ? new StoreIdentifier[0] : ldiStores;
	}
	
	/**
	 * @param ldiStores
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setLdiStores(final StoreIdentifier[] ldiStores) {
		this.ldiStores = ldiStores;
		return this;
	}

	/**
	 * @return True if options are to be shown, false otherwise
	 */
	public Boolean getDisplayOptions() {
		return displayOptions;
	}
	
	/**
	 * @param displayOptions
	 * @return the updated PostDownlinkProductProcessorOptions object
	 */
	public PostDownlinkProductProcessorOptions setDisplayOptions(final boolean displayOptions) {
		this.displayOptions = displayOptions;
		return this;
	}
}
