#!/usr/bin/env bash

if [ $# -ne "1" ]; then
    echo "usage: generate-dataset <path to project>" # <path to output folder>
    exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )" # from https://stackoverflow.com/a/246128

$DIR/gradlew --console=plain -p $DIR runMethodsMover -PprojectFolder="$PWD/$1" #-PoutputDir="$PWD/$2"

