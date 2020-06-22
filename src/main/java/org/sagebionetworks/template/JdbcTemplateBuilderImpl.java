package org.sagebionetworks.template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.SQLException;

public class JdbcTemplateBuilderImpl implements JdbcTemplateBuilder {

    @Override
    public JdbcTemplate getJdbcTemplate(String endpoint, String user, String password) throws SQLException {
        SimpleDriverDataSource dataSource = setupDataSource(user, password, endpoint);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        return template;
    }

    private static SimpleDriverDataSource setupDataSource(String user, String pwd, String dbEndpoint) throws SQLException {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql://" + dbEndpoint);
        dataSource.setUsername(user);
        dataSource.setPassword(pwd);
        return dataSource;
    }

}
