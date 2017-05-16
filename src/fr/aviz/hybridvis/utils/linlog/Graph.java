/*
 * HybridVis - Hybrid visualizations generator and library
 * Copyright (C) 2016 Inria
 *
 * This file is part of HybridVis.
 *
 * HybridVis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HybridVis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HybridVis.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.aviz.hybridvis.utils.linlog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class Graph
 * 
 * @author Jean-Daniel Fekete
 * @version $Revision$
 */
public class Graph extends HashMap<String,Map<String,Double>> {
    Map<String,Node> nameToNode;
    List<Node> nodes;
    List<Edge> edges;
    
    /**
     * Creates an empty graph.
     */
    public Graph() {
    }
    
    /**
     * Reads and returns a graph from the specified file.
     * The graph is returned as a nested map: Each source node 
     * of an edge is mapped to a map representing its outgoing edges.  
     * This map maps each target node of the outgoing edges to the edge weight
     * (the weight of the edge from the source node to the target node).
     * Schematically, source -> target -> edge weight.
     * 
     * @param filename name of the file to read from.
     * @return read graph.
     */
    public static Graph readGraph(String filename) {
        return new Graph().read(filename);
    }

    public static Graph readGraph(String filename, String sepRegex) {
        return new Graph().read(filename, sepRegex);
    }
    
    public Graph read(String filename, String sepRegexp) {
        Graph result = new Graph();
        try {
            BufferedReader file = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filename), 
                            "utf-8"));
            String line;
            while ((line = file.readLine()) != null) {
                String[] fields = line.split(sepRegexp);
                if (fields.length < 2) continue;
                String source = fields[0]; // st.nextToken();
                String target = fields[1]; // st.nextToken();
                double weight = fields.length > 2 ? Double.parseDouble(fields[2]) : 1.0;
                result.addEdge(source, target, weight);
            }
            file.close();
        } catch (IOException e) { // ignores error for now
            return null;
        }
        return makeSymmetricGraph(result);
    }
    
    public Graph read(String filename) {
        return read(filename, " +");
    }
    
    
    public Graph addEdge(String source, String target, double weight) {
        if (this.get(source) == null) this.put(source, new HashMap<String,Double>());
        this.get(source).put(target, weight);
        return this;
    }
    
    public Graph addEdge(String source, String target) {
        return addEdge(source, target, 1.0);
    }
    
    public Graph makeSymmetricGraph(Graph graph) {
        clear();
        Graph result = this;
        for (String source : graph.keySet()) {
            for (String target : graph.get(source).keySet()) {
                double weight = graph.get(source).get(target);
                double revWeight = 0.0f;
                if (graph.get(target) != null && graph.get(target).get(source) != null) {
                    revWeight = graph.get(target).get(source);
                }
                if (result.get(source) == null) 
                    result.put(source, new HashMap<String,Double>());
                result.get(source).put(target, weight+revWeight);
                if (result.get(target) == null) 
                    result.put(target, new HashMap<String,Double>());
                result.get(target).put(source, weight+revWeight);
            }
        }
        return result;
    }
    

    /**
     * Construct a map from node names to nodes for a given graph, 
     * where the weight of each node is set to its degree,
     * i.e. the total weight of its edges. 
     * 
     * @return map from each node names to nodes.
     */
    public Map<String,Node> makeNodes() {
        if (nameToNode != null) return nameToNode;
        Graph graph = this;
        Map<String,Node> result = new HashMap<String,Node>();
        for (String nodeName : graph.keySet()) {
            double nodeWeight = 0.0;
            for (double edgeWeight : graph.get(nodeName).values()) {
                nodeWeight += edgeWeight;
            }
            result.put(nodeName, new Node(nodeName, nodeWeight));
        }
        nameToNode = result;
        nodes = new ArrayList<Node>(nameToNode.values());
        return result;
    }

    /**
     * @return the list of nodes
     */
    public List<Node> getNodes() {
        if (nodes == null) makeNodes();
        return nodes;
    }
    
    /**
     * Retrieve a node from its name
     * @param name the node name
     * @return the node or null
     */
    public Node getNode(String name) {
        makeNodes();
        return nameToNode.get(name);
    }
    
    /**
     * Converts a given graph into a list of edges.
     * 
     * @return the given graph as list of edges.
     */
    public List<Edge> makeEdges() {
        if (edges != null) return edges;
        Graph graph = this;
        Map<String,Node> nameToNode = makeNodes();

        List<Edge> result = new ArrayList<Edge>();
        for (String sourceName : graph.keySet()) {
            for (String targetName : graph.get(sourceName).keySet()) {
                Node sourceNode = nameToNode.get(sourceName);
                Node targetNode = nameToNode.get(targetName);
                double weight = graph.get(sourceName).get(targetName);
                result.add( new Edge(sourceNode, targetNode, weight) );
            }
        }
        edges = result;
        return result;
    }
    
    /**
     * @return the edges
     */
    public List<Edge> getEdges() {
        if (edges == null) makeEdges();
        return edges;
    }
    
    /**
     * Computes a modularity clustering.
     * @return the mapping of nodes to cluser number
     */
    public Map<Node,Integer> makeClusters() {
        return new OptimizerModularity().execute(getNodes(), getEdges(), false);
    }

}
