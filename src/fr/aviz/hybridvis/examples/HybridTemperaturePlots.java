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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.utils.FontUtils;
import fr.aviz.hybridvis.utils.GradientColorScale;


/**
 * This class draws temperature plots for a couple of cities
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
 * @author wwillett (main code), pisenberg (added display of monthly averages)
 * 
 */
public class HybridTemperaturePlots extends HybridImageRenderer {

	static class DataPoint {
		String station;
		String country;
		String date;
		Calendar calendarDate;
		float temperature;
		float lat;
		float lon;
	}

	protected Hashtable<String, ArrayList<DataPoint>> pointsByStation = new Hashtable<String, ArrayList<DataPoint>>();
	protected GradientColorScale lineChartColorGradient = new GradientColorScale();
	protected GradientColorScale barChartColorGradient = new GradientColorScale();

	int minYear = 1990;
	int maxYear = 2013;
	//Scale bounds for the line charts
	double minTemp = -390;
	double maxTemp = 260;
	//Scale bounds for the bar charts
	double minTempChange = -90;
	double maxTempChange = 140;

	double maxScaleTemp = Math.max(Math.abs(minTemp), Math.abs(maxTemp));

	//switch to monthly derivations from the average view
	boolean showSlopeAverages = false;
	boolean scalePerStation = false; // if we want each station to have an individual scale -- currently only implemented for showSlopeAverages = false

	SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM");

	//max size gap to connect across missing data point
	int daysBeforeGap = 5;

	//Formatting
	DecimalFormat df = new DecimalFormat("#.##");
	String MONTHS = "JFMAMJJASOND";
	Integer[] stationOrder = new Integer[]{};/*{29,31,10,15,21,18,16,26,
			28,20, 1,17,11,14,25, 6,
			30, 23, 7,2, 4,12, 3,22,
			5, 8, 9,24,27, 0,19,13};*/

	public static void main(String[] args) {
		HybridTemperaturePlots viewer = new HybridTemperaturePlots();
		viewer.showOnScreen();
	}

	public HybridTemperaturePlots() { 

		super(); 
		//set up color scales
		lineChartColorGradient.addColor((float)minTemp, Color.getHSBColor(0.5f, 1.0f, 0.85f));  //Blue 
		lineChartColorGradient.addColor((float)minTemp/2, Color.getHSBColor(0.5f, 1.0f, 0.85f));  //Cyan 
		lineChartColorGradient.addColor(0.0f, Color.darkGray);   //Gray
		lineChartColorGradient.addColor((float)maxTemp/2, Color.getHSBColor(0.08f, 1.0f, 0.85f));  //Orange
		lineChartColorGradient.addColor((float)maxTemp, Color.getHSBColor(0.08f, 1.0f, 0.85f));  //Rust

		barChartColorGradient.addColor((float)minTempChange, Color.getHSBColor(0.6f, 1.0f, 0.85f));  //Blue 
		barChartColorGradient.addColor((float)minTempChange/2, Color.getHSBColor(0.5f, 1.0f, 0.85f));  //Cyan 
		barChartColorGradient.addColor(0.0f, Color.darkGray);   //Gray
		barChartColorGradient.addColor((float)maxTempChange/2, Color.getHSBColor(0.08f, 1.0f, 0.85f));  //Orange
		barChartColorGradient.addColor((float)maxTempChange, Color.getHSBColor(0.0f, 1.0f, 0.85f));  //Rust


		//loadData("data/Selected-Temperature-Data(1900-2013).short.txt");
		//loadData("data/temperatures/Selected-Temperature-Data(1990-2013).txt");
		loadData("data/temperatures/Selected-Temperature-Data(1990-2013)x75.csv");
		setHipassRadius(6);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setBlurRadius(12); //25
		setFarImageOpacity(0.4);
		setPostContrast(1.4);
		setPostBrightness(0.85);
	}

