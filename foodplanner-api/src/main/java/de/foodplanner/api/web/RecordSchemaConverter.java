package de.foodplanner.api.web;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Überträgt die Null-Annotationen der Records in die Spezifikation: Jedes Feld eines Records ist Pflicht,
 * und ein Feld mit JSpecify-{@code @Nullable} darf {@code null} sein. Ohne diesen Schritt wäre in der
 * Spezifikation jedes Feld optional, und die generierten Frontend-Typen würden {@code null} nicht kennen.
 */
class RecordSchemaConverter implements ModelConverter {

    private static final String REF_PREFIX = "#/components/schemas/";

    @Override
    public @Nullable Schema<?> resolve(
            AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> resolved = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        Class<?> raw = rawClass(type.getType());
        if (resolved == null || raw == null || !raw.isRecord() || !raw.getPackageName().startsWith("de.foodplanner")) {
            return resolved;
        }

        Schema<?> model = resolved;
        if (resolved.get$ref() != null && resolved.get$ref().startsWith(REF_PREFIX)) {
            model = context.getDefinedModels().get(resolved.get$ref().substring(REF_PREFIX.length()));
        }
        if (model == null || model.getProperties() == null) {
            return resolved;
        }

        for (RecordComponent component : raw.getRecordComponents()) {
            Schema<?> property = model.getProperties().get(component.getName());
            if (property == null) {
                continue;
            }
            if (model.getRequired() == null || !model.getRequired().contains(component.getName())) {
                model.addRequiredItem(component.getName());
            }
            if (component.getAnnotatedType().isAnnotationPresent(Nullable.class) && property.getTypes() != null) {
                Set<String> types = new LinkedHashSet<>(property.getTypes());
                types.add("null");
                property.setTypes(types);
            }
        }
        return resolved;
    }

    private static @Nullable Class<?> rawClass(@Nullable Object type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> c) {
            return c;
        }
        if (type == null) {
            return null;
        }
        try {
            // Jacksons JavaType, wie springdoc ihn übergibt
            return (Class<?>) type.getClass().getMethod("getRawClass").invoke(type);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
