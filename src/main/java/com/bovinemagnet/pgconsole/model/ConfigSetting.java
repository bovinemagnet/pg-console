package com.bovinemagnet.pgconsole.model;

/**
 * Represents a PostgreSQL configuration setting with health status.
 * <p>
 * Used to display configuration settings along with their current values,
 * recommended values, and health status indicators for the configuration
 * sanity check dashboard.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ConfigSetting {

	/**
	 * Health status levels for configuration settings.
	 */
	public enum Status {
		OK("OK", "bg-success", "text-success"),
		WARNING("Warning", "bg-warning text-dark", "text-warning"),
		CRITICAL("Critical", "bg-danger", "text-danger"),
		INFO("Info", "bg-info", "text-info"),
		UNKNOWN("Unknown", "bg-secondary", "text-muted");

		private final String displayName;
		private final String badgeCssClass;
		private final String textCssClass;

		Status(String displayName, String badgeCssClass, String textCssClass) {
			this.displayName = displayName;
			this.badgeCssClass = badgeCssClass;
			this.textCssClass = textCssClass;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getBadgeCssClass() {
			return badgeCssClass;
		}

		public String getTextCssClass() {
			return textCssClass;
		}
	}

	/**
	 * Categories of configuration settings.
	 */
	public enum Category {
		CONNECTIONS("Connections", "bi-plug"),
		MEMORY("Memory", "bi-memory"),
		WAL("WAL & Checkpoints", "bi-journal-bookmark"),
		VACUUM("Autovacuum", "bi-recycle"),
		LOGGING("Logging", "bi-file-text"),
		MONITORING("Monitoring", "bi-graph-up"),
		EXTENSIONS("Extensions", "bi-puzzle");

		private final String displayName;
		private final String iconClass;

		Category(String displayName, String iconClass) {
			this.displayName = displayName;
			this.iconClass = iconClass;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getIconClass() {
			return iconClass;
		}
	}

	private String name;
	private String currentValue;
	private String unit;
	private String recommendedValue;
	private String description;
	private Status status;
	private Category category;
	private String recommendation;
	private String source;

	public ConfigSetting() {}

	public ConfigSetting(String name, String currentValue, String unit, Status status, Category category, String description) {
		this.name = name;
		this.currentValue = currentValue;
		this.unit = unit;
		this.status = status;
		this.category = category;
		this.description = description;
	}

	// Getters and Setters

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getRecommendedValue() {
		return recommendedValue;
	}

	public void setRecommendedValue(String recommendedValue) {
		this.recommendedValue = recommendedValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	// Helper methods

	public String getStatusBadgeClass() {
		return status != null ? status.getBadgeCssClass() : "bg-secondary";
	}

	public String getStatusTextClass() {
		return status != null ? status.getTextCssClass() : "text-muted";
	}

	public String getStatusDisplayName() {
		return status != null ? status.getDisplayName() : "Unknown";
	}

	public String getCategoryDisplayName() {
		return category != null ? category.getDisplayName() : "Other";
	}

	public String getCategoryIconClass() {
		return category != null ? category.getIconClass() : "bi-gear";
	}

	public String getDisplayValue() {
		if (unit == null || unit.isEmpty()) {
			return currentValue;
		}
		return currentValue + " " + unit;
	}

	public boolean hasRecommendation() {
		return recommendation != null && !recommendation.isEmpty();
	}

	public boolean hasRecommendedValue() {
		return recommendedValue != null && !recommendedValue.isEmpty();
	}

	/**
	 * Creates a builder for fluent construction.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating ConfigSetting instances.
	 */
	public static class Builder {

		private final ConfigSetting setting = new ConfigSetting();

		public Builder name(String name) {
			setting.setName(name);
			return this;
		}

		public Builder currentValue(String currentValue) {
			setting.setCurrentValue(currentValue);
			return this;
		}

		public Builder unit(String unit) {
			setting.setUnit(unit);
			return this;
		}

		public Builder recommendedValue(String recommendedValue) {
			setting.setRecommendedValue(recommendedValue);
			return this;
		}

		public Builder description(String description) {
			setting.setDescription(description);
			return this;
		}

		public Builder status(Status status) {
			setting.setStatus(status);
			return this;
		}

		public Builder category(Category category) {
			setting.setCategory(category);
			return this;
		}

		public Builder recommendation(String recommendation) {
			setting.setRecommendation(recommendation);
			return this;
		}

		public Builder source(String source) {
			setting.setSource(source);
			return this;
		}

		public ConfigSetting build() {
			return setting;
		}
	}
}
