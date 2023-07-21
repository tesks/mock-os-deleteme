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
package jpl.gds.dictionary.impl.apid;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.ApidDefinitionFactory;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.SecondaryHeaderType;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.TimeProperties;

/**
 * ApidDefinition is the multi-mission class for storing the
 * definition of an Application Process IDentifier from the APID dictionary.
 * <p>
 * Instances of this class must be created via ApidDefinitionFactory. All
 * references to instance should be via the IApidDefinition interface rather
 * than via this class.
 * 
 *
 *
 * @see ApidDefinitionFactory         
 * 
 */
public class ApidDefinition implements IApidDefinition {

    /**
     * The name of the APID from the dictionary.
     */
    private String name = "Unknown";
    /**
     * The number of the APID from the dictionary.
     */
    private int number = -1;
    /**
     * The type of data content for this APID.
     */
    private ApidContentType type = ApidContentType.UNKNOWN;

    /**
     * Whether or not this APID is recorded (or realtime).
     */
    private boolean recorded = false;

    /**
     * Title of this APID.
     */
    private String title;

    /**
     * Description of this APID.
     */
    private String description;

    /**
     * Flag indicating if APID is for a product with commanded product header.
     */
    private boolean isCommanded = true;

    /**
     * Product viewer (decom handler)
     */
    private DecomHandler handler;
 
    /**
     *  Category map to hold category name and category value.
     *  
     */
    
    private Categories categories = new Categories();

    /**
     * Key-value attributes for this APID.
     */
	private KeyValueAttributes keyValueAttr = new KeyValueAttributes();
	
	private SecondaryHeaderType secondaryHeaderType = SecondaryHeaderType.TIME;
	
	private String secondaryHeaderExtractor = TimeProperties.CANONICAL_SCLK_ID;
	
	/** 
	 * Constructor.
	 * 
	 */
	public ApidDefinition() {
	    SystemUtilities.doNothing();
	}


    @Override
    public String getName() {
        return name;
    }


    @Override
    public void setName(final String apidName) {
        name = apidName;
    }


    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public void setNumber(final int number) {
        this.number = number;
    }


    @Override
    public ApidContentType getContentType() {
        return type;
    }

    /**
     *      Note this method has a side effect. If the ApidContentType is set to
     *      anything other than PRODUCT, this method sets isCommandedProduct to
     *      false.
     */
    @Override
    public void setContentType(final ApidContentType type) {
        this.type = type;	
        if (!type.equals(ApidContentType.DATA_PRODUCT)) {
            this.isCommanded = false;
        }
    }


    @Override
    public boolean isRecorded() {
        return recorded;
    }


    @Override
    public void setRecorded(boolean recorded) {
        this.recorded = recorded;
    }


    @Override
    public void setTitle(String apidTitle) {

        this.title = apidTitle;

    }


    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setDescription(String apidDescription) {
        this.description = apidDescription;

    }


    @Override
    public String getDescription() {
        return this.description;
    }


    @Override
    public DecomHandler getProductHandler() {
        return handler;
    }


    @Override
    public void setProductHandler(DecomHandler handler) {
        this.handler = handler;        
    }


    @Override
    public boolean isCommandedProduct() {
        return this.isCommanded;
    }


    @Override
    public void setCommandedProduct(boolean isCommanded) {
        this.isCommanded = isCommanded;
    }

	@Override	
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}


	@Override	
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}
	

	@Override	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}


	@Override	
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}


	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();
	}


    @Override
    public void setCategories(Categories map) {
        categories.copyFrom(map);
    }


    @Override
    public Categories getCategories() {
        return categories;
    }


    @Override
    public void setCategory(String catName, String catValue) {
        categories.setCategory(catName, catValue);        
    }


    @Override
    public String getCategory(String name) {
        return categories.getCategory(name);
    }

	@Override
	public String getSecondaryHeaderExtractor() {
		return secondaryHeaderExtractor;
	}

	@Override
	public SecondaryHeaderType getSecondaryHeaderType() {
		return secondaryHeaderType;
	}

	@Override
	public void setSecondaryHeaderType(SecondaryHeaderType extractorType) {
		secondaryHeaderType = extractorType;
	}

	@Override
	public void setSecondaryHeaderExtractor(String extractorId) {
		secondaryHeaderExtractor = extractorId;
	}

}
