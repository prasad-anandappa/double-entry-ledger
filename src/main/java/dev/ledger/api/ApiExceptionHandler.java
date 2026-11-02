package dev.ledger.api;

import dev.ledger.application.DuplicateAccountCodeException;
import dev.ledger.application.IdempotencyConflictException;
import dev.ledger.application.ResourceNotFoundException;
import dev.ledger.domain.InvalidLineException;
import dev.ledger.domain.UnbalancedEntryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain/application failures to RFC 7807 problem details with
 * deliberate status codes: 404 unknown resource, 409 conflicts
 * (duplicate code, idempotency-key reuse), 422 well-formed requests that
 * break a ledger rule, 400 malformed input (e.g. a bad statement CSV).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({DuplicateAccountCodeException.class, IdempotencyConflictException.class})
    ProblemDetail conflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({UnbalancedEntryException.class, InvalidLineException.class})
    ProblemDetail unprocessable(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        return body;
    }
}
