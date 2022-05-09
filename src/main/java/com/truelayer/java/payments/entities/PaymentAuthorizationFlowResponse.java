package com.truelayer.java.payments.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.truelayer.java.TrueLayerException;
import com.truelayer.java.payments.entities.paymentdetail.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonTypeInfo(
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        use = JsonTypeInfo.Id.NAME,
        property = "status",
        defaultImpl = PaymentAuthorizationFlowAuthorizing.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PaymentAuthorizationFlowAuthorizing.class, name = "authorizing"),
    @JsonSubTypes.Type(value = PaymentAuthorizationFlowAuthorizationFailed.class, name = "failed")
})
@Getter
@ToString
@EqualsAndHashCode
public abstract class PaymentAuthorizationFlowResponse {

    protected Status status;

    AuthorizationFlow authorizationFlow;

    @JsonIgnore
    public boolean isAuthorizing() {
        return this instanceof PaymentAuthorizationFlowAuthorizing;
    }

    @JsonIgnore
    public boolean isAuthorizationFailed() {
        return this instanceof PaymentAuthorizationFlowAuthorizationFailed;
    }

    @JsonIgnore
    public PaymentAuthorizationFlowAuthorizing asAuthorizing() {
        if (!isAuthorizing()) {
            throw new TrueLayerException(buildErrorMessage());
        }
        return (PaymentAuthorizationFlowAuthorizing) this;
    }

    @JsonIgnore
    public PaymentAuthorizationFlowAuthorizationFailed asAuthorizationFailed() {
        if (!isAuthorizationFailed()) {
            throw new TrueLayerException(buildErrorMessage());
        }
        return (PaymentAuthorizationFlowAuthorizationFailed) this;
    }

    private String buildErrorMessage() {
        return String.format("Response is of type %s.", this.getClass().getSimpleName());
    }
}