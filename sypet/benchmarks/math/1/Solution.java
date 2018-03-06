import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class Solution {

    public static RealMatrix invert(RealMatrix arg0) {
        SingularValueDecomposition v1 = new SingularValueDecomposition(arg0);
        DecompositionSolver v2 = v1.getSolver();
        RealMatrix v3 = v2.getInverse();
        return v3;
    }
}

