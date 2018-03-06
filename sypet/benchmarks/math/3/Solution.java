import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;

public class Solution {

    public static Complex[] findRoots(PolynomialFunction arg0, double arg1) {
        LaguerreSolver v1 = new LaguerreSolver();
        double[] v2 = arg0.getCoefficients();
        Complex[] v3 = v1.solveAllComplex(v2, arg1);
        return v3;
    }

}

