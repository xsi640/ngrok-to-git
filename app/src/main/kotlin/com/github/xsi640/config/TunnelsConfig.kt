package com.github.xsi640.config

class TunnelsConfig(
    var authtoken: String = "",
    var tunnels: Map<String, Tunnel> = emptyMap()
)

class Tunnel(
    var addr: Int = 0,
    var proto: String = ""
)