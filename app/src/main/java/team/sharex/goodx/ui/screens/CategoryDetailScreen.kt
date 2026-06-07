package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

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
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("← 返回", fontSize = 14.sp)
            }
            Text(
                text = "${category.iconEmoji()} ${category.displayName()}",
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
                items(goodItems) { item ->
                    GoodItemCard(item = item, onClick = { onGoodItemClick(item.id) })
                }
            }
        }
    }
}
