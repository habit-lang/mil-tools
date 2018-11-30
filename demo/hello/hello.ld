OUTPUT_FORMAT(elf32-i386)
ENTRY(entry)

SECTIONS {
  . = 0x100000; /* Load hello at 1MB */
  .text : {
    _text_start = .; *(.multiboot) *(.text) _text_end = .;
    _data_start = .; *(.rodata) *(.data)    _data_end = .;
    _bss_start  = .; *(COMMON) *(.bss)      _bss_end  = .;
  }
}
