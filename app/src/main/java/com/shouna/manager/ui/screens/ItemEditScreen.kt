package com.shouna.manager.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.PhotoStorage
import com.shouna.manager.data.db.entity.ItemEntity
import com.shouna.manager.data.db.entity.ItemZoneEntity
import com.shouna.manager.data.db.entity.SubZoneEntity
import com.shouna.manager.data.db.entity.ZoneEntity
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.domain.ShelfLife
import com.shouna.manager.domain.OcrParse
import com.shouna.manager.domain.TagUtils
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.AppTextField
import com.shouna.manager.ui.components.DateField
import com.shouna.manager.ui.components.SectionCard
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.PrimaryLight
import com.shouna.manager.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import com.shouna.manager.ocr.PaddleOcrEngine
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import com.shouna.manager.ui.theme.Bg

private data class LocationSelection(val zoneId: Long, val subZoneId: Long?)

@Composable
fun ItemEditScreen(itemId: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val allItems by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val commonTags = remember(allItems) {
        (TagUtils.defaultTags() + allItems.flatMap { TagUtils.split(it.tags) }).distinct()
    }

    var name by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var specModel by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf<Long?>(null) }
    var platform by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var productionDate by remember { mutableStateOf<Long?>(null) }
    var shelfLifeText by remember { mutableStateOf("") }
    var expiryMode by remember { mutableStateOf(0) } // 0 无 1 生产日期+保质期 2 直填
    var directExpiry by remember { mutableStateOf<Long?>(null) }
    var isConsumable by remember { mutableStateOf(false) }
    var restockThresholdText by remember { mutableStateOf("1") }
    var remindDaysBefore by remember { mutableStateOf<Int?>(null) }
    var locations by remember { mutableStateOf<List<LocationSelection>>(emptyList()) }
    var photoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    val selectedTags = TagUtils.split(tagsText)

    // 编辑态回填
    LaunchedEffect(itemId) {
        if (itemId == null) return@LaunchedEffect
        val item = AppGraph.itemRepository.getItem(itemId) ?: return@LaunchedEffect
        name = item.name
        tagsText = item.tags
        specModel = item.specModel.orEmpty()
        note = item.note
        purchaseDate = item.purchaseDate
        platform = item.platform.orEmpty()
        amountText = item.amount?.toString().orEmpty()
        quantityText = item.quantity.toString()
        productionDate = item.productionDate
        shelfLifeText = item.shelfLifeText.orEmpty()
        expiryMode = if (item.expiryDirect) 2 else if (item.expiryDate != null) 1 else 0
        directExpiry = if (item.expiryDirect) item.expiryDate else null
        locations = AppGraph.itemRepository.getItemZones(itemId).map { LocationSelection(it.zoneId, it.subZoneId) }
        photoPaths = AppGraph.itemRepository.getPhotos(itemId).map { it.path }
        isConsumable = item.isConsumable
        restockThresholdText = item.restockThreshold.toString()
        remindDaysBefore = item.remindDaysBefore
    }

    // 相机
    var captureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = captureUri
            if (uri != null) {
                PhotoStorage.saveImage(context, uri)?.let { p -> photoPaths = photoPaths + p }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = PhotoStorage.newCaptureUri(context)
            captureUri = uri
            takePicture.launch(uri)
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            PhotoStorage.saveImage(context, uri)?.let { p -> photoPaths = photoPaths + p }
        }
    }

    // ===== 扫保质期 OCR（调用相机拍照） =====
    val ocrEngine = remember { PaddleOcrEngine(context) }
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var ocrCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun fillFromOcr(texts: List<String>) {
        val parsed = OcrParse.parse(texts)
        if (parsed.productionDate != null) { productionDate = parsed.productionDate; if (expiryMode == 0) expiryMode = 1 }
        if (parsed.shelfLifeText != null) { shelfLifeText = parsed.shelfLifeText; if (expiryMode == 0) expiryMode = 1 }
        // 有效日期缺日 → 沿用生产日期的「日」
        if (parsed.expiryYmOnlyYear != null && parsed.expiryYmOnlyMonth != null) {
            val day = parsed.productionDate?.let {
                java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.DAY_OF_MONTH)
            } ?: 1
            directExpiry = OcrParse.makeMillis(parsed.expiryYmOnlyYear, parsed.expiryYmOnlyMonth, day)
            expiryMode = 2
        } else if (parsed.expiryDirect != null) {
            directExpiry = parsed.expiryDirect; expiryMode = 2
        }
        ocrText = texts.joinToString("  ")
    }

    fun recognizeOcr(uri: android.net.Uri) {
        scope.launch {
            ocrLoading = true
            ocrText = null
            try {
                val ok = ocrEngine.init()
                if (ok) {
                    val bmp = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                    if (bmp != null) {
                        val texts = withContext(Dispatchers.IO) { ocrEngine.recognize(bmp) }
                        fillFromOcr(texts)
                    } else {
                        ocrText = "读取图片失败"
                    }
                } else {
                    ocrText = "模型加载失败"
                }
            } catch (e: Exception) {
                android.util.Log.e("Ocr", "recognize failed", e)
                ocrText = "识别失败：${e.message}"
            } finally {
                ocrLoading = false
            }
        }
    }

    val ocrTakePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) ocrCaptureUri?.let { recognizeOcr(it) }
    }
    val ocrCamPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = PhotoStorage.newCaptureUri(context)
            ocrCaptureUri = uri
            ocrTakePicture.launch(uri)
        } else {
            ocrText = "未授予相机权限"
        }
    }

    val computedExpiry: Long? = when (expiryMode) {
        1 -> ExpiryCalculator.computeExpiry(productionDate, ShelfLife.parseDays(shelfLifeText), null)
        2 -> directExpiry
        else -> null
    }

    fun save() {
        if (name.isBlank()) return
        scope.launch {
            val existing = if (itemId != null) {
                runCatching { AppGraph.itemRepository.getItem(itemId) }.getOrNull()
            } else null
            val item = ItemEntity(
                id = itemId ?: 0,
                uuid = existing?.uuid ?: java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                tags = TagUtils.join(TagUtils.split(tagsText)),
                purchaseDate = purchaseDate,
                platform = platform.ifBlank { null },
                amount = amountText.toDoubleOrNull(),
                quantity = quantityText.toIntOrNull() ?: 1,
                productionDate = productionDate,
                shelfLifeText = shelfLifeText.ifBlank { null },
                expiryDate = computedExpiry,
                expiryDirect = expiryMode == 2,
                specModel = specModel.ifBlank { null },
                note = note,
                handled = existing?.handled ?: false,
                handledType = existing?.handledType,
                handledAt = existing?.handledAt,
                isConsumable = isConsumable,
                restockThreshold = restockThresholdText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                remindDaysBefore = remindDaysBefore,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val zoneEntities = locations.take(3).map { ItemZoneEntity(itemId = 0, zoneId = it.zoneId, subZoneId = it.subZoneId) }
            if (itemId == null) {
                AppGraph.itemRepository.add(item, zoneEntities, photoPaths)
            } else {
                AppGraph.itemRepository.update(item, zoneEntities, photoPaths)
            }
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        AppTopBar(if (itemId == null) "添加物品" else "编辑物品", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard("基本信息") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField("名称 *", name, { name = it }, placeholder = "如：生抽、洗衣液")
                    AppTextField("标签", tagsText, { tagsText = it }, placeholder = "逗号分隔，如：食品,可囤货")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commonTags.forEach { tag ->
                            val selected = tag in selectedTags
                            TagChip(text = tag, selected = selected) {
                                tagsText = if (selected) TagUtils.join(selectedTags - tag) else TagUtils.join(selectedTags + tag)
                            }
                        }
                    }
                    AppTextField("规格 / 型号", specModel, { specModel = it }, placeholder = "选填，如：500ml、A4")
                    AppTextField("备注", note, { note = it })
                }
            }

            SectionCard("购买信息") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("购买日期", purchaseDate, { purchaseDate = it })
                    AppTextField("购买平台", platform, { platform = it }, placeholder = "选填，如：京东、超市")
                    NumberField("购买金额（元）", amountText, { amountText = it }, decimal = true)
                    NumberField("购买数量", quantityText, { quantityText = it })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("消耗品（用完自动提醒补货）", fontSize = 14.sp)
                            Text("数量低于阈值时进入补货清单", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(checked = isConsumable, onCheckedChange = { isConsumable = it })
                    }
                    if (isConsumable) {
                        NumberField("补货阈值（数量 ≤ 此值时提醒）", restockThresholdText, { restockThresholdText = it })
                    }
                }
            }

            SectionCard("保质期 / 有效期") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = PhotoStorage.newCaptureUri(context)
                                ocrCaptureUri = uri
                                ocrTakePicture.launch(uri)
                            } else {
                                ocrCamPerm.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !ocrLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (ocrLoading) "识别中，首次需加载模型…" else "扫保质期(拍照识别)-测试中")
                    }
                    ocrText?.let {
                        Text(
                            text = "识别：$it",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = expiryMode == 0, onClick = { expiryMode = 0 })
                        Text("无有效期", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = expiryMode == 1, onClick = { expiryMode = 1 })
                        Text("按生产日期 + 保质期计算", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = expiryMode == 2, onClick = { expiryMode = 2 })
                        Text("直接填有效日期", fontSize = 14.sp)
                    }
                    if (expiryMode == 1) {
                        DateField("生产日期", productionDate, { productionDate = it })
                        AppTextField("保质期描述", shelfLifeText, { shelfLifeText = it }, placeholder = "如：15天 / 9个月 / 3年")
                        Text(
                            text = if (computedExpiry != null) "自动计算到期日：${Format.date(computedExpiry)}" else "输入生产日期与保质期后自动计算到期日",
                            fontSize = 12.sp,
                            color = if (computedExpiry != null) Primary else TextSecondary
                        )
                    }
                    if (expiryMode == 2) {
                        DateField("有效日期", directExpiry, { directExpiry = it })
                    }
                    // 到期提醒提前量（仅当设置了有效期）
                    if (expiryMode != 0) {
                        var remindMenu by remember { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("到期提醒提前", fontSize = 14.sp)
                            Box {
                                OutlinedButton(onClick = { remindMenu = true }) {
                                    Text(remindDaysBefore?.let { "$it 天" } ?: "默认（7 天）")
                                }
                                DropdownMenu(expanded = remindMenu, onDismissRequest = { remindMenu = false }) {
                                    DropdownMenuItem(text = { Text("默认（7 天）") }, onClick = { remindDaysBefore = null; remindMenu = false })
                                    listOf(1, 3, 15, 30).forEach { d ->
                                        DropdownMenuItem(text = { Text("$d 天") }, onClick = { remindDaysBefore = d; remindMenu = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionCard("存放位置（最多 3 个）") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (locations.isEmpty()) {
                        Text("未设置位置", fontSize = 13.sp, color = TextSecondary)
                    }
                    locations.forEachIndexed { index, loc ->
                        LocationSlot(
                            index = index,
                            zones = zones,
                            subZones = subZones,
                            selection = loc,
                            onSelected = { new -> locations = locations.toMutableList().also { it[index] = new } },
                            onRemove = { locations = locations.filterIndexed { i, _ -> i != index } }
                        )
                    }
                    if (locations.size < 3) {
                        TextButton(onClick = {
                            if (zones.isNotEmpty()) {
                                locations = locations + LocationSelection(zones.first().id, null)
                            }
                        }) { Text("＋ 添加位置") }
                    }
                }
            }

            SectionCard("照片") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photoPaths.forEachIndexed { index, path ->
                            val file = PhotoStorage.loadFile(context, path)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFFF0EAE3), RoundedCornerShape(10.dp))
                                    .clickable {
                                        photoPaths = photoPaths.filterIndexed { i, _ -> i != index }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                PhotoThumb(file.absolutePath)
                                Text("×", fontSize = 16.sp, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = PhotoStorage.newCaptureUri(context)
                                captureUri = uri
                                takePicture.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) { Text("拍照") }
                        TextButton(onClick = { pickImage.launch(arrayOf("image/*")) }) { Text("从相册选择") }
                    }
                }
            }

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                enabled = name.isNotBlank()
            ) {
                Text("保存", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all { it.isDigit() || (decimal && it == '.') }) onChange(input) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)
    )
}

