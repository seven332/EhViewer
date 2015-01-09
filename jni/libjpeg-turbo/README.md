# simd/jsimdext.inc

%define EXTN(name)   _ %+ name
to
%define EXTN(name)   name
or ld error for x86

# Add cmyk to rgb, cmyk to gray

In jdcolor.c, jdcolext.c
