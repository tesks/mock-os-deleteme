-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
-- R8.1 does not support ECDR with EHA Aggregates

/*
ALTER TABLE ChannelValue
   ADD INDEX ecdrScetIndex(sessionId, scetCoarse, scetFine);

ALTER TABLE SseChannelValue
   ADD INDEX ecdrScetIndex(sessionId, scetCoarse, scetFine);

ALTER TABLE HeaderChannelValue
   ADD INDEX ecdrErtIndex(sessionId, ertCoarse, ertFine);

ALTER TABLE MonitorChannelValue
   ADD INDEX ecdrErtIndex(sessionId, mstCoarse, mstFine);
*/