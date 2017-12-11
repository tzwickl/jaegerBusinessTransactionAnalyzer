package rocks.inspectit.jaeger.bt;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.jaeger.bt.analyzer.TracesAnalyzer;
import rocks.inspectit.jaeger.bt.analyzer.TracesAnalyzerElasticsearch;
import rocks.inspectit.jaeger.bt.connectors.IDatabase;
import rocks.inspectit.jaeger.bt.connectors.cassandra.Cassandra;
import rocks.inspectit.jaeger.bt.connectors.elasticsearch.Elasticsearch;
import rocks.inspectit.jaeger.bt.model.trace.elasticsearch.Trace;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Cassandra.class);

    private static final String DB_HOST = "host";
    private static final String DB_KEYSPACE = "keyspace";
    private static final String SERVICE_NAME = "service";
    private static final String START_TIME = "start";
    private static final String END_TIME = "end";
    private static final String FOLLOW = "follow";
    private static final String DATABASE = "database";
    private static final String CASSANDRA = "cassandra";
    private static final String ELASTICSEARCH = "elasticsearch";

    public static void main(String[] args) {
        final CommandLine cmd = parseArguments(args);
        String dbHost = cmd.getOptionValue(DB_HOST);
        String dbKeyspace = cmd.getOptionValue(DB_KEYSPACE);
        String serviceName = cmd.getOptionValue(SERVICE_NAME);
        IDatabase database = null;

        switch (cmd.getOptionValue(DATABASE)) {
            case CASSANDRA:
                database = new Cassandra(dbHost, dbKeyspace);
                logger.info("Using database " + CASSANDRA);
                break;
            case ELASTICSEARCH:
                database = new Elasticsearch(dbHost, dbKeyspace);
                logger.info("Using database " + ELASTICSEARCH);
                break;
            default:
                logger.error(cmd.getOptionValue(DATABASE) + " is not a known database!");
                System.exit(1);
        }

        Long startTime = null;
        Long endTime = null;
        try {
            startTime = Long.parseLong(cmd.getOptionValue(START_TIME)) * 1000000;
            endTime = Long.parseLong(cmd.getOptionValue(END_TIME)) * 1000000;
        } catch (NumberFormatException e) {
            // Do nothing
        }

        List<Trace> traces;

        if (startTime != null && endTime != null) {
            traces = database.getTraces(serviceName, startTime, endTime);
        } else if (startTime != null) {
            traces = database.getTraces(serviceName, startTime);
        } else {
            traces = database.getTraces(serviceName);
        }

        logger.info("Number of Traces to analyze: " + traces.size());

        if (traces.isEmpty()) {
            logger.warn("No Traces found to analyze!");
            System.exit(0);
        }

        TracesAnalyzer tracesAnalyzer = new TracesAnalyzerElasticsearch(traces);
        tracesAnalyzer.findBusinessTransactions();

        logger.info("Finished analyzing Traces");

        database.saveTraces(traces);

        logger.info("Updated Traces in database");

        System.exit(0);
    }

    private static CommandLine parseArguments(String[] args) {
        Options options = new Options();

        Option host = new Option("h", DB_HOST, true, "The database host");
        host.setRequired(true);
        options.addOption(host);

        Option keyspace = new Option("k", DB_KEYSPACE, true, "The database keyspace name");
        keyspace.setRequired(true);
        options.addOption(keyspace);

        Option service = new Option("s", SERVICE_NAME, true, "The service name to validate");
        service.setRequired(true);
        options.addOption(service);

        Option startTime = new Option("b", START_TIME, true, "The start time in seconds (inclusive)");
        startTime.setRequired(false);
        options.addOption(startTime);

        Option endTime = new Option("e", END_TIME, true, "The end time in seconds (inclusive)");
        endTime.setRequired(false);
        options.addOption(endTime);

        Option follow = new Option("f", FOLLOW, true, "Poll every x seconds");
        follow.setRequired(false);
        options.addOption(follow);

        Option database = new Option("d", DATABASE, true, "The database to use (cassandra, elasticsearch)");
        database.setRequired(true);
        options.addOption(database);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
             return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("jaegerBusinessTransactionDetector", options);

            System.exit(1);
            return null;
        }
    }
}
