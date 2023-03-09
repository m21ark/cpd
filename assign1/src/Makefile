main:
	g++ c/matrix_product.cpp -O2 -lpapi -Wall -Wextra -o c/matrix_product_c
	go build -o go/matrix_product_go go/matrix_product_go.go 
	
clean:
	rm -f c/matrix_product_c go/matrix_product_go go_stats.txt cpp_stats.txt

go_run: 
	./go/matrix_product_go

c_run:
	./c/matrix_product_c

go_stats:
	./go/matrix_product_go stats

c_stats:
	./c/matrix_product_c stats
