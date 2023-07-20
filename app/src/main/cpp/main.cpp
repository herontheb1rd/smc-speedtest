#include <jni.h>
#include <string>
#include "curl/curl.h"

//setting initial values to be impossible
//so we can check if they are empty later
double dlspeed = -1.0;
double ulspeed = -1.0;
long latency = -1;
std::string network_provider = "";

extern "C"
JNIEXPORT void JNICALL
Java_com_herontheb1rd_smcspeedtest_speedtestJava_runSpeedtest(JNIEnv *env, jobject thiz) {
    // for now this returns random values
    // TODO: add speedtest code
    dlspeed = 10.2;
    ulspeed = 10.2;
    latency = 10;
    network_provider = "asdf";
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_speedtestJava_getDLSpeed(JNIEnv *env, jobject thiz) {
    if(dlspeed == -1){
        throw "Download Speed not set";
    } else{
        return dlspeed;
    }
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_herontheb1rd_smcspeedtest_speedtestJava_getULSpeed(JNIEnv *env, jobject thiz) {
    if(ulspeed == -1){
        throw "Upload Speed not set";
    } else{
        return ulspeed;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_herontheb1rd_smcspeedtest_speedtestJava_getLatency(JNIEnv *env, jobject thiz) {
    if(latency == -1){
        throw "Latency not set";
    } else{
        return latency;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_herontheb1rd_smcspeedtest_speedtestJava_getNetworkProvider(JNIEnv *env, jobject thiz) {
    if(network_provider == ""){
        throw "Network Provider not set";
    } else{
        return env->NewStringUTF(network_provider.c_str());
    }
}





