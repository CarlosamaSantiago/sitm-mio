#!/bin/bash
# Sourced por cada script de nodo. Define CP y va al root del repo.
SCRIPT_DIR="$( cd "$( /usr/bin/dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."
CP() { echo "nodes/$1/build/runtime-libs/*:nodes/$1/build/classes/java/main"; }
HOST=127.0.0.1
