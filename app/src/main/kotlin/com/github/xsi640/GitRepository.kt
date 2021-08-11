package com.github.xsi640

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.IOException
import java.net.*

class GitRepository(
    val config: GistsConfig,
    val gitDir: File
) {

    private lateinit var credential: CredentialsProvider
    private lateinit var git: Git

    init {
        initProxy()
        initCredential()
    }

    private fun initCredential() {
        credential = UsernamePasswordCredentialsProvider(config.username, config.password)
    }

    private fun initProxy() {
        if (config.proxy != null) {
            ProxySelector.setDefault(object : ProxySelector() {
                val delegate = getDefault()
                override fun select(uri: URI): List<Proxy> {
                    if (uri.toString().contains("github", true)) {
                        return listOf(
                            Proxy(
                                Proxy.Type.HTTP,
                                InetSocketAddress.createUnresolved(config.proxy!!.host, config.proxy!!.port)
                            )
                        )
                    }
                    return if (delegate == null) listOf(Proxy.NO_PROXY) else delegate.select(uri)
                }

                override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {
                }
            })
        }
    }

    fun clone() {
        git = Git.cloneRepository()
            .setCredentialsProvider(credential)
            .setURI(config.url)
            .setDirectory(gitDir).call()
    }

    fun pull() {
        git.pull()
    }

    fun commit(message: String) {
        git.add().addFilepattern(".").call()
        git.commit().setMessage(message).call()
        git.push().setCredentialsProvider(credential)
            .setRemote("origin").setRefSpecs(RefSpec("master")).call()
    }
}