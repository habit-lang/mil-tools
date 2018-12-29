#include <stdio.h>
#include <stdlib.h>
  
#ifdef WORD32
typedef int  word;
#define WORD "%d"
#else
#ifndef WORD64
#error "Please specify either -DWORD32 or -DWORD64 as a command line option"
#endif
typedef long word;
#define WORD "%ld"
#endif

void* alloc(word size) {
//printf("Allocating object of size " WORD "\n", size);
  return calloc(size, sizeof(word));
}
