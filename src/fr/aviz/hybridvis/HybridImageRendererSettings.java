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

package fr.aviz.hybridvis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import fr.aviz.hybridvis.utils.settings.Configurable;
import fr.aviz.hybridvis.utils.settings.Separator;
import fr.aviz.hybridvis.utils.settings.Settings;

/**
 * 
 * @author dragice
 *
 */
public class HybridImageRendererSettings extends Settings {

  @Configurable
  boolean drawBackground = true;

  @Separator
  @Configurable
  boolean drawNearImage = true;

  @Configurable(min=0, max=100, majorTick=20, pow=1.5)
  int hipassRadius = 30; // in pixels. Set to 0 to deactivate.

  @Configurable
  boolean transparentHipass = false;

  @Configurable(min=0, max=2, majorTick=1, minorTick=0.25)
  double hipassContrast = 1.5;

  @Configurable(min=0, max=2, majorTick=1, minorTick=0.25)
  double hipassBrightness = 1.5;

  @Configurable(min=0, max=1, majorTick=0.5, minorTick=0.25)
  double nearImageOpacity = 1.0;

  @Separator
  @Configurable
  boolean drawFarImage = true;

  @Configurable(min=0, max=100, majorTick=20, pow=1.5)
  int blurRadius = 30;   // in pixels. Set to 0 to deactivate.

  @Configurable(min=0, max=1, majorTick=0.5, minorTick=0.25)
  double farImageOpacity = 0.5;

  @Separator
  @Configurable(min=0, max=2.5, majorTick=1, minorTick=0.25)
  double postContrast = 2;

  @Configurable(min=0, max=2, majorTick=1, minorTick=0.25)
  double postBrightness = 0.77;

  @Separator
  @Configurable
  boolean drawBezels = true;

  @Configurable
  boolean drawSettings = false;
  
  @Separator
  @Configurable
  boolean drawPowerSpectrum = true;

  @Configurable
  boolean logScalePowerSpectrumY = true;

  //@Configurable
  boolean highlightFrequencyBand = false;
  
  //@Configurable(min=2, max=200, majorTick=100, minorTick=20)
  double frequencyBandMidpoint = 10;
  
  //@Configurable(min=2, max=50, majorTick=10, minorTick=5)
  double frequencyBandWidth = 2;

  // Simulated display path, relative to project path.
  String displayPath = "";

  // Near image path, relative to project path.
  String nearImagePath = "";

  // Far image path, relative to project path.
  String farImagePath = "";
  
  
  public HybridImageRendererSettings() {
    super("Hybrid renderer settings");
  }

  /**
   * Relativizes <pre>path</pre> w.r.t. the parent directory of <pre>projectPath</pre>.
   * @param projectPath - Absolute path of the project (including file name).
   * @param path - Path to relativize.
   */
  private static String relativize(Path projectPath, Path path){
    if(path.isAbsolute()){
      return projectPath.getParent().relativize(path).toString();
    } else {
      return path.toString();
    }
  }

  /**
   * Resolve 'path' from the parent folder of 'projectPath'.
   * @param projectPath - Absolute path of the project (including file name).
   * @param path - Path to resolve.
   */
  private static String resolve(Path projectPath, Path path){
    return projectPath.getParent().resolve(path).toString();
  }

