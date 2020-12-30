
# publish.sh <testcases>

NAME=cs132
TESTCASES=$1

if [ -z "$TESTCASES" ]; then
  echo "usage: $0 <testcases dir>"
  exit 1;
fi

set -e
echo "git archive"
git archive --format tar --prefix=$NAME/ HEAD | tar -xC /tmp/

echo "git -C (use repository directory)"
git -C $TESTCASES archive --format tar --prefix=$NAME/testcases/ master | tar -xC /tmp/

echo "tar compress"
tar zcf $NAME.tgz -C /tmp $NAME

echo "cleanup"
rm -rf /tmp/$NAME
