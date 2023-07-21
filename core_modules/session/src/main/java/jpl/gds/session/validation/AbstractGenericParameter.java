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
package jpl.gds.session.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.sys.SystemUtilities;


/**
 * Abstract base class for parameter classes that adds the generic type.
 *
 * This is split out from AbstractParameter so that a class can extend
 * the one it needs.
 *
 * The typedNull makes PMD stop suggesting that the null constants be made
 * static. They cannot be because of the generic parameter. But if we make
 * the right side also dependent upon T, all is well.
 *
 * The functor interfaces must take their own generic parameter; they cannot
 * use AbstractGenericParameter's. But they must be defined here instead of in
 * AbstractParameterFunctor because they are used by ParameterAttributes, and
 * potentially other non-functor classes.
 *
 *
 * @param <T> Enum of parameters
 */
abstract public class AbstractGenericParameter<T extends Enum<T>>
    extends AbstractParameter
{

    /** Convenience for subclasses */
    protected final Level1Functor<T> NULL_LEVEL_1_FUNCTOR =
        SystemUtilities.<Level1Functor<T>>typedNull();

    /** Convenience for subclasses */
    protected final Level2Functor<T> NULL_LEVEL_2_FUNCTOR =
        SystemUtilities.<Level2Functor<T>>typedNull();

    /** Convenience for subclasses */
    protected final Level3Functor<T> NULL_LEVEL_3_FUNCTOR =
        SystemUtilities.<Level3Functor<T>>typedNull();

    /** Convenience for subclasses */
    protected final DefaultFunctor<T> NULL_DEFAULT_FUNCTOR =
        SystemUtilities.<DefaultFunctor<T>>typedNull();

    /** Convenience for subclasses */
    protected final SpecialFunctor<T> NULL_SPECIAL_FUNCTOR =
        SystemUtilities.<SpecialFunctor<T>>typedNull();

    /** Convenience for subclasses */
    protected final AssignmentFunctor<T> NULL_ASSIGNMENT_FUNCTOR =
        SystemUtilities.<AssignmentFunctor<T>>typedNull();

    protected final PostProcessingFunctor<T> NULL_POST_PROCESSING_FUNCTOR =
        SystemUtilities.<PostProcessingFunctor<T>>typedNull();


    /**
     * Constructor.
     */
    protected AbstractGenericParameter()
    {
        super();
    }


    /**
     * Level 1 functor interface.
     *
     * @param <T> Parameter enum type
     */
    protected interface Level1Functor<T extends Enum<T>>
    {
        /**
         * Validate at level 1.
         *
         * @param pa     Parameter attribute
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void validate(final ParameterAttributes<T> pa,
                             final State                  state,
                             final List<String>           errors)
            throws ParameterException;
    }


    /**
     * Level 2 functor interface.
     * @param <T> class the extends Enum to operate on
     */
    protected interface Level2Functor<T extends Enum<T>>
    {
        /**
         * Validate at level 2.
         *
         * @param pa     Parameter attribute
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void validate(final ParameterAttributes<T> pa,
                             final State                  state,
                             final List<String>           errors)
            throws ParameterException;
    }


    /**
     * Level 3 functor interface.
     * @param <T> class the extends Enum to operate on
     */
    protected interface Level3Functor<T extends Enum<T>>
    {
        /**
         * Validate at level 3.
         *
         * @param map     Map of parameter attributes
         * @param state   State
         * @param errors  List to be populated with errors
         * @param serious Set of serious errors above us
         *
         * @throws ParameterException On any error
         */
        public void validate(final Map<T, ParameterAttributes<T>> map,
                             final State                          state,
                             final List<String>                   errors,
                             final Set<T>                         serious)
            throws ParameterException;
    }


    /**
     * Default functor interface.
     * @param <T> class the extends Enum to operate on
     */
    public interface DefaultFunctor<T extends Enum<T>>
    {
        /**
         * Construct a default if required.
         *
         * @param sc     SessionConfiguration
         * @param map    Parameter attributes
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void constructDefault(
                        final SessionConfiguration           sc,
                        final Map<T, ParameterAttributes<T>> map,
                        final State                          state,
                        final List<String>                   errors)
            throws ParameterException;
    }


    /**
     * Special functor interface.
     * @param <T> class the extends Enum to operate on
     */
    protected interface SpecialFunctor<T extends Enum<T>>
    {
        /**
         * Perform special processing as required.
         *
         * @param map    Parameter attributes
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void specialProcessing(
                        final Map<T, ParameterAttributes<T>> map,
                        final State                          state,
                        final List<String>                   errors)
            throws ParameterException;
    }


    /**
     * Assignment functor interface.
     * @param <T> class the extends Enum to operate on
     */
    public interface AssignmentFunctor<T extends Enum<T>>
    {
        /**
         * Assign parameter to appropriate configuration object.
         *
         * @param sc     SessionConfiguration
         * @param pa     Parameter attribute
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void assign(final SessionConfiguration   sc,
                           final ParameterAttributes<T> pa,
                           final State                  state,
                           final List<String>           errors)
            throws ParameterException;
    }


    /**
     * Post-processing functor interface.
     * @param <T> class the extends Enum to operate on
     */
    protected interface PostProcessingFunctor<T extends Enum<T>>
    {
        /**
         * Performs actions after all parameters have been dealt with..
         *
         * @param sc     SessionConfiguration
         * @param map    Parameter attributes
         * @param state  State
         * @param errors List to be populated with errors
         *
         * @throws ParameterException On any error
         */
        public void perform(final SessionConfiguration           sc,
                            final Map<T, ParameterAttributes<T>> map,
                            final State                          state,
                            final List<String>                   errors)
            throws ParameterException;
    }
}
