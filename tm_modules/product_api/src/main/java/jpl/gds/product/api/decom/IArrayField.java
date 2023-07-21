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
package jpl.gds.product.api.decom;


/**
 * The IArrayField interface is to be implemented by all classes that
 * implement array-type data fields for decommutation.
 * 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IArrayField defines the methods that must be implemented by all array field
 * decommutation classes, and allows for such operations as setting the length 
 * attributes of the array, or setting the index names, etc.  IArrayField extends
 * IFieldContainer, because it must contain IDefinitionField elements that define
 * the contents of the array.
 * 
 */
public interface IArrayField extends IFieldContainer {

    /**
     * Retrieves the length is in bytes flag, which indicates whether the
     * length of the array is specified in bytes or in number of array elements.
     * 
     * @return true if the array length is in bytes, false if in elements
     */
    public abstract boolean isLengthIsInBytes();
    /**
     * Retrieves the length is in bytes flag, which indicates whether the
     * length of the array is specified in bytes or in number of array elements.
     * 
     * @param lengthIsInBytes true if the array length is in bytes, false if in elements
     */
    public abstract void setLengthIsInBytes(final boolean lengthIsInBytes);

    /**
     * Returns the DefinitionField object representing the data field
     * that contains the length of a dynamic array.
     * 
     * @return the ISimpleField representing the array length
     */
    public abstract ISimpleField getLengthVariable();

    /**
     * Sets the DefinitionField object representing the product data field that
     * contains the length of a dynamic array.
     * 
     * @param lengthVariable The lengthVariable to set.
     */
    public abstract void setLengthVariable(final ISimpleField lengthVariable);

    // TODO - No one seems to use these index enum fields. Remove them everywhere.
    /**
     * @return the indexEnum
     */
    public abstract String getIndexEnum();

    /**
     * @param indexEnum the indexEnum to set
     */
    public abstract void setIndexEnum(final String indexEnum);

    /**
     * Returns the array field type, which indicates how the length of the
     * array is computed.
     * 
     * @return the ArrayType for this IArrayField
     */
    public abstract ArrayType getArrayType();

    /**
     * Sets the array field type, which indicates that this is a variable or fixed
     * length array field, and if variable, what type of variable array.
     * 
     * @param type the ArrayType to set
     */
    public abstract void setArrayType(final ArrayType type);

    /**
     * Adds an index label to the list of index labels for the array elements.
     * Index labels must be added to the array in the same order as 
     * DefinitionElements are added.
     * 
     * @param title the index label.
     */
    public abstract void addIndexLabel(final String title);

    /**
     * Gets an array index label.
     * 
     * @param index the array index
     * @return the corresponding index label, or null if none defined
     */
    public abstract String getIndexLabel(final int index);

    /**
     * Gets the length of (number of array elements in) the array.
     * 
     * @return the maximum array length, or -1 for variable length arrays.
     */
    public abstract int getMaxLength();

}