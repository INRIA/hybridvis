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

package fr.aviz.hybridvis.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_2D;
import fr.aviz.hybridvis.display.ClientDisplayConfiguration;
import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.display.WILDDisplay;
import fr.aviz.hybridvis.models.CSF;
import fr.aviz.hybridvis.models.FrequenciesAndAngles;


public class ImageStatistics {


  protected static final float DISPLAY_PIXEL_DENSITY = 1f/40f; // cm/pixel
  //protected static DisplayConfiguration displayConfiguration = new WILDDisplay();
  protected static DisplayConfiguration displayConfiguration = new ClientDisplayConfiguration(0);

  int bandCenter = 10;
  int bandSpread = 2;

  /**
   * Takes a list of of frequency distributions (as float arrays) and draws 
   * a power spectrum chart showing them into the image provided.
   * @param outputImage
   * @param frequencyDists
   * @param frequencyColors
   * @param simulatedDisplay TODO
   * @param clientDisplay TODO
   * @param rescaleToSimulated TODO
   * @param logScaleY TODO
   * @param highlightBand TODO
   * @param x
   * @param y
   * @param width
   * @param height
   */
  public static void drawPowerSpectrums(BufferedImage outputImage,
      List<float[]> frequencyDists, Color[] frequencyColors, int[] frequencyWidths, 
      DisplayConfiguration simulatedDisplay, DisplayConfiguration clientDisplay, 
      boolean rescaleToSimulated, boolean logScaleY, double[] highlightBand, 
      int x, int y, int width, int height) {

    Graphics2D g = outputImage.createGraphics();
    g.setColor(Color.white);
    g.fillRect(x, y, width, height);
    g.setFont(g.getFont().deriveFont(9f));

    //parse frequency distributions for min/max/count
    float binMin = 0;
    float binAbsMax = Float.NEGATIVE_INFINITY;
    float binMax = Float.NEGATIVE_INFINITY;
    int binCount = 0;
    for(int fi=0; fi < frequencyDists.size(); fi++){
      float[] bins = frequencyDists.get(fi);
      if(bins.length > binCount) binCount = bins.length;
      for(int b=1; b<bins.length;b++){
        if(bins[b] < binMin) binMin = bins[b];
        if(bins[b] > binMax) binMax = bins[b];
        if(Math.abs(bins[b]) > binAbsMax) binAbsMax = Math.abs(bins[b]);
      }
    }  

    //draw reference contrast sensitivity curves at several distances
    double[] distances = new double[]{3.0, 0.7, simulatedDisplay.getViewerDistance()};
    for(int di=0;di<distances.length;di++){
      double distance = distances[di];
      DisplayConfiguration simulatedWallAtDistance = new WILDDisplay(distance);
      double[] csf = new double[binCount];
      double csfMin = Double.POSITIVE_INFINITY;
      double csfMax = Double.NEGATIVE_INFINITY;
      double csfMaxIndex = 0;
      //TODO: Check to make srue we don't need to take pixel sizes into account
      double wallRatio = rescaleToSimulated ? clientDisplay.getViewerDistance() / simulatedDisplay.getViewerDistance() : 1;

      //compute CSF at each distance
      for(int b = 1; b < binCount; b ++){ 
        double cyclesPerPixel = b /(double)(binCount-1);
        double simulatedCyclesPerPixel = wallRatio * cyclesPerPixel;
        double cyclesPerDegree = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(simulatedCyclesPerPixel, simulatedWallAtDistance);
        double csfValue = CSF.getCSF(cyclesPerDegree);
        csf[b] = csfValue;
        if(csfValue < csfMin) csfMin = csfValue;
        if(csfValue > csfMax){
          csfMax = csfValue;
          csfMaxIndex = b;
        }
      }

      //Plot CSF
      Stroke oldStroke = g.getStroke();
      g.setColor(di > 1 ? Color.gray : Color.lightGray);
      g.setStroke(new BasicStroke(2));
      double lastX = Double.NaN;
      double lastY = Double.NaN;
      for(int b = 1; b < binCount; b ++){
        double pointX = x + width * Math.log10(b) / Math.log10(binCount);
        double pointY = logScaleY ? 
            y + height - height * Math.log10(csf[b] - csfMin + 1) / Math.log10(csfMax - csfMin + 1) : 
              y + height - height * (csf[b] - csfMin) / (csfMax - csfMin);
            if(!Double.isNaN(lastX)){
              g.drawLine((int)pointX, (int)pointY, (int)lastX, (int)lastY);
            }
            lastX = pointX;
            lastY = pointY;
            if(b == csfMaxIndex){
              DecimalFormat df = new DecimalFormat("###.#");
              g.drawString(df.format(distance) + "m", (int)pointX - 7, (int)pointY + 10);
            }
      }
      g.setStroke(oldStroke);
    }

    //draw power spectrums
    for(int fi=0; fi < frequencyDists.size(); fi++){
      float[] bins = frequencyDists.get(fi);
      Color color = Color.red;
      int strokeWidth = 2;
      if(frequencyColors != null && fi < frequencyColors.length){
        color = frequencyColors[fi];
      }
      if(frequencyWidths != null && fi < frequencyWidths.length){
        strokeWidth = frequencyWidths[fi];
      }
      g.setColor(color);
      g.setStroke(new BasicStroke(strokeWidth));
      double lastX = Double.NaN;
      double lastY = Double.NaN;
      for(int b=1; b<bins.length;b++){
        if(bins[b]==0f) continue; //empty bins generally mean no component at that distance, so ignore.
        double pointX = x + width * Math.log10(b) / Math.log10(binCount);
        double pointY = logScaleY ? 
            y + height - height * Math.log10(bins[b] - binMin + 1) / Math.log10(binMax - binMin + 1) :
              y + height - height * (bins[b] - binMin) / (binMax - binMin);
            if(!Double.isNaN(lastX)){
              g.drawLine((int)pointX, (int)pointY, (int)lastX, (int)lastY);
              g.fillOval((int)pointX-2, (int)pointY-2, 4, 4);
            }
            lastX = pointX;
            lastY = pointY;
      } 
    }

    //draw reference lines
    g.setColor(Color.gray);
    g.setStroke(new BasicStroke(1));
    for(int pixelsPerCycle : new int[]{400, 100, 40, 20, 10, 5}){
      //convert pixels/cycle to cycles/pixel, cycles/degree, and measures for the simulated display
      double cyclesPerPixel = 1.0 / pixelsPerCycle;
      double cyclesPerDegree = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(cyclesPerPixel, simulatedDisplay);
      double simulatedCyclesPerPixel = FrequenciesAndAngles.getEquivalentCyclesPerPixel(cyclesPerPixel, simulatedDisplay, clientDisplay);
      double simulatedCyclesPerMeter = simulatedCyclesPerPixel * clientDisplay.getXResolution() / clientDisplay.getMetricWidth();
      double lineFrequency = binCount * 2 * (rescaleToSimulated ? simulatedCyclesPerPixel : cyclesPerPixel);

      if(lineFrequency > 1 && lineFrequency < binCount){
        double xFrequency = x + width * Math.log10(lineFrequency) / Math.log10(binCount);
        g.drawLine((int)xFrequency, y, (int)xFrequency, y + height);
        g.drawString(String.valueOf(pixelsPerCycle) + "px", (int)xFrequency + 1, y + height - 1);
        DecimalFormat df = new DecimalFormat("#####.###");
        g.drawString(df.format(cyclesPerDegree) + "c/d", (int)xFrequency + 1, y + height - 12);
        if(rescaleToSimulated){
          g.drawString(df.format(1/simulatedCyclesPerMeter) + "m", (int)xFrequency + 1, y + height - 23);
          g.drawString(df.format(1/simulatedCyclesPerPixel) + "px", (int)xFrequency + 1, y + height - 34);
        }
      }
    }    

    //draw highlight band
    if(highlightBand != null && highlightBand.length == 2){
      double bandMinCyclesPerPixel = 1.0 / highlightBand[0];
      double bandMinSimulatedCyclesPerPixel = FrequenciesAndAngles.getEquivalentCyclesPerPixel(
          bandMinCyclesPerPixel, simulatedDisplay, clientDisplay);
      double bandMinLineFrequency = binCount * 2 * 
          (rescaleToSimulated ? bandMinSimulatedCyclesPerPixel : bandMinCyclesPerPixel);
      double bandMaxCyclesPerPixel = 1.0 / highlightBand[1];
      double bandMaxSimulatedCyclesPerPixel = FrequenciesAndAngles.getEquivalentCyclesPerPixel(
          bandMaxCyclesPerPixel, simulatedDisplay, clientDisplay);
      double bandMaxLineFrequency = binCount * 2 * 
          (rescaleToSimulated ? bandMaxSimulatedCyclesPerPixel : bandMaxCyclesPerPixel);

      if(bandMaxLineFrequency > 1 || bandMinLineFrequency < binCount){
        double minXFrequency = x + width * Math.log10(bandMinLineFrequency) / Math.log10(binCount);
        double maxXFrequency = x + width * Math.log10(bandMaxLineFrequency) / Math.log10(binCount);
        g.setColor(new Color(0f,1f,0f,0.2f));
        g.fillRect((int)maxXFrequency, y,
            (int)Math.max(2, Math.abs(maxXFrequency - minXFrequency)), y + height);	
      }
    }

    //draw axes
    g.setColor(Color.darkGray);
    g.drawLine(x, y, x, y + height);
    g.drawLine(x, y + height, x + width, y + height);
    g.drawString(String.valueOf(convertPeriodToFrequency(binCount , 1)) + "px", x + 2, y + height - 1);
    g.drawString(String.valueOf(convertPeriodToFrequency(binCount , binCount)) + "px", x + width + 1, y + height - 1);
    g.setColor(Color.red);
    g.drawString(String.valueOf(binMax), x, y);
    g.drawString(String.valueOf(binMin), x, y + height + 10);
  }

