package com.github.xsi640

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.xsi640.config.NgrokConfig
import com.github.xsi640.config.TunnelsConfig
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
import java.net.URL


@SpringBootApplication
class Application : CommandLineRunner, DisposableBean {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    @Autowired
    private lateinit var ngrokConfig: NgrokConfig

    private lateinit var gitRepository: GitRepository
    private lateinit var gitDir: File
    private lateinit var loggerFileMonitor: LoggerFileMonitor
    private var process: Process? = null
    private var tunnelsMap = mutableMapOf<String, Int>()

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
        if (!config.exists()) {
            this.javaClass.getResourceAsStream("/config.yml").use { src ->
                IOUtils.copy(src, FileOutputStream(config))
            }
        }

        val mapper = ObjectMapper(YAMLFactory())
        val tunnelsConfig = mapper.readValue(config, TunnelsConfig::class.java)
        tunnelsConfig.tunnels.forEach { k, v ->
            tunnelsMap[k] = v.addr
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
            map.containsKey("url") && map["url"]!!.isNotEmpty() &&
            map.containsKey("name") && map["name"]!!.isNotEmpty()
        ) {
            val name = map["name"]!!.replace("\"", "")
            if (tunnelsMap.containsKey(name)) {
                val url = URL(map["url"])
                writeGitFile(name, url.toString())
            }
        }
    }

    fun writeGitFile(name: String, url: String) {
        gitRepository.pull()
        val file = File(gitDir, ngrokConfig.git!!.fileName)
        val mapper = ObjectMapper()

        val map = try {
            mapper.readValue(file, object : TypeReference<Map<String, String>>() {}).toMutableMap()
        } catch (e: JsonParseException) {
            mutableMapOf()
        }
        if (map.containsKey(name) && map[name] == url) {
            return
        }
        map[name] = url
        logger.info("--> begin update file")
        logger.info(mapper.writeValueAsString(map))
        logger.info("--> end update file")
        mapper.writeValue(file, map)
        gitRepository.commit("$name=$url")
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


