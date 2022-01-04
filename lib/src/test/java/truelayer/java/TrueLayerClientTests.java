package truelayer.java;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import truelayer.java.auth.AuthenticationHandler;
import truelayer.java.payments.PaymentHandler;

import static org.junit.jupiter.api.Assertions.*;

public class TrueLayerClientTests {

    @Test
    @DisplayName("It should yield an authentication handler")
    @SneakyThrows
    public void itShouldBuildAnAuthenticationHandler() {
        var trueLayerClient = TrueLayerClient.builder()
                .clientCredentialsOptions(TestUtils.getClientCredentialsOptions())
                .build();

        var authenticationHandler = (AuthenticationHandler) trueLayerClient.auth();

        assertNotNull(authenticationHandler);
        assertNotNull(authenticationHandler.getAuthenticationApi());
        assertNotNull(authenticationHandler.getClientCredentialsOptions());
    }

    @Test
    @DisplayName("It should yield the same instance of the authentication handler if auth() is called multiple times")
    @SneakyThrows
    public void itShouldYieldTheSameAuthenticationHandler() {
        var trueLayerClient = TrueLayerClient.builder()
                .clientCredentialsOptions(TestUtils.getClientCredentialsOptions())
                .build();

        var authenticationHandler1 = trueLayerClient.auth();
        var authenticationHandler2 = trueLayerClient.auth();

        assertTrue(authenticationHandler1 == authenticationHandler2);
    }

    @Test
    @DisplayName("It should yield a payment handler")
    public void itShouldBuildAPaymentClient() {
        var trueLayerClient = TrueLayerClient.builder()
                .clientCredentialsOptions(TestUtils.getClientCredentialsOptions())
                .signingOptions(TestUtils.getSigningOptions())
                .build();

        var paymentsHandler = (PaymentHandler) trueLayerClient.payments();

        assertNotNull(paymentsHandler);
        assertNotNull(paymentsHandler.getPaymentsApi());
        assertNotNull(paymentsHandler.getSigningOptions());
        assertTrue(paymentsHandler.getPaymentsScopes().length > 0);
    }

    @Test
    @DisplayName("It should yield the same instance of the payment handler if payment() is called multiple times")
    @SneakyThrows
    public void itShouldYieldTheSamePaymentHandler() {
        var trueLayerClient = TrueLayerClient.builder()
                .clientCredentialsOptions(TestUtils.getClientCredentialsOptions())
                .signingOptions(TestUtils.getSigningOptions())
                .build();

        var paymentHandler1 = trueLayerClient.payments();
        var paymentHandler2 = trueLayerClient.payments();

        assertTrue(paymentHandler1 == paymentHandler2);
    }

    @Test
    @DisplayName("It should throw an exception if credentials options are missing")
    @SneakyThrows
    public void itShouldBuildASandboxTrueLaterClient() {
        var trueLayerClient = TrueLayerClient.builder()
                .build();

        var thrown = assertThrows(NullPointerException.class, () -> trueLayerClient.auth());

        assertEquals("client credentials options must be set.", thrown.getMessage());
    }

    @Test
    @DisplayName("It should throw an exception if signing options are missing")
    @SneakyThrows
    public void itShouldBuildAnExceptionIfSigningOptionMissing() {
        var trueLayerClient = TrueLayerClient.builder()
                .clientCredentialsOptions(TestUtils.getClientCredentialsOptions())
                .build();

        var thrown = assertThrows(TrueLayerException.class, () -> trueLayerClient.payments());

        assertEquals("signing options must be set.", thrown.getMessage());
    }
}