  /**
   * Persists this settings object to a file. The paths saved by this settings object
   * (e.g. near and far image, display settings) are stored relative to <pre>path</pre>.
   * @param path - path of the file to persist this object to.
   * @throws IOException - if an error happens while writing the file.
   */
  public void persist(File path) throws IOException{
    // New settings added to HybridImageRendererSettings should be reflected in this method.
    Properties props = new Properties();
    final Path projectPath = path.toPath();
    props.setProperty("displayPath", relativize(projectPath, new File(displayPath).toPath()));
    props.setProperty("nearImagePath", relativize(projectPath, new File(nearImagePath).toPath()));
    props.setProperty("farImagePath", relativize(projectPath, new File(farImagePath).toPath()));

    props.setProperty("drawBackground", ts(drawBackground));
    props.setProperty("drawNearImage", ts(drawNearImage));
    props.setProperty("hipassRadius", ts(hipassRadius));
    props.setProperty("transparentHipass",ts(transparentHipass));
    props.setProperty("hipassContrast", ts(hipassContrast));
    props.setProperty("hipassBrightness", ts(hipassBrightness));
    props.setProperty("nearImageOpacity", ts(nearImageOpacity));
    props.setProperty("drawFarImage", ts(drawFarImage));
    props.setProperty("blurRadius", ts(blurRadius));
    props.setProperty("farImageOpacity", ts(farImageOpacity));
    props.setProperty("postContrast", ts(postContrast));
    props.setProperty("postBrightness", ts(postBrightness));
    props.setProperty("drawBezels", ts(drawBezels));
    props.setProperty("drawSettings", ts(drawSettings));
    props.setProperty("drawPowerSpectrum", ts(drawPowerSpectrum));
    props.setProperty("logScalePowerSpectrumY", ts(logScalePowerSpectrumY));
    props.setProperty("highlightFrequencyBand", ts(highlightFrequencyBand));
    props.setProperty("frequencyBandMidpoint", ts(frequencyBandMidpoint));
    props.setProperty("frequencyBandWidth", ts(frequencyBandWidth));
    
    FileOutputStream fos = null;
    try{
      fos = new FileOutputStream(path);
      props.store(fos, "Hybrid image project properties");
    } finally{
      if(fos != null){
        fos.close();
      }
    }
  }

  // toString (uses implicit object boxing for base types)
  private static String ts(Object obj){
    return obj.toString();
  }

  /**
   * Loads and returns an HybridImageRendererSettings from <pre>path</pre>.
   * Paths stored in this settings file are resolved relative to <pre>path</pre> at
   * load time.
   * @throws IOException - if an error happens while reading the file.
   */
  public static HybridImageRendererSettings fromFile(File path) throws IOException{
    HybridImageRendererSettings retval = new HybridImageRendererSettings();
    Properties props = new Properties();
    FileInputStream fis = null;
    try{
      fis = new FileInputStream(path);
      props.load(fis);
    } finally{
      fis.close();
    }
    final Path loadPath = path.toPath();
    retval.displayPath = resolve(loadPath, new File(props.getProperty("displayPath")).toPath());
    retval.nearImagePath = resolve(loadPath, new File(props.getProperty("nearImagePath")).toPath());
    retval.farImagePath = resolve(loadPath, new File(props.getProperty("farImagePath")).toPath());

    retval.drawBackground = Boolean.parseBoolean(props.getProperty("drawBackground"));
    retval.drawNearImage = Boolean.parseBoolean(props.getProperty("drawNearImage"));
    retval.hipassRadius = Integer.parseInt(props.getProperty("hipassRadius"));
    retval.transparentHipass = Boolean.parseBoolean(props.getProperty("transparentHipass"));
    retval.hipassContrast = Double.parseDouble(props.getProperty("hipassContrast"));
    retval.hipassBrightness = Double.parseDouble(props.getProperty("hipassBrightness"));
    retval.nearImageOpacity = Double.parseDouble(props.getProperty("nearImageOpacity"));
    retval.drawFarImage = Boolean.parseBoolean(props.getProperty("drawFarImage"));
    retval.blurRadius = Integer.parseInt(props.getProperty("blurRadius"));
    retval.farImageOpacity = Double.parseDouble(props.getProperty("farImageOpacity"));
    retval.postContrast = Double.parseDouble(props.getProperty("postContrast"));
    retval.postBrightness = Double.parseDouble(props.getProperty("postBrightness"));
    retval.drawBezels = Boolean.parseBoolean(props.getProperty("drawBezels"));
    retval.drawSettings = Boolean.parseBoolean(props.getProperty("drawSettings"));
    retval.drawPowerSpectrum = Boolean.parseBoolean(props.getProperty("drawPowerSpectrum"));
    retval.logScalePowerSpectrumY = Boolean.parseBoolean(props.getProperty("logScalePowerSpectrumY"));
    retval.highlightFrequencyBand = Boolean.parseBoolean(props.getProperty("highlightFrequencyBand"));
    retval.frequencyBandMidpoint = Double.parseDouble(props.getProperty("frequencyBandMidpoint"));

    return retval;
  }
}

