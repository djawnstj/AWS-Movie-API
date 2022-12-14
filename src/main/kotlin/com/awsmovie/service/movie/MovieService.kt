package com.awsmovie.service.movie

import com.awsmovie.controller.dto.movie.GenreDto
import com.awsmovie.controller.dto.movie.MovieDto
import com.awsmovie.controller.dto.movie.MovieRateDto
import com.awsmovie.controller.dto.user.UserDto
import com.awsmovie.controller.handler.ErrorCode
import com.awsmovie.entity.movie.Movie
import com.awsmovie.entity.movie.MovieImage
import com.awsmovie.entity.movie.genre.GenreCode
import com.awsmovie.entity.movie.genre.MovieGenre
import com.awsmovie.exception.nonExistent.NonExistentMovieException
import com.awsmovie.repository.genre.GenreRepository
import com.awsmovie.repository.movie.MovieImageRepository
import com.awsmovie.repository.movie.MovieRepository
import com.awsmovie.repository.movieGenre.MovieGenreRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MovieService(
    private val movieRepository: MovieRepository,
    private val genreRepository: GenreRepository,
    private val movieGenreRepository: MovieGenreRepository,
    private val movieImageRepository: MovieImageRepository
) {

    /**
     * 영화 저장
     */
    @Transactional
    fun saveMovie(movieName: String, runTime: Int, openingDate: LocalDateTime, summary: String, genres: List<GenreCode>, multipartFile: MultipartFile): Movie {

        val imagePath = movieImageRepository.uploadToS3(multipartFile)

        val genreList = genreRepository.findByGenreIn(genres)

        // 영화 이미지 객체 생성
        val movieImage = MovieImage.createMovieImage(imagePath)

        // 영화 객체 생성
        val movie = Movie.createMovie(movieName, runTime, openingDate, summary, movieImage)

        movieRepository.save(movie)

        genreList.forEach { genre ->
            movieGenreRepository.save(MovieGenre.create(genre, movie))
        }

        return movie
    }

    /**
     * 영화 평점순 조회
     */
    fun getMovieListByRating(pageable: Pageable): List<MovieDto> {

        val movies = movieRepository.findAll(pageable)

        val result = mutableListOf<MovieDto>()

        movies.forEach { movie ->

            val genres = mutableListOf<GenreDto>()
            val rates = mutableListOf<MovieRateDto>()

            movie.genres.forEach {
                it.movieGenreGenre.genre.apply {
                    genres += GenreDto(genreCode, genreKrName, genreEnName)
                }
            }

            movie.rates.forEach {
                rates += MovieRateDto(
                    it.movieRateId ?: -1,
                    UserDto(it.user.uid, it.user.userName, it.user.userId, it.user.userPw),
                    movie.movieId ?: -1,
                    it.rate,
                    it.comment
                )
            }

            result += MovieDto(
                movieId = movie.movieId ?: -1,
                movieName= movie.movieName,
                runTime = movie.runTime,
                openingDate = movie.openingDate,
                summary = movie.summary,
                genres = genres,
                movieImagePath = movie.movieImage.imagePath,
                rates = rates)
        }

        return result
    }

    /**
     * 영화 ID로 영화 조회
     */
    fun findMovieInfo(movieId: Long): MovieDto {

        val movie = movieRepository.findByIdOrNull(movieId)

        check(movie != null) { throw NonExistentMovieException(ErrorCode.MOVIE_NOT_FOUND) }

        movie.apply {

            val genreList = mutableListOf<GenreDto>()
            genres.forEach {
                it.movieGenreGenre.genre.apply {
                    genreList += GenreDto(genreCode, genreKrName, genreEnName)
                }
            }

            val rateList = mutableListOf<MovieRateDto>()

            rates.forEach {
                rateList += MovieRateDto(
                    it.movieRateId ?: -1,
                    UserDto(it.user.uid, it.user.userName, it.user.userId, it.user.userPw),
                    movieId,
                    it.rate,
                    it.comment
                )
            }

            return  MovieDto(
                movieId = movieId,
                movieName = movieName,
                runTime = runTime,
                openingDate = openingDate,
                summary = summary,
                genres = genreList,
                movieImagePath = movieImage.imagePath,
                rates = rateList,
            )

        }

    }

}