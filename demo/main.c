#include <stdio.h>

extern void init(void);

/* For our program entry, we just call the init() function for the
 * generated code.
 */
int main(int argc, char** argv) {
  init();
}

/* Provide an implementation for the printWord primitive.
 */
void printWord(int x) {
  printf("%d\n", x);
}

