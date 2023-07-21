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
package jpl.gds.station.api.dsn.chdo;

/**
 * An interface to be implemented by by classes that represent CHDO SFDU definitions.
 * 
 *
 * @since R8
 */
public interface IChdoDefinition {

    /**
     * Byte length of CHDO type field in header.
     */
    public static final int CHDO_TYPE_SIZE = 2;
    /**
     * Byte length of CHDO length field in header.
     */
    public static final int CHDO_LENGTH_SIZE = 2;
    /**
     * Name of CHDO type field.
     */
    public static final String CHDO_TYPE_FIELD = "chdo_type";
    /**
     * Name of CHDO length field.
     */
    public static final String CHDO_LENGTH_FIELD = "chdo_length";

    /**
     * Adds a field definition to this CHDO definition.
     * @param fieldName name of the field
     * @param currentField definition object for the field
     */
    public void addFieldMapping(String fieldName, IChdoFieldDefinition currentField);

    /**
     * Gets a field definition by name.
     * @param fieldName Name of the field to get
     * @return field definition object, or null if not a field in this CHDO definition
     */
    public IChdoFieldDefinition getFieldDefinitionByName(String fieldName);

    /**
     * Gets the name of the data fields in this CHDO Definition.
     * 
     * @return array of names
     */
    public String[] getFieldNames();

    /**
     * Gets the classification of this CHDO definition: primary, secondary, tertiary, aggregation, data.
     * 
     * @return the classification.
     */
    public String getClassification();

    /**
     * Sets the classification of this CHDO definition: primary, secondary, tertiary, aggregation, data.
     *
     * @param classification The classification to set.
     */
    public void setClassification(String classification);

    /**
     * Gets the name of this CHDO definition.
     * 
     * @return the name, or null if none set
     */
    public String getName();

    /**
     * Sets the name of this CHDO definition.
     *
     * @param name The name to set.
     */
    public void setName(String name);

    /**
     * Gets the type number of this CHDO definition.
     * 
     * @return the CHDO type indicator
     */
    public int getType();

    /**
     * Sets the type number for this CHDO definition.
     *
     * @param type The type to set.
     */
    public void setType(int type);

    /**
     * Get the size of the CHDO in bytes.
     *
     * @return Size
     */
    public int getByteSize();

    /**
     * Set the size of the CHDO in bytes.
     *
     * @param size Size in bytes
     */
    public void setByteSize(int size);

}