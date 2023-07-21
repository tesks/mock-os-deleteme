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
package jpl.gds.time.impl.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.Pair;
import jpl.gds.time.api.config.TimeCorrelationProperties;
import jpl.gds.time.api.message.ISseTimeCorrelationMessage;
import jpl.gds.time.api.message.ITimeCorrelationMessageFactory;
import jpl.gds.time.api.service.ITimeCorrelationService;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is the SSE time correlation adapter service, an instance of a
 * DownlinkService that watches incoming packet messages for those matching a
 * configured time correlation packet APID. When it sees them, it parses the
 * information out of the TC packet and sends out an SseTimeCorrelationMessage.
 * 
 * This adapter is known to work only with the JPL SSE. There is
 * no guarantee it will work for any other mission or GSE/SSE setup.
 * 
 *
 */
public class SseTimeCorrelationService implements ITimeCorrelationService, MessageSubscriber
{
	private final Tracer tracer;
	
	private int timeApid; 
	/* MPCS-7289 - 4/30/15. Removed packet header length */
	
	private final ISclkExtractor sclkExtractor;	
	private final IMessagePublicationBus bus;
    private final ITimeCorrelationMessageFactory tcMessageFactory;
    private final TimeCorrelationProperties tcProperties;
    private final IContextConfiguration contextConfig;
	
	/**
	 * Constructor.
	 * @param serveContext the current application context
	 */
	public SseTimeCorrelationService(final ApplicationContext serveContext)
	{
		super();
        this.tracer = TraceManager.getDefaultTracer(serveContext);
		this.sclkExtractor = TimeProperties.getInstance().getCanonicalExtractor();
		this.bus = serveContext.getBean(IMessagePublicationBus.class);
		this.tcMessageFactory = serveContext.getBean(ITimeCorrelationMessageFactory.class);
		this.tcProperties = serveContext.getBean(TimeCorrelationProperties.class);
		this.contextConfig = serveContext.getBean(IContextConfiguration.class);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService()
	{
		this.timeApid = tcProperties.getTcPacketApid(true);
		/* MPCS-7289 - 4/30/15. Removed setting of packet header length */
		bus.subscribe(TmServiceMessageType.TelemetryPacket, this);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() 
	{
		bus.unsubscribeAll(this);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleMessage(final IMessage message)
	{
		final ITelemetryPacketMessage pm = (ITelemetryPacketMessage)message;
		if (!contextConfig.accept(pm) || pm.getPacketInfo().isFill() || pm.getPacketInfo().getApid() != this.timeApid)
		{
			return;
		}
		
		tracer.debug("Received SSE time correlation packet with sequence number " + pm.getPacketInfo().getSeqCount());
		
		final List<Pair<ISclk,IAccurateDateTime>> pairs = new ArrayList<>(32);
		try
		{
		    final byte[] packetData = pm.getPacket();
		    /* MPCS-7289 - 4/30/15. Use packet info to get header length */
		    int offset = pm.getPacketInfo().getPrimaryHeaderLength() + pm.getPacketInfo().getSecondaryHeaderLength();
	        
		    while(sclkExtractor.hasEnoughBytes(packetData, offset))
		    {
	            final ISclk sclk = sclkExtractor.getValueFromBytes(packetData,offset);
	            offset += sclk.getByteLength();
	            
		        final Calendar cal = Calendar.getInstance();
	            
	            final long irigvalh = GDR.get_u32(packetData,offset);
	            offset += 4;
	            
	            final long irigvall = GDR.get_u32(packetData,offset);
	            offset += 4;
	            
	            final int days = (int)((irigvalh & 0x0FFF0000) >>> 16);
	            final int hours = (int)((irigvalh & 0x0000FF00) >>> 8);
	            final int mins = (int)(irigvalh & 0x000000FF);
	            final int secs = (int)((irigvall & 0xFF000000) >>> 24);
	            final int millis = (int)((irigvall & 0x00FFF000) >>> 12);
	            final int micros = (int)(irigvall & 0x00000FFF);
	            
	            //assume year is current year
	            cal.set(Calendar.DAY_OF_YEAR,days);
	            cal.set(Calendar.HOUR_OF_DAY,hours);
	            cal.set(Calendar.MINUTE,mins);
	            cal.set(Calendar.SECOND,secs);
	            cal.set(Calendar.MILLISECOND,millis);
	            
	            final IAccurateDateTime ert = new AccurateDateTime(cal.getTimeInMillis(),micros,false);
	            
	            pairs.add(new Pair<ISclk,IAccurateDateTime>(sclk,ert));
		    }
	    }
	    catch(final RuntimeException e)
	    {
	    	tracer.error("Error encountered while processing SSE time correlation packet (" + pm.getPacketInfo().getIdentifierString() + "): " + e.getMessage());
	    	return;
	    }

	    if(!pairs.isEmpty())
	    {
	    	publishCorrelationMessage(pm,pairs);
	    }
	    else
	    {
	    	tracer.warn("Received SSE time correlation packet " + pm.getPacketInfo().getIdentifierString() + " that contained no time correlation information."); 
	    }
	}
	
	/**
	 * Publishes an SseTimeCorrelationMessage
	 * 
	 * @param pm
	 *            The IPacketMessage that contains the TC packet
	 * @param pairs
	 *            the SCLK/ERT correlation pairs parsed from the packet
	 */
	private void publishCorrelationMessage(final ITelemetryPacketMessage pm, final List<Pair<ISclk,IAccurateDateTime>> pairs)
	{
		final ISseTimeCorrelationMessage m = tcMessageFactory.createSseTimeCorrelationMessage(pairs,
                pm.getPacketInfo().getErt(), pm.getPacketInfo().getSclk());

		m.setFromSse(true);

		bus.publish(m);
	}
}
