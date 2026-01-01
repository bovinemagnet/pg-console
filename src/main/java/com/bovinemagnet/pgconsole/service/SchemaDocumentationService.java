package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.FunctionSchema;
import com.bovinemagnet.pgconsole.model.SequenceSchema;
import com.bovinemagnet.pgconsole.model.TableSchema;
import com.bovinemagnet.pgconsole.model.TypeSchema;
import com.bovinemagnet.pgconsole.model.ViewSchema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for generating schema documentation in multiple formats.
 * Uses SchemaExtractorService to gather metadata and generates
 * comprehensive data dictionaries.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
@ApplicationScoped
public class SchemaDocumentationService {

    @Inject
    SchemaExtractorService schemaExtractor;

    /**
     * Supported output formats for schema documentation.
     */
    public enum OutputFormat {
        HTML, MARKDOWN, ASCIIDOC
    }

    /**
     * Options for controlling documentation generation.
     */
    public static class DocumentationOptions {
        public boolean includeTables = true;
        public boolean includeViews = true;
        public boolean includeFunctions = true;
        public boolean includeSequences = true;
        public boolean includeTypes = true;
        public boolean includeIndexes = true;
        public boolean includeConstraints = true;
        public boolean includeTriggers = true;
        public boolean includeComments = true;
        public boolean includeExtensions = true;

        public static DocumentationOptions all() {
            return new DocumentationOptions();
        }
    }

    /**
     * Generate schema documentation in the specified format.
     *
     * @param instanceId the database instance identifier
     * @param schemaName the schema to document (e.g., "public")
     * @param format     the output format (HTML, MARKDOWN, ASCIIDOC)
     * @param options    options controlling what to include
     * @return the generated documentation as a string
     */
    public String generateDocumentation(String instanceId, String schemaName,
                                        OutputFormat format, DocumentationOptions options) {
        // Gather all schema metadata
        List<TableSchema> tables = options.includeTables ?
                schemaExtractor.extractTables(instanceId, schemaName) : List.of();
        List<ViewSchema> views = options.includeViews ?
                schemaExtractor.extractViews(instanceId, schemaName) : List.of();
        List<FunctionSchema> functions = options.includeFunctions ?
                schemaExtractor.extractFunctions(instanceId, schemaName) : List.of();
        List<SequenceSchema> sequences = options.includeSequences ?
                schemaExtractor.extractSequences(instanceId, schemaName) : List.of();
        List<TypeSchema> types = options.includeTypes ?
                schemaExtractor.extractTypes(instanceId, schemaName) : List.of();
        Map<String, String> extensions = options.includeExtensions ?
                schemaExtractor.extractExtensions(instanceId) : Map.of();
        Map<String, Integer> summary = schemaExtractor.getSchemaSummary(instanceId, schemaName);

        // Generate in requested format
        return switch (format) {
            case HTML -> generateHtml(instanceId, schemaName, tables, views, functions,
                    sequences, types, extensions, summary, options);
            case MARKDOWN -> generateMarkdown(instanceId, schemaName, tables, views, functions,
                    sequences, types, extensions, summary, options);
            case ASCIIDOC -> generateAsciiDoc(instanceId, schemaName, tables, views, functions,
                    sequences, types, extensions, summary, options);
        };
    }

    /**
     * Get list of available schemas for an instance.
     */
    public List<String> getSchemas(String instanceId) {
        return schemaExtractor.getSchemas(instanceId);
    }

    // ========================================
    // HTML Generation
    // ========================================

