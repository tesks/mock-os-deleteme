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
package jpl.gds.shared.types;


/**
 * The FormatType class is...
 *
 */
public enum FormatType
{
     /** Character format */
     CHARACTER,

     /** Signed integer format */
     SIGNED_INT,

     /** Unsigned integer format */
     UNSIGNED_INT,

     /** Float format */
     FLOAT,

     /** Float as exponential format */
     EXPONENTIAL,

     /** Float-either-way format */
     FLOAT_OR_EXPONENTIAL,

     /** String format */
     STRING;

     /**
      * Get FormatType corresponding to char.
      *
      * @param c Type character
      *
      * @return Format type
      */     
     public static FormatType createFromTypeChar(char c) {
         switch (c) {
             case 'd': return FormatType.SIGNED_INT;
             case 'i': return FormatType.SIGNED_INT;
             case 'f': return FormatType.FLOAT;
             case 'F': return FormatType.FLOAT;
             case 'e': return FormatType.EXPONENTIAL;
             case 'E': return FormatType.EXPONENTIAL;
             case 'g': return FormatType.FLOAT_OR_EXPONENTIAL;
             case 'G': return FormatType.FLOAT_OR_EXPONENTIAL;
             case 'x': return FormatType.UNSIGNED_INT;
             case 'X': return FormatType.UNSIGNED_INT;
             case 'o': return FormatType.UNSIGNED_INT;
             case 'u': return FormatType.UNSIGNED_INT;
             case 'c': return FormatType.CHARACTER;
             case 's': return FormatType.STRING;
             default:
                 throw new IllegalArgumentException("Unrecognized formatter " + c);
         }
     }
     

     /**
      * Get is-signed state.
      *
      * @return Is-signed state
      */  
     public boolean isSigned() {
         switch(this) {
             case SIGNED_INT:
             case FLOAT:
             case EXPONENTIAL:
             case FLOAT_OR_EXPONENTIAL: return true;
             default: return false;      
         }
     }
     

     /**
      * Get is-integer state.
      *
      * @return Is-integer state
      */  
     public boolean isInteger() {
         return this == FormatType.SIGNED_INT || this == FormatType.UNSIGNED_INT;
     }


     /**
      * Get is-float state.
      *
      * @return Is-float state
      */  
     public boolean isFloat() {
         return this == FormatType.FLOAT || this == FormatType.EXPONENTIAL ||
         this == FormatType.FLOAT_OR_EXPONENTIAL;
     }     
}
