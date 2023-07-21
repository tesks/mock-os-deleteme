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
package jpl.gds.evr.impl.service.notify;

import java.util.HashSet;
import java.util.Set;

import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.service.IEvrNotificationTrigger;

/**
 * This class defines the trigger that is based on EVR's categories, namely the
 * EVR's level, module, and operational category. Trigger can have a set of
 * levels, a set of modules, and set a set of operational categories. Not all
 * categories have to be defined. For those categories that have trigger
 * definitions, the boolean-AND of the triggered categories have to be true for
 * this trigger to be tripped. Within a single category, the boolean-OR has to
 * be true (which is obvious).
 * 
 *
 */
public class EvrCategorialTrigger implements IEvrNotificationTrigger {
	private static final int LEVEL_SET_SIZE = 2;
	private static final int MODULE_SET_SIZE = 2;
	private static final int OPERATIONAL_CATEGORY_SET_SIZE = 2;

	private final Set<String> levels;
	private final Set<String> modules;
	private final Set<String> opscats;

	/**
	 * Default constructor.
	 */
	public EvrCategorialTrigger() {
		levels = new HashSet<String>(LEVEL_SET_SIZE);
		modules = new HashSet<String>(MODULE_SET_SIZE);
		opscats = new HashSet<String>(OPERATIONAL_CATEGORY_SET_SIZE);
	}

	/**
	 * Add a level trigger to the levels set.
	 * 
	 * @param level
	 *            level to look for in EVRs
	 */
	public void addLevel(final String level) {
		levels.add(level.toUpperCase()); // for case-insensitivity
	}

	/**
	 * Add a module trigger to the modules set.
	 * 
	 * @param module
	 *            module to look for in EVRs
	 */
	public void addModule(final String module) {
		modules.add(module.toUpperCase()); // for case-insensitivity
	}

	/**
	 * Add an operational category to the trigger set.
	 * 
	 * @param opscat
	 *            operational category to look for in EVRs
	 */
	public void addOperationalCategory(final String opscat) {
		opscats.add(opscat.toUpperCase()); // for case-insensitivity
	}

	/**
	 * Get the set of levels defined in this trigger.
	 * 
	 * @return list of levels that this trigger looks for
	 */
	public Set<String> getLevels() {
		return levels;
	}

	/**
	 * Get the set of modules defined in this trigger.
	 * 
	 * @return list of modules that this trigger looks for
	 */
	public Set<String> getModules() {
		return modules;
	}

	/**
	 * Get the set of operational categories defined in this trigger.
	 * 
	 * @return list of operational categories that this trigger looks for
	 */
	public Set<String> getOperationalCategories() {
		return opscats;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.evr.api.service.IEvrNotificationTrigger#evrTriggersNotification(jpl.gds.evr.api.IEvr)
	 */
	@Override
	public boolean evrTriggersNotification(final IEvr evr) {
		boolean levelMatches = true;
		boolean moduleMatches = true;
		boolean opscatMatches = true;

		if (!levels.isEmpty()) {
			levelMatches = false;
		}

		if (!modules.isEmpty()) {
			moduleMatches = false;
		}

		if (!opscats.isEmpty()) {
			opscatMatches = false;
		}

		if (evr.getLevel() != null
				&& levels.contains(evr.getLevel().toUpperCase())) { // for
																	// case-insensitivity
			levelMatches = true;
		}

		if (evr.getCategory(IEvrDefinition.MODULE) != null
				&& modules.contains(evr.getCategory(IEvrDefinition.MODULE).toUpperCase())) { // for
																		// case-insensitivity
			moduleMatches = true;
		}

		if (evr.getCategory(IEvrDefinition.OPS_CAT) != null
				&& opscats.contains(evr.getCategory(IEvrDefinition.OPS_CAT).toUpperCase())) { // for
																		// case-insensitivity
			opscatMatches = true;
		}

		if (levelMatches && moduleMatches && opscatMatches) {
			return true;
		} else {
			return false;
		}

	}

}