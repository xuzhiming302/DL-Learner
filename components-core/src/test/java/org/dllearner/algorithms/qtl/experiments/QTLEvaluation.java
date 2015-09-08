/**
 * 
 */
package org.dllearner.algorithms.qtl.experiments;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.collections15.ListUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.dllearner.algorithms.qtl.QTL2Disjunctive;
import org.dllearner.algorithms.qtl.QueryTreeUtils;
import org.dllearner.algorithms.qtl.datastructures.impl.EvaluatedRDFResourceTree;
import org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree;
import org.dllearner.algorithms.qtl.heuristics.QueryTreeHeuristic;
import org.dllearner.algorithms.qtl.heuristics.QueryTreeHeuristicSimple;
import org.dllearner.algorithms.qtl.impl.QueryTreeFactoryBase;
import org.dllearner.algorithms.qtl.operations.lgg.LGGGenerator;
import org.dllearner.algorithms.qtl.operations.lgg.LGGGeneratorSimple;
import org.dllearner.algorithms.qtl.util.Entailment;
import org.dllearner.algorithms.qtl.util.PrefixCCPrefixMapping;
import org.dllearner.algorithms.qtl.util.filters.PredicateExistenceFilter;
import org.dllearner.algorithms.qtl.util.filters.PredicateExistenceFilterDBpedia;
import org.dllearner.algorithms.qtl.util.statistics.TimeMonitors;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.Heuristics;
import org.dllearner.learningproblems.Heuristics.HeuristicType;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.utilities.QueryUtils;
import org.dllearner.core.StringRenderer;
import org.dllearner.core.StringRenderer.Rendering;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_NotEquals;
import com.hp.hpl.jena.sparql.expr.E_NotExists;
import com.hp.hpl.jena.sparql.expr.E_NumAbs;
import com.hp.hpl.jena.sparql.expr.E_Subtract;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountVarDistinct;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.util.TripleComparator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jamonapi.MonitorFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

/**
 * @author Lorenz Buehmann
 *
 */
public class QTLEvaluation {
	
	private static final Logger logger = Logger.getLogger(QTLEvaluation.class.getName());
	
	private static final ParameterizedSparqlString superClassesQueryTemplate2 = new ParameterizedSparqlString(
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> PREFIX owl: <http://www.w3.org/2002/07/owl#> "
			+ "SELECT ?sup WHERE {"
			+ "?sub ((rdfs:subClassOf|owl:equivalentClass)|^owl:equivalentClass)+ ?sup .}");
	
	private static final ParameterizedSparqlString superClassesQueryTemplate = new ParameterizedSparqlString(
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> PREFIX owl: <http://www.w3.org/2002/07/owl#> "
			+ "SELECT ?sup WHERE {"
			+ "?sub (rdfs:subClassOf|owl:equivalentClass)+ ?sup .}");
	
	enum NoiseMethod {
		RANDOM, SIMILAR, SIMILARITY_PARAMETERIZED
	}
	
	enum Baseline {
		RANDOM, MOST_POPULAR_TYPE_IN_KB, MOST_FREQUENT_TYPE_IN_EXAMPLES, MOST_INFORMATIVE_EDGE_IN_EXAMPLES, LGG, MOST_FREQUENT_EDGE_IN_EXAMPLES
	}

	QueryExecutionFactory qef;
	
	private org.dllearner.algorithms.qtl.impl.QueryTreeFactory queryTreeFactory;
	private ConciseBoundedDescriptionGenerator cbdGen;
	
	RandomDataGenerator rnd = new RandomDataGenerator();

	private EvaluationDataset dataset;
	
	private Map<String, List<String>> cache = new HashMap<String, List<String>>();
	
	private int kbSize;

	private boolean splitComplexQueries = true;
	
	PredicateExistenceFilter filter = new PredicateExistenceFilterDBpedia(null);
	
	// the directory where all files, results etc. are maintained
	private File benchmarkDirectory;

	// whether to write eval results to a database
	private boolean write2DB;
	
	// DB related objects
	private Connection conn;
	private PreparedStatement psInsertOverallEval;
	private PreparedStatement psInsertDetailEval;

	// max. time for each QTL run
	private int maxExecutionTimeInSeconds = 60;

	int minNrOfPositiveExamples = 9;
	
	int minTreeDepth = 3;
	int maxTreeDepth = 3;
	
	NoiseMethod noiseMethod = NoiseMethod.RANDOM;
	
	// whether to override existing results
	private boolean override = false;
	
	// parameters
	int[] nrOfExamplesIntervals = {
//					5,
//					10,
//					15,
					20,
//					25,
//					30
					}; 
			
	double[] noiseIntervals = {
					0.0,
//					0.1,
					0.2,
//					0.3,
//					0.4,
//					0.6
					};
			
	QueryTreeHeuristic[] heuristics = {
					new QueryTreeHeuristicSimple(),
//					new QueryTreeHeuristicComplex(qef)
			};
			
	HeuristicType[] measures = {
					HeuristicType.PRED_ACC, 
					HeuristicType.FMEASURE,
//					HeuristicType.MATTHEWS_CORRELATION
					};

	private File cacheDirectory;

	private boolean useEmailNotification = false;

	private int nrOfThreads;


	public QTLEvaluation(EvaluationDataset dataset, File benchmarkDirectory, boolean write2DB, boolean override, int maxQTLRuntime, boolean useEmailNotification, int nrOfThreads) throws ComponentInitException {
		this.dataset = dataset;
		this.benchmarkDirectory = benchmarkDirectory;
		this.write2DB = write2DB;
		this.override = override;
		this.maxExecutionTimeInSeconds = maxQTLRuntime;
		this.useEmailNotification = useEmailNotification;
		this.nrOfThreads = nrOfThreads;

		queryTreeFactory = new QueryTreeFactoryBase();
		queryTreeFactory.setMaxDepth(maxTreeDepth);
		
		// add some filters to avoid resources with namespaces like http://dbpedia.org/property/
		queryTreeFactory.addDropFilters((Filter<Statement>[]) dataset.getQueryTreeFilters().toArray(new Filter[]{}));
		
		qef = dataset.getKS().getQueryExecutionFactory();
		
		cbdGen = new ConciseBoundedDescriptionGeneratorImpl(qef);
		cbdGen.setRecursionDepth(maxTreeDepth);
		
		rnd.reSeed(123);
		
		kbSize = getKBSize();
		
		if(write2DB) {
			setupDatabase();
		}

		cacheDirectory = new File(benchmarkDirectory, "cache");
	}
	
