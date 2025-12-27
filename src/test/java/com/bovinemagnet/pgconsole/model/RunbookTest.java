package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Runbook and related model classes.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class RunbookTest {

    @Test
    void testCategoryEnumProperties() {
        Runbook.Category incident = Runbook.Category.INCIDENT;
        assertEquals("Incident Response", incident.getDisplayName());
        assertEquals("bi-exclamation-diamond", incident.getIcon());
        assertNotNull(incident.getDescription());

        Runbook.Category maintenance = Runbook.Category.MAINTENANCE;
        assertEquals("Maintenance", maintenance.getDisplayName());
        assertEquals("bi-wrench", maintenance.getIcon());

        Runbook.Category troubleshooting = Runbook.Category.TROUBLESHOOTING;
        assertEquals("Troubleshooting", troubleshooting.getDisplayName());

        Runbook.Category recovery = Runbook.Category.RECOVERY;
        assertEquals("Recovery", recovery.getDisplayName());
    }

    @Test
    void testTriggerTypeEnumProperties() {
        Runbook.TriggerType manual = Runbook.TriggerType.MANUAL;
        assertEquals("Manual", manual.getDisplayName());
        assertNotNull(manual.getDescription());

        Runbook.TriggerType alert = Runbook.TriggerType.ALERT;
        assertEquals("Alert", alert.getDisplayName());

        Runbook.TriggerType anomaly = Runbook.TriggerType.ANOMALY;
        assertEquals("Anomaly", anomaly.getDisplayName());

        Runbook.TriggerType scheduled = Runbook.TriggerType.SCHEDULED;
        assertEquals("Scheduled", scheduled.getDisplayName());
    }

    @Test
    void testStepActionTypeEnumProperties() {
        Runbook.Step.ActionType navigate = Runbook.Step.ActionType.NAVIGATE;
        assertEquals("Navigate", navigate.getDisplayName());
        assertEquals("bi-box-arrow-up-right", navigate.getIcon());
        assertNotNull(navigate.getDescription());

        Runbook.Step.ActionType query = Runbook.Step.ActionType.QUERY;
        assertEquals("Query", query.getDisplayName());

        Runbook.Step.ActionType sqlTemplate = Runbook.Step.ActionType.SQL_TEMPLATE;
        assertEquals("SQL Template", sqlTemplate.getDisplayName());

        Runbook.Step.ActionType manualAction = Runbook.Step.ActionType.MANUAL;
        assertEquals("Manual", manualAction.getDisplayName());

        Runbook.Step.ActionType documentation = Runbook.Step.ActionType.DOCUMENTATION;
        assertEquals("Documentation", documentation.getDisplayName());
    }

    @Test
    void testStepConstructor() {
        Runbook.Step step = new Runbook.Step(1, "Check Connections",
                "Verify current connection count", Runbook.Step.ActionType.NAVIGATE, "/activity");

        assertEquals(1, step.getOrder());
        assertEquals("Check Connections", step.getTitle());
        assertEquals("Verify current connection count", step.getDescription());
        assertEquals(Runbook.Step.ActionType.NAVIGATE, step.getActionType());
        assertEquals("/activity", step.getAction());
        assertFalse(step.isAutoExecute());
        assertFalse(step.isRequiresConfirmation());
    }

    @Test
    void testStepSettersAndGetters() {
        Runbook.Step step = new Runbook.Step();

        step.setOrder(2);
        step.setTitle("Run Query");
        step.setDescription("Execute diagnostic query");
        step.setActionType(Runbook.Step.ActionType.QUERY);
        step.setAction("SELECT count(*) FROM pg_stat_activity");
        step.setAutoExecute(true);
        step.setRequiresConfirmation(true);

        assertEquals(2, step.getOrder());
        assertEquals("Run Query", step.getTitle());
        assertEquals("Execute diagnostic query", step.getDescription());
        assertEquals(Runbook.Step.ActionType.QUERY, step.getActionType());
        assertEquals("SELECT count(*) FROM pg_stat_activity", step.getAction());
        assertTrue(step.isAutoExecute());
        assertTrue(step.isRequiresConfirmation());
    }

    @Test
    void testGetStepCountWithSteps() {
        Runbook runbook = new Runbook();

        List<Runbook.Step> steps = new ArrayList<>();
        steps.add(new Runbook.Step(1, "Step 1", "Desc 1", Runbook.Step.ActionType.MANUAL, "action1"));
        steps.add(new Runbook.Step(2, "Step 2", "Desc 2", Runbook.Step.ActionType.MANUAL, "action2"));
        steps.add(new Runbook.Step(3, "Step 3", "Desc 3", Runbook.Step.ActionType.MANUAL, "action3"));
        runbook.setSteps(steps);

        assertEquals(3, runbook.getStepCount());
    }

    @Test
    void testGetStepCountNullSteps() {
        Runbook runbook = new Runbook();
        runbook.setSteps(null);

        assertEquals(0, runbook.getStepCount());
    }

    @Test
    void testGetStepCountEmptySteps() {
        Runbook runbook = new Runbook();
        runbook.setSteps(new ArrayList<>());

        assertEquals(0, runbook.getStepCount());
    }

    @Test
    void testGetAutoStepCount() {
        Runbook runbook = new Runbook();

        List<Runbook.Step> steps = new ArrayList<>();
        Runbook.Step autoStep1 = new Runbook.Step(1, "Auto Step", "Auto", Runbook.Step.ActionType.QUERY, "query");
        autoStep1.setAutoExecute(true);
        steps.add(autoStep1);

        Runbook.Step manualStep = new Runbook.Step(2, "Manual Step", "Manual", Runbook.Step.ActionType.MANUAL, "action");
        manualStep.setAutoExecute(false);
        steps.add(manualStep);

        Runbook.Step autoStep2 = new Runbook.Step(3, "Auto Step 2", "Auto 2", Runbook.Step.ActionType.NAVIGATE, "/page");
        autoStep2.setAutoExecute(true);
        steps.add(autoStep2);

        runbook.setSteps(steps);

        assertEquals(2, runbook.getAutoStepCount());
    }

    @Test
    void testGetAutoStepCountNullSteps() {
        Runbook runbook = new Runbook();
        runbook.setSteps(null);

        assertEquals(0, runbook.getAutoStepCount());
    }

    @Test
    void testGetFormattedDurationMinutesOnly() {
        Runbook runbook = new Runbook();
        runbook.setEstimatedDurationMinutes(30);

        assertEquals("30 minutes", runbook.getFormattedDuration());
    }

    @Test
    void testGetFormattedDurationHoursAndMinutes() {
        Runbook runbook = new Runbook();
        runbook.setEstimatedDurationMinutes(90);

        assertEquals("1h 30m", runbook.getFormattedDuration());
    }

    @Test
    void testGetFormattedDurationExactHours() {
        Runbook runbook = new Runbook();
        runbook.setEstimatedDurationMinutes(120);

        assertEquals("2h 0m", runbook.getFormattedDuration());
    }

    @Test
    void testGetFormattedDurationNull() {
        Runbook runbook = new Runbook();
        runbook.setEstimatedDurationMinutes(null);

        assertEquals("Unknown", runbook.getFormattedDuration());
    }

    @Test
    void testDefaultConstructorValues() {
        Runbook runbook = new Runbook();

        assertEquals(1, runbook.getVersion());
        assertTrue(runbook.isEnabled());
        assertNotNull(runbook.getCreatedAt());
        assertNotNull(runbook.getUpdatedAt());
    }

    @Test
    void testSettersAndGetters() {
        Runbook runbook = new Runbook();

        runbook.setId(123L);
        runbook.setName("high-connections");
        runbook.setTitle("High Connections Troubleshooting");
        runbook.setDescription("Diagnose and resolve high connection count");
        runbook.setCategory(Runbook.Category.TROUBLESHOOTING);
        runbook.setTriggerType(Runbook.TriggerType.ALERT);
        runbook.setTriggerConditions("{\"metric\": \"connections\", \"threshold\": 100}");
        runbook.setVersion(2);
        runbook.setEnabled(false);
        runbook.setCreatedBy("admin");
        runbook.setEstimatedDurationMinutes(15);

        Instant now = Instant.now();
        runbook.setCreatedAt(now);
        runbook.setUpdatedAt(now);

        assertEquals(123L, runbook.getId());
        assertEquals("high-connections", runbook.getName());
        assertEquals("High Connections Troubleshooting", runbook.getTitle());
        assertEquals("Diagnose and resolve high connection count", runbook.getDescription());
        assertEquals(Runbook.Category.TROUBLESHOOTING, runbook.getCategory());
        assertEquals(Runbook.TriggerType.ALERT, runbook.getTriggerType());
        assertNotNull(runbook.getTriggerConditions());
        assertEquals(2, runbook.getVersion());
        assertFalse(runbook.isEnabled());
        assertEquals("admin", runbook.getCreatedBy());
        assertEquals(15, runbook.getEstimatedDurationMinutes());
        assertEquals(now, runbook.getCreatedAt());
        assertEquals(now, runbook.getUpdatedAt());
    }

    @Test
    void testStepsListOperations() {
        Runbook runbook = new Runbook();

        List<Runbook.Step> steps = new ArrayList<>();
        steps.add(new Runbook.Step(1, "First Step", "Do first thing", Runbook.Step.ActionType.NAVIGATE, "/page1"));
        steps.add(new Runbook.Step(2, "Second Step", "Do second thing", Runbook.Step.ActionType.MANUAL, "action"));

        runbook.setSteps(steps);

        assertNotNull(runbook.getSteps());
        assertEquals(2, runbook.getSteps().size());
        assertEquals("First Step", runbook.getSteps().get(0).getTitle());
        assertEquals("Second Step", runbook.getSteps().get(1).getTitle());
    }

    @Test
    void testAllCategoryEnums() {
        // Verify all categories have required properties
        for (Runbook.Category category : Runbook.Category.values()) {
            assertNotNull(category.getDisplayName());
            assertFalse(category.getDisplayName().isEmpty());
            assertNotNull(category.getIcon());
            assertFalse(category.getIcon().isEmpty());
            assertNotNull(category.getDescription());
        }
    }

    @Test
    void testAllTriggerTypeEnums() {
        // Verify all trigger types have required properties
        for (Runbook.TriggerType triggerType : Runbook.TriggerType.values()) {
            assertNotNull(triggerType.getDisplayName());
            assertFalse(triggerType.getDisplayName().isEmpty());
            assertNotNull(triggerType.getDescription());
        }
    }

    @Test
    void testAllActionTypeEnums() {
        // Verify all action types have required properties
        for (Runbook.Step.ActionType actionType : Runbook.Step.ActionType.values()) {
            assertNotNull(actionType.getDisplayName());
            assertFalse(actionType.getDisplayName().isEmpty());
            assertNotNull(actionType.getIcon());
            assertFalse(actionType.getIcon().isEmpty());
            assertNotNull(actionType.getDescription());
        }
    }
}
