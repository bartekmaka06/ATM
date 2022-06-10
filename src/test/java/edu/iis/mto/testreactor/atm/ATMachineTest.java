package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.List;
import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class ATMachineTest {

    @Mock
    private Bank bank;

    private ATMachine atMachine;
    private PinCode pinCode;
    private Card card;
    private Money amount;
    private Money tooLargeAmount;
    private Money amountWithWrongCurrency;
    private Money wrongAmount;

    @BeforeEach
    void setUp() throws Exception {
        Currency currency = Money.DEFAULT_CURRENCY;
        atMachine = new ATMachine(bank, currency);
        List<BanknotesPack> banknotesPack = List.of(BanknotesPack.create(10, Banknote.PL_200),BanknotesPack.create(10, Banknote.PL_100));
        MoneyDeposit moneyDeposit=MoneyDeposit.create(currency,banknotesPack);
        atMachine.setDeposit(moneyDeposit);
        pinCode = PinCode.createPIN(1,2,3,4);
        card = Card.create("0000");
        amount = new Money(500,currency);
        amountWithWrongCurrency = new Money(500,Currency.getInstance(Locale.UK));
        tooLargeAmount=new Money(5000,currency);
        wrongAmount = new Money(500.123,currency);
    }


    @Test
    void atMachineWithdrawRequestedAmountOfMoneywithSuccess() throws ATMOperationException {
        Withdrawal result =atMachine.withdraw(pinCode,card,amount);
        List<Banknote>expectedBanknotes=List.of(Banknote.PL_200,Banknote.PL_200,Banknote.PL_100);
        List<Banknote>resultBanknotes =  result.getBanknotes();
        assertEquals(resultBanknotes, expectedBanknotes);
    }

    @Test
    void atMachineHasNotEnoughMoneyInDeposit() {
        ATMOperationException exception=assertThrows(ATMOperationException.class,()->atMachine.withdraw(pinCode,card,tooLargeAmount));
        assertEquals(exception.getErrorCode(),ErrorCode.WRONG_AMOUNT);
    }

    @Test
    void atMachineDetectedWrongAuthorization() throws AuthorizationException {
        doThrow(AuthorizationException.class).when(bank).autorize(any(),any());
        ATMOperationException exception=assertThrows(ATMOperationException.class,()->atMachine.withdraw(pinCode,card,tooLargeAmount));
        assertEquals(exception.getErrorCode(),ErrorCode.AHTHORIZATION);
    }

    @Test
    void atMachineDetectedWrongCurrencyInRequestedAmountOfMoney(){
        ATMOperationException exception=assertThrows(ATMOperationException.class,()->atMachine.withdraw(pinCode,card,amountWithWrongCurrency));
        assertEquals(exception.getErrorCode(),ErrorCode.WRONG_CURRENCY);
    }

    @Test
    void atMachineCannotWithdrawRequestedAmountOfMoneyBecauseOfNoFundsOnAccount() throws AccountException {
        doThrow(AccountException.class).when(bank).charge(any(),any());
        ATMOperationException exception=assertThrows(ATMOperationException.class,()->atMachine.withdraw(pinCode,card,amount));
        assertEquals(exception.getErrorCode(),ErrorCode.NO_FUNDS_ON_ACCOUNT);
    }

    @Test
    void atMachineDetectedWrongAmountOfMoney(){
        ATMOperationException exception=assertThrows(ATMOperationException.class,()->atMachine.withdraw(pinCode,card,wrongAmount));
        assertEquals(exception.getErrorCode(),ErrorCode.WRONG_AMOUNT);
    }

    @Test
    void bankShouldTakePartInTwoOperationInCorrectOrderDuringWithdraw() throws ATMOperationException, AuthorizationException, AccountException {
        atMachine.withdraw(pinCode,card,amount);
        InOrder inOrder = Mockito.inOrder(bank);
        inOrder.verify(bank,times(1)).autorize(any(),any());
        inOrder.verify(bank,times(1)).charge(any(),any());
    }
}
