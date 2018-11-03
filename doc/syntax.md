# The Syntax of the MIL and LC languages in mil-tools

### [Rough Draft, November 2018]

This document is an initial attempt to provide a summary of the
syntax for the languages used in mil-tools.

The document is in four parts:

1. Lexical details: descriptions of the notation that is used for
   identifiers, literals, punctuation, comments etc.

2. The core language:  This is a subset of both the MIL and LC
   languages that provides a common syntax for kinds and types, as
   well as the syntax for `data`, `bitdata`, `struct`, `type`, and
   `external` definitions.

3. MIL: This section describes the notation that is used for MIL
   programs (in `.mil` or `.lmil` files).

4. LC: This section describes the notation that is used for LC
   programs (in `.lc` or `.llc` files).

[Note: This document has been written using some of the conventions
of MarkDown notation, but it may or may not be displayed correctly
in a MarkDown viewer.]

## 1 - Lexical details

### Comments:

The syntax for comments is the same as in Haskell:

- Single line comments begin with `--` (but not with a symbol) and
  extend to the end of the line.

- Nesting comments are enclosed between matching pairs of `{-` and
  `-}` markers.

- Literate files are also supported (using `.llc` and `.lmil` file
  suffixes instead of `.lc` and `.mil`, respectively) in which
  code lines are marked with a `>` in the first column and all
  other lines are considered comment lines.  Note that there must
  be at least one blank line between otherwise adjacent groups of
  comment lines and code lines.

### Tokens:

The following are tokens in core/mil/lc:

- Single character tokens:

    `{` `}` `[` `]` `(` `)` `,` `;`

- Natural number literals (`NATLIT`):  A numeric literal consists
  of an optional radix prefix, a sequence of digits (in the
  appropriate radix, defaulting to 10 if no radix was specified),
  and a multiplier suffix.  The prefix may be `0b` or `0B` to
  specify that the rest of the token should be interpreted using
  binary notation; `0o` or `0O` specify the use of octal; and `0x`
  or `0X` specify hexadecimal notation.  The multiplier prefix at
  the end of a numeric literal may be one of the following:

  - `K`, indicating a multiplier of 2^10

  - `M`, indicating a multiplier of 2^20

  - `G`, indicating a multiplier of 2^30

  - `T`, indicating a multiplier of 2^40

  For example, `0x800`, `2K`, and `2048` are different but
  equivalent ways to write the same value as a numeric literal.

- Bit literals (`BITLIT`):  A bit literal starts with a capital 'B',
  'O', or 'X' (specifying binary, octal, and hexadecimal notation,
  respectively) and is followed by a sequence of one or more
  digits in the respective radix/base.  To aid with readability,
  bit literals may also use underscores to separate groups of
  digits, as in `Xff_00`.  The choice of a specific radix and the
  presence or absence of underscores in a bit literals does not
  change the value that is represented.  For example, `X012`,
  `O0022` and `B0000_0001_0010` all represent the same bit vector
  of length 12.

