package org.springframework.data.marklogic.repository.custom;

import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.marklogic.client.query.StructuredQueryBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicOperations;

import java.util.List;
import java.util.stream.Collectors;

public class PersonRepositoryImpl implements PersonRepositoryCustom {

    private MarkLogicOperations operations;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    PersonRepositoryImpl(MarkLogicOperations operations) {
        this.operations = operations;
    }

    @Override
    public List<Person> findAllPersons() {

        List<DocumentRecord> records = operations.search(
                operations.sortQuery(
                        new Sort("name"),
                        qb.and(
                                qb.collection("Employee", "Contact"),
                                qb.directory(true, "/")
                        )
                )
        );

        return records.stream()
                .map(record -> record.getContent(new JacksonDatabindHandle<>(Person.class)).get())
                .collect(Collectors.toList());
    }
}
