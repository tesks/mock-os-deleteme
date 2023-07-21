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
package jpl.gds.tc.mps.impl.ctt;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code InternalMpsCommandStemMetadata} is a package-private data structure that represents information about a
 * command stem according to CTS.
 *
 * @since 8.2.0
 */
class InternalMpsCommandStemMetadata {

    /*
    Note: Because this is a pure get/set POJO, no unit test needed.
     */

    private String title;
    private int opcode;
    private String moduleName;
    private List<Integer> categoryNumbers = new ArrayList<>();
    private List<Integer> classNumbers = new ArrayList<>();
    private List<InternalMpsCommandArgumentMetadata> args = new ArrayList<>();

    /**
     * @return
     */
    String getTitle() {
        return this.title;
    }

    /**
     * @param title
     * @return
     */
    InternalMpsCommandStemMetadata setTitle(final String title) {
        this.title = title;
        return this;
    }

    int getOpcode() {
        return this.opcode;
    }

    InternalMpsCommandStemMetadata setOpcode(final int opcode) {
        this.opcode = opcode;
        return this;
    }

    String getModuleName() {
        return this.moduleName;
    }

    InternalMpsCommandStemMetadata setModuleName(final String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    List<Integer> getCategoryNumbers() {
        return this.categoryNumbers;
    }

    InternalMpsCommandStemMetadata addCategoryNumber(final int categoryNumber) {
        this.categoryNumbers.add(categoryNumber);
        return this;
    }

    List<Integer> getClassNumbers() {
        return this.classNumbers;
    }

    InternalMpsCommandStemMetadata addClassNumber(final int classNumber) {
        this.classNumbers.add(classNumber);
        return this;
    }

    List<InternalMpsCommandArgumentMetadata> getArgumentsMetadata() {
        return this.args;
    }

    InternalMpsCommandStemMetadata addArgumentMetadata(final InternalMpsCommandArgumentMetadata argumentMetadata) {
        args.add(argumentMetadata);
        return this;
    }

}
