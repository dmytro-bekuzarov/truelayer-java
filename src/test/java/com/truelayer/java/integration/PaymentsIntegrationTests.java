package com.truelayer.java.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.truelayer.java.TestUtils.assertNotError;
import static org.junit.jupiter.api.Assertions.*;

import com.truelayer.java.TestUtils;
import com.truelayer.java.TestUtils.RequestStub;
import com.truelayer.java.http.entities.ApiResponse;
import com.truelayer.java.http.entities.ProblemDetails;
import com.truelayer.java.payments.entities.*;
import com.truelayer.java.payments.entities.paymentdetail.PaymentDetail;
import com.truelayer.java.payments.entities.paymentdetail.Status;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Payments integration tests")
public class PaymentsIntegrationTests extends IntegrationTests {

    public static final String A_PAYMENT_ID = "a-payment-id";

    @DisplayName("It should create and return a payment")
    @ParameterizedTest(name = "of a payment with create response status {0}")
    @ValueSource(strings = {"AUTHORIZATION_REQUIRED", "AUTHORIZED", "FAILED"})
    @SneakyThrows
    public void shouldCreateAndReturnAPaymentMerchantAccount(Status expectedStatus) {
        String jsonResponseFile = "payments/201.create_payment." + expectedStatus.getStatus() + ".json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/payments"))
                .withAuthorization()
                .withSignature()
                .withIdempotencyKey()
                .status(201)
                .bodyFile(jsonResponseFile)
                .build();
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder().build();

        ApiResponse<CreatePaymentResponse> response =
                tlClient.payments().createPayment(paymentRequest).get();

        assertNotError(response);
        CreatePaymentResponse expected = TestUtils.deserializeJsonFileTo(jsonResponseFile, CreatePaymentResponse.class);
        assertEquals(expectedStatus, response.getData().getStatus());
        assertEquals(expected, response.getData());
    }

    @Test
    @DisplayName("It should return an error if the signature is not valid")
    @SneakyThrows
    public void shouldReturnErrorIfSignatureIsInvalid() {
        String jsonResponseFile = "payments/401.invalid_signature.json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/payments"))
                .withAuthorization()
                .withSignature()
                .withIdempotencyKey()
                .status(401)
                .bodyFile(jsonResponseFile)
                .build();
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder().build();

        ApiResponse<CreatePaymentResponse> paymentResponse =
                tlClient.payments().createPayment(paymentRequest).get();

        assertTrue(paymentResponse.isError());
        ProblemDetails expected = TestUtils.deserializeJsonFileTo(jsonResponseFile, ProblemDetails.class);
        assertEquals(expected, paymentResponse.getError());
    }

    @DisplayName("It should get the payment details")
    @ParameterizedTest(name = "of a payment with status {0}")
    @ValueSource(strings = {"AUTHORIZATION_REQUIRED", "AUTHORIZING", "AUTHORIZED", "EXECUTED", "SETTLED", "FAILED"})
    @SneakyThrows
    public void shouldReturnAPaymentDetail(Status expectedStatus) {
        String jsonResponseFile = "payments/200.get_payment_by_id." + expectedStatus.getStatus() + ".json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("get")
                .path(urlPathMatching("/payments/" + A_PAYMENT_ID))
                .withAuthorization()
                .status(200)
                .bodyFile(jsonResponseFile)
                .build();

        ApiResponse<PaymentDetail> response =
                tlClient.payments().getPayment(A_PAYMENT_ID).get();

        assertNotError(response);
        PaymentDetail expected = TestUtils.deserializeJsonFileTo(jsonResponseFile, PaymentDetail.class);
        assertEquals(expectedStatus, response.getData().getStatus());
        assertEquals(expected, response.getData());
    }

    @Test
    @DisplayName("It should return an error if a payment is not found")
    @SneakyThrows
    public void shouldThrowIfPaymentNotFound() {
        String jsonResponseFile = "payments/404.payment_not_found.json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/payments"))
                .withAuthorization()
                .withSignature()
                .withIdempotencyKey()
                .status(404)
                .bodyFile(jsonResponseFile)
                .build();
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder().build();

        ApiResponse<CreatePaymentResponse> paymentResponse =
                tlClient.payments().createPayment(paymentRequest).get();

        assertTrue(paymentResponse.isError());
        ProblemDetails expected = TestUtils.deserializeJsonFileTo(jsonResponseFile, ProblemDetails.class);
        assertEquals(expected, paymentResponse.getError());
    }

    @SneakyThrows
    @Test
    @DisplayName("It should return a request invalid error")
    public void shouldThrowARequestInvalidError() {
        String jsonResponseFile = "payments/400.request_invalid.json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/payments"))
                .withAuthorization()
                .withSignature()
                .withIdempotencyKey()
                .status(400)
                .bodyFile(jsonResponseFile)
                .build();
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder().build();

        ApiResponse<CreatePaymentResponse> paymentResponse =
                tlClient.payments().createPayment(paymentRequest).get();

        assertTrue(paymentResponse.isError());
        ProblemDetails expected = TestUtils.deserializeJsonFileTo(jsonResponseFile, ProblemDetails.class);
        assertEquals(expected, paymentResponse.getError());
    }

    @SneakyThrows
    @ParameterizedTest(name = "and get a response of type {0}")
    @ValueSource(
            strings = {
                "authorizing.provider_selection",
                "authorizing.redirect",
                "authorizing.consent",
                "authorizing.form",
                "authorizing.wait",
                "failed"
            })
    @DisplayName("It should start an authorization flow")
    public void shouldStartAnAuthorizationFlow(String status) {
        String jsonResponseFile = "payments/200.start_authorization_flow." + status + ".json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathMatching("/payments/" + A_PAYMENT_ID + "/authorization-flow"))
                .withAuthorization()
                .withIdempotencyKey()
                .status(200)
                .bodyFile(jsonResponseFile)
                .build();
        StartAuthorizationFlowRequest request =
                StartAuthorizationFlowRequest.builder().build();

        ApiResponse<AuthorizationFlowResponse> response = tlClient.payments()
                .startAuthorizationFlow(A_PAYMENT_ID, request)
                .get();

        assertNotError(response);
        AuthorizationFlowResponse expected =
                TestUtils.deserializeJsonFileTo(jsonResponseFile, AuthorizationFlowResponse.class);
        assertEquals(expected, response.getData());
    }

    @SneakyThrows
    @ParameterizedTest(name = "and get a response of type {0}")
    @ValueSource(strings = {"AUTHORIZING", "FAILED"})
    @DisplayName("It should submit a provider selection")
    public void shouldSubmitProviderSelection(Status status) {
        String jsonResponseFile = "payments/200.submit_provider_selection." + status.getStatus() + ".json";
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/connect/token"))
                .status(200)
                .bodyFile("auth/200.access_token.json")
                .build();
        RequestStub.New()
                .method("post")
                .path(urlPathEqualTo("/payments/" + A_PAYMENT_ID + "/authorization-flow/actions/provider-selection"))
                .withAuthorization()
                .withIdempotencyKey()
                .status(200)
                .bodyFile(jsonResponseFile)
                .build();

        SubmitProviderSelectionRequest submitProviderSelectionRequest =
                SubmitProviderSelectionRequest.builder().build();
        ApiResponse<AuthorizationFlowResponse> response = tlClient.payments()
                .submitProviderSelection(A_PAYMENT_ID, submitProviderSelectionRequest)
                .get();

        assertNotError(response);
        AuthorizationFlowResponse expected =
                TestUtils.deserializeJsonFileTo(jsonResponseFile, AuthorizationFlowResponse.class);
        assertEquals(status, response.getData().getStatus());
        assertEquals(expected, response.getData());
    }
}
