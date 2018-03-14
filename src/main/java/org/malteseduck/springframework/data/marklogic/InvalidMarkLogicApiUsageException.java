package org.malteseduck.springframework.data.marklogic;

import org.springframework.dao.InvalidDataAccessApiUsageException;

public class InvalidMarkLogicApiUsageException extends InvalidDataAccessApiUsageException {

	public InvalidMarkLogicApiUsageException(String msg) {
        super(msg);
	}

	public InvalidMarkLogicApiUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