  /**
   * Analyze the frequency of a region of a BufferedImage. Draw
   * the 2d discrete Fourier transform into the image and/or draw
   * a 1d frequency histogram in the upper left-hand corner.
   * @param inputImage
   * @param outputImage TODO
   * @param regionBounds
   * @param anglesToSample Angles to sample the 2d FFT to generate the 1d histogram.
   * @param draw2dFFT
   * @param draw1dHistogram
   */
  public static void annotateBufferedImageWithFrequencies(BufferedImage inputImage, 
      BufferedImage outputImage, Rectangle2D.Double regionBounds, 
      int anglesToSample, boolean draw2dFFT, boolean draw1dHistogram, Color plotColor) {
    float[][] imageFFT = fftImageRegion(inputImage, (int) regionBounds.x, 
        (int) regionBounds.x + (int)regionBounds.width, 
        (int)regionBounds.y, (int)regionBounds.y + (int)regionBounds.height);
    float[] frequency1d = radiallyBin2dFrequenciesTo1d(imageFFT);
    //float[] frequency1d = sample2dFrequenciesTo1dAtAngle(imageFFT, 1);

    if(outputImage == null)
      outputImage = inputImage;

    //Draw the 2D FFT
    if(draw2dFFT)
      floatArray2dToImageRegion(imageFFT, outputImage, true, (int) regionBounds.x, (int) regionBounds.y);

    //Draw a 1D Histogram
    if(draw1dHistogram){
      List<float[]> frequencyList = new ArrayList<float[]>();
      frequencyList.add(frequency1d);
      drawPowerSpectrums(outputImage, 
          frequencyList, new Color[]{plotColor}, null, new ClientDisplayConfiguration(0), new ClientDisplayConfiguration(0),
          true, 
          true, null, (int)regionBounds.x + 10, (int)regionBounds.y + 10, (int)Math.min(frequency1d.length, regionBounds.width / 2), (int)Math.min(frequency1d.length, regionBounds.height / 2));
    }
  }



