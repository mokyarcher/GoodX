package team.sharex.goodx.model

data class Comment(
    val id: String? = null,
    val content: String? = null,
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val user: Author? = null,
    val parentId: String? = null,
    val replies: List<Comment>? = emptyList()
)
