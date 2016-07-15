package com.magnusjason.githubprstats

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<UserNode, String> {

    fun findByUserName(userName: String): UserNode?

}
