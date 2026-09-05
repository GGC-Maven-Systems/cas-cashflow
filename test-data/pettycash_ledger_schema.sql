/*
SQLyog Ultimate v8.55 
MySQL - 5.7.44-log : Database - gcasys_dbf
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*Table structure for table `pettycash_ledger` */

DROP TABLE IF EXISTS `pettycash_ledger`;

CREATE TABLE `pettycash_ledger` (
  `sPettyIDx` VARCHAR(7) NOT NULL,
  `nLedgerNo` SMALLINT(6) UNSIGNED DEFAULT NULL,
  `sSourceCD` VARCHAR(4) NOT NULL,
  `sSourceNo` VARCHAR(12) NOT NULL,
  `dTransact` DATE DEFAULT NULL,
  `nDebtAmtx` DECIMAL(10,2) DEFAULT NULL,
  `nCrdtAmtx` DECIMAL(10,2) DEFAULT NULL,
  `sBatchNox` VARCHAR(12) DEFAULT NULL,
  `cReversex` CHAR(1) DEFAULT NULL,
  `dModified` DATETIME DEFAULT NULL,
  `dTimeStmp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sPettyIDx`,`sSourceCD`,`sSourceNo`)
) ENGINE=INNODB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
