#!/usr/bin/perl
# this script takes delly, duppy and my RPSR data and tests the accurracy of the methods against a bed file of known Variant locations
# chr29   989283  989603  990497  990875  TANDEM  8       0       0       8.0  <- RPSR
# chr29   989283  990910  1627    69      35.4454 >Duplication_xxx_00000000<    <- delly/duppy
# NOTE: this variant program merges both delly and RPSR

use strict;
use kentBinTools;
my $usage = "perl $0 <rpsr dels> <rpsr tands> <delly> <duppy> <known locs bed> <outbase>\n";

chomp(@ARGV);
if(scalar(@ARGV) < 6){
	print $usage;
	exit;
}

# load variants into container
my %container; # {chr}->{bin}->[] variant
open(IN, "< $ARGV[4]") || die "Could not open known locs bed: $ARGV[4]!\n";
my %variantcount;
while(my $line = <IN>){
	chomp $line;
	my @segs = split(/\t/, $line);
	my $var = variant->new(
		'chr' => $segs[0],
		'start' => $segs[1],
		'end' => $segs[2],
		'type' => $segs[3]);
	my $bin = kent_bin_tools::getbin($segs[1], $segs[2]);
	push(@{$container{$segs[0]}->{$bin}}, $var);
	if(exists($variantcount{$segs[3]})){
		$variantcount{$segs[3]} += 1;
	}else{
		$variantcount{$segs[3]} = 1;
	}
}
close IN;

