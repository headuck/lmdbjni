LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := lmdbjni
LOCAL_SRC_FILES := buffer.c hawtjni.c hawtjni-callback.c mdb.c midl.c lmdbjni.c lmdbjni_stats.c lmdbjni_structs.c
LOCAL_CFLAGS := -DMDB_DSYNC=O_SYNC -DHAVE_CONFIG_H

include $(BUILD_SHARED_LIBRARY)

