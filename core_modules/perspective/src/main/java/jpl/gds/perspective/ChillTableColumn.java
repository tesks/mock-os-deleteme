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
package jpl.gds.perspective;



/**
 * The ChillTableColumn class is used to represent a single column in a table in the 
 * user perspective.

 *
 */
public class ChillTableColumn {
	
	/**
	 * Enumerated list of sort types
	 * 
	 *
	 */
	public enum SortType {
		
	    /**
		 * String sort
		 */
		CHARACTER,
		
		/**
		 * Sort by integer number
		 */
		NUMERIC,
		/**
		 * Sort by double number.
		 * Only usable in NAT table views.
		 */
		DOUBLE;
	}
    
    /**
     * Default column width in pixels
     */
    public static final Integer DEFAULT_COLUMN_WIDTH = 50;
    
    /**
     * Disabled column index
     */
    public static final Integer DISABLED_INDEX = -1;
    
    /**
     * XML tag name for a table column
     */
    public static final String COLUMN_TAG = "TableColumn";
    
    /**
     * XML tag name for the official column name
     */
    public static final String OFFICIAL_NAME_TAG = "OfficialName";
    
    /**
     * XML tag name for the display name
     */
    public static final String DISPLAY_NAME_TAG = "DisplayName";
    
    /**
     * XML tag name for the configuration number
     */
    public static final String CONFIG_NUMBER_TAG = "ConfigurationNum";
    
    /**
     * XML tag name for the column's position in the table
     */
    public static final String POSITION_TAG = "Position";
    
    /**
     * XML tag name for the column's default width
     */
    public static final String DEFAULT_WIDTH_TAG = "DefaultWidth";
    
    /**
     * XML tag name for the column's actual width
     */
    public static final String CURRENT_WIDTH_TAG = "Width";
    
    /**
     * XML tag name for the column enabled flag
     */
    public static final String ENABLED_TAG = "Enabled";
    
    /**
     * XML tag name for the sort column flag
     */
    public static final String SORT_TAG = "Sort";
    
    /**
     * XML tag name for the sort type (numeric or character)
     */
    public static final String SORT_TYPE_TAG = "SortType";
    
    private String officialName;
    private boolean isColEnabled;
    private String displayName;
    private int configurationNumber;
    private int currentPosition;
    private boolean isColSortColumn;
    private int defaultWidth;
    private int currentWidth;
    private SortType sortType = SortType.CHARACTER;
    private boolean deprecated;
    
    /**
     * Retrieves the defaultWidth.
     * @return the defaultWidth
     */
    public int getDefaultWidth() {
        return this.defaultWidth;
    }

    
    /**
     * Sets the defaultWidth.
     * @param defaultWidth the defaultWidth to set
     */
    public void setDefaultWidth(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    
    /**
     * Retrieves the currentWidth.
     * @return the currentWidth
     */
    public int getCurrentWidth() {
        return this.currentWidth;
    }

    
    /**
     * Sets the currentWidth.
     * @param currentWidth the currentWidth to set
     */
    public void setCurrentWidth(int currentWidth) {
        this.currentWidth = currentWidth;
    }

    /**
     * Retrieves the officialName.
     * @return the officialName
     */
    public String getOfficialName() {
        return this.officialName;
    }
    
    /**
     * Sets the officialName.
     * @param officialName the officialName to set
     */
    public void setOfficialName(String officialName) {
        this.officialName = officialName;
    }
    
    /**
     * Retrieves the isEnabled.
     * @return the isEnabled
     */
    public boolean isEnabled() {
        return this.isColEnabled && !this.deprecated;
    }
    
    /**
     * Sets the isEnabled.
     * @param isEnabled the isEnabled to set
     */
    public void setEnabled(boolean isEnabled) {
        this.isColEnabled = isEnabled;
        if (!this.isColEnabled) {
            this.currentPosition = -1;
        }
    }
    
    /**
     * Gets the deprecated column flag. Deprecated columns will not show.
     * @return true if deprecated, false if not
     */
    public boolean isDeprecated() {
		return deprecated;
	}

    /**
     * Sets the deprecated column flag. Deprecated columns will not show.
     * @param deprecated true to deprecate, false to not
     */
	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
		if (deprecated) {
			setEnabled(false);
		}
	}

	/**
     * Retrieves the displayName.
     * @return the displayName
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Sets the displayName.
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Retrieves the configurationNumber.
     * @return the configurationNumber
     */
    public int getConfigurationNumber() {
        return this.configurationNumber;
    }
    
    /**
     * Sets the configurationNumber.
     * @param configurationNumber the configurationNumber to set
     */
    public void setConfigurationNumber(int configurationNumber) {
        this.configurationNumber = configurationNumber;
    }
    
    /**
     * Retrieves the currentPosition.
     * @return the currentPosition
     */
    public int getCurrentPosition() {
        return this.currentPosition;
    }
    
    /**
     * Sets the currentPosition.
     * @param currentPosition the currentPosition to set
     */
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }
    
    /**
     * Retrieves the isSortColumn.
     * @return the isSortColumn
     */
    public boolean isSortColumn() {
        return this.isColSortColumn;
    }
    
    /**
     * Sets the isSortColumn.
     * @param isSortColumn the isSortColumn to set
     */
    public void setSortColumn(boolean isSortColumn) {
        this.isColSortColumn = isSortColumn;
    }
    
    /**
     * Gets the sorting type for this column.
     * @return SortType
     */
    public SortType getSortType() {
		return sortType;
	}

    /**
     * Sets the sorting type for this column.
     * @param sortType the SortType to set
     */
	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}

	/**
	 * Creates an XML representation of this column.
	 * @return XML string
	 */
	public String toXml() {
        StringBuffer result = new StringBuffer();
        result.append("<" + COLUMN_TAG);
        result.append(" " + OFFICIAL_NAME_TAG + "=\"" + this.officialName + "\"");
        result.append(" " + DISPLAY_NAME_TAG + "=\"" + this.displayName + "\"");
        result.append(" " + CONFIG_NUMBER_TAG + "=\"" + this.configurationNumber + "\"");
        result.append(" " + POSITION_TAG + "=\"" + this.currentPosition + "\"");
        result.append(" " + DEFAULT_WIDTH_TAG + "=\"" + this.defaultWidth + "\"");
        result.append(" " + CURRENT_WIDTH_TAG + "=\"" + this.currentWidth + "\"");
        result.append(" " + ENABLED_TAG + "=\"" + String.valueOf(this.isColEnabled && !this.deprecated) + "\"");       
        result.append(" " + SORT_TAG + "=\"" + this.isColSortColumn + "\"");
        result.append("/>");
        
        return result.toString();
    }
    
    /**
     * Creates a copy of this column object.
     * @return ChillTableColumn
     */
    public ChillTableColumn copy() {
        ChillTableColumn newCol = new ChillTableColumn();
        newCol.setOfficialName(this.officialName);
        newCol.setEnabled(this.isColEnabled);
        newCol.setDeprecated(this.deprecated);
        newCol.setDisplayName(this.displayName);
        newCol.setConfigurationNumber(this.configurationNumber);
        newCol.setCurrentPosition(this.currentPosition);
        newCol.setCurrentWidth(this.currentWidth);
        newCol.setDefaultWidth(this.defaultWidth);
        newCol.setSortColumn(this.isColSortColumn);
        newCol.setSortType(this.sortType);
        return newCol;
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toXml();
    }
}
