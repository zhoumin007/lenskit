package org.grouplens.reflens.knn;

import static java.lang.Math.sqrt;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.util.Iterator;

import org.grouplens.reflens.data.vector.SparseVector;

/**
 * Similarity function using Pearson correlation.
 * 
 * <p>This class implements the Pearson correlation similarity function over
 * sparse vectors.  Only the items occurring in both vectors are considered when
 * computing the variance.
 * 
 * <p>See Desrosiers, C. and Karypis, G., <i>A Comprehensive Survey of
 * Neighborhood-based Recommendation Methods</i>.  In Ricci, F., Rokach, L.,
 * Shapira, B., and Kantor, P. (eds.), <i>Recommender Systems Handbook</i>,
 * Springer. 2010, pp. 107-144.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class PearsonCorrelation implements OptimizableVectorSimilarity<SparseVector> {

	@Override
	public double similarity(SparseVector vec1, SparseVector vec2) {
		// First check for empty vectors - then we can assume at least one element
		if (vec1.isEmpty() || vec2.isEmpty())
			return 0;
		
		/*
		 * Basic strategy: walk in parallel across the two vectors, computing
		 * the dot product and simultaneously computing the variance within each
		 * vector of the items also contained in the other vector.  Pearson
		 * correlation only considers items shared by both vectors; other items
		 * aren't entirely discarded for the purpose of similarity computation.
		 */
		final double mu1 = vec1.mean();
		final double mu2 = vec2.mean();
		
		double var1 = 0;
		double var2 = 0;
		double dot = 0;
		int nCoratings = 0; // number of common items rated
		Iterator<Long2DoubleMap.Entry> it1 = vec1.fastIterator();
		Iterator<Long2DoubleMap.Entry> it2 = vec2.fastIterator();
		Long2DoubleMap.Entry e1 = it1.next();
		Long2DoubleMap.Entry e2 = it2.next();
		do {
			/* Do one step of the parallel walk.  If the two entries have the
			 * same key, add to the accumulators and advance both.  Otherwise,
			 * advance the one further back to try to catch up.
			 */
			if (e1.getLongKey() == e2.getLongKey()) {
				final double v1 = e1.getDoubleValue() - mu1;
				final double v2 = e2.getDoubleValue() - mu2;
				var1 += v1 * v1; 
				var2 += v2 * v2;
				dot += v1 * v2;
				nCoratings += 1;
				e1 = it1.next();
				e2 = it2.next();
			} else if (e1.getLongKey() < e2.getLongKey()) {
				e1 = it1.next();
			} else {
				e2 = it2.next();
			}
		} while (it1.hasNext() && it2.hasNext());
		
		return computeFinalCorrelation(nCoratings, dot, var1, var2);
	}
	
	protected double computeFinalCorrelation(int nCoratings, double dot, double var1, double var2) {
		if (nCoratings == 0)
			return 0;
		else
			return dot / sqrt(var1 * var2);
	}
}