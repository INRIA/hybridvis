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

import java.util.Hashtable;

/**
 * A simple profiling class.
 * @author dragice
 *
 */
public class ExecTime {

	static class Operation {
		double time0 = 0;
		double pausetime0 = 0;
		double pausetime = 0;
	}
	
	static boolean enabled = false;
	static Hashtable<String, Operation> operations = new Hashtable<String, Operation>();
	
	public static void enable(boolean enable) {
		enabled = enable;
	}
	
	public static void start(String name) {
		if (!enabled)
			return;
		if (operations.containsKey(name)) {
			Operation op = operations.get(name);
			if (op.pausetime0 > 0) {
				op.pausetime += timems() - op.pausetime0;
				op.pausetime0 = 0;
			}
		} else {
			Operation op = new Operation();
			operations.put(name, op);
			op.time0 = timems();
		}
	}
	
	public static void end(String name) {
		double time = timems();
		if (!enabled)
			return;
		if (operations.containsKey(name)) {
			Operation op = operations.get(name);
			if (op.pausetime0 > 0) {
				op.pausetime += time - op.pausetime0;
				op.pausetime0 = 0;
			}
			System.err.println(name + " executed in " + toString(time - op.time0 - op.pausetime));
			operations.remove(name);
		} else {
			System.err.println(name + ": missing call to start()");
		}
	}
	
	public static void endAll() {
		double time = timems();
		if (!enabled)
			return;
		if (operations.size() > 0)
			System.err.println();
		for (String name : operations.keySet()) {
			Operation op = operations.get(name);
			if (op.pausetime0 > 0) {
				op.pausetime += time - op.pausetime0;
				op.pausetime0 = 0;
			}
			System.err.println(name + " executed in " + toString(time - op.time0 - op.pausetime));
		}
		operations.clear();
	}

	public static void pause(String name) {
		double time = timems();
		if (!enabled)
			return;
		if (operations.containsKey(name)) {
			Operation op = operations.get(name);
			if (op.pausetime0 > 0) 
				return; // already paused
			op.pausetime0 = time;
		}
	}

	public static double timems() {
		return System.nanoTime()/1000000.0;
	}
	
	public static String toString(double ms) {
		String s = "";
		if (ms < 1000)
			s += round(ms, 1) + " ms";
		else
			s += round(ms/1000, 1) + " s";
		s += " (" + round(1000 / ms, 1) + " Hz)";
		return s;
	}
	
	private static double round(double n, int digits) {
		int exp = (int)Math.pow(10, digits);
		return (Math.round(n * exp) / (double)exp);
	}
}
