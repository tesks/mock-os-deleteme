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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.eu.IEUCalculationFactory;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;
import jpl.gds.dictionary.api.decom.types.IBooleanDefinition;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;
import jpl.gds.dictionary.api.decom.types.IDynamicArrayDefinition;
import jpl.gds.dictionary.api.decom.types.IEnumDataDefinition;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition;
import jpl.gds.dictionary.api.decom.types.IIntegerDefinition;
import jpl.gds.dictionary.api.decom.types.INumericDataDefinition;
import jpl.gds.dictionary.api.decom.types.IOpcodeDefinition;
import jpl.gds.dictionary.api.decom.types.IStaticArrayDefinition;
import jpl.gds.dictionary.api.decom.types.IStringDefinition;
import jpl.gds.dictionary.api.decom.types.ITimeDefinition;
import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * This listener writes generic decom events to a print stream.
 *
 */
public class DecomPrinterListener extends AbstractChannelDecomListener {

	private static final String DEFAULT_FLOAT = "16.3%f";
    private final Tracer         log;   
	private final PrintWriter out;

	private final Map<String, String> stemByOpcodeMap;

	private final SclkFormatter sclkFormatter;
	private final IEUCalculationFactory euFactory;

	/**
	 * Construct a new DecomPrinterListener
	 * @param out the print writer that output text should be written to
	 * @param appContext the current application context
	 */
	public DecomPrinterListener(final PrintWriter out, final ApplicationContext appContext) {
		super(appContext.getBean(IChannelDefinitionProvider.class).getChannelDefinitionMap());
        this.log = TraceManager.getTracer(appContext, Loggers.TLM_EHA);
		this.out = out;

		final ICommandDefinitionProvider provider = appContext.getBean(ICommandDefinitionProvider.class);
		this.stemByOpcodeMap = provider == null ? Collections.emptyMap() : provider.getStemByOpcodeMap();

		sclkFormatter = TimeProperties.getInstance().getSclkFormatter();
		euFactory = appContext.getBean(IEUCalculationFactory.class);
		
	}

	@Override
	public void onEnum(final IEnumDataDefinition def, final int val) {
		String value = null;
		
		if (def.getChannelId().isEmpty() || !def.shouldChannelize()) {
			value = (def.getLookupTable() == null) ? null : def.getLookupTable().getValue(val);
		} else {
			final IChannelDefinition chanDef = findChannelValue(def);
			if (chanDef == null) {
				log.warn("Could not find definition for field with channel ID=" + def.getChannelId());
			} else {
				value = (chanDef.getLookupTable() == null) ? null : chanDef.getLookupTable().getValue(val);
			}
		}
		printValue(def, value);

	}

	@Override
	public void onOpcode(final IOpcodeDefinition def, final int val) {
		Indent.print(out);
		out.println(String.format("%s: %s", def.getName(), stemByOpcodeMap.getOrDefault(val, "OPCODE_NOT_FOUND")));
	}

	@Override
	public void onArrayStart(final IStaticArrayDefinition def) {
        Indent.print(out);
        out.println("Array " + def.getName() + " (length="
                + def.getLength() + ")");
        Indent.incr();
	}

	@Override
	public void onArrayEnd(final IStaticArrayDefinition def) {
		Indent.decr();
	}

	@Override
	public void onArrayStart(final IDynamicArrayDefinition def) {
		Indent.print(out);
		out.println("Array " + def.getName() + " (length=variable" + ")");
	}

	@Override
	public void onArrayEnd(final IDynamicArrayDefinition def) {
		Indent.decr();
	}

	@Override
	public void onTime(final ITimeDefinition def, final ISclk sclk) {
		Indent.print(out);
		out.println(String.format("%s: %s", def.getAlgorithmId(), sclkFormatter.fmt(sclk)));
	}

	@Override
	public void onBoolean(final IBooleanDefinition def, final boolean val) {
		String valueFormat = def.getFormat();
		if (valueFormat.isEmpty()) { 
			valueFormat = "%s";
		}
		String valAsStr = def.getFalseString();
		if (val) {
			valAsStr = def.getTrueString();
		}
		printValue(def, valAsStr);
	}

