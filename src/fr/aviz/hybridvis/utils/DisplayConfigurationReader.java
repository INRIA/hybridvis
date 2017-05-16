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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import fr.aviz.hybridvis.display.DisplayConfiguration;

public class DisplayConfigurationReader {
  public static final String KEY_X_RES        = "xResolution";
  public static final String KEY_Y_RES        = "yResolution";
  public static final String KEY_X_TILES      = "xTiles";
  public static final String KEY_Y_TILES      = "yTiles";
  public static final String KEY_PIXEL_WIDTH  = "pixelWidth";
  public static final String KEY_PIXEL_HEIGHT = "pixelHeight";

  public static DisplayConfiguration fromStream(InputStream in) throws IOException {
    Properties props = new Properties();
    props.load(in);

    int xResolution    = Integer.parseInt(props.getProperty(KEY_X_RES));
    int yResolution    = Integer.parseInt(props.getProperty(KEY_Y_RES));
    int xTiles         = Integer.parseInt(props.getProperty(KEY_X_TILES));
    int yTiles         = Integer.parseInt(props.getProperty(KEY_Y_TILES));
    double pixelWidth  = Double.parseDouble(props.getProperty(KEY_PIXEL_WIDTH));
    double pixelHeight = Double.parseDouble(props.getProperty(KEY_PIXEL_HEIGHT));

    return new DisplayConfiguration(xResolution, 
                                    yResolution,
                                    xTiles,
                                    yTiles,
                                    pixelWidth,
                                    pixelHeight);
  }

  public static DisplayConfiguration fromFile(File f) throws IOException {
    FileInputStream is = null;
    DisplayConfiguration retval;
    try {
      is = new FileInputStream(f);
      retval = fromStream(is);
    } finally {
      if(is != null){
        is.close();
      }
    }
    return retval;
  }
}

