package de.foodplanner.api.shoppinglist;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListHandler;

/**
 * {@code POST /api/v1/shopping-list/summary → 200} (§5.2). Die Liste liegt im Browser; der Endpunkt ist eine reine
 * Funktion des Request-Bodys und hält keinen Zustand (§4.6). Der Controller bindet nur; die Grenzen prüft der
 * {@code @Validated}-Handler (§3.2), gerechnet wird in {@code domain}.
 */
@RestController
@RequestMapping("/api/v1/shopping-list")
class ShoppingListController {

    private final SummariseShoppingListHandler handler;

    ShoppingListController(SummariseShoppingListHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/summary")
    ShoppingListSummaryResponse summary(@RequestBody ShoppingListSummaryRequest request) {
        return ShoppingListSummaryResponse.from(handler.handle(request.toQuery()));
    }
}
