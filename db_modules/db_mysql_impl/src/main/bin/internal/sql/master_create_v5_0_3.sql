-- Master create. Note Product may be incomplete. See mission addendum.
-- MPCS-10244 : All tables are now InnoDB

-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
system echo 'Dropping existing tables' `date`

NOWARNING;

DROP TABLE IF EXISTS Host;

DROP TABLE IF EXISTS Session;

DROP TABLE IF EXISTS EndSession;

DROP TABLE IF EXISTS Frame;

DROP TABLE IF EXISTS FrameBody;

DROP TABLE IF EXISTS Packet;

DROP TABLE IF EXISTS PacketBody;

DROP TABLE IF EXISTS SsePacket;

DROP TABLE IF EXISTS SsePacketBody;

DROP TABLE IF EXISTS Evr;

DROP TABLE IF EXISTS EvrMetadata;

DROP TABLE IF EXISTS SseEvr;

DROP TABLE IF EXISTS SseEvrMetadata;

DROP TABLE IF EXISTS ChannelData;

DROP TABLE IF EXISTS LogMessage;

DROP TABLE IF EXISTS CommandMessage;

DROP TABLE IF EXISTS CommandStatus;

DROP TABLE IF EXISTS Product;

DROP TABLE IF EXISTS CfdpIndication;

DROP TABLE IF EXISTS CfdpFileGeneration;

DROP TABLE IF EXISTS CfdpFileUplinkFinished;

DROP TABLE IF EXISTS CfdpRequestReceived;

DROP TABLE IF EXISTS CfdpRequestResult;

DROP TABLE IF EXISTS CfdpPduReceived;

DROP TABLE IF EXISTS CfdpPduSent;

DROP TABLE IF EXISTS ContextConfig;

DROP TABLE IF EXISTS ContextConfigKeyValue;

-- MPCS-9105 - Create extended SCET tables by default
DROP TABLE IF EXISTS Evr2;
DROP TABLE IF EXISTS Packet2;
DROP TABLE IF EXISTS Product2;
DROP TABLE IF EXISTS SseEvr2;
DROP TABLE IF EXISTS SsePacket2;

DROP TABLE IF EXISTS ChannelAggregate;
DROP TABLE IF EXISTS HeaderChannelAggregate;
DROP TABLE IF EXISTS MonitorChannelAggregate;
DROP TABLE IF EXISTS SseChannelAggregate;

WARNINGS;

-- hostOffset is constant. It is a value 0-255 that creates the range for
-- hostId's. There are two bytes available for hosts on any given DB, giving us
-- 65536 possible hosts. The hostOffset makes up the top byte.

system echo 'Creating Host table' `date`

