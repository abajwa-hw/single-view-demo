## EDW optimization and Single view lab


- Goals:
  - EDW optimization: demonstrate how you can bulk import data from EDW/RDBMS into Hive and then incrementally keep the Hive tables updated periodically 
  - Single view: demonstrate how you can use Hive to get a single view of product by combining ETL/CRM data from EDW/DB, along with web traffic and social media data (collected using HDF)


#### Demo overview
In this lab we model a hadoop cluster with 2 tenants, IT & Marketing.  IT is responsible for onboarding data while Marketing is responsible of running analytical queries.  IT jobs are batch oriented while Marketing queries are typically interactive in nature.  The lab includes steps for setting queues, onboarding data, applying security and running analytical queries.  For the lab, we allocate cluster capacity equally between IT and Marketing.

1. [Start HDP 2.3 sandbox and enable Hive features like transactions, queues, preemption, Tez and sessions](https://github.com/abajwa-hw/single-view-demo#part-1---start-sandbox-vm-and-enable-hive-features)
2. [Sqoop - import CRM/ERP data from DB/EDW into Hive](https://github.com/abajwa-hw/single-view-demo#part-2---import-data-from-mysql-to-hive-orc-table-via-sqoop)
3. [Nifi - import related tweets into Hive](https://github.com/abajwa-hw/single-view-demo#part-4-import-tweets-for-users-into-hive-orc-table-via-storm) 
4. [Nifi - Import simulated web traffic logs into Hive](https://github.com/abajwa-hw/single-view-demo#part-3---import-web-history-data-from-log-file-to-hive-orc-table-via-flume) 
5. [Analyze tables to populate statistics](https://github.com/abajwa-hw/single-view-demo#part-5-analyze-table-to-populate-statistics)
6. [Use Hive view to correlate the data from multiple data sources](https://github.com/abajwa-hw/single-view-demo#part-6-run-hive-query-to-correlate-the-data-from-thee-different-sources)
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

  - In case your system time is not accurate, fix it to avoid errors from Twitter in later steps
  
  ```
	yum install -y ntp
	service ntpd stop
	ntpdate pool.ntp.org
	service ntpd start
  ```

  
###### Setup security policies
  
- Create *HDFS* related security policies for the IT group in Ranger:
  - Login to Ranger (admin/admin) at http://sandbox.hortonworks.com:6080/

    
  - Add new HDFS policy at http://sandbox.hortonworks.com:6080/index.html#!/service/5/policies/create
    - Policy name: IT global allow on root dir
    - Resource Path: /
    - Select group: IT
    - Permissions: Read Write Execute

  ![Image](../master/screenshots/lab/Ranger-policy-HDFS.png?raw=true)

- Create *Hive* related security policies for the IT group in Ranger:
  - Disable the Global allow policy that grants Hive permissions to all users on sandbox
    - open http://sandbox.hortonworks.com:6080/index.html#!/service/1/policies/6/edit
    - Click the enabled button so it becomes disabled and click Save:  
  ![Image](../master/screenshots/lab/Ranger-policy-disable-global-hive.png?raw=true)    

  - Add new Hive policy for IT group and hive user at http://sandbox.hortonworks.com:6080/index.html#!/service/1/policies/create
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
    - user: it1  password: it1

  ![Image](../master/screenshots/lab/ambari-adduser1.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser2.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser3.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-adduser4.png?raw=true)
  
    - Create non-admin user via 'Manage Users' menu:
      - user: mktg1 password: mktg1
      - under 'Permissions': add readonly permission to mktg1
      - under 'Views': Navigate to Hive > Hive > Under 'Permissions' grant mktg1 access to Hive view

  ![Image](../master/screenshots/lab/ambari-mktg1-user-1.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-mktg1-user-2.png?raw=true)
  ![Image](../master/screenshots/lab/ambari-mktg1-user-3.png?raw=true)

  - Note in a real scenario, these users would be part of LDAP/AD and Ambari would simply authenticate against it.

- At this point, you should be able to login to Ambari as either it1 or mktg1 users
  
----------------



##### Part 2 - Import data from PostGres to Hive table via Sqoop 

###### Setup and download data


- As postgres user, login to Postgres and complete below to setup psql for user it1:
  - create contoso db
  - create it1 user
  - grant privileges on contoso to it1
  - check user got created
```
su postgres
psql
create database contoso;
CREATE USER it1 WITH PASSWORD 'it1';
GRANT ALL PRIVILEGES ON DATABASE contoso to it1;
\du
\q
exit
```

- Back as root, complete below to complete setup of it1 user
  - enable it1 user to login to psql by editing pg_hba.conf and add a line with: `host all all 127.0.0.1/32 md5`
```
service ambari stop
service postgresql stop
echo "host all all 127.0.0.1/32 md5" >> /var/lib/pgsql/data/pg_hba.conf
service postgresql start
service ambari start
```

- As root, setup Sqoop for postgres by downloading the appropriate JDBC jar from [here](https://jdbc.postgresql.org/download.html) e.g. "JDBC42 Postgresql Driver, Version 9.4-1207". Note: to confirm what version of postgres you have, run the following via psql: `SELECT version();`
```
wget https://jdbc.postgresql.org/download/postgresql-9.4.1207.jar -P /usr/hdp/current/sqoop-client/lib
```

**Next set of steps will be run as it1 user**


- Login as it1
```
su - it1
```
- Pull the latest lab code/scripts
```
cd
git clone https://github.com/abajwa-hw/single-view-demo.git 
```

- Download retail data set (contoso) into local /tmp dir on sandbox. More details on the data set available [here](https://en.wikipedia.org/wiki/Contoso)
```
cd /tmp
wget https://www.dropbox.com/s/r70i8j1ujx4h7j8/data.zip
unzip data.zip
```
- Inspect one of the fils containing the CSV data
```
head /tmp/data/FactSales.csv
```


----------------------


- Note the hive queries shown below can either
  - run via beeline CLI from your terminal shell prompt or 
  - using Hive view in Ambari by logging in as either it1 or mktg1 (depending on which user is supposed to run it): http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
    - make sure just to copy the SQL (and not the beeline command)
    - make sure to run the queries *one SQL at a time*


###### Bulk import of data into Hive from RDBMS



- As it1 user connect to psql and create/import data from downloaded sample data (this may take a few minutes)
```
su - it1
export PGPASSWORD=it1
psql -U it1 -d contoso -h localhost -f ~/single-view-demo/contoso-psql.sql

```

- Ensure sqoop can access tables in contoso db as it1 user
```
sqoop list-tables --connect jdbc:postgresql://localhost:5432/contoso --username it1 --password it1 -- schema contoso 
```

- Make sure Hive service is up via Ambari IU and start the bulk load of all the PSql tables into hive (as text) using Sqoop. This will run for some time.
```
sqoop import-all-tables --username it1 --password it1 --connect jdbc:postgresql://localhost:5432/contoso  --hive-import  --direct
```


- Ideally we would now convert all tables to final ORC tables in Hive. In this lab, we are showing how to do this for factsales table:
  - Run below using the Hive view in Ambari (http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive), one sql at a time:
```
CREATE TABLE `factsales_final` (
`SalesKey` int ,
`DateKey` timestamp ,  
`channelKey` int ,  
`StoreKey` int,
`ProductKey` int,
`PromotionKey` int,
`CurrencyKey` int,
`UnitCost` float,
`UnitPrice` float,
`SalesQuantity` int , 
`ReturnQuantity` int,
`ReturnAmount` float,
`DiscountQuantity` int,
`DiscountAmount` float,
`TotalCost` float,
`SalesAmount` float,
`ETLLoadID` int,
`LoadDate` timestamp , 
`UpdateDate` timestamp 
 )
clustered by (saleskey) into 7 buckets
stored as orc
TBLPROPERTIES ('transactional'='true')
;

insert into factsales_final select * from factsales;

```
#### Incremental import of data into Hive from RDBMS

- Now that we did the one time bulk import, next we will setup an incremental sqoop job

- create password file containing it1 user's password in HDFS. This is done to allow invocations of the job to be automated/scheduled (without having to manually pass the password )
```
# use -n to ensure newline is not added
echo -n "it1" > .password
hadoop fs -put .password /user/it1/
rm .password
```
- create incremental import sqoop job for factsales table and point it as below: 
  - --table: table the job is for (i.e. factsales)
  - --password-file: the HDFS location of the password file
  - --incremental: lastmodified (we want to use lastmodified logic to find delta records)
  - --check-column: specify which column that will be used to determine which delta records will be picked up (in this case, records whose updatedate column value is later than 2015-01-01 will be picked up)
  - see [Sqoop documentation on incremental imports](https://sqoop.apache.org/docs/1.4.2/SqoopUserGuide.html#_incremental_imports) for more details
```
sqoop job -create factsales -- import --verbose --connect 'jdbc:postgresql://localhost:5432/contoso' --table factsales -username it1 --password-file hdfs://sandbox.hortonworks.com:8020/user/it1/.password --check-column updatedate --incremental lastmodified --last-value '2015-01-01' --hive-import  --direct
```

- Update records in factsales table in postgres
```
psql -U it1 -d contoso -h localhost -c "update factsales set updatedate = '2016-01-01 00:00:00' where saleskey in (1,2);"
```

- In Hive, truncate staging table by running below in Hive view
```
truncate table factsales;
```

- run incremental sqoop job for factsales to import updated records from postgres into hive staging table
```
sqoop job -exec factsales
```
- In Hive, check only records we changed were picked up in the hive staging table
```
SELECT * FROM default.factsales;
```
- In Hive, move data from staging table to final table (one at a time, using Hive view)
  - first remove the records from final table that are also found in staging table
  - move data from staging table to final table
  - truncate staging table
```
delete from factsales_final where saleskey in (select saleskey from factsales);
insert into factsales_final select * from factsales;
truncate table factsales;
```

- In Hive, check the records updated in hive final table 
```
select * from  factsales_final where saleskey in (1,2);
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

- At this point we have shown how you can bulk import data from EDW/RDBMS into Hive and then incrementally keep the Hive tables updated periodically 

------------------------


##### Part 3: Import product-related tweets into Hive via Nifi


- Twitter4J requires you to have a Twitter account and obtain developer keys by registering an "app". Create a Twitter account and app and get your consumer key/token and access keys/tokens:
  - Open https://apps.twitter.com 
  - sign in
  - create new app
  - fill anything
  - create access tokens


- Follow steps from earlier lab to install Nifi via Ambari, monitor certain tweets and push to Hive/Solr:
https://community.hortonworks.com/articles/1282/sample-hdfnifi-flow-to-push-tweets-into-solrbanana.html

----------------


##### Part 4 - Import web history data from log file to Hive via Nifi 

- As it1, use beeline or Hive view to create table webtraffic to store the userid and web url enabling transactions and partition into day month year: http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive
 
````
beeline -u 'jdbc:hive2://localhost:10000/default' -n it1 -p '' -e "
create external table weblog(session int,visited timestamp,product string)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS textfile
location '/user/hive/weblog';
"
````

![Image](../master/screenshots/create-webtraffic-table.png?raw=true)


- *TODO* add steps to enable Nifi to tail web log file


- Run the createlog-psql.sh script which will generate  dummy web traffic log events 
```
cd ~/single-view-demo
./createlog-psql.sh /tmp/data/FactSales.csv 10 >> /tmp/webtraffic.log
```
- (Optional) Tailing the webtraffic file in another terminal to see the webtraffic records
```
tail -F /tmp/webtraffic.log
```
- The webtraffic.log should start displaying the webtraffic
```
1,1,2007-09-11 23:22:10,Samsung Galaxy Tab A 8.0
2,1,2007-09-11 23:23:12,Apple iPhone 6S
3,1,2007-09-11 23:23:31,Sony Xperia Z5 Premium
4,1,2007-09-11 23:27:02,Microsoft Lumia 950
```


- After 6-7 min, notice that the script has completed and the webtraffic table now has records created (while waiting you can get your Twitter consumer key/secrets - see part 4)

http://sandbox.hortonworks.com:8080/#/main/views/HIVE/1.0.0/Hive

![Image](../master/screenshots/screenshot-view-webtraffic-data.png?raw=true)

- Open Files view and navigate to /apps/hive/warehouse/webtraffic/year=xxxx/month=xx/day=xx/delta_xxxxxxxxx and view the files

http://sandbox.hortonworks.com:8080/#/main/views/FILES/1.0.0/Files
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS.png?raw=true)

- Notice the table is stored in ORC format
![Image](../master/screenshots/screenshot-view-webtraffic-HDFS-ORC.png?raw=true)



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

- Query most visited product query
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "

select product,count(1) popular 
from weblog group by product order by popular desc limit 10;
"
```

- Query for top products with most time spent by year
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "

select product,year_visited, sum(time_spent) as total_time from
(select table1.product,year(visited) as year_visited, 
month(visited) as month,unix_timestamp(table1.lead_window_0) - unix_timestamp(table1.visited) as time_spent
from (select product,visited,lead(visited)  over (PARTITION BY session) from weblog) table1 
where table1.lead_window_0 is not NULL) table2
group by product,year_visited 
order by total_time desc limit 100;

"
```


- Query for most popular web_path
```
beeline -u 'jdbc:hive2://localhost:10000/default' -n mktg1 -p '' -e "

select web_path,count(1) as path_count
from
(select session,concat_ws("->",collect_list(product)) as web_path
from weblog group by session) table1
group by web_path
order by path_count desc;

"
```


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
  

