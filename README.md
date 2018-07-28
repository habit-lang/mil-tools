# Tools for MIL, a Monadic Intermediate Language

The Java code in this repository implements a set of tools for
working with MIL programs, including code for generating MIL from
LC or MIL source files, optimizing MIL code, and translating MIL
to LLVM.

These tools are primarily intended for use as a backend for
the alb compiler for Habit.  However, they can also be used
independently, as suggested by the following quick demo.
(Note that this command sequence must run in the top level
directory; we need to do a better job with search paths for
source files in future releases!)

First, we need to build the mil-tools, and the simplest way to
do this (assuming you already have a suitable JDK and copy of
Apache Ant installed) is as follows:

    # Build mil-tools:
    ant

The process for compiling an LC source file (in this case,
`demo/fib.lc`) to MIL and then generating LLVM is illustrated by
the following commands (this time assuming that you have a
suitable installation of LLVM):

    # Use mil-tools to compile fib.lc into LLVM:
    java -jar mil-tools.jar demo/fib.lc -ltmp/fib.ll

    # Build an executable from the generated LLVM code:
    clang -o tmp/fib tmp/fib.ll demo/runtime.c

Depending on details of your LLVM installation/platform (for
example, on macOS), you may need to substitute `gcc` for `clang`
in the second command here.

Other methods for compiling the generated LLVM code may be useful
in other settings.  For example, you can use the following two
commands to compile `fib.ll` via assembly with a specific target
architecture (in this case, 32-bit x86):

    # Compile the generated LLVM code:
    llc -O2 -filetype=asm -march=x86 tmp/fib.ll

    # Build an executable that includes the generated code:
    gcc -m32 -o tmp/fib -Wl,-no_pie demo/runtime.c tmp/fib.s

You may see a message of the form `warning: overriding the module
target triple with ...` when you run the commands above; we will
need to improve the LLVM code generator to avoid this in a future
release, but you should be able to ignore this message for now.

Finally, you can run the compiled program:

    # Run the generated executable:
    tmp/fib

The result should look something like the following, with a
pair of arbitrarily chosen numbers bracketing two 144s, which
have been calculated using two slightly different definitions
of the Fibonnacci function:

    91
    144
    144
    17

Alternatively, you can use the following command line to run
the program directly through the bytecode interpreter:

    java -jar mil-tools.jar -x demo/fib.lc

Replace `-x` with `-m` to see the generated MIL code, or
with `-l` to see the generated LLVM code, etc.  (Or just
run `java -jar mil-tools.jar` for a summary of additional
command line options.)