CREATE TABLE Host
(
  hostId     MEDIUMINT UNSIGNED NOT NULL,
  hostName   VARCHAR(64)        NOT NULL,
  hostOffset TINYINT UNSIGNED   NOT NULL,

  PRIMARY KEY(hostId),
  KEY hostNameIndex(hostName)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating Session table' `date`

CREATE TABLE Session
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,

  name VARCHAR(64) NOT NULL,
  type VARCHAR(64) NULL,
  description VARCHAR(255) NULL,
  fullName VARCHAR(255) NOT NULL,
  user VARCHAR(32) NOT NULL,
  host VARCHAR(64) NOT NULL,
  outputDirectory  VARCHAR(1024) NOT NULL,
  outputDirectoryOverride TINYINT UNSIGNED NOT NULL,
  fswDictionaryDir VARCHAR(1024) NULL,
  sseDictionaryDir VARCHAR(1024) NULL,
  sseVersion VARCHAR(64) NULL,
  fswVersion VARCHAR(64) NULL,
  venueType ENUM('UNKNOWN',
                 'TESTSET',
                 'TESTBED',
                 'ATLO',
                 'OPS',
                 'CRUISE',
                 'SURFACE',
                 'ORBIT') NOT NULL,
  testbedName  VARCHAR(32) NULL,
  rawInputType VARCHAR(64) NULL,
  startTime        DATETIME NOT NULL,
  startTimeCoarse  INT      UNSIGNED NOT NULL,
  startTimeFine    SMALLINT UNSIGNED NOT NULL,
  spacecraftId     SMALLINT UNSIGNED NOT NULL,
  downlinkStreamId VARCHAR(16) NOT NULL,
  mpcsVersion      VARCHAR(16) NOT NULL,
  fswDownlinkHost  VARCHAR(64) NULL,
  fswUplinkHost    VARCHAR(64) NULL,
  fswUplinkPort    SMALLINT UNSIGNED NULL,
  fswDownlinkPort  SMALLINT UNSIGNED NULL,
  sseHost VARCHAR(64) NULL,
  sseUplinkPort SMALLINT UNSIGNED NULL,
  sseDownlinkPort SMALLINT UNSIGNED NULL,
  inputFile VARCHAR(1024) NULL,
  downlinkConnectionType VARCHAR(32) NULL,
  uplinkConnectionType   VARCHAR(32) NULL,
  topic    VARCHAR(128) NULL,
  subtopic VARCHAR(128) NULL,
  dssId    SMALLINT UNSIGNED NOT NULL,
  vcid     INT      UNSIGNED NULL,
  fswDownlinkFlag   TINYINT UNSIGNED NOT NULL,
  sseDownlinkFlag   TINYINT UNSIGNED NOT NULL,
  uplinkFlag        TINYINT UNSIGNED NOT NULL,
  databaseSessionId MEDIUMINT UNSIGNED NULL,
  databaseHost      VARCHAR(64) NULL,

  PRIMARY KEY(hostId, sessionId, sessionFragment),
  KEY        idIndex(sessionId, sessionFragment),
  UNIQUE KEY fullNameIndex(fullName),
  KEY        hostIndex(host),
  KEY        nameIndex(name),
  KEY        startTimeIndex(startTimeCoarse, startTimeFine),
  KEY        userIndex(user)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating EndSession table' `date`

CREATE TABLE EndSession
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,

  endTime       DATETIME NOT NULL,
  endTimeCoarse INT      UNSIGNED NOT NULL,
  endTimeFine   SMALLINT UNSIGNED NOT NULL,

  PRIMARY KEY (hostId, sessionId, sessionFragment)
) ENGINE=InnoDB CHARSET=latin1 ROW_FORMAT=COMPACT;


system echo 'Inserting Host information' `date`

CALL populateHost(@offset);

system echo 'Creating Frame table' `date`

CREATE TABLE Frame
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  type VARCHAR(32) NOT NULL,
  rctCoarse INT      UNSIGNED NOT NULL,
  rctFine   SMALLINT UNSIGNED NOT NULL,
  ertCoarse INT UNSIGNED NOT NULL,
  ertFine   INT UNSIGNED NOT NULL,
  relaySpacecraftId SMALLINT UNSIGNED NULL,
  dssId SMALLINT UNSIGNED NOT NULL,
  vcid  INT      UNSIGNED NOT NULL,
  vcfc  BIGINT   UNSIGNED NOT NULL,
  bitRate FLOAT  UNSIGNED NULL,
  badReason ENUM('UNKNOWN',
                 'RS_ERROR',
                 'CRC_ERROR',
                 'BAD_VERSION',
                 'BAD_SCID',
                 'BAD_HEADER',
                 'BAD_VCID',
                 'BAD_PKT_POINTER',
                 'TURBO_ERROR',
                 'UNKNOWN_VCID') NULL,
  fillFrame     TINYINT  UNSIGNED NOT NULL,
  bodyLength    SMALLINT UNSIGNED NOT NULL,
  headerLength  SMALLINT UNSIGNED NULL,
  trailerLength TINYINT  UNSIGNED NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexFrame();



system echo 'Creating FrameBody table' `date`

CREATE TABLE FrameBody
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  body            VARBINARY(7000) NOT NULL,
  header          VARBINARY(1024) NULL,
  trailer         VARBINARY(255)  NULL,
  PRIMARY KEY(hostId, sessionId, sessionFragment, id)
) ENGINE=MyISAM CHARSET=latin1;



system echo 'Creating Packet table' `date`

