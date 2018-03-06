import org.apache.commons.math.linear.RealVector;

import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.util.wrapper.CommonsMathWrapper;

public class Solution {

    public static double getInnerProduct(DoubleMatrix1D arg0, DoubleMatrix1D arg1) {
        double[] v3 = arg1.toArray();
        RealVector v4 = CommonsMathWrapper.wrap(arg0);
        double var5 = v4.dotProduct(v3);
        return var5;
    }

}
