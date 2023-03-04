#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <math.h>
#include <fstream>

using namespace std;

#define SYSTEMTIME clock_t

#define START_PAPI              \
	ret = PAPI_start(EventSet); \
	if (ret != PAPI_OK)         \
		cout << "ERROR: Start PAPI" << endl;

#define STOP_PAPI                                \
	ret = PAPI_stop(EventSet, values);           \
	if (ret != PAPI_OK)                          \
		cout << "ERROR: Stop PAPI" << endl;      \
	file << "\t\tL1 DCM: " << values[0];         \
	file << "\t\tL2 DCM: " << values[1] << endl; \
	ret = PAPI_reset(EventSet);                  \
	if (ret != PAPI_OK)                          \
		std::cout << "FAIL reset" << endl;

void OnMult(int m_ar, int m_br)
{

	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_br) * sizeof(double));
	phb = (double *)malloc((m_ar * m_br) * sizeof(double));
	phc = (double *)malloc((m_ar * m_br) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
			pha[i * m_br + j] = (double)1.0;

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = clock();

	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
		{
			temp = 0;
			for (k = 0; k < m_ar; k++)
			{
				temp += pha[i * m_ar + k] * phb[k * m_br + j];
			}
			phc[i * m_ar + j] = temp;
		}
	}

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

