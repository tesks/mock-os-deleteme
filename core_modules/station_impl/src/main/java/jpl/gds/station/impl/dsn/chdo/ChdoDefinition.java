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
package jpl.gds.station.impl.dsn.chdo;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition;

/**
 * This class represents the definition of a specific Compressed Header Data Object (CHDO) 
 * type.
 * 
 *
 *
 * 		**NOTE** The byteSize attribute for each CHDO structure is 
 * currently hidden. It can be displayed if requested by replacing the @XmlTransient 
 * notation with @XmlAttribute Before doing so, double check the byteSize math for CHDO's
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
class ChdoDefinition implements IChdoDefinition
{
	@XmlAttribute private int type;	
	@XmlAttribute private String name;
	@XmlAttribute private String classification;
	
    /** Size of CHDO in bytes (currently hidden from xml output*/
    @XmlTransient private int byteSize = 0;
	
	@XmlElement(name = "FieldList")
	@XmlJavaTypeAdapter(ChdoXmlAdapter.class)
	private final Map<String,IChdoFieldDefinition> fieldNameToDefMap;


	/**
	 * Basic constructor.
	 */
	/* package */ ChdoDefinition()
	{
		this.fieldNameToDefMap = new HashMap<String,IChdoFieldDefinition>();
	}
	
	
	/**
	 * ChdoDefinition constructor
	 * @param id Chdo type (integer)
	 * @param classify Chdo classification
	 * @param name Chdo name
	 */
	/* package */ ChdoDefinition(final int id, final String classify, final String name) 
	{ 
		this.type = id;
		this.classification = classify;
		this.name = name;
		this.fieldNameToDefMap = new HashMap<String, IChdoFieldDefinition>();
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void addFieldMapping(final String fieldName, final IChdoFieldDefinition fieldValue)
	{
		this.fieldNameToDefMap.put(fieldName,fieldValue);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getFieldDefinitionByName(java.lang.String)
     */
	@Override
    public IChdoFieldDefinition getFieldDefinitionByName(final String fieldName)
	{
		final IChdoFieldDefinition value = this.fieldNameToDefMap.get(fieldName);
		return(value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o)
	{
		if((o instanceof IChdoDefinition) == false)
		{
			return(false);
		}
		
		final IChdoDefinition def = (IChdoDefinition)o;
		return(def.getType() == this.type);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return(this.type);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getFieldNames()
     */
	@Override
    public String[] getFieldNames()
	{
		final String[] names = this.fieldNameToDefMap.keySet().toArray(new String[] { });
		return(names);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getClassification()
     */
	@Override
    public String getClassification()
	{
		return this.classification;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#setClassification(java.lang.String)
     */
	@Override
    public void setClassification(final String classification)
	{
		this.classification = classification;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getName()
     */
	@Override
    public String getName()
	{
		return this.name;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#setName(java.lang.String)
     */
	@Override
    public void setName(final String name)
	{
		this.name = name;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getType()
     */
	@Override
    public int getType()
	{
		return this.type;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#setType(int)
     */
	@Override
    public void setType(final int type)
	{
		this.type = type;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuffer buffer = new StringBuffer(1024);
		
		buffer.append("CHDO (Type=\"");
	    buffer.append(this.type);
	    buffer.append("\", Classification=\"");
	    buffer.append(this.classification);
	    buffer.append("\", Name=\"");
	    buffer.append(this.name);
	    buffer.append("\")\n");
	    
	    final String[] names = getFieldNames();
	    for(int i=0; i < names.length; i++)
	    {
	    	buffer.append("\t");
	    	buffer.append(this.fieldNameToDefMap.get(names[i]));
	    	buffer.append("\n");
	    }
		
		return(buffer.toString());
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#getByteSize()
     */
    @Override
    public int getByteSize()
    {
        return byteSize;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoDefinition#setByteSize(int)
     */
    @Override
    public void setByteSize(final int size)
    {
        byteSize = size;
    }
}
