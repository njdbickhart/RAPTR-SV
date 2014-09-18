#1 num simulations
#2 base output folder

mkdir $2


for((i=1;i<=${1};i++))
do
    mkdir ${2}/var_${i}
    perl generateTANDAndDelBedRegions.pl umd3_chr29.fa.nonrepeats $2/true_variant_locs_${i}.bed 40 
    perl introduceTandAndDupsLONG.pl -r umd3_chr29.fa -i $2/true_variant_locs_${i}.bed -o $2/chr29_variant_${i}.fa
    sh pipeline.sh var_${i} ${2}/var_${i} $2/chr29_variant_${i}.fa &
    echo "loaded $i"

    ((i++))
    mkdir ${2}/var_${i}
    perl generateTANDAndDelBedRegions.pl umd3_chr29.fa.nonrepeats $2/true_variant_locs_${i}.bed 40
    perl introduceTandAndDupsLONG.pl -r umd3_chr29.fa -i $2/true_variant_locs_${i}.bed -o $2/chr29_variant_${i}.fa
    sh pipeline.sh var_${i} ${2}/var_${i} $2/chr29_variant_${i}.fa &
    echo "loaded $i"

    ((i++))
    mkdir ${2}/var_${i}
    perl generateTANDAndDelBedRegions.pl umd3_chr29.fa.nonrepeats $2/true_variant_locs_${i}.bed 40
    perl introduceTandAndDupsLONG.pl -r umd3_chr29.fa -i $2/true_variant_locs_${i}.bed -o $2/chr29_variant_${i}.fa
    sh pipeline.sh var_${i} ${2}/var_${i} $2/chr29_variant_${i}.fa &
    echo "loaded $i"

    ((i++))
    mkdir ${2}/var_${i}
    perl generateTANDAndDelBedRegions.pl umd3_chr29.fa.nonrepeats $2/true_variant_locs_${i}.bed 40
    perl introduceTandAndDupsLONG.pl -r umd3_chr29.fa -i $2/true_variant_locs_${i}.bed -o $2/chr29_variant_${i}.fa
    sh pipeline.sh var_${i} ${2}/var_${i} $2/chr29_variant_${i}.fa &
    echo "loaded $i"

    ((i++))
    mkdir ${2}/var_${i}
    perl generateTANDAndDelBedRegions.pl umd3_chr29.fa.nonrepeats $2/true_variant_locs_${i}.bed 40
    perl introduceTandAndDupsLONG.pl -r umd3_chr29.fa -i $2/true_variant_locs_${i}.bed -o $2/chr29_variant_${i}.fa
    sh pipeline.sh var_${i} ${2}/var_${i} $2/chr29_variant_${i}.fa
    echo "loaded $i"

done

echo "All done!"
