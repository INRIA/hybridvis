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

public class MathUtils {
    public static double EPSILON = 1e-30;



    /// <summary>
    ///  Rounds an input value down according to its logarithm. The method takes the
    /// floor of the logarithm of the value and then uses the resulting value as an
    ///  exponent for the base value.
    /// </summary>
    /// <param name="x">the number for which to compute the logarithm floor.</param>
    /// <param name="b">the base of the logarithm.</param>
    /// <returns>the rounded-by-logarithm value.</returns>
    public static double logFloor(double x, double b)
    {
        if(x > 0){
            return Math.pow(b, Math.floor(Math.log(x) / Math.log(b)));
        }
        else{
            return -Math.pow(b, -Math.floor(-Math.log(-x) / Math.log(b)));
        }
    }

    /// <summary>
    /// Finds where in the range between min and max a specific value is. 
    /// </summary>
    /// <param name="value"></param>
    /// <param name="min"></param>
    /// <param name="max"></param>
    /// <returns>The position within a range 0..1</returns>
    public static double inverseLinearInterpolation(double value, double min, double max)
    {
        double denominator = (max - min);

        //check if the denominator is around 0 and return 0 if it is
        if (denominator < EPSILON && denominator > -EPSILON) return 0;

        return (value - min) / denominator;
    }

    /// <summary>
    /// Computes a value between min and max in regards to the fraction provided.
    /// </summary>
    /// <param name="fraction"></param>
    /// <param name="min"></param>
    /// <param name="max"></param>
    /// <returns></returns>
    public static double linearInterpolation(double fraction, double min, double max)
    {
        return min + fraction * (max - min);
    }

    /// <summary>
    /// Pierre wrote this interpolation function so the variable names are not my fault
    /// </summary>
    /// <param name="x"></param>
    /// <param name="x0"></param>
    /// <param name="y0"></param>
    /// <param name="x1"></param>
    /// <param name="y1"></param>
    /// <returns></returns>
    public static double interpolate(double x, double x0, double y0, double x1, double y1)
    {
        return y0 + (x - x0) / (x1 - x0) * (y1 - y0);
    }

    /// <summary>
    /// Again Pierre's function. I guess it calculates the angle between two lines given by three points?.
    /// </summary>
    /// <param name="x0"></param>
    /// <param name="y0"></param>
    /// <param name="x1"></param>
    /// <param name="y1"></param>
    /// <param name="x2"></param>
    /// <param name="y2"></param>
    /// <returns></returns>
    public static double angle(double x0, double y0, double x1, double y1, double x2, double y2)
    {
        double dotProduct = (x1 - x0) * (x2 - x1) + (y1 - y0) * (y2 - y1);
        double norm1 = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        double norm2 = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        return Math.acos(dotProduct / (norm1 * norm2));
    }

    /// <summary>
    /// Also Pierre's code. Something is intersection something else
    /// </summary>
    /// <param name="x1"></param>
    /// <param name="y1"></param>
    /// <param name="x2"></param>
    /// <param name="y2"></param>
    /// <param name="x3"></param>
    /// <param name="y3"></param>
    /// <param name="x4"></param>
    /// <param name="y4"></param>
    /// <returns></returns>
    public static double[] intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
    {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0) return null;
        double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        return new double[] { xi, yi };
    }

    public static void bezier(double x0, double y0, double x1, double y1, double x2, double y2, double t, double[] result)
    {
        result[0] = (1 - t) * (1 - t) * x0 + 2 * (1 - t) * t * x1 + t * t * x2;
        result[1] = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * y1 + t * t * y2;
    }

    /// <summary>
    /// Twice the signed area of the triangle (p0, p1, p2)
    /// </summary>
    /// <param name="p0">A first point of the triangle </param>
    /// <param name="p1">A second point of the triangle</param>
    /// <param name="p2">A third point of the triangle</param>
    /// <returns>Twice the signed area of the triangle (p0, p1, p2)</returns>
//    public static double Area2(Point p0, Point p1, Point p2)
//    {
//        return p0.X * (p1.Y - p2.Y) + p1.X * (p2.Y - p0.Y) + p2.X * (p0.Y - p1.Y);
//    }

//    /// <summary>
//    /// Based on code from here:http://oldschooldotnet.blogspot.com/2008/11/c-latin-squares-or-shuffling-2d-array.html
//    /// </summary>
//    /// <param name="startPattern"></param>
//    /// <returns></returns>
//    public static int[,] getLatinSquare(int[] Pattern)
//    {
//        try
//        {
//            //Pattern = Shuffle(Pattern);
//            var LatinSquare = new int[Pattern.Length, Pattern.Length];
//
//            //Use the first row as a pattern for the rest 
//            //of the Latin Square
//            for (int i = 0; i < Pattern.Length; i++)
//            {
//                {
//                    LatinSquare[i, 0] = Pattern[i];
//                }
//            }
//
//            for (int x = 0; x < Pattern.Length; x++)
//            {
//                for (int y = 1; y < Pattern.Length; y++)
//                {
//                    LatinSquare[Pattern[y] - 1, y] = LatinSquare[LatinSquare[x, 0] - 1, 0];
//                }
//
//                Pattern = RotatePattern(Pattern);
//            }
//
//            return (int[,])LatinSquare;
//        }
//        catch (Exception e)
//        {
//            Console.WriteLine(e);
//            return null;
//        }
//    }

//    private static int[] Shuffle(int[] OriginalArray)
//    {
//        var matrix = new System.Collections.SortedList();
//        var r = new Random();
//
//        for (int x = 0; x <= OriginalArray.GetUpperBound(0); x++)
//        {
//            int i = r.Next();
//
//            if (!matrix.ContainsKey(i))
//            {
//                matrix.Add(i, OriginalArray[x]);
//            }
//        }
//
//        var OutputArray = new int[OriginalArray.Length];
//
//        var counter = 0;
//        foreach (DictionaryEntry entry in matrix)
//        {
//            OutputArray[counter++] = (int)entry.Value;
//        }
//
//        return OutputArray;
//    }

//    private static int[] RotatePattern(int[] Pattern)
//    {
//        int temp = Pattern[0];
//
//        ; for (int y = 0; y < Pattern.Length - 1; y++)
//        {
//            Pattern[y] = Pattern[y + 1];
//        }
//
//        Pattern[Pattern.Length - 1] = temp;
//
//        return Pattern;
//    }

//    private static void PrintLatinSquare(int[,] LatinSquare)
//    {
//        int side = (int)Math.Sqrt(LatinSquare.Length);
//        //Print out the Latin Square
//        for (int i = 0; i < side; i++)
//        {
//            for (int j = 0; j < side; j++)
//            {
//                Console.Write(LatinSquare[j, i].ToString().PadLeft(3));
//            }
//            Console.WriteLine();
//        }
//    }

    public static double DegreeToRadian(double angle)
    {
        return Math.PI * angle / 180.0;
    }
    
    public static double round(double n, int precision) {
	int mul = (int)Math.pow(10, precision);
	return Math.round(n * mul) / (double)mul;
    }



}
