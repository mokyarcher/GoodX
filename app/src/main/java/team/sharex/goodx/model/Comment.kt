package team.sharex.goodx.model

data class Comment(
    val id: String,
    val content: String,
    val createdAt: Long,
    val user: Author? = null
)
