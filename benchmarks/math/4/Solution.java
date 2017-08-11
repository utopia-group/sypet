import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.SingularValueDecomposition;
import org.apache.commons.math.linear.SingularValueDecompositionImpl;

import com.opengamma.analytics.math.linearalgebra.SVDecompositionCommonsResult;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.util.wrapper.CommonsMathWrapper;

public class Solution {

    public static SVDecompositionCommonsResult evaluate(DoubleMatrix2D arg0) {
        RealMatrix v1 = CommonsMathWrapper.wrap(arg0);
        SingularValueDecomposition v2 = new SingularValueDecompositionImpl(v1);
        SVDecompositionCommonsResult v3 = new SVDecompositionCommonsResult(v2);
        return v3;
    }

}

