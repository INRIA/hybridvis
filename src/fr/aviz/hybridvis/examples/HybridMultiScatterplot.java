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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;
import fr.aviz.hybridvis.HybridImageRenderer;

/**
 * Class HybridMultiScatterplot
 * 
 * @author Jean-Daniel Fekete
 * @version $Revision$
 */
public class HybridMultiScatterplot extends HybridImageRenderer {
    protected DoubleArrayList xvals;
    protected DoubleArrayList yvals;
    protected IntArrayList clusterIndex;
    protected transient volatile Ellipse2D.Double tmpEllipse = new Ellipse2D.Double();
    boolean fitToDisplay = false;
    
    public class Cluster {
        public float hue;
        public double x;
        public double y;
        public double size;
//        public double size2;
//        public double angle;
        public int[] items;
        public String label;
    };
    
    protected double xmin, xmax, ymin, ymax;
    
    protected ArrayList<Cluster> clusters;
    
    public static void main(String[] args) {
        HybridMultiScatterplot viewer = new HybridMultiScatterplot();
        viewer.readData(null);
        viewer.showOnScreen();
    }
    
    public HybridMultiScatterplot() {
        setHipassRadius(-10); // neg is disabled
        setHipassContrast(1.5f);
        setHipassBrightness(1.5f);
        setBlurRadius(50); // neg is disabled
        setFarImageOpacity(0.55);
        setPostContrast(1.4);
        setPostBrightness(0.85);
    }
    
    protected void readData(String filename) {
        int tempWidth = getWallWidth();
        int tempHeight = getWallHeight();
        
        RandomEngine rand = RandomEngine.makeDefault();
        Normal normal = new Normal(0, 1, rand);
        int nCluster = 10;
        int ptByCluster = 100;
        int w2 = tempWidth / 2;
        int h2 = tempHeight / 2;
        int radius = Math.min(tempWidth, tempHeight);
        
        xvals = new DoubleArrayList(1000);
        yvals = new DoubleArrayList(1000);
        clusterIndex = new IntArrayList();
        clusters = new ArrayList<Cluster>(nCluster);
        
        for (int i = 0; i < nCluster; i++) {
            // pick a cluster center uniformly distributed inside the viewport
            Cluster c = new Cluster();
            c.hue = i / (float)(nCluster-1);
            c.x = rand.nextDouble() * w2 + w2/2;
            c.y = rand.nextDouble() * h2 + h2/2;
            c.size = (rand.nextDouble() + 0.5) * radius / 3;
            c.items = new int[ptByCluster];
            for (int j = 0; j < ptByCluster; j++) {
                double x = (normal.nextDouble() * c.size / 4) + c.x;
                double y = (normal.nextDouble() * c.size / 4) + c.y;
                c.items[j] = xvals.size();
                xvals.add(x);
                yvals.add(y);
                clusterIndex.add(i);
            }
            clusters.add(c);
        }
        updateMinMax();
        if (fitToDisplay)
        	fitToDisplay();
    }
    
    protected void fitToDisplay() {
    	double ax = getSimulatedDisplay().getXResolution() / (xmax - xmin);
    	double ay = getSimulatedDisplay().getYResolution() / (ymax - ymin);
        int size = xvals.size();
        for (int i = 0; i < size; i++) {
        	double x = xvals.getQuick(i);
        	xvals.setQuick(i, (x-xmin) * ax);
        	double y = yvals.getQuick(i);
        	yvals.setQuick(i, (y-ymin) * ay);
        }
        for (Cluster c : clusters) {
        	c.x = (c.x-xmin) * ax;
        	c.y = (c.y-ymin) * ay;
        }
        updateMinMax();
    }
    
    protected void updateMinMax() {
        int size = xvals.size();
        if (size == 0) {
            xmin = xmax = Double.NaN;
            ymin = ymax = Double.NaN;
            return;
        }
        xmin = xmax = xvals.getQuick(0);
        ymin = ymax = yvals.getQuick(0);
        for (int i = 1; i < size; i++) {
            double x = xvals.getQuick(i);
            if (x < xmin) xmin = x;
            else if (x > xmax) xmax = x;
            
            double y = yvals.getQuick(i);
            if (y < ymin) ymin = y;
            else if (y > ymax) ymax = y;
        }
    }
    
    public Color getColor(float h, float s, float b, float a) {
        int color = Color.HSBtoRGB(h, s, b);
        Color c = new Color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, (color & 0xFF), (int)(255*a));  
        
        return c;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawNearGraphics(Graphics2D g) {
        Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
        g.setColor(Color.white);
        g.fill(wallbounds);  // hipass requires an opaque image
        double r = 15;

        int size = xvals.size();
        for (int i = 0; i < size; i++) {
            tmpEllipse.setFrame(xvals.getQuick(i)-r/2, yvals.getQuick(i)-r/2, r, r);
            g.setColor(getColor(clusters.get(clusterIndex.get(i)).hue, 1.0f, 1.0f, 1.0f));
            g.fill(tmpEllipse);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawFarGraphics(Graphics2D g) {
        Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
        g.setColor(Color.white);
        float[] dist = { 0.0f, 1.0f};
        Color[] colors = { Color.RED, Color.RED };
        
        g.fill(wallbounds);
        for (Cluster c : clusters) {
            double r = c.size / 2;
            Point2D center = new Point2D.Double(c.x, c.y);
            colors[0] = getColor(c.hue, 1.0f, 0.8f, 0.5f);
            colors[1] = getColor(c.hue, 1.0f, 0.8f, 0.1f);
            RadialGradientPaint p = new RadialGradientPaint(center, (float)r, dist, colors);
            g.setPaint(p);
            tmpEllipse.setFrame(c.x-r, c.y-r, c.size, c.size);
            g.fill(tmpEllipse);
        }
        g.setColor(Color.black);
    }
}
