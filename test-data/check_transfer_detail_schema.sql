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
/*Table structure for table `check_transfer_detail` */

DROP TABLE IF EXISTS `check_transfer_detail`;

CREATE TABLE `check_transfer_detail` (
  `sTransNox` varchar(12) NOT NULL,
  `nEntryNox` smallint(6) NOT NULL,
  `sSourceCd` varchar(4) DEFAULT NULL,
  `sSourceNo` varchar(12) DEFAULT NULL,
  `sPayloadx` varchar(512) DEFAULT NULL,
  `cReceived` char(1) DEFAULT '0',
  `sRemarksx` varchar(256) DEFAULT NULL,
  `cReversex` char(1) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`,`nEntryNox`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