	/**
	 * Loads the Temperature data.
	 */
	public void loadData(String dataFile) {
		System.out.print("Loading data... ");
		int l = 0;
		try {
			File appBase = new File("."); //current directory
			String path = appBase.getAbsolutePath();
			System.out.println(path);
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			String line = br.readLine();

			while (line != null) {
				String[] cells = line.split(",");
				if (l >= 2) {
					DataPoint p = new DataPoint();
					p.station = cells[11];
					p.country = cells[1];
					p.date = cells[2];
					
					String[] dateParts = p.date.split("/");
					int year = Integer.parseInt(dateParts[0].trim());
					int month = Integer.parseInt(dateParts[1]) - 1;
					int day = Integer.parseInt(dateParts[2]);
					p.calendarDate = Calendar.getInstance();
					p.calendarDate.set(year,month,day);
					p.temperature = Float.parseFloat(cells[12]);

					p.lat = Float.parseFloat(cells[5]);
					p.lon = Float.parseFloat(cells[7]);
					
					if(!pointsByStation.containsKey(p.station)){
						pointsByStation.put(p.station, new ArrayList<HybridTemperaturePlots.DataPoint>());
					}
					ArrayList<HybridTemperaturePlots.DataPoint> list = pointsByStation.get(p.station);
					list.add(p);
				}
				line = br.readLine();
				l++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Loaded " + l + " points from " + pointsByStation.size() + " stations.");
		
		stationOrder = computeStationOrder(pointsByStation);
	}

	
	
	/**
	 * Decide where to position each chart based on lat/lon of the stations. 
	 * @param pointsByStation
	 * @return
	 */
	Integer[] computeStationOrder(Hashtable<String, ArrayList<DataPoint>> pointsByStation){
	  
	  int optimizationIterations = 100000;
	  int rows = getSimulatedDisplay().getYTiles();
	  int cols = getSimulatedDisplay().getXTiles();
	  Integer[] stationOrder = new Integer[rows*cols];
	  for(int i=0;i<rows*cols;i++) stationOrder[i] = i;
	  
	  Object[] pointsByStationArray = pointsByStation.values().toArray();
	  
	  Random rand = new Random();
	  
	  //Use a super-primitive iterative sort routine
	  for(int j=0;j<optimizationIterations;j++){
	    
	    //get two elements
	    int randRowA = rand.nextInt(rows);
	    int randColA = rand.nextInt(cols);
	    int randIdxA = randRowA * cols + randColA;
	    int rowOffset = rand.nextInt(3) - 1;
	    int colOffset = rand.nextInt(3) - 1;
	    int randRowB = (int)Math.max(0, Math.min(rows-1, randRowA + rowOffset));
	    int randColB = (int)Math.max(0, Math.min(cols-1, randColA + colOffset));
      int randIdxB = randRowB * cols + randColB;
      int redirectA = stationOrder[randIdxA];
      int redirectB = stationOrder[randIdxB];
      
      float rAlat = ((ArrayList<DataPoint>)pointsByStationArray[redirectA]).get(0).lat;
      float rBlat = ((ArrayList<DataPoint>)pointsByStationArray[redirectB]).get(0).lat;
      float rAlon = ((ArrayList<DataPoint>)pointsByStationArray[redirectA]).get(0).lon;
      float rBlon = ((ArrayList<DataPoint>)pointsByStationArray[redirectB]).get(0).lon;
	    
      if((rAlon > rBlon && randColA < randColB) || (rAlat > rBlat && randRowA > randRowB)){
        stationOrder[randIdxA] = redirectB;
        stationOrder[randIdxB] = redirectA;
        
      }
	  }
	  return stationOrder;
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
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution());

		g.setColor(Color.white);
		g.fill(wallbounds);  // hipass requires an opaque image
		
		Iterator<ArrayList<DataPoint>> pointsIterator = pointsByStation.values().iterator();
		int si = 0;
		while(pointsIterator.hasNext()){
			ArrayList<DataPoint> stationData = pointsIterator.next();
			
			int reordered_si = java.util.Arrays.asList(stationOrder).indexOf(si);
			if(reordered_si == -1) reordered_si = si;

			Rectangle2D.Double bounds = new Rectangle2D.Double(
					(reordered_si % 15) * getSimulatedDisplay().getTileXResolution(),
					Math.floor(reordered_si / 15) * getSimulatedDisplay().getTileYResolution(),
					getSimulatedDisplay().getTileXResolution(),
					getSimulatedDisplay().getTileYResolution());
			drawLineChartForStation(g, bounds, stationData);
			si++;
		}
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
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution());

		g.setColor(Color.white);
		g.fill(wallbounds);

		Iterator<ArrayList<DataPoint>> pointsIterator = pointsByStation.values().iterator();
    int si = 0;
    while(pointsIterator.hasNext()){
      ArrayList<DataPoint> stationData = pointsIterator.next();

	    int reordered_si = java.util.Arrays.asList(stationOrder).indexOf(si);
      if(reordered_si == -1) reordered_si = si;

			Rectangle2D.Double bounds = new Rectangle2D.Double(
					(reordered_si % getSimulatedDisplay().getXTiles()) * getSimulatedDisplay().getTileXResolution(),
					Math.floor(reordered_si / getSimulatedDisplay().getXTiles()) * getSimulatedDisplay().getTileYResolution(),
					getSimulatedDisplay().getTileXResolution(),
					getSimulatedDisplay().getTileYResolution());
			drawBarChartForStation(g, bounds, stationData);
			si++;
		}

	}

