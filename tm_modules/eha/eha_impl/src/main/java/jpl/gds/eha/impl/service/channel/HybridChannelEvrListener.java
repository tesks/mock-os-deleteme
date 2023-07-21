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

import jpl.gds.decom.algorithm.DecomArgs;
import jpl.gds.decom.algorithm.EvrBuilder;
import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.types.IEventRecordDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.evr.api.service.extractor.*;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.string.SprintfUtil;
import jpl.gds.shared.string.SprintfUtilException;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.types.BitBuffer;
import org.springframework.context.ApplicationContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This listener extends the channelization listener by creating IEvr objects that may result
 * from the custom decom algorithm in addition to channel values. This is in a separate class 
 * to keep code dealing with channel values and EVRs as separate as possible in downlink.
 * 
 * Class is not thread safe.
 *
 */
public class HybridChannelEvrListener extends ChannelizationListener {

	private List<IEvr> evrList = new ArrayList<IEvr>();
	private final Map<Long, IEvrDefinition> evrLookup;
	private final IEvrExtractorUtility extractorUtil;
    private final IEvrFactory evrFactory;
    private final IRawEvrDataFactory rawEvrDataFactory;
    private final IEvrExtractor evrExtractor;


    /**
     * Create the hybrid listener.
     * 
     * @param appContext
     *            the current application context
     */
    public HybridChannelEvrListener(final ApplicationContext appContext) {
        super(appContext);
        this.evrLookup = appContext.getBean(IEvrDefinitionProvider.class).getEvrDefinitionMap();
        this.extractorUtil = appContext.getBean(IEvrExtractorUtility.class);
        this.evrFactory = appContext.getBean(IEvrFactory.class);
        this.rawEvrDataFactory = appContext.getBean(IRawEvrDataFactory.class);
        this.evrExtractor = appContext.getBean(IEvrExtractor.class);
	}

	/**
	 * Constructor.  Will probably be mostly used for unit testing.
	 * @param channelLookup
	 * @param chanFactory
	 * @param extractorUtil
	 * @param evrMap
	 * @param evrFactory
	 */
	HybridChannelEvrListener(final Map<String, IChannelDefinition> channelLookup,
									final IChannelValueFactory chanFactory,
									IEvrExtractorUtility extractorUtil,
									Map<Long, IEvrDefinition> evrMap,
									IEvrFactory evrFactory,
									IRawEvrDataFactory rawEvrDataFactory,
									IEvrExtractor evrExtractor) {
    	super(channelLookup, chanFactory);
    	this.extractorUtil = extractorUtil;
    	this.evrLookup = evrMap;
    	this.evrFactory = evrFactory;
    	this.rawEvrDataFactory = rawEvrDataFactory;
    	this.evrExtractor = evrExtractor;
	}


