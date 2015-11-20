DROP TABLE IF EXISTS "property";
CREATE TABLE "property" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) NOT NULL,
  `PERSISTENT` bit(1) NOT NULL,
  `INT_VALUE` int(11) DEFAULT NULL,
  `DOUBLE_VALUE` double DEFAULT NULL,
  `MONEY_VALUE` decimal(15,6) DEFAULT NULL,
  `TEXT_VALUE` varchar(255) DEFAULT NULL,
  `DATE_TIME_VALUE` datetime DEFAULT NULL,
  `BOOLEAN_VALUE` bit(1) DEFAULT NULL,
  `PROPERTY_HOLDER_FK` bigint(20) NOT NULL,
  `VERSION` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`,`PROPERTY_HOLDER_FK`)
) ;


DROP TABLE IF EXISTS "account";
CREATE TABLE "account" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(20) NOT NULL,
  `ACTIVE` bit(1) NOT NULL,
  `BROKER` varchar(100) DEFAULT NULL,
  `ORDER_SERVICE_TYPE` varchar(100) NOT NULL,
  `SESSION_QUALIFIER` varchar(10) DEFAULT NULL,
  `EXT_ACCOUNT` varchar(20) DEFAULT NULL,
  `EXT_ACCOUNT_GROUP` varchar(20) DEFAULT NULL,
  `EXT_ALLOCATION_PROFILE` varchar(20) DEFAULT NULL,
  `EXT_CLEARING_ACCOUNT` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`),
  KEY (`ACTIVE`),
  KEY (`BROKER`),
  KEY (`ORDER_SERVICE_TYPE`)
) ;


DROP TABLE IF EXISTS "strategy";
CREATE TABLE "strategy" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(30) NOT NULL,
  `AUTO_ACTIVATE` bit(1) NOT NULL,
  `ALLOCATION` double NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`)
) ;


DROP TABLE IF EXISTS "exchange";
CREATE TABLE "exchange" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(50) NOT NULL,
  `CODE` varchar(10) DEFAULT NULL,
  `MIC` varchar(4) DEFAULT NULL,
  `BLOOMBERG_CODE` varchar(3) DEFAULT NULL,
  `IB_CODE` varchar(10) DEFAULT NULL,
  `TIME_ZONE` varchar(50) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`)
) ;


DROP TABLE IF EXISTS "trading_hours";
CREATE TABLE "trading_hours" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `OPEN` time NOT NULL,
  `CLOSE` time NOT NULL,
  `SUNDAY` bit(1) NOT NULL,
  `MONDAY` bit(1) NOT NULL,
  `TUESDAY` bit(1) NOT NULL,
  `WEDNESDAY` bit(1) NOT NULL,
  `THURSDAY` bit(1) NOT NULL,
  `FRIDAY` bit(1) NOT NULL,
  `SATURDAY` bit(1) NOT NULL,
  `EXCHANGE_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`EXCHANGE_FK`),
  CONSTRAINT `TRADING_HOURS_EXCHANGE_FKC` FOREIGN KEY (`EXCHANGE_FK`) REFERENCES "exchange" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "holiday";
CREATE TABLE "holiday" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE` date NOT NULL,
  `LATE_OPEN` time DEFAULT NULL,
  `EARLY_CLOSE` time DEFAULT NULL,
  `EXCHANGE_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`EXCHANGE_FK`),
  CONSTRAINT `HOLIDAY_EXCHANGE_FKC` FOREIGN KEY (`EXCHANGE_FK`) REFERENCES "exchange" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "security_family";
CREATE TABLE "security_family" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(20) NOT NULL,
  `SYMBOL_ROOT` varchar(20) DEFAULT NULL,
  `ISIN_ROOT` varchar(20) DEFAULT NULL,
  `RIC_ROOT` varchar(20) DEFAULT NULL,
  `CURRENCY` varchar(100) NOT NULL,
  `CONTRACT_SIZE` double NOT NULL,
  `SCALE` int(11) NOT NULL,
  `TICK_SIZE_PATTERN` varchar(100) NOT NULL,
  `EXECUTION_COMMISSION` decimal(5,2) DEFAULT NULL,
  `CLEARING_COMMISSION` decimal(5,2) DEFAULT NULL,
  `FEE` decimal(5,2) DEFAULT NULL,
  `TRADEABLE` bit(1) NOT NULL,
  `SYNTHETIC` bit(1) NOT NULL,
  `PERIODICITY` varchar(100) DEFAULT NULL,
  `MAX_GAP` int(11) DEFAULT NULL,
  `UNDERLYING_FK` bigint(20) DEFAULT NULL,
  `EXCHANGE_FK` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`),
  KEY (`UNDERLYING_FK`),
  KEY (`EXCHANGE_FK`),
  CONSTRAINT `SECURITY_FAMILY_EXCHANGE_FKC` FOREIGN KEY (`EXCHANGE_FK`) REFERENCES "exchange" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "bond_family";
