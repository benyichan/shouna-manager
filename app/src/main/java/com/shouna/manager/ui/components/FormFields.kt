package com.shouna.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shouna.manager.domain.ExpiryStatus
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.theme.Amber
import com.shouna.manager.ui.theme.AmberLight
import com.shouna.manager.ui.theme.Green
import com.shouna.manager.ui.theme.GreenLight
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.RedLight
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun AppTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val display = if (value != null) Format.date(value) else "未设置"
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(if (display == "未设置") "未设置" else display, fontSize = 15.sp)
        }
        if (showDialog) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = value ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        onChange(state.selectedDateMillis)
                        showDialog = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("取消") }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }
}

@Composable
fun StatusBadge(status: ExpiryStatus, modifier: Modifier = Modifier) {
    val (bg, fg, text) = when (status) {
        ExpiryStatus.NORMAL -> Triple(GreenLight, Green, "正常")
        ExpiryStatus.EXPIRING -> Triple(AmberLight, Amber, "临期")
        ExpiryStatus.EXPIRED -> Triple(RedLight, Red, "已过期")
        ExpiryStatus.HANDLED -> Triple(Color(0xFFF0F0F0), TextSecondary, "已处理")
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** 位置标签拼接：主区域名 · 子区域名 */
@Composable
fun LocationTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFF5EFE8), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 12.sp, color = TextSecondary)
    }
}
