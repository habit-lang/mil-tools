/* ------------------------------------------------------------------------
 * hello.c:  hello, kernel world
 *
 * Mark P. Jones, February 2008
 */

/*-------------------------------------------------------------------------
 * Video RAM:
 */
#define COLUMNS         80
#define LINES           25
#define ATTRIBUTE       12
#define VIDEO           0xB8000

typedef unsigned char single[2];
typedef single        row[COLUMNS];
typedef row           screen[LINES];

static screen* video = (screen*)VIDEO;

/*-------------------------------------------------------------------------
 * Cursor coordinates:
 */
static int xpos = 0;
static int ypos = 0;

/*-------------------------------------------------------------------------
 * Clear the screen.
 */
void cls(void) {
    int i, j;
    for (i=0; i<LINES; ++i) {
      for (j=0; j<COLUMNS; ++j) {
        (*video)[i][j][0] = ' ';
        (*video)[i][j][1] = ATTRIBUTE+j+i;
      }
    }
    ypos = 0;
    xpos = 0;
}

/*-------------------------------------------------------------------------
 * Output a single character.
 */
void putchar(char c) {
    int i, j;

    if (c!='\n' && c!='\r') {
        (*video)[ypos][xpos][0] = c & 0xFF;
        (*video)[ypos][xpos][1] = ATTRIBUTE;
        if (++xpos < COLUMNS) {
            return;
        }
    }

    xpos = 0;               // Perform a newline
    if (++ypos >= LINES) {  // scroll up top lines of screen ... 
        ypos = LINES-1;
        for (i=0; i<ypos; ++i) {
          for (j=0; j<COLUMNS; ++j) {
            (*video)[i][j][0] = (*video)[i+1][j][0];
            (*video)[i][j][1] = (*video)[i+1][j][1];
          }
        }
        for (j=0; j<COLUMNS; ++j) { // fill in new blank line
          (*video)[ypos][j][0] = ' ';
          (*video)[ypos][j][1] = ATTRIBUTE;
        }
    }
}

/*-------------------------------------------------------------------------
 * Output a zero-terminated string.
 */
void puts(char *msg) {
  while (*msg) {
    putchar(*msg++);
  }
}

/*-------------------------------------------------------------------------
 * Main program:
 */
void hello() {
  int i;
  cls();
  for (i=0; i<2; i++) {
    puts("hhhh   hhhh\n");
    puts(" hh    hhh        lll lll\n");
    puts(" hh    hh   eeee  ll  ll   oooo\n");
    puts(" hhhhhhhh  ee  ee ll  ll  oo  oo\n");
    puts(" hh    hh eeeeeee ll  ll oo   oo\n");
    puts(" hh    hh  ee     ll  ll oo  oo\n");
    puts("hhh   hhhh  eeee  ll  ll  oooo\n");
    puts("\n");
    puts("    K e r n e l   W o r l d\n");
    puts("\n on October 3rd\n");
  }
}

/* --------------------------------------------------------------------- */
