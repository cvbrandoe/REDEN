package fr.ign.georeden.talismane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.AStarShortestPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

import fr.ign.georeden.graph.LabeledEdge;
import fr.ign.georeden.utils.GraphVisualisation;

public class TalismaneManager {

	private final List<String> lines;

	public TalismaneManager(String fileName) throws IOException {
		Path filePath = Paths.get(fileName);
		lines = new ArrayList<>();
		Files.lines(filePath).forEach(lines::add);
	}

	public List<List<String>> cutIntoSentences() {
		List<List<String>> sentences = new ArrayList<>();
		List<String> currentSentence = new ArrayList<>();
		for (String line : lines) {
			if ("".equals(line)) {
				sentences.add(new ArrayList<>(currentSentence));
				currentSentence.clear();
			} else {
				currentSentence.add(line);
			}
		}
		return sentences;
	}

	public SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> getGraph(List<String> sentence) {
		@SuppressWarnings("unchecked")
		SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> graph = new SimpleDirectedGraph<>(
				(Class<? extends LabeledEdge<TalismaneVertex, String>>) LabeledEdge.class);
		graph.addVertex(new TalismaneVertex("0", "root", ""));

		Map<String[], String> edgesToAdd = new HashMap<>();
		for (String line : sentence) {
			String[] splitedLine = line.split("\t");
			TalismaneVertex v = new TalismaneVertex(splitedLine[0], splitedLine[1], splitedLine[3]);
			graph.addVertex(v);
			String[] ids = new String[2];
			ids[0] = splitedLine[0]; // neud actuel
			ids[1] = splitedLine[splitedLine.length - 2]; // neud cible
			edgesToAdd.put(ids, splitedLine[splitedLine.length - 1]);
		}
		for (Entry<String[], String> entry : edgesToAdd.entrySet()) {
			String[] ids = entry.getKey();
			Optional<TalismaneVertex> ov1 = graph.vertexSet().stream().filter(v -> v.getId().equals(ids[0]))
					.findFirst();
			Optional<TalismaneVertex> ov2 = graph.vertexSet().stream().filter(v -> v.getId().equals(ids[1]))
					.findFirst();
			if (!ov1.isPresent() || !ov2.isPresent())
				continue;
			TalismaneVertex v1 = ov1.get();
			TalismaneVertex v2 = ov2.get();
			String edge = entry.getValue();
			graph.addEdge(v1, v2, new LabeledEdge<TalismaneVertex, String>(v1, v2, edge));
		}
		return graph;
	}

	/**
	 * Filter graph by keeping only path between toponyms and orientations.
	 *
	 * @param graph
	 *            the graph
	 * @return the simple directed graph
	 */
	public SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> filterGraph(
			SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> graph) {
		List<LabeledEdge<TalismaneVertex, String>> edgesToremove = new ArrayList<>();
		List<TalismaneVertex> leavesToRemove = new ArrayList<>();
		// on selectionne les feuilles à supprimer
		graph.vertexSet().stream().filter(v -> graph.inDegreeOf(v) == 0 && !v.getType().equals("NPP")
				 && !v.getLabel().contains("Nord") && !v.getLabel().contains("Sud") && !v.getLabel().contains("Ouest") && !v.getLabel().contains("Est"))
				.forEach(leavesToRemove::add);
		// S'il n'y a pas de feuilles à supprimer on arrête là
		if (leavesToRemove.isEmpty())
			return graph;
		@SuppressWarnings("unchecked")
		SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> newGraph = new SimpleDirectedGraph<>(
				(Class<? extends LabeledEdge<TalismaneVertex, String>>) LabeledEdge.class);
		// on créer un nouveau graphe avec sans les feuilles à supprimer et les
		// arretes correspondantes.
		graph.vertexSet().stream().filter(v -> !leavesToRemove.contains(v)).forEach(newGraph::addVertex);
		graph.edgeSet().stream().filter(e -> !leavesToRemove.contains(e.getVertex1()))
				.forEach(e -> newGraph.addEdge(e.getVertex1(), e.getVertex2(),
						new LabeledEdge<TalismaneVertex, String>(e.getVertex1(), e.getVertex2(), e.toString())));
		return filterGraph(newGraph);
	}

	public void displayGraph() {
		List<List<String>> sentences = cutIntoSentences();
		List<List<String>> selectedSentences = new ArrayList<>();
		sentences.stream()
				.filter(s -> (s.stream()
						.anyMatch(l -> l.contains("Nord") || l.contains("Sud") || l.contains("Ouest")
								|| l.contains("Est")))
						&& s.stream().filter(l2 -> l2.contains("NPP")).count() >= 3)
				.forEach(list -> selectedSentences.add(list));

		int i = 0;
		for (List<String> firstSentence : selectedSentences) {
			SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> graph = getGraph(firstSentence);
			graph = filterGraph(graph);
//			List<GraphPath<TalismaneVertex, LabeledEdge<TalismaneVertex, String>>> paths = getShortestPath(graph);
			Object[] roots = new Object[1];
			roots[0] = graph.vertexSet().stream().filter(v -> "0".equals(v.getId())).findFirst().get();
			GraphVisualisation<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> window = new GraphVisualisation<>(
					graph, roots, "Graphe " + i);
			window.init(1024, 768);
			i++;
		}
	}

	public List<GraphPath<TalismaneVertex, LabeledEdge<TalismaneVertex, String>>> getShortestPath(
			SimpleDirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> directedGraph) {
		AsUndirectedGraph<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> graph = new AsUndirectedGraph<>(directedGraph);
		AStarShortestPath<TalismaneVertex, LabeledEdge<TalismaneVertex, String>> aStarShortestPath = new AStarShortestPath<>(
				graph);
		List<TalismaneVertex> orientations = new ArrayList<>();
		List<TalismaneVertex> toponyms = new ArrayList<>();
		graph.vertexSet().stream()
				.filter(v -> v.getLabel().contains("Nord") || v.getLabel().contains("Sud")
						|| v.getLabel().contains("Ouest") || v.getLabel().contains("Est"))
				.forEach(v -> orientations.add(v));
		graph.vertexSet().stream().filter(v -> v.getType().equals("NPP") && !orientations.contains(v))
				.forEach(v -> toponyms.add(v));
		AStarAdmissibleHeuristic<TalismaneVertex> heuristic = new AStarAdmissibleHeuristic<TalismaneVertex>() {

			@Override
			public double getCostEstimate(TalismaneVertex arg0, TalismaneVertex arg1) {
				// TODO Auto-generated method stub
				return Math.abs(Integer.parseInt(arg0.getId()) - Integer.parseInt(arg1.getId())); // 0
			}
		};
		List<GraphPath<TalismaneVertex, LabeledEdge<TalismaneVertex, String>>> paths = new ArrayList<>();
		for (TalismaneVertex orientation : orientations) {
			for (TalismaneVertex toponym : toponyms) {
				paths.add(aStarShortestPath.getShortestPath(orientation, toponym, heuristic));
			}
		}
		return paths;
	}
}
