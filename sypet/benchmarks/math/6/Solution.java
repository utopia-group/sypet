import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

public class Solution {

    public static double[] solveLinear(double[][] arg0, double[] arg1) {
        RealMatrix v1 = MatrixUtils.createRealMatrix(arg0);
        RealMatrix v2 = v1.transpose();
        LUDecomposition v3 = new LUDecomposition(v2);
        DecompositionSolver v4 = v3.getSolver();
        RealMatrix v5 = v4.getInverse();
        double[] v6 = v5.preMultiply(arg1);
        return v6;
    }

}
