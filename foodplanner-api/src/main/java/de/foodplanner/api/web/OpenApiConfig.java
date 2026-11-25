package de.foodplanner.api.web;

import java.util.List;
import java.util.Locale;

import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Beschreibung der API für springdoc. Aus ihr entsteht {@code web/openapi.json}, aus der das Frontend
 * seine Typen generiert (§7).
 */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI foodPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("FoodPlanner API").version("v1"))
                // Relativ, damit die Spezifikation nicht von Host und Port des Aufrufs abhängt.
                .servers(List.of(new Server().url("/")));
    }

    @Bean
    RecordSchemaConverter recordSchemaConverter() {
        return new RecordSchemaConverter();
    }

    /**
     * Die Enums aus {@link WebConfig#SNAKE_CASE_ENUMS} werden als {@code price_asc} usw. angenommen
     * ({@link SnakeCaseEnumConverter}). Ohne Anpassung stünden in der Spezifikation die Java-Namen wie
     * {@code PRICE_ASC}.
     */
    @Bean
    ParameterCustomizer snakeCaseEnumParameters() {
        return (parameter, methodParameter) -> {
            Class<?> type = methodParameter.getParameterType();
            if (parameter == null || !WebConfig.SNAKE_CASE_ENUMS.contains(type)) {
                return parameter;
            }
            StringSchema schema = new StringSchema();
            for (Object constant : type.getEnumConstants()) {
                schema.addEnumItem(((Enum<?>) constant).name().toLowerCase(Locale.ROOT));
            }
            if (parameter.getSchema() != null && parameter.getSchema().getDefault() != null) {
                schema.setDefault(parameter.getSchema().getDefault().toString().toLowerCase(Locale.ROOT));
            }
            parameter.setSchema(schema);
            return parameter;
        };
    }
}
