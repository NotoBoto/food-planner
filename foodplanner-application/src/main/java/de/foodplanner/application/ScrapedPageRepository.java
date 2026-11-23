package de.foodplanner.application;

import java.net.URI;
import java.util.Optional;

public interface ScrapedPageRepository {

    Optional<ScrapedPage> findByUrl(URI url);

    void save(ScrapedPage page);
}
