package com.example.starterhack

import android.app.Application
import android.util.Log

/**
 * Set ADSP_LIBRARY_PATH and LD_LIBRARY_PATH before any LiteRT-LM / QNN library loads.
 *
 * The Hexagon DSP cdsp domain reads ADSP_LIBRARY_PATH **once at QnnHtp init** (which happens
 * transitively when libLiteRtDispatch_Qualcomm.so is dlopened). If we set it later inside
 * InferenceEngine.initialize(), QnnHtp has already captured an empty path and can't find
 * libQnnHtpV79Skel.so on this Samsung S25 Ultra (Samsung places it in /vendor/lib64/rfs/dsp/snap/,
 * which is not in FastRPC's hardcoded fallback list).
 */
class ShieldTextApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val paths = listOf(
            nativeLibDir,
            "/vendor/lib64/rfs/dsp/snap", // Samsung S25 Ultra Hexagon V79 skel
            "/vendor/lib64/hw/audio",     // Samsung alternate skel location
            "/vendor/dsp/cdsp",
            "/vendor/lib64",
            "/vendor/lib64/snap",         // matches InferenceEngine.configureNativeRuntime()
            "/system/lib64",
        ).joinToString(":")
        runCatching {
            android.system.Os.setenv("ADSP_LIBRARY_PATH", paths, true)
            android.system.Os.setenv("LD_LIBRARY_PATH", paths, true)
            Log.i("ShieldTextApp", "Pre-init ADSP_LIBRARY_PATH=$paths")
        }.onFailure { Log.w("ShieldTextApp", "Failed to seed DSP library paths: ${it.message}") }
    }
}
