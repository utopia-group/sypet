public static boolean test() throws Throwable {
    double[][] matrix = {{3,-1},{1,1}};
    double[] vec = {3,5};
    double[] result = Source.solveLinear(matrix, vec);
    
    double[][] matrix2 = {{1,2,1},{2,-1,3},{3,1,2}};
    double[] vec2 = {7,7,18};
    double[] result2 = Source.solveLinear(matrix2, vec2);
    boolean flag1 = ((result.length == 2) && (result[0] == 2.0) && (result[1] == 3.0));
    boolean flag2 = ((result2.length == 3) && (result2[0] == 7.0) && (result2[1] == 1.0) && (result2[2] == -2.0));

    if(flag1 && flag2)
	return true;
    else 
	return false;
}
