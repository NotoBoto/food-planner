package de.foodplanner.architecture.fixtures.outward.infrastructure;

import de.foodplanner.architecture.fixtures.outward.api.OutwardEndpoint;
import de.foodplanner.architecture.fixtures.outward.ingestion.OutwardCommand;

public class OutwardAdapter {
    OutwardEndpoint toApi;
    OutwardCommand toIngestion;
}
