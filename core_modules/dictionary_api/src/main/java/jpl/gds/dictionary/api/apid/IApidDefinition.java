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
package jpl.gds.dictionary.api.apid;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.IAttributesSupport;
import jpl.gds.dictionary.api.ICategorySupport;

/**
 * The IApidDefinition interface is the dictionary interface that must be
 * implemented by all APID definition classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IApidDefinition object is the multi-mission representation of the
 * definition of an application process identifier, which is used to label
 * packets such that they can be routed to the proper processing components in
 * the ground system. IApidDefinition defines methods needed to interact with
 * APID Definition objects as required by the APID Dictionary interface. It is
 * primarily used by APID file parser implementations in conjunction with the
 * ApidDefinitionFactory, which is used to create actual multi-mission
 * IApidDefinition objects in the parsers. Alternatively, parsers may implement
 * their own mission-specific classes that implement this interface. Dictionary
 * objects should interact with APID Definition objects only through the Factory
 * and the IApidDefinition interfaces. Interaction with the actual APID
 * Definition implementation classes in an ApidDictionary implementation is
 * contrary to multi-mission development standards.
 *
 *          
 *
 * @see IApidDictionary
 * @see ApidDefinitionFactory
 */
public interface IApidDefinition extends IAttributesSupport, ICategorySupport {
    /**
     * Sets the name for the APID. This name is used for labeling packets and
     * products with this APID in the database and on ground system displays.
     * The allowed character set is limited to letters, numbers, underscores,
     * and dashes, and length is restricted to 64. Name is required for APID
     * definitions. It may not be null. 
     * <p>
     * This name corresponds to the value of the &lt;name&gt; element within the
     * &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @param apidName
     *            the APID name to set; should be "Unknown" rather than null if
     *            the current dictionary implementation does not support names
     *            for APIDs.
     */
	public void setName(String apidName);
	
    /**
     * Retrieve the name for the APID. This name is used for labeling packets
     * and products with this APID in the database and on ground system
     * displays. The allowed character set is limited to letters, numbers,
     * underscores, and dashes, and length is restricted to 64.  Name is required 
     * for APID definitions. It may not be null. 
     * <p>
     * This name corresponds to the value of the &lt;name&gt; element within the
     * &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @return the APID name string; "Unknown" if the current dictionary
     *         implementation does not support names for APIDs.
     */
	public String getName();
	
    /**
     * Sets the human-readable title for the APID. May be any length. May not
     * contain line feeds. Title is optional and may be null.
     * <p>
     * This title corresponds to the value of the &lt;title&gt; element within the
     * &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @param apidTitle
     *            the APID title to set; should be null if the current
     *            dictionary implementation does not support titles for APIDs.
     */
    public void setTitle(String apidTitle);
    
    /**
     * Retrieve the human-readable title for the APID. May be any length. May
     * not contain line feeds. Title is optional and may be null.
     * 
     * <p>
     * This title corresponds to the value of the &lt;title&gt; element within the
     * &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @return the APID title string; null if the current dictionary
     *         implementation does not support title for APIDs.
     */
    public String getTitle();
    
    /**
     * Sets the human-readable description for the APID. May be any length. May
     * contain line feeds. Description is optional and may be null.
     * 
     * <p>
     * This description corresponds to the value of the &lt;description&gt;
     * element within the &lt;apid_definition&gt; element in the Multimission
     * APID dictionary schema.
     * 
     * @param apidDescription
     *            the APID description to set; should be null if the current
     *            dictionary implementation does not support descriptions for
     *            APIDs.
     */
    public void setDescription(String apidDescription);
    
    /**
     * Retrieve the human-readable description for the APID. May be any length. May
     * contain line feeds. Description is optional and may be null.
     * 
     * <p>
     * This description corresponds to the value of the &lt;description&gt;
     * element within the &lt;apid_definition&gt; element in the Multimission
     * APID dictionary schema.
     * 
     * @return the APID description string; null if the current dictionary
     *         implementation does not support description for APIDs.
     */
    public String getDescription();

    /**
     * Sets the APID number for this APID. This number is used for labeling
     * packets and products with this APID in the database and on ground system
     * displays. APID number ranges from 1 to 2047 and is required for all APID
     * definitions.
     * <p>
     * This number corresponds to the value of the "apid" attribute
     * on the &lt;apid_definition&gt; element in the Multimission APID
     * dictionary schema.
     * 
     * @param number
     *            the APID number
     */
	public void setNumber(int number);
	
	/**
	 * Retrieve the APID number for this APID. This number is used for labeling
	 * packets and products with this APID in the database and on ground system
	 * displays. APID number ranges from 1 to 2047 and is required for all APID
     * definitions.
	 * <p>
     * This number corresponds to the value of the "apid" attribute
     * on the &lt;apid_definition&gt; element in the Multimission APID
     * dictionary schema.
     * 
	 * @return the APID number
	 */
	public int getNumber();
	
