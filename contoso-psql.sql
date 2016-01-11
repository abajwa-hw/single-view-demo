CREATE TABLE FactStrategyPlan (
StrategyPlanKey int ,
Datekey timestamp ,  
EntityKey int ,  
ScenarioKey int,
AccountKey int,
CurrencyKey int,
ProductCategoryKey int,
Amount float,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
)
;
\copy FactStrategyPlan FROM '/tmp/data/FactStrategyPlan.csv' DELIMITER ',' CSV HEADER
select * from FactStrategyPlan limit 5;


CREATE TABLE FactSalesQuota (
SalesQuotaKey int ,
ChannelKey int ,  
StoreKey int ,  
ProductKey int,
DateKey timestamp,
CurrencyKey int,
ScenarioKey int,
SalesQuantityQuota float,
SalesAmountQuota float,
GrossMarginQuota float , 
ETLLoadID int,
LoadDate timestamp,
UpdateDate timestamp 
 )
 ;
\copy FactSalesQuota FROM '/tmp/data/FactSalesQuota.csv' DELIMITER ',' CSV HEADER
select * from FactSalesQuota limit 5;


CREATE TABLE FactSales (
SalesKey int ,
DateKey timestamp ,  
channelKey int ,  
StoreKey int,
ProductKey int,
PromotionKey int,
CurrencyKey int,
UnitCost float,
UnitPrice float,
SalesQuantity int , 
ReturnQuantity int,
ReturnAmount float,
DiscountQuantity int,
DiscountAmount float,
TotalCost float,
SalesAmount float,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy FactSales FROM '/tmp/data/FactSales.csv' DELIMITER ',' CSV HEADER
select * from FactSales limit 5;


CREATE TABLE FactOnlineSales (
OnlineSalesKey int ,
DateKey timestamp ,  
StoreKey int ,  
ProductKey int,
PromotionKey int,
CurrencyKey int,
CustomerKey int,
SalesOrderNumber varchar,
SalesOrderLineNumber int,
SalesQuantity int , 
SalesAmount float,
ReturnQuantity int,
ReturnAmount float,
DiscountQuantity int,
DiscountAmount float,
TotalCost float,
UnitCost float,
UnitPrice float,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy FactOnlineSales FROM '/tmp/data/FactOnlineSales.csv' DELIMITER ',' CSV HEADER
select * from FactOnlineSales limit 5;


CREATE TABLE FactITSLA (
ITSLAkey int ,
DateKey timestamp ,  
StoreKey int ,  
MachineKey int,
OutageKey int,
OutageStartTime timestamp,
OutageEndTime timestamp,
DownTime int,
ETLLoadID int,
LoadDate timestamp,
UpdateDate timestamp
 )
 ;
\copy FactITSLA FROM '/tmp/data/FactITSLA.csv' DELIMITER ',' CSV HEADER
select * from FactITSLA limit 5;

CREATE TABLE FactITMachine (
ITMachinekey int ,
MachineKey int,
DateKey timestamp ,  
CostAmount float,
CostType varchar,  
ETLLoadID int,
LoadDate timestamp,
UpdateDate timestamp
 )
 ;
\copy FactITMachine FROM '/tmp/data/FactITMachine.csv' DELIMITER ',' CSV HEADER
select * from FactITMachine limit 5;


CREATE TABLE FactInventory (
InventoryKey int ,
DateKey timestamp ,  
StoreKey int,
ProductKey int,
CurrencyKey int,
OnHandQuantity int , 
OnOrderQuantity int,
SafetyStockQuantity int,
UnitCost float,
DaysInStock int,
MinDayInStock int,
MaxDayInStock int,
Aging int,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy FactInventory FROM '/tmp/data/FactInventory.csv' DELIMITER ',' CSV HEADER
select * from FactInventory limit 5;


CREATE TABLE FactExchangeRate (
ExchangeRateKey int ,
CurrencyKey int,
DateKey timestamp ,  
AverageRate float, 
EndOfDayRate float,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy FactExchangeRate FROM '/tmp/data/FactExchangeRate.csv' DELIMITER ',' CSV HEADER
select * from FactExchangeRate limit 5;

	
CREATE TABLE DimStore (
StoreKey int,
GeographyKey int,
StoreManager int,
StoreType varchar,
StoreName varchar,
StoreDescription varchar,
Status varchar,
OpenDate timestamp ,  
CloseDate timestamp ,  
EntityKey int,
ZipCode varchar,
ZipCodeExtension varchar,
StorePhone varchar,
StoreFax varchar,
CloseReason varchar,
EmployeeCount int,
SellingAreaSize float,
LastRemodelDate timestamp ,  
ETLLoadID int,
SomeDate1 timestamp , 
SomeDate2 timestamp , 
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy DimStore FROM '/tmp/data/DimStore.csv' DELIMITER ',' CSV HEADER
select * from DimStore limit 5;



CREATE TABLE DimSalesTerritory (
SalesTerritoryKey int,
GeographyKey int,
SalesTerritoryLabel varchar,
SalesTerritoryName varchar,
SalesTerritoryRegion varchar,
SalesTerritoryCountry varchar,
SalesTerritoryGroup varchar,
SalesTerritoryLevel varchar,
SalesTerritoryManager int,
StartDate timestamp,
EndDate timestamp,
Status varchar,
ETLLoadID int,
LoadDate timestamp , 
UpdateDate timestamp 
 )
 ;
\copy DimSalesTerritory FROM '/tmp/data/DimSalesTerritory.csv' DELIMITER ',' CSV HEADER
select * from DimSalesTerritory limit 5;
