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
package jpl.gds.eha.impl.service.channel;

import jpl.gds.decom.IDecomDelegate;
import jpl.gds.decom.algorithm.ChannelValueBuilder;
import jpl.gds.decom.algorithm.DecomArgs;
import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.types.*;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelizationListener;
import jpl.gds.eha.impl.channel.*;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.*;
import jpl.gds.shared.types.BitBuffer;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Decom listener that creates channel values from decom events.  Does not publish new channel values
 * itself; merely creates the values and then provides its internal list to the owner of the
 * listener object.
 * 
 * Channel values are not necessarily stamped with the same ISclk value; the listener
 * caches visited SCLK values.  This makes the listener stateful; it is not thread safe
 * and should not be shared.  It can, however, be reused for multiple sequential
 * invocations of decom.
 * 
 * If {@link #setCurrentTimes(ISclk, IAccurateDateTime, IAccurateDateTime, int)} is not called, this class
 * stamps channel values with zero SCLKs and dummy SCETs.
 * 
 * Class is not thread safe.
 * 
 *
 */
public class ChannelizationListener extends AbstractChannelDecomListener implements  IDecomDelegate, IChannelizationListener
{
	protected Tracer log;
	
	private List<IServiceChannelValue> cvList = new ArrayList<IServiceChannelValue>();

    /**
     * current spacecraft clock
     */
	protected ISclk currentSclk = Sclk.MIN_SCLK;

	/* 
	 * These fields are only needed when the listener is performing SCET conversion, which will not
	 *  be applicable for station data and possibly some other types of data. 
	 */

	/**
	 * Cached SCET value to apply to new telemetry being created
	 */
	protected IAccurateDateTime currentScet = new AccurateDateTime(true);

	/**
     * The one ERT for channels currently being created. This class should not change this value itself -
     * It should only change via a call to {@link #setCurrentTimes(ISclk, IAccurateDateTime, IAccurateDateTime, int)}.
     * There is no reason for decom to somehow encounter a new earth receive time for telemetry inside of data.
     */
	protected IAccurateDateTime currentErt = new AccurateDateTime(true);
	
	/** SCID needed for sclk-scet conversions.*/
	protected int scid = 0;


    private final IChannelValueFactory chanFactory;
	
	/* End fields needed for SCET conversions */

	// Multimission EVR fields
	// =======================
	protected int apid;
	protected Integer vcid;
	protected int dssId;
	protected int seqCount;
	// ========================


	/**
     * Create an instance that will use the given channel dictionary to obtain
     * definitions.
     * 
     * @param channelLookup
     *            the channel definition table providing channel id to channel
     *            definition mappings. Must already be initialized.
     * @param chanFactory
     *            factory for creating channel values
     */
	 ChannelizationListener(final Map<String, IChannelDefinition> channelLookup,
								  final IChannelValueFactory chanFactory) {
		this(channelLookup, chanFactory, TraceManager.getTracer(Loggers.TLM_EHA));
	 }

    /**
     * Constructor.
     * 
     * @param appContext
     *            the current application context
     */
    public ChannelizationListener(final ApplicationContext appContext) {
    	this(appContext.getBean(IChannelDefinitionProvider.class).getChannelDefinitionMap(),
				appContext.getBean(IChannelValueFactory.class),
				TraceManager.getTracer(appContext, Loggers.TLM_EHA));
    }

	/**
	 * Create an instance that will use the given channel dictionary to obtain
	 * definitions.
	 *
	 * @param channelLookup
	 *            the channel definition table providing channel id to channel
	 *            definition mappings. Must already be initialized.
	 * @param chanFactory
	 *            factory for creating channel values
	 * @param tracer Logger
	 */
	 ChannelizationListener(final Map<String, IChannelDefinition> channelLookup,
								  final IChannelValueFactory chanFactory, Tracer tracer) {
    	super(channelLookup);
    	this.chanFactory = chanFactory;
    	this.log = tracer;
	}

	private void createChannelValue(final IChannelizableDataDefinition def, final Supplier<IServiceChannelValue> create) {

	    final IChannelDefinition channelDef = findChannelValue(def);
	    if (channelDef != null) {
	        final IServiceChannelValue chanVal = create.get();
	        chanVal.setSclk(currentSclk);
	        chanVal.setScet(currentScet);
	        cvList.add(chanVal);
	    }
	}
	
	
	    /**
     * {@inheritDoc}
     */
	@Override
    public void setCurrentTimes(final ISclk sclk, final IAccurateDateTime scet, final IAccurateDateTime ert, final int scid) {
		this.currentSclk = sclk;
		this.currentScet = scet;
		this.currentErt = ert;
		this.scid = scid;
	}

	/**
	 * Set the parameters that will be used as the basis
	 * for time tagging new channel values and evrs.
	 * @param pm packetInfo from the packet
	 */
	public void setPacketInfo(ITelemetryPacketInfo pm) {
		this.currentSclk = pm.getSclk();
		this.currentScet = pm.getScet();
		this.currentErt = pm.getErt();
		this.scid = pm.getScid();
		this.apid = pm.getApid();
		this.vcid = pm.getVcid();
		this.dssId = pm.getDssId();
		this.seqCount = pm.getSeqCount();
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IServiceChannelValue> collectChannelValues() {
		final List<IServiceChannelValue> returnList = this.cvList;
		this.cvList = new ArrayList<>();
		return returnList;
	}

	@Override
	public void onInteger(final IIntegerDefinition def, final long value) {
	    final IChannelDefinition channelDef = findChannelValue(def);
	    if (def != null && channelDef != null) {
	        createChannelValue(def, () -> { return chanFactory.createServiceChannelValue(channelDef, Long.valueOf(value));
	        });
	    }
	        
//			if (def.isUnsigned()) {
//				return chanFactory.create(channelDef, Long.valueOf(value));
//			} else {
//				return new IntegerChannelValue(value);
//			}
	}

	@Override
	public void onString(final IStringDefinition def, final String value) {
	    final IChannelDefinition channelDef = findChannelValue(def);
	    if (def != null && channelDef != null) {
	        createChannelValue(def, () -> {
	            return chanFactory.createServiceChannelValue(channelDef, value);
	        });
	    }
	}

	@Override
	public void onBoolean(final IBooleanDefinition def, final boolean value) {
	    final IChannelDefinition channelDef = findChannelValue(def);
	    if (def != null && channelDef != null) {
	        createChannelValue(def, () -> {
	            return chanFactory.createServiceChannelValue(channelDef, value);
	        });
	    }
	}
	
	@Override
    public void onFloat(final IFloatingPointDefinition def, final float val) {
	    final IChannelDefinition channelDef = findChannelValue(def); 
	    if (def != null && channelDef != null) {
	        createChannelValue(def, () -> {
	            return chanFactory.createServiceChannelValue(channelDef, Float.valueOf(val));
	        });
	    }
	}


	@Override
	public void onDouble(final IFloatingPointDefinition def, final double value) {
	    final IChannelDefinition channelDef = findChannelValue(def);
	    if (def != null && channelDef != null) {
	        createChannelValue(def, () -> {
	            return chanFactory.createServiceChannelValue(channelDef, Double.valueOf(value));
	        });
	    }
	}
	
	@Override
	public void onTime(final ITimeDefinition def, final ISclk sclk) {
		if (def.isDelta()) {
			currentSclk = currentSclk.increment(sclk.getCoarse(), sclk.getFine());
		} else {
			currentSclk = sclk;
		}
		currentScet = SclkScetUtility.getScet(currentSclk, currentErt, scid);
	}
	
	@Override
	public void onEnum(final IEnumDataDefinition def, final int value) {
	    final IChannelDefinition channelDef = findChannelValue(def);
        if (def != null && channelDef != null) {
            createChannelValue(def, () -> {
                return chanFactory.createServiceChannelValue(channelDef, Integer.valueOf(value));
            });
        }		
	}
	
	@Override
	public void onChannel(final IChannelStatementDefinition statement, final long val) {
		IServiceChannelValue chanVal;
		chanVal = new IntegerChannelValue(val);
		chanVal.setChannelDefinition(statement.getChannelDefinition());
		chanVal.setSclk(currentSclk);
		chanVal.setScet(currentScet);
		cvList.add(chanVal);
	}

	@Override
	public void onChannel(final IChannelStatementDefinition statement, final float val) {
		final IServiceChannelValue chanVal = new FloatChannelValue(val);
		chanVal.setChannelDefinition(statement.getChannelDefinition());
		chanVal.setSclk(currentSclk);
		chanVal.setScet(currentScet);
		cvList.add(chanVal);
		
	}

	@Override
	public void onChannel(final IChannelStatementDefinition statement, final double val) {
		final IServiceChannelValue chanVal = new FloatChannelValue(val);
		chanVal.setChannelDefinition(statement.getChannelDefinition());
		chanVal.setSclk(currentSclk);
		chanVal.setScet(currentScet);
		cvList.add(chanVal);
	} 

	@Override
	public void onChannel(final IChannelStatementDefinition statement, final String val) {
		final IServiceChannelValue chanVal = new ASCIIChannelValue(val);
		chanVal.setChannelDefinition(statement.getChannelDefinition());
		chanVal.setSclk(currentSclk);
		chanVal.setScet(currentScet);
		cvList.add(chanVal);
	};

	@Override
	public void decom(final IDecommutator algorithm, final BitBuffer data, final Map<String, Object> args) throws DecomException {
		args.put(DecomArgs.SCLK, currentSclk);
		algorithm.decom(data, args);
		final List<ChannelValueBuilder> channels = algorithm.collectChannelValues();
		for (final ChannelValueBuilder builder : channels) {
			final IChannelDefinition def = channelLookup.get(builder.getChannelId());
			if (def == null) {
				log.warn(String.format("Custom decom algorithm %s produced an unknown channel ID: %s", algorithm.getClass().getName(), builder.getChannelId()));
				continue;
			}
			IServiceChannelValue val;
			switch(def.getChannelType()) {
	        case SIGNED_INT:
	        case STATUS:
	            val = new IntegerChannelValue();
	            break;
	        case DIGITAL:
	        case UNSIGNED_INT:
	        case TIME:
	            val = new UnsignedChannelValue();
	            break;
	        case FLOAT:
	            val = new FloatChannelValue();
	            break;
	        case ASCII:
	            val = new ASCIIChannelValue();
	            break;
	        case BOOLEAN:
	            val = new BooleanChannelValue();
	            break;
	        default:
	        	log.warn(
	                    "Unrecognized or unsupported channel type: " + def.getChannelType());
	        	continue;
	        }
	        val.setChannelDefinition(def);
	        if (!currentSclk.equals(builder.getSclk())) {
	        	currentSclk = builder.getSclk();
                currentScet = SclkScetUtility.getScet(builder.getSclk(), currentErt, scid, log);
	        }
        	val.setSclk(currentSclk);
        	val.setScet(currentScet);
	        val.setDn(builder.getDn());

	        if (builder.isEuSet() && def.getChannelType() != ChannelType.ASCII) {
	        	val.setEu(builder.getEu());
	        }

	        cvList.add(val);
		}
		

	}
	
	@Override
    public void onMapReference(final IDecomMapReference statement) {
		channelMappingStack.push(statement.getNameToChannelMap());
	}
	
	@Override
    public void onMapEnd(final IDecomMapDefinition statement) {
		if (!channelMappingStack.isEmpty()) {
			channelMappingStack.pop();
		}
	}
}
