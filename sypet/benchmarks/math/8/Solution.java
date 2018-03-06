import org.apache.commons.math3.stat.regression.SimpleRegression;

public class Solution {

    public static double predict(double[][] arg0, double arg1) {
        SimpleRegression v1 = new SimpleRegression();
        v1.addData(arg0);
        double v2 = v1.predict(arg1);
        return v2;
    }

}
