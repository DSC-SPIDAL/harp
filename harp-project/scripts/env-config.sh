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

#!/bin/bash/env bash
set -e

#set environment variables
export HARP_ROOT_DIR=$(git rev-parse --show-toplevel)
export HARP_HOME=$HARP_ROOT_DIR/harp-project


#Setting local classpath.
HARP_CLASSPATH=$HARP_HOME/scripts
for i in ${HARP_ROOT_DIR}/third_party/*.jar;
  do HARP_CLASSPATH=$i:${HARP_CLASSPATH}
done

for i in $HARP_HOME/target/*.jar;
  do HARP_CLASSPATH=$i:${HARP_CLASSPATH}
done

