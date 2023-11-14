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
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_runSpeedtest(JNIEnv *env, jobject thiz, jobject jPreResultTV) {
    double dlspeed, ulspeed;
    long latency;

    //for setting the text
    jclass clazz = env->FindClass("android/widget/TextView");
    jmethodID setText = env->GetMethodID(clazz, "setText", "(Ljava/lang/CharSequence;)V");

    signal(SIGPIPE, SIG_IGN);

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo serverQualityInfo;

    env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Acquiring network provider info"));
    if (!sp.ipInfo(info)){

    }else {
        auto serverList = sp.serverList();
        env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Acquiring server list"));
        if (serverList.empty()) {
        } else {
            env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Computing latency"));
            serverInfo = sp.bestServer(10, [](bool success) {});
            //get latency
            latency = sp.latency();

            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(jPreResultTV, setText,
                                env->NewStringUTF("Computing download speed"));
            //get upload and download speed
            if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {

            }

            env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Computing upload speed"));
            if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {

            }
        }
    }

    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/NetPerf");
    jmethodID cid = env->GetMethodID(c, "<init>", "(DDI)V");
    jobject resultsObj = env->NewObject(c, cid, dlspeed, ulspeed, latency);

    return resultsObj;
}





