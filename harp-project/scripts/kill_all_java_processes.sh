#
# Copyright 2013-2016 Indiana University
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#######################################################
# In case of errors, you can use this script to kill  #
# all java processes in the nodes listed.             #
#######################################################
#!/bin/bash

# First parameter is your username.
if [ $# -ne 1 ]; then
  echo Usage: [username]
  exit -1
fi

# Kill processes from nodes
for line in `cat $HARP_HOME/scripts/nodes`;do
  if [[ $line =~ ^\# ]]; then
    continue
  fi
  echo $line
  for pp in `ssh $line pgrep -u $1 java`;do
    echo $pp
    ssh $line kill -9 $pp
  done
done

# Kill local Java processes
for pp in `pgrep -u $1 java`;do
  echo $pp
  kill -9 $pp
done

