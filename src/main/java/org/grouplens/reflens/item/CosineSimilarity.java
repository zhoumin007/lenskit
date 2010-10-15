/* RefLens, a reference implementation of recommender algorithms.
 * Copyright 2010 Michael Ekstrand <ekstrand@cs.umn.edu>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.grouplens.reflens.item;

import java.util.Map;

import org.grouplens.reflens.Similarity;
import org.grouplens.reflens.SymmetricBinaryFunction;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class CosineSimilarity<I>
	implements Similarity<Map<I,Float>>, SymmetricBinaryFunction {

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.Similarity#similarity(java.lang.Object, java.lang.Object)
	 */
	@Override
	public float similarity(Map<I,Float> vec1, Map<I,Float> vec2) {
		float dot = 0.0f;
		float ssq1 = 0.0f;
		float ssq2 = 0.0f;
		for (Map.Entry<I,Float> e: vec1.entrySet()) {
			I i = e.getKey();
			float v = e.getValue();
			if (vec2.containsKey(i)) {
				dot += v * vec2.get(i);
			}
			ssq1 += v * v;
		}
		for (Float v: vec2.values()) {
			ssq2 += v * v;
		}
		double denom = Math.sqrt(ssq1) * Math.sqrt(ssq2);
		if (denom == 0.0f) {
			return Float.NaN;
		} else { 
			return dot / (float) denom;
		}
	}

}