// add code here for line x line matriz multiplication
void OnMultLine(int m_ar, int m_br)
{

	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = clock();

	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

// add code here for block x block matriz multiplication
void OnMultBlock_Iterative(int m_ar, int m_br, int bkSize)
{

	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = clock();

	int num_blocks_i = std::ceil(static_cast<double>(m_ar) / bkSize);
	int num_blocks_j = std::ceil(static_cast<double>(m_br) / bkSize);
	int num_blocks_k = std::ceil(static_cast<double>(m_ar) / bkSize);

	for (int i = 0; i < num_blocks_i; i++)
	{
		for (int j = 0; j < num_blocks_j; j++)
		{
			for (int k = 0; k < num_blocks_k; k++)
			{
				int i_start = i * bkSize;
				int i_end = std::min((i + 1) * bkSize, m_ar);
				int j_start = j * bkSize;
				int j_end = std::min((j + 1) * bkSize, m_br);
				int k_start = k * bkSize;
				int k_end = std::min((k + 1) * bkSize, m_ar);

				for (int i = i_start; i < i_end; i++)
				{
					for (int k = k_start; k < k_end; k++)
					{
						for (int j = j_start; j < j_end; j++)
						{
							phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j]; // LINHA POR LINHA
						}
					}
				}
			}
		}
	}

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

static int DIM;

void mm(int crow, int ccol,
		int arow, int acol,
		int brow, int bcol,
		double *a, double *b, double *c,
		int l, int m, int n, int bkSize)
{ // 4 deste parametros podem ser removidos

	int lhalf[3], mhalf[3], nhalf[3];
	int i, j, k;
	double *aptr, *bptr, *cptr;

	if (m * n > bkSize)
	{ // neste caso serão sempre o mesmo n e m

		/*

			---------   l[0]
			|   |   |   l[l/2]
			|   |   |   l[l - l/2] ... https://moodle.up.pt/pluginfile.php/173686/mod_resource/content/1/CPD_blockMatrixMult.pdf
			---------   ...... slide 3 ... todas as possiblidades que aparecem no slide


		*/

		lhalf[0] = 0;
		lhalf[1] = l / 2;
		lhalf[2] = l - l / 2;
		mhalf[0] = 0;
		mhalf[1] = m / 2;
		mhalf[2] = m - m / 2;
		nhalf[0] = 0;
		nhalf[1] = n / 2;
		nhalf[2] = n - n / 2;

		for (i = 0; i < 2; i++)
		{
			for (j = 0; j < 2; j++)
			{
				for (k = 0; k < 2; k++)
				{

					mm(crow + lhalf[i], ccol + mhalf[j],
					   arow + lhalf[i], acol + mhalf[k],
					   brow + mhalf[k], bcol + nhalf[j],
					   a, b, c,
					   lhalf[i + 1], mhalf[k + 1], nhalf[j + 1], bkSize);
				}
			}
		}
	}
	else
	{

		for (i = 0; i < l; i++)
		{
			for (j = 0; j < n; j++)
			{
				cptr = c + (crow + i) * DIM + (ccol + j);
				aptr = a + (arow + i) * DIM + (acol);
				bptr = b + (brow)*DIM + (bcol + j);

				for (k = 0; k < m; k++)
				{
					*cptr += *(aptr++) * *bptr;
					bptr += DIM;
				}
			}
		}
	}
}

void OnMultBlock_Recursive(int m_ar, int m_br, int bkSize)
{

	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = clock();

	mm(0, 0, 0, 0, 0, 0, pha, phb, phc, m_ar, m_br, m_br, bkSize);

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

void handle_error(int retval)
{
	printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
	exit(1);
}

void init_papi()
{
	int retval = PAPI_library_init(PAPI_VER_CURRENT);
	if (retval != PAPI_VER_CURRENT && retval < 0)
	{
		printf("PAPI library version mismatch!\n");
		exit(1);
	}
	if (retval < 0)
		handle_error(retval);

	std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
			  << " MINOR: " << PAPI_VERSION_MINOR(retval)
			  << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

void statistics()
{

	int EventSet = PAPI_NULL;
	long long values[2];
	int ret;

	ret = PAPI_library_init(PAPI_VER_CURRENT);
	if (ret != PAPI_VER_CURRENT)
		std::cout << "FAIL" << endl;

	ret = PAPI_create_eventset(&EventSet);
	if (ret != PAPI_OK)
		cout << "ERROR: create eventset" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L1_DCM" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L2_DCM" << endl;

	ofstream file("cpp_stats.txt");

	file << "Execution time for matrix multiplication in C (All times are in seconds)\n";

	file << "\n\nOption 1 (Multiplication) - Size 600x600 to 3000x3000 (+400)\n";
	cout << "\n\nOption 1 (Multiplication) - Size 600x600 to 3000x3000 (+400)\n";

	for (int size = 600; size <= 3000; size += 400)
	{

		START_PAPI

		file << "\t" << size << "x" << size << ":  ";

		clock_t t1 = clock();
		OnMult(size, size);
		clock_t t2 = clock();

		file << "\t" << (double)(t2 - t1) / CLOCKS_PER_SEC;

		STOP_PAPI
	}

	file << "\n\nOption 2 (Line Multiplication) - Size 600x600 to 3000x3000 (+400)\n";
	cout << "\n\nOption 2 (Line Multiplication) - Size 600x600 to 3000x3000 (+400)\n";

	for (int size = 600; size <= 3000; size += 400)
	{

		START_PAPI

		file << "\t" << size << "x" << size << ":  ";

		clock_t t1 = clock();
		OnMultLine(size, size);
		clock_t t2 = clock();

		file << "\t" << (double)(t2 - t1) / CLOCKS_PER_SEC;

		STOP_PAPI
	}

	for (int size = 4096; size <= 10240; size += 2048)
	{

		START_PAPI

		file << "\t" << size << "x" << size << ":  ";

		clock_t t1 = clock();
		OnMultLine(size, size);
		clock_t t2 = clock();

		file << "\t" << (double)(t2 - t1) / CLOCKS_PER_SEC;

		STOP_PAPI
	}

	file << "\n\nOption 3 (Block Multiplication) - Size 4096x4096 to 10240x10240 (+2048) and Block 128 up to 512\n\n";
	cout << "\n\nOption 3 (Block Multiplication) - Size 4096x4096 to 10240x10240 (+2048) and Block 128 up to 512\n\n";

	for (int block = 128; block <= 512; block *= 2)
	{
		file << "\n\tBlock: " << block << "\n\t============================\n";
		for (int size = 4096; size <= 10240; size += 2048)
		{

			START_PAPI

			file << "\t" << size << "x" << size << ":  ";

			clock_t t1 = clock();
			OnMultBlock_Iterative(size, size, block);
			clock_t t2 = clock();

			file << "\t" << (double)(t2 - t1) / CLOCKS_PER_SEC;

			STOP_PAPI
		}
	}

	file << "\n\nOption 4 (Recursive Block Multiplication) - Size 4096x4096 to 10240x10240 (+2048) and Block 128 up to 512\n\n";
	cout << "\n\nOption 4 (Recursive Block Multiplication) - Size 4096x4096 to 10240x10240 (+2048) and Block 128 up to 512\n\n";

	for (int block = 128; block <= 512; block *= 2)
	{
		file << "\n\tBlock: " << block << "\n\t============================\n";
		for (int size = 4096; size <= 10240; size += 2048)
		{
			DIM = size;

			START_PAPI

			file << "\t" << size << "x" << size << ":  ";

			clock_t t1 = clock();
			OnMultBlock_Recursive(size, size, block);
			clock_t t2 = clock();

			file << "\t" << (double)(t2 - t1) / CLOCKS_PER_SEC;

			STOP_PAPI
		}
	}

	ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_destroy_eventset(&EventSet);
	if (ret != PAPI_OK)
		std::cout << "FAIL destroy" << endl;

	file.close();
}

int main(int argc, char *argv[])
{

	if (argc > 1)
	{
		if ((string)argv[1] == "stats")
		{
			cout << "Generating statistics..." << endl;
			statistics();
			cout << "All statistics are done!" << endl;
			return 0;
		}

		cout << "Usage: go run main.go [stats]" << endl;
		return 0;
	}

	char c;
	int lin, col, blockSize;
	int op;

	op = 1;
	do
	{
		cout << endl
			 << "1. Multiplication" << endl;
		cout << "2. Line Multiplication" << endl;
		cout << "3. Block Multiplication" << endl;
		cout << "4. Block Multiplication (Optional Recursive)" << endl;
		cout << ">: ";
		cin >> op;
		if (op == 0)
			break;
		cout << "Dimensions: lins=cols ? ";
		cin >> lin;
		col = lin;
		DIM = lin;

		switch (op)
		{
		case 1:
			OnMult(lin, col);
			break;
		case 2:
			OnMultLine(lin, col);
			break;
		case 3:
			cout << "Block Size? ";
			cin >> blockSize;
			if (blockSize > lin)
				blockSize = lin;
			OnMultBlock_Iterative(lin, col, blockSize);
			break;
		case 4:
			cout << "Block Size? ";
			cin >> blockSize;
			if (blockSize > lin)
				blockSize = lin;
			OnMultBlock_Recursive(lin, col, blockSize);
			break;
		}

	} while (op != 0);
}
