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
package jpl.gds.dictionary.impl.channel;


import java.util.Map;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.channel.ChannelDefinitionFactory;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.shared.template.Templatable;

/**
 * ChannelDefinition encapsulates all the attributes of a single telemetry
 * channel as read from the channel dictionary. It is essentially the
 * multi-mission definition of a channel. It implements the IChannelDefinition
 * interface for access in mission adaptations.
 * <p>
 * Instances of this class must be created via ChannelDefinitionFactory. All
 * references to instance should be via the IChannelDefinition interface rather
 * than via this class.
 * 
 *
 * @see ChannelDefinitionFactory
 */
public class ChannelDefinition implements Templatable, IChannelDefinition {

    
    private String id;
    private String title = "";
    private String description;
    private int index = NO_INDEX;
    private String name;
    private String groupId;
    private ChannelType channelType = ChannelType.UNKNOWN;
    private String dnFormat = null;
    private String euFormat = null;
    private String dnUnits = "";
    private String euUnits = "";
    private int size; 
    private EnumerationDefinition states;
    private IEUDefinition dnToEu;
    private boolean hasAnEu;
    private boolean isChannelDerived;
    private DerivationType derivationType = DerivationType.NONE;
    private ChannelDefinitionType definitionType = ChannelDefinitionType.FSW;
    private String sourceDerivationId = null;
	private final KeyValueAttributes keyValueAttr = new KeyValueAttributes();
    private final Categories categories = new Categories();

    /**
     * Creates an instance of ChannelDefinition with the given data type.
     * 
     * @param ct the ChannelType for the new channel
     * 
     *
     */
    ChannelDefinition(final ChannelType ct) {
        channelType = ct;
    }

    /**
     * Creates an instance of ChannelDefinition with the given data type and channel ID.
     * 
     * @param ct the ChannelType for the new channel
     * @param cid the id of the new channel
     * 
     */
    ChannelDefinition(final ChannelType ct, final String cid) {
        this(ct);
        setId(cid);
    }

    /**
     * Creates an instance of an untyped ChannelDefinition with the given ID.
     * 
     * @param cid the id of the new channel
     * 
     */
    ChannelDefinition(final String cid) {
        setId(cid);
    }



    @Override
    public String getId() {
        return id;
    }


    @Override
    public void setId(final String id) {
        this.id = id;
    }

   
    @Override
    public void setDescription(final String desc) {
        description = desc;
    }


    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIndex(final int idx) {
        index = idx;
    }


    @Override
    public int getIndex() {
        return index;
    }


    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public String getOpsCategory() {
        return getCategory(OPS_CAT);
    }


    @Override
    @Deprecated /*  Replaced with ICategorySupport. */
    public void setOpsCategory(final String opsCategory) {
        setCategory(OPS_CAT, opsCategory);
    }

    
    @Override
    public String getTitle() {
        return title;
    }

  
    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    /*
     * Removed methods to get/set FSW description. We do not
     * need two types of descriptions.
     */
 

    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public void setModule(final String mod) {
        setCategory(IChannelDefinition.MODULE, mod);
    }

  
    @Override
    @Deprecated /*  Replaced with ICategorySupport. */
   public String getModule() {
        return getCategory(IChannelDefinition.MODULE);
    }


