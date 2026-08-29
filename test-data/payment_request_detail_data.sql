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
/*Table structure for table `payment_request_detail` */

DROP TABLE IF EXISTS `payment_request_detail`;

CREATE TABLE `payment_request_detail` (
  `sTransNox` varchar(12) NOT NULL,
  `nEntryNox` smallint(6) NOT NULL,
  `sPrtclrID` varchar(10) DEFAULT NULL,
  `sRecurrNo` varchar(12) DEFAULT NULL,
  `sPRFRemxx` varchar(256) DEFAULT NULL,
  `nAmountxx` decimal(12,4) DEFAULT NULL,
  `nDiscount` decimal(3,2) DEFAULT NULL,
  `nAddDiscx` decimal(10,2) DEFAULT NULL,
  `cVATaxabl` char(1) DEFAULT '0',
  `nTWithHld` decimal(12,4) DEFAULT NULL,
  `cReversex` char(1) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`,`nEntryNox`),
  KEY `sPrtclrID` (`sPrtclrID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
