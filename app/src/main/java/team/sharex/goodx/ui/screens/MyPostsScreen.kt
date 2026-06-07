package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.data.remote.UpdateGoodItemRequest
import team.sharex.goodx.data.remote.errorMessage
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
fun MyPostsScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var myItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf<GoodItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<GoodItem?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun loadMyItems() {
        scope.launch {
            isLoading = true
            try {
                val meResponse = RetrofitClient.apiService.getMe()
                val userId = meResponse.body()?.id
                val response = RetrofitClient.apiService.getGoodItems(
                    author = userId,
                    sort = "newest"
                )
                if (response.isSuccessful) {
                    myItems = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }
            isLoading = false
        }
    }

    LaunchedEffect(refreshTrigger) { loadMyItems() }

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
                text = "我的发布",
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
        } else if (myItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无发布", color = TextSecondary, fontSize = 16.sp)
                    Text("去发布你的第一个好物吧", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myItems) { item ->
                    MyPostItemCard(
                        item = item,
                        onEdit = { showEditDialog = item },
                        onDelete = { showDeleteConfirm = item }
                    )
                }
            }
        }
    }

    // 编辑对话框
    showEditDialog?.let { item ->
        EditGoodItemDialog(
            item = item,
            onDismiss = { showEditDialog = null },
            onUpdated = {
                showEditDialog = null
                refreshTrigger++
                android.widget.Toast.makeText(context, "✓ 修改成功！", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = Surface,
            title = { Text("确认删除", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${item.title}」吗？", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteGoodItem(item.id)
                                if (response.isSuccessful) {
                                    showDeleteConfirm = null
                                    refreshTrigger++
                                    android.widget.Toast.makeText(context, "✓ 删除成功！", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MyPostItemCard(
    item: GoodItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图片缩略
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(TextSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.images.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = "http://124.223.50.79:3002${item.images.first()}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(item.category.iconEmoji(), fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = item.category.displayName(),
                    color = Accent,
                    fontSize = 11.sp
                )
                if (!item.subCategory.isNullOrBlank()) {
                    Text(
                        text = " · ",
                        color = TextSecondary.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "🛒 ${item.subCategory}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = "♥ ${item.likes}",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 操作按钮
        Column {
            TextButton(
                onClick = onEdit,
                colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary),
                modifier = Modifier.height(32.dp)
            ) {
                Text("编辑", fontSize = 12.sp)
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = Accent),
                modifier = Modifier.height(32.dp)
            ) {
                Text("删除", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EditGoodItemDialog(
    item: GoodItem,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description ?: "") }
    var platform by remember { mutableStateOf(item.subCategory ?: "") }
    var selectedCategory by remember { mutableStateOf(item.category) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("编辑好物", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 品类选择
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedCategory.iconEmoji()} ${selectedCategory.displayName()}")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        Category.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.iconEmoji()} ${cat.displayName()}", color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("标题", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述", color = TextSecondary) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("平台/品牌", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "请输入标题"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val response = RetrofitClient.apiService.updateGoodItem(
                                id = item.id,
                                request = UpdateGoodItemRequest(
                                    title = title,
                                    description = description,
                                    category = selectedCategory.name,
                                    subCategory = platform.takeIf { it.isNotBlank() }
                                )
                            )
                            if (response.isSuccessful) {
                                android.widget.Toast.makeText(context, "✓ 修改成功！", android.widget.Toast.LENGTH_SHORT).show()
                                onUpdated()
                                return@launch
                            } else {
                                error = "修改失败: ${response.errorMessage()}"
                            }
                        } catch (e: Exception) {
                            error = "网络错误: ${e.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("取消")
            }
        }
    )
}
