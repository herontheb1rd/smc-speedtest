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


//TODO: add error handling if test failed
//returns a string array of the results
//we can convert them back later when uploading to the database
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

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Network info acquired."), 5);
    if (!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Cannot retrieve network info"), 5);
    }else {
        auto serverList = sp.serverList();
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Server list acquired"), 5);
        if (serverList.empty()) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Server list is empty"), 5);
        } else {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Latency computed"), 5);
            serverInfo = sp.bestServer(10, [](bool success) {});
            //get latency
            latency = sp.latency();

            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Download speed computed"), 5);
            //get upload and download speed
            if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute download speed"), 5);
            }

            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Upload speed computed"), 5);
            if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {
                env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to compute upload speed"), 5);
            }
        }
    }

    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/NetPerf");
    jmethodID cid = env->GetMethodID(c, "<init>", "(DDI)V");
    jobject resultsObj = env->NewObject(c, cid, dlspeed, ulspeed, latency);

    return resultsObj;
}





