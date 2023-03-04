package main

import (
	"fmt"
	"math"
	"os"
	"time"
)

func deleteIfExists(filename string) error {
	if _, err := os.Stat(filename); os.IsNotExist(err) {
		return nil
	}
	if err := os.Remove(filename); err != nil {
		return err
	}
	return nil
}

func minInt(a int, b int) int {
	if a < b {
		return a
	}
	return b
}

// Function used only for debugging
/*
func printMatrix(m_ar int, m_br int, phc []float64) {
	fmt.Println("\nResult matrix:")

	minVal_a := m_ar //minInt(m_ar, 10)
	minVal_b := m_br //minInt(m_br, 10)

	fmt.Println("Showing only ", minVal_a, "x", minVal_b, "values")

	for i := 0; i < minVal_a; i++ {
		for j := 0; j < minVal_b; j++ {
			fmt.Print(phc[i*m_ar+j], " ")
		}
		fmt.Println()
	}

	fmt.Println()
}
*/

func genStartMatrix(m_ar int, m_br int) ([]float64, []float64, []float64) {

	// why m_ar * m_ar? shouldnt be m_ar * m_br?
	pha := make([]float64, m_ar*m_br)
	phb := make([]float64, m_ar*m_br)
	phc := make([]float64, m_ar*m_br)

	var i, j int

	for i = 0; i < m_ar; i++ {
		for j = 0; j < m_ar; j++ {
			pha[i*m_ar+j] = 1.0
		}
	}

	for i = 0; i < m_ar; i++ {
		for j = 0; j < m_br; j++ {
			phb[i*m_br+j] = float64(i) + 1.0
		}
	}

	return pha, phb, phc
}

func OnMult(m_ar int, m_br int) {

	pha, phb, phc := genStartMatrix(m_ar, m_br)

	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_br; j++ {

			temp := 0.0
			for k := 0; k < m_ar; k++ {
				temp += pha[i*m_ar+k] * phb[k*m_br+j]
			}
			phc[i*m_ar+j] = temp

		}
	}

	// printMatrix(m_ar, m_br, phc)
}

func OnMultLine(m_ar int, m_br int) {

	pha, phb, phc := genStartMatrix(m_ar, m_br)

	for i := 0; i < m_ar; i++ {
		for k := 0; k < m_ar; k++ {
			for j := 0; j < m_br; j++ {
				phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j]
			}
		}
	}

	// printMatrix(m_ar, m_br, phc)

}

func OnMultBlock(m_ar int, m_br int, bkS int) {

	pha, phb, phc := genStartMatrix(m_ar, m_br)

	var bkSize float64 = float64(bkS)

	num_blocks_i := int(math.Ceil(float64(m_ar) / bkSize))
	num_blocks_j := int(math.Ceil(float64(m_br) / bkSize))
	num_blocks_k := int(math.Ceil(float64(m_ar) / bkSize))

	for i := 0; i < num_blocks_i; i++ {
		for j := 0; j < num_blocks_j; j++ {
			for k := 0; k < num_blocks_k; k++ {

				i_start := i * bkS
				i_end := minInt((i+1)*bkS, m_ar)

				j_start := j * bkS
				j_end := minInt((j+1)*bkS, m_br)

				k_start := k * bkS
				k_end := minInt((k+1)*bkS, m_ar)

				for i2 := i_start; i2 < i_end; i2++ {
					for k2 := k_start; k2 < k_end; k2++ {
						for j2 := j_start; j2 < j_end; j2++ {
							phc[i2*m_ar+j2] += pha[i2*m_ar+k] * phb[k2*m_br+j2] // LINHA POR LINHA
						}
					}
				}
			}
		}
	}

}

func statistics() {

	var txt string

	deleteIfExists("go_stats.txt")
	file, _ := os.Create("go_stats.txt")

	file.Write([]byte("Execution time for matrix multiplication in Go (All times are in seconds)"))

	file.Write([]byte("\n\nOption 1 (Multiplication) - Size 600x600 to 3000x3000 (+400)\n"))
	for size := 600; size <= 3000; size += 400 {

		txt = fmt.Sprintf("\t%dx%d:  ", size, size)
		file.Write([]byte(txt))

		t1 := time.Now()
		OnMult(size, size)
		t2 := time.Now()

		txt = fmt.Sprintf("\t%v\n", t2.Sub(t1).Seconds())

		file.Write([]byte(txt))
	}

	file.Write([]byte("\n\nOption 2 (Line Multiplication) - Size 600x600 to 3000x3000 (+400)\n"))
	for size := 600; size <= 3000; size += 400 {

		txt = fmt.Sprintf("\t%dx%d:  ", size, size)
		file.Write([]byte(txt))

		t1 := time.Now()
		OnMultLine(size, size)
		t2 := time.Now()

		txt = fmt.Sprintf("\t%v\n", t2.Sub(t1).Seconds())

		file.Write([]byte(txt))
	}

	for size := 4096; size <= 10240; size += 2048 {

		txt = fmt.Sprintf("\t%dx%d:  ", size, size)
		file.Write([]byte(txt))

		t1 := time.Now()
		OnMultLine(size, size)
		t2 := time.Now()

		txt = fmt.Sprintf("\t%v\n", t2.Sub(t1).Seconds())

		file.Write([]byte(txt))
	}

	file.Write([]byte("\n\nOption 3 (Block Multiplication) - Size 4096x4096 to 10240x10240 (+2048) and Block 128 up to 512\n\n"))
	for block := 128; block <= 512; block *= 2 {
		file.Write([]byte("\tBlock: " + fmt.Sprint(block) + "\n\t============================\n"))
		for size := 4096; size <= 10240; size += 2048 {

			txt = fmt.Sprintf("\t%dx%d:  ", size, size)
			file.Write([]byte(txt))

			t1 := time.Now()
			OnMultBlock(size, size, block)
			t2 := time.Now()

			txt = fmt.Sprintf("\t%v\n", t2.Sub(t1).Seconds())

			file.Write([]byte(txt))
		}
		file.Write([]byte("\n\n"))
	}

	file.Close()
}

func main() {

	if len(os.Args) > 1 {
		if os.Args[1] == "stats" {
			fmt.Println("Generating statistics... ")
			statistics()
			fmt.Println("All statistics are done!")
			return
		} else {
			fmt.Println("Usage: go run main.go [stats]")
			return
		}
	}

	var lin, blockSize int

	fmt.Print("Matrix size (N=M): ")
	fmt.Scan(&lin)
	fmt.Println("Values given where: ", lin, "x", lin)

	fmt.Println("1. Multiplication")
	fmt.Println("2. Line Multiplication")
	fmt.Println("3. Block Multiplication")

	var op int
	fmt.Print("> ")
	fmt.Scan(&op)

	t1 := time.Now()

	switch op {
	case 1:
		fmt.Println("Multiplication of", lin, "X", lin, "started...")
		OnMult(lin, lin)
	case 2:
		fmt.Println("Line Multiplication of", lin, "X", lin, "started...")
		OnMultLine(lin, lin)
	case 3:
		fmt.Print("Block size: ")
		fmt.Scan(&blockSize)
		if blockSize > lin || blockSize < 0 {
			blockSize = lin
		}
		fmt.Println("Block Multiplication of", lin, "X", lin, "and size", blockSize, "started...")
		t1 = time.Now()
		OnMultBlock(lin, lin, blockSize)
	default:
		fmt.Println("Invalid option")
	}

	t2 := time.Now()

	fmt.Println("Time elapsed: ", t2.Sub(t1))
}
