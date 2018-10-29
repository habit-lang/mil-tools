#include <stdio.h>

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

extern void initPrioset();
extern void clearPrioset();
extern void insertPriority(word);
extern void removePriority(word);
extern word highestPriority1();

void add(word val) {
  printf("Inserting " WORD "\n", val);
  insertPriority(val);
}

void rem(word val) {
  printf("Removing " WORD "\n", val);
  removePriority(val);
}

int main(int argc, char** argv) {
  printf("Priority set demo\n");
  initPrioset();

  printf("Inserting some numbers:\n");
  add(12);
  add(5);
  add(7);
  add(128);
  add(67);

  printf("Removing three numbers:\n");
  for (int i=0; i<3; i++) {
    rem(highestPriority1());
  }

  printf("Adding some more numbers:\n");
  add(3);
  add(32);
  add(10);

  printf("Draining the queue:\n");
  word p;
  while ((p=highestPriority1())>=0) {
    rem(p);
  }
  printf("Done!\n");
}

