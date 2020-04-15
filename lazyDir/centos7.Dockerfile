# Pull base image from official repo
FROM centos:centos7.7.1908

# Import local GPG keys and enable epel repo
RUN rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7 && \
    yum -q clean expire-cache && \
    yum -q -y update && \
    yum -y install --setopt=tsflags=nodocs epel-release && \
    rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7 && \
    yum -q -y clean all --enablerepo='*'

# Install common requirements
RUN yum -q clean expire-cache && \
    yum -q -y update && \
    yum -y install --setopt=tsflags=nodocs \
      git \
      wget \
      unzip \
      which \
    && \
    yum -q -y clean all --enablerepo='*'

# Add user to build and package
ARG uid=1000
ARG user=dummy
ARG gid=1000
ARG group=dummy

RUN groupadd -g "${gid}" "${group}" && \
    useradd -ms /bin/bash -g "${group}" -u "${uid}" "${user}"

# Get script directory from lazyLib
ARG dir=.
