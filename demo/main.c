#include <stdio.h>

extern void init(void);
extern int fib12;

int main(int argc, char** argv) {
  init();
  printf("fib(12) = %d\n", fib12);
}

