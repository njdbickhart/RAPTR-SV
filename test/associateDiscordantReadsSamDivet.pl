#!/usr/bin/perl
# This script is designed to process a series of input bed coordinates and associate discordant read pairs with those locations
# It can process sam files (readname sorted) and divet files (from RAPTR-SV)
# NOTE: It assumes that sam files are readname sorted, and that there is one mapping location per read of the pair

use strict;
use Getopt::Std;
use kentBinTools;

my %opts;
my $usage = "perl $0 -b <bed coordinate locations> AND -s <sam file rd sort> OR -d <divet file>\n";

getopt('bsd', \%opts);

unless(defined($opts{'b'}) && ($opts{'s'} || $opts{'d'})){
	print $usage;
	exit;
}

my $kent = kentBinTools->new();
my $storage = bedStoreCounter->new();

# Open bed file and store locations
open(BED, "< $opts{b}") || die "Could not open bed location file! $opts{b}\n$usage";
while(my $line = <BED>){
	chomp $line;
	$storage->addBed($line);
}
close BED;
print STDERR "Finished loading bed file...\n"; 

# Now, check the context of the file input and begin the association
if(defined($opts{'s'})){
	open(SAM, "< $opts{s}") || die "Could not open readname sorted sam! $opts{s}\n";
	my $counter = 0;
	while(my $line = <SAM>){
		if($line =~ /^@/){
			$counter = tell(SAM);
		}else{
			last;
		}
	}
	if($counter > 0){
		seek(SAM, $counter, 0);
	}
	while(my $first = <SAM>){
		my $second = <SAM>;
		my @fsegs = split(/\t/, $first);
		my @ssegs = split(/\t/, $second);
		
		if($fsegs[2] ne $ssegs[2]){next;}
		my $start = $kent->least($fsegs[3], $ssegs[3]);
		my $end = $kent->most($fsegs[3], $ssegs[3]);
		$storage->searchIncrement($fsegs[2], $start, $end);
	}
	close SAM;
}elsif(defined($opts{'d'})){
	open(DIV, "< $opts{d}") || die "Could not open divet file! $opts{d}\n";
	while(my $line = <DIV>){
		chomp $line;
		my @segs = split(/\t/, $line);
		my $start = $kent->least($segs[2], $segs[6]);
		my $end = $kent->most($segs[2], $segs[6]);
		$storage->searchIncrement($segs[1], $start, $end);
	}
	close DIV;
}

# Print out the results
$storage->printOut();

exit;



BEGIN{
package bedCount;
use Mouse;
use namespace::autoclean;

has 'chr' => (is => 'ro', isa => 'Str');
has ['start', 'end'] => (is => 'ro', isa => 'Int');
has 'count' => (
	traits => ['Counter'],
	is => 'rw',
	isa => 'Int',
	default => 0,
	handles => {
		inc_bed => 'inc',
	}
);

sub retString{
	my ($self) = @_;
	return $self->chr . "\t" . $self->start . "\t" . $self->end . "\t" . $self->count;
}
__PACKAGE__->meta->make_immutable;

package bedStoreCounter;
use Mouse;
use namespace::autoclean;
use kentBinTools;

has 'storage' => (
	traits => ['Hash'],
	is => 'rw',
	isa => 'HashRef[HashRef[ArrayRef[bedCount]]]',
	default => sub{{}},
	handles => {
		set_value => 'set',
		get_value => 'get',
	},
);

sub addBed{
	my ($self, $line) = @_;
	my @segs = split(/\t/, $line);
	if(scalar(@segs) < 3){
		return;
	}
	my $kent = kentBinTools->new();
	my $bin = $kent->getbin($segs[1], $segs[2]);
	
	my $bed = bedCount->new('chr' => $segs[0], 'start' => $segs[1], 'end' => $segs[2]);
	my $href = $self->storage();
	push(@{$href->{$segs[0]}->{$bin}}, $bed);
	
	$self->storage($href);			
}

sub searchIncrement{
	my ($self, $chr, $start, $end) = @_;
	my $kent = kentBinTools->new();
	my $href = $self->storage();
	if(!defined($start) || ! defined($end)){return;}
	my $vlen = $end - $start;
	if(exists($href->{$chr})){
		my @bins = $kent->searchbins($start, $end);
		foreach my $b (@bins){
			if(exists($href->{$chr}->{$b})){
				foreach my $c (@{$href->{$chr}->{$b}}){
					if($kent->overlap($c->start, $c->end, $start, $end) >= $vlen / 2){
						$c->inc_bed();
					}
				}
			}
		}
	}
}

sub printOut{
	my ($self) = @_;
	my $href = $self->storage();
	foreach my $chr (sort{$a cmp $b} keys(%{$href})){
		foreach my $b (sort{$a <=> $b} keys(%{$href->{$chr}})){
			foreach my $bed (@{$href->{$chr}->{$b}}){
				print $bed->retString() . "\n";
			}
		}
	}
}

__PACKAGE__->meta->make_immutable;
1;
}
