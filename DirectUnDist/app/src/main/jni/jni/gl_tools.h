#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <GLES2/gl2platform.h>


extern bool setupGraphics(int w, int h);
extern void renderFrame();

extern void printGLInt(const char *name, GLenum s);
extern void printGLString(const char *name, GLenum s);
extern void checkGlError(const char* op);
extern GLuint createProgram(const char* pVertexSource, const char* pFragmentSource);
