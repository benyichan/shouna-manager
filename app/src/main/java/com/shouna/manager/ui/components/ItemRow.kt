package com.shouna.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shouna.manager.data.db.entity.ItemEntity
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.theme.Amber
import com.shouna.manager.ui.theme.Green
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.TextPrimary
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun ItemRow(item: ItemEntity, locationLabel: String, onClick: () -> Unit) {
    val status = ExpiryCalculator.statusOf(item)
    val hintColor = when (status) {
        com.shouna.manager.domain.ExpiryStatus.EXPIRED -> Red
        com.shouna.manager.domain.ExpiryStatus.EXPIRING -> Amber
        else -> TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (locationLabel.isNotEmpty()) {
                Text(
                    text = locationLabel,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = Format.expiryHint(item.expiryDate, item.handled),
                fontSize = 12.sp,
                color = hintColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        StatusBadge(status)
    }
}
