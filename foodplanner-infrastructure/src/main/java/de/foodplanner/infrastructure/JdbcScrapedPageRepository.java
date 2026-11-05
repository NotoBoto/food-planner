package de.foodplanner.infrastructure;

import de.foodplanner.application.ScrapedPage;
import de.foodplanner.application.ScrapedPageRepository;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcScrapedPageRepository implements ScrapedPageRepository {

    private final JdbcTemplate jdbc;

    public JdbcScrapedPageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ScrapedPage> findByUrl(URI url) {
        return jdbc.query("select url, last_modified, fetched_at from scraped_page where url = ?",
                        (rs, rowNum) -> new ScrapedPage(URI.create(rs.getString("url")),
                                rs.getTimestamp("last_modified") == null ? null
                                        : rs.getTimestamp("last_modified").toInstant(),
                                rs.getTimestamp("fetched_at").toInstant()), url.toString())
                .stream().findFirst();
    }

    @Override
    public void save(ScrapedPage page) {
        jdbc.update("""
                insert into scraped_page (url, last_modified, fetched_at)
                values (?, ?, ?)
                on conflict (url) do update set last_modified = excluded.last_modified, fetched_at = excluded.fetched_at
                """, page.url().toString(), page.lastModified() == null ? null : Timestamp.from(page.lastModified()),
                Timestamp.from(page.fetchedAt()));
    }
}
