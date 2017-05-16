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

package fr.aviz.hybridvis.examples;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gicentre.utils.colour.ColourTable;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import fr.aviz.hybridvis.utils.GUIUtils;
import fr.aviz.hybridvis.utils.linlog.Edge;
import fr.aviz.hybridvis.utils.linlog.Graph;
import fr.aviz.hybridvis.utils.linlog.LinLogLayout;
import fr.aviz.hybridvis.utils.linlog.Node;

/**
 * Class HybridNodeLinkDiagram
 * 
 * @author Jean-Daniel Fekete
 * @version $Revision$
 */
public class HybridNodeLinkDiagramVariant extends HybridMultiScatterplot {
    Graph graph;
    Map<Node,double[]> layout;
    Font nearFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    Font nearFontNoCluster = new Font(Font.SANS_SERIF, 0, 9);
    Font farFont = new Font(Font.SANS_SERIF, Font.BOLD, 200);
    Stroke farStroke = new BasicStroke(10);
    Stroke nearStroke = new BasicStroke(1);
    Stroke nearStrokeNoCluster = new BasicStroke(0.25f);
    Map<String,Integer> clusterNames;
    
    /**
     * Main program
     * @param args a graph file to load or nothing
     */
    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : "data/graph/saclay.el";
        
        HybridNodeLinkDiagramVariant viewer = new HybridNodeLinkDiagramVariant();

