package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.categories
import team.sharex.goodx.model.defaultContentType
import team.sharex.goodx.model.description
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.model.subtitle
import team.sharex.goodx.ui.components.LiquidGlassCard
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun ContentTypeDetailScreen(
    contentType: ContentType,
    onBack: () -> Unit,
    onCategoryClick: (Category) -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Text(
            text = "${contentType.iconEmoji()} ${contentType.displayName()}",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 8.dp)
        )
        Text(
            text = contentType.subtitle(),
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contentType.categories()) { category ->
                CategoryGridCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    category: Category,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .height(132.dp)
            .clickable { onClick() },
        cornerRadius = 22.dp,
        tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = category.iconEmoji(), fontSize = 30.sp)
            Column {
                Text(
                    text = category.displayName(),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.description(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun ContentTypePostsScreen(
    contentType: ContentType,
    onBack: () -> Unit,
    onGoodItemClick: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    var goodItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getGoodItems(
                    contentType = contentType.name,
                    sort = "newest"
                )
                if (response.isSuccessful) goodItems = response.body() ?: emptyList()
            } catch (e: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(contentType) { loadItems() }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${contentType.iconEmoji()} ${contentType.displayName()}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
        else if (goodItems.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("暂无内容", color = TextSecondary, fontSize = 16.sp); Text("去发布第一个吧", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) } }
        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(goodItems, key = { it.id }) { item ->
                GoodItemCard(item = item, onClick = { cacheGoodItemPreview(item); onGoodItemClick(item.id) })
            }
        }
    }
}

@Composable
fun CategoryDetailScreen(
    category: Category,
    onBack: () -> Unit,
    onGoodItemClick: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    var goodItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getGoodItems(
                    category = category.name,
                    sort = "newest"
                )
                if (response.isSuccessful) {
                    goodItems = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }
            isLoading = false
        }
    }

    LaunchedEffect(category) { loadItems() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${category.defaultContentType().displayName()} · ${category.displayName()}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (goodItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("该品类暂无好物", color = TextSecondary, fontSize = 16.sp)
                    Text("去发布第一个吧", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goodItems, key = { it.id }) { item ->
                    GoodItemCard(
                        item = item,
                        onClick = {
                            cacheGoodItemPreview(item)
                            onGoodItemClick(item.id)
                        }
                    )
                }
            }
        }
    }
}
