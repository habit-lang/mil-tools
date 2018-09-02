#include <stdio.h>

extern void initialize();
extern int fib12, fib15;

int main(int argc, char** argv) {
  initialize();
  printf("fib(12)=%d, fib(15)=%d\n", fib12, fib15);
}

