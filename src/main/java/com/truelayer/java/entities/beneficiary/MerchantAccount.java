package com.truelayer.java.entities.beneficiary;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MerchantAccount extends Beneficiary {
    private final Type type = Type.MERCHANT_ACCOUNT;

    private String merchantAccountId;

    private String accountHolderName;

    private String reference;
}
