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

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;

/**
 * An interface to be implemented by decom field factories.
 * 
 *
 * @since R8
 */
public interface IProductDecomFieldFactory {
	/**
	 * Creates a bit array field.
	 * 
	 * @param name name of the field
	 * @param bitlength bit length of the field
	 * @return new field instance
	 */
	IFieldContainer createBitArrayField(String name, int bitlength);

	/**
     * Creates a bit field.
     * 
     * @param name name of the field
     * @param bitlength bit length of the field
     * @return new field instance
     */
	IBitField createBitField(String name, int bitlength);
	
	/**
	 * Indicates if channels should be extracted from the field.
	 * 
	 * @return true if should do channels.
	 */
	boolean isShouldDoChannels();
	
	/**
	 * The channel definition table in use by this field
	 * 
	 * @return the channel definition provider
	 */
	IChannelDefinitionProvider getChannelTable();

    /**
     * Creates a simple field.
     * 
     * @param name name of the field
     * @param type data type of the field
     * @param chanTable channel definition provider
     * @param shouldDoChannels true if channels should be extracted from this field
     * @return new field instance
     */
    ISimpleField createSimpleField(final String name, final DecomDataType type, IChannelDefinitionProvider chanTable, boolean shouldDoChannels);

    /**
     * Creates a simple field. Flag to channelize 
     * will be set to the value of shouldDoChannels().
     * 
     * @param name name of the field
     * @param type data type of the field
     * @param chanTable channel definition provider
     * @return new field instance
     */
    default ISimpleField createSimpleField(final String name, final DecomDataType type, final IChannelDefinitionProvider chanTable) {
    	return createSimpleField(name, type, chanTable, isShouldDoChannels());
    }

    /**
     * Creates a simple field with an implicit length prefix.
     * 
     * @param name name of the field
     * @param dataType data type of the field
     * @param chanTable channel definition provider
     * @param shouldDoChannels true if channels should be extracted from this field
     * @param lengthLength byte length of the length prefix
     * @return new field instance
     */
    ISimpleField createSimpleField(final String name, final DecomDataType dataType, IChannelDefinitionProvider chanTable, boolean shouldDoChannels, final int lengthLength);

    /**
     * Creates a simple field with an implicit length prefix. Flag to channelize 
     * will be set to the value of shouldDoChannels().
     * 
     * @param name name of the field
     * @param dataType data type of the field
     * @param chanTable channel definition provider
     * @param lengthLength byte length of the length prefix
     * @return new field instance
     */
    default ISimpleField createSimpleField(final String name, final DecomDataType dataType, final IChannelDefinitionProvider chanTable, final int lengthLength) {
    	return createSimpleField(name, dataType, chanTable, isShouldDoChannels(), lengthLength);
    }

    /**
     * Creates a simple field with an implicit length prefix. Flag to channelize 
     * will be set to the value of shouldDoChannels() and channel table to
     * value of getChannelTable();
     * 
     * @param name name of the field
     * @param dataType data type of the field
     * @param chanTable channel definition provider
     * @param lengthLength byte length of the length prefix
     * @return new field instance
     */
    default ISimpleField createSimpleField(final String name, final DecomDataType dataType, final int lengthLength) {
    		return createSimpleField(name, dataType, getChannelTable(), isShouldDoChannels(), lengthLength);
    }

    /**
     * Creates a simple field. Flag to channelize 
     * will be set to the value of shouldDoChannels() and channel table to
     * value of getChannelTable();
     * 
     * @param name name of the field
     * @param dataType data type of the field
     * @param chanTable channel definition provider
     * @param lengthLength byte length of the length prefix
     * @return new field instance
     */
    default ISimpleField createSimpleField(final String name, final DecomDataType dataType) {
    		return createSimpleField(name, dataType, getChannelTable(), isShouldDoChannels());
    }
    
	
	/**
	 * Creates a stream field.
	 * 
	 * @param name name of the field
	 * @param maxlength maximum byte length of the field
     * @param display the display preference for the field: should be "text",
     *            "hexdump", or "none".
	 * @return new field instance
	 */
	IProductDecomField createStreamField(String name, int maxlength, String display);
	
    /**
     * Creates an array field.
     * 
     * @param name name of the field
     * @param maxlength maximum byte length of the array
     * @return new field instance
     */
    IArrayField createArrayField(final String name, final int maxlength);
    
    /**
     * Creates an array field with an implicit length prefix.
     * 
     * @param name name of the field
     * @param maxlength maximum byte length of the array
     * @param lengthLength the byte length of the prefix 
     * @return new field instance
     */
    IArrayField createArrayField(final String name, final int maxlength, final int lengthLength);
    
    /** 
     * Creates an array field that gets the array length from another simple field.
     * 
     * @param name name of the field
     * @param lengthField reference to the length field
     * @return new field instance
     */
    IArrayField createArrayField(final String name, final ISimpleField lengthField);
	
	/**
	 * Creates a structure field.
	 * 
	 * @param name name of the field
	 * @return new field instance
	 */
	IStructureField createStructureField(final String name);
}
