//
// Created by gensh on 2017/9/19.
//

#ifndef HELLO_USTB_DB_H
#define HELLO_USTB_DB_H

#include <jni.h>
#include <string>

//if file db.cpp can not be accessed, please implements  your own cpp file instead.
extern "C"
JNIEXPORT jstring JNICALL
Java_me_gensh_natives_DatabaseEncrypted_getDBEncryptedPassword(JNIEnv *env, jobject /* this */);

#endif //HELLO_USTB_DB_H
