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



//returns a string array of the results
//we can convert them back later when uploading to the database
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_herontheb1rd_smcspeedtest_ResultsFragment_runSpeedtest(JNIEnv *env, jobject thiz, jobject jPreResultTV) {
    double dlspeed, ulspeed;
    long latency;
    std::string networkProvider;

    //used to check if the test failed 
    bool isTestSuccessful = true;
    std::string failMessage;

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
        isTestSuccessful = false;
        failMessage = "Failed getting ISP info";
    }else{
        //get isp
        networkProvider = info.isp;
        auto serverList = sp.serverList();
        env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Confiscating server list"));
        if (serverList.empty()){
            isTestSuccessful = false;
            failMessage = "Failed getting server info/latency";
        }else{
            env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Procuring latency"));
            serverInfo = sp.bestServer(10, [](bool success) {});
            //get latency
            latency = sp.latency();

            //skip the pretest, saving a minute or so
            //uses the broadband config found in TestConfigTemplate.h
            double preSpeed = 20.0;
            TestConfig uploadConfig;
            TestConfig downloadConfig;
            testConfigSelector(preSpeed, uploadConfig, downloadConfig);

            env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Pilfering download speed"));
            //get upload and download speed
            if(!sp.downloadSpeed(serverInfo, downloadConfig, dlspeed, [](bool success){})){
                isTestSuccessful = false;
                failMessage = "Failed getting download speed";
            }

            env->CallVoidMethod(jPreResultTV, setText, env->NewStringUTF("Liberating upload speed"));
            if(!sp.uploadSpeed(serverInfo, uploadConfig, ulspeed, [](bool success){})){
                isTestSuccessful = false;
                failMessage = "Failed getting upload speed";
            }
        }
    }

    jobjectArray resultsObj;

    //if the test was successful, return all values
    //if not, return a single value array with a fail message
    if(isTestSuccessful){
        std::string resultsStr[] = {std::to_string(dlspeed),
                                    std::to_string(ulspeed),
                                    std::to_string(latency),
                                    networkProvider
        };

        resultsObj = env->NewObjectArray( 4, env->FindClass("java/lang/String"), env->NewStringUTF(""));
        for(int i = 0; i < 4; i++){
            env->SetObjectArrayElement(resultsObj, i, env->NewStringUTF(resultsStr[i].c_str()));
        }
    }else{
        resultsObj = env->NewObjectArray( 1, env->FindClass("java/lang/String"), env->NewStringUTF(""));
        //for debugging
        env->SetObjectArrayElement(resultsObj, 0, env->NewStringUTF(failMessage.c_str()));
    }

    return resultsObj;
}





