package com.truelayer.java;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import com.truelayer.java.auth.AuthenticationHandler;
import com.truelayer.java.auth.IAuthenticationHandler;
import com.truelayer.java.commonapi.ICommonApi;
import com.truelayer.java.hpp.HostedPaymentPageLinkBuilder;
import com.truelayer.java.hpp.IHostedPaymentPageLinkBuilder;
import com.truelayer.java.http.OkHttpClientFactory;
import com.truelayer.java.http.RetrofitFactory;
import com.truelayer.java.http.auth.cache.ICredentialsCache;
import com.truelayer.java.http.auth.cache.SimpleCredentialsCache;
import com.truelayer.java.http.interceptors.logging.DefaultLogConsumer;
import com.truelayer.java.mandates.IMandatesApi;
import com.truelayer.java.mandates.IMandatesHandler;
import com.truelayer.java.mandates.MandatesHandler;
import com.truelayer.java.merchantaccounts.IMerchantAccountsApi;
import com.truelayer.java.merchantaccounts.IMerchantAccountsHandler;
import com.truelayer.java.merchantaccounts.MerchantAccountsHandler;
import com.truelayer.java.payments.IPaymentsApi;
import com.truelayer.java.paymentsproviders.IPaymentsProvidersHandler;
import com.truelayer.java.paymentsproviders.PaymentsProvidersHandler;
import com.truelayer.java.versioninfo.VersionInfoLoader;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;

/**
 * Builder class for TrueLayerClient instances.
 */
public class TrueLayerClientBuilder {
    private ClientCredentials clientCredentials;

    private SigningOptions signingOptions;

    /**
     * Optional timeout configuration that defines a time limit for a complete HTTP call.
     * This includes resolving DNS, connecting, writing the request body, server processing, as well as
     * reading the response body. If not set, the internal HTTP client configuration are used.
     */
    private Duration timeout;

    /**
     * Optional configuration for internal connection pool.
     */
    private ConnectionPoolOptions connectionPoolOptions;

    /**
     * Optional execution service to be used by the internal HTTP client.
     */
    private ExecutorService requestExecutor;

    // By default, production is used
    private Environment environment = Environment.live();

    private Consumer<String> logMessageConsumer;

    private ICredentialsCache credentialsCache;

    TrueLayerClientBuilder() {}

    /**
     * Utility to set the client credentials required for Oauth2 protected endpoints.
     * @param credentials the credentials object that holds client id and secret.
     * @return the instance of the client builder used.
     * @see ClientCredentials
     */
    public TrueLayerClientBuilder clientCredentials(ClientCredentials credentials) {
        this.clientCredentials = credentials;
        return this;
    }

    /**
     * Utility to set the signing options required for payments.
     * @param signingOptions the signing options object that holds signature related informations.
     * @return the instance of the client builder used.
     * @see SigningOptions
     */
    public TrueLayerClientBuilder signingOptions(SigningOptions signingOptions) {
        this.signingOptions = signingOptions;
        return this;
    }

    /**
     * Utility to set a call timeout for the client.
     * @param timeout Optional timeout configuration that defines a time limit for a complete HTTP call.
     * This includes resolving DNS, connecting, writing the request body, server processing, as well as
     * reading the response body. If not set, the internal HTTP client configuration are used.
     * @return the instance of the client builder used.
     */
    public TrueLayerClientBuilder withTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets a connection pool for the internal HTTP client
     * @param connectionPoolOptions optional connection pool to be used
     * @return the instance of the client builder used.
     */
    public TrueLayerClientBuilder withConnectionPool(ConnectionPoolOptions connectionPoolOptions) {
        this.connectionPoolOptions = connectionPoolOptions;
        return this;
    }

    /**
     * Sets a custom HTTP request dispatcher for the internal HTTP client
     * @param requestExecutor an executor service responsible for handling the HTTP requests
     * @return the instance of the client builder used.
     */
    public TrueLayerClientBuilder withRequestExecutor(ExecutorService requestExecutor) {
        this.requestExecutor = requestExecutor;
        return this;
    }

