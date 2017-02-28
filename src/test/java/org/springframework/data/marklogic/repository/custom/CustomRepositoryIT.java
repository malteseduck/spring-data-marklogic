package org.springframework.data.marklogic.repository.custom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.marklogic.repository.config.EnableMarkLogicRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CustomRepositoryIT {

    @Configuration
    @EnableMarkLogicRepositories
    @ImportResource("classpath:integration.xml")
    static class Config {}

    @Autowired
    CustomMarkLogicRepository repository;

    @Test
    public void shouldExecuteMethodOnCustomRepositoryImplementation() {
        repository.findAllByNameCustom("bob");
    }
}
