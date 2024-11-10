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

    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    sp.setInsecure(true);

    ServerInfo serverInfo = convertObjToStruct(env, jServerInfo);

    jmethodID cppLogger = env->GetMethodID(env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger", "(Ljava/lang/String;)V");

    if(sp.setServer(serverInfo)) {
        env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Download - Connected to server successfully"));

        double preSpeed = 20.0;
        TestConfig uploadConfig;
        TestConfig downloadConfig;
        testConfigSelector(preSpeed, uploadConfig, downloadConfig);

        env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Download - Getting download speed"));
        sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success) {});
        env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Download - Got download speed"));

    }else{
        env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Download - Failed to connect to server"));
    }
    return dlspeed;
}


extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeUlspeed(JNIEnv *env, jobject thiz, jobject jServerInfo) {
    double ulspeed = -1;

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    sp.setInsecure(true);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo javaServerInfo = convertObjToStruct(env, jServerInfo);

    jmethodID cppLogger = env->GetMethodID(env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger", "(Ljava/lang/String;)V");

    if(!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("Upload - Failed to get IP Info"));
        ulspeed = -1;
    }else {
        auto serverList = sp.serverList();

        if(serverList.empty()){
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Upload - Failed to get server list"));
            ulspeed = -1;
        }else {
            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Host of server:"));
            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF(javaServerInfo.host.c_str()));

            int i = 0;
            env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Server list:"));

            serverInfo.host.append(javaServerInfo.host.c_str());
            sp.setServer(serverInfo);

            for (auto &s : serverList) {
                if (s.host == serverInfo.host)
                    serverInfo.id = s.id;
            }

            if (sp.setServer(serverInfo)) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF(
                                            "Upload - Connected to server successfully"));

                double preSpeed = 20.0;
                TestConfig uploadConfig;
                TestConfig downloadConfig;
                testConfigSelector(preSpeed, uploadConfig, downloadConfig);

                env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Getting upload speed"));
                sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success) {});
                env->CallVoidMethod(thiz, cppLogger, env->NewStringUTF("Upload - Got upload speed"));

            } else {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Upload - Failed to connect to server"));
            }


        }
    }

    return ulspeed;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_computeLatency(JNIEnv *env, jobject thiz, jobject jServerInfo) {
    int latency = -1;

    signal(SIGPIPE, SIG_IGN);
    auto sp = SpeedTest(SPEED_TEST_MIN_SERVER_VERSION);
    sp.setInsecure(true);
    IPInfo info;
    ServerInfo serverInfo;
    ServerInfo javaServerInfo = convertObjToStruct(env, jServerInfo);

    jmethodID cppLogger = env->GetMethodID(env->FindClass("com/herontheb1rd/smcspeedtest/ResultsFragment"), "cppLogger", "(Ljava/lang/String;)V");

    if(!sp.ipInfo(info)){
        env->CallVoidMethod(thiz, cppLogger,
                            env->NewStringUTF("Latency - Failed to get IP Info"));
        latency = -1;
    }else {
        auto serverList = sp.serverList();

        if(serverList.empty()){
            env->CallVoidMethod(thiz, cppLogger,
                                env->NewStringUTF("Latency - Failed to get server list"));
            latency = -1;
        }else {
/*
            serverInfo.host.append(javaServerInfo.host.c_str());
            sp.setServer(serverInfo);

            for (auto &s : serverList) {
                if (s.host == serverInfo.host)
                    serverInfo.id = s.id;
            }

            latency = sp.latency();
            */


            if (sp.setServer(javaServerInfo)) {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF(
                                            "Latency - Connected to server successfully"));

                latency = sp.latency();
            } else {
                env->CallVoidMethod(thiz, cppLogger,
                                    env->NewStringUTF("Latency - Failed to connect to server"));
            }


        }
    }
    return latency;
}


