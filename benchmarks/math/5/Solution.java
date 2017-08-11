import org.apache.commons.math.linear.RealMatrix;

import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.util.wrapper.CommonsMathWrapper;

public class Solution {

    public static DoubleMatrix2D invert(DoubleMatrix2D arg0) {
        RealMatrix v1 = CommonsMathWrapper.wrap(arg0);
        RealMatrix v2 = v1.inverse();
        DoubleMatrix2D v3 = CommonsMathWrapper.unwrap(v2);
        return v3;
    }

}

