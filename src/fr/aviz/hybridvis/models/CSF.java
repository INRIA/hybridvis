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

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.tanh;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import ac.essex.graphing.plotting.ContinuousFunctionPlotter;
import ac.essex.graphing.plotting.Graph;
import ac.essex.graphing.plotting.PlotSettings;
import ac.essex.graphing.swing.GraphApplication;
import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.display.WILDDisplay;


/**
 * Implements contrast sensitivity functions (CSF) models from previous literature.
 * 
 * From [3]: "The CSF measures the response of the visual system to different frequencies. Another perspective of
 * CSF is that it is the reciprocal of the contrast necessary for a given frequency to be perceived."
 * 
 * REFs:
 * [1] J. L. Mannos, D. J. Sakrison, ``The Effects of a Visual Fidelity Criterion on the Encoding of Images'', IEEE Transactions on Information Theory, pp. 525-535, Vol. 20, No 4, (1974)
 *     https://www.dropbox.com/s/wn9jo3m67t8j2vy/mannos-sakrison-1974.pdf
 * [2] http://www.cg.tuwien.ac.at/research/theses/matkovic/node20.html.
 * [3] Ajeetkumar Gaddipatti et al. Steering Image Generation with Wavelet Based Perceptual Metric.
 *     http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.44.5644&rep=rep1&type=pdf
 * [4] Peter G. L. Barten (1999). Contrast Sensitivity of the Human Eye and its Effects on Image Quality.
 *     http://alexandria.tue.nl/extra2/9901043.pdf
 *
 * @author dragice
 *
 */
public class CSF {

	///////// Main /////////
//	public static void main(String[] args) {
//				
//		//showPixelCSFs(new WILDDisplay(), 1, true, true, null);
//		//showPixelCSFs(new AppleCinemaDisplay(), 0.0315, true, true, null);
//
//		//AnalyzeWILDvisibility();
//		//showCSFs();
//		//showCSFsLogLog();
//		
//		//showBartenFig34();
//		//showBartenFig37();
//		//showSensitivityForLogFrequencyPerDistance(new WILDDisplay());
//		//showSensitivityForPixelSizePerDistance(WILDDisplay);		
//	}
	
	/**
	 * Barten is far better than Mannos and Sakrison, despite the latter being widely used for image quality metrics.
	 * @param fs
	 * @return
	 */
	public static double getCSF(double fs) {
		//return getMannosSakrisonCSF(fs);
		return getBartenCSF(fs);
	}
	
	/**
	 * Returns the CSF for a given frequency according to the model by Mannos and Sakrison [1] (Eq 23 p. 535)
	 * The equation is also given in [2] and [3] (equation 2).
	 * 
	 * It has a peak of value 1.0 at fr = 8.0 cycles/degree and a zero-frequency intercept of 0.5.
	 * This peak value is too high as the authors themselves admit, and is believed to be between 3 and 4 cycles/degree.
	 *
	 * @param fs The spatial frequency of the visual stimuli given in cycles per degree of visual angle
	 * @return A(fs), the contrast sensitivity, between 0 and 1.
	 */
	public static double getMannosSakrisonCSF(double fs) {
		return 2.6 * (0.0192 + 0.114 * fs) * exp(-pow(0.114*fs, 1.1));
	}