  /**
   * Returns a copy of the input image that has been band-passed to contain only 
   * images in the specified frequency range.
   * @param inputImage
   * @param bandMax in pixels per cycle
   * @param bandMin in pixels per cycle
   * @return 
   */
  public static BufferedImage getBandPassedCopy(BufferedImage inputImage, 
      double minPixelsPerCycle, double maxPixelsPerCycle, boolean invertBandPass){
    float[][] imageAsFloatArray = bufferedImageRegionToFloatArray2d(inputImage, 
        0, inputImage.getWidth(), 0, inputImage.getHeight());
    FloatFFT_2D fft = new FloatFFT_2D(imageAsFloatArray.length, 
        imageAsFloatArray[0].length/2);
    fft.realForwardFull(imageAsFloatArray);
    bandPassFloatArray(imageAsFloatArray, 1/maxPixelsPerCycle, 1/minPixelsPerCycle, false, invertBandPass);
    fft.complexInverse(imageAsFloatArray, true);
    BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
    floatArray2dToImageRegion(imageAsFloatArray, outputImage, false, 0, 0);
    return outputImage;
  }


  /**
   * Produces a transparent overlay highlighting areas in the image that fall
   * within the frequency band 
   * @param inputImage
   * @param bandCenter in pixels per cycle
   * @param bandWidth in pixels per cycle
   * @return
   */
  public static BufferedImage getBandPassedOverlay(BufferedImage inputImage, 
      double bandMin, double bandMax){
    BufferedImage bandPassImage = getBandPassedCopy(inputImage, 
        bandMin, bandMax, false);
    return bandPassImage;
  }



