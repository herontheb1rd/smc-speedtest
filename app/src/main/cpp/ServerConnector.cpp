#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include <csignal>
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jobject JNICALL
Java_com_herontheb1rd_smcspeedtest_RunTestFragment_getServerInfo(JNIEnv *env, jobject thiz) {
    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo serverQualityInfo;

    int testError = 0;

    jclass clazz = env->FindClass("com/herontheb1rd/smcspeedtest/RunTestFragment");
    jmethodID updateProgress = env->GetMethodID(clazz, "updateProgress", "(Ljava/lang/String;)V");

    sp.setInsecure(true);



    env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Retrieving IP info..."));
    if (!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Failed to get IP info, retrying..."));
        testError = 1;
    }else {
        env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Retrieving server list..."));
        auto serverList = sp.serverList();
        
        if (serverList.empty()) {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Server list empty, retrying..."));
            testError = 2;
        } else {
            env->CallVoidMethod(thiz, updateProgress, env->NewStringUTF("Finding best server..."));
            serverInfo = sp.bestServer(5, [](bool success) {});
        }
    }


    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/ServerInfo");
    jmethodID cid = env->GetMethodID(c, "<init>", "()V");
    jobject serverInfoObj = env->NewObject(c, cid);

    if(testError == 1){
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "name", "Ljava/lang/String;"), env->NewStringUTF("ipInfo"));
    }else if(testError == 2) {
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "name", "Ljava/lang/String;"), env->NewStringUTF("serverList"));
    } else {
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "url", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.url.c_str()));
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "name", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.name.c_str()));
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "country", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.country.c_str()));
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "country_code", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.country_code.c_str()));
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "host", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.host.c_str()));
        env->SetObjectField(serverInfoObj, env->GetFieldID(c, "sponsor", "Ljava/lang/String;"),
                            env->NewStringUTF(serverInfo.sponsor.c_str()));
        env->SetIntField(serverInfoObj, env->GetFieldID(c, "id", "I"), serverInfo.id);
        env->SetFloatField(serverInfoObj, env->GetFieldID(c, "lat", "F"), serverInfo.lat);
        env->SetFloatField(serverInfoObj, env->GetFieldID(c, "lon", "F"), serverInfo.lon);
        env->SetFloatField(serverInfoObj, env->GetFieldID(c, "distance", "F"), serverInfo.distance);
    }

    return serverInfoObj;
}

