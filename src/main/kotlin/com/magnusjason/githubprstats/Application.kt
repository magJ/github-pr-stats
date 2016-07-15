package com.magnusjason.githubprstats

import org.apache.http.impl.client.cache.CacheConfig
import org.apache.http.impl.client.cache.CachingHttpClients
import org.eclipse.egit.github.core.client.GitHubClient
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.neo4j.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.config.Neo4jConfiguration
import java.nio.file.Paths


@Configuration
@EnableNeo4jRepositories
@Import(PropertyPlaceholderAutoConfiguration::class)
open class Application : Neo4jConfiguration() {

    init {
        setBasePackage("com.magnusjason.githubprstats")
    }

    @Bean
    open fun githubClient(@Value("\${github.oauth.token}") oauthToken: String): GitHubClient {
        val cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(100000)
                .setMaxObjectSize(1024 * 1024)
                .setSharedCache(false)
                .build()

        val httpClient = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setHttpCacheStorage(FileHttpCacheStorage(Paths.get("httpcache")))
                .build()


        val client = HcGithubClient(httpClient)//RateLimitedGitHubClient()
        client.setOAuth2Token(oauthToken)
        return client
    }

    @Bean
    open fun graphDatabaseService(): GraphDatabaseService {
        val db = GraphDatabaseFactory().newEmbeddedDatabase("neo4j.db")
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { db.shutdown() }))
        return db
    }

    @Bean
    open fun pullRequestSucker(
            ghClient: GitHubClient,
            db: GraphDatabaseService,
            userRepository: UserRepository,
            repositoryRepository: RepositoryRepository,
            pullRequestRepository: PullRequestRepository,
            commentRepository: CommentRepository): PullRequestSucker {
        return PullRequestSucker(
                ghClient,
                db,
                userRepository,
                repositoryRepository,
                pullRequestRepository,
                commentRepository)
    }

}