CREATE TABLE Packet
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  frameId         BIGINT    UNSIGNED NULL,
  rctCoarse     INT      UNSIGNED NOT NULL,
  rctFine       SMALLINT UNSIGNED NOT NULL,
  scetCoarse    INT      UNSIGNED NOT NULL,
  scetFine      SMALLINT UNSIGNED NOT NULL,
  ertCoarse     INT UNSIGNED NOT NULL,
  ertFine       INT UNSIGNED NOT NULL,
  sclkCoarse    INT UNSIGNED NOT NULL,
  sclkFine      INT UNSIGNED NOT NULL,
  dssId         SMALLINT  UNSIGNED NOT NULL,
  vcid          INT       UNSIGNED NULL,
  sourceVcfc    BIGINT    UNSIGNED NULL,
  apid          INT       UNSIGNED NOT NULL,
  spsc          INT       UNSIGNED NOT NULL,
  apidName      VARCHAR(64) NULL,
  badReason     TINYINT   UNSIGNED NULL,
  fillFlag      TINYINT   UNSIGNED NOT NULL,
  bodyLength    MEDIUMINT UNSIGNED NOT NULL,
  headerLength  SMALLINT  UNSIGNED NULL,
  trailerLength TINYINT   UNSIGNED NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexPacket();



system echo 'Creating PacketBody table' `date`

CREATE TABLE PacketBody
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  body            MEDIUMBLOB NOT NULL,
  header          VARBINARY(1024) NULL,
  trailer         VARBINARY(255)  NULL,
  PRIMARY KEY(hostId, sessionId, sessionFragment, id)
) ENGINE=MyISAM CHARSET=latin1;



system echo 'Creating SsePacket table' `date`

CREATE TABLE SsePacket
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  rctCoarse  INT      UNSIGNED NOT NULL,
  rctFine    SMALLINT UNSIGNED NOT NULL,
  scetCoarse INT      UNSIGNED NOT NULL,
  scetFine   SMALLINT UNSIGNED NOT NULL,
  ertCoarse  INT UNSIGNED NOT NULL,
  ertFine    INT UNSIGNED NOT NULL,
  sclkCoarse INT UNSIGNED NOT NULL,
  sclkFine   INT UNSIGNED NOT NULL,
  apid INT UNSIGNED NOT NULL,
  spsc INT UNSIGNED NOT NULL,
  apidName VARCHAR(64) NULL,
  badReason     TINYINT   UNSIGNED NULL,
  bodyLength    MEDIUMINT UNSIGNED NOT NULL,
  headerLength  SMALLINT  UNSIGNED NULL,
  trailerLength TINYINT   UNSIGNED NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexSsePacket();



system echo 'Creating SsePacketBody table' `date`

CREATE TABLE SsePacketBody
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  body            MEDIUMBLOB NOT  NULL,
  header          VARBINARY(1024) NULL,
  trailer         VARBINARY(255)  NULL,
  PRIMARY KEY(hostId, sessionId, sessionFragment, id)
) ENGINE=MyISAM CHARSET=latin1;



system echo 'Creating Evr table' `date`

-- MPCS-7917 - Increase message size to 2048 from 400

CREATE TABLE Evr
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  packetId        BIGINT    UNSIGNED NULL,
  name        VARCHAR(128) NULL,
  eventId     INT UNSIGNED NOT NULL,
  rctCoarse   INT      UNSIGNED NOT NULL,
  rctFine     SMALLINT UNSIGNED NOT NULL,
  ertCoarse   INT UNSIGNED NOT NULL,
  ertFine     INT UNSIGNED NOT NULL,
  scetCoarse  INT      UNSIGNED NOT NULL,
  scetFine    SMALLINT UNSIGNED NOT NULL,
  sclkCoarse  INT UNSIGNED NOT NULL,
  sclkFine    INT UNSIGNED NOT NULL,
  dssId       SMALLINT UNSIGNED NOT NULL,
  vcid        INT      UNSIGNED NULL,
  level       VARCHAR(16)   NOT NULL,
  module      VARCHAR(32)   NULL,
  message     VARCHAR(2048) NOT NULL,
  isRealtime  TINYINT UNSIGNED NOT NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexEvr();



system echo 'Creating SseEvr table' `date`

-- MPCS-7917 - Increase message size to 2048 from 400

