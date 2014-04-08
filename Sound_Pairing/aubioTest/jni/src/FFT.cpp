/**********************************************************************

  FFT.cpp

  Dominic Mazzoni

  September 2000

*******************************************************************//*!

\file FFT.cpp
\brief Fast Fourier Transform routines.

  This file contains a few FFT routines, including a real-FFT
  routine that is almost twice as fast as a normal complex FFT,
  and a power spectrum routine when you know you don't care
  about phase information.

  Some of this code was based on a free implementation of an FFT
  by Don Cross, available on the web at:

    http://www.intersrv.com/~dcross/fft.html

  The basic algorithm for his code was based on Numerican Recipes
  in Fortran.  I optimized his code further by reducing array
  accesses, caching the bit reversal table, and eliminating
  float-to-double conversions, and I added the routines to
  calculate a real FFT and a real power spectrum.

*//*******************************************************************/
/*
  Salvo Ventura - November 2006
  Added more window functions:
    * 4: Blackman
    * 5: Blackman-Harris
    * 6: Welch
    * 7: Gaussian(a=2.5)
    * 8: Gaussian(a=3.5)
    * 9: Gaussian(a=4.5)
*/

//#include <wx/intl.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>

#include "FFT.h"
#include "utils.h"

int **gFFTBitTable = NULL;
const int MaxFastBits = 16;

/* Declare Static functions */
static int IsPowerOfTwo(int x);
static int NumberOfBitsNeeded(int PowerOfTwo);
static int ReverseBits(int index, int NumBits);
static void InitFFT();

int IsPowerOfTwo(int x)
{
   if (x < 2)
      return false;

   if (x & (x - 1))             /* Thanks to 'byang' for this cute trick! */
      return false;

   return true;
}

int NumberOfBitsNeeded(int PowerOfTwo)
{
   int i;

   if (PowerOfTwo < 2) {
      fprintf(stderr, "Error: FFT called with size %d\n", PowerOfTwo);
      exit(1);
   }

   for (i = 0;; i++)
      if (PowerOfTwo & (1 << i))
         return i;
}

int ReverseBits(int index, int NumBits)
{
   int i, rev;

   for (i = rev = 0; i < NumBits; i++) {
      rev = (rev << 1) | (index & 1);
      index >>= 1;
   }

   return rev;
}

void InitFFT()
{
   gFFTBitTable = new int *[MaxFastBits];

   int len = 2;
   for (int b = 1; b <= MaxFastBits; b++) {

      gFFTBitTable[b - 1] = new int[len];

      for (int i = 0; i < len; i++)
         gFFTBitTable[b - 1][i] = ReverseBits(i, b);

      len <<= 1;
   }
}

#ifdef EXPERIMENTAL_USE_REALFFTF
#include "RealFFTf.h"
#endif

void DeinitFFT()
{
   if (gFFTBitTable) {
      for (int b = 1; b <= MaxFastBits; b++) {
         delete[] gFFTBitTable[b-1];
      }
      delete[] gFFTBitTable;
   }
#ifdef EXPERIMENTAL_USE_REALFFTF
   // Deallocate any unused RealFFTf tables
   CleanupFFT();
#endif
}

inline int FastReverseBits(int i, int NumBits)
{
   if (NumBits <= MaxFastBits)
      return gFFTBitTable[NumBits - 1][i];
   else
      return ReverseBits(i, NumBits);
}

/*
 * Complex Fast Fourier Transform
 */

