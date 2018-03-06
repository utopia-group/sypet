import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.util.wrapper.CommonsMathWrapper;

public class Solution {

    public static DoubleMatrix2D getOuterProduct(DoubleMatrix1D arg0, DoubleMatrix1D arg1) {
        RealVector v1 = CommonsMathWrapper.wrap(arg1);
        RealVector v2 = CommonsMathWrapper.wrap(arg0);
        RealMatrix v3 = v2.outerProduct(v1);
        DoubleMatrix2D v4 = CommonsMathWrapper.unwrap(v3);
        return v4;
    }

}
