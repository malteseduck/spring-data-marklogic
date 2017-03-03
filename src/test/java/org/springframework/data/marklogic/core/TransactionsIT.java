package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integration.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionsIT {

    private MarkLogicTemplate template;

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();
    }

    private void cleanDb() {
        template.deleteAll(Person.class);
    }

    @Test
    @Transactional
    public void testTransactionRolledBack() throws Exception {
        Person person = new Person("Bob");
        template.write(person);
    }

    // Not the best way, but the simplest, just to test to make sure the @Transactional is working like expected
    @Test
    public void verify() {
        long count = template.count(Person.class);
        cleanDb();

        assertThat(count).as("All writes should be rolled back").isEqualTo(0);
    }


}
