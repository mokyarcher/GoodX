package team.sharex.goodx.model

data class GoodItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: Category,
    val subCategory: String? = null,
    val images: List<String> = emptyList(),
    val link: String? = null,
    val author: Author? = null,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Author(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null
)

enum class Category {
    ELECTRONICS,    // 电子数码
    LIFESTYLE,      // 生活日用
    FASHION,        // 服饰穿搭
    GAME,           // 游戏
    MOVIE,          // 影视
    BOOK,           // 阅读
    SOFTWARE,       // 软件工具
    SUBSCRIPTION    // 订阅服务
}

fun Category.displayName(): String = when (this) {
    Category.ELECTRONICS -> "电子数码"
    Category.LIFESTYLE -> "生活日用"
    Category.FASHION -> "服饰穿搭"
    Category.GAME -> "游戏"
    Category.MOVIE -> "影视"
    Category.BOOK -> "阅读"
    Category.SOFTWARE -> "软件工具"
    Category.SUBSCRIPTION -> "订阅服务"
}

fun Category.iconEmoji(): String = when (this) {
    Category.ELECTRONICS -> "📱"
    Category.LIFESTYLE -> "🏠"
    Category.FASHION -> "👔"
    Category.GAME -> "🎮"
    Category.MOVIE -> "🎬"
    Category.BOOK -> "📚"
    Category.SOFTWARE -> "💻"
    Category.SUBSCRIPTION -> "📦"
}
