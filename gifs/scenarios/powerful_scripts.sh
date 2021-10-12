#!/bin/bash

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clear

  # Put your stuff here
  pe "echo 'println(\"TODO: turn gifs/scenarios/powerful_scripts.sh into proper scenario showing Scripts are as powerful as other programs" key="scripts-as-apps" scripting="true\")' | scala-cli -"

  # Wait a bit to read output of last command
  sleep 2
  echo " "
fi