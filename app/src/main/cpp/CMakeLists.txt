cmake_minimum_required(VERSION 3.22.1)

project("smcspeedtest" LANGUAGES CXX)

set (SpeedTest_VERSION_MAJOR 1)
set (SpeedTest_VERSION_MINOR 15)
set (SpeedTest_SYSTEM_PROCESSOR ${CMAKE_SYSTEM_PROCESSOR})
set (SpeedTest_SYSTEM ${CMAKE_SYSTEM})

set (SpeedTest_AUTHOR "Francesco Laurita <francesco.laurita@gmail.com>")
set (SpeedTest_HOME_PAGE "https://github.com/taganaka/SpeedTest")
set (SpeedTest_USER_AGENT "Mozilla/5.0 ${CMAKE_SYSTEM}; U; ${CMAKE_SYSTEM_PROCESSOR}; en-us (KHTML, like Gecko) SpeedTest++/${SpeedTest_VERSION_MAJOR}.${SpeedTest_VERSION_MINOR}")
set (SpeedTest_SERVER_LIST_URL "https://www.speedtest.net/speedtest-servers.php")
set (SpeedTest_IP_INFO_API_URL "https://api.ipapi.is/")
set (SpeedTest_API_URL "http://www.speedtest.net/api/api.php")
set (SpeedTest_API_REFERER "http://c.speedtest.net/flash/speedtest.swf")
set (SpeedTest_API_KEY "297aae72")
set (SpeedTest_MIN_SERVER_VERSION "2.3")
set (SpeedTest_LATENCY_SAMPLE_SIZE 80)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/Modules/")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-result -Wno-pessimizing-move -Wno-deprecated-declarations")

set(SOURCE_FILES
        SpeedTestRunner.cpp
        ServerConnector.cpp
        SpeedTest.cpp
        SpeedTest.h
        SpeedTestClient.cpp
        SpeedTestClient.h
        TestConfigTemplate.h
        MD5Util.cpp
        MD5Util.h
        DataTypes.h
        )
configure_file (
        "${PROJECT_SOURCE_DIR}/SpeedTestConfig.h.in"
        "${PROJECT_BINARY_DIR}/SpeedTestConfig.h"
)
add_library(smcspeedtest
        SHARED
        ${SOURCE_FILES})
include_directories("${PROJECT_BINARY_DIR}")
INCLUDE (CheckIncludeFiles)

add_library(LIBXML2
        STATIC
        IMPORTED)
set_target_properties(LIBXML2
        PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libxml2/${ANDROID_ABI}/lib/libxml2.a)
target_include_directories(smcspeedtest PUBLIC ${CMAKE_SOURCE_DIR}/libxml2/${ANDROID_ABI}/include/libxml2)

find_package(curl REQUIRED CONFIG)

target_link_libraries(smcspeedtest curl::curl LIBXML2)