void FFT(int NumSamples,
         bool InverseTransform,
         float *RealIn, float *ImagIn, float *RealOut, float *ImagOut)
{
   int NumBits;                 /* Number of bits needed to store indices */
   int i, j, k, n;
   int BlockSize, BlockEnd;

   double angle_numerator = 2.0 * M_PI;
   double tr, ti;                /* temp real, temp imaginary */

   static long lTotal_1 = 0, lTotal_2 = 0, lTotal_3 = 0, lTotal_4 = 0;
   static long lCount2 = 0;
   long lTickCount = getTickCount();

   if (!IsPowerOfTwo(NumSamples)) {
      fprintf(stderr, "%d is not a power of two\n", NumSamples);
      exit(1);
   }

   lTotal_1 += (getTickCount() - lTickCount);
   lTickCount = getTickCount();

   if (!gFFTBitTable)
      InitFFT();

   if (!InverseTransform)
      angle_numerator = -angle_numerator;

   NumBits = NumberOfBitsNeeded(NumSamples);

   lTotal_2 += (getTickCount() - lTickCount);
   lTickCount = getTickCount();
   /*
    **   Do simultaneous data copy and bit-reversal ordering into outputs...
    */

   for (i = 0; i < NumSamples; i++) {
      j = FastReverseBits(i, NumBits);
      RealOut[j] = RealIn[i];
      ImagOut[j] = (ImagIn == NULL) ? 0.0 : ImagIn[i];
   }

   lTotal_3 += (getTickCount() - lTickCount);
   lTickCount = getTickCount();
   /*
    **   Do the FFT itself...
    */

   BlockEnd = 1;
   for (BlockSize = 2; BlockSize <= NumSamples; BlockSize <<= 1) {

      double delta_angle = angle_numerator / (double) BlockSize;

      double sm2 = sin(-2 * delta_angle);
      double sm1 = sin(-delta_angle);
      double cm2 = cos(-2 * delta_angle);
      double cm1 = cos(-delta_angle);
      double w = 2 * cm1;
      double ar0, ar1, ar2, ai0, ai1, ai2;

      for (i = 0; i < NumSamples; i += BlockSize) {
         ar2 = cm2;
         ar1 = cm1;

         ai2 = sm2;
         ai1 = sm1;

         for (j = i, n = 0; n < BlockEnd; j++, n++) {
            ar0 = w * ar1 - ar2;
            ar2 = ar1;
            ar1 = ar0;

            ai0 = w * ai1 - ai2;
            ai2 = ai1;
            ai1 = ai0;

            k = j + BlockEnd;
            tr = ar0 * RealOut[k] - ai0 * ImagOut[k];
            ti = ar0 * ImagOut[k] + ai0 * RealOut[k];

            RealOut[k] = RealOut[j] - tr;
            ImagOut[k] = ImagOut[j] - ti;

            RealOut[j] += tr;
            ImagOut[j] += ti;
         }
      }

      BlockEnd = BlockSize;
   }

   lTotal_4 += (getTickCount() - lTickCount);
   lCount2++;
   //LOGE("FFT(), -------------->[%ld , %ld , %ld , %ld ]\n", (lTotal_1/lCount2), (lTotal_2/lCount2), (lTotal_3/lCount2), (lTotal_4/lCount2));
   /*
      **   Need to normalize if inverse transform...
    */

   if (InverseTransform) {
      float denom = (float) NumSamples;

      for (i = 0; i < NumSamples; i++) {
         RealOut[i] /= denom;
         ImagOut[i] /= denom;
      }
   }
}

/*
 * Real Fast Fourier Transform
 *
 * This function was based on the code in Numerical Recipes in C.
 * In Num. Rec., the inner loop is based on a single 1-based array
 * of interleaved real and imaginary numbers.  Because we have two
 * separate zero-based arrays, our indices are quite different.
 * Here is the correspondence between Num. Rec. indices and our indices:
 *
 * i1  <->  real[i]
 * i2  <->  imag[i]
 * i3  <->  real[n/2-i]
 * i4  <->  imag[n/2-i]
 */

