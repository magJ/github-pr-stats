package com.magnusjason.githubprstats

import com.integralblue.httpresponsecache.HttpResponseCache
import org.eclipse.egit.github.core.client.GitHubClient
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.config.Neo4jConfiguration
import java.io.File


fun main(args : Array<String>) {
    HttpResponseCache.install(File("httpcache"), 100 * 1024 * 1024) //100 MB
    val context = SpringApplication.run(Application::class.java, *args)
    val prs = context.getBean(PullRequestSucker::class.java)
    prs.analyzeOrg("luxbet")
}


@SpringBootApplication
@Configuration
@EnableNeo4jRepositories
open class Application : Neo4jConfiguration() {

    init {
        setBasePackage("com.magnusjason.githubprstats")
    }

    @Bean
    open fun githubClient(@Value("github.oauth.token") oauthToken: String): GitHubClient {
        val client = RateLimitedGitHubClient()
        client.setOAuth2Token(oauthToken)
        return client
    }

    @Bean
    open fun graphDatabaseService(): GraphDatabaseService {
        return GraphDatabaseFactory().newEmbeddedDatabase("neo4j.db")
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




