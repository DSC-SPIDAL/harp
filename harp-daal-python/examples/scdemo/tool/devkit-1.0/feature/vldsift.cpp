extern "C" {
#include "vl/generic.h"
#include "vl/stringop.h"
#include "vl/pgm.h"
#include "vl/dsift.h"
#include "vl/sift.h"
#include "vl/getopt_long.h"
}

#include <errno.h>
#include <cstdlib>
#include <cstdio>
#include <cassert>
#include <iostream>
#include <sstream>
#include <cmath>
#include <string>
#include <cstdio>
#include <cstring>
#include <cstdarg>
#include "vldsift.hpp"
using namespace std;


enum { DEBUG = 0, INFO, WARN, ERROR, FATAL, SIGNAL }; // log levels
int loglevel = INFO;

#define verify(_x)						\
  do {								\
    if (!(_x)) {						\
      LOG(FATAL, "Assertion failure in %s line %d of %s: %s\n", \
	  __FUNCTION__, __LINE__, __FILE__, #_x);		\
    }								\
  } while (0)

void LOG(int level, const char *fmt, ...)
{
  static const char *level_str[] = {
    "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "SIGNAL"
  };
  assert(loglevel >= DEBUG);
  assert(loglevel <= SIGNAL);
  if (level >= loglevel) {
    va_list args;
    fprintf(stderr, "[%s] ", level_str[level]);
    if (level == ERROR) {
      fprintf(stderr, "(errno: %s) ", strerror(errno));
    }
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
  }
  if (level == FATAL) {
    exit(1);
  }
}

char* input_path = NULL;
int verbose = 0;
int step_size = 10;
int cur_scale = 0;
int flag_ascii = 1;

void printUsageAndExit() { 
  cerr << "vldsift [options] input_file" << endl
       << "-s stepsize , default 10" << endl
       << "-b output binary ( default ascii )" << endl
       << "-l current scale ( default 0, image is assumed to have been reduced by 2^l. position is for the original image)" << endl
       << "stdout: vldsift format" << endl;
  exit(1);
}

bool parseArgs(int argc, char** argv) {
  //get rid of program name
  argc--; argv++;

  while(argc > 0)
    {
      if(!strcmp(*argv, "-s")){
	argc--; argv++; 
	if(argc == 0) {
	  fprintf(stderr, "must specify -s\n"); 
	  return false;
	} else {
	  step_size = atoi(*argv);
	}
      }
      if(!strcmp(*argv, "-l")){
	argc--; argv++; 
	if(argc == 0) {
	  fprintf(stderr, "must specify -s\n"); 
	  return false;
	} else {
	  cur_scale = atoi(*argv);
	}
      }
      else if(!strcmp(*argv, "-b")){
	flag_ascii=0;
      }else {
	input_path = *argv;
      }
      argc--; argv++; 
    }
  if( input_path == NULL ) {
    return false;
  }
  return true;
}


int main(int argc, char** argv ) {


  if( !parseArgs(argc, argv) ) {
    printUsageAndExit();
  }

  vl_uint8        *data  = 0 ;
  vl_sift_pix     *fdata = 0 ;
  int err;
  VlPgmImage pim ;
  FILE* in;

  /* open input file */
  if( !strcmp(input_path, "-" )) {
    in = stdin;
  } else {
    in = fopen (input_path, "rb") ;
  }
  if (!in) {
    LOG(FATAL, "cannot open file %s\n", input_path);
  }
    
  /* ...............................................................
   *                                                       Read data
   * ............................................................ */

  /* read PGM header */
  err = vl_pgm_extract_head (in, &pim) ;

  if (err) {
    switch (vl_err_no) {
    case  VL_ERR_PGM_IO :
      /*
	snprintf(err_msg, sizeof(err_msg),  
	"Cannot read from '%s'.", name) ;
      */
      err = VL_ERR_IO ;
      break ;

    case VL_ERR_PGM_INV_HEAD :
      /*
	snprintf(err_msg, sizeof(err_msg),  
	       
      */
      LOG(FATAL, "'%s' contains a malformed PGM header.", input_path);
      err = VL_ERR_IO ;
    }
  }
    
  if (verbose)
    fprintf (stderr, "sift: image is %d by %d pixels\n",
	    pim. width,
	    pim. height) ;
  /* allocate buffer */
  data  = (vl_uint8*)malloc(vl_pgm_get_npixels (&pim) * 
			    vl_pgm_get_bpp       (&pim) * sizeof (vl_uint8)   ) ;
  fdata = (float*)malloc(vl_pgm_get_npixels (&pim) * 
			       vl_pgm_get_bpp       (&pim) * sizeof (float)) ;
    
  if (!data || !fdata) {
    err = VL_ERR_ALLOC ;
    /*
      snprintf(err_msg, sizeof(err_msg), 
      "Could not allocate enough memory.") ;
    */
    LOG(FATAL,	     "Could not allocate enough memory.") ;
  } 
    
  /* read PGM body */
  err  = vl_pgm_extract_data (in, &pim, data) ;

  if (err) {
    /*
      snprintf(err_msg, sizeof(err_msg), "PGM body malformed.") ;
    */
    err = VL_ERR_IO ;
    LOG(FATAL,"PGM body malformed.");
  }

  /* convert data type */
  for (int q = 0 ; q < pim.width * pim.height ; ++q)
    fdata [q] = data [q] ;

  VlDsiftFilter* df =  vl_dsift_new(pim.width, pim.height);
  vl_dsift_set_steps(df, step_size, step_size);
  vl_dsift_process(df, fdata);
  int num_keypoints = vl_dsift_get_keypoint_num(df);
  
  const VlDsiftKeypoint* points = vl_dsift_get_keypoints(df);
  const float*  descriptors = vl_dsift_get_descriptors(df);
  int dim =  vl_dsift_get_descriptor_size(df);


  Points_vldsift mypoints;

  verify(VLDSIFT_DIM==dim);
  
  for( int i = 0 ;i< num_keypoints; i++ ) {
    const VlDsiftKeypoint& p = points[i];
    mypoints.vecs.push_back(Point_vldsift());
    Point_vldsift& q = mypoints.vecs.back();
    q.x = p.x / (float)pim.width;
    q.y = p.y / (float)pim.height;
    q.scale = cur_scale;
    q.norm = p.norm;
    for( int k = 0; k < dim; k++ ) {
      q.desc[k] = descriptors[i*dim+k];
    }
  }

  if( flag_ascii ) {
    cout << mypoints.toASCII() << endl;
  } else {
    string s = mypoints.toBinary();
    cout.write((char*)s.data(),s.length());
  }
  vl_dsift_delete(df);

  return 0;
}

