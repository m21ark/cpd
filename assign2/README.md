# Server-Client Game Project

## About the project

This course project is a client-server multiplayer game using TCP sockets in Java.
The client authenticates and can then play a simple guess the number game, receiving feedback from the server if the guessed value is to high or low until the attempts end or the correct value is guessed.
The code uses threads and uses locks to avoid concurrency problems and can recover from failures from both client and server by using player tokens and serialized objects.

## Checklist

All objectives were sucessfully completed:

- [x] Client instance
- [x] Server Instance
- [x] Fully Playable Game
- [x] Authentication with Optional Player Token (with expiration)
- [x] New Player Registration
- [x] Simple and Ranked Matchmaking modes
- [x] Scheduler for Rank Relaxation on Ranked Mode
- [x] Fault tolerance on Client (on every state)
- [x] Fault tolerance on Server (serialized state)
- [x] Locks for concurrent access (data structures and files)
- [x] Safe Thread Management

## Building and Execution

This project was written in Java SE 17 and requires the Java SDK to build.

To facilitate the compilation and testing of the program, we created a simple Makefile with four commands:

> \>`make`          # builds all .class files needed to run project
>
> \>`make clean`    # removes all .class and cache files
>
> \>`make server`   # runs the server instace
>
> \>`make client`    # runs a client instance

**Note:** The commands are only available on Linux and macOS. On Windows, the commands can be run manually or, as we recommend, via **InteliJ**.

## Code Architecture and Communication Protocol

More information regarding the project can be accessed in the PDF file in the 'doc' folder.

## Authentication

The client can authenticate using an existing player credentials or by registering a new player.
The credentials are stored in the `database/users.txt` file.

## Game Modes

The game has two modes: simple and ranked.
The initial configuration is Simple with thread pool size of 5 with two players on each.

The mode can be changed easily on the `resources/config.properties` file.

We recommend the following:

For Simple Mode:
> mode=Simple
> 
> gamePoolSize=5
> 
> nrMaxPlayersInGame=2

For Ranked Mode:
> mode=Ranked
> 
> gamePoolSize=1
> 
> nrMaxPlayersInGame=2

We recommend using a game pool size of 1, we ensure that only one game is running at a time, which makes testing the Ranked Mode easier.

## Important Notes

Our code uses a cache folder to save the state of clients and server. 
We recommend that you delete the contents of this folder before running the program for the first time to avoid unexpected behavior.
That can be done using `make clean` or manually.

## Authors

- Lia Vieira - up202005042
- Marco André - up202004891
- Ricardo Matos - up202007962

