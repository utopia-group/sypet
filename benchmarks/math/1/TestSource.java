public static boolean test() throws Throwable {
    double [][] mat1 = new double[][]{{1,2,3},{4,5,6}};
    double [][] mat2 = new double[][]{{-0.944444,0.444444},{-0.111111,0.111111},{0.722222,-0.222222}};
    org.apache.commons.math3.linear.RealMatrix mat = new org.apache.commons.math3.linear.Array2DRowRealMatrix(mat1);
    org.apache.commons.math3.linear.RealMatrix target = new org.apache.commons.math3.linear.Array2DRowRealMatrix(mat2);
    org.apache.commons.math3.linear.RealMatrix result = invert(mat);
    for (int i = 0; i < 3; ++i) {
        for (int j = 0; j < 2; ++j) {
            if (Math.abs(target.getEntry(i,j) - result.getEntry(i,j)) > 1e-6) return false;
        }
    }
    return true;
}
