#!/bin/bash -e

prompt=${1?No prompt message read}
case ${prompt} in
Username*)
  echo "${GIT_USER}"
  ;;
Password*)
  echo "${GIT_PASSWORD}"
  ;;
esac
