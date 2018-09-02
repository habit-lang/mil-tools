#include <stdio.h>

extern int fib(int);
extern int itfib(int);
extern int recfac(int);
extern int itfac(int);

int main(int argc, char** argv) {
  for (int i=0; i<10; i++) {
    printf("%d\t%d\t%d\t%d\t%d\n",
           i, fib(i), itfib(i), recfac(i), itfac(i));
  }
}

