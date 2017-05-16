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

import fr.aviz.hybridvis.display.WILDDisplay;

public class GeneratePaperFigure {

	// Values for generating a figure for the paper
	// TODO: clean up this
	static final double[] hybridMap1Freq = new double[] {
		1.8906353,
		2.7799795,
		3.694318,
		6.9366355,
		11.723263,
		17.246567,
		24.054293,
		30.781824
	};

	static final double[] hybridMap2Freq = new double[] {
		0.0433358,
		0.12488108,
		0.639092,
		4.8306255,
		15.721668,
		27.172413,
		41.67002,
		51.392845
	};
	
	static final double[] hybridMap7Freq = new double[] {
		1.1563975,
		1.714072,
		2.173269,
		2.94884,
		2.3152258,
		1.3626577,
		1.0179069,
		1.2110574,
	};

	static final double[] hybridMap8Freq = new double[] {
		0.026651073,
		0.037733063,
		0.08918452,
		1.1155498,
		10.062784,
		24.670338,
		40.823265,
		51.15639
	};
	
//	static final double[] hybridMap6Freq = new double[] {
//		0.028266437,
//		0.03650969,
//		0.08457244,
//		0.88054657,
//		7.955077,
//		19.90843,
//		33.60587,
//		42.77417
//	};
//
//	static final double[] hybridMap3Freq = new double[] {
//		0.44771746,
//		0.6696108,
//		0.8652685,
//		1.5746064,
//		7.1057453,
//		18.565718,
//		32.403656,
//		42.041115
//	};
//	
//	static final double[] hybridMap4Freq = new double[] {
//		1.1385126,
//		1.6272323,
//		2.1695144,
//		3.681085,
//		9.135636,
//		21.538603,
//		36.45933,
//		46.468853
//	};

//	static final double[] hybridMap5Freq = new double[] {
//		2.0625641,
//		2.9336362,
//		3.896813,
//		5.9772944,
//		6.084126,
//		5.53963,
//		6.009614,
//		6.940717
//	};

	static final double[] pixelsPerCycle = new double[] {2, 4, 8, 16, 32, 64, 128, 256};

	public static void main(String[] args) {
		
		//ImageStatistics.printFrequencyBinsForImage("screenshots/HybridMap-1.png");
		//ImageStatistics.printFrequencyBinsForImage("screenshots/whitenoise.png");
		//ImageStatistics.printFrequencyBinsForImage("screenshots/HybridMap-7.png");
		//ImageStatistics.printFrequencyBinsForImage("screenshots/HybridMap-8.png");
		
		//showPixelCSFs(new WILDDisplay(), 1, true, true, null);
		//showPixelCSFs(new AppleCinemaDisplay(), 0.0315, true, true, null);
		//CSF.showPixelCSFs(new WILDDisplay(), 0.9, true, true, hybridMap4Freq);
		
//CSF.showPixelCSFs(new WILDDisplay(), 0.54, true, true, hybridMap1Freq);
//		CSF.showPixelCSFs(new WILDDisplay(), 2.39, true, true, hybridMap2Freq);
		
//CSF.showPixelCSFs(new WILDDisplay(), 0.88, true, true, hybridMap7Freq);
		//CSF.showPixelCSFs(new WILDDisplay(), 4.58, true, true, hybridMap8Freq);
		
//		CSF.showPixelCSFs(new WILDDisplay(), 0.185, true, true, pixelsPerCycle, hybridMap1Freq);
//		CSF.showPixelCSFs(new WILDDisplay(), 0.185, true, true, pixelsPerCycle, hybridMap2Freq);
//
//		CSF.showPixelCSFs(new WILDDisplay(), 0.29, true, true, pixelsPerCycle, hybridMap7Freq);
//		CSF.showPixelCSFs(new WILDDisplay(), 0.29, true, true, pixelsPerCycle, hybridMap8Freq);

		CSF.showPixelCSFs(new WILDDisplay(), 0.207, true, true, pixelsPerCycle, hybridMap1Freq);
		CSF.showPixelCSFs(new WILDDisplay(), 0.207, true, true, pixelsPerCycle, hybridMap2Freq);

		CSF.showPixelCSFs(new WILDDisplay(), 0.33, true, true, pixelsPerCycle, hybridMap7Freq);
		CSF.showPixelCSFs(new WILDDisplay(), 0.33, true, true, pixelsPerCycle, hybridMap8Freq);

	}
	
}
