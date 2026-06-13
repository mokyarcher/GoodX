package team.sharex.goodx.model

data class GoodItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val contentType: ContentType = ContentType.GOODS,
    val category: Category,
    val subCategory: String? = null,
    val images: List<String>? = emptyList(),
    val link: String? = null,
    val author: Author? = null,
    val likes: Int = 0,
    val likedBy: List<String>? = emptyList(),
    val latestInteraction: LatestInteraction? = null,
    val comments: List<Comment>? = emptyList(),
    val commentsCount: Int = 0,
    val status: String = "active",
    val removeReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class LatestInteraction(
    val type: String = "",     // "comment" or "like"
    val user: InteractionUser? = null
)

data class InteractionUser(
    val nickname: String? = null
)

data class Author(
    val id: String? = null,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null
)

enum class ContentType {
    GOODS,
    MOMENTS,
    ENTERTAINMENT
}

fun ContentType.displayName(): String = when (this) {
    ContentType.GOODS -> "好物"
    ContentType.MOMENTS -> "此刻"
    ContentType.ENTERTAINMENT -> "文娱"
}

fun ContentType.iconEmoji(): String = when (this) {
    ContentType.GOODS -> "✨"
    ContentType.MOMENTS -> "🌿"
    ContentType.ENTERTAINMENT -> "🎬"
}

fun ContentType.subtitle(): String = when (this) {
    ContentType.GOODS -> "值得入手的东西"
    ContentType.MOMENTS -> "值得记录的瞬间"
    ContentType.ENTERTAINMENT -> "值得体验的作品"
}

enum class Category {
    ELECTRONICS,          // 电子数码
    LIFESTYLE,            // 生活日用
    FASHION,              // 服饰穿搭
    SOFTWARE,             // 软件工具
    SUBSCRIPTION,         // 订阅服务
    OTHER_GOODS,          // 其他好物
    SCENERY,              // 风景
    CITY,                 // 城市
    TRAVEL,               // 旅行
    DAILY,                // 日常
    PLACE,                // 地点
    OTHER_MOMENTS,        // 其他此刻
    MOVIE,                // 电影
    SERIES,               // 剧集
    MUSIC,                // 音乐
    BOOK,                 // 阅读
    GAME,                 // 游戏
    ANIME,                // 动漫
    PODCAST,              // 播客
    OTHER_ENTERTAINMENT   // 其他文娱
}

fun Category.displayName(): String = when (this) {
    Category.ELECTRONICS -> "电子数码"
    Category.LIFESTYLE -> "生活日用"
    Category.FASHION -> "服饰穿搭"
    Category.SOFTWARE -> "软件工具"
    Category.SUBSCRIPTION -> "订阅服务"
    Category.OTHER_GOODS -> "其他好物"
    Category.SCENERY -> "风景"
    Category.CITY -> "城市"
    Category.TRAVEL -> "旅行"
    Category.DAILY -> "日常"
    Category.PLACE -> "地点"
    Category.OTHER_MOMENTS -> "其他此刻"
    Category.MOVIE -> "电影"
    Category.SERIES -> "剧集"
    Category.MUSIC -> "音乐"
    Category.BOOK -> "阅读"
    Category.GAME -> "游戏"
    Category.ANIME -> "动漫"
    Category.PODCAST -> "播客"
    Category.OTHER_ENTERTAINMENT -> "其他文娱"
}

fun Category.iconEmoji(): String = when (this) {
    Category.ELECTRONICS -> "📱"
    Category.LIFESTYLE -> "🏠"
    Category.FASHION -> "👔"
    Category.SOFTWARE -> "💻"
    Category.SUBSCRIPTION -> "📦"
    Category.OTHER_GOODS -> "✨"
    Category.SCENERY -> "🌄"
    Category.CITY -> "🏙️"
    Category.TRAVEL -> "🧳"
    Category.DAILY -> "☀️"
    Category.PLACE -> "📍"
    Category.OTHER_MOMENTS -> "🌿"
    Category.MOVIE -> "🎬"
    Category.SERIES -> "📺"
    Category.MUSIC -> "🎵"
    Category.BOOK -> "📚"
    Category.GAME -> "🎮"
    Category.ANIME -> "🧩"
    Category.PODCAST -> "🎙️"
    Category.OTHER_ENTERTAINMENT -> "🎭"
}

fun Category.description(): String = when (this) {
    Category.ELECTRONICS -> "手机、电脑、耳机、智能设备"
    Category.LIFESTYLE -> "家居、厨房、户外、文具"
    Category.FASHION -> "服装、鞋靴、配饰"
    Category.SOFTWARE -> "生产力、设计、开发、效率工具"
    Category.SUBSCRIPTION -> "流媒体、云服务、会员、数字服务"
    Category.OTHER_GOODS -> "其他值得推荐的好物"
    Category.SCENERY -> "自然风景、天空、山海、季节景色"
    Category.CITY -> "城市角落、建筑、街景、夜景"
    Category.TRAVEL -> "旅途记录、目的地、路线体验"
    Category.DAILY -> "生活日常、随手记录、瞬间心情"
    Category.PLACE -> "店铺、展览、空间、公园、校园"
    Category.OTHER_MOMENTS -> "其他值得记录的瞬间"
    Category.MOVIE -> "电影、纪录片、长片"
    Category.SERIES -> "电视剧、网剧、综艺、番剧"
    Category.MUSIC -> "歌曲、专辑、歌单、音乐人"
    Category.BOOK -> "小说、非虚构、漫画、文章"
    Category.GAME -> "Steam、主机、手游、独立游戏"
    Category.ANIME -> "动画、漫画、ACG 内容"
    Category.PODCAST -> "播客、电台、音频节目"
    Category.OTHER_ENTERTAINMENT -> "其他文娱作品"
}

fun ContentType.categories(): List<Category> = when (this) {
    ContentType.GOODS -> listOf(
        Category.ELECTRONICS,
        Category.LIFESTYLE,
        Category.FASHION,
        Category.SOFTWARE,
        Category.SUBSCRIPTION,
        Category.OTHER_GOODS
    )
    ContentType.MOMENTS -> listOf(
        Category.SCENERY,
        Category.CITY,
        Category.TRAVEL,
        Category.DAILY,
        Category.PLACE,
        Category.OTHER_MOMENTS
    )
    ContentType.ENTERTAINMENT -> listOf(
        Category.MOVIE,
        Category.SERIES,
        Category.MUSIC,
        Category.BOOK,
        Category.GAME,
        Category.ANIME,
        Category.PODCAST,
        Category.OTHER_ENTERTAINMENT
    )
}

fun Category.defaultContentType(): ContentType = when (this) {
    Category.SCENERY,
    Category.CITY,
    Category.TRAVEL,
    Category.DAILY,
    Category.PLACE,
    Category.OTHER_MOMENTS -> ContentType.MOMENTS
    Category.MOVIE,
    Category.SERIES,
    Category.MUSIC,
    Category.BOOK,
    Category.GAME,
    Category.ANIME,
    Category.PODCAST,
    Category.OTHER_ENTERTAINMENT -> ContentType.ENTERTAINMENT
    else -> ContentType.GOODS
}
