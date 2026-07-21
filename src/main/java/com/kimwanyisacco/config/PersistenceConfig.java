package com.kimwanyisacco.config;

import com.kimwanyisacco.config.ConfigLoader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Since this project deliberately uses plain Spring (not Spring Boot),
 * there is no auto-configuration - every infrastructure bean the app
 * needs (DataSource, EntityManagerFactory, TransactionManager) is wired
 * explicitly here.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.kimwanyisacco.repository")
@ComponentScan(basePackages = {
        "com.kimwanyisacco.config",
        "com.kimwanyisacco.service",
        "com.kimwanyisacco.controller",
        "com.kimwanyisacco.security",
        "com.kimwanyisacco.payment",
        "com.kimwanyisacco.web"
})
@PropertySource("classpath:application.properties")
public class PersistenceConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.driver}")
    private String dbDriver;

    @Value("${hibernate.hbm2ddl.auto}")
    private String hbm2ddlAuto;

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Value("${hibernate.show_sql}")
    private String showSql;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ConfigLoader.get("db.url"));
        config.setUsername(ConfigLoader.get("db.username"));
        config.setPassword(ConfigLoader.get("db.password"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.kimwanyisacco.model.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        jpaProperties.put("hibernate.dialect", hibernateDialect);
        jpaProperties.put("hibernate.show_sql", showSql);
        jpaProperties.put("hibernate.format_sql", "true");
        emf.setJpaProperties(jpaProperties);

        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
