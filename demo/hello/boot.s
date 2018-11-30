#-----------------------------------------------------------------------------
# boot.s:  Multiboot startup file
#
# Mark P. Jones, March 2006

#-- Multiboot header: --------------------------------------------------------

        .set    MB_MAGIC,       0x1BADB002
        .set    MB_ALIGN,       1<<0    # Align modules on page boundaries
        .set    MB_MEMMAP,      1<<1    # Request memory map
        .set    MB_FLAGS,       MB_ALIGN|MB_MEMMAP

        .section .multiboot
        .align 4                        # Multiboot header
multiboot_header:
        .long MB_MAGIC                  # multiboot magic number
        .long MB_FLAGS                  # multiboot flags
        .long -(MB_MAGIC + MB_FLAGS)    # checksum

        .globl	mbi                     # cache for multiboot info pointer
mbi:    .long   0
        .globl	mbi_magic               # cache for multiboot magic number
mbi_magic:
        .long   0

#-- Entry point --------------------------------------------------------------

        .text
        .globl	entry
entry:  cli                             # Turn off interrupts
        movl    %eax, mbi_magic         # Save multiboot information
        movl    %ebx, mbi
        leal    stack, %esp             # Set up initial kernel stack
        call    hello
1:      hlt                             # Catch all, in case hello returns
        jmp     1b

        .data                           # Make space for initial stack
        .space  4096
stack:

#-- Done ---------------------------------------------------------------------
