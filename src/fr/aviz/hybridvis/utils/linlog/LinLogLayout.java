package fr.aviz.hybridvis.utils.linlog;
//Copyright (C) 2008 Andreas Noack
//
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU Lesser General Public
//License as published by the Free Software Foundation; either
//version 2.1 of the License, or (at your option) any later version.
//
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//Lesser General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public
//License along with this library; if not, write to the Free Software
//Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA 


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * LinLogLayout is a simple program for computing graph layouts 
 * (positions of the nodes of a graph in two- or three-dimensional space) 
 * and graph clusterings for visualization and knowledge discovery.
 * It reads a graph from a file, computes a layout and a clustering, writes 
 * the layout and the clustering to a file, and displays them in a dialog.
 * LinLogLayout can be used to identify groups of densely connected nodes 
 * in graphs, like groups of friends or collaborators in social networks,
 * related documents in hyperlink structures (e.g. web graphs),
 * cohesive subsystems in software models, etc.
 * With a change of a parameter in the <code>main</code> method,
 * it can also compute classical "nice" (i.e. readable) force-directed layouts.
 * The program is primarily intended as a demo for the use of its core layouter 
 * and clusterer classes <code>MinimizerBarnesHut</code>,
 * <code>MinimizerClassic</code>, and <code>OptimizerModularity</code>.  
 * While <code>MinimizerBarnesHut</code> is faster, <code>MinimizerClassic</code>
 * is simpler and not limited to a maximum of three dimensions.
 * 
 * @author Andreas Noack (an@informatik.tu-cottbus.de)
 * @version 13.11.2008
 */
public class LinLogLayout {
    static private Random rand = new Random();

    /**
     * @return the rand
     */
    public static Random getRand() {
        return rand;
    }
    
	/**
	 * Returns, for each node in a given list,
	 * a random initial position in two- or three-dimensional space. 
	 * 
	 * @param nodes node list.
     * @param is3d initialize 3 (instead of 2) dimension with random numbers.
	 * @return map from each node to a random initial positions.
	 */
	private static Map<Node,double[]> makeInitialPositions(List<Node> nodes, boolean is3d) {
        Map<Node,double[]> result = new HashMap<Node,double[]>();
		for (Node node : nodes) {
            double[] position = { rand.nextDouble()- 0.5,
                                  rand.nextDouble() - 0.5,
                                  is3d ? rand.nextDouble() - 0.5 : 0.0 };
            result.put(node, position);
		}
		return result;
	}
	
	/**
	 * Writes a given layout and clustering into the specified file.
	 * 
	 * @param nodeToPosition map from each node to its layout position.
     * @param nodeToPosition map from each node to its cluster.
	 * @param filename name of the file to write into.
	 */
	private static void writePositions(Map<Node,double[]> nodeToPosition, 
            Map<Node,Integer>nodeToCluster, String filename) {
		try {
			BufferedWriter file = new BufferedWriter(new FileWriter(filename));
			for (Node node : nodeToPosition.keySet()) {
				double[] position = nodeToPosition.get(node);
                int cluster = nodeToCluster.get(node);
				file.write(node.name + " " + position[0] + " " + position[1] 
                                     + " " + position[2] + " " + cluster);
                file.write("\n");
			}
			file.close();
		} catch (IOException e) {
		      System.err.println("Exception while writing the graph:"); 
			  System.err.println(e);
			  System.exit(1);
		}
	}
	
	/**
	 * Reads a graph from a specified input file, 
     * computes a layout and a clustering, 
     * writes the layout and the clustering into a specified output file, 
     * and displays them in a dialog.
	 * 
	 * @param args number of dimensions, name of the input file and of the output file.
	 * 	 If <code>args.length != 3</code>, the method outputs a help message.
	 */
	public static void main(final String[] args) {
		if (args.length != 3 || (!args[0].equals("2") && !args[0].equals("3")) ) {
			System.out.println(
				  "Usage: java LinLogLayout <dim> <inputfile> <outputfile>\n"
				+ "Computes a <dim>-dimensional layout and a clustering for the graph\n"
                + "in <inputfile>, writes the layout and the clustering into <outputfile>,\n" 
                + "and displays (the first 2 dimensions of) the layout and the clustering.\n"
                + "<dim> must be 2 or 3.\n\n"
				+ "Input file format:\n"
				+ "Each line represents an edge and has the format:\n"
				+ "<source> <target> <nonnegative real weight>\n"
				+ "The weight is optional, the default value is 1.0.\n\n"
				+ "Output file format:\n"
				+ "<node> <x-coordinate> <y-coordinate> <z-coordinate (0.0 for 2D)> <cluster>"
			);
			System.exit(0);
		}
		
		Graph graph = Graph.readGraph(args[1]);
        List<Node> nodes = graph.getNodes();
        List<Edge> edges = graph.getEdges();
		Map<Node,double[]> nodeToPosition = makeInitialPositions(nodes, args[0].equals("3"));
		// see class MinimizerBarnesHut for a description of the parameters;
		// for classical "nice" layout (uniformly distributed nodes), use
		//new MinimizerBarnesHut(nodes, edges, -1.0, 2.0, 0.05).minimizeEnergy(nodeToPosition, 100);
		new MinimizerBarnesHut(nodes, edges, 0.0, 1.0, 0.05).minimizeEnergy(nodeToPosition, 100);
        // see class OptimizerModularity for a description of the parameters
        Map<Node,Integer> nodeToCluster = graph.makeClusters(); 
		writePositions(nodeToPosition, nodeToCluster, args[2]);
		(new GraphFrame(nodeToPosition, nodeToCluster)).setVisible(true);
	}
	
