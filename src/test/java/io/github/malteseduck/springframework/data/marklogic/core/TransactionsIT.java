package io.github.malteseduck.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integration.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionsIT {

    private static MarkLogicTemplate template;

    @Rule
    public ExpectedException expectation = ExpectedException.none();

    @AfterClass
    public static void clean() {
        cleanDb();
    }

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    private static void cleanDb() {
        template.dropCollection(Person.class);
    }

    // Not the best way, but the simplest, just to test to make sure the @Transactional is working like expected
    @Test
    @Transactional
    public void testTransactionRolledBack() throws Exception {
        Person person = new Person("Bob");
        template.write(person);
    }

    @Test
    public void testTransactionRolledBackVerify() {
        long count = template.count(Person.class);
        cleanDb();

        assertThat(count).as("All writes should be rolled back").isEqualTo(0);
    }

    @Test
    @Commit
    @Transactional(timeout = 1)
    public void testTransactionTimedOutRolledBack() throws Exception {
        expectation.expect(TransactionSystemException.class);

        Person person = new Person("Bob");
        template.write(person);
        Thread.sleep(3000);
    }

    @Test
    public void testTransactionTimedOutRolledBackVerify() {
        long count = template.count(Person.class);
        cleanDb();

        assertThat(count).as("All writes should be rolled back").isEqualTo(0);
    }
}
