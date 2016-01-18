## HDP Single view demo


#### Lab overview
In this lab we demonstrate how HDP sandbox can be used to build a single view of a product or customer

High level steps:
- 1. Start HDP 2.3 sandbox and enable Hive features like transactions, queues, preemption, Tez and sessions
- 2. Sqoop - import CRM/ERP data from DB/EDW into Hive 
- 3. Nifi/Flume - Import simulated web traffic logs into Hive
- 4. Nifi/Storm - import related tweets into Hive  
- 5. Analyze tables to populate statistics
- 6. Use Hive view to correlate the data from multiple data sources

This is available in 2 flavors:
  - Zeppelin notebook
  - Manual lab 


#### Zeppelin Notebook

- For demo purposes and those enrolled into the "Technical Sales Professional" course, a simplified version of this is available as a Zeppelin notebook:
  - Web version (readonly) available [here](https://www.zeppelinhub.com/viewer/notebooks/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2hvcnRvbndvcmtzLWdhbGxlcnkvemVwcGVsaW4tbm90ZWJvb2tzL21hc3Rlci8yQkJCVzc1VlMvbm90ZS5qc29u)
  - Code available [here](https://raw.githubusercontent.com/hortonworks-gallery/zeppelin-notebooks/master/2BBBW75VS/note.json)

- Pre-reqs:
  - HDP 2.3.2 sandbox (or later) from [here](http://hortonworks.com/sandbox) where Zeppelin is already installed 
  OR
  - Ambari installed HDP cluster 
  
- To Install:
  - To install this notebook on current [HDP sandbox](http://hortonworks.com/sandbox) where Zeppelin is already installed:
  ```
  curl -sSL https://raw.githubusercontent.com/hortonworks-gallery/zeppelin-notebooks/master/update_all_notebooks.sh | sudo -u zeppelin -E sh
  ```
  - Otherwise, to install Zeppelin on a cluster (or older sandbox): [install Zeppelin via Ambari](https://github.com/hortonworks-gallery/ambari-zeppelin-service). In this case, this notebook will automatically be installed as one of the demo notebooks.
  
- Now launch Zeppelin on your sandbox/cluster, open the "Single view" notebook and follow the instructions there and execute the cells one by one

#### Manual Lab

- Lab for HDP 2.3 sandbox/PostGres can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-psql-advanced-23.md)
  - This includes security features via Ranger


##### Older labs

- Lab for HDP 2.3 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-advanced-23.md)
- Basic lab for HDP 2.3 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-23.md)
- Basic lab for HDP 2.2 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-22.md)
- Basic lab for HDP 2.1 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-21.md)