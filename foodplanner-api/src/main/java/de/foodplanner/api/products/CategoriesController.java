package de.foodplanner.api.products;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.foodplanner.application.products.categories.GetCategoriesHandler;
import de.foodplanner.application.products.categories.GetCategoriesQuery;

/** {@code GET /api/v1/categories → 200 [ "Brot", "Obst", … ]}: Kategorien der sichtbaren Produkte (§5.1). */
@RestController
@RequestMapping("/api/v1/categories")
class CategoriesController {

    private final GetCategoriesHandler handler;

    CategoriesController(GetCategoriesHandler handler) {
        this.handler = handler;
    }

    @GetMapping
    List<String> categories() {
        return handler.handle(new GetCategoriesQuery());
    }
}
