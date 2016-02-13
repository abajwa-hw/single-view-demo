## HDP Single view demo


#### Overview
In this demo/lab we demonstrate how HDP sandbox + HDF can be used to build a single view of a product or customer

High level steps:
- 1. Start HDP 2.3 sandbox and complete pre-req steps
- 2. Sqoop - import CRM/ERP data from DB/EDW into Hive 
- 3. Nifi/Flume - Import simulated web traffic logs into Hive
- 4. Nifi/Storm - import related tweets into Hive  
- 5. Analyze tables to populate statistics
- 6. Use Zeppelin/Hive view to correlate the data from multiple data sources


Components used:
 - HDP
   - Ambari
   - Hive
   - Sqoop
   - Postgres/Mysql
   - YARN/Ranger (in the workshop version)
 - Zeppelin     
 - HDF


The materials is available in 2 flavors:
  - Zeppelin notebook demo
  - Hands on Lab/Workshop 

Notes:
  - The data set being downloaded to /tmp is ~500MB so if you are re-using a sandbox VM, you may need to remove unused files to cleanup space 
  - To check space requirement: run `df -h /`


## Step 1: Download and install the HDP Sandbox

Download and install the HDP 2.3 Sandbox on your laptop by following these instructions:

<http://hortonworks.com/products/hortonworks-sandbox/#install>

The Sandbox requires 8GB or RAM and 50GB of hard disk space. If your laptop doesn’t have sufficient RAM, you can launch it on Microsoft Azure cloud for free. Azure gives a free 30-day trial. Sign up for the free Azure trial and follow these instructions to install the Sandbox:

<http://hortonworks.com/blog/hortonworks-sandbox-with-hdp-2-3-is-now-available-on-microsoft-azure-gallery/> 

You’ll be asked to specify the VM size while creating the Sandbox on Azure. Please make sure to use A4 or higher VM sizes as the Sandbox is quite resource-intensive. Note down the VM username and password as you'll need it to log in to the Sandbox later.



## Step 2: Log in as root user 

You'll need root priveleges to finish this lab. There are different ways to log into the Sandbox as the root user, depending on whether it's running locally on your laptop or if it's running on Azure.

### Local Sandbox
Start the sandbox, and note its IP address shown on the VBox/VMware screen. Add the IP address into your laptop's "hosts" file so you don't need to type the Sandbox's IP address and can reach it much more conveniently at "sandbox.hortonworks.com". You will need to add a line similar to this in your hosts file:

```
192.168.191.241 sandbox.hortonworks.com sandbox    
```

Note: The IP address will likely be different for you, use that. 

The hosts file is located at `/etc/hosts` for Mac and Linux computers. For Windows it's normally at `\WINDOWS\system32\drivers\etc`. If it's not there, create one and add the entry as shown above.

The username for SSH is "root", and the password is "hadoop". You'll be the root user once logged in. Mac and Linux users can log in by:


```
$ ssh root@sandbox.hortonworks.com

```

Windows users can use the Putty client to SSH using "root" and "hadoop" as the username and password respectively. The hostname is "sandbox.hortonworks.com"

### Azure Sandbox
Note down the public IP address of the Sandbox VM from the Azure Portal UI. Add the IP address into your laptop's "hosts" file so you don't need to type the Sandbox's IP address and can reach it much more conveniently at "sandbox.hortonworks.com". You will need to add a line similar to this in your hosts file:

```
10.144.39.48 sandbox.hortonworks.com sandbox    
```

Note: The IP address will likely be different for you, use that. 

The hosts file is located at `/etc/hosts` for Mac and Linux computers. For Windows it's normally at `\WINDOWS\system32\drivers\etc`. If it's not there, create one and add the entry as shown above. 

Now we can log in to the sandbox.

Using the username which you entered while deploying the Sandbox on Azure, Mac and Linux users can log in by:

```
$ ssh <your_vm_username>@sandbox.hortonworks.com

```
The password is the VM password you specified while deploying the VM.

Windows users can use the Putty client to SSH and log in to "sandbox.hortonworks.com". Use the username and password you specified while creating the VM for SSH credentials. Once logged in, you need to assume the root user identity. Do this:

```
sudo su
```
Again, enter the same password you used to SSH in. Now you should be the root user.


### Single View demo steps

#### Option 1: Demo using Zeppelin Notebook

This is a simplified version of the lab available as a Zeppelin notebook that automates the steps.

- Audience:
  - For those looking for an automated 'single view' demo with minimal CLI steps required
  - For students enrolled into the "Technical Sales Professional" course

- Materials:
  - Web version (readonly) of notebook available [here](https://www.zeppelinhub.com/viewer/notebooks/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2hvcnRvbndvcmtzLWdhbGxlcnkvemVwcGVsaW4tbm90ZWJvb2tzL21hc3Rlci8yQkJCVzc1VlMvbm90ZS5qc29u)
  - Code available [here](https://raw.githubusercontent.com/hortonworks-gallery/zeppelin-notebooks/master/2BBBW75VS/note.json)

- Pre-reqs:
  - HDP 2.3.2 sandbox (or later) where Zeppelin is already installed. Can be downloaded from [here](http://hortonworks.com/sandbox)
  - OR
  - Ambari installed HDP cluster 
  
- To Install:
  - To install this notebook on current [HDP sandbox](http://hortonworks.com/sandbox) (where Zeppelin is already installed):
  ```
  curl -sSL https://raw.githubusercontent.com/hortonworks-gallery/zeppelin-notebooks/master/update_all_notebooks.sh | sudo -u zeppelin -E sh
  ```
  - Otherwise, to install Zeppelin on a cluster (or older sandbox) follow steps to: [install Zeppelin via Ambari](https://github.com/hortonworks-gallery/ambari-zeppelin-service). In this case, this notebook will automatically be installed as one of the demo notebooks.
  
- Now run through the 'Single view demo' notebook:
  - launch Zeppelin UI on your sandbox/cluster e.g. http://sandbox.hortonworks.com:9995
  - open the "Single view" notebook 
  - run the setup steps
  - execute the cells one by one

#### Option 2: Lab/Workshop

This is a longer version of the demo available as a hand-on half-day lab/workshop. This option includes features not available in the demo version:
  - YARN features (queues, preemption) 
  - Hive features (transactions and sessions)
  - security features (authorization policies and audit via Ranger)

- Audience:
  - For those looking for a hands-on half-day lab/workshop that requires CLI usage and covers more feautures
  
- Materials:   
  - Lab steps for HDP 2.3 sandbox/PostGres can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-psql-advanced-23.md)


###### Older labs

- Lab steps for HDP 2.3 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-advanced-23.md)
- Basic lab steps for HDP 2.3 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-23.md)
- Basic lab steps for HDP 2.2 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-22.md)
- Basic lab steps for HDP 2.1 sandbox/MySQL can be found [here](https://github.com/abajwa-hw/single-view-demo/blob/master/singleview-mysql-basic-21.md)