package org.springframework.data.marklogic.repository.custom;

import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.pojo.PojoQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.Person;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

public class CustomMarkLogicRepositoryImpl implements CustomMarkLogicRepository {

    @Autowired
    private MarkLogicOperations operations;
    private PojoQueryBuilder<Person> qb;

    @PostConstruct
    public void init() {
        qb = operations.qb(Person.class);
    }

    @Override
    public List<Person> findAllByNameCustom(String name) {
        List<DocumentRecord> records = operations.search(qb.value("name", name));

        return records.stream()
                .map(record -> {
                    // TODO: Determine which type the record is and convert?
                    return record.getContentAs(Person.class);
                }).collect(Collectors.toList());
    }

}
