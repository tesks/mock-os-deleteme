/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.clt.entitymap;

import jpl.gds.cfdp.clt.ACfdpClt;

import static jpl.gds.cfdp.clt.ENonActionCommandType.ENTITY_MAP;

public class EntityMapClt extends ACfdpClt {

    private static final String MNEMONIC_COLUMN_HEADER = "Mnemonic";
    private static final String CFDP_ENTITY_ID_COLUMN_HEADER = "CFDP Entity ID";

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        showHelp(ENTITY_MAP.getCltCommandStr());
    }

    @Override
    public void run() {
        String[] keys = new String[cfdpCommonProperties.getMnemonics().size()];
        String[] values = new String[keys.length];
        int maxKeyLength = MNEMONIC_COLUMN_HEADER.length();
        int maxValueLength = CFDP_ENTITY_ID_COLUMN_HEADER.length();

        int i = 0;

        // Populate the table entries, also figuring out how much width we'll need for each column
        for (final String mnemonic : cfdpCommonProperties.getMnemonics()) {
            keys[i] = mnemonic;
            maxKeyLength = maxKeyLength < mnemonic.length() ? mnemonic.length() : maxKeyLength;
            values[i] = Long.toUnsignedString(cfdpCommonProperties.getEntityId(mnemonic));
            maxValueLength = maxValueLength < values[i].length() ? values[i].length() : maxValueLength;
            i++;
        }

        final String rowFormat = "| %-" + maxKeyLength + "s | %-" + maxValueLength + "s |%n";

        final String mnemonicColumnBar = new String(new char[maxKeyLength + 2]).replace("\0", "-");
        final String cfdpEntityIdColumnBar = new String(new char[maxValueLength + 2]).replace("\0", "-");
        final String fullRowBar = "+" + mnemonicColumnBar + "+" + cfdpEntityIdColumnBar + "+";

        System.out.println(fullRowBar);
        // Header
        System.out.format(rowFormat, MNEMONIC_COLUMN_HEADER, CFDP_ENTITY_ID_COLUMN_HEADER);
        System.out.println(fullRowBar);

        // Iterate rows and print
        for (i = 0; i < keys.length; i++) {
            System.out.format(rowFormat, keys[i], values[i]);
        }

        System.out.println(fullRowBar);
    }

}