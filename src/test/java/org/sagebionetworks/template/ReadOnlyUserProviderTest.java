package org.sagebionetworks.template;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyUserProviderTest {

    @Mock
    JdbcTemplate mockTemplate;
    @Captor
    ArgumentCaptor<String> sqlCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this.getClass());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreateReadOnlyUser() throws Exception {
        ReadOnlyUserProviderImpl provider = new ReadOnlyUserProviderImpl(mockTemplate);
        provider.createReadOnlyUser("user", "password", "schema");
        verify(mockTemplate, times(2)).update(sqlCaptor.capture());
        List<String> queries = sqlCaptor.getAllValues();
        assertEquals("CREATE USER IF NOT EXISTS 'user'@'%' IDENTIFIED BY 'password'", queries.get(0));
        assertEquals("GRANT SELECT ON schema.* TO 'user'@'%'", queries.get(1));
    }

    @Test
    public void testDropReadOnlyUser() throws Exception {
        ReadOnlyUserProviderImpl provider = new ReadOnlyUserProviderImpl(mockTemplate);
        provider.dropReadOnlyUser("user", "schema");
        verify(mockTemplate).update(sqlCaptor.capture());
        String query = sqlCaptor.getValue();
        assertEquals("DROP USER IF EXISTS 'user'@'%'", query);
    }

}