package com.project.manifesto.infra.redis

interface LockService {
    fun tryLock(key: String, ttlSeconds: Long = 10): LockToken?
    fun unlock(lockToken: LockToken): Boolean

    data class LockToken(
        val key: String,
        val token: String
    )
}
