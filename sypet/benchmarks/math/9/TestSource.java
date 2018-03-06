public static boolean test0() throws Throwable {
	double[][] mat = new double[][] { { 0, -20 }, { 10, 10 } };
	org.apache.commons.math3.linear.RealMatrix matrix = new org.apache.commons.math3.linear.Array2DRowRealMatrix(mat);
	org.apache.commons.math3.geometry.euclidean.twod.Vector2D result = eigenvalue(matrix, 0);
	org.apache.commons.math3.geometry.euclidean.twod.Vector2D target = new org.apache.commons.math3.geometry.euclidean.twod.Vector2D(5, 5*Math.sqrt(7));
	return Math.abs(result.getX() - target.getX()) < 1e-6 && Math.abs(result.getY() - target.getY()) < 1e-6;
}

public static boolean test1() throws Throwable {
	double[][] mat = new double[][] { { 0, 2 }, { 2, 0 } };
	org.apache.commons.math3.linear.RealMatrix matrix = new org.apache.commons.math3.linear.Array2DRowRealMatrix(mat);
	org.apache.commons.math3.geometry.euclidean.twod.Vector2D result = eigenvalue(matrix, 1);
	org.apache.commons.math3.geometry.euclidean.twod.Vector2D target = new org.apache.commons.math3.geometry.euclidean.twod.Vector2D(-2, 0);
	return Math.abs(result.getX() - target.getX()) < 1e-6 && Math.abs(result.getY() - target.getY()) < 1e-6;
}

public static boolean test() throws Throwable {
	return test0() && test1();
}