	/**
	 * The CSF model from Barten [4] pp 36-61.
	 * 
	 * This model relies on the following assumptions:
	 * - The object being viewed is square (see [4] for how to correct for rectangular or circular objects)
	 * - Average observer
	 * - Foveal vision
	 * - Photopic (bright light) viewing conditions
	 * 
	 * @param fs the spatial frequency in cycles/deg of visual angle.
	 * @param X0 the angular size of the object in degrees.
	 * @param L the average luminance of the object in cd/m^2.
	 * @param p the photon conversion factor in photons/sec/deg^2/Td (depends on the light source).
	 * @return Su, normalized to be between 0 and 1.
	 */
	public static double getBartenCSF(double fs, double X0, double L, double p) {

		// The input variable in Barten's model is called u and it looks like it is sometimes
		// expressed in degrees and sometimes expressed in minutes of arc, a unit that is
		// equal to one sixtieth (1/60) of a degree
		double u_degrees = fs;
		double u_minutes_arc = fs / 60.0;
		
		// Typical constant values provided by Barten p. 37, valid for an average
		// observer, foveal vision and photopic viewing conditions.
		// For a given subject only sigma0, eta and k have to be adapted. 
		double sigma0 = 0.5; // pupil size constant (good vision), in arc min
		double eta = 0.03; // quantum efficiency of the eye
		double k = 3.0; // signal-to-noise ratio
		double T = 0.1; // integration time of the eye, in sec
		double Cab = 0.08; // pupil size constant (good vision), in arc min / mm
		double Xmax = 12; // maximum angular size of the integration area, in degrees
		double Nmax = 15; // maximum number of cycles over which the eye can integrate information
		double Phi0 = 3e-8; // spectral density of the neural noise, in sec deg^2
		double u0 = 7; // spatial frequency above which lateral inhibition ceases, in cycles / deg.

		// --- Pupil size
		//double d = 5 - 3*tanh(0.4 * log(L)); // simpler formulation
		double d = 5 - 3 * tanh(0.4 * log(L * sq(X0) / sq(40))); // pupil diameter in mm, given by Eq 3.9
		
		// -- Conversion from avg object luminance L (cd/m^2) to retinal illuminance E (Troland)
		//double E = PI * sq(d) / 4 * L;  // simpler formulation, Eq 3.15
		double E = PI * sq(d) / 4 * L * (1 - sq(d/9.7) + sq(sq(d/12.4))); // Eq 3.16

		// --- Optical MTF for the eye
		double sigma = sqrt(sq(sigma0) + sq(Cab * d));
		double Mopt = exp(-2 * sq(PI) * sq(sigma) * sq(u_minutes_arc)); // Optical MTF for the eye

		//  --- The three multiplicative factors in the denominator of Eq. 3.26
		double factor1 = 2 / T;
		double factor2 = (1 / sq(X0) + 1 / sq(Xmax) + sq(u_degrees) / sq(Nmax));
		double factor3 = (1 / (eta * p * E) + Phi0 / (1 - exp(-sq(u_degrees/u0))));
		
		// S(u) in Eq. 3.26
		double Su = Mopt / k / sqrt(factor1 * factor2 * factor3);

		// The S(u) function seems to return values up to ~800.
		// In order to normalize contrast sensitivity between 0 and 1 like Mannos and Sakrison's formula,
		// we divide Su by this hypothetical maximum value.
		double maxSu = 800.0;
		
		return Su / maxSu;
	}
		
	/**
	 * Returns Barten's CSF function by setting the photon conversion factor to
	 * a reasonable value of 1.240 * 10e6 (CRT monitor -- there was no LCD at the time)
	 */
	public static double getBartenCSF(double fs, double X0, double L) {
		// The photon conversion factor in 10^6 photons/sec/deg^2/Td, defined as
		// "the number of photons per unit of time, per unit of angular area, and per unit of
		// luminous flux per angular area of the light entering the eye".
		// This value depends on the light source, especially its color. Several values are given
		// in Table 3.2. The closest to an LCD screen is P4 - white CRT phosphor, with a value
		// of 1.240 for photopic vision. This values assumes luminance L is measured in photopic
		// units.
		double p = 1.240 * 10e6;
		
		return getBartenCSF(fs, X0, L, p);
	}
	
