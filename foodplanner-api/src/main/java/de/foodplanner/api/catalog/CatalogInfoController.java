package de.foodplanner.api.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.foodplanner.application.catalog.info.GetCatalogInfoHandler;
import de.foodplanner.application.catalog.info.GetCatalogInfoQuery;
import de.foodplanner.application.port.CatalogInfo;

/** {@code GET /api/v1/catalog/info → 200 { productCount, lastScrapedAt }} (§5.1). */
@RestController
@RequestMapping("/api/v1/catalog")
class CatalogInfoController {

    private final GetCatalogInfoHandler handler;

    CatalogInfoController(GetCatalogInfoHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/info")
    CatalogInfo info() {
        return handler.handle(new GetCatalogInfoQuery());
    }
}
