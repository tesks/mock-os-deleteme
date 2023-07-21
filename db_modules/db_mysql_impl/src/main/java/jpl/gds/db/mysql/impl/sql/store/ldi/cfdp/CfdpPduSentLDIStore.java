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
package jpl.gds.db.mysql.impl.sql.store.ldi.cfdp;

import org.springframework.context.ApplicationContext;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpPduSentMessage;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduSentLDIStore;
import jpl.gds.db.mysql.impl.sql.store.ldi.AbstractLDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.TimeTooLargeException;

public class CfdpPduSentLDIStore extends AbstractCfdpLDIStore implements ICfdpPduSentLDIStore {

	private final BytesBuilder _bb = new BytesBuilder();

	/**
	 * Creates an instance of ICfdpPduSentMessageLDIStore.
	 *
	 * @param appContext
	 *            The information about the current test session
	 */
	public CfdpPduSentLDIStore(final ApplicationContext appContext) {
		super(appContext, ICfdpPduSentLDIStore.STORE_IDENTIFIER, false);
	}

	/**
	 * Stop storing command messages.
	 */
	@Override
	protected void stopResource() {
		super.stopResource();

		if (messagePublicationBus != null) {
			messagePublicationBus.unsubscribe(CfdpMessageType.CfdpPduSent, handler);
		}

	}

	@Override
	public void insertMessage(ICfdpPduSentMessage m) throws DatabaseException {

		if (dbProperties.getUseDatabase() && !isStoreStopped.get() && m instanceof ICfdpPduSentMessage) {
			final ICfdpPduSentMessage cfdpPduSentMessage = m;

			try {

				synchronized (this) {
					if (!archiveController.isUp()) {
						throw new IllegalStateException("This connection has already been closed");
					}

					_bb.clear();

					// Format as a line for LDI

					insertValOrNull(_bb, extractSessionIdIfAny(cfdpPduSentMessage.getContextKey()));
					_bb.insertSeparator();

					insertValOrNull(_bb, extractSessionHostIdIfAny(cfdpPduSentMessage.getContextKey()));
					_bb.insertSeparator();

					insertValOrNull(_bb, extractSessionFragmentIfAny(cfdpPduSentMessage.getContextKey()));
					_bb.insertSeparator();

					try {
						_bb.insertDateAsCoarseFineSeparate(cfdpPduSentMessage.getEventTime());
					} catch (final TimeTooLargeException ttle) {
						TraceManager.getDefaultTracer().warn(

								"pduTime exceeded maximum");
					}

					_bb.insertTextAllowReplace(cfdpPduSentMessage.getHeader().getCfdpProcessorInstanceId());
					_bb.insertSeparator();

					_bb.insertTextAllowReplace(cfdpPduSentMessage.getPduId());
					_bb.insertSeparator();

					String flattenedMetadata = "";
					String delim = "";
					
					for (String s : cfdpPduSentMessage.getMetadata()) {
						flattenedMetadata += delim + s.replaceAll(",", "\\,");
						delim = ",";
					}
					_bb.insertTextAllowReplace(flattenedMetadata);
					_bb.insertSeparator();

					_bb.insert(extractContextId(cfdpPduSentMessage.getContextKey()));
					_bb.insertSeparator();

					_bb.insert(extractContextHostId(cfdpPduSentMessage.getContextKey()));
					_bb.insertTerminator();

					// Add the line to the LDI batch
					writeToStream(_bb);
				}

			} catch (

			final RuntimeException re) {
				throw re;
			} catch (final Exception e) {
				throw new DatabaseException("Error inserting CfdpPduSent record intodatabase", e);
			}

		}

	}

	/**
	 * Start this store.
	 */
	@Override
	public void startResource() {
		super.startResource();

		handler = new BaseMessageHandler() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public synchronized void handleMessage(final IMessage m) {
				ICfdpPduSentMessage cfdpPduSentMessage = (ICfdpPduSentMessage) m;

				try {
					insertMessage(cfdpPduSentMessage);
				} catch (final DatabaseException de) {
					trace.error("LDI CfdpPduSent storage failed: ", de);
				}

			}

		};

		// Subscribe to CFDP PDU Sent Messages on the internal bus
		if (messagePublicationBus != null) {
			messagePublicationBus.subscribe(CfdpMessageType.CfdpPduSent, handler);
		}
	}

}