	/**
	 * Returns Barten's CSF function using reasonable values for the free parameters:
	 * - The angular object size is set to 10 degrees
	 * - The object's brightness is set to 100 cd/m^2
	 * - The photon conversion factor value is set to 1.240 * 10e6 (white CRT monitor)
	 * @param u
	 * @return
	 */
	public static double getBartenCSF(double fs) {

		// The angular size of the object in degrees, also called "field size".
		// Barten uses 10x10 deg in his example on Fig 3.4 p. 37.
		// "The field size causes a vertical shift of the low frequency part of the
		//  curves, whereas the high frequency part remains the same, due to the effect
		//  of the limited number of cycles".
		// We use 30 degrees because from (Swaminathan & Sato, 1997), Interaction design
		// for large displays:
		// "Many interrelated factors have led to a standard page of text (and therefore
		//  a computer display) designed to occupy a visual angle of approximately 20 to
		//  40 degrees at the center of the visual cone and meant to be read without
		//  rotating the neck." 
		double X0 = 30;
		
		// L is the average object brightness in Candelas per square meter.
		// Barten uses 100 cd/m^2, 1 cd/m^2 and 0.01 cd/m^2 in his examples on Fig 3.4 p. 37.
		// Here http://computer.howstuffworks.com/monitor6.htm we can read:
		// "Typical brightness ratings range from 250 to 350 cd/m2 for monitors that perform
		// general-purpose tasks. For displaying movies, a brighter luminance rating such as
		// 500 cd/m2 is desirable.
		// Now, we can't expect the visualization to be all white. So let's use 100.
		double L = 100;
		
		return getBartenCSF(fs, X0, L);
		
	}

	protected static double sq(double x) {
		return x*x;
	}
	protected static double sqrt(double x) {
		return Math.sqrt(x);
	}
	
	
	
	
	
	
	
	
	//////////  Debugging
	
	public static void AnalyzeWILDvisibility() {
		
		DisplayConfiguration display = new WILDDisplay();
		
		double[] dist = new double[] {0.20, 0.70, 1.50, 2.00, 4.00, 10.0};
		double[] cpg = new double[] {-1, 60.0, 3.1, -2, 0.00053}; // lower limits and peak obtained by visualizing Barten's CSF
		
		// WILD is 20480 x 6400 pixels
		for (double d : dist) {
			System.out.println("From " + d + " meters:");
			display.setViewerDistance(d);
			for (double c : cpg) {
				double widthp = 0;
				if (c > 0) {
					double cpp = FrequenciesAndAngles.cyclesPerDegreeToCyclesPerPixel(c, display);
					widthp = 0.5 / cpp;
					widthp = ((int)(widthp * 10)) / 10.0;
				} else if (c == -1) {
					widthp = 1;
					c = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(0.5 / widthp, display);
				} else if (c == -2) {
					widthp = 100;
					c = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(0.5 / widthp, display);
				}
				double cs = 100 * getBartenCSF(c);
				cs = (int)(cs * 10) / 10.0;
				String visibility;
				if (cs < 0.05)
					visibility = "are invisible";
				else if (cs > 96)
					visibility = "have maximum visibility";
				else
					visibility = "generate a visual response of " + cs + " %";
				System.out.println("  Contrasted bands of " + widthp + " pixels " + visibility);
			}
		}
	}
		
