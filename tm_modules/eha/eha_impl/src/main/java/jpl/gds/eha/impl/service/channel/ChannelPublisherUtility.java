/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control dlaws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.eha.impl.service.channel;

import static jpl.gds.shared.exceptions.ExceptionTools.printStack;
import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.eu.AlgorithmicDNtoEU;
import jpl.gds.common.eu.IEUCalculationFactory;
import jpl.gds.common.eu.ParameterizedAlgorithmicDNtoEU;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IParameterizedAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.eha.channel.api.IAlgorithmUtility;
import jpl.gds.eha.channel.api.ParameterizedEuBase;
import jpl.gds.eha.impl.ChannelTimeComparator;
import jpl.gds.eha.impl.service.channel.derivation.ACVMap;
import jpl.gds.eha.impl.service.channel.derivation.AlgorithmicDerivation;
import jpl.gds.eha.impl.service.channel.derivation.BitUnpackDerivation;
import jpl.gds.eha.impl.service.channel.derivation.DerivationMap;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.VcidHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * The ChannelPublisherUtility class contains common methods for use in deriving
 * channel values, and publishing EHA messages.
 * 
 * 9/3/15. Suppressed deprecation warnings for
 *          ACVMap, which has been temporarily deprecated to discourage its use
 *          by customers.
 */
@SuppressWarnings("deprecation")
public class ChannelPublisherUtility implements IChannelPublisherUtility {
    private final Tracer              deriveLog;   

	private static final Random  RANDOM        = new Random();
	private static final int     MAX_CYCLE     = 10;
	private static final int     MAX_ERROR     = 5;
    private final long                       maxWait;
    private final boolean                    useTriggers;
    private final boolean                    doDerivation;
    private final boolean                    USE_THREADS = false;

	private boolean setSolTimes = false;
    private final IMessagePublicationBus messageBus;
    private final IChannelLad lad;
    private final ChannelTimeComparator timeCompare;
    private DerivationMap derivationMap;
    private final IChannelDefinitionProvider chanTable;
    private final ApplicationContext appContext;
	private final IEhaMessageFactory ehaMessageFactory;
    private final IChannelValueFactory chanFactory;
    private final IEUCalculationFactory euFactory;
    private final SseContextFlag             sseFlag;

	// Keep track of algorithms we have already reported as exceeding the
	// maximum error count.
	private static final Set<String> _exceeded = new HashSet<String>();

    /**
     * Constructor.
     * 
     * @param serviceContext
     *            the current application context
     */
    public ChannelPublisherUtility(final ApplicationContext serviceContext) {
	    this.messageBus = serviceContext.getBean(IMessagePublicationBus.class);
	    this.setSolTimes = serviceContext.getBean(EnableLstContextFlag.class).isLstEnabled();
	    this.chanTable = serviceContext.getBean(IChannelDefinitionProvider.class);
	    this.lad = serviceContext.getBean(IChannelLad.class);
	    this.timeCompare = new ChannelTimeComparator(serviceContext.getBean(TimeComparisonStrategyContextFlag.class));
	    this.appContext = serviceContext;
        this.deriveLog = TraceManager.getTracer(serviceContext, Loggers.TLM_DERIVATION);
		this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
	    this.chanFactory = appContext.getBean(IChannelValueFactory.class);
        final EhaProperties ehaProps = appContext.getBean(EhaProperties.class);
        maxWait = ehaProps.getDerivationTimeout();
        useTriggers = ehaProps.isUseTriggerChannels();
        doDerivation = ehaProps.isDerivationEnabled();
        euFactory = appContext.getBean(IEUCalculationFactory.class);
        sseFlag = appContext.getBean(SseContextFlag.class);
	}
	
