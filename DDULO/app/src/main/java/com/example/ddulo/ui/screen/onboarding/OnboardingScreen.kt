package com.example.ddulo.ui.screen.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.ddulo.R
import com.example.ddulo.data.preferences.OnboardingPreferences
import com.example.ddulo.ui.theme.Teal40

private const val PAGE_COUNT = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onboardingPreferences: OnboardingPreferences,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Skip: 1·2페이지에서는 보이고, 3페이지에서는 공간만 유지(투명)해 이미지 위치 고정
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val isLastPage = currentPage == PAGE_COUNT - 1
                    TextButton(
                        onClick = {
                            if (!isLastPage) {
                                onboardingPreferences.setOnboardingCompleted(true)
                                navController.navigate("search_graph") {
                                    popUpTo("onboarding_flow") { inclusive = true }
                                }
                            }
                        },
                    enabled = !isLastPage,
                    modifier = Modifier.then(if (isLastPage) Modifier.alpha(0f) else Modifier)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = Teal40,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 페이지 콘텐츠
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                OnboardingPageContent(page = page)
            }

            // 하단: 인디케이터 + 버튼 (위로 올림)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 페이지 인디케이터 (점 3개)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(PAGE_COUNT) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentPage == index) Teal40
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (currentPage == PAGE_COUNT - 1) {
                    // 마지막 페이지: "시작하기"
                    Button(
                        onClick = {
                            onboardingPreferences.setOnboardingCompleted(true)
                            navController.navigate("search_graph") {
                                popUpTo("onboarding_flow") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal40)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_start),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    // 1·2페이지: "다음" 버튼 (높이 52.dp로 고정해 점 3개 위치 유지)
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            color = Teal40,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: Int,
    modifier: Modifier = Modifier
) {
    val imageRes = when (page) {
        0 -> R.drawable.onboarding_page1
        1 -> R.drawable.onboarding_page2
        else -> R.drawable.onboarding_page3
    }
    val titleRes = when (page) {
        0 -> R.string.onboarding_title_1
        1 -> R.string.onboarding_title_2
        else -> R.string.onboarding_title_3
    }
    val descRes = when (page) {
        0 -> R.string.onboarding_desc_1
        1 -> R.string.onboarding_desc_2
        else -> R.string.onboarding_desc_3
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 위쪽 여백을 아래보다 작게 해서 이미지·텍스트를 조금 더 위로
        Spacer(modifier = Modifier.weight(0.5f))
        // 일러스트 영역 (361:292 비율, 흰 배경, 이미지 가득)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(361f / 292f)
                .padding(vertical = 16.dp)
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 제목 (볼드)
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 설명 (titleMedium)
        Text(
            text = stringResource(descRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
