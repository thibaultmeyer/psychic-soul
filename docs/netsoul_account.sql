DROP TABLE IF EXISTS `ns_account`;

CREATE TABLE `ns_account` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`username` varchar(35) NOT NULL,
	`password` varchar(50) NOT NULL,
	`group` varchar(15) NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
