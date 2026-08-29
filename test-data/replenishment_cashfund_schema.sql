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
/*Table structure for table `cashfund` */

DROP TABLE IF EXISTS `cashfund`;

CREATE TABLE `cashfund` (
  `sCashFIDx` varchar(15) NOT NULL,
  `sBranchCD` varchar(4) DEFAULT NULL,
  `sDeptIDxx` varchar(3) DEFAULT NULL,
  `sCompnyID` varchar(4) DEFAULT NULL,
  `sIndstCdx` varchar(4) DEFAULT NULL,
  `sCashFDsc` varchar(70) DEFAULT NULL,
  `nBalancex` decimal(10,2) DEFAULT NULL,
  `nBegBalxx` decimal(10,2) DEFAULT NULL,
  `dBegDatex` date DEFAULT NULL,
  `sCashFMgr` varchar(12) DEFAULT NULL,
  `nLedgerNo` smallint(6) DEFAULT NULL,
  `dLastTran` date DEFAULT NULL,
  `cTranStat` char(1) DEFAULT NULL,
  `sModified` varchar(10) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sCashFIDx`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