	@Override
    public void initDerivations() {
		this.derivationMap = this.appContext.getBean(DerivationMap.class);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IChannelPublisherUtility#genStreamId(java.lang.String)
     */
    @Override
    public String genStreamId(final String trailer) {
    	long r = 0L;
    	final StringBuffer sb = new StringBuffer("Stream");
    
    	synchronized (ChannelPublisherUtility.class) {
    		r = RANDOM.nextLong();
    	}
    
    	sb.append(Math.abs(r)).append(trailer);
    
    	return sb.toString();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IChannelPublisherUtility#doChannelDerivations(java.util.List, boolean, java.util.Date, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.Sclk, jpl.gds.shared.time.ILocalSolarTime, int, java.lang.Integer)
     */
	@Override
    public void doChannelDerivations(
			final List<IServiceChannelValue> ehaList,
			final boolean                    isRealtime,
			final IAccurateDateTime           rct,
			final IAccurateDateTime           ert,
			final IAccurateDateTime           scet,
			final ISclk                      sclk,
			final ILocalSolarTime             sol,
			final int						 dss,
			final Integer					 vcid)
	{
        doChannelDerivations(ehaList, isRealtime, rct, ert, scet, sclk, sol, dss, vcid, useTriggers);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IChannelPublisherUtility#doChannelDerivations(java.util.List, boolean, java.util.Date, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.Sclk, jpl.gds.shared.time.ILocalSolarTime, int, java.lang.Integer, boolean)
     */
	@Override
    public void doChannelDerivations(
			final List<IServiceChannelValue> ehaList,
			final boolean                    isRealtime,
			final IAccurateDateTime          rct,
			final IAccurateDateTime           ert,
			final IAccurateDateTime           scet,
			final ISclk                      sclk,
			final ILocalSolarTime             sol,
			final int						 dss,
			final Integer					 vcid,
			final boolean                    useTriggers)
	{
        if (!doDerivation) {
			return;
		}
		
		if(ehaList.isEmpty())
		{
			return;
		}

		if (this.derivationMap == null) {
		    initDerivations();
		}
		
		int                        cycle       = 0;

		List<IServiceChannelValue> workingList = new ArrayList<IServiceChannelValue>(ehaList.size());
		workingList.addAll(ehaList);

		// Initially, all are from the same packet, so get first id

		final PacketIdHolder packetId =
				workingList.get(0).getPacketId();

		while (! workingList.isEmpty())
		{
			++cycle;

			if (cycle > MAX_CYCLE)
			{
				deriveLog.error("Channel derivation cycle count exceeded, " +
						"possible infinite loop");
				break;
			}

			final List<IServiceChannelValue> newChannelValues =
					new ArrayList<IServiceChannelValue>(16);

			// Do the bit unpack derivations, accumulating the new channels

			doBitUnpack(workingList, derivationMap, newChannelValues);

			// Do the algorithmic derivations, accumulating the new channels

			if (!useTriggers) 
			{	
				doAlgorithmicNoTrigger(workingList, derivationMap, newChannelValues);
			}
			else
			{
				doAlgorithmicWithTrigger(workingList, derivationMap, newChannelValues);
			}

			for(final IServiceChannelValue chanval : newChannelValues)
			{
				chanval.setPacketId(packetId);
				/*
				 * EU computation is now done
				 * by the sendChannelMessages() method.
				 */
			}

			if (! newChannelValues.isEmpty())
			{
				final String streamID = genStreamId("Derived");

				sendChannelMessages(newChannelValues,
						rct,
						ert,
						scet,
						sclk,
						sol,
						streamID,
						isRealtime,
						dss,
						vcid);

			}

			workingList = newChannelValues;
		}
	}

	/*
	 * Wrapper method created below to address the said JIRA issue.
	 */

	@Override
    public void publishFlightAndDerivedChannels(final boolean disableDerivations,
			final List<IServiceChannelValue> ehaList, final IAccurateDateTime rct,
			final IAccurateDateTime ert, final IAccurateDateTime scet,
			final ISclk sclk, final ILocalSolarTime sol, final String streamID,
			final boolean isRealtime, final int dss, final Integer vcid,
			final Boolean useTriggers) {

		final IMessage start = ehaMessageFactory.createStartChannelProcMessage(streamID);
		messageBus.publish(start);

		sendChannelMessages(ehaList, rct, ert, scet, sclk,
				sol, streamID, isRealtime, dss, vcid);

		if (!disableDerivations) {

			if (useTriggers == null) {
				doChannelDerivations(ehaList, isRealtime,
						rct, ert, scet, sclk, sol, dss, vcid);
			} else {
				doChannelDerivations(ehaList, isRealtime,
						rct, ert, scet, sclk, sol, dss, vcid,
						useTriggers.booleanValue());
			}

		}

		final IMessage end = ehaMessageFactory.createEndChannelProcMessage(streamID);
		messageBus.publish(end);

	}

	    /**
     * Derive the bit-unpack channels. These are derived from a single parent
     * channel and create a single child channel. The children are added to the
     * new channel list.
     * 
     * @param workingList
     *            list of parent channel values
     * @param map
     *            the current derivation map table
     * @param newChannelValues
     *            derived channel value list to which child channels should be
     *            added
     */
	private void doBitUnpack(final List<IServiceChannelValue> workingList,
			final DerivationMap map,
			final List<IServiceChannelValue> newChannelValues) {

		for (final IServiceChannelValue val : workingList) {
			Set<BitUnpackDerivation> channelDerivs = null;
			try {
				channelDerivs = map.getBitDerivationsForParent(val.getChanId());
			} catch (final RuntimeException e) {
				e.printStackTrace();
			}

			if ((channelDerivs == null) || channelDerivs.isEmpty()) {
				continue;
			}

			for (final BitUnpackDerivation def : channelDerivs) {
				try {
					final IServiceChannelValue child = def.deriveChannel(val, lad.getDefinitionProvider(), chanFactory);

					if (child != null)
					{
						child.setRealtime(val.isRealtime());
						child.setScet(val.getScet());
						child.setSclk(val.getSclk());
						child.setErt(val.getErt());

                        child.setDssId(val.getDssId());
						child.setVcid(val.getVcid());

						newChannelValues.add(child);

						if (deriveLog.isDebugEnabled()) {
						    deriveLog.debug("Generated " + child + " through bit unpack");
						}
					}
				} catch (final DerivationException e) {
			        deriveLog.error("Problem producing derived bit channels: " + e);

					e.printStackTrace();
				} catch (final SecurityException e) {
				    deriveLog.error("Security exception encountered producing derived bit channels: " + e);
				    
				    e.printStackTrace();
				}
			}
		}
	}


	/**
	 * Derive the algorithmic channels. These are derived from M parent channels
	 * and create N child channels. The children are added to the new channel
	 * list.
	 * 
	 * This is MSL style, with id and not trigger channel.
	 * 
	 * The working list is compressed to take only the latest values.
	 *
	 * We only need a single parent in order to do the derivation; missing data
	 * is taken from the LAD. We can't get ALL the data from the LAD, though.
	 * 
	 * @param workingList
	 * @param map
	 * @param newChannelValues
	 */	
	private void doAlgorithmicNoTrigger(
			final List<IServiceChannelValue> workingList,
			final DerivationMap              map,
			final List<IServiceChannelValue> newChannelValues)
	{
		final ACVMap                           parents    = new ACVMap();
		final ACVMap                           workingMap =
				new ACVMap(workingList);
		//final Collection<IInternalChannelValue> working    = workingMap.values();

		// We must first determine whether we are working in real-time.
		// Do that by looking at any input channel value

		if (workingList.isEmpty())
		{
			return;
		}

		boolean rt = false;

		// If this is realtime, then the entire workingList is realtime
		// If this is monitor channel, we are performing monitor derivations and
		// LAD values should be queried by the station that this representative
		// came from.
		final IServiceChannelValue chanValRep = workingList.get(0);

		rt = chanValRep.isRealtime();

		// Determine the station ID to use to query the LAD
		// If it is monitor channel, it should be the same station as the
		// representative
		// If it is not monitor channel, we do not care so set to 0
		int stationId = 0;

		if (ChannelDefinitionType.M.equals(chanValRep.getDefinitionType())) {
			stationId = chanValRep.getDssId();
		}

		// Get algorithms we have parents for in the current channel list
		// That should narrow the algorithm list to just a few that really apply
		final Set<AlgorithmicDerivation> algosForParents = map
				.getAlgorithmicDerivations(workingList);
		for (final AlgorithmicDerivation algo : algosForParents) {
			// Find the ACVs in the working list that correspond to the
			// required parents of the algorithm

			parents.clear();

			/* 
			 * Must now go through definition object
			 * to get derivation attributes (parents, children, id, etc.)
			 */
			final List<String> aparents = algo.getDefinition().getParents();
			int foundCount = 0;

			IServiceChannelValue firstParent = null;

			for (final String ci : aparents) {
				/*
				 *  Modifications throughout
				 * the parent selection below to use latest value for all 
				 * parent channels, whether in the packet or in the LAD.
				 */
				boolean found = false;

				/*
				 * First look for the parent in the list of channels
				 * found in the last packet or derivation round. If it 
				 * is there, set the found flag.
				 */
				final IServiceChannelValue acv = workingMap.get(ci);
				if (acv != null) {

					if (firstParent == null) {
						firstParent = acv;
					}

					found = true;

					++foundCount;
				}

				/*
				 * Also get the last value of the parent channel from the LAD.
				 */
				 final IServiceChannelValue from_lad = (IServiceChannelValue) lad.getMostRecentValue(ci,
						 rt, stationId);

				 /*
				  * If the value was found in the current packet...
				  */
				 if (found) {
					 /*
					  * If it was also found in the LAD, then we want the later
					  * of the LAD sample or the sample in the packet.
					  */
					 if (from_lad != null) {
						 if (timeCompare.timestampIsLater(from_lad, acv)) {
							 parents.add(acv);
						 } else {
							 parents.add(from_lad);
						 }
					 } else {
						 parents.add(acv);
					 }
				 } else if (from_lad != null) {
					 /*
					  * The parent was not in the packet. Use the LAD value.
					  */
					 parents.add(from_lad);
				 } else {
					 /*
					  * There is no parent value, period. Cannot derive.
					  */
					 continue;
				 }
			}

			if ((foundCount > 0) && (parents.size() == aparents.size()))
			{
				// We have all the required channels, so derive.
				// Note that at least one channel value must be from the packet.

				long start = 0;
				//if (deriveLog.isEnabledFor(TraceSeverity.TRACE)) {
				start = System.currentTimeMillis();
				//}
				final List<IServiceChannelValue> tempChannels = new ArrayList<IServiceChannelValue>(1);
				doDerivation(algo, parents, tempChannels);
				if (deriveLog.isEnabledFor(TraceSeverity.TRACE)) {
					deriveLog.trace("Algorithm " + algo + " took " + (System.currentTimeMillis() - start) + " milliseconds");
				}

				IAccurateDateTime scet = null;
				IAccurateDateTime ert = null;
                ISclk sclk = null;
				ILocalSolarTime sol = null;
				if (firstParent != null) {
					scet = firstParent.getScet();
					sclk = firstParent.getSclk();
					ert = firstParent.getErt();
					if (setSolTimes) {
						sol = firstParent.getLst();
					}

				}
				// Set the timestamps on these child channels to match that on the
				// incoming channels
				for (final IServiceChannelValue acv : tempChannels)
				{
					if (firstParent != null) {
						acv.setScet(scet);
						acv.setSclk(sclk);
						acv.setErt(ert);
						acv.setLst(sol);
					}

					acv.setRealtime(rt);
				}

				newChannelValues.addAll(tempChannels);
			}
		}
	}


	/**
	 * Derive the algorithmic channels. These are derived from M parent channels
	 * and create N child channels. The children are added to the new channel
	 * list.
	 * 
	 * This is non-MSL style, with trigger channel instead of plain id.
	 * 
	 * The working list is compressed to take only the latest values.
	 * 
	 * @param workingList list of channel values that may be parents to derivations
	 * @param map the derivation map to use to locate the derivation algorithms
	 * @param newChannelValues the new channel values produced by one or more derivations
	 */
	private void doAlgorithmicWithTrigger (
			final List<IServiceChannelValue> workingList,
			final DerivationMap map,
			final List<IServiceChannelValue> newChannelValues) {

		final ACVMap parents = new ACVMap();
		final ACVMap workingMap = new ACVMap(workingList);
		final Collection<IServiceChannelValue> working = workingMap.values();

		final Set<AlgorithmicDerivation> algosForParents = map.getAlgorithmicDerivations(workingList);

		if (algosForParents == null || algosForParents.isEmpty()) {
			// This block was empty. Based on the
			// condition, it should return if we do not find any derivation
			// algorithms from the given map
			return;
		}

		//see if any channel in the working set is a trigger channel for a derivation
		for (final IServiceChannelValue trigger : working)
		{
			final String  tci = trigger.getChanId();
			final boolean rt  = trigger.isRealtime();

			for (final AlgorithmicDerivation algo : algosForParents) {
				/* 
				 * Must now go through definition object
				 * to get derivation attributes (parents, children, id, etc.)
				 */

				/*
				 * Derivations now have a trigger ID,
				 * The trigger channel ID no longer has to match the derivation/
				 * algorithm ID as before.
				 */
				final String trigger_id = algo.getDefinition().getTriggerId();

				// trigger channels can be identified by comparing the
				// trigger ID with the channel ID. 
				if (!tci.equals(trigger_id)) {
					continue;
				}

				// At this point, we found an algorithm that should be triggered
				// by this incoming channel value

				// Got a trigger.

				// Find the ACVs in the working list that correspond to the
				// required parents of the algorithm. Note that the trigger
				// need not be a parent.

				int stationId = 0;

				// check to see if the trigger is a
				// monitor channel
				if (ChannelDefinitionType.M.equals(trigger.getDefinitionType())) {
					stationId = trigger.getDssId();
				}

				parents.clear();

				// get the parents of the algorithm
				final List<String> aparents = algo.getDefinition().getParents();

				// for each parent, obtain its value. First check the working
				// set (from the incoming packet). If it is not in the working
				// set, get it from the LAD
				for (final String ci : aparents) {

					/*
					 * Modifications throughout
					 * the parent selection below to use latest value for all 
					 * parent channels, whether in the packet or in the LAD.
					 */
					boolean found = false;
					/*
					 * First look for the parent in the list of channels
					 * found in the last packet or derivation round. If it 
					 * is there, set the found flag.
					 */
					final IServiceChannelValue acv = workingMap.get(ci);
					if (acv != null) {
						found = true;
					}

					/*
					 * Also get the last value of the parent channel from the LAD.
					 */
					final IServiceChannelValue from_lad = (IServiceChannelValue) lad.getMostRecentValue(ci,
							rt, stationId);

					/*
					 * If the value was found in the current packet...
					 */
					if (found) {
						/*
						 * If it was also found in the LAD, then we want the later
						 * of the LAD sample or the sample in the packet.
						 */
						if (from_lad != null) {
							if (timeCompare.timestampIsLater(from_lad, acv)) {
								parents.add(acv);
							} else {
								parents.add(from_lad);
							}
						} else {
							parents.add(acv);
						}
					} else if (from_lad != null) {
						/*
						 * The parent was not in the packet. Use the LAD value.
						 */
						parents.add(from_lad);
					} else {
						/*
						 * There is no parent value, period. Cannot derive.
						 */
						continue;
					}
				}

				if (parents.size() == aparents.size()) {
					// We have all the required channels, so derive

					long start = 0;
					//if (deriveLog.isEnabledFor(TraceSeverity.TRACE)) {
					start = System.currentTimeMillis();
					//}
					final List<IServiceChannelValue> tempChannels = new ArrayList<IServiceChannelValue>(1);
					doDerivation(algo, parents, tempChannels);
					if (deriveLog.isEnabledFor(TraceSeverity.TRACE)) {
						deriveLog.trace("Algorithm " + algo + " took " + (System.currentTimeMillis() - start) + " milliseconds");
					}

					for (final IServiceChannelValue acv : tempChannels)
					{
						acv.setScet(trigger.getScet());
						acv.setSclk(trigger.getSclk());
						acv.setErt(trigger.getErt());
						acv.setLst(trigger.getLst());
						acv.setRealtime(rt);
					}

					newChannelValues.addAll(tempChannels);
				}
			}
		}
	}


	/**
	 * Perform an algorithmic derivation.
	 *
	 * @param algo
	 * @param parents
	 * @param newChannelValues
	 */
	private void doDerivation(
			final AlgorithmicDerivation      algo,
			final ACVMap                     parents,
			final List<IServiceChannelValue> newChannelValues)
	{
		// Check parents to make sure
		// channels are all flight or all monitor, not mixed.
		final Collection<IServiceChannelValue> parentChannels = parents.values();
		Boolean isMonitorDerivation = null;

		for (final IServiceChannelValue chanVal : parentChannels) {

			final ChannelDefinitionType chanDefType = chanVal.getDefinitionType();

			if (chanDefType == null) {
				// we cannot have a null definition type
				deriveLog.error("Unknown Channel Definition Type for channel, "
						+ chanVal.getChanId() + ". Aborting derivation.");
				return;
			}

			// first iteration will determine whether we "expect" the parent
			// channels to be monitor or not.
			if (isMonitorDerivation == null) {
				isMonitorDerivation = ChannelDefinitionType.M
						.equals(chanDefType);

				// nothing to check for first iteration
				continue;
			}

			// if we are expecting monitor channels for parents, and a
			// parent is not a monitor channel
			final boolean errorCondition1 = isMonitorDerivation
					&& !ChannelDefinitionType.M.equals(chanDefType);

			// if we are not expecting monitor channels for parents, and a
			// parent is a monitor channel
			final boolean errorCondition2 = !isMonitorDerivation
					&& ChannelDefinitionType.M.equals(chanDefType);

			if (errorCondition1 || errorCondition2) {
				// just abandon if the error count reaches MAX_ERROR
				if (algo.getErrorCount() >= MAX_ERROR) {
					/* 
					 * Must now go through definition object
					 * to get derivation attributes (parents, children, id, etc.)
					 */
					if (_exceeded.add(algo.getDefinition().getId())) {
						deriveLog
						.error("Derived algorithm '"
								+ algo.getDefinition().getId()
								+ "' has exceeded permissible error count, not run: Parent/Child monitor channel type inconsistent.");
					}

					return;
				}

				// if we meet any error conditions issue warning and abandon
				// derivation
				deriveLog
				.warn("Detected a mixture of monitor and non monitor channels for parents to the derivation: "
						+ algo.getDefinition().getId() + ". Abandoning derivation");
				algo.incrementErrorCount();
				return;
			}
		}

		/* 
		 * Must now go through definition object
		 * to get derivation attributes (parents, children, id, etc.)
		 */
		final String algo_id = algo.getDefinition().getId();

		if (algo.getErrorCount() >= MAX_ERROR)
		{
			if (_exceeded.add(algo_id))
			{
				deriveLog.error("Derived algorithm '" +
						algo_id               +
						"' has exceeded permissible error count, not run");
			}

			return;
		}

		// Run as thread, timing out if it runs away

		// (Do NOT log at debug level; we want to know the name of the last
		// algorithm in case the process has to be killed.)
		//
		//The problem with the above statement is that it causes us to do costly
		//console I/O every time an algorithm runs. (brn)
		//log.info("Running algorithmic derivation: " + algo_id);
		if (deriveLog.isDebugEnabled()) {
		    deriveLog.debug("Running algorithmic derivation: " + algo_id);
		}

        final RunAlgorithm ra = new RunAlgorithm(algo, parents, maxWait);

		if (USE_THREADS)
		{
			ra.start();
		}
		else
		{
			ra.run();
		}

		final Pair<ACVMap, Integer> result = ra.getResult();

		if (result != null)
		{
			final ACVMap children = result.getOne();

			if ((children != null) && !children.isEmpty())
			{
				newChannelValues.addAll(children.values());

				// The loop still executes, even if DEBUG is not enabled.
				// This wastes cycles. Added the debug check.
				if (deriveLog.isDebugEnabled()) {
					for (final String ci : children.keySet())
					{
						deriveLog.debug("Generated " + ci + " through " + algo_id);
					}
					deriveLog.debug(algo_id + " returned " + result.getTwo());
				}
			}
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IChannelPublisherUtility#sendChannelMessages(java.util.List, java.util.Date, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.IAccurateDateTime, jpl.gds.shared.time.Sclk, jpl.gds.shared.time.ILocalSolarTime, java.lang.String, boolean, int, java.lang.Integer)
     */
	@Override
    public void sendChannelMessages(final List<IServiceChannelValue> ehaList,
                                    final IAccurateDateTime rct, final IAccurateDateTime ert,
                                    final IAccurateDateTime scet,
			final ISclk sclk, final ILocalSolarTime sol, final String streamID, final boolean isRealtime, final int dss, final Integer vcid) {

		/*
		 * Moved out StartChannelProcMessage send so that each derivation cycle
		 * does not trigger these messages.
		 */

		for (final IServiceChannelValue chanVal : ehaList) {
			if (chanVal == null) {
				continue;
			}

			final IChannelValueMessage m = ehaMessageFactory.createInternalChannelMessage(chanVal);

			m.setStreamId(streamID);
            m.setFromSse(sseFlag.isApplicationSse());

			if (chanVal.getRct() == null) {
			    chanVal.setRct(rct);
			}
			if (chanVal.getScet() == null) {
				chanVal.setScet(scet);
			}
			if (chanVal.getErt() == null) {
				chanVal.setErt(ert);
			}
			if (chanVal.getSclk() == null) {
				chanVal.setSclk(sclk);
			} 
			if (chanVal.getLst() == null) {
				chanVal.setLst(sol);
			}
			if (chanVal.getDssId() <= 0) {
				chanVal.setDssId(dss);
			}
			if (chanVal.getVcid() == null) {
				chanVal.setVcid(vcid);
			}

			chanVal.setRealtime(isRealtime);

			/*
			 * Now compute EU here. This has the result that
			 * EU is calculated for each channel immediately before it goes into
			 * the LAD, and ensures that the EU calculation can use any channel
			 * from the LAD that arrive PREVIOUSLY in the packet.  The latter
			 * behavior is needed by some CCL algorithms.
			 */
			computeAndSetEu(chanVal);

			messageBus.publish(m);
		}

		/*
		 * 09/26/2013 Moved out EndChannelProcMessage
		 * send so that the publishing of the flight channels and each
		 * derivation cycle by themselves do not trigger these messages.
		 */

	}


	/**
     * Assign packet id to all channel values.
     *
     * @param ehaList Collection of extracted channel values
     * @param pm      PacketMessage
     */
    @Override
    public void assignPacketId(final Collection<IServiceChannelValue> ehaList,
                                      final ITelemetryPacketMessage             pm)
    {

        final PacketIdHolder pi = pm.getPacketId();
    
        for (final IServiceChannelValue icv : ehaList)
        {
            icv.setPacketId(pi);
        }
    }


    /**
	 * Class to run and time out user-supplied derived algorithms. We time
	 * them out and kill them, because we don't know what else we can do
	 * to stop them.
	 *
	 * The thread is run at diminished priority to make it easier to stop.
	 */
	private class RunAlgorithm extends Thread
	{
		private final AlgorithmicDerivation _algo;
		private final ACVMap                _parents;
		private final long                  _wait;
		private final String                _id;

		private Pair<ACVMap, Integer> _result = null;


		/**
		 * Runs an algorithm derivation.
		 * @param algo algorithm derivation to run
		 * @param parents set of channel values
		 * @param wait wait time for the thread
		 */
		public RunAlgorithm(final AlgorithmicDerivation algo,
				final ACVMap                parents,
				final long                  wait)
		{
			super();

			_algo    = algo;
			_parents = parents;
			_wait    = Math.max(wait, 1L);
			/* 
			 * Must now go through definition object
			 * to get derivation attributes (parents, children, id, etc.)
			 */
			_id      = _algo.getDefinition().getId();

			setDaemon(true);
			setName("Algorithm_" + _id);
			setPriority(Thread.MIN_PRIORITY);
		}


		/**
		 * Get the result, or null if error. Wait until the thread terminates
		 * or times out.
		 *
		 * @return Result of running algorithm or null
		 */
		public Pair<ACVMap, Integer> getResult()
		{
			try
			{
				SleepUtilities.fullJoin(this, _wait);
			}
			catch (final ExcessiveInterruptException eie)
			{
				deriveLog.error("EhaPublisherUtility.getResult Could not join: " +
						rollUpMessages(eie));

				// Do not return here; the thread may not be alive
			}

			if (isAlive())
			{
				// Timed out, no choice but to kill it

				// Get state BEFORE we stop it
				final Thread.State        state = getState();
				final StackTraceElement[] stack = getStackTrace();

				this.stop();

				_algo.incrementErrorCount();

				deriveLog.error("Forced to kill derived algorithm id '" +
						_id                                +
						"' in state "                           +
						state);

				// Don't put tracebacks in logs

				System.out.println("Traceback for '" +
						_id               +
						"':\n"            +
						printStack(stack));
			}

			return _result;
		}


		/**
		 * Run the algorithm. The deriveChannels method takes care of all
		 * throwables and rethrows as the DerivationException. We also check
		 * for throwable just in case, so we can properly account for the
		 * thread's death.
		 */
		@Override
		public void run()
		{
			try
			{
				_result = _algo.deriveChannels(appContext, _parents);
			}
			catch (final ThreadDeath td)
			{
				throw td;
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
				_algo.incrementErrorCount();
				deriveLog.error("Problem producing derived algorithmic channels for id '" + _id + "': " + rollUpMessages(t));
			}
		}
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IChannelPublisherUtility#computeAndSetEu(jpl.gds.eha.api.channel.IServiceChannelValue)
     */
    @Override
    public void computeAndSetEu(final IServiceChannelValue chanval) {
        final IChannelDefinition chanDef = chanval.getChannelDefinition();
        if (chanval.hasEu() && chanval.getChannelType().isNumberType())
        {   
            try {
                double euVal = 0.0;
                if (chanval.getChannelType().isNumberType() && chanval.hasEu()) {
                    final IEUCalculation dnToEu = euFactory.createEuCalculator(chanDef.getDnToEu());

                    /* Set builtin parameters for parameterized EU */
                    if (dnToEu instanceof ParameterizedAlgorithmicDNtoEU) {
                        populateBuiltinEuParameters((ParameterizedAlgorithmicDNtoEU)dnToEu, chanval);
                    }

                    /* The dnToEu object is not the actual user algorithm instance.
                     * If it is an AlgorithmicDNToEU, we want to instantiate the actual user class inside of it.
                     */
                    if (dnToEu instanceof AlgorithmicDNtoEU) {
                        
                        final Object temp = ((AlgorithmicDNtoEU)dnToEu).getAlgorithmInstance();

                        /*  Now if the actual user algorithm instance supports the
                         * algorithm utility interface, use that interface to set things like the channel definition
                         * map and LAD, so that the algorithm instance has access to these things. 
                         */
                        if (temp instanceof IAlgorithmUtility) {

                            final IAlgorithmUtility algorithm = (IAlgorithmUtility)temp;
                            try {
                                algorithm.setChannelDefinitionMap(lad.getDefinitionProvider().getChannelDefinitionMap());
                                algorithm.setLogger(TraceManager.getTracer(appContext, Loggers.TLM_DERIVATION));
                                algorithm.setDictionaryProperties(appContext.getBean(DictionaryProperties.class));
                                algorithm.setLadProvider(lad);
                                try {
                                    final ISequenceDefinitionProvider dict = appContext.getBean(ISequenceDefinitionProvider.class);
                                    algorithm.setSequenceDictionary(dict);
                                } catch (final Exception e) {
                                    deriveLog.warn("No sequence dictionary found; sequence operations not available in EU conversions");
                                }
                                algorithm.setOpcodeToStemMap(appContext.getBean(ICommandDefinitionProvider.class).getStemByOpcodeMap());
                            } catch (final DerivationException e) {
                                /*  We need a visible stack trace here and the warning log does not seem to include it. */
                                e.printStackTrace();
                                deriveLog.warn("Error initializing DN-EU conversion for channel: " + e.toString(), e);
                            }
                        }
                    }
                    euVal = dnToEu.eu(chanval.doubleValue());
                    chanval.setEu(euVal);
                }
            } catch (final EUGenerationException e) {
                e.setChannelId(chanDef.getId());
                /*  We need a visible stack trace here and the warning log does not seem to include it. */
                e.printStackTrace();
                deriveLog.warn("Error performing DN-EU conversion for channel: " + e.toString(), e);

            }
        }
    }

    /**
     * Populates built-in EU parameters in the parameter map of the supplied DN
     * to EU object.
     * 
     * @param dnToEu
     *            ParameterizedAlgorithmicDNtoEU to add parameters to
     * @param parent
     *            the channel value for which EU is to be calculated
     */
    private void populateBuiltinEuParameters(
            final ParameterizedAlgorithmicDNtoEU dnToEu,
            final IServiceChannelValue parent) {
        final IParameterizedAlgorithmicEUDefinition def = (IParameterizedAlgorithmicEUDefinition) dnToEu.getDefinition();
        def.addParameter(ParameterizedEuBase.STATION_PARAM,
                String.valueOf(parent.getDssId()));
        def.addParameter(ParameterizedEuBase.REALTIME_PARAM,
                String.valueOf(parent.isRealtime()));
        def.addParameter(ParameterizedEuBase.VCID_PARAM,
                parent.getVcid() == null ? String.valueOf(VcidHolder.UNSPECIFIED_VALUE) : 
                    String.valueOf(parent.getVcid()));
    }
}