CREATE TABLE "bond_family" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MATURITY_DISTANCE` varchar(100) NOT NULL,
  `LENGTH` int(11) NOT NULL,
  `QUOTATION_STYLE` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `BOND_FAMILYIFKC` FOREIGN KEY (`ID`) REFERENCES "security_family" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "future_family";
CREATE TABLE "future_family" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `INTREST` double NOT NULL,
  `DIVIDEND` double NOT NULL,
  `EXPIRATION_TYPE` varchar(100) NOT NULL,
  `EXPIRATION_DISTANCE` varchar(100) NOT NULL,
  `LENGTH` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `FUTURE_FAMILYIFKC` FOREIGN KEY (`ID`) REFERENCES "security_family" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "generic_future_family";
CREATE TABLE "generic_future_family" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `EXPIRATION_TYPE` varchar(100) NOT NULL,
  `EXPIRATION_DISTANCE` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `GENERIC_FUTURE_FAMILYIFKC` FOREIGN KEY (`ID`) REFERENCES "security_family" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "option_family";
CREATE TABLE "option_family" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `INTREST` double NOT NULL,
  `DIVIDEND` double NOT NULL,
  `EXPIRATION_TYPE` varchar(100) NOT NULL,
  `EXPIRATION_DISTANCE` varchar(100) NOT NULL,
  `STRIKE_DISTANCE` double NOT NULL,
  `WEEKLY` bit(1) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `OPTION_FAMILYIFKC` FOREIGN KEY (`ID`) REFERENCES "security_family" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "broker_parameters";
CREATE TABLE "broker_parameters" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BROKER` varchar(100) NOT NULL,
  `EXCHANGE_CODE` varchar(10) DEFAULT NULL,
  `SYMBOL_ROOT` varchar(20) DEFAULT NULL,
  `CONTRACT_SIZE` double DEFAULT NULL,
  `SCALE` int(11) DEFAULT NULL,
  `TICK_SIZE_PATTERN` varchar(100) DEFAULT NULL,
  `EXECUTION_COMMISSION` decimal(5,2) DEFAULT NULL,
  `CLEARING_COMMISSION` decimal(5,2) DEFAULT NULL,
  `FEE` decimal(5,2) DEFAULT NULL,
  `PRICE_MULTIPLIER` double DEFAULT NULL,
  `SECURITY_FAMILY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`BROKER`,`SECURITY_FAMILY_FK`),
  KEY (`SECURITY_FAMILY_FK`),
  CONSTRAINT `BROKER_PARAMETERS_SECURITY_FKC` FOREIGN KEY (`SECURITY_FAMILY_FK`) REFERENCES "security_family" (`ID`)
) ;


DROP TABLE IF EXISTS "security";
CREATE TABLE "security" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `SYMBOL` varchar(30) DEFAULT NULL,
  `DESCRIPTION` varchar(100) DEFAULT NULL,
  `ISIN` varchar(20) DEFAULT NULL,
  `BBGID` varchar(12) DEFAULT NULL,
  `RIC` varchar(20) DEFAULT NULL,
  `CONID` varchar(30) DEFAULT NULL,
  `LMAXID` varchar(30) DEFAULT NULL,
  `TTID` varchar(255) DEFAULT NULL,
  `UNDERLYING_FK` bigint(20) DEFAULT NULL,
  `SECURITY_FAMILY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`SYMBOL`,`SECURITY_FAMILY_FK`),
  UNIQUE KEY (`ISIN`),
  UNIQUE KEY (`BBGID`),
  UNIQUE KEY (`RIC`),
  UNIQUE KEY (`CONID`),
  UNIQUE KEY (`LMAXID`),
  UNIQUE KEY (`TTID`),
  KEY (`SECURITY_FAMILY_FK`),
  KEY (`UNDERLYING_FK`),
  CONSTRAINT `SECURITY_SECURITY_FAMILY_FKC` FOREIGN KEY (`SECURITY_FAMILY_FK`) REFERENCES "security_family" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `SECURITY_UNDERLYING_FKC` FOREIGN KEY (`UNDERLYING_FK`) REFERENCES "security" (`ID`) ON UPDATE CASCADE
) ;

