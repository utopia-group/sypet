public static boolean test0() throws Throwable {
    java.awt.geom.Point2D p1 = new java.awt.geom.Point2D.Double(1,1);
    java.awt.geom.Point2D p2 = new java.awt.geom.Point2D.Double(1,2);
    return (distance(p1,p2) == 1);
}

public static boolean test1() throws Throwable {
    java.awt.geom.Point2D p1 = new java.awt.geom.Point2D.Double(0,0);
    java.awt.geom.Point2D p2 = new java.awt.geom.Point2D.Double(0,2);
    return (distance(p1,p2) == 2);
}

public static boolean test() throws Throwable {
    return test0() && test1();
}
