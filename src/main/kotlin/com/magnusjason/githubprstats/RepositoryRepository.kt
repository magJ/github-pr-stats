package com.magnusjason.githubprstats

import org.springframework.data.repository.CrudRepository

interface RepositoryRepository : CrudRepository<RepositoryNode, String> {

    fun findByName(name: String): RepositoryNode

}
