package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.components.EmptyStateBox
import com.example.ui.components.StatusPill
import com.example.ui.components.WarmTopBar
import com.example.ui.components.warmMeshBackground
import com.example.ui.theme.LocalMessMateColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfPreviewScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val extraColors = LocalMessMateColors.current
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val pdfFile = remember(filePath) { File(filePath) }

    LaunchedEffect(filePath) {
        isLoading = true
        val pages = withContext(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            if (pdfFile.exists()) {
                runCatching {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (i in 0 until renderer.pageCount) {
                                renderer.openPage(i).use { page ->
                                    val bmp = Bitmap.createBitmap(
                                        page.width * 2,
                                        page.height * 2,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    bmp.eraseColor(Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    bitmaps.add(bmp)
                                }
                            }
                        }
                    }
                }
            }
            bitmaps
        }
        renderedPages = pages
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            renderedPages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .warmMeshBackground()
    ) {
        WarmTopBar(
            title = stringResource(R.string.records_pdf_preview_title),
            onNavigateBack = onNavigateBack,
            actions = {
                if (pdfFile.exists()) {
                    Surface(
                        onClick = {
                            runCatching {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, context.getString(R.string.records_share_pdf))
                                )
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.records_pdf_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extraColors.divider),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.records_share_pdf),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                renderedPages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center)
                    ) {
                        EmptyStateBox(message = stringResource(R.string.records_pdf_error))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(renderedPages) { index, bitmap ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(
                                    text = stringResource(R.string.records_page_indicator, index + 1, renderedPages.size),
                                    color = MaterialTheme.colorScheme.primary,
                                    containerColor = extraColors.primarySoft
                                )
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, extraColors.divider),
                                    shadowElevation = if (extraColors.isDark) 0.dp else 3.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "PDF Page ${index + 1}",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
