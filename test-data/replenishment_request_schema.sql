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
/*Table structure for table `replenishment_request` */

DROP TABLE IF EXISTS `replenishment_request`;

CREATE TABLE `replenishment_request` (
  `sTransNox` VARCHAR(12) NOT NULL,
  `dTransact` DATE DEFAULT NULL,
  `cFundType` CHAR(1) DEFAULT NULL,
  `sFundIdxx` VARCHAR(15) DEFAULT NULL,
  `sRemarksx` VARCHAR(120) DEFAULT NULL,
  `nTranAmtx` DECIMAL(12,4) DEFAULT NULL,
  `cTranStat` CHAR(1) DEFAULT NULL,
  `sModified` VARCHAR(10) DEFAULT NULL,
  `dModified` DATETIME DEFAULT NULL,
  `dTimeStmp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`)
) ENGINE=INNODB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
