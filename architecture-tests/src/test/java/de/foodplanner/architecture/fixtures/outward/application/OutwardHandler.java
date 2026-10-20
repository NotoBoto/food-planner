package de.foodplanner.architecture.fixtures.outward.application;

import de.foodplanner.architecture.fixtures.outward.api.OutwardEndpoint;
import de.foodplanner.architecture.fixtures.outward.infrastructure.OutwardAdapter;
import de.foodplanner.architecture.fixtures.outward.ingestion.OutwardCommand;

public class OutwardHandler {
    OutwardAdapter toInfrastructure;
    OutwardEndpoint toApi;
    OutwardCommand toIngestion;
}
