package com.magnusjason.githubprstats

import org.eclipse.egit.github.core.client.GitHubClient
import java.net.HttpURLConnection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RateLimitedGitHubClient : GitHubClient() {

    var resetTime : Long = -1
    val pendingRequests = AtomicInteger()

    override fun createConnection(uri: String?): HttpURLConnection? {
        val totalRemainingRequests = remainingRequests + pendingRequests.andDecrement
        if(remainingRequests != -1 && totalRemainingRequests < 20){
            println("Remaining Requests: $remainingRequests, Request Limit: $requestLimit")
            val secondsRemaining = resetTime - Instant.now().epochSecond
            val resetTimeFormatted = DateTimeFormatter.ISO_DATE_TIME
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(resetTime))
            println("Sleeping until $resetTimeFormatted")
            TimeUnit.SECONDS.sleep(secondsRemaining)
        }
        return super.createConnection(uri)
    }

    override fun updateRateLimits(request: HttpURLConnection): GitHubClient? {
        pendingRequests.set(0)
        resetTime = try{
             request.getHeaderField("X-RateLimit-Reset").toLong()
        } catch (e : NumberFormatException){
            -1
        }
        return super.updateRateLimits(request)
    }

}