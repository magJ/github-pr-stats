package com.magnusjason.githubprstats

import org.apache.http.HttpRequest
import org.apache.http.client.cache.HttpCacheContext
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.GitHubRequest
import org.eclipse.egit.github.core.client.GitHubResponse
import java.io.InputStream
import java.net.HttpURLConnection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HcGithubClient(val httpClient : CloseableHttpClient) : GitHubClient() {

    var token : String? = null
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

    override fun get(request: GitHubRequest): GitHubResponse? {
        val response = getInternal(request)
        val code = response.statusLine.statusCode
        if(isOk(code)){
            return HcGithubResponse(response, getBody(request, response.entity.content))
        }
        if(isEmpty(code)){
            return HcGithubResponse(response, null)
        }
        throw createException(response.entity.content, code, response.statusLine.reasonPhrase)
    }

    fun getInternal(request: GitHubRequest): CloseableHttpResponse {
        val uri = super.createUri(request.generateUri());
        val getRequest = HttpGet(uri)
        configureRequest(getRequest)
        val accept = request.responseContentType
        if(accept != null){
            getRequest.addHeader(HEADER_ACCEPT, accept)
        }
        val context = HttpCacheContext.create();
        val response = httpClient.execute(getRequest, context)
        println("Cache hit status: ${context.cacheResponseStatus.name}")
        updateRateLimits(MockHttpURLConnection(response))

        return response
    }

    fun configureRequest(request : HttpRequest){
        if (token != null){
            request.setHeader(HEADER_AUTHORIZATION, token)
        }
        request.setHeader(HEADER_USER_AGENT, USER_AGENT)
        request.setHeader(HEADER_ACCEPT, "application/vnd.github.beta+json")
    }

    override fun getStream(request: GitHubRequest): InputStream? {
        val response = getInternal(request)
        val code = response.statusLine.statusCode
        if(isOk(code)){
            return response.entity.content
        } else {
            throw createException(response.entity.content, code, response.statusLine.reasonPhrase)
        }
    }

    override fun setOAuth2Token(token: String?): GitHubClient? {
        this.token = "token " + token
        return super.setOAuth2Token(token)
    }
}

class HcGithubResponse(val httpResponse: CloseableHttpResponse, body: Any?) : GitHubResponse(null, body) {

    override fun getHeader(name: String?): String? {
        return httpResponse.getFirstHeader(name).value
    }
}

class MockHttpURLConnection(val httpResponse: CloseableHttpResponse) : HttpURLConnection(null) {

    override fun getHeaderField(name: String?): String? {
        return httpResponse.getFirstHeader(name).value
    }

    override fun usingProxy(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun disconnect() {
        throw UnsupportedOperationException()
    }

    override fun connect() {
        throw UnsupportedOperationException()
    }

}