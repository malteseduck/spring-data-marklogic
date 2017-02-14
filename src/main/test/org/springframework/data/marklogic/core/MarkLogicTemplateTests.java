package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InfrastructureConfiguration.class)
public class MarkLogicTemplateTests {

    private MarkLogicTemplate template;

    @Autowired
    public void setClient(DatabaseClient client) {
        this.template = new MarkLogicTemplate(client);
    }

    @Test
    public void testSimpleWrite() {
        Person person = new Person("Bob");

        this.template.write(person);

        Person saved = this.template.read(person.getId(), Person.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(person.getId());
    }

}
