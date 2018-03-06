public static boolean test() throws Throwable {
	double[][] xMat = new double[][]{{1,2},{2,2},{2,1}};
	com.opengamma.analytics.math.matrix.DoubleMatrix2D x = new com.opengamma.analytics.math.matrix.DoubleMatrix2D(xMat);
	com.opengamma.analytics.math.linearalgebra.SVDecompositionResult res2 = Source.evaluate(x);
	double inv_sqrt2 = 1/Math.sqrt(2);
	double[][] UMat = new double[][]{{3/Math.sqrt(34),-1/Math.sqrt(2)},{4/Math.sqrt(34),0},{3/Math.sqrt(34),1/Math.sqrt(2)}};
	double[][] SMat = new double[][]{{Math.sqrt(17),0},{0,1}};
	double[][] VTMat = new double[][]{{inv_sqrt2,inv_sqrt2},{inv_sqrt2,-inv_sqrt2}};
	
	double[][] UMatRes = res2.getU().toArray();
	double[][] SMatRes = res2.getS().toArray();
	double[][] VTMatRes = res2.getVT().toArray();
	
	if(UMat.length != UMatRes.length ||
	SMat.length != SMatRes.length ||
	VTMat.length != VTMatRes.length){
		return false;
	}
	
	for(int i=0; i<UMat.length; i++){
		for(int j=0;j<UMat[i].length; j++){
			if(Math.abs(UMat[i][j]-UMatRes[i][j])>0.000005)
				return false;
		}
	}
	
	for(int i=0; i<SMat.length; i++){
		for(int j=0;j<SMat[i].length; j++){
			if(Math.abs(SMat[i][j]-SMatRes[i][j])>0.000005)
				return false;
		}
	}
	
	for(int i=0; i<VTMat.length; i++){
		for(int j=0;j<VTMat[i].length; j++){
			if(Math.abs(VTMat[i][j]-VTMatRes[i][j])>0.000005)
				return false;
		}
	}
	
	return true;
}
