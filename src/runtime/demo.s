# Sample program to demonstrate linkage between bootstrap and
# generated assembler code for cse minijava projects.  HP 2/10
# Modified for x86-64, 11/11, 2/15
#
# This code simulates a MiniJava "main" program and a sample
# factorial function, but does not implement object creation or
# ordinary methods, as would be done in the regular project.

# This version works on linux.  To make it work on Windows or OS X,
# leading underscores need to be added to the external names (_asm_main,
# _put).  However, remember that the final version included in your
# project must work on Allen School Linux machines.

# To run: compile with boot.c then execute/debug/etc.:
#    gcc <whatever options you want> -o demo demo.s boot.c
#    ./demo

    .text
    .globl  asm_main    # label for "main" program

# main function - print 5, then print fact(5)
asm_main:
    pushq   %rbp        # prologue - save frame ptr
    movq    %rsp,%rbp   # no local vars - no additional stack

    movq    $5,%rdi     # System.out.println(5)
    call    put

    movq    $5,%rdi     # System.out.println(fact(5))
    call    fact
    movq    %rax,%rdi
    call    put

    movq    %rbp,%rsp   # epilogue - return
    popq    %rbp        # (could use leave instead of movq/popq)
    ret

# long fact(long n) - return fact(n) for n >= 1
fact:
    pushq   %rbp        # prologue
    movq    %rsp,%rbp
    subq    $16,%rsp    # allocate stack frame for local copy of arg
    movq    %rdi,-8(%rbp)   # save n

    cmpq    $1,%rdi     # compare n to 1
    jg      fact_else   # jump if n > 1
    movq    $1,%rax     # return 1
    jmp     fact_exit
fact_else:
    subq    $1,%rdi     # new argument is n-1
    call    fact        # call fact(n-1) (result in rax)
    movq    -8(%rbp),%rdx   # reload n into rdx
    imulq   %rdx,%rax   # compute product in rax
fact_exit:              # return (result in rax here)
    movq    %rbp,%rsp   # epilogue
    popq    %rbp
    ret
