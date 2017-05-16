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

/**
 * Misc definitions and conversions for Gaussian filters.
 * 
 * @author dragice
 *
 */
public class GaussianFilter {

	/**
	 * The impulse response function that defines the 1-D Gaussian filter.
	 * 
     * From http://en.wikipedia.org/wiki/Gaussian_filter:
     * g(x) = 1/(sqrt(2*pi)*sigma) * exp(-x*x/(2*sigma*sigma))
     * 
	 * @param sigma the standard deviation of the filter
	 * @param x the input signal
	 * @return the output signal g(x)
	 */
	public static double get1DImpulseResponse(double sigma, double x) {
		double g = 1/(Math.sqrt(2*Math.PI)*sigma) * Math.exp(-x*x/(2*sigma*sigma));
		return g;
	}
	
	/**
	 * Returns the standard deviation in the frequency domain of a Gaussian filter
	 * with standard deviation sigma.
	 * 
	 * From http://en.wikipedia.org/wiki/Gaussian_filter:
	 * sigmaf = 1 / (2*pi*sigma)
     *
	 * @param sigma the filter's standard deviation
	 * @return the filter's standard deviation in the frequency domain.
	 */
	public static double getSigmaFFromSigma(double sigma) {
		double sigmaf = 1 / (2*Math.PI*sigma);
		return sigmaf;
	}
	
	public static double getSigmaFromSigmaF(double sigmaf) {
		double sigma = 1 / (2*Math.PI*sigmaf);
		return sigma;
	}

	/**
	 * The frequency response function of a 1-D Gaussian filter.
	 * 
     * From http://en.wikipedia.org/wiki/Gaussian_filter:
     * ^g(f) = exp(-f*f/(2*sigmaf*sigmaf))
     * 
	 * @param sigma the standard deviation of the filter
	 * @param x the input frequency
	 * @param the frequency response ^g(f)
	 */
	public static double get1DFrequencyResponse(double sigma, double f) {
		double sigmaf = getSigmaFFromSigma(sigma);
		double ghat = Math.exp(-f*f/(2*sigmaf*sigmaf));
		return ghat;
	}

	/**
	 * Returns the radius of a Gaussian filter given its standard deviation.
	 * 
	 * This is the formula used by jhlabs's Gaussian filter implementation (figured out by looking
	 * at the code). It corresponds to the radius of the convolution kernel used in most implementations
	 * of spatial domain Gaussian filters.
	 */
	public static double getRadiusFromSigma(double sigma) {
		double radius = sigma * 3.0;
		return radius;
	}

	/**
	 * Returns the standard deviation of a Gaussian filter given its radius.
	 * 
	 * See getRadiusFromSigma() for more information.
	 * 
	 */
	public static double getSigmaFromRadius(double radius) {
		double sigma = radius / 3.0;
		return sigma;
	}
	
	/**
	 * Returns the radius of a Gaussian filter given its standard deviation, as defined in Photoshop.
	 * 
	 * Photoshop defines the radius of a Gaussian blur as its standard deviation Sigma
	 * (figured out by comparing images blurred by jhlabs and blurred by photoshop) 
	 */
	public static double getPhotoshopRadiusFromSigma(double sigma) {
		double radius = sigma;
		return radius;
	}

	/**
	 * Returns the standard deviation of a Gaussian filter given its radius as defined in Photoshop.
	 * 
	 * Photoshop defines the radius of a Gaussian blur as its standard deviation Sigma
	 * (figured out by comparing images blurred by jhlabs and blurred by photoshop) 
	 */
	public static double getSigmaFromPhotoshopRadius(double radius) {
		double sigma = radius;
		return sigma;
	}
	
	/**
	 * Returns the cut-off frequency of a Guassian filter, defined as its standard deviation in the
	 * frequency domain.
	 * 
	 * This is one of the definitions of cut-off from http://en.wikipedia.org/wiki/Gaussian_filter
	 * but getCutoffFrequencyFromSigma is probably a better choice.
	 * The response value of the Gaussian filter at this cut-off frequency equals exp(-0.5) ~ 0.607.
	 * 
	 * @param sigma
	 * @return
	 */
	public static double getCutoffFrequency2FromSigma(double sigma) {
		double cutoff = getSigmaFFromSigma(sigma);
		return cutoff;
	}
	
	/**
	 * Returns the cut-off frequency of a Gaussian filter, given an arbitrary cutoff in response.
	 * 
	 * From http://en.wikipedia.org/wiki/Gaussian_filter.
	 * 
	 * @param sigma
	 * @param responseValue
	 * @return
	 */
	public static double getCutoffFrequencyFromSigma(double sigma, double responseCutoff) {
		double fc = Math.sqrt(2 * Math.log(1/responseCutoff)) * getSigmaFFromSigma(sigma);
		return fc;
	}
	
	/**
	 * Returns the standard deviation given the cutoff frequency using the standard definition of
	 * cutoff as the -3dB point.
	 * 
	 * @param f
	 * @return
	 */
	public static double getSigmaFromCutoffFrequency(double f) {
		double sigma = getSigmaFromCutoffFrequency(f, 0.5);
		return sigma;
	}
	
	public static double getSigmaFromCutoffFrequency(double f, double responseCutoff) {
		double sigmaf =  f / Math.sqrt(2 * Math.log(1/responseCutoff));
		double sigma = getSigmaFromSigmaF(sigmaf);
		return sigma;
	}

	/**
	 * Returns the cut-off frequency of a Gaussian filter, defined as the point where the filter response
	 * is reduced to 0.5 in power spectra (the -3 dB point). This is maybe the most standard
	 * definition.
	 * 
	 * From http://en.wikipedia.org/wiki/Gaussian_filter.
	 */
	public static double getCutoffFrequencyFromSigma(double sigma) {
		return getCutoffFrequencyFromSigma(sigma, 0.5);
	}
	
	public static void main(String[] args) {
		
		double cutoff_freq = 10;
		double sigma = getSigmaFromCutoffFrequency(cutoff_freq);
		
		System.err.println(getCutoffFrequency2FromSigma(sigma));
		System.err.println(getCutoffFrequencyFromSigma(sigma, Math.exp(-0.5)));
		System.err.println(getCutoffFrequencyFromSigma(sigma, 0.5));
		System.err.println(getCutoffFrequencyFromSigma(sigma));
	}
}
