package com.example.yvonnecai.core.model

/**
 * Failure while loading logs, expressed without any networking-library types so upper layers
 * can choose a message without knowing about Retrofit, OkHttp or kotlinx.serialization.
 *
 * @property reason coarse category used to choose the message shown to the user.
 */
class LogLoadException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception("Failed to load logs: $reason", cause) {

    /** Why loading failed. */
    enum class Reason {
        /** No connectivity, timeout, DNS failure... */
        NETWORK,

        /** The server answered with a non-2xx status. */
        SERVER,

        /** The payload could not be parsed. */
        INVALID_DATA,

        /** Anything else. */
        UNKNOWN,
    }
}
