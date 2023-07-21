package jpl.gds.tc.mps.impl.ctt.repo;

import java.io.File;

/**
 * Path holder for command translation table file paths
 *
 */
public class CommandTranslationTablePaths {
    private final String forwardTranslationPath;
    private final String reverseTranslationPath;

    /**
     * Constructor
     *
     * @param forwardTranslationPath path to the forward translation table
     * @param reverseTranslationPath path to the reverse translation table
     */
    public CommandTranslationTablePaths(final String forwardTranslationPath, final String reverseTranslationPath) {
        final File validateForward = new File(forwardTranslationPath);
        final File validateReverse = new File(reverseTranslationPath);

        if (!validateForward.exists() || !validateReverse.exists()) {
            final StringBuilder message = new StringBuilder();

            if (!validateForward.exists()) {
                message.append("The forward translation table path is invalid. ");
            }
            if (!validateReverse.exists()) {
                message.append("The reverse translation table path is invalid.");
            }

            throw new IllegalStateException(message.toString());
        }

        this.forwardTranslationPath = forwardTranslationPath;
        this.reverseTranslationPath = reverseTranslationPath;
    }

    /**
     * Forward translation table path
     *
     * @return
     */
    public String getForwardTranslationPath() {
        return forwardTranslationPath;
    }

    /**
     * Return translation table path
     *
     * @return
     */
    public String getReverseTranslationPath() {
        return reverseTranslationPath;
    }
}
