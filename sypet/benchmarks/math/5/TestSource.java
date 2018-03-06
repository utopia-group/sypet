public static boolean test() throws Throwable {
    double[][] mat = new double[][]{{1,2},{3,4}};
    com.opengamma.analytics.math.matrix.DoubleMatrix2D m = new com.opengamma.analytics.math.matrix.DoubleMatrix2D(mat);
    
    double[][] mat2 = new double[][]{{-2,1},{1.5,-0.5}};
	double[][] res = Source.invert(m).toArray();
	
	if(mat2.length != res.length || mat2[0].length != res[0].length)
		return false;
	
	for(int i=0; i<mat2.length; i++){
		for(int j=0; j<mat2[0].length; j++){
			if(Math.abs(mat2[i][j] - res[i][j]) > 0.00000005){
				return false;
			}
		}
	}
	
	return true;
}
