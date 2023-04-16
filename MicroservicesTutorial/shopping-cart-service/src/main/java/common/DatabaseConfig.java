package common;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import shopping.cart.ShoppingCart;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Configure the necessary components required for integration with Akka Projections */
@Configuration
public class DatabaseConfig {

  private final Config config;
  public DatabaseConfig(Config config) {
    this.config = config;
  }


  /**
   * Returns a {@link DataSource} configured with the settings in {@code
   * jdbc-connection-settings.driver}. See src/main/resources/persistence.conf and
   * src/main/resources/local-shared.conf
   */
  @Bean
  public DataSource dataSource() {

    HikariDataSource dataSource = new HikariDataSource();

    Config jdbcConfig = jdbcConfig();

    // pool configuration
    dataSource.setPoolName("read-side-connection-pool");
    dataSource.setMaximumPoolSize(jdbcConfig.getInt("connection-pool.max-pool-size"));

    long timeout = jdbcConfig.getDuration("connection-pool.timeout", TimeUnit.MILLISECONDS);
    dataSource.setConnectionTimeout(timeout);

    // database configuration
    dataSource.setDriverClassName(jdbcConfig.getString("driver"));
    dataSource.setJdbcUrl(jdbcConfig.getString("url"));
    dataSource.setUsername(jdbcConfig.getString("user"));
    dataSource.setPassword(jdbcConfig.getString("password"));
    dataSource.setAutoCommit(false);

    return dataSource;
  }

  private Config jdbcConfig() {
    return config.getConfig("jdbc-connection-settings");
  }

  /**
   * Additional JPA properties can be passed through config settings under {@code
   * jdbc-connection-settings.additional-properties}. The properties must be defined as key/value
   * pairs of String/String.
   */
  @Bean(name = "jpaProperties")
  Properties jpaProperties() {
    Properties properties = new Properties();

    Config additionalProperties = jdbcConfig().getConfig("additional-properties");
    Set<Map.Entry<String, ConfigValue>> entries = additionalProperties.entrySet();

    for (Map.Entry<String, ConfigValue> entry : entries) {
      Object value = entry.getValue().unwrapped();
      if (value != null) properties.setProperty(entry.getKey(), value.toString());
    }

    return properties;
  }

  @Bean
  public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }
}