	@Override
	public void onInteger(final IIntegerDefinition def, final long val) {
		if (def.getChannelId() == null || channelLookup.get(def.getChannelId()) == null) { 
			
			/** Added channel lookup to print ENUM */
			final IChannelDefinition chanDef = findChannelValue(def);
			
			if (chanDef != null && chanDef.getLookupTable() != null) {
				final String state = chanDef.getLookupTable().getValue(val);
				if (state != null && !state.isEmpty()) {
					printValue(def, state);
					return;
				}
			}
			
		}
		String valueFormat = def.getFormat();
		
		if (def.getFormat().isEmpty()) {
			if (def.isUnsigned()) {
				valueFormat = "%u";
			} else {
				valueFormat = "%d";
			}
		} 
		printValue(def, val, valueFormat);
	}

	@Override
	public void onString(final IStringDefinition def, final String val) {
		String valueFormat = def.getFormat();
		if (valueFormat.isEmpty()) {
			valueFormat = "%s";
		}
        final String valStr = new SprintfFormat().anCsprintf(valueFormat, val);
		printValue(def, valStr);
	}


	@Override
	public void onDouble(final IFloatingPointDefinition def, final double val) {
		Indent.print(out);
		String valueFormat = def.getFormat();
		if (valueFormat == null || valueFormat.isEmpty()) {
			valueFormat = "%f";
		}
		printValue(def, val, valueFormat);
	}

	private void printValue(final IDecomDataDefinition def, final String formatted) {
		out.println(String.format("%s: %s", def.getName(), formatted));
	}

	private void printValue(final INumericDataDefinition def, final Number dn, final String dnFormat) {
		IEUDefinition dnToEu = null;
		String numberFormat = dnFormat;
		String numberUnits = def.getUnits();
		if (def.getChannelId().isEmpty() || !def.shouldChannelize()) {
			dnToEu = def.getDnToEu();
		} else {
			final IChannelDefinition chanDef = findChannelValue(def);
			if (chanDef == null) {
				log.warn("Could not find definition for field with channel ID=" + def.getChannelId());
			} else {
				dnToEu = chanDef.getDnToEu();
				if (dnToEu != null) {
					numberFormat = chanDef.getEuFormat();
					numberUnits = chanDef.getEuUnits();
				} else {
					dnToEu = def.getDnToEu();
				}
			}
		}

		String valString = null;
		if (dnToEu != null) {
			try {
				final IEUCalculation euCalc = euFactory.createEuCalculator(dnToEu);
                valString = new SprintfFormat().anCsprintf(numberFormat, euCalc.eu(dn.doubleValue()));
			} catch (final EUGenerationException e) {
				log.warn("DN to EU conversion failed during decom: " + e.toString());
			}
		}
		if (valString == null) {
            valString = new SprintfFormat().anCsprintf(numberFormat, dn);
		}
		if (numberUnits != null && numberUnits.length() > 0) {
			out.println(String.format("%s: %s %s", def.getName(), valString, numberUnits));
		} else {
			out.println(String.format("%s: %s", def.getName(), valString));
		}
	}

	@Override
	public void onFloat(final IFloatingPointDefinition def, final float val) {
		String valueFormat = def.getFormat();
		if (valueFormat.isEmpty()) {
			valueFormat = "%f";
		}
		printValue(def, val, valueFormat);
	}

	@Override
	public void onVariable(final IVariableStatementDefinition def, final long val) {
		Indent.print(out);
		out.println(String.format("%s: %s", def.getVariableName(), Long.toUnsignedString(val)));
	}

	@Override
	public void onChannel(final IChannelStatementDefinition def, final long val) {
		Indent.print(out);
		if (def.getChannelType() == ChannelType.UNSIGNED_INT) {
			out.println(String.format("%s: %s", def.getChannelDefinition().getTitle(), Long.toUnsignedString(val)));
		} else {
			out.println(String.format("%s: %d", def.getChannelDefinition().getTitle(), val));
		}

	}

	@Override
	public void onChannel(final IChannelStatementDefinition def, final float val) {
		Indent.print(out);
		out.println(String.format("%s: " + DEFAULT_FLOAT, def.getChannelDefinition().getTitle(), val));
	}

	@Override
	public void onChannel(final IChannelStatementDefinition def, final double val) {
		Indent.print(out);
		out.println(String.format("%s: " + DEFAULT_FLOAT, def.getChannelDefinition().getTitle(), val));
	}

	@Override
	public void onChannel(final IChannelStatementDefinition def, final String val) {
		Indent.print(out);
		out.println(String.format("%s: %s", def.getChannelDefinition().getTitle(), val));
	}

}
