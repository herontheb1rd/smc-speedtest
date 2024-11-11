#include <iostream>
#include <map>
#include <iomanip>
#include "SpeedTest.h"
#include "TestConfigTemplate.h"
#include <csignal>
#include <jni.h>
#include <string>



//https://stackoverflow.com/questions/41820039/jstringjni-to-stdstringc-with-utf8-characters
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

ServerInfo convertObjToStruct(JNIEnv *env, jobject jServerInfo){
    ServerInfo serverInfo;

    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/ServerInfo");

    serverInfo.host = jstring2string(env, (jstring)env->GetObjectField(jServerInfo, env->GetFieldID(c, "host", "Ljava/lang/String;")));
    serverInfo.id = (int)(env->GetIntField(jServerInfo, env->GetFieldID(c, "id", "I")));

    return serverInfo;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeDlspeed(JNIEnv *env, jobject thiz, jobject jServerInfo) {
    double dlspeed = -1;

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(std::to_string(SPEED_TEST_MIN_SERVER_VERSION));
    sp.setInsecure(true);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo javaServerInfo = convertObjToStruct(env, jServerInfo);

    jmethodID cppLogger = env->GetMethodID(env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger", "(Ljava/lang/String;)V");

    signal(SIGPIPE, SIG_IGN);

    int MAX_TRIES = 1;
    for (int i = 0; i < MAX_TRIES; i++) {

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("dl - Run"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(std::to_string(i).c_str()));
        if (!sp.ipInfo(info)) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("dl - Failed to get IP Info"));
            dlspeed = -1;
            continue;
        }

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("IP"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(info.ip_address.c_str()));

        auto serverList = sp.serverList();

        if (serverList.empty()) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("dl - Failed to get server list"));
            dlspeed = -1;
            continue;
        } else {

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("dl - Didn't fail"));

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("dl - Java Host"));
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF(javaServerInfo.host.c_str()));

            serverInfo.host.append(javaServerInfo.host.c_str());

            if(!sp.setServer(serverInfo)) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Set server failed"));
                continue;
            }

            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("dlspeed - Getting ddl speed"));
            sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {});
            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("dl - Got dl speed"));
        }
    }

    return dlspeed;
}


extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeUlspeed(JNIEnv *env, jobject thiz, jobject jServerInfo) {
    double ulspeed = -1;

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(std::to_string(SPEED_TEST_MIN_SERVER_VERSION));
    sp.setInsecure(true);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo javaServerInfo = convertObjToStruct(env, jServerInfo);

    jmethodID cppLogger = env->GetMethodID(env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger", "(Ljava/lang/String;)V");

    signal(SIGPIPE, SIG_IGN);

    int MAX_TRIES = 1;
    for (int i = 0; i < MAX_TRIES; i++) {

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("ulspeed - Run"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(std::to_string(i).c_str()));
        if (!sp.ipInfo(info)) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("ulspeed - Failed to get IP Info"));
            ulspeed = -1;
            continue;
        }

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("IP"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(info.ip_address.c_str()));

        auto serverList = sp.serverList();

        if (serverList.empty()) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("ulspeed - Failed to get server list"));
            ulspeed = -1;
            continue;
        } else {

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("ulspeed - Didn't fail"));

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("ulspeed - Java Host"));
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF(javaServerInfo.host.c_str()));

            serverInfo.host.append(javaServerInfo.host.c_str());

            if(!sp.setServer(serverInfo)) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Set server failed"));
                continue;
            }


            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Getting upload speed"));
            sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {});
            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Got upload speed"));
        }
    }

    return ulspeed;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeLatency(JNIEnv *env, jobject thiz, jobject jServerInfo) {
    int latency = -1;

    signal(SIGPIPE, SIG_IGN);

    auto sp = SpeedTest(std::to_string(SPEED_TEST_MIN_SERVER_VERSION));
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo javaServerInfo = convertObjToStruct(env, jServerInfo);

    sp.setInsecure(true);

    jmethodID cppLogger = env->GetMethodID(
            env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger",
            "(Ljava/lang/String;)V");

    int MAX_TRIES = 1;
    for (int i = 0; i < MAX_TRIES; i++) {

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("Latency - Run"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(std::to_string(i).c_str()));
        if (!sp.ipInfo(info)) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Failed to get IP Info"));
            latency = -1;
            continue;
        }

        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("IP"));
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF(info.ip_address.c_str()));

        auto serverList = sp.serverList();

        if (serverList.empty()) {
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Failed to get server list"));
            latency = -1;
            continue;
        } else {

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Didn't fail"));

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Java Host"));
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF(javaServerInfo.host.c_str()));

            serverInfo.host.append(javaServerInfo.host.c_str());

            if(!sp.setServer(serverInfo)) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Set server failed"));
                continue;
            }

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Set server success"));

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Host"));

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Host"));
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF(serverInfo.host.c_str()));

            for (auto &s : serverList) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Latency - FL Host"));
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF(s.host.c_str()));
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Latency - FL ID"));
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF(std::to_string(s.id).c_str()));
                if (s.host == serverInfo.host)
                    serverInfo.id = s.id;
            }

            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - ID"));
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF(std::to_string(serverInfo.id).c_str()));


            latency = sp.latency();
        }
    }
    return latency;
}



extern "C"
JNIEXPORT jobject JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeNetPerf(JNIEnv *env, jobject thiz) {
    double dlspeed = -1;
    double ulspeed = -1;
    long latency = -1;
    //for setting the text

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(std::to_string(SPEED_TEST_MIN_SERVER_VERSION));
    IPInfo info;
    ServerInfo serverInfo;

    sp.setInsecure(true);
    if (!sp.ipInfo(info)){
        //env->ThrowNew(env->FindClass("java/lang/Exception"), "Cannot retrieve network info");
    }else {
        auto serverList = sp.serverList();
        if (serverList.empty()) {
            //env->ThrowNew(env->FindClass("java/lang/Exception"), "Server list is empty");
        } else {
            serverInfo = sp.bestServer(10, [](bool success) {});
            //get latency
            latency = sp.latency();
            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);
            //get upload and download speed
            if (!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {})) {
                //env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to compute download speed");
            }
            if (!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {})) {
                //env->ThrowNew(env->FindClass("java/lang/Exception"), "Failed to compute upload speed");
            }
        }
    }
    jclass c = env->FindClass("com/herontheb1rd/smcspeedtest/NetPerf");
    jmethodID cid = env->GetMethodID(c, "<init>", "(DDI)V");
    jobject resultsObj = env->NewObject(c, cid, dlspeed, ulspeed, latency);
    return resultsObj;
}