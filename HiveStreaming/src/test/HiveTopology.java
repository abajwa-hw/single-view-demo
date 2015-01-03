package test;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

//import storm.kafka.BrokerHosts;
//import storm.kafka.ZkHosts;
//import storm.kafka.SpoutConfig;
//import storm.kafka.KafkaSpout;
//import backtype.storm.spout.RawMultiScheme;
//import backtype.storm.spout.SchemeAsMultiScheme;
import test.TwitterScheme;

import org.apache.storm.hive.bolt.mapper.DelimitedRecordHiveMapper;
import org.apache.storm.hive.common.HiveOptions;
import org.apache.storm.hive.bolt.HiveBolt;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import twitter4j.*;
import java.util.*;
import twitter4j.conf.ConfigurationBuilder;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;


public class HiveTopology {
	static final String TWITTER_CONSUMER_KEY = "vq6tUwExEkkYR6PbgYwmBitRb";
	static final String TWITTER_CONSUMER_SECRET = "wSd3AKyIsPwNN3oNZAQpPeBLWYL1kGiSB8nw8qFchqVqyDwwHz";
	static final String	TWITTER_TOKEN = "2885109412-Qj0NvQ96qmixniPEOdowWwUt4uuAhZw90eXKhbk";
	static final String	TWITTER_TOKEN_SECRET = "59LN6pVwIB3cS5Nz2b8XEsqozTa4O1olp3fVMYIUtGI8q";
	
						
    static final String USER_SPOUT_ID = "twitter-spout";
    static final String KAFKA_SPOUT_ID = "kafka-spout";
    static final String BOLT_ID = "my-hive-bolt";
    static final String TOPOLOGY_NAME = "hive-twitter-topology";
    static final int TOPOLOGY_TIMEOUT_SECS = 120;
 	static final String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
 	static final int TWITTER4J_USERS_LIMIT = 400;
 	

    public static void main(String[] args) throws Exception {
        String metaStoreURI = args[0];
        String dbName = args[1];
        String tblName = args[2];
        String[] colNames = {"twitterid","userid","displayname","created","language","tweet"};
        Config config = new Config();
        config.setNumWorkers(1);
        UserDataSpout spout = new UserDataSpout();
        DelimitedRecordHiveMapper mapper = new DelimitedRecordHiveMapper()
            .withColumnFields(new Fields(colNames));
        HiveOptions hiveOptions;
        if (args.length == 6) {
            hiveOptions = new HiveOptions(metaStoreURI,dbName,tblName,mapper)
                .withTxnsPerBatch(10)
                .withBatchSize(10)
                .withIdleTimeout(20)
                .withKerberosKeytab(args[4])
                .withKerberosPrincipal(args[5]);
        } else {
            hiveOptions = new HiveOptions(metaStoreURI,dbName,tblName,mapper)
                .withTxnsPerBatch(10)
                .withBatchSize(10)
                .withIdleTimeout(20);
        }

			
        HiveBolt hiveBolt = new HiveBolt(hiveOptions);
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(USER_SPOUT_ID, spout, 1);
        
		//Setup Kafka spout
		//BrokerHosts hosts = new ZkHosts("sandbox.hortonworks.com:2181");
		//String topic = "twitter_events";
		//String zkRoot = "";
		//String consumerGroupId = "group1";
		//SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, zkRoot, consumerGroupId);
		//spoutConfig.scheme = new RawMultiScheme();
		//spoutConfig.scheme = new SchemeAsMultiScheme(new TwitterScheme());
		//KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
		//builder.setSpout(KAFKA_SPOUT_ID, kafkaSpout);		
        
        // SentenceSpout --> MyBolt
        builder.setBolt(BOLT_ID, hiveBolt, 1)
                .shuffleGrouping(USER_SPOUT_ID);
        //builder.setBolt(BOLT_ID, hiveBolt, 1)
        //        .shuffleGrouping(KAFKA_SPOUT_ID);
        
        if (args.length == 3) {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
            /*waitForSeconds(TOPOLOGY_TIMEOUT_SECS);
            cluster.killTopology(TOPOLOGY_NAME);
            System.out.println("Begin shutdown of cluster because timeout of " + TOPOLOGY_TIMEOUT_SECS + " seconds reached");
            cluster.shutdown();
            System.out.println("cluster shutdown because timeout of " + TOPOLOGY_TIMEOUT_SECS + " seconds reached");
            System.exit(0); */
        } else if(args.length >= 4) {
            StormSubmitter.submitTopology(args[3], config, builder.createTopology());
        } else {
            System.out.println("Usage: HiveTopology metastoreURI dbName tableName [topologyNamey] [keytab file] [principal name]");
        }
    }

