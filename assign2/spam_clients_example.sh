#!/bin/bash

# run this from root git folder : ./assign2/spam_clients_example.sh

# Compile all .java files in the src folder and its subfolders
find assign2/src -name "*.java" -exec javac -d assign2/bin {} +

# Start the server
# java -cp assign2/bin game.server.GameServer &

# Start 10 clients
for i in {1..10}
do
  echo "Starting client $i"
  java -cp assign2/bin game.client.Client > /dev/null &
done

echo "All clients started"

