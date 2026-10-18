package com.example.yvonnecai.core.network

/** The one place the log endpoint is defined. */
object LogEndpoint {
    /**
     * Full URL from the challenge brief. The object name keeps its `%2F` (Firebase Storage expects
     * `data/logs_5k.json` as a single, percent-encoded segment); it is passed to Retrofit through
     * `@Url` so it is not encoded a second time. The token is the public download token embedded
     * in that URL, not a secret.
     */
    const val URL =
        "https://firebasestorage.googleapis.com/v0/b/fieldinspectiondev.firebasestorage.app/o/" +
            "data%2Flogs_5k.json?alt=media&token=15c66bf6-9716-44da-b3d1-ba9bb241baf8"

    /** Retrofit requires a base URL; every call overrides it with an absolute `@Url`. */
    internal const val PLACEHOLDER_BASE_URL = "https://firebasestorage.googleapis.com/"
}
