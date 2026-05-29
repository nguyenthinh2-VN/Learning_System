package com.example.learning_system_spring.application.usecase.wallet;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Wallet.TransactionItemOutput;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.GetMyTransactionsUseCase;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GetMyTransactionsUseCaseTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private GetMyTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private WalletTransaction tx(Long id, TxSource source, String amount) {
        return WalletTransaction.reconstitute(
                id, 5L, "REF" + id, new BigDecimal(amount), TxStatus.COMPLETED, source,
                null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now().plusYears(1));
    }

    @Test
    void execute_EmptyResult() {
        when(walletTransactionRepository.findByUserId(5L, 0, 20))
                .thenReturn(PageResult.of(0, 0, 0, 20, List.of()));

        PageResult<TransactionItemOutput> result = useCase.execute(5L, 0, 20);

        assertEquals(0, result.totalElements());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void execute_SinglePage_MapsDirectionFromSource() {
        when(walletTransactionRepository.findByUserId(5L, 0, 20)).thenReturn(
                PageResult.of(2, 1, 0, 20, List.of(
                        tx(2L, TxSource.PURCHASE, "300000"),
                        tx(1L, TxSource.MOCK, "500000"))));

        PageResult<TransactionItemOutput> result = useCase.execute(5L, 0, 20);

        assertEquals(2, result.items().size());
        assertEquals("DEBIT", result.items().get(0).direction());   // PURCHASE
        assertEquals("CREDIT", result.items().get(1).direction());  // MOCK
        assertEquals(0, result.items().get(0).amount().compareTo(new BigDecimal("300000")));
    }

    @Test
    void execute_DirectionForAllSources() {
        when(walletTransactionRepository.findByUserId(5L, 0, 20)).thenReturn(
                PageResult.of(4, 1, 0, 20, List.of(
                        tx(1L, TxSource.MOCK, "1"),
                        tx(2L, TxSource.VIETQR, "1"),
                        tx(3L, TxSource.ADMIN, "1"),
                        tx(4L, TxSource.PURCHASE, "1"))));

        List<TransactionItemOutput> items = useCase.execute(5L, 0, 20).items();

        assertEquals("CREDIT", items.get(0).direction()); // MOCK
        assertEquals("CREDIT", items.get(1).direction()); // VIETQR
        assertEquals("CREDIT", items.get(2).direction()); // ADMIN
        assertEquals("DEBIT", items.get(3).direction());  // PURCHASE
    }

    @Test
    void execute_MultiPage_PassesPageAndSizeToRepo() {
        when(walletTransactionRepository.findByUserId(5L, 1, 10))
                .thenReturn(PageResult.of(25, 3, 1, 10, List.of()));

        PageResult<TransactionItemOutput> result = useCase.execute(5L, 1, 10);

        assertEquals(3, result.totalPages());
        verify(walletTransactionRepository).findByUserId(eq(5L), eq(1), eq(10));
    }

    @Test
    void execute_OnlyQueriesByRequesterId() {
        when(walletTransactionRepository.findByUserId(42L, 0, 20))
                .thenReturn(PageResult.of(0, 0, 0, 20, List.of()));

        useCase.execute(42L, 0, 20);

        verify(walletTransactionRepository).findByUserId(eq(42L), eq(0), eq(20));
        verifyNoMoreInteractions(walletTransactionRepository);
    }

    @Test
    void execute_InvalidSize_Throws() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, 0, 101));
        verify(walletTransactionRepository, never()).findByUserId(any(), anyInt(), anyInt());
    }

    @Test
    void execute_InvalidPage_Throws() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(5L, -1, 20));
        verify(walletTransactionRepository, never()).findByUserId(any(), anyInt(), anyInt());
    }
}