void RealFFT(int NumSamples, float *RealIn, float *RealOut, float *ImagOut)
{
#ifdef EXPERIMENTAL_USE_REALFFTF
   // Remap to RealFFTf() function
   int i;
   HFFT hFFT = GetFFT(NumSamples);
   float *pFFT = new float[NumSamples];
   // Copy the data into the processing buffer
   for(i=0; i<NumSamples; i++)
      pFFT[i] = RealIn[i];

   // Perform the FFT
   RealFFTf(pFFT, hFFT);

   // Copy the data into the real and imaginary outputs
   for(i=1;i<(NumSamples/2);i++) {
      RealOut[i]=pFFT[hFFT->BitReversed[i]  ];
      ImagOut[i]=pFFT[hFFT->BitReversed[i]+1];
   }
   // Handle the (real-only) DC and Fs/2 bins
   RealOut[0] = pFFT[0];
   RealOut[i] = pFFT[1];
   ImagOut[0] = ImagOut[i] = 0;
   // Fill in the upper half using symmetry properties
   for(i++ ; i<NumSamples; i++) {
      RealOut[i] =  RealOut[NumSamples-i];
      ImagOut[i] = -ImagOut[NumSamples-i];
   }
   delete [] pFFT;
   ReleaseFFT(hFFT);

#else

   int Half = NumSamples / 2;
   int i;

   float theta = M_PI / Half;

   float *tmpReal = new float[Half];
   float *tmpImag = new float[Half];

   for (i = 0; i < Half; i++) {
      tmpReal[i] = RealIn[2 * i];
      tmpImag[i] = RealIn[2 * i + 1];
   }

   FFT(Half, 0, tmpReal, tmpImag, RealOut, ImagOut);

   float wtemp = float (sin(0.5 * theta));

   float wpr = -2.0 * wtemp * wtemp;
   float wpi = -1.0 * float (sin(theta));
   float wr = 1.0 + wpr;
   float wi = wpi;

   int i3;

   float h1r, h1i, h2r, h2i;

   for (i = 1; i < Half / 2; i++) {

      i3 = Half - i;

      h1r = 0.5 * (RealOut[i] + RealOut[i3]);
      h1i = 0.5 * (ImagOut[i] - ImagOut[i3]);
      h2r = 0.5 * (ImagOut[i] + ImagOut[i3]);
      h2i = -0.5 * (RealOut[i] - RealOut[i3]);

      RealOut[i] = h1r + wr * h2r - wi * h2i;
      ImagOut[i] = h1i + wr * h2i + wi * h2r;
      RealOut[i3] = h1r - wr * h2r + wi * h2i;
      ImagOut[i3] = -h1i + wr * h2i + wi * h2r;

      wr = (wtemp = wr) * wpr - wi * wpi + wr;
      wi = wi * wpr + wtemp * wpi + wi;
   }

   RealOut[0] = (h1r = RealOut[0]) + ImagOut[0];
   ImagOut[0] = h1r - ImagOut[0];

   delete[]tmpReal;
   delete[]tmpImag;
#endif //EXPERIMENTAL_USE_REALFFTF
}

#ifdef EXPERIMENTAL_USE_REALFFTF
/*
 * InverseRealFFT
 *
 * This function computes the inverse of RealFFT, above.
 * The RealIn and ImagIn is assumed to be conjugate-symmetric
 * and as a result the output is purely real.
 * Only the first half of RealIn and ImagIn are used due to this
 * symmetry assumption.
 */
void InverseRealFFT(int NumSamples, float *RealIn, float *ImagIn, float *RealOut)
{
   // Remap to RealFFTf() function
   int i;
   HFFT hFFT = GetFFT(NumSamples);
   float *pFFT = new float[NumSamples];
   // Copy the data into the processing buffer
   for(i=0; i<(NumSamples/2); i++)
      pFFT[2*i  ] = RealIn[i];
   if(ImagIn == NULL) {
      for(i=0; i<(NumSamples/2); i++)
         pFFT[2*i+1] = 0;
   } else {
      for(i=0; i<(NumSamples/2); i++)
         pFFT[2*i+1] = ImagIn[i];
   }
   // Put the fs/2 component in the imaginary part of the DC bin
   pFFT[1] = RealIn[i];

   // Perform the FFT
   InverseRealFFTf(pFFT, hFFT);

   // Copy the data to the (purely real) output buffer
   ReorderToTime(hFFT, pFFT, RealOut);

   delete [] pFFT;
   ReleaseFFT(hFFT);
}
#endif // EXPERIMENTAL_USE_REALFFTF

