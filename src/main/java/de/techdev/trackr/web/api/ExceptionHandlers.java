package de.techdev.trackr.web.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * @author Moritz Schulze
 */
@ControllerAdvice
@Slf4j
public class ExceptionHandlers {

    /**
     * This exception handler <i>should</i> handle violations of unique constraints.
     * TODO: JpaSystemException is really broad, we need to check somehow that this only gets invoked for unique constraint violations
     * @param e The exception to handle
     * @return An error message
     */
    @ExceptionHandler(JpaSystemException.class)
    @ResponseBody
    @ResponseStatus(CONFLICT)
    public String handleJpaSystemException(JpaSystemException e) {
        return e.getMostSpecificCause().getMessage();
    }

    /**
     * This is for custom controllers (i.e. not spring-data-rest) that do validation.
     * @param ex The BindException thrown by the controller.
     * @return A map of fieldnames to FieldErrors
     */
    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Map<String, FieldError> handleBindException(BindException ex) {
        Map<String, FieldError> errorMessages = new HashMap<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errorMessages.put(fieldError.getField(), fieldError);
        }
        return errorMessages;
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, FieldError> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, FieldError> errorMessages = new HashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            FieldError fieldError = toFieldError(violation);
            errorMessages.put(fieldError.getField(), fieldError);
        }
        return errorMessages;
    }

    /**
     * This handler is needed because Spring hides the {@link javax.validation.ConstraintViolationException} in a {@link org.springframework.transaction.TransactionSystemException}
     * if postgres is used.
     * <p>
     * In case the TransactionSystemException contains a ConstraintViolationException it gets converted to JSON errors with
     * {@link #handleConstraintViolationException(javax.validation.ConstraintViolationException)}
     * <p>
     * <a href="http://forum.spring.io/forum/spring-projects/data/124385-difference-in-exception-handling-between-postgresql-and-hsql">See here (spring.io forum)</a>
     *
     * @param e The exception to be handled
     * @return If the exception has a ConstraintViolationException as the second root cause, the errors from that.
     */
    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(TransactionSystemException.class)
    public Map<String, FieldError> handleTransactionSystemException(TransactionSystemException e) {
        if (e.getCause() != null && e.getCause().getCause() != null && ConstraintViolationException.class.isAssignableFrom(e.getCause().getCause().getClass())) {
            log.debug("Extracting ConstraintViolationException from TransactionSystemException");
            return handleConstraintViolationException((ConstraintViolationException) e.getCause().getCause());
        }
        return null;
    }

    protected FieldError toFieldError(ConstraintViolation<?> violation) {
        String beanName = violation.getRootBeanClass().getSimpleName();
        String path = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object invalidValue = violation.getInvalidValue();
        return new FieldError(beanName, path, invalidValue, false, new String[]{}, new Object[]{}, message);
    }

}