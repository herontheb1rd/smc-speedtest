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


//TODO: add error handling if test failed
extern "C"
JNIEXPORT jobject JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_runSpeedtest(JNIEnv *env, jobject thiz) {
    double dlspeed = -1;
    double ulspeed = -1;
    long latency = -1;

    //for setting the text
    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;I)V");

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo serverQualityInfo;

    sp.setInsecure(true);

    if (!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Cannot retrieve network info"), 0);
        //env->ThrowNew(env->FindClass("java/lang/Exception"), "Cannot retrieve network info");
    }else {
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF(info.ip_address.c_str()), 5);
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Network info acquired. Checking server list"), 5);
        auto serverList = sp.serverList();
        if (serverList.empty()) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Server list is empty"), 0);
            //env->ThrowNew(env->FindClass("java/lang/Exception"), "Server list is empty");
        } else {
            serverInfo = sp.bestServer(10, [](bool success) {});
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Best server chosen"), 5);
            //get latency
            latency = sp.latency();
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Latency to server computed"), 10);

            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            //get upload and download speed
            if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute download speed"), 0);
                //env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to compute download speed");
            }

            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Download speed computed"), 10);
            if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute upload speed"), 0);
                //env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to compute upload speed");
            }
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Upload speed computed"), 10);

        }
    }

    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/NetPerf");
    jmethodID cid = env->GetMethodID(c, "<init>", "(DDI)V");
    jobject resultsObj = env->NewObject(c, cid, dlspeed, ulspeed, latency);

    return resultsObj;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getServerInfo(JNIEnv *env, jobject thiz) {
    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo ipInfo;
    ServerInfo serverInfo;

    sp.setInsecure(true);

    if (!sp.ipInfo(ipInfo)){
        return 0;
    }else {
        auto serverList = sp.serverList();
        if (serverList.empty()) {
            return 0;
        } else {
            serverInfo = sp.bestServer(10, [](bool success) {});
        }
    }

    //allocates the server info to the heap to be accessed later
    //we're returning the memory address of a copy of the stored server info to java as a long
    ServerInfo *serverCpy = (ServerInfo *)malloc(sizeof(serverInfo));
    memcpy(serverCpy, &serverInfo, sizeof(serverInfo));
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


//https://stackoverflow.com/questions/11621449/send-c-string-to-java-via-jni
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getServerURLBytes(JNIEnv *env, jclass thiz, jlong serverPtr) {
    std::string url = "";

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    ServerInfo serverInfo = *((ServerInfo *)serverPtr);

    sp.setInsecure(true);

    if(sp.setServer(serverInfo)) {
        url = serverInfo.url.c_str();
    }

    int byteCount = url.length();
    jbyte* pNativeString = (jbyte*)(url.c_str());
    jbyteArray bytes = env->NewByteArray(byteCount);
    env->SetByteArrayRegion(bytes, 0, byteCount, pNativeString);

    return bytes;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_freeServerPtr(JNIEnv *env, jobject thiz, jlong serverPtr) {
    free((ServerInfo *)serverPtr);
}