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
package jpl.gds.dictionary.impl.evr;

import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.evr.EvrDefinitionType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.serialization.evr.Proto3EvrDefinition;
import jpl.gds.serialization.evr.Proto3EvrDefinition.HasModuleCase;
import jpl.gds.serialization.evr.Proto3EvrDefinition.HasNameCase;
import jpl.gds.serialization.evr.Proto3EvrDefinition.HasOpsCatCase;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * AbstractEvrDefinition contains the shared portions of the definition of a single Evr type.
 * This class is extended to create mission-specific EVR classes.
 *
 *
 */
public abstract class AbstractEvrDefinition implements IEvrDefinition {
	/** Shared logger instance */
    protected static final Tracer    debugTracer  = TraceManager.getTracer(Loggers.TLM_EVR);


    private long                     id;
    private String                   level;
	private String name;
	private String formatString;
	private final KeyValueAttributes keyValueAttr = new KeyValueAttributes();
	private EvrDefinitionType defType = EvrDefinitionType.FSW;

	/**
	 * Basic constructor.
	 * 
	 */
	AbstractEvrDefinition() {
		super();
		this.id = 0;
		this.level = null;
		this.name = null;
		this.formatString = null;
	}

   
     /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
        return "EvrDefinition[name=" + this.name
        					   + ",id=" + this.id
                               + ",level=" + this.level
                               + ",format=" + this.formatString
                               + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormatString() {
        return this.formatString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFormatString(final String formatString) {
        this.formatString = formatString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getId() {
        return this.id;
    }

    /**
	 * {@inheritDoc}
     */
    @Override
    public void setId(final long id) {
        this.id = id;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getLevel() {
        return this.level;
    }

    /**
	 * {@inheritDoc}
     */
    @Override
    public void setLevel(final String level) {
        this.level = level;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public String getModule() {
        return this.getCategory(MODULE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /*Replaced with ICategorySupport. */
    public void setModule(final String module) {
        this.setCategory(MODULE, module);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public String getOpsCategory() {
        return this.getCategory(OPS_CAT);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public void setSubsystem(final String subsys) {
        this.setCategory(SUBSYSTEM, subsys);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public String getSubsystem() {
        return this.getCategory(SUBSYSTEM);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public void setOpsCategory(final String opscat) {
        this.setCategory(OPS_CAT, opscat);
    }
    	/**
	 * Gets the name of this EVR definition.
	 * 
	 * @return the name
	 */
	@Override
    public String getName()
	{
		return (this.name);
	}

	/**
	 * Sets the name of this EvR definition
	 *
	 * @param name The name to set.
	 */
	@Override
    public void setName(final String name)
	{
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
     * @{inheritDoc}
     */
	@Override
	public void setKeyValueAttribute(final String key, final String value) {
		keyValueAttr.setKeyValue(key, value);
	}

	/**
     * @{inheritDoc}
     */
	@Override
	public String getKeyValueAttribute(final String key) {
		return keyValueAttr.getValueForKey(key);
	}

    /**
     * @{inheritDoc}
     */
	@Override
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}

	/**
     * @{inheritDoc}
     */
	@Override
	public void setKeyValueAttributes(final KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

	/** {@inheritDoc}
	 *
	 */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();		
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
    public void setDefinitionType(final EvrDefinitionType type) {
	    if (type == null) {
	        throw new IllegalArgumentException("EVR definition type may not be null");
	    }
	    this.defType = type;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
    public EvrDefinitionType getDefinitionType() {
	    return this.defType;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.evr.adaptation.IEvrDefinition#build()
	 */
	@Override
	public Proto3EvrDefinition build(){
		final Proto3EvrDefinition.Builder retVal = Proto3EvrDefinition.newBuilder();
		
		retVal.setLevel(this.level);
		final String mod = this.getCategory(IEvrDefinition.MODULE);
		if(mod != null){
			retVal.setModule(mod);
		}
		final String opsCat = this.getCategory(IEvrDefinition.OPS_CAT);
		if(opsCat != null){
			retVal.setOpsCat(opsCat);
		}
		if(this.name != null){
		    retVal.setName(this.name);
		}
		retVal.setEventId(this.id);
		
		return retVal.build();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.evr.adaptation.IEvrDefinition#load(Proto3EvrDefinition)
	 */
	@Override 
	public void load(final Proto3EvrDefinition msg){
		this.setLevel(msg.getLevel());
        if (msg.getHasModuleCase().equals(HasModuleCase.MODULE)) {
            this.setCategory(IEvrDefinition.MODULE, msg.getModule());
        }
        if (msg.getHasOpsCatCase().equals(HasOpsCatCase.OPSCAT)) {
            this.setCategory(IEvrDefinition.OPS_CAT, msg.getOpsCat());
        }
        if (msg.getHasNameCase().equals(HasNameCase.NAME)) {
            this.setName(msg.getName());
        }
		this.setId(msg.getEventId());
	}
	
}
