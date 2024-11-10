#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include <csignal>
#include <jni.h>
#include <string>
#include <algorithm>

extern "C"
JNIEXPORT jobject JNICALL
Java_com_herontheb1rd_smcspeedtest_RunTestFragment_getServerInfo(JNIEnv *env, jobject thiz) {
    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;

    int testError = 0;

    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/RunTestFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;)V");

    sp.setInsecure(true);

    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Retrieving IP info..."));
    if (!sp.ipInfo(info)) {
        testError = 1;
    }else{
        auto serverList = sp.serverList();

        if (serverList.empty()) {
            testError = 2;
        } else {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Finding best server..."));
            serverInfo = serverList[0];

        }
    }

    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/ServerInfo");
    jmethodID cid = env->GetMethodID(c, "<init>", "()V");
    jobject serverInfoObj = env->NewObject(c, cid);

    if(testError == 1){
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "error", "Ljava/lang/String;"), env->NewStringUTF("ipInfo"));
    }else if(testError == 2) {
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "error", "Ljava/lang/String;"), env->NewStringUTF("serverList"));
    } else {
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "host", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.host.c_str()));
        env->SetIntField(serverInfoObj, env->GetFieldID(c, "id", "I"), serverInfo.id);
    }

    return serverInfoObj;
}