my @types = ("RPSRDELS", "RPSRTAND", "DELLY", "DUPPY");
# Filtered types: RPSRRDFILTDELS RPSRNFILTDELS RPSRRDFILTTAND RPSRNFILTTAND TANDBOTH DELBOTH
my $isfilteredfile = 0;
my @files = ($ARGV[0], $ARGV[1], $ARGV[2], $ARGV[3]);
my %foundcount;
my %totalcount;
# Now to work on the files in an orderly fashion!
for(my $x = 0; $x < scalar(@types); $x++){
	my $t = $types[$x];
	$foundcount{$t} = 0;
	$totalcount{$t} = 0;
	if($x == 0){
		open(IN, "perl -lane 'print \"\$F[0]\\t\$F[2]\\t\$F[3]\";' < $files[$x] | perl ~/bin/sortBedFileSTDIN.pl | mergeBed -i stdin |") || die "Could not open $t file: $files[$x]!\n";
	}elsif($x == 1){
		open(IN, "perl -lane 'print \"\$F[0]\\t\$F[1]\\t\$F[4]\";' < $files[$x] | perl ~/bin/sortBedFileSTDIN.pl | mergeBed -i stdin |") || die "Could not open $t file: $files[$x]!\n"; 
	}elsif($x == 2 || $x == 3){
		open(IN, "grep '>' $files[$x] | perl -lane 'print \"\$F[0]\\t\$F[1]\\t\$F[2]\";' | perl ~/bin/sortBedFileSTDIN.pl | mergeBed -i stdin |") || die "Could not open $t file: $files[$x]!\n";
	}
	while(my $line = <IN>){
		chomp $line;
		my @segs = split(/\t/, $line);
		if($x == 2 || $x == 3){
			# It's a delly or duppy file 
			#if($segs[6] =~ /\>.+\</){
				my @bins = kent_bin_tools::searchbins($segs[1], $segs[2]);
				my $addcount = searcher($segs[0], \@bins, \%container, $t, $segs[1], $segs[2]);
				$foundcount{$t} = $foundcount{$t} + scalar(@{$addcount});
			#}
		}else{
			# It's an RPSR file
			if($x == 0){
				# Deletions
				my @bins = kent_bin_tools::searchbins($segs[1], $segs[2]);
				my $addcount;
				if(scalar(@segs) > 10){
					$isfilteredfile = 1;
					$addcount = searcher($segs[0], \@bins, \%container, "RPSRRDFILTDELS", $segs[1], $segs[2], $segs[12]);
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"RPSRRDFILTDELS"} = $foundcount{"RPSRRDFILTDELS"} + 1;
						}else{
							$foundcount{"RPSRRDFILTTAND"} = $foundcount{"RPSRRDFILTTAND"} + 1;
							$totalcount{"RPSRRDFILTTAND"} = $totalcount{"RPSRRDFILTTAND"} + 1;
						}
					}
					
					if(!$segs[12]){
						$totalcount{"RPSRRDFILTDELS"} = $totalcount{"RPSRRDFILTDELS"} + 1;
					}
					
					$addcount = searcher($segs[0], \@bins, \%container, "RPSRNFILTDELS", $segs[1], $segs[2], $segs[11]);
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"RPSRNFILTDELS"} = $foundcount{"RPSRNFILTDELS"} + 1;
						}else{
							$foundcount{"RPSRNFILTTAND"} = $foundcount{"RPSRNFILTTAND"} + 1;
							$totalcount{"RPSRNFILTTAND"} = $totalcount{"RPSRNFILTTAND"} + 1;
						}
					}
					if(!$segs[11]){
						$totalcount{"RPSRNFILTDELS"} = $totalcount{"RPSRNFILTDELS"} + 1;
					}
					
					$addcount = searcher($segs[0], \@bins, \%container, "DELBOTH", $segs[1], $segs[2], (($segs[11] || $segs[12])? 1 : 0));
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"DELBOTH"} = $foundcount{"DELBOTH"} + 1;
						}else{
							$foundcount{"TANDBOTH"} = $foundcount{"TANDBOTH"} + 1;
							$totalcount{"TANDBOTH"} = $totalcount{"TANDBOTH"} + 1;
						}
					}
					if(!$segs[11] && !$segs[12]){
						$totalcount{"DELBOTH"} = $totalcount{"DELBOTH"} + 1;
					}
				}
				$addcount = searcher($segs[0], \@bins, \%container, $t, $segs[1], $segs[2]);
				foreach my $a (@{$addcount}){
					if($a eq "DEL"){
						$foundcount{"RPSRDELS"} = $foundcount{"RPSRDELS"} + 1;
					}else{
						$foundcount{"RPSRTAND"} = $foundcount{"RPSRTAND"} + 1;
						$totalcount{"RPSRTAND"} = $totalcount{"RPSRTAND"} + 1;
					}
				}
				
			}else{
				# Dups
				my @bins = kent_bin_tools::searchbins($segs[1], $segs[2]);
				my $addcount;
				if(scalar(@segs) > 10){
					$isfilteredfile = 1;
					$addcount = searcher($segs[0], \@bins, \%container, "RPSRRDFILTTAND", $segs[1], $segs[2], $segs[12]);
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"RPSRRDFILTDELS"} = $foundcount{"RPSRRDFILTDELS"} + 1;
						}else{
							$foundcount{"RPSRRDFILTTAND"} = $foundcount{"RPSRRDFILTTAND"} + 1;
						}
					}
					if(!$segs[12]){
						$totalcount{"RPSRRDFILTTAND"} = $totalcount{"RPSRRDFILTTAND"} + 1;
					}
					$addcount = searcher($segs[0], \@bins, \%container, "RPSRNFILTTAND", $segs[1], $segs[2], $segs[11]);
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"RPSRNFILTDELS"} = $foundcount{"RPSRNFILTDELS"} + 1;
						}else{
							$foundcount{"RPSRNFILTTAND"} = $foundcount{"RPSRNFILTTAND"} + 1;
						}
					}
					if(!$segs[11]){
						$totalcount{"RPSRNFILTTAND"} = $totalcount{"RPSRNFILTTAND"} + 1;
					}
					$addcount = searcher($segs[0], \@bins, \%container, "TANDBOTH", $segs[1], $segs[2], (($segs[11] || $segs[12])? 1 : 0));
					foreach my $a (@{$addcount}){
						if($a eq "DEL"){
							$foundcount{"DELBOTH"} = $foundcount{"DELBOTH"} + 1;
						}else{
							$foundcount{"TANDBOTH"} = $foundcount{"TANDBOTH"} + 1;
						}
					}
					if(!$segs[11] && !$segs[12]){
						$totalcount{"TANDBOTH"} = $totalcount{"TANDBOTH"} + 1;
					}
				}
				$addcount = searcher($segs[0], \@bins, \%container, $t, $segs[1], $segs[2]);
				foreach my $a (@{$addcount}){
					if($a eq "DEL"){
						$foundcount{"RPSRDELS"} = $foundcount{"RPSRDELS"} + 1;
					}else{
						$foundcount{"RPSRTAND"} = $foundcount{"RPSRTAND"} + 1;
					}
				}
			}
		}
			$totalcount{$t} = $totalcount{$t} + 1;

	}
	close IN;
}

open(OUT, "> $ARGV[5].tab");
if($isfilteredfile){
	push(@types, ("RPSRRDFILTDELS", "RPSRNFILTDELS", "RPSRRDFILTTAND", "RPSRNFILTTAND", "DELBOTH", "TANDBOTH"));
}
my @sortedkeys = sort{$a cmp $b} @types;