	private void setupDatabase() {
		try {
			Properties config = new Properties();
			config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/dllearner/algorithms/qtl/qtl-eval-config.properties"));

			String url = config.getProperty("url");
			String username = config.getProperty("username");
			String password = config.getProperty("password");
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, username, password);
			
			String sql;
			java.sql.Statement stmt = conn.createStatement();

//			// empty tables if override
//			if(override) {
//				sql = "DROP TABLE IF EXISTS eval_overall,eval_detailed;";
//				sql = "ALTER TABLE IF EXISTS eval_overall DROP PRIMARY KEY;";
//				stmt.execute(sql);
//			}

			// create tables if not exist
			sql = "CREATE TABLE IF NOT EXISTS eval_overall (" +
					   "heuristic VARCHAR(100), " + 
					   "heuristic_measure VARCHAR(100), " +
					   "nrOfExamples TINYINT, " +
	                   "noise DOUBLE, " + 
	                   "avg_fscore_best_returned DOUBLE, " + 
	                   "avg_precision_best_returned DOUBLE, " + 
	                   "avg_recall_best_returned DOUBLE, " + 
	                   "avg_predacc_best_returned DOUBLE, " + 
	                   "avg_mathcorr_best_returned DOUBLE, " + 
	                   "avg_position_best DOUBLE, " +
	                   "avg_fscore_best DOUBLE, " + 
	                   "avg_precision_best DOUBLE, " + 
	                   "avg_recall_best DOUBLE, " + 
	                   "avg_predacc_best DOUBLE, " + 
	                   "avg_mathcorr_best DOUBLE, " + 
	                   "avg_fscore_baseline DOUBLE, " + 
	                   "avg_precision_baseline DOUBLE, " + 
	                   "avg_recall_baseline DOUBLE, " + 
	                   "avg_predacc_baseline DOUBLE, " + 
	                   "avg_mathcorr_baseline DOUBLE, " +
	                   "PRIMARY KEY(heuristic, heuristic_measure, nrOfExamples, noise))";
			stmt.execute(sql);
			
			sql = "CREATE TABLE IF NOT EXISTS eval_detailed (" +
					   "target_query VARCHAR(500)," +
					   "nrOfExamples TINYINT, " +
	                   "noise DOUBLE, " + 
	                   "heuristic VARCHAR(100), " +
	                   "heuristic_measure VARCHAR(100), " +
	                   "query_top VARCHAR(5000), " + 
	                   "fscore_top DOUBLE, " + 
	                   "precision_top DOUBLE, " + 
	                   "recall_top DOUBLE, " + 
	                   "best_query TEXT," +
	                   "best_rank TINYINT, " + 
	                   "best_fscore DOUBLE, " + 
	                   "best_precision DOUBLE, " + 
	                   "best_recall DOUBLE, " + 
	                   "baseline_query TEXT," +
	                   "baseline_fscore DOUBLE, " + 
	                   "baseline_precision DOUBLE, " + 
	                   "baseline_recall DOUBLE, " + 
	                   "PRIMARY KEY(target_query, nrOfExamples, noise, heuristic, heuristic_measure)) ENGINE=MyISAM";
			stmt.execute(sql);
			
			sql = "INSERT INTO eval_overall ("
					+ "heuristic, heuristic_measure, nrOfExamples, noise, "
					+ "avg_fscore_best_returned, avg_precision_best_returned, avg_recall_best_returned,"
					+ "avg_predacc_best_returned, avg_mathcorr_best_returned, "
					+ "avg_position_best, avg_fscore_best, avg_precision_best, avg_recall_best, avg_predacc_best, avg_mathcorr_best,"
					+ "avg_fscore_baseline, avg_precision_baseline, avg_recall_baseline, avg_predacc_baseline, avg_mathcorr_baseline"
					+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			if(override) {
				sql += " ON DUPLICATE KEY UPDATE ";
				sql += "avg_fscore_best_returned = VALUES(avg_fscore_best_returned),";
				sql += "avg_precision_best_returned = VALUES(avg_precision_best_returned),";
				sql += "avg_recall_best_returned = VALUES(avg_recall_best_returned),";
				sql += "avg_predacc_best_returned = VALUES(avg_predacc_best_returned),";
				sql += "avg_mathcorr_best_returned = VALUES(avg_mathcorr_best_returned),";
				sql += "avg_position_best = VALUES(avg_position_best),";
				sql += "avg_fscore_best = VALUES(avg_fscore_best),";
				sql += "avg_precision_best = VALUES(avg_precision_best),";
				sql += "avg_recall_best = VALUES(avg_recall_best),";
				sql += "avg_predacc_best = VALUES(avg_predacc_best),";
				sql += "avg_mathcorr_best = VALUES(avg_mathcorr_best),";
				sql += "avg_fscore_baseline = VALUES(avg_fscore_baseline),";
				sql += "avg_precision_baseline = VALUES(avg_precision_baseline),";
				sql += "avg_recall_baseline = VALUES(avg_recall_baseline),";
				sql += "avg_predacc_baseline = VALUES(avg_predacc_baseline),";
				sql += "avg_mathcorr_baseline = VALUES(avg_mathcorr_baseline)";
			}
			psInsertOverallEval = conn.prepareStatement(sql);
			
			sql = "INSERT INTO eval_detailed ("
					+ "target_query, nrOfExamples, noise, heuristic, heuristic_measure, "
					+ "query_top, fscore_top, precision_top, recall_top,"
					+ "best_query, best_rank, best_fscore, best_precision, best_recall, "
					+ "baseline_query,baseline_fscore, baseline_precision, baseline_recall"
					+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			if(override) {
				sql += " ON DUPLICATE KEY UPDATE ";
				sql += "query_top = VALUES(query_top),";
				sql += "fscore_top = VALUES(fscore_top),";
				sql += "precision_top = VALUES(precision_top),";
				sql += "recall_top = VALUES(recall_top),";
				sql += "best_query = VALUES(best_query),";
				sql += "best_rank = VALUES(best_rank),";
				sql += "best_fscore = VALUES(best_fscore),";
				sql += "best_precision = VALUES(best_precision),";
				sql += "best_recall = VALUES(best_recall),";
				sql += "baseline_query = VALUES(baseline_query),";
				sql += "baseline_fscore = VALUES(baseline_fscore),";
				sql += "baseline_precision = VALUES(baseline_precision),";
				sql += "baseline_recall = VALUES(baseline_recall)";
			}
			psInsertDetailEval = conn.prepareStatement(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int getKBSize() {
		String query = "SELECT (COUNT(*) AS ?cnt) WHERE {[] a ?type . ?type a <http://www.w3.org/2002/07/owl#Class> .}";
		
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		int size = rs.next().get("cnt").asLiteral().getInt();
		
		qe.close();
		
		return size;
	}
	
	private List<String> getSparqlQueries(File queriesFile) throws IOException {
		List<String> sparqlQueries = new ArrayList<String>();
		
		for (String queryString : Files.readLines(queriesFile, Charsets.UTF_8)) {
			Query q = QueryFactory.create(queryString);
			int subjectObjectJoinDepth = QueryUtils.getSubjectObjectJoinDepth(q, q.getProjectVars().get(0));
			if(subjectObjectJoinDepth == 2 && subjectObjectJoinDepth < maxTreeDepth) {
				sparqlQueries.add(queryString);
			}
		}
		return sparqlQueries;
	}
	
	private List<String> filter(List<String> queries, int nrOfQueriesPerDepth) {
		List<String> subset = new ArrayList<>();
		for(int depth = 1; depth <= maxTreeDepth; depth++) {
			List<String> tmp = new ArrayList<>();
			Iterator<String> iterator = queries.iterator();
			while(iterator.hasNext() && tmp.size() < nrOfQueriesPerDepth) {
				String queryString = iterator.next();
				Query q = QueryFactory.create(queryString);
				int subjectObjectJoinDepth = QueryUtils.getSubjectObjectJoinDepth(q, q.getProjectVars().get(0));
				if(subjectObjectJoinDepth < depth) {
					tmp.add(queryString);
				}
			}
			subset.addAll(tmp);
		}
		return subset;
	}

	public void run(File queriesFile, int maxNrOfProcessedQueries, int maxTreeDepth) throws Exception{
		this.maxTreeDepth = maxTreeDepth;
		
		List<String> queries = getSparqlQueries(queriesFile);
		logger.info("#loaded queries: " + queries.size());
		
		queries = filter(queries, maxNrOfProcessedQueries / 3);
//		queries = queries.subList(0, Math.min(queries.size(), maxNrOfProcessedQueries));
		logger.info("#queries to process: " + queries.size());
		
		// generate examples for each query
		final Map<String, ExampleCandidates> query2Examples = new HashMap<>();
		for (String query : queries) {
			query2Examples.put(query, generateExamples(query));
		}
		
		final int totalNrOfQTLRuns = heuristics.length * measures.length * nrOfExamplesIntervals.length * noiseIntervals.length * queries.size();
		logger.info("#QTL runs: " + totalNrOfQTLRuns);

		final AtomicInteger currentNrOfFinishedRuns = new AtomicInteger(0);

		// loop over heuristics
		for(final QueryTreeHeuristic heuristic : heuristics) {
			final String heuristicName = heuristic.getClass().getAnnotation(ComponentAnn.class).shortName();
			
			// loop over heuristics measures
			for (HeuristicType measure : measures) {
				final String measureName = measure.toString();
				heuristic.setHeuristicType(measure);
			
				double[][] data = new double[nrOfExamplesIntervals.length][noiseIntervals.length];
				
				// loop over number of positive examples
				for (int i = 0; i < nrOfExamplesIntervals.length; i++) {
					 final int nrOfExamples = nrOfExamplesIntervals[i];
					 
					// loop over noise value
					for (int j = 0; j < noiseIntervals.length; j++) {
						final double noise = noiseIntervals[j];
						
						// check if not already processed
						File logFile = new File(benchmarkDirectory, "qtl2-" + nrOfExamples + "-" + noise + "-" + heuristicName + "-" + measureName + ".log");
						File statsFile = new File(benchmarkDirectory, "qtl2-" + nrOfExamples + "-" + noise + "-" + heuristicName + "-" + measureName + ".stats");
						
						if(!override && logFile.exists() && statsFile.exists()) {
							logger.info("Eval config already processed. For re-running please remove corresponding output files.");
							continue;
						}
						
						FileAppender appender = null;
						try {
							appender = new FileAppender(new SimpleLayout(), logFile.getPath(), false);
							Logger.getRootLogger().addAppender(appender);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						
						logger.info("#examples: " + nrOfExamples + " noise: " + noise);
						
						final DescriptiveStatistics nrOfReturnedSolutionsStats = new SynchronizedDescriptiveStatistics();
						
						final DescriptiveStatistics baselinePrecisionStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics baselineRecallStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics baselineFMeasureStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics baselinePredAccStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics baselineMathCorrStats = new SynchronizedDescriptiveStatistics();
						
						final DescriptiveStatistics bestReturnedSolutionPrecisionStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestReturnedSolutionRecallStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestReturnedSolutionFMeasureStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestReturnedSolutionPredAccStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestReturnedSolutionMathCorrStats = new SynchronizedDescriptiveStatistics();
						
						final DescriptiveStatistics bestSolutionPrecisionStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestSolutionRecallStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestSolutionFMeasureStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestSolutionPredAccStats = new SynchronizedDescriptiveStatistics();
						final DescriptiveStatistics bestSolutionMathCorrStats = new SynchronizedDescriptiveStatistics();
						
						final DescriptiveStatistics bestSolutionPositionStats = new SynchronizedDescriptiveStatistics();
						
						MonitorFactory.getTimeMonitor(TimeMonitors.CBD_RETRIEVAL.name()).reset();
						MonitorFactory.getTimeMonitor(TimeMonitors.TREE_GENERATION.name()).reset();
						
						ExecutorService tp = Executors.newFixedThreadPool(nrOfThreads);
						
						// indicates if the execution for some of the queries failed
						final AtomicBoolean failed = new AtomicBoolean(false);
						
		//				if(nrOfExamples != 7) continue;
						// loop over SPARQL queries
						for (final String sparqlQuery : queries) {
							
//							if(!sparqlQuery.contains("Band"))continue;
							
							tp.submit(new Runnable(){
	
								@Override
								public void run() {
							
									logger.info("##############################################################");
									logger.info("Processing query\n" + sparqlQuery);
									
									try {
										ExamplesWrapper examples = query2Examples.get(sparqlQuery).get(nrOfExamples, nrOfExamples, noise);
										logger.info("pos. examples:\n" + Joiner.on("\n").join(examples.correctPosExamples));
										logger.info("neg. examples:\n" + Joiner.on("\n").join(examples.correctNegExamples));


										// compute baseline
										logger.info("Computing baseline...");
										RDFResourceTree baselineSolution = applyBaseLine(examples, Baseline.MOST_INFORMATIVE_EDGE_IN_EXAMPLES);
										logger.info("done. \nBaseline solution:\n" + QueryTreeUtils.toOWLClassExpression(baselineSolution));
										logger.info("Evaluating baseline...");
										Score baselineScore = computeScore(sparqlQuery, baselineSolution, noise);
										logger.info("Baseline score:\n" + baselineScore);
										String baseLineQuery = QueryTreeUtils.toSPARQLQueryString(
												baselineSolution, dataset.getBaseIRI(), dataset.getPrefixMapping());
										baselinePrecisionStats.addValue(baselineScore.precision);
										baselineRecallStats.addValue(baselineScore.recall);
										baselineFMeasureStats.addValue(baselineScore.fmeasure);
										baselinePredAccStats.addValue(baselineScore.predAcc);
										baselineMathCorrStats.addValue(baselineScore.mathCorr);
										
										// compute or load cached solutions
										List<EvaluatedRDFResourceTree> solutions = generateSolutions(examples, noise, heuristic);
										nrOfReturnedSolutionsStats.addValue(solutions.size());
										
										// the best returned solution by QTL
										EvaluatedRDFResourceTree bestSolution = solutions.get(0);
										logger.info("Got " + solutions.size() + " query trees.");
										logger.info("Best computed solution:\n" + bestSolution.asEvaluatedDescription());
										logger.info("QTL Score:\n" + bestSolution.getTreeScore());
				
										// convert to SPARQL query
										RDFResourceTree tree = bestSolution.getTree();
				//						filter.filter(tree);
										String learnedSPARQLQuery = QueryTreeUtils.toSPARQLQueryString(
												tree, dataset.getBaseIRI(), dataset.getPrefixMapping());
				
										// compute score
										Score score = computeScore(sparqlQuery, tree, noise);
										bestReturnedSolutionPrecisionStats.addValue(score.precision);
										bestReturnedSolutionRecallStats.addValue(score.recall);
										bestReturnedSolutionFMeasureStats.addValue(score.fmeasure);
										bestReturnedSolutionPredAccStats.addValue(score.predAcc);
										bestReturnedSolutionMathCorrStats.addValue(score.mathCorr);
										logger.info(score);
				
										// find the extensionally best matching tree in the list
										Pair<EvaluatedRDFResourceTree, Score> bestMatchingTreeWithScore = findBestMatchingTreeFast(solutions, sparqlQuery, noise, examples);
										EvaluatedRDFResourceTree bestMatchingTree = bestMatchingTreeWithScore.getFirst();
										Score bestMatchingScore = bestMatchingTreeWithScore.getSecond();
										
										// position of best tree in list of solutions
										int positionBestScore = solutions.indexOf(bestMatchingTree);
										bestSolutionPositionStats.addValue(positionBestScore);
				
										Score bestScore = score;
										if (positionBestScore > 0) {
											logger.info("Position of best covering tree in list: " + positionBestScore);
											logger.info("Best covering solution:\n" + bestMatchingTree.asEvaluatedDescription());
											logger.info("Tree score: " + bestMatchingTree.getTreeScore());
											bestScore = bestMatchingScore;
											logger.info(bestMatchingScore);
										} else {
											logger.info("Best returned solution was also the best covering solution.");
										}
										bestSolutionRecallStats.addValue(bestScore.recall);
										bestSolutionPrecisionStats.addValue(bestScore.precision);
										bestSolutionFMeasureStats.addValue(bestScore.fmeasure);
										bestSolutionPredAccStats.addValue(bestScore.predAcc);
										bestSolutionMathCorrStats.addValue(bestScore.mathCorr);
										
										for ( RDFResourceTree negTree : examples.negExamplesMapping.values()) {
											if(QueryTreeUtils.isSubsumedBy(negTree, bestMatchingTree.getTree())) {
												Files.append(sparqlQuery + "\n", new File("/tmp/negCovered.txt"), Charsets.UTF_8);
												break;
											}
										}

										String bestQuery = QueryFactory.create(QueryTreeUtils.toSPARQLQueryString(
												filter.filter(bestMatchingTree.getTree()), 
												dataset.getBaseIRI(), dataset.getPrefixMapping())).toString();
										
										if(write2DB) {
											write2DB(sparqlQuery, nrOfExamples, examples, noise, 
													baseLineQuery, baselineScore, 
													heuristicName, measureName,
													QueryFactory.create(learnedSPARQLQuery).toString(), score, 
													bestQuery, positionBestScore, bestScore);
										}
				
									} catch (Exception e) {
										failed.set(true);
										logger.error("Error occured.", e);
										try {
											Files.append(sparqlQuery, new File(benchmarkDirectory, "failed-" + nrOfExamples + "-" + noise + "-" + heuristicName + "-" + measureName + ".txt"), Charsets.UTF_8);
										} catch (IOException e1) {
											e1.printStackTrace();
										}
									} finally {
										int cnt = currentNrOfFinishedRuns.incrementAndGet();
										logger.info("***********Evaluation Progress:"
												+ NumberFormat.getPercentInstance().format((double)cnt / totalNrOfQTLRuns)
												+ "(" + cnt + "/" + totalNrOfQTLRuns + ")"
												+ "***********");
									}
								}
						});
						
						}
						
						tp.shutdown();
						tp.awaitTermination(12, TimeUnit.HOURS);
					
						Logger.getRootLogger().removeAppender(appender);
						
						if(!failed.get()) {
							String result = "";
							result += "\nBaseline Precision:\n" + baselinePrecisionStats;
							result += "\nBaseline Recall:\n" + baselineRecallStats;
							result += "\nBaseline F-measure:\n" + baselineFMeasureStats;
							result += "\nBaseline PredAcc:\n" + baselinePredAccStats;
							result += "\nBaseline MathCorr:\n" + baselineMathCorrStats;
							
							result += "#Returned solutions:\n" + nrOfReturnedSolutionsStats;
							
							result += "\nOverall Precision:\n" + bestReturnedSolutionPrecisionStats;
							result += "\nOverall Recall:\n" + bestReturnedSolutionRecallStats;
							result += "\nOverall F-measure:\n" + bestReturnedSolutionFMeasureStats;
							result += "\nOverall PredAcc:\n" + bestReturnedSolutionPredAccStats;
							result += "\nOverall MathCorr:\n" + bestReturnedSolutionMathCorrStats;
							
							result += "\nPositions of best solution:\n" + Arrays.toString(bestSolutionPositionStats.getValues());
							result += "\nPosition of best solution stats:\n" + bestSolutionPositionStats;
							result += "\nOverall Precision of best solution:\n" + bestSolutionPrecisionStats;
							result += "\nOverall Recall of best solution:\n" + bestSolutionRecallStats;
							result += "\nOverall F-measure of best solution:\n" + bestSolutionFMeasureStats;
							
							result += "\nCBD generation time(total):\t" + MonitorFactory.getTimeMonitor(TimeMonitors.CBD_RETRIEVAL.name()).getTotal() + "\n";
							result += "CBD generation time(avg):\t" + MonitorFactory.getTimeMonitor(TimeMonitors.CBD_RETRIEVAL.name()).getAvg() + "\n";
							result += "Tree generation time(total):\t" + MonitorFactory.getTimeMonitor(TimeMonitors.TREE_GENERATION.name()).getTotal() + "\n";
							result += "Tree generation time(avg):\t" + MonitorFactory.getTimeMonitor(TimeMonitors.TREE_GENERATION.name()).getAvg() + "\n";
									
							logger.info(result);
							
							try {
								Files.write(result, statsFile, Charsets.UTF_8);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							data[i][j] = bestReturnedSolutionFMeasureStats.getMean();
							
							if(write2DB) {
								write2DB(heuristicName, measureName, nrOfExamples, noise, 
										bestReturnedSolutionFMeasureStats.getMean(), 
										bestReturnedSolutionPrecisionStats.getMean(), 
										bestReturnedSolutionRecallStats.getMean(),
										bestReturnedSolutionPredAccStats.getMean(),
										bestReturnedSolutionMathCorrStats.getMean(),
										bestSolutionPositionStats.getMean(),
										bestSolutionFMeasureStats.getMean(),
										bestSolutionPrecisionStats.getMean(),
										bestSolutionRecallStats.getMean(),
										bestSolutionPredAccStats.getMean(),
										bestSolutionMathCorrStats.getMean(),
										baselineFMeasureStats.getMean(), 
										baselinePrecisionStats.getMean(), 
										baselineRecallStats.getMean(),
										baselinePredAccStats.getMean(),
										baselineMathCorrStats.getMean()
										);
							}
						}
					}
				}
				
				
				String content = "###";
				String separator = "\t";
				for(int j = 0; j < noiseIntervals.length; j++) {
					content += separator + noiseIntervals[j];
				}
				content += "\n";
				for(int i = 0; i < nrOfExamplesIntervals.length; i++) {
					content += nrOfExamplesIntervals[i];
					for(int j = 0; j < noiseIntervals.length; j++) {
						content += separator + data[i][j];
					}
					content += "\n";
				}
				
				File examplesVsNoise = new File(benchmarkDirectory, "examplesVsNoise-" + heuristicName + "-" + measureName + ".txt");
				try {
					Files.write(content, examplesVsNoise, Charsets.UTF_8);
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
		
		if(useEmailNotification) {
			sendFinishedMail();
		}
	}
	
	private void sendFinishedMail() throws EmailException, IOException {
		Properties config = new Properties();
		config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/dllearner/algorithms/qtl/qtl-mail.properties"));

		Email email = new SimpleEmail();
		email.setHostName(config.getProperty("hostname"));
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(config.getProperty("username"), config.getProperty("password")));
		email.setSSLOnConnect(true);
		email.setFrom(config.getProperty("from"));
		email.setSubject("QTL evaluation finished.");
		email.setMsg("QTL evaluation finished.");
		email.addTo(config.getProperty("to"));
		email.send();
	}

	/*
	 * Compute a baseline solution.
	 * 
	 * From simple to more complex:
	 * 
	 * 1. random type
	 * 2. most popular type in KB
	 * 3. most frequent type in pos. examples
	 * 4. most informative edge, e.g. based on information gain
	 * 5. LGG of all pos. examples
	 * 
	 */
	private RDFResourceTree applyBaseLine(ExamplesWrapper examples, Baseline baselineApproach) {
		Collection<RDFResourceTree> posExamples = examples.posExamplesMapping.values();
		Collection<RDFResourceTree> negExamples = examples.negExamplesMapping.values();
		
		switch (baselineApproach) {
		case RANDOM:// 1.
			String query = "SELECT ?cls WHERE {?cls a owl:Class .} ORDER BY RAND() LIMIT 1";
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			if(rs.hasNext()) {
				QuerySolution qs = rs.next();
				Resource cls = qs.getResource("cls");
				RDFResourceTree solution = new RDFResourceTree();
				solution.addChild(new RDFResourceTree(cls.asNode()), RDF.type.asNode());
				return solution;
			}
		case MOST_POPULAR_TYPE_IN_KB:// 2.
			query = "SELECT ?cls WHERE {?cls a owl:Class . ?s a ?cls .} ORDER BY DESC(COUNT(?s)) LIMIT 1";
			qe = qef.createQueryExecution(query);
			rs = qe.execSelect();
			if(rs.hasNext()) {
				QuerySolution qs = rs.next();
				Resource cls = qs.getResource("cls");
				RDFResourceTree solution = new RDFResourceTree();
				solution.addChild(new RDFResourceTree(cls.asNode()), RDF.type.asNode());
				return solution;
			}
		case MOST_FREQUENT_TYPE_IN_EXAMPLES:// 3.
			Multiset<Node> types = HashMultiset.create();
			for (RDFResourceTree ex : posExamples) {
				List<RDFResourceTree> children = ex.getChildren(RDF.type.asNode());
				for (RDFResourceTree child : children) {
					types.add(child.getData());
				}
			}
			Node mostFrequentType = Ordering.natural().onResultOf(new Function<Multiset.Entry<Node>, Integer>() {
				  public Integer apply(Multiset.Entry<Node> entry) {
				    return entry.getCount();
				  }
				}).max(types.entrySet()).getElement();
			RDFResourceTree solution = new RDFResourceTree();
			solution.addChild(new RDFResourceTree(mostFrequentType), RDF.type.asNode());
			return solution;
		case MOST_FREQUENT_EDGE_IN_EXAMPLES:// 4.
			{Multiset<Pair<Node, Node>> pairs = HashMultiset.create();
			for (RDFResourceTree ex : posExamples) {
				SortedSet<Node> edges = ex.getEdges();
				for (Node edge : edges) {
					List<RDFResourceTree> children = ex.getChildren(edge);
					for (RDFResourceTree child : children) {
						pairs.add(new Pair<Node, Node>(edge, child.getData()));
					}
				}
			}
			Pair<Node, Node> mostFrequentPair = Ordering.natural().onResultOf(new Function<Multiset.Entry<Pair<Node, Node>>, Integer>() {
				  public Integer apply(Multiset.Entry<Pair<Node, Node>> entry) {
				    return entry.getCount();
				  }
				}).max(pairs.entrySet()).getElement();
			solution = new RDFResourceTree();
			solution.addChild(new RDFResourceTree(mostFrequentPair.getValue()), mostFrequentPair.getKey());
			return solution;}
		case MOST_INFORMATIVE_EDGE_IN_EXAMPLES:
			// get all p-o in pos examples
			Multiset<Pair<Node, Node>> edgeObjectPairs = HashMultiset.create();
			for (RDFResourceTree ex : posExamples) {
				SortedSet<Node> edges = ex.getEdges();
				for (Node edge : edges) {
					List<RDFResourceTree> children = ex.getChildren(edge);
					for (RDFResourceTree child : children) {
						edgeObjectPairs.add(new Pair<Node, Node>(edge, child.getData()));
					}
				}
			}
			
			double bestAccuracy = -1;
			solution = new RDFResourceTree();
			
			for (Pair<Node, Node> pair : edgeObjectPairs.elementSet()) {
				Node edge = pair.getKey();
				Node childValue = pair.getValue();
				
				// compute accuracy
				int tp = edgeObjectPairs.count(pair);
				int fn = posExamples.size() - tp;
				int fp = 0;
				for (RDFResourceTree ex : negExamples) { // compute false positives
					List<RDFResourceTree> children = ex.getChildren(edge);
					if(children != null) {
						for (RDFResourceTree child : children) {
							if(child.getData().equals(childValue)) {
								fp++;
								break;
							}
						}
					}
				}
				int tn = negExamples.size() - fp;
				
				double accuracy = Heuristics.getPredictiveAccuracy(
						posExamples.size(), 
						negExamples.size(), 
						tp, 
						tn, 
						1.0);
				// update best solution
				if(accuracy >= bestAccuracy) {
					solution = new RDFResourceTree();
					solution.addChild(new RDFResourceTree(childValue), edge);
					bestAccuracy = accuracy;
				}
			}
			return solution;
		case LGG:
			LGGGenerator lggGenerator = new LGGGeneratorSimple();
			RDFResourceTree lgg = lggGenerator.getLGG(Lists.newArrayList(posExamples));
			return lgg;
		
		default:
			break;
		
		}
		return null;
	}
	
	private List<EvaluatedRDFResourceTree> generateSolutions(ExamplesWrapper examples, double noise, QueryTreeHeuristic heuristic) throws ComponentInitException {
		// run QTL
		PosNegLPStandard lp = new PosNegLPStandard();
		lp.setPositiveExamples(examples.posExamplesMapping.keySet());
		lp.setNegativeExamples(examples.negExamplesMapping.keySet());
//		lp.init();

		QTL2Disjunctive la = new QTL2Disjunctive(lp, qef);
		la.setReasoner(dataset.getReasoner());
		la.setEntailment(Entailment.RDFS);
		la.setTreeFactory(queryTreeFactory);
		la.setPositiveExampleTrees(examples.posExamplesMapping);
		la.setNegativeExampleTrees(examples.negExamplesMapping);
		la.setNoise(noise);
		la.setHeuristic(heuristic);
		la.setMaxExecutionTimeInSeconds(maxExecutionTimeInSeconds);
		la.setMaxTreeComputationTimeInSeconds(maxExecutionTimeInSeconds);
		la.init();
		la.start();

		List<EvaluatedRDFResourceTree> solutions = new ArrayList<EvaluatedRDFResourceTree>(la.getSolutions());
		
		return solutions;
	}
	
	private void solutionsFromCache(String sparqlQuery, int possibleNrOfExamples, double noise) {
		HashFunction hf = Hashing.md5();
		String hash = hf.newHasher()
				.putString(sparqlQuery, Charsets.UTF_8)
				.putInt(possibleNrOfExamples)
				.putDouble(noise)
				.hash().toString();
		File file = new File(cacheDirectory, hash + "-data.ttl");
		if(file.exists()) {
			
		}
	}
	
	private Pair<EvaluatedRDFResourceTree, Score> findBestMatchingTree(Collection<EvaluatedRDFResourceTree> trees,
			String targetSPARQLQuery, double noise) throws Exception {
	logger.info("Finding best matching query tree...");
		//get the tree with the highest fMeasure
		EvaluatedRDFResourceTree bestTree = null;
		Score bestScore = null;
		double bestFMeasure = -1;
		
		for (EvaluatedRDFResourceTree evalutedTree : trees) {
			RDFResourceTree tree = evalutedTree.getTree();
			
			// compute score
			Score score = computeScore(targetSPARQLQuery, tree, noise);
			double fMeasure = score.fmeasure;
			
			// we can stop if f-score is 1
			if(fMeasure == 1.0){
				return new Pair<>(evalutedTree, score);
			}
			
			if(fMeasure > bestFMeasure){
				bestFMeasure = fMeasure;
				bestTree = evalutedTree;
				bestScore = score;
			}
		}
		return new Pair<>(bestTree, bestScore);
	}
	
	/**
	 * find best query tree by searching for the tree which covers
	 * (1) most of the correct positive examples and
	 * (2) none of the noise positive examples
	 * @throws Exception 
	 */
	private Pair<EvaluatedRDFResourceTree, Score> findBestMatchingTreeFast(
			Collection<EvaluatedRDFResourceTree> trees, String targetSPARQLQuery, double noise,
			ExamplesWrapper examples) throws Exception{
		logger.info("Searching for best matching query tree...");
		
		Set<RDFResourceTree> correctPositiveExampleTrees = new HashSet<RDFResourceTree>();
		for (String ex  : examples.correctPosExamples) {
			correctPositiveExampleTrees.add(examples.posExamplesMapping.get(new OWLNamedIndividualImpl(IRI.create(ex))));
		}
		Set<RDFResourceTree> noisyPositiveExampleTrees = new HashSet<RDFResourceTree>();
		for (String ex  : examples.falsePosExamples) {
			noisyPositiveExampleTrees.add(examples.posExamplesMapping.get(new OWLNamedIndividualImpl(IRI.create(ex))));
		}
		
		
		EvaluatedRDFResourceTree bestTree = null;
		int coveredNoiseTreesBest = 0;
		int coveredCorrectTreesBest = 0;
		
		for (EvaluatedRDFResourceTree evaluatedTree : trees) {
			RDFResourceTree tree = evaluatedTree.getTree();
			
			int coveredNoiseTrees = 0;
			for (RDFResourceTree noiseTree : noisyPositiveExampleTrees) {
				if(QueryTreeUtils.isSubsumedBy(noiseTree, tree)) {
					coveredNoiseTrees++;
				}
			}
			
			int coveredCorrectTrees = 0;
			for (RDFResourceTree correctTree : correctPositiveExampleTrees) {
				if(QueryTreeUtils.isSubsumedBy(correctTree, tree)) {
					coveredCorrectTrees++;
				}
			}
			
//			System.err.println("+" + coveredCorrectTrees + "|-" + coveredNoiseTrees);
			// this is obviously the most perfect solution according to the input
			if(coveredNoiseTrees == 0 && coveredCorrectTrees == correctPositiveExampleTrees.size()) {
				bestTree = evaluatedTree;
				break;
			}
			
			if(coveredCorrectTrees > coveredCorrectTreesBest || coveredNoiseTrees < coveredNoiseTreesBest) {
				bestTree = evaluatedTree;
				coveredCorrectTreesBest = coveredCorrectTrees;
				coveredNoiseTreesBest = coveredNoiseTrees;
			} 
		}
		
		// compute score
		String learnedSPARQLQuery = QueryTreeUtils.toSPARQLQueryString(bestTree.getTree(), dataset.getBaseIRI(), dataset.getPrefixMapping());
		System.out.println(learnedSPARQLQuery);
		
		Score score = computeScore(targetSPARQLQuery, bestTree.getTree(), noise);
		return new Pair<>(bestTree, score);
	}
	
	private ExampleCandidates generateExamples(String sparqlQuery) throws Exception{
		logger.info("Generating examples for query ..." + sparqlQuery);
		HashFunction hf = Hashing.md5();

		File examplesDirectory = new File(cacheDirectory, "examples");
		examplesDirectory.mkdirs();
		
		// get all pos. examples, i.e. resources returned by the query 
		List<String> posExamples;
		String hash = hf.newHasher().putString(sparqlQuery, Charsets.UTF_8).hash().toString();
		File file = new File(examplesDirectory, hash + ".tp");
		if(file.exists()) {
			posExamples = Files.readLines(file, Charsets.UTF_8);
		} else {
			posExamples = getResult(sparqlQuery, false);
			Files.write(Joiner.on("\n").join(posExamples), file, Charsets.UTF_8);
		}
		Collections.sort(posExamples);
		logger.info("#Pos. examples: " + posExamples.size());
		
		// get some neg. examples, i.e. resources not returned by the query
		int maxNrOfNegExamples = 100;
		List<String> negExamples;
		hash = hf.newHasher().putString(sparqlQuery, Charsets.UTF_8).putInt(maxNrOfNegExamples).toString();
		file = new File(examplesDirectory,  hash + ".tn");
		if(file.exists()) {
			negExamples = Files.readLines(file, Charsets.UTF_8);
		} else {
			negExamples = new NegativeExampleSPARQLQueryGenerator(qef).getNegativeExamples(sparqlQuery, maxNrOfNegExamples);
			Files.write(Joiner.on("\n").join(negExamples), file, Charsets.UTF_8);
		}
		Collections.sort(negExamples);
		logger.info("#Neg. examples: " + negExamples.size());
		
		// get some noise candidates, i.e. resources used as false pos. examples
		int maxNrOfNoiseCandidates = 100;
		List<String> noiseCandidates;
		hash = hf.newHasher().putString(sparqlQuery, Charsets.UTF_8).putInt(maxNrOfNoiseCandidates).toString();
		file = new File(examplesDirectory, hash + ".fp");
		if(file.exists()) {
			noiseCandidates = Files.readLines(file, Charsets.UTF_8);
		} else {
			noiseCandidates = generateNoiseCandidates(sparqlQuery, noiseMethod, ListUtils.union(posExamples, negExamples), maxNrOfNoiseCandidates);
			Files.write(Joiner.on("\n").join(noiseCandidates), file, Charsets.UTF_8);
		}
		logger.info("#False pos. example candidates: " + noiseCandidates.size());
		
		return new ExampleCandidates(posExamples, negExamples, noiseCandidates);
	}
	
	private RDFResourceTree getSimilarTree(RDFResourceTree tree, String property, int maxTreeDepth){
		String query = "SELECT ?o WHERE {?s <" + property + "> ?o. FILTER(isURI(?o) && ?o != <" + tree.getData() + ">)} LIMIT 1";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		if(rs.hasNext()){
			Resource object = rs.next().getResource("o");
			Model cbd = cbdGen.getConciseBoundedDescription(object.getURI(), maxTreeDepth);
			RDFResourceTree similarTree = queryTreeFactory.getQueryTree(object, cbd);
			similarTree.setData(object.asNode());
			return similarTree;
		}
		return null;
	}
	
	/**
	 * Generates a list of candidates that are not contained in the given set of examples.
	 * @param sparqlQuery
	 * @param posExamples
	 * @param negExamples
	 * @return
	 */
	private List<String> generateNoiseCandidates(String sparqlQuery, NoiseMethod noiseMethod, List<String> examples, int limit) {
		List<String> noiseCandidates = new ArrayList<>();
		
		switch(noiseMethod) {
		case RANDOM: noiseCandidates = generateNoiseCandidatesRandom(examples, limit);
			break;
		case SIMILAR:noiseCandidates = generateNoiseCandidatesSimilar(examples, sparqlQuery, limit);
			break;
		case SIMILARITY_PARAMETERIZED://TODO implement configurable noise method
			break;
		default:noiseCandidates = generateNoiseCandidatesRandom(examples, limit);
			break;
		}
		Collections.sort(noiseCandidates);
		return noiseCandidates;
	}
	
	private Pair<List<String>, List<String>> generateNoise(List<String> examples, String sparqlQuery, double noise, Random randomGen) {
		// generate noise example candidates
		List<String> noiseCandidateExamples = null;
		switch(noiseMethod) {
		case RANDOM: noiseCandidateExamples = generateNoiseCandidatesRandom(examples, 20);
			break;
		case SIMILAR:noiseCandidateExamples = generateNoiseCandidatesSimilar(examples, sparqlQuery, 20);
			break;
		case SIMILARITY_PARAMETERIZED://TODO implement configurable noise method
			break;
		default:noiseCandidateExamples = generateNoiseCandidatesRandom(examples, 20);
			break;
		}
		Collections.shuffle(noiseCandidateExamples, randomGen);

		// add some noise by using instances close to the positive examples
		// we have two ways of adding noise t_n
		// 1: iterate over pos. examples and if random number is below t_n, replace the example
		// 2: replace the (#posExamples * t_n) randomly chosen pos. examples by randomly chosen negative examples
		boolean probabilityBased = false;

		if (probabilityBased) {
			// 1. way
			List<String> newExamples = new ArrayList<String>();
			for (Iterator<String> iterator = examples.iterator(); iterator.hasNext();) {
				String posExample = iterator.next();
				double rnd = randomGen.nextDouble();
				if (rnd <= noise) {
					// remove the positive example
					iterator.remove();
					// add one of the negative examples
					String negExample = noiseCandidateExamples.remove(0);
					newExamples.add(negExample);
					logger.info("Replacing " + posExample + " by " + negExample);
				}
			}
			examples.addAll(newExamples);
			
			return null;
		} else {
			// 2. way
			// replace at least 1 but not more than half of the examples
			int upperBound = examples.size() / 2;
			int nrOfPosExamples2Replace = (int) Math.ceil(noise * examples.size());
			nrOfPosExamples2Replace = Math.min(nrOfPosExamples2Replace, upperBound);
			logger.info("replacing " + nrOfPosExamples2Replace + "/" + examples.size() + " examples to introduce noise");
			List<String> posExamples2Replace = new ArrayList<>(examples.subList(0, nrOfPosExamples2Replace));
			examples.removeAll(posExamples2Replace);
			List<String> negExamples4Replacement = noiseCandidateExamples.subList(0, nrOfPosExamples2Replace);
			List<String> noiseExamples = new ArrayList<String>(negExamples4Replacement);
			List<String> correctExamples = new ArrayList<String>(examples);
			examples.addAll(negExamples4Replacement);
			logger.info("replaced " + posExamples2Replace + " by " + negExamples4Replacement);
			
			return new Pair<>(correctExamples, noiseExamples);
		}
	}
	
	/**
	 * Randomly pick {@code n} instances from KB that do not belong to given set of instances {@code examples}.
	 * @param examples the instances that must not be contained in the returned list
	 * @param n the number of random instances
	 * @return
	 */
	private List<String> generateNoiseCandidatesRandom(List<String> examples, int n) {
		List<String> noiseExampleCandidates = new ArrayList<>();
		
		rnd.reSeed(123);
		// get max number of instances in KB
		String query = "SELECT (COUNT(*) AS ?cnt) WHERE {[] a ?type . ?type a <http://www.w3.org/2002/07/owl#Class> .}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		int max = rs.next().get("cnt").asLiteral().getInt();
		
		// generate random instances
		while(noiseExampleCandidates.size() < n) {
			int offset = rnd.nextInt(0, max);
			query = "SELECT ?s WHERE {?s a ?type . ?type a <http://www.w3.org/2002/07/owl#Class> .} LIMIT 1 OFFSET " + offset;
			
			qe = qef.createQueryExecution(query);
			rs = qe.execSelect();
			
			String resource = rs.next().getResource("s").getURI();
			
			if(!examples.contains(resource) && !resource.contains("__")) {
				noiseExampleCandidates.add(resource);
			}
			qe.close();
		}
		
		return noiseExampleCandidates;
	}
	
	private List<String> generateNoiseCandidatesSimilar(List<String> examples, String queryString, int limit){
		List<String> negExamples = new ArrayList<>();
		
		Query query = QueryFactory.create(queryString);
		
		QueryUtils queryUtils = new QueryUtils();
		
		Set<Triple> triplePatterns = queryUtils.extractTriplePattern(query);
		
		Set<String> negExamplesSet = new TreeSet<String>();
		
		if(triplePatterns.size() == 1){
			Triple tp = triplePatterns.iterator().next();
			Node var = NodeFactory.createVariable("var");
			Triple newTp = Triple.create(tp.getSubject(), tp.getPredicate(), var);
			
			ElementTriplesBlock triplesBlock = new ElementTriplesBlock();
			triplesBlock.addTriple(newTp);
			
			ElementFilter filter = new ElementFilter(new E_NotEquals(new ExprVar(var), NodeValue.makeNode(tp.getObject())));
			
			ElementGroup eg = new ElementGroup();
			eg.addElement(triplesBlock);
			eg.addElementFilter(filter);
			
			Query q = new Query();
			q.setQuerySelectType();
			q.setDistinct(true);
			q.addProjectVars(query.getProjectVars());
			
			q.setQueryPattern(eg);
//			System.out.println(q);
			
			List<String> result = getResult(q.toString());
			negExamplesSet.addAll(result);
		} else {
			// we modify each triple pattern <s p o> by <s p ?var> . ?var != o
			Set<Set<Triple>> powerSet = new TreeSet<>(new Comparator<Set<Triple>>() {

				@Override
				public int compare(Set<Triple> o1, Set<Triple> o2) {
					return ComparisonChain.start().compare(o1.size(), o2.size()).compare(o1.hashCode(), o2.hashCode()).result();
				}
			});
			powerSet.addAll(Sets.powerSet(triplePatterns));
			
			for (Set<Triple> set : powerSet) {
				if(!set.isEmpty() && set.size() != triplePatterns.size()){
					List<Triple> existingTriplePatterns = new ArrayList<>(triplePatterns);
					List<Triple> newTriplePatterns = new ArrayList<>();
					List<ElementFilter> filters = new ArrayList<ElementFilter>();
					int cnt = 0;
					for (Triple tp : set) {
						if(tp.getObject().isURI() || tp.getObject().isLiteral()){
							Node var = NodeFactory.createVariable("var" + cnt++);
							Triple newTp = Triple.create(tp.getSubject(), tp.getPredicate(), var);
							
							existingTriplePatterns.remove(tp);
							newTriplePatterns.add(newTp);
							
							ElementTriplesBlock triplesBlock = new ElementTriplesBlock();
							triplesBlock.addTriple(tp);
							
							ElementGroup eg = new ElementGroup();
							eg.addElement(triplesBlock);
							
							ElementFilter filter = new ElementFilter(new E_NotExists(eg));
							filters.add(filter);
						}
					}
					Query q = new Query();
					q.setQuerySelectType();
					q.setDistinct(true);
					q.addProjectVars(query.getProjectVars());
					List<Triple> allTriplePatterns = new ArrayList<Triple>(existingTriplePatterns);
					allTriplePatterns.addAll(newTriplePatterns);
					ElementTriplesBlock tripleBlock = new ElementTriplesBlock(BasicPattern.wrap(allTriplePatterns));
					ElementGroup eg = new ElementGroup();
					eg.addElement(tripleBlock);
					
					for (ElementFilter filter : filters) {
						eg.addElementFilter(filter);
					}
					
					q.setQueryPattern(eg);
//					System.out.println(q);
					
					List<String> result = getResult(q.toString());
					result.removeAll(examples);
					
					if(result.isEmpty()){
						q = new Query();
						q.setQuerySelectType();
						q.setDistinct(true);
						q.addProjectVars(query.getProjectVars());
						tripleBlock = new ElementTriplesBlock(BasicPattern.wrap(existingTriplePatterns));
						eg = new ElementGroup();
						eg.addElement(tripleBlock);
						
						for (ElementFilter filter : filters) {
							eg.addElementFilter(filter);
						}
						
						q.setQueryPattern(eg);
//						System.out.println(q);
						
						result = getResult(q.toString());
						result.removeAll(examples);
					}
					negExamplesSet.addAll(result);
				}
			}
		}
		
		negExamplesSet.removeAll(examples);
		if(negExamples.isEmpty()){
			logger.error("Found no negative example.");
			System.exit(0);
		}
		negExamples.addAll(negExamplesSet);
		return new ArrayList<>(negExamples).subList(0, Math.min(negExamples.size(), limit));
	}
	
	private List<RDFResourceTree> getQueryTrees(List<String> resources){
		List<RDFResourceTree> trees = new ArrayList<RDFResourceTree>();
		
		for (String resource : resources) {
			trees.add(getQueryTree(resource));
		}
		
		return trees;
	}
	
	private RDFResourceTree getQueryTree(String resource){
		// get CBD
		MonitorFactory.getTimeMonitor(TimeMonitors.CBD_RETRIEVAL.name()).start();
		Model cbd = cbdGen.getConciseBoundedDescription(resource);
		MonitorFactory.getTimeMonitor(TimeMonitors.CBD_RETRIEVAL.name()).stop();
		
		// rewrite NAN to NaN to avoid parse exception
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			cbd.write(baos, "N-TRIPLES", null);
			String modelAsString = new String(baos.toByteArray());
			modelAsString = modelAsString.replace("NAN", "NaN");
			Model newModel = ModelFactory.createDefaultModel();
			newModel.read(new StringReader(modelAsString), null, "TURTLE");
			cbd = newModel;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// generate tree
		MonitorFactory.getTimeMonitor(TimeMonitors.TREE_GENERATION.name()).start();
		RDFResourceTree tree = queryTreeFactory.getQueryTree(resource, cbd);
		MonitorFactory.getTimeMonitor(TimeMonitors.TREE_GENERATION.name()).stop();
		return tree;
	}
	
	private List<String> getResult(String sparqlQuery){
		return getResult(sparqlQuery, true);
	}
	
	private List<String> getResult(String sparqlQuery, boolean useCache){
		logger.trace(sparqlQuery);
		List<String> resources = cache.get(sparqlQuery);
		if(resources == null || !useCache) {
			resources = new ArrayList<String>();
//			sparqlQuery = getPrefixedQuery(sparqlQuery);
			
			// we assume a single projection var
			Query query = QueryFactory.create(sparqlQuery);
			String projectVar = query.getProjectVars().get(0).getName();
			ResultSet rs = qef.createQueryExecution(sparqlQuery).execSelect();
			QuerySolution qs;
			while(rs.hasNext()){
				qs = rs.next();
				
				if(qs.get(projectVar).isURIResource()){
					resources.add(qs.getResource(projectVar).getURI());
				} else if(qs.get(projectVar).isLiteral()){
					resources.add(qs.getLiteral(projectVar).toString());
				}
			}
			cache.put(sparqlQuery, resources);
		}
		
		return resources;
	}
	
	/**
	 * Split the SPARQL query and join the result set of each split. This
	 * allows for the execution of more complex queries.
	 * @param sparqlQuery
	 * @return
	 */
	private List<String> getResultSplitted(String sparqlQuery){
		Query query = QueryFactory.create(sparqlQuery);
		logger.trace("Getting result set for\n" + query);
		
		QueryUtils queryUtils = new QueryUtils();
		Set<Triple> triplePatterns = queryUtils.extractTriplePattern(query);
		
		// remove triple patterns with unbound object vars
		if(triplePatterns.size() > 10) {
			query = removeUnboundObjectVarTriples(query);
			triplePatterns = queryUtils.extractTriplePattern(query);
		} 
		
	//  Virtuoso bug workaround with literals of type xsd:float and xsd:double
		for (Iterator<Triple> iterator = triplePatterns.iterator(); iterator.hasNext();) {
			Node object = iterator.next().getObject();
			if(object.isLiteral() && object.getLiteralDatatype() != null 
					&& (object.getLiteralDatatype().equals(XSDDatatype.XSDfloat) || object.getLiteralDatatype().equals(XSDDatatype.XSDdouble))){
				iterator.remove();
			}
		}
					
		
		Var targetVar = query.getProjectVars().get(0); // should be ?x0
		
		final Multimap<Var, Triple> var2TriplePatterns = HashMultimap.create();
		for (Triple tp : triplePatterns) {
			var2TriplePatterns.put(Var.alloc(tp.getSubject()), tp);
		}
		
		// we keep only the most specific types for each var
		filterOutGeneralTypes(var2TriplePatterns);
		
		// 1. get the outgoing triple patterns of the target var that do not have
		// outgoing triple patterns
		Set<Triple> fixedTriplePatterns = new HashSet<Triple>();
		Set<Set<Triple>> clusters = new HashSet<Set<Triple>>();
		Collection<Triple> targetVarTriplePatterns = var2TriplePatterns.get(targetVar);
		boolean useSplitting = false;
		for (Triple tp : targetVarTriplePatterns) {
			Node object = tp.getObject();
			if(object.isConcrete() || !var2TriplePatterns.containsKey(Var.alloc(object))){
				fixedTriplePatterns.add(tp);
			} else {
				Set<Triple> cluster = new TreeSet<>(new Comparator<Triple>() {
					@Override
					public int compare(Triple o1, Triple o2) {
						return ComparisonChain.start().
						compare(o1.getSubject().toString(), o2.getSubject().toString()).
						compare(o1.getPredicate().toString(), o2.getPredicate().toString()).
						compare(o1.getObject().toString(), o2.getObject().toString()).
						result();
					}
				});
				cluster.add(tp);
				clusters.add(cluster);
				useSplitting = true;
			}
		}
		
		if(!useSplitting){
			clusters.add(Sets.newHashSet(fixedTriplePatterns));
		} else {
			logger.trace("Query too complex. Splitting...");
			// 2. build clusters for other
			for (Set<Triple> cluster : clusters) {
				Triple representative = cluster.iterator().next();
				cluster.addAll(var2TriplePatterns.get(Var.alloc(representative.getObject())));
				cluster.addAll(fixedTriplePatterns);
			}
		}
		
		// again split clusters to have only a maximum number of triple patterns
		int maxNrOfTriplePatternsPerQuery = 20;// number of outgoing triple patterns form the target var in each executed query
		Set<Set<Triple>> newClusters = new HashSet<Set<Triple>>();
		for (Set<Triple> cluster : clusters) {
			int cnt = 0;
			for (Triple triple : cluster) {
				if(triple.getSubject().matches(targetVar)) {
					cnt++;
				}
			}
			
			if(cnt > maxNrOfTriplePatternsPerQuery) {
				Set<Triple> newCluster = new HashSet<Triple>();
				for (Triple triple : cluster) {
					if(triple.getSubject().matches(targetVar)) {
						newCluster.add(triple);
					}
					if(newCluster.size() == maxNrOfTriplePatternsPerQuery) {
						newClusters.add(newCluster);
						newCluster = new HashSet<Triple>();
					}
				}
				if(!newCluster.isEmpty()) {
					newClusters.add(newCluster);
				}
			}
		}
		
		for (Set<Triple> cluster : newClusters) {
			for(int i = 1; i < maxTreeDepth; i++) {
				Set<Triple> additionalTriples = new HashSet<Triple>();
				for (Triple triple : cluster) {
					if(triple.getObject().isVariable()){
						Collection<Triple> triples = var2TriplePatterns.get(Var.alloc(triple.getObject()));
						additionalTriples.addAll(triples);
					}
				}
				cluster.addAll(additionalTriples);
			}
		}
//		clusters = newClusters;
		
		
		
		Set<String> resources = null;
		// 3. run query for each cluster
		for (Set<Triple> cluster : clusters) {
			// remove redundant edges
			SortedSet<Triple> tmp = new TreeSet<Triple>(new Comparator<Triple>() {
				
				TripleComparator comp = new TripleComparator();
				
				@Override
				public int compare(Triple o1, Triple o2) {
					boolean same = o1.subjectMatches(o2.getSubject()) 
							&& o2.predicateMatches(o2.getPredicate()) 
							&& o1.getObject().isVariable() && o2.getObject().isVariable();
//							&& !var2TriplePatterns.containsKey(o1.getObject());
					if(same) return 0;
					return comp.compare(o1, o2);
				}
			});
			tmp.addAll(cluster);
			cluster = tmp;
			
			// build query
			Query q = new Query();
			q.addProjectVars(Collections.singleton(targetVar));
			ElementTriplesBlock el = new ElementTriplesBlock();
			for (Triple triple : cluster) {
				el.addTriple(triple);
			}
			q.setQuerySelectType();
			q.setDistinct(true);
			q.setQueryPattern(el);
			
			q = VirtuosoUtils.rewriteForVirtuosoDateLiteralBug(q);
//			q = rewriteForVirtuosoFloatingPointIssue(q);
			logger.trace(q);
//			sparqlQuery = getPrefixedQuery(sparqlQuery);
			System.out.println(q);
			List<String> partialResult = getResult(q.toString());
			Set<String> resourcesTmp = new HashSet<String>(partialResult);
			
			if(resourcesTmp.isEmpty()) {
				System.err.println("Empty query result");
				System.err.println(q);
//				System.exit(0);
				return Collections.EMPTY_LIST;
			}
			
			if(resources == null){
				resources = resourcesTmp;
			} else {
				resources.retainAll(resourcesTmp);
			}
		}
		
		return new ArrayList<String>(resources);
	}
	
	private Query removeUnboundObjectVarTriples(Query query) {
		QueryUtils queryUtils = new QueryUtils();
		Set<Triple> triplePatterns = queryUtils.extractTriplePattern(query);
		
		Multimap<Var, Triple> var2TriplePatterns = HashMultimap.create();
		for (Triple tp : triplePatterns) {
			var2TriplePatterns.put(Var.alloc(tp.getSubject()), tp);
		}
		
		Iterator<Triple> iterator = triplePatterns.iterator();
		while (iterator.hasNext()) {
			Triple triple = iterator.next();
			Node object = triple.getObject();
			if(object.isVariable() && !var2TriplePatterns.containsKey(Var.alloc(object))) {
				iterator.remove();
			}
		}
		
		Query newQuery = new Query();
		newQuery.addProjectVars(query.getProjectVars());
		ElementTriplesBlock el = new ElementTriplesBlock();
		for (Triple triple : triplePatterns) {
			el.addTriple(triple);
		}
		newQuery.setQuerySelectType();
		newQuery.setDistinct(true);
		newQuery.setQueryPattern(el);
		
		return newQuery;
	}
	
	private void filterOutGeneralTypes(Multimap<Var, Triple> var2Triples) {
		// keep the most specific types for each subject
		for (Var subject : var2Triples.keySet()) {
			Collection<Triple> triplePatterns = var2Triples.get(subject);
			Collection<Triple> triplesPatterns2Remove = new HashSet<Triple>();

			for (Triple tp : triplePatterns) {
				if (tp.getObject().isURI() && !triplesPatterns2Remove.contains(tp)) {
					// get all super classes for the triple object
					Set<Node> superClasses = getSuperClasses(tp.getObject());

					// remove triple patterns that have one of the super classes as object
					for (Triple tp2 : triplePatterns) {
						if(tp2 != tp && superClasses.contains(tp2.getObject())) {
							triplesPatterns2Remove.add(tp2);
						}
					}
				}
			}
			
			// remove triple patterns
			triplePatterns.removeAll(triplesPatterns2Remove);
		}
	}
	
	private Set<Node> getSuperClasses(Node cls){
		Set<Node> superClasses = new HashSet<Node>();
		
		superClassesQueryTemplate.setIri("sub", cls.getURI());
		
		String query = superClassesQueryTemplate.toString();
		
		try {
			QueryExecution qe = qef.createQueryExecution(query);
			ResultSet rs = qe.execSelect();
			while(rs.hasNext()){
				QuerySolution qs = rs.next();
				superClasses.add(qs.getResource("sup").asNode());
			}
			qe.close();
		} catch (Exception e) {
			System.out.println(query);
			throw e;
		}
		
		return superClasses;
	}
	
	private int getResultCount(String sparqlQuery){
		sparqlQuery = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " + sparqlQuery;
		int cnt = 0;
		QueryExecution qe = qef.createQueryExecution(sparqlQuery);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()){
			rs.next();
			cnt++;
		}
		qe.close();
		return cnt;
	}
	
	
	private Query rewriteForVirtuosoFloatingPointIssue(Query query){
		QueryUtils queryUtils = new QueryUtils();
		Set<Triple> triplePatterns = queryUtils.extractTriplePattern(query);
		
		Set<Triple> newTriplePatterns = new TreeSet<>(new Comparator<Triple>() {
			@Override
			public int compare(Triple o1, Triple o2) {
				return ComparisonChain.start().
				compare(o1.getSubject().toString(), o2.getSubject().toString()).
				compare(o1.getPredicate().toString(), o2.getPredicate().toString()).
				compare(o1.getObject().toString(), o2.getObject().toString()).
				result();
			}
		});
		List<ElementFilter> filters = new ArrayList<>();
		int cnt = 0;
		// <s p o>
		for (Iterator<Triple> iter = triplePatterns.iterator(); iter.hasNext();) {
			Triple tp = iter.next();
			if(tp.getObject().isLiteral()){
				RDFDatatype dt = tp.getObject().getLiteralDatatype();
				if(dt != null && (dt.equals(XSDDatatype.XSDfloat) || dt.equals(XSDDatatype.XSDdouble))){
					iter.remove();
					// new triple pattern <s p ?var> 
					Node objectVar = NodeFactory.createVariable("floatVal" + cnt++);
					newTriplePatterns.add(Triple.create(
							tp.getSubject(), 
							tp.getPredicate(),
							objectVar)
							);
					// add FILTER(STR(?var) = lexicalform(o))
					System.out.println(tp.getObject());
					double epsilon = 0.00001;
					Expr filterExpr = new E_LessThanOrEqual(
							new E_NumAbs(
									new E_Subtract(
											new ExprVar(objectVar), 
											NodeValue.makeNode(tp.getObject())
											)
									),
									NodeValue.makeDouble(epsilon)
							);
					ElementFilter filter = new ElementFilter(filterExpr);
					filters.add(filter);
				}
			}
		}
		
		newTriplePatterns.addAll(triplePatterns);
		
		Query q = new Query();
		q.addProjectVars(query.getProjectVars());
		ElementTriplesBlock tripleBlock = new ElementTriplesBlock();
		for (Triple triple : newTriplePatterns) {
			tripleBlock.addTriple(triple);
		}
		ElementGroup eg = new ElementGroup();
		eg.addElement(tripleBlock);
		for (ElementFilter filter : filters) {
			eg.addElementFilter(filter);
		}
		q.setQuerySelectType();
		q.setDistinct(true);
		q.setQueryPattern(eg);
		
		return q;
	}
	

	
	private Score computeScore(String referenceSparqlQuery, RDFResourceTree tree, double noise) throws Exception{
		// apply some filters
		QueryTreeUtils.removeVarLeafs(tree);
		QueryTreeUtils.prune(tree, null, Entailment.RDF);
		
		String learnedSPARQLQuery = QueryTreeUtils.toSPARQLQueryString(tree, dataset.getBaseIRI(), dataset.getPrefixMapping());
		
		if(QueryUtils.getTriplePatterns(QueryFactory.create(learnedSPARQLQuery)).size() < 25) {
			return computeScoreBySparqlCount(referenceSparqlQuery, tree, noise);
		}
		
		// get the reference resources
		List<String> referenceResources = getResult(referenceSparqlQuery);
		if (referenceResources.isEmpty()) {
			logger.error("Reference SPARQL query returns no result.\n" + referenceSparqlQuery);
			return new Score();
		}

		// if query is most general one P=|TARGET|/|KB| R=1
		if (learnedSPARQLQuery.equals(QueryTreeUtils.EMPTY_QUERY_TREE_QUERY)) {
			
			int tp = referenceResources.size();
			int fp = kbSize - tp;
			int tn = 0;
			int fn = 0;
			
			return score(tp, fp, tn, fn);
		}

		// get the learned resources
		List<String> learnedResources = splitComplexQueries ? getResultSplitted(learnedSPARQLQuery) : getResult(learnedSPARQLQuery);
		if (learnedResources.isEmpty()) {
			logger.error("Learned SPARQL query returns no result.\n" + learnedSPARQLQuery);
			return new Score();
		}

		// get the overlapping resources
		int overlap = Sets.intersection(Sets.newHashSet(referenceResources), Sets.newHashSet(learnedResources)).size();

		int tp = overlap;
		int fp = Sets.difference(Sets.newHashSet(learnedResources), Sets.newHashSet(referenceResources)).size();
		int fn = Sets.difference(Sets.newHashSet(referenceResources), Sets.newHashSet(learnedResources)).size();
		int tn = kbSize - tp - fp - fn;
		
		return score(tp, fp, tn, fn);
	}
	
	private Score computeScoreBySparqlCount(String referenceSparqlQuery, RDFResourceTree tree, double noise) throws Exception{
		
		String learnedSPARQLQuery = QueryTreeUtils.toSPARQLQueryString(tree, dataset.getBaseIRI(), dataset.getPrefixMapping());
		
		final ExprVar s = new ExprVar("s");
		Var cntVar = Var.alloc("cnt");
		
		// Q1
		Query q1 = QueryFactory.create(referenceSparqlQuery);
		Query q1Count = QueryFactory.create();
		q1Count.setQuerySelectType();
		q1Count.getProject().add(cntVar, new ExprAggregator(s.asVar(), new AggCountVarDistinct(s)));
		q1Count.setQueryPattern(q1.getQueryPattern());
		QueryExecution qe = qef.createQueryExecution(q1Count);
		ResultSet rs = qe.execSelect();
		QuerySolution qs = rs.next();
		int referenceCnt = qs.getLiteral(cntVar.getName()).getInt();
		qe.close();
				
		// if query is most general one P=|TARGET|/|KB| R=1
		if (learnedSPARQLQuery.equals(QueryTreeUtils.EMPTY_QUERY_TREE_QUERY)) {
			
			int tp = referenceCnt;
			int fp = kbSize - tp;
			int tn = 0;
			int fn = 0;
			
			return score(tp, fp, tn, fn);
		}
				
		// Q2
		Query q2 = QueryFactory.create(learnedSPARQLQuery);
		Query q2Count = QueryFactory.create();
		q2Count.setQuerySelectType();
		q2Count.getProject().add(cntVar, new ExprAggregator(s.asVar(), new AggCountVarDistinct(s)));
		q2Count.setQueryPattern(q2.getQueryPattern());
		logger.info("Learned query:\n" + q2Count);
		q2Count = VirtuosoUtils.rewriteForVirtuosoDateLiteralBug(q2Count);
		qe = qef.createQueryExecution(q2Count);
		rs = qe.execSelect();
		qs = rs.next();
		int learnedCnt = qs.getLiteral(cntVar.getName()).getInt();
		qe.close();
		
		
		// Q1 ∪ Q2
		// if noise = 0 then Q1 ∪ Q2 = Q2
		int overlap = Math.min(learnedCnt, referenceCnt);
		if(noise > 0) {
			Query q12 = QueryFactory.create();
			q12.setQuerySelectType();
			q12.getProject().add(cntVar, new ExprAggregator(s.asVar(), new AggCountVarDistinct(s)));
			ElementGroup whereClause = new ElementGroup();
			for (Element el : ((ElementGroup)q1.getQueryPattern()).getElements()) {
				whereClause.addElement(el);
			}
			for (Element el : ((ElementGroup)q2.getQueryPattern()).getElements()) {
				whereClause.addElement(el);
			}
			q12.setQueryPattern(whereClause);
			logger.info("Combined query:\n" + q12);
			q12 = VirtuosoUtils.rewriteForVirtuosoDateLiteralBug(q12);
			qe = qef.createQueryExecution(q12);
			rs = qe.execSelect();
			qs = rs.next();
			overlap = qs.getLiteral(cntVar.getName()).getInt();
			qe.close();
		}
		
		int tp = overlap;
		int fp = learnedCnt - overlap;
		int fn = referenceCnt - overlap;
		int tn = kbSize - tp - fp - fn;
		
		return score(tp, fp, tn, fn);
	}
	
	private Score score(int tp, int fp, int tn, int fn) throws Exception {
		// P
		double precision = (tp == 0 && fp == 0) ? 1.0 : (double) tp / (tp + fp);
		
		// R
		double recall = (tp == 0 && fn == 0) ? 1.0 : (double) tp / (tp + fn);
		
		//F_1
		double fMeasure = Heuristics.getFScore(recall, precision);
		
		// pred. acc
		double predAcc = (tp + tn) / (double)((tp + fn) + (tn + fp));
		BigDecimal denominator = BigDecimal.valueOf(tp + fp).
				multiply(BigDecimal.valueOf(tp + fn)).
				multiply(BigDecimal.valueOf(tn + fp)).
				multiply(BigDecimal.valueOf(tn + fn));
		
		// Mathews CC
		double mathCorr = denominator.doubleValue() == 0 ? 0 : (tp * tn - fp * fn) / Math.sqrt(denominator.doubleValue());
		
		if(Double.isNaN(predAcc) || Double.isNaN(mathCorr)){
			throw new Exception(Double.isNaN(predAcc) ? ("PredAcc") : ("MC") + " not a number.");
		}
		
		return new Score(precision, recall, fMeasure, predAcc, mathCorr);
	}
	
	private void write2DB(String targetQuery, int nrOfExamples, ExamplesWrapper examples,
			double noise, String baseLineQuery, Score baselineScore, String heuristicName, String heuristicMeasure,
			String returnedQuery, Score returnedQueryScore, String bestQuery, int bestQueryPosition,
			Score bestQueryScore) {
		logger.trace("Writing to DB...");
		try {
			psInsertDetailEval.setString(1, targetQuery);
			psInsertDetailEval.setInt(2, nrOfExamples);
			psInsertDetailEval.setDouble(3, noise);
			psInsertDetailEval.setString(4, heuristicName);
			psInsertDetailEval.setString(5, heuristicMeasure);
			
			psInsertDetailEval.setString(6, returnedQuery);
			psInsertDetailEval.setDouble(7, returnedQueryScore.fmeasure);
			psInsertDetailEval.setDouble(8, returnedQueryScore.precision);
			psInsertDetailEval.setDouble(9, returnedQueryScore.recall);
			
			psInsertDetailEval.setString(10, bestQuery);
			psInsertDetailEval.setInt(11, bestQueryPosition);
			psInsertDetailEval.setDouble(12, bestQueryScore.fmeasure);
			psInsertDetailEval.setDouble(13, bestQueryScore.precision);
			psInsertDetailEval.setDouble(14, bestQueryScore.recall);
			
			psInsertDetailEval.setString(15, baseLineQuery);
			psInsertDetailEval.setDouble(16, baselineScore.fmeasure);
			psInsertDetailEval.setDouble(17, baselineScore.precision);
			psInsertDetailEval.setDouble(18, baselineScore.recall);

			logger.trace(psInsertDetailEval);
			psInsertDetailEval.executeUpdate();
			logger.trace("...finished writing to DB.");
		} catch (Exception e) {
			logger.error("Writing to DB failed with " + psInsertDetailEval, e);
		}
		
	}
	
	private synchronized void write2DB(
			String heuristic, String heuristicMeasure, int nrOfExamples, double noise, 
			double fmeasure, double precision, double recall, double predAcc, double mathCorr,
			double bestSolutionPosition, double bestSolutionFmeasure, double bestSolutionPrecision, double bestSolutionRecall, double bestSolutionPredAcc, double bestSolutionMathCorr,
			double baselineFmeasure, double baselinePrecision, double baselineRecall, double baselinePredAcc, double baselineMathCorr
			) {
		logger.trace("Writing to DB...");
		try {
			psInsertOverallEval.setString(1, heuristic);
			psInsertOverallEval.setString(2, heuristicMeasure);
			psInsertOverallEval.setInt(3, nrOfExamples);
			psInsertOverallEval.setDouble(4, noise);
			
			psInsertOverallEval.setDouble(5, fmeasure);
			psInsertOverallEval.setDouble(6, precision);
			psInsertOverallEval.setDouble(7, recall);
			psInsertOverallEval.setDouble(8, predAcc);
			psInsertOverallEval.setDouble(9, mathCorr);
			
			psInsertOverallEval.setDouble(10, bestSolutionPosition);
			psInsertOverallEval.setDouble(11, bestSolutionFmeasure);
			psInsertOverallEval.setDouble(12, bestSolutionPrecision);
			psInsertOverallEval.setDouble(13, bestSolutionRecall);
			psInsertOverallEval.setDouble(14, bestSolutionPredAcc);
			psInsertOverallEval.setDouble(15, bestSolutionMathCorr);
			
			psInsertOverallEval.setDouble(16, baselineFmeasure);
			psInsertOverallEval.setDouble(17, baselinePrecision);
			psInsertOverallEval.setDouble(18, baselineRecall);
			psInsertOverallEval.setDouble(19, baselinePredAcc);
			psInsertOverallEval.setDouble(20, baselineMathCorr);
			
			logger.trace(psInsertOverallEval);
			psInsertOverallEval.executeUpdate();
			logger.trace("...finished writing to DB.");
		} catch (Exception e) {
			logger.error("Writing to DB failed with " + psInsertOverallEval, e);
		}
	}
	
	public static void main(String[] args) throws Exception {
		StringRenderer.setRenderer(Rendering.DL_SYNTAX);
		Logger.getLogger(QTLEvaluation.class).addAppender(
				new FileAppender(new SimpleLayout(), "log/qtl-qald.log", false));
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(QTL2Disjunctive.class).setLevel(Level.INFO);
		Logger.getLogger(QTLEvaluation.class).setLevel(Level.INFO);
		Logger.getLogger(QueryExecutionFactoryCacheEx.class).setLevel(Level.INFO);
		
		OptionParser parser = new OptionParser();
		OptionSpec<File> benchmarkDirectorySpec = parser.accepts("d", "base directory").withRequiredArg().ofType(File.class).required();
		OptionSpec<File> queriesFileSpec = parser.accepts("q", "processed queries file").withRequiredArg().ofType(File.class).required();
		OptionSpec<URL> endpointURLSpec = parser.accepts("e", "endpoint URL").withRequiredArg().ofType(URL.class).required();
		OptionSpec<String> defaultGraphSpec = parser.accepts("g", "default graph").withRequiredArg().ofType(String.class);
		OptionSpec<Boolean> overrideSpec = parser.accepts("o", "override previous results").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.FALSE);
		OptionSpec<Boolean> write2DBSpec = parser.accepts("db", "write to database").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.FALSE);
		OptionSpec<Boolean> emailNotificationSpec = parser.accepts("mail", "enable email notification").withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.FALSE);
		OptionSpec<Integer> maxNrOfQueriesSpec = parser.accepts("max-queries", "max. nr. of process queries").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		OptionSpec<Integer> maxTreeDepthSpec = parser.accepts("max-tree-depth", "max. depth of processed queries and generated trees").withRequiredArg().ofType(Integer.class).defaultsTo(3);
		OptionSpec<Integer> maxQTLRuntimeSpec = parser.accepts("max-qtl-runtime", "max. runtime of each QTL run").withRequiredArg().ofType(Integer.class).defaultsTo(10).required();
		OptionSpec<Integer> nrOfThreadsSpec = parser.accepts("thread-count", "number of threads used for parallel evaluation").withRequiredArg().ofType(Integer.class).defaultsTo(1);

		

        OptionSet options = parser.parse(args);

		File benchmarkDirectory = options.valueOf(benchmarkDirectorySpec);
		File queriesFile = options.valueOf(queriesFileSpec);
		boolean write2DB = options.valueOf(write2DBSpec);
		boolean override = options.valueOf(overrideSpec);
		boolean useEmailNotification = options.valueOf(emailNotificationSpec);
		URL endpointURL = options.valueOf(endpointURLSpec);
		String defaultGraph = options.has(defaultGraphSpec) ? options.valueOf(defaultGraphSpec) : null;
		SparqlEndpoint endpoint = SparqlEndpoint.create(endpointURL.toString(), defaultGraph);
		int maxNrOfQueries = options.valueOf(maxNrOfQueriesSpec);
		int maxTreeDepth = options.valueOf(maxTreeDepthSpec);
		int maxQTLRuntime = options.valueOf(maxQTLRuntimeSpec);
		int nrOfThreads = options.valueOf(nrOfThreadsSpec);

		new QTLEvaluation(new DBpediaEvaluationDataset(benchmarkDirectory, endpoint), benchmarkDirectory, write2DB, override, maxQTLRuntime, useEmailNotification, nrOfThreads).run(queriesFile, maxNrOfQueries, maxTreeDepth);

//		new QALDExperiment(Dataset.BIOMEDICAL).run();
	}
	
	class Score {
		int tp, fp, tn, fn = 0;
		double precision, recall, fmeasure, predAcc, mathCorr = 0;

		public Score() {}
		
		public Score(double precision, double recall, double fmeasure, double predAcc, double mathCorr) {
			this.precision = precision;
			this.recall = recall;
			this.fmeasure = fmeasure;
			this.predAcc = predAcc;
			this.mathCorr = mathCorr;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("P=%f\nR=%f\nF-score=%f\nPredAcc=%f\nMC=%f", precision, recall, fmeasure, predAcc, mathCorr);
		}
		
	}
	

	
	class ExampleCandidates {

		List<String> correctPosExampleCandidates;
		List<String> falsePosExampleCandidates;
		List<String> correctNegExampleCandidates;
		
		public ExampleCandidates(List<String> correctPosExampleCandidates,
				List<String> correctNegExampleCandidates,
				List<String> falsePosExampleCandidates) {
			this.correctPosExampleCandidates = correctPosExampleCandidates;
			this.falsePosExampleCandidates = falsePosExampleCandidates;
			this.correctNegExampleCandidates = correctNegExampleCandidates;
		}
		
		public ExamplesWrapper get(int nrOfPosExamples, int nrOfNegExamples, double noise) {
			Random rnd = new Random(123);
			
			// random sublist of the pos. examples
			List<String> correctPosExamples = new ArrayList<>(correctPosExampleCandidates);
			Collections.sort(correctPosExamples);
			Collections.shuffle(correctPosExamples, rnd);
			correctPosExamples = new ArrayList<>(correctPosExamples.subList(0, Math.min(correctPosExamples.size(), nrOfPosExamples)));
			
			// random sublist of the neg. examples
			List<String> negExamples = new ArrayList<>(correctNegExampleCandidates);
			Collections.sort(negExamples);
			Collections.shuffle(negExamples, rnd);
			negExamples = new ArrayList<>(negExamples.subList(0, Math.min(negExamples.size(), nrOfNegExamples)));
			

			List<String> falsePosExamples = new ArrayList<>();
			if(noise > 0) {
				// randomly replace some of the pos. examples by false examples
				// 2 options
				// 1: iterate over pos. examples and if random number is below t_n, replace the example
				// 2: replace the (#posExamples * t_n) randomly chosen pos. examples by randomly chosen negative examples
				List<String> falsePosExampleCandidates = new ArrayList<>(this.falsePosExampleCandidates);
				Collections.sort(falsePosExampleCandidates);
				Collections.shuffle(falsePosExampleCandidates, rnd);



				boolean probabilityBased = false;

				if (probabilityBased) {
					// 1. way
					for (Iterator<String> iterator = correctPosExamples.iterator(); iterator.hasNext();) {
						String posExample = iterator.next();
						double rndVal = rnd.nextDouble();
						if (rndVal <= noise) {
							// remove the positive example
							iterator.remove();

							// add one of the negative examples
							String falsePosExample = falsePosExampleCandidates.remove(0);
							falsePosExamples.add(falsePosExample);
							logger.info("Replacing " + posExample + " by " + falsePosExample);
						}
					}
				} else {
					// 2. way
					// replace at least 1 but not more than half of the examples
					int upperBound = correctPosExamples.size() / 2;
					int nrOfPosExamples2Replace = Math.min((int) Math.ceil(noise * correctPosExamples.size()), upperBound);

					logger.info("replacing " + nrOfPosExamples2Replace + "/" + correctPosExamples.size() + " examples to introduce noise");
					List<String> posExamples2Replace = new ArrayList<>(correctPosExamples.subList(0, nrOfPosExamples2Replace));
					correctPosExamples.removeAll(posExamples2Replace);

					falsePosExamples = falsePosExampleCandidates.subList(0, nrOfPosExamples2Replace);
					logger.info("replaced " + posExamples2Replace + "\nby\n" + falsePosExamples);
				}
			}
			
			// ensure determinism
			Collections.sort(correctPosExamples);
			Collections.sort(negExamples);
			Collections.sort(falsePosExamples);
			
			// generate trees
			SortedMap<OWLIndividual, RDFResourceTree> posExamplesMapping = new TreeMap<>();
			for (String ex : ListUtils.union(correctPosExamples, falsePosExamples)) {
				posExamplesMapping.put(new OWLNamedIndividualImpl(IRI.create(ex)), getQueryTree(ex));
			}
			SortedMap<OWLIndividual, RDFResourceTree> negExamplesMapping = new TreeMap<>();
			for (String ex : negExamples) {
				negExamplesMapping.put(new OWLNamedIndividualImpl(IRI.create(ex)), getQueryTree(ex));
			}
			
			return new ExamplesWrapper(correctPosExamples, falsePosExamples, negExamples, posExamplesMapping, negExamplesMapping);
		}
	}
}
