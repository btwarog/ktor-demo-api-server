package com.example.quotes

import com.example.common.exceptions.*
import com.example.common.exceptions.NotFoundException
import com.example.common.utils.*
import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.quoteRoutes(quoteService: QuoteService, imageUploadService: ImageUploadService) {
    route("/api/v1") {
        route("/quotes") {
            
            post({
                description = "Create a new quote"
            }) {
                try {
                    val multipart = call.receiveMultipart()
                    var content: String? = null
                    var author: String? = null
                    var imageUrl: String? = null
                    var category: String? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                when (part.name) {
                                    "content" -> content = part.value
                                    "author" -> author = part.value
                                    "category" -> category = part.value
                                }
                            }

                            is PartData.FileItem -> {
                                imageUrl = imageUploadService.saveImage(part)
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    if (content != null && author != null) {
                        val quote = Quote(0, content!!, author!!, imageUrl, category)
                        val createdQuote = quoteService.createQuote(quote)
                        call.respond(HttpStatusCode.Created, createdQuote)
                    } else {
                        call.respondError(
                            HttpStatusCode.BadRequest,
                            "Missing content or author",
                            "INVALID_INPUT",
                            mapOf(
                                "content" to (content ?: "missing"),
                                "author" to (author ?: "missing")
                            )
                        )
                    }
                } catch (e: BadRequestException) {
                    call.respondError(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Invalid file",
                        "INVALID_FILE"
                    )
                }
            }

            get({
                description = "Get all quotes with pagination"
            }) {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

                val quotesResponse = quoteService.getAllQuotes(page, pageSize)
                call.respond(HttpStatusCode.OK, quotesResponse)
            }


            get("/{id}", {
                description = "Get a specific quote by ID"
            }) {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid ID format")

                val quote = quoteService.getQuoteById(id)
                    ?: throw NotFoundException("Quote not found")

                call.respond(quote)
            }

            authenticate {
                

                post({
                    description = "Create a new quote (admin only)"
                }) {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal!!.payload.getClaim("role").asString()
                    if (role != "ADMIN") {
                        throw ForbiddenException("Only admins can create quotes")
                    }

                    val quote = call.receive<Quote>()
                    val createdQuote = quoteService.createQuote(quote)
                    call.respond(HttpStatusCode.Created, createdQuote)
                }


                put("/{id}", {
                    description = "Update an existing quote (admin only)"
                }) {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal!!.payload.getClaim("role").asString()
                    if (role != "ADMIN") {
                        call.respondError(
                            HttpStatusCode.Forbidden,
                            "Only admins can update quotes",
                            "INSUFFICIENT_PERMISSIONS"
                        )
                        return@put
                    }

                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respondError(
                            HttpStatusCode.BadRequest,
                            "Invalid ID format",
                            "INVALID_ID_FORMAT",
                            mapOf("id" to (call.parameters["id"] ?: "missing"))
                        )
                        return@put
                    }

                    val updatedQuote = call.receive<Quote>()
                    if (quoteService.updateQuote(id, updatedQuote)) {
                        call.respond(HttpStatusCode.OK, "Quote updated successfully")
                    } else {
                        call.respondError(
                            HttpStatusCode.NotFound,
                            "Quote not found",
                            "QUOTE_NOT_FOUND",
                            mapOf("id" to id)
                        )
                    }
                }


                delete("/{id}", {
                    description = "Delete a quote (admin only)"
                }) {
                    val principal = call.principal<JWTPrincipal>()
                    val role = principal!!.payload.getClaim("role").asString()
                    if (role != "ADMIN") {
                        call.respondError(
                            HttpStatusCode.Forbidden,
                            "Only admins can delete quotes",
                            "INSUFFICIENT_PERMISSIONS"
                        )
                        return@delete
                    }

                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respondError(
                            HttpStatusCode.BadRequest,
                            "Invalid ID format",
                            "INVALID_ID_FORMAT",
                            mapOf("id" to (call.parameters["id"] ?: "missing"))
                        )
                        return@delete
                    }

                    if (quoteService.deleteQuote(id)) {
                        call.respond(HttpStatusCode.OK, "Quote deleted successfully")
                    } else {
                        call.respondError(
                            HttpStatusCode.NotFound,
                            "Quote not found",
                            "QUOTE_NOT_FOUND",
                            mapOf("id" to id)
                        )
                    }
                }
            }
        }

        get("/category/{category}", {
            description = "Get quotes by category with pagination"
        }) {
            val category =
                call.parameters["category"] ?: throw IllegalArgumentException("Missing category")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

            val quotes = quoteService.getQuotesByCategory(category, page, pageSize)
            val totalQuotes = quoteService.getTotalQuotesByCategory(category)
            val totalPages = (totalQuotes + pageSize - 1) / pageSize

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "quotes" to quotes,
                    "page" to page,
                    "pageSize" to pageSize,
                    "totalQuotes" to totalQuotes,
                    "totalPages" to totalPages,
                    "category" to category
                )
            )
        }
    }
}