    private String generateHtml(String instanceId, String schemaName,
                                List<TableSchema> tables, List<ViewSchema> views,
                                List<FunctionSchema> functions, List<SequenceSchema> sequences,
                                List<TypeSchema> types, Map<String, String> extensions,
                                Map<String, Integer> summary, DocumentationOptions options) {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Schema Documentation: ").append(escapeHtml(schemaName)).append("</title>\n");
        sb.append("<style>\n");
        sb.append(getHtmlStyles());
        sb.append("</style>\n</head>\n<body>\n");

        // Header
        sb.append("<header>\n");
        sb.append("<h1>Schema Documentation</h1>\n");
        sb.append("<p class=\"meta\">Instance: <strong>").append(escapeHtml(instanceId))
                .append("</strong> | Schema: <strong>").append(escapeHtml(schemaName))
                .append("</strong> | Generated: ").append(timestamp).append("</p>\n");
        sb.append("</header>\n");

        // Table of Contents
        sb.append("<nav id=\"toc\">\n<h2>Contents</h2>\n<ul>\n");
        sb.append("<li><a href=\"#summary\">Summary</a></li>\n");
        if (!tables.isEmpty()) sb.append("<li><a href=\"#tables\">Tables (").append(tables.size()).append(")</a></li>\n");
        if (!views.isEmpty()) sb.append("<li><a href=\"#views\">Views (").append(views.size()).append(")</a></li>\n");
        if (!functions.isEmpty()) sb.append("<li><a href=\"#functions\">Functions (").append(functions.size()).append(")</a></li>\n");
        if (!sequences.isEmpty()) sb.append("<li><a href=\"#sequences\">Sequences (").append(sequences.size()).append(")</a></li>\n");
        if (!types.isEmpty()) sb.append("<li><a href=\"#types\">Custom Types (").append(types.size()).append(")</a></li>\n");
        if (!extensions.isEmpty()) sb.append("<li><a href=\"#extensions\">Extensions (").append(extensions.size()).append(")</a></li>\n");
        sb.append("</ul>\n</nav>\n");

        // Summary
        sb.append("<section id=\"summary\">\n<h2>Summary</h2>\n");
        sb.append("<table class=\"summary-table\">\n");
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            sb.append("<tr><td>").append(escapeHtml(entry.getKey()))
                    .append("</td><td>").append(entry.getValue()).append("</td></tr>\n");
        }
        sb.append("</table>\n</section>\n");

        // Tables
        if (!tables.isEmpty()) {
            sb.append("<section id=\"tables\">\n<h2>Tables</h2>\n");
            for (TableSchema table : tables) {
                appendTableHtml(sb, table, options);
            }
            sb.append("</section>\n");
        }

        // Views
        if (!views.isEmpty()) {
            sb.append("<section id=\"views\">\n<h2>Views</h2>\n");
            for (ViewSchema view : views) {
                appendViewHtml(sb, view);
            }
            sb.append("</section>\n");
        }

        // Functions
        if (!functions.isEmpty()) {
            sb.append("<section id=\"functions\">\n<h2>Functions</h2>\n");
            for (FunctionSchema func : functions) {
                appendFunctionHtml(sb, func);
            }
            sb.append("</section>\n");
        }

        // Sequences
        if (!sequences.isEmpty()) {
            sb.append("<section id=\"sequences\">\n<h2>Sequences</h2>\n");
            sb.append("<table class=\"data-table\">\n");
            sb.append("<thead><tr><th>Name</th><th>Data Type</th><th>Start</th><th>Increment</th><th>Min</th><th>Max</th><th>Cycle</th><th>Owner</th></tr></thead>\n");
            sb.append("<tbody>\n");
            for (SequenceSchema seq : sequences) {
                sb.append("<tr>");
                sb.append("<td>").append(escapeHtml(seq.getSequenceName())).append("</td>");
                sb.append("<td>").append(escapeHtml(seq.getDataType())).append("</td>");
                sb.append("<td>").append(seq.getStartValue()).append("</td>");
                sb.append("<td>").append(seq.getIncrement()).append("</td>");
                sb.append("<td>").append(seq.getMinValue()).append("</td>");
                sb.append("<td>").append(seq.getMaxValue()).append("</td>");
                sb.append("<td>").append(seq.isCycle() ? "Yes" : "No").append("</td>");
                sb.append("<td>").append(escapeHtml(nullSafe(seq.getOwnedByDisplay()))).append("</td>");
                sb.append("</tr>\n");
            }
            sb.append("</tbody></table>\n</section>\n");
        }

        // Types
        if (!types.isEmpty()) {
            sb.append("<section id=\"types\">\n<h2>Custom Types</h2>\n");
            for (TypeSchema type : types) {
                appendTypeHtml(sb, type);
            }
            sb.append("</section>\n");
        }