        viewer.fitToDisplay = true;
        viewer.readData(filename);
        viewer.showOnScreen();
    }
    
    private HybridNodeLinkDiagramVariant() {
        setHipassRadius(10); // neg is disabled
        setHipassContrast(1.5f);
        setHipassBrightness(1.5f);
        setBlurRadius(20); // neg is disabled
        setFarImageOpacity(0.50);
        setPostContrast(1.4);
        setPostBrightness(0.85);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readData(String filename) {
        LinLogLayout.getRand().setSeed(0);
        graph = Graph.readGraph(filename, ";");
        int dot = filename.lastIndexOf('.');
        String prefix = dot != -1 ? filename.substring(0, dot) : filename;
        Map<Node,Integer> nodeToCluster = loadClusters( prefix+ ".aff");
        if (nodeToCluster == null)
            nodeToCluster = graph.makeClusters();
        
        layout = loadLayout(prefix+".tsv");
        if (layout == null)
            layout = LinLogLayout.layout(graph, -1.0, 2.0, 0.05);
        
        xvals = new DoubleArrayList(layout.size());
        yvals = new DoubleArrayList(layout.size());
        clusterIndex = new IntArrayList(layout.size());
        clusters = new ArrayList<Cluster>();
        
        for (Node node : graph.getNodes()) {
            double[] xyc = layout.get(node);
            xvals.add(xyc[0]);
            yvals.add(xyc[1]);
            Integer cl = nodeToCluster.get(node);
            int c = cl == null ? 0 : cl.intValue();
            clusterIndex.add(c);
            while (c >= clusters.size()) {
                clusters.add(new Cluster());
            }
            Cluster cluster = clusters.get(c);
            cluster.size++;
            if (isCluster(node)) {
                cluster.x = xyc[0];
                cluster.y = xyc[1];
                cluster.label = node.name;
            }
            xyc[0] = xvals.size()-1; // to point back to node position
        }
        
        int nCluster = clusters.size();
        for (int i = 0; i < nCluster; i++) {
            Cluster cluster = clusters.get(i);
            cluster.hue = i; 
            cluster.items = new int[(int)cluster.size];
            cluster.size = 0;
        }
        updateMinMax();
        updateClusters();
        updateMinMax();
        if (fitToDisplay)
        	fitToDisplay();
    }
    
    private Map<Node,double[]> loadLayout(String filename) {
        Map<Node,double[]> results = null;
        try {
            BufferedReader file = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filename), 
                            "utf-8"));
            String line = file.readLine();
            if (! line.startsWith("name\tx\ty")) return null;
            results = new HashMap<Node, double[]>();
            while ((line = file.readLine()) != null) {
                String[] fields = line.split("\t");
                Node node = graph.getNode(fields[0]);
                if (node == null) {
                    System.err.println("Invalid node named "+fields[0]);
                    file.close();
                    return null;
                }
                double[] pos = new double[3];
                try {
                    pos[0] = Double.parseDouble(fields[1]);
                    pos[1] = Double.parseDouble(fields[2]);
                }
                catch(Exception e) {
                    continue;
                }
                results.put(node, pos);
            }
            file.close();
        } catch (IOException e) { // ignores error for now
            return null;
        }
        return results;
    }
    
    private Map<Node,Integer> loadClusters(String filename) {
        clusterNames = new HashMap<String, Integer>();
        Map<Node,Integer> results = new HashMap<Node, Integer>();

        try {
            BufferedReader file = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filename), 
                            "utf-8"));
            String line;
            while ((line = file.readLine()) != null) {
                String[] fields = line.split(";");
                Integer c = clusterNames.get(fields[1]);
                if (c == null) {
                    c = new Integer(clusterNames.size()+1); // start with 1, cluster 0 will be for others
                    clusterNames.put(fields[1], c);
                }
                if (fields[2].equals("last"))
                    results.put(graph.getNode(fields[0]), c);
            }
            file.close();
        } catch (IOException e) { // ignores error for now
            return null;
        }
        return results;
    }
    
    private boolean isCluster(Node node) {
        return clusterNames.containsKey(node.name);
    }

    private void updateClusters() {
//        int nCluster = clusters.size();
        double sx = (getWallWidth()-100) / (xmax - xmin);
        double sy = (getWallHeight()-100) / (ymax - ymin);
        double xoff = 50, yoff = 50;
        if (sx > sy) {
            sx = sy;
            xoff = (getWallWidth()-(xmax-xmin)*sx)/2;
        }
        else {
            sy = sx;
            yoff = (getWallHeight()-(ymax-ymin)*sy)/2;
        }
        int size = xvals.size();
        for (int i = 0; i < size; i++) {
            // rescale to wallw/wallh
            double x = (xvals.getQuick(i)-xmin)*sx + xoff;
            double y = (yvals.getQuick(i)-ymin)*sy + yoff;
            xvals.set(i, x);
            yvals.set(i, y);
            Cluster cluster = clusters.get(clusterIndex.get(i));
//            cluster.x += x;
//            cluster.y += y;
            cluster.items[(int)cluster.size++] = i;
        }
        
        for (Cluster c : clusters) {
            c.x = (c.x-xmin)*sx + xoff;
            c.y = (c.y-ymin)*sy + yoff;
        }
        xmin = ymin = 0;
        xmax = getWallWidth();
        ymax = getWallHeight();
        
//        DoubleArrayList xs = new DoubleArrayList();
//        Point2D points[] = new Point2D[1000];
        
//        for (int i = 0; i < nCluster; i++) {
            //xs.clear();
//            Cluster cluster = clusters.get(i);
//            cluster.x /= cluster.size;
//            cluster.y /= cluster.size;
            
//            for (int j = 0; j < cluster.items.length; j++) {
//                Point2D p = points[j];
//                if (p == null) {
//                    p = points[j] = new Point2D.Double();
//                }
//                p.setLocation(xvals.get(cluster.items[j]), yvals.get(cluster.items[j]));
//                xs.add(Math.hypot(
//                        cluster.x - xvals.get(cluster.items[j]),
//                        cluster.y - yvals.get(cluster.items[j])));;
//            }
//            FitEllipse.EllipseParams params = FitEllipse.fitParametric(points, 0, cluster.items.length);
//            if (params == null) {
//                cluster.size = 0;
//                for (int j = 0; j < cluster.items.length; j++) {
//                    double d = points[j].distance(cluster.x, cluster.y);
//                    if (d > cluster.size)
//                        cluster.size = d;
//                }
//                cluster.angle = 0;
//                cluster.size2 = cluster.size;
//            }
//            else {
//                cluster.x = params.cx;
//                cluster.y = params.cy;
//                cluster.angle = params.angle;
//                cluster.size = params.rX;
//                cluster.size2 = params.rY;
//            }            
//            xs.sort();
//            double xq = Descriptive.quantile(xs, 1)*2; // median
//            cluster.size = xq;
//            cluster.size = 100;
//        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void drawNearGraphics(Graphics2D g) {
        Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
        g.setColor(Color.white);
        g.fill(wallbounds);  // hipass requires an opaque image
        
        Stroke saved = g.getStroke();
        g.setStroke(nearStroke);
        g.setFont(nearFont);
        drawLinks(g);
        g.setStroke(saved);

        int size = xvals.size();
        for (int i = 0; i < size; i++) {
        	if (isCluster(graph.getNodes().get(i)))
        		continue;
        	double r = (clusterIndex.get(i) > 0) ? 20 : 5;
            tmpEllipse.setFrame(xvals.getQuick(i)-r/2, yvals.getQuick(i)-r/2, r, r);
            g.setColor(getColor(i));
            g.fill(tmpEllipse);
        }
        drawLabels(g);
    }

    static ColourTable ctable;
    private Color getColor(int node) {
        int c = clusterIndex.get(node);
        if (c == 0)
            return Color.gray;
        else
            return getColor(clusters.get(c).hue, 1.0f, 1.0f, 1.0f);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Color getColor(float h, float s, float b, float a) {
        int index = (int)h;
        if ((index % 2) == 0) s = s * 0.5f;
        if (ctable == null) {
            ctable = ColourTable.getPresetColourTable(ColourTable.PAIRED_12, 0, 12);
        }
        
        index = index % 11;
        if (index == 10) index = 11; // skip yellow
        return new Color(ctable.findColour(index));
//        return super.getColor(h / clusters.size(), s, b, a);
//        return super.getColor(h / clusters.size(), s, b, a);
    }
    
    private void drawLinks(Graphics2D g) {
        Line2D.Double line = new Line2D.Double();
        Color c = new Color(0f, 0f, 0f, 0.2f);
        
        g.setColor(c);
        for (Edge edge : graph.getEdges()) {
            int i = (int)layout.get(edge.startNode)[0];
            int j = (int)layout.get(edge.endNode)[0];
            if (isCluster(edge.startNode) || isCluster(edge.endNode))
            	continue;
            g.setStroke(nearStroke);
            if (clusterIndex.get(i) == 0 || clusterIndex.get(j) == 0)
                g.setStroke(nearStrokeNoCluster);
            g.setColor(GUIUtils.mix(getColor(i), getColor(j), 0.5f));
            line.x1 = xvals.get(i);
            line.y1 = yvals.get(i);
            line.x2 = xvals.get(j);
            line.y2 = yvals.get(j);
            g.draw(line);
        }
    }

    private void drawLabels(Graphics2D g) {
        int size = xvals.size();
        for (int i = 0; i < size; i++) {
            String label = graph.getNodes().get(i).name;
            if (label == null || label.length() == 0)
                continue;
        	if (isCluster(graph.getNodes().get(i)))
        		continue;
            try {
            	g.setColor(GUIUtils.mix(Color.black, getColor(i), 0.5f));
            	if (clusterIndex.get(i) > 0) {
            		g.setFont(nearFont);
            		g.drawString(label, (float)xvals.get(i) + 12, (float)yvals.get(i) + 6);
            	} else {
            		g.setFont(nearFontNoCluster);
            		g.drawString(label, (float)xvals.get(i) + 4, (float)yvals.get(i) + 3);
            	}
            }
            catch(InternalError e) {
                //invalid string
                System.err.println("Invalid node name: "+label);
            }
        }
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawFarGraphics(Graphics2D g) {
        Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
        g.setColor(Color.white);
        g.setFont(farFont);
        g.setStroke(farStroke);
        g.fill(wallbounds);  // hipass requires an opaque image
        
//        Stroke saved = g.getStroke();
//        g.setStroke(farStroke);
//        drawLinks(g);
//        g.setStroke(saved);
//
//        double r = 200;
//        int size = xvals.size();
//        for (int i = 0; i < size; i++) {
//            if (clusterIndex.get(i) == 0)
//                continue; // skip cluster 0 for far graphicsaxi
//            tmpEllipse.setFrame(xvals.getQuick(i)-r/2, yvals.getQuick(i)-r/2, r, r);
//            g.setColor(getColor(clusters.get(clusterIndex.get(i)).hue, 1.0f, 0.2f, 1.0f));
//            g.fill(tmpEllipse);
//        }
        Line2D.Double line = new Line2D.Double();

        g.setColor(new Color(0f, 0f, 0f, 0.2f));
        for (Edge edge : graph.getEdges()) {
            if (isCluster(edge.startNode) && isCluster(edge.endNode)) {
                int i = (int)layout.get(edge.startNode)[0];
                int j = (int)layout.get(edge.endNode)[0];
                g.setColor(GUIUtils.mix(getColor(i), getColor(j), 0.5f));
                line.x1 = xvals.get(i);
                line.y1 = yvals.get(i);
                line.x2 = xvals.get(j);
                line.y2 = yvals.get(j);
                g.draw(line);
            }
        }
      //  g.setStroke(null);
        double r = 400;
//        for (Cluster c : clusters) {
//            tmpEllipse.setFrame(c.x-r/2, c.y-r/2, r, r);
//            g.setColor(getColor(c.hue, 1.0f, 0.7f, .4f));
//          //  g.fill(tmpEllipse);
//        } 
        g.setColor(Color.black);
        for (Cluster c : clusters) {
        	g.setColor(getColor(c.hue, .7f, 0.7f, 1.0f));
            String label = c.label;
            if (label == null || label.length() == 0)
                continue;
            try {
                Rectangle2D bounds = farFont.getStringBounds(label, g.getFontRenderContext());
                g.drawString(label, (float)(c.x - bounds.getCenterX()), (float)(c.y - bounds.getCenterY()));
            }
            catch(InternalError e) {
                //invalid string
                System.err.println("Invalid node name: "+label);
            }
        }
    }
    
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void drawFarGraphics(Graphics2D g) {
//        Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
//        g.setColor(Color.white);
//        
//        g.fill(wallbounds);
//        for (Cluster c : clusters) {
//            AffineTransform saved = g.getTransform();
//            g.setColor(getColor(c.hue, 1.0f, 0.2f, 0.5f));
//            g.translate(c.x, c.y);
//            g.rotate(c.angle);
//            tmpEllipse.setFrame(-c.size, -c.size2, 2*c.size, 2*c.size2);
//            g.fill(tmpEllipse);
//            g.setTransform(saved);
//        }
//    }
}

