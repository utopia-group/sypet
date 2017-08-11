public static boolean test1() throws Throwable {
    double[][] known = new double[][] {{1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}};
    double x = 1.5;
    return predict(known, x) == 2.5;
}

public static boolean test2() throws Throwable {
    double[][] known = new double[][] {{1, 3}, {2, 4}, {3, 5}, {4, 6}, {5, 7}};
    double x = 2.5;
    return predict(known, x) == 4.5;
}

public static boolean test() throws Throwable {
    return test1() && test2();
}
