## Hive streaming workshop
This demo is part of a 'Interactive Query with Apache Hive' webinar.

The webinar recording and slides are available at http://hortonworks.com/partners/learn/#hive

Instructions for HDP 2.2 can be found [here](https://github.com/abajwa-hw/hdp22-hive-streaming/blob/master/README-22.md)

#### Demo overview

1. [Start HDP 2.3 sandbox and enable Hive features like transactions, queues, preemption, Tez and sessions](https://github.com/abajwa-hw/hdp22-hive-streaming#part-1---start-sandbox-vm-and-enable-hive-features)
2. [Sqoop - import PII data of users from MySql into Hive ORC table](https://github.com/abajwa-hw/hdp22-hive-streaming#part-2---import-data-from-mysql-to-hive-orc-table-via-sqoop)
3. [Flume - import browsing history of users e.g. userid,webpage,timestamp from simulated weblogs into Hive ORC table](https://github.com/abajwa-hw/hdp22-hive-streaming#part-3---import-web-history-data-from-log-file-to-hive-orc-table-via-flume) 
4. [Storm - import tweets for those users into Hive ORC table](https://github.com/abajwa-hw/hdp22-hive-streaming#part-4-import-tweets-for-users-into-hive-orc-table-via-storm) 
5. [Analyze tables to populate statistics](https://github.com/abajwa-hw/hdp22-hive-streaming#part-5-analyze-table-to-populate-statistics)
6. [Run Hive queries to correlate the data from thee different sources](https://github.com/abajwa-hw/hdp22-hive-streaming#part-6-run-hive-query-to-correlate-the-data-from-thee-different-sources)
7. [What to try next?](https://github.com/abajwa-hw/hdp22-hive-streaming#what-to-try-next)


##### Part 1 - Start sandbox VM and enable Hive features 

- Download HDP 2.3 sandbox VM image (Sandbox_HDP_2.3_VMware.ova) from [Hortonworks website](http://hortonworks.com/products/hortonworks-sandbox/)
- Import Sandbox_HDP_2.3_VMware.ova into VMWare and set the VM memory size to 8GB
- Now start the VM
- After it boots up, find the IP address of the VM and add an entry into your machines hosts file e.g.
```
192.168.191.241 sandbox.hortonworks.com sandbox    
```
- Connect to the VM via SSH (password hadoop)
```
ssh root@sandbox.hortonworks.com
```

- Ranger policy:
  - HDFS plugin
    - disable global allow
    - create IT global allow on /

  - Hive
    - disable global allow
    - create IT global allow on default DB
    - add hive user to same (needed for Storm topology later)
  
- Setup YARN queues:  
  - Maximum AM Resource: 30%
  - Queue mappings: g:IT:batch,g:Marketing:default
  - User limit: 1 for batch and 2 for default
  - ordering policy: for default set to fair
  - max capacity: batch is 50%, default: 100%

- Hive changes:
  - Acid: on
  - start tez at init: true
  - sessions per queue: 2
  - authorization: Ranger
  - # containers held: 1
  - fetch column stats at compiler: true


- Run below as root for initial setup
  - Create home dirs in HDFS
```
sudo -u hdfs hadoop fs -mkdir /user/it1
sudo -u hdfs hadoop fs -chown it1:IT /user/it1
sudo -u hdfs hadoop fs -mkdir /user/mktg1
sudo -u hdfs hadoop fs -chown mktg1:Marketing /user/mktg1
```

  - Install maven from epel
  
```
curl -o /etc/yum.repos.d/epel-apache-maven.repo https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo
yum -y install apache-maven ntp
```

  - In case your system time is not accurate, fix it to avoid errors from Twitter4J
  
```
service ntpd stop
ntpdate pool.ntp.org
service ntpd start
```


- In Ambari:
  - Create users:
    - mktg1 : non admin, password: admin
    - it1 : admin: password: admin
  
  - View permissions:
    - Give mktg1 access to HDFS Files and Hive views  
  


----------------



##### Part 2 - Import data from MySQL to Hive ORC table via Sqoop 

- Pull the latest Hive streaming code/scripts
```
su it1
cd
git clone https://github.com/abajwa-hw/hdp22-hive-streaming.git 
```
- Inspect CSV of user personal data
```
cat ~/hdp22-hive-streaming/data/PII_data_small.csv
```
- Import users personal data into MySQL
```
mysql -u root -p
#empty password

create database people;
use people;
create table persons (people_id INT PRIMARY KEY, sex text, bdate DATE, firstname text, lastname text, addresslineone text, addresslinetwo text, city text, postalcode text, ssn text, id2 text, email text, id3 text,lastupdate timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP );
LOAD DATA LOCAL INFILE '~/hdp22-hive-streaming/data/PII_data_small.csv' REPLACE INTO TABLE persons FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n';
```
- In MySQL, verify that the data was imported and exit. The remaining queries will be run in Hive
```
select people_id, firstname, lastname, city from persons where lastname='SMITH';
exit;
```

- **TODO** remove if not neeeded
```
hadoop fs -mkdir -p staging/sqoop/persons
```

- Create the staging table in Hive
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "

create table persons_staging (people_id INT , sex string, bdate DATE, firstname string
, lastname string
, addresslineone string
, addresslinetwo string
, city string
, postalcode string
, ssn string
, id2 string
, email string
, id3 string
,lastupdate timestamp)
stored as orc
;
"
```

- view shows table is created

- put empty password file into HDFS
```
touch mysqlpasswd.txt
hadoop fs -put mysqlpasswd.txt /user/it1
```

- create a incremental sqoop job pointing to password file
```
sqoop job -create persons_staging -- import --verbose --connect 'jdbc:mysql://localhost/people' --table persons --username root --password-file hdfs://sandbox.hortonworks.com:8020/user/it1/mysqlpasswd.txt --hcatalog-table persons_staging  -m 1 --check-column lastupdate --incremental lastmodified --last-value '1900-01-01'
```
- validate it got created
```
sqoop job -list
```

  - should show

```
Available jobs:
  persons_staging
```

- Run first iteration
```
sqoop job -exec persons_staging
```

- (Optional) In case you need to do it over:
```
sqoop job -delete persons_staging
```

- Loin to ambari as it1/admin and verify that 400 records created


- Create persons table
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create table persons (people_id INT , sex string, bdate DATE, firstname string
, lastname string
, addresslineone string
, addresslinetwo string
, city string
, postalcode string
, ssn string
, id2 string
, email string
, id3 string
,lastupdate timestamp)
clustered by (people_id) into 7 buckets
stored as orc
TBLPROPERTIES ('transactional'='true')
;
"
```

- Move data form staging table to final table. 
  - first remove records in final table that are also found in staging table
  - move data from staging table to final table
  - truncate staging table
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
delete from persons where persons.people_id in (select people_id from persons_staging);

insert into persons select people_id,sex,bdate,firstname,lastname,addresslineone,addresslinetwo,city,postalcode,ssn,substr(ssn,length(ssn)-4),id2,email,id3,lastupdate from persons_staging;

truncate table persons_staging;
"
```

- Update existing record in mysql
```
mysql -u root -p
#empty password

use people;
select bdate from persons where people_id=619561879;
update persons set bdate='2007-02-02' where people_id=619561879;
select bdate from persons where people_id=619561879;
exit
```

- Run second iteration of sqoop job
```
sqoop job -exec persons_staging
```

- Now look at staging table and it should only show one record with bdate: 2007-02-02

- Now merge this record back to final table using same SQLs as above:
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
delete from persons where persons.people_id in (select people_id from persons_staging);

insert into persons select people_id,sex,bdate,firstname,lastname,addresslineone,addresslinetwo,city,postalcode,ssn,id2,email,id3,lastupdate from persons_staging;
# insert into persons select * from persons_staging;


truncate table persons_staging;
"
```

- Verify the record was also updated in final table
```
select bdate from persons where people_id=619561879;
```

- As it1, create persons table view with masked SSN

```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create view persons_view as 
select people_id,sex,bdate,firstname,lastname,addresslineone,addresslinetwo,city,postalcode,substr(ssn,length(ssn)-3) last4ssn ,id2,email,id3,lastupdate from persons;
;
"
```

- As mktg1 try to query tables
```
su mktg1
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
show tables;
"
```
- Now login to Ranger UI as admin/admin and create a policy for Marketing to access default db and persons_view table

- As mktg1 try to query tables
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select * from persons_view limit 5
"
```


##### Part 3 - Import web history data from log file to Hive ORC table via Flume 

- Use Hive view to create table webtraffic to store the userid and web url enabling transactions and partition into day month year: http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
 
````
su it1

beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create table if not exists webtraffic (id int, val string) 
partitioned by (year string,month string,day string) 
clustered by (id) into 7 buckets 
stored as orc 
TBLPROPERTIES ('transactional'='true');
"
````

![Image](../master/screenshots/create-webtraffic-table.png?raw=true)

- Now lets configure the Flume agent. High level:
  - The *source* will be of type exec that tails our weblog file using a timestamp intersept (i.e. flume interseptor adds timestamp header to the payload)
  - The *channel* will be a memory channel which is ideal for flows that need higher throughput but could lose the data in the event of agent failures
  - The *sink* will be of type Hive that writes userid and url to default.webtraffic table partitioned by year, month, day
  - More details about each type of source, channel, sink are available [here](http://flume.apache.org/FlumeUserGuide.html) 
- In Ambari > Flume > Service Actions > Turn off maintenance mode  
- In Ambari > Flume > Configs > flume.conf enter the below, Save and restart Flume
```

## Flume NG Apache Log Collection
## Refer to https://cwiki.apache.org/confluence/display/FLUME/Getting+Started
##

agent.sources = webserver
agent.sources.webserver.type = exec
agent.sources.webserver.command = tail -F /tmp/webtraffic.log
agent.sources.webserver.batchSize = 20
agent.sources.webserver.channels = memoryChannel
agent.sources.webserver.interceptors = intercepttime
agent.sources.webserver.interceptors.intercepttime.type = timestamp

## Channels ########################################################
agent.channels = memoryChannel
agent.channels.memoryChannel.type = memory
agent.channels.memoryChannel.capacity = 1000
agent.channels.memoryChannel.transactionCapacity = 1000

## Sinks ###########################################################

agent.sinks = hiveout
agent.sinks.hiveout.type = hive
agent.sinks.hiveout.hive.metastore=thrift://localhost:9083
agent.sinks.hiveout.hive.database=default
agent.sinks.hiveout.hive.table=webtraffic
agent.sinks.hiveout.hive.batchSize=1
agent.sinks.hiveout.hive.partition=%Y,%m,%d
agent.sinks.hiveout.serializer = DELIMITED
agent.sinks.hiveout.serializer.fieldnames =id,val
agent.sinks.hiveout.channel = memoryChannel
```

- Start tailing the flume agent log file in one terminal...
```
tail -F /var/log/flume/flume-agent.log
```

- After a few seconds the agent log should contain the below
```
02 Jan 2015 20:35:31,782 INFO  [lifecycleSupervisor-1-0] (org.apache.flume.source.ExecSource.start:163)  - Exec source starting with command:tail -F /tmp/webtraffic.log
02 Jan 2015 20:35:31,782 INFO  [lifecycleSupervisor-1-1] (org.apache.flume.instrumentation.MonitoredCounterGroup.register:119)  - Monitored counter group for type: SINK, name: hiveout: Successfully registered new MBean.
02 Jan 2015 20:35:31,782 INFO  [lifecycleSupervisor-1-1] (org.apache.flume.instrumentation.MonitoredCounterGroup.start:95)  - Component type: SINK, name: hiveout started
02 Jan 2015 20:35:31,783 INFO  [lifecycleSupervisor-1-1] (org.apache.flume.sink.hive.HiveSink.start:611)  - hiveout: Hive Sink hiveout started
02 Jan 2015 20:35:31,785 INFO  [lifecycleSupervisor-1-0] (org.apache.flume.instrumentation.MonitoredCounterGroup.register:119)  - Monitored counter group for type: SOURCE, name: webserver: Successfully registered new MBean.
02 Jan 2015 20:35:31,785 INFO  [lifecycleSupervisor-1-0] (org.apache.flume.instrumentation.MonitoredCounterGroup.start:95)  - Component type: SOURCE, name: webserver started
```

- Using another terminal window, run the createlog.sh script which will generate 400 dummy web traffic log events at a rate of one event per second
```
cd ~/hdp22-hive-streaming
./createlog.sh ./data/PII_data_small.csv 400 >> /tmp/webtraffic.log
```
- Start tailing the webtraffic file in another terminal
```
tail -F /tmp/webtraffic.log
```
- The webtraffic.log should start displaying the webtraffic
```
581842607,http://www.google.com
493259972,http://www.yahoo.com
607729813,http://cnn.com
53802950,http://www.hortonworks.com
```

- The Flume agent log should start outputting below
```
02 Jan 2015 20:42:37,380 INFO  [SinkRunner-PollingRunner-DefaultSinkProcessor] (org.apache.flume.sink.hive.HiveWriter.commitTxn:251)  - Committing Txn id 14045 to {metaStoreUri='thrift://localhost:9083', database='default', table='webtraffic', partitionVals=[2015, 01, 02] }
```
- After 6-7min, notice that the script has completed and the webtraffic table now has records created

http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive

![Image](../master/screenshots/screenshot-view-webtraffic-data.png?raw=true)

- Open Files view and navigate to /apps/hive/warehouse/webtraffic/year=xxxx/month=xx/day=xx/delta_0000001_0000100 and view the files

http://sandbox.hortonworks.com:8000/filebrowser/view//apps/hive/warehouse/webtraffic
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS.png?raw=true)

- Notice the table is stored in ORC format
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS-ORC.png?raw=true)

------------------------


##### Part 4: Import tweets for users into Hive ORC table via Storm


- Add your Twitter consumer key/secret, token/secret under [hdp22-hive-streaming/src/test/HiveTopology.java](https://github.com/abajwa-hw/hdp22-hive-streaming/blob/master/src/test/HiveTopology.java#L40)

- Create hive table for tweets that has transactions turned on and ORC enabled
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create table if not exists user_tweets (twitterid string, userid int, displayname string, created string, language string, tweet string) clustered by (userid) into 7 buckets stored as orc tblproperties('orc.compress'='NONE','transactional'='true');
"
```
![Image](../master/screenshots/create-usertweets-table.png?raw=true)



- Optional: build the storm uber jar (may take 10-15min first time). You can skip this to use the pre-built jar in the target dir. 
```
cd ~/hdp22-hive-streaming
mvn package
```

- Using Ambari, make sure Storm has maintenance mode turned off and is started  (it is stopped by default on the sandbox) and twitter_topology does not already exist
- Open up the Storm webview or Storm webui
  - http://sandbox.hortonworks.com:8080/#/main/views/Storm_Monitoring/0.1.0/Storm
  - http://sandbox.hortonworks.com:8744/
  
![Image](../master/screenshots/screenshot-storm-home.png?raw=true)

- Run the topology on the cluster and notice twitter_topology appears on Storm webui
```
cd /root/hdp22-hive-streaming
storm jar ./target/storm-integration-test-1.0-SNAPSHOT.jar test.HiveTopology thrift://sandbox.hortonworks.com:9083 default user_tweets twitter_topology
```

Note: to run in local mode (ie without submitting it to cluster), run the above without the twitter_topology argument

- In Storm view or UI, drill down into the topology to see the details and refresh periodically. The numbers under emitted, transferred and acked should start increasing.
![Image](../master/screenshots/screenshot-storm-topology.png?raw=true)

In Storm view or UI, you can also click on "Show Visualization" under "Topology Visualization" to see the topology visually
![Image](../master/screenshots/screenshot-storm-visualization.png?raw=true)

![Image](../master/screenshots/storm-view-twittertopology.png?raw=true)

- After 20-30 seconds, kill the topology from the Storm UI or using the command below to avoid overloading the VM
```
storm kill twitter_topology
```

- After a few seconds, navigate to Hive view and query the user_tweets table and notice it now contains tweets
![Image](../master/screenshots/screenshot-hiveview-usertweets.png?raw=true)

  - If using Hue: You may encounter the below error through Hue when browsing this table. This is because in this version, Hue beeswax does not support UTF-8 and there were such characters present in the tweets
```
'ascii' codec can't decode byte 0xf0 in position 62: ordinal not in range(128)
```
  - To workaround replace ```default_filters=['unicode', 'escape'],``` with ```default_filters=['decode.utf8', 'unicode', 'escape'],``` in /usr/lib/hue/desktop/core/src/desktop/lib/django_mako.py
```
cp  /usr/lib/hue/desktop/core/src/desktop/lib/django_mako.py  /usr/lib/hue/desktop/core/src/desktop/lib/django_mako.py.orig
sed -i "s/default_filters=\['unicode', 'escape'\],/default_filters=\['decode.utf8', 'unicode', 'escape'\],/g" /usr/lib/hue/desktop/core/src/desktop/lib/django_mako.py
```

- Open Files view and navigate to /apps/hive/warehouse/user_tweets: http://sandbox.hortonworks.com:8080/#/main/views/FILES/1.0.0/Files

![Image](../master/screenshots/screenshot-filesview-usertweets-HDFS.png?raw=true)

- Notice the table is stored in ORC format

![Image](../master/screenshots/screenshot-filesview-usertweets-HDFS-ORC.png?raw=true)

- In case you want to empty the table for future runs, you can run below 
```
delete from user_tweets;
```
Note: the 'delete from' command are only supported in 2.2 when Hive transactions are turned on)


----------------

##### Part 5: Analyze table to populate Hive statistics

- Run Hive table statistics
```
analyze table persons compute statistics;
analyze table user_Tweets compute statistics;
analyze table webtraffic partition(year,month,day) compute statistics;
```

- Run Hive column statistics
```
analyze table persons compute statistics for columns;
analyze table user_Tweets compute statistics for columns;
analyze table webtraffic partition(year,month,day) compute statistics for columns;
```

------------------


##### Part 6: Run Hive query to correlate the data from thee different sources

- Using the Hive view http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive :

- Check size of PII table
```
select count(*) from persons;
```
returns 400 rows

- Correlate browsing history with PII data
```
select  p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.ssn, w.val
from persons p, webtraffic w 
where w.id = p.people_id;
```
Notice the last field contains the browsing history:
![Image](../master/screenshots/screenshot-hiveview-query1.png?raw=true)

- Correlate tweets with PII data
```
select t.userid, t.twitterid, p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.ssn, t.tweet 
from persons p, user_tweets t 
where t.userid = p.people_id;
```
Notice the last field contains the Tweet history:
![Image](../master/screenshots/screenshot-hiveview-query2.png?raw=true)

- Correlate all 3
```
select  p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.ssn, w.val, t.tweet
from persons p, user_tweets t, webtraffic w 
where w.id = t.userid and t.userid = p.people_id
order by p.ssn;
```
Notice the last 2 field contains the browsing and Tweet history:
![Image](../master/screenshots/screenshot-hiveview-query3.png?raw=true)

- Notice that for these queries Hive view provides the option to view Visual Explain of the query for performance tuning.
![Image](../master/screenshots/hive-visualexplain.png?raw=true)

- Also notice that for these queries Hive view provides the option to view Tez graphical view to help aid debugging.
![Image](../master/screenshots/hive-tezgraph.png?raw=true)

-----------------------

##### What to try next?

- Enhance the sample Twitter Storm topology
  - Import the above Storm sample into Eclipse on the sandbox VM using an *Ambari stack for VNC* and use the Maven plugin to compile the code. Steps available at https://github.com/abajwa-hw/vnc-stack
  - Update [HiveTopology.java](https://github.com/abajwa-hw/hdp22-hive-streaming/blob/master/src/test/HiveTopology.java#L250) to pass hashtags or languages or locations or Twitter user ids to filter Tweets
  - Add other Bolts to this basic topology to process the Tweets (e.g. rolling count) and write them to different components (like HBase, Solr etc). Here is a HDP 2.2 sample project showing a more complicated topology with Tweets being generated from a Kafka producer and being emitted into local filesystem, HDFS, Hive, Solr and HBase: https://github.com/abajwa-hw/hdp22-twitter-demo 
  
- Use Sqoop to import data into ORC tables from other databases (e.g. Oracle, MSSQL etc). See [this blog entry](http://hortonworks.com/hadoop-tutorial/import-microsoft-sql-server-hortonworks-sandbox-using-sqoop/) for more details

- Experiment with Flume
  - Change the Flume configuration to use different channels (e.g. FileChannel or Spillable Memory Channel) or write to different sinks (e.g HBase). See the [Flume user guide](http://flume.apache.org/FlumeUserGuide.html) for more details.
  

