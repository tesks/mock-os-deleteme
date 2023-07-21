NOWARNING;

DROP FUNCTION IF EXISTS FROM_JAVATIME;
DROP FUNCTION IF EXISTS TO_JAVATIME;

DROP FUNCTION IF EXISTS COARSE_FROM_EXACT_TIME;

DROP FUNCTION IF EXISTS FINE_FROM_EXACT_TIME;
DROP FUNCTION IF EXISTS EXACT_TIME_FROM_COARSE_FINE;

DROP FUNCTION IF EXISTS ERT_FINE_FROM_EXACT_TIME;
DROP FUNCTION IF EXISTS ERT_EXACT_TIME_FROM_COARSE_FINE;
DROP FUNCTION IF EXISTS ERT_EXACT_FINE_FROM_FINE;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK;
DROP FUNCTION IF EXISTS TO_SCLK;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_MSL;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_MSL;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_MSL;
DROP FUNCTION IF EXISTS TO_SCLK_MSL;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_MSL;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_MSL;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_MER;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_MER;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_MER;
DROP FUNCTION IF EXISTS TO_SCLK_MER;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_MER;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_MER;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_MSAP;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_MSAP;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_MSAP;
DROP FUNCTION IF EXISTS TO_SCLK_MSAP;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_MSAP;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_MSAP;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_GENERIC;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_GENERIC;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_GENERIC;
DROP FUNCTION IF EXISTS TO_SCLK_GENERIC;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_GENERIC;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_GENERIC;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_DAWN;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_DAWN;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_DAWN;
DROP FUNCTION IF EXISTS TO_SCLK_DAWN;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_DAWN;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_DAWN;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_DLRE;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_DLRE;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_DLRE;
DROP FUNCTION IF EXISTS TO_SCLK_DLRE;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_DLRE;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_DLRE;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_EPOXI;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_EPOXI;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_EPOXI;
DROP FUNCTION IF EXISTS TO_SCLK_EPOXI;
DROP FUNCTION IF EXISTS TO_SCLK_STRING_EPOXI;
DROP FUNCTION IF EXISTS TO_SCLK_TICKS_EPOXI;

DROP FUNCTION IF EXISTS COARSE_FROM_PACKEDSCLK_ANY;
DROP FUNCTION IF EXISTS FINE_FROM_PACKEDSCLK_ANY;
DROP FUNCTION IF EXISTS TO_PACKEDSCLK_ANY;

DROP FUNCTION IF EXISTS FORMAT_RUNFLAGS;

DROP FUNCTION IF EXISTS INDEX_EXISTS;

-- From Java time as a long int to Datetime
CREATE FUNCTION FROM_JAVATIME(JT BIGINT)
    RETURNS Datetime DETERMINISTIC
    RETURN CONVERT_TZ(FROM_UNIXTIME(JT div 1000),@@time_zone,'+00:00');

-- From Datetime to Java time as a long int
CREATE FUNCTION TO_JAVATIME(DT Datetime)
    RETURNS BIGINT DETERMINISTIC
    RETURN UNIX_TIMESTAMP(CONVERT_TZ(DT,'+00:00',@@time_zone))*1000;

