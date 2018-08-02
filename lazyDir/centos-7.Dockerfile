# Pull base image from official repo
FROM centos:centos7.5.1804

# Enable epel repo and Install all current updates
RUN yum -q -y update \
	&& rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7 \
	&& yum -y install epel-release \
	&& rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7 \
	&& yum -y upgrade \
	&& yum -q clean all

# Install common requirements
RUN yum -q -y update \
	&& yum -y install \
	git \
	wget \
	unzip \
	which \
	&& yum -q clean all

# Add user to build and package
ARG uid=1000
ARG user=dummy
ARG gid=1000
ARG group=dummy

RUN groupadd -g "${gid}" "${group}" && useradd -ms /bin/bash -g "${group}" -u "${uid}" "${user}"

# Get script directory from lazyLib
ARG dir=.
