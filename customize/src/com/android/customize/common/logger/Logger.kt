package com.android.customize.common.logger

import android.util.Log

interface Logger {
    fun i(msg: String)
    fun d(msg: String)
    fun w(msg: String)
    fun e(msg: String, cause: Throwable)

    fun <T> mi(key: String, defaultValue: T, value: () -> T?): T
    fun <T> md(key: String, defaultValue: T, value: () -> T?): T
    fun <T> mw(key: String, defaultValue: T, value: () -> T?): T

    suspend fun <T> smi(key: String, defaultValue: T, value: suspend () -> T?): T
    suspend fun <T> smd(key: String, defaultValue: T, value: suspend () -> T?): T
    suspend fun <T> smw(key: String, defaultValue: T, value: suspend () -> T?): T
}

class MyLogger(tag: String) : Logger {

    private val tagWrapper = "OK.DEBUG:$tag"

    override fun i(msg: String) {
        Log.i(tagWrapper, msg)
    }

    override fun d(msg: String) {
        Log.d(tagWrapper, msg)
    }

    override fun w(msg: String) {
        Log.w(tagWrapper, msg)
    }

    override fun e(msg: String, cause: Throwable) {
        Log.e(tagWrapper, msg)
    }

    override fun <T> mi(key: String, defaultValue: T, value: () -> T?): T {
        return p(key, defaultValue, value) {
            i(it)
        }
    }

    override fun <T> md(key: String, defaultValue: T, value: () -> T?): T {
        return p(key, defaultValue, value) {
            d(it)
        }
    }

    override fun <T> mw(key: String, defaultValue: T, value: () -> T?): T {
        return p(key, defaultValue, value) {
            w(it)
        }
    }

    override suspend fun <T> smi(key: String, defaultValue: T, value: suspend () -> T?): T {
        return sp(key, defaultValue, value) {
            i(it)
        }
    }

    override suspend fun <T> smd(key: String, defaultValue: T, value: suspend () -> T?): T {
        return sp(key, defaultValue, value) {
            d(it)
        }
    }

    override suspend fun <T> smw(key: String, defaultValue: T, value: suspend () -> T?): T {
        return sp(key, defaultValue, value) {
            w(it)
        }
    }

    private fun <T> p(
        key: String,
        defaultValue: T,
        value: () -> T?,
        aLogger: (msg: String) -> Unit
    ): T {
        val v = try {
            value() ?: defaultValue
        } catch (e: Exception) {
            e(key, e)
            defaultValue
        }
        aLogger("$key = $v")
        return v
    }

    private suspend fun <T> sp(
        key: String,
        defaultValue: T,
        value: suspend () -> T?,
        aLogger: (msg: String) -> Unit
    ): T {
        val v = try {
            value() ?: defaultValue
        } catch (e: Exception) {
            e(key, e)
            defaultValue
        }
        aLogger("$key = $v")
        return v
    }
}