package com.project.manifesto.infra.redis

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
@Profile("!test & !generate")
class RedisLockService(
    private val redisTemplate: StringRedisTemplate
) : LockService {

    override fun tryLock(key: String, ttlSeconds: Long): LockService.LockToken? {
        val token = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, token, Duration.ofSeconds(ttlSeconds))
            ?: false
        return if (acquired) LockService.LockToken(key, token) else null
    }

    override fun unlock(lockToken: LockService.LockToken): Boolean {
        val currentValue = redisTemplate.opsForValue().get(lockToken.key)
        return if (currentValue == lockToken.token) {
            redisTemplate.delete(lockToken.key)
            true
        } else {
            false
        }
    }
}
