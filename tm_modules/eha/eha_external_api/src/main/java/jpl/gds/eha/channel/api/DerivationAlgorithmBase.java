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
package jpl.gds.eha.channel.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.annotation.AmpcsLocked;
import jpl.gds.shared.annotation.CustomerExtensible;
import jpl.gds.shared.annotation.Mutator;


/**
 * DerivationAlgorithmBase is an abstract class that implements the
 * IDerivationAlgorithm interface. It is to be used by customers as a base class
 * for building custom algorithmic channel derivation classes. The subclass must
 * define the deriveChannels() method. Override of the init() and cleanup()
 * methods is optional. This class provides base implementations for the other
 * methods in the IDerivationAlgorithm interface, including the ability to get
 * and set parent channel IDs, and to get and set algorithm parameters.
 * <p>
 * If the subclass overrides the init() or cleanup() methods, it must invoke the
 * super method.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * 11/15/17 Extend new base class GeneralAlgorithmBase
 *          Methods to get LAD values and create channel values moved to the new
 *          base class.
 *
 *
 * @see IDerivationAlgorithm
 */
@CustomerExtensible(immutable = false)
public abstract class DerivationAlgorithmBase extends GeneralAlgorithmBase implements IDerivationAlgorithm
{
	private int returnValue;
	private List<String> parents = new ArrayList<>(16);
	private String derivationId;
    private IChannelSampleFactory sampleFactory;

	/*
	 * The two data structures below are duplicates. parametersList allows
	 * us to pick out entries by their order, and parametersMap allows us
	 * to pick out entries by a key.
	 */
	private List<String> parametersList = new ArrayList<>(16);
	private Map<String, String> parametersMap = new HashMap<>(16);

	/**
	 * Constructor.
	 */
	protected DerivationAlgorithmBase()
	{
		super();
	}

	@Override
	public void init() {
		returnValue = 0;
	}


	@Override
	public void cleanup() {}

	@Override
    public Map<String, String> getParametersMap()
	{
		return(parametersMap);
	}

	@Override
    public List<String> getParametersList()
	{
		return(parametersList);
	}
	

	@Override
    @Mutator
    @AmpcsLocked
	public void setParameters(final List<String> parametersList, final Map<String, String> parametersMap)
	{
        /** 11/30/18: Removed SecurityManager checks for performance improvements */
		this.parametersList = parametersList;
		this.parametersMap = parametersMap;
	}

	@Override
    public String getParameter(final int index)
	{
		return(parametersList.get(index));
	}

	@Override
    public String getParameter(final String key)
	{
		return(parametersMap.get(key));
	}

	@Override
    public String getDerivationId()
	{
		return derivationId;
	}


	@Override
    @Mutator
    @AmpcsLocked
	public void setDerivationId(final String derivationId)
	{
        /** Removed SecurityManager checks for performance improvements */
		this.derivationId = derivationId;
	}

	@Override
    @Mutator
    @AmpcsLocked
	public void setParents(final List<String> parents)
	{
        /** Removed SecurityManager checks for performance improvements */
		this.parents = parents;
	}
    

	@Override
    public List<String> getParents()
	{
		return(parents);
	}


	@Override
    public void setReturnValue(final int returnValue)
	{
		this.returnValue = returnValue;
	}

	@Override
	public int getReturnValue()
	{
		return returnValue;
	}
   
    @Override
    @Mutator
    @AmpcsLocked
    public void setSampleFactory(final IChannelSampleFactory factory) {
        /** Removed SecurityManager checks for performance improvements */
        this.sampleFactory = factory;
    }
    

    @Override
    public IChannelValue createChannelValue(final String ci,
            final Number value) {
        
        if (sampleFactory == null) {
            throw new IllegalStateException("Channel sample factory is null");
        }

        return sampleFactory.create(ci, value);
    }

    @Override
    public IChannelValue createChannelValue(final String ci,
            final String value) {
        
        if (sampleFactory == null) {
            throw new IllegalStateException("Channel sample factory is null");
        }

        return sampleFactory.create(ci, value);
    }

    @Override
    public IChannelValue createChannelValue(final String ci,
            final boolean value) {
        
        if (sampleFactory == null) {
            throw new IllegalStateException("Channel sample factory is null");
        }

        return sampleFactory.create(ci, value);
    }


}
