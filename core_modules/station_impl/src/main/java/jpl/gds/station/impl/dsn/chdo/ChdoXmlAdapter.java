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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import jpl.gds.station.api.dsn.chdo.ChdoFieldFormatEnum;
import jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition;

/**
 * XmlAdapter class for marshaling CHDO Java objects using JAXB
 * This class overrides the default marshaler to parse the CHDO
 * Objects into an easily-readable XML String
 * 
 */
class ChdoXmlAdapter extends XmlAdapter< ChdoXmlAdapter.ChdoMapElement, Map<String, ChdoFieldDefinition> >{ 
	
	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
	 */
	@Override
	public Map<String, ChdoFieldDefinition> unmarshal(ChdoMapElement v) throws Exception {
		if ( v == null) return null;
		
		Map<String, ChdoFieldDefinition> FieldNameToDefMap = new HashMap<String, ChdoFieldDefinition>();
		for(ChdoEntry e: v.entries) { 
			ChdoFieldDefinition def = new ChdoFieldDefinition(e.fieldId, e.bitLength, e.bitOffset, e.byteOffset);	
			
			def.setFieldFormat(e.format);
			if (e.min != null && e.max != null) { 
				def.setMinValue(e.min);
				def.setMaxValue(e.max);
			} 
			else if (e.fixedValue != null)
				def.setFixedValue(e.fixedValue);
			else if (e.defaultValue != null)
				def.setDefaultValue(e.defaultValue);
			
			FieldNameToDefMap.put(e.fieldId, def);
		}
		return FieldNameToDefMap;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
	 */
	@Override
	public ChdoMapElement marshal(Map<String, ChdoFieldDefinition> v) throws Exception {
		if (v == null || v.isEmpty()) return null;
		
		ChdoMapElement map = new ChdoMapElement();
		for(String key: v.keySet()) { 
			map.addEntry(key, v.get(key));
		}
		return map;
	}

	
	/**
	 * Internal class used for XML parsing of CHDO objects 
	 */
	public static class ChdoEntry { 
		
		@XmlAttribute public String fieldId;
		@XmlAttribute public int bitLength;
		@XmlAttribute public int byteOffset;
		@XmlAttribute public int bitOffset;
		@XmlAttribute public ChdoFieldFormatEnum format;

	    /** For integer types, min and max values.
	     *  Range can have default, or field can have
	     *  a fixed value.
	     */
	    @XmlAttribute public Long min = null;
	    @XmlAttribute public Long max = null;
	    @XmlAttribute public Long defaultValue = null;
	    @XmlAttribute public Long fixedValue   = null;
		
	    /**
	     * Default ChdoEntry constructor 
	     * Initializes fields
	     */
		public ChdoEntry() { 
			fieldId = null;
			bitLength = 0;
			byteOffset = 0;
			bitOffset = 0;
			format = null;
		}
		
		/**
		 * ChdoEntry constructor 
		 * @param fid fieldId
		 * @param bitL bitLength
		 * @param bitO bitOffset
		 * @param byteO byteOffset
		 * @param en FieldFormat enumeration (TIME or UNSIGNED_INTEGER)
		 */
		public ChdoEntry(String fid, int bitL, int bitO, int byteO, ChdoFieldFormatEnum en) { 
			this.fieldId = fid;
			this.bitLength = bitL;
			this.bitOffset = bitO;
			this.byteOffset = byteO;
			this.format = en;
		}

		/**
		 * Setter for the minimum ChdoEntry value
		 * @param minValue minimum value
		 */
		public void setMin(Long minValue) {
			this.min = minValue;
		}

		/**
		 * Setter for the maximum ChdoEntry value
		 * @param maxValue maximum value
		 */
		public void setMax(Long maxValue) {
			this.max = maxValue;
		}

		/**
		 * Setter for the default ChdoEntry value
		 * @param defaultValue default value
		 */
		public void setDefaultValue(Long defaultValue) {
			this.defaultValue = defaultValue;
		}

		/**
		 * Setter for the fixed ChdoEntry value 
		 * @param fixedValue fixed value
		 */
		public void setFixedValue(Long fixedValue) {
			this.fixedValue = fixedValue;
		}
	} // End ChdoEntry
	
	/**
	 * Internal class used for XML parsing of CHDO objects
	 * ChdoMapElement is a container for ChdoEntry's
	 */
	public static class ChdoMapElement { 
		
		@XmlElement(name = "Field")
		public List<ChdoEntry> entries = new ArrayList<ChdoEntry>();
		
		/**
		 * Adds a ChdoEntry to the entries ArrayList
		 * @param fieldId ChdoDefinition fieldId
		 * @param def ChdoFieldDefinition 
		 */
		public void addEntry(String fieldId, IChdoFieldDefinition def) { 
			ChdoEntry entry = new ChdoEntry(fieldId, def.getBitLength(), def.getBitOffset(), 
					def.getByteLength(), def.getFieldFormat() );
			
			if (def.getMinValue() != null) { 
				entry.setMin(def.getMinValue());
				entry.setMax(def.getMaxValue());
			}
			else if (def.getDefaultValue() != null)
				entry.setDefaultValue(def.getDefaultValue());		
			else if (def.getFixedValue() != null)
				entry.setFixedValue(def.getFixedValue());
			
			entries.add(entry);
		}
	} // End ChdoMapElement

} // End ChdoXmlAdapter