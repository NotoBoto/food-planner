package de.foodplanner.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

/** Welche Verletzungen als 400 gelten und unter welchem Schlüssel sie stehen (§5, §5.1). */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void parameterViolationIs400KeyedByLastNode() {
        ProblemDetail problem = handler.constraintViolation(exception(violation("muss zwischen 1 und 100 liegen",
                node("handle", ElementKind.METHOD), node("query", ElementKind.PARAMETER),
                node("pageSize", ElementKind.PROPERTY))));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(errors(problem)).containsExactly(Map.entry("pageSize", "muss zwischen 1 und 100 liegen"));
    }

    @Test
    void unnamedBeanNodeFallsBackToPrecedingName() {
        ProblemDetail problem = handler.constraintViolation(exception(violation("ungültig",
                node("handle", ElementKind.METHOD), node("query", ElementKind.PARAMETER),
                node(null, ElementKind.BEAN))));

        assertThat(errors(problem)).containsExactly(Map.entry("query", "ungültig"));
    }

    @Test
    void returnValueViolationIsRethrown() {
        ConstraintViolationException exception = exception(violation("darf nicht null sein",
                node("handle", ElementKind.METHOD), node("<return value>", ElementKind.RETURN_VALUE)));

        assertThatThrownBy(() -> handler.constraintViolation(exception)).isSameAs(exception);
    }

    @Test
    void beanViolationWithoutParameterIsRethrown() {
        ConstraintViolationException exception = exception(violation("darf nicht leer sein",
                node("name", ElementKind.PROPERTY)));

        assertThatThrownBy(() -> handler.constraintViolation(exception)).isSameAs(exception);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> errors(ProblemDetail problem) {
        return (Map<String, String>) problem.getProperties().get("errors");
    }

    private static ConstraintViolationException exception(ConstraintViolation<?> violation) {
        return new ConstraintViolationException(Set.of(violation));
    }

    private static ConstraintViolation<?> violation(String message, Path.Node... nodes) {
        Path path = mock(Path.class);
        List<Path.Node> list = Arrays.asList(nodes);
        when(path.iterator()).thenAnswer(invocation -> list.iterator());
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static Path.Node node(String name, ElementKind kind) {
        Path.Node node = mock(Path.Node.class);
        when(node.getName()).thenReturn(name);
        when(node.getKind()).thenReturn(kind);
        return node;
    }
}
