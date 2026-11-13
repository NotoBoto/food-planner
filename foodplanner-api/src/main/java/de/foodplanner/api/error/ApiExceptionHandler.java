package de.foodplanner.api.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

/**
 * Übersetzt Fehler der Anwendungsschicht in {@link ProblemDetail}-Antworten (§5, §5.1).
 * Die Validierung selbst liegt nie im Endpunktcode (§3.2), sondern im {@code @Validated}-Handler;
 * hier wird nur ihre Verletzung als 400 mit {@code errors} je Feld ausgegeben.
 *
 * <p>Issue #25 ergänzt diese Klasse um Bindungsfehler, 404 und 409.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    /**
     * Nur Verletzungen an Methodenparametern sind Fehler der Anfrage. Eine Verletzung am Rückgabewert oder an einem
     * Objekt, das der Server selbst gebaut hat, ist ein Serverfehler; sie wird weitergeworfen und endet als 500.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException exception) {
        if (exception.getConstraintViolations().isEmpty()
                || !exception.getConstraintViolations().stream()
                        .allMatch(violation -> isParameterViolation(violation.getPropertyPath()))) {
            throw exception;
        }
        Map<String, List<String>> messages = new TreeMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            messages.computeIfAbsent(lastNode(violation.getPropertyPath()), key -> new ArrayList<>())
                    .add(violation.getMessage());
        }
        Map<String, String> errors = new TreeMap<>();
        messages.forEach((field, list) -> errors.put(field, String.join("; ", list.stream().sorted().toList())));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Die Anfrage enthält ungültige Werte.");
        problem.setTitle("Ungültige Anfrage");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static boolean isParameterViolation(Path path) {
        for (Path.Node node : path) {
            if (node.getKind() == ElementKind.PARAMETER) {
                return true;
            }
        }
        return false;
    }

    /**
     * Schlüssel ist der Name des letzten benannten Knotens, z. B. {@code pageSize} aus {@code handle.query.pageSize}.
     * Bean-Knoten einer klassenweiten Prüfung haben keinen Namen; dann zählt der Knoten davor, etwa {@code query}.
     */
    private static String lastNode(Path path) {
        String name = "";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                name = node.getName();
            }
        }
        return name;
    }
}
