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

import java.util.ArrayList;
import java.util.List;

/**
 * A class that computes 'nice' labels for graph axes.
 * Adapted from Paul S. Eckbert chapter of Graphics Gems, p. 61
 * and the corresponding C implementation p. 657.
 *
 * @param min range minimum
 * @param max range maximum
 * @param nLabels desired number of labels
 */
public class NiceLabels {
  private final double min;
  private final double max;
  private final long nLabels;

  public NiceLabels(double min, double max, long nLabels){

    if(min >= max){
      throw new IllegalArgumentException("Expected max > min");
    }
    this.min = min;
    this.max = max;

    if(nLabels < 2){
      throw new IllegalArgumentException("Expected nLabels >= 2");
    }
    this.nLabels = nLabels;
  }

  public List<Double> computeLabels(){
    List<Double> retval = new ArrayList<Double>();
    double range = niceNum(this.max - this.min, false);
    double d = niceNum(range / (nLabels - 1), true); //tick mark spacing
    double graphMin = Math.floor(this.min / d) * d;
    double graphMax = Math.ceil(this.max / d) * d;
    long nfrac;
    for(double x = graphMin; x < graphMax + 0.5*d; x += d){
      retval.add(x);
    }
    return retval;
  }

  /**
   * Finds a 'nice' number approximately equal to x.
   * Rounds the number if round is true, or take ceiling otherwise.
   */
  public static double niceNum(double x, boolean round){
    long exp = (long)Math.floor(Math.log10(x));
    double frac = x / Math.pow(10, exp);
    double niceFrac = 0;
    if(round){
      if(frac < 1.5) 
        niceFrac = 1;
      else if(frac < 3) 
        niceFrac = 2;
      else if(frac < 7)
        niceFrac = 5;
      else 
        niceFrac = 10;
    } else {
      if(frac <= 1)
        niceFrac = 1;
      else if(frac <= 2)
        niceFrac = 2;
      else if(frac <= 5)
        niceFrac = 5;
      else
        niceFrac = 10;
    }
    return niceFrac * Math.pow(10, exp);
  }
}
