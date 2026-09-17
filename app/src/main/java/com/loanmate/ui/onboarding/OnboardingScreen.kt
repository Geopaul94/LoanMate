package com.loanmate.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loanmate.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🏦",
        title = "Track Your Debt Journey",
        description = "Personal loans, home loans, EMIs, or KSFE chitties — organize everything in one beautiful dashboard.",
        primaryColor = Color(0xFF00695C),
        secondaryColor = Color(0xFF4DB6AC)
    ),
    OnboardingPage(
        emoji = "🔔",
        title = "Never Miss a Payment",
        description = "Smart, timely reminders ensure you stay on top of your EMIs and protect your credit score.",
        primaryColor = Color(0xFFE65100),
        secondaryColor = Color(0xFFFFB74D)
    ),
    OnboardingPage(
        emoji = "🎯",
        title = "Achieve Debt Freedom",
        description = "Simulate prepayments, analyze savings, and watch your timeline to financial freedom shrink.",
        primaryColor = Color(0xFF1B5E20),
        secondaryColor = Color(0xFF81C784)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1
    
    val currentPage = pages[pagerState.currentPage]

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            currentPage.primaryColor.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Skip button
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (!isLastPage) {
                        TextButton(
                            onClick = {
                                viewModel.complete()
                                onFinished()
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Skip", color = currentPage.primaryColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    OnboardingPageContent(pages[pageIndex])
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Dot indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pages.size) { index ->
                            val isActive = index == pagerState.currentPage
                            val width by animateDpAsState(
                                targetValue = if (isActive) 32.dp else 8.dp,
                                label = "indicator"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) currentPage.primaryColor
                                        else currentPage.primaryColor.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    // Action button
                    Button(
                        onClick = {
                            if (isLastPage) {
                                viewModel.complete()
                                onFinished()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentPage.primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Continue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Shadow circle
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .alpha(0.1f)
                    .background(page.primaryColor, CircleShape)
            )
            // Icon container
            Surface(
                modifier = Modifier.size(180.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = page.emoji, fontSize = 84.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = page.primaryColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