  /**
   * Analyze a BufferedImage in rows and columns. Draw
   * the 2d discrete fourier transform into the image and/or draw
   * a 1d frequency histogram in the upper left-hand corner.
   * @param inputImage
   * @param columns
   * @param rows
   * @param anglesToSample Angles to sample the 2d FFT to generate the 1d histogram.
   * @param draw2dFFT
   * @param draw1dHistogram
   * @param plotColor TODO
   */
  public static void annotateBufferedImageWithFrequencies(BufferedImage inputImage, 
      BufferedImage outputImage, int columns,
      int rows, int anglesToSample, boolean draw2dFFT, boolean draw1dHistogram, Color plotColor) {
    for(int i=0; i < columns; i++){
      for(int j=0; j < rows; j++){
        double width = inputImage.getWidth()/columns;
        double x = i * width;
        double height = inputImage.getHeight()/rows;
        double y = j * height;
        Rectangle2D.Double regionBounds = new Rectangle2D.Double(x, y, width, height);
        annotateBufferedImageWithFrequencies(inputImage, outputImage, 
            regionBounds, anglesToSample, draw2dFFT, draw1dHistogram, plotColor);
      }
    }
  }



  /**
   * Analyze the frequency of a BufferedImage. Draw
   * the 2d discrete Fourier transform into the image and/or draw
   * a 1d frequency histogram in the upper left-hand corner.
   * @param inputImage
   * @param regionBounds
   * @param anglesToSample Angles to sample the 2d FFT to generate the 1d histogram.
   * @param draw2dFFT
   * @param draw1dHistogram
   */
  public static void annotateBufferedImageWithFrequencies(BufferedImage inputImage, 
      BufferedImage outputImage, int anglesToSample, boolean draw2dFFT, boolean draw1dHistogram, Color plotColor) {
    Rectangle2D.Double regionBounds = new Rectangle2D.Double(0.0, 0.0, 
        inputImage.getWidth(), inputImage.getHeight());
    annotateBufferedImageWithFrequencies(inputImage, outputImage, 
        regionBounds, anglesToSample, draw2dFFT, draw1dHistogram, plotColor);
  }






  /**
   * Perform an FFT on a BufferedImage and return the result as 
   *  a log-scaled array of floats with values between [0-255].
   *  @param image
   * @return
   */
  public static float[][] fftImageRegion(BufferedImage image){
    return fftImageRegion(image, 0, image.getWidth(), 0, image.getHeight());
  }

  /** 
   * Perform an FFT on section of a BufferedImage and return the result as 
   *  a log-scaled array of floats with values between [0-255].
   * @param image
   * @param x0
   * @param x1
   * @param y0
   * @param y1
   * @return
   */
  public static float[][] fftImageRegion(BufferedImage image, int x0, int x1, int y0, int y1){
    return fftAbsAndRescale(bufferedImageRegionToFloatArray2d(image, x0, x1, y0, y1));
  }

  /**
   * Sample a 2d frequency array along multiple orientations and average the components 
   * with the same frequency to create a 1d array. 
   * @param floatArray2d
   * @param numAnglesToSample
   * @return
   */
  public static float[] sample2dFrequenciesTo1dAtAngle(float[][] floatArray2d, int numAnglesToSample){
    int smallerDimension = Math.min(floatArray2d.length, floatArray2d[0].length / 2);

    int maxFreq = smallerDimension / 2;
    float[] frequencySums = new float[maxFreq];
    float[] frequencyCount = new float[maxFreq];

    for(int i=0; i < numAnglesToSample; i++){
      double sampleAngleDegrees = 180.0/numAnglesToSample * i;
      double sampleAngleRadians = Math.toRadians(sampleAngleDegrees);
      double unscaledX = Math.cos(sampleAngleRadians);
      double unscaledY = Math.sin(sampleAngleRadians);


      for(int j=0; j < smallerDimension / 2; j++){
        int x = (int) Math.round(unscaledX * j);
        if(x < 0) x += smallerDimension;
        int y = (int) Math.round(unscaledY * j);
        if(y < 0) y += smallerDimension;
        float value = floatArray2d[x][y*2];
        frequencySums[j] += value;
        frequencyCount[j]++;
      }
    }
    //normalize by count
    for(int i=0; i < maxFreq; i++){
      frequencySums[i] /= frequencyCount[i];
    }
    return frequencySums;
  }