CREATE TABLE SseEvr
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  packetId        BIGINT    UNSIGNED NULL,
  name        VARCHAR(128)       NULL,
  eventId     INT       UNSIGNED NOT NULL,
  rctCoarse   INT       UNSIGNED NOT NULL,
  rctFine     SMALLINT  UNSIGNED NOT NULL,
  ertCoarse   INT       UNSIGNED NOT NULL,
  ertFine     INT       UNSIGNED NOT NULL,
  scetCoarse  INT       UNSIGNED NOT NULL,
  scetFine    SMALLINT  UNSIGNED NOT NULL,
  sclkCoarse  INT       UNSIGNED NOT NULL,
  sclkFine    INT       UNSIGNED NOT NULL,
  level       VARCHAR(16)        NOT NULL,
  module      VARCHAR(32)        NULL,
  message     VARCHAR(2048)      NOT NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexSseEvr();



system echo 'Creating EvrMetadata table' `date`

-- MPCS-7917 - Increase value size to 400 from 219

CREATE TABLE EvrMetadata
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  keyword         ENUM('UNKNOWN',
                       'TaskName',
                       'SequenceId',
                       'CategorySequenceId',
                       'AddressStack',
                       'Source',
                       'TaskId',
                       'errno') NOT NULL,
  value           VARCHAR(400)  NOT NULL,
  KEY comboIndex(hostId, sessionId, sessionFragment, id)
) ENGINE=MyISAM CHARSET=latin1;



system echo 'Creating SseEvrMetadata table' `date`

-- MPCS-7917 - Increase value size to 400 from 219

CREATE TABLE SseEvrMetadata
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              BIGINT    UNSIGNED NOT NULL,
  keyword         ENUM('UNKNOWN',
                       'TaskName',
                       'SequenceId',
                       'CategorySequenceId',
                       'AddressStack',
                       'Source',
                       'TaskId',
                       'errno') NOT NULL,
  value           VARCHAR(400)  NOT NULL,
  KEY comboIndex(hostId, sessionId, sessionFragment, id)
) ENGINE=MyISAM CHARSET=latin1;



system echo 'Creating ChannelData table' `date`
-- MPCS-7917 -  Add TIME to ChannelData.type

CREATE TABLE ChannelData
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  id              INT       UNSIGNED NOT NULL,
  channelId       VARCHAR(9)         NOT NULL,
  fromSse         TINYINT   UNSIGNED NOT NULL,

  type ENUM('UNKNOWN','SIGNED_INT','UNSIGNED_INT','DIGITAL','STATUS',
            'FLOAT','BOOLEAN','ASCII','TIME') NOT NULL,

  channelIndex SMALLINT UNSIGNED NULL,
  module       VARCHAR(32)       NULL,
  name         VARCHAR(64)       NULL,
  dnFormat     VARCHAR(16)       NULL,
  euFormat     VARCHAR(16)       NULL,

  PRIMARY KEY(hostId, sessionId, sessionFragment, fromSse, id),
  KEY channelIdIndex(hostId, sessionId, sessionFragment, channelId),
  KEY moduleIndex(hostId, sessionId, sessionFragment, module)
) ENGINE=InnoDB CHARSET=latin1 ROW_FORMAT=COMPACT;


system echo 'Creating LogMessage table' `date`

