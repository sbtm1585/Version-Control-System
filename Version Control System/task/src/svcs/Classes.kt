package svcs

import java.io.File
import java.security.*
import kotlin.io.path.*

class VCS(private val args: Array<String>) {

    private val rootDir = File("vcs").also { it.mkdir() }
    private val commitsDir = File("$rootDir\\commits").also { it.mkdir() }
    private val indexFile = Path("$rootDir\\index.txt")
    private val logFile = Path("$rootDir\\log.txt")
    private val configFile = Path("$rootDir\\config.txt")
    private val indexFileList = buildList { if (indexFile.exists()) indexFile.forEachLine { add(it) } }
    private val oldCommitIDs = Path(".\\old_IDs.txt").also { if (it.notExists()) it.createFile() }
    private val newCommitIDs = Path(".\\new_IDs.txt").also { if (it.notExists()) it.createFile() }

    fun print(arg: String?) {
        println(
            when (arg)  {
                "--help", null -> HELP
                else -> {
                    if (arg !in commandsMap) {
                        "\'$arg\' is not a SVCS command."
                    } else {
                        commandsMap.getValue(arg)
                    }
                }
            }
        )
    }

    fun config() {
        when (args.size) {
            1 -> {
                if (configFile.exists()) {
                    println("The username is ${configFile.readText()}.")
                } else {
                    println("Please, tell me who you are.")
                }
            }
            2 -> {
                println("The username is ${args[1]}.")
                if (configFile.notExists()) configFile.createFile()
                configFile.writeText(args[1])
            }
        }
    }

    fun add() {
        when (args.size) {
            1 -> {
                if (indexFile.exists()) {
                    println("Tracked files:\n${indexFile.readText()}")
                } else {
                    println("Add a file to the index.")
                }
            }
            2 -> {
                if (indexFile.notExists()) indexFile.createFile()
                if (existing(args[1])) {
                    println("The file \'${args[1]}\' is tracked.")
                    indexFile.appendText("${args[1]}\n")
                    oldCommitIDs.writeText("")
                    indexFile.forEachLine {
                        val commitID = File(it).getMD5()
                        oldCommitIDs.appendText("$it $commitID\n")
                    }
                } else {
                    println("Can't find \'${args[1]}\'.")
                }
            }
        }
    }

    fun log(hash: String = "", message: String = "") {
        if (logFile.notExists()) {
            println("No commits yet.")
        } else {
            if (hash.isEmpty() && message.isEmpty()) {
                logFile.forEachLine { println(it) }
            } else {
                val prevLog = buildList { logFile.forEachLine { add("$it\n") } }
                logFile.writeText("")
                logFile.writeText("commit $hash\nAuthor: ${configFile.readText()}\n$message\n\n")
                prevLog.forEach { logFile.appendText(it) }
            }
        }
    }

    fun commit() {
        when (args.size) {
            1 -> println("Message was not passed.")
            2 -> {
                newCommitIDs.writeText("")
                indexFileList.forEach { newCommitIDs.appendText("$it ${File(it).getMD5()}\n") }
                if (logFile.notExists()) logFile.createFile()
                val diff = getDifferences(oldCommitIDs.toFile(), newCommitIDs.toFile())
                if (diff.isNotEmpty() || logFile.readText().isBlank()) {
                    val commitHash = args[1].getMD5()
                    val targetDir = File("$commitsDir\\$commitHash\\")
                    targetDir.mkdir()
                    indexFileList.forEach { File(it).copyTo(File("$targetDir\\$it"), overwrite = true) }
                    log(commitHash, args[1])
                    println("Changes are committed.")
                } else {
                    println("Nothing to commit.")
                }
            }
        }
    }

    fun checkout() {
        when (args.size) {
            1 -> println("Commit id was not passed.")
            2 -> {
                val directory = Path("$commitsDir\\${args[1]}\\")
                if (directory.notExists()) {
                    println("Commit does not exist.")
                } else {
                    directory.forEachDirectoryEntry { Path(it.toString()).copyTo(Path(it.name), overwrite = true) }
                    println("Switched to commit ${args[1]}.")
                }
            }
        }
    }

    private fun getDifferences(oldMD5: File, newMD5: File): List<List<String>> {
        val oldCommits = buildList { oldMD5.forEachLine { add(it.split(" ")) } }
        val newCommits = buildList { newMD5.forEachLine { add(it.split(" ")) } }
        return newCommits.filter { it !in oldCommits }
    }

    private fun File.getMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun String.getMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun existing(file: String): Boolean {
        val files = File(".\\")
        return files.walkTopDown().find { it.name == file }?.isFile == true
    }
}