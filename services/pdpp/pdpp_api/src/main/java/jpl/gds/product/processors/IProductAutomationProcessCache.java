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
package jpl.gds.product.processors;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.entity.*;
import jpl.gds.product.processors.exceptions.AutomationProcessException;

import java.util.Collection;
import java.util.List;

/**
 * Interface for classes that start and stop product automation processes
 */
public interface IProductAutomationProcessCache {

    /**
     * Parses list of bogus dictionaries to determine if we should reactivate
     * them again
     * @return a list of bogus ids that have been returned to active status.
     */
    Collection<Long> checkBogusDictionaries();

    /**
     * Finds the least busy process for the given action.
     *
     * @param action ProductAutomationAction to be assigned to a process
     *
     * @return ProcessAutomationProcess The process that will perform the desired action on the specified product
     *
     * @throws AutomationException - Automation error occurred
     * @throws DictionaryException - Dictionary error occurred
     */
    ProductAutomationProcess getProcessor(final ProductAutomationAction action) throws AutomationException, DictionaryException;

    /**
     * Finds the least busy process for the given action type and status.
     *
     * @param actionClassMap ProductAutomationClassMap, the type of action to be performed on the product
     * @param status ProductAutomationStatus, which includes a reference to the product to be processed
     *
     * @return ProcessAutomationProcess The process that will perform the desired action on the specified product
     *
     * @throws AutomationException - Automation error occurred
     * @throws DictionaryException - Dictionary error occurred
     */
    ProductAutomationProcess getProcessor(final ProductAutomationClassMap actionClassMap, final ProductAutomationStatus status) throws AutomationException, DictionaryException;

    /**
     * Finds the least busy process for the given fsw build id and class map.
     *
     * @param product The product that needs to find a processor
     * @param actionClassMap The action to be performed on the product
     *
     * @return ProcessAutomationProcess The process that will perform the desired action on the specified product
     *
     * @throws AutomationException - Automation error occurred
     * @throws AutomationProcessException - Process could not start due to an unmapped or missing dictionary.
     * @throws DictionaryException - error loading mapper file.
     */
    ProductAutomationProcess getProcessor(final ProductAutomationProduct product, final ProductAutomationClassMap actionClassMap) throws AutomationException, DictionaryException;

    /**
     * Checks all running processes and will check to see if they are dead.  This is done by
     * checking if the process has assigned actions but has not completed any for a specific amount of time.
     *
     * @param deadTime Amount of time to be considered dead.
     * @return a list of processIds for all dead processes
     */
    List<Long> getDeadProcessIds(final int deadTime);

    /**
     * Kill all the processes in the cache.
     */
    void killAllProcesses();

    /**
     * Stop the process and will mark all the proper places in
     * the database for the process to shutdown.
     *
     * @param process process to be stopped
     * @throws AutomationException - Automation error occurred
     */
    void killProcess(final ProductAutomationProcess process) throws AutomationException;

    /**
     * Stop the process and will mark all the proper places in
     * the database for the process to shutdown.
     *
     * @param processId ID of process to be stopped
     * @throws AutomationException - Automation error occurred
     */
    void killProcess(final Long processId) throws AutomationException;
}
