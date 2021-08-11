package com.github.xsi640

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.FileOutputStream
import java.net.*

@SpringBootApplication
class Application : CommandLineRunner, DisposableBean {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    @Autowired
    private lateinit var ngrokConfig: NgrokConfig

    private lateinit var gitRepository: GitRepository
    private lateinit var gitDir: File
    private lateinit var loggerFileMonitor: LoggerFileMonitor
    private var process: Process? = null

    override fun run(vararg args: String?) {
        val app = ngrokConfig.path
        if (!File(app).exists()) {
            throw IllegalArgumentException("The ngrok application not exists.")
        }
        val logPath = ngrokConfig.logPath
        val logFile = File(logPath)
        if (!logFile.parentFile.exists()) {
            logFile.parentFile.mkdirs()
        }

        gitDir = File(File(app).parent, "git")
        if (gitDir.exists())
            FileSystemUtils.deleteRecursively(gitDir)
        gitRepository = GitRepository(ngrokConfig.git!!, gitDir)
        gitRepository.clone()

        startNgrokService()
        monitLoggerFile()
    }

    fun startNgrokService() {
        val app = File(ngrokConfig.path)
        val config = File(app.parentFile, "config.yml")
        if (config.exists())
            config.delete()

        this.javaClass.getResourceAsStream("/config.yml").use { src ->
            IOUtils.copy(src, FileOutputStream(config))
        }

        val log = File(ngrokConfig.logPath)
        if (log.exists())
            log.delete()

        val pb = ProcessBuilder(
            app.absolutePath, "start",
            "--config=${config.absolutePath}", "--all", "-log=${log.absolutePath}"
        )
        process = pb.start()
    }

    fun monitLoggerFile() {
        val log = File(ngrokConfig.logPath)
        loggerFileMonitor = LoggerFileMonitor(log) {
            parseNewLine(it)
        }
        loggerFileMonitor.start()
    }

    fun parseNewLine(line: String) {
        val items = line.split(" ")
        if (items.isEmpty())
            return
        val map = mutableMapOf<String, String>()
        items.forEach { item ->
            val keys = item.trim().split("=")
            if (keys.size == 2) {
                map[keys[0].trim()] = keys[1].trim()
            }
        }
        if (map.isEmpty())
            return

        if (map.containsKey("obj") && map["obj"] == "tunnels" &&
            map.containsKey("addr") && map["addr"]!!.isNotEmpty() &&
            map.containsKey("url") && map["url"]!!.isNotEmpty()
        ) {
            val addr = URL(map["addr"])
            val url = URL(map["url"])
            writeGitFile(url.toString(), addr.port)
        }
    }

    fun writeGitFile(url: String, port: Int) {
        gitRepository.pull()
        val file = File(gitDir, ngrokConfig.git!!.fileName)
        val lines = FileUtils.readLines(file, Charsets.UTF_8)
        val newLines = mutableListOf<String>()
        var flag = false
        lines.forEach { line ->
            val arr = line.split("=")
            if (arr.size == 2) {
                if (arr[0].trim() == port.toString()) {
                    newLines.add("$port=$url")
                    flag = true
                } else {
                    newLines.add(line)
                }
            }
        }
        if (!flag)
            newLines.add("$port=$url")
        if (newLines.isEmpty()) {
            newLines.add("empty")
        }
        logger.info("--> begin update file")
        newLines.forEach {
            logger.info("--> $it")
        }
        logger.info("--> end update file")
        FileUtils.writeLines(file, Charsets.UTF_8.name(), newLines)

        if (!lines.equals(newLines)) {
            gitRepository.commit("$port=$url")
        }
    }

    override fun destroy() {
        if (process != null) {
            process!!.destroy()
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}


