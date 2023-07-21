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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import jpl.gds.shared.time.AccurateDateTime;


/**
 * Entity class for the product automation process table.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 * MPCS-8568 - 12/12/16 - Added fswVersion and fswDirectory properties. Added get and set methods
 */
@Entity
@Table(name = "process")

public class ProductAutomationProcess implements java.io.Serializable,
	Comparable<ProductAutomationProcess> {

	// Fields

	/**
	 * MPCS-6544  9/2014 - Added member variables for the new database fields as well as all required
	 * setters and getters. 
	 */
	private static final long serialVersionUID = 7548649403209072835L;
	private Long processId;
	private Long fswBuildId;
	private String fswVersion;
	private String fswDirectory;
	private ProductAutomationClassMap classMapping;
	private String processHost;
	private Long pid;
	private Timestamp initializeTime;
	private Timestamp startTime;
	private Timestamp shutDownTime;
	private String killer;
	private int pause;
	private int pauseAck;
	
	private Long assignedActions = new Long(0);
	private Long completedActions = new Long(0);
	private Long lastCompleteTime = new Long(0);
	
	private Long versionId;

	// Constructors

	/** default constructor */
	public ProductAutomationProcess() {
	}

	/**
	 * minimal constructor 
	 * @param versionId the version ID
	 */
	public ProductAutomationProcess(Long versionId) {
		this.versionId = versionId;
	}

	/**
	 * full constructor 
	 * @param fswBuildId the flight software build ID for the dictionary to be used with this process
	 * @param fswVersion the flight software version for the dictionary to be used with this process
	 * @param fswDirectory the directory of the flight software dictionary to be used with this process
	 * @param classMapping a ProductAutomationClassMap for associating an action mnemonic with a classname 
	 * @param processHost the name of the host that executes this process
	 * @param pid the process id on the host associated with this process. Not the same as processId
	 * @param initializeTime the time this process is initialized
	 * @param startTime the time this process is started
	 * @param shutDownTime the time this process is shut down
	 * @param killer "arbiter" or "process", depending on whether the arbiter or the process itself killed this process
	 * @param pause 1 if paused, 0 if not
	 * @param pauseAck 1 if this process acknowledges when it is paused, 0 if not. 
	 * @param assignedActions the number of actions assigned to this process
	 * @param completedActions the number of completed actions
	 * @param lastCompleteTime the time when the most recent action was completed
	 * @param versionId the version ID
	 * */
	public ProductAutomationProcess(Long fswBuildId, String fswVersion, String fswDirectory, ProductAutomationClassMap classMapping,
			String processHost, Long pid, Timestamp initializeTime,
			Timestamp startTime, Timestamp shutDownTime, String killer, int pause, int pauseAck,
			Long assignedActions, Long completedActions, Long lastCompleteTime,
			Long versionId) {
		this.fswBuildId = fswBuildId;
		this.fswVersion = fswVersion;
		this.fswDirectory = fswDirectory;
		this.classMapping = classMapping;
		this.processHost = processHost;
		this.pid = pid;
		this.initializeTime = initializeTime;
		this.startTime = startTime;
		this.shutDownTime = shutDownTime;
		this.killer = killer;
		this.pause = pause;
		this.pauseAck = pauseAck;
		
		this.assignedActions = assignedActions;
		this.completedActions = completedActions;
		this.lastCompleteTime = lastCompleteTime;
		
		this.versionId = versionId;
	}

	// Property accessors
	/**
	 * Get the process ID for this object
	 * 
	 * @return the Long value that uniquely identifies this process
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "processId", unique = true, nullable = false)
	public Long getProcessId() {
		return this.processId;
	}

	/**
	 * Set the process ID for this object
	 * 
	 * @param processId a Long value that will uniquely identify this process
	 */
	public void setProcessId(Long processId) {
		this.processId = processId;
	}

	/**
	 * Get the flight software build ID of the dictionary associated with this
	 * process
	 * 
	 * @return a Long representing the flight software build ID
	 */
	@Column(name = "fswBuildId")
	public Long getFswBuildId() {
		return this.fswBuildId;
	}


	/**
	 * Set the flight software build ID
	 * 
	 * @param fswBuildId
	 *            the Long flight software build ID for a dictionary that is to
	 *            be associated with this process
	 */
	public void setFswBuildId(Long fswBuildId) {
		this.fswBuildId = fswBuildId;
	}

	/**
	 * Get the flight software relase version
	 * 
	 * @return a String representing the flight software releaseversion
	 */
	@Column(name = "fswVersion", length = 256)
	public String getFswVersion(){
		return this.fswVersion;
	}
	
	/**
	 * Set the flight software dictionary version
	 * 
	 * @param fswVersion
	 *            the flight software dictionary version to be associated with
	 *            this process. The flight software dictionary version is the
	 *            version associated with a combined flight software release and
	 *            ground dictionary release.
	 */
	public void setFswVersion(String fswVersion){
		this.fswVersion = fswVersion;
	}
	
	/**
	 * Get the directory to the flight software dictionary
	 * 
	 * @return the directory path to the flight software dictionary
	 */
	@Column(name = "fswDirectory", length = 1024)
	public String getFswDirectory(){
		return this.fswDirectory;
	}
	
	/**
	 * Set the directory path to the flight software dictionary being used. This
	 * path does not include the mission directory or the directory directly
	 * containing the dictionary files
	 * 
	 * @param fswDirectory
	 *            the directory path to the flight software dictionary
	 */
	public void setFswDirectory(String fswDirectory){
		this.fswDirectory = fswDirectory;
	}
	
	/**
	 * Get the classmap for this process. The classmap has the
	 * action mnemonic and associated class name
	 * 
	 * @return a ProductAutomationClassMap for this process
	 */
	@ManyToOne
	@JoinColumn(name = "action")
	public ProductAutomationClassMap getAction() {
		return this.classMapping;
	}

	/**
	 * Set the classmap for this process.
	 * 
	 * @param action
	 *            the ProductAutomationClassMap to associate this process with a
	 *            specific class that performs a certain type of processing
	 *            action
	 */
	public void setAction(ProductAutomationClassMap action) {
		this.classMapping = action;
	}

	/**
	 * Get the host that runs this process
	 * 
	 * @return the String name of the host of the process
	 */
	@Column(name = "processHost", length = 1024)
	public String getProcessHost() {
		return this.processHost;
	}

	/**
	 * Set the host of this process.
	 * 
	 * @param processHost A string host/machine name
	 */
	public void setProcessHost(String processHost) {
		this.processHost = processHost;
	}

	/**
	 * Get the host's process id for the process executing this process
	 * 
	 * @return the Long representing the active process id on the host running
	 *         this proces
	 */
	@Column(name = "pid")
	public Long getPid() {
		return this.pid;
	}

	/**
	 * Set the machine process id. This value should be the active process id
	 * where this process is running
	 * 
	 * @param pid
	 *            the host process id
	 */
	public void setPid(Long pid) {
		this.pid = pid;
	}

	/**
	 * Get the time when the process was initialized
	 * 
	 * @return a Timestamp indicating when this process was initialized
	 */
	@Column(name = "initializeTime", length = 19)
	public Timestamp getInitializeTime() {
		return this.initializeTime;
	}

	/**
	 * Set the time when this process was initialized
	 * 
	 * @param initializeTime a Timestamp when this process is initialized
	 */
	public void setInitializeTime(Timestamp initializeTime) {
		this.initializeTime = initializeTime;
	}

	/**
	 * Get the time this process was started
	 * 
	 * @return a Timestamp indicating when this process was started
	 */
	@Column(name = "startTime", length = 19)
	public Timestamp getStartTime() {
		return this.startTime;
	}

	/**
	 * set the time this process was started. This time should be no earlier
	 * than the time in getInitializedTime()
	 * 
	 * @param startTime
	 *            a Timestamp when this process is started.
	 */
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	/**
	 * Get the time this process was shut down.
	 * 
	 * @return a Timestamp indicating when this process was shut down
	 */
	@Column(name = "shutDownTime", length = 19)
	public Timestamp getShutDownTime() {
		return this.shutDownTime;
	}

	/**
	 * Set the shut down time. This time should be no earlier than the time in
	 * getStartTime()
	 * 
	 * @param shutDownTime
	 *            a Timestamp when this process was shut down
	 */
	public void setShutDownTime(Timestamp shutDownTime) {
		this.shutDownTime = shutDownTime;
	}

	/**
	 * Get the String value of the killer process type.
	 * 
	 * @return "arbiter" if killed by the arbiter, "process" if self terminated
	 */
	@Column(name = "killer", length = 8)
	public String getKiller() {
		return this.killer;
	}

	/**
	 * Set the killer process type
	 * 
	 * @param killer
	 *            "arbiter" if the process was killed by the arbiter, "process"
	 *            if self terminated
	 */
	public void setKiller(String killer) {
		this.killer = killer;
	}

	/**
	 * Get the pause value
	 * @return 1 if paused, 0 if not
	 */
	@Column(name = "pause")
	public int getPause() {
		return this.pause;
	}
	
	/**
	 * Set the pause value
	 * 
	 * @param pause 1 if paused, 0 if not
	 */
	public void setPause(int pause) {
		this.pause = pause;
	}

	/**
	 * Get the pause acknowledge value
	 * 
	 * @return 1 if acknowledging the process is paused, 0 if not
	 */
	@Column(name = "pauseAck")
	public int getPauseAck() {
		return this.pauseAck;
	}
	
	/**
	 * Set the pause acknowledge value
	 * 
	 * @param pauseAck
	 *            1 if acknowledging the process is paused, 0 if not
	 */
	public void setPauseAck(int pauseAck) {
		this.pauseAck = pauseAck;
	}
	
	/**
	 * Get when an action was most recently completed
	 * 
	 * @return a Long for the Unix time when an action was most recently
	 *         completed
	 */
	@Column(name = "lastCompleteTime", nullable = true)
	public Long getLastCompleteTime() {
		return this.lastCompleteTime;
	}
	
	/**
	 * Set when an action was most recently completed
	 * 
	 * @param lastCompleteTime a Long for the Unix time when an action was most recently
	 *         completed
	 */
	public void setLastCompleteTime(Long lastCompleteTime) {
		this.lastCompleteTime = lastCompleteTime;
	}

	/**
	 * Get when an action was most recently completed as a date and time String
	 * in the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
	 * 
	 * @return A date and time String for the time when an action was last completed
	 */
	@Transient
	public String getLastCompleteTimeStr() {
		if (lastCompleteTime > 0) {
			return new AccurateDateTime(getLastCompleteTime(), 0L).getFormattedErt(true);
		} else {
			return "";
		}
	}
	
	/**
	 * Get the version ID for this process
	 * 
	 * @return a Long for the version ID of this process
	 */
	@Version
	@Column(name = "versionId", nullable = false)
	public Long getVersionId() {
		return this.versionId;
	}

	/**
	 * Set the version ID
	 * 
	 * @param versionId a Long for the version ID of this process
	 */
	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	/**
	 * Get the number of actions assigned to this process. These are actions
	 * that have not been processed
	 * 
	 * @return Long value for the number of actions that have been assigned
	 */
	@Column(name = "assignedActions")
	public Long getAssignedActions() {
		return assignedActions;
	}

	/**
	 * Set the value representing the number of actions assigned to this process
	 * 
	 * @param assignedActions
	 *            a Long value for the number of actions that have been assigned
	 *            to this process
	 */
	public void setAssignedActions(Long assignedActions) {
		this.assignedActions = assignedActions;
	}

	/**
	 * Get the number of actions completed by this process.
	 * 
	 * @return Long value for the number of actions that have been completed
	 */
	@Column(name = "completedActions")
	public Long getCompletedActions() {
		return completedActions;
	}

	/**
	 * Set the value representing the number of actions that have been completed
	 * by this process
	 * 
	 * @param completedActions
	 *            a Long value for the number of actions that have been
	 *            completed by this process
	 */
	public void setCompletedActions(Long completedActions) {
		this.completedActions = completedActions;
	}

	/**
	 * Adjusts the assigned and completed actions.  If completedAdjust is greater than 0, this will update the 
	 * last complete time as well with the current timestamp.
	 * 
	 * @param assignedAdjust assignedActions is adjusted by this value
	 * @param completedAdjust completedActions is adjusted by this value
	 */
	@Transient
	public void adjust(int assignedAdjust, int completedAdjust) {
		assignedActions += assignedAdjust;
		completedActions += completedAdjust;

		if (completedAdjust > 0) {
			this.setLastCompleteTime(System.currentTimeMillis());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		result = prime * result + ((processId == null) ? 0 : processId.hashCode());

		result = prime * result + ((classMapping == null) ? 0 : classMapping.hashCode());
		result = prime * result
				+ ((fswBuildId == null) ? 0 : fswBuildId.hashCode());
		result = prime * result
				+ ((initializeTime == null) ? 0 : initializeTime.hashCode());
		result = prime * result + ((killer == null) ? 0 : killer.hashCode());
		result = prime * result + ((pid == null) ? 0 : pid.hashCode());
		result = prime * result
				+ ((processHost == null) ? 0 : processHost.hashCode());
		result = prime * result
				+ ((shutDownTime == null) ? 0 : shutDownTime.hashCode());
		result = prime * result
				+ ((startTime == null) ? 0 : startTime.hashCode());
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
		if (!(obj instanceof ProductAutomationProcess)) {
			return false;
		}
		ProductAutomationProcess other = (ProductAutomationProcess) obj;

		if (this.processId == null) {
			if (other.processId != null) {
				return false;
			}
		} else if (!processId.equals(other.processId)) {
			return false;
		}
		if (classMapping == null) {
			if (other.classMapping != null) {
				return false;
			}
		} else if (!classMapping.equals(other.classMapping)) {
			return false;
		}
		if (fswBuildId == null) {
			if (other.fswBuildId != null) {
				return false;
			}
		} else if (!fswBuildId.equals(other.fswBuildId)) {
			return false;
		}
		if (initializeTime == null) {
			if (other.initializeTime != null) {
				return false;
			}
		} else if (!initializeTime.equals(other.initializeTime)) {
			return false;
		}
		if (killer == null) {
			if (other.killer != null) {
				return false;
			}
		} else if (!killer.equals(other.killer)) {
			return false;
		}
		if (pid == null) {
			if (other.pid != null) {
				return false;
			}
		} else if (!pid.equals(other.pid)) {
			return false;
		}
		if (processHost == null) {
			if (other.processHost != null) {
				return false;
			}
		} else if (!processHost.equals(other.processHost)) {
			return false;
		}
		if (shutDownTime == null) {
			if (other.shutDownTime != null) {
				return false;
			}
		} else if (!shutDownTime.equals(other.shutDownTime)) {
			return false;
		}
		if (startTime == null) {
			if (other.startTime != null) {
				return false;
			}
		} else if (!startTime.equals(other.startTime)) {
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return "ProductAutomationProcess [processId=" + processId
				+ ", fswBuildId=" + fswBuildId + ", classMapping="
				+ classMapping + ", processHost=" + processHost + ", pid="
				+ pid + ", initializeTime=" + initializeTime + ", startTime="
				+ startTime + ", shutDownTime=" + shutDownTime + ", killer="
				+ killer + ", pause=" + pause + ", pauseAck=" + pauseAck
				+ ", assignedActions=" + assignedActions
				+ ", completedActions=" + completedActions + ", versionId="
				+ versionId + "]";
	}

	@Override
	public int compareTo(ProductAutomationProcess o) {
		if (o.getStartTime() == null && this.getStartTime() == null) {
			return 0;
		} else if (this.getStartTime() == null) {
			return -1;
		} else if (o.getStartTime() == null) {
			return 1;
		} else {
			/**
			 * MPCS-6671  - this was causing issues with sorted sets because some processes
			 * had the same start time.  Instead so use the process id to help with the decision.
			 */
			int cmp = this.getStartTime().compareTo(o.getStartTime());
			return cmp == 0 ? getProcessId().compareTo(o.getProcessId()) : cmp;
		}
	}
}