	protected LinkedHashMap<String, Float> monthlyAverages(ArrayList<DataPoint> stationData){
		//find the average temperature for each month of each year
		Hashtable<String,Float> monthSums = new Hashtable<String,Float>();
		Hashtable<String,Integer> monthCounts = new Hashtable<String,Integer>();
		LinkedHashMap<String,Float> monthAverages = new LinkedHashMap<String,Float>();

		for(DataPoint point: stationData){
			int intMonth = point.calendarDate.get(Calendar.MONTH);

			String month = (((intMonth < 10) ? "0" : "")  + intMonth); //prints 01, 02, ...

			//Count and sum entries to calculate averages for each month of each year
			if(!monthSums.containsKey(month)){monthSums.put(month, point.temperature);
			monthCounts.put(month, 1);
			}
			else{
				monthSums.put(month, monthSums.get(month) + point.temperature);
				monthCounts.put(month, monthCounts.get(month) + 1);
			}

		}

		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		//Compute Averages for each month of each year
		for(String month: monthSums.keySet()){
			float average = monthSums.get(month) / monthCounts.get(month);
			monthAverages.put(month, average);
			if(average > max) max = average;
			if(average < min) min = average;

		}

		monthAverages.put("MAX", max);
		monthAverages.put("MIN", min);

		return monthAverages;
	}

