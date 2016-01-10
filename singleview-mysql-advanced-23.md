## Hive streaming workshop
This demo is part of a 'Interactive Query with Apache Hive' webinar.

The webinar recording and slides are available at http://hortonworks.com/partners/learn/#hive

Instructions for HDP 2.2 can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/README-22.md)

#### Demo overview
In this lab we model a hadoop cluster with 2 tenants, IT & Marketing.  IT is responsible for onboarding data while Marketing is responsible of running analytical queries.  IT jobs are batch oriented while Marketing queries are typically interactive in nature.  The lab includes steps for setting queues, onboarding data, applying security and running analytical queries.  For the lab, we allocate cluster capacity equally between IT and Marketing.

1. [Start HDP 2.3 sandbox and enable Hive features like transactions, queues, preemption, Tez and sessions](https://github.com/abajwa-hw/single-view-demo#part-1---start-sandbox-vm-and-enable-hive-features)
2. [Sqoop - import PII data of users from MySql into Hive ORC table](https://github.com/abajwa-hw/single-view-demo#part-2---import-data-from-mysql-to-hive-orc-table-via-sqoop)
3. [Flume - import browsing history of users e.g. userid,webpage,timestamp from simulated weblogs into Hive ORC table](https://github.com/abajwa-hw/single-view-demo#part-3---import-web-history-data-from-log-file-to-hive-orc-table-via-flume) 
4. [Storm - import tweets for those users into Hive ORC table](https://github.com/abajwa-hw/single-view-demo#part-4-import-tweets-for-users-into-hive-orc-table-via-storm) 
5. [Analyze tables to populate statistics](https://github.com/abajwa-hw/single-view-demo#part-5-analyze-table-to-populate-statistics)
6. [Run Hive queries to correlate the data from thee different sources](https://github.com/abajwa-hw/single-view-demo#part-6-run-hive-query-to-correlate-the-data-from-thee-different-sources)
7. [What to try next?](https://github.com/abajwa-hw/single-view-demo#what-to-try-next)


##### Part 1 - Start sandbox VM and tenant onboarding

- Download HDP 2.3 sandbox VM image (Sandbox_HDP_2.3_VMware.ova) from [Hortonworks website](http://hortonworks.com/products/hortonworks-sandbox/)
- Import Sandbox_HDP_2.3_VMware.ova into VMWare and set the VM memory size to 8GB
- Now start the VM
- After it boots up, find the IP address of the VM and add an entry into your machines hosts file e.g.
```
192.168.191.241 sandbox.hortonworks.com sandbox    
```
- Connect to the VM via SSH (password hadoop). You can also open web-based SSH session by opening http://sandbox.hortonworks.com:4200
```
ssh root@sandbox.hortonworks.com
```

###### Setup user directories

- Run below as root for initial setup
  - Create home dirs for IT/mktg users in HDFS
  ```
	sudo -u hdfs hadoop fs -mkdir /user/it1
	sudo -u hdfs hadoop fs -chown it1:IT /user/it1
	sudo -u hdfs hadoop fs -mkdir /user/mktg1
	sudo -u hdfs hadoop fs -chown mktg1:Marketing /user/mktg1
  ```

  - Start Ranger admin service (if not already started)
  ```
  service ranger-admin start
  ```
  
  - (Optional) Install maven from epel. This is only needed if you will not be using the vanilla storm jar (i.e. if you will be recompiling the storm jar)
  
  ```
  curl -o /etc/yum.repos.d/epel-apache-maven.repo https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo
  yum -y install apache-maven-3.2* ntp
  ```
  - In case your system time is not accurate, fix it to avoid errors from Twitter4J
  
  ```
	service ntpd stop
	ntpdate pool.ntp.org
	service ntpd start
  ```

  
###### Setup security policies
  
- Create *HDFS* related security policies for the IT group in Ranger:
  - Login to Ranger (admin/admin) at http://sandbox.hortonworks.com:6080/

  - Disable the Global allow policy that grants HDFS permissions to all users on sandbox
    - open http://sandbox.hortonworks.com:6080/index.html#!/service/1/policies/2/edit
    - Click the enabled button so it becomes disabled and click Save:
  ![Image](../master/screenshots/lab/Ranger-policy-disable-global-HDFS.png?raw=true)
    
  - Add new HDFS policy at http://sandbox.hortonworks.com:6080/index.html#!/service/1/policies
    - Policy name: IT global allow on root dir
    - Resource Path: /
    - Select group: IT
    - Permissions: Read Write Execute

  ![Image](../master/screenshots/lab/Ranger-policy-HDFS.png?raw=true)

- Create *Hive* related security policies for the IT group in Ranger:
  - Disable the Global allow policy that grants Hive permissions to all users on sandbox
    - open http://sandbox.hortonworks.com:6080/index.html#!/service/2/policies/5/edit
    - Click the enabled button so it becomes disabled and click Save:  
  ![Image](../master/screenshots/lab/Ranger-policy-disable-global-hive.png?raw=true)    

  - Add new Hive policy for IT group and hive user at http://sandbox.hortonworks.com:6080/index.html#!/service/2/policies
    - Policy name: IT group permission on default DB
    - Hive Database: default
    - table: *
    - Hive column: *
    - Group: IT
    - User: hive
    - Permissions: All
          
  ![Image](../master/screenshots/lab/Ranger-policy-hive.png?raw=true)


###### Setup user YARN queues
  
- Setup/configure 'batch' and 'default' YARN queues using 'YARN Queue Manager' view in Ambari (login as admin/admin): http://sandbox.hortonworks.com:8080/#/main/views/CAPACITY-SCHEDULER/1.0.0/AUTO_CS_INSTANCE
  - For the default queue, make the below changes:
    - Capacity: 50%
    - Maximum AM Resource: 30%
    - Queue mappings: g:IT:batch,g:Marketing:default
    - User limit: 2 
    - ordering policy: set to fair
    - max capacity: 100%
  ![Image](../master/screenshots/lab/queue-default.png?raw=true)
  
  - Create a batch queue at the same level as default queue (first highlight root queue, then click "Add Queue") and ensure the below are set changes:
    - Capacity: 50%  
    - max capacity: 50%
  ![Image](../master/screenshots/lab/queue-batch.png?raw=true)

  - Actions > Save and refresh queues > Save changes. This should start a 'Refresh Yarn Capacity Scheduler' operation
  
- Under Ambari > Dashboard > Hive > Config, make the below changes then: Save > OK > Proceed Anyway > OK > Restart all affected > Confirm restart all
  - Acid transactions: on
  - start tez at init: true
  - sessions per queue: 2
  - authorization: Ranger
  - # containers held: 1
  - fetch column stats at compiler: true

  ![Image](../master/screenshots/lab/hive-configs.png?raw=true)


###### Enable users to log into Ambari views

- In Ambari follow steps below:
  - Create a user with admin privileges via 'Manage Users' menu:
    - user: it1  password: admin

  ![Image](../master/screenshots/lab/ambari-adduser1.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser2.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser3.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser4.png?raw=true)
  
    - Create non-admin user via 'Manage Users' menu:
      - user: mktg1 password: admin
      - under 'Permissions': add readonly permission to mktg1
      - under 'Views': Navigate to Hive > Hive > Under 'Permissions' grant mktg1 access to Hive view

  ![Image](../master/screenshots/lab/ambari-mktg1-user-1.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-mktg1-user-2.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-mktg1-user-3.png?raw=true)

  
  - Note in a real scenario, these users would be part of LDAP/AD and Ambari would simply authenticate against it.
  
----------------



##### Part 2 - Import data from MySQL to Hive ORC table via Sqoop 

- Note the hive queries shown below can either
  - run via beeline CLI from your terminal shell prompt or 
  - using Hive view in Ambari by logging in as either it1 or mktg1 (depending on which user is supposed to run it): http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
    - make sure just to copy the SQL (and not the beeline command)
    - make sure to run the queries *one SQL at a time*

- Login as it1
```
su it1
```
- Pull the latest Hive streaming code/scripts
```
cd
git clone https://github.com/abajwa-hw/single-view-demo.git 
```
- Inspect CSV of user personal data
```
cat ~/single-view-demo/data/PII_data_small.csv
```
- Import users personal data into MySQL
```
mysql -u root -p
#empty password

create database people;
use people;
create table persons (people_id INT PRIMARY KEY, sex text, bdate DATE, firstname text, lastname text, addresslineone text, addresslinetwo text, city text, postalcode text, ssn text, id2 text, email text, id3 text,lastupdate timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP );
LOAD DATA LOCAL INFILE '~/single-view-demo/data/PII_data_small.csv' REPLACE INTO TABLE persons FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n';
```
- In MySQL, verify that the data was imported and exit. 
```
select people_id, firstname, lastname, city from persons where lastname='SMITH';
exit;
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

- If using the Hive view, note that:
  - you can click the 'refresh' icon next to the Database explorer and click on default database to confirm the new table was created.
  - you can click on the 'list' icon next to each table as a shortcut to preview its contents

![Image](../master/screenshots/lab/persons-staging.png?raw=true)

- Sqoop will require the MySQL password (empty in this case). Put empty password file into HDFS
```
touch mysqlpasswd.txt
hadoop fs -put mysqlpasswd.txt /user/it1
```

- Create an incremental sqoop job (pointing to password file) 
```
sqoop job -create persons_staging -- import --verbose --connect 'jdbc:mysql://localhost/people' --table persons --username root --password-file hdfs://sandbox.hortonworks.com:8020/user/it1/mysqlpasswd.txt --hcatalog-table persons_staging  -m 1 --check-column lastupdate --incremental lastmodified --last-value '1900-01-01'  --driver com.mysql.jdbc.Driver
```
- validate the job got created
  - this should show available jobs
```
sqoop job -list
```

- Run the first iteration of Sqoop job to import users from MySQL to staging table
```
sqoop job -exec persons_staging
```

- (Optional) In case you made a mistake and need to do it over:
```
sqoop job -delete persons_staging
```

- Login to Hive view as it1 and verify that 400 records created in persons_staging: http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive

![Image](../master/screenshots/lab/persons-staging-count.png?raw=true)


- Create persons (final) table in hive 
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
![Image](../master/screenshots/lab/persons.png?raw=true)

- Move data from staging table to final table. 
  - first remove the records from final table that are also found in staging table
  - move data from staging table to final table
  - truncate staging table
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
delete from persons where persons.people_id in (select people_id from persons_staging);

insert into persons select people_id,sex,bdate,firstname,lastname,addresslineone,addresslinetwo,city,postalcode,ssn,id2,email,id3,lastupdate from persons_staging;

truncate table persons_staging;
"
```

- Update existing record **in mysql**. The remaining queries will be run in Hive
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

 ![Image](../master/screenshots/lab/stagingtable-ssn.png?raw=true)

- Now merge this record back to final table using same SQLs as above:
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "

delete from persons where persons.people_id in (select people_id from persons_staging);

insert into persons select * from persons_staging;

truncate table persons_staging;
"
```

- Verify the record was also updated in final table
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
select bdate from persons where people_id=619561879
;
"
```
 ![Image](../master/screenshots/lab/bdate-query.png?raw=true)

- As it1, create persons table view with masked SSN

```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create view persons_view as 
select people_id,sex,bdate,firstname,lastname,addresslineone,addresslinetwo,city,postalcode,substr(ssn,length(ssn)-3) last4ssn ,id2,email,id3,lastupdate from persons
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
![Image](../master/screenshots/lab/showtables.png?raw=true)

- Now login to Ranger UI as admin/admin and create a policy for Marketing to access default db and persons_view table
  - Open http://sandbox.hortonworks.com:6080/index.html#!/service/2/policies and click Add New Policy
  - Create a policy with below details:
    - Policy name: Marketing view tables
    - Hive Database: default
    - table: persons_view
    - Hive column: *
    - Group: Marketing
    - Permissions: select
  
 ![Image](../master/screenshots/lab/Ranger-policy-hive-views1.png?raw=true)  

- As mktg1 try to query tables. To use the Hive view as mktg1 user, you can open the url in a different browser and login as mktg1: http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select * from persons_view limit 5
"
```

- As it1 user, open Files view and navigate to /apps/hive/warehouse/persons and open one of the delta_* folders and download/view one of the bucket* files

http://sandbox.hortonworks.com:8080/#/main/views/FILES/1.0.0/Files
![Image](../master/screenshots/screenshot-filesview-persons-HDFS.png?raw=true)

- Notice the table is stored in ORC format
![Image](../master/screenshots/screenshot-hiveview-persons-data-ORC.png?raw=true)

- Check the YARN UI at http://sandbox.hortonworks.com:8088/cluster
  - Notice the jobs run as it1 user were routed to the batch queue while the others went to the default queue
 ![Image](../master/screenshots/lab/YARN-UI1.png?raw=true)  


----------------


##### Part 3 - Import web history data from log file to Hive ORC table via Flume 

- As it1, use beeline or Hive view to create table webtraffic to store the userid and web url enabling transactions and partition into day month year: http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
 
````
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

- (Optional) Start tailing the flume agent log file in one terminal...
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

- Using another terminal window, as it1 user, run the createlog.sh script which will generate 400 dummy web traffic log events at a rate of one event per second
```
cd ~/single-view-demo
./createlog.sh ./data/PII_data_small.csv 400 >> /tmp/webtraffic.log
```
- (Optional) Tailing the webtraffic file in another terminal to see the webtraffic records
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
  - You may see the below errors. These are caused by Ambari Metrics service being down and can be ignored.
  
  ```
  I/O exception (java.net.ConnectException) caught when processing request: Connection refused
  Unable to send metrics to collector by address:http://sandbox.hortonworks.com:6188/ws/v1/timeline/metrics
  ```
  
- This will run for ~6 min. You can take this time to sign up for a Twitter account and obtain developer keys (needed for the next part of the workshop)

- After 6-7 min, notice that the script has completed and the webtraffic table now has records created (while waiting you can get your Twitter consumer key/secrets - see part 4)

http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive

![Image](../master/screenshots/screenshot-view-webtraffic-data.png?raw=true)

- Open Files view and navigate to /apps/hive/warehouse/webtraffic/year=xxxx/month=xx/day=xx/delta_xxxxxxxxx and view the files

http://sandbox.hortonworks.com:8080/#/main/views/FILES/1.0.0/Files
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS.png?raw=true)

- Notice the table is stored in ORC format
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS-ORC.png?raw=true)

------------------------


##### Part 4: Import tweets for users into Hive ORC table via Storm


- Twitter4J requires you to have a Twitter account and obtain developer keys by registering an "app". Create a Twitter account and app and get your consumer key/token and access keys/tokens:
  - Open https://apps.twitter.com 
  - sign in
  - create new app
  - fill anything
  - create access tokens

- Add your Twitter consumer key/secret, token/secret under [single-view-demo/src/test/HiveTopology.java](https://github.com/abajwa-hw/single-view-demo/blob/master/src/test/HiveTopology.java#L40)

- Create hive table for tweets that has transactions turned on and ORC enabled
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create table if not exists user_tweets (twitterid string, userid int, displayname string, created string, language string, tweet string) clustered by (userid) into 7 buckets stored as orc tblproperties('orc.compress'='NONE','transactional'='true')
;
"
```
![Image](../master/screenshots/create-usertweets-table.png?raw=true)


- Point maven to hortonworks nexus repo
```
mkdir ~/.m2
wget https://gist.githubusercontent.com/abajwa-hw/7cdfc0bf6b2774ae7ccf/raw/16abe28ceb9452d2cf3a69405b1ad3cd74fc04dc/settings.xml -O ~/.m2/settings.xml  
```

- Build the storm uber jar using maven (may take 10-15min first time). 
```
cd ~/single-view-demo
#set JAVA_HOME e.g. /usr/lib/jvm/java-1.7.0-openjdk.x86_64
export JAVA_HOME=<your JAVA_HOME>
mvn package
```

  
- Using Ambari, make sure Storm has maintenance mode turned off and is started  (it is stopped by default on the sandbox) and twitter_topology does not already exist
- Open up the Storm webview or Storm webui
  - http://sandbox.hortonworks.com:8080/#/main/views/Storm_Monitoring/0.1.0/Storm
  - http://sandbox.hortonworks.com:8744/
  
![Image](../master/screenshots/screenshot-storm-home.png?raw=true)

- Run the topology on the cluster and notice twitter_topology appears on Storm webui
```
cd ~/single-view-demo
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


- Open Files view and navigate to /apps/hive/warehouse/user_tweets/delta_xxxx/bucket_xxxx: http://sandbox.hortonworks.com:8080/#/main/views/FILES/1.0.0/Files

![Image](../master/screenshots/screenshot-filesview-usertweets-HDFS.png?raw=true)

- Notice the table is stored in ORC format

![Image](../master/screenshots/screenshot-filesview-usertweets-HDFS-ORC.png?raw=true)

- Troubleshooting:
  - In case no tweets appear after 30s, double check that you added 'hive' user to the 'IT group permission on default DB' policy you created at the start
  
- (Optional) In case you want to empty the table for future runs, you can run below 
```
delete from user_tweets;
```
Note: the 'delete from' command are only supported in 2.2 when Hive transactions are turned on)

----------------


- Create views from user_Tweets and webtraffic tables for mktg1 user to access
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create view user_Tweets_view as select * from user_Tweets;
create view webtraffic_view as select * from webtraffic;
"
```

- Now login to Ranger UI as admin/admin and create a policy for Marketing to access user_Tweets_view and webtraffic_view tables

![Image](../master/screenshots/lab/Ranger-policy-hive-views.png?raw=true)


----------------

##### Part 5: Analyze table to populate Hive statistics

- As it1 user...

- Run Hive table statistics
```
analyze table persons compute statistics;
analyze table user_Tweets compute statistics;
analyze table webtraffic partition(year,month,day) compute statistics;
```

- Run Hive column statistics (these may take 20-30s each)
```
analyze table persons compute statistics for columns;
analyze table user_Tweets compute statistics for columns;
analyze table webtraffic partition(year,month,day) compute statistics for columns;
```

------------------


##### Part 6: Run Hive query to correlate the data from thee different sources

- As mktg1 user, run the below BI queries using the Hive view http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive :

- Check size of PII table
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select count(*) from persons_view;
select count(*) from webtraffic_view;
select count(*) from user_tweets_view;
"
```
returns 400 rows

- Correlate browsing history with PII data
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select  p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.last4ssn, w.val
from persons_view p, webtraffic_view w 
where w.id = p.people_id;
"
```
Notice the last field contains the browsing history:
![Image](../master/screenshots/lab/BI-query1.png?raw=true)

- Correlate tweets with PII data
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select t.userid, t.twitterid, p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.last4ssn, t.tweet 
from persons_view p, user_tweets_view t 
where t.userid = p.people_id;
"
```
Notice the last field contains the Tweet history:
![Image](../master/screenshots/lab/BI-query2.png?raw=true)

- Correlate all 3
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "
select  p.firstname, p.lastname, p.sex, p.addresslineone, p.city, p.last4ssn, w.val, t.tweet
from persons_view p, user_tweets_view t, webtraffic_view w 
where w.id = t.userid and t.userid = p.people_id
order by p.last4ssn;
"
```
Notice the last 2 field contains the browsing and Tweet history:
![Image](../master/screenshots/lab/BI-query3.png?raw=true)

- Notice that for these queries Hive view provides the option to view Visual Explain of the query for performance tuning.
![Image](../master/screenshots/lab/vizexplain-bi.png?raw=true)

- If you run the query as it1 user, you will notice that for these queries Hive view provides the option to view Tez graphical view to help aid debugging.
![Image](../master/screenshots/lab/tezview-it.png?raw=true)


- Finally notice how Ranger is keeping audit: http://sandbox.hortonworks.com:6080/index.html#!/reports/audit/bigData
  - Ranger is keeping tab of what user is accessing what resource across Hadoop components
  - Ranger also provides a single pane view to set authorization policies and review audits across Hadoop

![Image](../master/screenshots/lab/ranger-audits.png?raw=true)

-----------------------

##### What to try next?

- Enhance the sample Twitter Storm topology
  - Import the above Storm sample into Eclipse on the sandbox VM using an *Ambari stack for VNC* and use the Maven plugin to compile the code. Steps available at https://github.com/abajwa-hw/vnc-stack
  - Update [HiveTopology.java](https://github.com/abajwa-hw/single-view-demo/blob/master/src/test/HiveTopology.java#L250) to pass hashtags or languages or locations or Twitter user ids to filter Tweets
  - Add other Bolts to this basic topology to process the Tweets (e.g. rolling count) and write them to different components (like HBase, Solr etc). Here is a HDP 2.2 sample project showing a more complicated topology with Tweets being generated from a Kafka producer and being emitted into local filesystem, HDFS, Hive, Solr and HBase: https://github.com/abajwa-hw/hdp22-twitter-demo 
  
- Use Sqoop to import data into ORC tables from other databases (e.g. Oracle, MSSQL etc). See [this blog entry](http://hortonworks.com/hadoop-tutorial/import-microsoft-sql-server-hortonworks-sandbox-using-sqoop/) for more details

- Experiment with Flume
  - Change the Flume configuration to use different channels (e.g. FileChannel or Spillable Memory Channel) or write to different sinks (e.g HBase). See the [Flume user guide](http://flume.apache.org/FlumeUserGuide.html) for more details.
  

