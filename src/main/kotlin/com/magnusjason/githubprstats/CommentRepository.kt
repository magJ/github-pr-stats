package com.magnusjason.githubprstats

import org.springframework.data.repository.CrudRepository

interface CommentRepository : CrudRepository<CommentNode, String>