CREATE TABLE LogMessage
(
  sessionId       MEDIUMINT UNSIGNED DEFAULT NULL,
  hostId          MEDIUMINT UNSIGNED DEFAULT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  rctCoarse       INT       UNSIGNED NOT NULL,
  rctFine         SMALLINT  UNSIGNED NOT NULL,
  eventTimeCoarse INT       UNSIGNED NOT NULL,
  eventTimeFine   SMALLINT  UNSIGNED NOT NULL,
  classification  ENUM('UNKNOWN',
                       'All',
                       'Trace',
                       'Debug',
                       'Info',
                       'User',
                       'Warning',
                       'Error',
                       'Fatal',
                       'Off') NOT NULL,
  message   VARCHAR(1600)    NOT NULL,
  type      VARCHAR(64)      NOT NULL,
  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=MyISAM CHARSET=latin1;



CALL IndexLogMessage();


system echo 'Creating CommandMessage table' `date`

-- CommandMessage links to CommandStatus+ across master key/requestId
-- CommandStatus are time-ordered
-- MPCS-7917 - Increase message size to 4096 from 1024

CREATE TABLE CommandMessage
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  requestId       VARCHAR(100)       NOT NULL,
  message         VARCHAR(4096)      NOT NULL,
  type ENUM('UNKNOWN',
            'FileLoad',
            'FlightSoftwareCommand',
            'HardwareCommand',
            'RawUplinkData',
            'Scmf',
            'SequenceDirective',
            'SseCommand',
            'FileCfdp',
            'CltuF') NOT NULL,
  originalFile  VARCHAR(1024) NULL,
  scmfFile      VARCHAR(1024) NULL,
  commandedSide VARCHAR(16)   NULL,
  finalized     TINYINT UNSIGNED NOT NULL,
  checksum      INT     UNSIGNED NULL,
  totalCltus    INT     UNSIGNED NULL,

  PRIMARY KEY(hostId, sessionId, requestId),
  KEY finalizedKey(finalized)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;



system echo 'Creating CommandStatus table' `date`

CREATE TABLE CommandStatus
(
  sessionId            MEDIUMINT UNSIGNED NOT NULL,
  hostId               MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment      SMALLINT  UNSIGNED NOT NULL,
  requestId            VARCHAR(100)       NOT NULL,
  rctCoarse            INT       UNSIGNED NOT NULL,
  rctFine              SMALLINT  UNSIGNED NOT NULL,
  eventTimeCoarse      INT       UNSIGNED NOT NULL,
  eventTimeFine        SMALLINT  UNSIGNED NOT NULL,
  status               VARCHAR(32)        NOT NULL,
  failReason           VARCHAR(128)       NULL,
  dssId                SMALLINT  UNSIGNED NOT NULL,
  bit1RadTimeCoarse    INT       UNSIGNED NULL,
  bit1RadTimeFine      SMALLINT  UNSIGNED NULL,
  lastBitRadTimeCoarse INT       UNSIGNED NULL,
  lastBitRadTimeFine   SMALLINT  UNSIGNED NULL,
  final                TINYINT   UNSIGNED NOT NULL,

  KEY comboIndex(hostId, sessionId, sessionFragment, requestId),
  KEY finalKey(final)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating Product table' `date`

CREATE TABLE Product
(
  sessionId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  sessionFragment SMALLINT  UNSIGNED NOT NULL,
  rctCoarse          INT      UNSIGNED NOT NULL,
  rctFine            SMALLINT UNSIGNED NOT NULL,
  creationTimeCoarse INT      UNSIGNED NOT NULL,
  creationTimeFine   SMALLINT UNSIGNED NOT NULL,
  dvtScetCoarse INT      UNSIGNED NOT NULL,
  dvtScetFine   SMALLINT UNSIGNED NOT NULL,
  vcid      INT     UNSIGNED NOT NULL,
  isPartial TINYINT UNSIGNED NOT NULL,
  apid INT UNSIGNED NOT NULL,
  apidName VARCHAR(64) NULL,
  sequenceId INT UNSIGNED NOT NULL,
  sequenceVersion SMALLINT UNSIGNED NOT NULL,
  commandNumber INT UNSIGNED NOT NULL,
  xmlVersion INT UNSIGNED NOT NULL,
  totalParts SMALLINT UNSIGNED NOT NULL,
  dvtSclkCoarse INT UNSIGNED NOT NULL,
  dvtSclkFine   INT UNSIGNED NOT NULL,
  fullPath VARCHAR(1024) NOT NULL,
  fileName VARCHAR(1024) NOT NULL,
  ertCoarse INT UNSIGNED NOT NULL,
  ertFine   INT UNSIGNED NOT NULL,
  groundStatus ENUM('UNKNOWN',
                    'PARTIAL',
                    'COMPLETE',
                    'COMPLETE_NO_CHECKSUM',
                    'COMPLETE_CHECKSUM_PASS',
                    'COMPLETE_CHECKSUM_FAIL',
                    'PARTIAL_CHECKSUM_FAIL') NOT NULL,
  sequenceCategory VARCHAR(8)   NULL,
  sequenceNumber   INT UNSIGNED NULL,
  version DECIMAL(6,3) UNSIGNED NOT NULL,
  checksum          BIGINT UNSIGNED NOT NULL,
  cfdpTransactionId BIGINT UNSIGNED NOT NULL,
  fileSize          BIGINT UNSIGNED NOT NULL
) ENGINE=MyISAM CHARSET=latin1;


CALL IndexProduct();



system echo 'Creating CfdpIndication table' `date`

CREATE TABLE CfdpIndication
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  indicationTimeCoarse INT      UNSIGNED NOT NULL,
  indicationTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  type ENUM('TRANSACTION',
            'REPORT',
            'SUSPENDED',
            'RESUMED',
            'FAULT',
            'TRANSACTION_FINISHED',
            'ABANDONED',
            'NEW_TRANSACTION_DETECTED',
            'EOF_SENT',
            'METADATA_RECV',
            'FILE_SEGMENT_RECV',
            'EOF_RECV') NOT NULL,
  faultCondition ENUM('NO_ERROR',
                 'POSITIVE_ACK_LIMIT_REACHED',
                 'KEEP_ALIVE_LIMIT_REACHED',
                 'INVALID_TRANSMISSION_MODE',
                 'FILESTORE_REJECTION',
                 'FILE_CHECKSUM_FAILURE',
                 'FILE_SIZE_ERROR',
                 'NAK_LIMIT_REACHED',
                 'INACTIVITY_DETECTED',
                 'INVALID_FILE_STRUCTURE',
                 'RESERVED_BY_CCSDS_10',
                 'RESERVED_BY_CCSDS_11',
                 'RESERVED_BY_CCSDS_12',
                 'RESERVED_BY_CCSDS_13',
                 'SUSPEND_REQUEST_RECEIVED',
                 'CANCEL_REQUEST_RECEIVED') NULL,
  transactionDirection ENUM('IN',
                            'OUT') NOT NULL,
  sourceEntityId BIGINT UNSIGNED NOT NULL,
  transactionSequenceNumber BIGINT UNSIGNED NOT NULL,
  serviceClass TINYINT UNSIGNED NOT NULL,
  destinationEntityId BIGINT UNSIGNED NOT NULL,
  involvesFileTransfer BOOL NOT NULL,
  totalBytesSentOrReceived BIGINT UNSIGNED NOT NULL,
  triggeringType ENUM('PDU',
                      'REQUEST') NULL,
  pduId varchar(64) NULL,
  pduHeaderVersion TINYINT UNSIGNED NULL,
  pduHeaderType ENUM('DIRECTIVE',
                     'DATA') NULL,
  pduHeaderDirection ENUM('TO_RECEIVER',
                           'TO_SENDER') NULL,
  pduHeaderTransmissionMode ENUM('ACKNOWLEDGED',
                                 'UNACKNOWLEDGED') NULL,
  pduHeaderCrcFlagPresent BOOL NULL,
  pduHeaderDataFieldLength INT UNSIGNED NULL,
  pduHeaderEntityIdLength INT UNSIGNED NULL,
  pduHeaderTransactionSequenceNumberLength INT UNSIGNED NULL,
  pduHeaderSourceEntityId BIGINT UNSIGNED NULL,
  pduHeaderTransactionSequenceNumber BIGINT UNSIGNED NULL,
  pduHeaderDestinationEntityId BIGINT UNSIGNED NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpFileGeneration table' `date`

CREATE TABLE CfdpFileGeneration
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  eventTimeCoarse INT      UNSIGNED NOT NULL,
  eventTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  downlinkFileMetadataFileLocation varchar(512) NOT NULL,
  downlinkFileLocation varchar(512) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpFileUplinkFinished table' `date`

CREATE TABLE CfdpFileUplinkFinished
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  eventTimeCoarse INT      UNSIGNED NOT NULL,
  eventTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  uplinkFileMetadataFileLocation varchar(512) NOT NULL,
  uplinkFileLocation varchar(512) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpRequestReceived table' `date`

CREATE TABLE CfdpRequestReceived
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  eventTimeCoarse INT      UNSIGNED NOT NULL,
  eventTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  requestId varchar(64) NOT NULL,
	requesterId varchar(64) NOT NULL,
	httpUser varchar(64) NOT NULL,
	httpHost varchar(64) NOT NULL,
	requestContent varchar(1023) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpRequestResult table' `date`

CREATE TABLE CfdpRequestResult
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  eventTimeCoarse INT      UNSIGNED NOT NULL,
  eventTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  requestId varchar(64) NOT NULL,
  rejected BOOL NOT NULL,
	resultContent varchar(1023) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpPduReceived table' `date`

CREATE TABLE CfdpPduReceived
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  pduTimeCoarse INT      UNSIGNED NOT NULL,
  pduTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  pduId varchar(64) NOT NULL,
  metadata varchar(4095) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;


system echo 'Creating CfdpPduSent table' `date`

CREATE TABLE CfdpPduSent
(
  sessionId       MEDIUMINT UNSIGNED,
  hostId          MEDIUMINT UNSIGNED,
  sessionFragment SMALLINT  UNSIGNED,

  pduTimeCoarse INT      UNSIGNED NOT NULL,
  pduTimeFine   SMALLINT UNSIGNED NOT NULL,
  cfdpProcessorInstanceId varchar(32) NOT NULL,
  pduId varchar(64) NOT NULL,
  metadata varchar(4095) NOT NULL,

  contextId       MEDIUMINT UNSIGNED DEFAULT NULL,
  contextHostId   MEDIUMINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

system echo 'Creating ContextConfig table' `date`

CREATE TABLE ContextConfig
(
  contextId       MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  user VARCHAR(32) NOT NULL,
  type VARCHAR(64) NOT NULL,
  host VARCHAR(64) NOT NULL,
  name VARCHAR(64),
  sessionId MEDIUMINT UNSIGNED,
  parentId MEDIUMINT UNSIGNED,
  mpcsVersion      VARCHAR(16) NOT NULL,

  PRIMARY KEY(contextId, hostId),
  KEY        hostIndex(host),
  KEY        userIndex(user)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

CREATE TABLE ContextConfigKeyValue
(
  keyValueId      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  contextId       MEDIUMINT UNSIGNED NOT NULL,
  hostId          MEDIUMINT UNSIGNED NOT NULL,
  keyName         VARCHAR(128) NOT NULL,
  value           VARCHAR(4096),
  PRIMARY KEY     (keyValueId, contextId, hostId),
  KEY         keyIndex(keyName)
) ENGINE=InnoDB ROW_FORMAT=COMPACT CHARSET=latin1;

system echo 'Creating ChannelAggregate tables' `date`

-- id is unique within each master key and channelType.

CREATE TABLE ChannelAggregate
(
    hostId          MEDIUMINT UNSIGNED NOT NULL,
    sessionId       MEDIUMINT UNSIGNED NOT NULL,
    sessionFragment SMALLINT  UNSIGNED NOT NULL,
    id              INT       UNSIGNED NOT NULL,

    channelType     ENUM('FSW_RT',
                         'FSW_REC') NOT NULL,

    packetIds       VARCHAR(3000) NULL,
    
    beginRctCoarse  INT      UNSIGNED NOT NULL,
    endRctCoarse    INT      UNSIGNED NOT NULL,

    beginErtCoarse  INT      UNSIGNED NOT NULL,
    beginErtFine    INT      UNSIGNED NOT NULL,
    
    endErtCoarse    INT      UNSIGNED NOT NULL,
    endErtFine      INT      UNSIGNED NOT NULL,

    beginSclkCoarse INT      UNSIGNED NULL,
    endSclkCoarse   BIGINT   UNSIGNED NULL,
    
    beginScetCoarse INT      UNSIGNED NULL,
    endScetCoarse   INT      UNSIGNED NULL,

    dssId           SMALLINT UNSIGNED NOT NULL,
    vcid            INT      UNSIGNED NULL,
    count           SMALLINT UNSIGNED NOT NULL,
    distinctCount   SMALLINT UNSIGNED NOT NULL,

    contents        BLOB NOT NULL, -- 65535; want bigger, use MEDIUMBLOB
    chanIdsString   VARCHAR(3000) NOT NULL,
    
    -- Serves for uniqueness and session + channel type index
    PRIMARY KEY(hostId, sessionId, sessionFragment, channelType, id)

) ENGINE=InnoDB ROW_FORMAT=DYNAMIC CHARSET=LATIN1;

CALL IndexChannelAggregate();

system echo 'Creating SseChannelAggregate tables' `date`

-- id is unique within each master key and channelType.

CREATE TABLE SseChannelAggregate
(
    hostId          MEDIUMINT UNSIGNED NOT NULL,
    sessionId       MEDIUMINT UNSIGNED NOT NULL,
    sessionFragment SMALLINT  UNSIGNED NOT NULL,
    id              INT       UNSIGNED NOT NULL,

    packetIds       VARCHAR(3000) NULL,

    beginRctCoarse  INT      UNSIGNED NOT NULL,
    endRctCoarse    INT      UNSIGNED NOT NULL,

    beginErtCoarse  INT      UNSIGNED NOT NULL,
    beginErtFine    INT      UNSIGNED NOT NULL,
    
    endErtCoarse    INT      UNSIGNED NOT NULL,
    endErtFine      INT      UNSIGNED NOT NULL,

    beginSclkCoarse INT      UNSIGNED NULL,
    endSclkCoarse   BIGINT   UNSIGNED NULL,

    beginScetCoarse INT      UNSIGNED NULL,
    endScetCoarse   INT      UNSIGNED NULL,

    count           SMALLINT UNSIGNED NOT NULL,
    distinctCount   SMALLINT UNSIGNED NOT NULL,

    contents        BLOB NOT NULL, -- 65535; want bigger, use MEDIUMBLOB
    chanIdsString   VARCHAR(3000) NOT NULL,
    
    -- Serves for uniqueness and session + channel type index
    PRIMARY KEY(hostId, sessionId, sessionFragment, id)

) ENGINE=InnoDB ROW_FORMAT=DYNAMIC CHARSET=LATIN1;

CALL IndexSseChannelAggregate();

system echo 'Creating HeaderChannelAggregate tables' `date`

-- id is unique within each master key and channelType.

CREATE TABLE HeaderChannelAggregate
(
    hostId          MEDIUMINT UNSIGNED NOT NULL,
    sessionId       MEDIUMINT UNSIGNED NOT NULL,
    sessionFragment SMALLINT  UNSIGNED NOT NULL,
    id              INT       UNSIGNED NOT NULL,

    channelType     ENUM('FRAME_HEADER',
                         'PACKET_HEADER',
                         'SSE_HEADER') NOT NULL,

    beginRctCoarse  INT      UNSIGNED NOT NULL,
    endRctCoarse    INT      UNSIGNED NOT NULL,

    beginErtCoarse  INT      UNSIGNED NOT NULL,
    beginErtFine    INT      UNSIGNED NOT NULL,
    
    endErtCoarse    INT      UNSIGNED NOT NULL,
    endErtFine      INT      UNSIGNED NOT NULL,

    apid            INT      UNSIGNED NULL,
    dssId           INT      UNSIGNED NULL,
    vcid            INT      UNSIGNED NULL,
    count           SMALLINT UNSIGNED NOT NULL,
    distinctCount   SMALLINT UNSIGNED NOT NULL,

    contents        BLOB NOT NULL, -- 65535; want bigger, use MEDIUMBLOB
    chanIdsString   VARCHAR(3000) NOT NULL,
    
    -- Serves for uniqueness and session + channel type index
    PRIMARY KEY(hostId, sessionId, sessionFragment, channelType, id)

) ENGINE=InnoDB ROW_FORMAT=DYNAMIC CHARSET=LATIN1;

CALL IndexHeaderChannelAggregate();

system echo 'Creating MonitorChannelAggregate tables' `date`

-- id is unique within each master key and channelType.

CREATE TABLE MonitorChannelAggregate
(
    hostId          MEDIUMINT UNSIGNED NOT NULL,
    sessionId       MEDIUMINT UNSIGNED NOT NULL,
    sessionFragment SMALLINT  UNSIGNED NOT NULL,
    id              INT       UNSIGNED NOT NULL,

    beginRctCoarse  INT      UNSIGNED NOT NULL,
    endRctCoarse    INT      UNSIGNED NOT NULL,

    beginMstCoarse  INT      UNSIGNED NOT NULL,
    beginMstFine    INT      UNSIGNED NOT NULL,
    
    endMstCoarse    INT      UNSIGNED NOT NULL,
    endMstFine      INT      UNSIGNED NOT NULL,
    
    dssId           SMALLINT UNSIGNED NOT NULL,
    count           SMALLINT UNSIGNED NOT NULL,
    distinctCount   SMALLINT UNSIGNED NOT NULL,

    contents        BLOB NOT NULL, -- 65535; want bigger, use MEDIUMBLOB
    chanIdsString   VARCHAR(3000) NOT NULL,
    
    -- Serves for uniqueness and session + channel type index
    PRIMARY KEY(hostId, sessionId, sessionFragment, id)

) ENGINE=InnoDB ROW_FORMAT=DYNAMIC CHARSET=LATIN1;

CALL IndexMonitorChannelAggregate();
