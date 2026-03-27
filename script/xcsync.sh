#!/bin/bash
if [ $# -lt 1 ]; then
    echo "Usage: xsync.sh <file_or_directory>"
    exit 1
fi
p1=$1
fname=$(basename $p1)
pdir=$(cd -P $(dirname $p1); pwd)
cuser=$(whoami)
for host in bjc56 bjc57; do
    echo "========== Syncing $fname to $host =========="
    rsync -av $pdir/$fname $cuser@$host:$pdir/
done