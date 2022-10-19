package com.awsmovie.controller

import com.awsmovie.controller.response.BaseResponse
import com.awsmovie.controller.response.ListResponse
import com.awsmovie.entity.movie.genre.GenreCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/aws-movie-api/v1")
class GenreController {

    @GetMapping("/genres")
    fun genres(): ResponseEntity<BaseResponse> {
        val result = GenreCode.values()

        val res = ListResponse(
            HttpStatus.OK.value(),
            HttpStatus.OK,
            "장르 조회 성공",
            result.size,
            result.toList()
        )

        return ResponseEntity(res, res.status)
    }

}