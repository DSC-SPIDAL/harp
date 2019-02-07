FROM ubuntu:18.04

##############################################################################
# Build Instructions
#
# First build Harp project.
# Build Docker container by running following command from HARP_ROOT_DIR
# docker build -t username/harp-k8s:0.1.0 -f core/harp-collective/src/main/java/edu/iu/harp/example/kmeans/k8s/Dockerfile .
##############################################################################

# Disable prompts from apt.
ENV DEBIAN_FRONTEND noninteractive

##############################################################################
# install apt-utils and locales, nano, set up language
RUN apt-get update && apt-get install -y apt-utils locales locales-all nano
ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

##############################################################################
# Install Java 8.
# Based on:
#    https://github.com/dockerfile/java/blob/master/oracle-java8/Dockerfile
#    https://stackoverflow.com/questions/49914574/install-jdk-8-update-172-in-dockerfile-with-ubuntu-image
#

RUN \
   apt-get update && \
   apt-get -y install software-properties-common && \
   echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
   add-apt-repository -y ppa:webupd8team/java && \
   apt-get update && \
   apt-get install -y oracle-java8-installer && \
   rm -rf /var/cache/oracle-jdk8-installer

# Define JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

###############################################################################
# Install Harp files and packages
#
ENV CLASSPATH="/harp/resources/:/harp/lib/*"

ADD core/harp-collective/target/harp-collective-0.1.0.jar \
    third_party/fastutil-7.0.13.jar \
    third_party/log4j-1.2.17.jar \
    third_party/client-java-3.0.0.jar \
    third_party/client-java-api-3.0.0.jar \
    third_party/client-java-proto-3.0.0.jar \
    third_party/guava-20.0.jar \
    third_party/commons-codec-1.11.jar \
    third_party/commons-compress-1.15.jar \
    third_party/commons-lang3-3.6.jar \
    third_party/okhttp-2.7.5.jar \
    third_party/okhttp-ws-2.7.5.jar \
    third_party/slf4j-api-1.7.25.jar \
    /harp/lib/

ADD core/scripts/log4j.properties /harp/resources/

WORKDIR /harp/
