package nextstep.session.domain;

import java.util.Currency;
import java.util.Locale;

import nextstep.session.exception.MoneyIllegalArgumentException;

public class Money {
    private final long amount;
    private final Currency currency;

    public Money(long amount) {
        this(amount, Currency.getInstance(Locale.KOREA));
    }

    public Money(long amount, Currency currency) {
        validate(amount);

        this.amount = amount;
        this.currency = currency;
    }

    private void validate(long amount) {
        if (amount < 0) {
            throw new MoneyIllegalArgumentException();
        }
    }

    public boolean isValid(long amount) {
        return this.amount == amount;
    }

    public Currency getCurrency() {
        return currency;
    }
}
