<script>
  import { createEventDispatcher, onMount } from 'svelte';

  const dispatch = createEventDispatcher();

  export let consentState = 'unconfirmed'; // 'granted' | 'denied' | 'unconfirmed'
  export let showBanner = true;

  onMount(() => {
    try {
      const stored = localStorage.getItem('telemetry_consent');
      if (stored === 'granted' || stored === 'denied') {
        consentState = stored;
        showBanner = false;
      } else {
        consentState = 'unconfirmed';
        showBanner = true;
      }
    } catch (e) {
      consentState = 'unconfirmed';
      showBanner = true;
    }
  });

  function acceptConsent() {
    consentState = 'granted';
    showBanner = false;
    try {
      localStorage.setItem('telemetry_consent', 'granted');
    } catch (e) {}
    dispatch('consentChanged', { state: 'granted' });
  }

  function declineConsent() {
    consentState = 'denied';
    showBanner = false;
    try {
      localStorage.setItem('telemetry_consent', 'denied');
    } catch (e) {}
    dispatch('consentChanged', { state: 'denied' });
  }

  export function resetConsent() {
    consentState = 'unconfirmed';
    showBanner = true;
    try {
      localStorage.removeItem('telemetry_consent');
    } catch (e) {}
    dispatch('consentChanged', { state: 'unconfirmed' });
  }
</script>

{#if showBanner && consentState === 'unconfirmed'}
  <aside
    id="telemetry-consent-banner"
    role="region"
    aria-label="Согласие на сбор телеметрии"
    class="fixed bottom-0 inset-x-0 z-50 p-4 sm:p-5 bg-white border-t-2 border-[#003f87] shadow-2xl transition-all font-sans text-[#191c1e]"
  >
    <div class="max-w-[1440px] mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
      <div class="flex items-start gap-3.5 max-w-3xl">
        <div class="w-10 h-10 rounded-lg bg-[#d3e4fe] text-[#003f87] flex items-center justify-center text-xl font-bold flex-shrink-0">
          🛡️
        </div>
        <div>
          <h3 class="text-sm font-bold text-[#003f87] flex items-center gap-2">
            <span>Согласие на обработку телеметрии и сбора метрик (152-ФЗ)</span>
          </h3>
          <p class="text-xs text-[#424752] mt-1 leading-relaxed">
            Мы используем техническую телеметрию и обезличенные данные навигации для измерения скорости загрузки каталога и точности поиска.
            Сбор метрик начнется только после вашего прямого согласия.
          </p>
        </div>
      </div>

      <div class="flex items-center gap-3 w-full md:w-auto justify-end flex-wrap sm:flex-nowrap">
        <button
          type="button"
          id="decline-telemetry-btn"
          on:click={declineConsent}
          class="min-h-[44px] min-w-[44px] px-5 py-2.5 bg-[#eceef0] hover:bg-[#e0e3e5] text-[#191c1e] text-xs font-semibold rounded-lg border border-[#c2c6d4] transition-colors focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2"
        >
          Отклонить
        </button>

        <button
          type="button"
          id="accept-telemetry-btn"
          on:click={acceptConsent}
          class="min-h-[44px] min-w-[44px] px-6 py-2.5 bg-[#003f87] hover:bg-[#002b5e] text-white text-xs font-bold rounded-lg shadow-sm transition-colors focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2"
        >
          Принять все
        </button>
      </div>
    </div>
  </aside>
{/if}
