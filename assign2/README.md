# Server-Client Game Project

## About the project

This course project is a client-server multiplayer game using TCP sockets in Java.
The client authenticates and can then play a simple guess the number game, receiving feedback from the server if the guessed value is to high or low until the attempts end or the correct value is guessed.
The code uses threads and uses locks to avoid concurrency problems and can recover from failures from both client and server by using player tokens and serialized objects. 

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


## Code Architecture and Communication Protocol

More information regarding the project can be accessed in the PDF file in the 'doc' folder.
