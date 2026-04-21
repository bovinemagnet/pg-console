package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OverviewStatsTableSizeTest {

    @Test
    void fullNameCombinesSchemaAndTable() {
        OverviewStats.TableSize t = new OverviewStats.TableSize();
        t.setSchemaName("public");
        t.setTableName("users");
        assertThat(t.getFullName()).isEqualTo("public.users");
    }

    @Test
    void fullNameOmitsSchemaWhenNull() {
        OverviewStats.TableSize t = new OverviewStats.TableSize();
        t.setSchemaName(null);
        t.setTableName("users");
        assertThat(t.getFullName()).isEqualTo("users");
    }

    @Test
    void fullNameOmitsSchemaWhenEmpty() {
        OverviewStats.TableSize t = new OverviewStats.TableSize();
        t.setSchemaName("");
        t.setTableName("users");
        assertThat(t.getFullName()).isEqualTo("users");
    }

    @Test
    void fullNameNeverContainsLiteralNull() {
        OverviewStats.TableSize t = new OverviewStats.TableSize();
        assertThat(t.getFullName()).doesNotContain("null");
    }
}
