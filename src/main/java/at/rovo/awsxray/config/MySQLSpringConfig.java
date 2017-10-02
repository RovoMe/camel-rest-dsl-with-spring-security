package at.rovo.awsxray.config;

import at.rovo.awsxray.config.settings.MySQLSettings;
import at.rovo.awsxray.domain.AuditLogService;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(MySQLSettings.class)
public class MySQLSpringConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private MySQLSettings mySQLSettings;

    @Bean
    public DataSource messageDataSource() {
        SimpleDriverDataSource db = new SimpleDriverDataSource();
        db.setDriverClass(com.mysql.jdbc.Driver.class);
        db.setUrl("jdbc:mysql://" + mySQLSettings.getHost() + ":" + mySQLSettings.getPort() + "/" + mySQLSettings.getDatabase() + "?useSSL=false");
        LOG.debug("Selecting DB {} as datasource", mySQLSettings.getDatabase());
        db.setUsername(mySQLSettings.getUser());
        return db;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(messageDataSource());
    }

    @Bean
    public Properties hibernateProperties() {
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.hbm2ddl.auto", "create");
        return props;
    }

    @Bean(name = "entityManagerFactory")
    public EntityManagerFactory entityManagerFactory() throws Exception {
        LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(messageDataSource());
        entityManagerFactory.setPackagesToScan("at.rovo.awsxray.domain.entities.jpa");
        entityManagerFactory.setPersistenceProvider(new HibernatePersistenceProvider());
        entityManagerFactory.setPersistenceUnitName("pun");
        entityManagerFactory.setPersistenceXmlLocation("classpath:META-INF/persistence-config.xml");
        entityManagerFactory.setJpaProperties(hibernateProperties());
        entityManagerFactory.afterPropertiesSet();
        return entityManagerFactory.getObject();
    }

    @Bean(name = "dbTransactionManager")
    public PlatformTransactionManager transactionManager() throws Exception {
        JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory());
        transactionManager.setDataSource(messageDataSource());
        transactionManager.setJpaDialect(new HibernateJpaDialect());
        return transactionManager;
    }

    @Bean
    public AuditLogService auditLogService() {
        return new AuditLogService();
    }
}
