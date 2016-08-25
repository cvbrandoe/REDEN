package fr.ign.georeden.algorithms.string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import fr.ign.georeden.algorithms.utils.AlgoUtils;

public class StringComparisonDamLev implements IStringComparison {
	DamerauLevenshteinAlgorithm damLev;
	public StringComparisonDamLev() {
		damLev = new DamerauLevenshteinAlgorithm(1, 1, 1, 1);
	}
	
	
	
	/**
	 * s1 and s2 must be tokenized. Return the value of lambdaIJ
	 * @param s1
	 * @param s2
	 * @return
	 */
	public double lambdaIJ(String s1, String s2) {
		return 1 - ((double)damLev.execute(s1, s2) / Math.max(s1.length(), s2.length()));
	}
	
	public double[][] computeMatrix(String[] tokenArray1, String[] tokenArray2) {
		double[][] matrix = new double[tokenArray1.length][tokenArray2.length];
		for (int i = 0; i < tokenArray1.length; i++) {
			String token1 = tokenArray1[i];
			for (int j = 0; j < tokenArray2.length; j++) {
				String token2 = tokenArray2[j];
				double score = Math.round(lambdaIJ(token1, token2) * 10000f) / 10000f;
				matrix[i][j] = score;
			}
		}
		return matrix;
	}

	@Override	
	public double computeSimilarity(String s1, String s2) {
		if (s1 == null || s2 == null)
			return 0.0;
		String[] tokenArray1 = AlgoUtils.tokenizeString(s1);
		String[] tokenArray2 = AlgoUtils.tokenizeString(s2);
		double[][] matrix = computeMatrix(tokenArray1, tokenArray2);
		double mu = (tokenArray1.length +tokenArray2.length) / 2f;
		HashMap<Integer, Integer> optimums = new HashMap<>();
		for (int j = 0; j < matrix[0].length; j++) {
			double columnMax = 0;
			int iValue = -1;
			for (int i = 0; i < matrix.length; i++) {
				if (!optimums.containsValue(i) && matrix[i][j] > columnMax) {
					columnMax = matrix[i][j];
					iValue = i;
				}
			}
			if (iValue > -1)
				optimums.put(j, iValue);
		}
		
		double sum = 0f;
		for (Iterator<Integer> iterator = optimums.keySet().iterator(); iterator.hasNext();) {
			Integer j = iterator.next();
			Integer i = optimums.get(j);
			sum += matrix[i][j];
		}

		//displayMatrix(matrix);
		return sum / mu;
	}
	
	public void displayMatrix(double[][] matrix) {
		String line = "";
		for (int j = 0; j < matrix[0].length; j++) {
			for (int i = 0; i < matrix.length; i++) {
				line += matrix[i][j] + "\t";
			}
			System.out.println(line);
			line = "";
		}
	}
}
