public static boolean test() throws Throwable {
    com.opengamma.analytics.math.matrix.DoubleMatrix1D mat1 = new com.opengamma.analytics.math.matrix.DoubleMatrix1D(1, 1, 1);
    com.opengamma.analytics.math.matrix.DoubleMatrix1D mat2 = new com.opengamma.analytics.math.matrix.DoubleMatrix1D(1, 2, 3);
    if (Source.getInnerProduct(mat1, mat2) == 6)
	return true;
    else
	return false;
}