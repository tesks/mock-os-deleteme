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
package jpl.gds.common.config.types;


/**
 * 
 * This class enumerates all the venue types that are allowed in session
 * configurations. The venue essentially defines the type of environment and
 * will dictate how some capabilities behave.
 * 
 */
public enum VenueType {
    
    /**
     * Unknown venue type.
     */
    UNKNOWN,

    /**
     * Testset venue type. This is for development environments.
     */
    TESTSET,
    /**
     * Testbed venue type. This is for flight testebed environments.
     */
    TESTBED,
    /**
     * Atlo venue type. The is for flight assembly-test-launch environments.
     */
    ATLO,
    /**
     * Ops venue type. This is for any operations environment.
     */
    OPS,
    /**
     * Cruise venue type. This is for an operations environment when the 
     * spacecraft is in cruise phase.
     */
    CRUISE,
    /**
     * Surface venue type. This is for an operations environment when the 
     * spacecraft is landed on a planetary body.
     */
    SURFACE,
    /**T
     * Orbit venue type. This is for an operations environment when the 
     * spacecraft is orbiting a planetary body.
     */
    ORBIT;

    /**
     * Returns topic name delimiter, or null.
     * 
     * @return topic name
     */
    public String getTopicNameDelimiter()
    {

        switch (this)
        {
        case CRUISE:
            return ("cruise");

        case SURFACE:
            return ("surface");

        case ORBIT:
            return ("orbit");

        case OPS:
            return ("ops");

        case TESTSET:
        case TESTBED:
        case ATLO:
        default:
            break;
        }

        return null;
    }


    /**
     * Returns true if this is testbed or ATLO, which is a popular
     * thing to check.
     * 
     * @return True if testbed or ATLO
     */
    public boolean isTestFacility()
    {

        return ((this == TESTBED) || (this == ATLO));
    }


    /**
     * Returns state of Testbed name.
     * 
     * @return True if there is a testbed name
     */
    public boolean hasTestbedName() {
        return (this == TESTBED || this == ATLO);
    }


    /**
     * Returns if the venue has streams.
     * 
     * @return if venue has streams
     */
    public boolean hasStreams() {
        return (this == TESTBED || this == ATLO);
    }

    /**
     * Returns if the venue is an ops venue.
     * 
     * @return if ops venue
     */
    public boolean isOpsVenue() {
        return ((this == CRUISE) || (this == SURFACE)
                || (this == OPS) || (this == ORBIT));
    }

//    /**
//     * Tells whether this venue is an OPS venue for the current mission.
//     * 
//     * @return True if this venue is an OPS venue for the current mission
//     */
//    public boolean isOpsVenueForMission() {
//        return isOpsVenue() && SessionProperties.getGlobalInstance().isVenueForMission(this);
//    }

}
