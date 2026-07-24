# m05 — Duplicate/non-unique indexes and dead columns

- **Severity:** Minor
- **Area:** Migrations
- **Locations:** `db/migration/V1__initial_schema.sql:408,236,279-284`

`idx_active_alert_id` duplicates the UNIQUE index; `idx_comparison_profile_default` is non-unique so two is_default=TRUE profiles are possible (findDefault picks arbitrarily); `notification_channel.description/success_count/failure_count` are never read/written. Clean up.
