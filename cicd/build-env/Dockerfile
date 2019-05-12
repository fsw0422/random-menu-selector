FROM ubuntu:18.04

# Install common
RUN apt update && apt install -y gnupg2 git openjdk-8-jdk

# Install SBT
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && apt update && apt install -y sbt

WORKDIR /workspace
