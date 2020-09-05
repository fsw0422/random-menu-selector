FROM ubuntu:18.04

ENV LC_ALL=C.UTF-8

# Essentials
RUN apt update \
	&& apt install -y \
		wget \
		unzip \

# Liquibase
RUN wget https://github.com/liquibase/liquibase/releases/download/liquibase-parent-3.8.0/liquibase-3.8.0-bin.zip -O /opt/liquibase.zip \
	&& unzip /opt/liquibase.zip -d /opt/liquibase \
	&& chmod +x /opt/liquibase/liquibase \
	&& ln -s /opt/liquibase/liquibase /usr/local/bin

# Add custom user and group id (For Linux users)
ARG USER_NAME
ARG USER_ID
ARG GROUP_ID
RUN if [ ! -z ${USER_NAME} ] && [ ! -z ${USER_ID} ] && [ ! -z ${GROUP_ID} ]; then \
	groupadd -g ${GROUP_ID} ${USER_NAME} \
	&& useradd -l -u ${USER_ID} -g ${USER_NAME} ${USER_NAME} \
	&& install -d -m 0755 -o ${USER_NAME} -g ${USER_NAME} /home/${USER_NAME} \
	&& usermod -aG docker ${USER_NAME} \
;fi
