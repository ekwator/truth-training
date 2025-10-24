package com.truth.training.client

object TruthCore {
    private val lock = Any()
    init {
        System.loadLibrary("truthcore")
    }

    external fun initNode()
    external fun getInfo(): String
    external fun freeString(ptr: Long)
    external fun processJsonRequest(request: String): String

    fun processJson(request: String): String = synchronized(lock) {
        processJsonRequest(request)
    }
}