	public static void showCSFs() {
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "mannos";}
			@Override
			public double getY(double x) {
				return getMannosSakrisonCSF(x);
			}
		};
		ContinuousFunctionPlotter cfp2 = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "barten";}
			@Override
			public double getY(double x) {
				return getBartenCSF(x);
			}
		};
		PlotSettings p = new PlotSettings();
		p.setMinX(0);
		p.setMaxX(60);
		p.setMinY(0);
		p.setMaxY(1);
		p.setGridSpacingX(10);
		p.setGridSpacingY(0.1);
		Graph graph = new Graph(p);
		graph.functions.add(cfp);
		graph.functions.add(cfp2);
		new GraphApplication(graph);
	}
	
	public static void showCSFsLogLog() {
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getMannosSakrisonCSF(Math.exp(x)));
			}
		};
		ContinuousFunctionPlotter cfp2 = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getBartenCSF(Math.exp(x)));
			}
		};
		PlotSettings p = new PlotSettings();
		p.setMinX(Math.log(0.0013));
		//p.setMinX(Math.log(3.5));
		p.setMaxX(Math.log(60));
		p.setMinY(Math.log(0.001));
		p.setMaxY(Math.log(1));
		p.setGridSpacingX(1);
		p.setGridSpacingY(1);
		Graph graph = new Graph(p);
		graph.functions.add(cfp);
		graph.functions.add(cfp2);
		new GraphApplication(graph);
	}
	
	/**
	 * Tries to reproduce Fig 3.4 from Barten's book, assuming a value of p ~ 1.2.
	 * The plots are similar but not quite exactly the same: values of L seem to have less impact.
	 * Strangely enough, when luminances are set to 100, 0.1 and 0.001 instead, the figures are almost
	 * exactly the same.
	 */
	public static void showBartenFig34() {
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getBartenCSF(Math.exp(x), 10, 100));
			}
		};
		ContinuousFunctionPlotter cfp2 = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getBartenCSF(Math.exp(x), 10, 1));
			}
		};
		ContinuousFunctionPlotter cfp3 = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getBartenCSF(Math.exp(x), 10, 0.01));
			}
		};
		PlotSettings p = new PlotSettings();
		p.setMinX(Math.log(0.1));
		p.setMaxX(Math.log(100));
		p.setMinY(Math.log(10/800.0));
		p.setMaxY(Math.log(10000/800.0));
		p.setGridSpacingX(1);
		p.setGridSpacingY(1);
		Graph graph = new Graph(p);
		graph.functions.add(cfp);
		graph.functions.add(cfp2);
		graph.functions.add(cfp3);
		new GraphApplication(graph);
	}
	
	/**
	 * Tries to reproduce Fig 3.7 from Barten's book, but without changing the subject eyes' characteristics.
	 */
	public static void showBartenFig37() {
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double x) {
				return Math.log(getBartenCSF(Math.exp(x), 2.5, 20));
			}
		};
		PlotSettings p = new PlotSettings();
		p.setMinX(Math.log(0.1));
		p.setMaxX(Math.log(100));
		p.setMinY(Math.log(1/800.0));
		p.setMaxY(Math.log(1000/800.0));
		p.setGridSpacingX(1);
		p.setGridSpacingY(1);
		Graph graph = new Graph(p);
		graph.functions.add(cfp);
		new GraphApplication(graph);
	}
	
	public static void showFrequencyConversion(final DisplayConfiguration screen) {
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "function";}
			@Override
			public double getY(double d) {
				double fi = 0.1; // 0.5 cycle / pixel
				screen.setViewerDistance(d);
				double y = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(fi, screen);
				return y;
			}
		};
		PlotSettings p = new PlotSettings();
		p.setMinX(0.0);
		p.setMaxX(10.0);
		p.setMinY(0.0);
		p.setMaxY(100.0);
		p.setGridSpacingX(1);
		p.setGridSpacingY(10);
		Graph graph = new Graph(p);
		graph.functions.add(cfp);
		new GraphApplication(graph);
	}
	
	public static void showSensitivityForLogFrequencyPerDistance(final DisplayConfiguration screen) {
		PlotSettings p = new PlotSettings();
		
		double lowestFreq = 1.0 / (screen.getXResolution() / 2); // half-screen size
		double highestFreq = 0.5; // one-pixel lines

		double lowestLogFreq = Math.log(lowestFreq);
		double highestLogFreq = Math.log(highestFreq);

		p.setMinX(lowestLogFreq);
		p.setMaxX(highestLogFreq);
		p.setGridSpacingX(1);
		
		p.setMinY(0);
		p.setMaxY(1);
		p.setGridSpacingY(0.1);

		Graph graph = new Graph(p);
		double[] dist_list = new double[]{0.3, 3.5, 40}; 
		for (double dist: dist_list) {
			final DisplayConfiguration conf = new DisplayConfiguration(screen);
			conf.setViewerDistance(dist);
			ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
				@Override
				public String getName() { return "distance " + conf.getViewerDistance();}
				@Override
				public double getY(double logFreq) {
					double fs = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(Math.exp(logFreq), conf);
					return getCSF(fs);
				}
			};
			graph.functions.add(cfp);
		}
		new GraphApplication(graph);
	}
	
	public static void showPixelCSFs(DisplayConfiguration display, final double yScale, final boolean includeCapacity, final boolean includeViewingArea, double[] pixelPerCycles, double[] freqs) {
		
		// Create curves for envelope
		final int n = 200;
		double[] envelopePixelPerCycles = new double[n];
		for (int i=0; i<n; i++)
			envelopePixelPerCycles[i] = 2 * Math.pow(2, (double)i / n * 10);
		
		PlotSettings p = new PlotSettings();
		p.setMinX(0.20);
		p.setMaxX(4.00);
		p.setMinY(0);
		p.setMaxY(100);
		p.setGridSpacingX(0.5);
		p.setGridSpacingY(25);
		Graph graph = new Graph(p);

		// Envelope
		if (freqs == null) {
			ContinuousFunctionPlotter cfp = getFunction(display, envelopePixelPerCycles, yScale, includeCapacity, includeViewingArea, 1, 2, new Color(0.95f, 0.95f, 0.95f));
			graph.functions.add(cfp);
		}
		// 4, 16, 64
		for (int i=1; i<pixelPerCycles.length; i+=2) {
			if (i > 6) continue;
			final double ppc = pixelPerCycles[i];
			final double multiplier = freqs == null ? 1 : freqs[i];
			Color color = Color.black;
			color = new Color(Color.HSBtoRGB(i / 8f, 1f, 1f));
			ContinuousFunctionPlotter cfp2 = getFunction(display, new double[] {ppc}, yScale, includeCapacity, includeViewingArea, multiplier, 0, color);
			graph.functions.add(cfp2);
		};
		new GraphApplication(graph);
	}
	
	protected static ContinuousFunctionPlotter getFunction(final DisplayConfiguration display, final double[] ppc, final double yScale, final boolean includeCapacity, final boolean includeViewingArea, final double multiplier, final float stroke, final Color color) {
				
		ContinuousFunctionPlotter cfp = new ContinuousFunctionPlotter() {
			@Override
			public String getName() { return "" + ppc;}
			@Override
			public double getY(double d) {
				double maxY = Double.MIN_VALUE;
				for (double p : ppc) {
					double y = getY(p, d);
					if (y > maxY)
						maxY = y;
				}
				return maxY;
			}
			protected double getY(double ppc, double d) {
				// Contrast sensitivity
				display.setViewerDistance(d);
				double cpp = 1.0 / ppc;
				double cpd = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(cpp, display);
				double sensitivity = getBartenCSF(cpd) * 100 * 1.025;

				// Information capacity
				double capacity = 1.0 / (Math.pow(ppc, 2));
				capacity /= 1.0/4.0;

				// Viewing area
				final double inspectableAngle = 40 / 180.0 * Math.PI;
				display.setViewerDistance(d);
				double viewingArea = display.getVisibleMetricArea(inspectableAngle, inspectableAngle) / display.getTotalMetricArea();
				
				// 
				double total = sensitivity;
				if (includeCapacity)
					total *= capacity;
				if (includeViewingArea)
					total *= viewingArea;
				total *= multiplier;
				
				// normalize y
				if (includeCapacity && includeViewingArea) {
					total *= 100.0 / 0.44;
				} else if (includeCapacity) {
					total *= 1.0;
				} else if (includeViewingArea) {
					total *= 1.0;
				}
				
				return total * yScale;
			}
			public Stroke getStroke() {
		    	if (stroke == 0) {
		    		float thickness = 0.1f + 5f * (float)Math.pow(ppc[0]/2/128, 0.5);
		    		return new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		    	} else {
		    		return new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		    	}
			}
			public Color getColor() {
				return color;
			}
		};
		return cfp;
	}
}
