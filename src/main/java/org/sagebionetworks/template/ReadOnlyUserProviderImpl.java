package org.sagebionetworks.template;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

public class ReadOnlyUserProviderImpl implements ReadOnlyUserProvider {

    private final JdbcTemplate jdbcTemplate;

    public ReadOnlyUserProviderImpl(JdbcTemplate template) throws SQLException {
        this.jdbcTemplate = template;
    }

    @Override
    public void createReadOnlyUser(String readOnlyUserName, String userPassword, String schema) {
        String sql = createUserSql(readOnlyUserName, userPassword);
        jdbcTemplate.update(sql);
        sql = grantSelectUserSql(readOnlyUserName, schema);
        jdbcTemplate.update(sql);
    }

    private static final String CREATE_USER_FMT = "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'";
    public static String createUserSql(String userName, String password) {
        String sqlCreateUSer = String.format(CREATE_USER_FMT, userName, password);
        return sqlCreateUSer;
    }

    private static final String GRANT_SELECT_USER_FMT = "GRANT SELECT ON %2$s.* TO '%1$s'@'%%'";
    public static String grantSelectUserSql(String userName, String schema) {
        String sqlGrantUser = String.format(GRANT_SELECT_USER_FMT, userName, schema);
        return sqlGrantUser;
    }

}
