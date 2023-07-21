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
package jpl.gds.shared.channel;


/**
 * Keep track of channel ids, expanding ranges.
 *
 */
public class ChannelListRange
{
    private final static int    SHORT_CHANNEL_NUMBER = 3;
    private final static String RANGE_TOKEN                       = "..";
    private final static String RANGE_NAME_NUMBER_SEPERATOR_TOKEN = "-";

    private String   firstRangeName       = "";
    private String   secondRangeName      = "";
    private String   firstRangeNumber     = "";
    private String   secondRangeNumber    = "";
    private int      firstRangeValue      =  0;
    private int      secondRangeValue     =  0;
    private boolean  hasLeadingZeros      = false;
    private boolean  errorDuringParse     = false;
    private boolean  namesDoNotMatch      = false;
    private boolean  invalid1stRangeValue = false;
    private boolean  invalid2ndRangeValue = false;
    private boolean  rangeValuesNotOrdinal= false;
    private boolean  errorOccured         = false;
    private String[] channelList          = null;


    /**
     * Creates an instance of ChannelListRange.
     * 
     * @throws ChannelListRangeException Badly formatted range found
     */
    public ChannelListRange() throws ChannelListRangeException {
    }



    /**
     * Get list of channel ids.
     * 
     * @return Channels
     */
    public String[] getChannelList() {
        return channelList;
    } // end member function getChannelList()

    
    

    /**
     * Set list from input array.
     *
     * @param aNewChannelList Array of channel ids
     */
    public void setChannelList ( final String[] aNewChannelList ) {
        channelList = new String [ aNewChannelList.length ];
        for ( int i = 0; i < aNewChannelList.length; ++i ) {
            channelList [ i ] = ( aNewChannelList [ i ] );
        }
    } // end member function setChannelList()




    /**
     * Check if range names match.
     * 
     * @param aRangeValue Range
     * @throws ChannelListRangeException Bad range
     */
    private void parseRangeValue ( final String aRangeValue ) throws ChannelListRangeException {
        if ( rangeHasToken ( RANGE_TOKEN, aRangeValue ) ) {
            if ( rangeNamesMatch ( aRangeValue ) ) {
                setFirstRangeValue  ( aRangeValue );
                setSecondRangeValue ( aRangeValue );
                if ( rangeValuesAreValid() ) {
                    hasLeadingZeros = channelRangeHasLeadingZeros ( aRangeValue );
                }
            }
        }
    } // end member function parseRangeValue()



    
    /**
     * Get first range number.
     * 
     * @return Range number
     */
    private String getFirstRangeNumber() {
        return firstRangeNumber;
    } // end member function getFirstRangeNumber()
    

    /**
     * Get second range number.
     * 
     * @return Range number
     */
    private String getSecondRangeNumber() {
        return secondRangeNumber;
    } // end member function getSecondRangeNumber()


    
    /**
     * Set first range value.
     * 
     * @param aRangeValue Range
     * @throws ChannelListRangeException Bad range
     */
    private void setFirstRangeValue ( final String aRangeValue ) throws ChannelListRangeException {
        firstRangeValue = channelRangeStartingValue ( aRangeValue );
    } // end member function setFirstRangeNumber()



    /**
     * Set second range value.
     * 
     * @param aRangeValue Range
     * @throws ChannelListRangeException Bad range
     */
    private void setSecondRangeValue ( final String aRangeValue ) throws ChannelListRangeException {
        secondRangeValue = channelRangeEndingValue ( aRangeValue );
    }
    

    /**
     * Get first range value.
     * 
     * @return Value
     */
    private int getFirstRangeValue() {
        return firstRangeValue;
    }

    
    /**
     * Get second range value.
     * 
     * @return Value
     */
    private int getSecondRangeValue() {
        return secondRangeValue;
    }


    /**
     * Check if range has token.
     * 
     * @param aToken      Token
     * @param aRangeValue Range
     * @return Trie if has token
     */
    private static boolean rangeHasToken ( final String aToken, final String aRangeValue ) {
        return ( 0 < aRangeValue.indexOf ( aToken ) );
    } // end member function rangeHasToken()