    @Override
    public void setName(final String fsn) {
        name = fsn;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setGroupId(final String groupName) {
        groupId = groupName;
    }

    
    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    @Deprecated /* eplaced with ICategorySupport. */
    public void setSubsystem(final String subs) {
        setCategory(SUBSYSTEM, subs);
    }


    @Override
    @Deprecated /*  Replaced with ICategorySupport. */
    public String getSubsystem() {
        return getCategory(SUBSYSTEM);
    }


    @Override
	public ChannelDefinitionType getDefinitionType() {
        return definitionType;
    }


    @Override
	public void setDefinitionType(final ChannelDefinitionType definitionType) {
        this.definitionType = definitionType;
    }


    @Override
    public ChannelType getChannelType() {
        return (channelType);
    }


    @Override
    public void setChannelType(final ChannelType type) {
        channelType = type;
    }


    @Override
    public String getDnFormat() {
        return dnFormat;
    }


    @Override
    public String getEuFormat() {
        return euFormat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDnFormat(final String _dn) {
        if (_dn == null || _dn.equals("")) {
            dnFormat = null;
        } else {
            dnFormat = _dn;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEuFormat(final String _eu) {
        if (_eu == null || _eu.equals("")) {
            euFormat = null;
        } else {
            euFormat = _eu;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDnUnits() {
        return dnUnits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEuUnits() {
        return euUnits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDnUnits(final String _dn) {
        dnUnits = _dn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEuUnits(final String _eu) {
        euUnits = _eu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSize(final int _sz) {
        size = _sz;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return size;
    }
    
    /*
     *  Moved getStatus() method to AbstractChannelValue.
     */


    @Override
    public EnumerationDefinition getLookupTable() {
        return states;
    }


    @Override
    public void setLookupTable(final EnumerationDefinition def) {
        states = def;
    }




    @Override
    public boolean hasEu() {
        return hasAnEu;
    }


    @Override
    public void setHasEu(final boolean yes) {
        hasAnEu = yes;
    }


    @Override
    public IEUDefinition getDnToEu() {
        return dnToEu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDnToEu(final IEUDefinition eu) {
        dnToEu = eu;
        if (dnToEu != null) {
            hasAnEu = true;
        }
    }


    @Override
    public String toString() {
        /**  Added derived flag to output. **/
        return "CHID: " + getId() + " type: " + this.getChannelType() + " index: " + 
        this.getIndex() + " is" + (this.isDerived() ? "" : " not") + " derived";
    }



    @Override
    public void setTemplateContext(final Map<String,Object> map) {

        if (id != null) {
            map.put("channelId", id);
        } else {
            map.put("channelId", "");
        }
        if (getCategory(MODULE) != null) {
            map.put("module", getCategory(MODULE));
        } else {
            map.put("module", "");
        }
        if (getCategory(SUBSYSTEM) != null) {
            map.put("subsystem", getCategory(SUBSYSTEM));
        } else {
            map.put("subsystem", "");
        }
        if (title != null) {
            map.put("title", title);
        } else {
            map.put("title", "");
        }  

        if (getCategory(OPS_CAT) != null) {
            map.put("opsCategory", getCategory(OPS_CAT));
        } else {
            map.put("opsCategory", "");
        }    
        /*
         *  Set both old and new "name" hash entries.
         */
        if (name != null) {
            map.put("fswName", name); //Deprecated for R8
            map.put("name", name);
        } else {
            map.put("fswName", ""); //Deprecated for R8
            map.put("name", "");
        }

        map.put("index", getIndex());

        map.put("channelType", getChannelType());
        if (dnUnits != null) {
            map.put("dnUnits", dnUnits);
        } else {
            map.put("dnUnits", "");
        }
        if (euUnits != null) {
            map.put("euUnits", euUnits);
        } else {
            map.put("euUnits", "");
        }
    }
    


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDerived() {
        return isChannelDerived;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDerived(final boolean isDerived) {
        this.isChannelDerived = isDerived;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DerivationType getDerivationType() {
        return derivationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDerivationType(final DerivationType derivationType) {
        this.derivationType = derivationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ChannelDefinition)) {
            return false;
        }
        if (id == null) {
            return false;
        }
        return id.equals(((ChannelDefinition)obj).getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (id == null) {
            return 0;
        }
        return id.hashCode();
    }

    @Override
    public String getSourceDerivationId() {
        return this.sourceDerivationId;
    }


    @Override
    public void setSourceDerivationId(final String sourceId) {
        this.sourceDerivationId = sourceId;
    }

    /**
     * {@inheritDoc}
     */
	@Override	
	public void setKeyValueAttribute(final String key, final String value) {
		keyValueAttr.setKeyValue(key, value);
	}

	/**
     * {@inheritDoc}
     */
	@Override	
	public String getKeyValueAttribute(final String key) {
		return keyValueAttr.getValueForKey(key);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}
	
	/**
     * {@inheritDoc}
     */
	@Override	
	public void setKeyValueAttributes(final KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

    /**
     * {@inheritDoc}
     *
     */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategories(final Categories map) {
        categories.copyFrom(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Categories getCategories() {
        return categories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategory(final String catName, final String catValue) {
        categories.setCategory(catName, catValue);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory(final String name) {
        return categories.getCategory(name);
    }
    
}
