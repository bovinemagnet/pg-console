package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.QueryFingerprint;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for normalising SQL queries and grouping them by fingerprint.
 * Queries with the same structure but different literal values will have the same fingerprint.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class QueryFingerprintService {

	// Pattern to match string literals (single quotes, handling escaped quotes)
	private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");

	// Pattern to match numeric literals (integers and decimals)
	private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("(?<![a-zA-Z_])\\b\\d+\\.?\\d*\\b(?![a-zA-Z_])");

	// Pattern to match IN clause lists like IN (1, 2, 3) or IN ('a', 'b', 'c')
	private static final Pattern IN_LIST_PATTERN = Pattern.compile("\\bIN\\s*\\(\\s*\\?(?:\\s*,\\s*\\?)*\\s*\\)", Pattern.CASE_INSENSITIVE);

	// Pattern to collapse multiple whitespace into single space
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	// Pattern to match $1, $2 style positional parameters (PostgreSQL)
	private static final Pattern POSITIONAL_PARAM_PATTERN = Pattern.compile("\\$\\d+");

	/**
	 * Normalises a SQL query by replacing literal values with placeholders.
	 * This allows queries with the same structure but different values to be grouped together.
	 *
	 * @param query the original SQL query
	 * @return the normalised query with literals replaced by '?'
	 */
	public String normaliseQuery(String query) {
		if (query == null || query.isEmpty()) {
			return "";
		}

		String normalised = query;

		// Replace string literals with ?
		normalised = STRING_LITERAL_PATTERN.matcher(normalised).replaceAll("?");

		// Replace PostgreSQL positional parameters ($1, $2, etc.) with ?
		normalised = POSITIONAL_PARAM_PATTERN.matcher(normalised).replaceAll("?");

		// Replace numeric literals with ?
		normalised = NUMERIC_LITERAL_PATTERN.matcher(normalised).replaceAll("?");

		// Collapse IN (?, ?, ?) to IN (...)
		normalised = IN_LIST_PATTERN.matcher(normalised).replaceAll("IN (...)");

		// Normalise whitespace
		normalised = WHITESPACE_PATTERN.matcher(normalised).replaceAll(" ").trim();

		// Convert to lowercase for consistent fingerprinting
		normalised = normalised.toLowerCase();

		return normalised;
	}

	/**
	 * Computes a fingerprint (hash) for a normalised query.
	 * Queries with identical normalised forms will have identical fingerprints.
	 *
	 * @param normalisedQuery the normalised SQL query
	 * @return a hex string fingerprint (first 16 chars of SHA-256)
	 */
	public String computeFingerprint(String normalisedQuery) {
		if (normalisedQuery == null || normalisedQuery.isEmpty()) {
			return "";
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(normalisedQuery.getBytes(StandardCharsets.UTF_8));

			// Convert first 8 bytes to hex (16 characters)
			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < 8 && i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 should always be available
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	/**
	 * Groups a list of slow queries by their fingerprint.
	 * Similar queries (same structure, different values) are grouped together.
	 *
	 * @param queries the list of slow queries to group
	 * @return a list of QueryFingerprint objects, each containing grouped queries
	 */
	public List<QueryFingerprint> groupQueries(List<SlowQuery> queries) {
		Map<String, QueryFingerprint> fingerprintMap = new LinkedHashMap<>();

		for (SlowQuery query : queries) {
			String normalised = normaliseQuery(query.getQuery());
			String fingerprint = computeFingerprint(normalised);

			QueryFingerprint group = fingerprintMap.computeIfAbsent(fingerprint, fp -> {
				QueryFingerprint qf = new QueryFingerprint(fp, normalised);
				return qf;
			});

			group.addInstance(query);
		}

		// Recalculate stats for each group
		List<QueryFingerprint> result = new ArrayList<>(fingerprintMap.values());
		for (QueryFingerprint qf : result) {
			qf.recalculateStats();
		}

		return result;
	}

	/**
	 * Groups queries and sorts by total time descending (most expensive groups first).
	 *
	 * @param queries the list of slow queries to group
	 * @return a sorted list of QueryFingerprint objects
	 */
	public List<QueryFingerprint> groupQueriesSortedByTotalTime(List<SlowQuery> queries) {
		List<QueryFingerprint> grouped = groupQueries(queries);
		grouped.sort((a, b) -> Double.compare(b.getTotalTime(), a.getTotalTime()));
		return grouped;
	}

	/**
	 * Groups queries and sorts by total calls descending (most frequently called groups first).
	 *
	 * @param queries the list of slow queries to group
	 * @return a sorted list of QueryFingerprint objects
	 */
	public List<QueryFingerprint> groupQueriesSortedByCalls(List<SlowQuery> queries) {
		List<QueryFingerprint> grouped = groupQueries(queries);
		grouped.sort((a, b) -> Long.compare(b.getTotalCalls(), a.getTotalCalls()));
		return grouped;
	}

	/**
	 * Groups queries and sorts by average mean time descending (slowest groups first).
	 *
	 * @param queries the list of slow queries to group
	 * @return a sorted list of QueryFingerprint objects
	 */
	public List<QueryFingerprint> groupQueriesSortedByAvgTime(List<SlowQuery> queries) {
		List<QueryFingerprint> grouped = groupQueries(queries);
		grouped.sort((a, b) -> Double.compare(b.getAvgMeanTime(), a.getAvgMeanTime()));
		return grouped;
	}
}
