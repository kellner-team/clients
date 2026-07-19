package org.datepollsystems.waiterrobot.android.stripe

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.CollectPaymentIntentConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.datepollsystems.waiterrobot.shared.core.di.injectLoggerForClass
import org.koin.core.component.KoinComponent
import kotlin.coroutines.suspendCoroutine

suspend fun Terminal.Companion.retrievePaymentIntent(clientSecret: String) = suspendCoroutine {
    getInstance().retrievePaymentIntent(clientSecret, SuspendingPaymentIntentCallback(it))
}

suspend fun PaymentIntent.collectPaymentMethod(
    config: CollectPaymentIntentConfiguration = CollectPaymentIntentConfiguration.Builder().build()
) = suspendCancellableCoroutine {
    val cancelable = Terminal.getInstance()
        .collectPaymentMethod(this, SuspendingPaymentIntentCallback(it), config)
    it.invokeOnCancellation { cancelable.cancel(NoopCallback("Cancel collectPayment")) }
}

suspend fun PaymentIntent.confirm() = suspendCancellableCoroutine {
    val cancelable = Terminal.getInstance()
        .confirmPaymentIntent(this, SuspendingPaymentIntentCallback(it))
    it.invokeOnCancellation { cancelable.cancel(NoopCallback("Cancel confirmPayment")) }
}

suspend fun PaymentIntent.cancel() = suspendCoroutine {
    Terminal.getInstance().cancelPaymentIntent(this, SuspendingPaymentIntentCallback(it))
}

class NoopCallback(private val identifier: String) : Callback, KoinComponent {
    private val logger by injectLoggerForClass()

    override fun onFailure(e: TerminalException) {
        // Ignore
        logger.e("Failed for $identifier", e)
    }

    override fun onSuccess() {
        // Ignore
        logger.d("Success for $identifier")
    }
}
