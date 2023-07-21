ALTER TABLE Packet
   DROP INDEX comboIndex,
   ADD PRIMARY KEY(hostId, sessionId, id);

ALTER TABLE SsePacket
   DROP INDEX comboIndex,
   ADD PRIMARY KEY(hostId, sessionId, id);

ALTER TABLE Frame
   DROP INDEX comboIndex,
   ADD PRIMARY KEY(hostId, sessionId, id);

CREATE TABLE IF NOT EXISTS Changes
(
    name        VARCHAR(32)   NOT NULL,
    modified    DATETIME      NOT NULL,
    description VARCHAR(1024) NOT NULL,

    PRIMARY KEY(name)
) ENGINE=InnoDB;

INSERT INTO Changes VALUES(
    "includePacketInfo",
    NOW(),
    "07/24/2012: Drop comboIndex on Frame, Packet, and SsePacket, and replace with primary key on master key and id");
