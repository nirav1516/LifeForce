#!/bin/bash
#
# creates the python classes for our .proto
#

project_base="/home/dhruv/workspace_KEPLER/core-netty-4.2/python"


rm ${project_base}/src/comm_pb2.py

protoc -I=/home/dhruv/workspace_KEPLER/core-netty-4.2/resources --python_out=. /home/dhruv/workspace_KEPLER/core-netty-4.2/resources/comm.proto 
