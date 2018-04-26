Artifact of the TACAS 2018 paper:
**Strategy Representation by Decision Trees in Reactive Synthesis**  
Tomas Brazdil, Krishnendu Chatterjee, Jan Kretinsky, and Viktor Toman

[![DOI](https://zenodo.org/badge/DOI/10.6084/m9.figshare.5923915.svg)](https://doi.org/10.6084/m9.figshare.5923915) Final version

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.1134156.svg)](https://doi.org/10.5281/zenodo.1134156) Evaluation-submitted version

This repository contains the evaluation-submitted version.

Structure:
* benchmarks -- AIGER files and parity automata used in the experiments
* bin -- compiled Java bytecodes
* lib -- JavaBDD library used to manipulate with BDDs
* results -- generated training sets, decision trees, and experiment reports
* src -- source code
* run.sh -- shell script to run the program on Linux
* run.bat -- PowerShell script to run the program on Windows

The program (i.e. script) expects one input string-argument:
* 'a'   -- creates AIGER games and solves them
* 'wX'  -- creates Wash games with X (2..4) tanks and solves them
* 'Ra'  -- represents computed AIGER strategies
* 'RwX' -- represents computed Wash strategies with X (2..4) tanks (X=0 for reachability)
* 'rabN' -- creates naive Rabinizer games, solves them and represents computed strategies
* 'rabE' -- creates encoded Rabinizer games, solves them and represents computed strategies
* 'aTOTAL' -- performs entire Bit Shifter experiments
* 'wTOTAL' -- performs entire Scheduling of Washing Cycles experiments
* 'rTOTAL' -- performs entire Random LTL experiments

In addition, we provide a simple C++ binary results/BDDreorder  
and a shell script results/runBDDreorder.sh that runs reordering  
experiments on all data sets located in results/datasets
