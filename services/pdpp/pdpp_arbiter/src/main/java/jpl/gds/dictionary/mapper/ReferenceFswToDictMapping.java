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

package jpl.gds.dictionary.mapper;

import jpl.gds.dictionary.api.mapper.IFswToDictMapping;

/**
 * This class was copied and modified from the MSL/M20 PDPP implementation. Not all of the code here is necessary for
 * non-M20 adaptations, but we did not have time during the PDPP multimissionization effort to simplify it.
 *
 * Comments were brought over intact for their historical value.
 */
public class ReferenceFswToDictMapping implements IFswToDictMapping{
    final long	fswBuildVersion;
    final String	fswReleaseVersion;
    final String	dictionaryVersion;
    final String    fswDirectory;
    final int		groundRevision;
    final String	customer;
    final String	timeStamp;

    /* MPCS-8242 10/21/16: Removed gdsVersion attribute */

    /**
     * MPCS-8276  6/13/2016 - Adding the mpdu_size attribute support.
     */
    final int mpduSize;

    /**
     * constructor sets all values.
     * @param fswBuildVersion
     * @param fswReleaseVersion
     * @param dictionaryVersion
     * @param groundRevision
     * @param fswDirectory
     * @param customer
     * @param timeStamp
     * @param mpduSize
     */
    public ReferenceFswToDictMapping(final long fswBuildVersion, final String fswReleaseVersion, final String dictionaryVersion, final int groundRevision, final String fswDirectory, final String customer, final String timeStamp, final int mpduSize) {
        this.fswBuildVersion = fswBuildVersion;
        this.fswReleaseVersion = fswReleaseVersion;
        this.dictionaryVersion = dictionaryVersion;
        this.groundRevision = groundRevision;
        this.fswDirectory = fswDirectory;
        this.customer = customer;
        this.timeStamp = timeStamp;
        this.mpduSize = mpduSize;
    }

    /**
     * @return the fswBuildVersion
     */
    @Override
    public long getFswBuildVersion() {
        return fswBuildVersion;
    }


    /**
     * @return the fswReleaseVersion
     */
    @Override
    public String getFswReleaseVersion() {
        return fswReleaseVersion;
    }

    /**
     * @return the dictionaryVersion
     */
    @Override
    public String getDictionaryVersion() {
        return dictionaryVersion;
    }

    /**
     * @return the groundRevision
     */
    @Override
    public int getGroundRevision() {
        return groundRevision;
    }

    /**
     * @return the flight software dictioanry directory
     */
    @Override
    public String getFswDirectory(){
        return this.fswDirectory;
    }

    /**
     * @return the customer
     */
    @Override
    public String getCustomer() {
        return customer;
    }

    /**
     * @return the timeStamp
     */
    @Override
    public String getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return the mpdu size attribute value.
     */
    @Override
    public int getMpduSize() {
        return mpduSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((customer == null) ? 0 : customer.hashCode());
        result = prime * result + ((dictionaryVersion == null) ? 0 : dictionaryVersion.hashCode());
        result = prime * result + (int)(fswBuildVersion ^ (fswBuildVersion >>> 32));
        result = prime * result + ((fswReleaseVersion == null) ? 0 : fswReleaseVersion.hashCode());
        result = prime * result + ((fswDirectory == null) ? 0 : fswDirectory.hashCode());
        result = prime * result + groundRevision;
        result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ReferenceFswToDictMapping other = (ReferenceFswToDictMapping)obj;
        if (customer == null) {
            if (other.customer != null)
                return false;
        }
        else if (!customer.equals(other.customer))
            return false;
        if (dictionaryVersion == null) {
            if (other.dictionaryVersion != null)
                return false;
        }
        else if (!dictionaryVersion.equals(other.dictionaryVersion))
            return false;
        if (fswBuildVersion != other.fswBuildVersion)
            return false;
        if (fswReleaseVersion == null) {
            if (other.fswReleaseVersion != null)
                return false;
        }
        else if (!fswReleaseVersion.equals(other.fswReleaseVersion))
            return false;
        if (groundRevision != other.groundRevision)
            return false;
        if (timeStamp == null) {
            if (other.timeStamp != null)
                return false;
        }
        else if (!timeStamp.equals(other.timeStamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "<FswToDictionaryMapping FSW_build_version_id=\"" + getFswBuildVersion() + "\" FSW_release_version_id=\"" + getFswReleaseVersion() + "\" dictionary_version_id=\""
                + getDictionaryVersion() + "\" ground_revision=\"" + getGroundRevision() + "\" FSW_directory=\"" + getFswDirectory() + "\" customer=\"" + getCustomer() + "\" timestamp=\"" + getTimeStamp()
                + "\"/>";
    }

    @Override
    public int compareTo(final IFswToDictMapping arg0) {
        return (int)(fswBuildVersion - arg0.getFswBuildVersion());
    }
}