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
package jpl.gds.product.automation.hibernate.entity;

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * ProductAutomationLog entity.
 *  
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="logs")

public class ProductAutomationLog  implements java.io.Serializable, Comparable<ProductAutomationLog> {

	/**
	 * MPCS-4330 - 11/28/2012  - Adding a reference to a product for logs.
	 */
    // Fields    
	private static final long serialVersionUID = -8699005818249329063L;
	private Long logId;
     private String level;
     private String message;
     private String host;
     private Long processorId;
     private Long product;
     private Timestamp eventTime;


    // Constructors

    /** default constructor */
    public ProductAutomationLog() {
    }

    /** full constructor 
     * @param level the trace severity of this log
     * @param message the message contained in this log
     * @param host the name of the host that produced this message
     * @param processorId the processor ID that produced this message
     * @param product the product that this message is in relation to
     * @param eventTime the time this message was created
     * */
    public ProductAutomationLog(String level, String message, String host, Long processorId, Long product, Timestamp eventTime) {
        this.level = level;
        this.message = message;
        this.host = host;
        this.eventTime = eventTime;
        this.product = product;
        this.processorId = processorId;
    }

   
    // Property accessors
	/**
	 * Get the log ID for this object
	 * 
	 * @return the Long value that uniquely identifies this log message
	 */
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="logId", unique=true, nullable=false)
    public Long getLogId() {
        return this.logId;
    }
    
	/**
	 * Get the log ID for this object
	 * 
	 * @param logId
	 *            a Long value that will uniquely identifies this log message
	 */
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
	/**
	 * Get the trace severity of this log message
	 * 
	 * @return A String representing the severity of this log message (DEBUG,
	 *         INFO, WARN, etc)
	 */
    @Column(name="level", length=7, nullable=false)
    public String getLevel() {
        return this.level;
    }
    
	/**
	 * Set the trace severity of this log message
	 * 
	 * @param level
	 *            the severity of this log message (DEBUG, INFO, WARN, etc)
	 */
    public void setLevel(String level) {
        this.level = level;
    }
    
    /**
     * Get the message reported by this log message
     * 
     * @return the String message reported by this message
     */
    @Column(name="message", length=1536, nullable=false)
    public String getMessage() {
        return this.message;
    }
    
	/**
	 * Set the message reported by this log message
	 * 
	 * @param message
	 *            a String representing the message that is reported by this log
	 */
    public void setMessage(String message) {
        this.message = message;
    }
    
	/**
	 * Get the name of the host that generated this log message
	 * 
	 * @return the name of the host in String format
	 */
    @Column(name="host", length=256, nullable=false)
    public String getHost() {
        return this.host;
    }
    
	/**
	 * Set the host that generated this message
	 * 
	 * @param host
	 *            the name of the machine that generated this log message
	 */
    public void setHost(String host) {
        this.host = host;
    }
    
	/**
	 * The ID number of the processor thread that generated this log message
	 * 
	 * @return a long identifying a single processor thread
	 */
    @Column(name = "processorId")
    public Long getProcessorId() {
    		return this.processorId;
    }
    
    /**
     * Set the processor ID that created this message
     * 
     * @param processorId a long identifyhing a single processor thread
     */
    public void setProcessorId(Long processorId) {
    		this.processorId = processorId;
    }
    
    /**
     * Get the time that this log message was generated
     * 
     * @return a Timestamp showing when this log message was generated
     */
    @Column(name="eventTime", nullable=false, length=19)
    public Timestamp getEventTime() {
        return this.eventTime;
    }
    
	/**
	 * Set the time for this log message
	 * 
	 * @param eventTime
	 *            the Timestamp of when this log message was created
	 */
    public void setEventTime(Timestamp eventTime) {
        this.eventTime = eventTime;
    }
    
	/**
	 * Get the product this log message is for
	 * 
	 * @return the Long productId for the product in relation to this message
	 */
    @Column(name="product", nullable=true)
    public Long getProduct() {
    	return this.product;
    }
    
    /**
     * Set the product this log message is for
     * 
     * @param product the Long productId for the product in relation to this message
     */
    public void setProduct(Long product) {
    	this.product = product;
    }


    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((eventTime == null) ? 0 : eventTime.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((logId == null) ? 0 : logId.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result
				+ ((processorId == null) ? 0 : processorId.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ProductAutomationLog)) {
			return false;
		}
		ProductAutomationLog other = (ProductAutomationLog) obj;
		if (eventTime == null) {
			if (other.eventTime != null) {
				return false;
			}
		} else if (!eventTime.equals(other.eventTime)) {
			return false;
		}
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		if (level == null) {
			if (other.level != null) {
				return false;
			}
		} else if (!level.equals(other.level)) {
			return false;
		}
		if (logId == null) {
			if (other.logId != null) {
				return false;
			}
		} else if (!logId.equals(other.logId)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (processorId == null) {
			if (other.processorId != null) {
				return false;
			}
		} else if (!processorId.equals(other.processorId)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(ProductAutomationLog o) {
		return this.getLogId().compareTo(o.getLogId());
	}
}