	protected void drawBarChartForStation(Graphics2D g, Rectangle2D bounds, ArrayList<DataPoint> stationData){


		Hashtable<String, Float> slopeAverages = null;
		LinkedHashMap<String, Float> monthAverages = null;

		if(showSlopeAverages){
			slopeAverages = changeInAverageFromFirstToLast(stationData);
		}
		else{
			monthAverages = monthlyAverages(stationData);	
		}

		double margin = 0.09 * getSimulatedDisplay().getTileXResolution();
		double barPadding = 0.015 * getSimulatedDisplay().getTileYResolution();

		int barWidth = (int) ((bounds.getWidth() - 2 * margin) / 12);

		String monthWithSmallestBar = "00";
		String monthWithLargestBar = "00";
		
		//find a font size that doesn't exceed the bar width
		Font mediumFont = g.getFont().deriveFont(55f);
		float fontSize = FontUtils.findFontSizeForWidth(g, "M..", mediumFont, barWidth);
		mediumFont = g.getFont().deriveFont(fontSize);
		
		g.setFont(mediumFont);
		FontMetrics fmedium = getFontMetrics(mediumFont);

		//draw bars
		for(int m = 0; m < 12; m++){
			String month = ((m < 10) ? "0" : "")  + m;

			if(showSlopeAverages){
				//this is Wes' original code
				if(slopeAverages.containsKey(month)){
					int barX = (int)(bounds.getX() + (m) * barWidth + margin + barPadding);
					float averageChange = slopeAverages.get(month);
					if(averageChange > slopeAverages.get(monthWithLargestBar)) monthWithLargestBar = month;
					if(averageChange < slopeAverages.get(monthWithSmallestBar)) monthWithSmallestBar = month; 

					int barHeight = (int) (Math.abs(averageChange) / (maxTempChange - minTempChange) * 
							(bounds.getHeight() - 2 * margin));
					int barY = (int)(bounds.getY() + margin + 
							(bounds.getHeight() - 2 * margin) *
							(maxTempChange - (averageChange > 0 ? averageChange : 0 )) /
							(maxTempChange - minTempChange));
					g.setColor(barChartColorGradient.getColor(averageChange, 1f));
					g.fillRect(barX, barY, (int) (barWidth - 2 * barPadding), barHeight);
				}
			}
			else{
				//this is for displaying the monthly averages per station
				float max = monthAverages.get("MAX");
				float min = monthAverages.get("MIN");
				float absMax = Math.max(Math.abs(max), Math.abs(min));
				if(scalePerStation) maxScaleTemp = absMax;


				if(monthAverages.containsKey(month)){


					int barX = (int)(bounds.getX() + (m) * barWidth + margin + barPadding);
					float average = monthAverages.get(month);
					if(average > monthAverages.get(monthWithLargestBar)) monthWithLargestBar = month;
					if(average < monthAverages.get(monthWithSmallestBar)) monthWithSmallestBar = month; 

					double halfDrawingHeight = (0.5 * (bounds.getHeight() - 2 * margin));
					int barHeight = (int)(( halfDrawingHeight / maxScaleTemp) * Math.abs(average)  );

					int barY = 0;
					if(average > 0){
						barY = (int) (bounds.getY() + margin + (bounds.getHeight() - 2 * margin) * 0.5 - barHeight);
					}
					else{
						barY = (int) (bounds.getY() + margin + (bounds.getHeight() - 2 * margin) * 0.5);
					}

					g.setColor(barChartColorGradient.getColor(average, 1f));
					g.fillRect(barX, barY, (int)(barWidth - 2 * barPadding), barHeight);
				}
			}


			String monthLabel = MONTHS.substring(m, m + 1);


			//Month labels
			g.setColor(new Color(0f,0f,0f,0.5f));

			double textYLocation = 0;
			if(showSlopeAverages){
				textYLocation = (int)(bounds.getY() + margin + (bounds.getHeight() - 2 * margin) *
						maxTempChange / (maxTempChange - minTempChange) + 100);
			}
			else{
				textYLocation = (bounds.getY() + margin + (bounds.getHeight() - 2 * margin) * 0.5 + fmedium.getHeight());
			}

			g.drawString(monthLabel, 
					//(int)(bounds.getX() + (m) * barWidth + margin + barPadding + 30),
					(int)(bounds.getX() + m * (barWidth ) + barPadding + margin),
					(int)textYLocation);
		}


		//label biggest and smallest bars

		for(String month : new String[]{monthWithSmallestBar, monthWithLargestBar}){

			int barX = (int)(bounds.getX() + Integer.parseInt(month) * barWidth + margin);

			float displayAverage = 0;
			int barY = 0;

			if(showSlopeAverages){
				displayAverage = slopeAverages.get(month);

				int barHeight = (int) (Math.abs(displayAverage) / (maxTempChange - minTempChange) * 
						(bounds.getHeight() - 2 * margin));
				barY = (int)(bounds.getY() + margin + 
						(bounds.getHeight() - 2 * margin) *
						(maxTempChange -  (displayAverage > 0 ? displayAverage : 0 )) /
						(maxTempChange - minTempChange) +
						(month == monthWithSmallestBar ? barHeight + getSimulatedDisplay().getTileYResolution() * 0.018 : -getSimulatedDisplay().getTileYResolution() * 0.018)) ;
			}
			else{

				//TODO
				displayAverage = monthAverages.get(month);
				double halfDrawingHeight = (0.5 * (bounds.getHeight() - 2 * margin));

				float max = monthAverages.get("MAX");
				float min = monthAverages.get("MIN");
				float absMax = Math.max(Math.abs(max), Math.abs(min));
				if(scalePerStation) maxScaleTemp = absMax;

				int barHeight = (int)(( halfDrawingHeight / maxScaleTemp) * Math.abs(displayAverage)  );

				if(displayAverage > 0){
					barY = (int) (bounds.getY() + margin + (bounds.getHeight() - 2 * margin) * 0.5 - barHeight - getSimulatedDisplay().getTileYResolution() * 0.018);
				}
				else{
					barY = (int) (bounds.getY() + margin + (bounds.getHeight() - 2 * margin) * 0.5 + barHeight + getSimulatedDisplay().getTileYResolution() * 0.018);
				}


			}

			g.setStroke(new BasicStroke(10));
			g.drawLine(barX, barY, barX + barWidth, barY);
			g.drawString(df.format(displayAverage / 10) + "C", barX, 
					barY + (displayAverage < 0 ? (int)(getSimulatedDisplay().getTileYResolution() * 0.0625) : (int)(-getSimulatedDisplay().getTileYResolution() * 0.025)));
		}

		
		//also set a maximum Font height
		//a heuristic here. 1/20 of a tile
		

			
		//Big Station Label
		Font largeFont = g.getFont().deriveFont((getSimulatedDisplay().getTileYResolution() / 10.0f));
		DataPoint dataPoint = stationData.get(0);
		
		int fontSizeStation = FontUtils.findFontSizeForWidth(g, dataPoint.station, largeFont, getSimulatedDisplay().getTileXResolution() - getSimulatedDisplay().getTileXResolution() / 5.0);
		int fontSizeCountry = FontUtils.findFontSizeForWidth(g, dataPoint.country, largeFont, getSimulatedDisplay().getTileXResolution() - getSimulatedDisplay().getTileXResolution() / 5.0);
		//System.out.println(fontSizeStation + " " + fontSizeCountry);

		largeFont = g.getFont().deriveFont((float)Math.min(fontSizeStation, fontSizeCountry));
		g.setFont(largeFont);
		FontMetrics fm = getFontMetrics(largeFont);
		
		
		//System.out.println(fm.getFont().getSize());
		
		//FIXME: Substract Bezel Size from Y-position
		g.drawString(dataPoint.station, 
				(int)(bounds.getCenterX() - fm.stringWidth(dataPoint.station) / 2), 
				(int)(bounds.getCenterY() + bounds.getHeight() / 2.0 - fm.getHeight() - fm.getDescent() - margin));
		g.drawString(dataPoint.country, 
				(int)(bounds.getCenterX() - fm.stringWidth(dataPoint.country) / 2), 
				(int)(bounds.getCenterY() + bounds.getHeight() / 2.0 - fm.getDescent() - margin));
	}


