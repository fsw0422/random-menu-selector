FROM ubuntu:18.04

ENV LC_ALL=C.UTF-8

# Essentials
RUN apt update \
	&& apt install -y \
		git \
		wget \
		unzip \
		curl \
		apt-transport-https \
		ca-certificates \
		gnupg-agent \
		software-properties-common \
		python3 \
		python3-pip

# Docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - \
	&& add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
	&& curl -L "https://github.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
	&& chmod +x /usr/local/bin/docker-compose \
	&& apt update \
	&& apt install -y \
		docker-ce \
		docker-ce-cli \
		containerd.io

# SBT
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
	&& apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 \
	&& apt update \
	&& apt install -y \
		openjdk-8-jdk \
		sbt

# Liquibase
RUN wget https://github.com/liquibase/liquibase/releases/download/liquibase-parent-3.8.0/liquibase-3.8.0-bin.zip -O /opt/liquibase.zip \
	&& unzip /opt/liquibase.zip -d /opt/liquibase \
	&& chmod +x /opt/liquibase/liquibase \
	&& ln -s /opt/liquibase/liquibase /usr/local/bin
