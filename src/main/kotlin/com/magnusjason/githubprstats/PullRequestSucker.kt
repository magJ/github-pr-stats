package com.magnusjason.githubprstats

import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.IssueService
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.egit.github.core.service.RepositoryService
import org.neo4j.graphdb.GraphDatabaseService
import java.util.concurrent.Executors

/**
 * TODO
 * count comments by pr author separately
 * comments with :ship: equivalent
 * size of comments
 * if comments contain code samples
 * conversations
 * response time to conversations
 * merges without shipit
 *
 *
 * https://github.com/spring-guides/gs-accessing-data-neo4j/blob/master/complete/src/main/java/hello/Application.java
 *
 */
class PullRequestSucker(
        ghClient: GitHubClient,
        val db: GraphDatabaseService,
        val userRepository: UserRepository,
        val repositoryRepository: RepositoryRepository,
        val pullRequestRepository: PullRequestRepository,
        val commentRepository: CommentRepository) {

    val repoService = RepositoryService(ghClient)
    val prService = PullRequestService(ghClient)
    val issueService = IssueService(ghClient)
    val executor = Executors.newFixedThreadPool(5)
    val userLock = Object()

    fun analyzePr(repo: Repository, pr: PullRequest): PullRequestNode {
        println("Analyzing pr ${repo.generateId()} ${pr.number}")
        val pullRequest = PullRequestNode()
        pullRequest.author = getOrCreateUser(pr.user.login)
        pullRequest.number = pr.number
        pullRequest.title = pr.title
        pullRequest.createdAt = pr.createdAt.time
        pullRequest.closedAt = pr.closedAt?.time
        pullRequest.updatedAt = pr.updatedAt.time

        issueService.getComments(pr.base.repo.owner.login, pr.base.repo.name, pr.number).forEach {
            val comment = CommentNode()
            comment.author = getOrCreateUser(it.user.login)
            comment.body = it.body
            comment.createdAt = it.createdAt.time
            comment.number = it.id
            commentRepository.save(comment)
            pullRequest.comments.plusAssign(comment)
        }

        prService.getComments(repo, pr.number).forEach {
            val comment = CommentNode()
            comment.author = getOrCreateUser(it.user.login)
            comment.body = it.body
            comment.createdAt = it.createdAt.time
            comment.position = it.position
            comment.number = it.id
            comment.line = it.line
            comment.path = it.path
            commentRepository.save(comment)
            pullRequest.comments.plusAssign(comment)
        }

        prService.getCommits(repo, pr.number).forEach {
            val commit = CommitNode()
        }

        pullRequestRepository.save(pullRequest)
        return pullRequest

    }

    fun analyzeRepository(repo: Repository){
        println("Analyzing repo ${repo.generateId()}")
        val repoNode = RepositoryNode()
        repoNode.name = repo.name
        repoNode.owner = getOrCreateUser(repo.owner.login)
        repoNode.language = repo.language
        prService.getPullRequests(repo, "all").forEach {
            val pullRequest = analyzePr(repo, it)
            repoNode.pullRequests.plusAssign(pullRequest)
        }
        repositoryRepository.save(repoNode)
    }

    fun analyzeOrg(org: String){
        println("Analyzing org $org")
        db.beginTx().use {
            repoService.getOrgRepositories(org).map {
                executor.submit {
                    analyzeRepository(it)
                }
            }.forEach { it.get() }
        }
    }

    fun getOrCreateUser(userName: String): UserNode {
        synchronized(userLock, {
            val user = userRepository.findByUserName(userName)
            if(user != null){
                return user
            } else {
                val newUser = UserNode()
                newUser.userName = userName
                userRepository.save(newUser)
                return newUser
            }
        })
    }
}