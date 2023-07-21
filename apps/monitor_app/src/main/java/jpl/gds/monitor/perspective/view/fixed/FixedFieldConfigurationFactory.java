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
package jpl.gds.monitor.perspective.view.fixed;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.fields.BoxFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ImageFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.LineFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TextFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;

/**
 * This is a factory for creating fixed field configuration objects for fixed layout views.
 *
 */
public class FixedFieldConfigurationFactory {

	/**
	 * Creates a fixed field configuration for the given fixed field type.
	 * 
	 * @param type the desired FixedFieldType
	 * @return a new FixedFieldConfiguration of the appropriate type
	 */
	public static IFixedFieldConfiguration create(final ApplicationContext appContext, final FixedFieldType type) {
		switch(type) {
		case BOX: return new BoxFieldConfiguration();
		case LINE: return new LineFieldConfiguration();
		case TEXT: return new TextFieldConfiguration();
		case CHANNEL: return new ChannelFieldConfiguration(appContext);
		case BUTTON: return new ButtonFieldConfiguration();
		case IMAGE: return new ImageFieldConfiguration();
		case TIME: return new TimeFieldConfiguration(appContext);
		case HEADER: 
			final HeaderFieldConfiguration hc =  new HeaderFieldConfiguration(appContext);
			hc.load();
			return hc;
		}
		return null;
	}
}

