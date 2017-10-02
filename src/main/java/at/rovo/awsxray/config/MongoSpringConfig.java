package at.rovo.awsxray.config;

import at.rovo.awsxray.config.settings.MongoSettings;
import at.rovo.awsxray.domain.CompanyService;
import at.rovo.awsxray.domain.FileService;
import at.rovo.awsxray.domain.UserService;
import at.rovo.awsxray.utils.BigDecimalConverter;
import at.rovo.awsxray.utils.MongoBuilder;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import javax.annotation.Resource;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(MongoSettings.class)
public class MongoSpringConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BCRYPT_ENCODER = "bcrypt_encoder";

    @Resource
    private MongoSettings settings;

    @Bean(name = BCRYPT_ENCODER)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongo() throws MongoException, IOException {
        return new MongoBuilder(settings)
                .withSocketTimeout(60000)
                .withConnectTimeout(15000)
                .withMaxConnectionIdleTime(600000)
                .withReadPreference(ReadPreference.primaryPreferred())
                .build();
    }

    @Bean
    public Morphia morphia() {
        Morphia morphia = new Morphia().mapPackage("at.rovo.awsxray.db.entities.mongo");

        morphia.getMapper().getConverters().addConverter(BigDecimalConverter.class);

        return morphia;
    }

    @Bean
    public Datastore datastore() throws MongoException, UnknownHostException {
        MongoClient mongo = null;
        try {
            mongo = mongo();
        } catch (IOException e) {
            LOG.error("Could not create the local database (embedded MongoDB)", e);
        }

        if (mongo == null) {
            throw new IllegalStateException("Unable to create the MongoClient");
        }

        Datastore datastore = morphia().createDatastore(mongo, settings.getDatabase());

        // Ensure that indexes and caps are applied, but only if it's not then read-only user
        if (StringUtils.isEmpty(settings.getUser()) || !settings.getUser().endsWith("-read")) {
            datastore.ensureIndexes();
            datastore.ensureCaps();
        }

        return datastore;
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public CompanyService companyService() {
        return new CompanyService();
    }

    @Bean
    public FileService fileService() {
        return new FileService();
    }
}