- Character literals (`CHARLIT`) (enclosed between single quotes, as
  in `'c'`) and string literals (`STRLIT`) (enclosed between double
  quotes, as in `"str"`) follow the syntax of Haskell, including the
  following forms of escape sequence:

  - Special characters: `\a` `\b` `\f` `\n` `\r` `\t` `\v` `\\`
  `\"` `\'`

  - ASCII names: `\NUL` `\SOH` `\STX` `\ETX` `\EOT` `\ENQ` `\ACK`
    `\BEL` `\BS` `\HT` `\LF` `\VT` `\FF` `\CR` `\SO` `\SI` `\DLE`
    `\DC1` `\DC2` `\DC3` `\DC4` `\NAK` `\SYN` `\ETB` `\CAN` `\EM`
    `\SUB` `\ESC` `\FS `\GS` `\RS` `\US` `\SP` \DEL`

  - Control characters `\^A` `\^B` `\^C` `\^D` `\^E` `\^F` `\^G`
    `\^H` `\^I` `\^J` `\^K` `\^L` `\^M` `\^N` `\^O` `\^P` `\^Q`
    `\^R` `\^S` `\^T` `\^U` `\^V` `\^W` `\^X` `\^Y` `\^Z` `\^[`
    `\^\` `\^]` `\^^` `\^_`

  - Numeric character escapes, in decimal (`\` followed by a
    sequence of one or more decimal digits); octal escapes (`\o`
    followed by a sequence of one or more octal digits); and
    hexadecimal escapes (`\x` followed by a sequence of one or
    more hexadecimal digits).

  String literals may also include the following elements:

  - The separator, written `\&`, which does not represent any
    characters but can be used to mark the end of an escape
    sequence.

  - Gaps, which comprise a sequence of whitespace characters
    enclosed by backslashes; gaps are commonly used, for example,
    to allow the text of a long string to be split across multiple
    input lines.

- Identifiers, which take one of two forms:

  - Alphanumeric: Following the syntax of Java, every alphanumeric
    identifier starts with a character described by
    `isJavaIdentifierStart`, followed by 0 or more characters
    described by `isJavaIdentifierPart`.  Some identifiers are
    treated as reserved words; these will be written between
    double quotes in the following, as in `"where"`.  Character
    sequences that start with `'B'`, `'O'`, or `'X'` and that
    match the syntax for bit literals will be treated as literals,
    as in `B101`.  The remaining set of identifiers are classified
    as instances of either `CONID` (if they begin with a capital
    letter) or `VARID` (all other identifiers).

  - Symbols: Based on the syntax of Haskell, a symbol is a
    sequence of one or more symbol characters, each of which is
    one of the following:

      - One of the following ASCII characters:
          `!` `#` `$` `%` `&` `*` `+` `.` `/` `<` `=` `>` `?` `@`
          `\` `^` `|` `-` `~`

      - Any character that is included in one of the following
        UNICODE classes: `DASH_PUNCTUATION`, `START_PUNCTUTATION`,
        `END_PUNCTUATION`, `CONNECTOR_PUNCTUATION`,
        `OTHER_PUNCTUATION`, `MATH_SYMBOL`, `CURRENCY_SYMBOL`,
        `MODIFIER_SYMBOL`, `OTHER_SYMBOL`.

    A symbol may optionally end with a `$` character followed
    immediately by an arbitrary identifier.  (This feature is used
    specifically to support the generation of "unique names" for
    specialized versions of operator symbols in the Habit compiler
    pipeline.)

    Some symbols are treated as reserved; these will be written
    between double quotes in the following, such as `"|"`.  The
    remaining symols are classified as either `CONSYM` (if the
    first character is a colon) or as `VARSYM` (all other
    symbols).

### Layout:

Layout information is used, in the same way as it is in Haskell,
as a substitute for explicit use of the `"{"`, `";"` and `"}"`
punctutation that is used to describe lists in program syntax.

Note, however, that the lexer will not insert a `";"` symbol if
the next token is `"then"`, `"else"`, `"of"`, or `"in"`.


## 2 - The core language:

The mil-tools core language provides a common syntax for kinds,
types, type definitions, and external definitions in both MIL and
LC programs.  It is not intended to be used as a standalone
language.

Note that the grammar fragments below follow the notation that was
used in the Habit report.  For example:

* `List(X)` represents a list of one or more items matching `X`;
* `Opt(X)` represents an optional item matching `X`;
* `Sep(X, S)` represents a list with one or more items matching `X`
  with a separator matching `S` between each pair of adjacent items.

### Kind expressions:

Kinds are used to classify type constructors.  Standard nullary
type constructors, for example, have kind `*` (which can also
be written as `type`) but there are also kinds such as `nat`
(representing type-level natural numbers) and function kinds
such as `nat -> *` (representing type constructors).

    Kind          = KindAtom Opt("->" Kind)    -- kind expressions
    KindAtom      = VARID                      -- e.g., nat, lab, ...
                  | VARSYM                     -- e.g., *, ...
                  | "type"                     -- synonym for *
                  | "area"                     -- the area kind ("area" is not a VARID)
                  | "(" Kind ")"               -- parenthesized kinds

### Type expressions:

The syntax of type expressions in the core language is
described by the following grammar:

    Type          = TypeOp Opt("::" Kind)      -- kind annotations
    TypeOp        = TypeAp Opt(Arrow TypeOp)   -- function types
    TypeAp        = Opt(TypeAp) TypeSel        -- type applications
    TypeSel       = TypeAtom List0("." CONID)  -- type selections
    TypeAtom      = VARID                      -- type variables
                  | CONID                      -- type constants
                  | NATLIT                     -- type literals of kind nat
                  | STRLIT                     -- type literals of kind lab
                  | "(" Arrow ")"              -- special syntax for arrow types
                  | "(" Type ")"               -- parenthesized types
    Arrow         = "->"                       -- the function space constructor

### Core definitions:

Core definitions are used to introduce new types (`data`,
`bitdata`, and `struct` definitions), to introduce new names for
existing types (`type` definitions), or to specify the names and
types of values defined outside the current program (`external`
definitions):

    CoreDefn      = DataDefn
                  | BitdataDefn
                  | StructDefn
                  | TypeDefn
                  | ExternalDefn

### Data type definitions:

Algebraic data types are introduced using simple `data` definitions
that specify a type name, zero or more parameters, and zero or more
constructors:

    DataDefn      = "data" CONID Params Opt("=" Sep(ConDefn, "|"))
    Params        = List(TypeAtom)
    ConDefn       = CONID List0(TypeAtom)

The syntax of the parameters for `data` definitions is described
here using the `TypeAtom` nonterminal, which reflects how they are
parsed.  However, subsequent syntactic checks will only allow type
expressions consisting of type variables, kind annotations, and
parentheses.

### Bitdata type definitions:

Bitdata definitions are used to define algebraic datatypes with
a concrete bit-level representation; the syntax used here is
essentially the same as in Habit (without support for deriving).

    BitdataDefn   = "bitdata" CONID Opt(Size) Opt("=" Sep(BitdataCon, "|"))
    Size          = "/" TypeSel
    BitdataCon    = CONID "[" Sep(BitdataRegion, "|") "]"
    BitdataRegion = Sep(BitdataField, ",") "::" Type
                  | NATLIT
                  | BITLIT
    BitdataField  = VARID Opt("=" InfixExpr)

Note that the syntax for specifying a default value (the `Opt`
portion of the definition of `BitdataField`) is only permitted in
LC.

### Structure definitions:

Structure definitions, introduced using the `struct` keyword, are
used to introduce names for type constants of kind `area`, each of
which describes the layout (and, optionally, the alignment) of a
region of memory:

    StructDefn    = "struct" CONID
                       Opt(Size)
                       "[" Sep(StructRegion, "|") "]"
                       Alignment
    StructRegion  = Opt(Sep(StructField, ",") "::") Type
    StructField   = VARID Opt("<-" InfixExpr)
    Alignment     = Opt("aligned" Type)

To be valid, all fields of a structure must be accessible via
appropriately aligned addresses.  To ensure this, the alignment of
the structure must be divisible by the aligment for each of the
fields in the structure (the least common multiple is used as the
default if no explicit alignment is specified).  In addition, the
offset of every field (in bytes, from the start of the structure),
must be divisible by the alignment of that structure.

Note that the syntax for specifying a default initializer (the
`Opt` portion of the definition of `StructField`) is only
permitted in LC.

### Type definitions:

Simple type synonyms are introduced using `type` definitions.
Note that (unlike Habit and Haskell, for example, we do not allow
parameters in `type` definition.  As such, type synonyms are most
useful for introducing one word abbreviations for more complicated
types.

    TypeDefn      = "type" CONID "=" Type               -- Type synonym
                  | "type" CONID Opt(NATLIT) "::" Kind  -- Primitive type

### External definitions:

External definitions introduce names and corresponding types for
values whose definition will be provided elsewhere (in general,
"outside" the current program, hence the term "external").

    ExternalDefn  = "external" Sep(ExternalId, ",") "::" Type
    ExternalId    = Id Opt("{" ExternalRef List(TypeAtom) "}")
    ExternalRef   = Id
                  | NATLIT
                  | STRLIT

The optional portion (in braces) after each identifier name in an
`external definition` is used to provide extra information about
how the definition of the value will be provided, but the details
are not currently well-documented.

### Identifiers and identifier lists:

The nonterminal `Id` represents an identifier, in either alphanumeric
or symbol form.

    Id            = Var
                  | Con
    Var           = VARID
                  | VARSYM
    Con           = CONID
                  | CONSYM
    Ids           = Sep(Id, ",")

## 3 - MIL - a monadic intermediate language:

This section describes the concrete syntax of the MIL programs, as
accepted by the current mil-tools implementation in either `.mil`
or `.lmil` (literate) files.  The mil-tools implementation follows
the formal syntax of MIL fairly closely, but does provide some
syntactic sugar to simplify the task of entering some MIL
definitions.  Each of these extensions, including their
translations into standard MIL code, are described below.

MIL extends the core language with a new form of "tuple type" that
groups a list of zero or more types, written between a pair of
square brackts, into a type expression of kind `tuple`.  There is
also a corresponding function type constructor, written `->>`,
which has kind `tuple -> tuple -> *` and describes first-class
function values (i.e., closures) that map a list of inputs
(specified by the types in the first tuple) to a list of outputs
(specified by the types in the second tuple):

    Type           = ... as in core language ...
                   | TupleType
    TupleType      = "[" Sep0(Type, ",") "]"
    Arrow          = ... as in core language ...
                   | "->>"

MIL also introduces two new forms of types for blocks and closure
definitions (the latter of which is, in fact, shared with data
constructor functions).  Note, however, that these are not
included in the grammar for `Type`, and that blocks and closure
constructors are _not_ first-class values in MIL:

    BlockType      = Type ">>=" Type              -- block type
    AllocType      = "{" Sep0(Type, ",") "}" Type -- allocator type

A MIL program is a sequence of definitions, which can be either
definitions in core or definitions that are specific to MIL.  The
`"{"`, `";"` and `"}"` symbols in the following production are not
actually needed in concrete programs because they can be generated
automatically from the layout of the program:

    MILProgram    = "{" Sep(Opt(MILDefn), ";") "}"
    MILDefn       = CoreDefn
                  | RequireDefn
                  | BlockDefn
                  | ClosureDefn
                  | TopLevelDefn
                  | PrimitiveDefn
                  | TypeAnn
                  | Entrypoint
                  | Export

NOTE: mil-tools allows recursive definitions in MIL code.  In many
cases, this works as expected.  However, recursive definitions
that require the use of a value before it has been properly
initialized are not handled correctly by the current code
generator.  At some point, we intend to add syntactic checks to
detect and reject programs that include definitions like these.

### Require definitions:

A `require` definition is used to specify another `.mil` file
whose content is needed in the current file.  The mil-tools driver
will attempt to read and then process all input `.mil` files in an
order that is consistent with the provided `require` definitions,
but there is no current support for loops (i.e., for files that
`require` themselves, directly or indirectly).

    RequireDefn   = "require" STRLIT                   -- specify a required file
 
### Block definitions:

A block definition provides a name for the block, a list of zero
or more argument names, and a code sequence to be executed when
the block is called with an appropriate list of arguments:

    BlockDefn      = Id "[" Ids0 "]" "=" Code          -- block definitions

The body of each block definition is a code sequence that is
formed from a list of monadic binds and a concluding "terminator"
expression (which is either a generalized tail call, or a `case`
or `if` statement):

    Code           = Lhs "<-" Tail      Opt(";") Code  -- monadic binds
                   | "assert" Atom Cfun Opt(";") Code  -- constructor assertions
                   | Tail                              -- generalized tail calls
                   | "case" Atom "of"                  -- case statements
                        List0(Alt)                     -- ... with alternatives
                        Opt("_" "->" BlockCall)        -- ... and optional default
                   | "if" Atom                         -- if statements
                       "then" BlockCall                -- ... with true
                       "else" BlockCall                -- ... and false branches
    Alt            = Cfun "->" BlockCall
    BlockCall      = Id "[" Atoms0 "]"
    Cfun           = Con Opt("." Id)                   -- C or T.C constructor function

Tail expressions describe single calls or operations that make
up the individual steps in a code sequence.  When a call to a
block appears at the end of a code sequence it can potentially
be implemented as a tail call---that is, as a jump rather than
a call---which is why we sometimes refer to the more general
collection of `Tail` expressions as "generalized tail calls".
However, `Tail` expressions are also used on the right hand
side of monadic binds within code sequences where the values
that they return can be captured and bound to the variables
on the left hand side.  Note that we distinguish between some
of the different forms of call expressions by using different
sets of parentheses.  Among other things, this means that we
can use separate namespaces for blocks, primitives, and data
and closure constructors.

    Tail           = Cfun NATLIT Atom             -- data selectors
                   | Cfun "(" "(" Atoms0 ")" ")"  -- primitive calls (double parens)
                   | Id "("     Atoms0     ")"    -- data allocators (single parens)
                   | Id "["     Atoms0     "]"    -- block calls (brackets)
                   | Id "{"     Atoms0     "}"    -- closure constructors (braces)
                   | Atom @ Atoms                 -- function applications
                   | "return" Atoms               -- monadic returns
    Atoms0         = Sep0(Atom, ",")
    Atoms          = Atom
                   | "[" Atoms0 "]"

Atoms are single token expressions that represent either a
constant or the value bound to an identifier.  The latter could be
the name of a variable introduced in a top level definition; the
name of an argument or stored field on the left hand side of a
block or closure definition; or a temporary name introduced on the
left hand side of a monadic bind.  MIL does not prohibit shadowing
of variable names, but its scoping rules are otherwise completely
unsurprising.

    Atom           = Id       -- variable name
                   | NATLIT   -- numeric literals of type Word
                   | BITLIT   -- numeric literals of type Bit n (for some n)
                   | STRLIT   -- string literals of type Ref String

### Closure definitions:

A closure definition specifies the computation that should be
performed when a closure with a given code pointer, and list of
stored fields, is entered with a given set of arguments.  These
three components correspond to the nonterminals `Id`, `Ids0`,
and `Lhs` in the following production:

    ClosureDefn    = Id "{" Ids0 "}" Lhs "=" Code     -- closure definitions

For convenience, mil-tools accepts an arbitrary code sequence on
the right hand side of a closure definition, even though MIL only
allows a single `Tail`.  In situations where the code sequence is
not a tail, the mil-tools front end will automatically rewrite it
as a call to a new block.  For example, a definition of the form:

    k{x,...} [y,...] = code

with some nontrivial `code` sequence, will be rewritten as

    k{x,...} [y,...] = b[x,...,y,...]

where `b` is a new block defined by:

    b[x,...,y,...] = code

### Top-level definitions:

Top level definitions are used to introduce variable names that
can be used throughout the rest of a program.  The simplest form
of top level definition provides a list of variable names and a
corresponding tail expression whose results will provide the
values for those variables.  (The number of values returned by
the tail must match the number of variables that are bound in the
top level definition.)  There are also special forms of top-level
definition corresponding to Habit-style memory areas and to
string constants that are also stored in memory.  (Although the
preceding syntax for atoms in MIL includes string literals, that
is actually just syntactic sugar for a reference to a top-level
definition for that string.)

    TopLevelDefn   = Lhs "<-" Opt("{" Ids "}") Code
                   | Id "<-" area Type Atom Alignment
                   | Id "<-" STRLIT
    Lhs            = Id
                   | "[" Ids0 "]"
    Ids0           = Sep0(Id, ",")

Strictly speaking, MIL only allows a single `Tail` on the right
hand side of a top level definition, but the syntax shown above
generalizes this to allow an arbitrary code sequence, optionally
prefixed by a list of identifiers in braces.  If these features
are used, then mil-tools will automatically generate a chain of
function closures, with one level for each identifier that appears
between the braces, that ultimately ends with a block that executes
the specified code sequence.  For example, a definition of the
following form:

    bar <- {x,y} code

will automatically be rewritten as the following sequence of
definitions:

    entrypoint bar :: [A] ->> [[B] ->> C]
    bar <- k2{}

    k2     :: {} [A] ->> [[B] ->> C]
    k2{} t0 = k1{t0}

    k1       :: {A} [B] ->> C
    k1{t0} t1 = b0[t0, t1]

    b0       :: [A,B] -> C
    b0[t0,t1] = code

(This assumes that `code` will return a tuple of results specified
by the type `C`, assuming that variables `t0` and `t1` have types
`A` and `B`, respectively.)

### Primitive definitions:

Primitive definitions are used to allow calls from MIL code to
external functions.  The primitive definition specifies the name
of the external function, a "purity label" (which is used to
determine which rewrites can be performed during optimization),
and a block type that specifies the types of the external
functions inputs and results.

    PrimitiveDefn  = "primitive" Var
                        Opt(Purity)
                        Opt("{" "}" Opt("[" "]"))
                        "::" TupleType ">>=" TupleType
    Purity         = VARID

The `Purity` nonterminal here is shown as a general `VARID`, but an
error will be reported if the text of that symbol is not one of the
strings `pure`, `observer`, `volatile`, `impure`, or `doesntReturn`.
The purity label defaults to `impure`, which is the safest choice if
no explicit alternative is provided (or if the appropriate label is
not known).

Calls to primitive functions are written using double parentheses
around the argument lists, as in `add((x,y))`.  If the definition
of a primitive includes the annotation `"{" "}"`, then mil-tools
will automatically generate code for a top-level definition of a
curried function with the same name.  For example:

    -- Primitive definition:
    primitive foo impure {} :: [Word, Word] >>= [Word]

    -- Generated code:
    -- Corresponds to an LC function of type Word -> Word -> Word
    foo :: [Word] ->> [[Word] ->> [Word]]
    foo <- k2{}

    k2 :: {} [Word] ->> [[Word] ->> [Word]]
    k2{} t0 = k1{t0}

    k1 :: {Word} [Word] ->> [Word]
    k1{t0} t1 = foo((t0, t1))

If an additional `"[" "]"` annotation is included, then the generated
code for the top-level variable will include an additional (empty tuple)
argument corresponding to a monadic thunks:

    -- Primitive definition:
    primitive foo impure {} [] :: [Word, Word] >>= [Word]

    -- Generated code:
    -- Corresponds to an LC function of type Word -> Word -> Proc Word
    foo :: [Word] ->> [[Word] ->> [[] ->> [Word]]]
    foo <- k2{}

    k2 :: {} [Word] ->> [[Word] ->> [[] ->> [Word]]]
    k2{} t0 = k1{t0}

    k1 :: {Word} [Word] ->> [[] ->> [Word]]
    k1{t0} t1 = k0{t0, t1}

    k0 :: {Word, Word} [] ->> [Word]
    k0{t0, t1} [] = foo((t0, t1))

The difference between these two code fragments occurs in the definition
of closure `k1`, either invoking the primitive directly (with the `{}`
annotation) or else creating a further closure (with the `{} []` annotation),
which must then be entered (with an `[]` argument list) before the
primitive function is invoked.

### Type annotations:

Type annotations are used to specify explicit types for values defined
in top-level definitions, for blocks, and for closure definitions; the
interpretation of any given type annotation is determined by the form
of the type after the `::` ("has type") symbol: top-level variables,
blocks, and closures all have syntactically distinct forms of type, so
there is no ambiguity in determining which items are referenced if, for
example, a program uses the same name for both a block and a closure
constructor.

    TypeAnn        = Ids "::" Type        -- specify types of top-level variables
                   | Ids "::" BlockType   -- specify types of blocks
                   | Ids "::" AllocType   -- specify type of closure constructor

### Entrypoint and export definitions:

Entrypoint and export definitions are used to identify top-level
definitions, closure definitions, and block definitions (in that
order) that should be made available outside the file in which
those definitions appear.  Items listed as entrypoints will be
visible outside the program (and only definitions that are
reachable from one or more entrypoints will be included in the
final program).  Items listed as exports are not necessarily
available outside the program, but can be accessed by other parts
of the program.  For example, if `a.mil` includes a definition for
`f` and lists it as an `export`, and if `b.mil` includes a
`require "a.mil"` statement, then `f` will also be in scope
(although it can still be shadowed) in `b.mil`.

    EntrypointDefn = "entrypoint" Ids
                   | "export" TypeAnn
    ExportDefn     = "export" Ids
                   | "export" TypeAnn

For convenience, we allow `entrypoint` and `export` to be used as
prefixes to type annotations, translating to a corresponding pair
of definitions.  For example:

    entrypoint f, g :: [Word] >>= [Word]

is interpreted as a shorthand for the following definitions:

    entrypoint f, g
    f, g :: [Word] >>= [Word]

One anomalous detail here is that mil-tools tries to find the
bindings corresponding to identifiers in `entrypoint` and `export`
definitions by looking for top-level variables first, then closure
definitions, and finally block definitions.  As such, if a program
containing either of the two groups of definition above defines
`f` in a top level definition, then mil-tools will treat the
`entrypoint` portion as a reference to the top level definition,
but, because of the form of the type, it will assume that the type
annotation portion refers to a block of the same name.

## 4 - LC - "LambdaCase", a simple functional language

LC is a simple, strongly typed functional language with support
for lambda expressions and case constructs.  In particular, LC
reflects the design of "LambdaCase"---a name that also highlights
these two particular language features---which was first introduced
as an intermediate language in the original implementation of Habit.

Following the same general pattern as MIL, an LC program is a
sequence of definitions that can be either definitions in core or
definitions that are specific to MIL.  In the latter case, there
is a further subdivision in to definitions that can only be used
at the top level (represented by the `TopDefn` nonterminal) and
those that can also be used within local definitions (represented
by the `LCDefn` nonterminal):

    LCProgram     = "{" Sep(Opt(Defn), ";") "}"
    Defn          = CoreDefn
                  | TopDefn
                  | LCDefn

    TopDefn       = RequireDefn
                  | Entrypoint
                  | Export
                  | AreaDefn

    LCDefn        = Equation
                  | TypeAnn

The syntax for `RequireDefn` is exactly the same in MIL and LC.
The only difference between the two languages here is that, in LC,
the string literal may specify not only `.mil` or `.lmil` files,
but also `.lc` or `.llc` files.  (Note, however, that all MIL
files will be loaded before any LC files; it is not currently
possible to use an item defined in LC in a MIL definition.)

On the other hand, although we use the same names as in the
grammar for MIL, there are two reasons why the `Entrypoint`,
`Export`, and `TypeAnn` nonterminals shown here have subtly
different definitions in LC.  The first is because there is only
one form of type annotation definition in LC (i.e., there is no
need for the separate top level, block and closure cases that are
used in MIL).  The second is because LC uses the (unextended) core
grammar for `Type` (e.g., The syntax of types in LC does not
include the `->>` constructor or the tuple types from MIL).

    EntrypointDefn = "entrypoint" Ids
                   | "export" TypeAnn
    ExportDefn     = "export" Ids
                   | "export" TypeAnn
    TypeAnn        = Ids "::" Type

Strongly typed memory areas are defined using a syntax that is
based on the syntax of Habit, except that every area defined in
this way requires an explicit initializer:

    AreaDefn      = "area" Sep(AreaVar, ",")
                      "::" Type
                      Alignment
    AreaVar       = VARID "<-" InfixExpr

Values in LC are defined using equations of the form:

    Equation      = InfixExpr "=" Expr

The grammar here shows an `InfixExpr` on the right hand side,
reflecting the syntax that the mil-tools parser accepts, but
subsequent syntactic checks on the resulting abstract syntax
tree further restrict this to require an application of a
function name to a sequence of zero or more variables.  There
is currently no support for pattern matching, or for using
more than one equation for a single function name.

    Expr          = Opt(Expr "|") TExpr          -- fatbar
    TExpr         = CExpr Opt("::" Type)         -- type annotated expression
    CExpr         = "\" Vars "->" Expr           -- lambda expressions
                  | LetBinding "in" Expr         -- local definitions
                  | "do" Block
                  | IfExpr
                  | CaseExpr
                  | InfixExpr
    InfixExpr     = AExpr Sym  InfixExpr
                  | AExpr "&&" InfixExpr
                  | AExpr "||" InfixExpr
                  | AExpr
    AExpr         = VARID
                  | CONID
                  | CONID "[" BitdataFields "]"  -- bitdata constructors
                  | CONID "[" InitFields "]"     -- structure initializers
                  | NATLIT                       -- literals of type Word
                  | BITLIT                       -- bit vector literals
                  | STRLIT                       -- string literals of type Ref String
                  | "(" Sym ")"                  -- infix operator sections
                  | "(" Expr ")"                 -- parenthesized expressions
                  | AExpr "." VARID              -- bitdata field selectors
                  | AExpr "[" BitdataFields "]"  -- bitdata updates
    BitdataFields = Sep0(VARID "=" CExpr, "|")   -- lists of field names and values
    InitFields    = Sep0(VARID "<-" CExpr, "|")  -- lists of field names and initializers

Note that `"&&"` and `"||"` are considered reserved symbols and
can only be used as infix operators.  These uses are immediately
mapped to the appropriate conditional expressions, ensuring the
usual "lazy" semantics for these operations in which the second
argument is only evaluated if its result is needed to determine
the overall value of the expression:

    e && f  ==>  if e then f else False
    e || f  ==>  if e then True else f

The grammar above includes (at least) one ambiguity, allowing
expressions of the form `CONID []` to be interpreted either as a
bitdata constructor or as a structure intializer.  The current
implementation always assumes the first of these two options.
(The implementation could potentially do a better job here by
using static analysis to determine whether the given `CONID`
refers to a bitdata or structure type.)

The notation for expressions includes syntactic sugar for
monadic code:

    Block         = "{" Stmts "}"

    IfExpr        = "if"      Expr "then" TExpr "else" TExpr
                  | "if" "<-" Expr "then" Block Opt("else" Block)

    IfStmt        = "if" Opt("<-") Expr "then" Block Opt("else" Block)

    CaseExpr      = "case"      Expr "of" ExprAlts
                  | "case" "<-" Expr "of" BlockAlts
    CaseStmt      = "case" Opt("<-") Expr "of" BlockAlts

    ExprAlts      = "{" Sep(Opt(ExprAlt),  ";") "}"
    ExprAlt       = Pat "->" TExpr
    Pat           = Con List(AExpr)        -- AExprs are required to be identifiers

    BlockAlts     = "{" Sep(Opt(BlockAlt), ";") "}"
    BlockAlt      = Pat "->" Block

    Stmts         = ";" Stmts                -- empty statements
                  | LamVar "<-" TExpr ";" Stmts
                  | LetBinding        ";" Stmts
                  | LetBinding "in" Block Opt(";" Stmts)
                  | IfStmt                Opt(";" Stmts)
                  | CaseStmt              Opt(";" Stmts)
                  | TExpr                 Opt(";" Stmts)

    LetBinding    = "let" "{" Sep(Opt(LCDefn), ";") "}"