	/**
	 * Computes the layout.
	 * @param filename input file name
	 * @param repuExponent exponent of the distance in the repulsion energy.
     *   Exception: The value 0.0 corresponds to logarithmic repulsion.  
     *   Is 0.0 in both the LinLog and the Fruchterman-Reingold energy model.
     *   Negative values are permitted.
     * @param attrExponent exponent of the distance in the attraction energy.
     *   Is 1.0 in the LinLog model (which is used for computing clusters,
     *   i.e. dense subgraphs), 
     *   and 3.0 in standard energy model of Fruchterman and Reingold.  
     *   Must be greater than <code>repuExponent</code>.
     * @param gravFactor  factor for the gravitation energy.
     *   Gravitation attracts each node to the barycenter of all nodes,
     *   to prevent distances between unconnected graph components
     *   from approaching infinity.  
     *   Typical values are 0.0 if the graph is guaranteed to be connected,
     *   and positive values significantly smaller 1.0 (e.g. 0.05) otherwise.
	 * @return a map of nodes and x,y,cluster array
	 */	
	public static Map<Node,double[]> layout(
	        String filename,
	        double repuExponent, double attrExponent, double gravFactor) {
	    
	    Graph graph = Graph.readGraph(filename);
	    if (graph == null) {
            System.err.println("Exception while reading the graph"); 
            System.exit(1);	        
	    }
	    return layout(graph, repuExponent, attrExponent, gravFactor);
	}
	
	   /**
     * Computes the layout.
     * @param graph the graph to layout
     * @param repuExponent exponent of the distance in the repulsion energy.
     *   Exception: The value 0.0 corresponds to logarithmic repulsion.  
     *   Is 0.0 in both the LinLog and the Fruchterman-Reingold energy model.
     *   Negative values are permitted.
     * @param attrExponent exponent of the distance in the attraction energy.
     *   Is 1.0 in the LinLog model (which is used for computing clusters,
     *   i.e. dense subgraphs), 
     *   and 3.0 in standard energy model of Fruchterman and Reingold.  
     *   Must be greater than <code>repuExponent</code>.
     * @param gravFactor  factor for the gravitation energy.
     *   Gravitation attracts each node to the barycenter of all nodes,
     *   to prevent distances between unconnected graph components
     *   from approaching infinity.  
     *   Typical values are 0.0 if the graph is guaranteed to be connected,
     *   and positive values significantly smaller 1.0 (e.g. 0.05) otherwise.
     * @return a map of nodes and x,y,cluster array
     */ 
    public static Map<Node,double[]> layout(
            Graph graph,
            double repuExponent, double attrExponent, double gravFactor) {

        Map<Node,double[]> nodeToPosition = makeInitialPositions(graph.getNodes(), false);
        // see class MinimizerBarnesHut for a description of the parameters;
        // for classical "nice" layout (uniformly distributed nodes), use
        //new MinimizerBarnesHut(nodes, edges, -1.0, 2.0, 0.05).minimizeEnergy(nodeToPosition, 100);
        new MinimizerBarnesHut(graph.getNodes(), graph.getEdges(), repuExponent, attrExponent, gravFactor).minimizeEnergy(nodeToPosition, 100);
        // see class OptimizerModularity for a description of the parameters
//        Map<Node,Integer> nodeToCluster = graph.makeClusters();
//        
//        for (Map.Entry<Node, double[]> entry: nodeToPosition.entrySet()) {
//            Integer cluster = nodeToCluster.get(entry.getKey());
//            entry.getValue()[2] = cluster.doubleValue();
//        }
        return nodeToPosition;
    }
	
    /**
     * Computes the layout.
     * @param graph the graph to layout
     * 
     * @return a map of nodes and x,y,cluster array
     */
	   public static Map<Node,double[]> layout(Graph graph) {
	       return layout(graph, 0.0, 1.0, 0.05);
	   }
}
