package com.lomen.tv.domain.model

data class WebDavConfig(
    val id: String = "",
    val name: String = "",
    val protocol: Protocol = Protocol.HTTP,
    val host: String = "",
    val port: Int = 8893,
    val path: String = "/mnt/media/pushes",
    val username: String = "",
    val password: String = "",
    val isEnabled: Boolean = true
) {
    enum class Protocol {
        HTTP, HTTPS
    }

    fun getFullUrl(): String {
        val protocolStr = protocol.name.lowercase()
        return "$protocolStr://$host:$port"
    }

    fun getQrCodeContent(): String {
        return getFullUrl()
    }
}
