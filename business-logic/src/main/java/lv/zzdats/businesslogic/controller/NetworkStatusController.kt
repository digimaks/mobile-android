// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.controller

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface NetworkStatusController {
    val networkStatus: StateFlow<NetworkStatus>
    fun startMonitoring(context: Context)
    fun stopMonitoring()
}

class NetworkStatusControllerImpl : NetworkStatusController {
    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    override val networkStatus = _networkStatus.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkStatus.value = NetworkStatus.Connected
        }

        override fun onLost(network: Network) {
            _networkStatus.value = NetworkStatus.Disconnected
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            val status = when {
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkStatus.Connected
                else -> NetworkStatus.Disconnected
            }
            _networkStatus.value = status
        }
    }

    override fun startMonitoring(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)

        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        _networkStatus.value = when {
            capabilities == null -> NetworkStatus.Disconnected
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkStatus.Connected
            else -> NetworkStatus.Disconnected
        }
    }

    override fun stopMonitoring() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        connectivityManager = null
    }
}

sealed class NetworkStatus {
    object Unknown : NetworkStatus()
    object Connected : NetworkStatus()
    object Disconnected : NetworkStatus()
}