	protected void drawLineChartForStation(Graphics2D g, Rectangle2D bounds, ArrayList<DataPoint> stationData){
		int margin = (int)(0.09 * getSimulatedDisplay().getTileXResolution());

		BasicStroke thinStroke = new BasicStroke(1.0f);
		BasicStroke dashedStroke = new BasicStroke(1.0f,
				BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER,
				5.0f, new float[]{2.0f}, 1.0f);

		int lastX = -1, lastY = -1, lastDay = -1, lastYear = -1;

		int yearHeight = (int)((bounds.getHeight() - 2.0 * margin)/(maxYear - minYear));

		int currentMonth = 1;
		float currentMonthSum = 0;
		float currentMonthCount = 0;

		Font mediumFont = g.getFont().deriveFont(12f);
		Font smallFont = g.getFont().deriveFont(8f);
		Font smallerFont = g.getFont().deriveFont(6f);
		
		
		FontMetrics fmMedium = getFontMetrics(mediumFont);
		FontMetrics fmSmall = getFontMetrics(smallFont);
		g.setFont(smallerFont);

		//draw line charts
		for(DataPoint p: stationData){
			int day = p.calendarDate.get(Calendar.DAY_OF_YEAR);
			int month = p.calendarDate.get(Calendar.MONTH);

			//draw the main chart line
			int x = (int)(day/366.0 * 
					(bounds.getWidth() - 2.0 * margin) + margin + bounds.getX());
			int year = p.calendarDate.get(Calendar.YEAR);
			int y = (int)((year - minYear * 1.0)*yearHeight + margin + bounds.getY()); //baseline
			y = y + (int)((1.0 - (p.temperature - minTemp) / (maxTemp - minTemp)) * yearHeight); //height
			
			if(lastYear == year && day > lastDay && day - daysBeforeGap < lastDay){
				g.setStroke(thinStroke);
				g.setColor(lineChartColorGradient.getColor(p.temperature, 1f));
				
				g.drawLine(lastX, lastY, x, y);
			}

			//at the end of the month, label the monthly average
			if(month != currentMonth){

				float currentMonthAverage = currentMonthSum / currentMonthCount;
				Calendar lastMonth = Calendar.getInstance();
				lastMonth.set(lastYear, currentMonth, 1);

				currentMonth = month;
				currentMonthSum = 0;
				currentMonthCount = 0;

				int avgX = (int)( lastMonth.get(Calendar.DAY_OF_YEAR) / 366.0 *
						(bounds.getWidth() - 2.0 * margin) + margin + bounds.getX());
				int avgWidth = (int) ((bounds.getWidth() - 2.0 * margin)/12.0);
				int avgY = (int)((lastYear - minYear * 1.0)*yearHeight + margin + bounds.getY()); //baseline
				avgY = avgY + (int)((1.0 - (currentMonthAverage - minTemp) / (maxTemp - minTemp)) * yearHeight); //height

				g.setStroke(dashedStroke);
				g.setColor(lineChartColorGradient.getColor(currentMonthAverage, 0.2f));
				g.drawLine(avgX, avgY, avgX + avgWidth, avgY);
				g.drawString(df.format((currentMonthAverage / 10)) + "C", 
						avgX + avgWidth / 2 - 10 , avgY);

				//also put a label for the month in the line chart
				if(year != minYear){
	  				Calendar c = Calendar.getInstance();
	  				c.set(lastYear, month, 1);
	  				String monthName = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
	  				g.setFont(smallFont);
	  				g.setColor(new Color(0f,0f,0f,0.2f));
	  				
	  				int xs = (int) (bounds.getX() + margin + month * avgWidth + avgWidth * 0.5 - fmSmall.stringWidth(monthName) * 0.5);
	  				g.drawString(monthName, xs ,  (int)((year - minYear * 1.0) * yearHeight + margin + bounds.getY() - 0.5 * yearHeight + 1.5 * fmSmall.getAscent()));
				}
			}
			currentMonthSum += p.temperature;
			currentMonthCount++;

			//Save the last point for line drawing
			if(day > lastDay || year > lastYear){ //control for weirdness around leap year?
				lastX = x;
				lastY = y;
				lastDay = day;
				lastYear = year;
			}
		} 

		//draw grid
		Calendar c = Calendar.getInstance();
		g.setStroke(new BasicStroke(1));
		g.setColor(new Color(0f,0f,0f,0.2f));

		//Vertical lines for months
		for(int i=1; i <= 12; i++){
			c.set(Calendar.MONTH, i);
			c.set(Calendar.DAY_OF_MONTH, 1);
			
			int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
			int monthX = (int) (dayOfYear/366f * (bounds.getWidth() - 2 * margin) + bounds.getX() + margin);
			g.drawLine(monthX, (int)bounds.getY(), monthX, (int)(bounds.getY()+bounds.getHeight()));
			
			g.setFont(mediumFont);
			String monthName = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
			g.drawString(monthName, 
					(int) (monthX + (bounds.getWidth() - 2 * margin)/12/2 - fmMedium.stringWidth(monthName)/2) , 
					(int)bounds.getY() + margin - mediumFont.getSize());
		}

		//Horizontal baselines for years
		g.setStroke(new BasicStroke(1));
		//lines for temperature across each year
		for(int j=0; j <= maxYear - minYear; j++){

			// main line at 0C
			int yearY = j * yearHeight + (int)((1.0 - (0 - minTemp) / (maxTemp - minTemp)) * yearHeight);
			int y = (int)(bounds.getY() + margin + yearY);
			g.setColor(new Color(0f,0f,0f,0.2f));
			g.drawLine((int)bounds.getX() + margin, y, (int)(bounds.getX() + bounds.getWidth() - margin), y);
			g.setFont(smallFont);

			//reference lines with labels
			for(Float degreeLabel: new Float[]{0f, -150f, 150f}){
				String temperatureLabel = (int)(degreeLabel/10) + "C";
				g.setColor(new Color(0f,0f,0f,0.05f));
				int temperatureLabelY = (int)(y - degreeLabel/(maxTemp-minTemp)*yearHeight);
				g.drawLine((int)bounds.getX() + margin, temperatureLabelY, 
						(int)(bounds.getX() + bounds.getWidth() - margin), temperatureLabelY);

				g.setColor(new Color(0f,0f,0f,0.5f));
				g.drawString(temperatureLabel, 
						(int)(bounds.getX() + margin - fmSmall.stringWidth(temperatureLabel)), 
						temperatureLabelY + 3);
			}
			g.setColor(new Color(0f,0f,0f,0.5f));
			g.setFont(mediumFont);
			g.drawString(""+(minYear+j), (int)(bounds.getX() + margin - 75), y + 7);
		}
	}


