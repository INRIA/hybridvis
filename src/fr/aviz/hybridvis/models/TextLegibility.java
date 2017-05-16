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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import fr.aviz.hybridvis.display.ClientDisplayConfiguration;
import fr.aviz.hybridvis.display.DisplayConfiguration;

/**
 * Various models of font legibility and useful conversion functions.
 * 
 * @author dragice
 *
 */
public class TextLegibility {

	static Graphics2D tmpGraphics = (new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)).createGraphics();
	
	static final double TYPOGRAPHIC_POINT_SIZE = 352.78e-6; // in meters
	
	/**
	 * (Solomon and Pelli, 1994) used a noise masking paradigm to show that the most important frequency
	 * for reading text is 3 cycles per letter. But it does not say much about how frequency filtering
	 * affects reading (e.g., a letter can still be read after strong hi-pass filtering). 
	 * http://www.staff.city.ac.uk/~solomon/pubs/solomonpelli94/Solomon&Pelli94.pdf
	 * 
	 */
	public static double getSolomonOptimumCyclesPerLetter() {
		return 3.0;
	}

	/**
	 * According to (Solomon and Pelli, 1994), low-pass filtering with a cut-off below 2 cycles per letter
	 * "severely" impairs character legibility, but they only used noise masking and cite previous studies to
	 * support this. http://www.staff.city.ac.uk/~solomon/pubs/solomonpelli94/Solomon&Pelli94.pdf
	 * 
	 * One of those previous studies is (Legge and al, 1985): http://legge.psych.umn.edu/pdf/read1.pdf
	 * They used a non-Gaussian optical diffuser and showed that 2 cycles per letter is a threshold
	 * above which asymptotical reading rate is reached (Fig 5 p. 7). So 2 cycles per letter is an optimum
	 * rather than a critical threshold beyond which text becomes illegible.
	 * 
	 * The other study is (Ginsburg, 1978), cited in Legge and al, 1985: "Ginsburg (1978) prepared pictures of
	 * single letters that were spatial-frequency filtered. He demonstrated that legibility required bandwidths
	 * in the range 1.5-3 cycles per character width.
	 */
	public static double getLeggeOptimumCyclesPerLetter() {
		return 2.0;
	}
	
	/**
	 * X-height is the height of the 'x' letter of the font and is sometimes used as a
	 * measure of legibility.
	 * 
	 * See (Rossum, 1998) http://homepages.inf.ed.ac.uk/mvanross/reprints/legibility.pdf
	 * @param f
	 * @return
	 */
	public static double getXHeightInPixels(Font font) {
		char letter = 'x';
		GlyphVector gv = font.createGlyphVector(tmpGraphics.getFontRenderContext(), new char[]{letter});
		GlyphMetrics gm = gv.getGlyphMetrics(0);
		double xheight = gm.getBounds2D().getHeight();
		return xheight;
	}
	
	/**
	 * Returns the average width in pixels of small-cap letters for this font, including
	 * spacing between letters.
	 * 
	 * @param f
	 * @return
	 */
	public static double getAverageLetterWidthInPixels(Font f, boolean uppercase) {
		FontMetrics metrics = tmpGraphics.getFontMetrics(f);
		String letters = uppercase ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "abcdefghijklmnopqrstuvwxyz";
		int width = metrics.stringWidth(letters);
		return width / (double)letters.length();
	}
	
	public static double typographicPointsToMeters(double points) {
		return points * TYPOGRAPHIC_POINT_SIZE;
	}
	
	public static double metersToTypographicPoints(double m) {
		return m / TYPOGRAPHIC_POINT_SIZE;
	}

	/**
	 * Returns the blur radius corresponding to the "asymptotic reading rate" (Legge et al, 1985).
	 * 
	 * Legge et al use a cutoff value of 1/e ~ 0.36 in signal amplitude: "As a measure of bandwidth, we used the
	 * spatial frequency at which the MTF has declined to 1/e." But recall they don't use a Gaussian filter
	 * so it's difficult to compare.
	 * 
	 * @param f
	 * @return
	 */
	public static int getLeggeOptimumBlurRadius(Font f, boolean uppercase) {
		double averageLetterWidthPixels = getAverageLetterWidthInPixels(f, uppercase);
		double minCyclesPerLetter = getLeggeOptimumCyclesPerLetter();
		double minCyclesPerPixel = minCyclesPerLetter / averageLetterWidthPixels;
		double maxSigma = GaussianFilter.getSigmaFromCutoffFrequency(minCyclesPerPixel, 1.0 / Math.E);
		double maxRadius = GaussianFilter.getRadiusFromSigma(maxSigma);
		return (int)maxRadius;
	}
	
	/**
	 * Returns the blur radius for which the given font starts to be totally illegible.
	 *   
	 * This function is based on single-user subjective empirical data (Pierre). See the file
	 * font-readability-pilot.xlsx in the experiments/ folder.
	 *   
	 * @param font
	 * @return
	 */
	public static int getMinimumIllegibleBlurRadius(Font font, boolean uppercase) {
		// Multiplying average letter width in pixels with this coefficient yields the standard deviation
		// sigma of the Gaussian blur. The current value for this coefficient has been obtained by linear
		// regression on single-user empirical data. 
		final double legibilityCoeff = 0.28;
		return getBlurRadiusForLegibility(font, uppercase, legibilityCoeff);
	}

	/**
	 * Returns the blur radius for which the given font appears blurry but can be comfortably read.
	 * Larger blur radii might make the font look too blurry. Twice this radius will make the font
	 * illegible (see getMinimumIllegibleBlurRadius()).
	 *   
	 * This function is based on single-user subjective empirical data (Pierre). See the file
	 * font-readability-pilot.xlsx in the experiments/ folder.
	 *   
	 * @param font
	 * @return
	 */
	public static int getMaximumComfortableBlurRadius(Font font, boolean uppercase) {
		// Multiplying average letter width in pixels with this coefficient yields the standard deviation
		// sigma of the Gaussian blur. The current value for this coefficient has been obtained by linear
		// regression on single-user empirical data. 
		final double legibilityCoeff = 0.14;
		return getBlurRadiusForLegibility(font, uppercase, legibilityCoeff);
	}

	/**
	 * Returns the blur radius for which the given font is very comfortable to read and appears almost
	 * undistorted. Smaller blur radii won't make much of a difference.
	 *   
	 * This function is based on single-user subjective empirical data (Pierre). See the file
	 * font-readability-pilot.xlsx in the experiments/ folder.
	 *   
	 * @param font
	 * @return
	 */
	public static int getMinimumComfortableBlurRadius(Font font, boolean uppercase) {
		// Multiplying average letter width in pixels with this coefficient yields the standard deviation
		// sigma of the Gaussian blur. The current value for this coefficient has been obtained by linear
		// regression on single-user empirical data. 
		final double legibilityCoeff = 0.09;
		return getBlurRadiusForLegibility(font, uppercase, legibilityCoeff);
	}

	/**
	 * Returns the blur radius corresponding to a desired coefficient of "legibility" for this font. 
	 * The higher the coefficient, the less legible the text will be.
	 * - A value of 0 leaves the text unchanged.
	 * - A value between 0.10 and 0.15 produces text that can be comfortably read. Below 0.10 reading
	 *   speed should not be affected (Legge et al, 1985).
	 * - A value of 0.25 or more will make the text illegible. 
	 * 
	 * The coefficient of legibility corresponds to the amount of Gaussian blur (expressed as its
	 * standard deviation sigma) as a proportion of average letter width. In our pilot experiment,
	 * we found a much better correlation with average letter width than with x-height.
	 *  
	 * @param font
	 * @param coeff
	 * @return
	 */
	public static int getBlurRadiusForLegibility(Font font, boolean uppercase, double legibilityCoeff) {
		double averageFontWidthInPixels = getAverageLetterWidthInPixels(font, uppercase);
		double sigma = averageFontWidthInPixels * legibilityCoeff;
		double radius = GaussianFilter.getRadiusFromSigma(sigma);
		return (int)Math.round(radius);
	}
	
	/**
	 * Returns the legibility coefficient that corresponds to the given font and blur radius.
	 * 
	 * Note that the lower the legibility coefficient returned, the higher the legibility.
	 * Refer to the documentation of getBlurRadiusForLegibility() for details.
	 * 
	 * @param font
	 * @param blurRadius
	 * @return
	 */
	public static double getLegibilityForBlurRadius(Font font, boolean uppercase, double blurRadius) {
		double sigma = GaussianFilter.getSigmaFromRadius(blurRadius);
		double averageFontWidthInPixels = getAverageLetterWidthInPixels(font, uppercase);
		double legibilityCoeff = sigma / averageFontWidthInPixels;
		return legibilityCoeff;
	}

	/**
	 * Returns the font size that will yield the desired legibility coefficient after the
	 * provided blur radius will be applied. This function is more reliable with large
	 * font sizes, as small font sizes (say <14) tend to be rendered in unpredictable ways.
	 * 
	 * Note that the lower the legibility coefficient the higher the legibility.
	 * Refer to the documentation of getBlurRadiusForLegibility() for details.
	 * 
	 * @param font
	 * @param blurRadius
	 * @return
	 */
	public static int getFontSizeForLegibility(Font font, boolean uppercase, double blurRadius, double legibilityCoeff) {
		double sigma = GaussianFilter.getSigmaFromRadius(blurRadius);
		float referenceFontSize = 100;
		double referenceAverageFontWidthInPixels = getAverageLetterWidthInPixels(font.deriveFont(referenceFontSize), uppercase);
		double averageFontWidthInPixels = sigma / legibilityCoeff;
		return (int)Math.round(100 * averageFontWidthInPixels / referenceAverageFontWidthInPixels);
	}

	public static void main(String[] args) {
		
		// Font metrics testing
		Font font = new Font("Helvetica", 0, 16);
		System.err.println("Font size: " + font.getSize());
		System.err.println("X-height: " + getXHeightInPixels(font));
		System.err.println("Average width: " + getAverageLetterWidthInPixels(font, false));
		System.err.println();
		
		// Maximum blur radii according to (Solomon and Pelli, 1994)
		// Note it's wrong
		for (Font f : new Font[] {new Font("Helvetica", 0, 5000), new Font("Times", 0, 5000), new Font("Courier", 0, 5000)}) {
			double leggeMaxBlur = getLeggeOptimumBlurRadius(f, false);
			System.err.println("Legge's optimal blur radius: " + leggeMaxBlur);
			System.err.println("Corresponding legibility: " + getLegibilityForBlurRadius(f, false, leggeMaxBlur));
		}
		System.err.println();

		// Analysis of (Rossum, 1998) experimental setup p.5
		// http://homepages.inf.ed.ac.uk/mvanross/reprints/legibility.pdf
		
		DisplayConfiguration display = new ClientDisplayConfiguration();
		double fontSizePoints = 16;
		System.err.println("Font size: " + fontSizePoints + " points");
		double fontSizeMeters = typographicPointsToMeters(fontSizePoints);
		System.err.println("Font size: " + fontSizeMeters * 1000 + " mm");
		int fontSizePixels = (int)Math.round(display.heightToPixels(fontSizeMeters));
		font = new Font("Times", 0, fontSizePixels);
		System.err.println("Font size: " + fontSizePixels + " pixels");
		double xHeightPixels = getXHeightInPixels(font);
		System.err.println("X-height: " + xHeightPixels + " pixels");
		double xHeightMeters = xHeightPixels * display.getPixelHeight();
		System.err.println("X-height: " + xHeightMeters * 1000 + " mm");
		
		// Blur sigma between 0.8 and 1.1 mm 
		double minBlurSigmaMeters = 0.8 / 1000.0; // 0.8 mm
		double maxBlurSigmaMeters = 1.1 / 1000.0; // 1.1 mm
		double minBlurSigmaPixels = display.heightToPixels(minBlurSigmaMeters);
		double maxBlurSigmaPixels = display.heightToPixels(maxBlurSigmaMeters);
		double minBlurRadiusPixels = GaussianFilter.getRadiusFromSigma(minBlurSigmaPixels);
		double maxBlurRadiusPixels = GaussianFilter.getRadiusFromSigma(maxBlurSigmaPixels);
		System.err.println("Blur between " + minBlurRadiusPixels + " and " + maxBlurRadiusPixels + " pixels.");
		
		// Blur sigma 92 +/- % of the full size font
		double minBlur = xHeightPixels * 90 / 100;
		double maxBlur = xHeightPixels * 94 / 100;
		System.err.println("Average blur between " + minBlur + " pixels and " + maxBlur + " pixels"); 
	}
}
