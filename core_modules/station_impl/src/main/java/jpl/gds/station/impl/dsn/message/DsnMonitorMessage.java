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
package jpl.gds.station.impl.dsn.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.serialization.station.Proto3DsnMonitorMessage;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;
import jpl.gds.station.impl.dsn.chdo.ChdoConfiguration;
import jpl.gds.station.impl.dsn.chdo.ChdoSfdu;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.ByteString;

/**
 * This is an internal message class used to transport an SFDU containing
 * DSN station monitor data (a MON-0158 SFDU).
 * 
 *
 */
public class DsnMonitorMessage extends Message implements IDsnMonitorMessage {

    private IChdoSfdu theMonSfdu;
    private static final Tracer log = TraceManager.getDefaultTracer();

    /**
     * Constructor.
     * 
     * @param sfdu the MON-0158 SFDU this message carries
     */
    public DsnMonitorMessage(final IChdoSfdu sfdu) {
        super(StationMessageType.DsnStationMonitor, System.currentTimeMillis());
        setMonitorSfdu(sfdu);
    }

    /**
     * Constructor that requires a Proto3DsnMonitorMessage.
     *
     * @param msg the received protobuf message
     * @param factory IStationHeaderFactory
     *
     * @throws IOException an error occurs during parsing of the message
     */
    public DsnMonitorMessage(final Proto3DsnMonitorMessage msg, final IStationHeaderFactory factory) throws IOException {
        super(StationMessageType.DsnStationMonitor, msg.getSuper());

        try {
            theMonSfdu = factory.createChdoSfdu();
            final IChdoSfdu sfdu = new ChdoSfdu(new ChdoConfiguration(new SseContextFlag(fromSse)));
            final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(msg.getChdoSfdu().toByteArray()));
            sfdu.readSfdu(dis);
            this.theMonSfdu = sfdu;
        }
        catch (SfduException e) {
            log.error("Error creating CHDO SFDU " + ExceptionTools.getMessage(e));
        }
    }

    @Override
    public IChdoSfdu getMonitorSfdu() {
        return theMonSfdu;
    }

    /**
     * Sets the ChdoSfdu containing the MON data.
     * 
     * @param monSfdu ChdoSfdu object to set
     */
    public void setMonitorSfdu(final IChdoSfdu monSfdu) {

        if (monSfdu == null) {
            throw new IllegalArgumentException("The input SFDU object cannot be null");
        }

        this.theMonSfdu = monSfdu;
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {

        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));

    }

    @Override
    public String getOneLineSummary() {
        return "MonitorSfdu" + " time " + getEventTimeString() + " length " +
                (theMonSfdu.getBytes() == null ? "0" : theMonSfdu.getBytes().length);
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }

    /**
     * BinaryParseHandler is the message-specific SAX parse handler for creating this Message from its binary
     * representation.
     *
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {
        final private IStationHeaderFactory factory;

        /**
         * Constructor.
         *
         * @param context
         *            current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            this.factory = context.getBean(IStationHeaderFactory.class);
        }

        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {
            for (final byte[] msgBytes : content) {
                final Proto3DsnMonitorMessage dsnMsg = Proto3DsnMonitorMessage.parseFrom(msgBytes);
                final IDsnMonitorMessage message = new DsnMonitorMessage(dsnMsg, factory);
                addMessage(message);
            }
            return getMessages();
        }
    }

    @Override
    public synchronized byte[] toBinary() {
        return this.build().toByteArray();
    }

    @Override
    public Proto3DsnMonitorMessage build(){
        final Proto3DsnMonitorMessage.Builder retVal = Proto3DsnMonitorMessage.newBuilder();

        retVal.setSuper((Proto3AbstractMessage)super.build());
        retVal.setChdoSfdu(ByteString.copyFrom(this.theMonSfdu.getBytes()));

        return retVal.build();
    }
}
