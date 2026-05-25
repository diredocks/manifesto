package com.project.manifesto.modules.vote.event

import com.project.manifesto.infra.rabbitmq.RabbitConfig
import com.project.manifesto.modules.ranking.service.RankingService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class PostVotedConsumer(
    private val rankingService: RankingService
) {

    @RabbitListener(queues = [RabbitConfig.QUEUE_POST_VOTED])
    fun handlePostVoted(event: PostVotedEvent) {
        rankingService.recalculatePostScore(event.postId)
    }
}
