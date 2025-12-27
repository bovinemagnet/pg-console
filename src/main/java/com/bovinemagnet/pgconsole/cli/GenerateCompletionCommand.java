package com.bovinemagnet.pgconsole.cli;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * CLI command to generate shell completion scripts.
 * <p>
 * Generates completion scripts for bash and zsh shells that provide
 * tab-completion for pg-console commands and options.
 * <p>
 * Usage:
 * <pre>
 * # Generate bash completion
 * pg-console generate-completion --bash > ~/.pg-console-completion.bash
 * source ~/.pg-console-completion.bash
 *
 * # Generate zsh completion
 * pg-console generate-completion --zsh > ~/.zsh/completions/_pg-console
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "generate-completion",
        description = "Generate shell completion scripts for bash or zsh",
        mixinStandardHelpOptions = true
)
public class GenerateCompletionCommand implements Runnable {

    @Option(names = {"--bash"},
            description = "Generate bash completion script")
    private boolean bash;

    @Option(names = {"--zsh"},
            description = "Generate zsh completion script")
    private boolean zsh;

    @Option(names = {"-o", "--output"},
            description = "Output file path (default: stdout)")
    private String outputFile;

    @ParentCommand
    private PgConsoleCommand parent;

    @Override
    public void run() {
        if (!bash && !zsh) {
            System.out.println("Please specify --bash or --zsh");
            System.out.println();
            System.out.println("Usage examples:");
            System.out.println("  pg-console generate-completion --bash > ~/.pg-console-completion.bash");
            System.out.println("  pg-console generate-completion --zsh > ~/.zsh/completions/_pg-console");
            System.exit(1);
        }

        try {
            PrintWriter out;
            if (outputFile != null && !outputFile.isBlank()) {
                out = new PrintWriter(new FileWriter(outputFile));
            } else {
                out = new PrintWriter(System.out, true);
            }

            CommandLine cmd = new CommandLine(new PgConsoleCommand());

            if (bash) {
                generateBashCompletion(out, cmd);
            } else if (zsh) {
                generateZshCompletion(out, cmd);
            }

            if (outputFile != null) {
                out.close();
                System.err.println("Completion script written to: " + outputFile);
                System.err.println();
                System.err.println("To enable, add to your shell profile:");
                if (bash) {
                    System.err.println("  source " + outputFile);
                } else {
                    System.err.println("  fpath=($(dirname " + outputFile + ") $fpath)");
                    System.err.println("  autoload -Uz compinit && compinit");
                }
            }

            System.exit(0);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private void generateBashCompletion(PrintWriter out, CommandLine cmd) {
        // Generate using Picocli's AutoComplete
        String script = AutoComplete.bash("pg-console", cmd);
        out.println(script);
    }

    private void generateZshCompletion(PrintWriter out, CommandLine cmd) {
        // Generate zsh completion script
        out.println("#compdef pg-console");
        out.println();
        out.println("# PG Console zsh completion script");
        out.println("# Generated automatically - do not edit");
        out.println();
        out.println("_pg-console() {");
        out.println("    local curcontext=\"$curcontext\" state line");
        out.println("    typeset -A opt_args");
        out.println();
        out.println("    _arguments -C \\");
        out.println("        '(-h --help)'{-h,--help}'[Show help message]' \\");
        out.println("        '(-V --version)'{-V,--version}'[Print version information]' \\");
        out.println("        '(-p --port)'{-p,--port}'[HTTP server port]:port:' \\");
        out.println("        '--host[HTTP bind address]:host:' \\");
        out.println("        '--no-history[Disable history sampling]' \\");
        out.println("        '--no-alerting[Disable alerting]' \\");
        out.println("        '(-i --instance)'{-i,--instance}'[Default instance]:instance:->instances' \\");
        out.println("        '(-c --config)'{-c,--config}'[Config file]:file:_files' \\");
        out.println("        '--verbose[Enable verbose logging]' \\");
        out.println("        '1: :->command' \\");
        out.println("        '*::arg:->args'");
        out.println();
        out.println("    case $state in");
        out.println("        command)");
        out.println("            local commands=(");
        out.println("                'health-check:Test database connectivity'");
        out.println("                'list-instances:List configured instances'");
        out.println("                'init-schema:Initialise pgconsole schema'");
        out.println("                'reset-stats:Reset pg_stat_statements'");
        out.println("                'export-report:Generate incident report'");
        out.println("                'export-config:Export configuration'");
        out.println("                'validate-config:Validate configuration'");
        out.println("                'generate-completion:Generate completion script'");
        out.println("            )");
        out.println("            _describe -t commands 'pg-console command' commands");
        out.println("            ;;");
        out.println("        instances)");
        out.println("            # Could be enhanced to read from config");
        out.println("            local instances=(default production staging)");
        out.println("            _describe -t instances 'instance' instances");
        out.println("            ;;");
        out.println("        args)");
        out.println("            case $line[1] in");
        out.println("                health-check)");
        out.println("                    _arguments \\");
        out.println("                        '(-i --instance)'{-i,--instance}'[Target instance]:instance:' \\");
        out.println("                        '--verbose[Show detailed output]'");
        out.println("                    ;;");
        out.println("                list-instances)");
        out.println("                    _arguments \\");
        out.println("                        '(-v --verbose)'{-v,--verbose}'[Show details]' \\");
        out.println("                        '--json[Output as JSON]'");
        out.println("                    ;;");
        out.println("                init-schema)");
        out.println("                    _arguments \\");
        out.println("                        '(-i --instance)'{-i,--instance}'[Target instance]:instance:' \\");
        out.println("                        '--dry-run[Show what would be done]' \\");
        out.println("                        '--force[Force re-run migrations]'");
        out.println("                    ;;");
        out.println("                reset-stats)");
        out.println("                    _arguments \\");
        out.println("                        '(-i --instance)'{-i,--instance}'[Target instance]:instance:' \\");
        out.println("                        '(-f --force)'{-f,--force}'[Skip confirmation]' \\");
        out.println("                        '--all[Reset all databases]' \\");
        out.println("                        '(-d --database)'{-d,--database}'[Specific database]:database:'");
        out.println("                    ;;");
        out.println("                export-report)");
        out.println("                    _arguments \\");
        out.println("                        '(-i --instance)'{-i,--instance}'[Target instance]:instance:' \\");
        out.println("                        '(-o --output)'{-o,--output}'[Output file]:file:_files' \\");
        out.println("                        '--format[Output format]:format:(text markdown json)' \\");
        out.println("                        '--include-queries[Include query text]' \\");
        out.println("                        '--top[Number of items]:count:'");
        out.println("                    ;;");
        out.println("                export-config)");
        out.println("                    _arguments \\");
        out.println("                        '(-o --output)'{-o,--output}'[Output file]:file:_files' \\");
        out.println("                        '--format[Output format]:format:(properties yaml env)' \\");
        out.println("                        '--prefix[Filter prefix]:prefix:' \\");
        out.println("                        '--show-sources[Show config sources]' \\");
        out.println("                        '--include-sensitive[Include passwords]'");
        out.println("                    ;;");
        out.println("                validate-config)");
        out.println("                    _arguments \\");
        out.println("                        '(-c --config)'{-c,--config}'[Config file]:file:_files' \\");
        out.println("                        '--strict[Treat warnings as errors]' \\");
        out.println("                        '--skip-db[Skip database checks]' \\");
        out.println("                        '--verbose[Show all properties]'");
        out.println("                    ;;");
        out.println("                generate-completion)");
        out.println("                    _arguments \\");
        out.println("                        '--bash[Generate bash completion]' \\");
        out.println("                        '--zsh[Generate zsh completion]' \\");
        out.println("                        '(-o --output)'{-o,--output}'[Output file]:file:_files'");
        out.println("                    ;;");
        out.println("            esac");
        out.println("            ;;");
        out.println("    esac");
        out.println("}");
        out.println();
        out.println("_pg-console \"$@\"");
    }
}
