package com.magnusjason.githubprstats

import org.neo4j.graphdb.Transaction

/**
 * Function to automatically commit and close a neo4j transaction
 * Adapted from kotlin's closable#use
 */
inline fun <T : Transaction, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        val a = block(this)
        this.success()
        return a
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {

        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}