    /**
     * Utility to configure the library to interact a specific <i>TrueLayer</i> environment.
     * By default, <i>TrueLayer</i> production environment is used.
     * @param environment the environment to use
     * @return the instance of the client builder used.
     * @see Environment
     */
    public TrueLayerClientBuilder environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Utility to enable default logs for HTTP traces.
     * @return the instance of the client builder used
     */
    public TrueLayerClientBuilder withHttpLogs() {
        this.logMessageConsumer = new DefaultLogConsumer();
        return this;
    }

    /**
     * Utility to enable custom logging for HTTP traces. Please notice that blocking
     * in the context of this consumer invocation will affect performance. An asynchronous implementation is
     * strongly advised.
     * @param logConsumer a custom log consumer
     * @return the instance of the client builder used
     */
    public TrueLayerClientBuilder withHttpLogs(Consumer<String> logConsumer) {
        this.logMessageConsumer = logConsumer;
        return this;
    }

    /**
     * Utility to enable default in memory caching for Oauth credentials.
     * @return the instance of the client builder used
     */
    public TrueLayerClientBuilder withCredentialsCaching() {
        this.credentialsCache = new SimpleCredentialsCache(Clock.systemUTC());
        return this;
    }

    /**
     * Utility to enable a custom cache for Oauth credentials.
     * @return the instance of the client builder used
     */
    public TrueLayerClientBuilder withCredentialsCaching(ICredentialsCache credentialsCache) {
        this.credentialsCache = credentialsCache;
        return this;
    }

    /**
     * Builds the Java library main class to interact with TrueLayer APIs.
     * @return a client instance
     * @see TrueLayerClient
     */
    public TrueLayerClient build() {
        if (isEmpty(clientCredentials)) {
            throw new TrueLayerException("client credentials must be set");
        }

        OkHttpClientFactory httpClientFactory = new OkHttpClientFactory(new VersionInfoLoader());

        OkHttpClient baseHttpClient = httpClientFactory.buildBaseApiClient(
                timeout, connectionPoolOptions, requestExecutor, logMessageConsumer);

        OkHttpClient authHttpClient = httpClientFactory.buildAuthApiClient(baseHttpClient, clientCredentials);

        IAuthenticationHandler authenticationHandler = AuthenticationHandler.New()
                .clientCredentials(clientCredentials)
                .httpClient(RetrofitFactory.build(authHttpClient, environment.getAuthApiUri()))
                .build();

        IHostedPaymentPageLinkBuilder hppLinkBuilder =
                HostedPaymentPageLinkBuilder.New().uri(environment.getHppUri()).build();

        // We're reusing a client with only User agent and Idempotency key interceptors and give it our base payment
        // endpoint
        ICommonApi commonApiHandler = RetrofitFactory.build(authHttpClient, environment.getPaymentsApiUri())
                .create(ICommonApi.class);

        // As per our RFC, if signing options is not configured we create a client which is able to interact
        // with the Authentication API only
        if (isEmpty(signingOptions)) {
            return new TrueLayerClient(authenticationHandler, hppLinkBuilder, commonApiHandler);
        }

        OkHttpClient paymentsHttpClient = httpClientFactory.buildPaymentsApiClient(
                authHttpClient, authenticationHandler, signingOptions, credentialsCache);

        IPaymentsApi paymentsHandler = RetrofitFactory.build(paymentsHttpClient, environment.getPaymentsApiUri())
                .create(IPaymentsApi.class);

        IPaymentsProvidersHandler paymentsProvidersHandler = PaymentsProvidersHandler.New()
                .clientCredentials(clientCredentials)
                .httpClient(RetrofitFactory.build(baseHttpClient, environment.getPaymentsApiUri()))
                .build();

        IMerchantAccountsApi merchantAccountsApi = RetrofitFactory.build(
                        paymentsHttpClient, environment.getPaymentsApiUri())
                .create(IMerchantAccountsApi.class);
        IMerchantAccountsHandler merchantAccountsHandler = new MerchantAccountsHandler(merchantAccountsApi);

        IMandatesApi mandatesApi = RetrofitFactory.build(paymentsHttpClient, environment.getPaymentsApiUri())
                .create(IMandatesApi.class);
        IMandatesHandler mandatesHandler = new MandatesHandler(mandatesApi);

        return new TrueLayerClient(
                authenticationHandler,
                paymentsHandler,
                paymentsProvidersHandler,
                merchantAccountsHandler,
                mandatesHandler,
                hppLinkBuilder,
                commonApiHandler);
    }
}
