import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public class Solution {

    public static Vector2D eigenvalue(RealMatrix arg0, int arg1) {
        EigenDecomposition v1 = new EigenDecomposition(arg0);
        double v2 = v1.getImagEigenvalue(arg1);
        double v3 = v1.getRealEigenvalue(arg1);
        Vector2D v4 = new Vector2D(v3, v2);
        return v4;
    }

}

