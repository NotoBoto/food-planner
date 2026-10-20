package de.foodplanner.architecture.fixtures.outward.domain;

import de.foodplanner.architecture.fixtures.outward.api.OutwardEndpoint;
import de.foodplanner.architecture.fixtures.outward.application.OutwardHandler;
import de.foodplanner.architecture.fixtures.outward.infrastructure.OutwardAdapter;
import de.foodplanner.architecture.fixtures.outward.ingestion.OutwardCommand;

public class OutwardDomain {
    OutwardHandler toApplication;
    OutwardAdapter toInfrastructure;
    OutwardEndpoint toApi;
    OutwardCommand toIngestion;
}
