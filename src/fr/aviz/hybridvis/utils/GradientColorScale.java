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

import java.awt.Color;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Gradient color scale interpolator. 
 * @author wjwillett
 *
 */
public class GradientColorScale {

  protected Hashtable<Float, Color> colorTable = new Hashtable<Float, Color>();
  protected List<Float> orderedKeys;
  
  public GradientColorScale() {}
  
  
  /**
   * Add a color.
   * @param keyValue
   * @param keyColor
   */
  public void addColor(float keyValue, Color keyColor) {
    colorTable.put(keyValue, keyColor);
    Enumeration<Float> keys = colorTable.keys();
    orderedKeys = Collections.list(keys);
    Collections.sort(orderedKeys);
  } 

  /**
   * Get an interpolated color based on the current colors in the scale.
   * @param value
   * @param coloralpha
   * @return
   */
  public Color getColor(float value, float coloralpha) { 
    if (value <= orderedKeys.get(0)) {
      return colorTable.get(orderedKeys.get(0)); 
    }
    for (int ki = 0; ki < orderedKeys.size() - 1; ki++) {
      if (value > orderedKeys.get(ki) && value <= orderedKeys.get(ki+1)) { 
        float keyValueFraction = (value - orderedKeys.get(ki)) / (orderedKeys.get(ki+1) - orderedKeys.get(ki)); 
        return GUIUtils.mix(colorTable.get(orderedKeys.get(ki)),
            colorTable.get(orderedKeys.get(ki+1)),
            keyValueFraction);
      }
    }
    return colorTable.get(orderedKeys.get(orderedKeys.size() - 1));
  }
}
