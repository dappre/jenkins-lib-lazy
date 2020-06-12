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
FROM ubuntu:16.04

# Install common requirements
RUN apt-get -y update && \
    apt-get -y install \
      git \
      wget \
      unzip \
      which \
    && \
    apt-get clean

# Add user to build and package
ARG uid=1000
ARG user=dummy
ARG gid=1000
ARG group=dummy

RUN groupadd -g "${gid}" "${group}" && \
    useradd -ms /bin/bash -g "${group}" -u "${uid}" "${user}"

# Get script directory from lazyLib
ARG dir=.
