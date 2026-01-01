package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScheduledMaintenance model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class ScheduledMaintenanceTest {

    @Test
    void testTaskTypeEnumProperties() {
        ScheduledMaintenance.TaskType vacuum = ScheduledMaintenance.TaskType.VACUUM;
        assertEquals("VACUUM", vacuum.getDisplayName());
        assertEquals("bi-wind", vacuum.getIcon());
        assertNotNull(vacuum.getDescription());

        ScheduledMaintenance.TaskType vacuumFull = ScheduledMaintenance.TaskType.VACUUM_FULL;
        assertEquals("VACUUM FULL", vacuumFull.getDisplayName());

        ScheduledMaintenance.TaskType analyse = ScheduledMaintenance.TaskType.ANALYSE;
        assertEquals("ANALYSE", analyse.getDisplayName());

        ScheduledMaintenance.TaskType reindex = ScheduledMaintenance.TaskType.REINDEX;
        assertEquals("REINDEX", reindex.getDisplayName());

        ScheduledMaintenance.TaskType cluster = ScheduledMaintenance.TaskType.CLUSTER;
        assertEquals("CLUSTER", cluster.getDisplayName());
    }

    @Test
    void testScheduleTypeEnumProperties() {
        ScheduledMaintenance.ScheduleType intelligent = ScheduledMaintenance.ScheduleType.INTELLIGENT;
        assertEquals("Intelligent", intelligent.getDisplayName());
        assertNotNull(intelligent.getDescription());

        ScheduledMaintenance.ScheduleType cron = ScheduledMaintenance.ScheduleType.CRON;
        assertEquals("Cron", cron.getDisplayName());

        ScheduledMaintenance.ScheduleType oneTime = ScheduledMaintenance.ScheduleType.ONE_TIME;
        assertEquals("One-Time", oneTime.getDisplayName());
    }

    @Test
    void testIsAllTablesTrue() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetObject("*");

        assertTrue(task.isAllTables());
    }

    @Test
    void testIsAllTablesFalse() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetObject("users");

        assertFalse(task.isAllTables());
    }

    @Test
    void testIsAllTablesNull() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetObject(null);

        assertFalse(task.isAllTables());
    }

    @Test
    void testGetTargetDisplayAllTables() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetObject("*");
        task.setTargetSchema("public");

        String display = task.getTargetDisplay();
        assertTrue(display.contains("All tables"));
        assertTrue(display.contains("public"));
    }

    @Test
    void testGetTargetDisplaySpecificTable() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetObject("orders");
        task.setTargetSchema("sales");

        assertEquals("sales.orders", task.getTargetDisplay());
    }

    @Test
    void testGetSqlCommandVacuum() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.VACUUM);
        task.setTargetSchema("public");
        task.setTargetObject("users");

        assertEquals("VACUUM public.users", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandVacuumAllTables() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.VACUUM);
        task.setTargetObject("*");

        assertEquals("VACUUM", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandVacuumFull() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.VACUUM_FULL);
        task.setTargetSchema("public");
        task.setTargetObject("orders");

        assertEquals("VACUUM FULL public.orders", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandAnalyse() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.ANALYSE);
        task.setTargetSchema("schema1");
        task.setTargetObject("table1");

        assertEquals("ANALYSE schema1.table1", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandReindexTable() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.REINDEX);
        task.setTargetSchema("public");
        task.setTargetObject("accounts");

        assertEquals("REINDEX TABLE public.accounts", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandReindexDatabase() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.REINDEX);
        task.setTargetObject("*");

        assertEquals("REINDEX DATABASE", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandCluster() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.CLUSTER);
        task.setTargetSchema("public");
        task.setTargetObject("events");

        assertEquals("CLUSTER public.events", task.getSqlCommand());
    }

    @Test
    void testGetSqlCommandClusterAll() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTaskType(ScheduledMaintenance.TaskType.CLUSTER);
        task.setTargetObject("*");

        assertEquals("CLUSTER", task.getSqlCommand());
    }

    @Test
    void testGetScheduleDescriptionIntelligent() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setScheduleType(ScheduledMaintenance.ScheduleType.INTELLIGENT);
        task.setActivityThreshold(15.0);

        String desc = task.getScheduleDescription();
        assertTrue(desc.contains("activity"));
        assertTrue(desc.contains("15"));
    }

    @Test
    void testGetScheduleDescriptionIntelligentWithWindow() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setScheduleType(ScheduledMaintenance.ScheduleType.INTELLIGENT);
        task.setActivityThreshold(20.0);
        task.setPreferredWindowStart(LocalTime.of(2, 0));
        task.setPreferredWindowEnd(LocalTime.of(5, 0));

        String desc = task.getScheduleDescription();
        assertTrue(desc.contains("20"));
        assertTrue(desc.contains("02:00"));
        assertTrue(desc.contains("05:00"));
    }

    @Test
    void testGetScheduleDescriptionCron() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setScheduleType(ScheduledMaintenance.ScheduleType.CRON);
        task.setCronExpression("0 3 * * 0");

        String desc = task.getScheduleDescription();
        assertTrue(desc.contains("Cron"));
        assertTrue(desc.contains("0 3 * * 0"));
    }

    @Test
    void testGetScheduleDescriptionOneTime() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setScheduleType(ScheduledMaintenance.ScheduleType.ONE_TIME);
        Instant scheduled = Instant.now();
        task.setScheduledTime(scheduled);

        String desc = task.getScheduleDescription();
        assertTrue(desc.contains("At"));
    }

    @Test
    void testGetScheduleDescriptionOneTimeNotScheduled() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setScheduleType(ScheduledMaintenance.ScheduleType.ONE_TIME);
        task.setScheduledTime(null);

        assertEquals("Not scheduled", task.getScheduleDescription());
    }

    @Test
    void testDefaultConstructorValues() {
        ScheduledMaintenance task = new ScheduledMaintenance();

        assertEquals(ScheduledMaintenance.ScheduleType.INTELLIGENT, task.getScheduleType());
        assertEquals("public", task.getTargetSchema());
        assertTrue(task.isEnabled());
        assertEquals(5, task.getPriority());
        assertEquals(60, task.getMaxDurationMinutes());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    void testSettersAndGetters() {
        ScheduledMaintenance task = new ScheduledMaintenance();

        task.setId(123L);
        task.setInstanceId("prod-db");
        task.setName("daily-vacuum");
        task.setDescription("Daily vacuum of critical tables");
        task.setTaskType(ScheduledMaintenance.TaskType.VACUUM);
        task.setTargetSchema("public");
        task.setTargetObject("transactions");
        task.setScheduleType(ScheduledMaintenance.ScheduleType.CRON);
        task.setCronExpression("0 3 * * *");
        task.setActivityThreshold(25.0);
        task.setPreferredWindowStart(LocalTime.of(1, 0));
        task.setPreferredWindowEnd(LocalTime.of(4, 0));
        task.setPreferredDays(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        task.setMinIntervalHours(24);
        task.setMaxTableSizeGb(10.0);
        task.setMaxDurationMinutes(120);
        task.setPriority(10);
        task.setEnabled(false);
        task.setCreatedBy("dba");
        task.setLastRunDurationMs(5000L);
        task.setLastRunStatus("SUCCESS");

        Instant now = Instant.now();
        task.setScheduledTime(now);
        task.setLastRunAt(now);
        task.setNextRunAt(now.plusSeconds(3600));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        assertEquals(123L, task.getId());
        assertEquals("prod-db", task.getInstanceId());
        assertEquals("daily-vacuum", task.getName());
        assertEquals("Daily vacuum of critical tables", task.getDescription());
        assertEquals(ScheduledMaintenance.TaskType.VACUUM, task.getTaskType());
        assertEquals("public", task.getTargetSchema());
        assertEquals("transactions", task.getTargetObject());
        assertEquals(ScheduledMaintenance.ScheduleType.CRON, task.getScheduleType());
        assertEquals("0 3 * * *", task.getCronExpression());
        assertEquals(25.0, task.getActivityThreshold());
        assertEquals(LocalTime.of(1, 0), task.getPreferredWindowStart());
        assertEquals(LocalTime.of(4, 0), task.getPreferredWindowEnd());
        assertNotNull(task.getPreferredDays());
        assertEquals(2, task.getPreferredDays().size());
        assertEquals(24, task.getMinIntervalHours());
        assertEquals(10.0, task.getMaxTableSizeGb());
        assertEquals(120, task.getMaxDurationMinutes());
        assertEquals(10, task.getPriority());
        assertFalse(task.isEnabled());
        assertEquals("dba", task.getCreatedBy());
        assertEquals(5000L, task.getLastRunDurationMs());
        assertEquals("SUCCESS", task.getLastRunStatus());
        assertEquals(now, task.getScheduledTime());
        assertEquals(now, task.getLastRunAt());
        assertNotNull(task.getNextRunAt());
    }

    @Test
    void testGetTargetTable() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetTable("my_table");

        assertEquals("my_table", task.getTargetTable());
        assertEquals("my_table", task.getTargetObject());
    }

    @Test
    void testSetTargetTable() {
        ScheduledMaintenance task = new ScheduledMaintenance();
        task.setTargetTable("another_table");

        assertEquals("another_table", task.getTargetObject());
    }

    @Test
    void testAllTaskTypeEnums() {
        for (ScheduledMaintenance.TaskType taskType : ScheduledMaintenance.TaskType.values()) {
            assertNotNull(taskType.getDisplayName());
            assertFalse(taskType.getDisplayName().isEmpty());
            assertNotNull(taskType.getIcon());
            assertNotNull(taskType.getDescription());
        }
    }

    @Test
    void testAllScheduleTypeEnums() {
        for (ScheduledMaintenance.ScheduleType scheduleType : ScheduledMaintenance.ScheduleType.values()) {
            assertNotNull(scheduleType.getDisplayName());
            assertFalse(scheduleType.getDisplayName().isEmpty());
            assertNotNull(scheduleType.getDescription());
        }
    }
}
