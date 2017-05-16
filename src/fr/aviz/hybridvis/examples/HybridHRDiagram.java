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
import java.awt.geom.AffineTransform;
import java.io.IOException;

import processing.core.PApplet;
import processing.data.Table;
import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.models.DualScaleGrid;
import fr.aviz.hybridvis.utils.GradientColorScale;

/**
 * This class draws a Hertzsprung-Russell diagram with dual scale labels and grids.
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized image (key 'S').
 * 
 * See HybridImageRenderer.java for more info.
 * 
 * @author wjwillett
 * 
 */
public class HybridHRDiagram extends HybridImageRenderer {

	protected GradientColorScale colorGradient = new GradientColorScale();
	protected Table table;
	protected float minColorIndex = Float.POSITIVE_INFINITY;
	protected float maxColorIndex = Float.NEGATIVE_INFINITY;
	protected float minAbsMag = Float.POSITIVE_INFINITY;
	protected float maxAbsMag = Float.NEGATIVE_INFINITY;
	protected DualScaleGrid gridRenderer = null;
	
	public double biggerFontSize = getSimulatedDisplay().getYResolution() * 0.0468; //300
	public double bigFontSize = getSimulatedDisplay().getYResolution() * 0.0390625; //250 these are just some heuristics
	public double largerFontSize = getSimulatedDisplay().getYResolution() * 0.03125; //200

  public static void main(String[] args) {
    HybridHRDiagram viewer = new HybridHRDiagram();
    viewer.showOnScreen();
  }
	
	public HybridHRDiagram() {
		super();
		// Read data
		readData();
		
		// Configure the HybridImage Renderer.
		setHipassRadius(3);
		setBlurRadius(20);
		setPostBrightness(0.8);   

		//set up our color gradient
		colorGradient.addColor(-0.3f, Color.getHSBColor(0.7f, 1.0f, 0.85f));	//Violet
		colorGradient.addColor(-0.1f, Color.getHSBColor(0.6f, 1.0f, 0.85f));	//Blue
		colorGradient.addColor(0.65f, Color.getHSBColor(0.5f, 1.0f, 0.85f)); 	//Cyan 
		colorGradient.addColor(0.75f, Color.getHSBColor(0.13f, 1.0f, 0.85f)); 	//Gold
		colorGradient.addColor(1.1f, Color.getHSBColor(0.08f, 1.0f, 0.85f));	//Orange
		colorGradient.addColor(2.0f, Color.getHSBColor(0.0f, 1.0f, 0.85f)); 	//Red
		
		// Configure a new dual-scale grid renderer
		gridRenderer = new DualScaleGrid(getSimulatedDisplay(), null,
        0.6, 4.0, getBlurRadius(), 
        new double[]{minColorIndex, maxColorIndex}, 
        new double[]{minAbsMag, maxAbsMag});
		gridRenderer.farLinesPerDisplayHorizontal = 2;
		gridRenderer.farLinesPerDisplayVertical = 2;
    
	}

