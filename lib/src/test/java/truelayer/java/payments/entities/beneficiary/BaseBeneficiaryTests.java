package truelayer.java.payments.entities.beneficiary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import truelayer.java.TrueLayerException;

class BaseBeneficiaryTests {

    @Test
    @DisplayName("It should throw an exception if a user tries to access a merchant account as an external account")
    public void shouldThrowAnExceptionIfMerchantIsAccessedAsExternalAccount() {
        BaseBeneficiary beneficiary = MerchantAccount.builder().build();

        var thrown = Assertions.assertThrows(TrueLayerException.class, () -> beneficiary.asExternalAccount());
        assertEquals(
                "beneficiary is of type MerchantAccount. Consider using asMerchantAccount() instead.",
                thrown.getMessage());
    }

    @Test
    @DisplayName("It should throw an exception if a user tries to access an external account as a merchant account")
    public void shouldThrowAnExceptionIfExternalIsAccessedAsMerchantAccount() {
        BaseBeneficiary beneficiary = ExternalAccount.builder().build();

        var thrown = Assertions.assertThrows(TrueLayerException.class, () -> beneficiary.asMerchantAccount());
        assertEquals(
                "beneficiary is of type ExternalAccount. Consider using asExternalAccount() instead.",
                thrown.getMessage());
    }
}