        // Extensions
        if (!extensions.isEmpty()) {
            sb.append("<section id=\"extensions\">\n<h2>Extensions</h2>\n");
            sb.append("<table class=\"data-table\">\n");
            sb.append("<thead><tr><th>Extension</th><th>Version</th></tr></thead>\n");
            sb.append("<tbody>\n");
            for (Map.Entry<String, String> ext : extensions.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(ext.getKey()))
                        .append("</td><td>").append(escapeHtml(ext.getValue())).append("</td></tr>\n");
            }
            sb.append("</tbody></table>\n</section>\n");
        }

        sb.append("<footer>\n<p>Generated by PG Console Schema Documentation</p>\n</footer>\n");
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private void appendTableHtml(StringBuilder sb, TableSchema table, DocumentationOptions options) {
        sb.append("<article class=\"object-card\" id=\"table-").append(escapeHtml(table.getTableName())).append("\">\n");
        sb.append("<h3>").append(escapeHtml(table.getTableName()));
        if (table.isPartitioned()) sb.append(" <span class=\"badge\">Partitioned</span>");
        sb.append("</h3>\n");

        if (options.includeComments && table.getComment() != null && !table.getComment().isEmpty()) {
            sb.append("<p class=\"comment\">").append(escapeHtml(table.getComment())).append("</p>\n");
        }

        // Columns
        if (table.getColumns() != null && !table.getColumns().isEmpty()) {
            sb.append("<h4>Columns</h4>\n");
            sb.append("<table class=\"data-table\">\n");
            sb.append("<thead><tr><th>Column</th><th>Type</th><th>Nullable</th><th>Default</th>");
            if (options.includeComments) sb.append("<th>Comment</th>");
            sb.append("</tr></thead>\n<tbody>\n");
            for (TableSchema.ColumnDefinition col : table.getColumns()) {
                sb.append("<tr>");
                sb.append("<td><code>").append(escapeHtml(col.getColumnName())).append("</code>");
                if (col.isIdentity()) sb.append(" <span class=\"badge-small\">IDENTITY</span>");
                if (col.isGenerated()) sb.append(" <span class=\"badge-small\">GENERATED</span>");
                sb.append("</td>");
                sb.append("<td>").append(escapeHtml(col.getDataType())).append("</td>");
                sb.append("<td>").append(col.isNullable() ? "Yes" : "No").append("</td>");
                sb.append("<td><code>").append(escapeHtml(nullSafe(col.getDefaultValue()))).append("</code></td>");
                if (options.includeComments) {
                    sb.append("<td>").append(escapeHtml(nullSafe(col.getComment()))).append("</td>");
                }
                sb.append("</tr>\n");
            }
            sb.append("</tbody></table>\n");
        }

        // Primary Key
        if (options.includeConstraints && table.getPrimaryKey() != null) {
            sb.append("<h4>Primary Key</h4>\n");
            sb.append("<p><strong>").append(escapeHtml(table.getPrimaryKey().getConstraintName()))
                    .append("</strong>: ").append(String.join(", ", table.getPrimaryKey().getColumns())).append("</p>\n");
        }

        // Foreign Keys
        if (options.includeConstraints && table.getForeignKeys() != null && !table.getForeignKeys().isEmpty()) {
            sb.append("<h4>Foreign Keys</h4>\n");
            sb.append("<ul>\n");
            for (TableSchema.ForeignKeyDefinition fk : table.getForeignKeys()) {
                sb.append("<li><strong>").append(escapeHtml(fk.getConstraintName())).append("</strong>: ");
                sb.append(String.join(", ", fk.getColumns())).append(" → ");
                sb.append(escapeHtml(fk.getReferencedTable())).append("(");
                sb.append(String.join(", ", fk.getReferencedColumns())).append(")");
                sb.append(" [ON DELETE ").append(fk.getOnDelete()).append(", ON UPDATE ").append(fk.getOnUpdate()).append("]");
                sb.append("</li>\n");
            }
            sb.append("</ul>\n");
        }

        // Indexes
        if (options.includeIndexes && table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            sb.append("<h4>Indexes</h4>\n");
            sb.append("<table class=\"data-table\">\n");
            sb.append("<thead><tr><th>Name</th><th>Columns</th><th>Type</th><th>Unique</th></tr></thead>\n<tbody>\n");
            for (TableSchema.IndexDefinition idx : table.getIndexes()) {
                sb.append("<tr>");
                sb.append("<td>").append(escapeHtml(idx.getIndexName())).append("</td>");
                sb.append("<td>").append(String.join(", ", idx.getColumns())).append("</td>");
                sb.append("<td>").append(escapeHtml(idx.getIndexType())).append("</td>");
                sb.append("<td>").append(idx.isUnique() ? "Yes" : "No").append("</td>");
                sb.append("</tr>\n");
            }
            sb.append("</tbody></table>\n");
        }

        sb.append("</article>\n");
    }

    private void appendViewHtml(StringBuilder sb, ViewSchema view) {
        sb.append("<article class=\"object-card\" id=\"view-").append(escapeHtml(view.getViewName())).append("\">\n");
        sb.append("<h3>").append(escapeHtml(view.getViewName()));
        if (view.isMaterialised()) sb.append(" <span class=\"badge\">Materialised</span>");
        sb.append("</h3>\n");

        if (view.getComment() != null && !view.getComment().isEmpty()) {
            sb.append("<p class=\"comment\">").append(escapeHtml(view.getComment())).append("</p>\n");
        }

        sb.append("<h4>Definition</h4>\n");
        sb.append("<pre><code>").append(escapeHtml(view.getDefinition())).append("</code></pre>\n");

        sb.append("</article>\n");
    }

    private void appendFunctionHtml(StringBuilder sb, FunctionSchema func) {
        sb.append("<article class=\"object-card\" id=\"func-").append(escapeHtml(func.getFunctionName())).append("\">\n");
        sb.append("<h3>").append(escapeHtml(func.getFunctionName())).append("(");
        sb.append(escapeHtml(nullSafe(func.getArguments()))).append(")</h3>\n");

        sb.append("<table class=\"meta-table\">\n");
        sb.append("<tr><td>Returns</td><td>").append(escapeHtml(func.getReturnType())).append("</td></tr>\n");
        sb.append("<tr><td>Language</td><td>").append(escapeHtml(func.getLanguage())).append("</td></tr>\n");
        sb.append("<tr><td>Volatility</td><td>").append(escapeHtml(func.getVolatility().getDisplayName())).append("</td></tr>\n");
        if (func.isSecurityDefiner()) {
            sb.append("<tr><td>Security</td><td>SECURITY DEFINER</td></tr>\n");
        }
        sb.append("</table>\n");

        sb.append("</article>\n");
    }

    private void appendTypeHtml(StringBuilder sb, TypeSchema type) {
        sb.append("<article class=\"object-card\" id=\"type-").append(escapeHtml(type.getTypeName())).append("\">\n");
        sb.append("<h3>").append(escapeHtml(type.getTypeName()));
        sb.append(" <span class=\"badge\">").append(getTypeKindDisplay(type)).append("</span></h3>\n");

        if (type.getKind() == TypeSchema.TypeKind.ENUM && type.getEnumLabels() != null) {
            sb.append("<p><strong>Values:</strong> ").append(String.join(", ", type.getEnumLabels())).append("</p>\n");
        } else if (type.getKind() == TypeSchema.TypeKind.COMPOSITE && type.getAttributes() != null) {
            sb.append("<h4>Attributes</h4>\n<ul>\n");
            for (TypeSchema.CompositeAttribute attr : type.getAttributes()) {
                sb.append("<li><code>").append(escapeHtml(attr.getAttributeName()))
                        .append("</code>: ").append(escapeHtml(attr.getDataType())).append("</li>\n");
            }
            sb.append("</ul>\n");
        } else if (type.getKind() == TypeSchema.TypeKind.DOMAIN) {
            sb.append("<p><strong>Base Type:</strong> ").append(escapeHtml(nullSafe(type.getBaseType()))).append("</p>\n");
        }

        sb.append("</article>\n");
    }

    private String getTypeKindDisplay(TypeSchema type) {
        if (type.getKind() == null) return "UNKNOWN";
        return type.getKind().getDisplayName();
    }

    private String getHtmlStyles() {
        return """
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px;
                       color: #333; background: #f8f9fa; }
                header { border-bottom: 2px solid #0d6efd; padding-bottom: 20px; margin-bottom: 30px; }
                h1 { color: #0d6efd; margin-bottom: 10px; }
                h2 { color: #495057; border-bottom: 1px solid #dee2e6; padding-bottom: 10px; margin-top: 40px; }
                h3 { color: #212529; }
                h4 { color: #6c757d; font-size: 0.95rem; margin-top: 20px; }
                .meta { color: #6c757d; font-size: 0.9rem; }
                #toc { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                #toc ul { list-style: none; padding-left: 0; }
                #toc li { margin: 8px 0; }
                #toc a { color: #0d6efd; text-decoration: none; }
                #toc a:hover { text-decoration: underline; }
                section { background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;
                          box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .object-card { border: 1px solid #dee2e6; border-radius: 6px; padding: 15px; margin: 15px 0; }
                .badge { background: #0d6efd; color: white; padding: 2px 8px; border-radius: 4px;
                         font-size: 0.75rem; vertical-align: middle; }
                .badge-small { background: #6c757d; color: white; padding: 1px 5px; border-radius: 3px;
                               font-size: 0.7rem; }
                .comment { color: #6c757d; font-style: italic; }
                .data-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
                .data-table th, .data-table td { border: 1px solid #dee2e6; padding: 8px; text-align: left; }
                .data-table th { background: #f8f9fa; font-weight: 600; }
                .data-table tr:nth-child(even) { background: #f8f9fa; }
                .meta-table { width: auto; }
                .meta-table td { padding: 4px 12px 4px 0; }
                .meta-table td:first-child { font-weight: 600; color: #6c757d; }
                .summary-table { width: auto; }
                .summary-table td { padding: 8px 20px; }
                .summary-table td:first-child { font-weight: 600; }
                pre { background: #f8f9fa; padding: 15px; border-radius: 6px; overflow-x: auto; }
                code { font-family: 'SF Mono', Monaco, 'Courier New', monospace; font-size: 0.9em; }
                footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #dee2e6;
                         color: #6c757d; font-size: 0.85rem; text-align: center; }
                @media print { body { background: white; } section, #toc { box-shadow: none; } }
                """;
    }

    // ========================================
    // Markdown Generation
    // ========================================

    private String generateMarkdown(String instanceId, String schemaName,
                                    List<TableSchema> tables, List<ViewSchema> views,
                                    List<FunctionSchema> functions, List<SequenceSchema> sequences,
                                    List<TypeSchema> types, Map<String, String> extensions,
                                    Map<String, Integer> summary, DocumentationOptions options) {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("# Schema Documentation: ").append(schemaName).append("\n\n");
        sb.append("**Instance:** ").append(instanceId).append("  \n");
        sb.append("**Schema:** ").append(schemaName).append("  \n");
        sb.append("**Generated:** ").append(timestamp).append("\n\n");

        // Table of Contents
        sb.append("## Contents\n\n");
        sb.append("- [Summary](#summary)\n");
        if (!tables.isEmpty()) sb.append("- [Tables](#tables) (").append(tables.size()).append(")\n");
        if (!views.isEmpty()) sb.append("- [Views](#views) (").append(views.size()).append(")\n");
        if (!functions.isEmpty()) sb.append("- [Functions](#functions) (").append(functions.size()).append(")\n");
        if (!sequences.isEmpty()) sb.append("- [Sequences](#sequences) (").append(sequences.size()).append(")\n");
        if (!types.isEmpty()) sb.append("- [Custom Types](#custom-types) (").append(types.size()).append(")\n");
        if (!extensions.isEmpty()) sb.append("- [Extensions](#extensions) (").append(extensions.size()).append(")\n");
        sb.append("\n");

        // Summary
        sb.append("## Summary\n\n");
        sb.append("| Object Type | Count |\n|-------------|-------|\n");
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            sb.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
        }
        sb.append("\n");

        // Tables
        if (!tables.isEmpty()) {
            sb.append("## Tables\n\n");
            for (TableSchema table : tables) {
                appendTableMarkdown(sb, table, options);
            }
        }

        // Views
        if (!views.isEmpty()) {
            sb.append("## Views\n\n");
            for (ViewSchema view : views) {
                appendViewMarkdown(sb, view);
            }
        }

        // Functions
        if (!functions.isEmpty()) {
            sb.append("## Functions\n\n");
            for (FunctionSchema func : functions) {
                appendFunctionMarkdown(sb, func);
            }
        }

        // Sequences
        if (!sequences.isEmpty()) {
            sb.append("## Sequences\n\n");
            sb.append("| Name | Type | Start | Increment | Min | Max | Cycle | Owner |\n");
            sb.append("|------|------|-------|-----------|-----|-----|-------|-------|\n");
            for (SequenceSchema seq : sequences) {
                sb.append("| ").append(seq.getSequenceName())
                        .append(" | ").append(seq.getDataType())
                        .append(" | ").append(seq.getStartValue())
                        .append(" | ").append(seq.getIncrement())
                        .append(" | ").append(seq.getMinValue())
                        .append(" | ").append(seq.getMaxValue())
                        .append(" | ").append(seq.isCycle() ? "Yes" : "No")
                        .append(" | ").append(nullSafe(seq.getOwnedByDisplay()))
                        .append(" |\n");
            }
            sb.append("\n");
        }

        // Types
        if (!types.isEmpty()) {
            sb.append("## Custom Types\n\n");
            for (TypeSchema type : types) {
                appendTypeMarkdown(sb, type);
            }
        }

        // Extensions
        if (!extensions.isEmpty()) {
            sb.append("## Extensions\n\n");
            sb.append("| Extension | Version |\n|-----------|--------|\n");
            for (Map.Entry<String, String> ext : extensions.entrySet()) {
                sb.append("| ").append(ext.getKey()).append(" | ").append(ext.getValue()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("---\n\n*Generated by PG Console Schema Documentation*\n");

        return sb.toString();
    }

    private void appendTableMarkdown(StringBuilder sb, TableSchema table, DocumentationOptions options) {
        sb.append("### ").append(table.getTableName());
        if (table.isPartitioned()) sb.append(" *(Partitioned)*");
        sb.append("\n\n");

        if (options.includeComments && table.getComment() != null && !table.getComment().isEmpty()) {
            sb.append("> ").append(table.getComment()).append("\n\n");
        }

        // Columns
        if (table.getColumns() != null && !table.getColumns().isEmpty()) {
            sb.append("#### Columns\n\n");
            sb.append("| Column | Type | Nullable | Default |");
            if (options.includeComments) sb.append(" Comment |");
            sb.append("\n|--------|------|----------|---------|");
            if (options.includeComments) sb.append("---------|");
            sb.append("\n");
            for (TableSchema.ColumnDefinition col : table.getColumns()) {
                sb.append("| `").append(col.getColumnName()).append("`");
                if (col.isIdentity()) sb.append(" (IDENTITY)");
                if (col.isGenerated()) sb.append(" (GENERATED)");
                sb.append(" | ").append(col.getDataType());
                sb.append(" | ").append(col.isNullable() ? "Yes" : "No");
                sb.append(" | `").append(nullSafe(col.getDefaultValue())).append("`");
                if (options.includeComments) {
                    sb.append(" | ").append(nullSafe(col.getComment()));
                }
                sb.append(" |\n");
            }
            sb.append("\n");
        }

        // Primary Key
        if (options.includeConstraints && table.getPrimaryKey() != null) {
            sb.append("#### Primary Key\n\n");
            sb.append("**").append(table.getPrimaryKey().getConstraintName()).append("**: ");
            sb.append(String.join(", ", table.getPrimaryKey().getColumns())).append("\n\n");
        }

        // Foreign Keys
        if (options.includeConstraints && table.getForeignKeys() != null && !table.getForeignKeys().isEmpty()) {
            sb.append("#### Foreign Keys\n\n");
            for (TableSchema.ForeignKeyDefinition fk : table.getForeignKeys()) {
                sb.append("- **").append(fk.getConstraintName()).append("**: ");
                sb.append(String.join(", ", fk.getColumns())).append(" → ");
                sb.append(fk.getReferencedTable()).append("(");
                sb.append(String.join(", ", fk.getReferencedColumns())).append(") ");
                sb.append("[ON DELETE ").append(fk.getOnDelete());
                sb.append(", ON UPDATE ").append(fk.getOnUpdate()).append("]\n");
            }
            sb.append("\n");
        }

        // Indexes
        if (options.includeIndexes && table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            sb.append("#### Indexes\n\n");
            sb.append("| Name | Columns | Type | Unique |\n|------|---------|------|--------|\n");
            for (TableSchema.IndexDefinition idx : table.getIndexes()) {
                sb.append("| ").append(idx.getIndexName());
                sb.append(" | ").append(String.join(", ", idx.getColumns()));
                sb.append(" | ").append(idx.getIndexType());
                sb.append(" | ").append(idx.isUnique() ? "Yes" : "No").append(" |\n");
            }
            sb.append("\n");
        }
    }

    private void appendViewMarkdown(StringBuilder sb, ViewSchema view) {
        sb.append("### ").append(view.getViewName());
        if (view.isMaterialised()) sb.append(" *(Materialised)*");
        sb.append("\n\n");

        if (view.getComment() != null && !view.getComment().isEmpty()) {
            sb.append("> ").append(view.getComment()).append("\n\n");
        }

        sb.append("```sql\n").append(view.getDefinition()).append("\n```\n\n");
    }

    private void appendFunctionMarkdown(StringBuilder sb, FunctionSchema func) {
        sb.append("### ").append(func.getFunctionName()).append("(").append(nullSafe(func.getArguments())).append(")\n\n");
        sb.append("| Property | Value |\n|----------|-------|\n");
        sb.append("| Returns | ").append(func.getReturnType()).append(" |\n");
        sb.append("| Language | ").append(func.getLanguage()).append(" |\n");
        sb.append("| Volatility | ").append(func.getVolatility().getDisplayName()).append(" |\n");
        if (func.isSecurityDefiner()) {
            sb.append("| Security | SECURITY DEFINER |\n");
        }
        sb.append("\n");
    }

    private void appendTypeMarkdown(StringBuilder sb, TypeSchema type) {
        sb.append("### ").append(type.getTypeName()).append(" (").append(getTypeKindDisplay(type)).append(")\n\n");

        if (type.getKind() == TypeSchema.TypeKind.ENUM && type.getEnumLabels() != null) {
            sb.append("**Values:** ").append(String.join(", ", type.getEnumLabels())).append("\n\n");
        } else if (type.getKind() == TypeSchema.TypeKind.COMPOSITE && type.getAttributes() != null) {
            sb.append("**Attributes:**\n\n");
            for (TypeSchema.CompositeAttribute attr : type.getAttributes()) {
                sb.append("- `").append(attr.getAttributeName()).append("`: ").append(attr.getDataType()).append("\n");
            }
            sb.append("\n");
        } else if (type.getKind() == TypeSchema.TypeKind.DOMAIN) {
            sb.append("**Base Type:** ").append(nullSafe(type.getBaseType())).append("\n\n");
        }
    }

    // ========================================
    // AsciiDoc Generation
    // ========================================

    private String generateAsciiDoc(String instanceId, String schemaName,
                                    List<TableSchema> tables, List<ViewSchema> views,
                                    List<FunctionSchema> functions, List<SequenceSchema> sequences,
                                    List<TypeSchema> types, Map<String, String> extensions,
                                    Map<String, Integer> summary, DocumentationOptions options) {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("= Schema Documentation: ").append(schemaName).append("\n");
        sb.append(":toc: left\n");
        sb.append(":toclevels: 3\n");
        sb.append(":sectnums:\n");
        sb.append(":source-highlighter: highlight.js\n\n");

        sb.append("[.lead]\n");
        sb.append("*Instance:* ").append(instanceId).append(" +\n");
        sb.append("*Schema:* ").append(schemaName).append(" +\n");
        sb.append("*Generated:* ").append(timestamp).append("\n\n");

        // Summary
        sb.append("== Summary\n\n");
        sb.append("[cols=\"2,1\", options=\"header\"]\n|===\n| Object Type | Count\n");
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            sb.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append("\n");
        }
        sb.append("|===\n\n");

        // Tables
        if (!tables.isEmpty()) {
            sb.append("== Tables\n\n");
            for (TableSchema table : tables) {
                appendTableAsciiDoc(sb, table, options);
            }
        }

        // Views
        if (!views.isEmpty()) {
            sb.append("== Views\n\n");
            for (ViewSchema view : views) {
                appendViewAsciiDoc(sb, view);
            }
        }

        // Functions
        if (!functions.isEmpty()) {
            sb.append("== Functions\n\n");
            for (FunctionSchema func : functions) {
                appendFunctionAsciiDoc(sb, func);
            }
        }

        // Sequences
        if (!sequences.isEmpty()) {
            sb.append("== Sequences\n\n");
            sb.append("[cols=\"2,1,1,1,1,1,1,2\", options=\"header\"]\n|===\n");
            sb.append("| Name | Type | Start | Increment | Min | Max | Cycle | Owner\n");
            for (SequenceSchema seq : sequences) {
                sb.append("| ").append(seq.getSequenceName())
                        .append(" | ").append(seq.getDataType())
                        .append(" | ").append(seq.getStartValue())
                        .append(" | ").append(seq.getIncrement())
                        .append(" | ").append(seq.getMinValue())
                        .append(" | ").append(seq.getMaxValue())
                        .append(" | ").append(seq.isCycle() ? "Yes" : "No")
                        .append(" | ").append(nullSafe(seq.getOwnedByDisplay()))
                        .append("\n");
            }
            sb.append("|===\n\n");
        }

        // Types
        if (!types.isEmpty()) {
            sb.append("== Custom Types\n\n");
            for (TypeSchema type : types) {
                appendTypeAsciiDoc(sb, type);
            }
        }

        // Extensions
        if (!extensions.isEmpty()) {
            sb.append("== Extensions\n\n");
            sb.append("[cols=\"2,1\", options=\"header\"]\n|===\n| Extension | Version\n");
            for (Map.Entry<String, String> ext : extensions.entrySet()) {
                sb.append("| ").append(ext.getKey()).append(" | ").append(ext.getValue()).append("\n");
            }
            sb.append("|===\n\n");
        }

        sb.append("---\n\n_Generated by PG Console Schema Documentation_\n");

        return sb.toString();
    }

    private void appendTableAsciiDoc(StringBuilder sb, TableSchema table, DocumentationOptions options) {
        sb.append("=== ").append(table.getTableName());
        if (table.isPartitioned()) sb.append(" [small]#(Partitioned)#");
        sb.append("\n\n");

        if (options.includeComments && table.getComment() != null && !table.getComment().isEmpty()) {
            sb.append("[quote]\n____\n").append(table.getComment()).append("\n____\n\n");
        }

        // Columns
        if (table.getColumns() != null && !table.getColumns().isEmpty()) {
            sb.append("==== Columns\n\n");
            sb.append("[cols=\"2,2,1,2");
            if (options.includeComments) sb.append(",3");
            sb.append("\", options=\"header\"]\n|===\n| Column | Type | Nullable | Default");
            if (options.includeComments) sb.append(" | Comment");
            sb.append("\n");
            for (TableSchema.ColumnDefinition col : table.getColumns()) {
                sb.append("| `").append(col.getColumnName()).append("`");
                if (col.isIdentity()) sb.append(" [small]#IDENTITY#");
                if (col.isGenerated()) sb.append(" [small]#GENERATED#");
                sb.append(" | ").append(col.getDataType());
                sb.append(" | ").append(col.isNullable() ? "Yes" : "No");
                sb.append(" | `").append(nullSafe(col.getDefaultValue())).append("`");
                if (options.includeComments) {
                    sb.append(" | ").append(nullSafe(col.getComment()));
                }
                sb.append("\n");
            }
            sb.append("|===\n\n");
        }

        // Primary Key
        if (options.includeConstraints && table.getPrimaryKey() != null) {
            sb.append("==== Primary Key\n\n");
            sb.append("*").append(table.getPrimaryKey().getConstraintName()).append("*: ");
            sb.append(String.join(", ", table.getPrimaryKey().getColumns())).append("\n\n");
        }

        // Foreign Keys
        if (options.includeConstraints && table.getForeignKeys() != null && !table.getForeignKeys().isEmpty()) {
            sb.append("==== Foreign Keys\n\n");
            for (TableSchema.ForeignKeyDefinition fk : table.getForeignKeys()) {
                sb.append("* *").append(fk.getConstraintName()).append("*: ");
                sb.append(String.join(", ", fk.getColumns())).append(" → ");
                sb.append(fk.getReferencedTable()).append("(");
                sb.append(String.join(", ", fk.getReferencedColumns())).append(") ");
                sb.append("[ON DELETE ").append(fk.getOnDelete());
                sb.append(", ON UPDATE ").append(fk.getOnUpdate()).append("]\n");
            }
            sb.append("\n");
        }

        // Indexes
        if (options.includeIndexes && table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            sb.append("==== Indexes\n\n");
            sb.append("[cols=\"2,3,1,1\", options=\"header\"]\n|===\n| Name | Columns | Type | Unique\n");
            for (TableSchema.IndexDefinition idx : table.getIndexes()) {
                sb.append("| ").append(idx.getIndexName());
                sb.append(" | ").append(String.join(", ", idx.getColumns()));
                sb.append(" | ").append(idx.getIndexType());
                sb.append(" | ").append(idx.isUnique() ? "Yes" : "No").append("\n");
            }
            sb.append("|===\n\n");
        }
    }

    private void appendViewAsciiDoc(StringBuilder sb, ViewSchema view) {
        sb.append("=== ").append(view.getViewName());
        if (view.isMaterialised()) sb.append(" [small]#(Materialised)#");
        sb.append("\n\n");

        if (view.getComment() != null && !view.getComment().isEmpty()) {
            sb.append("[quote]\n____\n").append(view.getComment()).append("\n____\n\n");
        }

        sb.append("[source,sql]\n----\n").append(view.getDefinition()).append("\n----\n\n");
    }

    private void appendFunctionAsciiDoc(StringBuilder sb, FunctionSchema func) {
        sb.append("=== ").append(func.getFunctionName()).append("(").append(nullSafe(func.getArguments())).append(")\n\n");
        sb.append("[cols=\"1,2\"]\n|===\n");
        sb.append("| Returns | ").append(func.getReturnType()).append("\n");
        sb.append("| Language | ").append(func.getLanguage()).append("\n");
        sb.append("| Volatility | ").append(func.getVolatility().getDisplayName()).append("\n");
        if (func.isSecurityDefiner()) {
            sb.append("| Security | SECURITY DEFINER\n");
        }
        sb.append("|===\n\n");
    }

    private void appendTypeAsciiDoc(StringBuilder sb, TypeSchema type) {
        sb.append("=== ").append(type.getTypeName()).append(" (").append(getTypeKindDisplay(type)).append(")\n\n");

        if (type.getKind() == TypeSchema.TypeKind.ENUM && type.getEnumLabels() != null) {
            sb.append("*Values:* ").append(String.join(", ", type.getEnumLabels())).append("\n\n");
        } else if (type.getKind() == TypeSchema.TypeKind.COMPOSITE && type.getAttributes() != null) {
            sb.append("*Attributes:*\n\n");
            for (TypeSchema.CompositeAttribute attr : type.getAttributes()) {
                sb.append("* `").append(attr.getAttributeName()).append("`: ").append(attr.getDataType()).append("\n");
            }
            sb.append("\n");
        } else if (type.getKind() == TypeSchema.TypeKind.DOMAIN) {
            sb.append("*Base Type:* ").append(nullSafe(type.getBaseType())).append("\n\n");
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
