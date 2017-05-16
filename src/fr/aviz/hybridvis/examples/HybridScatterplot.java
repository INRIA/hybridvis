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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import fr.aviz.hybridvis.HybridImageRenderer;


/**
 * This class draws a Hertzsprung-Russell diagram with unobtrusive star labels.
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized image (key 'S').
 *  
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Click again to switch back to the overview mode.
 * - When transitioning between zoom levels, a preview will be shown consisting in the near and far content alpha-blended (no high-pass and blurring).
 * - Hit S to generate the full image and save it on the disk (this process will take about 10 min).
 *
 * See HybridImageRenderer.java for more info.
 * 
 * @author dragice
 * 
 */
public class HybridScatterplot extends HybridImageRenderer {

	static class DataPoint {
		double x, y;
		double z; // used for filtering and label size
		String label;
	}
	
	ArrayList<DataPoint> data = new ArrayList<DataPoint>();
	double xmin, xmax, ymin, ymax;
	
	public static void main(String[] args) {
		HybridScatterplot viewer = new HybridScatterplot();
		viewer.showOnScreen();
	}

	
	public HybridScatterplot() {
		
		super();

		loadData("data/stars/HYG.csv");

		setHipassRadius(10);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setBlurRadius(10);
		setFarImageOpacity(0.55);
		setPostContrast(1.4);
		setPostBrightness(0.85);
	}
	
	/**
	 * Loads the Hertzsprung-Russell data.
	 * FIXME: generalize to any csv file.
	 */
	public void loadData(String dataFile) {
		System.out.print("Loading data... ");
		xmin = ymin = Double.MAX_VALUE;
		xmax = ymax = Double.MIN_VALUE;
	    try {
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
	        String line = br.readLine();
	        int l = 0;
	        while (line != null) {
	        	String[] cells = line.split(";");
	        	if (l >= 2) {
	        		DataPoint p = new DataPoint();
	        		p.label = cells[6].length() > 0 ? cells[6] : cells[4]; // Star name
	        		p.x = Double.parseDouble(cells[16]); // Star Color Index
	        		p.y = Double.parseDouble(cells[14]); // Star Absolute Magnitude
	        		p.z = Double.parseDouble(cells[9]);  // Star Distance (used for filtering and label size)
	        		if (p.x < 2.5) { // filter
		        		if (p.x > xmax) xmax = p.x;
		        		if (p.x < xmin) xmin = p.x;
		        		if (p.y > ymax) ymax = p.y;
		        		if (p.y < ymin) ymin = p.y;
		        		data.add(p);
	        		}
	        	}
	            line = br.readLine();
	            l++;
	        }
		    br.close();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
		System.out.println("Loaded " + data.size() + " points.");
	}

	/**
	 * Draws the visual content that will be visible when the user is close to the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * The high-pass filter requires an opaque image, so make sure you first fill the background with an opaque color.
	 */
	@Override
	public void drawNearGraphics(Graphics2D g) {
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
		g.setColor(Color.white);
		g.fill(wallbounds);  // hipass requires an opaque image
		drawScatterplot(g, wallbounds, 0, new Color(0f, 0f, 0f, 1f), true, true);
	}

	/**
	 * Draws the visual content that will be visible when the user is far from the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * For best results, first fill the background with an opaque color. 
	 */
	@Override
	public void drawFarGraphics(Graphics2D g) {
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(), getWallHeight());
		g.setColor(Color.white);
		g.fill(wallbounds);
		drawScatterplot(g, wallbounds, 15, new Color(0f, 0f, 0f, 0.75f), false, false);
	}
	
	/**
	 * A pointSize of 0 enables automatic size (FIXME).
	 */
	protected void drawScatterplot(Graphics2D g, Rectangle2D bounds, double pointSize, Color pointColor, boolean label, boolean hideUnlabelledPoints) {
		g.setColor(pointColor);

		double r = pointSize / 2;
		Rectangle2D.Double tmpRect = new Rectangle2D.Double();
		Ellipse2D.Double tmpEllipse = new Ellipse2D.Double();
		if (label) {
			int fontSize = (int)(pointSize) + 4;
			Font f = new Font("Helvetica", 0, fontSize);
			g.setFont(f);
		}
		for (DataPoint p: data) {
			if (hideUnlabelledPoints && p.label.length() == 0)
				continue;
			double x = bounds.getX() + (p.x - xmin) / (xmax - xmin) * bounds.getWidth();
			double y = bounds.getY() + (p.y - ymin) / (ymax - ymin) * bounds.getHeight();
//			if (x < bounds.getX() - r || x > bounds.getMaxX() + r || y < bounds.getY() - r || y > bounds.getMaxY() + r)
//				continue;
			tmpEllipse.setFrame(x - r, y - r, r*2, r*2);
			// FIXME: temporary hack for auto size
			if (pointSize == 0) {
				r = 15 / 2 * 0.4;
				tmpEllipse.setFrame(x - r, y - r, r*2, r*2);
			}
			g.fill(tmpEllipse);
			if (label && p.label.length() > 0) {
				// FIXME: temporary hack for auto size
				double r2 = r;
				if (pointSize == 0) {
					r2 = 15 / 2 * (1.5 / (p.z+0.1));
					if (r2 > 30) r2 = 30;
					int fontSize = (int)(r2 * 2) + 4;
					Font f = new Font("Helvetica", Font.BOLD, fontSize);
					g.setFont(f);
				}
				tmpRect.setRect(x + r, y - 0.5, r2 * 1.0, 1);
				g.fill(tmpRect);
				g.drawString(p.label, (int)(x + r + r2 * 1.5), (int)(y + 2 + r2 * 0.6));
			}
		}
	}
	
}
