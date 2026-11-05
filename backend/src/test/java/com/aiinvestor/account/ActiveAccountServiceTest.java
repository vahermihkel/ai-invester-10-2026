package com.aiinvestor.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aiinvestor.audit.AuditService;
import com.aiinvestor.config.TradingProperties;
import com.aiinvestor.domain.TradingMode;
import com.aiinvestor.settings.AppSettings;
import com.aiinvestor.settings.SettingsService;

class ActiveAccountServiceTest {

    private final SettingsService settingsService = mock(SettingsService.class);
    private final AccountService accounts = mock(AccountService.class);
    private final ActiveAccountChangeRepository changes = mock(ActiveAccountChangeRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final AppSettings settings = mock(AppSettings.class);

    @BeforeEach
    void setUp() {
        when(settingsService.currentForUpdate()).thenReturn(settings);
        when(accounts.find("paper")).thenReturn(Optional.of(account("paper", TradingMode.PAPER, true)));
        when(accounts.find("other-paper")).thenReturn(Optional.of(account("other-paper", TradingMode.PAPER, true)));
        when(accounts.find("live")).thenReturn(Optional.of(account("live", TradingMode.LIVE, true)));
        when(accounts.find("disabled")).thenReturn(Optional.of(account("disabled", TradingMode.PAPER, false)));
        when(accounts.find("unknown")).thenReturn(Optional.empty());
    }

    private static BrokerAccount account(String id, TradingMode mode, boolean enabled) {
        return new BrokerAccount(id, "ANY_BROKER", id, mode, "USD", enabled);
    }

    private ActiveAccountService service(String envAccount, boolean allowLive) {
        return new ActiveAccountService(settingsService, accounts, changes, audit,
                new TradingProperties(envAccount, allowLive));
    }

    private void givenStored(String active, String lastSeenEnv) {
        when(settings.getActiveAccountId()).thenReturn(active);
        when(settings.getLastSeenEnvActiveAccount()).thenReturn(lastSeenEnv);
    }

    // Switching from the UI

    @Test
    void switchesToEnabledPaperAccount() {
        givenStored("paper", "paper");

        service("paper", false).switchTo("other-paper");

        verify(settings).setActiveAccountId("other-paper");
        verify(changes).save(any());
    }

    @Test
    void refusesLiveAccountWhileLiveTradingIsOff() {
        givenStored("paper", "paper");

        assertThatThrownBy(() -> service("paper", false).switchTo("live"))
                .isInstanceOf(AccountSwitchRefusedException.class)
                .hasMessageContaining("ALLOW_LIVE_TRADING");

        verify(settings, never()).setActiveAccountId(any());
    }

    @Test
    void allowsLiveAccountOnlyWhenExplicitlyEnabled() {
        givenStored("paper", "paper");

        assertThatCode(() -> service("paper", true).switchTo("live")).doesNotThrowAnyException();

        verify(settings).setActiveAccountId("live");
    }

    @Test
    void refusesDisabledAccount() {
        givenStored("paper", "paper");

        assertThatThrownBy(() -> service("paper", false).switchTo("disabled"))
                .isInstanceOf(AccountSwitchRefusedException.class);

        verify(settings, never()).setActiveAccountId(any());
    }

    @Test
    void refusesUnknownAccount() {
        givenStored("paper", "paper");

        assertThatThrownBy(() -> service("paper", false).switchTo("unknown"))
                .isInstanceOf(AccountSwitchRefusedException.class);

        verify(settings, never()).setActiveAccountId(any());
    }

    @Test
    void switchingNeverTouchesTheKillSwitch() {
        givenStored("paper", "paper");

        service("paper", false).switchTo("other-paper");

        verify(settings, never()).setTradingEnabled(org.mockito.ArgumentMatchers.anyBoolean());
        verify(settingsService, never()).setTradingEnabled(org.mockito.ArgumentMatchers.anyBoolean());
    }

    // Startup

    @Test
    void changedEnvWinsOverLastUiChoice() {
        givenStored("other-paper", "paper-before");

        service("paper", false).resolveAtStartup();

        verify(settings).setActiveAccountId("paper");
        verify(settings).setLastSeenEnvActiveAccount("paper");
    }

    @Test
    void unchangedEnvKeepsLastUiChoice() {
        givenStored("other-paper", "paper");

        service("paper", false).resolveAtStartup();

        verify(settings, never()).setActiveAccountId(any());
    }

    @Test
    void startsWithNoActiveAccountIfEnvPointsToLiveWhileLiveTradingIsOff() {
        givenStored("paper", "paper");

        service("live", false).resolveAtStartup();

        verify(settings).setActiveAccountId(null);
    }

    @Test
    void turningOffLiveTradingDropsActiveLiveAccount() {
        givenStored("live", "live");

        service("live", false).resolveAtStartup();

        verify(settings).setActiveAccountId(null);
    }

    @Test
    void liveAccountChosenInUiIsDroppedWhenLiveTradingIsOff() {
        givenStored("live", "paper");

        service("paper", false).resolveAtStartup();

        verify(settings).setActiveAccountId(null);
    }

    @Test
    void accountThatBecameDisabledIsDropped() {
        givenStored("disabled", "paper");

        service("paper", false).resolveAtStartup();

        verify(settings).setActiveAccountId(null);
    }

    @Test
    void noEnvAndNothingStoredStaysEmpty() {
        givenStored(null, null);

        service(null, false).resolveAtStartup();

        verify(settings, never()).setActiveAccountId(any());
    }

    @Test
    void envIsUsedWhenNothingIsActive() {
        givenStored(null, "paper");

        service("paper", false).resolveAtStartup();

        verify(settings).setActiveAccountId("paper");
    }
}
