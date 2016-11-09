#include "log.h"
#include "com_example_luffy_directundist_CamSurface.h"
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "com_example_luffy_directundist_CamRenderer.h"
#include "gl_tools.h"


cv::Mat _rawFrame;
cv::Mat globalRawBuf;


void processFrame(JNIEnv* env, jbyteArray dataArray, jint width, jint height){
	jbyte *pData = (env)->GetByteArrayElements(dataArray, 0);
	unsigned char* input = (unsigned char*)pData;
	int bufLen = width*height*3/2;
	cv::Mat yuvImg;
    
	yuvImg.create(height*3/2, width, CV_8UC1);

    
	memcpy(yuvImg.data, input, bufLen*sizeof(unsigned char));


	cv::cvtColor(yuvImg, _rawFrame, CV_YUV2RGBA_NV21);


	if( _rawFrame.empty() )
			LOGI("chao: native: rawFrame is empty");
	else{
		_rawFrame.copyTo(globalRawBuf); // this step can avoid crash, need find why, chao
	}
}


JNIEXPORT void JNICALL Java_com_example_luffy_directundist_CamSurface_processFrame
	(JNIEnv * env, jobject, jbyteArray dataArray, jint width, jint height, jint, jboolean, jboolean){

		processFrame(env,   dataArray,  width, height);

}


 const char gVertexShader[] = 
    "attribute vec4 vPosition;\n"
	"attribute vec2 aTexCoord;\n"
	"varying  vec2  vTexCoord;\n"
	"uniform mat4 uMVMatrix;\n"
    "uniform mat4 uPMatrix;\n"
    "void main() {\n"
	"  gl_Position = vPosition;\n"
	"  vTexCoord = aTexCoord;\n"
    "}\n";

  const char gFragmentShader[] = 
    "precision mediump float;\n"
	"varying  vec2  vTexCoord;\n"
	"uniform sampler2D uTexture;\n"
    "void main() {\n"
	"  vec2 xy = vTexCoord - vec2( 1.0/2.0, 1.0/2.0 );\n"
	"  float r2 = dot( xy, xy )  ;\n"
	"  float r4 = r2 * r2  ;\n"
	"  float coeff = -0.4 * r2  + 0.008 * r4  ;\n" 
	"  xy = ( xy + xy * vec2(coeff, coeff ) ) +  vec2( 1.0/2.0, 1.0/2.0 ) ;\n"
	"  gl_FragColor = texture2D(uTexture, xy);\n"
    "}\n";

GLuint gProgram;
GLuint gvPositionHandle;
GLuint textureUniformHandle;
GLuint texCoordUniformHandle;
GLuint texID;

float video_w, video_h;
float rect_w, rect_h;
JNIEXPORT void JNICALL Java_com_example_luffy_directundist_CamRenderer_initDraw
  (JNIEnv *, jobject, jint w, jint h){

	gProgram = createProgram(gVertexShader, gFragmentShader);
    if (!gProgram) {
        LOGE("Could not create program.");
        return;
    }
    gvPositionHandle = glGetAttribLocation(gProgram, "vPosition");
    checkGlError("glGetAttribLocation");
    LOGI("glGetAttribLocation(\"vPosition\") = %d\n",
            gvPositionHandle);
	textureUniformHandle   = glGetUniformLocation(gProgram, "uTexture");
	texCoordUniformHandle  = glGetAttribLocation (gProgram, "aTexCoord");
    glViewport(0, 0, w, h);
    checkGlError("glViewport");

}



float def = 0.0f;

const GLfloat gTriangleVertices[] = { 0.0f, 0.5f, -0.5f, -0.5f,
        0.5f, -0.5f };
JNIEXPORT void JNICALL Java_com_example_luffy_directundist_CamRenderer_drawFrame
	(JNIEnv *, jobject){

	video_w = globalRawBuf.cols;
	video_h = globalRawBuf.rows;

	const GLfloat gObjVertices[] = { -1.0f,  1.0f, 0.0f, 1.0f,
								      1.0f,  1.0f, 0.0f, 1.0f,
									 -1.0f, -1.0f, 0.0f, 1.0f,
									  1.0f, -1.0f, 0.0f, 1.0f  };


	GLfloat gTexCoord[] = {
										   0.0f,    0.0f,
										1.0,    0.0f,
										   0.0f, 1.0,
										1.0, 1.0
	};


	glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    checkGlError("glClear");

    glUseProgram(gProgram);
    checkGlError("glUseProgram");

	glVertexAttribPointer(gvPositionHandle, 4, GL_FLOAT, GL_FALSE, 0, gObjVertices);
    checkGlError("glVertexAttribPointer");
    glEnableVertexAttribArray(gvPositionHandle);
    checkGlError("glEnableVertexAttribArray");

	
	glVertexAttribPointer(texCoordUniformHandle, 2, GL_FLOAT, GL_FALSE, 0, gTexCoord);
	checkGlError("glVertexAttribPointer");
	glEnableVertexAttribArray(texCoordUniformHandle);
    checkGlError("glEnableVertexAttribArray");


	if (texID == 0)
	{
		glGenTextures(1, &texID);
		glBindTexture(GL_TEXTURE_2D, texID);
		checkGlError("glBindTexture");
		glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); 
		checkGlError("glTexParameteri");
		glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); 
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); 
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, video_w, video_h, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
		glBindTexture(GL_TEXTURE_2D, 0);
		checkGlError("glUnBindTexture");
	}
	if(!globalRawBuf.empty()){

		
		glBindTexture(GL_TEXTURE_2D, texID);
		checkGlError("glBindTexture");
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		checkGlError("glPixelStorei");
		if (globalRawBuf.data!=NULL)
		{
			LOGI("texID  = %d", texID);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, video_w, video_h, 0, GL_RGBA, GL_UNSIGNED_BYTE, globalRawBuf.data);
			checkGlError("glTexImage2D");
		}
		
	}


	glUniform1i(textureUniformHandle, 0);
	checkGlError("glUniform1i");
	glViewport(0, 0, 1280, 960);
	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	checkGlError("glDrawArrays");

	glBindTexture(GL_TEXTURE_2D, 0);
	checkGlError("glUnBindTexture");

}