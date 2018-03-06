public static boolean test() throws Throwable {
    double init = 0.0;
    final double[] coeff = { 1, -2, 1 }; 

    org.apache.commons.math3.analysis.polynomials.PolynomialFunction pf = new org.apache.commons.math3.analysis.polynomials.PolynomialFunction(coeff);
    org.apache.commons.math3.complex.Complex[] comp = Source.findRoots(pf, init);
    boolean flag = ((comp.length == 2) && (comp[0].getReal() == 1.0) && (comp[0].getImaginary() == 0.0));
    
    if(flag) return true;
    else return false;
}