ALTER TABLE "security_family"
  ADD CONSTRAINT `SECURITY_FAMILY_UNDERLAYING_FKC` FOREIGN KEY (`UNDERLYING_FK`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE;


DROP TABLE IF EXISTS "security_reference";
CREATE TABLE "security_reference" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(100) NOT NULL,
  `OWNER_FK` bigint(20) NOT NULL,
  `TARGET_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`OWNER_FK`),
  KEY (`TARGET_FK`),
  CONSTRAINT `SECURITY_REFERENCE_TARGET_FKC` FOREIGN KEY (`TARGET_FK`) REFERENCES "security" (`ID`),
  CONSTRAINT `SECURITY_REFERENCE_OWNER_FKC` FOREIGN KEY (`OWNER_FK`) REFERENCES "security" (`ID`)
) ;


DROP TABLE IF EXISTS "bond";
CREATE TABLE "bond" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MATURITY` date NOT NULL,
  `COUPON` double NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`MATURITY`),
  KEY (`ID`),
  CONSTRAINT `BONDIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "combination";
CREATE TABLE "combination" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `UUID` char(36) NOT NULL,
  `TYPE` varchar(100) NOT NULL,
  `PERSISTENT` bit(1) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `COMBINATIONIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "component";
CREATE TABLE "component" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `QUANTITY` bigint(20) NOT NULL,
  `PERSISTENT` bit(1) NOT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  `COMBINATION_FK` bigint(20) NOT NULL,
  `VERSION` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`SECURITY_FK`,`COMBINATION_FK`),
  KEY (`COMBINATION_FK`),
  KEY (`SECURITY_FK`),
  CONSTRAINT `COMPONENT_COMBINATION_FK` FOREIGN KEY (`COMBINATION_FK`) REFERENCES "combination" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `COMPONENT_SECURITY_FKC` FOREIGN KEY (`SECURITY_FK`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "commodity";
CREATE TABLE "commodity" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `COMMODITYIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`)
) ;


DROP TABLE IF EXISTS "forex";
CREATE TABLE "forex" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BASE_CURRENCY` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `FOREXIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "fund";
CREATE TABLE "fund" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `FUNDIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "future";
CREATE TABLE "future" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `EXPIRATION` date NOT NULL,
  `FIRST_NOTICE` date DEFAULT NULL,
  `LAST_TRADING` date DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY (`EXPIRATION`),
  KEY (`FIRST_NOTICE`),
  KEY (`LAST_TRADING`),
  KEY (`ID`),
  CONSTRAINT `FUTUREIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "generic_future";
CREATE TABLE "generic_future" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DURATION` varchar(100) NOT NULL,
  `ASSET_CLASS` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`DURATION`),
  KEY (`ID`),
  CONSTRAINT `GENERIC_FUTUREIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "index";
CREATE TABLE "index" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ASSET_CLASS` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `INDEXIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "intrest_rate";
CREATE TABLE "intrest_rate" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DURATION` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ID`),
  CONSTRAINT `INTREST_RATEIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "option";
CREATE TABLE "option" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `STRIKE` decimal(12,5) NOT NULL,
  `EXPIRATION` date NOT NULL,
  `TYPE` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`EXPIRATION`),
  KEY (`STRIKE`),
  KEY (`TYPE`),
  KEY (`ID`),
  CONSTRAINT `OPTIONIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "stock";
CREATE TABLE "stock" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `GICS` char(8) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY (`GICS`),
  KEY (`ID`),
  CONSTRAINT `STOCKIFKC` FOREIGN KEY (`ID`) REFERENCES "security" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "easy_to_borrow";
CREATE TABLE "easy_to_borrow" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE` date NOT NULL,
  `BROKER` varchar(100) NOT NULL,
  `QUANTITY` bigint(20) NOT NULL,
  `STOCK_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`DATE`,`BROKER`,`STOCK_FK`),
  KEY (`STOCK_FK`),
  CONSTRAINT `EASY_TO_BORROW_STOCK_FKC` FOREIGN KEY (`STOCK_FK`) REFERENCES "stock" (`ID`)
) ;