@Composable
private fun LocationSlot(
    index: Int,
    zones: List<ZoneEntity>,
    subZones: List<SubZoneEntity>,
    selection: LocationSelection,
    onSelected: (LocationSelection) -> Unit,
    onRemove: () -> Unit
) {
    val zone = zones.find { it.id == selection.zoneId }
    val zoneSubs = subZones.filter { it.zoneId == selection.zoneId }
    val subName = selection.subZoneId?.let { id -> zoneSubs.find { it.id == id }?.name }
    val label = listOfNotNull(zone?.name, subName).joinToString("·").ifBlank { "选择位置" }

    // 扁平选项：每个「主」和「主·子」都是一项
    val options = buildList {
        zones.forEach { z ->
            val subs = subZones.filter { it.zoneId == z.id }
            if (subs.isEmpty()) add(LocationSelection(z.id, null) to z.name)
            else subs.forEach { s -> add(LocationSelection(z.id, s.id) to "${z.name}·${s.name}") }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            var zoneMenu by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .background(PrimaryLight, RoundedCornerShape(10.dp))
                        .clickable { zoneMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("位置${index + 1}：${label}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                DropdownMenu(expanded = zoneMenu, onDismissRequest = { zoneMenu = false }) {
                    options.forEach { (loc, text) ->
                        DropdownMenuItem(text = { Text(text) }, onClick = {
                            onSelected(loc)
                            zoneMenu = false
                        })
                    }
                }
            }
        }
        TextButton(onClick = onRemove) { Text("移除", color = com.shouna.manager.ui.theme.Red) }
    }
}

@Composable
private fun TagChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else Color(0xFFF5EFE8)
    val fg = if (selected) Color.White else TextSecondary
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = fg)
    }
}

@Composable
private fun PhotoThumb(path: String) {    val bmp = remember(path) { PhotoStorage.decodeThumb(java.io.File(path), 160) }
    if (bmp != null) {
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
    }
}
