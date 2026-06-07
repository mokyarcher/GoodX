package team.sharex.goodx.model

data class User(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val inviteCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
