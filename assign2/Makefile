main:
	find src -name "*.java" -print | xargs javac 
	
clean:
	find src -name "*.class" -type f -delete && rm -rf cache

server: 
	java -cp src game.server.GameServer

client:
	java -cp src game.client.Client
