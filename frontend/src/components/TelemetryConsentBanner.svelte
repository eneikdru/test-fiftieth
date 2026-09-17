<script>
  import { onMount, createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let consentState = null; // null | 'granted' | 'denied'
  let isVisible = false;

  export function checkConsent() {
    try {
      const stored = localStorage.getItem('telemetry_consent');
      if (stored === 'granted' || stored === 'denied') {
        consentState = stored;
        isVisible = false;
      } else {
        consentState = null;
        isVisible = true;
      }
    } catch (e) {
      consentState = null;
      isVisible = true;
    }
    return consentState === 'granted';
  }

  onMount(() => {
    checkConsent();
  });

  function acceptConsent() {
    try {
      localStorage.setItem('telemetry_consent', 'granted');
    } catch (e) {}
    consentState = 'granted';
    isVisible = false;
    dispatch('consentChange', { consent: 'granted' });
  }

  function declineConsent() {
    try {
      localStorage.setItem('telemetry_consent', 'denied');
    } catch (e) {}
    consentState = 'denied';
    isVisible = false;
    dispatch('consentChange', { consent: 'denied' });
  }
</script>

{#if isVisible}
  <aside
    id="telemetry-consent-banner"
    role="region"
    aria-label="Уведомление о сборе телеметрии и конфиденциальности"
    aria-live="polite"
    class="fixed bottom-0 left-0 right-0 z-50 p-4 sm:p-5 bg-white border-t-2 border-[#003f87] shadow-2xl transition-all"
  >
    <div class="max-w-[1440px] mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
      <div class="flex items-start gap-3 max-w-3xl">
        <div class="p-2 bg-[#d3e4fe] text-[#003f87] rounded-lg shrink-0 mt-0.5">
          <span class="text-xl" aria-hidden="true">🛡️</span>
        </div>
        <div>
          <h2 class="text-sm font-bold text-[#191c1e] tracking-tight">
            Согласие на сбор сведений телеметрии (152-ФЗ)
          </h2>
          <p class="text-xs text-[#424752] mt-1 leading-relaxed">
            Мы используем обезличенную телеметрию навигации и поиска для повышения скорости работы каталога. Сбор метрик запускается строго после вашего согласия.
          </p>
        </div>
      </div>

      <div class="flex items-center gap-3 w-full sm:w-auto shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-[#e0e3e5]">
        <button
          type="button"
          id="telemetry-decline-btn"
          on:click={declineConsent}
          class="flex-1 sm:flex-none min-h-[44px] min-w-[44px] px-4 py-2.5 bg-[#f7f9fb] hover:bg-[#eceef0] focus:ring-2 focus:ring-[#003f87] focus:outline-none border border-[#c2c6d4] text-[#191c1e] text-xs font-semibold rounded-lg transition-colors"
          aria-label="Отклонить сбор телеметрии"
        >
          Отклонить
        </button>

        <button
          type="button"
          id="telemetry-accept-btn"
          on:click={acceptConsent}
          class="flex-1 sm:flex-none min-h-[44px] min-w-[44px] px-5 py-2.5 bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2 focus:outline-none text-white text-xs font-bold rounded-lg shadow-sm transition-colors"
          aria-label="Принять сбор телеметрии"
        >
          Принять
        </button>
      </div>
    </div>
  </aside>
{/if}