	protected Hashtable<String, Float> changeInAverageFromFirstToLast(
			ArrayList<DataPoint> stationData) {
		//find the average temperature for each month of each year
		Hashtable<String,Float> yearAndMonthSums = new Hashtable<String,Float>();
		Hashtable<String,Integer> yearAndMonthCounts = new Hashtable<String,Integer>();
		LinkedHashMap<String,Float> yearAndMonthAverages = new LinkedHashMap<String,Float>();

		Hashtable<Integer, Integer> firstYearWithMonth = new Hashtable<Integer, Integer>();
		Hashtable<Integer, Integer> lastYearWithMonth = new Hashtable<Integer, Integer>();

		for(DataPoint point: stationData){
			int intMonth = point.calendarDate.get(Calendar.MONTH);
			int intYear = point.calendarDate.get(Calendar.YEAR);
			String yearAndMonth = (point.calendarDate.get(Calendar.YEAR) + "_" + 
					((intMonth < 10) ? "0" : "")  + intMonth);
			//Count and sum entries to calculate averages for each month of each year
			if(!yearAndMonthSums.containsKey(yearAndMonth)){
				yearAndMonthSums.put(yearAndMonth, point.temperature);
				yearAndMonthCounts.put(yearAndMonth, 1);
			}
			else{
				yearAndMonthSums.put(yearAndMonth, yearAndMonthSums.get(yearAndMonth) + point.temperature);
				yearAndMonthCounts.put(yearAndMonth, yearAndMonthCounts.get(yearAndMonth) + 1);
			}
			//Track the first and last years a month appears in
			if(!firstYearWithMonth.containsKey(intMonth) || 
					intYear < firstYearWithMonth.get(intMonth))
				firstYearWithMonth.put(intMonth, intYear);
			if(!lastYearWithMonth.containsKey(intMonth) || 
					intYear > lastYearWithMonth.get(intMonth))
				lastYearWithMonth.put(intMonth, intYear);
		}
		//Compute Averages for each month of each year
		for(String yearAndMonth: yearAndMonthSums.keySet()){
			yearAndMonthAverages.put(yearAndMonth, yearAndMonthSums.get(yearAndMonth) / 
					yearAndMonthCounts.get(yearAndMonth));
		}

		//For each month, compute the difference between the last year and the average for the previous years 
		Hashtable<String, Float> averageChanges = new Hashtable<String, Float>();
		for(int month: firstYearWithMonth.keySet()){
			if(lastYearWithMonth.containsKey(month)){
				String strMonth = ((month < 10) ? "0" : "") + month;
				String firstYearAndMonth = firstYearWithMonth.get(month) + "_" + strMonth;
				String lastYearAndMonth = lastYearWithMonth.get(month) + "_" + strMonth;
				float firstYearAverage = yearAndMonthAverages.get(firstYearAndMonth);
				float lastYearAverage = yearAndMonthAverages.get(lastYearAndMonth);

				//get average across all years
				float sumOfAverages = 0;
				float countOfAverages = 0;
				int firstYear = firstYearWithMonth.get(month);
				int lastYear = lastYearWithMonth.get(month);
				for(int year = firstYear; year <= lastYear; year++){
					String yearAndMonth = year + "_" + strMonth;
					if(yearAndMonthAverages.containsKey(yearAndMonth)){
						sumOfAverages += yearAndMonthAverages.get(yearAndMonth);
						countOfAverages++;
					}
				}
				float allYearsAverage = sumOfAverages / countOfAverages;
				//averageChanges.put(strMonth, (lastYearAverage - firstYearAverage));
				averageChanges.put(strMonth, (lastYearAverage - allYearsAverage));
			}
		}

		return averageChanges;

	}




}
