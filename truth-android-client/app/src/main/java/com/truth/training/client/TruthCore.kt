package com.truth.training.client

import android.util.Log

object TruthCore {
    private const val TAG = "TruthCore"
    private val lock = Any()
    private val nativeLoaded: Boolean

    init {
        nativeLoaded = try {
            System.loadLibrary("truthcore")
            true
        } catch (error: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library truthcore unavailable, falling back to no-op stubs", error)
            false
        }
    }

    private external fun nativeInitNode()
    private external fun nativeGetInfo(): String
    private external fun nativeFreeString(ptr: Long)
    private external fun nativeProcessJsonRequest(request: String): String

    fun initNode() {
        if (!nativeLoaded) {
            Log.i(TAG, "initNode skipped (native library not loaded)")
            return
        }
        try {
            nativeInitNode()
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native method nativeInitNode not found, skipping", e)
        }
    }

    fun getInfo(): String {
        if (!nativeLoaded) {
            return """{"status":"unavailable"}"""
        }
        return try {
            nativeGetInfo()
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native method nativeGetInfo not found, returning unavailable", e)
            """{"status":"unavailable"}"""
        }
    }

    fun freeString(ptr: Long) {
        if (!nativeLoaded) return
        try {
            nativeFreeString(ptr)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native method nativeFreeString not found, skipping", e)
        }
    }

    fun processJsonRequest(request: String): String {
        if (!nativeLoaded) {
            return """{"status":"unavailable","echo":$request}"""
        }
        return try {
            nativeProcessJsonRequest(request)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native method nativeProcessJsonRequest not found, returning echo", e)
            """{"status":"unavailable","echo":$request}"""
        }
    }

    fun processJson(request: String): String = synchronized(lock) {
        processJsonRequest(request)
    }
}
