/*
SQLyog Enterprise - MySQL GUI v8.05 RC 
MySQL - 5.7.44-log : Database - gcasys_dbf
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `department` */

CREATE TABLE `department` (
  `sDeptIDxx` varchar(4) NOT NULL,
  `sDeptName` varchar(30) DEFAULT NULL,
  `sDeptHead` varchar(8) DEFAULT NULL,
  `sMobileNo` varchar(11) DEFAULT NULL,
  `sEMailAdd` varchar(50) DEFAULT NULL,
  `sMainIDxx` varchar(4) DEFAULT NULL,
  `sDeptCode` varchar(5) DEFAULT NULL,
  `sHAssgnID` varchar(12) DEFAULT NULL,
  `sSAssgnID` varchar(12) DEFAULT NULL,
  `sGenMgrID` varchar(12) DEFAULT NULL,
  `cEntLevel` char(1) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT NULL,
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sDeptIDxx`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;