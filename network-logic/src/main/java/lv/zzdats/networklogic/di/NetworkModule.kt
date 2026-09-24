// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.di

import lv.zzdats.analyticslogic.provider.CrashlyticsProvider
import lv.zzdats.businesslogic.config.AppBuildType
import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.provider.AppInstanceIdProvider
import lv.zzdats.networklogic.api.attestation.AttestationApi
import lv.zzdats.networklogic.api.attestation.AttestationApiClient
import lv.zzdats.networklogic.api.attestation.AttestationApiClientImpl
import lv.zzdats.networklogic.api.config.VersionApi
import lv.zzdats.networklogic.api.config.VersionApiClient
import lv.zzdats.networklogic.api.config.VersionApiClientImpl
import lv.zzdats.networklogic.api.session.SessionApi
import lv.zzdats.networklogic.api.session.SessionApiClient
import lv.zzdats.networklogic.api.session.SessionApiClientImpl
import lv.zzdats.networklogic.api.wallet.WalletApi
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.networklogic.api.wallet.WalletApiClientImpl
import lv.zzdats.networklogic.error.ApiErrorHandler
import lv.zzdats.networklogic.error.ApiErrorHandlerImpl
import lv.zzdats.networklogic.interceptor.AppInstanceIdInterceptor
import lv.zzdats.networklogic.interceptor.AuthenticationInterceptor
import lv.zzdats.networklogic.repository.WalletAttestationRepository
import lv.zzdats.networklogic.repository.WalletAttestationRepositoryImpl
import lv.zzdats.networklogic.session.SecureTokenStorage
import lv.zzdats.networklogic.session.SessionManager
import lv.zzdats.networklogic.session.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@ComponentScan("lv.zzdats.networklogic")
class NetworkModule

@Factory
fun providesHttpLoggingInterceptor(configLogic: ConfigLogic) = HttpLoggingInterceptor()
    .apply {
        level = when (configLogic.appBuildType) {
            AppBuildType.DEBUG -> HttpLoggingInterceptor.Level.BODY
            AppBuildType.RELEASE -> HttpLoggingInterceptor.Level.NONE
        }
    }

@Factory
fun provideAuthInterceptor(
    tokenStorage: TokenStorage
): AuthenticationInterceptor = AuthenticationInterceptor(tokenStorage)

@Factory
fun provideAppInstanceIdInterceptor(
    appInstanceIdProvider: AppInstanceIdProvider
): AppInstanceIdInterceptor = AppInstanceIdInterceptor(appInstanceIdProvider)

@Single
fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

@Single
fun provideHttpClient(
    json: Json,
    configLogic: ConfigLogic,
    appInstanceIdProvider: AppInstanceIdProvider
): HttpClient = HttpClient(OkHttp) {
    defaultRequest {
        headers.append(
            AppInstanceIdInterceptor.APP_INSTANCE_ID_HEADER,
            appInstanceIdProvider.getAppInstanceId()
        )
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = when (configLogic.appBuildType) {
            AppBuildType.DEBUG -> LogLevel.BODY
            AppBuildType.RELEASE -> LogLevel.NONE
        }
    }

    install(ContentNegotiation) {
        json(
            json = json,
            contentType = ContentType.Application.Json
        )
    }
}

@Single
fun provideWalletAttestationRepository(
    okHttpClient: OkHttpClient
): WalletAttestationRepository = WalletAttestationRepositoryImpl(okHttpClient)

@Factory
fun provideOkHttpClient(
    appInstanceIdInterceptor: AppInstanceIdInterceptor,
    httpLoggingInterceptor: HttpLoggingInterceptor,
    authInterceptor: AuthenticationInterceptor,
    configLogic: ConfigLogic,
    logController: LogController,
): OkHttpClient {
    return OkHttpClient.Builder()
        .readTimeout(configLogic.environmentConfig.readTimeoutSeconds, TimeUnit.SECONDS)
        .connectTimeout(configLogic.environmentConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(appInstanceIdInterceptor)
        .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()
}

@Factory
fun provideConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

@Single
@Named("mainRetrofit")
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getServerHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("walletRetrofit")
fun provideWalletRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getWalletApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("sessionRetrofit")
fun provideSessionRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getSessionApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("attestationRetrofit")
fun provideAttestationRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getWalletApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
fun provideApiErrorHandler(
    logController: LogController,
    crashlyticsProvider: CrashlyticsProvider
): ApiErrorHandler {
    return ApiErrorHandlerImpl(logController, crashlyticsProvider)
}

@Factory
fun provideWalletApi(
    @Named("walletRetrofit") retrofit: Retrofit
): WalletApi = retrofit.create(WalletApi::class.java)

@Single
fun provideWalletApiClient(
    walletApi: WalletApi,
    errorHandler: ApiErrorHandler
): WalletApiClient = WalletApiClientImpl(walletApi, errorHandler)

@Single
fun provideTokenStorage(
    prefsController: PrefsController
): TokenStorage = SecureTokenStorage(prefsController)

@Factory
fun provideSessionApi(
    @Named("sessionRetrofit") retrofit: Retrofit
): SessionApi = retrofit.create(SessionApi::class.java)

@Factory
fun provideVersionApi(
    @Named("walletRetrofit") retrofit: Retrofit
): VersionApi = retrofit.create(VersionApi::class.java)

@Single
fun provideSessionApiClient(
    sessionApi: SessionApi,
    errorHandler: ApiErrorHandler
): SessionApiClient = SessionApiClientImpl(sessionApi, errorHandler)

@Single
fun provideVersionApiClient(
    versionApi: VersionApi,
    errorHandler: ApiErrorHandler
): VersionApiClient = VersionApiClientImpl(versionApi, errorHandler)

@Single
fun provideSessionManager(
    sessionApiClient: SessionApiClient,
    tokenStorage: TokenStorage,
): SessionManager = SessionManager(sessionApiClient, tokenStorage)

@Factory
fun provideAttestationApi(
    @Named("attestationRetrofit") retrofit: Retrofit
): AttestationApi = retrofit.create(AttestationApi::class.java)

@Single
fun provideAttestationApiClient(
    attestationApi: AttestationApi,
    errorHandler: ApiErrorHandler,
    secureAreaRepository: SecureAreaRepository
): AttestationApiClient = AttestationApiClientImpl(attestationApi, errorHandler, secureAreaRepository)
