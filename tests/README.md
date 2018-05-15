# How to run SyPet using as an HTTP server?

Step 1. Launch SyPet in server mode with:

Compile SyPet:
```
ant
```

Launch server:
```
ant server
```

Step 2. Send a post request with the configuration .json file as the body. 

Example:
curl -X POST -d @test1.json http://127.0.0.1:9092 --header "Content-Type:application/json"

Output:
```
java.awt.geom.Rectangle2D scale(java.awt.geom.Rectangle2D sypet_arg0, double sypet_arg1, double sypet_arg2) {

java.awt.Shape sypet_var273 = sypet_arg0;
java.awt.geom.AffineTransform sypet_var274 = java.awt.geom.AffineTransform.getScaleInstance(sypet_arg1,sypet_arg2);
java.awt.geom.Path2D.Float sypet_var275 = new java.awt.geom.Path2D.Float(sypet_var273,sypet_var274);
java.awt.geom.Rectangle2D sypet_var276 = sypet_var275.getBounds2D();
return sypet_var276;

}
```

Where test1.json:

```
{
  "methodName": "scale",
  "paramNames": [
    "sypet_arg0",
    "sypet_arg1",
    "sypet_arg2"
  ],
  "srcTypes": [
    "java.awt.geom.Rectangle2D",
    "double",
    "double"
  ],
  "tgtType": "java.awt.geom.Rectangle2D",
  "packages": [
    "java.awt.geom"
  ],
  "testBody": "public static boolean test() throws Throwable { java.awt.geom.Rectangle2D rec = new java.awt.geom.Rectangle2D.Double(10, 20, 10, 2); java.awt.geom.Rectangle2D target = new java.awt.geom.Rectangle2D.Double(20, 60, 20, 6); java.awt.geom.Rectangle2D result = scale(rec, 2, 3); return (target.equals(result));}"
}
```

# Manual Testing

Examples for manual testing in the localhost:
- curl -X POST -d @test1.json http://127.0.0.1:9092 --header "Content-Type:application/json"
- curl -X POST -d @test2.json http://127.0.0.1:9092 --header "Content-Type:application/json"
- curl -X POST -d @test3.json http://127.0.0.1:9092 --header "Content-Type:application/json"
- curl -X POST -d @test4.json http://127.0.0.1:9092 --header "Content-Type:application/json"
- curl -X POST -d @test5.json http://127.0.0.1:9092 --header "Content-Type:application/json"
- curl -X POST -d @test6.json http://127.0.0.1:9092 --header "Content-Type:application/json"


# Automatic Testing

Testing can be done by running the script "run-tests.sh". This should be done from outside the container to test the network communication. If a VPN is used then you should change the "ip" field to 100.120.0.7 instead of 192.168.0.7.

After running the script you should get the following output:

```
              ____     ___      __
             / __/_ __/ _ \___ / /_
            _\ \/ // / ___/ -_) __/
           /___/\_, /_/   \__/\__/
               /___/
[SyPet] Running  benchmarks...
[SyPet] Benchmark test1.json            [OK]
java.awt.geom.Rectangle2D scale(java.awt.geom.Rectangle2D sypet_arg0, double sypet_arg1, double sypet_arg2) {

java.awt.Shape sypet_var3191 = sypet_arg0;
java.awt.geom.AffineTransform sypet_var3192 = java.awt.geom.AffineTransform.getScaleInstance(sypet_arg1,sypet_arg2);
java.awt.geom.Path2D.Float sypet_var3193 = new java.awt.geom.Path2D.Float(sypet_var3191,sypet_var3192);
java.awt.geom.Rectangle2D sypet_var3194 = sypet_var3193.getBounds2D();
return sypet_var3194;

}
[SyPet] Benchmark test2.json            [OK]
java.awt.geom.Rectangle2D shear(java.awt.geom.Rectangle2D sypet_arg0, double sypet_arg1, double sypet_arg2) {

java.awt.Shape sypet_var3481 = sypet_arg0;
java.awt.geom.AffineTransform sypet_var3482 = java.awt.geom.AffineTransform.getShearInstance(sypet_arg1,sypet_arg2);
java.awt.geom.Path2D.Double sypet_var3483 = new java.awt.geom.Path2D.Double(sypet_var3481,sypet_var3482);
java.awt.geom.Rectangle2D sypet_var3484 = sypet_var3483.getBounds2D();
return sypet_var3484;

}
[SyPet] Benchmark test3.json            [OK]
java.awt.geom.Rectangle2D rotateQuadrant(java.awt.geom.Rectangle2D sypet_arg0, int sypet_arg1) {

java.awt.Shape sypet_var3527 = sypet_arg0;
java.awt.geom.AffineTransform sypet_var3528 = java.awt.geom.AffineTransform.getQuadrantRotateInstance(sypet_arg1);
java.awt.geom.Path2D.Double sypet_var3529 = new java.awt.geom.Path2D.Double(sypet_var3527,sypet_var3528);
java.awt.geom.Rectangle2D sypet_var3530 = sypet_var3529.getBounds2D();
return sypet_var3530;

}
[SyPet] Benchmark test4.json            [OK]
java.awt.geom.Area rotate(java.awt.geom.Area sypet_arg0, java.awt.geom.Point2D sypet_arg1, double sypet_arg2) {

double sypet_var3962 = sypet_arg1.getY();
double sypet_var3963 = sypet_arg1.getX();
java.awt.geom.AffineTransform sypet_var3964 = java.awt.geom.AffineTransform.getRotateInstance(sypet_arg2,sypet_var3963,sypet_var3962);
java.awt.geom.Area sypet_var3965 = sypet_arg0.createTransformedArea(sypet_var3964);
return sypet_var3965;

}
[SyPet] Benchmark test5.json            [OK]
java.awt.geom.Rectangle2D translate(java.awt.geom.Rectangle2D sypet_arg0, double sypet_arg1, double sypet_arg2) {

java.awt.Shape sypet_var4224 = sypet_arg0;
java.awt.geom.AffineTransform sypet_var4225 = java.awt.geom.AffineTransform.getTranslateInstance(sypet_arg1,sypet_arg2);
java.awt.geom.Path2D.Double sypet_var4226 = new java.awt.geom.Path2D.Double(sypet_var4224,sypet_var4225);
java.awt.geom.Rectangle2D sypet_var4227 = sypet_var4226.getBounds2D();
return sypet_var4227;

}
[SyPet] Benchmark test6.json            [OK]
java.awt.geom.Rectangle2D getIntersection(java.awt.geom.Rectangle2D sypet_arg0, java.awt.geom.Ellipse2D sypet_arg1) {

java.awt.Shape sypet_var4236 = sypet_arg1;
java.awt.geom.Path2D.Float sypet_var4237 = new java.awt.geom.Path2D.Float(sypet_var4236);
java.awt.geom.Rectangle2D sypet_var4238 = sypet_var4237.getBounds2D();
java.awt.geom.Rectangle2D sypet_var4239 = sypet_var4238.createIntersection(sypet_arg0);
return sypet_var4239;

}
```
