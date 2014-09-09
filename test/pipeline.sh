# $1 = basename
# $2 = outputdir
# $3 = input variant chr29 fasta
workdir=/mnt/iscsi/vnx_gliu_7

wgsimf1=$2/${1}_r1.fq
wgsimf2=$2/${1}_r2.fq
wgsimkey=$2/${1}_snpindel.key

bwasai1=$2/${1}_r1.sai
bwasai2=$2/${1}_r2.sai
bwasam=$2/${1}.sam
bwabam=$2/${1}.bam
bwasorted=$2/${1}.sorted

#mrssam1=$2/${1}_r1.sam
#mrssam2=$2/${1}_r2.sam
#mrsbam1=$2/${1}_mrsfast.r1.bam
#mrsbam2=$2/${1}_mrsfast.r2.bam
#mrssortedbam1=$2/${1}_mrsfast.r1.sorted
#mrssortedbam2=$2/${1}_mrsfast.r2.sorted


#srbase=$2/${1}.sr
#divet=$srbase.divet.vh
#anchor=$srbase.single.txt
#splitfq=$srbase.split.fq
#splitsam=$srbase.split.sam
#splitbam=$srbase.split.bam
#splitsorted=$srbase.split.sorted
filter=`echo ${1}"_mrsfast*"`;

#rdhits=$2/${1}_rd.hits
#gzhits=$2/${1}_rd.hits.gz

rpsrpreprocess=${2}/${1}_rpsr.preprocess
rpsrcluster=${2}/${1}_rpsr.cluster

dellyout=$2/${1}_vars_delly.txt
dellybr=$2/${1}_breaks_delly.br
duppyout=$2/${1}_vars_duppy.txt
duppybr=$2/${1}_breaks_duppy.br

#list=$2/${1}_vh.list
#vhsrout=$2/${1}_vh.calls

mkdir $2

./wgsim-master/wgsim -N 2500000 -1 100 -2 100 -r 0.0010 -R 0.0 ${3} $wgsimf1 $wgsimf2 > $wgsimkey
perl -ne '$_ =~ s/_/-/g; print $_;' < $wgsimf1 > temp
mv temp $wgsimf1
perl -ne '$_ =~ s/_/-/g; print $_;' < $wgsimf2 > temp
mv temp $wgsimf2

       
#mrsfast --search umd3_chr29.fa --seq $wgsimf1 -o $mrssam1 -e 4
#mrsfast --search umd3_chr29.fa --seq $wgsimf2 -o $mrssam2 -e 4

# Doing BWA alignment in the background
bwa aln umd3_chr29.fa $wgsimf1 > $bwasai1
bwa aln umd3_chr29.fa $wgsimf2 > $bwasai2
bwa sampe umd3_chr29.fa $bwasai1 $bwasai2 $wgsimf1 $wgsimf2 > $bwasam 
	
	# OK, now to generate the pairing information
# ~/jdk1.7.0/bin/java -jar ../bin/pairMatchMrsfastSam.jar -i1 $mrssam1 -i2 $mrssam2 -f1 $wgsimf1 -f2 $wgsimf2 -o $srbase -m 400000 -l 300 -u 650
        
	
	# Now to align the split reads:
#mrsfast --search umd3_chr29.fa --seq $splitfq -e 2 -o $splitsam

	
	# Now to make the RD windows
#samtools view -bS -t umd3_chr29.fa.fai $mrssam1 > $mrsbam1
#samtools view -bS -t umd3_chr29.fa.fai $mrssam2 > $mrsbam2
#~/jdk1.7.0/bin/java -jar ~/bin/CondenseDOCBamsHash.jar -I ${2} -F ${filter} -O $rdhits -X umd3_chr29.fa.fai
#gzip $rdhits

#samtools sort $mrsbam1 $mrssortedbam1 &
#samtools sort $mrsbam2 $mrssortedbam2 
	
	# OK, now I'm ready to run my VHSR program on all the files
	# Just make the list file...
#samtools view -bS -t umd3_chr29.fa.fai $splitsam > $splitbam 
#samtools index $mrssortedbam1.bam &
#samtools index $mrssortedbam2.bam &
#samtools sort $splitbam $splitsorted
#echo -e "$splitsorted.bam\t$divet\t$anchor\t500\t50" > $list

samtools view -bS $bwasam > $bwabam
samtools sort $bwabam $bwasorted	
samtools index $bwasorted.bam

# RPSR preprocessing
~/jdk1.8.0_05/bin/java -jar ~/bin/RPSR.jar preprocess -i $bwasorted.bam -o $rpsrpreprocess -r umd3_chr29.fa -s 10000 
~/jdk1.8.0_05/bin/java -jar ~/bin/RPSR.jar cluster -s $rpsrpreprocess.flat -c chr29 -g /mnt/iscsi/vnx_gliu_7/reference/umd3_gaps_ftp.bed -o $rpsrcluster -f 3 
#samtools index $splitsorted.bam



$workdir/delly_source_v0.0.9/pemgr/delly/delly -o $dellyout -g umd3_chr29.fa -b $dellybr $bwasorted.bam &
$workdir/delly_source_v0.0.9/pemgr/duppy/duppy -o $duppyout -g umd3_chr29.fa -b $duppybr $bwasorted.bam

#rm $mrssam1 $mrssam2 $splitsam $mrssam1.nohit $mrssam2.nohit $splitsam.nohit
#rm $mrsbam1 $mrsbam2 $splitbam $bwabam $bwasam
rm $bwasam $bwabam
