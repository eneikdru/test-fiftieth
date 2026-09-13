<script>
  import { createEventDispatcher, onMount } from 'svelte';

  const dispatch = createEventDispatcher();

  export let isVisible = true;
  export let consentState = null; // 'accepted' | 'declined' | null

  onMount(() => {
    try {
      const stored = localStorage.getItem('cookie_consent');
      if (stored === 'true' || stored === 'accepted') {
        consentState = 'accepted';
        isVisible = false;
      } else if (stored === 'false' || stored === 'declined') {
        consentState = 'declined';
        isVisible = false;
      } else {
        consentState = null;
        isVisible = true;
      }
    } catch (e) {
      isVisible = true;
    }
  });

  function handleAccept() {
    try {
      localStorage.setItem('cookie_consent', 'accepted');
    } catch (e) {}
    consentState = 'accepted';
    isVisible = false;
    dispatch('accept');
  }

  function handleDecline() {
    try {
      localStorage.setItem('cookie_consent', 'declined');
    } catch (e) {}
    consentState = 'declined';
    isVisible = false;
    dispatch('decline');
  }
</script>

{#if isVisible}
  <div
    id="consent-banner"
    role="region"
    aria-label="Согласие на использование файлов cookie и аналитики"
    aria-live="polite"
    class="fixed bottom-0 inset-x-0 z-50 bg-white border-t border-[#e0e3e5] shadow-2xl p-4 sm:p-6 text-[#191c1e] font-sans transition-all"
  >
    <div class="max-w-[1440px] mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
      <div class="space-y-1.5 max-w-3xl">
        <h2 id="consent-title" class="text-sm font-bold text-[#191c1e] flex items-center gap-2">
          <span class="text-base" aria-hidden="true">🍪</span>
          <span>Использование файлов cookie и аналитики</span>
        </h2>
        <p id="consent-description" class="text-xs text-[#424752] leading-relaxed">
          Мы используем аналитические метрики для улучшения работы системы. В соответствии с GDPR (Art. 6, 7) и 152-ФЗ обработка файлов cookie и сбор телеметрии заблокированы по умолчанию до вашего явного согласия.
        </p>
      </div>

      <div class="flex items-center gap-3 w-full md:w-auto justify-end">
        <button
          id="consent-decline-btn"
          type="button"
          on:click={handleDecline}
          class="min-h-[44px] min-w-[44px] px-4 py-2 bg-white border border-[#c2c6d4] hover:bg-[#eceef0] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-[#191c1e] text-xs font-semibold rounded-lg transition-colors"
          aria-label="Отклонить использование файлов cookie и телеметрии"
        >
          Отклонить
        </button>
        <button
          id="consent-accept-btn"
          type="button"
          on:click={handleAccept}
          class="min-h-[44px] min-w-[44px] px-5 py-2 bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-white text-xs font-semibold rounded-lg transition-colors shadow-sm"
          aria-label="Принять все файлы cookie и разрешить сбор телеметрии"
        >
          Принять все
        </button>
      </div>
    </div>
  </div>
{/if}
