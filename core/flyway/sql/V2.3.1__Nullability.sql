ALTER TABLE `account` MODIFY `BROKER` enum('IB','JPM','DC','RBS','RT','LMAX','FXCM','CNX') NOT NULL;

ALTER TABLE `bar` MODIFY `FEED_TYPE` enum('IB','BB','DC','LMAX','FXCM','CNX') NOT NULL;

UPDATE `tick` SET `FEED_TYPE` = 'IB' WHERE ID <= 3;

ALTER TABLE `tick` MODIFY `FEED_TYPE` enum('IB','BB','DC','LMAX','FXCM','CNX') NOT NULL;

ALTER TABLE `exchange` MODIFY `TIME_ZONE` varchar(50) NOT NULL;

ALTER TABLE `generic_future_family` MODIFY  `EXPIRATION_DISTANCE` enum('MSEC_1','SEC_1','MIN_1','MIN_2','MIN_5','MIN_15','MIN_30','HOUR_1','HOUR_2','DAY_1','DAY_2','WEEK_1','WEEK_2','MONTH_1','MONTH_2','MONTH_3','MONTH_4','MONTH_5','MONTH_6','MONTH_7','MONTH_8','MONTH_9','MONTH_10','MONTH_11','MONTH_18','YEAR_1','YEAR_2') NOT NULL; 

ALTER TABLE `order_property` MODIFY `ORDER_FK` int(11) NOT NULL;

ALTER TABLE `order_status` MODIFY `SEQUENCE_NUMBER` bigint(20) NOT NULL;

ALTER TABLE `position` MODIFY  `REALIZED_P_L` double NOT NULL;