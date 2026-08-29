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
  `sTransNox` VARCHAR(12) NOT NULL,
  `nEntryNox` SMALLINT(6) NOT NULL,
  `sPrtclrID` VARCHAR(10) DEFAULT NULL,
  `sRecurrNo` VARCHAR(12) DEFAULT NULL,
  `sPRFRemxx` VARCHAR(256) DEFAULT NULL,
  `nAmountxx` DECIMAL(12,4) DEFAULT NULL,
  `nDiscount` DECIMAL(3,2) DEFAULT NULL,
  `nAddDiscx` DECIMAL(10,2) DEFAULT NULL,
  `cVATaxabl` CHAR(1) DEFAULT '0',
  `nTWithHld` DECIMAL(12,4) DEFAULT NULL,
  `cReversex` CHAR(1) DEFAULT NULL,
  `dModified` DATETIME DEFAULT NULL,
  `dTimeStmp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`,`nEntryNox`)
) ENGINE=INNODB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
