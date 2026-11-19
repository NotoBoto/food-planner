package de.foodplanner.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @Test
    void resourceNotFoundIs404() {
        ProblemDetail problem = handler.resourceNotFound(
                new ResourceNotFoundException("Rezept 42 existiert nicht."));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Ressource nicht gefunden");
        assertThat(problem.getDetail()).isEqualTo("Rezept 42 existiert nicht.");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void resourceConflictIs409() {
        ProblemDetail problem = handler.resourceConflict(
                new ResourceConflictException("Rezept existiert bereits."));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Konflikt mit bestehender Ressource");
        assertThat(problem.getDetail()).isEqualTo("Rezept existiert bereits.");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void methodArgumentNotValidCollectsFieldAndGlobalErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "recipeRequest");
        bindingResult.addError(new FieldError("recipeRequest", "title", "darf nicht leer sein"));
        bindingResult.addError(new FieldError("recipeRequest", "title", "muss mindestens 3 Zeichen haben"));
        bindingResult.addError(new ObjectError("recipeRequest", "Startdatum muss vor Enddatum liegen"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ProblemDetail problem = handler.methodArgumentNotValid(exception);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(errors(problem)).containsExactly(
                Map.entry("recipeRequest", "Startdatum muss vor Enddatum liegen"),
                Map.entry("title", "darf nicht leer sein; muss mindestens 3 Zeichen haben"));
    }

    @Test
    void typeMismatchIs400KeyedByParameterName() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "recipeId", mock(MethodParameter.class),
                new IllegalArgumentException("For input string: \"abc\""));

        ProblemDetail problem = handler.typeMismatch(exception);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(errors(problem)).containsExactly(Map.entry("recipeId", "Ungültiger Wert."));
    }

    @Test
    void notReadableBodyIs400WithoutErrorsProperty() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error", mock(HttpInputMessage.class));

        ProblemDetail problem = handler.notReadable(exception);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Ungültige Anfrage");
        assertThat(problem.getDetail()).isEqualTo("Der Anfragekörper ist ungültig oder fehlt.");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> errors(ProblemDetail problem) {
        return (Map<String, String>) Objects.requireNonNull(problem.getProperties()).get("errors");
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
