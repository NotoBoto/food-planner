package de.foodplanner.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;

/** Null-Annotationen der Records landen richtig in der Spezifikation, auch bei Feldern, die selbst ein Record sind. */
class RecordSchemaConverterTest {

    record Inner(String value) {
    }

    record Outer(String name, @Nullable String label, @Nullable Inner optionalInner, Inner inner) {
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> schemas() {
        ModelConverters converters = new ModelConverters(true);
        converters.addConverter(new RecordSchemaConverter());
        return converters.readAll(Outer.class);
    }

    private static Schema<?> property(String name) {
        return (Schema<?>) schemas().get("Outer").getProperties().get(name);
    }

    @Test
    void everyRecordFieldIsRequired() {
        assertThat(schemas().get("Outer").getRequired())
                .containsExactlyInAnyOrder("name", "label", "optionalInner", "inner");
    }

    @Test
    void nullableSimpleFieldAllowsNull() {
        assertThat(property("label").getTypes()).containsExactlyInAnyOrder("string", "null");
        assertThat(property("name").getTypes()).containsExactly("string");
    }

    @Test
    void nullableRecordFieldBecomesAnyOfWithNull() {
        Schema<?> optionalInner = property("optionalInner");

        assertThat(optionalInner.get$ref()).isNull();
        assertThat(optionalInner.getAnyOf()).hasSize(2);
        assertThat(optionalInner.getAnyOf().get(0).get$ref()).isEqualTo("#/components/schemas/Inner");
        assertThat(optionalInner.getAnyOf().get(1).getTypes()).containsExactly("null");
    }

    @Test
    void nonNullableRecordFieldStaysAPlainReference() {
        Schema<?> inner = property("inner");

        assertThat(inner.get$ref()).isEqualTo("#/components/schemas/Inner");
        assertThat(inner.getAnyOf()).isNull();
    }
}
