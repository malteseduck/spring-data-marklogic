package org.springframework.data.marklogic.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.marklogic.core.mapping.*;

@Document(type = "Person")
public class PersonToStream {

    // TODO: Should it be required to specify this?  It could be worked around, if necessary
    // Need to identify the id property in the document
    @Id
    private String id;

    // Not deserializing into a POJO, but this allows us to do JPA-like queries against gender
    private String gender;

    // There is no actual property "petsName" in the JSON, this lets us specify an index for sorting
    @Indexed(path = "/pets/name")
    private String petsName;

    // Can override the default behavior of using a path range index for sorting, even though not using this as a POJO
    @Indexed(type = IndexType.ELEMENT)
    private String name;
}
