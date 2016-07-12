package com.magnusjason.githubprstats

import org.springframework.data.repository.CrudRepository

interface PullRequestRepository : CrudRepository<PullRequestNode, String>
