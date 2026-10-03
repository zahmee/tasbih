package com.sakinah.tasbih.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.BuildConfig
import com.sakinah.tasbih.R
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors

private const val CDIT_WEBSITE_URL = "https://cdit.co"
private const val CDIT_CONTACT_URL = "https://cdit.co/contact.html"
private const val CDIT_WHATSAPP_URL = "https://wa.me/966502010911"
private const val CDIT_EMAIL_ADDRESS = "info@cdit.co"
internal const val QuranAppStoreUrl = "https://play.google.com/store/apps/details?id=com.mushaf.reader"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    SakinahScreenBackground {
        Scaffold(
            modifier = Modifier.testTag("about_screen"),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    scrollBehavior = scrollBehavior,
                    title = { Text(stringResource(R.string.about_app)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SakinahContentMaxWidth)
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .testTag("about_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 20.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        AboutHero(
                            version = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            releaseDate = stringResource(
                                R.string.about_release_date,
                                java.time.LocalDate.parse(BuildConfig.RELEASE_DATE).format(
                                    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
                                        .withLocale(java.util.Locale.forLanguageTag("ar"))
                                        .withDecimalStyle(java.time.format.DecimalStyle.of(java.util.Locale.forLanguageTag("ar"))),
                                ),
                            ),
                        )
                    }

                    item { SakinahSectionHeader(stringResource(R.string.about_trust_title)) }
                    item { AboutTrustCard() }

                    item { SakinahSectionHeader(stringResource(R.string.about_content_title)) }
                    item { AboutContentCard() }

                    item { SakinahSectionHeader(stringResource(R.string.about_our_apps)) }
                    item {
                        AboutQuranAppCard(onOpenStore = { context.openExternalLink(QuranAppStoreUrl) })
                    }

                    item { SakinahSectionHeader(stringResource(R.string.about_developer_title)) }
                    item {
                        AboutDeveloperCard(
                            onVisitWebsite = { context.openExternalLink(CDIT_WEBSITE_URL) },
                        )
                    }

                    item { SakinahSectionHeader(stringResource(R.string.about_contact_title)) }
                    item {
                        AboutContactCard(
                            onWhatsapp = { context.openExternalLink(CDIT_WHATSAPP_URL) },
                            onEmail = { context.openEmail() },
                            onContactPage = { context.openExternalLink(CDIT_CONTACT_URL) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHero(version: String, releaseDate: String) {
    val brand = LocalSakinahBrandColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = brand.heroStart,
            contentColor = brand.onHero,
        ),
        border = BorderStroke(1.dp, brand.onHero.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.large,
                    color = brand.onHero.copy(alpha = 0.13f),
                    contentColor = brand.onHero,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnaaMark(
                            modifier = Modifier.size(40.dp),
                            tint = brand.onHero,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    AnaaWordmark(Modifier.width(108.dp).height(70.dp), tint = brand.onHero)
                    Text(
                        text = stringResource(R.string.about_app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = brand.onHero.copy(alpha = 0.82f),
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.labelLarge,
                        color = brand.onHero.copy(alpha = 0.72f),
                    )
                    Text(
                        text = releaseDate,
                        modifier = Modifier.testTag("about_release_date"),
                        style = MaterialTheme.typography.labelMedium,
                        color = brand.onHero.copy(alpha = 0.72f),
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutHeroChip(stringResource(R.string.about_tag_free))
                AboutHeroChip(stringResource(R.string.about_tag_no_ads))
                AboutHeroChip(stringResource(R.string.about_tag_no_account))
            }
        }
    }
}

@Composable
private fun AboutHeroChip(text: String) {
    val brand = LocalSakinahBrandColors.current
    Surface(
        shape = CircleShape,
        color = brand.onHero.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, brand.onHero.copy(alpha = 0.16f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = brand.onHero,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AboutTrustCard() {
    AboutDetailCard {
        AboutInformationRow(
            icon = Icons.Outlined.CheckCircle,
            title = stringResource(R.string.about_free_title),
            body = stringResource(R.string.about_free_body),
        )
        AboutDivider()
        AboutInformationRow(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.about_privacy_title),
            body = stringResource(R.string.about_privacy_body),
        )
        AboutDivider()
        AboutInformationRow(
            icon = Icons.Outlined.FavoriteBorder,
            title = stringResource(R.string.about_purpose_title),
            body = stringResource(R.string.about_purpose_body),
        )
    }
}

@Composable
private fun AboutContentCard() {
    AboutDetailCard {
        AboutInformationRow(
            icon = Icons.Outlined.Verified,
            title = stringResource(R.string.about_hisn_title),
            body = stringResource(R.string.about_hisn_body),
        )
    }
}

@Composable
private fun AboutQuranAppCard(onOpenStore: () -> Unit) {
    AboutDetailCard {
        AboutInformationRow(
            icon = Icons.Outlined.AutoStories,
            title = stringResource(R.string.about_quran_app_name),
            body = stringResource(R.string.about_quran_app_description),
        )
        Spacer(Modifier.height(16.dp))
        AboutActionButton(
            title = stringResource(R.string.about_quran_open_store),
            detail = stringResource(R.string.about_quran_store_detail),
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            tag = "about_quran_store",
            onClick = onOpenStore,
        )
    }
}

@Composable
private fun AboutDeveloperCard(onVisitWebsite: () -> Unit) {
    AboutDetailCard {
        AboutInformationRow(
            icon = Icons.Outlined.Business,
            title = stringResource(R.string.about_developer_title),
            body = stringResource(R.string.about_developer_body),
        )
        AboutDivider()
        Text(
            text = stringResource(R.string.about_commercial_register),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        AboutActionButton(
            title = stringResource(R.string.about_developer_website),
            detail = stringResource(R.string.about_developer_website_detail),
            icon = Icons.Outlined.Language,
            tag = "about_developer_website",
            onClick = onVisitWebsite,
        )
    }
}

@Composable
private fun AboutContactCard(
    onWhatsapp: () -> Unit,
    onEmail: () -> Unit,
    onContactPage: () -> Unit,
) {
    AboutDetailCard {
        Text(
            text = stringResource(R.string.about_contact_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        AboutActionButton(
            title = stringResource(R.string.about_whatsapp_title),
            detail = stringResource(R.string.about_whatsapp_number),
            icon = Icons.Outlined.ChatBubbleOutline,
            tag = "about_whatsapp",
            onClick = onWhatsapp,
        )
        Spacer(Modifier.height(8.dp))
        AboutActionButton(
            title = stringResource(R.string.about_email_title),
            detail = stringResource(R.string.about_email_address),
            icon = Icons.Outlined.Email,
            tag = "about_email",
            onClick = onEmail,
        )
        Spacer(Modifier.height(8.dp))
        AboutActionButton(
            title = stringResource(R.string.about_contact_page_title),
            detail = stringResource(R.string.about_contact_page_detail),
            icon = Icons.Outlined.Language,
            tag = "about_contact_page",
            onClick = onContactPage,
        )
    }
}

@Composable
private fun AboutDetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = sakinahCardBorder(0.2f),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

@Composable
private fun AboutInformationRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = LocalSakinahBrandColors.current.antiqueGold.copy(alpha = 0.2f),
    )
}

@Composable
private fun AboutActionButton(
    title: String,
    detail: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Context.openExternalLink(url: String) {
    openExternalIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun Context.openEmail() {
    openExternalIntent(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CDIT_EMAIL_ADDRESS")).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_email_subject))
        },
    )
}

private fun Context.openExternalIntent(intent: Intent) {
    runCatching { startActivity(intent) }
        .onFailure {
            Toast.makeText(this, getString(R.string.about_contact_unavailable), Toast.LENGTH_SHORT).show()
        }
}
