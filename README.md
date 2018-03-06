                              ____     ___      __
                             / __/_ __/ _ \___ / /
                            _\ \/ // / ___/ -_) __/
                           /___/\_, /_/   \__/\__/
                               /___/    


SyPet is a novel type-directed tool for component-based synthesis. The key 
novelty of our approach is the use of a compact Petri-net representation to 
model relationships between methods in an API. Given a target method signature 
S, our approach performs reachability analysis on the underlying Petri-net model 
to identify sequences of method calls that could be used to synthesize an 
implementation of S. The programs synthesized by our algorithm are guaranteed to 
type check and pass all test cases provided by the user.

# Release 0.1 (November 2016)
This release corresponds to binary version of SyPet that was used in the paper: 
"Component-based synthesis for complex APIs" Yu Feng, Ruben Martins, Yuepeng 
Wang, Isil Dillig, Thomas W. Reps. POPL 2017.

For more details see the website of this release at: 
https://fredfeng.github.io/sypet/

# A. Reproducing the results from the POPL'17 paper

All experiments were conducted using Oracle HotSpot JVM 1.7.0 75 on an Intel 
Xeon(R) computer with an E5-2640 v3 CPU and 32G of memory running Ubuntu 14.04.

SyPet can run either on Linux or Mac OS X distributions (it does not support 
Windows). Note that different Java versions or computer architectures may lead 
to running times that are different from the ones reported in the paper. For 
your convenience, we supply the logs from the main experiment under the 
directory 'logs'.

Steps to reproduce the results from Table 1:

1. Run the script ./run-all.sh
2. SyPet will solve all benchmarks from the paper (this should take around one 
   hour)
3. After completion, the logs files from this experiment are available on the 
   directory 'output'
4. The contents of Table 1 will be automatically generated and appear in the 
   file 'result.csv'

# B. How to solve a new benchmark with SyPet?

To run SyPet the user must provide:

1. a .json file with the following parameters:
  +   "id"         -- an integer id for the benchmark
  +   "methodName" -- the method name for the synthesized function. The 
         method name is used by our similarity metrics to use APIs with 
	     similar names first. 
  +  "paramNames"  -- names for the arguments of the synthesized function.
  +   "srcTypes"   -- the full quantified Java type of each "paramNames"
  +    "tgtType"   -- the full quantified Java type of the goal
  +   "packages"   -- which packages should be used to search for the APIs 
	     that will transform "srcTypes" into "tgtType". The user can specify 
	     any granularity of the package. A more specific granularity will 
	     lead to a smaller search space and better performance for SyPet.
  +  "libs"        -- the location of the Java libraries that contain the 
	     "packages" described in vi)
  +  "testPath"    -- the location of the test function

2. a .java file that contains a set of test cases. 
  - the following method must exist in this Java file: 
  public static boolean test() throws Throwable { }
  - all types should be fully quantified

The directory 'example' contains a new benchmark that for computing the distance 
between two points:
  1.  The file 'example.json' contains the .json file with the parameters used 
      by SyPet to search for a solution. 
  2. The file 'TestSource.java' contains a set of test cases 

SyPet can be run on this new example by executing the command line:
```
./run-sypet.sh example/example.json
```

Please make sure that all test cases are correct before you run SyPet, otherwise 
it will lead to incorrect solution or timeout. e.g., some of test cases under 
"joda/" are sensitive to current date or year. If you run into a timeout, please 
double check the correctness of corresponding test cases.

# C. SyPet Options

By default all options in SyPet are enable.

Options:

-disableObjfunction : Disable objective function based on the similarity metrics

-disablePruning     : Disable pruning based on reachability analysis.

-disableRoundrobin  : Disable round robin exploration of different path lengths. 

Example:
```
./run-sypet.sh example/example.json "-disableObjfunction -disablePruning"
```

We refer the interested reader to the paper for more information regarding the 
underlying techniques used in these options. 


# D. Directory Structure
.

+-- README : this file

+-- example : directory with a new example

|	+-- example.json : .json file for a new example

|   +-- TestSource.java : .java test file for a new example

+-- logs : directory with the logs of the experiment done for the paper

+-- output : directory that will contain the output of the experiment of 'run-all.sh'

+-- run-all.sh : script to run all benchmarks

+-- run-sypet.sh : script to run SyPet

+-- sypet

|	+-- benchmarks : directory with the .json and Java test cases

| 	+-- build.xml

| 	+-- data : NLP data for the similarity metrics

|	+-- lib : Java libraries used to run SyPet and to run the benchmarks

|	+-- sypet.jar
