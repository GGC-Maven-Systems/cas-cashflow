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
/*Table structure for table `pettycash` */

DROP TABLE IF EXISTS `pettycash`;

CREATE TABLE `pettycash` (
  `sPettyIDx` VARCHAR(7) NOT NULL,
  `sBranchCD` VARCHAR(4) NOT NULL,
  `sDeptIDxx` VARCHAR(3) NOT NULL,
  `sCompnyID` VARCHAR(4) DEFAULT NULL,
  `sIndstCdx` VARCHAR(4) DEFAULT NULL,
  `sPettyDsc` VARCHAR(70) DEFAULT NULL,
  `nBalancex` DECIMAL(10,2) DEFAULT NULL,
  `nBegBalxx` DECIMAL(10,2) DEFAULT NULL,
  `dBegDatex` DATE DEFAULT NULL,
  `sPettyMgr` VARCHAR(12) DEFAULT NULL,
  `nLedgerNo` SMALLINT(6) DEFAULT NULL,
  `dLastTran` DATE DEFAULT NULL,
  `cTranStat` CHAR(1) DEFAULT NULL,
  `sModified` VARCHAR(32) DEFAULT NULL,
  `dModified` DATETIME DEFAULT NULL,
  `dTimeStmp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sPettyIDx`)
) ENGINE=INNODB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
