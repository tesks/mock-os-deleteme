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
package jpl.gds.eha.impl.channel.aggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.springframework.context.ApplicationContext;

import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.aggregation.IAggregationStatistics;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupDiscriminator;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata;
import jpl.gds.eha.api.service.channel.IGroupedChannelAggregationService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * Class GroupedChannelAggregationService
 *
 */
public class GroupedChannelAggregationService implements IGroupedChannelAggregationService {
    private final long  groupTimeToLive;
    private final int maxGroupSize;
    private final Tracer trace;

	
   /**
     * The message bus on which to publish GroupedChannelValueMessages
     */
    private final IMessagePublicationBus messageBus;

    /**
     * This is the timer that limits the amount of time that any EHA group may
     * exist without being serialized and transmitted.
     *
     * 5/31/19 - Name the timer thread
     */
    private final Timer timer = new Timer("Channel Aggregation Service ");
    
    /**
     * A map that associates the important variants of an IAlarmedChannelValueMessage with 
     * a IEhaChannelGroupMetadata object.
     */
    private final Map<IEhaChannelGroupDiscriminator, EhaChannelGroupMetadata>  metadataMap = new HashMap<>();
    
    /**
     * This is a diagnostic list of sent messages.
     * 
     * TODO: 11/21/2016: Should be removed for production
     */
    private final List<IEhaChannelGroupMetadata> sent = new ArrayList<>();
	private final IEhaMessageFactory ehaMessageFactory;


    /**
     * Constructor for service that will accept IAlarmedChannelValueMessages, and group them
     * into aggregated, serialized messages suitable for database archive storage.
     * 
     * @param serviceContext the Service Context for this Service
     * 
     * @throws InvalidMetadataException if the Service Context is invalid.
     */
    public GroupedChannelAggregationService(final ApplicationContext serviceContext) {
        this.trace = TraceManager.getTracer(serviceContext, Loggers.TLM_EHA);
        this.messageBus = serviceContext.getBean(IMessagePublicationBus.class);
        this.ehaMessageFactory = serviceContext.getBean(IEhaMessageFactory.class);
        final EhaProperties ehaProps = serviceContext.getBean(EhaProperties.class);
        groupTimeToLive = ehaProps.getChannelGroupTimeToLive();
        maxGroupSize = ehaProps.getMaxChannelGroupSize();

    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {
        if (null != messageBus) {
            messageBus.subscribe(EhaMessageType.AlarmedEhaChannel, this);
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        waitForCompletion();
        if (null != messageBus) {
            messageBus.unsubscribe(EhaMessageType.AlarmedEhaChannel, this);
            // 5/31/19. Stop Timer, otherwise it blocks service shutdown
            timer.cancel();
        }
    }
    
    /**
     * Adds IAlarmedChannelValueMessage to existing or new IEhaChannelGroupMetadata.
     * 
     * @param m the IAlarmedChannelValueMessage to process
     * @return the IEhaChannelGroupMetadata object to which this message's channel sample was added.
     */
    public IEhaChannelGroupMetadata updateOrCreateMetadataGroup(final IAlarmedChannelValueMessage m) {
        final IEhaChannelGroupDiscriminator key = new EhaChannelGroupDiscriminator(m);
        EhaChannelGroupMetadata md = metadataMap.get(key);
        if (null == md) {
            synchronized (metadataMap) {
                md = metadataMap.get(key);
                if (null == md) {
                    md = new EhaChannelGroupMetadata(this, key, m);
                    timer.schedule(md, groupTimeToLive);
                    metadataMap.put(key, md);
                }
            }
        }
        else {
            md.updateMetadata(new AccurateDateTime(), m.getChannelValue());
        }
        return md;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.
     * message.IMessage)
     */
    @Override
    public void handleMessage(final IMessage m) {
        try {
        	final IEhaChannelGroupMetadata md = updateOrCreateMetadataGroup((IAlarmedChannelValueMessage) m);
            if (md.size() >= maxGroupSize) {
                publish(md);
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param md
     */
    public void publish(final IEhaChannelGroupMetadata md) {
        synchronized(metadataMap) {
            md.cancel();
            metadataMap.remove(md.getDiscriminatorKey());
        }
        try {
            final IEhaGroupedChannelValueMessage message = ehaMessageFactory.createGroupedChannelMessage(md);
        	if (null != messageBus) {
        		this.messageBus.publish(message);
        	}
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    
    public void waitForCompletion() {
    	synchronized (timer) {
    		while (metadataMap.size() > 0) {
    			trace.info(this.getClass().getName() + ".diagnosticDump(): Waiting for send queue to empty. Currently contains: " + metadataMap.size() + " entries...");
    			try {
    				timer.wait(1000);
    			}
    			catch (final InterruptedException e) {
    				// do nothing
    			}
    		}
    	}
    }
    
    /**
     * For Debug Only
     * 
     * @return a Map of Discriminators to Group Metadata Objects
     */
    public List<IEhaChannelGroupMetadata> getSentMetadataGroupList() {
    	return Collections.unmodifiableList(sent);
    }
    
    public IAggregationStatistics getAggregationStatistics() {
    	return new AggregationStatistics();
	}

	/* package */ class AggregationStatistics implements IAggregationStatistics {
		private final int   sentGroups;
		private final int	channelSamples;
		private final int	maxSclksPerGroup;
		private final long	maxGroupSize;
		private final long	totalDataSize;
		private final long	totalUniqueSclks;

		private AggregationStatistics() {
			int	 _channelSamples = 0;
			int	 _maxSclksPerGroup = 0;
			long _maxGroupSize = 0;
			long _totalDataSize = 0;
			long _totalUniqueSclks = 0;

        	waitForCompletion();
            
            for (final IEhaChannelGroupMetadata gmd: sent) {
                _channelSamples += gmd.size();
                _maxSclksPerGroup = Math.max(_maxSclksPerGroup,  gmd.getUniqueSclkCount());
                _totalUniqueSclks += gmd.getUniqueSclkCount();
                final long groupSize =  gmd.build().toByteArray().length;
                _totalDataSize += groupSize;
                _maxGroupSize = Math.max(_maxGroupSize, groupSize);
            }
            sentGroups = sent.size();
            channelSamples = _channelSamples;
            maxSclksPerGroup = _maxSclksPerGroup;
            maxGroupSize = _maxGroupSize;
            totalDataSize = _totalDataSize;
            totalUniqueSclks = _totalUniqueSclks;
    	}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getChannelSamples()
		 */
		@Override
		public int getSentGroups() {
			return sentGroups;
		}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getChannelSamples()
		 */
		@Override
		public int getChannelSamples() {
			return channelSamples;
		}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getMaxSclksPerGroup()
		 */
		@Override
		public int getMaxSclksPerGroup() {
			return maxSclksPerGroup;
		}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getMaxGroupSize()
		 */
		@Override
		public long getMaxGroupSize() {
			return maxGroupSize;
		}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getTotalDataSize()
		 */
		@Override
		public long getTotalDataSize() {
			return totalDataSize;
		}

		/* (non-Javadoc)
		 * @see jpl.gds.eha.impl.channel.aggregation.IAggregationStatistics#getTotalUniqueSclks()
		 */
		@Override
		public long getTotalUniqueSclks() {
			return totalUniqueSclks;
		}
    }
}
