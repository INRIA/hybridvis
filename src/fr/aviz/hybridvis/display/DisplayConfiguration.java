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

package fr.aviz.hybridvis.display;

import fr.aviz.hybridvis.models.FrequenciesAndAngles;
import fr.aviz.hybridvis.utils.settings.Configurable;
import fr.aviz.hybridvis.utils.settings.Settings;

/**
 * 
 * @author dragice
 *
 */
public class DisplayConfiguration extends Settings {

	public static final double MM = 1/1000.0;
	public static final double CM = 1/100.0;
	
	int xResolution; // in pixels 
	int yResolution; // in pixels
	int xTiles = 1;
	int yTiles = 1;
	double pixelWidth = 0.250 * MM; // in meters (SI)
	double pixelHeight = 0.250 * MM; // in meters (SI)
	
	//FIXME:
	//Let Bezel size be set in display configuration!
	
	@Configurable(label="Viewer distance (m)", min=10 * CM, max=20, majorTick=2, pow=2)
	double viewerDistance = 70 * CM; // in meters (SI)
	
	boolean initialized = false;

	public DisplayConfiguration(int xResolution, int yResolution) {
		super();
		init(xResolution, yResolution, xTiles, yTiles, pixelWidth, pixelHeight);
	}

	public DisplayConfiguration(int xResolution, int yResolution, int xTiles, int yTiles, double pixelWidth, double pixelHeight) {
		super();
		init(xResolution, yResolution, xTiles, yTiles, pixelWidth, pixelHeight);
	}
	
	public DisplayConfiguration(DisplayConfiguration conf) {
		super();
		init(conf.xResolution, conf.yResolution, conf.xTiles, conf.yTiles, conf.pixelWidth, conf.pixelHeight);
	}

	protected DisplayConfiguration() {
		super();
	}
		
	protected void init(int xResolution, int yResolution, int xTiles, int yTiles, double pixelWidth, double pixelHeight) {
		this.xResolution = xResolution;
		this.yResolution = yResolution;
		this.xTiles = xTiles;
		this.yTiles = yTiles;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
		initialized = true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}

	public int getXResolution() {
		return xResolution;
	}

	public int getYResolution() {
		return yResolution;
	}
	
	public int getXTiles() {
		return xTiles;
	}

	public int getYTiles() {
		return yTiles;
	}

	/**
	 * @return pixel width in meters.
	 */
	public double getPixelWidth() {
		return pixelWidth;
	}

	/**
	 * @return pixel height in meters.
	 */
	public double getPixelHeight() {
		return pixelHeight;
	}

	/**
	 * @return total display width in meters.
	 */
	public double getMetricWidth() {
		return xResolution * pixelWidth;
	}
	
	/**
	 * @return total display height in meters.
	 */
	public double getMetricHeight() {
		return yResolution * pixelHeight;
	}
	
	public double getMetricDiagonal() {
		double w = getMetricWidth();
		double h = getMetricHeight();
		return Math.sqrt(w*w + h*h);
	}
	
	public int getTileXResolution() {
		return xResolution / xTiles;
	}

	public int getTileYResolution() {
		return yResolution / yTiles;
	}
	
	public double getTileMetricWidth() {
		return getTileXResolution() * pixelWidth;
	}

	public double getTileMetricHeight() {
		return getTileYResolution() * pixelHeight;
	}

	public void setViewerDistance(double viewerDistance) {
		this.viewerDistance = viewerDistance;
		fireSettingsChangedFromCode();
	}

	public double getViewerDistance() {
		return viewerDistance;
	}
	
	public double pixelsToWidth(double pixels) {
		return pixels * pixelWidth;
	}
		
	public double widthToPixels(double width) {
		return width / pixelWidth;
	}

	public double pixelsToHeight(double pixels) {
		return pixels * pixelHeight;
	}
		
	public double heightToPixels(double height) {
		return height / pixelHeight;
	}

	/**
	 * @return the horizontal visual angle occupied by this display given the current viewer distance, in radians.
	 */
	public double getHorizontalVisualAngleRadians() {
		return FrequenciesAndAngles.getVisualAngleRadians(getMetricWidth(), getViewerDistance()); 
	}

	/**
	 * @return the horizontal visual angle occupied by this display given the current viewer distance, in radians.
	 */
	public double getVerticalVisualAngleRadians() {
		return FrequenciesAndAngles.getVisualAngleRadians(getMetricHeight(), getViewerDistance()); 
	}

	/**
	 * @return the horizontal visual angle occupied by a pixel given the current viewer distance, in radians.
	 */
	public double getHorizontalPixelVisualAngleRadians() {
		return FrequenciesAndAngles.getVisualAngleRadians(getPixelWidth(), getViewerDistance()); 
	}

	/**
	 * @return the horizontal visual angle occupied by a pixel given the current viewer distance, in radians.
	 */
	public double getVerticalPixelVisualAngleRadians() {
		return FrequenciesAndAngles.getVisualAngleRadians(getPixelHeight(), getViewerDistance()); 
	}
	
	public double getDistanceForHorizontalVisualAngleRadians(double angle) {
		return FrequenciesAndAngles.getDistanceForVisualAngleRadians(getMetricWidth(), angle);
	}

	public double getDistanceForVerticalVisualAngleRadians(double angle) {
		return FrequenciesAndAngles.getDistanceForVisualAngleRadians(getMetricHeight(), angle);
	}
	
	public double getMetricSizeForVisualAngleRadians(double angle) {
		return FrequenciesAndAngles.getSizeForVisualAngleRadians(getViewerDistance(), angle);
	}

	public double getXPixelsForVisualAngleRadians(double angle) {
		return getMetricSizeForVisualAngleRadians(angle) / pixelWidth;
	}

	public double getYPixelsForVisualAngleRadians(double angle) {
		return getMetricSizeForVisualAngleRadians(angle) / pixelHeight;
	}

	public double getTotalMetricArea() {
		return getMetricWidth() * getMetricHeight();
	}
	
	public double getVisibleMetricArea(double hangle, double vangle) {
		double w = FrequenciesAndAngles.getSizeForVisualAngleRadians(getViewerDistance(), hangle);
		double maxw = getMetricWidth();
		if (w > maxw)
			w = maxw;
		double h = FrequenciesAndAngles.getSizeForVisualAngleRadians(getViewerDistance(), vangle);
		double maxh = getMetricHeight();
		if (h > maxh)
			h = maxh;
		double area = w * h;
		return area;
	}


	@Override
	public String toString() {
		String s = "Display configuration:\n";
		s += "  Resolution " + xResolution + " x " + yResolution + " pixels\n"; 
		s += "  Pixel size " + (pixelWidth / MM) + " x " + (pixelHeight / MM) + " mm\n";
		return s;
	}	
}
