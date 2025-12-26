package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.QueryBookmark;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for managing query bookmarks.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class BookmarkService {

    private static final Logger LOG = Logger.getLogger(BookmarkService.class);

    @Inject
    DataSource dataSource;

    /**
     * Creates a new bookmark.
     */
    public QueryBookmark create(QueryBookmark bookmark) {
        String sql = """
            INSERT INTO pgconsole.query_bookmark
            (created_at, updated_at, instance_id, query_id, query_text, title, notes, tags, created_by, priority, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = Timestamp.from(Instant.now());
            stmt.setTimestamp(1, now);
            stmt.setTimestamp(2, now);
            stmt.setString(3, bookmark.getInstanceId());
            stmt.setString(4, bookmark.getQueryId());
            stmt.setString(5, bookmark.getQueryText());
            stmt.setString(6, bookmark.getTitle());
            stmt.setString(7, bookmark.getNotes());

            if (bookmark.getTags() != null && !bookmark.getTags().isEmpty()) {
                Array tagsArray = conn.createArrayOf("TEXT", bookmark.getTags().toArray());
                stmt.setArray(8, tagsArray);
            } else {
                stmt.setArray(8, null);
            }

            stmt.setString(9, bookmark.getCreatedBy());
            stmt.setString(10, bookmark.getPriority().name().toLowerCase());
            stmt.setString(11, bookmark.getStatus().name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bookmark.setId(rs.getLong("id"));
                    bookmark.setCreatedAt(now.toInstant());
                    bookmark.setUpdatedAt(now.toInstant());
                }
            }

            LOG.infof("Created bookmark %d for query %s", bookmark.getId(), bookmark.getQueryId());

        } catch (SQLException e) {
            LOG.errorf("Failed to create bookmark: %s", e.getMessage());
        }

        return bookmark;
    }

    /**
     * Updates an existing bookmark.
     */
    public QueryBookmark update(QueryBookmark bookmark) {
        String sql = """
            UPDATE pgconsole.query_bookmark
            SET updated_at = ?, title = ?, notes = ?, tags = ?, priority = ?, status = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = Timestamp.from(Instant.now());
            stmt.setTimestamp(1, now);
            stmt.setString(2, bookmark.getTitle());
            stmt.setString(3, bookmark.getNotes());

            if (bookmark.getTags() != null && !bookmark.getTags().isEmpty()) {
                Array tagsArray = conn.createArrayOf("TEXT", bookmark.getTags().toArray());
                stmt.setArray(4, tagsArray);
            } else {
                stmt.setArray(4, null);
            }

            stmt.setString(5, bookmark.getPriority().name().toLowerCase());
            stmt.setString(6, bookmark.getStatus().name().toLowerCase());
            stmt.setLong(7, bookmark.getId());

            stmt.executeUpdate();
            bookmark.setUpdatedAt(now.toInstant());

            LOG.infof("Updated bookmark %d", bookmark.getId());

        } catch (SQLException e) {
            LOG.errorf("Failed to update bookmark: %s", e.getMessage());
        }

        return bookmark;
    }

    /**
     * Deletes a bookmark.
     */
    public boolean delete(long id) {
        String sql = "DELETE FROM pgconsole.query_bookmark WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                LOG.infof("Deleted bookmark %d", id);
                return true;
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to delete bookmark: %s", e.getMessage());
        }

        return false;
    }

    /**
     * Gets a bookmark by ID.
     */
    public QueryBookmark getById(long id) {
        String sql = """
            SELECT id, created_at, updated_at, instance_id, query_id, query_text,
                   title, notes, tags, created_by, priority, status
            FROM pgconsole.query_bookmark
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBookmark(rs);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get bookmark %d: %s", id, e.getMessage());
        }

        return null;
    }

    /**
     * Gets a bookmark by query ID.
     */
    public QueryBookmark getByQueryId(String instanceId, String queryId) {
        String sql = """
            SELECT id, created_at, updated_at, instance_id, query_id, query_text,
                   title, notes, tags, created_by, priority, status
            FROM pgconsole.query_bookmark
            WHERE instance_id = ? AND query_id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setString(2, queryId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBookmark(rs);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get bookmark for query %s: %s", queryId, e.getMessage());
        }

        return null;
    }

    /**
     * Checks if a query is bookmarked.
     */
    public boolean isBookmarked(String instanceId, String queryId) {
        return getByQueryId(instanceId, queryId) != null;
    }

    /**
     * Gets all bookmarks for an instance.
     */
    public List<QueryBookmark> getBookmarksForInstance(String instanceId) {
        return getBookmarks(instanceId, null, null);
    }

    /**
     * Gets bookmarks with filters.
     */
    public List<QueryBookmark> getBookmarks(String instanceId, String status, String tag) {
        List<QueryBookmark> bookmarks = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT id, created_at, updated_at, instance_id, query_id, query_text,
                   title, notes, tags, created_by, priority, status
            FROM pgconsole.query_bookmark
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (instanceId != null && !instanceId.isEmpty()) {
            sql.append(" AND instance_id = ?");
            params.add(instanceId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status.toLowerCase());
        }
        if (tag != null && !tag.isEmpty()) {
            sql.append(" AND ? = ANY(tags)");
            params.add(tag);
        }

        sql.append(" ORDER BY updated_at DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(mapBookmark(rs));
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get bookmarks: %s", e.getMessage());
        }

        return bookmarks;
    }

    /**
     * Gets all unique tags across all bookmarks.
     */
    public List<String> getAllTags(String instanceId) {
        List<String> tags = new ArrayList<>();

        String sql = """
            SELECT DISTINCT UNNEST(tags) as tag
            FROM pgconsole.query_bookmark
            WHERE instance_id = ?
            ORDER BY tag
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("tag"));
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get tags: %s", e.getMessage());
        }

        return tags;
    }

    /**
     * Gets bookmark summary statistics.
     */
    public BookmarkSummary getSummary(String instanceId) {
        BookmarkSummary summary = new BookmarkSummary();

        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE status = 'active') as active,
                COUNT(*) FILTER (WHERE status = 'investigating') as investigating,
                COUNT(*) FILTER (WHERE status = 'resolved') as resolved,
                COUNT(*) FILTER (WHERE priority = 'critical') as critical,
                COUNT(*) FILTER (WHERE priority = 'high') as high
            FROM pgconsole.query_bookmark
            WHERE instance_id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalBookmarks(rs.getInt("total"));
                    summary.setActiveBookmarks(rs.getInt("active"));
                    summary.setInvestigatingBookmarks(rs.getInt("investigating"));
                    summary.setResolvedBookmarks(rs.getInt("resolved"));
                    summary.setCriticalBookmarks(rs.getInt("critical"));
                    summary.setHighPriorityBookmarks(rs.getInt("high"));
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get bookmark summary: %s", e.getMessage());
        }

        return summary;
    }

    private QueryBookmark mapBookmark(ResultSet rs) throws SQLException {
        QueryBookmark bookmark = new QueryBookmark();
        bookmark.setId(rs.getLong("id"));
        bookmark.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        bookmark.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        bookmark.setInstanceId(rs.getString("instance_id"));
        bookmark.setQueryId(rs.getString("query_id"));
        bookmark.setQueryText(rs.getString("query_text"));
        bookmark.setTitle(rs.getString("title"));
        bookmark.setNotes(rs.getString("notes"));

        Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            String[] tags = (String[]) tagsArray.getArray();
            bookmark.setTags(Arrays.asList(tags));
        }

        bookmark.setCreatedBy(rs.getString("created_by"));
        bookmark.setPriorityFromString(rs.getString("priority"));
        bookmark.setStatusFromString(rs.getString("status"));

        return bookmark;
    }

    /**
     * Summary statistics for bookmarks.
     */
    public static class BookmarkSummary {
        private int totalBookmarks;
        private int activeBookmarks;
        private int investigatingBookmarks;
        private int resolvedBookmarks;
        private int criticalBookmarks;
        private int highPriorityBookmarks;

        public int getTotalBookmarks() { return totalBookmarks; }
        public void setTotalBookmarks(int value) { this.totalBookmarks = value; }

        public int getActiveBookmarks() { return activeBookmarks; }
        public void setActiveBookmarks(int value) { this.activeBookmarks = value; }

        public int getInvestigatingBookmarks() { return investigatingBookmarks; }
        public void setInvestigatingBookmarks(int value) { this.investigatingBookmarks = value; }

        public int getResolvedBookmarks() { return resolvedBookmarks; }
        public void setResolvedBookmarks(int value) { this.resolvedBookmarks = value; }

        public int getCriticalBookmarks() { return criticalBookmarks; }
        public void setCriticalBookmarks(int value) { this.criticalBookmarks = value; }

        public int getHighPriorityBookmarks() { return highPriorityBookmarks; }
        public void setHighPriorityBookmarks(int value) { this.highPriorityBookmarks = value; }
    }
}
