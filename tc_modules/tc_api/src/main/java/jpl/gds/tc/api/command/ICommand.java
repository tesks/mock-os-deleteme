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
package jpl.gds.tc.api.command;

/**
 * Empty marker interface for runtime (not dictionary) command classes. Hardware
 * commands, FSW commands, and SSE commands all implement this interface.
 * 
 *
 * 6/23/14 - MPCS-6304. Added interface, replacing the existing
 *          AbstractCommand class.
 */
public interface ICommand extends IDatabaseArchivableCommand {}
