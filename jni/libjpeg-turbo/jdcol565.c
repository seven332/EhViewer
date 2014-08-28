/*
 * jdcol565.c
 *
 * This file was part of the Independent JPEG Group's software:
 * Copyright (C) 1991-1997, Thomas G. Lane.
 * Modifications:
 * Copyright (C) 2013, Linaro Limited.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains output colorspace conversion routines.
 */

/* This file is included by jdcolor.c */


#define PACK_SHORT_565(r, g, b)   ((((r) << 8) & 0xf800) |  \
                                   (((g) << 3) & 0x7E0) | ((b) >> 3))
#define PACK_TWO_PIXELS(l, r)     ((r << 16) | l)
#define PACK_NEED_ALIGNMENT(ptr)  (((size_t)(ptr)) & 3)

#define WRITE_TWO_PIXELS(addr, pixels) {  \
  ((INT16*)(addr))[0] = (pixels);  \
  ((INT16*)(addr))[1] = (pixels) >> 16;  \
}
#define WRITE_TWO_ALIGNED_PIXELS(addr, pixels)  ((*(INT32 *)(addr)) = pixels)

#define DITHER_565_R(r, dither)  ((r) + ((dither) & 0xFF))
#define DITHER_565_G(g, dither)  ((g) + (((dither) & 0xFF) >> 1))
#define DITHER_565_B(b, dither)  ((b) + ((dither) & 0xFF))


/* Declarations for ordered dithering
 *
 * We use a 4x4 ordered dither array packed into 32 bits.  This array is
 * sufficent for dithering RGB888 to RGB565.
 */

#define DITHER_MASK       0x3
#define DITHER_ROTATE(x)  (((x) << 24) | (((x) >> 8) & 0x00FFFFFF))
static const INT32 dither_matrix[4] = {
  0x0008020A,
  0x0C040E06,
  0x030B0109,
  0x0F070D05
};


METHODDEF(void)
ycc_rgb565_convert (j_decompress_ptr cinfo,
                    JSAMPIMAGE input_buf, JDIMENSION input_row,
                    JSAMPARRAY output_buf, int num_rows)
{
  my_cconvert_ptr cconvert = (my_cconvert_ptr) cinfo->cconvert;
  register int y, cb, cr;
  register JSAMPROW outptr;
  register JSAMPROW inptr0, inptr1, inptr2;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;
  /* copy these pointers into registers if possible */
  register JSAMPLE * range_limit = cinfo->sample_range_limit;
  register int * Crrtab = cconvert->Cr_r_tab;
  register int * Cbbtab = cconvert->Cb_b_tab;
  register INT32 * Crgtab = cconvert->Cr_g_tab;
  register INT32 * Cbgtab = cconvert->Cb_g_tab;
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int r, g, b;
    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;

    if (PACK_NEED_ALIGNMENT(outptr)) {
      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[y + Crrtab[cr]];
      g = range_limit[y + ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                            SCALEBITS))];
      b = range_limit[y + Cbbtab[cb]];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[y + Crrtab[cr]];
      g = range_limit[y + ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                            SCALEBITS))];
      b = range_limit[y + Cbbtab[cb]];
      rgb = PACK_SHORT_565(r, g, b);

      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[y + Crrtab[cr]];
      g = range_limit[y + ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                            SCALEBITS))];
      b = range_limit[y + Cbbtab[cb]];
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(r, g, b));

      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      y  = GETJSAMPLE(*inptr0);
      cb = GETJSAMPLE(*inptr1);
      cr = GETJSAMPLE(*inptr2);
      r = range_limit[y + Crrtab[cr]];
      g = range_limit[y + ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                            SCALEBITS))];
      b = range_limit[y + Cbbtab[cb]];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
    }
  }
}


