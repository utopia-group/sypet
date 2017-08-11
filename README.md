```                            
                            ____     ___      __ 
                           / __/_ __/ _ \___ / /_
                          _\ \/ // / ___/ -_) __/
                         /___/\_, /_/   \__/\__/ 
                             /___/               
      
```

#Component-Based Synthesis for Complex APIs

SyPet is a novel type-directed tool for component-based synthesis. The key 
novelty of our approach is the use of a compact Petri-net representation to 
model relationships between methods in an API. Given a target method signature 
S, our approach performs reachability analysis on the underlying Petri-net model 
to identify sequences of method calls that could be used to synthesize an 
implementation of S. The programs synthesized by our algorithm are guaranteed 
to type check and pass all test cases provided by the user.


#Publication

Component-Based Synthesis for Complex APIs. 
Yu Feng, Ruben Martins, Yuepeng Wang, Isil Dillig, Thomas W. Reps. POPL 2017.

#Disclaimer

The open source version of SyPet is a simplified version of the one that was 
described in the POPL 2017 paper. In particular, we did not include the 
optimizations done by pruning or by the objective function (see Appendix D of 
the extended version of the paper for more details on the impact of those 
features). Note that, this version is more compact than the original SyPet 
version and should be easier to extend. 

The open version of SyPet should be extended in the next year with new features. 
In this version, the user can provide additional information to SyPet using 
the CONFIG.json file. In particular, the user can specify a set of "blacklist" 
keywords and all methods that contain those keywords will not be used when 
constructing the Petri-net. Additionally, the user can also manually specify 
which methods are polymorphic in the "poly" field. Finally, the user can specify 
packages that will always be included in the construction of the Petri-net in 
the "buildinPkg".

NOTE: If you want to compare against the original version of SyPet and reproduce 
the results from the POPL 2017 paper, we recommend you to use the binary version 
available at SyPet's webpage: https://fredfeng.github.io/sypet/

#Usage

```
$ ant
$ ./run-sypet.sh benchmarks/xml/23/benchmark23.json


#Config file (CONFIG.json)

*blacklist: SyPet will not include those methods in the PetriNet

*poly: Specify sub-typing relationship on-demand

*buildinPkg: Build-in packages included by SyPet

#Requirements
 - JDK 1.7+
 - ANT

