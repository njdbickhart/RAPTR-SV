RAPTR-SV
====

RAPTR-SV stands for "**R** e **A** d **P** air spli **T** - **R** ead **S** tructural **V** ariant." It is a program designed to process previously aligned, Illumina Paired-end whole genome sequence data to identify structural variants such as deletions, insertions and tandem duplications.

### Getting Started
Much of the details for getting started with the program are currently on the [wiki](https://github.com/njdbickhart/RAPTR-SV/wiki)! Please refer to that for detailed usage and an example workflow.


First, please download the latest JDK for Java version 1.8 (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

In order to generate preliminary data using the "preprocess" mode, you will also need to download and install a version of MrsFAST (http://mrsfast.sourceforge.net/Home ). The MrsFAST executable must be installed into your $PATH in order to run the program.

Then, click on the "release" link at the top of this repository page, or navigate to: https://github.com/njdbickhart/RAPTR-SV/releases

You can download the latest RAPTR-SV jar files from that link. In order to run RAPTR-SV, you will have to invoke it with the java 1.8 executable. Here is an example of how to run the program:

> /path/to/jdk1.8.0/bin/java -jar /path/to/RAPTR-SV.jar

Running the program without any arguments will list the program help menu.

### Troubleshooting
RAPTR-SV has been tested on numerous BAM files, but it is not entirely "bug free." If you run into problems, please check your data first for any inconsistencies. If you cannot identify any obvious issues with your data, please run the same RAPTR-SV command but with the "-d" command line flag appended. This is the "debug mode" for the program that will create a very detailed log file. The log file will be generated in the folder that you run RAPTR-SV every time that you run the program. Please send the log file to the following email address ( derek "dot" bickhart "at" ars "dot" usda "dot" gov ) with a detailed description of the problem, and I will try to troubleshoot the issue with you.

### Preliminary considerations
A key component of the algorithm is the tightness of the distribution of paired end insert sizes in your sequencing library. If the standard deviations are too high, then the resolution of RAPTR-SV will decrease appreciably. If you intend to generate new data for use in RAPTR-SV, we recommend using a method for fragment size selection in order to reduce the distribution width. 

### Usage
RAPTR-SV currently has two modes:
  * preprocess
  * cluster

##### common to all modes
RAPTR-SV makes use of temporary file directories to store information. At times, this temporary storage might get overloaded from the sheer amount of data needed by the program! If you get an error from the program that suggests that there is something wrong with your temporary directory (ie. _temp_ _directory_ _ran_ _out_ _of_ _space_ ), you can select a new temporary directory with the -p flag. Also, RAPTR-SV has threading options that can scale with the number of processor cores on your system. You can specify the number of threads for the program to use with the -t option. All of the following arguments are OPTIONAL:

> -p  Alternate temporary file directory (default is /tmp, depending on your system). HIGHLY recommended for all program modes if your system has a partitioned "tmp" directory!

> -t  Number of threads to use for processing (default is 1)

> -d  Debug mode (for use in troubleshooting)

##### preprocess
This is the mode that generates metadata from an input bam file. You will need two required input files to run this stage of the program:
  1. A BWA-aligned BAM
  2. A MrsFAST-indexed, Repeat-Masked Reference genome fasta

In order to view the command line options for this mode of the program, just enter the following command on the command line:

> /path/to/jdk1.8.0/bin/java -jar /path/to/RAPTR-SV.jar preprocess

Here is a brief listing of the command line options for the "preprocess" mode:

> -i (file name) (REQUIRED) An input BAM file. The BAM must be sorted by basepair coordinates. If the BAM is not indexed, RAPTR-SV will index the BAM before processing it

> -o (file path) (REQUIRED) The path and base name for all of the "preprocess" output files. This is the prefix text that will be appended to all of the output files. This can include the output directory (NOTE: this directory must exist ahead of time!)

> -r (file name) (REQUIRED) A MrsFAST indexed reference genome file. Please make sure that the chromosome names of this reference file match the chromosome names in your BAM header! Also, repeatmasking of this reference may increase speed of processing.


> -g (nothing) (OPTIONAL) Ignore the read groups in the BAM. This may result in faster performance if your bam has multiple read groups that are all derived from the same DNA library.

> -m (integer) (OPTIONAL) Maximum distance threshold for discordant read pairs [default: 1,000,000 bp]. Any read pair that has an insert length above this threshold will be ignored by the program. This helps reduce false positives, but may cause you to ignore true, large events.

> -s (integer) (OPTIONAL) Metadata sampling limit [default: 10,000 read pairs]. This is the number of read pairs sampled by the "preprocess" mode to determine the average insert size and insert standard deviation. This value is also used to calculate how many read pairs are stored in memory while determining read pair discordancy, so reducing this value will reduce memory overhead.

RAPTR-SV will generate four files for each read group of the bam that you "preprocess" (unless specified by using the '-g' option, which ignores all readgroups). Here are the files and a brief description of each:
  1. A discordant read pair map file (*.divet)
  2. A BAM file with split-read anchoring reads (*.anchor.bam)
  3. A BAM file with split-read mappings (*.bam)
  4. A tab-delimited text file giving the full paths of the above three files, and the average insert length and insert standard deviation for each read group of the bam (*.flat)

Please note that the "flat file" (*.flat) is the input for the RAPTR-SV "cluster" mode which will make insertion, deletion and tandem duplication calls on your data. If you have multiple bam files for the same sample, you can "preprocess" each bam individually and then concatenate the "flat files" together before running the entire dataset through RAPTR-SV "cluster" mode.

If you run RAPTR-SV with the "-d" flag, it will generate a "Samsupport.tab" file that contains all of the discordant reads in the BAM file. This is simply for debugging purposes, and this file can be safely deleted after running the program.
##### cluster
This is the mode that uses metadata generated in the preprocess stage of the pipeline to call variants. There are several runtime filter settings that can be set to reduce the number of false positive calls, though in most cases, you can run the program with the default settings.

In order to view the command line options for this mode of the program, just enter the following command on the command line:

> /path/to/jdk1.8.0/bin/java -jar /path/to/RAPTR-SV.jar cluster

Here is a brief listing of the command line options for the "cluster" mode:

> -s (file name) (REQUIRED) A flat file generated from the RAPTR-SV "preprocess" mode. You can concatenate the contents of multiple flat files together for each run of RATPR-SV "cluster", but it is assumed that the flat files must point to the same individual sample.

> -o (file path) (REQUIRED) The path and base name for all of the "cluster" output files. This is the prefix text that will be appended to all of the output files. This can include the output directory (NOTE: this directory must exist ahead of time!)


> -c (string)    (OPTIONAL) Process only this chromosome. This option allows you to focus on only one chromosome at a time in the program which may be useful for parallelizing RAPTR-SV clustering. Default behavior is to process every chromosome present in the split read BAM file generated by the "preprocess" step.

> -g (file name) (OPTIONAL) Assembly gap file. This file contains a list of assembly gaps in "BED" file format (please see: http://genome.ucsc.edu/FAQ/FAQformat.html#format1 ) that will be ignored by the "cluster" algorithm. This is not required, but is highly recommended to reduce false positive calls.

> -m (float)     (OPTIONAL) Set the mapping quality filter for discordant read pairs [default: 0.0001]. This will eliminate ambiguous mappings from the "divet" file prior to clustering. Very useful for highly repetitive genomes/scaffolds. 

> -f (float)     (OPTIONAL) Set the set weight threshold for SV calls [default: 1.00]. This filter can be raised or lowered to increase or decrease the sensitivity of the caller, respectively. Higher values improve performance of the program but may miss some variants that have lower depth of coverage. (Will be streamlined in future versions of the algorithm)

> -i (integer)   (OPTIONAL) Set the raw read count threshold for SV calls [default: 2]. Increasing this value can improve the performance and specificity of the program. Still, higher values may miss SVs within areas of low sequence coverage.

> -b (integer)   (OPTIONAL) The number of sets to hold in memory at one time [default: 10]. In order to reduce the memory overhead (but increase the runtime of the program) you can lower this value. WARNING: this value must be greater than zero!

Insertion, deletion and tandem duplication output files will be generated by the cluster mode and will display information in a tab delimited file with the following columns:
  1. chromosome
  2. Outside start (the extreme outer edge of read alignments used to find this event)
  3. Inside start (the beginning of the probable SV event -- if split reads map to this event, this is likely the start breakpoint of the SV)
  4. Inside end (the end of the probable SV event -- again, more accurate if split reads aligned)
  5. Outside end (the extreme outer edge of read alignments used to find this event)
  6. The SV type prediction
  7. The number of discordant read pairs that support this event
  8. The number of balanced split reads that support this event
  9. The number of unbalanced split reads that support this event
  10. The total weighted support of all reads that supported this event (cumulative scores from discordant reads and split reads; "-f" controls the minimum value reported)

#### What to do after running the program
While I work on improving the general ease of use of the program, I have included several "helper" programs in my GitHub account to help process the data from RAPTR-SV into better callsets. From personal use, here are my suggestions (I would be very happy to hear of your user experience and/or custom utilities, so please feel free to contact me at the above listed email address!):

##### Filtering
I have created a filtration program (written in perl) that helps narrow down the list of RAPTR-SV calls to a more manageable size, and format the output into a more standard type (here: BED format). In order to run this program, you will need to clone my "perl_toolchain" repository and run a local::lib setup using the install scripts in the base folder of the repository. The repository is at this address: https://github.com/njdbickhart/perl_toolchain

Here is an example usage of the filtration program:

> $ perl ~/perl_toolchain/sequence_data_scripts/filterRAPTRSVFiles.pl -b original.bam.file.used.in.preprocess.bam -i raptr-sv.output.file.deletions -o output.filtered.bed

##### Annotation
Usually, you will want to check to see if your SV calls intersect with other genomic features (ie. genes). I have created a java program that will automatically intersect your SV calls from RAPTR-SV (from the filtered script, or raw output) with multiple genomic feature files, and generate a spreadsheet using the jxl library (http://jexcelapi.sourceforge.net/ ). The repository for this program is at this address: https://github.com/njdbickhart/AnnotateUsingGenomicInfo

Please read the README.md at the repository location for instructions for use of the program.