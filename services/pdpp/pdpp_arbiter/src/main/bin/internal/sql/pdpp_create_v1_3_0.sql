-- MPCS-8179 06/09/16 - PORTED from MPCS for MSL to AMPCS.
--           Updated to match newer style
-- MPCS-8568 12/08/16 - Added fswVersion and fswDirectory to process
-- MPCS-8773 - Removed stats table.


DROP TABLE IF EXISTS action;
DROP TABLE IF EXISTS process;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS status;
DROP TABLE IF EXISTS logs;
DROP TABLE IF EXISTS classmaps;


system echo 'Creating action table' `date`

CREATE TABLE action(
   actionId      BIGINT     AUTO_INCREMENT,
   actionName    BIGINT     NOT NULL,
   process       BIGINT,
   product       BIGINT     NOT NULL,
   passNumber    BIGINT     NOT NULL,
   assignedTime  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
   acceptedTime  TIMESTAMP  NULL,
   completedTime TIMESTAMP  NULL,
   reassign      TINYINT(1) DEFAULT 0,
   versionId     BIGINT     NOT NULL,
   
   PRIMARY KEY (actionId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE UNIQUE INDEX ACTION_PRIMARY ON action(product,passNumber, actionName);



system echo 'Creating process table' `date`

CREATE TABLE process(
   processId        BIGINT     AUTO_INCREMENT,
   fswBuildId       BIGINT,
   fswVersion       VARCHAR(256),
   fswDirectory     VARCHAR(1024),
   action           BIGINT,
   processHost      VARCHAR(1024),
   pid              BIGINT,
   initializeTime   TIMESTAMP  NULL,
   startTime        TIMESTAMP  NULL,
   shutDownTime     TIMESTAMP  NULL,
   killer           ENUM('arbiter', 'process'),  
   pause            TINYINT(1) DEFAULT 0,
   pauseAck         TINYINT(1) DEFAULT 0,
   assignedActions  BIGINT     DEFAULT 0,
   completedActions BIGINT     DEFAULT 0,
   lastCompleteTime BIGINT     DEFAULT NULL,
   versionId        BIGINT     NOT NULL,
   
   PRIMARY KEY (processId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE UNIQUE INDEX PROCESS_PRIMARY on process(processId);



system echo 'Creating products table' `date`

CREATE TABLE products(
   productId          BIGINT AUTO_INCREMENT,
   productPath        VARCHAR(767)  NOT NULL UNIQUE,
   parent             BIGINT,
   fswBuildId         BIGINT        NOT NULL,
   dictVersion        VARCHAR(256)  NOT NULL,
   fswDirectory       VARCHAR(1024) NOT NULL,
   sessionId          BIGINT        NOT NULL,
   sessionHost        VARCHAR(1024) NOT NULL,
   apid               INT UNSIGNED  NOT NULL,
   vcid               INT UNSIGNED  NOT NULL,
   sclkCoarse         BIGINT        NOT NULL,
   sclkFine           BIGINT        NOT NULL,
   isCompressed       TINYINT(1)    NOT NULL,
   realTimeExtraction TINYINT(1)    NOT NULL,
   addTime            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NULL,
   
   PRIMARY KEY (productId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE UNIQUE INDEX PRODUCTS_PRIMARY on products(productId);



system echo 'Creating status table' `date`

CREATE TABLE status(
   statusId   BIGINT    AUTO_INCREMENT,
   statusName ENUM('uncategorized', 'categorized', 'started', 'completed', 'failed', 'ignored', 'timedout', 'reassigned', 'completed_pre', 'unknown_complete') NOT NULL,
   product    BIGINT    NOT NULL,
   passNumber BIGINT    NOT NULL,
   statusTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   
   PRIMARY KEY (statusId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE UNIQUE INDEX STATUS_PRIMARY on status(statusId);
CREATE index STATUS_PRODUCT on status(product);



system echo 'Creating logs table' `date`

CREATE TABLE 	logs (
	logId       BIGINT        AUTO_INCREMENT,
	level       ENUM('FATAL', 'ERROR', 'WARN', 'USER', 'INFO', 'DEBUG', 'TRACE', 'UNKNOWN') NOT NULL,
	message     VARCHAR(1536) NOT NULL,
	host        VARCHAR(256)  NOT NULL,
	processorId BIGINT,
	product     BIGINT,
	eventTime   TIMESTAMP     NOT NULL,
	PRIMARY KEY (logId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;



system echo 'Creating classmaps table' `date`

CREATE TABLE classmaps(
	classId   BIGINT       AUTO_INCREMENT,
	mnemonic  VARCHAR(8)   NOT NULL,
	className VARCHAR(512) NOT NULL,
	enabled   TINYINT(1)   NOT NULL,
	PRIMARY KEY (classId)
)ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE UNIQUE INDEX CLASS_PRIMARY on classmaps(mnemonic, className);

-- Insert to add Generic processor classes
INSERT INTO classmaps (mnemonic, className, enabled) VALUES ('logger', 'jpl.gds.product.processors.ConsoleLoggerProcessor', 1);
