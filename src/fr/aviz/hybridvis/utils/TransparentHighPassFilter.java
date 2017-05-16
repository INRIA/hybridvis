/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package fr.aviz.hybridvis.utils;

import java.awt.image.BufferedImage;

import com.jhlabs.image.HighPassFilter;

/**
 * Applies hi-pass then makes gray transparent.
 * 
 * @author dragice, Jerry Huxtable
 */
public class TransparentHighPassFilter extends HighPassFilter {
	
	public TransparentHighPassFilter() {
		super();
	}
	
    @Override
	public BufferedImage filter( BufferedImage src, BufferedImage dst ) {
        int width = src.getWidth();
        int height = src.getHeight();

        if ( dst == null )
            dst = createCompatibleDestImage( src, null );

        int[] inPixels = new int[width*height];
        int[] outPixels = new int[width*height];
        src.getRGB( 0, 0, width, height, inPixels, 0, width );

		if ( radius > 0 ) {
			convolveAndTranspose(kernel, inPixels, outPixels, width, height, alpha, alpha && premultiplyAlpha, false, CLAMP_EDGES);
			convolveAndTranspose(kernel, outPixels, inPixels, height, width, alpha, false, alpha && premultiplyAlpha, CLAMP_EDGES);
		}

        src.getRGB( 0, 0, width, height, outPixels, 0, width );

		int index = 0;
		for ( int y = 0; y < height; y++ ) {
			for ( int x = 0; x < width; x++ ) {
				int rgb1 = outPixels[index];
				int a1 = (rgb1 >> 24) & 0xff;
				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >> 8) & 0xff;
				int b1 = rgb1 & 0xff;

				int rgb2 = inPixels[index];
//				int a2 = (rgb2 >> 24) & 0xff;
				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >> 8) & 0xff;
				int b2 = rgb2 & 0xff;

				r1 = (r1 + 255-r2) / 2;
				g1 = (g1 + 255-g2) / 2;
				b1 = (b1 + 255-b2) / 2;
				a1 = Math.min(255, 4 * Math.max(Math.max(Math.abs(127 - r1), Math.abs(127 - g1)), Math.abs(127 - b1)));

				inPixels[index] = (a1 << 24) | (r1 << 16) | (g1 << 8) | b1;
				index++;
			}
		}

        dst.setRGB( 0, 0, width, height, inPixels, 0, width );
        return dst;
    }
}
