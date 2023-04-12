package org.palladiosimulator.dependencytool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.palladiosimulator.dependencytool.dependencies.DependencyCalculator;
import org.palladiosimulator.dependencytool.dependencies.UpdateSiteTypes;
import org.palladiosimulator.dependencytool.github.RepositoryObject;
import org.palladiosimulator.dependencytool.graph.GraphicalRepresentation;
import org.palladiosimulator.dependencytool.neo4j.EmbeddedNeo4j;
import org.palladiosimulator.dependencytool.util.OutputType;
import org.palladiosimulator.dependencytool.util.Views;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Command Line Interface for the dependency tool.
 */
public final class DependencyCLI {

    private static final Logger LOGGER = Logger.getLogger(DependencyCLI.class.getName());

    /**
     * Main method for CLI.
     *
     * @param args organization and authentication-token are required arguments.
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            LOGGER.warning(e.getMessage());
            printHelp(options);
            return;
        }

        final String githubOAuthToken = cmd.getOptionValue("at");
        final String updateSiteUrl = cmd.getOptionValue("us");
        final boolean includeImports = cmd.hasOption("ii");
        final boolean jsonOutput = cmd.hasOption("json");
        final boolean includeArchived = cmd.hasOption("ia");
        UpdateSiteTypes updateSiteType = UpdateSiteTypes.NIGHTLY;
        final Set<String> reposToIgnore = new HashSet<>();

        if (cmd.hasOption("help")) {
            printHelp(options);
            return;
        }
        if (cmd.hasOption("ur")) {
            updateSiteType = UpdateSiteTypes.RELEASE;
        }
        if (cmd.hasOption("ri")) {
            reposToIgnore.addAll(Arrays.asList(cmd.getOptionValue("ri").split(",")));
        }
        if (cmd.hasOption("rif")) {
            try (BufferedReader in = new BufferedReader(new FileReader(cmd.getOptionValue("rif")))) {
                String line = null;
                while ((line = in.readLine()) != null) {
                    reposToIgnore.add(line);
                }
            } catch (final IOException e) {
                LOGGER.warning("Something went wrong while opening the file, please make sure the path is correct. "
                        + e.getMessage());
            }
        }
        final OutputType outputType = OutputType.valueOf(cmd.getOptionValue("o").toUpperCase());

        try {
            Set<GHRepository> repos = repositoriesFromArgs(cmd.getArgList(), GitHub.connectUsingOAuth(githubOAuthToken));
            final DependencyCalculator dc = new DependencyCalculator(updateSiteUrl, updateSiteType, includeImports, reposToIgnore, includeArchived);
            dc.addAll(repos);

            final Set<RepositoryObject> repositories = dc.calculateDependencies();
            final GraphicalRepresentation graphRep = new GraphicalRepresentation(repositories);
            graphRep.createTopologyHierarchy();
            final List<Set<RepositoryObject>> topology = graphRep.getTopologyHierachy();

            createOutput(outputType, jsonOutput, repositories, topology);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.warning("Please make sure you entered the correct organization and authentication token. "
                    + e.getMessage());
        }
    }

    private static Options createOptions() {
        final Options options = new Options();
        options.addRequiredOption("at", "oauth", true, "OAuth authentication token for GitHub API.")
                .addRequiredOption("us", "update-site", true, "The update site to use")
                .addRequiredOption("o", "output", true, "Decide what to output. One of " + Arrays.toString(OutputType.values()) + ".");

        options.addOption("h", "help", false, "Print this message")
                .addOption("ur", "use-release", false, "Use release update site instead of nightly.")
                .addOption("ii", "include-imports", false, "Consider feature.xml includes while calculating dependencies.")
                .addOption("j", "json", false, "Format the output as json.")
                .addOption("ri", "repository-ignore", true, "Specify one or more repositories which should be ignored when calculating dependencies. Split by an underscore.")
                .addOption("rif", "repository-ignore-file", true, "Path to file with repositories to ignore. Each repository name must be in a new line.")
                .addOption("ia", "include-archived", false, "Include archived repositories into the dependency calculation.");

        return options;
    }

    private static void createOutput(OutputType outputType, boolean jsonOutput,
            Set<RepositoryObject> repositories, List<Set<RepositoryObject>> topology) throws JsonProcessingException {

        switch (outputType) {
            case NEO4J -> {
                try (EmbeddedNeo4j neo4j = new EmbeddedNeo4j()) {
                    neo4j.commit(repositories);
                } catch (final Exception e) {
                    LOGGER.warning(e.getMessage());
                }
            }
            case TOPOLOGY -> {
                if (jsonOutput) {
                    final ObjectMapper objectMapper = new ObjectMapper();
                    final DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
                    final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
                    pp.indentArraysWith(indenter);
                    pp.indentObjectsWith(indenter);
                    System.out.println(objectMapper.writer(pp).withView(Views.Topology.class).writeValueAsString(topology));
                } else {
                    System.out.println(topology.toString().replaceAll("],", "],\n"));
                }
            }
            case DEPENDENCIES -> {
                if (jsonOutput) {
                    final ObjectMapper objectMapper = new ObjectMapper();
                    for (final RepositoryObject repo : repositories) {
                        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().withView(Views.Dependency.class)
                                .writeValueAsString(repo));
                    }
                } else {
                    for (final RepositoryObject repo : repositories) {
                        final StringBuilder dependencyString = new StringBuilder();
                        dependencyString.append("Name: ").append(repo.getRepositoryName()).append("\n");
                        dependencyString.append("Address: ").append(repo.getRepositoryAddress()).append("\n");
                        dependencyString.append("UpdateSite: ").append(repo.getUpdateSite()).append("\n");
                        dependencyString.append("Dependencies: ").append(repo.getDependencies()).append("\n");
                        System.out.println(dependencyString.toString());
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unknown output type: " + outputType);
        }
    }

    private static void printHelp(final Options options) {
        new HelpFormatter().printHelp("java -jar dependencytool.jar [flags] [<org> <user/repo> ...]", options);
    }

    private static Set<GHRepository> repositoriesFromArgs(final List<String> args, final GitHub github) throws IOException {
        final HashSet<GHRepository> githubRepos = new HashSet<>();
        for (String repoOrOrganization : args) {
            final boolean isOrganization = !repoOrOrganization.contains("/");
            if (isOrganization) {
                githubRepos.addAll(github.getOrganization(repoOrOrganization).getRepositories().values());
            } else {
                githubRepos.add(github.getRepository(repoOrOrganization));
            }
        }
        return githubRepos;
    }

    /**
     * Private constructor to avoid object generation.
     */
    private DependencyCLI() {
        throw new IllegalStateException("Utility-class constructor.");
    }

}
