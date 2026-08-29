DROP TABLE IF EXISTS company;

CREATE TABLE company (
    sCompnyID CHAR(4) NOT NULL,
    sCompnyNm CHAR(64),
    sCompnyCd CHAR(8),
    sAddressx CHAR(128),
    sTownIDxx CHAR(4),
    sTaxIDNox CHAR(16),
    sEmplyrNo CHAR(16),
    cRecdStat CHAR(1) DEFAULT '1',
    sModified CHAR(32),
    dModified TIMESTAMP,
    dTimeStmp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sCompnyID)
);

