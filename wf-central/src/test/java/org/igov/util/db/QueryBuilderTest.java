package org.igov.util.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.igov.service.controller.IntegrationTestsApplicationConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.igov.util.db.queryloader.QueryLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.igov.util.db.QueryBuilder.extractParameter;

/**
 * @author dgroup
 * @since 02.09.2015
 */
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("default")
@ContextConfiguration(classes = IntegrationTestsApplicationConfiguration.class)
public class QueryBuilderTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryBuilderTest.class);

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private QueryLoader sqlStorage;

    @Test
    public void parametersExtracting() {
        assertEquals("type_id", extractParameter(" where type_id = :type_id"));
    }

    @Test
    public void smoke() {
        String sql = sqlStorage.get("get_PlaceTree_down_by_id.sql");

        Session session = sessionFactory.openSession();
        QueryBuilder query = new QueryBuilder(session, sql)
                .setParam("placeId", 100)
                .append(" where type_id = :type_id", 5);

        assertThat(query.toString(), containsString("where type_id"));

        LOG.warn(query.toString());
        session.close();
    }
}
