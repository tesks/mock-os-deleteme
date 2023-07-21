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
package jpl.gds.tcapp.app.gui.fault;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;

/**
 * An enumeration that describes the flow of steps in the fault injection wizard
 * for HW/FSW commands.
 * 
 *
 */
public enum CommandFaultInjectionPageState implements
		UplinkFaultInjectorPageState {
	/**
	 * Command builder state
	 */
	COMMAND_BUILDER {
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.tcapp.app.gui.fault.CommandFaultInjectionPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
				final Composite contentComposite) throws FaultInjectorException {
			try {
				return (new CommandBuilderComposite(appContext, contentComposite, false));
			} catch (final DictionaryException e) {
				throw new FaultInjectorException(e);
			}
		}
	},

	/**
	 * Frame editor state
	 */
	FRAME_EDITOR {
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.tcapp.app.gui.fault.CommandFaultInjectionPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
				final Composite contentComposite) throws FaultInjectorException {
			return (new FrameEditorComposite(appContext, contentComposite));
		}
	},

	/**
	 * CLTU editor state
	 */
	CLTU_EDITOR {
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.tcapp.app.gui.fault.CommandFaultInjectionPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
				final Composite contentComposite) throws FaultInjectorException {
			return (new CltuEditorComposite(appContext, contentComposite));
		}
	},

	/**
	 * Raw output editor state
	 */
	RAW_OUTPUT_EDITOR {
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.tcapp.app.gui.fault.CommandFaultInjectionPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
				final Composite contentComposite) throws FaultInjectorException {
			return (new RawOutputEditorComposite(contentComposite));
		}
	},

	/**
	 * Uplink output state
	 */
	UPLINK_OUTPUT {
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.tcapp.app.gui.fault.CommandFaultInjectionPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
				final Composite contentComposite) throws FaultInjectorException {
			return (new UplinkOutputComposite(appContext, contentComposite));
		}
	};

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#getFirstPage()
	 */
	@Override
	public CommandFaultInjectionPageState getFirstPage() {
		return (values()[0]);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#getLastPage()
	 */
	@Override
	public CommandFaultInjectionPageState getLastPage() {
		return (values()[values().length - 1]);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#isFirstPage()
	 */
	@Override
	public boolean isFirstPage() {
		return (this.ordinal() == getFirstPage().ordinal());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#isLastPage()
	 */
	@Override
	public boolean isLastPage() {
		return (this.ordinal() == getLastPage().ordinal());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#getCurrentPageNumber()
	 */
	@Override
	public int getCurrentPageNumber() {
		return (this.ordinal() + 1);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#getLastPageNumber()
	 */
	@Override
	public int getLastPageNumber() {
		return (values().length);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#backState()
	 */
	@Override
	public CommandFaultInjectionPageState backState() {
		final int curValue = ordinal();
		if (curValue != 0) {
			return (values()[curValue - 1]);
		}

		return (null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#nextState()
	 */
	@Override
	public CommandFaultInjectionPageState nextState() {
		final int curValue = ordinal();
		if (curValue != (values().length - 1)) {
			return (values()[curValue + 1]);
		}

		return (null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.UplinkFaultInjectorPageState#getComponentForState(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public abstract FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext,
			Composite contentComposite) throws FaultInjectorException;
}