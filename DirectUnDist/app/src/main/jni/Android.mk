LOCAL_PATH := $(call my-dir)
PVRSDKDIR := $(my-dir)/

include $(CLEAR_VARS)
#OPENGLES_LIB:= -lGLESv1_CM
OPENGLES_LIB:= -lGLESv2
OPENGLES_DEF:= -DUSE_OPENGL_ES_1_1
OPENCV_INSTALL_MODULES :=on
OPENCV_LIB_TYPE:=STATIC
include C:\SUN\Android\OpenCV-2.4.10-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_MODULE:= yscl_undist_interface

LOCAL_SRC_FILES:= jni/main.cpp \
                  jni/gl_tools.cpp

LOCAL_LDLIBS +=  $(OPENGLES_LIB) -llog -ldl -lEGL -landroid




OPENCV_INSTALL_MODULES :=on
OPENCV_LIB_TYPE:=STATIC
LOCAL_LDLIBS += -llog
include $(BUILD_SHARED_LIBRARY)

