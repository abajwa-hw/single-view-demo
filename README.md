## Hive streaming demo
This demo is part of a 'Interactive Query with Apache Hive' webinar.

The webinar recording and slides are available at http://hortonworks.com/partners/learn

#### Demo overview

1. Sqoop - import personal data of users from MySql into Hive ORC table 
2. Flume - import browsing history of users into Hive ORC table ie userid,webpage,timestamp
3. Storm - import tweets for those 400 users into Hive ORC table 
4. Run Hive queries

##### Setup demo on HDP 2.2 sandbox VM 

- Download HDP 2.2 sandbox VM image (Sandbox_HDP_2.2_VMware.ova) from [Hortonworks website](http://hortonworks.com/products/hortonworks-sandbox/)
- Import Sandbox_HDP_2.2_VMware.ova into VMWare
- Now start the VM
- After it boots up, find the IP address of the VM and add an entry into your machines hosts file e.g.
```
192.168.191.241 sandbox.hortonworks.com sandbox    
```
- Connect to the VM via SSH (password hadoop) and start Ambari server
```
ssh root@sandbox.hortonworks.com
/root/start_ambari.sh
```
After bringing up Ambari, also make the below config changes to Hive and YARN

  - Under Hive config, increase memory settings: 
```
hive.heapsize=1024 
hive.tez.container.size=1024
hive.tez.java.opts=-Xmx820m
```
  - Under Hive config, turn on Hive txns (only worker threads property needs to be changed in 2.2 sandbox):
```
hive.support.concurrency=true
hive.txn.manager=org.apache.hadoop.hive.ql.lockmgr.DbTxnManager
hive.compactor.initiator.on=true
hive.compactor.worker.threads=2
hive.enforce.bucketing=true
hive.exec.dynamic.partition.mode=nonstrict
```
  - Under YARN config, increase YARN memory settings:
```
yarn.nodemanager.resource.memory-mb=4096
yarn.scheduler.minimum-allocation-mb=1024
yarn.scheduler.maximum-allocation-mb=4096
```
  - Under YARN config, define queues - change these two existing properties:
```
yarn.scheduler.capacity.root.default.capacity=50
yarn.scheduler.capacity.root.queues=default,hiveserver	
```
  - Under YARN config, define sub-queues - add below new properties:
```
yarn.scheduler.capacity.root.hiveserver.capacity=50
yarn.scheduler.capacity.root.hiveserver.hive1.capacity=50
yarn.scheduler.capacity.root.hiveserver.hive1.user-limit-factor=4
yarn.scheduler.capacity.root.hiveserver.hive2.capacity=50
yarn.scheduler.capacity.root.hiveserver.hive2.user-limit-factor=4
yarn.scheduler.capacity.root.hiveserver.queues=hive1,hive2
```
  - Under YARN config, enable preemption:
```
yarn.resourcemanager.scheduler.monitor.enable=true
yarn.resourcemanager.scheduler.monitor.policies=org.apache.hadoop.yarn.server.resourcemanager.monitor.capacity.ProportionalCapacityPreemptionPolicy
yarn.resourcemanager.monitor.capacity.preemption.monitoring_interval=1000
yarn.resourcemanager.monitor.capacity.preemption.max_wait_before_kill=5000
yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round=0.4
```
  - Under Hive config, enable Tez and sessions (few of these already set in 2.2)
```
hive.execution.engine=tez
hive.server2.tez.initialize.default.sessions=true
hive.server2.tez.default.queues=hive1,hive2
hive.server2.tez.sessions.per.default.queue=1
hive.server2.enable.doAs=false
hive.vectorized.groupby.maxentries=10240
hive.vectorized.groupby.flush.percent=0.1
```


##### Step 1 - Import data from MySQL to Hive ORC table via Sqoop 
- FTP over PII_data_small.csv.zip and unzip it
```
unzip ~/PII_data_small.csv.zip
```
- Import users personal data into MySQL
```
mysql -u root -p
#empty password

create database people;
use people;
create table persons (people_id INT PRIMARY KEY, sex VARCHAR(10), bdate DATE, firstname VARCHAR(50), lastname VARCHAR(50), addresslineone VARCHAR(150), addresslinetwo VARCHAR(150), city VARCHAR(100), postalcode VARCHAR(10), ssn VARCHAR(100), id2 VARCHAR(100), email VARCHAR(150), id3 VARCHAR(150));
LOAD DATA LOCAL INFILE '~/PII_data_small.csv' REPLACE INTO TABLE persons FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n';
select people_id, firstname, lastname, city from persons where lastname='SMITH';
```

- Make Sqoop use newer version of mysql connector. This is a workaround for [SQOOP-1400](https://issues.apache.org/jira/browse/SQOOP-1400)
```
cp /usr/hdp/2.2.0.0-2041/ranger-admin/ews/webapp/WEB-INF/lib/mysql-connector-java-5.1.31.jar /usr/share/java/
rm -f /usr/share/java/mysql-connector-java.jar
ln -s /usr/share/java/mysql-connector-java-5.1.31.jar /usr/share/java/mysql-connector-java.jar
ls -la /usr/share/java/my*
```

- Notice in HCat there is no persons table
http://sandbox.hortonworks.com:8000/hcatalog/

- Import data from MySQL to Hive ORC table using Sqoop
```
sqoop import --verbose --connect 'jdbc:mysql://localhost/people' --table persons --username root --hcatalog-table persons --hcatalog-storage-stanza "stored as orc" -m 1 --create-hcatalog-table --fetch-size -2147483648
```

- Now notice persons table created and has 999,396 records

- Open the table in Hive and click view file location and then on part-m-00000
http://sandbox.hortonworks.com:8000/beeswax/table/default/persons

- Notice the table is stored in ORC format
http://sandbox.hortonworks.com:8000/filebrowser/view//apps/hive/warehouse/persons/part-m-00000

- Compare the contents of sample_07 which is stored in text format
http://sandbox.hortonworks.com:8000/filebrowser/view//apps/hive/warehouse/sample_07/sample_07

##### Step 2 - Import web history data from log file to Hive ORC table via Flume 

- Create table test allowing transactions and partition into day month year (flume interseptor adds timestamp header to payload then specific hiveout.hive.partition)
````
create table if not exists test (id int, val string) partitioned by (year string,month string,day string) clustered by (id) into 32 buckets stored as orc TBLPROPERTIES ("transactional"="true");
````
- Create dummy web traffic log
```
vi /tmp/test.txt
6856266,http://www.google.com,2014,12,29
18426858,http://www.yahoo.com,2014,12,29
21612169,http://www.hortonworks.com,2014,12,29
```

- In Ambari > Flume > Config > flume.conf
```

## Flume NG Apache Log Collection
## Refer to https://cwiki.apache.org/confluence/display/FLUME/Getting+Started
##

agent.sources = webserver
agent.sources.webserver.type = exec
agent.sources.webserver.command = tail -F /tmp/test.txt
agent.sources.webserver.batchSize = 1
agent.sources.webserver.channels = memoryChannel
agent.sources.webserver.interceptors = intercepttime
agent.sources.webserver.interceptors.intercepttime.type = timestamp

## Channels ########################################################
agent.channels = memoryChannel
agent.channels.memoryChannel.type = memory
agent.channels.memoryChannel.capacity = 10000

## Sinks ###########################################################

agent.sinks = hiveout
agent.sinks.hiveout.type = hive
agent.sinks.hiveout.hive.metastore=thrift://localhost:9083
agent.sinks.hiveout.hive.database=default
agent.sinks.hiveout.hive.table=test
agent.sinks.hiveout.hive.partition=%Y,%m,%d
agent.sinks.hiveout.serializer = DELIMITED
agent.sinks.hiveout.serializer.fieldnames =id,val
agent.sinks.hiveout.channel = memoryChannel
```

- Now notice test table now has records created
http://sandbox.hortonworks.com:8000/beeswax/table/default/test

- Notice the table is stored in ORC format
http://sandbox.hortonworks.com:8000/filebrowser/view//apps/hive/warehouse/test/year=2014/month=12/day=29/delta_0000001_0000100/bucket_00017
