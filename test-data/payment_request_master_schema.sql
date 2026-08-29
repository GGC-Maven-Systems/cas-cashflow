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
/*Table structure for table `payment_request_master` */

DROP TABLE IF EXISTS `payment_request_master`;

CREATE TABLE `payment_request_master` (
  `sTransNox` VARCHAR(12) NOT NULL,
  `sIndstCdx` VARCHAR(2) DEFAULT NULL,
  `sCompnyID` VARCHAR(4) DEFAULT NULL,
  `dTransact` DATE DEFAULT NULL,
  `sBranchCd` VARCHAR(4) DEFAULT NULL,
  `sDeptIDxx` VARCHAR(4) DEFAULT NULL,
  `sPayeeIDx` VARCHAR(10) DEFAULT NULL,
  `cSourcexx` CHAR(1) DEFAULT '0',
  `sSeriesNo` VARCHAR(10) DEFAULT NULL,
  `nTranTotl` DECIMAL(12,4) DEFAULT NULL,
  `sRemarksx` VARCHAR(256) DEFAULT NULL,
  `nDiscAmtx` DECIMAL(12,4) DEFAULT NULL,
  `nTaxAmntx` DECIMAL(12,4) DEFAULT NULL,
  `nNetTotal` DECIMAL(12,4) DEFAULT NULL,
  `nAmtPaidx` DECIMAL(12,4) DEFAULT NULL,
  `nEntryNox` SMALLINT(6) DEFAULT NULL,
  `sSourceCd` VARCHAR(4) DEFAULT NULL,
  `sSourceNo` VARCHAR(12) DEFAULT NULL,
  `cWithSOAx` CHAR(1) DEFAULT '0',
  `cProcessd` CHAR(1) DEFAULT '0',
  `cTranStat` CHAR(1) DEFAULT NULL,
  `sModified` VARCHAR(32) DEFAULT NULL,
  `dModified` DATETIME DEFAULT NULL,
  `dTimeStmp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`)
) ENGINE=INNODB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
