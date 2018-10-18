#include <stdio.h>

// A trivial run time library

/* Provide an implementation for the printWord primitive.  */
void printWord(int x) {
  printf("%d\n", x);
}

/* Provide an implementation for the printString primitive.  */
void printString(char* s) {
  puts(s);
}

