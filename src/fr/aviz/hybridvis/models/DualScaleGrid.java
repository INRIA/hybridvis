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

package fr.aviz.hybridvis.models;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.text.DecimalFormat;

import fr.aviz.hybridvis.display.DisplayConfiguration;

/**
 * Dual-scale tile-aligned grid renderer for large displays.
 * @author wjwillett
 *
 */
public class DualScaleGrid {

  public double nearDistance = 0.6; 
  public double farDistance = 4.0;
  public double blurRadius = 5.0;
  public Rectangle2D.Double rect = null;

  public boolean nearLabels = true;
  public boolean farLabels = true;
  public boolean nearLabelsPerScreen = true;
  public boolean farLabelsPerScreen = false;

  public int farLinesPerDisplayVertical = 4;
  public int farLinesPerDisplayHorizontal = 4;
  public int nearLinesPerFar = 4;

  public Color gridColor = new Color(0,0,0,0.1f);
  public Color textColor = Color.gray;

  public double[] xRange = null;
  public String xFormat = "#####.###";
  public double[] yRange = null;
  public String yFormat = "#####.###";  


  protected DisplayConfiguration display = null;

  /**
   * @param display
   */
  public DualScaleGrid(DisplayConfiguration display) {
    this.display = display;
  }


  /**
   * @param display
   * @param rect
   * @param nearDistance
   * @param farDistance
   * @param blurRadius
   * @param xRange
   * @param yRange
   */
  public DualScaleGrid(DisplayConfiguration display, Double rect,
      double nearDistance, double farDistance, double blurRadius, double[] xRange,
      double[] yRange) {
    super();
    this.nearDistance = nearDistance;
    this.farDistance = farDistance;
    this.blurRadius = blurRadius;
    this.rect = rect;
    this.xRange = xRange;
    this.xFormat = xFormat;
    this.yRange = yRange;
    this.yFormat = yFormat;
    this.display = display;
  }


  /**
   * Draws a grid that should be visible from the near viewing distance.
   * @param g
   */
  public void drawNear(Graphics2D g){
    int strokeWidth = getStrokeWidthForDistance(nearDistance, 0);
    drawGrid(g, farLinesPerDisplayHorizontal * nearLinesPerFar * display.getXTiles(), 
        farLinesPerDisplayHorizontal * nearLinesPerFar * display.getYTiles(), 
        strokeWidth, gridColor, textColor, nearLabelsPerScreen, false);
  }


  /**
   * Draws a grid that should be visible from the far viewing distance.
   * @param g
   */
  public void drawFar(Graphics2D g){
    int strokeWidth = getStrokeWidthForDistance(farDistance, blurRadius);
    drawGrid(g, farLinesPerDisplayHorizontal * display.getXTiles(), 
        farLinesPerDisplayHorizontal * display.getYTiles(), 
        strokeWidth, gridColor, textColor, farLabelsPerScreen, false);

  }


  /**
   * TODO: This is where the secret sauce needs to go to select a line weight based on distance and blur
   * @param distance
   * @param blurRadius
   * @return
   */
  private int getStrokeWidthForDistance(double distance, double blurRadius) {

    
    return 1;
  }

  
  /**
   * 
   * @param value
   * @return
   */
  protected double scaleX(double value){
    value /= rect.width;
    if(xRange == null || xRange.length != 2)
      return value;
    else return value * (xRange[1] - xRange[0]) + xRange[0];
  }

  
  protected double scaleY(double value){
    value /= rect.height;
    if(yRange == null || yRange.length != 2)
      return value;
    else return value * (yRange[1] - yRange[0]) + yRange[0];
  }


  /**
   * Draw the underlying grid.
   * @param g
   * @param xLines
   * @param yLines
   * @param strokeWidth
   * @param gridColor
   * @param textColor
   * @param labelsPerScreen
   * @param useLegibilityModelForTextSize
   */
  protected void drawGrid(Graphics2D g, 
      int xLines, int yLines, int strokeWidth, 
      Color gridColor, Color textColor, boolean labelsPerScreen, 
      boolean useLegibilityModelForTextSize) {

    if(rect == null){
      rect = new Rectangle2D.Double(0, 0, 
          display.getXResolution(), 
          display.getYResolution());
    }

    float fontSize = 0;
    if(labelsPerScreen)
      fontSize = 15.0f * strokeWidth - 5f;
    else fontSize = TextLegibility.getFontSizeForLegibility(
        g.getFont(), true, labelsPerScreen ? 0: blurRadius, 0.15f);

    Font labelFont = g.getFont().deriveFont(fontSize);
    g.setFont(labelFont);
    g.setStroke(new BasicStroke(strokeWidth));

    DecimalFormat xDecimalFormat = new DecimalFormat(xFormat);
    DecimalFormat yDecimalFormat = new DecimalFormat(yFormat);


    int xSpacing = (display.getXResolution()/xLines);
    int ySpacing = (display.getYResolution()/yLines);
    int tileWidth = display.getTileXResolution();
    int tileHeight = display.getTileYResolution();
    int tileCountX = display.getXTiles();
    int tileCountY = display.getYTiles();

    //vertical lines
    g.setColor(gridColor);
    for(int i=0; i < xLines; i++){
      int xPosition = xSpacing*i;
      if(xPosition < rect.x || xPosition > rect.x + rect.width) continue;
      g.drawLine(xPosition, (int)rect.y, xPosition, (int)(rect.y + rect.height));
      //vertical labels
      g.setColor(textColor);
      for(int j=0; j < (labelsPerScreen ? tileCountY : 1); j++){
        g.drawString(yDecimalFormat.format(scaleX(xPosition)), 
        			 xPosition + 10 , (tileCountY - j)*tileHeight - 10);
      }
    }

    //horizontal lines
    g.setColor(gridColor);
    for(int j=0; j < yLines; j++){
      int yPosition = ySpacing*j;
      if(yPosition < rect.y || yPosition > rect.y + rect.height) continue;
      g.drawLine((int)rect.x, yPosition, (int)(rect.x + rect.width), yPosition);
      //horizontal labels
      g.setColor(textColor);
      for(int i=0; i < (labelsPerScreen ? tileCountX : 1); i++){
        g.drawString(xDecimalFormat.format(scaleY(yPosition)), 
            i*tileWidth + 10, yPosition + fontSize + 10);
      }
    }
  }  
}
