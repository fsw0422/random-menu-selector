FROM ubuntu:18.04

ENV LC_ALL=C.UTF-8

# Essentials
RUN echo "Installing Dependencies" \
  && apt update \
  && apt install -y \
    curl \
		apt-transport-https \
		ca-certificates \
		gnupg-agent \
		software-properties-common \
	# Docker
  && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - \
	&& add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
	# SBT
  && echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
	&& apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 \
  && apt update \
  && apt install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
		openjdk-8-jdk \
		sbt \
    python3 \
    python3-pip \
  # Docker Compose
  && curl -L "https://github.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
  && chmod +x /usr/local/bin/docker-compose

# Dependencies
RUN apt install -y libpq-dev python-dev \
  && pip3 install \
		SQLAlchemy==1.3.10 \
		psycopg2==2.8.4 \
		testcontainers==2.5

# Add custom user and group id (For Linux users)
ARG USER_NAME
ARG USER_ID
ARG GROUP_ID
RUN if [ -n ${USER_NAME} ] && [ ${USER_ID:-1} -ne 0 ] && [ ${GROUP_ID:-1} -ne 0 ]; then \
	groupadd -g ${GROUP_ID} ${USER_NAME} \
	&& useradd -l -u ${USER_ID} -g ${USER_NAME} ${USER_NAME} \
	&& install -d -m 0755 -o ${USER_NAME} -g ${USER_NAME} /home/${USER_NAME} \
	&& usermod -aG docker ${USER_NAME} \
;fi
