service ntpd stop
ntpdate pool.ntp.org
service ntpd start

storm jar ./target/storm-integration-test-1.0-SNAPSHOT.jar test.HiveTopology thrift://sandbox.hortonworks.com:9083 default user_tweets
