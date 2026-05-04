package com.focusfirst.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.PlanetSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinSelectorSheet(
    currentSkin: PlanetSkin,
    isPro: Boolean,
    onSkinSelected: (PlanetSkin) -> Unit,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Choose your world",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                    )
                }
            }

            // ── Skin grid ─────────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(PlanetSkin.entries) { skin ->
                    SkinCard(
                        skin = skin,
                        isSelected = skin == currentSkin,
                        isPro = isPro,
                        onClick = {
                            if (skin.isPro && !isPro) onUpgradeClick()
                            else onSkinSelected(skin)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Upgrade banner ────────────────────────────────────────────────
            if (!isPro) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Unlock all worlds",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "Monthly Pro subscription",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                        Button(
                            onClick = onUpgradeClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text(text = "Upgrade", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── SkinCard ──────────────────────────────────────────────────────────────────

@Composable
private fun SkinCard(
    skin: PlanetSkin,
    isSelected: Boolean,
    isPro: Boolean,
    onClick: () -> Unit,
) {
    val isLocked = skin.isPro && !isPro

    Surface(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, Color.White, RoundedCornerShape(16.dp))
                else
                    Modifier.border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            ),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                MiniPlanetPreview(
                    skin = skin,
                    dimmed = isLocked,
                )
                if (isLocked) {
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd),
                        color = Color(0xFFFFB800),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Pro",
                            tint = Color.Black,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = skin.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(2.dp))

            when {
                skin.isPro && !isPro -> Text(
                    text = "PRO",
                    fontSize = 10.sp,
                    color = Color(0xFFFFB800),
                    letterSpacing = 0.1.sp,
                )
                isSelected -> Text(
                    text = "ACTIVE",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.1.sp,
                )
                else -> Text(
                    text = skin.description,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                )
            }
        }
    }
}

// ── MiniPlanetPreview ─────────────────────────────────────────────────────────

private val skinBaseColor = mapOf(
    PlanetSkin.EARTH to Color(0xFF1a6b3c),
    PlanetSkin.MARS  to Color(0xFF8B2500),
    PlanetSkin.OCEAN to Color(0xFF0a2a6e),
    PlanetSkin.ICE   to Color(0xFFa8d8ea),
    PlanetSkin.LAVA  to Color(0xFFff4500),
    PlanetSkin.ALIEN to Color(0xFF6400b8),
)

@Composable
private fun MiniPlanetPreview(
    skin: PlanetSkin,
    dimmed: Boolean = false,
) {
    val base      = skinBaseColor[skin] ?: Color.Gray
    val baseAlpha = if (dimmed) 0.4f else 1f

    Canvas(modifier = Modifier.size(64.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        when (skin) {
            PlanetSkin.MARS -> {
                // Base rust red
                drawCircle(
                    color  = Color(0xFF8B2500).copy(alpha = baseAlpha),
                    radius = radius,
                    center = center,
                )
                // Ice cap hint at top
                drawArc(
                    color       = Color(0xCCFFFFFF).copy(alpha = baseAlpha),
                    startAngle  = 200f,
                    sweepAngle  = 140f,
                    useCenter   = false,
                    topLeft     = Offset(center.x - radius * 0.7f, center.y - radius * 0.9f),
                    size        = Size(radius * 1.4f, radius * 0.6f),
                )
                // Atmosphere edge hint
                drawCircle(
                    color  = Color(0x33E8884A).copy(alpha = baseAlpha),
                    radius = radius * 1.05f,
                    center = center,
                    style  = Stroke(width = size.width * 0.06f),
                )
            }
            else -> {
                // Base sphere
                drawCircle(
                    color  = base.copy(alpha = baseAlpha),
                    radius = radius,
                    center = center,
                )
                // Radial shine — top-left white highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f * baseAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                        radius = radius * 0.8f,
                    ),
                    radius = radius,
                    center = center,
                )
                // Shadow — right side dark overlay
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f * baseAlpha),
                        ),
                        center = Offset(center.x + radius * 0.2f, center.y + radius * 0.2f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
    }
}