/*
 * PowerSpectrum
 *
 * This function computes the same as RealFFT, above, but
 * adds the squares of the real and imaginary part of each
 * coefficient, extracting the power and throwing away the
 * phase.
 *
 * For speed, it does not call RealFFT, but duplicates some
 * of its code.
 */

#include "fftw3.h"

void PowerSpectrum(int NumSamples, float *In, float *Out)
{
#ifdef EXPERIMENTAL_USE_REALFFTF
   // Remap to RealFFTf() function
   int i;
   HFFT hFFT = GetFFT(NumSamples);
   float *pFFT = new float[NumSamples];
   // Copy the data into the processing buffer
   for(i=0; i<NumSamples; i++)
      pFFT[i] = In[i];

   // Perform the FFT
   RealFFTf(pFFT, hFFT);

   // Copy the data into the real and imaginary outputs
   for(i=1;i<NumSamples/2;i++) {
      Out[i]= (pFFT[hFFT->BitReversed[i]  ]*pFFT[hFFT->BitReversed[i]  ])
         + (pFFT[hFFT->BitReversed[i]+1]*pFFT[hFFT->BitReversed[i]+1]);
   }
   // Handle the (real-only) DC and Fs/2 bins
   Out[0] = pFFT[0]*pFFT[0];
   Out[i] = pFFT[1]*pFFT[1];
   delete [] pFFT;
   ReleaseFFT(hFFT);

#else // EXPERIMENTAL_USE_REALFFTF

   long lTickCount = getTickCount();

   int Half = NumSamples / 2;
   int i;

   float theta = M_PI / Half;

   static float *tmpReal = new float[Half];
   static float *tmpImag = new float[Half];
   static float *RealOut = new float[Half];
   static float *ImagOut = new float[Half];
   static long lTotal1 = 0, lTotal2 = 0, lTotal3 = 0, lTotal4 = 0;
   static long lCount = 0;

   lTotal1 += (getTickCount() - lTickCount);
   //LOGE("PowerSpectrum(), 1 takes %ld ms\n", (getTickCount() - lTickCount));
   lTickCount = getTickCount();

   for (i = 0; i < Half; i++) {
      tmpReal[i] = In[2 * i];
      tmpImag[i] = In[2 * i + 1];
   }

   lTotal2 += (getTickCount() - lTickCount);
   //LOGE("PowerSpectrum(), 2 takes %ld ms\n", (getTickCount() - lTickCount));
   lTickCount = getTickCount();

   FFT(Half, 0, tmpReal, tmpImag, RealOut, ImagOut);

   lTotal3 += (getTickCount() - lTickCount);
   //LOGE("PowerSpectrum(), 3 takes %ld ms\n", (getTickCount() - lTickCount));
   lTickCount = getTickCount();

   float wtemp = float (sin(0.5 * theta));

   float wpr = -2.0 * wtemp * wtemp;
   float wpi = -1.0 * float (sin(theta));
   float wr = 1.0 + wpr;
   float wi = wpi;

   int i3;

   float h1r, h1i, h2r, h2i, rt, it;

   lTickCount = getTickCount();

   for (i = 1; i < Half / 2; i++) {

      i3 = Half - i;

      h1r = 0.5 * (RealOut[i] + RealOut[i3]);
      h1i = 0.5 * (ImagOut[i] - ImagOut[i3]);
      h2r = 0.5 * (ImagOut[i] + ImagOut[i3]);
      h2i = -0.5 * (RealOut[i] - RealOut[i3]);

      rt = h1r + wr * h2r - wi * h2i;
      it = h1i + wr * h2i + wi * h2r;

      Out[i] = rt * rt + it * it;

      rt = h1r - wr * h2r + wi * h2i;
      it = -h1i + wr * h2i + wi * h2r;

      Out[i3] = rt * rt + it * it;

      wr = (wtemp = wr) * wpr - wi * wpi + wr;
      wi = wi * wpr + wtemp * wpi + wi;
   }

   lTotal4 += (getTickCount() - lTickCount);
   lCount++;
   //LOGE("PowerSpectrum(), [%ld , %ld , %ld , %ld ]\n", (lTotal1/lCount), (lTotal2/lCount), (lTotal3/lCount), (lTotal4/lCount));

   rt = (h1r = RealOut[0]) + ImagOut[0];
   it = h1r - ImagOut[0];
   Out[0] = rt * rt + it * it;

   lTickCount = getTickCount();

   rt = RealOut[Half / 2];
   it = ImagOut[Half / 2];
   Out[Half / 2] = rt * rt + it * it;

   lTickCount = getTickCount();

//   delete[]tmpReal;
//   delete[]tmpImag;
//   delete[]RealOut;
//   delete[]ImagOut;

//   static fftwf_plan plan_forward;
//   static float *tmpOut = NULL;
//   static int s_init = 0;
//   if(!s_init){
//
//	   tmpOut = (float *)malloc(NumSamples* sizeof(float));
//	   plan_forward = fftwf_plan_r2r_1d ( NumSamples, In, tmpOut, FFTW_R2HC, FFTW_ESTIMATE);
//	   s_init = 1;
//   }
//
//   long lTickCount = getTickCount();
//
//   int Half = NumSamples / 2;
//   int i;
//
//   float theta = M_PI / Half;
//
////   float *tmpReal = new float[Half];
////   float *tmpImag = new float[Half];
////   float *RealOut = new float[Half];
////   float *ImagOut = new float[Half];
//
//   LOGE("PowerSpectrum(), 1 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
////   for (i = 0; i < Half; i++) {
////      tmpReal[i] = In[2 * i];
////      tmpImag[i] = In[2 * i + 1];
////   }
//
//   LOGE("PowerSpectrum(), 2 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
////   FFT(Half, 0, tmpReal, tmpImag, RealOut, ImagOut);
//   fftwf_execute ( plan_forward );
//
//   LOGE("PowerSpectrum(), 3 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
//   float wtemp = float (sin(0.5 * theta));
//
//   float wpr = -2.0 * wtemp * wtemp;
//   float wpi = -1.0 * float (sin(theta));
//   float wr = 1.0 + wpr;
//   float wi = wpi;
//
//   int i3;
//
//   float h1r, h1i, h2r, h2i, rt, it;
//
//   LOGE("PowerSpectrum(), 4 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
//   for (i = 1; i < Half / 2; i++) {
//
//      i3 = Half - i;
//
//      h1r = 0.5 * (tmpOut[2*i] + tmpOut[2*i3]);
//      h1i = 0.5 * (tmpOut[2*i+1] - tmpOut[2*i3-1]);
//      h2r = 0.5 * (tmpOut[2*i+1] + tmpOut[2*i3-1]);
//      h2i = -0.5 * (tmpOut[2*i] - tmpOut[2*i3]);
//
//      rt = h1r + wr * h2r - wi * h2i;
//      it = h1i + wr * h2i + wi * h2r;
//
//      Out[i] = rt * rt + it * it;
//
//      rt = h1r - wr * h2r + wi * h2i;
//      it = -h1i + wr * h2i + wi * h2r;
//
//      Out[i3] = rt * rt + it * it;
//
//      wr = (wtemp = wr) * wpr - wi * wpi + wr;
//      wi = wi * wpr + wtemp * wpi + wi;
//   }
//
//   LOGE("PowerSpectrum(), 5 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
//   rt = (h1r = Out[0]) + Out[1];
//   it = h1r - Out[1];
//   Out[0] = rt * rt + it * it;
//
//   LOGE("PowerSpectrum(), 6 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
//   rt = Out[Half];
//   it = Out[Half / 2 +1];
//   Out[Half / 2] = rt * rt + it * it;
//
//   LOGE("PowerSpectrum(), 7 takes %ld ms\n", (getTickCount() - lTickCount));
//   lTickCount = getTickCount();
//
////   delete[]tmpReal;
////   delete[]tmpImag;
////   delete[]RealOut;
////   delete[]ImagOut;
#endif // EXPERIMENTAL_USE_REALFFTF
}

