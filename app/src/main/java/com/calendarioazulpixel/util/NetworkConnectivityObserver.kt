package com.calendarioazulpixel.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class ConnectivityStatus {
    Available, Unavailable, Losing, Lost
}

interface ConnectivityObserver {
    fun observe(): Flow<ConnectivityStatus>
}

class NetworkConnectivityObserver(context: Context) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    // No enviamos Available aquí todavía, esperamos a onCapabilitiesChanged
                    // para confirmar que realmente hay acceso a internet validado.
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    trySend(ConnectivityStatus.Losing)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectivityStatus.Lost)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    trySend(ConnectivityStatus.Unavailable)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    if (hasInternet && isValidated) {
                        trySend(ConnectivityStatus.Available)
                    } else {
                        trySend(ConnectivityStatus.Unavailable)
                    }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            
            // Comprobación inicial
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            trySend(if (isConnected) ConnectivityStatus.Available else ConnectivityStatus.Unavailable)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }
}