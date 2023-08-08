#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include "TestConfigTemplate.h"
#include <csignal>
#include <jni.h>
#include <string>
#include <android/log.h>

//for logging
#define LOGV(TAG,...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(TAG,...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(TAG,...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(TAG,...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(TAG,...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)

double global_dlspeed;
double global_ulspeed;
long global_latency;
std::string global_network_provider;

extern "C"
JNIEXPORT void JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_runSpeedtest(JNIEnv *env, jobject thiz) {
    double dlspeed;
    double ulspeed;
    long latency;
    std::string network_provider;

    signal(SIGPIPE, SIG_IGN);

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo serverQualityInfo;

    //nesting these for now
    //TODO: change function to return an int for success/failure
    //if 0 success
    //if 1 fail
    //that way no nesting
    //like a shitty main function
    if (!sp.ipInfo(info)){

    }else{
        network_provider = info.isp;
    }

    auto serverList = sp.serverList();
    if (serverList.empty()){
        //cant find server list
    }else{
        serverInfo = sp.bestServer(10, [](bool success) {});


        latency = sp.latency();

        //skip the pretest
        //i know roughly how fast the internet is
        //and this just selects a config to use for the actual test
        //so i'll just put a dummy value thats in the range i want
        //cuts off a minute in test time
        //check TestConfigTemplate.h
        double preSpeed = 20.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success){});
        sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success){});

        //store variables in global variables
        //not messing around with pointers
        //when i can just do this
        global_dlspeed = dlspeed;
        global_ulspeed = ulspeed;
        global_latency = latency;
        global_network_provider = network_provider;

    }
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getDLSpeed(JNIEnv *env, jobject thiz) {
    return global_dlspeed;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getULSpeed(JNIEnv *env, jobject thiz) {
    return global_ulspeed;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getLatency(JNIEnv *env, jobject thiz) {
    return global_latency;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_getNetworkProvider(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(global_network_provider.c_str());
}





