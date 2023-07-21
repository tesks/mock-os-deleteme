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

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.station.api.dsn.chdo.IChdoConfiguration;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoProperty;

/**
 * This is an abstract class that implements the IChdoDictionary Interface
 * These methods are used by ChdoDictionary classes
 * 
 * This abstract class also reads an alias configuration file to create 
 * a HashMap in order to remove redundancies in the ChdoFieldDefinitions names
 * 
 *
 */
public abstract class AbstractChdoConfiguration implements IChdoConfiguration  {
    
    /** Shared logger instance */
    protected final Tracer                        log        = TraceManager.getDefaultTracer();

	
    /** map of CHDO type to definition */
	protected final Map<Integer, IChdoDefinition> typeToDefinitionMap;
	/** map of CHDO property name to CHDO property object */
	protected final Map<String, IChdoProperty> nameToPropertyMap;
	/** list of CAIDs */
	protected final List<String> controlAuthorityIds;

	
	/**
	 * Constructor
	 */
	protected AbstractChdoConfiguration()  { 
		super();
		typeToDefinitionMap = new HashMap<Integer, IChdoDefinition>();
		nameToPropertyMap = new HashMap<String, IChdoProperty>();
		controlAuthorityIds = new ArrayList<String>(5);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IChdoDefinition getDefinitionByType(final int type) { 
		return typeToDefinitionMap.get(Integer.valueOf(type));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Integer, IChdoDefinition> getTypeToDefinitionMap() { 
		return typeToDefinitionMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getControlAuthorityIds() { 
		return(controlAuthorityIds.toArray(new String[] { }));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IChdoProperty getPropertyByName(String propertyName) { 
		return nameToPropertyMap.get(propertyName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, IChdoProperty> getNameToPropertyMap() { 
		return nameToPropertyMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		typeToDefinitionMap.clear();
		nameToPropertyMap.clear();
		controlAuthorityIds.clear();
	}
}