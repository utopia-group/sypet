public static boolean test() throws Throwable {
    com.opengamma.analytics.math.matrix.DoubleMatrix1D mat1 = new com.opengamma.analytics.math.matrix.DoubleMatrix1D(1, 1, 1);
    com.opengamma.analytics.math.matrix.DoubleMatrix1D mat2 = new com.opengamma.analytics.math.matrix.DoubleMatrix1D(1, 2, 3);
    double[][] resMat = new double[][] { { 1, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 } };
    com.opengamma.analytics.math.matrix.DoubleMatrix2D res = new com.opengamma.analytics.math.matrix.DoubleMatrix2D(resMat);
    if (res.toString().equals(Source.getOuterProduct(mat1, mat2).toString()))
	return true;
    else
	return false;
}
