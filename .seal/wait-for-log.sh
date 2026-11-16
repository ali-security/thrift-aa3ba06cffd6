#!/bin/sh
# Sealed-build helper (not part of upstream Thrift), run from Travis after_script.
# Travis archives job logs asynchronously; the sealer harvests the log as soon as
# the job ends, so keep the job open until its own public log.txt contains the
# result line of the final test command. Prints a heartbeat on every poll so the
# 10-minute no-output watchdog never fires. after_script cannot change the
# build result; this always exits 0.
LEG="${1:-job}"
URL="https://api.travis-ci.com/v3/job/${TRAVIS_JOB_ID}/log.txt"
PATTERN='lib/java check" exited with [0-9]'
CAP=1800
waited=0
while [ "$waited" -lt "$CAP" ]; do
  if curl -fsSL "$URL" 2>/dev/null | grep -Eq "$PATTERN"; then
    echo "[$LEG] Travis log archived after ${waited}s"
    exit 0
  fi
  echo "[$LEG] waiting for Travis log archive... (${waited}s)"
  sleep 20
  waited=$((waited + 20))
done
echo "[$LEG] gave up waiting for Travis log archive after ${CAP}s"
exit 0