DROP TABLE IF EXISTS "order";
CREATE TABLE "order" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `class` varchar(255) NOT NULL,
  `INT_ID` varchar(30) NOT NULL,
  `EXT_ID` varchar(50) DEFAULT NULL,
  `DATE_TIME` datetime NOT NULL,
  `SIDE` varchar(100) NOT NULL,
  `QUANTITY` bigint(20) NOT NULL,
  `TIF` varchar(100) NOT NULL,
  `TIF_DATE_TIME` datetime DEFAULT NULL,
  `LIMIT` decimal(15,6) DEFAULT NULL,
  `STOP` decimal(15,6) DEFAULT NULL,
  `ACCOUNT_FK` bigint(20) NOT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  `EXCHANGE_FK` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ACCOUNT_FK`),
  KEY (`SECURITY_FK`),
  KEY (`STRATEGY_FK`),
  KEY (`EXCHANGE_FK`),
  CONSTRAINT `ORDER_EXCHANGE_FKC` FOREIGN KEY (`EXCHANGE_FK`) REFERENCES "exchange" (`ID`),
  CONSTRAINT `ORDER_ACCOUNT_FKC` FOREIGN KEY (`ACCOUNT_FK`) REFERENCES "account" (`ID`),
  CONSTRAINT `ORDER_SECURITY_FKC` FOREIGN KEY (`SECURITY_FK`) REFERENCES "security" (`ID`),
  CONSTRAINT `ORDER_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`)
) ;


DROP TABLE IF EXISTS "order_preference";
CREATE TABLE "order_preference" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(50) NOT NULL,
  `ORDER_TYPE` varchar(100) NOT NULL,
  `DEFAULT_ACCOUNT_FK` bigint(20) DEFAULT NULL,
  `VERSION` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`NAME`),
  KEY (`DEFAULT_ACCOUNT_FK`),
  CONSTRAINT `ORDER_PREFERENCE_DEFAULT_ACCOC` FOREIGN KEY (`DEFAULT_ACCOUNT_FK`) REFERENCES "account" (`ID`) ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "order_property";
CREATE TABLE "order_property" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(30) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  `TYPE` varchar(100) NOT NULL,
  `ORDER_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ORDER_FK`),
  CONSTRAINT `ORDER_PROPERTY_ORDER_FKC` FOREIGN KEY (`ORDER_FK`) REFERENCES "order" (`ID`)
) ;


DROP TABLE IF EXISTS "allocation";
CREATE TABLE "allocation" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `VALUE` double NOT NULL,
  `ORDER_PREFERENCE_FK` bigint(20) NOT NULL,
  `ACCOUNT_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`ORDER_PREFERENCE_FK`,`ACCOUNT_FK`),
  KEY (`ACCOUNT_FK`),
  KEY (`ORDER_PREFERENCE_FK`),
  CONSTRAINT `ALLOCATION_ACCOUNT_FKC` FOREIGN KEY (`ACCOUNT_FK`) REFERENCES "account" (`ID`),
  CONSTRAINT `ALLOCATION_ORDER_PREFERENCE_FKC` FOREIGN KEY (`ORDER_PREFERENCE_FK`) REFERENCES "order_preference" (`ID`)
) ;


