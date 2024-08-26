package svcs

fun main(args: Array<String>) {
    val vcs = VCS(args)
    when (val arg = args.firstOrNull()) {
        "config" -> vcs.config()
        "add" -> vcs.add()
        "commit" -> vcs.commit()
        "log" -> vcs.log()
        "checkout" -> vcs.checkout()
        else -> vcs.print(arg)
    }
}

