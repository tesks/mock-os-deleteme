/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.impl.scmf;

import jpl.gds.tc.api.IScmfSfduHeader;

public class ScmfSfduHeader implements IScmfSfduHeader {

    /** The data set ID is always the same for an SCMF */
    private static String           DATA_SET_ID                  = "SCMF";

    private String productVersion;
    private String time;
    private String spacecraftName;
    private String spacecraftId;
    private String missionName;
    private String missionId;
    private String fileName;

    private String getHeaderLine(final String label, final String value) {
        final StringBuilder buffer = new StringBuilder(128);

        buffer.append(label);
        buffer.append(" = ");
        buffer.append(value);
        buffer.append(";\n");

        return (buffer.toString());
    }

    @Override
    public String getHeaderString() {
        final StringBuffer buffer = new StringBuffer(1024);

        buffer.append("\n");
        buffer.append(getHeaderLine(DATA_SET_ID_LABEL, DATA_SET_ID));
        buffer.append(getHeaderLine(IScmfSfduHeader.FILE_NAME_LABEL, getFileName().trim()));
        buffer.append(getHeaderLine(IScmfSfduHeader.MISSION_NAME_LABEL, getMissionName()
                .toUpperCase().trim()));
        buffer.append(getHeaderLine(MISSION_ID_LABEL, getMissionId()));
        buffer.append(getHeaderLine(SPACECRAFT_NAME_LABEL, getSpacecraftName()
                .trim()));
        buffer.append(getHeaderLine(IScmfSfduHeader.SPACECRAFT_ID_LABEL, getSpacecraftId()));
        buffer.append(getHeaderLine(IScmfSfduHeader.PRODUCT_CREATION_TIME_LABEL,
                getProductCreationTime().trim()));
        buffer.append(getHeaderLine(IScmfSfduHeader.PRODUCT_VERSION_LABEL, getProductVersion()
                .trim()));

        return (buffer.toString());
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getMissionId() {
        return missionId;
    }

    @Override
    public void setMissionId(final String missionId) {
        this.missionId = missionId;
    }

    @Override
    public String getMissionName() {
        return missionName;
    }

    @Override
    public void setMissionName(final String missionName) {
        this.missionName = missionName;
    }

    @Override
    public String getSpacecraftId() {
        return spacecraftId;
    }

    @Override
    public void setSpacecraftId(final String spacecraftId) {
        this.spacecraftId = spacecraftId;
    }

    @Override
    public String getSpacecraftName() {
        return spacecraftName;
    }

    @Override
    public void setSpacecraftName(final String spacecraftName) {
        this.spacecraftName = spacecraftName;
    }

    @Override
    public String getProductCreationTime() {
        return time;
    }

    @Override
    public void setProductCreationTime(final String time) {
        this.time = time;
    }

    @Override
    public String getProductVersion() {
        return productVersion;
    }

    @Override
    public void setProductVersion(final String productVersion) {
        this.productVersion = productVersion;
    }
}
