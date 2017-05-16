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

import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.display.WILDDisplay;

/**
 * Various conversions between frequencies, visual angles, etc.
 * 
 * @author dragice
 *
 */
public class FrequenciesAndAngles {

	/**
	 * 
	 * Converts cycles per pixel into cycles per degree using the formula from [4], also given in [3] (equation 3). 
	 * 
	 * [4] Kim Y., Cho I., Lee I, Yun T., Park K. T., "Wavelet transform image compression using human visual characteristics and a tree structure with a height attribute, Optical Engineering, Vol. 35, No.1, January 1996, pp. 204-212.]"
	 * 
	 * @param fi The image frequency obtained from Fourier transform, in cycles per pixel
	 * @param d The viewing distance in units of diagonal image size in pixels
	 * @param h The horizontal image size in pixels
	 * @param v the vertical image size in pixels
	 * @return fs, the spatial frequency in cycles per degree of visual angle
	 */
	public static double cyclesPerPixelToCyclesPerDegreeKim(double fi, double d, double h, double v) {
		// compute the viewing distance with respect to the diagonal image size
		return fi * getPixelsPerDegreeKim(d, h, v);
	}
	
	/**
	 * Here same as above but distance is specified in meters rather than units of diagonal image size.
	 * 
	 * @param fi
	 * @param distance
	 * @param conf
	 * @return
	 */
	public static double cyclesPerPixelToCyclesPerDegreeKim(double fi, DisplayConfiguration conf) {
		double diagonal = conf.getMetricDiagonal();
		double d = conf.getViewerDistance()/diagonal;
		return cyclesPerPixelToCyclesPerDegreeKim(fi, d, conf.getXResolution(), conf.getYResolution());
	}
	
	public static double cyclesPerPixelToCyclesPerDegree(double fi, DisplayConfiguration conf) {
		double cyclesPerPixel = fi;
		double pixelsPerCycle = 1.0 / cyclesPerPixel;
		double metersPerCycle = conf.getPixelWidth() * pixelsPerCycle;
		double radiansPerCycle = getVisualAngleRadians(metersPerCycle, conf.getViewerDistance()); 
		double degreesPerCycle = radiansPerCycle / Math.PI * 180;
		double cyclesPerDegree = 1.0 / degreesPerCycle;
		return cyclesPerDegree;
	}
	
	/**
	 * $cpd = \pi/(360 atan(p/{2d} ppc)$
     *
	 */
	public static double pixelsPerCycleToCyclesPerDegree_paperformula(double ppc, DisplayConfiguration conf) {
		double psize = conf.getPixelWidth();
		double d = conf.getViewerDistance();
		double cpd = Math.PI / (2 * 180 * Math.atan(psize / (2 * d) * ppc));
		return cpd;
	}
	
	/**
	 * Reverse of cyclesPerPixelToCyclesPerDegree.
	 * @param cd The image frequency from a given distance in cycles per degree
	 * @param d The viewing distance in units of diagonal image size in pixels.
	 * @param h The horizontal image size in pixels
	 * @param v the vertical image size in pixels
	 * @return the spatial frequency of the image in cycles per pixel
	 */
	public static double cyclesPerDegreeToCyclesPerPixelKim(double cd, double d, double h, double v){
		return cd / getPixelsPerDegreeKim(d, h, v);
	}

	/**
	 * Here same as above but distance is specified in meters rather than units of diagonal image size.
	 * 
	 * @param fi
	 * @param distance
	 * @param conf
	 * @return
	 */
	public static double cyclesPerDegreeToCyclesPerPixel(double cd, DisplayConfiguration conf) {
		double diagonal = conf.getMetricDiagonal();
		double d = conf.getViewerDistance()/diagonal;
		return cyclesPerDegreeToCyclesPerPixelKim(cd, d, conf.getXResolution(), conf.getYResolution());
	}
	
	
	/**
	 * @param d The viewing distance in units of diagonal image size in pixels
	 * @param h The horizontal image size in pixels
	 * @param v the vertical image size in pixels
	 * @return fn, the number of pixels per degree of visual angle
	 */
	public static double getPixelsPerDegreeKim(double d, double h, double v) {
		return 1 / (Math.asin( 180/Math.PI * 1/(Math.sqrt(1 + d*d*(h*h+v*v)) )));
	}

	
	/**
	 * Compute the cycles per pixel on the target display that's equivalent 
	 * to the given cycles per pixel on the source. 
	 * @param d
	 * @param source
	 * @param target
	 * @return
	 */
	public static double getEquivalentCyclesPerPixel(double d, DisplayConfiguration source, DisplayConfiguration target){
		double cyclesPerDegree = cyclesPerPixelToCyclesPerDegree(d, source);
		double targetCyclesPerPixel = FrequenciesAndAngles.cyclesPerDegreeToCyclesPerPixel(cyclesPerDegree, target);
		return targetCyclesPerPixel;
	}
	

	/**
	 * Returns the horizontal visual angle in radians occupied by an object on the display given its size
	 * and the current viewer distance, both expressed in meters.
	 * 
	 * The object is perpendicular to the observer's line of sight.
	 */
	public static double getVisualAngleRadians(double size, double distance) {
		return 2 * Math.atan2(size / 2, distance); 
	}

	public static double getDistanceForVisualAngleRadians(double size, double angle) {
		return (size / 2) / Math.tan(angle / 2);
	}

	public static double getSizeForVisualAngleRadians(double distance, double angle) {
		return 2 * distance * Math.tan(angle / 2);
	}
	
	public static void main(String[] args) {
//		DisplayConfiguration disp = new ClientDisplayConfiguration();
		DisplayConfiguration disp = new WILDDisplay();
		disp.setViewerDistance(12.21);
		double ppc = 2560;
		double cpp = 1/ppc;
		double cpd = cyclesPerPixelToCyclesPerDegree(cpp, disp);
		System.err.println(cpd);
		double cpdk = cyclesPerPixelToCyclesPerDegreeKim(cpp, disp);
		System.err.println(cpdk);
		double cpdp = pixelsPerCycleToCyclesPerDegree_paperformula(ppc, disp);
		System.err.println(cpdp);
		
		//double ppd = getPixelsPerDegree(3, 0.5, 0.5);
		//System.err.println(ppd);
		
//		double d = disp.getXPixelsForVisualAngle(3);
		//System.err.println(d);
	}
	
}
