#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include "TestConfigTemplate.h"
#include <csignal>
#include <jni.h>
#include <string>

//main.cpp
//Code by Heron Nalasa

extern "C"
JNIEXPORT jlong JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getServerInfo(JNIEnv *env, jobject thiz) {
    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo ipInfo;
    ServerInfo serverInfo;

    sp.setInsecure(true);

    if (!sp.ipInfo(ipInfo)){
        //TODO: figure out how to throw an error without making app crash
    }else {
        auto serverList = sp.serverList();
        if (serverList.empty()) {

        } else {
            serverInfo = sp.bestServer(10, [](bool success) {});
        }
    }

    //allocates the server info to the heap to be accessed later
    //we're returning the memory address of a copy of the stored server info to java as a long
    ServerInfo *serverCpy = (ServerInfo *)malloc(sizeof(ServerInfo));
    memcpy(serverCpy, &serverInfo, sizeof(ServerInfo));
    unsigned long long serverPtr = (unsigned long long)serverCpy;

    return serverPtr;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeDlspeed(JNIEnv *env, jobject thiz, jlong serverPtr) {
    double dlspeed = -1;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *(ServerInfo *)serverPtr;

    sp.setInsecure(true);


    if(sp.setServer(serverInfo)) {
        double preSpeed = 0.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {});
    }
    return dlspeed;
}


extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeUlspeed(JNIEnv *env, jobject thiz, jlong serverPtr) {
    double ulspeed = -1;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *(ServerInfo *)serverPtr;

    sp.setInsecure(true);

    if(sp.setServer(serverInfo)) {
        double preSpeed = 0.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {});
    }

    return ulspeed;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeLatency(JNIEnv *env, jobject thiz, jlong serverPtr) {
    int latency = -1;

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *((ServerInfo *)serverPtr);

    sp.setInsecure(true);

    if(sp.setServer(serverInfo)) {
        latency = sp.latency();
    }
    return latency;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_freeServerPtr(JNIEnv *env, jobject thiz, jlong serverPtr) {
    free((ServerInfo *)serverPtr);
}