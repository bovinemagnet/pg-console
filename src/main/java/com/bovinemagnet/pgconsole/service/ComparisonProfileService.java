package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ComparisonProfile;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import com.bovinemagnet.pgconsole.repository.ComparisonProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing schema comparison profiles.
 * <p>
 * Handles CRUD operations for saved comparison configurations,
 * including import/export functionality.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComparisonProfileService {

    private static final Logger LOG = Logger.getLogger(ComparisonProfileService.class);

    @Inject
    ComparisonProfileRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Saves a new comparison profile.
     *
     * @param profile profile to save
     * @return saved profile with generated ID
     */
    public ComparisonProfile save(ComparisonProfile profile) {
        LOG.infof("Saving comparison profile: %s", profile.getName());
        return repository.save(profile);
    }

    /**
     * Updates an existing comparison profile.
     *
     * @param profile profile to update
     * @return updated profile
     */
    public ComparisonProfile update(ComparisonProfile profile) {
        LOG.infof("Updating comparison profile: %s", profile.getName());
        return repository.update(profile);
    }

    /**
     * Updates the last run information for a profile.
     *
     * @param profileId profile ID
     * @param summary comparison summary
     */
    public void updateLastRun(long profileId, SchemaComparisonResult.ComparisonSummary summary) {
        repository.updateLastRun(profileId, summary);
    }

    /**
     * Deletes a comparison profile.
     *
     * @param id profile ID
     */
    public void delete(long id) {
        LOG.infof("Deleting comparison profile: %d", id);
        repository.delete(id);
    }

    /**
     * Finds a profile by ID.
     *
     * @param id profile ID
     * @return profile or empty if not found
     */
    public Optional<ComparisonProfile> findById(long id) {
        return repository.findById(id);
    }

    /**
     * Gets all profiles.
     *
     * @return list of all profiles
     */
    public List<ComparisonProfile> findAll() {
        return repository.findAll();
    }

    /**
     * Finds profiles for a specific instance pair.
     *
     * @param sourceInstance source instance name
     * @param destInstance destination instance name
     * @return matching profiles
     */
    public List<ComparisonProfile> findByInstances(String sourceInstance, String destInstance) {
        return repository.findByInstances(sourceInstance, destInstance);
    }

    /**
     * Finds the default profile for an instance pair.
     *
     * @param sourceInstance source instance name
     * @param destInstance destination instance name
     * @return default profile or empty
     */
    public Optional<ComparisonProfile> findDefault(String sourceInstance, String destInstance) {
        return repository.findDefault(sourceInstance, destInstance);
    }

    /**
     * Exports a profile as JSON.
     *
     * @param profileId profile ID
     * @return JSON string or null if not found
     */
    public String exportAsJson(long profileId) {
        Optional<ComparisonProfile> profile = repository.findById(profileId);
        if (profile.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(profile.get());
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to export profile as JSON: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Imports a profile from JSON.
     *
     * @param json JSON string
     * @return imported profile or null if parse failed
     */
    public ComparisonProfile importFromJson(String json) {
        try {
            ComparisonProfile profile = objectMapper.readValue(json, ComparisonProfile.class);
            // Clear ID to create new record
            profile.setId(null);
            // Append "(imported)" to name to avoid conflicts
            profile.setName(profile.getName() + " (imported)");
            return repository.save(profile);
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to import profile from JSON: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a profile name already exists.
     *
     * @param name profile name
     * @return true if name exists
     */
    public boolean nameExists(String name) {
        return repository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }

    /**
     * Sets a profile as the default for its instance pair.
     *
     * @param profileId profile ID
     */
    public void setAsDefault(long profileId) {
        Optional<ComparisonProfile> profileOpt = repository.findById(profileId);
        if (profileOpt.isEmpty()) {
            return;
        }

        ComparisonProfile profile = profileOpt.get();

        // Clear existing default for this instance pair
        List<ComparisonProfile> existing = repository.findByInstances(
                profile.getSourceInstance(),
                profile.getDestinationInstance()
        );

        for (ComparisonProfile p : existing) {
            if (p.isDefault() && !p.getId().equals(profileId)) {
                p.setDefault(false);
                repository.update(p);
            }
        }

        // Set new default
        profile.setDefault(true);
        repository.update(profile);
    }

    /**
     * Creates a profile from a comparison result.
     *
     * @param result comparison result
     * @param name profile name
     * @param description profile description
     * @param username user creating the profile
     * @return created profile
     */
    public ComparisonProfile createFromResult(SchemaComparisonResult result,
                                               String name, String description,
                                               String username) {
        ComparisonProfile profile = ComparisonProfile.builder()
                .name(name)
                .description(description)
                .sourceInstance(result.getSourceInstance())
                .destinationInstance(result.getDestinationInstance())
                .sourceSchema(result.getSourceSchema())
                .destinationSchema(result.getDestinationSchema())
                .filter(result.getFilter())
                .createdBy(username)
                .build();

        profile.updateLastRun(result.getSummary());

        return repository.save(profile);
    }
}
