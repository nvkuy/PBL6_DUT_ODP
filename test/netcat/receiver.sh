#!/bin/bash

PORT=12345
RANDOM_NAME=$(date +%s%N | md5sum | head -c 10).zip
nc -l -p $PORT > "./$RANDOM_NAME"
echo "Data received and saved to $RANDOM_NAME"