	@Override
	public void drawNearGraphics(Graphics2D g) {
	  
	  //draw the near-visible grid
		int w = getSimulatedDisplay().getXResolution();
		int h = getSimulatedDisplay().getYResolution();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		gridRenderer.drawNear(g);

		//set basic label styles for all bubbles
		Font labelFont = g.getFont().deriveFont(10f);
		g.setFont(labelFont);
		g.setStroke(new BasicStroke(1));
		
		//display a point-sized bubble (with a label if we have one) for each star 
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			String label = table.getString(row, 6);
			if(label.length() <= 1 && table.getString(row, 5).length() > 2)
			  label = table.getString(row, 5);
			float absMag = table.getFloat(row, 11);
			float colorIndex = table.getFloat(row, 13);
			int x = colorIndexToX(colorIndex);
			int y = absMagToY(absMag);
			Color c = colorGradient.getColor(colorIndex, 0.9f);
			int diameter = 2;
			g.setColor(c);
			g.fillOval(x - diameter / 2, y - diameter / 2, diameter, diameter);

			//labels
			g.setColor(Color.gray);
			if (label.length() > 1) {
				g.drawLine(x + 2, y, x+10, y);
				g.drawString(label, x + 12, y + 5);
			}
		}
	}


	@Override
	public void drawFarGraphics(Graphics2D g) {
	  
	  //draw the far-visible grid
		int w = getSimulatedDisplay().getXResolution();
		int h = getSimulatedDisplay().getYResolution();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		gridRenderer.drawFar(g);

		//display a larger bubble (sized based on the distance to the next star) for each
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			float absMag = table.getFloat(row, 11);
			float colorIndex = table.getFloat(row, 13);
			int x = colorIndexToX(colorIndex);
			int y = absMagToY(absMag);
			Color c = colorGradient.getColor(colorIndex, 0.9f);
			int diameter = 10 + (int)(200 * table.getFloat(row, 15) / 0.02);
			g.setColor(c);
			g.fillOval(x - diameter / 2, y - diameter / 2, diameter, diameter);
		}

		//draw large labels for star groups
		Color bigLabelColor = new Color(0, 0, 0, 0.35f);
		drawLabel(g, "Main Sequence",(float) biggerFontSize, bigLabelColor, 0.4f, 3.0f, (float) (Math.PI / 12f));
		drawLabel(g, "Red Giants",(float) largerFontSize, bigLabelColor, 1.1f, 1.0f, 0f);
		drawLabel(g, "Red Supergiants",(float) largerFontSize, bigLabelColor, 1.5f, -2.0f, 0f);
		drawLabel(g, "Blue Giants",(float) largerFontSize, bigLabelColor, -0.1f, -7f, 0f);
		drawLabel(g, "White Dwarfs",(float) largerFontSize, bigLabelColor, 0.05f, 12f, 0f);
		drawLabel(g, "Color Index",(float) bigFontSize, bigLabelColor, 0.7f, 17.5f, 0f);
		drawLabel(g, "Absolute Magnitude",(float) bigFontSize, bigLabelColor, -0.32f,0f, (float) (Math.PI / 2f));
	}


	/**
	 * Read star magnitudes and spectral frequencies from source data file.
	 */
	protected void readData(){
	  //read the data file
		try {
		  table = new Table(new PApplet(),"data/stars/HYG-Stars(clipped).csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		table.removeTitleRow();
		//identify the min and max color indexes and magnitudes
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			float colorIndex = table.getFloat(row, 13);
			if(!Float.isNaN(colorIndex)){
				minColorIndex = Math.min(minColorIndex, colorIndex);
				maxColorIndex = Math.max(maxColorIndex, colorIndex);
			}
			float absMag = table.getFloat(row, 11);
			if(!Float.isNaN(absMag)){
				minAbsMag = Math.min(minAbsMag, absMag);  
				maxAbsMag = Math.max(maxAbsMag, absMag);
			}
		}
	}

	
	/**
   * Convert a color index to a position in X.
   * @param colorIndex
   * @return
   */
  private int colorIndexToX(float colorIndex){
  	return (int)((colorIndex - minColorIndex) / 
  			(maxColorIndex - minColorIndex) * getSimulatedDisplay().getXResolution());
  }

  /**
   * Convert a magnitude to a position in Y.
   * @param absMag
   * @return
   */
  private int absMagToY(float absMag){
  	return (int)((absMag - minAbsMag) / 
  			(maxAbsMag - minAbsMag) * getSimulatedDisplay().getYResolution());
  }

  /**
	 * Draws a star label using the current settings.
	 * @param g 
	 * @param labelText
	 * @param fontSize
	 * @param fontColor
	 * @param colorIndex
	 * @param absMag
	 * @param rotation
	 */
	private void drawLabel(Graphics2D g, String labelText, 
			float fontSize, Color fontColor, float colorIndex, float absMag, float rotation) {
		Font largeFont = g.getFont().deriveFont(fontSize);
		g.setFont(largeFont);
		g.setColor(fontColor);
		int x = colorIndexToX(colorIndex);
		int y = absMagToY(absMag);
		
		if(rotation != 0.0f){
			AffineTransform orig = g.getTransform();
			g.translate(x, y);
			g.rotate(rotation);
			g.drawString(labelText, 0, 0);
			g.setTransform(orig);
		}
		else g.drawString(labelText, x, y);
	}
}
