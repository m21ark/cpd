# Matrix Study Project

## About the project

For this project we developed 3 different algorithms for matrix multiplications in two programming languages with the goal of studying the exectuion time and the impact of the memory hierarchy on the processor's efficiency. For that we used a metrics measuring tool called PAPI.

The following presentedinstructions are directed to a Ubuntu based installation.

## Installation

This project uses both C++ and Go languages for analysis. Thus, assuming C/C++ compiler is already installed, only Go requires installation:

> \> `sudo apt install golang`

Note: As PAPI installation varies with each OS, we opted to not cover it here.

## Compilation

To facilitate the compilation and testing of the program, we created a simple Makefile with two commands:

> \>`make`          # compiles both programs in C++ and Go
>
> \>`make clean`    # removes all compiled files

## Execution

To run the generated executable for each language, `matrix_product_c` and `matrix_product_go`, just use:

> \> `make c_run`
> or
> \> `make go_run`

The program is very similar in both languages, asking the user for the matrix size, algorithm to be used for the computation and, in the third's algorithm case, the user is also prompted to give the block size to be used.

## Statistics

As the main goal of these programs was to retrieve large ammounts of data of different matrix sizes in the 3 algorithms, we made a simple life-saver. Both executables can optionally receive a   `stats` argument, making both programms compute all multipication combinations and dumping the desired metrics into new `cpp_stats.txt` and `go_stats.txt` files in the top directory.

Again, for ease of use, just use:

> \> `make c_stats`
> or
> \> `make go_stats`