DROP TABLE IF EXISTS "order_status";
CREATE TABLE "order_status" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_TIME` datetime NOT NULL,
  `EXT_DATE_TIME` datetime NOT NULL,
  `STATUS` varchar(100) NOT NULL,
  `FILLED_QUANTITY` bigint(20) NOT NULL,
  `REMAINING_QUANTITY` bigint(20) NOT NULL,
  `AVG_PRICE` decimal(15,6) DEFAULT NULL,
  `LAST_PRICE` decimal(15,6) DEFAULT NULL,
  `INT_ID` varchar(30) DEFAULT NULL,
  `EXT_ID` varchar(50) DEFAULT NULL,
  `SEQUENCE_NUMBER` bigint(20) NOT NULL,
  `REASON` varchar(255) DEFAULT NULL,
  `ORDER_FK` bigint(20) NOT NULL,
  `LAST_QUANTITY` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY (`ORDER_FK`),
  CONSTRAINT `ORDER_STATUS_ORDER_FKC` FOREIGN KEY (`ORDER_FK`) REFERENCES "order" (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "portfolio_value";
CREATE TABLE "portfolio_value" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_TIME` datetime NOT NULL,
  `SECURITIES_CURRENT_VALUE` decimal(15,6) NOT NULL,
  `CASH_BALANCE` decimal(15,6) NOT NULL,
  `NET_LIQ_VALUE` decimal(15,6) NOT NULL,
  `LEVERAGE` double NOT NULL,
  `ALLOCATION` double NOT NULL,
  `CASH_FLOW` decimal(15,6) DEFAULT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`DATE_TIME`,`STRATEGY_FK`),
  KEY (`STRATEGY_FK`),
  CONSTRAINT `PORTFOLIO_VALUE_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "position";
CREATE TABLE "position" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `QUANTITY` bigint(20) NOT NULL,
  `COST` double NOT NULL,
  `REALIZED_P_L` double NOT NULL,
  `PERSISTENT` bit(1) NOT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`SECURITY_FK`,`STRATEGY_FK`),
  KEY (`SECURITY_FK`),
  KEY (`STRATEGY_FK`),
  CONSTRAINT `POSITION_SECURITY_FKC` FOREIGN KEY (`SECURITY_FK`) REFERENCES "security" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `POSITION_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "subscription";
CREATE TABLE "subscription" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `SECURITY_FK` bigint(20) NOT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  `FEED_TYPE` varchar(100) NOT NULL,
  `PERSISTENT` bit(1) NOT NULL,
  `VERSION` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`SECURITY_FK`,`STRATEGY_FK`,`FEED_TYPE`),
  KEY (`PERSISTENT`),
  KEY (`SECURITY_FK`),
  KEY (`STRATEGY_FK`),
  KEY (`FEED_TYPE`),
  CONSTRAINT `SUBSCRIPTION_SECURITY_FKC` FOREIGN KEY (`SECURITY_FK`) REFERENCES "security" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `SUBSCRIPTION_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "transaction";
CREATE TABLE "transaction" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `UUID` char(36) NOT NULL,
  `EXT_ID` varchar(100) DEFAULT NULL,
  `INT_ORDER_ID` varchar(30) DEFAULT NULL,
  `EXT_ORDER_ID` varchar(50) DEFAULT NULL,
  `DATE_TIME` datetime NOT NULL,
  `SETTLEMENT_DATE` date DEFAULT NULL,
  `QUANTITY` bigint(20) NOT NULL,
  `PRICE` decimal(15,6) NOT NULL,
  `EXECUTION_COMMISSION` decimal(15,2) DEFAULT NULL,
  `CLEARING_COMMISSION` decimal(15,2) DEFAULT NULL,
  `FEE` decimal(15,2) DEFAULT NULL,
  `CURRENCY` varchar(100) NOT NULL,
  `TYPE` varchar(100) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `SECURITY_FK` bigint(20) DEFAULT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  `POSITION_FK` bigint(20) DEFAULT NULL,
  `ACCOUNT_FK` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`EXT_ID`),
  KEY (`DATE_TIME`),
  KEY (`SETTLEMENT_DATE`),
  KEY (`CURRENCY`),
  KEY (`STRATEGY_FK`),
  KEY (`POSITION_FK`),
  KEY (`SECURITY_FK`),
  KEY (`ACCOUNT_FK`),
  CONSTRAINT `TRANSACTION_ACCOUNT_FKC` FOREIGN KEY (`ACCOUNT_FK`) REFERENCES "account" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `TRANSACTION_POSITION_FKC` FOREIGN KEY (`POSITION_FK`) REFERENCES "position" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `TRANSACTION_SECURITY_FKC` FOREIGN KEY (`SECURITY_FK`) REFERENCES "security" (`ID`) ON UPDATE CASCADE,
  CONSTRAINT `TRANSACTION_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;

DROP TABLE IF EXISTS "cash_balance";
CREATE TABLE "cash_balance" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CURRENCY` varchar(100) NOT NULL,
  `AMOUNT` decimal(15,2) NOT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  `VERSION` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`CURRENCY`,`STRATEGY_FK`),
  KEY (`STRATEGY_FK`),
  CONSTRAINT `CASH_BALANCE_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;


