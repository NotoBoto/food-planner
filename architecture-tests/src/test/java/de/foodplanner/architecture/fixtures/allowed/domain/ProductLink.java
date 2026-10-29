package de.foodplanner.architecture.fixtures.allowed.domain;

import java.net.URI;
import java.net.URL;

/** {@link URL} als Wert ist erlaubt, nur das Öffnen einer Verbindung nicht. */
public record ProductLink(URI uri, URL url) {
}
