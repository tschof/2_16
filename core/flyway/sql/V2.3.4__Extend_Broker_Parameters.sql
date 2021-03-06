ALTER TABLE `broker_parameters` ADD `CONTRACT_SIZE` double DEFAULT NULL AFTER `SYMBOL_ROOT`;
ALTER TABLE `broker_parameters` ADD `SCALE` int(11) DEFAULT NULL AFTER `CONTRACT_SIZE`;
ALTER TABLE `broker_parameters` ADD `TICK_SIZE_PATTERN` varchar(100) DEFAULT NULL AFTER `SCALE`;
ALTER TABLE `broker_parameters` MODIFY `EXCHANGE_CODE` varchar(10) DEFAULT NULL AFTER `FEE`;
ALTER TABLE `broker_parameters` ADD `PRICE_MULTIPLIER` double DEFAULT NULL AFTER `EXCHANGE_CODE`;

ALTER TABLE `order` MODIFY `EXT_ID` varchar(50) DEFAULT NULL;

ALTER TABLE `order_status` MODIFY `EXT_ID` varchar(50) DEFAULT NULL;

ALTER TABLE `security_family` DROP TRADING_CLASS;

ALTER TABLE `transaction` MODIFY `EXT_ORDER_ID` varchar(50) DEFAULT NULL;
