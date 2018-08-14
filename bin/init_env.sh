#
# set the source code path
# SRCHOME
#
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
SOURCE="$(readlink "$SOURCE")"
[[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

export _gbtproject_=$DIR/../
export PYTHONPATH=$DIR/../src/:$PYTHONPATH
export PATH=$DIR:$PATH

# add useful functions
#alias draw_likelihood='python -m evaluation.test_likelihood -draw'

