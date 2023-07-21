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

import java.io.File;
import java.util.List;


/**
 * Level 1 validation functor for files.
 *
 * @param <T> Parameter set enum type
 *
 */
public final class Level1FileFunctor<T extends Enum<T>>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{
    /**
     * Constructor.
     */
    public Level1FileFunctor()
    {
        super();
    }


    /**
     * Validate string as file name at level 1.
     *
     * @param pa     Parameter attribute
     * @param state  State
     * @param errors List to be populated with errors
     *
     * @throws ParameterException On any error
     */
    @Override
    public void validate(final ParameterAttributes<T> pa,
                         final State                  state,
                         final List<String>           errors)
        throws ParameterException
    {
        final Value valueObject = pa.getValue();

        mustHaveValue(valueObject);

        final Constraints constraints = pa.getConstraints();
        final String      value       =
            valueObject.getValueAsString(UppercaseBool.DO_NOT_UPPERCASE);
        final File        file        = new File(value);
        final String      full        = getAbsolutePath(file);

        if (! checkFileExistence(pa, file.exists(),      full, errors) ||
            ! checkFileDirectory(pa, file.isDirectory(), full, errors))
        {
            return;
        }

        if (! constraints.checkBounds(full))
        {
            errors.add("Option "                         +
                       pa.getDisplayName()               +
                       " file-path '"                    +
                       full                              +
                       "' of length "                    +
                       full.length()                     +
                       " does not lie in length range [" +
                       constraints.getMinimum()          +
                       ","                               +
                       constraints.getMaximum()          +
                       "]");
            return;
        }

        valueObject.setValue(file);
    }


    /**
     * See whether it is OK for the file to exist (or not).
     *
     * @param pa     Parameter attribute
     * @param exists Does file exist?
     * @param full   Full file name
     * @param errors Error list
     *
     * @return True if OK
     */
    private boolean checkFileExistence(final ParameterAttributes<T> pa,
                                       final boolean                exists,
                                       final String                 full,
                                       final List<String>           errors)
    {
        switch (pa.getConstraints().getMustExist())
        {
            case MUST_EXIST:
                if (exists)
                {
                    return true;
                }

                break;

            case MUST_NOT_EXIST:
                if (! exists)
                {
                    return true;
                }

                break;

            case MAY_EXIST:
            default:
                return true;
        }

        errors.add("Option "           +
                   pa.getDisplayName() +
                   " file-path '"      +
                   full                +
                   "'"                 +
                   (exists ? " cannot exist" : " does not exist"));

        return false;
    }


    /**
     * See whether it is OK for the file to be a directory (or not).
     *
     * @param pa        Parameter attribute
     * @param directory Is file a directory?
     * @param full      Full file name
     * @param errors    Error list
     *
     * @return True if OK
     */
    private boolean checkFileDirectory(final ParameterAttributes<T> pa,
                                       final boolean                directory,
                                       final String                 full,
                                       final List<String>           errors)
    {
        final DirectoryFileBool wantDirectory =
            pa.getConstraints().getDirectory();

        if (directory)
        {
            if (! wantDirectory.get())
            {
                errors.add("Option "                  +
                           pa.getDisplayName()        +
                           " file-path '"             +
                           full                       +
                           "' specifies a directory " +
                           "but a regular file is needed");

                return false;
            }
        }
        else if (wantDirectory.get())
        {
            errors.add("Option "                     +
                       pa.getDisplayName()           +
                       " file-path '"                +
                       full                          +
                       "' specifies a regular file " +
                       "but a directory is needed");

            return false;
        }

        return true;
    }
}
