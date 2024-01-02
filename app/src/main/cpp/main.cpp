//modified main code to return an object array of the results
//instead of printing them to console

#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include "TestConfigTemplate.h"
#include <csignal>
#include <jni.h>
#include <string>


extern "C"
JNIEXPORT jlong JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getServerInfo(JNIEnv *env, jobject thiz) {
    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo ipInfo;
    ServerInfo serverInfo;

    //for setting the text
    jclass resultsClazz = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID updateProgress = env->GetMethodID(resultsClazz, "updateProgress", "(Ljava/lang/String;I)V");

    sp.setInsecure(true);

    if (!sp.ipInfo(ipInfo)){
        //TODO: figure out how to throw an error without making app crash
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("IP info could not be obtained"), 0);
    }else {
        auto serverList = sp.serverList();
        if (serverList.empty()) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Server list could not be obtained"), 0);
        } else {
            serverInfo = sp.bestServer(10, [](bool success) {});
        }
    }

    //allocates the server info to the heap to be accessed later
    //we're returning the memory address of a copy of the stored server info to java as a long
    ServerInfo *serverCpy = (ServerInfo *)malloc(sizeof(ServerInfo));
    memcpy(serverCpy, &serverInfo, sizeof(ServerInfo));
    unsigned long long serverPtr = (unsigned long long)serverCpy;

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Connected to server"), 10);

    return serverPtr;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeDlspeed(JNIEnv *env, jobject thiz, jlong serverPtr) {
    double dlspeed = -1;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *(ServerInfo *)serverPtr;

    //for setting the text
    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;I)V");

    if(sp.setServer(serverInfo)) {
        double preSpeed = 20.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        //get upload and download speed
        if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute download speed"), 0);
        }
    }

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Download speed computed"), 30);

    return dlspeed;
}


extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeUlspeed(JNIEnv *env, jobject thiz, jlong serverPtr) {
    double ulspeed = 0;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *(ServerInfo *)serverPtr;

    //for setting the text
    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;I)V");

    if(sp.setServer(serverInfo)) {
        double preSpeed = 20.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute upload speed"), 0);
        }
    }

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Upload speed computed"), 30);

    return ulspeed;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeLatency(JNIEnv *env, jobject thiz, jlong serverPtr) {
    int latency = -1;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *((ServerInfo *)serverPtr);

    //for setting the text
    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;I)V");

    if(sp.setServer(serverInfo)) {
        latency = sp.latency();
    }

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Latency computed"), 20);

    return latency;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_freeServerPtr(JNIEnv *env, jobject thiz, jlong serverPtr) {
    free((ServerInfo *)serverPtr);
}