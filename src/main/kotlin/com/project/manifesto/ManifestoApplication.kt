package com.project.manifesto

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ManifestoApplication

fun main(args: Array<String>) {
    runApplication<ManifestoApplication>(*args)
}