	/**
	 * Sets the content type of the APID, which dictates how it will be routed in the
	 * ground system. Content type is required for all APID definitions.
	 * <p>
     * The content type corresponds to the value of the "format" attribute
     * on the &lt;apid_definition&gt; element in the Multimission APID
     * dictionary schema.
	 * 
	 * @param type the ApidContentType to set; may not be null
	 */
	public void setContentType(ApidContentType type);
	
    /**
     * Gets the content type of the APID, which dictates how it will be routed
     * in the ground system. Content type is required for all APID definitions.
     * This method will return ApidContentType.UNKNOWN rather than null if if
     * the APID type is not set or unrecognized.
     * <p>
     * The content type corresponds to the value of the "format" attribute on
     * the &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @return the ApidContentType
     */
	public ApidContentType getContentType();

    /**
     * Retrieves the flag indicating whether or not this APID is for recorded
     * data. This flag is optional and will default to false if not supplied
     * in the dictionary.
     *<p>
     * The recorded flag corresponds to the value of the "recorded" attribute on
     * the &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @return true if APID is recorded or not, false if realtime
     */
    public boolean isRecorded();
    
    /**
     * Sets the flag indicating whether this APID is for recorded data. This
     * flag is optional and will default to false if not supplied in the
     * dictionary.
     * <p>
     * The recorded flag corresponds to the value of the "recorded" attribute on
     * the &lt;apid_definition&gt; element in the Multimission APID dictionary
     * schema.
     * 
     * @param recorded
     *            true if data in packets with this APID is recorded, false if
     *            realtime
     */
    public void setRecorded(boolean recorded);
    
    /**
     * Gets the product decommutation handler object for this APID. NOTE: For
     * missions that use the multimission product adaptation, the product
     * DecomHandler should be fetched from the product dictionary as opposed to
     * the APID dictionary. This method is present only for use by non-standard
     * product adaptations.
     * 
     * <p>
     * The product handler corresponds to the value of the &lt;viewer&gt;
     * element within the &lt;apid_definition&gt; element in the Multimission
     * APID dictionary schema.
     * 
     * @return the definition of the product decommutation handler, or null if
     *         not defined for this APID.
     */
    public DecomHandler getProductHandler();
    
    /**
     * Sets the product decommutation handler object for this APID. NOTE: For
     * missions that use the multimission product adaptation, the product
     * DecomHandler should be set in the product dictionary as opposed to
     * the APID dictionary. This method is present only for use by non-standard
     * product adaptations.
     * 
     * <p>
     * The product handler corresponds to the value of the &lt;viewer&gt;
     * element within the &lt;apid_definition&gt; element in the Multimission
     * APID dictionary schema.
     * 
     * @param handler the definition of the product decommutation handler
     */
    public void setProductHandler(DecomHandler handler);
    
    /**
     * Indicates whether this data product APID uses a commanded product header
     * in product packets. This flag is used by the multimission product
     * adaptation when it processes product PDU packets and is dictated by the
     * MSAP heritage of the flight module that produces the flight PDUs. The
     * value should be set to false for all non-product APID definitions.
     * 
     * <p>
     * The commanded product flag corresponds to the value of the
     * &lt;isCommandedProduct&gt; element within the &lt;apid_definition&gt;
     * element in the Multimission APID dictionary schema.
     * 
     * @return true if a commanded product or non-product APID, false if not (a
     *         streaming or autonomous product)
     */
    public boolean isCommandedProduct();
    
    /**
     * Sets the flag indicating whether this data product APID uses a commanded
     * product header in product packets. This flag is used by the multimission
     * product adaptation when it processes product PDU packets and is dictated
     * by the MSAP heritage of the flight module that produces the flight PDUs.
     * The value should be set to false for all non-product APID definitions.
     * 
     * <p>
     * The commanded product flag corresponds to the value of the
     * &lt;isCommandedProduct&gt; element within the &lt;apid_definition&gt;
     * element in the Multimission APID dictionary schema.
     * 
     * @param isCommanded
     *            true if a commanded product or non-product APID, false if not
     *            (a streaming or autonomous product)
     */
    public void setCommandedProduct(boolean isCommanded);
    
    /**
     * Get the secondary header handler for this APID.
     * 
     * @return String id of the secondary header extractor
     */
	public String getSecondaryHeaderExtractor();
	
    /**
     * Get the secondary header type for this APID.
     * 
     * @return SecondaryHeaderType
     */
	public SecondaryHeaderType getSecondaryHeaderType();

    /**
     * Get the secondary header type for this APID.
     * 
     * @param extractorType SecondaryHeaderType to set
     */
	public void setSecondaryHeaderType(SecondaryHeaderType extractorType);

	/**
	 * Sets the secondary header handler for this APID.
	 * 
	 * @param extractorId id of the secondary header extractor
	 */
	public void setSecondaryHeaderExtractor(String extractorId);
		
}