  /**
   * Radially bin and average a 2d frequency array along all orientations and average the 
   * components with the same frequency to create a 1d array.
   * @param floatArray2d
   * @return A 1d array containing the average gain for each frequency. Indexed
   *   by (cycles/pixel) / longest dimension in pixels.
   */
  public static float[] radiallyBin2dFrequenciesTo1d(float[][] floatArray2d){

    //determine maximum frequency and use it to size the output array
    int w = floatArray2d.length;
    int h = (int) Math.ceil(floatArray2d[0].length / 2.0);
    int maxFreq = (int)Math.ceil(pythagoreanTherorem(w / 2.0, h / 2)) + 1;
    float[] frequencySums = new float[maxFreq];
    float[] frequencyCount = new float[maxFreq];

    //sample all pixels in the top half of the FFT (bottom is symmetric)
    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length/2; j = j+2){
        int x = (i >= floatArray2d.length / 2 ? i - floatArray2d.length : i);
        int y = j/2;
        //get frequency (cycles/image)
        double cyclesPerImage = Math.ceil(pythagoreanTherorem(x, y));
        //convert to (cycles/pixel) / normalized by the longest dimension in pixels
        double imagePixelsAlongOrientation = Math.min(
            Math.abs((w * cyclesPerImage) / (2 * x)),
            Math.abs((h * cyclesPerImage) / (2 * y)));
        double cyclesPerPixel = (cyclesPerImage / imagePixelsAlongOrientation );
        int normalizedCyclesPerPixel = (int)(cyclesPerPixel * (maxFreq - 1));
        float arrayVal = floatArray2d[i][j];
        frequencySums[normalizedCyclesPerPixel] += arrayVal;
        frequencyCount[normalizedCyclesPerPixel]++;
      }
    }

    //normalize by count
    for(int i=0; i < maxFreq; i++){
      frequencySums[i] /= Math.max(frequencyCount[i], 1);
    }
    return frequencySums;
  }

  /**
   * Print a binned summary of the frequency content of an image to standard out.
   * @param imageFileName
   */
  public static void printFrequencyBinsForImage(String imageFileName){
    BufferedImage inputImage = null;
    try {
      inputImage = ImageIO.read(new File(imageFileName));
    } catch (IOException e) {
    }
    printFrequencyBinsForBufferedImage(inputImage);
  }

  /**
   * Print a binned summary of the frequency content of a BufferedImage to standard out.
   * @param imageFileName
   */
  public static void printFrequencyBinsForBufferedImage(BufferedImage inputImage){
  
    float[][] imageFFT = fftImageRegion(inputImage);
    float[] gainIn1d = radiallyBin2dFrequenciesTo1d(imageFFT);
    //float[] gainIn1d = sample2dFrequenciesTo1dAtAngle(imageFFT,1);
  
    int numBins = (int) Math.ceil(Math.log(gainIn1d.length*2)/Math.log(2));
    float[] frequencyBins = new float[numBins];
    float[] binCounts = new float[numBins];
    for(int i=1; i < gainIn1d.length; i++){
      float pixelsPerCycle = gainIn1d.length*2/i;
      float gain = gainIn1d[i];
      int bin = (int)Math.round(Math.log(pixelsPerCycle)/Math.log(2));
      frequencyBins[bin-1] += gain;
      binCounts[bin-1]++;
    }
    System.out.println("Normalized Gain");
    System.out.println("Bin (lower limit)\tGain");
    for(int bi=0; bi < frequencyBins.length; bi++){
      System.out.println(Math.pow(2, bi+1) + "\t" + frequencyBins[bi]/(binCounts[bi]==0?1:binCounts[bi]));
    }
  }

  /**
   * Takes a BufferedImage and returns a float array corresponding to 
   *  the 1d frequency distribution of the image.
   * @param inputImage
   * @return
   */
  public static float[] get1dFrequencies(BufferedImage inputImage){
    float[][] imageFFT = bufferedImageRegionToFloatArray2d(inputImage, 0, 
        inputImage.getWidth(), 0, inputImage.getHeight());
    FloatFFT_2D fft = new FloatFFT_2D(imageFFT.length, imageFFT[0].length/2);
    fft.realForwardFull(imageFFT);
    absFloatArray(imageFFT);
    float[] frequency1d = radiallyBin2dFrequenciesTo1d(imageFFT);
    return frequency1d;
  }

  /**
   * Paints the contents of a 2d float array into a subsection of a PImage. 
   * @param floatArray2d
   * @param image
   * @param recenter
   * @param x0
   * @param y0
   */
  public static void floatArray2dToImageRegion(float[][] floatArray2d, BufferedImage image,
      boolean recenter, int x0, int y0){
    int imageWidth = floatArray2d.length;
    int imageHeight = (floatArray2d.length > 0 ? floatArray2d[0].length : 0) / 2;

    for(int i=0; i < Math.min(floatArray2d.length, image.getWidth() - x0); i++){
      for(int j=0; j < Math.min(floatArray2d[i].length / 2, image.getHeight() - y0); j++){
        float colorVal = floatArray2d[i][j*2];
        Color colorRGB = Color.getHSBColor(1, 0, colorVal/255); 
        int x = x0 + (recenter ? (i + imageWidth/2) % imageWidth: i);
        int y = y0 + (recenter ? (j + imageHeight/2) % imageHeight : j);
        image.setRGB(x, y, colorRGB.getRGB());
      }
    }
  }

  /**
   * Draw a histogram into an image based on the array provided.
   * @param image
   * @param x
   * @param y
   * @param width
   * @param height
   * @param bins
   */
  public static void drawSimpleHistogram(BufferedImage image, int x, int y, int width, int height, float[] bins){
    float binMin = 0;
    float binAbsMax = Float.NEGATIVE_INFINITY;
    float binMax = Float.NEGATIVE_INFINITY;
    for(int b=1; b<bins.length;b++){
      if(bins[b] < binMin) binMin = bins[b];
      if(bins[b] > binMax) binMax = bins[b];
      if(Math.abs(bins[b]) > binAbsMax) binAbsMax = Math.abs(bins[b]);
    }
    int scaledWidth = Math.max(1, width / bins.length);

    //draw green lines showing the frequencies outside of close FOV (full, and focused). 
    double nearFOV = convertVisualAngleToFrequency(120f, bins.length * 2,
        DISPLAY_PIXEL_DENSITY, 50);
    double nearFocus = convertVisualAngleToFrequency(10f, bins.length * 2,
        DISPLAY_PIXEL_DENSITY, 50);
    //draw yellow reference lines showing frequencies corresponding to 10, 20, and 40 pixel periods
    double period10pix = convertPeriodToFrequency(bins.length * 2, 10);
    double period20pix = convertPeriodToFrequency(bins.length * 2, 20);
    double period40pix = convertPeriodToFrequency(bins.length * 2, 40);
    //show a blue line at the frequency that should give the smallest discernable at 3m
    double farSmallestDiscernable = convertVisualAngleToFrequency(0.005f, bins.length * 2,
        DISPLAY_PIXEL_DENSITY, 300);
    for(int j=0; j < height; j++){
      image.setRGB((int) (x + nearFOV * scaledWidth), y + j, Color.green.getRGB());
      image.setRGB((int) (x + nearFocus * scaledWidth), y + j, Color.green.getRGB());
      image.setRGB((int) (x + period10pix * scaledWidth), y + j, Color.yellow.getRGB());
      image.setRGB((int) (x + period20pix * scaledWidth), y + j, Color.yellow.getRGB());
      image.setRGB((int) (x + period40pix * scaledWidth), y + j, Color.yellow.getRGB());
      //image.setRGB((int) (x + farSmallestDiscernable * scaledWidth), y + j, Color.blue.getRGB());
    }

    //draw histogram in red
    for(int b=1; b<bins.length;b++){
      int scaledHeight = (int) (height / (binAbsMax - binMin) * Math.abs(bins[b]) + 1);
      for(int j=0; j < scaledHeight; j++){
        for(int i=0; i < scaledWidth; i++){
          image.setRGB(x + b*scaledWidth + i, y + height - scaledHeight + j, Color.red.getRGB());
        }
      }
    }	
  }



  /**
   * Convert a subsection of a BufferedImage to a 2d float array suitable for FFT.
   * @param inputImage
   * @param x0
   * @param x1
   * @param y0
   * @param y1
   * @return
   */
  protected static float[][] bufferedImageRegionToFloatArray2d(BufferedImage bufferedImage, int x0, int x1, int y0, int y1){
    int imageWidth = x1 - x0;
    int imageHeight = y1 - y0;
    float[][] outputArray = new float[imageWidth][imageHeight*2];
    for(int i=0; i < Math.min(bufferedImage.getWidth()-x0,x1-x0); i++){
      for(int j=0; j < Math.min(bufferedImage.getHeight()-y0,y1-y0); j++){
        Color c = new Color(bufferedImage.getRGB(x0+i, y0+j));
        outputArray[i][j] = (c.getRed() + c.getGreen() + c.getBlue())/3;
      }
    }
    return outputArray;		
  }


  /**
   * @param imageAsFloatArray
   * @return
   */
  protected static float[][] fftAbsAndRescale(float[][] imageAsFloatArray){
    FloatFFT_2D fft = new FloatFFT_2D(imageAsFloatArray.length, imageAsFloatArray[0].length/2);
    fft.realForwardFull(imageAsFloatArray);
    absFloatArray(imageAsFloatArray);
    rescaleFloatArray(imageAsFloatArray, 1, 10000);
    logScaleFloatArray(imageAsFloatArray);
    rescaleFloatArray(imageAsFloatArray, 0, 255);
    return imageAsFloatArray;
  }


  protected static float[][] fftReverse(float[][] fftFloatArray){
    FloatFFT_2D fft = new FloatFFT_2D(fftFloatArray.length, fftFloatArray[0].length/2);
    fft.realInverseFull(fftFloatArray, true);
    return fftFloatArray;
  }

  /**
   * Rescale the real values of a 2d FFT array so they fall between the min and max values.
   * @param floatArray2d
   * @param minVal
   * @param maxVal
   */
  protected static void rescaleFloatArray(float[][] floatArray2d, float minVal, float maxVal){
    float dataMin = Float.POSITIVE_INFINITY;
    float dataMax = Float.NEGATIVE_INFINITY;
    //get min/max
    for(int i=0; i < floatArray2d.length; i++){ 
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        float arrayVal = floatArray2d[i][j];
        if(!Float.isNaN(arrayVal)){
          dataMin = Math.min(dataMin, arrayVal);
          dataMax = Math.max(dataMax, arrayVal);
        }
      }
    }
    float domain = dataMax - dataMin;
    float range = maxVal - minVal;
    //rescale
    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        float arrayVal = floatArray2d[i][j];
        arrayVal = (arrayVal - dataMin) / domain * range + minVal;
        floatArray2d[i][j] = arrayVal;
      }
    }
  }


  /**
   * Logarithmically scale the real values in a 2d FFT array.
   * @param floatArray2d
   */
  protected static void logScaleFloatArray(float[][] floatArray2d){	
    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        float arrayVal = floatArray2d[i][j];
        arrayVal = (float) Math.log10(arrayVal);
        floatArray2d[i][j] = arrayVal;
      }
    }
  }


  /**
   * Remove real values in a 2d FFT array that are outside of the supplied thresholds
   * and replace them with the threshold values.
   * @param floatArray2d
   * @param minThreshold
   * @param maxThreshold
   */
  protected static void clipFloatArray(float[][] floatArray2d,
      float minThreshold, float maxThreshold){
    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        float arrayVal = floatArray2d[i][j];
        if(arrayVal < minThreshold) floatArray2d[i][j] = minThreshold;
        else if(arrayVal > maxThreshold) floatArray2d[i][j] = maxThreshold;
      }
    }
  }

  /**
   * Replace all real values in a 2d FFT arry with their absolute value.
   * @param floatArray2d
   */
  protected static void absFloatArray(float[][] floatArray2d){
    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        floatArray2d[i][j] = Math.abs(floatArray2d[i][j]);
      }
    }
  }



  /**
   * Band-Pass filter a 2d FFT array, keeping only values within the range specified.
   * @param floatArray2d
   * @param min
   * @param max
   */
  protected static void bandPassFloatArray(float[][] floatArray2d, 
      double minCyclesPerPixel, double maxCyclesPerPixel, boolean useGaussianBand, boolean invertBandPass){

    //determine maximum frequency and use it to size the output array
    int w = floatArray2d.length;
    int h = (int) Math.ceil(floatArray2d[0].length / 2.0);

    for(int i=0; i < floatArray2d.length; i++){
      for(int j=0; j < floatArray2d[i].length; j = j+2){
        int x = (i >= floatArray2d.length / 2 ? i - floatArray2d.length : i);
        int y = (j >= floatArray2d[i].length / 4 ? j/2 - floatArray2d[i].length / 2 : j/2);
        //get frequency (cycles/image)
        double cyclesPerImage = Math.ceil(pythagoreanTherorem(x, y));
        //convert to (cycles/pixel)
        double imagePixelsAlongOrientation = Math.min(
            Math.abs((w * cyclesPerImage) / (2 * x)),
            Math.abs((h * cyclesPerImage) / (2 * y)));
        double cyclesPerPixel = (cyclesPerImage / imagePixelsAlongOrientation / 2);

        if(useGaussianBand){ 
          //FIXME: Gaussian-band passed images don't seem to survive the conversion back to the spatial domain - do I need to treat the imaginary component differently?
          double gaussian = gaussian(cyclesPerPixel, 1.0, (minCyclesPerPixel + maxCyclesPerPixel)/2, (maxCyclesPerPixel - minCyclesPerPixel)/10);
          floatArray2d[i][j] *= gaussian;
          floatArray2d[i][j + 1] *= gaussian;
        }
        else{
          boolean removeThis = invertBandPass ?          
              (cyclesPerPixel > minCyclesPerPixel && cyclesPerPixel < maxCyclesPerPixel) :
                (cyclesPerPixel < minCyclesPerPixel || cyclesPerPixel > maxCyclesPerPixel);
              if(removeThis){
                floatArray2d[i][j] = 0;
                floatArray2d[i][j+1] = 0;
              }
        }
      }
    }
  }


  protected static double gaussian(double x, double peakHeight, double center, double bellWidth){
    double num = -Math.pow(x-center, 2.0);
    double den = 2.0 * Math.pow(bellWidth, 2.0);
    double exp = num/den;
    double y = Math.pow(peakHeight*Math.E,exp);
    return y;
  }


  /**
   * c = sqrt(a^2 + b^2)
   * @param a
   * @param b
   * @return
   */
  protected static double pythagoreanTherorem(double a, double b){
    return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
  }


  /**
   * Convert a fequency in a region viewed from a given distance into a visual angle.
   * @param frequency
   * @param regionSizeInPixels
   * @param cmPerPixel
   * @param distanceInCM
   * @return
   */
  protected static double convertFrequencyToVisualAngle(float frequency, 
      float regionSizeInPixels, float cmPerPixel, float distanceInCM ){
    float periodInPixels = regionSizeInPixels / frequency;
    float periodInCM = periodInPixels * cmPerPixel;
    double visualAngleRadians = 2*Math.atan(periodInCM /(2*distanceInCM));
    double visualAngleDegrees = Math.toDegrees(visualAngleRadians);
    return visualAngleDegrees;
  }


  /**
   * Convert a visual angle seen from some distance into a fequency.
   * @param visualAngleDegrees
   * @param regionSizeInPixels
   * @param cmPerPixel
   * @param distanceInCM
   * @return
   */
  protected static double convertVisualAngleToFrequency(float visualAngleDegrees,
      float regionSizeInPixels, float cmPerPixel, float distanceInCM){
    float displayWidthInCM = regionSizeInPixels * cmPerPixel;
    double visualAngleInRadians = Math.toRadians(visualAngleDegrees);
    double periodInCM = 2 * distanceInCM * Math.tan(visualAngleInRadians / 2);
    double frequency = displayWidthInCM / periodInCM; 
    return frequency;
  }

  /**
   * Convert a frequency in a region to a period.
   * @param displayWidth
   * @param period
   * @return
   */
  protected static double convertPeriodToFrequency(float regionSize, float period){
    return regionSize / period;
  }

}
