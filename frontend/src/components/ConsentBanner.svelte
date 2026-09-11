<script>
  import { onMount, createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let storageKey = 'consent_preferences';
  export let forceVisible = false;

  let visible = false;
  let showDetails = false;

  let preferences = {
    essential: true,
    analytics: false,
    marketing: false
  };

  onMount(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        preferences = {
          essential: true,
          analytics: Boolean(parsed.analytics),
          marketing: Boolean(parsed.marketing)
        };
        visible = forceVisible;
      } else {
        visible = true;
      }
    } catch (e) {
      visible = true;
    }
  });

  $: if (forceVisible) {
    visible = true;
  }

  function saveConsent(updatedPrefs, actionStatus) {
    preferences = {
      essential: true,
      analytics: Boolean(updatedPrefs.analytics),
      marketing: Boolean(updatedPrefs.marketing)
    };

    const payload = {
      ...preferences,
      status: actionStatus,
      timestamp: new Date().toISOString()
    };

    try {
      localStorage.setItem(storageKey, JSON.stringify(payload));
      localStorage.setItem('consent_choice', actionStatus);
    } catch (e) {
      // LocalStorage access fallback
    }

    if (typeof window !== 'undefined' && typeof window.onConsentUpdated === 'function') {
      window.onConsentUpdated(payload);
    }

    dispatch('consentSaved', payload);
    visible = false;
  }

  function handleAcceptAll() {
    saveConsent({ essential: true, analytics: true, marketing: true }, 'accepted');
  }

  function handleRejectAll() {
    saveConsent({ essential: true, analytics: false, marketing: false }, 'rejected');
  }

  function handleSaveCustom() {
    saveConsent(preferences, 'custom');
  }

  function toggleDetails() {
    showDetails = !showDetails;
  }
</script>

{#if visible}
  <div
    id="consent-banner"
    role="dialog"
    aria-modal="false"
    aria-labelledby="consent-title"
    aria-describedby="consent-description"
    class="fixed bottom-0 left-0 right-0 z-50 p-4 sm:p-6 bg-white border-t-2 border-[#003f87] shadow-2xl transition-all duration-300 font-sans text-[#191c1e]"
  >
    <div class="max-w-7xl mx-auto flex flex-col gap-4">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div class="space-y-1.5 flex-1">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center justify-center w-6 h-6 rounded-full bg-[#d3e4fe] text-[#003f87] text-xs font-bold" aria-hidden="true">
              🛡️
            </span>
            <h2 id="consent-title" class="text-base sm:text-lg font-bold text-[#003f87]">
              Настройки конфиденциальности и файлов cookie
            </h2>
          </div>
          <p id="consent-description" class="text-xs sm:text-sm text-[#424752] leading-relaxed">
            Мы используем необходимые файлы cookie для работы платформы. С вашего согласия мы также используем аналитические и маркетинговые файлы cookie для улучшения сервиса. Отклонение дополнительных cookie не ограничит доступ к базовым функциям.
          </p>
        </div>

        <!-- Primary Actions: Reject All vs Accept All are equal in visual weight and accessibility -->
        <div class="flex flex-wrap items-center gap-2 sm:gap-3 shrink-0">
          <button
            type="button"
            id="consent-reject-all-btn"
            on:click={handleRejectAll}
            class="px-4 py-2.5 bg-[#eceef0] hover:bg-[#e0e3e5] text-[#191c1e] text-xs sm:text-sm font-semibold rounded-lg border border-[#c2c6d4] focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2 transition-colors min-h-[44px] flex items-center justify-center"
            aria-label="Отклонить все необязательные cookie"
          >
            Отклонить все
          </button>

          <button
            type="button"
            id="consent-toggle-details-btn"
            on:click={toggleDetails}
            aria-expanded={showDetails}
            aria-controls="consent-details-panel"
            class="px-4 py-2.5 bg-white hover:bg-[#f7f9fb] text-[#003f87] text-xs sm:text-sm font-semibold rounded-lg border border-[#003f87] focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2 transition-colors min-h-[44px] flex items-center justify-center"
          >
            {showDetails ? 'Скрыть настройки' : 'Настройки'}
          </button>

          <button
            type="button"
            id="consent-accept-all-btn"
            on:click={handleAcceptAll}
            class="px-4 py-2.5 bg-[#003f87] hover:bg-[#002b5e] text-white text-xs sm:text-sm font-semibold rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2 transition-colors min-h-[44px] flex items-center justify-center"
            aria-label="Принять все файлы cookie"
          >
            Принять все
          </button>
        </div>
      </div>

      <!-- Granular Preferences Panel -->
      {#if showDetails}
        <div
          id="consent-details-panel"
          class="mt-2 pt-4 border-t border-[#e0e3e5] grid grid-cols-1 md:grid-cols-3 gap-4 bg-[#f7f9fb] p-4 rounded-xl"
        >
          <!-- Essential Cookies (Mandatory) -->
          <div class="p-3 bg-white border border-[#e0e3e5] rounded-lg space-y-2">
            <div class="flex items-center justify-between">
              <label for="cookie-essential" class="text-xs font-bold text-[#191c1e]">
                Обязательные (152-ФЗ)
              </label>
              <input
                type="checkbox"
                id="cookie-essential"
                checked
                disabled
                class="w-4 h-4 text-[#003f87] border-[#727784] rounded focus:ring-[#003f87] cursor-not-allowed opacity-80"
              />
            </div>
            <p class="text-[11px] text-[#424752] leading-normal">
              Необходимы для аутентификации, сессий безопасности и работы основных модулей платформы.
            </p>
          </div>

          <!-- Analytics Cookies -->
          <div class="p-3 bg-white border border-[#e0e3e5] rounded-lg space-y-2">
            <div class="flex items-center justify-between">
              <label for="cookie-analytics" class="text-xs font-bold text-[#191c1e] cursor-pointer">
                Аналитика и Производительность
              </label>
              <input
                type="checkbox"
                id="cookie-analytics"
                bind:checked={preferences.analytics}
                class="w-4 h-4 text-[#003f87] border-[#727784] rounded focus:ring-[#003f87] cursor-pointer"
              />
            </div>
            <p class="text-[11px] text-[#424752] leading-normal">
              Сбор анонимной статистики использования, времени отклика страниц и траекторий навигации.
            </p>
          </div>

          <!-- Marketing Cookies -->
          <div class="p-3 bg-white border border-[#e0e3e5] rounded-lg space-y-2">
            <div class="flex items-center justify-between">
              <label for="cookie-marketing" class="text-xs font-bold text-[#191c1e] cursor-pointer">
                Персонализация и Уведомления
              </label>
              <input
                type="checkbox"
                id="cookie-marketing"
                bind:checked={preferences.marketing}
                class="w-4 h-4 text-[#003f87] border-[#727784] rounded focus:ring-[#003f87] cursor-pointer"
              />
            </div>
            <p class="text-[11px] text-[#424752] leading-normal">
              Персонализация уведомлений об эпидемиологических отчетах и адаптивные рекомендации материалов.
            </p>
          </div>

          <div class="md:col-span-3 flex justify-end gap-2 pt-2 border-t border-[#e0e3e5]">
            <button
              type="button"
              id="consent-save-custom-btn"
              on:click={handleSaveCustom}
              class="px-4 py-2 bg-[#003f87] hover:bg-[#002b5e] text-white text-xs font-semibold rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-1 transition-colors min-h-[40px]"
            >
              Сохранить выбранные настройки
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
{/if}
