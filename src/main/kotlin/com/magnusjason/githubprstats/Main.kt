package com.magnusjason.githubprstats

import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args : Array<String>) {
    val context = SpringApplicationBuilder()
            .headless(true)
            .web(false)
            .bannerMode(Banner.Mode.OFF)
            .sources(Application::class.java)
            .build()
            .run(*args)
    val prs = context.getBean(PullRequestSucker::class.java)
    prs.analyzeOrg("luxbet")
    context.close()
}