    /**
     * Check if it is a range.
     * 
     * @param aRangeValue Possible range
     * @return True if range
     */
    private boolean isRange ( final String aRangeValue ) // does not throw an exception when testing for value
    {
        return ( 0 < aRangeValue.indexOf ( RANGE_TOKEN ) );
    }





    /**
     * Set first range name.
     * 
     * @param aRangeValue Range
     * @throws ChannelListRangeException Bad range
     */
    private void setFirstRangeName ( final String aRangeValue ) throws ChannelListRangeException {
        if ( rangeHasToken ( RANGE_TOKEN, aRangeValue ) ) {
            if ( rangeHasToken ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN, aRangeValue ) ) {
                firstRangeName = ( aRangeValue.substring ( 0, aRangeValue.indexOf ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN ) ) );
            }
            if ( 0 == firstRangeName.length() ) {
                firstRangeName = "";
            }
        }
    } // end member function setFirstRangeName()






    /**
     * Set second range name.
     * 
     * @param aRangeValue Range
     * @throws ChannelListRangeException Bad range
     */
    private void setSecondRangeName ( final String aRangeValue ) throws ChannelListRangeException {
        String nextRangeValue = aRangeValue.substring ( aRangeValue.indexOf ( RANGE_TOKEN ) + RANGE_TOKEN.length() );
        if ( !isRange ( nextRangeValue ) ) {
            if ( rangeHasToken ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN, nextRangeValue ) ) {
                secondRangeName = ( nextRangeValue.substring ( 0, nextRangeValue.indexOf ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN ) ) );
            } else {
                errorDuringParse = true;
                errorOccured     = true;
                throw new ChannelListRangeException ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN + " was not found in range: " + aRangeValue );
            }
            if ( 0 == secondRangeName.length() ) {
                errorDuringParse = true;
                errorOccured     = true;
                secondRangeName = "";
            }
        } else {
            errorDuringParse = true;
            errorOccured     = true;
            throw new ChannelListRangeException ( RANGE_TOKEN + " found in second range value " + aRangeValue );
        }
    } // end member function setSecondRangeName()




    /**
     * Check that range values are valid.
     * 
     * @return True if OK
     * @throws ChannelListRangeException Bad range
     */
    private boolean rangeValuesAreValid() throws ChannelListRangeException {
        if ( getFirstRangeNumber().length() != getSecondRangeNumber().length() ) {
            errorDuringParse = true;
            errorOccured     = true;
            throw new ChannelListRangeException (   "The count of the digits is different between first # and second #: ( "
                                                  + getFirstRangeNumber() + ", " + getSecondRangeNumber() );
        }
        if ( getFirstRangeValue() >= getSecondRangeValue() ) {
            rangeValuesNotOrdinal = true;
            throw new ChannelListRangeException (   "Range values must be ordinal (first # < second #): ( "
                                                  + getFirstRangeNumber() + ", " + getSecondRangeNumber() + " )" );
        }

        return true;
    }



    /**
     * Get first range name.
     * 
     * @return First range name
     */
    private String getFirstRangeName() {
        return firstRangeName;
    } // end member function getFirstRangeName()

    
    /**
     * Get second range name.
     * 
     * @return Name
     */
    private String getSecondRangeName() {
        return secondRangeName;
    } // end member function getSecondRangeName()



    /**
     * Check if range names match.
     * 
     * @param aRangeValue Range
     * @return True if OK
     * @throws ChannelListRangeException Bad range
     */
    private boolean rangeNamesMatch ( final String aRangeValue ) throws ChannelListRangeException {
        boolean returnValue = false;

        if ( rangeHasToken ( RANGE_TOKEN, aRangeValue ) ) {
            setFirstRangeName  ( aRangeValue );
            setSecondRangeName ( aRangeValue );
            if ( firstRangeName.equalsIgnoreCase ( secondRangeName ) ) {
                return true;
            } else {
                namesDoNotMatch = true;
                throw new ChannelListRangeException ( "Channel name values do not match: " + getFirstRangeName() + ", " + getSecondRangeName() );
            }
        }
        return returnValue;
    } // end member function rangeNamesMatch()






    /**
     * Return true if no errors.
     *
     * @return True if OK
     */
    private boolean noErrorsHappened() {
        return ( ! ( errorDuringParse || namesDoNotMatch || invalid1stRangeValue || invalid2ndRangeValue || rangeValuesNotOrdinal || errorOccured ) );
    } // end member function noErrorsHappened()





    /**
     * Check if range is valid.
     * 
     * @param aRangeValue Range
     * @return True if OK
     * @throws ChannelListRangeException Bad range
     */
    private boolean rangeIsValid ( final String aRangeValue ) throws ChannelListRangeException {
        parseRangeValue ( aRangeValue );
        return ( noErrorsHappened() && rangeNamesMatch ( aRangeValue ) );
    } // end member function rangeIsValid()




    /**
     * Check if range has leading zeroes.
     * 
     * @param aRangeOfChannels Range
     * @return True if so
     */
    private boolean channelRangeHasLeadingZeros ( final String aRangeOfChannels ) {
        return ( aRangeOfChannels.substring ( aRangeOfChannels.indexOf ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN ) + 1 ).startsWith ( "0" ) );
    } // end member function channelRangeHasLeadingZeros()


    
    /**
     * Get starting range of index.
     * 
     * @param aRangeOfChannels
     * @return Starting range
     * @throws ChannelListRangeException Bad range
     */
    private int channelRangeStartingValue ( final String aRangeOfChannels ) throws ChannelListRangeException {
        int firstRangeValue = 0;
        if ( rangeHasToken ( RANGE_TOKEN, aRangeOfChannels ) ) {
            // Then there is something to do.
            try {
                firstRangeNumber = ( aRangeOfChannels.substring ( aRangeOfChannels.indexOf ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN ) + RANGE_NAME_NUMBER_SEPERATOR_TOKEN.length(),
                                                aRangeOfChannels.indexOf ( RANGE_TOKEN ) )
                                              );
                firstRangeValue  = Integer.valueOf( firstRangeNumber ).intValue ();
            } catch ( NumberFormatException nfE ) {
                invalid1stRangeValue = true;
                throw new ChannelListRangeException ( "Invalid starting channel value number: " + aRangeOfChannels );
            }
        }
        return firstRangeValue;
    } // end member function channelRangeStartingValue()







    /**
     * Get ending range.
     * 
     * @param aRangeOfChannels
     * @return Ending range
     * @throws ChannelListRangeException Bad range
     */
    private int channelRangeEndingValue ( final String aRangeOfChannels ) throws ChannelListRangeException {
        int secondRangeValue = 0;
        if ( rangeHasToken ( RANGE_TOKEN, aRangeOfChannels ) ) {
            // Then there is something to do.
            try {
                secondRangeNumber = ( aRangeOfChannels.substring ( aRangeOfChannels.lastIndexOf ( RANGE_NAME_NUMBER_SEPERATOR_TOKEN ) + RANGE_NAME_NUMBER_SEPERATOR_TOKEN.length() ) );
                secondRangeValue  = Integer.valueOf( secondRangeNumber ).intValue();
            } catch ( NumberFormatException nfE ) {
                invalid2ndRangeValue = true;
                throw new ChannelListRangeException ( "Invalid ending channel value number: " + aRangeOfChannels );
            }
        }
        return secondRangeValue;
    } // end member function channelRangeEndingValue()






    /**
     * Get size of range.
     * 
     * @return Size of range
     */
    private int expandRangeCount() {
        int rangeCount = 0;

        int firstRangeValue  = getFirstRangeValue();
        int secondRangeValue = getSecondRangeValue();
        rangeCount = secondRangeValue - firstRangeValue + 1;

        return rangeCount;
    } // end member function expandRangeCount()







    private void initObjectMembers() {
        firstRangeName       = "";
        secondRangeName      = "";
        firstRangeNumber     = "";
        secondRangeNumber    = "";
        firstRangeValue      =  0;
        secondRangeValue     =  0;
        hasLeadingZeros      = false;
        errorDuringParse     = false;
        namesDoNotMatch      = false;
        invalid1stRangeValue = false;
        invalid2ndRangeValue = false;
        rangeValuesNotOrdinal= false;
        errorOccured         = false;
        channelList          = null;
    }








    /**
     * Generate channel list by expanding ranges.
     * 
     * @param aChannelSet Array of channel ids and ranges
     * @return Expanded ayy
     * @throws ChannelListRangeException Bad range found
     */
    public String[] genChannelListFromRange ( final String[] aChannelSet ) throws ChannelListRangeException {
        String[] aNewChannelSet = null;
        String aChannelRange = "";

        initObjectMembers();
        for ( int nxtChannelSetIndex = 0; nxtChannelSetIndex < aChannelSet.length; ++nxtChannelSetIndex ) {
            aChannelRange = aChannelSet [ nxtChannelSetIndex ];

            if ( isRange ( aChannelRange ) ) {
                if ( rangeIsValid ( aChannelRange ) ) {
                    // At this point all values have been parsed and if an exception happened it has already been thrown.
                    int startValue = getFirstRangeValue();
                    int endValue   = getSecondRangeValue();
                    String[] aChannelList = new String [ expandRangeCount() ];
                    String   aChannelName = getFirstRangeName();
                    for ( int nxtRangeValue = startValue; nxtRangeValue <= endValue; ++nxtRangeValue ) {
                        String newNumber = Integer.valueOf( nxtRangeValue ).toString().trim();
                        if ( hasLeadingZeros ) {
                            newNumber = ( "0000" + newNumber );
                            if ( SHORT_CHANNEL_NUMBER == firstRangeNumber.length() ) {
                                newNumber = newNumber.substring ( newNumber.length () - SHORT_CHANNEL_NUMBER );
                            } else {
                                newNumber = newNumber.substring ( newNumber.length () - ( SHORT_CHANNEL_NUMBER + 1 ) );
                            }
                            newNumber = newNumber.trim();
                        }
                        aChannelList [ nxtRangeValue - startValue ] = ( aChannelName + RANGE_NAME_NUMBER_SEPERATOR_TOKEN + newNumber );
                        if ( null == aNewChannelSet ) {
                            aNewChannelSet = new String [ 1 ];
                            aNewChannelSet [ 0 ] = aChannelList [ nxtRangeValue - startValue ];
                        } else {
                            int newChannelSize = aNewChannelSet.length;
                            String[] oldChannelSet = new String [ newChannelSize ];

                            for ( int i = 0; i < newChannelSize; ++i ) {
                                oldChannelSet [ i ] = ( aNewChannelSet [ i ] );
                            } // end for
                            aNewChannelSet = new String [ newChannelSize + 1 ];
                            for ( int j = 0; j < newChannelSize; ++j ) {
                                aNewChannelSet [ j ] = ( oldChannelSet [ j ] );
                            aNewChannelSet [ newChannelSize ] = aChannelList [ nxtRangeValue - startValue ];
                            } // end for
                        }
                    } // end for
                }
            } else {
                if ( null == aNewChannelSet ) {
                    aNewChannelSet = new String [ 1 ];
                    aNewChannelSet [ 0 ] = aChannelRange;
                } else {
                    int newChannelSize = aNewChannelSet.length;
                    String[] oldChannelSet = new String [ newChannelSize ];

                    for ( int i = 0; i < newChannelSize; ++i ) {
                        oldChannelSet [ i ] = ( aNewChannelSet [ i ] );
                    } // end for
                    aNewChannelSet = new String [ newChannelSize + 1 ];
                    for ( int j = 0; j < newChannelSize; ++j ) {
                        aNewChannelSet [ j ] = ( oldChannelSet [ j ] );
                    aNewChannelSet [ newChannelSize ] = aChannelRange;
                    } // end for
                }
            }
        } // end for

        // At this point aNewChannelSet should contain a complete channel expansion of the submitted channel range list.
        setChannelList ( aNewChannelSet );
        return aNewChannelSet;
    } // end member function genChannelListFromCommandLine()

}
