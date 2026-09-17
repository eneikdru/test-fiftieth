<script>
  import { createEventDispatcher, onMount } from 'svelte';
  const dispatch = createEventDispatcher();

  let showBanner = false;

  onMount(() => {
    // Check if consent has already been given or declined
    const consent = localStorage.getItem('telemetryConsent');
    if (consent === null) {
      showBanner = true;
    } else {
      // Fire the consent event based on previous choice
      dispatch('consent', { granted: consent === 'true' });
    }
  });

  function acceptConsent() {
    localStorage.setItem('telemetryConsent', 'true');
    showBanner = false;
    dispatch('consent', { granted: true });
  }

  function declineConsent() {
    localStorage.setItem('telemetryConsent', 'false');
    showBanner = false;
    dispatch('consent', { granted: false });
  }
</script>

{#if showBanner}
  <div
    class="fixed bottom-0 left-0 right-0 z-50 bg-[#191c1e] text-white p-4 shadow-lg border-t border-[#424752] flex flex-col sm:flex-row items-center justify-between gap-4"
    role="dialog"
    aria-labelledby="consent-banner-title"
    aria-describedby="consent-banner-desc"
  >
    <div class="flex-1">
      <h2 id="consent-banner-title" class="text-sm font-bold mb-1">Сбор аналитических данных</h2>
      <p id="consent-banner-desc" class="text-xs text-white/80">
        Мы используем телеметрию для улучшения качества сервиса. Пожалуйста, подтвердите согласие на сбор и обработку данных о вашем взаимодействии с системой.
      </p>
    </div>
    <div class="flex items-center gap-3 w-full sm:w-auto">
      <button
        type="button"
        on:click={declineConsent}
        class="flex-1 sm:flex-none px-4 py-2 border border-white text-white hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-white rounded text-xs font-semibold transition-colors"
      >
        Отклонить
      </button>
      <button
        type="button"
        on:click={acceptConsent}
        class="flex-1 sm:flex-none px-4 py-2 bg-[#003f87] text-white hover:bg-[#002b5e] focus:outline-none focus:ring-2 focus:ring-[#003f87] focus:ring-offset-2 focus:ring-offset-[#191c1e] rounded text-xs font-bold transition-colors shadow-sm"
      >
        Принять
      </button>
    </div>
  </div>
{/if}
