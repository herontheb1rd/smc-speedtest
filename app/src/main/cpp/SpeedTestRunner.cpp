#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include "TestConfigTemplate.h"
#include <csignal>
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jobject JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeNetPerf(JNIEnv *env, jobject thiz) {
    double dlspeed = -1;
    double ulspeed = -1;
    long latency = -1;

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;

    jclass resultsClass = env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment");
    jmethodID cppLogger = env->GetMethodID(resultsClass, "cppLogger", "(Ljava/lang/String;)V");
    jmethodID displayProgress = env->GetMethodID(resultsClass, "displayProgress", "(Ljava/lang/String;)V");
    jmethodID displayDownloadResult = env->GetMethodID(resultsClass, "displayDownloadResult", "(D)V");
    jmethodID displayUploadResult = env->GetMethodID(resultsClass, "displayUploadResult", "(D)V");
    jmethodID displayLatencyResult = env->GetMethodID(resultsClass, "displayLatencyResult", "(I)V");

    sp.setInsecure(true);


    env->CallVoidMethod(thiz, displayProgress,
                        env->NewStringUTF("Starting speed test"));
    if (!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, displayProgress,
                            env->NewStringUTF("Failed to get IP info"));
        return NULL;
    }else {
        env->CallVoidMethod(thiz, displayProgress,
                            env->NewStringUTF("Getting server list"));
        auto serverList = sp.serverList();
        if (serverList.empty()) {
            env->CallVoidMethod(thiz, displayProgress,
                                env->NewStringUTF("Failed to get server list"));
            return NULL;
        } else {
            env->CallVoidMethod(thiz, displayProgress,
                                env->NewStringUTF("Finding best server. This might take a while"));
            serverInfo = sp.bestServer(10, [](bool success) {});

            env->CallVoidMethod(thiz, displayProgress,
                                env->NewStringUTF("Computing latency"));
            //get latency
            latency = sp.latency();
            env->CallVoidMethod(thiz, displayLatencyResult, latency);

            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(thiz, displayProgress,
                                env->NewStringUTF("Getting download speed"));
            if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, displayProgress,
                                    env->NewStringUTF("Failed to get download speed"));
            }
            env->CallVoidMethod(thiz, displayDownloadResult, dlspeed);

            env->CallVoidMethod(thiz, displayProgress,
                                env->NewStringUTF("Getting upload speed"));
            if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, displayProgress,
                                    env->NewStringUTF("Failed to get upload speed"));
            }
            env->CallVoidMethod(thiz, displayUploadResult, ulspeed);
        }
    }
    jclass netPerfClass = env->FindClass("com/herontheb1rd/smcspeedtest/NetPerf");
    jmethodID cid = env->GetMethodID(netPerfClass, "<init>", "(DDI)V");
    jobject resultsObj = env->NewObject(netPerfClass, cid, dlspeed, ulspeed, latency);
    return resultsObj;
}