/*
 * Windowing Functions
 */

int NumWindowFuncs()
{
   return 10;
}

//const wxChar *WindowFuncName(int whichFunction)
//{
//   switch (whichFunction) {
//   default:
//   case 0:
//      return _("Rectangular");
//   case 1:
//      return wxT("Bartlett");
//   case 2:
//      return wxT("Hamming");
//   case 3:
//      return wxT("Hanning");
//   case 4:
//      return wxT("Blackman");
//   case 5:
//      return wxT("Blackman-Harris");
//   case 6:
//      return wxT("Welch");
//   case 7:
//      return wxT("Gaussian(a=2.5)");
//   case 8:
//      return wxT("Gaussian(a=3.5)");
//   case 9:
//      return wxT("Gaussian(a=4.5)");
//   }
//}

void WindowFunc(int whichFunction, int NumSamples, float *in)
{
   int i;
   double A;

   switch( whichFunction )
   {
   case 1:
      // Bartlett (triangular) window
      for (i = 0; i < NumSamples / 2; i++) {
         in[i] *= (i / (float) (NumSamples / 2));
         in[i + (NumSamples / 2)] *=
             (1.0 - (i / (float) (NumSamples / 2)));
      }
      break;
   case 2:
      // Hamming
      for (i = 0; i < NumSamples; i++)
         in[i] *= 0.54 - 0.46 * cos(2 * M_PI * i / (NumSamples - 1));
      break;
   case 3:
      // Hanning
      for (i = 0; i < NumSamples; i++)
         in[i] *= 0.50 - 0.50 * cos(2 * M_PI * i / (NumSamples - 1));
      break;
   case 4:
      // Blackman
      for (i = 0; i < NumSamples; i++) {
          in[i] *= 0.42 - 0.5 * cos (2 * M_PI * i / (NumSamples - 1)) + 0.08 * cos (4 * M_PI * i / (NumSamples - 1));
      }
      break;
   case 5:
      // Blackman-Harris
      for (i = 0; i < NumSamples; i++) {
          in[i] *= 0.35875 - 0.48829 * cos(2 * M_PI * i /(NumSamples-1)) + 0.14128 * cos(4 * M_PI * i/(NumSamples-1)) - 0.01168 * cos(6 * M_PI * i/(NumSamples-1));
      }
      break;
   case 6:
      // Welch
      for (i = 0; i < NumSamples; i++) {
          in[i] *= 4*i/(float)NumSamples*(1-(i/(float)NumSamples));
      }
      break;
   case 7:
      // Gaussian (a=2.5)
      // Precalculate some values, and simplify the fmla to try and reduce overhead
      A=-2*2.5*2.5;

      for (i = 0; i < NumSamples; i++) {
          // full
          // in[i] *= exp(-0.5*(A*((i-NumSamples/2)/NumSamples/2))*(A*((i-NumSamples/2)/NumSamples/2)));
          // reduced
          in[i] *= exp(A*(0.25 + ((i/(float)NumSamples)*(i/(float)NumSamples)) - (i/(float)NumSamples)));
      }
      break;
   case 8:
      // Gaussian (a=3.5)
      A=-2*3.5*3.5;
      for (i = 0; i < NumSamples; i++) {
          // reduced
          in[i] *= exp(A*(0.25 + ((i/(float)NumSamples)*(i/(float)NumSamples)) - (i/(float)NumSamples)));
      }
      break;
   case 9:
      // Gaussian (a=4.5)
      A=-2*4.5*4.5;

      for (i = 0; i < NumSamples; i++) {
          // reduced
          in[i] *= exp(A*(0.25 + ((i/(float)NumSamples)*(i/(float)NumSamples)) - (i/(float)NumSamples)));
      }
      break;
   default:
      fprintf(stderr,"FFT::WindowFunc - Invalid window function: %d\n",whichFunction);
   }
}

// Indentation settings for Vim and Emacs and unique identifier for Arch, a
// version control system. Please do not modify past this point.
//
// Local Variables:
// c-basic-offset: 3
// indent-tabs-mode: nil
// End:
//
// vim: et sts=3 sw=3
// arch-tag: 47691958-d393-488c-abc5-81178ea2686e

