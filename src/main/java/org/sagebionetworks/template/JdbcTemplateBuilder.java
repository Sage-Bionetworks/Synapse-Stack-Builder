package org.sagebionetworks.template;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

public interface JdbcTemplateBuilder {
    public JdbcTemplate getJdbcTemplate(String endpoint, String user, String password) throws SQLException;
}
