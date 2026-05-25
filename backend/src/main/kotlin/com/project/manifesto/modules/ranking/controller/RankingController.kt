package com.project.manifesto.modules.ranking.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.ranking.service.RankingService
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.entity.PostType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ranking")
@Tag(name = "Ranking", description = "Ranking feed APIs")
class RankingController(
    private val rankingService: RankingService
) {

    @GetMapping("/hot")
    @Operation(summary = "Get hot posts (HN style ranking)")
    fun getHot(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) type: PostType?
    ): ResponseEntity<ApiResponse<List<PostResponse>>> {
        val result = rankingService.getHotPosts(PageRequest.of(page, size), type)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/new")
    @Operation(summary = "Get newest posts")
    fun getNew(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) type: PostType?
    ): ResponseEntity<ApiResponse<List<PostResponse>>> {
        val result = rankingService.getNewPosts(PageRequest.of(page, size), type)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/top")
    @Operation(summary = "Get top posts by score")
    fun getTop(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) type: PostType?
    ): ResponseEntity<ApiResponse<List<PostResponse>>> {
        val result = rankingService.getTopPosts(PageRequest.of(page, size), type)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
