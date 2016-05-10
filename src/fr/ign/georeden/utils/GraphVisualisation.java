package fr.ign.georeden.utils;

import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.jgraph.JGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.organic.JGraphSelfOrganizingOrganicLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

/**
 * Permet de visualiser le graphe dans une fenêtre.
 */
public class GraphVisualisation<V, E> extends JFrame {
	
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	public GraphVisualisation(SimpleDirectedGraph<V, E> graph) {
		super("Graphe du fichier");

		JGraph jgraph = new JGraph(new JGraphModelAdapter<V, E>(graph));
		JGraphFacade facade = new JGraphFacade(jgraph);
		JGraphSelfOrganizingOrganicLayout layout = new JGraphSelfOrganizingOrganicLayout();
		layout.setDensityFactor(50000); // 0
		layout.setMaxIterationsMultiple(10000); // 20
		layout.setMinRadius(50); // 1
		layout.run(facade);
		final Map nestedMap = facade.createNestedMap(true, true);
		jgraph.getGraphLayoutCache().edit(nestedMap);

		JPanel container = new JPanel();
		JScrollPane scrPane = new JScrollPane(container);
		add(scrPane);
		container.add(jgraph);
	}
	
	public GraphVisualisation(SimpleDirectedGraph<V, E> graph, Object[] roots, String graphName) {
		super(graphName);
		
		JGraph jgraph = new JGraph(new JGraphModelAdapter<V, E>(graph));
		JGraphFacade facade = new JGraphFacade(jgraph, roots);
		JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
		layout.setOrientation(SwingConstants.SOUTH);
		layout.setDeterministic(true);
		layout.run(facade);
		@SuppressWarnings("rawtypes")
		final Map nestedMap = facade.createNestedMap(true, true);
		jgraph.getGraphLayoutCache().edit(nestedMap);

		JPanel container = new JPanel();
		JScrollPane scrPane = new JScrollPane(container);
		add(scrPane);
		container.add(jgraph);
	}
	
	/**
	 * Inits the window.
	 *
	 * @param width the width
	 * @param height the height
	 */
	public void init(int width, int height) {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(width, height);
		this.setVisible(true);
	}
}