DROP TABLE IF EXISTS "tick";
CREATE TABLE "tick" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_TIME` datetime NOT NULL,
  `LAST` decimal(13,6) DEFAULT NULL,
  `LAST_DATE_TIME` datetime DEFAULT NULL,
  `VOL` int(11) NOT NULL,
  `VOL_BID` int(11) NOT NULL,
  `VOL_ASK` int(11) NOT NULL,
  `BID` decimal(13,6) DEFAULT NULL,
  `ASK` decimal(13,6) DEFAULT NULL,
  `FEED_TYPE` varchar(100) DEFAULT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`DATE_TIME`,`SECURITY_FK`,`FEED_TYPE`),
  KEY (`DATE_TIME`),
  KEY (`SECURITY_FK`),
  KEY (`FEED_TYPE`)
) ;


DROP TABLE IF EXISTS "bar";
CREATE TABLE "bar" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_TIME` datetime NOT NULL,
  `OPEN` decimal(13,6) NOT NULL,
  `HIGH` decimal(13,6) NOT NULL,
  `LOW` decimal(13,6) NOT NULL,
  `CLOSE` decimal(13,6) NOT NULL,
  `VOL` int(11) NOT NULL,
  `BAR_SIZE` varchar(100) NOT NULL,
  `FEED_TYPE` varchar(100) DEFAULT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`DATE_TIME`,`SECURITY_FK`,`BAR_SIZE`),
  KEY (`DATE_TIME`),
  KEY (`SECURITY_FK`),
  KEY (`BAR_SIZE`)
) ;


DROP TABLE IF EXISTS "generic_tick";
CREATE TABLE "generic_tick" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_TIME` datetime NOT NULL,
  `FEED_TYPE` varchar(100) NOT NULL,
  `SECURITY_FK` bigint(20) NOT NULL,
  `TICK_TYPE` varchar(100) NOT NULL,
  `MONEY_VALUE` decimal(13,6) DEFAULT NULL,
  `DOUBLE_VALUE` double DEFAULT NULL,
  `INT_VALUE` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY (`SECURITY_FK`)
) ;


DROP TABLE IF EXISTS "measurement";
CREATE TABLE "measurement" (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) NOT NULL,
  `DATE_TIME` datetime NOT NULL,
  `INT_VALUE` int(11) DEFAULT NULL,
  `DOUBLE_VALUE` double DEFAULT NULL,
  `MONEY_VALUE` decimal(15,6) DEFAULT NULL,
  `TEXT_VALUE` varchar(50) DEFAULT NULL,
  `BOOLEAN_VALUE` bit(1) DEFAULT NULL,
  `STRATEGY_FK` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`DATE_TIME`,`NAME`,`STRATEGY_FK`),
  KEY (`STRATEGY_FK`),
  CONSTRAINT `MEASUREMENT_STRATEGY_FKC` FOREIGN KEY (`STRATEGY_FK`) REFERENCES "strategy" (`ID`) ON UPDATE CASCADE
) ;


INSERT INTO "strategy" (`ID`, `NAME`, `AUTO_ACTIVATE`, `ALLOCATION`, `VERSION`) VALUES (1,'SERVER','1',0,0);

INSERT INTO "cash_balance" (`ID`, `CURRENCY`, `AMOUNT`, `STRATEGY_FK`, `VERSION`) VALUES (1,'USD',1000000.00,1,1);

INSERT INTO "transaction" (`ID`, `UUID`, `EXT_ID`, `INT_ORDER_ID`, `EXT_ORDER_ID`, `DATE_TIME`, `SETTLEMENT_DATE`, `QUANTITY`, `PRICE`, `EXECUTION_COMMISSION`, `CLEARING_COMMISSION`, `FEE`, `CURRENCY`, `TYPE`, `DESCRIPTION`, `SECURITY_FK`, `STRATEGY_FK`, `POSITION_FK`, `ACCOUNT_FK`) VALUES (1,'',NULL,NULL,NULL,'2012-08-16 00:00:00',NULL,1,1000000.000000,NULL,NULL,NULL,'USD','CREDIT',NULL,NULL,1,NULL,NULL);

INSERT INTO "account"(ID, NAME, ACTIVE, BROKER, ORDER_SERVICE_TYPE) VALUES (1, 'TEST_ACCOUNT', True, 'IB', 'IB_NATIVE');

INSERT INTO "order_preference" (ID, NAME, ORDER_TYPE, DEFAULT_ACCOUNT_FK, VERSION) VALUES (200, 'DEFAULT', 'MARKET', 1, 0);
