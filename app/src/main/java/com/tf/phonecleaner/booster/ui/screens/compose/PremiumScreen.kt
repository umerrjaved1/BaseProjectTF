package com.tf.phonecleaner.booster.ui.screens.compose

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.app.AnalyticsManager
import com.tf.phonecleaner.booster.constants.Constants
import com.tf.phonecleaner.booster.iab.AppBillingClient
import com.tf.phonecleaner.booster.iab.ConnectResponse
import com.tf.phonecleaner.booster.iab.PurchaseResponse
import com.tf.phonecleaner.booster.iab.SubscriptionItem
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.ui.viewmodel.PremiumViewModel
import com.tf.phonecleaner.booster.utils.UIState
import kotlinx.coroutines.delay

enum class PlanType { WEEKLY, YEARLY }

@Composable
fun PremiumScreen(
    onNavigateNext: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val billingClient = remember { AppBillingClient.getInstance() }
    var selectedPlan by remember { mutableStateOf(PlanType.WEEKLY) }
    var showCloseButton by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Close button delay logic
    LaunchedEffect(Unit) {
        val delayMs = RemoteConfigManager.getPremiumScreenConfig().premiumCloseBtnDelay.toLong()
        delay(delayMs)
        showCloseButton = true
    }

    // Initialize Billing
    LaunchedEffect(Unit) {
        viewModel.analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "PremiumScreen")
        billingClient.initialize(context, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                viewModel.updateSubscriptions(subscriptionItems)
            }
            override fun onDisconnected() {
                // Do nothing
            }
            override fun onError(errorCode: Int, errorMessage: String) {
                // Do nothing
            }
        })
    }

    // Handle UI State navigation logic
    LaunchedEffect(uiState) {
        if (uiState is UIState.Success && (uiState as UIState.Success).data.isPremium) {
            onNavigateNext()
        }
    }

    val stateData = if (uiState is UIState.Success) (uiState as UIState.Success).data else null
    val subscriptions = stateData?.subscriptions ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Background illustration (Removed pro_bg and kept solid color)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                AnimatedVisibility(visible = showCloseButton) {
                    IconButton(
                        onClick = {
                            viewModel.analyticsManager.sendAnalytics("clicked", "PremiumScreen_close_button")
                            onNavigateNext()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Icon
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Premium",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Upgrade to Premium",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unlock all features and remove ads entirely.",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Plan Cards
            val weeklySub = subscriptions.find { it.sku == Constants.SKU_SUBSCRIPTION_WEEKLY }
            val yearlySub = subscriptions.find { it.sku == Constants.SKU_SUBSCRIPTION_YEARLY }

            PlanCard(
                title = "Weekly Plan",
                price = weeklySub?.formattedPrice ?: "$2.99",
                subtitle = "3 days free trial",
                isSelected = selectedPlan == PlanType.WEEKLY,
                onClick = { selectedPlan = PlanType.WEEKLY }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanCard(
                title = "Yearly Plan",
                price = yearlySub?.formattedPrice ?: "$29.99",
                subtitle = "Best Value",
                isSelected = selectedPlan == PlanType.YEARLY,
                onClick = { selectedPlan = PlanType.YEARLY },
                isBestValue = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = {
                    if (activity == null) return@Button
                    isLoading = true
                    viewModel.analyticsManager.sendAnalytics("clicked", "PremiumScreen_subscribe_${selectedPlan.name.lowercase()}")
                    
                    val planSku = if (selectedPlan == PlanType.WEEKLY) Constants.SKU_SUBSCRIPTION_WEEKLY else Constants.SKU_SUBSCRIPTION_YEARLY
                    val subscription = subscriptions.find { it.sku == planSku } ?: subscriptions.firstOrNull()
                    
                    if (subscription == null) {
                        isLoading = false
                        Toast.makeText(context, "Subscription not available", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val offerToken = if (selectedPlan == PlanType.WEEKLY) {
                        subscription.getOfferTokenById(Constants.OFFER_ID_TRIAL) ?: subscription.baseOfferToken
                    } else {
                        subscription.baseOfferToken
                    }

                    if (offerToken.isNullOrEmpty()) {
                        isLoading = false
                        Toast.makeText(context, "Unable to process subscription", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    billingClient.purchaseSubscription(
                        activity,
                        subscription,
                        offerToken,
                        object : PurchaseResponse {
                            override fun onPurchaseSuccess(productId: String) {
                                activity.runOnUiThread {
                                    isLoading = false
                                    viewModel.setPremiumStatus(true)
                                }
                            }
                            override fun onPurchasePending() {
                                activity.runOnUiThread {
                                    isLoading = false
                                    Toast.makeText(context, "Purchase pending...", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onPurchaseCancelled() {
                                activity.runOnUiThread {
                                    isLoading = false
                                }
                            }
                            override fun onPurchaseAlreadyOwned() {
                                activity.runOnUiThread {
                                    isLoading = false
                                    viewModel.setPremiumStatus(true)
                                }
                            }
                            override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                                activity.runOnUiThread {
                                    isLoading = false
                                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                enabled = subscriptions.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (selectedPlan == PlanType.WEEKLY) "Start Free Trial" else "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedPlan == PlanType.WEEKLY) 
                    stringResource(R.string.cancel_anytime_at_least_24_hours_before_renewal_trial) 
                else 
                    stringResource(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial),
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isBestValue: Boolean = false
) {
    val borderColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
    val backgroundColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isSelected) Color(0xFFF59E0B) else Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = if (isBestValue) Color(0xFF10B981) else Color(0xFF94A3B8),
                    fontWeight = if (isBestValue) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            Text(
                text = price,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        if (isBestValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(bottomStart = 12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SAVE 50%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