print OUT "Program\tFound\tTotal\tPrecis\tRecall\tDelcount\tTandcount\n";
foreach my $v (@sortedkeys){
	my $precision = 0;
	if(!exists($foundcount{$v})){
		$foundcount{$v} = 0;
	}
	if(exists($totalcount{$v})){
		if($totalcount{$v} == 0){
			$precision = 0;
		}
		else{
			$precision = $foundcount{$v} / $totalcount{$v};
		}
	}else{
		$totalcount{$v} = 0;
	}
	my $recall = 0;
	my $key = ($v eq "DELLY" || $v eq "RPSRRDFILTDELS" || $v eq "RPSRNFILTDELS" || $v eq "DELBOTH" || $v eq "RPSRDELS")? "DEL" : "TAND";
	$recall = ($variantcount{$key} > 0)? $foundcount{$v} / $variantcount{$key} : 0;
	print OUT "$v\t$foundcount{$v}\t$totalcount{$v}\t$precision\t$recall\t$variantcount{DEL}\t$variantcount{TAND}\n";
}
close OUT;

open(OUT, "> $ARGV[5].matrix");
print OUT "chr\tstart\tend\ttype\t" . join("\t", @sortedkeys) . "\n";
foreach my $chr (sort{$a cmp $b} keys(%container)){
	my @tempVar;
	foreach my $b (keys(%{$container{$chr}})){
		push(@tempVar, @{$container{$chr}->{$b}});
	}
	
	foreach my $v (sort {$a->start() <=> $b->start()} @tempVar){
		my $str = $v->printCoords(@sortedkeys);
		print OUT $str . "\n";
	}
}
close OUT;


open(OUT, "> $ARGV[5].support");
print OUT "chr\tstart\tend\ttype\n";
foreach my $chr (sort{$a cmp $b} keys(%container)){
	my @tempVar;
	foreach my $b (keys(%{$container{$chr}})){
		push(@tempVar, @{$container{$chr}->{$b}});
	}
	
	foreach my $v (sort {$a->start() <=> $b->start()} @tempVar){
		my $str = $v->printSupport(@sortedkeys);
		print OUT $str;
	}
}
close OUT;
print STDERR "Output in $ARGV[5].tab, $ARGV[5].matrix and $ARGV[5].support\n";
exit;
sub searcher{
	my($chr, $bins, $container, $type, $start, $end, $filtcol) = @_;
	my @added;
	foreach my $b (@{$bins}){
		if(exists($container->{$chr}->{$b})){
			foreach my $row (@{$container->{$chr}->{$b}}){
				if($filtcol){
						next;
				}
				if($start < $row->end() + 100 && $end > $row->start() - 100){
					
					if($row->exists_caller($type)){
						#my $num = $row->get_caller($type) + 1;
						#$row->set_caller($type => $num);
						$row->set_coords($type => [$chr, $start, $end]);
						next; # We don't want to add this as it is a redundant call!
					}else{
						$row->set_caller($type => 1);
						$row->set_coords($type => [$chr, $start, $end]);
					}
					push(@added, $row->type());
					last;
					# This "last" was added to only tally each deletion/dup once
				}
			}
		}
	}
	return \@added;
}
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
	
	has 'discovery' => (
		traits => ['Hash'],
		is => 'rw',
		isa => 'HashRef[Any]',
		default => sub{{}},
		handles => {
			set_caller => 'set',
			get_caller => 'get',
			exists_caller => 'exists',
		}
	);
	
	has 'coords' => (
		traits => ['Hash'],
		is => 'rw',
		isa => 'HashRef[ArrayRef[Any]]',
		default => sub{{}},
		handles => {
			set_coords => 'set',
			get_coords => 'get',
			exists_coords => 'exists',
		}
	);
	
	sub printCoords{
		my ($self, @callers) = @_;
		my $str = $self->chr() . "\t" . $self->start() . "\t". $self->end() . "\t" . $self->type();
		foreach my $c (@callers){
			if($self->exists_caller($c)){
				$str .= "\t" . $self->get_caller($c);
			}else{
				$str .= "\t";
			}
		}
		return $str;
	}
	
	sub printSupport{
		my ($self, @callers) = @_;
		my $str = $self->chr() . "\t" . $self->start() . "\t". $self->end() . "\t" . $self->type() . "\n";
		foreach my $c (@callers){
			if($self->exists_coords($c)){
				my $aref = $self->get_coords($c);
				$str .= "\t" . $c . "\t" . $aref->[0] . "\t" . $aref->[1] . "\t" . $aref->[2] . "\n";
			}
		}
		return $str;
	}
	
	__PACKAGE__->meta->make_immutable;
	1;
}
