package io.dedyn.engineermantra.omega.shared

interface ConfigFileInterface {
    fun load()
    fun save()
    fun get(token: String): String?
    fun set(token: String, value: String)

}