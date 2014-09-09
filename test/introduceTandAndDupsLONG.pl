#!/usr/bin/perl
# Takes an input file like this: <chr>\t<start loc>\t<end loc>\t<type [TAND, DEL]>\n

use strict;
use Getopt::Std;

my $usage = "perl $0 [options]
	-r	reference genome fasta (single chromosome!)
	-i	input variant file
	-o	output fasta file\n";
my %opts;
getopt('rio', \%opts);
unless(defined($opts{r}) && defined($opts{i}) && defined($opts{o})){
	print "Missing mandatory arguments!\n";
	print $usage;
	exit;
}

my @variants;
# input variant file for later lookup
open(IN, "< $opts{i}") || die "could not open variants file: $opts{i}!\n$usage";
while(my $line = <IN>){
	chomp $line;
	my @segs = split(/\t/, $line);
	my $temp = variant->new(
		'chr' => $segs[0],
		'start' => $segs[1],
		'end' => $segs[2],
		'type' => $segs[3]);
	push(@variants, $temp);
}
close IN;

# Sort variants 
@variants = sort{ $a->end() <=> $b->start()} @variants;

# Read in ref genome fasta and begin buffering
open(IN, "< $opts{r}") || die "Could not open ref genome fasta: $opts{r}!\n$usage";
open(OUT, "> $opts{o}");
my $head = <IN>;
print OUT $head;
my @seq;
while(my $line = <IN>){
	chomp $line; 
	my @bases = split('', $line);
	push(@seq, @bases);
}
close IN;

my @buffer;
my $printout = 0;
for(my $x = 0; $x < scalar(@seq); $x++){
	if(scalar(@variants) > 0){
		my $curvar = $variants[0];
		if($x < $curvar->start()){
			push(@buffer, $seq[$x]);
		}elsif($x == $curvar->start()){
			if($curvar->type() eq "TAND"){
				my $s = $printout;
				my $len = $curvar->end() - $curvar->start();
				$len *= 2;
				$len += $printout;
				for(my $y = $x; $y < $curvar->end(); $y++){
					push(@buffer, $seq[$y]);
					if(scalar(@buffer) >= 60){
						print OUT join('', @buffer) . "\n";
						@buffer = ();
						$printout += 60;
					}
				}
				for(my $y = $x; $y < $curvar->end(); $y++){
					push(@buffer, $seq[$y]);
					if(scalar(@buffer) >= 60){
						print OUT join('', @buffer) . "\n";
						@buffer = ();
						$printout += 60;
					}
				}
				print STDERR $curvar->printCoords() . "\t$s\t$len\n";
				$x = $curvar->end();
				shift(@variants);
			}elsif($curvar->type() eq "DEL"){
				$x = $curvar->end();
				print STDERR $curvar->printCoords() . "\t$printout\n";
				shift(@variants);
			}
		}
		if(scalar(@buffer) >= 60){
			print OUT join('', @buffer) . "\n";
			@buffer = ();
			$printout += 60;
		}
	}else{
		push(@buffer, $seq[$x]);
		if(scalar(@buffer) >= 60){
			print OUT join('', @buffer) . "\n";
			@buffer = ();
			$printout += 60;
		}
	}
}
close OUT;
exit;

BEGIN{
	package variant;
	use Mouse;
	use Mouse::Util::TypeConstraints;
	use namespace::autoclean;
	
	enum 'sv' => qw[ DEL TAND DUP ];
	coerce 'Int' => from 'Str' => via {0 + $_};
	
	has 'chr' => (is => 'ro', isa => 'Str');
	has ['start', 'end'] => (is => 'ro', isa => 'Int', coerce => 1);
	has 'type' => (is => 'ro', isa => 'sv');
	
	sub printCoords{
		my ($self) = @_;
		return $self->chr() . "\t" . $self->start() . "\t". $self->end() . "\t" . $self->type();
	}
	
	__PACKAGE__->meta->make_immutable;
	1;
}