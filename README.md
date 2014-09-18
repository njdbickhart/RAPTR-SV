RAPTR-SV
====

RAPTR-SV stands for "**R** e **A** d **P** air spli **T** - **R** ead **S** tructural **V** ariant." It is a program designed to process previously aligned, Illumina Paired-end whole genome sequence data to identify structural variants such as deletions, insertions and tandem duplications.

### Preliminary considerations
A key component of the algorithm is the tightness of the distribution of paired end insert sizes in your sequencing library. If the standard deviations are too high, then the resolution of RAPTR-SV will decrease appreciably. If you intend to generate new data for use in RAPTR-SV, we recommend using a method for fragment size selection in order to reduce the distribution width. 

### Usage
RAPTR-SV currently has two modes:
  * preprocess
  * cluster

##### common to all modes
RAPTR-SV makes use of temporary file directories to store information. At times, this temporary storage might get overloaded from the sheer amount of data needed by the program! If you get an error from the program that suggests that there is something wrong with your temporary directory (ie. _temp_ _directory_ _ran_ _out_ _of_ _space_ ), you can select a new temporary directory with the -p flag. Also, RAPTR-SV has threading options that can scale with the number of processor cores on your system. You can specifiy the number of threads for the program to use with the -t option.

> -p  Alternate temporary file directory (default is /tmp, depending on your system)

> -t  Number of threads to use for processing (default is 1)

##### preprocess
This is the mode that generates metadata from an input bam file. You will need two required input files to run this stage of the program:
  1. A BWA-aligned BAM
  2. A MrsFAST-indexed, Repeat-Masked Reference genome fasta

##### cluster
This is the mode that uses metadata generated in the preprocess stage of the pipeline to call variants. There are several runtime filter settings that can be set to reduce the number of false positive calls