	private static long[] FetchUserIds() throws SQLException{
		 List<String> userIds = new ArrayList<String>();

		try {
		  Class.forName(DRIVER_NAME);
		} catch (ClassNotFoundException e) {
		  e.printStackTrace();
		  System.exit(1);
		}

		System.out.println("Opening JDBC Hive connection....");
		Connection connect = DriverManager.getConnection("jdbc:hive2://localhost:10000/default", "hive","");
		System.out.println("Connected. Creating statement....");
		Statement state = connect.createStatement();

		//people_id, ssn, id2, id3
		//String idcolumn = "people_id";
		//String query = "select " + idcolumn + " from persons sort by " + idcolumn + " desc limit " + TWITTER4J_USERS_LIMIT;
		String query = "select people_id from persons";
		System.out.println("Fetching ids of persons");
		ResultSet res = state.executeQuery(query);
		while (res.next()) {
			//long l = Long.valueOf(res.getString(1));
			userIds.add(res.getString(1));
			
		}

		 long [] luserIds = new long[userIds.size()];
		 for (int i = 0; i < userIds.size(); i++) {     		 	
   		 	luserIds[i] = Long.valueOf(userIds.get(i)).longValue();     
		}   		 
		 
		return luserIds;
	}
	
    public static void waitForSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
        }
    }

    public static class UserDataSpout extends BaseRichSpout {
        private ConcurrentHashMap<UUID, Values> pending;
        private SpoutOutputCollector collector;
        private List<String> tweetList = new ArrayList<String>();
        private int index = 0;
        private int userindex = 0;
        long[] userIds = null;

        
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("twitterid","userid","displayname","created","language","tweet"));
            System.out.println("Declared fields");
        }

        public void open(Map config, TopologyContext context,
                         SpoutOutputCollector collector) {

			
            this.collector = collector;
            this.pending = new ConcurrentHashMap<UUID, Values>();

			try{
				userIds = FetchUserIds();
				//for (long user : userIds){
				//	System.out.println("userid: " + user);
				//}
				System.out.println("Found " + userIds.length + " userids");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		
			//initialize Twitter4J 
			ConfigurationBuilder cb = new ConfigurationBuilder();

			cb.setOAuthConsumerKey(TWITTER_CONSUMER_KEY)
					.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET)
					.setOAuthAccessToken(TWITTER_TOKEN)
					.setOAuthAccessTokenSecret(TWITTER_TOKEN_SECRET);

			TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
			twitterStream.addListener(new StatusListener(){
			        	        	
					public void onStatus(Status status) {
						
						String userid = Long.toString(status.getUser().getId());
						String displayname = status.getUser().getScreenName().replace("\n", "").replace("\r", "").replace("|","").replace(",","");
						String tweet = status.getText().replace("\n", "").replace("\r", "").replace("|","").replace(",","");
						Date created = status.getCreatedAt();
						String fullTweet = status.toString().replace("\n", "").replace("\r", "").replace("|","");
						String language = status.getLang();	 

						String inReplyToScreenName = status.getInReplyToScreenName();
						double longitude = 0; 
						double latitude = 0; 
						String address = "";
						String country = "";
						if (status.getGeoLocation() != null){
							longitude = status.getGeoLocation().getLongitude();
							latitude = status.getGeoLocation().getLatitude();
							Place p = status.getPlace();
							address = p.getStreetAddress();
							country = p.getCountry();
						}
								
				
						//System.out.format("*****Found tweet userid: %s displayname: %s lang: %s inreplyto: %s date: %s address/country %s / %s tweet: %s \n", userid, displayname, language, inReplyToScreenName, created.toString(), address, country, tweet);
						tweetList.add(userid+"||"+displayname+"||"+created+"||"+language+"||"+tweet);
				
						//KeyedMessage<String, String> data = new KeyedMessage<String, String>("twitter_events",userid+"||"+displayname+"||"+tweet+"||"+created+"||"+longitude+"||"+latitude+"||"+language+"||"+fullTweet);
						//producer.send(data);
					}
			
					public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
					public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
					public void onException(Exception ex) {
							ex.printStackTrace();
					 }
					public void onStallWarning(StallWarning warning) {
						System.out.println("Got stall warning:" + warning);
					}
					public void onScrubGeo(long userid, long upToStatusId) {
						System.out.println("Got scrub_geo event userid:" + userid + " upToStatusId:" + upToStatusId);
					}

			});
				
        			

			//String[] hashtags = {"$AAPL","$HDP","$GOOG"};
			String[] hashtags = {};
	
			//String[] languages = {"en"};
			String[] languages = {};
			
			//double[][] location = new double[][]{new double[]{-122.75,36.8}, new double[]{-121.75,37.8}};
			double[][] location = new double[][]{};

			//create a query to filter the tweets by hashtag and geolocation
			FilterQuery tweetFilterQuery = new FilterQuery();
		
			boolean useFilter = false;
			
			//if any of the filter parameters set, use a filter.
			//note that if using filters, at least one hashtag is required
			if (hashtags.length > 0){
				System.out.println("Filtering by hashtag(s): " + Arrays.toString(hashtags));
				tweetFilterQuery.track(hashtags);
				useFilter = true;
			}
			if (location.length > 0) {
				System.out.println("Filtering by location(s): " + Arrays.toString(location));
				tweetFilterQuery.locations(location);
				useFilter = true;
			}

			if (languages.length > 0 ){
				System.out.println("Filtering by language(s): " +Arrays.toString(languages));
				tweetFilterQuery.language(languages);
				useFilter = true;
			}

			//For demo purposes, we will not be filtering by userids, since we don't have real Twitter Ids for the users in question
			//
			//if (userIds.length > 0) {
				//tweetFilterQuery.follow(userIds);
				//useFilter = true;
			//}	

			if (useFilter){
				System.out.println("Starting Twitter stream with filters");
				twitterStream.filter(tweetFilterQuery);
			}
			//otherwise just read TwitterStream
			else{
				System.out.println("Starting Twitter stream without filters");
				// start listening on random sample of all public statuses
				twitterStream.sample();
			}
            
        }

        public void nextTuple() {
        	if (tweetList.size() > index){

				if (userindex > userIds.length - 1)
					userindex = 0;
				
				String [] user = tweetList.get(index).split("\\|\\|");
				//System.out.println("Language is " + user[3]);
				if (user[3].equals("en")){
				//For demo purposes, we emit a random userId from persons table along with the Twitter info to later  be able to query across multiple table
				System.out.format("****Emitting tweet: twitterid: %s userid: %s displayname: %s  date: %s lang: %s tweet: %s \n", user[0], String.valueOf(userIds[userindex]), user[1], user[2], user[3],user[4]);				
				Values values = new Values(user[0],userIds[userindex], user[1],user[2],user[3],user[4]);
				
				UUID msgId = UUID.randomUUID();
				this.pending.put(msgId, values);
				this.collector.emit(values, msgId);
				}
				index++;
				userindex++;
				
            }
            
        }

        public void ack(Object msgId) {
            this.pending.remove(msgId);
        }

        public void fail(Object msgId) {
            System.out.println("**** RESENDING FAILED TUPLE");
            this.collector.emit(this.pending.get(msgId), msgId);
        }
    }
}

