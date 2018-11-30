------------------------------------------------------------
This folder contains the source code for a simple "hello"
demo program that can boot and run on a bare metal PC, or in
a virtual machine using tools like QEMU, VirtualBox, etc.
This code was developed for and used as part of the courses
on "Languages and Low-level Programming" at Portland State
University in 2015, 2016, and 2017.

------------------------------------------------------------
The original version of this program was written in C and is
included in the file "hello.c".  This folder also contains a
second version, "hello.llc", that is written in LC, which is
an intermediate language for the Habit compiler; we hope that
this will be a good representative of the kind of program and
output that can be obtained when the Habit compiler is used
directly.  The LC version is also supported by some library
files:

- "vram.llc" is a simple library for handling video RAM
  output.

- "string.lc" and "string.mil" are the LC and MIL components
  of a simple library for supporting String cursors, which
  provides a type safe way to iterate through
  null-terminated strings

- "lib.llc" is a small library of "general purpose"
  functions that might reasonably be provided as part of the
  standard prelude or libraries in a more fully developed
  system.

The other files in this folder are as follows:

- "Makefile" provides the rules for building and running the
  demo program using the standard "make" tool.

- "boot.s" is a small fragment of assembly code that adds
  the headers that are needed to boot via Grub, and that
  reserves and configures a small stack for the main
  program.

- "grub.cfg" is a simple configuration file that provides
  settings for booting the main program via grub.

- "hello.ld" is a loader script that describes how the
  various object code files that are produced during the build
  process should be combined into a single executable file,
  including the specification of details such as the
  physical address where the program should be loaded into
  memory.

- "screenshot.png" is a screenshot that shows the results
  of running the program in QEMU.

- "README.txt" is this file!

------------------------------------------------------------
This code has been tested in several settings, most recently
using QEMU inside a Ubuntu 18.04.1 Linux virtual machine
that is hosted, via VirtualBox, on a macOS based computer.
The following packages were installed in order to build and
run this demo:

    gcc make perl clang llvm qemu xorriso libc6-x32 parted
    dafault-jdk git ant

You will also need to download mil-tools from:

    https://github.com/habit-lang/mil-tools

and then use the instructions provided there to build and
install the milc compiler.

Of course, it is possible that some of these may not be
required, or that other packages not listed here will be
required, depending on the starting configuration of the
host machine.

Be prepared for the fact that, depending on your level of
experience, building and configuring a virtual machine to
build and run this demo may be a non-trivial task.  At some
point, we may be able to share a pre-configured virtual
machine image for download (the catch being that this is a
very large file) or else we will provide more detailed
instructions for building a suitable virtual machine.  On
the other hand, if you already have a suitably configured
Linux machine, virtual or real, then you may be able to
run this demo with relatively little additional work.

------------------------------------------------------------
As one quick point of comparison, the code sizes (in
bytes) for the two versions of hello were as follows on
the specific virtual machine that was used for testing:

For the LC version: 10600 (11316 before stripping)
For the C version:  10072 (12884 before stripping)

Note that, in each case, these figures include 4096 bytes
of space that is reserved for the stack.  A preliminary
inspection of the generated intermediate code for the
LC version shows evidence of aggressive inlining, and
some resulting duplication of code, and it may be possible
to reduce this by adding noinline annotations, or by
switching to a program with less statically known data.

------------------------------------------------------------
