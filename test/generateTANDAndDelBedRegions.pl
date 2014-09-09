#!/usr/bin/perl
# This script samples regions of a genome to generate fake duplicates and deletions for simulation
# 08/28/2014: Changed the script to extend past repetitive regions so that I can get longer regions
# Also changed the selection areas to account for reads of 100bp in length

use strict;
my @SVs = ("TAND", "DEL");

chomp(@ARGV);
if(scalar(@ARGV) < 3){
	print "Usage: perl $0 <input non-repetitive bed> <output bed file name> <num entries <approx>>\n";
	exit;
}

# Assess nonrepeat file size
my $lines = 0;
open(IN, "< $ARGV[0]") || die "Could not open non-repetitive bed file!\n";
while(my $line = <IN>){
	my @segs = split(/\t/, $line);
	if($segs[2] - $segs[1] < 200){next;} # This avoids biasing our estimates of line counts by counting small fragments that we won't use
	$lines++;
}

seek(IN, 0, 0);

open(OUT, "> $ARGV[1]");
my $randval = int(($lines / $ARGV[2]) + 0.5);
my $variants = 0;
print STDERR "Number of suggested entries: $ARGV[2]\tRandom value cutoff: $randval\n";
my $start = 0;
my $tries = 0;
while(my $line = <IN>){
	chomp $line;
	my @segs = split(/\t/, $line);
	if($start == 0){
		# We haven't selected a start site. Let's check if we can get one and then start the next part of the algorithm
		my $len = $segs[2] - $segs[1];
		if($len > 200){
			# Only work within regions of 200bp or larger
			my $randnum = int(rand($lines));
			if($randnum <= $ARGV[2]){
				$start = $segs[1] + 100; # This should give enough space for the reads from the repeat
			}
		}
	}
	if($start > 0){
		# We have a start site! Let's test to see if we randomly assign an end site
		# I placed this in a separate conditional to test if the current segments contain the start site
		if($segs[2] - $start < 150){
			# This would create an event that is too close to the repeat end to detect
			# Skip!
			next;
		}
		my $randnum = int(rand(3)); # By probability, try to limit extension to about 3 tries
		if($randnum < 1){
			# 33% chance to be the end interval
			my $svtype = int(rand(2));
			my $end = $segs[2] - 150;
			print OUT "$segs[0]\t$start\t$end\t$SVs[$svtype]\n";
			$variants++;
			$start = 0; # Reset the start so we can select a new variant
		}
	}
	if($variants > $ARGV[2] * 1.5){
		last;
	}
}
close IN;
close OUT;
exit;