METHODDEF(void)
ycc_rgb565D_convert (j_decompress_ptr cinfo,
                     JSAMPIMAGE input_buf, JDIMENSION input_row,
                     JSAMPARRAY output_buf, int num_rows)
{
  my_cconvert_ptr cconvert = (my_cconvert_ptr) cinfo->cconvert;
  register int y, cb, cr;
  register JSAMPROW outptr;
  register JSAMPROW inptr0, inptr1, inptr2;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;
  /* copy these pointers into registers if possible */
  register JSAMPLE * range_limit = cinfo->sample_range_limit;
  register int * Crrtab = cconvert->Cr_r_tab;
  register int * Cbbtab = cconvert->Cb_b_tab;
  register INT32 * Crgtab = cconvert->Cr_g_tab;
  register INT32 * Cbgtab = cconvert->Cb_g_tab;
  INT32 d0 = dither_matrix[cinfo->output_scanline & DITHER_MASK];
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int r, g, b;

    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;
    if (PACK_NEED_ALIGNMENT(outptr)) {
      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[DITHER_565_R(y + Crrtab[cr], d0)];
      g = range_limit[DITHER_565_G(y +
                                   ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                                     SCALEBITS)), d0)];
      b = range_limit[DITHER_565_B(y + Cbbtab[cb], d0)];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[DITHER_565_R(y + Crrtab[cr], d0)];
      g = range_limit[DITHER_565_G(y +
                                   ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                                     SCALEBITS)), d0)];
      b = range_limit[DITHER_565_B(y + Cbbtab[cb], d0)];
      d0 = DITHER_ROTATE(d0);
      rgb = PACK_SHORT_565(r, g, b);

      y  = GETJSAMPLE(*inptr0++);
      cb = GETJSAMPLE(*inptr1++);
      cr = GETJSAMPLE(*inptr2++);
      r = range_limit[DITHER_565_R(y + Crrtab[cr], d0)];
      g = range_limit[DITHER_565_G(y +
                                   ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                                     SCALEBITS)), d0)];
      b = range_limit[DITHER_565_B(y + Cbbtab[cb], d0)];
      d0 = DITHER_ROTATE(d0);
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(r, g, b));

      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      y  = GETJSAMPLE(*inptr0);
      cb = GETJSAMPLE(*inptr1);
      cr = GETJSAMPLE(*inptr2);
      r = range_limit[DITHER_565_R(y + Crrtab[cr], d0)];
      g = range_limit[DITHER_565_G(y +
                                   ((int)RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
                                                     SCALEBITS)), d0)];
      b = range_limit[DITHER_565_B(y + Cbbtab[cb], d0)];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
    }
  }
}


METHODDEF(void)
rgb_rgb565_convert (j_decompress_ptr cinfo,
                    JSAMPIMAGE input_buf, JDIMENSION input_row,
                    JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW outptr;
  register JSAMPROW inptr0, inptr1, inptr2;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int r, g, b;

    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;
    if (PACK_NEED_ALIGNMENT(outptr)) {
      r = GETJSAMPLE(*inptr0++);
      g = GETJSAMPLE(*inptr1++);
      b = GETJSAMPLE(*inptr2++);
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      r = GETJSAMPLE(*inptr0++);
      g = GETJSAMPLE(*inptr1++);
      b = GETJSAMPLE(*inptr2++);
      rgb = PACK_SHORT_565(r, g, b);

      r = GETJSAMPLE(*inptr0++);
      g = GETJSAMPLE(*inptr1++);
      b = GETJSAMPLE(*inptr2++);
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(r, g, b));

      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      r = GETJSAMPLE(*inptr0);
      g = GETJSAMPLE(*inptr1);
      b = GETJSAMPLE(*inptr2);
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
    }
  }
}


