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
package jpl.gds.eha.channel.api;

import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.shared.annotation.CustomerExtensible;

/**
 * SimpleEuBase is an abstract class that implements the IEUCalculation
 * interface. It should be used as a base class for building custom simple EU
 * computation classes that do not require parameters. Because it extends
 * GeneralAlgorithmBase, it also provides utility methods for accessing the
 * channel LAD and performing some basic dictionary-related functions.
 * 
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * 
 *
 * @see IEUCalculation
 */
@CustomerExtensible(immutable = false)
public abstract class SimpleEuBase extends GeneralAlgorithmBase implements
IEUCalculation {
    // no new methods beyond GeneralAlgorithmBase
}
