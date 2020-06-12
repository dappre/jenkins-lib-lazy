#
#  This work is protected under copyright law in the Kingdom of
#  The Netherlands. The rules of the Berne Convention for the
#  Protection of Literary and Artistic Works apply.
#  Digital Me B.V. is the copyright owner.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# Pull base image from official repo
FROM centos:centos7.8.2003

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