METHODDEF(void)
rgb_rgb565D_convert (j_decompress_ptr cinfo,
                     JSAMPIMAGE input_buf, JDIMENSION input_row,
                     JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW outptr;
  register JSAMPROW inptr0, inptr1, inptr2;
  register JDIMENSION col;
  register JSAMPLE * range_limit = cinfo->sample_range_limit;
  JDIMENSION num_cols = cinfo->output_width;
  INT32 d0 = dither_matrix[cinfo->output_scanline & DITHER_MASK];
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int r, g, b;

    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;
    if (PACK_NEED_ALIGNMENT(outptr)) {
      r = range_limit[DITHER_565_R(GETJSAMPLE(*inptr0++), d0)];
      g = range_limit[DITHER_565_G(GETJSAMPLE(*inptr1++), d0)];
      b = range_limit[DITHER_565_B(GETJSAMPLE(*inptr2++), d0)];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      r = range_limit[DITHER_565_R(GETJSAMPLE(*inptr0++), d0)];
      g = range_limit[DITHER_565_G(GETJSAMPLE(*inptr1++), d0)];
      b = range_limit[DITHER_565_B(GETJSAMPLE(*inptr2++), d0)];
      d0 = DITHER_ROTATE(d0);
      rgb = PACK_SHORT_565(r, g, b);

      r = range_limit[DITHER_565_R(GETJSAMPLE(*inptr0++), d0)];
      g = range_limit[DITHER_565_G(GETJSAMPLE(*inptr1++), d0)];
      b = range_limit[DITHER_565_B(GETJSAMPLE(*inptr2++), d0)];
      d0 = DITHER_ROTATE(d0);
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(r, g, b));

      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      r = range_limit[DITHER_565_R(GETJSAMPLE(*inptr0), d0)];
      g = range_limit[DITHER_565_G(GETJSAMPLE(*inptr1), d0)];
      b = range_limit[DITHER_565_B(GETJSAMPLE(*inptr2), d0)];
      rgb = PACK_SHORT_565(r, g, b);
      *(INT16*)outptr = rgb;
    }
  }
}


METHODDEF(void)
gray_rgb565_convert (j_decompress_ptr cinfo,
                     JSAMPIMAGE input_buf, JDIMENSION input_row,
                     JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW inptr, outptr;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int g;

    inptr = input_buf[0][input_row++];
    outptr = *output_buf++;
    if (PACK_NEED_ALIGNMENT(outptr)) {
      g = *inptr++;
      rgb = PACK_SHORT_565(g, g, g);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      g = *inptr++;
      rgb = PACK_SHORT_565(g, g, g);
      g = *inptr++;
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(g, g, g));
      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      g = *inptr;
      rgb = PACK_SHORT_565(g, g, g);
      *(INT16*)outptr = rgb;
    }
  }
}


METHODDEF(void)
gray_rgb565D_convert (j_decompress_ptr cinfo,
                      JSAMPIMAGE input_buf, JDIMENSION input_row,
                      JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW inptr, outptr;
  register JDIMENSION col;
  register JSAMPLE * range_limit = cinfo->sample_range_limit;
  JDIMENSION num_cols = cinfo->output_width;
  INT32 d0 = dither_matrix[cinfo->output_scanline & DITHER_MASK];

  while (--num_rows >= 0) {
    INT32 rgb;
    unsigned int g;

    inptr = input_buf[0][input_row++];
    outptr = *output_buf++;
    if (PACK_NEED_ALIGNMENT(outptr)) {
      g = *inptr++;
      g = range_limit[DITHER_565_R(g, d0)];
      rgb = PACK_SHORT_565(g, g, g);
      *(INT16*)outptr = rgb;
      outptr += 2;
      num_cols--;
    }
    for (col = 0; col < (num_cols >> 1); col++) {
      g = *inptr++;
      g = range_limit[DITHER_565_R(g, d0)];
      rgb = PACK_SHORT_565(g, g, g);
      d0 = DITHER_ROTATE(d0);

      g = *inptr++;
      g = range_limit[DITHER_565_R(g, d0)];
      rgb = PACK_TWO_PIXELS(rgb, PACK_SHORT_565(g, g, g));
      d0 = DITHER_ROTATE(d0);

      WRITE_TWO_ALIGNED_PIXELS(outptr, rgb);
      outptr += 4;
    }
    if (num_cols & 1) {
      g = *inptr;
      g = range_limit[DITHER_565_R(g, d0)];
      rgb = PACK_SHORT_565(g, g, g);
      *(INT16*)outptr = rgb;
    }
  }
}
