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
/*Table structure for table `check_transfer_master` */

DROP TABLE IF EXISTS `check_transfer_master`;

CREATE TABLE `check_transfer_master` (
  `sTransNox` char(12) NOT NULL,
  `sIndstCdx` char(4) DEFAULT NULL,
  `dTransact` date DEFAULT NULL,
  `sDestinat` char(4) DEFAULT NULL,
  `sDeptIDxx` char(4) DEFAULT NULL,
  `nEntryNox` smallint(6) DEFAULT NULL,
  `nTranTotl` decimal(14,4) DEFAULT NULL,
  `sRemarksx` varchar(256) DEFAULT NULL,
  `sPrepared` char(32) DEFAULT NULL,
  `dPrepared` datetime DEFAULT NULL,
  `cTranStat` char(1) DEFAULT NULL,
  `cPrintedx` char(1) DEFAULT NULL,
  `sReceived` char(32) DEFAULT NULL,
  `dReceived` datetime DEFAULT NULL,
  `sModified` char(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
