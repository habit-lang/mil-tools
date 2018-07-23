# Tools for MIL, a Monadic Intermediate Language

The Java code in this repository implements a set of tools for
working with MIL programs, including code for generating MIL from
LC or MIL source files, optimizing MIL code, and translating MIL
to LLVM.

A quick demo (this command sequence must run in the top level
directory; we need to do a better job with search paths for
source files!)

    # Build mil-tools:
    ant

    # Use mil-tools to compile fib.lc into LLVM:
    java -jar mil-tools.jar demo/fib.lc -ltmp/fib.ll

    # Compile the generated LLVM code:
    llc -O2 -filetype=asm -march=x86 tmp/fib.ll

    # Build an executable that includes the generated code:
    gcc -m32 -o tmp/fib -Wl,-no_pie demo/main.c tmp/fib.s

    # Run the generated executable:
    tmp/fib

The result should look something like the following, with a
pair of randomly chosen numbers bracketing two 144s, which
have been calculated using two slightly different definitions
of the Fibonnacci function:

    91
    144
    144
    17