	@Override
	public void decom(final IDecommutator algorithm, final BitBuffer data, final Map<String, Object> args)
			throws DecomException {
		args.put(DecomArgs.SCLK, currentSclk);
		super.decom(algorithm, data, args);
		final List<EvrBuilder> evrBuilders = algorithm.collectEvrs();
		
		for (final EvrBuilder builder : evrBuilders) {
			final long eventId = builder.getEventId();
			final IEvrDefinition def = this.evrLookup.get(eventId);
			if (def == null) {
				throw new DecomException("Decommutator algorithm returned EVR with bad eventID" + eventId);
			}
            final IEvr evr = this.evrFactory.createEvr();
			evr.setEvrDefinition(def);
			final ISclk evrSclk = builder.getSclk();
			if (!evrSclk.equals(this.currentSclk)) {
				this.currentSclk = evrSclk;
                this.currentScet = SclkScetUtility.getScet(builder.getSclk(), currentErt, scid, log);
			}

			evr.setSclk(this.currentSclk);
			evr.setScet(this.currentScet);
			final List<IRawEvrData> argData = new ArrayList<>(builder.getArguments().size());
			try {
				for (final Object arg : builder.getArguments()) {
					if (arg instanceof Integer) {
						final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
						buffer.putInt(((Integer) arg).intValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Integer.class));
					} else if (arg instanceof Short) {
						final ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
						buffer.putShort(((Short) arg).shortValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Short.class));
					} else if (arg instanceof Byte) {
						final ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES);
						buffer.put(((Byte) arg).byteValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Byte.class));
					} else if (arg instanceof Long) {
						final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
						buffer.putLong(((Long) arg).longValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Short.class));
					} else if (arg instanceof Float) {
						final ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
						buffer.putFloat(((Float) arg).floatValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Float.class));
					} else if (arg instanceof Double) {
						final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
						buffer.putDouble(((Double) arg).doubleValue());
						argData.add(this.rawEvrDataFactory.create(buffer.array(), Double.class));
					} else {
						argData.add(this.rawEvrDataFactory.create(arg.toString().getBytes(), String.class));
					}
				}
				formatEvrMessage(def, evr, argData);
				
			} catch (final EvrExtractorException e) {
				throw new DecomException("Decommutation algorithm returned bad EVR", e);
			}
			this.evrList.add(evr);
		}
	}
	
	/* 07/28/16: Added this method. It is close to being copy-pasted from the EVR class's replaceParameters
	 * method and the AbstractEvrAdapter's extraction logic, which includes formatting of the EVR message. I believe the logic should be refactored,
	 * out of those classes into a common EVR formatter class or interface, but not on this JIRA.
	 */
	private void formatEvrMessage(final IEvrDefinition def, final IEvr evr, final List<IRawEvrData> args) throws DecomException {
		List<String> parameterFormats;
		String messageWithReplacements;
		try {
			messageWithReplacements = this.extractorUtil.replaceParameters(args, def.getFormatString(), def);
		} catch (final EvrExtractorException e) {
			throw new DecomException("Could not format EVR returned by decom algorithm", e);
		}
		try {
			parameterFormats = SprintfUtil.getFormatLetters( messageWithReplacements );
		} catch (final SprintfUtilException e) {			  
			throw new DecomException( "Could not format EVR message using format string ( " + messageWithReplacements+ "): " + e.getMessage());
		}
		 final List<Object> formattedParameters = new ArrayList<Object>(
	                args.size());

	        final Iterator<IRawEvrData> parameterIterator = args.iterator();
	        final Iterator<String> formatIterator = parameterFormats.iterator();

	        while (parameterIterator.hasNext()) {

	            final String currentFormat = formatIterator.next();
	            final IRawEvrData currentData = parameterIterator.next();

	            if (!formattedParameters.add(currentData.formatData(currentFormat))) {

	                throw new DecomException(
	                        "Could not process formatted EVR data element: "
	                                + currentData.getDumpDataInformation());
	            }

	    
	        }
        final SprintfFormat dataFormatter = new SprintfFormat();
	        String finalMessage = "";

	        try {

	            if (formattedParameters.size() == 1) {
                finalMessage = new SprintfFormat().anCsprintf(
	                        messageWithReplacements, formattedParameters.get(0));
	            } else {
	                finalMessage = dataFormatter.sprintf(messageWithReplacements,
                        formattedParameters.toArray());
	            }

	        } catch (final ClassCastException cce) {
	            throw new DecomException("EVR extraction failed for event ID "
	                    + evr.getEventId() + "--"
	                    + "mismatch between format statement and argument type",
	                    cce);
	        }

	        evr.setMessage(finalMessage);	
	}

	/**
	 * Get the list of EVRs that have been instantiated during decom. Creates a new empty
	 * internal list instance.
	 * @return EVR list
	 */
	public List<IEvr> collectEvrs() {
		final List<IEvr> returnList = this.evrList;
		this.evrList = new ArrayList<>();
		return returnList;
	}

	@Override
	public void onEvr(IEventRecordDefinition def, BitBuffer buffer) {
		IEvr evr = this.evrFactory.createEvr();
		try {
			int curr = buffer.position();
			evr = this.evrExtractor.extractEvr(buffer.get(buffer.remaining() / Byte.SIZE),
					0,0,
					this.apid, this.vcid,
					this.dssId, this.seqCount);
			evr.setSclk(this.currentSclk);
			evr.setScet(this.currentScet);
			buffer.position(curr + this.evrExtractor.getCurrentOffset());
		} catch (EvrExtractorException e) {
			// Do nothing.  Bad EVR returned from IEvrExtractor
		}
		this.evrList.add(evr);
	}
}
