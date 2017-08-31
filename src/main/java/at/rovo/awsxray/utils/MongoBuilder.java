package at.rovo.awsxray.utils;

import at.rovo.awsxray.config.settings.MongoSettings;
import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MongoSettings settings;
    private final MongoClientOptions.Builder optionsBuilder;

    public MongoBuilder(MongoSettings settings) {
        LOG.info("Creating a MongoBuilder for {}", settings.toString());
        this.settings = settings;
        this.optionsBuilder = MongoClientOptions.builder();
    }

    public MongoBuilder withReadPreference(ReadPreference preference) {
        optionsBuilder.readPreference(preference);
        return this;
    }

    public MongoBuilder withMaxConnectionIdleTime(int idleTime) {
        optionsBuilder.maxConnectionIdleTime(idleTime);
        return this;
    }

    public MongoBuilder withSocketTimeout(int timeout) {
        optionsBuilder.socketTimeout(timeout);
        return this;
    }

    public MongoBuilder withConnectTimeout(int timeout) {
        optionsBuilder.connectTimeout(timeout);
        return this;
    }

    public MongoClient build() {

        int port = settings.getPort();

        MongoClientOptions mongoOptions = optionsBuilder.build();
        MongoClient mongo;
        MongoCredential credential = null;

        boolean useCredentials = !Strings.isNullOrEmpty(settings.getUser()) && !Strings.isNullOrEmpty(settings.getPassword());

        if (useCredentials) {
            credential = MongoCredential.createScramSha1Credential(
                    settings.getUser(),
                    settings.getDatabase(),
                    settings.getPassword().toCharArray());
        }

        // Check if we have a single instance or a replica-set
        if (!StringUtils.isEmpty(settings.getHost())) {
            this.optionsBuilder.writeConcern(WriteConcern.JOURNALED);
            if (useCredentials) {
                mongo = new MongoClient(new ServerAddress(settings.getHost(), port), Collections.singletonList(credential), mongoOptions);
            } else {
                mongo = new MongoClient(new ServerAddress(settings.getHost(), port), mongoOptions);
            }

            LOG.info("Connecting to MongoDB: " + settings.getDatabase() + " on " + settings.getHost() + ":" + settings.getPort());
        } else {
            this.optionsBuilder.writeConcern(WriteConcern.MAJORITY.withWTimeout(15, TimeUnit.SECONDS).withJournal(true));
            List<ServerAddress> address = new ArrayList<>();

            LOG.debug("Mongo settings uses a replica set with {} nodes", settings.getReplica().length);
            for (String addr : settings.getReplica()) {
                LOG.debug("Adding mongodb server: {}:{}", addr, port);
                address.add(new ServerAddress(addr, port));
            }

            if (useCredentials) {
                mongo = new MongoClient(address, Collections.singletonList(credential), mongoOptions);
            } else {
                mongo = new MongoClient(address, mongoOptions);
            }
            LOG.info("Connecting to MongoDB: " + settings.getDatabase() + " on " + settings.getReplicaSetAsString() + ":" + settings.getPort());
        }

        return mongo;
    }
}
