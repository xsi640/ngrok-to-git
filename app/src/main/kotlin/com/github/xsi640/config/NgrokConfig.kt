package com.github.xsi640.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("ngrok")
class NgrokConfig(
    var path: String = "",
    var logPath: String = "",
    var git: GitConfig? = null,
)

class GitConfig(
    var username: String = "",
    var password: String = "",
    var url: String = "",
    var fileName: String = "",
    var proxy: ProxyConfig? = null
)

class ProxyConfig(
    var host: String = "",
    var port: Int = 0
)