-- From SCLK packed as an unsigned long int to SCLK coarse
--     FS is the byte size of the SCLK fine
CREATE FUNCTION COARSE_FROM_PACKEDSCLK(PS BIGINT  UNSIGNED,
                                       FS TINYINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN PS DIV POW(256, FS);

-- From Java time as a long int to coarse
CREATE FUNCTION COARSE_FROM_EXACT_TIME(T BIGINT)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN IF(T IS NULL, 0, T DIV 1000);

-- From Java time as a long int to fine
CREATE FUNCTION FINE_FROM_EXACT_TIME(T BIGINT)
    RETURNS SMALLINT UNSIGNED DETERMINISTIC
    RETURN IF(T IS NULL, 0, T MOD 1000);

-- From Java time as a long int plus extra to fine, for ERT
CREATE FUNCTION ERT_FINE_FROM_EXACT_TIME(T BIGINT,
                                         F BIGINT)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN IF(T IS NULL,
              0,
              ((T MOD 1000) * 1000000) + IF(F IS NULL, 0, F));

-- From coarse and fine to Java time
CREATE FUNCTION EXACT_TIME_FROM_COARSE_FINE(C INT      UNSIGNED,
                                            F SMALLINT UNSIGNED)
    RETURNS BIGINT DETERMINISTIC
    RETURN IF(C IS NULL, 0, (C * 1000)) + IF(F IS NULL, 0, F);

-- From coarse and fine to Java time, for ERT
CREATE FUNCTION ERT_EXACT_TIME_FROM_COARSE_FINE(C INT UNSIGNED,
                                                F INT UNSIGNED)
    RETURNS BIGINT DETERMINISTIC
    RETURN IF(C IS NULL,
              0,
              (C * 1000) + IF(F IS NULL, 0, F DIV 1000000));

-- From fine to Java fine time, for ERT
CREATE FUNCTION ERT_EXACT_FINE_FROM_FINE(F INT UNSIGNED)
    RETURNS BIGINT DETERMINISTIC
    RETURN IF(F IS NULL, 0, F MOD 1000000);

-- From SCLK packed as an unsigned long int to SCLK fine
--     FS is the byte size of the SCLK fine
CREATE FUNCTION FINE_FROM_PACKEDSCLK(PS BIGINT  UNSIGNED,
                                     FS TINYINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN PS % POW(256, FS);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int
--     FS is the byte size of the SCLK fine
CREATE FUNCTION TO_PACKEDSCLK(C  INT     UNSIGNED,
                              F  INT     UNSIGNED,
                              FS TINYINT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN (C * POW(256, FS)) + F;

-- From SCLK as coarse and fine to SCLK in decimal seconds
--     FSB is the bit size of the SCLK fine
CREATE FUNCTION TO_SCLK(C   INT     UNSIGNED,
                        F   INT     UNSIGNED,
                        FSB TINYINT UNSIGNED)
    RETURNS DECIMAL(20, 10) DETERMINISTIC
    RETURN
        C +
        (CONVERT(F, DECIMAL(20, 10)) / CONVERT(POW(2, FSB), DECIMAL(20, 10)));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for MSL
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_MSL(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for MSL
CREATE FUNCTION FINE_FROM_PACKEDSCLK_MSL(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for MSL
CREATE FUNCTION TO_PACKEDSCLK_MSL(C INT UNSIGNED,
                                  F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for MSL
CREATE FUNCTION TO_SCLK_MSL(C INT UNSIGNED,
                            F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for MSL
CREATE FUNCTION TO_SCLK_STRING_MSL(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_MSL(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for MSL
CREATE FUNCTION TO_SCLK_TICKS_MSL(C INT UNSIGNED,
                                  F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for MER
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_MER(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for MER
CREATE FUNCTION FINE_FROM_PACKEDSCLK_MER(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for MER
CREATE FUNCTION TO_PACKEDSCLK_MER(C INT UNSIGNED,
                                  F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for MER
CREATE FUNCTION TO_SCLK_MER(C INT UNSIGNED,
                            F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for MER
CREATE FUNCTION TO_SCLK_STRING_MER(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_MER(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for MER
CREATE FUNCTION TO_SCLK_TICKS_MER(C INT UNSIGNED,
                                  F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for MSAP
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_MSAP(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for MSAP
CREATE FUNCTION FINE_FROM_PACKEDSCLK_MSAP(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for MSAP
CREATE FUNCTION TO_PACKEDSCLK_MSAP(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for MSAP
CREATE FUNCTION TO_SCLK_MSAP(C INT UNSIGNED,
                             F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for MSAP
CREATE FUNCTION TO_SCLK_STRING_MSAP(C INT UNSIGNED,
                                    F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_MSAP(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for MSAP
CREATE FUNCTION TO_SCLK_TICKS_MSAP(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for GENERIC
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_GENERIC(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for GENERIC
CREATE FUNCTION FINE_FROM_PACKEDSCLK_GENERIC(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int,
--     for GENERIC
CREATE FUNCTION TO_PACKEDSCLK_GENERIC(C INT UNSIGNED,
                                      F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for GENERIC
CREATE FUNCTION TO_SCLK_GENERIC(C INT UNSIGNED,
                                F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for GENERIC
CREATE FUNCTION TO_SCLK_STRING_GENERIC(C INT UNSIGNED,
                                       F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_GENERIC(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for GENERIC
CREATE FUNCTION TO_SCLK_TICKS_GENERIC(C INT UNSIGNED,
                                      F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for DAWN
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_DAWN(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for DAWN
CREATE FUNCTION FINE_FROM_PACKEDSCLK_DAWN(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for DAWN
CREATE FUNCTION TO_PACKEDSCLK_DAWN(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for DAWN
CREATE FUNCTION TO_SCLK_DAWN(C INT UNSIGNED,
                             F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for DAWN
CREATE FUNCTION TO_SCLK_STRING_DAWN(C INT UNSIGNED,
                                    F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_DAWN(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for DAWN
CREATE FUNCTION TO_SCLK_TICKS_DAWN(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for DLRE
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_DLRE(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for DLRE
CREATE FUNCTION FINE_FROM_PACKEDSCLK_DLRE(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for DLRE
CREATE FUNCTION TO_PACKEDSCLK_DLRE(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for DLRE
CREATE FUNCTION TO_SCLK_DLRE(C INT UNSIGNED,
                             F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for DLRE
CREATE FUNCTION TO_SCLK_STRING_DLRE(C INT UNSIGNED,
                                    F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_DLRE(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for DLRE
CREATE FUNCTION TO_SCLK_TICKS_DLRE(C INT UNSIGNED,
                                   F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- -----------

-- From SCLK packed as an unsigned long int to SCLK coarse, for EPOXI
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_EPOXI(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN COARSE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK packed as an unsigned long int to SCLK fine, for EPOXI
CREATE FUNCTION FINE_FROM_PACKEDSCLK_EPOXI(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC
    RETURN FINE_FROM_PACKEDSCLK(PS, 2);

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int,
--     for EPOXI
CREATE FUNCTION TO_PACKEDSCLK_EPOXI(C INT UNSIGNED,
                                    F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC
    RETURN TO_PACKEDSCLK(C, F, 2);

-- From SCLK as coarse and fine to SCLK in decimal seconds, for EPOXI
CREATE FUNCTION TO_SCLK_EPOXI(C INT UNSIGNED,
                              F INT UNSIGNED)
    RETURNS DECIMAL(15, 5) DETERMINISTIC
    RETURN TO_SCLK(C, F, 16);

-- From SCLK as coarse and fine to SCLK in decimal seconds as char, for EPOXI
CREATE FUNCTION TO_SCLK_STRING_EPOXI(C INT UNSIGNED,
                                     F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN LPAD(CONVERT(TO_SCLK_EPOXI(C, F), CHAR), 16, '0');

-- From SCLK as coarse and fine to SCLK in ticks as char, for EPOXI
CREATE FUNCTION TO_SCLK_TICKS_EPOXI(C INT UNSIGNED,
                                    F INT UNSIGNED)
    RETURNS CHAR(16) DETERMINISTIC
    RETURN CONCAT(LPAD(C, 10, '0'), '-', LPAD(F, 5, '0'));

-- Test if index exists in table

CREATE FUNCTION INDEX_EXISTS(T VARCHAR(128),
                             I VARCHAR(128))
    RETURNS TINYINT DETERMINISTIC
    RETURN IF((SELECT COUNT(*)
               FROM information_schema.statistics
               WHERE ((TABLE_SCHEMA = DATABASE()) AND
                      (TABLE_NAME   = T)          AND
                      (INDEX_NAME   = I))) = 0,
              0, 1);

-- -----------

DELIMITER //

-- From SCLK packed as an unsigned long int to SCLK coarse, for any mission
CREATE FUNCTION COARSE_FROM_PACKEDSCLK_ANY(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC

BEGIN
    CASE @mission

    WHEN 'msl'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_MSL(PS);

    WHEN 'mer'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_MER(PS);

    WHEN 'msap'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_MSAP(PS);

    WHEN 'generic'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_GENERIC(PS);

    WHEN 'dawn'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_DAWN(PS);

    WHEN 'dlre'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_DLRE(PS);

    WHEN 'epoxi'
    THEN
        RETURN COARSE_FROM_PACKEDSCLK_EPOXI(PS);

    ELSE
        RETURN -1;

    END CASE;

END //

DELIMITER ;

-- -----------

DELIMITER //

-- From SCLK packed as an unsigned long int to SCLK coarse, for any mission
CREATE FUNCTION FINE_FROM_PACKEDSCLK_ANY(PS BIGINT UNSIGNED)
    RETURNS INT UNSIGNED DETERMINISTIC

BEGIN
    CASE @mission

    WHEN 'msl'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_MSL(PS);

    WHEN 'mer'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_MER(PS);

    WHEN 'msap'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_MSAP(PS);

    WHEN 'generic'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_GENERIC(PS);

    WHEN 'dawn'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_DAWN(PS);

    WHEN 'dlre'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_DLRE(PS);

    WHEN 'epoxi'
    THEN
        RETURN FINE_FROM_PACKEDSCLK_EPOXI(PS);

    ELSE
        RETURN -1;

    END CASE;

END //

DELIMITER ;

-- ------------------

DELIMITER //

-- From SCLK as coarse and fine to SCLK packed as an unsigned long int, for any
CREATE FUNCTION TO_PACKEDSCLK_ANY(C INT UNSIGNED,
                                  F INT UNSIGNED)
    RETURNS BIGINT UNSIGNED DETERMINISTIC

BEGIN
    CASE @mission

    WHEN 'msl'
    THEN
        RETURN TO_PACKEDSCLK_MSL(C, F);

    WHEN 'mer'
    THEN
        RETURN TO_PACKEDSCLK_MER(C, F);

    WHEN 'msap'
    THEN
        RETURN TO_PACKEDSCLK_MSAP(C, F);

    WHEN 'generic'
    THEN
        RETURN TO_PACKEDSCLK_GENERIC(C, F);

    WHEN 'dawn'
    THEN
        RETURN TO_PACKEDSCLK_DAWN(C, F);

    WHEN 'dlre'
    THEN
        RETURN TO_PACKEDSCLK_DLRE(C, F);

    WHEN 'epoxi'
    THEN
        RETURN TO_PACKEDSCLK_EPOXI(C, F);

    ELSE
        RETURN -1;

    END CASE;

END //

DELIMITER ;

-- ------------------

DROP PROCEDURE IF EXISTS populateHost;

DELIMITER //

CREATE PROCEDURE populateHost(offset tinyint unsigned) MODIFIES SQL DATA
BEGIN
    DECLARE l_done  int unsigned DEFAULT 0;
    DECLARE l_index int unsigned DEFAULT 0;
    DECLARE l_host  varchar(64);

    DECLARE cur CURSOR FOR
        SELECT distinct host FROM Session;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET l_done=1;

    OPEN cur;

    l_loop: LOOP
        FETCH cur INTO l_host;

        IF l_done=1
        THEN
            LEAVE l_loop;
        END IF;

        INSERT INTO Host VALUES((offset << 16) | l_index, l_host, offset);

        SET l_index=l_index + 1;
    END LOOP l_loop;

    CLOSE cur;

    IF l_index=0
    THEN
        -- Host table was empty

        SET l_index=locate('.', @@hostname);

        IF l_index=0
        THEN
            SET l_host=LOWER(@@hostname);
        ELSE
            SET l_host=substr(LOWER(@@hostname), 1, l_index - 1);
        END IF;

        INSERT INTO Host VALUES(offset << 16, l_host, offset);
    END IF;
END//

DELIMITER ;

-- ------------------

DROP PROCEDURE IF EXISTS DropTimeIndexes;

DELIMITER //

CREATE PROCEDURE DropTimeIndexes(tblName VARCHAR(64)) MODIFIES SQL DATA
BEGIN

    -- MPCS-6520 - Get rid of ECDR indexes if present for upgrade
    -- MPCS-7106 - Get rid of ECDR indexes if present for upgrade

    DECLARE l_done  int unsigned DEFAULT 0;
    DECLARE l_count int unsigned DEFAULT 0;
    DECLARE l_index VARCHAR(64);
    DECLARE drops   VARCHAR(2048);
    DECLARE cur CURSOR FOR
        SELECT distinct index_name
        FROM information_schema.statistics
        WHERE (table_schema = DATABASE()) AND
              (table_name   = tblName)    AND
              ((index_name = 'ertIndex')       OR
               (index_name = 'scetIndex')      OR
               (index_name = 'sclkIndex')      OR
               (index_name = 'ecdrScetIndex')  OR
               (index_name = 'ecdrErtIndex')   OR
               (index_name = 'eventTimeIndex') OR
               (index_name = 'creationTimeIndex'));

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET l_done=1;

    SET drops = CONCAT('ALTER TABLE ', tblName, ' ');

    OPEN cur;

    l_loop: LOOP
        FETCH cur INTO l_index;

        IF l_done = 1
        THEN
            LEAVE l_loop;
        END IF;

        IF l_count > 0
        THEN
            SET drops = CONCAT(drops, ', ');
        END IF;

        SET l_count = l_count + 1;

        SET drops = CONCAT(drops, 'DROP INDEX ', l_index);
    END LOOP l_loop;

    CLOSE cur;

    IF l_count > 0
    THEN
        SET @SQLStmt = drops;
        SELECT @SQLStmt;
        PREPARE s FROM @SQLStmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;

    -- Dummy statement to get rid of false error "Error 1329: No data..."
    -- when no indexes found.

    SELECT table_schema INTO l_index FROM information_schema.statistics LIMIT 1;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckTable;

DELIMITER //

CREATE PROCEDURE CheckTable(db  VARCHAR(64),
                            tbl VARCHAR(64))
BEGIN

    SELECT 'TABLES';
    SELECT * FROM information_schema.TABLES
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'TABLE_PRIVILEGES';
    SELECT * FROM information_schema.TABLE_PRIVILEGES
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'TABLE_CONSTRAINTS';
    SELECT * FROM information_schema.TABLE_CONSTRAINTS
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'COLUMNS';
    SELECT * FROM information_schema.COLUMNS
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'COLUMN_PRIVILEGES';
    SELECT * FROM information_schema.COLUMN_PRIVILEGES
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'KEY_COLUMN_USAGE';
    SELECT * FROM information_schema.KEY_COLUMN_USAGE
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'FILES';
    SELECT * FROM information_schema.FILES
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'STATISTICS';
    SELECT * FROM information_schema.STATISTICS
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'PARTITIONS';
    SELECT * FROM information_schema.PARTITIONS
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'VIEWS';
    SELECT * FROM information_schema.VIEWS
        WHERE table_schema=db AND table_name=tbl;

    SELECT 'TRIGGERS';
    SELECT * FROM information_schema.TRIGGERS
        WHERE trigger_schema=db AND
        ((event_object_table=tbl)         OR
         (action_reference_old_table=tbl) OR
         (action_reference_new_table=tbl));

    SELECT 'columns_priv';
    SELECT * FROM mysql.columns_priv
        WHERE Db=db AND Table_name=tbl;

    SELECT 'tables_priv';
    SELECT * FROM mysql.tables_priv
        WHERE Db=db AND Table_name=tbl;

END //

DELIMITER ;

-- ------------------

-- Expects table already created as MyISAM

DROP PROCEDURE IF EXISTS ForceInnoDbTable;

DELIMITER //

CREATE PROCEDURE ForceInnoDbTable(tbl VARCHAR(64))
BEGIN

    DECLARE alt    VARCHAR(1024);
    DECLARE status TINYINT DEFAULT 0;
    DECLARE count  TINYINT DEFAULT 0;

    DECLARE CONTINUE HANDLER FOR 1005
        SET status = 1;

    DECLARE CONTINUE HANDLER FOR 1025
        SET status = 1;

    SET alt = CONCAT('ALTER TABLE ', tbl, ' ENGINE=InnoDb');

    SELECT alt AS log;

    l_loop: LOOP

        SET count    = count + 1;
        SET status   = 0;
        SET @SQLStmt = alt;

        PREPARE s FROM @SQLStmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;

        IF (status = 0)
        THEN
            SELECT CONCAT('SUCCESS WITH ', count, ' TRIES') AS log;
            LEAVE l_loop;
        END IF;

        IF (count >= 5)
        THEN
            SELECT CONCAT('FAILURE AFTER ', count, ' TRIES') AS log;
            LEAVE l_loop;
        END IF;
    END LOOP l_loop;

END //

DELIMITER ;

DROP PROCEDURE IF EXISTS SetUpgradeParameters;

DELIMITER //

CREATE PROCEDURE SetUpgradeParameters()
BEGIN

    DECLARE pos  SMALLINT UNSIGNED;
    DECLARE temp VARCHAR(50);
    DECLARE ver1 VARCHAR(10);
    DECLARE ver2 VARCHAR(10);

    SET temp = VERSION();
    SET pos  = LOCATE('.', temp, 1);

    IF pos > 0
    THEN
        SET ver1 = SUBSTR(temp, 1, pos - 1);
        SET temp = SUBSTR(temp, pos + 1);
        SET pos  = LOCATE('.', temp, 1);

        IF pos > 0
        THEN
            SET ver2 = SUBSTR(temp, 1, pos - 1);
        ELSE
            SET ver2 = '0';
        END IF;
    ELSE
        SET ver1 = temp;
        SET ver2 = '0';
    END IF;

    SET GLOBAL CA_CACHE.key_buffer_size=0;
    SET GLOBAL EVR_CACHE.key_buffer_size=0;
    SET GLOBAL FRAME_CACHE.key_buffer_size=0;
    SET GLOBAL PACKET_CACHE.key_buffer_size=0;
    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET SESSION SQL_BIG_SELECTS=1;

    -- MPCS-5579 - Remove, doesn't belong here

    IF RIGHT(@@version_compile_machine, 3) = '_64'
    THEN
        SET bulk_insert_buffer_size=1*1024*1024*1024;
        SET GLOBAL key_buffer_size=1*1024*1024*1024;
        SET GLOBAL key_cache_division_limit=70;
        SET max_heap_table_size=1*1024*1024*1024;
        SET max_tmp_tables=32;
        SET myisam_sort_buffer_size=25*1024*1024*1024;
        SET preload_buffer_size=256*1024*1024;
        SET read_buffer_size=1*1024*1024*1024;
        SET read_rnd_buffer_size=64*1024*1024;
        SET sort_buffer_size=256*1024*1024;
        SET tmp_table_size=512*1024*1024;
    ELSE
        SET bulk_insert_buffer_size=32*1024*1024;
        SET GLOBAL key_buffer_size=512*1024*1024;
        SET GLOBAL key_cache_division_limit=70;
        SET max_heap_table_size=128*1024*1024;
        SET max_tmp_tables=8;
        SET myisam_sort_buffer_size=8*1024*1024;
        SET preload_buffer_size=64*1024*1024;
        SET read_buffer_size=64*1024*1024;
        SET read_rnd_buffer_size=64*1024*1024;
        SET sort_buffer_size=128*1024*1024;
        SET tmp_table_size=128*1024*1024;
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IssueDynamic;

DELIMITER //

CREATE PROCEDURE IssueDynamic(statement TEXT CHARSET latin1)
BEGIN

    SET @SQLStmt = statement;
    SELECT @SQLStmt dynamic;
    PREPARE s FROM @SQLStmt;
    EXECUTE s;
    DEALLOCATE PREPARE s;

END //

DELIMITER ;


-- Puts in the mission and any suffix.
-- Assumes that @idbname is set

DROP PROCEDURE IF EXISTS IssueByMission;

DELIMITER //

CREATE PROCEDURE IssueByMission(statement TEXT CHARSET latin1)
BEGIN

    CALL IssueDynamic(REPLACE(statement, 'mission_ampcs_prev', @idbname));

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS MY_SIGNAL;

DELIMITER //

CREATE PROCEDURE MY_SIGNAL(message VARCHAR(1024))
BEGIN

    SET @SQLStmt = CONCAT('UPDATE `', message, '` SET x = 1');

    PREPARE s FROM @SQLStmt;
    EXECUTE s;
    DEALLOCATE PREPARE s;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CopyInnoDB;

DELIMITER //

CREATE PROCEDURE CopyInnoDB(oldDb VARCHAR(256))
BEGIN

    DROP TABLE IF EXISTS Host;

    DROP TABLE IF EXISTS Session;

    DROP TABLE IF EXISTS EndSession;

    DROP TABLE IF EXISTS ChannelData;

    CALL IssueDynamic(CONCAT('CREATE TABLE Host LIKE ', oldDb, '.Host'));

    CALL IssueDynamic(CONCAT('CREATE TABLE Session LIKE ', oldDb, '.Session'));

    CALL IssueDynamic(
        CONCAT('CREATE TABLE EndSession LIKE ', oldDb, '.EndSession'));

    CALL IssueDynamic(
        CONCAT('CREATE TABLE ChannelData LIKE ', oldDb, '.ChannelData'));

    CALL IssueDynamic(
        CONCAT('INSERT INTO Host SELECT * FROM ', oldDb, '.Host'));

    CALL IssueDynamic(
        CONCAT('INSERT INTO Session SELECT * FROM ', oldDb, '.Session'));

    CALL IssueDynamic(
        CONCAT('INSERT INTO EndSession SELECT * FROM ', oldDb, '.EndSession'));

    CALL IssueDynamic(
        CONCAT('INSERT INTO ChannelData SELECT * FROM ', oldDb, '.ChannelData'));
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS TableSizes;

DELIMITER //

CREATE PROCEDURE TableSizes()
BEGIN

    SELECT table_name                      AS `table`,
           SUM(data_length + index_length) AS size
        FROM information_schema.tables
        WHERE (table_schema = DATABASE()) AND (table_type = 'BASE TABLE')
        GROUP BY table_name
        ORDER BY table_name;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS DatabaseSize;

DELIMITER //

CREATE PROCEDURE DatabaseSize()
BEGIN

    SELECT SUM(data_length + index_length) AS size
        FROM information_schema.tables
        WHERE (table_schema = DATABASE()) AND (table_type = 'BASE TABLE');

END //

DELIMITER ;


-- Load index caches for 32-bit hosts
DROP PROCEDURE IF EXISTS SmallCache;

DELIMITER //

CREATE PROCEDURE SmallCache()
BEGIN

    SET GLOBAL CA_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL key_buffer_size=512*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

END //

DELIMITER ;


-- Load index caches for 64-bit hosts with 32G or more
-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
DROP PROCEDURE IF EXISTS LoadCache;

DELIMITER //

CREATE PROCEDURE LoadCache()
BEGIN

    SET GLOBAL CA_CACHE.key_buffer_size=0;

    SET GLOBAL CA_CACHE.key_buffer_size=5*1024*1024*1024;

    SET GLOBAL CA_CACHE.key_cache_division_limit=70;

    CACHE INDEX ChannelAggregate        in CA_CACHE;
    CACHE INDEX SseChannelAggregate     in CA_CACHE;
    CACHE INDEX MonitorChannelAggregate in CA_CACHE;
    CACHE INDEX HeaderChannelAggregate  in CA_CACHE;

    LOAD INDEX INTO CACHE ChannelAggregate        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseChannelAggregate     IGNORE LEAVES;
    LOAD INDEX INTO CACHE MonitorChannelAggregate IGNORE LEAVES;
    LOAD INDEX INTO CACHE HeaderChannelAggregate  IGNORE LEAVES;

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=300*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr         in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

    -- Sse

    CACHE INDEX SseEvr         in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=2*1024*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=1*1024*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet IGNORE LEAVES;

    -- Sse

    CACHE INDEX SsePacket in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=64*1024*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=1*1024*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product    IGNORE LEAVES;

END //

DELIMITER ;


-- Load index caches for 64-bit hosts with 144G
DROP PROCEDURE IF EXISTS LoadBigCache;

DELIMITER //

CREATE PROCEDURE LoadBigCache()
BEGIN

    SET GLOBAL CA_CACHE.key_buffer_size=0;

    SET GLOBAL CA_CACHE.key_buffer_size=20*1024*1024*1024;

    SET GLOBAL CA_CACHE.key_cache_division_limit=70;

    CACHE INDEX ChannelAggregate        in CA_CACHE;
    CACHE INDEX SseChannelAggregate     in CA_CACHE;
    CACHE INDEX MonitorChannelAggregate in CA_CACHE;
    CACHE INDEX HeaderChannelAggregate  in CA_CACHE;

    LOAD INDEX INTO CACHE ChannelAggregate        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseChannelAggregate     IGNORE LEAVES;
    LOAD INDEX INTO CACHE MonitorChannelAggregate IGNORE LEAVES;
    LOAD INDEX INTO CACHE HeaderChannelAggregate  IGNORE LEAVES;

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=2*1024*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr         in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

-- Sse

    CACHE INDEX SseEvr         in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=8*1024*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=4*1024*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet IGNORE LEAVES;

-- Sse

    CACHE INDEX SsePacket in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=64*1024*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=4*1024*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product    IGNORE LEAVES;

END //

DELIMITER ;


-- Load index caches for 64-bit hosts with 24G
DROP PROCEDURE IF EXISTS Load24Cache;

DELIMITER //

CREATE PROCEDURE Load24Cache()
BEGIN

    SET GLOBAL CA_CACHE.key_buffer_size=0;

    SET GLOBAL CA_CACHE.key_buffer_size=3840*1024*1024;

    SET GLOBAL CA_CACHE.key_cache_division_limit=70;

    CACHE INDEX ChannelAggregate        in CA_CACHE;
    CACHE INDEX SseChannelAggregate     in CA_CACHE;
    CACHE INDEX MonitorChannelAggregate in CA_CACHE;
    CACHE INDEX HeaderChannelAggregate  in CA_CACHE;

    LOAD INDEX INTO CACHE ChannelAggregate        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseChannelAggregate     IGNORE LEAVES;
    LOAD INDEX INTO CACHE MonitorChannelAggregate IGNORE LEAVES;
    LOAD INDEX INTO CACHE HeaderChannelAggregate  IGNORE LEAVES;

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=225*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr         in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

    -- Sse

    CACHE INDEX SseEvr         in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr         IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=1536*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=768*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet IGNORE LEAVES;

    -- Sse

    CACHE INDEX SsePacket in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=49152*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=768*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product    IGNORE LEAVES;

END //

DELIMITER ;

DROP PROCEDURE IF EXISTS IndexChannelAggregate;

DELIMITER //

CREATE PROCEDURE IndexChannelAggregate()
BEGIN
    IF @extra != 0
    THEN
        ALTER TABLE ChannelAggregate
            ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
            ADD INDEX endErtIndex(endErtCoarse, endErtFine),
            ADD INDEX beginScetIndex(beginScetCoarse),
            ADD INDEX endScetIndex(endScetCoarse),
            ADD INDEX beginSclkIndex(beginSclkCoarse),
            ADD INDEX endSclkIndex(endSclkCoarse),
            ADD INDEX channelTypeIndex(channelType);
    ELSE
        ALTER TABLE ChannelAggregate
            ADD INDEX channelTypeIndex(channelType);
    END IF;

END //

DELIMITER ;

DROP PROCEDURE IF EXISTS IndexSseChannelAggregate;

DELIMITER //

CREATE PROCEDURE IndexSseChannelAggregate()
BEGIN
    IF @extra != 0
    THEN
        ALTER TABLE SseChannelAggregate
            ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
            ADD INDEX endErtIndex(endErtCoarse, endErtFine),
            ADD INDEX beginScetIndex(beginScetCoarse),
            ADD INDEX endScetIndex(endScetCoarse),
            ADD INDEX beginSclkIndex(beginSclkCoarse),
            ADD INDEX endSclkIndex(endSclkCoarse);
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexMonitorChannelAggregate;

DELIMITER //

CREATE PROCEDURE IndexMonitorChannelAggregate()
BEGIN
    IF @extra != 0
    THEN
        ALTER TABLE MonitorChannelAggregate
            ADD INDEX beginMstIndex(beginMstCoarse, beginMstFine),
            ADD INDEX endMstIndex(endMstCoarse, endMstFine);
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexHeaderChannelAggregate;

DELIMITER //

CREATE PROCEDURE IndexHeaderChannelAggregate()
BEGIN
    IF @extra != 0
    THEN
        ALTER TABLE HeaderChannelAggregate
            ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
            ADD INDEX endErtIndex(endErtCoarse, endErtFine),
            ADD INDEX channelTypeIndex(channelType);
    ELSE
        ALTER TABLE HeaderChannelAggregate
            ADD INDEX channelTypeIndex(channelType);
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexFrame;

DELIMITER //

CREATE PROCEDURE IndexFrame()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Frame
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id),
            ADD INDEX ertIndex(ertCoarse, ertFine);
    ELSE
        ALTER TABLE Frame
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexPacket;

DELIMITER //

CREATE PROCEDURE IndexPacket()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Packet
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE Packet
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexSsePacket;

DELIMITER //

CREATE PROCEDURE IndexSsePacket()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE SsePacket
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE SsePacket
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexEvr;

DELIMITER //

CREATE PROCEDURE IndexEvr()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Evr
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE Evr
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexSseEvr;

DELIMITER //

CREATE PROCEDURE IndexSseEvr()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE SseEvr
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE SseEvr
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexLogMessage;

DELIMITER //

CREATE PROCEDURE IndexLogMessage()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE LogMessage
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX comboContextIndex(contextHostId, contextId),
            ADD INDEX eventTimeIndex(eventTimeCoarse, eventTimeFine);
    ELSE
        ALTER TABLE LogMessage
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX comboContextIndex(contextHostId, contextId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexProduct;

DELIMITER //

CREATE PROCEDURE IndexProduct()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Product
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX creationTimeIndex(creationTimeCoarse, creationTimeFine),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(dvtScetCoarse, dvtScetFine),
            ADD INDEX sclkIndex(dvtSclkCoarse, dvtSclkFine);
    ELSE
        ALTER TABLE Product
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS OptimizeTables;

DELIMITER //

CREATE PROCEDURE OptimizeTables()
BEGIN

    OPTIMIZE TABLE CfdpIndication;
    OPTIMIZE TABLE CfdpFileGeneration;
    OPTIMIZE TABLE CfdpFileUplinkFinished;
    OPTIMIZE TABLE CfdpRequestReceived;
    OPTIMIZE TABLE CfdpRequestResult;
    OPTIMIZE TABLE CfdpPduReceived;
    OPTIMIZE TABLE CfdpPduSent;
    OPTIMIZE TABLE ChannelData;
    OPTIMIZE TABLE CommandMessage;
    OPTIMIZE TABLE CommandStatus;
    OPTIMIZE TABLE EndSession;
    OPTIMIZE TABLE Evr;
    OPTIMIZE TABLE EvrMetadata;
    OPTIMIZE TABLE Frame;
    OPTIMIZE TABLE FrameBody;
    OPTIMIZE TABLE Host;
    OPTIMIZE TABLE LogMessage;
    OPTIMIZE TABLE Packet;
    OPTIMIZE TABLE PacketBody;
    OPTIMIZE TABLE Product;
    OPTIMIZE TABLE Session;
    OPTIMIZE TABLE SseEvr;
    OPTIMIZE TABLE SseEvrMetadata;
    OPTIMIZE TABLE SsePacket;
    OPTIMIZE TABLE SsePacketBody;
    OPTIMIZE TABLE ContextConfig;
    OPTIMIZE TABLE ContextConfigKeyValue;
    OPTIMIZE TABLE ChannelAggregate;
    OPTIMIZE TABLE HeaderChannelAggregate;
    OPTIMIZE TABLE SseChannelAggregate;
    OPTIMIZE TABLE MonitorChannelAggregate;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS AnalyzeTables;

DELIMITER //

CREATE PROCEDURE AnalyzeTables()
BEGIN

    ANALYZE TABLE CfdpIndication;
    ANALYZE TABLE CfdpFileGeneration;
    ANALYZE TABLE CfdpFileUplinkFinished;
    ANALYZE TABLE CfdpRequestReceived;
    ANALYZE TABLE CfdpRequestResult;
    ANALYZE TABLE CfdpPduReceived;
    ANALYZE TABLE CfdpPduSent;
    ANALYZE TABLE ChannelData;
    ANALYZE TABLE CommandMessage;
    ANALYZE TABLE CommandStatus;
    ANALYZE TABLE EndSession;
    ANALYZE TABLE Evr;
    ANALYZE TABLE EvrMetadata;
    ANALYZE TABLE Frame;
    ANALYZE TABLE FrameBody;
    ANALYZE TABLE Host;
    ANALYZE TABLE LogMessage;
    ANALYZE TABLE Packet;
    ANALYZE TABLE PacketBody;
    ANALYZE TABLE Product;
    ANALYZE TABLE Session;
    ANALYZE TABLE SseEvr;
    ANALYZE TABLE SseEvrMetadata;
    ANALYZE TABLE SsePacket;
    ANALYZE TABLE SsePacketBody;
    ANALYZE TABLE ContextConfig;
    ANALYZE TABLE ContextConfigKeyValue;
    ANALYZE TABLE ChannelAggregate;
    ANALYZE TABLE HeaderChannelAggregate;
    ANALYZE TABLE SseChannelAggregate;
    ANALYZE TABLE MonitorChannelAggregate;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CopyTable;

DELIMITER //

CREATE PROCEDURE CopyTable(oldDb VARCHAR(256),
                           tble  VARCHAR(256))
BEGIN

    CALL IssueDynamic(CONCAT('DROP TABLE IF EXISTS ', tble));

    CALL IssueDynamic(CONCAT('CREATE TABLE ',
                             tble,
                             ' LIKE ',
                             oldDb,
                             '.',
                             tble));

    CALL IssueDynamic(CONCAT('INSERT INTO ',
                             tble,
                             ' SELECT * FROM ',
                             oldDb,
                             '.',
                             tble));
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CopyDatabase;

DELIMITER //

CREATE PROCEDURE CopyDatabase(oldDb VARCHAR(256))
BEGIN

    CALL CopyTable(oldDb, 'CfdpIndication');
    CALL CopyTable(oldDb, 'CfdpFileGeneration');
    CALL CopyTable(oldDb, 'CfdpFileUplinkFinished');
    CALL CopyTable(oldDb, 'CfdpRequestReceived');
    CALL CopyTable(oldDb, 'CfdpRequestResult');
    CALL CopyTable(oldDb, 'CfdpPduReceived');
    CALL CopyTable(oldDb, 'CfdpPduSent');
    CALL CopyTable(oldDb, 'ChannelData');
    CALL CopyTable(oldDb, 'CommandMessage');
    CALL CopyTable(oldDb, 'CommandStatus');
    CALL CopyTable(oldDb, 'EndSession');
    CALL CopyTable(oldDb, 'Evr');
    CALL CopyTable(oldDb, 'EvrMetadata');
    CALL CopyTable(oldDb, 'Frame');
    CALL CopyTable(oldDb, 'FrameBody');
    CALL CopyTable(oldDb, 'Host');
    CALL CopyTable(oldDb, 'LogMessage');
    CALL CopyTable(oldDb, 'Packet');
    CALL CopyTable(oldDb, 'PacketBody');
    CALL CopyTable(oldDb, 'Product');
    CALL CopyTable(oldDb, 'Session');
    CALL CopyTable(oldDb, 'SseEvr');
    CALL CopyTable(oldDb, 'SseEvrMetadata');
    CALL CopyTable(oldDb, 'SsePacket');
    CALL CopyTable(oldDb, 'SsePacketBody');
    CALL CopyTable(oldDb, 'ContextConfig');
    CALL CopyTable(oldDb, 'ContextConfigKeyValue');
    CALL CopyTable(oldDb, 'ChannelAggregate');
    CALL CopyTable(oldDb, 'HeaderChannelAggregate');
    CALL CopyTable(oldDb, 'SseChannelAggregate');
    CALL CopyTable(oldDb, 'MonitorChannelAggregate');

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckDatabase;

DELIMITER //

CREATE PROCEDURE CheckDatabase()
BEGIN

    DECLARE l_total int unsigned DEFAULT 0;
    DECLARE l_body  int unsigned DEFAULT 0;
    DECLARE l_join  int unsigned DEFAULT 0;

    -- Verify that there is exactly one distinct hostOffset

    SELECT COUNT(DISTINCT hostOffset) FROM Host INTO l_total;

    IF l_total != 1
    THEN
        SELECT CONCAT('Host offset not 1: ', l_total) AS error;
    END IF;

    -- Session versus Host

    SELECT COUNT(*) FROM Session INTO l_total;

    SELECT COUNT(*) FROM Session AS s STRAIGHT_JOIN Host AS h
        ON ((s.hostId=h.hostId) AND (s.host=h.hostName)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Session/Host disconnect: ', l_total - l_join) AS error;
    END IF;

    -- Session versus EndSession

    SELECT COUNT(*) FROM Session AS s STRAIGHT_JOIN EndSession AS e
        ON ((s.sessionId=e.sessionId) AND
            (s.hostId=e.hostId)       AND
            (s.sessionFragment=e.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Session/EndSession disconnect: ', l_total - l_join)
            AS error;
    END IF;


    -- ChannelAggregate versus Session

    SELECT SUM(count) FROM ChannelAggregate INTO l_total;

    SELECT SUM(count) FROM ChannelAggregate AS ca STRAIGHT_JOIN Session AS s
        ON ((ca.sessionId=s.sessionId) AND
            (ca.hostId=s.hostId)       AND
            (ca.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('ChannelAggregate/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;


    -- SseChannelAggregate versus Session

    SELECT SUM(count) FROM SseChannelAggregate INTO l_total;

    SELECT SUM(count) FROM SseChannelAggregate AS sca STRAIGHT_JOIN Session AS s
        ON ((sca.sessionId=s.sessionId) AND
            (sca.hostId=s.hostId)       AND
            (sca.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseChannelAggregate/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;


    -- MonitorChannelAggregate versus Session

    SELECT SUM(count) FROM MonitorChannelAggregate INTO l_total;

    SELECT SUM(count) FROM MonitorChannelAggregate AS ma STRAIGHT_JOIN Session AS s
        ON ((ma.sessionId=s.sessionId) AND
            (ma.hostId=s.hostId)       AND
            (ma.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('MonitorChannelAggregate/Session disconnect: ',
                      l_total - l_join) AS error;
    END IF;

    -- HeaderChannelAggregate versus Session

    SELECT SUM(count) FROM HeaderChannelAggregate INTO l_total;

    SELECT SUM(count) FROM HeaderChannelAggregate AS ha STRAIGHT_JOIN Session AS s
        ON ((ha.sessionId=s.sessionId) AND
            (ha.hostId=s.hostId)       AND
            (ha.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('HeaderChannelAggregate/Session disconnect: ',
                      l_total - l_join) AS error;
    END IF;


    -- Frame versus Session

    SELECT COUNT(*) FROM Frame     INTO l_total;
    SELECT COUNT(*) FROM FrameBody INTO l_body;

    SELECT COUNT(*) FROM Frame AS f STRAIGHT_JOIN Session AS s
        ON ((f.sessionId=s.sessionId) AND
            (f.hostId=s.hostId)       AND
            (f.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Frame/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- Frame versus FrameBody

    SELECT COUNT(*) FROM Frame AS f STRAIGHT_JOIN FrameBody AS fb
        ON ((f.sessionId       = fb.sessionId)       AND
            (f.hostId          = fb.hostId)          AND
            (f.sessionFragment = fb.sessionFragment) AND
            (f.id              = fb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('Frame disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('FrameBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- Packet versus Session

    SELECT COUNT(*) FROM Packet     INTO l_total;
    SELECT COUNT(*) FROM PacketBody INTO l_body;

    SELECT COUNT(*) FROM Packet AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Packet/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- Packet versus PacketBody

    SELECT COUNT(*) FROM Packet AS p STRAIGHT_JOIN PacketBody AS pb
        ON ((p.sessionId       = pb.sessionId)       AND
            (p.hostId          = pb.hostId)          AND
            (p.sessionFragment = pb.sessionFragment) AND
            (p.id              = pb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('Packet disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('PacketBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- SsePacket versus Session

    SELECT COUNT(*) FROM SsePacket     INTO l_total;
    SELECT COUNT(*) FROM SsePacketBody INTO l_body;

    SELECT COUNT(*) FROM SsePacket AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SsePacket/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SsePacket versus SsePacketBody

    SELECT COUNT(*) FROM SsePacket AS p STRAIGHT_JOIN SsePacketBody AS pb
        ON ((p.sessionId       = pb.sessionId)       AND
            (p.hostId          = pb.hostId)          AND
            (p.sessionFragment = pb.sessionFragment) AND
            (p.id              = pb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('SsePacket disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('SsePacketBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- Evr versus Session

    SELECT COUNT(*) FROM Evr INTO l_total;

    SELECT COUNT(*) FROM Evr AS e STRAIGHT_JOIN Session AS s
        ON ((e.sessionId=s.sessionId) AND
            (e.hostId=s.hostId)       AND
            (e.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Evr/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- EvrMetadata versus Evr

    SELECT COUNT(*) FROM EvrMetadata INTO l_total;

    SELECT COUNT(*) FROM Evr AS e STRAIGHT_JOIN EvrMetadata AS em
        ON ((e.sessionId       = em.sessionId)       AND
            (e.hostId          = em.hostId)          AND
            (e.sessionFragment = em.sessionFragment) AND
            (e.id              = em.id)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Evr/EvrMetadata disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SseEvr versus Session

    SELECT COUNT(*) FROM SseEvr INTO l_total;

    SELECT COUNT(*) FROM SseEvr AS e STRAIGHT_JOIN Session AS s
        ON ((e.sessionId=s.sessionId) AND
            (e.hostId=s.hostId)       AND
            (e.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseEvr/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SseEvrMetadata versus SseEvr

    SELECT COUNT(*) FROM SseEvrMetadata INTO l_total;

    SELECT COUNT(*) FROM SseEvr AS e STRAIGHT_JOIN SseEvrMetadata AS em
        ON ((e.sessionId       = em.sessionId)       AND
            (e.hostId          = em.hostId)          AND
            (e.sessionFragment = em.sessionFragment) AND
            (e.id              = em.id)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseEvr/SseEvrMetadata disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- LogMessage versus Session

    SELECT COUNT(*) FROM LogMessage INTO l_total;

    SELECT COUNT(*) FROM LogMessage AS lm STRAIGHT_JOIN Session AS s
        ON ((lm.sessionId=s.sessionId) AND
            (lm.hostId=s.hostId)       AND
            (lm.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('LogMessage/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- CommandMessage versus Session

    SELECT COUNT(*) FROM CommandMessage INTO l_total;

    SELECT COUNT(*) FROM CommandMessage AS cm STRAIGHT_JOIN Session AS s
        ON ((cm.sessionId=s.sessionId) AND
            (cm.hostId=s.hostId)       AND
            (cm.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('CommandMessage/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- CommandStatus versus CommandMessage

    SELECT COUNT(*) FROM CommandStatus INTO l_total;

    SELECT COUNT(*) FROM CommandMessage AS cm STRAIGHT_JOIN CommandStatus AS cs
        ON ((cm.sessionId       = cs.sessionId)       AND
            (cm.hostId          = cs.hostId)          AND
            (cm.sessionFragment = cs.sessionFragment) AND
            (cm.requestId       = cs.requestId)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('CommandMessage/CommandStatus disconnect: ',
                      l_total - l_join)
            AS error;
    END IF;

    -- Product versus Session

    SELECT COUNT(*) FROM Product INTO l_total;

    SELECT COUNT(*) FROM Product AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Product/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckTableCount;

DELIMITER //

CREATE PROCEDURE CheckTableCount(tble  VARCHAR(256),
                                 oldDb VARCHAR(256))
BEGIN

    CALL IssueDynamic(CONCAT('SELECT COUNT(*) FROM ',
                             tble,
                             ' INTO @l_total'));

    CALL IssueDynamic(CONCAT('SELECT COUNT(*) FROM ',
                             oldDb,
                             '.',
                             tble,
                             ' INTO @l_old'));
    IF @l_total != @l_old
    THEN
        SELECT CONCAT(tble, ' mismatch: ', @l_total, ' versus ', @l_old)
            AS error;
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckAggregateTableCount;

DELIMITER //

CREATE PROCEDURE CheckAggregateTableCount(tble  VARCHAR(256),
                                 oldDb VARCHAR(256))
BEGIN

    CALL IssueDynamic(CONCAT('SELECT SUM(count) FROM ',
                             tble,
                             ' INTO @l_total'));

    CALL IssueDynamic(CONCAT('SELECT SUM(count) FROM ',
                             oldDb,
                             '.',
                             tble,
                             ' INTO @l_old'));
    IF @l_total != @l_old
    THEN
        SELECT CONCAT(tble, ' mismatch: ', @l_total, ' versus ', @l_old)
            AS error;
    END IF;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckTableCounts;

DELIMITER //

CREATE PROCEDURE CheckTableCounts(oldDb VARCHAR(256))
BEGIN

    CALL CheckTableCount('CfdpIndication',      oldDb);
    CALL CheckTableCount('CfdpFileGeneration',  oldDb);
    CALL CheckTableCount('CfdpFileUplinkFinished', oldDb);
    CALL CheckTableCount('CfdpRequestReceived', oldDb);
    CALL CheckTableCount('CfdpRequestResult',   oldDb);
    CALL CheckTableCount('CfdpPduReceived',     oldDb);
    CALL CheckTableCount('CfdpPduSent',         oldDb);
    CALL CheckTableCount('ChannelData',         oldDb);
    CALL CheckTableCount('CommandMessage',      oldDb);
    CALL CheckTableCount('CommandStatus',       oldDb);
    CALL CheckTableCount('EndSession',          oldDb);
    CALL CheckTableCount('Evr',                 oldDb);
    CALL CheckTableCount('EvrMetadata',         oldDb);
    CALL CheckTableCount('Frame',               oldDb);
    CALL CheckTableCount('FrameBody',           oldDb);
    CALL CheckTableCount('Host',                oldDb);
    CALL CheckTableCount('LogMessage',          oldDb);
    CALL CheckTableCount('Packet',              oldDb);
    CALL CheckTableCount('PacketBody',          oldDb);
    CALL CheckTableCount('Product',             oldDb);
    CALL CheckTableCount('Session',             oldDb);
    CALL CheckTableCount('SseEvr',              oldDb);
    CALL CheckTableCount('SseEvrMetadata',      oldDb);
    CALL CheckTableCount('SsePacket',           oldDb);
    CALL CheckTableCount('SsePacketBody',       oldDb);
    CALL CheckTableCount('ContextConfig',oldDb);
    CALL CheckTableCount('ContextConfigKeyValue',oldDb);
    CALL CheckAggregateTableCount('ChannelAggregate',       oldDb);
    CALL CheckAggregateTableCount('HeaderChannelAggregate', oldDb);
    CALL CheckAggregateTableCount('SseChannelAggregate',    oldDb);
    CALL CheckAggregateTableCount('MonitorChannelAggregate',oldDb);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS AddTimeIndexes;

DELIMITER //

CREATE PROCEDURE AddTimeIndexes()
BEGIN

    ALTER TABLE ChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine),
        ADD INDEX beginScetIndex(beginScetCoarse),
        ADD INDEX endScetIndex(endScetCoarse),
        ADD INDEX beginSclkIndex(beginSclkCoarse),
        ADD INDEX endSclkIndex(endSclkCoarse);

    SHOW WARNINGS;

    ALTER TABLE Evr
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE Frame
        ADD INDEX ertIndex(ertCoarse, ertFine);

    SHOW WARNINGS;

    ALTER TABLE HeaderChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine);

    SHOW WARNINGS;

    ALTER TABLE LogMessage
        ADD INDEX eventTimeIndex(eventTimeCoarse, eventTimeFine);

    SHOW WARNINGS;

    ALTER TABLE MonitorChannelAggregate
        ADD INDEX beginMstIndex(beginMstCoarse, beginMstFine),
        ADD INDEX endMstIndex(endMstCoarse, endMstFine);

    SHOW WARNINGS;

    ALTER TABLE Packet
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE Product
        ADD INDEX creationTimeIndex(creationTimeCoarse, creationTimeFine),
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(dvtScetCoarse, dvtScetFine),
        ADD INDEX sclkIndex(dvtSclkCoarse, dvtSclkFine);

    SHOW WARNINGS;

    ALTER TABLE SseChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine),
        ADD INDEX beginScetIndex(beginScetCoarse),
        ADD INDEX endScetIndex(endScetCoarse),
        ADD INDEX beginSclkIndex(beginSclkCoarse),
        ADD INDEX endSclkIndex(endSclkCoarse);

    SHOW WARNINGS;

    ALTER TABLE SseEvr
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE SsePacket
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveTimeIndexes;

DELIMITER //

CREATE PROCEDURE RemoveTimeIndexes()
BEGIN

    ALTER TABLE ChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex,
        DROP INDEX beginScetIndex,
        DROP INDEX endScetIndex,
        DROP INDEX beginSclkIndex,
        DROP INDEX endSclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Evr
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Frame
        DROP INDEX ertIndex;

    SHOW WARNINGS;

    ALTER TABLE HeaderChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex;

    SHOW WARNINGS;

    ALTER TABLE LogMessage
        DROP INDEX eventTimeIndex;

    SHOW WARNINGS;

    ALTER TABLE MonitorChannelAggregate
        DROP INDEX beginMstIndex,
        DROP INDEX endMstIndex;

    SHOW WARNINGS;

    ALTER TABLE Packet
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Product
        DROP INDEX creationTimeIndex,
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SseChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex,
        DROP INDEX beginScetIndex,
        DROP INDEX endScetIndex,
        DROP INDEX beginSclkIndex,
        DROP INDEX endSclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SseEvr
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SsePacket
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveSession;

DELIMITER //

CREATE PROCEDURE RemoveSession(session   MEDIUMINT UNSIGNED,
                               hostIdent MEDIUMINT UNSIGNED)
BEGIN

    DELETE FROM Session    WHERE (sessionId=session) AND (hostId=hostIdent);
    DELETE FROM EndSession WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM ChannelData WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM CommandMessage WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM CommandStatus WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Evr WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM EvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Frame WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM FrameBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM LogMessage WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Packet WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM PacketBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Product WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseEvr WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseEvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SsePacket WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SsePacketBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM ChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM HeaderChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM MonitorChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveSubsession;

DELIMITER //

CREATE PROCEDURE RemoveSubsession(session    MEDIUMINT UNSIGNED,
                                  subsession TINYINT   UNSIGNED,
                                  hostIdent  MEDIUMINT UNSIGNED)
BEGIN

    DELETE FROM Session    WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);
    DELETE FROM EndSession WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM ChannelData WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                  (sessionFragment=subsession);

    DELETE FROM CommandMessage WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                     (sessionFragment=subsession);

    DELETE FROM CommandStatus WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                    (sessionFragment=subsession);

    DELETE FROM Evr WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM EvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                  (sessionFragment=subsession);

    DELETE FROM Frame WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM FrameBody WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM LogMessage WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM Packet WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM PacketBody WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM Product WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SseEvr WHERE (sessionId=session) AND (hostId=hostIdent) AND
                             (sessionFragment=subsession);

    DELETE FROM SseEvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                     (sessionFragment=subsession);

    DELETE FROM SsePacket WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SsePacketBody WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                    (sessionFragment=subsession);

    DELETE FROM ChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                   (sessionFragment=subsession);

    DELETE FROM HeaderChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);
    
    DELETE FROM MonitorChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SseChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                      (sessionFragment=subsession);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS ExamineCaches;

DELIMITER //

CREATE PROCEDURE ExamineCaches()
BEGIN

    SELECT @@GLOBAL.CA_CACHE.key_buffer_size;
    SELECT @@GLOBAL.CA_CACHE.key_cache_division_limit;

    SELECT @@GLOBAL.EVR_CACHE.key_buffer_size;
    SELECT @@GLOBAL.EVR_CACHE.key_cache_division_limit;

    SELECT @@GLOBAL.FRAME_CACHE.key_buffer_size;
    SELECT @@GLOBAL.FRAME_CACHE.key_cache_division_limit;

    SELECT @@GLOBAL.PACKET_CACHE.key_buffer_size;
    SELECT @@GLOBAL.PACKET_CACHE.key_cache_division_limit;

    SELECT @@GLOBAL.BODY_CACHE.key_buffer_size;
    SELECT @@GLOBAL.BODY_CACHE.key_cache_division_limit;

    SELECT @@GLOBAL.default.key_buffer_size;
    SELECT @@GLOBAL.default.key_cache_division_limit;

    SHOW GLOBAL VARIABLES LIKE 'key_buffer_size';
    SHOW GLOBAL VARIABLES LIKE 'key_cache_division_limit';

    SHOW STATUS LIKE 'key_blocks_used';
    SHOW STATUS LIKE 'key_reads';
    SHOW STATUS LIKE 'key_writes';
    SHOW STATUS LIKE 'key_read_requests';
    SHOW STATUS LIKE 'key_write_requests';

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CopyTableWithWhere;

DELIMITER //

CREATE PROCEDURE CopyTableWithWhere(oldDb VARCHAR(256),
                                    tble  VARCHAR(256),
                                    wher  VARCHAR(4096))
BEGIN

    CALL IssueDynamic(CONCAT(' ALTER TABLE ',
                             tble,
                             ' DISABLE KEYS'));

    CALL IssueDynamic(CONCAT('INSERT INTO ',
                             tble,
                             ' SELECT * FROM ',
                             oldDb,
                             '.',
                             tble,
                             ' WHERE ',
                             wher));

    CALL IssueDynamic(CONCAT(' ALTER TABLE ',
                             tble,
                             ' ENABLE KEYS'));
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckBodyLengths;

DELIMITER //

CREATE PROCEDURE CheckBodyLengths()
BEGIN

    SELECT p.sessionId, p.hostId, p.id, p.bodyLength,
           UNCOMPRESSED_LENGTH(pb.body) AS ulength
    FROM Packet AS p STRAIGHT_JOIN PacketBody AS pb
    ON
    (
        (p.hostId          = pb.hostId)          AND
        (p.sessionId       = pb.sessionId)       AND
        (p.sessionFragment = pb.sessionFragment) AND
        (p.id              = pb.id)
    )
    WHERE (p.bodyLength != UNCOMPRESSED_LENGTH(pb.body));

    SELECT sp.sessionId, sp.hostId, sp.id, sp.bodyLength,
           UNCOMPRESSED_LENGTH(spb.body) AS ulength
    FROM SsePacket AS sp STRAIGHT_JOIN SsePacketBody AS spb
    ON
    (
        (sp.hostId          = spb.hostId)          AND
        (sp.sessionId       = spb.sessionId)       AND
        (sp.sessionFragment = spb.sessionFragment) AND
        (sp.id              = spb.id)
    )
    WHERE (sp.bodyLength != UNCOMPRESSED_LENGTH(spb.body));

    SELECT f.sessionId, f.hostId, f.id, f.bodyLength,
           UNCOMPRESSED_LENGTH(fb.body) AS ulength
    FROM Frame AS f STRAIGHT_JOIN FrameBody AS fb
    ON
    (
        (f.hostId          = fb.hostId)          AND
        (f.sessionId       = fb.sessionId)       AND
        (f.sessionFragment = fb.sessionFragment) AND
        (f.id              = fb.id)
    )
    WHERE (f.bodyLength != UNCOMPRESSED_LENGTH(fb.body));

END //

DELIMITER ;


-- Validate the Host table as best we can
-- There's no good way to declare errors
-- MPCS-5073

DROP PROCEDURE IF EXISTS ValidateHostTable;

DELIMITER //

CREATE PROCEDURE ValidateHostTable()
BEGIN

    DECLARE l_total BIGINT UNSIGNED DEFAULT 0;

    SELECT COUNT(*) INTO l_total FROM Host;

    IF l_total = 0
    THEN
        CALL MY_SIGNAL("Host table is empty");
    END IF;

    SELECT COUNT(DISTINCT hostOffset) INTO l_total FROM Host;

    IF l_total > 1
    THEN
        CALL MY_SIGNAL("Host table has multiple host offsets");
    END IF;

    SELECT COUNT(*) INTO l_total FROM Host WHERE hostName = "";

    IF l_total > 0
    THEN
        CALL MY_SIGNAL("Host table has empty host names");
    END IF;

    SELECT COUNT(*) INTO l_total
        FROM Host AS h1 JOIN Host AS h2
        ON ((h1.hostName       =  h2.hostName) AND
            (h1.hostId         != h2.hostId)   AND
            ((h1.hostId >> 16) =  (h2.hostId >> 16)));

    IF l_total > 0
    THEN
        CALL MY_SIGNAL(CONCAT("Host table has multiple entries for the same ",
                              "host name at the same offset"));
    END IF;

END //

DELIMITER ;


-- Fix ChannelData.type of empty string, which could happen if
-- a TIME type was inserted.
-- Note: Make sure that ALL empty strings are from TIME type.

-- MPCS-7917

DROP PROCEDURE IF EXISTS FixChannelDataTimeType;

DELIMITER //

CREATE PROCEDURE FixChannelDataTimeType()
BEGIN

    UPDATE ChannelData SET type='TIME' WHERE type='';

END //

DELIMITER ;


-- Remove dead links, generally caused by not storing *Packets or Frames.
-- MPCS-7681

DROP PROCEDURE IF EXISTS RemoveDeadLinks;

DELIMITER //

CREATE PROCEDURE RemoveDeadLinks()
BEGIN

    UPDATE Packet AS p
           LEFT JOIN Frame AS f
           ON
           (
            (f.hostId          = p.hostId)          AND
            (f.sessionId       = p.sessionId)       AND
            (f.sessionFragment = p.sessionFragment) AND
            (f.id              = p.frameId)
           )
    SET p.frameId = NULL
    WHERE ((p.frameId IS NOT NULL) AND (f.id IS NULL));

    UPDATE Evr AS e
           LEFT JOIN Packet AS p
           ON
           (
            (p.hostId          = e.hostId)          AND
            (p.sessionId       = e.sessionId)       AND
            (p.sessionFragment = e.sessionFragment) AND
            (p.id              = e.packetId)
           )
    SET e.packetId = NULL
    WHERE ((e.packetId IS NOT NULL) AND (p.id IS NULL));

    UPDATE SseEvr AS e
           LEFT JOIN SsePacket AS p
           ON
           (
            (p.hostId          = e.hostId)          AND
            (p.sessionId       = e.sessionId)       AND
            (p.sessionFragment = e.sessionFragment) AND
            (p.id              = e.packetId)
           )
    SET e.packetId = NULL
    WHERE ((e.packetId IS NOT NULL) AND (p.id IS NULL));

END //

DELIMITER ;

WARNINGS;
